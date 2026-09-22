/**
 * checkout.js
 * Trang thanh toán (checkout.html):
 * - Chọn địa chỉ nhận hàng (từ Hồ sơ cá nhân)
 * - Chọn nhà vận chuyển
 * - Nhập & xem trước mã giảm giá (không đặt hàng)
 * - Chọn phương thức thanh toán: COD / VNPay / MoMo
 * - Đặt hàng; nếu cổng điện tử thì chuyển hướng sang paymentUrl
 * - Hiển thị kết quả khi quay về từ cổng (?pay=success|failed)
 */
(function () {
    "use strict";

    function $(id) { return document.getElementById(id); }

    function getToken() {
        var s = window.ScAuth ? window.ScAuth.getSession() : null;
        return (s && s.token) || localStorage.getItem("jwt_token");
    }

    function authHeaders() {
        return {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + getToken()
        };
    }

    function escapeHtml(str) {
        return String(str == null ? "" : str)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
    }

    function formatVnd(value) {
        var n = Number(value);
        if (isNaN(n)) return "0₫";
        return n.toLocaleString("vi-VN") + "₫";
    }

    var cartItems = [];
    var transporters = [];
    var selectedTransporterId = null;
    var promoPreview = null; // kết quả xem trước mã KM hiện tại

    var PAYMENT_LABELS = {
        COD: "Thanh toán khi nhận hàng (COD)",
        VNPAY: "Thanh toán qua VNPay (thẻ / QR / ví)",
        MOMO: "Thanh toán qua ví MoMo"
    };
    var PAY_STATUS_LABELS = {
        UNPAID: "Chưa thanh toán (thu khi giao hàng)",
        PENDING: "Chờ cổng thanh toán xác nhận",
        PAID: "Đã thanh toán",
        FAILED: "Thanh toán thất bại"
    };

    document.addEventListener("DOMContentLoaded", function () {
        if (!getToken()) {
            window.location.href = "/login.html";
            return;
        }
        loadCart();
        loadTransporters();
        loadAddresses();
        loadMyOrders();
        handlePaymentResultParam();

        $("btnApplyPromo").addEventListener("click", applyPromo);
        $("promoCode").addEventListener("keydown", function (e) {
            if (e.key === "Enter") applyPromo();
        });
        $("btnPlaceOrder").addEventListener("click", placeOrder);
    });

    function api(method, path, body) {
        return fetch("/api/user" + path, {
            method: method,
            headers: authHeaders(),
            body: body ? JSON.stringify(body) : undefined
        }).then(function (res) {
            if (res.status === 401 || res.status === 403) {
                if (window.ScAuth) window.ScAuth.clearSession();
                window.location.href = "/login.html";
                return new Promise(function () { });
            }
            return res.json().then(function (data) {
                if (!res.ok || data.success === false) {
                    throw new Error(data.message || "Đã có lỗi xảy ra.");
                }
                return data;
            });
        });
    }

    // ============ Giỏ hàng ============

    function loadCart() {
        api("GET", "/cart").then(function (res) {
            cartItems = res.data || [];
            renderCartItems();
            updateSummary();
        }).catch(function (err) { alert(err.message); });
    }

    function renderCartItems() {
        var box = $("checkoutItems");
        if (!cartItems.length) {
            box.innerHTML = '<p class="text-muted">Giỏ hàng trống — <a href="/shop.html">tiếp tục mua sắm</a>.</p>';
            $("btnPlaceOrder").disabled = true;
            return;
        }
        $("btnPlaceOrder").disabled = false;
        box.innerHTML = cartItems.map(function (it) {
            return "<li>" + escapeHtml(it.productName)
                + (it.size ? " (size " + escapeHtml(it.size) + ")" : "")
                + (it.color ? " - màu " + escapeHtml(it.color) : "")
                + " <span>x" + it.quantity + " — " + formatVnd(it.subtotal) + "</span></li>";
        }).join("");
    }

    // ============ Nhà vận chuyển ============

    function loadTransporters() {
        api("GET", "/transporters").then(function (res) {
            transporters = res.data || [];
            var box = $("transporterBox");
            if (!transporters.length) {
                box.innerHTML = '<small class="text-muted">Chưa có nhà vận chuyển khả dụng.</small>';
                return;
            }
            box.innerHTML = transporters.map(function (t, i) {
                var active = i === 0 ? " active" : "";
                if (i === 0) selectedTransporterId = t.id;
                return '<div class="transporter-option' + active + '" data-tid="' + t.id + '">' +
                    "<span><i class='fa fa-truck'></i> " + escapeHtml(t.name) + "</span>" +
                    "<b>" + formatVnd(t.baseFee) + "</b></div>";
            }).join("");
            updateSummary();
            // Đổi nhà vận chuyển có thể đổi giảm giá loại SHIPPING -> xem lại mã KM
            if (promoPreview) applyPromo();
            box.onclick = function (e) {
                var opt = e.target.closest(".transporter-option");
                if (!opt) return;
                selectedTransporterId = parseInt(opt.getAttribute("data-tid"), 10);
                Array.prototype.forEach.call(box.querySelectorAll(".transporter-option"), function (el) {
                    el.classList.remove("active");
                });
                opt.classList.add("active");
                updateSummary();
                if (promoPreview) applyPromo();
            };
        }).catch(function () { });
    }

    // ============ Địa chỉ nhận hàng ============

    function loadAddresses() {
        fetch("/api/profile/addresses", { headers: authHeaders() })
            .then(function (r) { return r.json(); })
            .then(function (data) {
                var list = data.data || [];
                var box = $("addressBox");
                if (!list.length) {
                    box.innerHTML = '<span style="color:#dc3545;">Bạn chưa có địa chỉ nhận hàng — ' +
                        '<a href="/profile.html">thêm địa chỉ ở Hồ sơ cá nhân</a> trước khi đặt hàng.</span>';
                    return;
                }
                box.innerHTML = list.map(function (a, i) {
                    var label = "<b>" + escapeHtml(a.receiverName) + "</b> — " +
                        escapeHtml(a.receiverPhone) + "<br><small>" +
                        escapeHtml([a.detailAddress, a.ward, a.district, a.province]
                            .filter(Boolean).join(", ")) + "</small>";
                    var badge = a.isDefault ? ' <span class="badge badge-default">Mặc định</span>' : "";
                    return '<label class="address-option">' +
                        '<input type="radio" name="addr" value="' + a.id + '"' +
                        (a.isDefault || (!list.some(function (x) { return x.isDefault; }) && i === 0) ? " checked" : "") + '>' +
                        "<span>" + label + badge + "</span></label>";
                }).join("");
            }).catch(function () {
                $("addressBox").innerHTML = '<span class="text-danger">Không tải được địa chỉ nhận hàng.</span>';
            });
    }

    // ============ Mã giảm giá ============

    function applyPromo() {
        var code = $("promoCode").value.trim();
        var msg = $("promoMsg");
        msg.style.display = "none";
        if (!code) {
            promoPreview = null;
            updateSummary();
            return;
        }
        api("POST", "/cart/preview-promo", {
            promoCode: code,
            transporterId: selectedTransporterId
        }).then(function (res) {
            promoPreview = res.data;
            if (!promoPreview.valid) {
                var firstError = (promoPreview.shops || []).map(function (s) { return s.promoError; })
                    .filter(Boolean)[0];
                msg.className = "promo-msg promo-msg-error";
                msg.textContent = firstError || "Mã giảm giá không áp dụng được cho giỏ hàng này.";
            } else {
                msg.className = "promo-msg promo-msg-ok";
                msg.textContent = "Đã áp dụng \"" + (promoPreview.promoName || code) +
                    "\" — giảm " + formatVnd(promoPreview.discountAmount) + ".";
            }
            msg.style.display = "block";
            updateSummary();
        }).catch(function (err) {
            promoPreview = null;
            msg.className = "promo-msg promo-msg-error";
            msg.textContent = err.message;
            msg.style.display = "block";
            updateSummary();
        });
    }

    // ============ Tổng tiền ============

    function updateSummary() {
        var subtotal = cartItems.reduce(function (s, it) { return s + Number(it.subtotal || 0); }, 0);
        var transporter = transporters.find(function (t) { return t.id === selectedTransporterId; });
        var shipping = transporter ? Number(transporter.baseFee || 0) : 0;
        var discount = promoPreview && promoPreview.valid ? Number(promoPreview.discountAmount || 0) : 0;
        $("sumSubtotal").textContent = formatVnd(subtotal);
        $("sumShipping").textContent = formatVnd(shipping);
        $("sumDiscount").textContent = "-" + formatVnd(discount);
        $("sumTotal").textContent = formatVnd(Math.max(0, subtotal + shipping - discount));
    }

    // ============ Đặt hàng ============

    function selectedPaymentMethod() {
        var checked = document.querySelector('input[name="paymentMethod"]:checked');
        return checked ? checked.value : "COD";
    }

    function placeOrder() {
        var alertEl = $("checkoutAlert");
        alertEl.style.display = "none";
        if (!cartItems.length) {
            alert("Giỏ hàng đang trống.");
            return;
        }
        if (!$("addressBox").querySelector('input[name="addr"]:checked')) {
            alert("Vui lòng chọn địa chỉ nhận hàng (hoặc thêm địa chỉ ở Hồ sơ cá nhân).");
            return;
        }
        var payload = {
            transporterId: selectedTransporterId,
            paymentMethod: selectedPaymentMethod(),
            note: $("orderNote").value.trim()
        };
        if (promoPreview && promoPreview.valid) {
            payload.promoCode = promoPreview.promoCode;
        }
        var btn = $("btnPlaceOrder");
        btn.disabled = true;
        btn.textContent = "Đang xử lý...";
        api("POST", "/orders", payload).then(function (res) {
            var result = res.data || {};
            if (result.paymentUrl) {
                // VNPAY/MOMO: chuyển hướng sang cổng thanh toán (hoặc trang mô phỏng)
                alertEl.className = "alert auth-alert alert-info mt-2";
                alertEl.innerHTML = "Đã tạo đơn hàng — đang chuyển tới cổng thanh toán...";
                alertEl.style.display = "block";
                window.location.href = result.paymentUrl;
                return;
            }
            // COD
            var lines = (result.orders || []).map(function (o) {
                return o.orderCode + " — " + formatVnd(o.grandTotal);
            }).join("<br>");
            alertEl.className = "alert auth-alert alert-success mt-2";
            alertEl.innerHTML = "<b>Đặt hàng thành công!</b><br>" + lines +
                "<br><small>Bạn sẽ thanh toán bằng tiền mặt khi nhận hàng. " +
                "Xem trạng thái ở \"Đơn hàng của tôi\" bên dưới.</small>";
            alertEl.style.display = "block";
            cartItems = [];
            promoPreview = null;
            $("promoCode").value = "";
            loadCart();
            loadMyOrders();
        }).catch(function (err) {
            alertEl.className = "alert auth-alert alert-danger mt-2";
            alertEl.textContent = err.message;
            alertEl.style.display = "block";
        }).finally(function () {
            btn.disabled = false;
            btn.textContent = "ĐẶT HÀNG";
        });
    }

    // ============ Kết quả quay về từ cổng thanh toán ============

    function handlePaymentResultParam() {
        var params = new URLSearchParams(window.location.search);
        var pay = params.get("pay");
        if (!pay) return;
        var ref = params.get("ref") || "";
        var alertEl = $("checkoutAlert");
        if (pay === "success") {
            alertEl.className = "alert auth-alert alert-success mt-2";
            alertEl.innerHTML = "<b>Thanh toán thành công!</b> Mã giao dịch: " + escapeHtml(ref) +
                "<br><small>Đơn hàng của bạn đã được ghi nhận là ĐÃ THANH TOÁN và chờ chi nhánh xác nhận giao hàng.</small>";
        } else {
            alertEl.className = "alert auth-alert alert-danger mt-2";
            alertEl.innerHTML = "<b>Thanh toán thất bại hoặc bị hủy.</b> Mã giao dịch: " + escapeHtml(ref) +
                "<br><small>Các đơn chưa thanh toán của giao dịch này đã được hủy và hoàn lại tồn kho. " +
                "Bạn có thể đặt hàng lại ở trang <a href=\"/shop.html\">cửa hàng</a>.</small>";
        }
        alertEl.style.display = "block";
        // Dọn URL để refresh không hiện lại thông báo cũ
        window.history.replaceState({}, "", "/checkout.html");
        loadCart();
        loadMyOrders();
    }

    // ============ Đơn hàng của tôi ============

    function loadMyOrders() {
        api("GET", "/orders").then(function (res) {
            renderMyOrders(res.data || []);
        }).catch(function () { });
    }

    function renderMyOrders(orders) {
        var container = $("myOrders");
        if (!container) return;
        if (!orders.length) {
            container.innerHTML = '<p class="text-muted mb-0">Bạn chưa có đơn hàng nào.</p>';
            return;
        }
        container.innerHTML = orders.map(function (o) {
            var items = (o.items || []).map(function (it) {
                return "<li>" + escapeHtml(it.productName) +
                    (it.size ? " (size " + escapeHtml(it.size) + ")" : "") +
                    (it.color ? " - màu " + escapeHtml(it.color) : "") +
                    " x" + it.quantity + "</li>";
            }).join("");
            var payBadge = '<span class="pay-status-badge pay-' + escapeHtml(o.paymentStatus || "UNPAID") + '">' +
                escapeHtml(PAY_STATUS_LABELS[o.paymentStatus] || o.paymentStatus || "") + "</span>";
            var actions = "";
            if (["NEW", "CONFIRMED", "SHIPPING"].indexOf(o.status) !== -1) {
                actions = '<button type="button" class="btn btn-sm btn-link btn-link-danger" ' +
                    'data-cancel="' + o.id + '">Hủy đơn</button>';
            } else if (o.status === "DELIVERED") {
                actions = '<button type="button" class="btn btn-sm btn-outline-store" ' +
                    'data-receive="' + o.id + '">Đã nhận được hàng</button> ' +
                    '<button type="button" class="btn btn-sm btn-link btn-link-danger" ' +
                    'data-cancel="' + o.id + '">Hủy đơn</button> ' +
                    '<button type="button" class="btn btn-sm btn-link btn-link-danger" ' +
                    'data-return="' + o.id + '">Yêu cầu trả hàng</button>';
            } else if (o.status === "RECEIVED") {
                actions = '<button type="button" class="btn btn-sm btn-link btn-link-danger" ' +
                    'data-return="' + o.id + '">Yêu cầu trả hàng</button>';
            }
            return '<div class="cart-item" style="align-items:flex-start;">' +
                "<div style='flex:1;'>" +
                "<b>" + escapeHtml(o.orderCode) + "</b> " +
                '<span class="order-status-badge status-' + escapeHtml(o.status) + '">' +
                escapeHtml(o.statusLabel || o.status) + "</span> " + payBadge +
                '<div><small class="text-muted">' + escapeHtml(o.shopName || "") + " — " +
                escapeHtml(o.createdAt || "") + " — " + escapeHtml(PAYMENT_LABELS[o.paymentMethod] || o.paymentMethod || "COD") + "</small></div>" +
                '<ul class="text-muted mb-0 mt-1" style="font-size:13px;">' + items + "</ul>" +
                (o.cancelReason ? '<small class="text-danger">Lý do hủy/trả: ' + escapeHtml(o.cancelReason) + "</small>" : "") +
                actions +
                "</div>" +
                '<div class="text-right"><b style="color:#e53637;">' + formatVnd(o.grandTotal) + "</b>" +
                (o.promoName ? '<div><small class="text-success">KM: ' + escapeHtml(o.promoName) + "</small></div>" : "") +
                (o.transporterName ? '<div><small class="text-muted">VC: ' + escapeHtml(o.transporterName) + "</small></div>" : "") +
                "</div></div>";
        }).join("");

        container.onclick = function (e) {
            var btn = e.target.closest("button");
            if (!btn) return;
            if (btn.getAttribute("data-cancel") !== null) {
                var id = btn.getAttribute("data-cancel");
                var reason = prompt("Lý do bạn muốn hủy đơn này:");
                if (reason === null) return;
                if (!reason.trim()) { alert("Vui lòng nhập lý do hủy đơn."); return; }
                api("PATCH", "/orders/" + id + "/cancel", { reason: reason.trim() }).then(function () {
                    alert("Đã hủy đơn hàng!");
                    loadMyOrders();
                }).catch(function (err) { alert(err.message); });
            } else if (btn.getAttribute("data-receive") !== null) {
                api("PATCH", "/orders/" + btn.getAttribute("data-receive") + "/receive", null).then(function () {
                    alert("Cảm ơn bạn đã xác nhận nhận hàng!");
                    loadMyOrders();
                }).catch(function (err) { alert(err.message); });
            } else if (btn.getAttribute("data-return") !== null) {
                var tid = btn.getAttribute("data-return");
                var rreason = prompt("Lý do trả hàng - hoàn tiền (VD: hàng lỗi, sai mẫu...):");
                if (rreason === null) return;
                if (!rreason.trim()) { alert("Vui lòng nhập lý do trả hàng."); return; }
                api("PATCH", "/orders/" + tid + "/return-request", { reason: rreason.trim() }).then(function () {
                    alert("Đã gửi yêu cầu trả hàng tới chi nhánh!");
                    loadMyOrders();
                }).catch(function (err) { alert(err.message); });
            }
        };
    }
})();

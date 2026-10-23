/**
 * cart.js
 * Trang giỏ hàng (shopping-cart.html):
 * - Xem/sửa giỏ hàng (tăng giảm số lượng, xóa)
 * - Chọn nhà vận chuyển, nhập mã khuyến mãi, đặt hàng
 * - Xem các đơn hàng đã đặt kèm trạng thái
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

    document.addEventListener("DOMContentLoaded", function () {
        if (!getToken()) {
            $("cartNotLoggedIn").style.display = "block";
            return;
        }
        $("cartWrapper").style.display = "block";
        loadCart();
        loadTransporters();
        loadMyOrders();
        loadDefaultAddress();

        $("btnClearCart").addEventListener("click", function () {
            if (!confirm("Xóa toàn bộ sản phẩm trong giỏ hàng?")) return;
            window.StoreChainCartAPI.clear().then(function () {
                loadCart();
            }).catch(function (err) { alert(err.message); });
        });
        $("btnCheckout").addEventListener("click", checkout);
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
        window.StoreChainCartAPI.get().then(function (items) {
            cartItems = Array.isArray(items) ? items : [];
            renderCart();
            updateSummary();
        }).catch(function (err) { alert(err.message); });
    }

    function renderCart() {
        var container = $("cartItems");
        if (!cartItems.length) {
            container.innerHTML =
                '<p class="text-muted mb-0">Giỏ hàng trống. <a href="/index.html">Tiếp tục mua sắm</a></p>';
            return;
        }
        container.innerHTML = cartItems.map(function (it) {
            return '<div class="cart-item">' +
                '<a href="/product-detail.html?productId=' + it.productId + '">' +
                '<img src="' + escapeHtml(it.imageUrl || "/img/product/product-1.jpg") + '" alt=""></a>' +
                '<div style="flex:1;">' +
                '<a href="/product-detail.html?productId=' + it.productId + '" style="color:inherit;">' +
                '<b>' + escapeHtml(it.productName) + "</b></a>" +
                (it.size ? '<div><small class="text-muted">Size: ' + escapeHtml(it.size) + "</small></div>" : "") +
                (it.color ? '<div><small class="text-muted">Màu: ' + escapeHtml(it.color) + "</small></div>" : "") +
                '<div><small class="text-muted">Male Fashion — ' +
                formatVnd(it.price) + "</small></div>" +
                "</div>" +
                '<div class="text-right">' +
                '<div class="qty-box">' +
                '<button type="button" data-dec="' + it.id + '">−</button>' +
                '<input type="text" value="' + it.quantity + '" readonly>' +
                '<button type="button" data-inc="' + it.id + '">+</button>' +
                "</div>" +
                '<div class="mt-1" style="font-weight:800;color:#e53637;">' + formatVnd(it.subtotal) + "</div>" +
                '<button type="button" data-del="' + it.id + '" ' +
                'style="border:none;background:none;color:#999;font-size:12px;text-decoration:underline;cursor:pointer;">Xóa</button>' +
                "</div></div>";
        }).join("");

        container.onclick = function (e) {
            var btn = e.target.closest("button");
            if (!btn) return;
            var itemId, item, qty;
            if (btn.getAttribute("data-inc") !== null) {
                itemId = parseInt(btn.getAttribute("data-inc"), 10);
                item = cartItems.find(function (x) { return x.id === itemId; });
                qty = item.quantity + 1;
                window.StoreChainCartAPI.update(itemId, qty)
                    .then(function () { loadCart(); })
                    .catch(function (err) { alert(err.message); });
            } else if (btn.getAttribute("data-dec") !== null) {
                itemId = parseInt(btn.getAttribute("data-dec"), 10);
                item = cartItems.find(function (x) { return x.id === itemId; });
                if (item.quantity <= 1) return;
                window.StoreChainCartAPI.update(itemId, item.quantity - 1)
                    .then(function () { loadCart(); })
                    .catch(function (err) { alert(err.message); });
            } else if (btn.getAttribute("data-del") !== null) {
                window.StoreChainCartAPI.remove(btn.getAttribute("data-del"))
                    .then(function () { loadCart(); updateSummary(); })
                    .catch(function (err) { alert(err.message); });
            }
        };
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
                var active = (i === 0) ? " active" : "";
                if (i === 0) selectedTransporterId = t.id;
                return '<div class="transporter-option' + active + '" data-tid="' + t.id + '">' +
                    "<span><i class='fa fa-truck'></i> " + escapeHtml(t.name) + "</span>" +
                    "<b>" + formatVnd(t.baseFee) + "</b></div>";
            }).join("");
            updateSummary();
            box.onclick = function (e) {
                var opt = e.target.closest(".transporter-option");
                if (!opt) return;
                selectedTransporterId = parseInt(opt.getAttribute("data-tid"), 10);
                Array.prototype.forEach.call(box.querySelectorAll(".transporter-option"), function (el) {
                    el.classList.remove("active");
                });
                opt.classList.add("active");
                updateSummary();
            };
        }).catch(function () { });
    }

    // ============ Địa chỉ mặc định ============

    function loadDefaultAddress() {
        fetch("/api/profile/addresses", { headers: authHeaders() })
            .then(function (r) { return r.json(); })
            .then(function (data) {
                var list = data.data || [];
                if (!list.length) {
                    $("addressBox").innerHTML = '<span style="color:#dc3545;">Bạn chưa có địa chỉ nhận hàng — hãy thêm ở Hồ sơ cá nhân.</span>';
                    return;
                }
                var a = list.find(function (x) { return x.isDefault; }) || list[0];
                $("addressBox").innerHTML = "<b>" + escapeHtml(a.receiverName) + "</b> — " +
                    escapeHtml(a.receiverPhone) + "<br>" +
                    escapeHtml([a.detailAddress, a.ward, a.district, a.province].filter(Boolean).join(", "));
            }).catch(function () { });
    }

    // ============ Tổng tiền (ước tính trước khi đặt) ============

    function updateSummary() {
        var subtotal = cartItems.reduce(function (s, it) { return s + Number(it.subtotal || 0); }, 0);
        var transporter = transporters.find(function (t) { return t.id === selectedTransporterId; });
        var shipping = transporter ? Number(transporter.baseFee || 0) : 0;
        // Ước tính khuyến mãi từ mã đang chọn (server tính lại chính xác khi đặt hàng)
        var coupon = window.appliedCouponData || null;
        var discount = 0;
        if (coupon && coupon.discountValue) {
            var value = Number(coupon.discountValue);
            if (coupon.promoType === "SHIPPING") {
                // Voucher chi nhánh loại SHIPPING: trừ vào phí vận chuyển
                discount = Math.min(value, shipping);
            } else if (coupon.discountType === "PERCENTAGE") {
                discount = Math.round(subtotal * value / 100);
                if (coupon.maxDiscountAmount) discount = Math.min(discount, Number(coupon.maxDiscountAmount));
                discount = Math.min(discount, subtotal);
            } else {
                discount = Math.min(value, subtotal);
            }
        }
        $("sumSubtotal").textContent = formatVnd(subtotal);
        $("sumShipping").textContent = formatVnd(shipping);
        $("sumDiscount").textContent = "-" + formatVnd(discount);
        $("sumTotal").textContent = formatVnd(subtotal + shipping - discount);
    }
    // shopping-cart.html gọi lại để cập nhật tổng tiền khi khách chọn/bỏ mã giảm giá
    window.updateSummary = updateSummary;

    document.addEventListener("input", function (e) {
    });

    // ============ Đặt hàng ============

    function checkout() {
        var alertEl = $("checkoutAlert");
        alertEl.style.display = "none";
        if (!cartItems.length) {
            alert("Giỏ hàng đang trống.");
            return;
        }
        // Mã giảm giá đang chọn ở dropdown voucher (server tự validate theo chi nhánh xử lý đơn)
        var couponSelect = document.getElementById("cart-coupon-select");
        var promoCode = couponSelect && couponSelect.value ? couponSelect.value.trim().toUpperCase() : null;
        var methodEl = document.querySelector('input[name="paymentMethod"]:checked');
        var paymentMethod = methodEl ? methodEl.value : "COD";
        var payload = {
            transporterId: selectedTransporterId,
            note: $("orderNote").value.trim(),
            promoCode: promoCode,
            paymentMethod: paymentMethod
        };
        api("POST", "/orders", payload).then(function (res) {
            var orders = res.data || [];
            // Đơn đã tạo — clear mã đã dùng để không áp lại cho lần đặt sau
            if (couponSelect) couponSelect.value = "";
            window.appliedCouponData = null;
            if (typeof appliedCoupon !== "undefined") appliedCoupon = null;
            $("cart-coupon-msg").textContent = "";
            updateSummary();
            var lines = orders.map(function (o) {
                return o.orderCode + " — " + formatVnd(o.grandTotal);
            }).join("<br>");
            alertEl.className = "alert auth-alert alert-success mt-2";
            alertEl.innerHTML = "<b>Đặt hàng thành công!</b><br>" + lines +
                '<br><small class="text-muted">Hệ thống đã phân phối đơn về chi nhánh gần địa chỉ của bạn nhất. ' +
                'Chi nhánh sẽ liên hệ xác nhận. Xem trạng thái ở "Đơn hàng của tôi".</small>';
            alertEl.style.display = "block";
            loadCart();
            loadMyOrders();
            // Thanh toán online: chuyển sang cổng ngay sau khi tạo đơn (các đơn tách cùng một paymentRef).
            // Nếu lỗi thì đơn vẫn còn, khách bấm "Thanh toán lại" ở danh sách đơn.
            if (paymentMethod !== "COD" && orders.length && orders[0].paymentRef && window.ScShop) {
                alertEl.innerHTML += "<br><b>Đang chuyển tới cổng thanh toán " + paymentMethod + "...</b>";
                ScShop.startPayment(orders[0].paymentRef).catch(function (err) {
                    alertEl.className = "alert auth-alert alert-warning mt-2";
                    alertEl.innerHTML = "Đơn đã được tạo nhưng chưa mở được cổng thanh toán: " +
                        escapeHtml(err.message) + '<br>Bạn có thể bấm "Thanh toán lại" ở mục Đơn hàng của tôi.';
                });
            }
        }).catch(function (err) {
            alertEl.className = "alert auth-alert alert-danger mt-2";
            alertEl.textContent = err.message;
            alertEl.style.display = "block";
        });
    }

    // ============ Đơn hàng của tôi ============

    function loadMyOrders() {
        api("GET", "/orders").then(function (res) {
            renderMyOrders(res.data || []);
        }).catch(function () { });
    }

    function renderMyOrders(orders) {
        var container = $("myOrders");
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
            // Khách có thể hủy đơn trước khi nhận hàng, xác nhận đã nhận,
            // hoặc yêu cầu trả hàng - hoàn tiền (chờ chi nhánh duyệt)
            var actions = "";
            if (["NEW", "CONFIRMED", "SHIPPING"].indexOf(o.status) !== -1) {
                actions = '<button type="button" class="btn btn-sm btn-link btn-link-danger" ' +
                    'data-customer-cancel="' + o.id + '">Hủy đơn</button>';
            } else if (o.status === "DELIVERED") {
                actions = '<button type="button" class="btn btn-sm btn-outline-store" ' +
                    'data-customer-receive="' + o.id + '">Đã nhận được hàng</button> ' +
                    '<button type="button" class="btn btn-sm btn-link btn-link-danger" ' +
                    'data-customer-cancel="' + o.id + '">Hủy đơn</button> ' +
                    '<button type="button" class="btn btn-sm btn-link btn-link-danger" ' +
                    'data-customer-return="' + o.id + '">Yêu cầu trả hàng</button>';
            } else if (o.status === "RECEIVED") {
                actions = '<button type="button" class="btn btn-sm btn-link btn-link-danger" ' +
                    'data-customer-return="' + o.id + '">Yêu cầu trả hàng</button>';
            }
            if (window.ScShop && ScShop.canRetryPayment(o)) {
                actions = '<button type="button" class="btn btn-sm btn-store mr-2" data-customer-pay="' +
                    escapeHtml(o.paymentRef) + '">Thanh toán lại</button>' + actions;
            }
            return '<div class="cart-item" style="align-items:flex-start;">' +
                "<div style='flex:1;'>" +
                "<b>" + escapeHtml(o.orderCode) + "</b> " +
                '<span class="order-status-badge status-' + escapeHtml(o.status) + '">' +
                escapeHtml(o.statusLabel || o.status) + "</span>" +
                '<div><small class="text-muted">' + escapeHtml(o.shopName || "") + " — " +
                escapeHtml(o.createdAt || "") + "</small></div>" +
                (window.ScShop ? ScShop.paymentHtml(o) : "") +
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
            if (btn.getAttribute("data-customer-pay") !== null) {
                ScShop.startPayment(btn.getAttribute("data-customer-pay")).catch(function (err) { alert(err.message); });
            } else if (btn.getAttribute("data-customer-cancel") !== null) {
                var id = btn.getAttribute("data-customer-cancel");
                var reason = prompt("Lý do bạn muốn hủy đơn này:");
                if (reason === null) return;
                if (!reason.trim()) { alert("Vui lòng nhập lý do hủy đơn."); return; }
                api("PATCH", "/orders/" + id + "/cancel", { reason: reason.trim() }).then(function () {
                    alert("Đã hủy đơn hàng!");
                    loadMyOrders();
                }).catch(function (err) { alert(err.message); });
            } else if (btn.getAttribute("data-customer-receive") !== null) {
                var rid = btn.getAttribute("data-customer-receive");
                api("PATCH", "/orders/" + rid + "/receive", null).then(function () {
                    alert("Cảm ơn bạn đã xác nhận nhận hàng!");
                    loadMyOrders();
                }).catch(function (err) { alert(err.message); });
            } else if (btn.getAttribute("data-customer-return") !== null) {
                var tid = btn.getAttribute("data-customer-return");
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

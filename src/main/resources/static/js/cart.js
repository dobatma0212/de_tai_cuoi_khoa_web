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
            api("DELETE", "/cart").then(function () {
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
        api("GET", "/cart").then(function (res) {
            cartItems = res.data || [];
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
                '<div><small class="text-muted">' + escapeHtml(it.shopName || "") + " — " +
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
                api("PATCH", "/cart/" + itemId, { quantity: qty })
                    .then(function () { loadCart(); })
                    .catch(function (err) { alert(err.message); });
            } else if (btn.getAttribute("data-dec") !== null) {
                itemId = parseInt(btn.getAttribute("data-dec"), 10);
                item = cartItems.find(function (x) { return x.id === itemId; });
                if (item.quantity <= 1) return;
                api("PATCH", "/cart/" + itemId, { quantity: item.quantity - 1 })
                    .then(function () { loadCart(); })
                    .catch(function (err) { alert(err.message); });
            } else if (btn.getAttribute("data-del") !== null) {
                api("DELETE", "/cart/" + btn.getAttribute("data-del"))
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
        $("sumSubtotal").textContent = formatVnd(subtotal);
        $("sumShipping").textContent = formatVnd(shipping);
        // Khuyến mãi chỉ server tính chính xác được, hiển thị tạm "-chờ xử lý" khi có mã
        var promo = $("promoCode").value.trim();
        $("sumDiscount").textContent = promo ? "áp dụng khi đặt" : "-0₫";
        $("sumTotal").textContent = formatVnd(subtotal + shipping);
    }

    document.addEventListener("input", function (e) {
        if (e.target && e.target.id === "promoCode") updateSummary();
    });

    // ============ Đặt hàng ============

    function checkout() {
        var alertEl = $("checkoutAlert");
        alertEl.style.display = "none";
        if (!cartItems.length) {
            alert("Giỏ hàng đang trống.");
            return;
        }
        var payload = {
            promoCode: $("promoCode").value.trim(),
            transporterId: selectedTransporterId,
            note: $("orderNote").value.trim()
        };
        api("POST", "/orders", payload).then(function (res) {
            var orders = res.data || [];
            var lines = orders.map(function (o) {
                return o.orderCode + " — " + formatVnd(o.grandTotal) + " (" + escapeHtml(o.shopName || "") + ")";
            }).join("<br>");
            alertEl.className = "alert auth-alert alert-success mt-2";
            alertEl.innerHTML = "<b>Đặt hàng thành công!</b><br>" + lines +
                '<br><small class="text-muted">Chi nhánh sẽ liên hệ xác nhận. Xem trạng thái ở "Đơn hàng của tôi".</small>';
            alertEl.style.display = "block";
            loadCart();
            loadMyOrders();
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
                    " x" + it.quantity + "</li>";
            }).join("");
            return '<div class="cart-item" style="align-items:flex-start;">' +
                "<div style='flex:1;'>" +
                "<b>" + escapeHtml(o.orderCode) + "</b> " +
                '<span class="order-status-badge status-' + escapeHtml(o.status) + '">' +
                escapeHtml(o.statusLabel || o.status) + "</span>" +
                '<div><small class="text-muted">' + escapeHtml(o.shopName || "") + " — " +
                escapeHtml(o.createdAt || "") + "</small></div>" +
                '<ul class="text-muted mb-0 mt-1" style="font-size:13px;">' + items + "</ul>" +
                "</div>" +
                '<div class="text-right"><b style="color:#e53637;">' + formatVnd(o.grandTotal) + "</b>" +
                (o.promoName ? '<div><small class="text-success">KM: ' + escapeHtml(o.promoName) + "</small></div>" : "") +
                (o.transporterName ? '<div><small class="text-muted">VC: ' + escapeHtml(o.transporterName) + "</small></div>" : "") +
                "</div></div>";
        }).join("");
    }
})();

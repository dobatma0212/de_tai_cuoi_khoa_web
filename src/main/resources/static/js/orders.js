/**
 * orders.js — Trang "Đơn hàng của tôi" (orders.html)
 * - Danh sách đơn hàng của người dùng đã đăng nhập
 * - Hủy đơn, xác nhận đã nhận hàng, yêu cầu trả hàng - hoàn tiền
 */
(function () {
    "use strict";

    var S = window.ScStore;
    var PAYMENT_LABELS = {
        COD: "Thanh toán khi nhận hàng (COD)",
        VNPAY: "Thanh toán qua VNPay",
        MOMO: "Thanh toán qua ví MoMo"
    };
    var PAY_STATUS_LABELS = {
        UNPAID: "Chưa thanh toán (thu khi giao hàng)",
        PENDING: "Chờ cổng thanh toán xác nhận",
        PAID: "Đã thanh toán",
        FAILED: "Thanh toán thất bại"
    };

    document.addEventListener("DOMContentLoaded", function () {
        if (!S.getToken()) {
            document.getElementById("ordersNotLoggedIn").style.display = "block";
            return;
        }
        document.getElementById("ordersWrapper").style.display = "block";
        loadOrders();
    });

    function loadOrders() {
        var box = document.getElementById("ordersList");
        box.innerHTML = '<p class="text-muted">Đang tải đơn hàng...</p>';
        S.api("GET", "/orders").then(function (res) {
            render(res.data || []);
        }).catch(function (err) {
            box.innerHTML = '<p class="text-danger mb-0">' + S.escapeHtml(err.message) + "</p>";
        });
    }

    function render(orders) {
        var box = document.getElementById("ordersList");
        if (!orders.length) {
            box.innerHTML = '<div class="order-card text-center"><p class="text-muted mb-2">' +
                "Bạn chưa có đơn hàng nào.</p>" +
                '<a href="/shop.html" class="btn-store" style="text-decoration:none;display:inline-block;">Mua sắm ngay</a></div>';
            return;
        }
        box.innerHTML = orders.map(function (o) {
            var items = (o.items || []).map(function (it) {
                return '<div class="order-item-row">' +
                    '<img src="' + S.imageUrl(it.imageUrl) + '" alt="">' +
                    '<div style="flex:1;">' +
                    "  <b>" + S.escapeHtml(it.productName) + "</b>" +
                    '  <div><small class="text-muted">' +
                    (it.size ? "Size: " + S.escapeHtml(it.size) + " · " : "") +
                    (it.color ? "Màu: " + S.escapeHtml(it.color) + " · " : "") +
                    "SL: " + it.quantity + "</small></div>" +
                    "</div>" +
                    '<div class="text-right"><b>' + S.formatVnd(it.subtotal) + "</b></div>" +
                    "</div>";
            }).join("");

            var actions = "";
            if (["NEW", "CONFIRMED", "SHIPPING"].indexOf(o.status) !== -1) {
                actions = '<button type="button" class="btn-outline-store" data-cancel="' + o.id + '">Hủy đơn</button>';
            } else if (o.status === "DELIVERED") {
                actions = '<button type="button" class="btn-outline-store" data-receive="' + o.id + '">Đã nhận được hàng</button> ' +
                    '<button type="button" class="btn-link-danger" data-cancel="' + o.id + '">Hủy đơn</button> ' +
                    '<button type="button" class="btn-link-danger" data-return="' + o.id + '">Yêu cầu trả hàng</button>';
            } else if (o.status === "RECEIVED") {
                actions = '<button type="button" class="btn-link-danger" data-return="' + o.id + '">Yêu cầu trả hàng</button>';
            }

            return '<div class="order-card">' +
                '<div class="d-flex justify-content-between flex-wrap">' +
                "  <div><b>" + S.escapeHtml(o.orderCode) + "</b> " +
                '    <span class="order-status-badge status-' + S.escapeHtml(o.status) + '">' +
                       S.escapeHtml(o.statusLabel || o.status) + "</span> " +
                '    <span class="pay-status-badge pay-' + S.escapeHtml(o.paymentStatus || "UNPAID") + '">' +
                       S.escapeHtml(PAY_STATUS_LABELS[o.paymentStatus] || o.paymentStatus || "") + "</span>" +
                '    <div><small class="text-muted">' + S.escapeHtml(o.shopName || "") + " · " +
                       S.escapeHtml(o.createdAt || "") + " · " +
                       S.escapeHtml(PAYMENT_LABELS[o.paymentMethod] || o.paymentMethod || "COD") + "</small></div>" +
                "  </div>" +
                '  <div class="text-right"><b style="color:#e53637;font-size:18px;">' +
                     S.formatVnd(o.grandTotal) + "</b>" +
                (o.promoName ? '<div><small class="text-success">KM: ' + S.escapeHtml(o.promoName) + "</small></div>" : "") +
                (o.transporterName ? '<div><small class="text-muted">VC: ' + S.escapeHtml(o.transporterName) + "</small></div>" : "") +
                "  </div>" +
                "</div>" +
                '<div class="mt-2">' + items + "</div>" +
                (o.receiverName ? '<div class="mt-2"><small class="text-muted">Giao tới: ' +
                    S.escapeHtml(o.receiverName) + " — " + S.escapeHtml(o.receiverPhone || "") + ", " +
                    S.escapeHtml(o.receiverAddress || "") + "</small></div>" : "") +
                (o.cancelReason ? '<div class="mt-2"><small class="text-danger">Lý do hủy/trả: ' +
                    S.escapeHtml(o.cancelReason) + "</small></div>" : "") +
                (actions ? '<div class="mt-3">' + actions + "</div>" : "") +
                "</div>";
        }).join("");

        box.onclick = function (e) {
            var btn = e.target.closest("button");
            if (!btn) return;
            if (btn.hasAttribute("data-cancel")) {
                var id = btn.getAttribute("data-cancel");
                var reason = prompt("Lý do bạn muốn hủy đơn này:");
                if (reason === null) return;
                if (!reason.trim()) { alert("Vui lòng nhập lý do hủy đơn."); return; }
                S.api("PATCH", "/orders/" + id + "/cancel", { reason: reason.trim() }).then(function () {
                    S.toast("Đã hủy đơn hàng!", "success");
                    loadOrders();
                }).catch(function (err) { S.toast(err.message, "error"); });
            } else if (btn.hasAttribute("data-receive")) {
                S.api("PATCH", "/orders/" + btn.getAttribute("data-receive") + "/receive", null).then(function () {
                    S.toast("Cảm ơn bạn đã xác nhận nhận hàng!", "success");
                    loadOrders();
                }).catch(function (err) { S.toast(err.message, "error"); });
            } else if (btn.hasAttribute("data-return")) {
                var tid = btn.getAttribute("data-return");
                var rreason = prompt("Lý do trả hàng - hoàn tiền (VD: hàng lỗi, sai mẫu...):");
                if (rreason === null) return;
                if (!rreason.trim()) { alert("Vui lòng nhập lý do trả hàng."); return; }
                S.api("PATCH", "/orders/" + tid + "/return-request", { reason: rreason.trim() }).then(function () {
                    S.toast("Đã gửi yêu cầu trả hàng tới chi nhánh!", "success");
                    loadOrders();
                }).catch(function (err) { S.toast(err.message, "error"); });
            }
        };
    }
})();

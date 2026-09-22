/**
 * order-history.js
 * Trang lịch sử mua hàng (order-history.html):
 * - Lọc đơn hàng theo tab trạng thái: Mới / Đã xác nhận / Đang giao / Đã giao /
 *   Hủy / Trả hàng - hoàn tiền (gồm cả chờ duyệt trả)
 * - Mỗi đơn có hành động theo trạng thái: hủy, xác nhận đã nhận, yêu cầu trả hàng
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

    var allOrders = [];
    var currentTab = "ALL";

    // Định nghĩa các tab: mỗi tab gồm nhóm trạng thái đi kèm
    var TABS = [
        { key: "ALL", label: "Tất cả", statuses: null },
        { key: "NEW", label: "Đơn hàng mới", statuses: ["NEW"] },
        { key: "CONFIRMED", label: "Đã xác nhận", statuses: ["CONFIRMED", "PICKED_UP"] },
        { key: "SHIPPING", label: "Đang giao", statuses: ["SHIPPING"] },
        { key: "DELIVERED", label: "Đã giao", statuses: ["DELIVERED", "RECEIVED"] },
        { key: "CANCELLED", label: "Hủy", statuses: ["CANCELLED"] },
        { key: "RETURN", label: "Trả hàng - hoàn tiền", statuses: ["RETURN_REQUESTED", "RETURNED"] }
    ];

    var PAY_STATUS_LABELS = {
        UNPAID: "Chưa thanh toán (thu khi giao)",
        PENDING: "Chờ thanh toán",
        PAID: "Đã thanh toán",
        FAILED: "Thanh toán thất bại"
    };

    var PAYMENT_LABELS = { COD: "COD", VNPAY: "VNPay", MOMO: "MoMo" };

    document.addEventListener("DOMContentLoaded", function () {
        if (!getToken()) {
            window.location.href = "/login.html";
            return;
        }
        loadOrders();
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

    function loadOrders() {
        api("GET", "/orders").then(function (res) {
            allOrders = res.data || [];
            renderTabs();
            renderOrders();
        }).catch(function (err) { alert(err.message); });
    }

    function renderTabs() {
        var box = $("orderTabs");
        box.innerHTML = TABS.map(function (tab) {
            var count = tab.statuses === null
                ? allOrders.length
                : allOrders.filter(function (o) {
                    return tab.statuses.indexOf(o.status) !== -1;
                }).length;
            var active = tab.key === currentTab ? " active" : "";
            return '<button type="button" class="order-tab' + active + '" data-tab="' + tab.key + '">' +
                escapeHtml(tab.label) + ' <span class="tab-count">' + count + "</span></button>";
        }).join("");
        box.onclick = function (e) {
            var btn = e.target.closest(".order-tab");
            if (!btn) return;
            currentTab = btn.getAttribute("data-tab");
            renderTabs();
            renderOrders();
        };
    }

    function currentOrders() {
        var tab = TABS.find(function (t) { return t.key === currentTab; });
        if (!tab || tab.statuses === null) return allOrders;
        return allOrders.filter(function (o) { return tab.statuses.indexOf(o.status) !== -1; });
    }

    function renderOrders() {
        var box = $("orderList");
        var orders = currentOrders();
        var summary = $("tabSummary");
        var tab = TABS.find(function (t) { return t.key === currentTab; });
        summary.textContent = orders.length
            ? orders.length + " đơn — tổng " + formatVnd(orders.reduce(function (s, o) {
                return s + Number(o.grandTotal || 0);
            }, 0))
            : "";
        if (!allOrders.length) {
            box.innerHTML = '<p class="text-muted mb-0">Bạn chưa có đơn hàng nào. ' +
                '<a href="/shop.html">Tiếp tục mua sắm</a>.</p>';
            return;
        }
        if (!orders.length) {
            box.innerHTML = '<p class="text-muted mb-0">Không có đơn hàng nào ở trạng thái "' +
                escapeHtml(tab.label) + '".</p>';
            return;
        }
        box.innerHTML = orders.map(function (o) { return renderOrder(o); }).join("");
        box.onclick = onOrderAction;
    }

    function renderOrder(o) {
        var items = (o.items || []).map(function (it) {
            return "<li>" + escapeHtml(it.productName) +
                (it.size ? " (size " + escapeHtml(it.size) + ")" : "") +
                (it.color ? " - màu " + escapeHtml(it.color) : "") +
                " x" + it.quantity + " — " + formatVnd(it.subtotal) + "</li>";
        }).join("");
        var payBadge = '<span class="pay-status-badge pay-' + escapeHtml(o.paymentStatus || "UNPAID") + '">' +
            escapeHtml(PAY_STATUS_LABELS[o.paymentStatus] || o.paymentStatus || "") + "</span>";
        var actions = "";
        if (["NEW", "CONFIRMED", "SHIPPING"].indexOf(o.status) !== -1) {
            actions = actionBtn("cancel", o.id, "Hủy đơn", "btn-link-danger");
        } else if (o.status === "DELIVERED") {
            actions = actionBtn("receive", o.id, "Đã nhận được hàng", "btn-outline-store") + " " +
                actionBtn("cancel", o.id, "Hủy đơn", "btn-link-danger") + " " +
                actionBtn("return", o.id, "Yêu cầu trả hàng", "btn-link-danger");
        } else if (o.status === "RECEIVED") {
            actions = actionBtn("return", o.id, "Yêu cầu trả hàng", "btn-link-danger");
        }
        return '<div class="order-card">' +
            '<div class="order-card-head">' +
            "<div><b>" + escapeHtml(o.orderCode) + "</b> " +
            '<span class="order-status-badge status-' + escapeHtml(o.status) + '">' +
            escapeHtml(o.statusLabel || o.status) + "</span> " + payBadge +
            '<div><small class="text-muted">' + escapeHtml(o.shopName || "") + " — " +
            escapeHtml(o.createdAt || "") + " — Thanh toán: " +
            escapeHtml(PAYMENT_LABELS[o.paymentMethod] || "COD") + "</small></div></div>" +
            '<div class="text-right"><b style="color:#e53637;">' + formatVnd(o.grandTotal) + "</b>" +
            (o.promoName ? '<div><small class="text-success">KM: ' + escapeHtml(o.promoName) + "</small></div>" : "") +
            (o.transporterName ? '<div><small class="text-muted">VC: ' + escapeHtml(o.transporterName) + "</small></div>" : "") +
            "</div></div>" +
            '<ul class="order-items">' + items + "</ul>" +
            (o.receiverName ? '<div class="order-ship"><small><b>Người nhận:</b> ' +
                escapeHtml(o.receiverName) + " — " + escapeHtml(o.receiverPhone) + "<br>" +
                escapeHtml(o.receiverAddress || "") + "</small></div>" : "") +
            (o.note ? '<div class="order-ship"><small><b>Ghi chú:</b> ' + escapeHtml(o.note) + "</small></div>" : "") +
            (o.cancelReason ? '<small class="text-danger">Lý do hủy/trả: ' + escapeHtml(o.cancelReason) + "</small>" : "") +
            (actions ? '<div class="order-actions">' + actions + "</div>" : "") +
            "</div>";
    }

    function actionBtn(action, id, label, cls) {
        return '<button type="button" class="btn btn-sm ' + cls + '" data-' + action + '="' + id + '">' +
            escapeHtml(label) + "</button>";
    }

    function onOrderAction(e) {
        var btn = e.target.closest("button");
        if (!btn) return;
        if (btn.getAttribute("data-cancel") !== null) {
            var id = btn.getAttribute("data-cancel");
            var reason = prompt("Lý do bạn muốn hủy đơn này:");
            if (reason === null) return;
            if (!reason.trim()) { alert("Vui lòng nhập lý do hủy đơn."); return; }
            api("PATCH", "/orders/" + id + "/cancel", { reason: reason.trim() }).then(function () {
                alert("Đã hủy đơn hàng!");
                loadOrders();
            }).catch(function (err) { alert(err.message); });
        } else if (btn.getAttribute("data-receive") !== null) {
            api("PATCH", "/orders/" + btn.getAttribute("data-receive") + "/receive", null).then(function () {
                alert("Cảm ơn bạn đã xác nhận nhận hàng!");
                loadOrders();
            }).catch(function (err) { alert(err.message); });
        } else if (btn.getAttribute("data-return") !== null) {
            var tid = btn.getAttribute("data-return");
            var rreason = prompt("Lý do trả hàng - hoàn tiền (VD: hàng lỗi, sai mẫu...):");
            if (rreason === null) return;
            if (!rreason.trim()) { alert("Vui lòng nhập lý do trả hàng."); return; }
            api("PATCH", "/orders/" + tid + "/return-request", { reason: rreason.trim() }).then(function () {
                alert("Đã gửi yêu cầu trả hàng tới chi nhánh!");
                loadOrders();
            }).catch(function (err) { alert(err.message); });
        }
    }
})();

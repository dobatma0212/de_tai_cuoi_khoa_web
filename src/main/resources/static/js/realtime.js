/**
 * Thông báo & cập nhật đơn hàng theo thời gian thực qua WebSocket (STOMP).
 *  - Khách hàng: nghe /topic/orders/user/{userId}
 *  - Manager / Nhân viên: nghe thêm /topic/orders/shop/{shopId}
 * Mỗi sự kiện: hiện toast và phát CustomEvent "sc:order-event" (detail = OrderEvent)
 * để các trang tự refresh danh sách đơn:
 *     window.addEventListener("sc:order-event", function (e) { reload(); });
 */
(function () {
    "use strict";

    var STOMP_CDN = "https://cdn.jsdelivr.net/npm/@stomp/stompjs@7.0.0/bundles/stomp.umd.min.js";

    function getAuth() {
        try {
            var raw = localStorage.getItem("sc_auth") || localStorage.getItem("user_info");
            var auth = raw ? JSON.parse(raw) : null;
            var token = (auth && auth.token) || localStorage.getItem("jwt_token");
            return token && auth && auth.id ? { token: token, user: auth } : null;
        } catch (e) {
            return null;
        }
    }

    function loadScript(src) {
        return new Promise(function (resolve, reject) {
            var s = document.createElement("script");
            s.src = src;
            s.onload = resolve;
            s.onerror = reject;
            document.head.appendChild(s);
        });
    }

    function toast(msg) {
        var box = document.getElementById("sc-toast-box");
        if (!box) {
            box = document.createElement("div");
            box.id = "sc-toast-box";
            box.style.cssText = "position:fixed;top:80px;right:20px;z-index:99999;display:flex;flex-direction:column;gap:8px;";
            document.body.appendChild(box);
        }
        var el = document.createElement("div");
        el.textContent = msg;
        el.style.cssText = "background:#323232;color:#fff;padding:12px 18px;border-radius:6px;" +
            "box-shadow:0 4px 12px rgba(0,0,0,.25);font-size:14px;max-width:320px;";
        box.appendChild(el);
        setTimeout(function () { el.remove(); }, 5000);
    }

    function authHeaders(token) {
        return { Authorization: "Bearer " + token };
    }

    /** Lấy shopId của manager / nhân viên (null nếu không phải nhân sự chi nhánh) */
    function fetchShopId(auth) {
        var roles = auth.user.roles || [auth.user.role];
        var url = null;
        if (roles.indexOf("ROLE_EMPLOYEE") >= 0) url = "/api/employee/shop";
        else if (roles.indexOf("ROLE_MANAGER") >= 0) url = "/api/manager/shop";
        if (!url) return Promise.resolve(null);
        return fetch(url, { headers: authHeaders(auth.token) })
            .then(function (r) { return r.ok ? r.json() : null; })
            .then(function (j) {
                var d = j && (j.data !== undefined ? j.data : j);
                if (Array.isArray(d)) d = d[0];
                return d && d.id ? d.id : null;
            })
            .catch(function () { return null; });
    }

    function handle(frame) {
        var ev;
        try { ev = JSON.parse(frame.body); } catch (e) { return; }
        toast(ev.message || ("Đơn " + ev.orderCode + " đã cập nhật"));
        window.dispatchEvent(new CustomEvent("sc:order-event", { detail: ev }));
    }

    function start() {
        var auth = getAuth();
        if (!auth || !window.StompJs) return;

        var client = new StompJs.Client({
            brokerURL: (location.protocol === "https:" ? "wss://" : "ws://") + location.host + "/ws",
            reconnectDelay: 5000,
            beforeConnect: function () {
                var a = getAuth();
                if (a) client.connectHeaders = authHeaders(a.token);
            }
        });

        client.onConnect = function () {
            client.subscribe("/topic/orders/user/" + auth.user.id, handle);
            fetchShopId(auth).then(function (shopId) {
                if (shopId && client.connected) {
                    client.subscribe("/topic/orders/shop/" + shopId, handle);
                }
            });
        };
        client.activate();
        window.ScRealtime = client;
    }

    document.addEventListener("DOMContentLoaded", function () {
        if (!getAuth()) return;
        (window.StompJs ? Promise.resolve() : loadScript(STOMP_CDN)).then(start).catch(function () {});
    });
})();

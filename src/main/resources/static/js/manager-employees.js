/**
 * manager-employees.js
 * Sub-tab "Nhân viên" trong tab Quản lý chi nhánh (chỉ MANAGER):
 * - Bổ nhiệm nhân viên (tạo tài khoản mới hoặc thăng USER hiện có)
 * - Sa thải nhân viên (trả về quyền khách hàng, gỡ khỏi chi nhánh)
 * Trách nhiệm nhân sự chi nhánh thuộc về quản lý chi nhánh.
 */
(function () {
    "use strict";

    var STAFF_API = "/api/manager/shop/employees";

    function $(id) { return document.getElementById(id); }

    function getSession() {
        return window.ScAuth ? window.ScAuth.getSession() : null;
    }

    function hasRole(session, keyword) {
        if (!session) return false;
        var user = session.user && typeof session.user === "object" ? session.user : session;
        var r = String(user.role || session.role || user.roleName || session.roleName || "").toUpperCase();
        if (r.indexOf(keyword) !== -1) return true;
        var roles = user.roles || session.roles;
        if (Array.isArray(roles)) {
            return roles.some(function (role) {
                return String(role || "").toUpperCase().indexOf(keyword) !== -1;
            });
        }
        return false;
    }

    function staffApi(method, path, body) {
        var s = getSession();
        var token = s && s.token ? s.token : localStorage.getItem("jwt_token");
        var headers = { "Content-Type": "application/json" };
        if (token) headers["Authorization"] = "Bearer " + token;
        return fetch(STAFF_API + path, {
            method: method,
            headers: headers,
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

    function showAlert(message, type) {
        var el = $("staffAlert");
        el.className = "alert auth-alert auth-alert--" + (type || "error");
        el.textContent = message;
        el.style.display = "block";
    }

    function escapeHtml(str) {
        return String(str == null ? "" : str)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    function loadStaff() {
        staffApi("GET", "").then(function (res) {
            renderStaff(res.data || []);
        }).catch(function (err) {
            showAlert(err.message, "error");
        });
    }

    function renderStaff(staff) {
        $("staffListTitle").textContent = "Danh sách nhân viên (" + staff.length + ")";
        var wrap = $("staffList");
        if (!staff.length) {
            wrap.innerHTML = '<p class="text-muted">Chi nhánh chưa có nhân viên nào. Hãy bổ nhiệm ở form phía trên.</p>';
            return;
        }
        var html = "";
        staff.forEach(function (u) {
            html += '<div class="address-card" style="margin-bottom:12px;">'
                + '<div class="d-flex align-items-start">'
                + '<div class="flex-grow-1">'
                + '<div class="font-weight-bold">' + escapeHtml(u.fullName) + ' <span class="badge badge-dark">@' + escapeHtml(u.username) + "</span></div>"
                + '<div class="text-muted" style="font-size:13px;">' + escapeHtml(u.email)
                + (u.phone ? " — " + escapeHtml(u.phone) : "") + "</div>"
                + '<div class="text-muted" style="font-size:12px;">Trạng thái: ' + escapeHtml(u.status || "active") + "</div>"
                + "</div>"
                + '<div class="text-nowrap" style="margin-left:10px;">'
                + '<button type="button" class="btn btn-sm btn-outline-danger" data-fire="' + u.id + '">Sa thải</button>'
                + "</div></div></div>";
        });
        wrap.innerHTML = html;
        wrap.querySelectorAll("button[data-fire]").forEach(function (b) {
            b.addEventListener("click", function () {
                fireStaff(Number(b.getAttribute("data-fire")), b);
            });
        });
    }

    function fireStaff(id, btn) {
        if (!confirm("Sa thải nhân viên này? Tài khoản sẽ trở lại là khách hàng thông thường (không bị xóa).")) return;
        btn.disabled = true;
        staffApi("DELETE", "/" + id).then(function (res) {
            showAlert(res.message, "success");
            loadStaff();
        }).catch(function (err) {
            btn.disabled = false;
            showAlert(err.message, "error");
        });
    }

    function init() {
        var session = getSession();
        if (!hasRole(session, "MANAGER")) return;

        var subBtn = document.querySelector('.shop-subbtn[data-sub="shopSubStaff"]');
        if (!subBtn) return;
        subBtn.addEventListener("click", loadStaff);

        $("hireForm").addEventListener("submit", function (e) {
            e.preventDefault();
            var userId = $("hireUserId").value.trim();
            var body = {
                username: $("hireUsername").value.trim(),
                password: $("hirePassword").value,
                email: $("hireEmail").value.trim(),
                fullName: $("hireFullName").value.trim(),
                phone: $("hirePhone").value.trim()
            };
            if (userId) body.userId = Number(userId);
            // Bổ nhiệm người dùng hiện có thì không cần username/password
            if (!userId && (!body.username || !body.password || !body.email || !body.fullName)) {
                showAlert("Vui lòng nhập đủ thông tin bắt buộc (*) hoặc điền ID người dùng hiện có.", "error");
                return;
            }
            staffApi("POST", "", body).then(function (res) {
                showAlert(res.message + " (" + res.data.fullName + " — @" + res.data.username + ")", "success");
                $("hireForm").reset();
                loadStaff();
            }).catch(function (err) {
                showAlert(err.message, "error");
            });
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();

/**
 * auth.js
 * Xử lý chung cho các trang: login.html, register.html, forgot-password.html
 * và cập nhật thanh header (Sign in / Xin chào ...) trên MỌI trang của site.
 */
(function () {
    "use strict";

    var API_BASE = "/api/auth";
    var STORAGE_KEY = "sc_auth"; // { token, type, id, username, email, fullName, role, roles }

    // ============ Helper gọi API ============
    function apiPost(path, body) {
        return fetch(API_BASE + path, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body || {})
        }).then(function (res) {
            return res.json().then(function (data) {
                if (!res.ok || data.success === false) {
                    var err = new Error(data.message || "Đã có lỗi xảy ra, vui lòng thử lại.");
                    err.payload = data;
                    throw err;
                }
                return data;
            });
        });
    }

    // ============ Quản lý phiên đăng nhập (localStorage) ============
    function saveSession(jwtResponse) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(jwtResponse));
        if (jwtResponse && jwtResponse.token) {
            localStorage.setItem("jwt_token", jwtResponse.token);
            localStorage.setItem("user_info", JSON.stringify(jwtResponse));
        }
    }

    function getSession() {
        var raw = localStorage.getItem(STORAGE_KEY);
        if (!raw) {
            var alt = localStorage.getItem("user_info");
            if (alt) {
                try {
                    return JSON.parse(alt);
                } catch (e) {
                    return null;
                }
            }
            return null;
        }
        try {
            return JSON.parse(raw);
        } catch (e) {
            return null;
        }
    }

    function clearSession() {
        localStorage.removeItem(STORAGE_KEY);
        localStorage.removeItem("jwt_token");
        localStorage.removeItem("user_info");
        sessionStorage.removeItem("jwt_token");
        sessionStorage.removeItem("user_info");
    }

    function isLoggedIn() {
        var s = getSession();
        return !!(s && (s.token || localStorage.getItem("jwt_token")));
    }

    function isAdmin(session) {
        if (!session) session = getSession();
        if (!session) return false;
        var user = session.user && typeof session.user === "object" ? session.user : session;
        var r = (user.role || session.role || "").toUpperCase();
        if (r.includes("ADMIN")) return true;
        var roles = user.roles || session.roles;
        if (Array.isArray(roles)) {
            return roles.some(function (role) {
                return (role || "").toUpperCase().includes("ADMIN");
            });
        }
        return false;
    }

    function isAdminOrManager(session) {
        if (!session) session = getSession();
        if (!session) return false;

        // Hỗ trợ cả dạng phẳng và dạng { token, user: {...} }
        var user = session.user && typeof session.user === "object" ? session.user : session;
        var r = (user.role || session.role || "").toUpperCase();
        if (r.includes("ADMIN") || r.includes("MANAGER")) return true;

        var roles = user.roles || session.roles;
        if (Array.isArray(roles)) {
            return roles.some(function (role) {
                var ro = (role || "").toUpperCase();
                return ro.includes("ADMIN") || ro.includes("MANAGER");
            });
        }
        return false;
    }

    function updateAdminNavVisibility() {
        var show = isLoggedIn() && isAdmin();
        var items = document.querySelectorAll(".admin-nav-only");
        for (var i = 0; i < items.length; i++) {
            items[i].style.display = show ? "" : "none";
        }
        if (show) {
            document.body.classList.add("is-admin-manager");
        } else {
            document.body.classList.remove("is-admin-manager");
        }
    }

    function logout() {
        clearSession();
        window.location.href = "/index.html";
    }

    // ============ Cập nhật giao diện Header (mọi trang) ============
    function updateHeaderAuthUI() {
        var session = getSession();
        var desktopLink = document.getElementById("navAuthDesktop");
        var mobileLink = document.getElementById("navAuthMobile");

        updateAdminNavVisibility();

        if (!desktopLink && !mobileLink) return;

        if (session && (session.token || localStorage.getItem("jwt_token"))) {
            var user = session.user && typeof session.user === "object" ? session.user : session;
            var displayName = user.fullName || user.username || session.fullName || session.username || "Tài khoản";
            var adminBtnHtml = "";
            if (isAdmin(session)) {
                adminBtnHtml = '<a href="/admin-users.html" class="auth-admin-btn" style="color: #ffffff; background: #e53637; padding: 3px 10px; border-radius: 4px; font-weight: 700; margin-right: 12px; font-size: 13px; text-decoration: none; display: inline-block;"><i class="fa fa-cogs" aria-hidden="true"></i> Quản trị Hệ thống</a>';
            }

            if (desktopLink) {
                desktopLink.innerHTML =
                    adminBtnHtml +
                    '<a href="/profile.html" style="color: inherit; text-decoration: none;"><i class="fa fa-user-circle-o" aria-hidden="true"></i> Xin chào, ' + escapeHtml(displayName) + "</a>" +
                    ' <span class="auth-logout-link" id="btnLogoutDesktop" style="margin-left: 6px; cursor: pointer; color: #888;">(Đăng xuất)</span>';
                desktopLink.removeAttribute("href");
            }
            if (mobileLink) {
                mobileLink.innerHTML =
                    adminBtnHtml +
                    '<a href="/profile.html" style="color: inherit; text-decoration: none;">Xin chào, ' + escapeHtml(displayName) + "</a>" +
                    ' <span class="auth-logout-link" id="btnLogoutMobile" style="margin-left: 6px; cursor: pointer; color: #888;">(Đăng xuất)</span>';
                mobileLink.removeAttribute("href");
            }

            var btnD = document.getElementById("btnLogoutDesktop");
            var btnM = document.getElementById("btnLogoutMobile");
            if (btnD) btnD.addEventListener("click", function (e) { e.preventDefault(); logout(); });
            if (btnM) btnM.addEventListener("click", function (e) { e.preventDefault(); logout(); });
        } else {
            if (desktopLink) {
                desktopLink.textContent = "Đăng nhập";
                desktopLink.setAttribute("href", "/login.html");
            }
            if (mobileLink) {
                mobileLink.textContent = "Đăng nhập";
                mobileLink.setAttribute("href", "/login.html");
            }
        }
    }

    function escapeHtml(str) {
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");
    }

    // ============ Hiển thị thông báo (alert box) ============
    function showAlert(containerEl, message, type) {
        if (!containerEl) return;
        containerEl.className = "auth-alert auth-alert--" + (type || "error");
        containerEl.textContent = message;
        containerEl.style.display = "block";
    }

    function hideAlert(containerEl) {
        if (!containerEl) return;
        containerEl.style.display = "none";
        containerEl.textContent = "";
    }

    // ============ Đếm ngược cho nút "Gửi lại mã OTP" ============
    function startResendCountdown(buttonEl, seconds) {
        var remaining = seconds;
        var originalText = "Gửi lại mã OTP";
        buttonEl.disabled = true;
        buttonEl.textContent = "Gửi lại mã (" + remaining + "s)";

        var timer = setInterval(function () {
            remaining--;
            if (remaining <= 0) {
                clearInterval(timer);
                buttonEl.disabled = false;
                buttonEl.textContent = originalText;
            } else {
                buttonEl.textContent = "Gửi lại mã (" + remaining + "s)";
            }
        }, 1000);
    }

    // Expose ra window để các trang login.html / register.html / forgot-password.html dùng
    window.ScAuth = {
        apiPost: apiPost,
        saveSession: saveSession,
        getSession: getSession,
        clearSession: clearSession,
        isLoggedIn: isLoggedIn,
        isAdmin: isAdmin,
        isAdminOrManager: isAdminOrManager,
        logout: logout,
        showAlert: showAlert,
        hideAlert: hideAlert,
        startResendCountdown: startResendCountdown,
        updateHeaderAuthUI: updateHeaderAuthUI
    };

    document.addEventListener("DOMContentLoaded", updateHeaderAuthUI);
})();

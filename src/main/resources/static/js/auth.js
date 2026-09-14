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
    }

    function getSession() {
        var raw = localStorage.getItem(STORAGE_KEY);
        if (!raw) return null;
        try {
            return JSON.parse(raw);
        } catch (e) {
            return null;
        }
    }

    function clearSession() {
        localStorage.removeItem(STORAGE_KEY);
    }

    function isLoggedIn() {
        var s = getSession();
        return !!(s && s.token);
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

        if (!desktopLink && !mobileLink) return;

        if (session && session.token) {
            var displayName = session.fullName || session.username || "Tài khoản";

            if (desktopLink) {
                desktopLink.innerHTML =
                    '<i class="fa fa-user-circle-o" aria-hidden="true"></i> Xin chào, ' + escapeHtml(displayName) +
                    ' <span class="auth-logout-link" id="btnLogoutDesktop">(Đăng xuất)</span>';
                desktopLink.removeAttribute("href");
            }
            if (mobileLink) {
                mobileLink.innerHTML = 'Xin chào, ' + escapeHtml(displayName) +
                    ' <span class="auth-logout-link" id="btnLogoutMobile">(Đăng xuất)</span>';
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
        logout: logout,
        showAlert: showAlert,
        hideAlert: hideAlert,
        startResendCountdown: startResendCountdown
    };

    document.addEventListener("DOMContentLoaded", updateHeaderAuthUI);
})();

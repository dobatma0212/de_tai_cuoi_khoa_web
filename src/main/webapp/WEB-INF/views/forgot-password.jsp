<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>

<!DOCTYPE html>
<html lang="zxx">
<head>
<meta charset="UTF-8">
    <meta name="description" content="Male_Fashion Template">
    <meta name="keywords" content="Male_Fashion, unica, creative, html">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Quên mật khẩu | Male-Fashion</title>

    <!-- Google Font -->

    <!-- Css Styles -->
</head>
<body>
<!-- Breadcrumb Section Begin -->
    <section class="breadcrumb-option">
        <div class="container">
            <div class="row">
                <div class="col-lg-12">
                    <div class="breadcrumb__text">
                        <h4>Quên mật khẩu</h4>
                        <div class="breadcrumb__links">
                            <a href="./index.html">Home</a>
                            <span>Quên mật khẩu</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
    <!-- Breadcrumb Section End -->

    <!-- Forgot Password Section Begin -->
    <section class="auth-section">
        <div class="container">
            <div class="auth-card">

                <!-- BƯỚC 1: NHẬP EMAIL -->
                <div class="auth-step active" id="stepEmail">
                    <h3 class="auth-card__title">Quên mật khẩu</h3>
                    <p class="auth-card__subtitle">Nhập email đã đăng ký để nhận mã OTP đặt lại mật khẩu</p>

                    <div id="emailAlert" class="auth-alert"></div>

                    <form id="emailForm" novalidate>
                        <div class="auth-field">
                            <label for="fpEmail">Email</label>
                            <input type="email" id="fpEmail" name="fpEmail" placeholder="Nhập email tài khoản" required>
                        </div>
                        <button type="submit" class="auth-btn" id="emailSubmitBtn">Gửi mã OTP</button>
                    </form>

                    <div class="auth-links">
                        <p>Đã nhớ mật khẩu? <a href="/login.html">Đăng nhập</a></p>
                    </div>
                </div>

                <!-- BƯỚC 2: NHẬP OTP + MẬT KHẨU MỚI -->
                <div class="auth-step" id="stepReset">
                    <h3 class="auth-card__title">Đặt lại mật khẩu</h3>
                    <p class="auth-otp-hint">
                        Mã OTP đã được gửi tới email <b id="resetEmailLabel"></b>.<br>
                        Nhập mã cùng mật khẩu mới của bạn bên dưới.
                    </p>

                    <div id="resetAlert" class="auth-alert"></div>

                    <form id="resetForm" novalidate>
                        <div class="auth-field auth-field--otp">
                            <label for="resetOtpCode">Mã OTP (6 số)</label>
                            <input type="text" id="resetOtpCode" name="resetOtpCode" placeholder="------" maxlength="6" inputmode="numeric" pattern="\d{6}" required>
                        </div>
                        <div class="auth-field">
                            <label for="newPassword">Mật khẩu mới</label>
                            <input type="password" id="newPassword" name="newPassword" placeholder="Từ 6-40 ký tự" required>
                        </div>
                        <div class="auth-field">
                            <label for="confirmNewPassword">Xác nhận mật khẩu mới</label>
                            <input type="password" id="confirmNewPassword" name="confirmNewPassword" placeholder="Nhập lại mật khẩu mới" required>
                        </div>
                        <button type="submit" class="auth-btn" id="resetSubmitBtn">Đặt lại mật khẩu</button>
                    </form>

                    <div class="auth-resend-row">
                        Không nhận được mã?
                        <button type="button" class="auth-btn auth-btn--link" id="resendResetOtpBtn">Gửi lại mã OTP</button>
                    </div>
                </div>

            </div>
        </div>
    </section>
    <!-- Forgot Password Section End -->

    <!-- Search Begin -->
    <div class="search-model">
        <div class="h-100 d-flex align-items-center justify-content-center">
            <div class="search-close-switch">+</div>
            <form class="search-model-form">
                <input type="text" id="search-input" placeholder="Search here.....">
            </form>
        </div>
    </div>
    <!-- Search End -->

    <!-- Js Plugins -->
    <script src="/js/jquery-3.3.1.min.js"></script>
    <script src="/js/bootstrap.min.js"></script>
    <script src="/js/jquery.nice-select.min.js"></script>
    <script src="/js/jquery.nicescroll.min.js"></script>
    <script src="/js/jquery.magnific-popup.min.js"></script>
    <script src="/js/jquery.countdown.min.js"></script>
    <script src="/js/jquery.slicknav.js"></script>
    <script src="/js/mixitup.min.js"></script>
    <script src="/js/owl.carousel.min.js"></script>
    <script src="/js/main.js"></script>
    <script src="/js/auth.js"></script>
    <script>
        (function () {
            if (window.ScAuth.isLoggedIn()) {
                window.location.href = "/index.html";
                return;
            }

            var stepEmail = document.getElementById("stepEmail");
            var stepReset = document.getElementById("stepReset");
            var targetEmail = "";

            function goToResetStep(email) {
                targetEmail = email;
                document.getElementById("resetEmailLabel").textContent = email;
                stepEmail.classList.remove("active");
                stepReset.classList.add("active");
                window.ScAuth.startResendCountdown(document.getElementById("resendResetOtpBtn"), 60);
            }

            // ---- BƯỚC 1: GỬI OTP ----
            var emailForm = document.getElementById("emailForm");
            var emailAlert = document.getElementById("emailAlert");
            var emailSubmitBtn = document.getElementById("emailSubmitBtn");

            emailForm.addEventListener("submit", function (e) {
                e.preventDefault();
                window.ScAuth.hideAlert(emailAlert);

                var email = document.getElementById("fpEmail").value.trim();
                if (!email) {
                    window.ScAuth.showAlert(emailAlert, "Vui lòng nhập email.", "error");
                    return;
                }

                emailSubmitBtn.disabled = true;
                emailSubmitBtn.textContent = "Đang gửi...";

                window.ScAuth.apiPost("/forgot-password", { email: email })
                    .then(function (res) {
                        window.ScAuth.showAlert(emailAlert, res.message, "success");
                        setTimeout(function () {
                            goToResetStep(email);
                        }, 600);
                    }).catch(function (err) {
                        emailSubmitBtn.disabled = false;
                        emailSubmitBtn.textContent = "Gửi mã OTP";
                        window.ScAuth.showAlert(emailAlert, err.message, "error");
                    });
            });

            // ---- BƯỚC 2: ĐẶT LẠI MẬT KHẨU ----
            var resetForm = document.getElementById("resetForm");
            var resetAlert = document.getElementById("resetAlert");
            var resetSubmitBtn = document.getElementById("resetSubmitBtn");

            resetForm.addEventListener("submit", function (e) {
                e.preventDefault();
                window.ScAuth.hideAlert(resetAlert);

                var otp = document.getElementById("resetOtpCode").value.trim();
                var newPassword = document.getElementById("newPassword").value;
                var confirmNewPassword = document.getElementById("confirmNewPassword").value;

                if (!/^\d{6}$/.test(otp)) {
                    window.ScAuth.showAlert(resetAlert, "Mã OTP gồm đúng 6 chữ số.", "error");
                    return;
                }
                if (newPassword.length < 6) {
                    window.ScAuth.showAlert(resetAlert, "Mật khẩu mới phải có ít nhất 6 ký tự.", "error");
                    return;
                }
                if (newPassword !== confirmNewPassword) {
                    window.ScAuth.showAlert(resetAlert, "Mật khẩu xác nhận không khớp.", "error");
                    return;
                }

                resetSubmitBtn.disabled = true;
                resetSubmitBtn.textContent = "Đang xử lý...";

                window.ScAuth.apiPost("/reset-password", {
                    email: targetEmail,
                    otp: otp,
                    newPassword: newPassword
                }).then(function (res) {
                    window.ScAuth.showAlert(resetAlert, res.message + " Đang chuyển đến trang đăng nhập...", "success");
                    setTimeout(function () {
                        window.location.href = "/login.html";
                    }, 1200);
                }).catch(function (err) {
                    resetSubmitBtn.disabled = false;
                    resetSubmitBtn.textContent = "Đặt lại mật khẩu";
                    window.ScAuth.showAlert(resetAlert, err.message, "error");
                });
            });

            // ---- GỬI LẠI OTP ----
            var resendBtn = document.getElementById("resendResetOtpBtn");
            resendBtn.addEventListener("click", function () {
                window.ScAuth.hideAlert(resetAlert);
                window.ScAuth.apiPost("/resend-otp", {
                    email: targetEmail,
                    purpose: "RESET_PASSWORD"
                }).then(function (res) {
                    window.ScAuth.showAlert(resetAlert, res.message, "success");
                    window.ScAuth.startResendCountdown(resendBtn, 60);
                }).catch(function (err) {
                    window.ScAuth.showAlert(resetAlert, err.message, "error");
                });
            });
        })();
    </script>
</body>
</html>

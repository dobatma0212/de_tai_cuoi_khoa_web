<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>

<!DOCTYPE html>
<html lang="zxx">
<head>
<meta charset="UTF-8">
    <meta name="description" content="Male_Fashion Template">
    <meta name="keywords" content="Male_Fashion, unica, creative, html">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Đăng ký | Male-Fashion</title>

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
                        <h4>Đăng ký tài khoản</h4>
                        <div class="breadcrumb__links">
                            <a href="./index.html">Home</a>
                            <span>Đăng ký</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
    <!-- Breadcrumb Section End -->

    <!-- Register Section Begin -->
    <section class="auth-section">
        <div class="container">
            <div class="auth-card">

                <!-- BƯỚC 1: FORM ĐĂNG KÝ -->
                <div class="auth-step active" id="stepRegister">
                    <h3 class="auth-card__title">Đăng ký</h3>
                    <p class="auth-card__subtitle">Tạo tài khoản mới để bắt đầu mua sắm</p>

                    <div id="registerAlert" class="auth-alert"></div>

                    <form id="registerForm" novalidate>
                        <div class="auth-field">
                            <label for="fullName">Họ và tên</label>
                            <input type="text" id="fullName" name="fullName" placeholder="Nhập họ và tên" required>
                        </div>
                        <div class="auth-field">
                            <label for="username">Tên đăng nhập</label>
                            <input type="text" id="username" name="username" placeholder="Từ 3-50 ký tự" required>
                        </div>
                        <div class="auth-field">
                            <label for="email">Email</label>
                            <input type="email" id="email" name="email" placeholder="Nhập email để nhận mã OTP kích hoạt" required>
                        </div>
                        <div class="auth-field">
                            <label for="phone">Số điện thoại</label>
                            <input type="text" id="phone" name="phone" placeholder="Không bắt buộc">
                        </div>
                        <div class="auth-field">
                            <label for="regPassword">Mật khẩu</label>
                            <input type="password" id="regPassword" name="password" placeholder="Từ 6-40 ký tự" required>
                        </div>
                        <div class="auth-field">
                            <label for="confirmPassword">Xác nhận mật khẩu</label>
                            <input type="password" id="confirmPassword" name="confirmPassword" placeholder="Nhập lại mật khẩu" required>
                        </div>
                        <button type="submit" class="auth-btn" id="registerSubmitBtn">Đăng ký</button>
                    </form>

                    <div class="auth-links">
                        <p>Đã có tài khoản? <a href="/login.html">Đăng nhập</a></p>
                    </div>
                </div>

                <!-- BƯỚC 2: XÁC THỰC OTP -->
                <div class="auth-step" id="stepVerify">
                    <h3 class="auth-card__title">Xác thực OTP</h3>
                    <p class="auth-otp-hint">
                        Mã OTP kích hoạt đã được gửi tới email <b id="verifyEmailLabel"></b>.<br>
                        Vui lòng kiểm tra hộp thư (kể cả mục Spam) và nhập mã bên dưới.
                    </p>

                    <div id="verifyAlert" class="auth-alert"></div>

                    <form id="verifyForm" novalidate>
                        <div class="auth-field auth-field--otp">
                            <label for="otpCode">Mã OTP (6 số)</label>
                            <input type="text" id="otpCode" name="otpCode" placeholder="------" maxlength="6" inputmode="numeric" pattern="\d{6}" required>
                        </div>
                        <button type="submit" class="auth-btn" id="verifySubmitBtn">Kích hoạt tài khoản</button>
                    </form>

                    <div class="auth-resend-row">
                        Không nhận được mã?
                        <button type="button" class="auth-btn auth-btn--link" id="resendOtpBtn">Gửi lại mã OTP</button>
                    </div>
                </div>

            </div>
        </div>
    </section>
    <!-- Register Section End -->

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

            var stepRegister = document.getElementById("stepRegister");
            var stepVerify = document.getElementById("stepVerify");
            var registeredEmail = "";

            function goToVerifyStep(email) {
                registeredEmail = email;
                document.getElementById("verifyEmailLabel").textContent = email;
                stepRegister.classList.remove("active");
                stepVerify.classList.add("active");
                window.ScAuth.startResendCountdown(document.getElementById("resendOtpBtn"), 60);
            }

            // ---- FORM ĐĂNG KÝ ----
            var registerForm = document.getElementById("registerForm");
            var registerAlert = document.getElementById("registerAlert");
            var registerSubmitBtn = document.getElementById("registerSubmitBtn");

            registerForm.addEventListener("submit", function (e) {
                e.preventDefault();
                window.ScAuth.hideAlert(registerAlert);

                var fullName = document.getElementById("fullName").value.trim();
                var username = document.getElementById("username").value.trim();
                var email = document.getElementById("email").value.trim();
                var phone = document.getElementById("phone").value.trim();
                var password = document.getElementById("regPassword").value;
                var confirmPassword = document.getElementById("confirmPassword").value;

                if (!fullName || !username || !email || !password || !confirmPassword) {
                    window.ScAuth.showAlert(registerAlert, "Vui lòng điền đầy đủ các trường bắt buộc.", "error");
                    return;
                }
                if (password.length < 6) {
                    window.ScAuth.showAlert(registerAlert, "Mật khẩu phải có ít nhất 6 ký tự.", "error");
                    return;
                }
                if (password !== confirmPassword) {
                    window.ScAuth.showAlert(registerAlert, "Mật khẩu xác nhận không khớp.", "error");
                    return;
                }

                registerSubmitBtn.disabled = true;
                registerSubmitBtn.textContent = "Đang xử lý...";

                window.ScAuth.apiPost("/register", {
                    fullName: fullName,
                    username: username,
                    email: email,
                    phone: phone,
                    password: password
                }).then(function (res) {
                    window.ScAuth.showAlert(registerAlert, res.message, "success");
                    setTimeout(function () {
                        goToVerifyStep(email);
                    }, 600);
                }).catch(function (err) {
                    registerSubmitBtn.disabled = false;
                    registerSubmitBtn.textContent = "Đăng ký";
                    window.ScAuth.showAlert(registerAlert, err.message, "error");
                });
            });

            // ---- FORM XÁC THỰC OTP ----
            var verifyForm = document.getElementById("verifyForm");
            var verifyAlert = document.getElementById("verifyAlert");
            var verifySubmitBtn = document.getElementById("verifySubmitBtn");

            verifyForm.addEventListener("submit", function (e) {
                e.preventDefault();
                window.ScAuth.hideAlert(verifyAlert);

                var otp = document.getElementById("otpCode").value.trim();
                if (!/^\d{6}$/.test(otp)) {
                    window.ScAuth.showAlert(verifyAlert, "Mã OTP gồm đúng 6 chữ số.", "error");
                    return;
                }

                verifySubmitBtn.disabled = true;
                verifySubmitBtn.textContent = "Đang xác thực...";

                window.ScAuth.apiPost("/verify-otp", {
                    email: registeredEmail,
                    otp: otp
                }).then(function (res) {
                    window.ScAuth.showAlert(verifyAlert, res.message + " Đang chuyển đến trang đăng nhập...", "success");
                    setTimeout(function () {
                        window.location.href = "/login.html";
                    }, 1200);
                }).catch(function (err) {
                    verifySubmitBtn.disabled = false;
                    verifySubmitBtn.textContent = "Kích hoạt tài khoản";
                    window.ScAuth.showAlert(verifyAlert, err.message, "error");
                });
            });

            // ---- GỬI LẠI OTP ----
            var resendBtn = document.getElementById("resendOtpBtn");
            resendBtn.addEventListener("click", function () {
                window.ScAuth.hideAlert(verifyAlert);
                window.ScAuth.apiPost("/resend-otp", {
                    email: registeredEmail,
                    purpose: "REGISTER"
                }).then(function (res) {
                    window.ScAuth.showAlert(verifyAlert, res.message, "success");
                    window.ScAuth.startResendCountdown(resendBtn, 60);
                }).catch(function (err) {
                    window.ScAuth.showAlert(verifyAlert, err.message, "error");
                });
            });
        })();
    </script>
</body>
</html>

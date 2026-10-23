<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>

<!DOCTYPE html>
<html lang="zxx">
<head>
<meta charset="UTF-8">
    <meta name="description" content="Male_Fashion Template">
    <meta name="keywords" content="Male_Fashion, unica, creative, html">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Đăng nhập | Male-Fashion</title>

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
                        <h4>Đăng nhập</h4>
                        <div class="breadcrumb__links">
                            <a href="./index.html">Home</a>
                            <span>Đăng nhập</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
    <!-- Breadcrumb Section End -->

    <!-- Login Section Begin -->
    <section class="auth-section">
        <div class="container">
            <div class="auth-card">
                <h3 class="auth-card__title">Đăng nhập</h3>
                <p class="auth-card__subtitle">Nhập thông tin tài khoản để tiếp tục mua sắm</p>

                <div id="loginAlert" class="auth-alert"></div>

                <form id="loginForm" novalidate>
                    <div class="auth-field">
                        <label for="usernameOrEmail">Tên đăng nhập hoặc Email</label>
                        <input type="text" id="usernameOrEmail" name="usernameOrEmail" placeholder="Nhập tên đăng nhập hoặc email" required>
                    </div>
                    <div class="auth-field">
                        <label for="password">Mật khẩu</label>
                        <input type="password" id="password" name="password" placeholder="Nhập mật khẩu" required>
                    </div>
                    <button type="submit" class="auth-btn" id="loginSubmitBtn">Đăng nhập</button>
                </form>

                <div class="auth-links">
                    <p><a href="/forgot-password.html">Quên mật khẩu?</a></p>
                    <p>Chưa có tài khoản? <a href="/register.html">Đăng ký ngay</a></p>
                </div>
            </div>
        </div>
    </section>
    <!-- Login Section End -->

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
            var form = document.getElementById("loginForm");
            var alertBox = document.getElementById("loginAlert");
            var submitBtn = document.getElementById("loginSubmitBtn");

            // Nếu đã đăng nhập rồi: Chỉ Admin vào trang quản trị users, còn lại (Manager, User) vào trang chủ
            if (window.ScAuth.isLoggedIn()) {
                var currentSession = window.ScAuth.getSession();
                if (window.ScAuth.isAdmin(currentSession)) {
                    window.location.href = "/admin-users.html";
                } else {
                    window.location.href = "/index.html";
                }
                return;
            }

            form.addEventListener("submit", function (e) {
                e.preventDefault();
                window.ScAuth.hideAlert(alertBox);

                var usernameOrEmail = document.getElementById("usernameOrEmail").value.trim();
                var password = document.getElementById("password").value;

                if (!usernameOrEmail || !password) {
                    window.ScAuth.showAlert(alertBox, "Vui lòng nhập đầy đủ thông tin.", "error");
                    return;
                }

                submitBtn.disabled = true;
                submitBtn.textContent = "Đang đăng nhập...";

                window.ScAuth.apiPost("/login", {
                    usernameOrEmail: usernameOrEmail,
                    password: password
                }).then(function (res) {
                    window.ScAuth.saveSession(res.data);
                    var isAdmin = window.ScAuth.isAdmin(res.data);
                    var redirectUrl = isAdmin ? "/admin-users.html" : "/index.html";
                    var msg = isAdmin 
                        ? "Đăng nhập thành công! Đang chuyển hướng đến Trang Quản Trị..." 
                        : "Đăng nhập thành công! Đang chuyển hướng...";
                    window.ScAuth.showAlert(alertBox, msg, "success");
                    setTimeout(function () {
                        window.location.href = redirectUrl;
                    }, 600);
                }).catch(function (err) {
                    submitBtn.disabled = false;
                    submitBtn.textContent = "Đăng nhập";
                    window.ScAuth.showAlert(alertBox, err.message, "error");
                });
            });
        })();
    </script>
</body>
</html>

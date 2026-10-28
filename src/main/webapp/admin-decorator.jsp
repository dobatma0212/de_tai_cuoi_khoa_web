<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<%
    String uri = request.getRequestURI();
    String activeUsers = uri.equals("/admin-users.html") ? "active" : "";
    String activeProducts = uri.equals("/admin-products.html") ? "active" : "";
    String activeCategories = uri.equals("/admin-categories.html") ? "active" : "";
    String activeShops = uri.equals("/admin-shops.html") ? "active" : "";
    String activeTransporters = uri.equals("/admin-transporters.html") ? "active" : "";
    String activePromotions = uri.equals("/admin-promotions.html") ? "active" : "";
%>

<!DOCTYPE html>
<html lang="zxx">
<head>
    <!-- Font tu host: dam bao hien thi dung tieng Viet -->
    <link rel="stylesheet" href="/css/be-vietnam-pro.css?v=20261028" type="text/css">
    <link rel="stylesheet" href="/css/bootstrap.min.css" type="text/css">
    <link rel="stylesheet" href="/css/font-awesome.min.css" type="text/css">
    <link rel="stylesheet" href="/css/elegant-icons.css" type="text/css">
    <link rel="stylesheet" href="/css/magnific-popup.css" type="text/css">
    <link rel="stylesheet" href="/css/nice-select.css" type="text/css">
    <link rel="stylesheet" href="/css/owl.carousel.min.css" type="text/css">
    <link rel="stylesheet" href="/css/slicknav.min.css" type="text/css">
    <link rel="stylesheet" href="/css/style.css" type="text/css">
    <link rel="stylesheet" href="/css/admin.css" type="text/css">
    <link rel="stylesheet" href="/css/vietnamese-ui-fix.css?v=20261028" type="text/css">
    <sitemesh:write property="head"/>
</head>
<body>
<!-- Page Preloder -->
    <div id="preloder">
        <div class="loader"></div>
    </div>

<!-- Offcanvas Menu Begin -->
    <div class="offcanvas-menu-overlay"></div>
    <div class="offcanvas-menu-wrapper">
        <div class="offcanvas__option">
            <div class="offcanvas__links" id="mobile-auth-section">
                <!-- Injected via JS -->
            </div>
        </div>
        <div class="offcanvas__nav__option">
            <a href="#" class="search-switch"><img src="/img/icon/search.png" alt=""></a>
            <a href="#"><img src="/img/icon/heart.png" alt=""></a>
            <a href="/shopping-cart.html"><img src="/img/icon/cart.png" alt=""> <span>0</span></a>
            <div class="price">$0.00</div>
        </div>
        <div id="mobile-menu-wrap"></div>
        <div class="offcanvas__text">
            <p>Hệ thống Quản lý Chuỗi Cửa hàng - Male Fashion</p>
        </div>
    </div>
    <!-- Offcanvas Menu End -->

<!-- Header Section Begin -->
    <header class="header admin-header">
        <div class="header__top">
            <div class="container">
                <div class="row align-items-center">
                    <div class="col-lg-5 col-md-5 col-5">
                        <div class="header__top__left">
                            <p><i class="fa fa-sliders"></i> Bảng điều khiển Quản trị</p>
                        </div>
                    </div>
                    <div class="col-lg-7 col-md-7 col-7">
                        <div class="header__top__right d-flex justify-content-end align-items-center">
                            <a href="/index.html" class="topbar-shop-link" title="Xem trang bán hàng">
                                <i class="fa fa-external-link"></i> Xem Cửa hàng
                            </a>
                            <div class="header__top__links" id="auth-user-info">
                                <span class="text-white mr-3" id="user-display-name"><i class="fa fa-user-circle"></i> Chưa đăng nhập</span>
                                <button class="btn btn-sm btn-outline-light" id="btn-header-login" onclick="openLoginModal()">Đăng nhập</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="container">
            <div class="row align-items-center">
                <div class="col-lg-2 col-md-3">
                    <div class="header__logo admin-header-logo-wrap">
                        <a href="<%= uri %>"><img src="/img/logo.png" alt="Male-Fashion Logo"></a>
                        <span class="admin-portal-tag">PORTAL</span>
                    </div>
                </div>
                <div class="col-lg-10 col-md-9">
                    <nav class="header__menu mobile-menu">
                        <ul class="admin-nav-list">
                            <li class="<%= activeUsers %>"><a href="/admin-users.html"><i class="fa fa-users"></i> Người dùng</a></li>
                            <li class="<%= activeProducts %>"><a href="/admin-products.html"><i class="fa fa-cube"></i> Sản phẩm</a></li>
                            <li class="<%= activeCategories %>"><a href="/admin-categories.html"><i class="fa fa-tags"></i> Danh mục</a></li>
                            <li class="<%= activeShops %>"><a href="/admin-shops.html"><i class="fa fa-building"></i> Chi nhánh</a></li>
                            <li class="<%= activeTransporters %>"><a href="/admin-transporters.html"><i class="fa fa-truck"></i> Vận chuyển</a></li>
                            <li class="<%= activePromotions %>"><a href="/admin-promotions.html"><i class="fa fa-percent"></i> Khuyến mãi</a></li>
                        </ul>
                    </nav>
                </div>
            </div>
            <div class="canvas__open"><i class="fa fa-bars"></i></div>
        </div>
    </header>
    <!-- Header Section End -->

    <sitemesh:write property="body"/>

<!-- Footer Section Begin -->
    <footer class="footer">
        <div class="container">
            <div class="row">
                <div class="col-lg-3 col-md-6 col-sm-6">
                    <div class="footer__about">
                        <div class="footer__logo">
                            <a href="#"><img src="/img/footer-logo.png" alt=""></a>
                        </div>
                        <p>Hệ thống bán hàng và quản lý chuỗi thương mại điện tử hiện đại.</p>
                        <a href="#"><img src="/img/payment.png" alt=""></a>
                    </div>
                </div>
                <div class="col-lg-2 offset-lg-1 col-md-3 col-sm-6">
                    <div class="footer__widget">
                        <h6>Mua sắm</h6>
                        <ul>
                            <li><a href="/shop.html">Bộ sưu tập mới</a></li>
                            <li><a href="/shop.html">Giảm giá</a></li>
                            <li><a href="/shop.html">Phụ kiện</a></li>
                        </ul>
                    </div>
                </div>
                <div class="col-lg-2 col-md-3 col-sm-6">
                    <div class="footer__widget">
                        <h6>Hệ thống</h6>
                        <ul>
                            <li class="<%= activeUsers %>"><a href="/admin-users.html">Quản lý người dùng</a></li>
                            <li><a href="/contact.html">Trung tâm hỗ trợ</a></li>
                            <li><a href="#">Điều khoản bảo mật</a></li>
                        </ul>
                    </div>
                </div>
                <div class="col-lg-3 offset-lg-1 col-md-6 col-sm-6">
                    <div class="footer__widget">
                        <h6>Nhận thông báo</h6>
                        <div class="footer__newslatter">
                            <p>Đăng ký nhận thông tin khuyến mãi và cập nhật hệ thống mới nhất.</p>
                            <form action="#">
                                <input type="text" placeholder="Email của bạn">
                                <button type="submit"><span class="icon_mail_alt"></span></button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
            <div class="row">
                <div class="col-lg-12 text-center">
                    <div class="footer__copyright__text">
                        <p>Copyright © <script>document.write(new Date().getFullYear());</script> StoreChain Group 4. All rights reserved.</p>
                    </div>
                </div>
            </div>
        </div>
    </footer>
    <!-- Footer Section End -->

    <script src="/js/realtime.js"></script>
</body>
</html>

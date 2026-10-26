<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<%
    String uri = request.getRequestURI();
    String activeHome = (uri.equals("/index.html") || uri.equals("/")) ? "active" : "";
    String activeShop = uri.equals("/shop.html") ? "active" : "";
    String activeBlog = (uri.equals("/blog.html") || uri.equals("/blog-details.html") || uri.equals("/about.html")) ? "active" : "";
    String activeContact = uri.equals("/contact.html") ? "active" : "";
%>


<!DOCTYPE html>
<html lang="zxx">
<head>
    <link href="https://fonts.googleapis.com/css2?family=Nunito+Sans:wght@300;400;600;700;800;900&display=swap"
    rel="stylesheet">
    <link rel="stylesheet" href="/css/bootstrap.min.css" type="text/css">
    <link rel="stylesheet" href="/css/font-awesome.min.css" type="text/css">
    <link rel="stylesheet" href="/css/elegant-icons.css" type="text/css">
    <link rel="stylesheet" href="/css/magnific-popup.css" type="text/css">
    <link rel="stylesheet" href="/css/nice-select.css" type="text/css">
    <link rel="stylesheet" href="/css/owl.carousel.min.css" type="text/css">
    <link rel="stylesheet" href="/css/slicknav.min.css" type="text/css">
    <link rel="stylesheet" href="/css/style.css" type="text/css">
    <link rel="stylesheet" href="/css/auth.css" type="text/css">
    <link rel="stylesheet" href="/css/storechain-rework.css" type="text/css">
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
            <div class="offcanvas__links">
                <a href="/login.html" id="navAuthMobile" class="nav-auth-link">Đăng nhập</a>
                <a href="#">FAQs</a>
            </div>
            <div class="offcanvas__top__hover">
                <span>Usd <i class="arrow_carrot-down"></i></span>
                <ul>
                    <li>USD</li>
                    <li>EUR</li>
                    <li>USD</li>
                </ul>
            </div>
        </div>
        <div class="offcanvas__nav__option">
            <a href="#" class="search-switch"><img src="/img/icon/search.png" alt=""></a>
            <a href="#"><img src="/img/icon/heart.png" alt=""></a>
            <a href="#"><img src="/img/icon/cart.png" alt=""> <span>0</span></a>
            <div class="price">$0.00</div>
        </div>
        <div id="mobile-menu-wrap"></div>
        <div class="offcanvas__text">
            <p>Mua sắm dễ dàng · Giao hàng tận nơi · Hỗ trợ tận tâm</p>
        </div>
    </div>
    <!-- Offcanvas Menu End -->

<!-- Header Section Begin -->
    <header class="header">
        <div class="header__top">
            <div class="container">
                <div class="row">
                    <div class="col-lg-6 col-md-7">
                        <div class="header__top__left">
                            <p>Mua sắm dễ dàng · Giao hàng tận nơi · Hỗ trợ tận tâm</p>
                        </div>
                    </div>
                    <div class="col-lg-6 col-md-5">
                        <div class="header__top__right">
                            <div class="header__top__links">
                                <a href="/login.html" id="navAuthDesktop" class="nav-auth-link">Đăng nhập</a>
                                <a href="#">FAQs</a>
                            </div>
                            <div class="header__top__hover">
                                <span>Usd <i class="arrow_carrot-down"></i></span>
                                <ul>
                                    <li>USD</li>
                                    <li>EUR</li>
                                    <li>USD</li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="container">
            <div class="row">
                <div class="col-lg-3 col-md-3">
                    <div class="header__logo">
                        <a href="./index.html"><img src="/img/logo.png" alt=""></a>
                    </div>
                </div>
                <div class="col-lg-6 col-md-6">
                    <nav class="header__menu mobile-menu">
                        <ul>
                            <li class="<%= activeHome %>"><a href="./index.html">Trang chủ</a></li>
                            <li class="<%= activeShop %>"><a href="./shop.html">Cửa hàng</a></li>
                            <li><a href="#">Danh mục</a>
                                <ul class="dropdown">
                                    <li><a href="./about.html">About Us</a></li>
                                    <li><a href="./shop-details.html">Shop Details</a></li>
                                    <li><a href="./shopping-cart.html">Shopping Cart</a></li>
                                    <li><a href="./checkout.html">Check Out</a></li>
                                    <li><a href="./blog-details.html">Blog Details</a></li>
                                    <li class="admin-nav-only" style="display: none;"><a href="./admin-users.html" style="color: #e53637; font-weight: 700;"><i class="fa fa-users"></i> Quản lý Users</a></li>
                                    <li class="admin-nav-only" style="display: none;"><a href="./admin-products.html" style="color: #e53637; font-weight: 700;"><i class="fa fa-cube"></i> Quản lý Sản phẩm</a></li>
                                    <li class="admin-nav-only" style="display: none;"><a href="./admin-categories.html" style="color: #e53637; font-weight: 700;"><i class="fa fa-folder-open"></i> Quản lý Danh mục</a></li>
                                    <li class="admin-nav-only" style="display: none;"><a href="./admin-shops.html" style="color: #e53637; font-weight: 700;"><i class="fa fa-building"></i> Quản lý Chi nhánh</a></li>
                                    <li class="admin-nav-only" style="display: none;"><a href="./admin-transporters.html" style="color: #e53637; font-weight: 700;"><i class="fa fa-truck"></i> Quản lý Vận chuyển</a></li>
                                </ul>
                            </li>
                            <li class="<%= activeBlog %>"><a href="./blog.html">Tin tức</a></li>
                            <li class="<%= activeContact %>"><a href="./contact.html">Liên hệ</a></li>
                        </ul>
                    </nav>
                </div>
                <div class="col-lg-3 col-md-3">
                    <div class="header__nav__option">
                        <a href="#" class="search-switch"><img src="/img/icon/search.png" alt=""></a>
                        <a href="/profile.html?tab=favorites" title="Sản phẩm yêu thích"><img src="/img/icon/heart.png" alt=""></a>
                        <a href="./shopping-cart.html"><img src="/img/icon/cart.png" alt=""> <span>0</span></a>
                        <div class="price">$0.00</div>
                    </div>
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
                        <p>StoreChain mang đến trải nghiệm mua sắm tiện lợi, sản phẩm đa dạng và dịch vụ tận tâm.</p>
                        <a href="#"><img src="/img/payment.png" alt=""></a>
                    </div>
                </div>
                <div class="col-lg-2 offset-lg-1 col-md-3 col-sm-6">
                    <div class="footer__widget">
                        <h6>Mua sắm</h6>
                        <ul>
                            <li><a href="/shop.html">Tất cả sản phẩm</a></li>
                            <li><a href="/shop.html">Sản phẩm nổi bật</a></li>
                            <li><a href="/shop.html">Danh mục sản phẩm</a></li>
                            <li><a href="/shop.html">Ưu đãi</a></li>
                        </ul>
                    </div>
                </div>
                <div class="col-lg-2 col-md-3 col-sm-6">
                    <div class="footer__widget">
                        <h6>Hỗ trợ</h6>
                        <ul>
                            <li><a href="/contact.html">Liên hệ</a></li>
                            <li><a href="/contact.html">Thanh toán</a></li>
                            <li><a href="/contact.html">Tài khoản của tôi</a></li>
                            <li><a href="/contact.html">Giỏ hàng</a></li>
                        </ul>
                    </div>
                </div>
                <div class="col-lg-3 offset-lg-1 col-md-6 col-sm-6">
                    <div class="footer__widget">
                        <h6>Cập nhật ưu đãi</h6>
                        <div class="footer__newslatter">
                            <p>Theo dõi StoreChain để không bỏ lỡ sản phẩm mới và chương trình ưu đãi.</p>
                            <form action="#">
                                <input type="text" placeholder="Your email">
                                <button type="submit"><span class="icon_mail_alt"></span></button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
            <div class="row">
                <div class="col-lg-12 text-center">
                    <div class="footer__copyright__text">
                        <!-- Link back to Colorlib can't be removed. Template is licensed under CC BY 3.0. -->
                        <p>Bản quyền ©
                            <script>
                                document.write(new Date().getFullYear());
                            </script>2020
                            Đã đăng ký bản quyền | Thiết kế với <i class="fa fa-heart-o"
                            aria-hidden="true"></i> bởi StoreChain
                        </p>
                        <!-- Link back to Colorlib can't be removed. Template is licensed under CC BY 3.0. -->
                    </div>
                </div>
            </div>
        </div>
    </footer>
    <!-- Footer Section End -->

    <script src="/js/realtime.js"></script>
</body>
</html>

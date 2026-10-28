<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>

<!DOCTYPE html>
<html lang="vi">
<head>
    <!-- Font tu host: dam bao hien thi dung tieng Viet -->
    <link rel="stylesheet" href="/css/be-vietnam-pro.css?v=20261028" type="text/css">
    <link rel="stylesheet" href="/css/bootstrap.min.css" type="text/css">
    <link rel="stylesheet" href="/css/font-awesome.min.css" type="text/css">
    <link rel="stylesheet" href="/css/elegant-icons.css" type="text/css">
    <link rel="stylesheet" href="/css/style.css" type="text/css">
    <link rel="stylesheet" href="/css/auth.css" type="text/css">
    <link rel="stylesheet" href="/css/layout-merged.css?v=20261028" type="text/css">
    <link rel="stylesheet" href="/css/vietnamese-ui-fix.css?v=20261028" type="text/css">
    <sitemesh:write property="head"/>
</head>
<body class="store-layout">
<!-- Header Section Begin -->
    <header class="header">
        <div class="container">
            <div class="header__bar">
                <div class="header__logo">
                    <a href="./index.html"><img src="/img/logo.png" alt=""></a>
                </div>
                <nav class="header__menu">
                    <ul>
                        <li><a href="./index.html">Home</a></li>
                        <li><a href="./shop.html">Shop</a></li>
                    </ul>
                </nav>
                <div class="header__bar__right">
                    <div class="header__top__links">
                        <a href="/login.html" id="navAuthDesktop" class="nav-auth-link">Sign in</a>
                    </div>
                    <div class="header__nav__option">
                        <a href="#"><img src="/img/icon/heart.png" alt=""></a>
                        <a href="./shopping-cart.html"><img src="/img/icon/cart.png" alt=""> <span>0</span></a>
                        <div class="price">$0.00</div>
                    </div>
                </div>
            </div>
        </div>
    </header>
    <!-- Header Section End -->

    <sitemesh:write property="body"/>

<script src="/js/realtime.js"></script>
</body>
</html>

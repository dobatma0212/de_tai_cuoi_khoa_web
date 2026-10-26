<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>

<!DOCTYPE html>
<html lang="zxx">
<head>
<meta charset="UTF-8">
    <meta name="description" content="Male_Fashion Template">
    <meta name="keywords" content="Male_Fashion, unica, creative, html">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Male-Fashion | Template</title>

    <!-- Google Font -->

    <link rel="stylesheet" href="/css/shop-redesign.css" type="text/css">
</head>
<body>
<!-- Breadcrumb Section Begin -->
    <section class="breadcrumb-option">
        <div class="container">
            <div class="row">
                <div class="col-lg-12">
                    <div class="breadcrumb__text">
                        <h4>Shop</h4>
                        <div class="breadcrumb__links">
                            <a href="./index.html">Home</a>
                            <span>Shop</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
    <!-- Breadcrumb Section End -->

    <!-- Shop Section Begin -->
    <section class="shop spad shop-redesign">
        <div class="container">
            <div class="row">
                <div class="col-lg-3">
                    <aside class="shop__sidebar shop-filter-panel">
                        <div class="shop-filter-heading"><div><span class="shop-eyebrow">TÌM KIẾM CỦA BẠN</span><h5>Bộ lọc sản phẩm</h5></div><button type="button" id="catalogResetFilters" class="shop-reset-btn" disabled>Đặt lại</button></div>
                        <div class="shop__sidebar__search">
                            <form id="catalogSearchForm">
                                <input type="text" id="catalogSearch" placeholder="Tìm kiếm sản phẩm...">
                                <button type="submit"><span class="icon_search"></span></button>
                            </form>
                        </div>
                        <div class="shop__sidebar__accordion">
                            <div class="accordion" id="accordionExample">
<div class="card">
                                    <div class="card-heading">
                                        <a data-toggle="collapse" data-target="#collapseThree">Lọc theo giá</a>
                                    </div>
                                    <div id="collapseThree" class="collapse show" data-parent="#accordionExample">
                                        <div class="card-body">
                                            <div class="shop__sidebar__price">
                                                <ul id="catalogPriceRanges">
                                                    <li><a href="#" data-min="" data-max="200000">Dưới 200.000₫</a></li>
                                                    <li><a href="#" data-min="200000" data-max="500000">200.000₫ - 500.000₫</a></li>
                                                    <li><a href="#" data-min="500000" data-max="1000000">500.000₫ - 1.000.000₫</a></li>
                                                    <li><a href="#" data-min="1000000" data-max="2000000">1.000.000₫ - 2.000.000₫</a></li>
                                                    <li><a href="#" data-min="2000000" data-max="">Trên 2.000.000₫</a></li>
                                                    <li><a href="#" data-min="" data-max="">Tất cả mức giá</a></li>
                                                </ul>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div class="card">
                                    <div class="card-heading">
                                        <a data-toggle="collapse" data-target="#collapseFour">Kích cỡ (Size)</a>
                                    </div>
                                    <div id="collapseFour" class="collapse show" data-parent="#accordionExample">
                                        <div class="card-body">
                                            <div class="shop__sidebar__size" id="catalogSizes">
                                                <label><input type="radio" name="catalogSize" value="" checked> Tất cả</label>
                                                <label><input type="radio" name="catalogSize" value="S"> S</label>
                                                <label><input type="radio" name="catalogSize" value="M"> M</label>
                                                <label><input type="radio" name="catalogSize" value="L"> L</label>
                                                <label><input type="radio" name="catalogSize" value="XL"> XL</label>
                                                <label><input type="radio" name="catalogSize" value="2XL"> 2XL</label>
                                                <label><input type="radio" name="catalogSize" value="3XL"> 3XL</label>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div class="card">
                                    <div class="card-heading">
                                        <a data-toggle="collapse" data-target="#collapseFive">Màu sắc</a>
                                    </div>
                                    <div id="collapseFive" class="collapse show" data-parent="#accordionExample">
                                        <div class="card-body">
                                            <div class="shop__sidebar__tags" id="catalogColors" style="display:flex;flex-wrap:wrap;gap:8px;align-items:center;">
                                                <a href="#" data-color="" style="font-size:12px;text-decoration:underline;">Tất cả</a>
                                                <a href="#" data-color="Đen" title="Đen" style="width:24px;height:24px;border-radius:50%;display:inline-block;background:#222222;border:1px solid #ddd;"></a>
                                                <a href="#" data-color="Trắng" title="Trắng" style="width:24px;height:24px;border-radius:50%;display:inline-block;background:#ffffff;border:1px solid #ddd;"></a>
                                                <a href="#" data-color="Xám" title="Xám" style="width:24px;height:24px;border-radius:50%;display:inline-block;background:#9e9e9e;border:1px solid #ddd;"></a>
                                                <a href="#" data-color="Xanh navy" title="Xanh navy" style="width:24px;height:24px;border-radius:50%;display:inline-block;background:#1b2a4a;border:1px solid #ddd;"></a>
                                                <a href="#" data-color="Đỏ" title="Đỏ" style="width:24px;height:24px;border-radius:50%;display:inline-block;background:#d32f2f;border:1px solid #ddd;"></a>
                                                <a href="#" data-color="Be" title="Be" style="width:24px;height:24px;border-radius:50%;display:inline-block;background:#d9c8a9;border:1px solid #ddd;"></a>
                                                <a href="#" data-color="Nâu" title="Nâu" style="width:24px;height:24px;border-radius:50%;display:inline-block;background:#7a4b2a;border:1px solid #ddd;"></a>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div class="card">
                                    <div class="card-heading">
                                        <a data-toggle="collapse" data-target="#collapseSix">Từ khóa</a>
                                    </div>
                                    <div id="collapseSix" class="collapse show" data-parent="#accordionExample">
                                        <div class="card-body">
                                            <div class="shop__sidebar__tags" id="catalogTags">
                                                <a href="#" data-tag="áo">Áo</a>
                                                <a href="#" data-tag="quần">Quần</a>
                                                <a href="#" data-tag="giày">Giày</a>
                                                <a href="#" data-tag="túi">Túi</a>
                                                <a href="#" data-tag="phụ kiện">Phụ kiện</a>
                                                <a href="#" data-tag="sneaker">Sneaker</a>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </aside>
                </div>
                <div class="col-lg-9">
                    <div class="shop-results-heading"><div><span class="shop-eyebrow">STORECHAIN COLLECTION</span><h3>Khám phá sản phẩm</h3><p>Tìm đúng phong cách của bạn với bộ lọc và sắp xếp nhanh.</p></div></div>
                    <div class="shop__product__option shop-toolbar">
                        <div class="row">
                            <div class="col-lg-6 col-md-6 col-sm-6">
                                <div class="shop__product__option__left">
                                    <p id="catalogCount">Đang tải sản phẩm...</p>
                                </div>
                            </div>
                            <div class="col-lg-6 col-md-6 col-sm-6">
                                <div class="shop__product__option__right">
                                    <p>Sắp xếp theo:</p>
                                    <select id="catalogSort">
                                        <option value="">Mới nhất</option>
                                        <option value="priceAsc">Giá thấp → cao</option>
                                        <option value="priceDesc">Giá cao → thấp</option>
                                        <option value="name">Tên A → Z</option>
                                        <option value="bestseller">Bán chạy nhất</option>
                                        <option value="rating">Đánh giá cao nhất</option>
                                        <option value="favorite">Yêu thích nhiều nhất</option>
                                    </select>
                                </div>
                            </div>
                        </div>
                    </div>
                    <h5 id="catalogCategoryTitle" style="display:none;"></h5>
                    <div class="row" id="categoryCardGrid">
                        <!-- Thẻ danh mục (ảnh = sản phẩm đại diện), render động bằng /js/catalog.js -->
                    </div>
                    <div class="row" id="catalogGrid">
                        <!-- Sản phẩm được render động bằng /js/catalog.js (danh mục toàn hãng) -->
                    </div>
                    <div class="shop-pagination-meta" id="catalogPaginationMeta">Chọn trang để xem thêm sản phẩm</div>
                    <div class="row" id="catalogPaginationRow" style="display:none;">
                        <div class="col-lg-12">
                            <div class="product__pagination" id="catalogPagination"></div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
    <!-- Shop Section End -->

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
    <script src="/js/cart-api.js"></script>
    <script src="/js/sc-shop-ui.js"></script>
    <script src="/js/catalog.js"></script>
</body>
</html>

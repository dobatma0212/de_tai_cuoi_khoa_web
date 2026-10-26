<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="description" content="StoreChain - Khám phá phong cách của bạn với bộ sưu tập thời trang mới nhất.">
    <meta name="keywords" content="thời trang, quần áo, phụ kiện, StoreChain">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>StoreChain | Thời trang dành cho bạn</title>
    <link rel="stylesheet" href="/css/home-redesign.css" type="text/css">
</head>
<body class="home-redesign">
    <main>
        <!-- Hero -->
        <section class="home-hero">
            <div class="home-hero__image" aria-hidden="true"></div>
            <div class="container home-hero__container">
                <div class="home-hero__copy">
                    <span class="home-eyebrow"><span></span> BỘ SƯU TẬP MÙA MỚI</span>
                    <h1>Mặc chất riêng.<br><em>Sống phong cách.</em></h1>
                    <p>Khám phá những thiết kế được tuyển chọn cho nhịp sống hiện đại — dễ phối, dễ mặc và luôn là chính bạn.</p>
                    <div class="home-hero__actions">
                        <a href="/shop.html" class="home-btn home-btn--dark">Khám phá cửa hàng <span aria-hidden="true">↗</span></a>
                        <a href="#homeFeatured" class="home-text-link">Xem sản phẩm nổi bật <span aria-hidden="true">↓</span></a>
                    </div>
                    <div class="home-proof">
                        <div class="home-proof__avatars" aria-hidden="true"><span>SC</span><span>+</span><span>♥</span></div>
                        <div><strong>Phong cách mỗi ngày</strong><small>Chọn món đồ hợp với bạn</small></div>
                    </div>
                </div>
                <div class="home-hero__note"><span>01 / 02</span><i></i><span>NEW SEASON EDIT</span></div>
            </div>
            <a class="home-scroll" href="#homeCategories"><span></span> CUỘN ĐỂ KHÁM PHÁ</a>
        </section>

        <!-- Trust strip -->
        <section class="home-benefits" aria-label="Dịch vụ cửa hàng">
            <div class="container home-benefits__grid">
                <div class="home-benefit"><span class="home-benefit__icon">↗</span><div><strong>Giao hàng tiện lợi</strong><small>Theo dõi đơn hàng dễ dàng</small></div></div>
                <div class="home-benefit"><span class="home-benefit__icon">◇</span><div><strong>Thanh toán an toàn</strong><small>Quy trình thanh toán rõ ràng</small></div></div>
                <div class="home-benefit"><span class="home-benefit__icon">♡</span><div><strong>Chọn món bạn thích</strong><small>Lưu sản phẩm yêu thích</small></div></div>
                <div class="home-benefit"><span class="home-benefit__icon">✓</span><div><strong>Hỗ trợ tận tâm</strong><small>Đồng hành cùng trải nghiệm mua sắm</small></div></div>
            </div>
        </section>

        <!-- Categories -->
        <section class="home-section home-categories" id="homeCategories">
            <div class="container">
                <div class="home-section-heading">
                    <div><span class="home-eyebrow">TÌM PHONG CÁCH CỦA BẠN</span><h2>Mua sắm theo <em>danh mục</em></h2></div>
                    <a href="/shop.html" class="home-arrow-link">Tất cả sản phẩm <span>↗</span></a>
                </div>
                <div class="home-category-grid">
                    <a href="/shop.html" class="home-category-card home-category-card--large">
                        <img src="/img/banner/banner-1.jpg" alt="Bộ sưu tập thời trang">
                        <span class="home-category-card__number">01</span><div><small>THIẾT KẾ DỄ PHỐI</small><h3>Trang phục</h3><span class="home-category-card__link">Khám phá <b>↗</b></span></div>
                    </a>
                    <a href="/shop.html" class="home-category-card">
                        <img src="/img/banner/banner-2.jpg" alt="Phụ kiện thời trang">
                        <span class="home-category-card__number">02</span><div><small>ĐIỂM NHẤN CÁ TÍNH</small><h3>Phụ kiện</h3><span class="home-category-card__link">Khám phá <b>↗</b></span></div>
                    </a>
                    <a href="/shop.html" class="home-category-card">
                        <img src="/img/banner/banner-3.jpg" alt="Giày và phong cách đường phố">
                        <span class="home-category-card__number">03</span><div><small>BƯỚC ĐI TỰ TIN</small><h3>Giày & lifestyle</h3><span class="home-category-card__link">Khám phá <b>↗</b></span></div>
                    </a>
                </div>
            </div>
        </section>

        <!-- Dynamic products: IDs retained for home.js -->
        <section class="home-section home-featured" id="homeFeatured">
            <div class="container">
                <div class="home-section-heading home-section-heading--products">
                    <div><span class="home-eyebrow">ĐƯỢC TUYỂN CHỌN CHO BẠN</span><h2>Khám phá <em>sản phẩm</em></h2><p>Những lựa chọn đáng chú ý, cập nhật theo xu hướng mua sắm.</p></div>
                    <a href="/shop.html" class="home-arrow-link">Đến cửa hàng <span>↗</span></a>
                </div>
                <div class="home-product-tabs-wrap"><ul class="filter__controls home-product-tabs" id="homeTabs">
                    <li class="active" data-type="new">Mới nhất</li><li data-type="bestseller">Bán chạy</li><li data-type="toprated">Đánh giá cao</li><li data-type="favorite">Yêu thích</li>
                </ul><span class="home-tabs-caption">CẬP NHẬT LIÊN TỤC</span></div>
                <div class="row home-product-grid" id="homeGrid"><div class="col-12 text-center py-4">Đang tải sản phẩm...</div></div>
                <div class="home-pagination-wrap"><div class="product__pagination home-pagination" id="homePagination"></div></div>
            </div>
        </section>

        <!-- Recently viewed -->
        <section class="home-section home-recent" id="homeRecentSection" style="display:none;">
            <div class="container"><div class="home-section-heading"><div><span class="home-eyebrow">QUAY LẠI VỚI LỰA CHỌN CỦA BẠN</span><h2>Sản phẩm đã xem <em>gần đây</em></h2></div></div><div class="row home-product-grid" id="homeRecentGrid"></div></div>
        </section>

        <!-- Editorial promotion -->
        <section class="home-editorial">
            <div class="container home-editorial__inner">
                <div class="home-editorial__visual"><img src="/img/hero/hero-2.jpg" alt="Gợi ý phối đồ cho phong cách hằng ngày"><span class="home-editorial__stamp">STYLE<br>YOUR<br>DAY</span></div>
                <div class="home-editorial__copy"><span class="home-eyebrow">ÍT HƠN NHƯNG CHẤT HƠN</span><h2>Phong cách không cần ồn ào.<br><em>Chỉ cần đúng là bạn.</em></h2><p>Tìm những món đồ có thể đồng hành qua nhiều dịp — từ ngày thường năng động đến những khoảnh khắc cần chỉn chu.</p><a href="/shop.html" class="home-btn home-btn--light">Tìm cảm hứng mua sắm <span>↗</span></a><div class="home-editorial__index">STORECHAIN <span>—</span> YOUR DAILY STYLE</div></div>
            </div>
        </section>

        <!-- Brand note -->
        <section class="home-bottom-note"><div class="container"><span class="home-eyebrow">STORECHAIN / YOUR EVERYDAY EDIT</span><h2>Chọn điều bạn thích.<br><em>Tạo nên dấu ấn riêng.</em></h2><a href="/shop.html" class="home-arrow-link">Bắt đầu mua sắm <span>↗</span></a></div></section>
    </main>

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
    <script src="/js/home.js"></script>
</body>
</html>

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>

<!DOCTYPE html>
<html lang="zxx">
<head>
<meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Chi tiết sản phẩm | Male-Fashion</title>

    

    

    <style>
        .pd-section {
            padding: 40px 0 80px;
            background: #f5f5f5;
            min-height: 60vh;
        }

        .pd-card {
            background: #fff;
            border-radius: 8px;
            box-shadow: 0 4px 20px rgba(0, 0, 0, .07);
            padding: 28px;
            margin-bottom: 24px;
        }

        .pd-image {
            width: 100%;
            max-height: 480px;
            object-fit: contain;
            border-radius: 8px;
            background: #fafafa;
        }

        .pd-name {
            font-size: 26px;
            font-weight: 800;
        }

        .pd-price {
            font-size: 30px;
            font-weight: 900;
            color: #e53637;
            cursor: pointer;
            display: inline-block;
            padding: 4px 10px;
            border-radius: 6px;
            transition: background .15s;
        }

        .pd-price:hover {
            background: #fff0f0;
            text-decoration: underline;
        }

        .pd-price-hint {
            font-size: 12px;
            color: #999;
        }

        .size-btn {
            min-width: 44px;
            padding: 7px 14px;
            border: 1.5px solid #ddd;
            background: #fff;
            border-radius: 6px;
            font-weight: 700;
            cursor: pointer;
        }

        .size-btn.active {
            border-color: #e53637;
            color: #e53637;
        }

        .buy-btn {
            background: #e53637;
            color: #fff;
            border: none;
            font-weight: 800;
            padding: 12px 34px;
            border-radius: 6px;
        }

        .buy-btn:hover {
            background: #c72b2c;
            color: #fff;
        }

        .buy-btn:disabled {
            opacity: .6;
            cursor: not-allowed;
        }

        .stock-badge {
            font-size: 13px;
        }

        /* Form bình luận: khung, nút tải ảnh/video và nút đăng đồng bộ với nút Mua hàng / Yêu thích */
        .review-form-box {
            padding: 20px 22px;
            border: 1px solid #eee;
            border-radius: 8px;
            background: #fafafa;
        }

        .review-actions {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 12px;
        }

        .review-upload {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 10px 12px;
            min-width: 0;
        }

        .review-upload-input {
            position: absolute;
            width: 1px;
            height: 1px;
            opacity: 0;
            overflow: hidden;
        }

        .review-upload-btn {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            margin: 0;
            padding: 11px 20px;
            border: 1.5px solid #e53637;
            border-radius: 6px;
            background: #fff;
            color: #e53637;
            font-size: 14px;
            font-weight: 800;
            line-height: 1.2;
            cursor: pointer;
            transition: background .15s, color .15s;
        }

        .review-upload-btn:hover,
        .review-upload-input:focus + .review-upload-btn {
            background: #e53637;
            color: #fff;
        }

        .review-file-info {
            font-size: 13px;
            color: #777;
            max-width: 340px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .review-file-info.has-files {
            color: #198754;
            font-weight: 700;
        }

        .review-submit {
            display: inline-flex;
            align-items: center;
            gap: 8px;
        }

        /* Hai nút cùng chiều cao */
        .review-upload-btn,
        .review-submit {
            height: 46px;
            padding-top: 0;
            padding-bottom: 0;
            box-sizing: border-box;
        }

        .review-submit {
            padding-left: 26px;
            padding-right: 26px;
        }

        .stars {
            color: #f5b301;
            font-size: 15px;
        }

        .stars .off {
            color: #ddd;
        }

        .review-item {
            border-bottom: 1px solid #eee;
            padding: 16px 0;
        }

        .review-avatar {
            width: 42px;
            height: 42px;
            border-radius: 50%;
            object-fit: cover;
            background: #eee;
        }

        .review-media {
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
            margin-top: 10px;
        }

        .review-media img {
            width: 110px;
            height: 110px;
            object-fit: cover;
            border-radius: 6px;
        }

        .review-media video {
            width: 240px;
            border-radius: 6px;
        }

        .star-pick {
            font-size: 26px;
            color: #ddd;
            cursor: pointer;
            margin-right: 4px;
        }

        .star-pick.on {
            color: #f5b301;
        }

        .back-link {
            color: #666;
            text-decoration: none;
            font-weight: 700;
        }

        .back-link:hover {
            color: #e53637;
        }
    
        .color-dot {
            width: 34px; height: 34px; border-radius: 50%;
            border: 2px solid #ddd; cursor: pointer; display: inline-block;
            transition: transform .12s, border-color .12s;
        }
        .color-dot:hover { transform: scale(1.1); }
        .color-dot.active { border-color: #e53637; box-shadow: 0 0 0 2px rgba(229,54,55,.25); }
    </style>
</head>
<body>

    

    <section class="breadcrumb-option">
        <div class="container">
            <div class="row">
                <div class="col-lg-12">
                    <div class="breadcrumb__text">
                        <h4>Chi tiết sản phẩm</h4>
                        <div class="breadcrumb__links">
                            <a href="./index.html">Home</a>
                            <a href="#" id="breadcrumbShopLink">Chi nhánh</a>
                            <span id="breadcrumbProduct"></span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <section class="pd-section">
        <div class="container">
            <a href="/shop.html" id="backToShop" class="back-link mb-3 d-inline-block">
                <i class="fa fa-arrow-left"></i> Tiếp tục mua sắm tại trang chủ hãng
            </a>

            <div id="pdError" class="alert alert-warning" style="display:none;"></div>

            <!-- Thông tin sản phẩm -->
            <div id="pdMain" class="pd-card" style="display:none;">
                <div class="row">
                    <div class="col-lg-6 mb-4">
                        <img id="pdImage" src="/img/product/product-1.jpg" class="pd-image" alt="product">
                    </div>
                    <div class="col-lg-6">
                        <small class="text-muted text-uppercase" id="pdCategory"></small>
                        <h2 id="pdName" class="pd-name"></h2>
                        <div class="stars my-2" id="pdRatingStars"></div>
                        <small class="text-muted" id="pdRatingText"></small>
                        <div class="my-3">
                            <span id="pdPrice" class="pd-price" title="Bấm để mua hàng"></span>
                            <div class="pd-price-hint"><i class="fa fa-hand-pointer-o"></i> Bấm vào giá để đưa sản phẩm vào giỏ hàng</div>
                        </div>
                        <p class="stock-badge mb-2"><b>Tồn kho:</b> <span id="pdStock"></span></p>
                        <div id="pdSizeSection" style="display:none;" class="mb-3">
                            <b>Chọn size:</b>
                            <div id="pdSizes" class="d-flex gap-2 flex-wrap mt-2"></div>
                        </div>
                        <div id="pdColorSection" style="display:none;" class="mb-3">
                            <b>Chọn màu:</b> <small id="pdColorName" class="text-muted"></small>
                            <div id="pdColors" class="d-flex gap-2 flex-wrap mt-2"></div>
                        </div>
                        <div class="mb-3">
                            <b>Số lượng:</b>
                            <input type="number" id="pdQuantity" class="form-control ml-2 d-inline-block"
                                   value="1" min="1" style="width:90px;">
                        </div>
                        <button type="button" id="pdBuyBtn" class="buy-btn">
                            <i class="fa fa-shopping-cart"></i> Mua hàng
                        </button>
                        <button type="button" id="pdFavBtn" class="btn btn-outline-danger ml-2" aria-pressed="false">
                            <i class="fa fa-heart-o"></i> <span id="pdFavText">Yêu thích</span>
                        </button>
                        <hr>
                        <h6 class="font-weight-bold">Mô tả sản phẩm</h6>
                        <p id="pdDescription" class="text-muted"></p>
                    </div>
                </div>
            </div>

            <!-- Đánh giá sản phẩm -->
            <div id="pdReviews" class="pd-card" style="display:none;">
                <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap">
                    <h5 class="font-weight-bold mb-0">Đánh giá sản phẩm</h5>
                    <div>
                        <span class="stars" id="pdAvgStars"></span>
                        <b id="pdAvgText" class="ml-1"></b>
                    </div>
                </div>

                <!-- Form thêm đánh giá -->
                <div id="reviewFormBox" style="display:none;" class="review-form-box mb-4">
                    <h6 class="font-weight-bold mb-2">Viết đánh giá của bạn</h6>
                    <div class="mb-2" id="starPicker"></div>
                    <textarea id="reviewComment" class="form-control mb-1" rows="3"
                              maxlength="2000" placeholder="Nhận xét của bạn về sản phẩm (tối thiểu 50 ký tự)..."></textarea>
                    <small id="reviewCounter" class="d-block mb-2 text-muted">0/50 ký tự tối thiểu</small>
                    <div class="review-actions">
                        <div class="review-upload">
                            <input type="file" id="reviewFiles" class="review-upload-input" multiple accept="image/*,video/mp4,video/webm">
                            <label for="reviewFiles" class="review-upload-btn"><i class="fa fa-camera"></i> Thêm ảnh / video</label>
                            <span id="reviewFileInfo" class="review-file-info">Tùy chọn · ảnh hoặc video minh họa</span>
                        </div>
                        <button type="button" id="btnSubmitReview" class="buy-btn review-submit">
                            <i class="fa fa-paper-plane"></i> Đăng đánh giá
                        </button>
                    </div>
                    <div class="alert auth-alert mt-2" id="reviewAlert" style="display:none;"></div>
                </div>
                <div id="reviewLoginHint" class="alert alert-light" style="display:none;">
                    <a href="/login.html">Đăng nhập</a> để viết đánh giá sản phẩm.
                </div>
                <div id="reviewNotAllowedHint" class="alert alert-light" style="display:none;"></div>

                <div id="reviewList"></div>
            </div>

            <!-- Sản phẩm đã xem gần đây -->
            <div id="pdRecentBox" class="pd-card" style="display:none;">
                <h5 class="font-weight-bold mb-3">Sản phẩm bạn đã xem gần đây</h5>
                <div class="row" id="pdRecentGrid"></div>
            </div>
        </div>
    </section>

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
    <script src="/js/sc-shop-ui.js?v=20261028"></script>
    <script src="/js/product-detail.js"></script>
</body>
</html>

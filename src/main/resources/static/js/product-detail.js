/**
 * product-detail.js
 * Trang chi tiết sản phẩm (product-detail.html?productId=...):
 * - Ảnh lớn, giá (bấm để mua), tồn kho, chọn size, mô tả
 * - Đánh giá 5 sao kèm ảnh/video minh họa
 * - Mua hàng => thêm vào giỏ hàng của user đang đăng nhập
 */
(function () {
    "use strict";

    function $(id) { return document.getElementById(id); }

    function getToken() {
        var s = window.ScAuth ? window.ScAuth.getSession() : null;
        return (s && s.token) || localStorage.getItem("jwt_token");
    }

    function isLoggedIn() { return !!getToken(); }

    function escapeHtml(str) {
        return String(str == null ? "" : str)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
    }

    function formatVnd(value) {
        var n = Number(value);
        if (isNaN(n)) return "0₫";
        return n.toLocaleString("vi-VN") + "₫";
    }

    /** "Đen, Trắng" -> ["Đen","Trắng"] */
    function parseColorList(value) {
        return String(value || "").split(",")
            .map(function (s) { return s.trim(); })
            .filter(function (s) { return s.length > 0; });
    }

    function starsHtml(rating) {
        var html = "";
        for (var i = 1; i <= 5; i++) {
            html += '<i class="fa fa-star' + (i <= Math.round(rating) ? "" : " off") + '"></i>';
        }
        return html;
    }

    var productId = null;
    var product = null;
    var selectedSize = "";
    var selectedColor = "";
    var pickedRating = 0;

    document.addEventListener("DOMContentLoaded", function () {
        var params = new URLSearchParams(window.location.search);
        productId = parseInt(params.get("productId"), 10);
        if (!productId || isNaN(productId)) {
            $("pdError").style.display = "block";
            $("pdError").textContent = "Không xác định được sản phẩm.";
            return;
        }
        loadProduct();
        loadReviews();
        setupReviewForm();
        setupFavorite();
        trackViewAndShowRecent();
        updateCartBadge();
    });

    // ============ Yêu thích ============

    function setupFavorite() {
        var btn = $("pdFavBtn");
        if (!btn) return;
        function paint() {
            var on = ScShop.isFav(productId);
            btn.classList.toggle("btn-danger", on);
            btn.classList.toggle("btn-outline-danger", !on);
            btn.setAttribute("aria-pressed", on ? "true" : "false");
            btn.querySelector("i").className = on ? "fa fa-heart" : "fa fa-heart-o";
            $("pdFavText").textContent = on ? "Đã yêu thích" : "Yêu thích";
        }
        ScShop.loadFavIds().then(paint);
        btn.addEventListener("click", function () {
            ScShop.toggleFav(productId).then(paint).catch(function (err) {
                if (err && err.message !== "Chưa đăng nhập") alert(err.message);
            });
        });
    }

    // ============ Sản phẩm đã xem ============

    /** Ghi nhận lượt xem và render dữ liệu sản phẩm gần đây cho cả khách và tài khoản. */
    function trackViewAndShowRecent() {
        var grid = $("pdRecentGrid");
        var box = $("pdRecentBox");
        if (!grid || !box || !window.ScShop) return;
        ScShop.bindCards(grid);
        ScShop.recordView(productId).then(function () {
            return ScShop.getRecentlyViewed(9);
        }).then(function (list) {
            var currentId = Number(productId);
            var others = (list || []).filter(function (p) { return Number(p.id) !== currentId; }).slice(0, 4);
            if (!others.length) {
                box.style.display = "none";
                grid.innerHTML = "";
                return;
            }
            grid.innerHTML = others.map(function (p) {
                return ScShop.cardHtml(p, "col-lg-3 col-md-4 col-sm-6");
            }).join("");
            ScShop.applyBackgrounds(grid);
            box.style.display = "block";
        }).catch(function (err) {
            if (window.console) console.warn("Không hiển thị được sản phẩm đã xem:", err);
            box.style.display = "none";
        });
    }

    window.addEventListener("pageshow", function (e) {
        if (e.persisted && productId) trackViewAndShowRecent();
    });

    /** Hiển thị số sản phẩm trong giỏ lên icon giỏ hàng ở header */
    function updateCartBadge() {
        if (!isLoggedIn()) return;
        if (!window.StoreChainCartAPI) return;
        window.StoreChainCartAPI.count().then(function (count) {
            var spans = document.querySelectorAll('a[href="./shopping-cart.html"] span, a[href="/shopping-cart.html"] span');
            Array.prototype.forEach.call(spans, function (span) { span.textContent = count == null ? 0 : count; });
        }).catch(function () { });
    }

    function loadProduct() {
        fetch("/api/public/products/" + productId)
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (data.success === false) throw new Error(data.message);
                product = data.data;
                renderProduct(product);
            })
            .catch(function (err) {
                $("pdError").style.display = "block";
                $("pdError").textContent = err.message || "Không tải được sản phẩm.";
            });
    }

    function renderProduct(p) {
        document.title = p.name + " | Male-Fashion";
        $("breadcrumbProduct").textContent = p.name;
        var shopUrl = "/shop.html";
        $("breadcrumbShopLink").href = shopUrl;
        $("backToShop").href = shopUrl;
        $("pdImage").src = p.imageUrl || "/img/product/product-1.jpg";
        $("pdCategory").textContent = p.categoryName || "";
        $("pdName").textContent = p.name;
        if (p.onFlashSale && p.originalPrice && Number(p.originalPrice) > Number(p.price)) {
            $("pdPrice").innerHTML = formatVnd(p.price) +
                ' <del style="color:#999;font-size:16px;font-weight:400;margin-left:8px;">' + formatVnd(p.originalPrice) + "</del>" +
                ' <span style="background:#e53637;color:#fff;font-size:12px;font-weight:700;padding:3px 8px;border-radius:4px;margin-left:8px;vertical-align:middle;"><i class="fa fa-bolt"></i> FLASH SALE -' + (p.discountPercent || "") + "%</span>";
        } else {
            $("pdPrice").textContent = formatVnd(p.price);
        }
        // Tồn theo size chỉ dùng NỘI BỘ để khóa size hết hàng & phân phối đơn —
        // không hiển thị số lượng tồn cho khách
        var sizeMap = p.totalSizeStocks || null;
        var hasSizes = p.sizesList && p.sizesList.length > 0 && sizeMap;
        function updateStockLabel() {
            if (hasSizes && selectedSize && sizeMap.hasOwnProperty(selectedSize)) {
                var qty = sizeMap[selectedSize] || 0;
                $("pdStock").textContent = qty > 0 ? "Còn hàng" : ("Hết size " + selectedSize + " — vui lòng chọn size khác");
                $("pdStock").style.color = qty > 0 ? "" : "#e53637";
            } else {
                $("pdStock").textContent = p.stock > 0 ? "Còn hàng" : "Hết hàng";
                $("pdStock").style.color = "";
            }
        }
        updateStockLabel();
        $("pdDescription").textContent = p.description || "Chưa có mô tả.";
        $("pdRatingStars").innerHTML = starsHtml(p.avgRating || 0);
        $("pdRatingText").textContent = p.reviewCount > 0
            ? (p.avgRating + "/5 — " + p.reviewCount + " đánh giá")
            : "Chưa có đánh giá";

        // Chọn size (size hết hàng bị khóa, hiện mờ kèm số tồn)
        var sizes = p.sizesList || [];
        if (sizes.length) {
            $("pdSizeSection").style.display = "block";
            $("pdSizes").innerHTML = sizes.map(function (s) {
                var qty = sizeMap && sizeMap.hasOwnProperty(s) ? (sizeMap[s] || 0) : null;
                var out = qty !== null && qty <= 0;
                return '<button type="button" class="size-btn" data-size="' + escapeHtml(s) + '"' +
                    (out ? ' disabled title="Tạm hết size này"' : '') +
                    ' style="' + (out ? "opacity:.4;" : "") + '">' +
                    escapeHtml(s) + "</button>";
            }).join("");
            Array.prototype.forEach.call($("pdSizes").querySelectorAll(".size-btn:not([disabled])"), function (btn) {
                btn.addEventListener("click", function () {
                    selectedSize = btn.getAttribute("data-size");
                    Array.prototype.forEach.call($("pdSizes").querySelectorAll(".size-btn"), function (b) {
                        b.classList.remove("active");
                    });
                    btn.classList.add("active");
                    updateStockLabel();
                });
            });
            var firstAvailable = $("pdSizes").querySelector(".size-btn:not([disabled])");
            if (firstAvailable) {
                selectedSize = firstAvailable.getAttribute("data-size");
                firstAvailable.classList.add("active");
            } else {
                selectedSize = null;
            }
            updateStockLabel();
        }

        // Chọn màu (vòng tròn màu, giống bảng màu bên trang quản lý)
        var colors = parseColorList(p.color);
        var COLOR_HEX = {
            "Đen": "#222222", "Trắng": "#ffffff", "Xám": "#9e9e9e",
            "Xanh navy": "#1b2a4a", "Đỏ": "#d32f2f", "Be": "#d9c8a9",
            "Nâu": "#7a4b2a", "Xanh lá": "#2e7d32", "Vàng": "#f2c200", "Hồng": "#f4a7b9"
        };
        if (colors.length) {
            $("pdColorSection").style.display = "block";
            $("pdColors").innerHTML = colors.map(function (c) {
                var hex = COLOR_HEX[c] || "#cccccc";
                return '<span class="color-dot" data-color="' + escapeHtml(c) + '" title="' + escapeHtml(c) + '"' +
                    ' style="background:' + hex + ';"></span>';
            }).join("");
            Array.prototype.forEach.call($("pdColors").querySelectorAll(".color-dot"), function (dot) {
                dot.addEventListener("click", function () {
                    selectedColor = dot.getAttribute("data-color");
                    $("pdColorName").textContent = "— " + selectedColor;
                    Array.prototype.forEach.call($("pdColors").querySelectorAll(".color-dot"), function (d) {
                        d.classList.remove("active");
                    });
                    dot.classList.add("active");
                });
            });
            selectedColor = colors[0];
            $("pdColorName").textContent = "— " + selectedColor;
            $("pdColors").querySelector(".color-dot").classList.add("active");
        } else {
            selectedColor = "";
        }

        $("pdMain").style.display = "block";
        $("pdReviews").style.display = "block";

        // Mua hàng: bấm giá hoặc nút Mua hàng
        $("pdBuyBtn").addEventListener("click", buyNow);
        $("pdPrice").addEventListener("click", buyNow);
    }

    function buyNow() {
        if (!isLoggedIn()) {
            if (confirm("Bạn cần đăng nhập để mua hàng. Chuyển đến trang đăng nhập?")) {
                window.location.href = "/login.html";
            }
            return;
        }
        var qty = Math.max(1, parseInt($("pdQuantity").value, 10) || 1);
        if (!window.StoreChainCartAPI) {
            alert("Module giỏ hàng chưa được tải. Vui lòng tải lại trang.");
            return;
        }
        window.StoreChainCartAPI.add(productId, qty, selectedSize, selectedColor).then(function () {
            alert("Đã thêm vào giỏ hàng: " + product.name +
                (selectedSize ? " (size " + selectedSize + ")" : "") +
                (selectedColor ? " - màu " + selectedColor : "") + " x" + qty);
            updateCartBadge();
        }).catch(function (err) {
            alert(err.message || "Không thể thêm vào giỏ hàng.");
        });
    }

    // ============ Đánh giá ============

    var MIN_COMMENT = 50;

    /** Chỉ hiện form cho người đã mua + nhận hàng và chưa đánh giá (server vẫn kiểm tra lại khi gửi) */
    function setupReviewForm() {
        if (!isLoggedIn()) {
            $("reviewLoginHint").style.display = "block";
            return;
        }
        ScShop.authFetch("/api/user/products/" + productId + "/review-eligibility").then(function (e) {
            if (e && e.canReview) {
                showReviewForm();
            } else {
                showReviewNotice(e && e.alreadyReviewed
                    ? "Bạn đã đánh giá sản phẩm này rồi."
                    : "Chỉ khách đã mua và nhận sản phẩm này mới có thể viết đánh giá.");
            }
        }).catch(function () {
            showReviewNotice("Không kiểm tra được quyền đánh giá, vui lòng tải lại trang.");
        });
    }

    function showReviewNotice(text) {
        $("reviewFormBox").style.display = "none";
        var hint = $("reviewNotAllowedHint");
        hint.textContent = text;
        hint.style.display = "block";
    }

    function showReviewForm() {
        $("reviewFormBox").style.display = "block";
        var commentEl = $("reviewComment");
        function updateCounter() {
            var len = commentEl.value.trim().length;
            var counter = $("reviewCounter");
            counter.textContent = len + "/" + MIN_COMMENT + " ký tự tối thiểu";
            counter.className = "d-block mb-2 " + (len >= MIN_COMMENT ? "text-success" : "text-muted");
        }
        commentEl.addEventListener("input", updateCounter);
        updateCounter();
        // 5 ngôi sao chọn điểm
        var picker = $("starPicker");
        for (var i = 1; i <= 5; i++) {
            var star = document.createElement("i");
            star.className = "fa fa-star star-pick";
            star.setAttribute("data-star", i);
            star.addEventListener("click", function () {
                pickedRating = parseInt(this.getAttribute("data-star"), 10);
                Array.prototype.forEach.call(picker.querySelectorAll(".star-pick"), function (s) {
                    s.classList.toggle("on", parseInt(s.getAttribute("data-star"), 10) <= pickedRating);
                });
            });
            picker.appendChild(star);
        }
        $("btnSubmitReview").addEventListener("click", submitReview);
        $("reviewFiles").addEventListener("change", updateFileInfo);
    }

    /** Hiển thị số tệp / tên tệp đã chọn cạnh nút "Thêm ảnh / video" */
    function updateFileInfo() {
        var files = $("reviewFiles").files;
        var info = $("reviewFileInfo");
        if (!files || !files.length) {
            info.textContent = "Tùy chọn · ảnh hoặc video minh họa";
            info.classList.remove("has-files");
            info.removeAttribute("title");
            return;
        }
        var names = Array.prototype.map.call(files, function (f) { return f.name; }).join(", ");
        info.textContent = "Đã chọn " + files.length + " tệp: " + names;
        info.title = names;
        info.classList.add("has-files");
    }

    function submitReview() {
        var alertEl = $("reviewAlert");
        alertEl.style.display = "none";
        if (!pickedRating) {
            alert("Vui lòng chọn số sao đánh giá.");
            return;
        }
        var commentLen = $("reviewComment").value.trim().length;
        if (commentLen < MIN_COMMENT) {
            alert("Bình luận phải có ít nhất " + MIN_COMMENT + " ký tự (hiện có " + commentLen + ").");
            return;
        }
        var files = $("reviewFiles").files;
        var uploadsLeft = files.length;
        // Khóa nút trong lúc gửi để không đăng trùng
        var submitBtn = $("btnSubmitReview");
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fa fa-spinner fa-spin"></i> Đang đăng...';
        var media = [];

        var sendReview = function () {
            fetch("/api/user/products/" + productId + "/reviews", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": "Bearer " + getToken()
                },
                body: JSON.stringify({
                    rating: pickedRating,
                    comment: $("reviewComment").value.trim(),
                    media: media
                })
            }).then(function (r) { return r.json(); }).then(function (data) {
                if (data.success === false) throw new Error(data.message);
                alertEl.textContent = "Đã gửi đánh giá. Cảm ơn bạn!";
                alertEl.style.display = "block";
                setTimeout(function () { alertEl.style.display = "none"; }, 4000);
                $("reviewComment").value = "";
                $("reviewFiles").value = "";
                updateFileInfo();
                pickedRating = 0;
                Array.prototype.forEach.call($("starPicker").querySelectorAll(".star-pick"), function (s) {
                    s.classList.remove("on");
                });
                showReviewNotice("Bạn đã đánh giá sản phẩm này. Cảm ơn bạn!");
                loadReviews();
                loadProduct(); // cập nhật điểm trung bình
            }).catch(function (err) {
                alertEl.textContent = err.message || "Không gửi được đánh giá.";
                alertEl.style.display = "block";
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<i class="fa fa-paper-plane"></i> Đăng đánh giá';
            });
        };

        if (!uploadsLeft) {
            sendReview();
            return;
        }
        // Tải từng tệp ảnh/video lên rồi gửi đánh giá
        Array.prototype.forEach.call(files, function (file) {
            var fd = new FormData();
            fd.append("file", file);
            fetch("/api/user/upload", {
                method: "POST",
                headers: { "Authorization": "Bearer " + getToken() },
                body: fd
            }).then(function (r) { return r.json(); }).then(function (data) {
                if (data.success === false) throw new Error(data.message);
                media.push({ url: data.data.url, type: data.data.type });
                uploadsLeft--;
                if (uploadsLeft === 0) sendReview();
            }).catch(function (err) {
                alert("Tải tệp thất bại: " + (err.message || file.name));
                uploadsLeft--;
                if (uploadsLeft === 0) sendReview();
            });
        });
    }

    function loadReviews() {
        fetch("/api/public/products/" + productId + "/reviews")
            .then(function (r) { return r.json(); })
            .then(function (data) {
                renderReviews(data.data || []);
            })
            .catch(function () { });
    }

    function renderReviews(list) {
        var container = $("reviewList");
        if (!list.length) {
            container.innerHTML = '<p class="text-muted">Chưa có đánh giá nào cho sản phẩm này.</p>';
            return;
        }
        container.innerHTML = list.map(function (rv) {
            var avatar = rv.userAvatar || "/img/icon/user.png";
            var mediaHtml = (rv.media || []).map(function (m) {
                if (m.type === "VIDEO") {
                    return '<video src="' + escapeHtml(m.url) + '" controls preload="metadata"></video>';
                }
                return '<img src="' + escapeHtml(m.url) + '" alt="minh họa">';
            }).join("");
            return '<div class="review-item d-flex gap-3">' +
                '<img src="' + escapeHtml(avatar) + '" class="review-avatar" alt="avatar">' +
                "<div style='flex:1;'>" +
                '<div><b>' + escapeHtml(rv.userName || "Người dùng") + "</b> " +
                '<span class="stars">' + starsHtml(rv.rating) + "</span></div>" +
                '<small class="text-muted">' + escapeHtml(rv.createdAt || "") + "</small>" +
                '<p class="mb-1">' + escapeHtml(rv.comment || "") + "</p>" +
                (mediaHtml ? '<div class="review-media">' + mediaHtml + "</div>" : "") +
                "</div></div>";
        }).join("");
    }
})();

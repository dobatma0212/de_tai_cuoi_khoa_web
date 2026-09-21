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
        updateCartBadge();
    });

    /** Hiển thị số sản phẩm trong giỏ lên icon giỏ hàng ở header */
    function updateCartBadge() {
        if (!isLoggedIn()) return;
        fetch("/api/user/cart/count", {
            headers: { "Authorization": "Bearer " + getToken() }
        }).then(function (r) { return r.json(); }).then(function (d) {
            var span = document.querySelector('a[href="./shopping-cart.html"] span');
            if (span && d.data !== undefined) span.textContent = d.data;
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
        $("pdPrice").textContent = formatVnd(p.price);
        $("pdStock").textContent = p.stock > 0 ? ("Còn " + p.stock + " sản phẩm") : "Hết hàng";
        $("pdDescription").textContent = p.description || "Chưa có mô tả.";
        $("pdRatingStars").innerHTML = starsHtml(p.avgRating || 0);
        $("pdRatingText").textContent = p.reviewCount > 0
            ? (p.avgRating + "/5 — " + p.reviewCount + " đánh giá")
            : "Chưa có đánh giá";

        // Chọn size
        var sizes = p.sizesList || [];
        if (sizes.length) {
            $("pdSizeSection").style.display = "block";
            $("pdSizes").innerHTML = sizes.map(function (s) {
                return '<button type="button" class="size-btn" data-size="' + escapeHtml(s) + '">' +
                    escapeHtml(s) + "</button>";
            }).join("");
            Array.prototype.forEach.call($("pdSizes").querySelectorAll(".size-btn"), function (btn) {
                btn.addEventListener("click", function () {
                    selectedSize = btn.getAttribute("data-size");
                    Array.prototype.forEach.call($("pdSizes").querySelectorAll(".size-btn"), function (b) {
                        b.classList.remove("active");
                    });
                    btn.classList.add("active");
                });
            });
            selectedSize = sizes[0];
            $("pdSizes").querySelector(".size-btn").classList.add("active");
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
        fetch("/api/user/cart", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + getToken()
            },
            body: JSON.stringify({
                productId: productId,
                size: selectedSize,
                color: selectedColor,
                quantity: qty
            })
        }).then(function (r) { return r.json(); }).then(function (data) {
            if (data.success === false) throw new Error(data.message);
            alert("Đã thêm vào giỏ hàng: " + product.name +
                (selectedSize ? " (size " + selectedSize + ")" : "") +
                (selectedColor ? " - màu " + selectedColor : "") + " x" + qty);
        }).catch(function (err) {
            alert(err.message || "Không thể thêm vào giỏ hàng.");
        });
    }

    // ============ Đánh giá ============

    function setupReviewForm() {
        if (!isLoggedIn()) {
            $("reviewLoginHint").style.display = "block";
            return;
        }
        $("reviewFormBox").style.display = "block";
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
    }

    function submitReview() {
        var alertEl = $("reviewAlert");
        alertEl.style.display = "none";
        if (!pickedRating) {
            alert("Vui lòng chọn số sao đánh giá.");
            return;
        }
        var files = $("reviewFiles").files;
        var uploadsLeft = files.length;
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
                pickedRating = 0;
                Array.prototype.forEach.call($("starPicker").querySelectorAll(".star-pick"), function (s) {
                    s.classList.remove("on");
                });
                loadReviews();
                loadProduct(); // cập nhật điểm trung bình
            }).catch(function (err) {
                alertEl.textContent = err.message || "Không gửi được đánh giá.";
                alertEl.style.display = "block";
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

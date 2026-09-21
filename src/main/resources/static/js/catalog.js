/**
 * catalog.js
 * Trang mua sắm toàn hãng (shop.html) — danh mục chung, không theo chi nhánh:
 * - Vào trang hiển thị các danh mục, ảnh đại diện = sản phẩm đại diện của danh mục
 * - Nhấp vào danh mục sẽ hiện sản phẩm của danh mục đó
 * - Bộ lọc: khoảng giá, size, màu sắc, từ khóa; sắp xếp, tìm kiếm, phân trang
 * - Sau khi đặt hàng, đơn sẽ được hệ thống phân phối về chi nhánh gần địa chỉ nhận nhất
 */
(function () {
    "use strict";

    function $(id) { return document.getElementById(id); }

    function getToken() {
        var s = window.ScAuth ? window.ScAuth.getSession() : null;
        return (s && s.token) || localStorage.getItem("jwt_token");
    }

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

    var state = {
        keyword: "",
        categoryId: 0,
        size: "",
        color: "",
        minPrice: null,
        maxPrice: null,
        sort: "",
        page: 0,
        pageSize: 12
    };

    var categories = [];

    document.addEventListener("DOMContentLoaded", function () {
        // Đọc từ khóa từ URL (nếu có, ví dụ từ ô tìm kiếm header)
        var params = new URLSearchParams(window.location.search);
        var kw = params.get("keyword");
        if (kw) {
            state.keyword = kw.trim();
        }
        loadCategories();
        bindEvents();
        loadProducts();
    });

    function bindEvents() {
        $("catalogSearchForm").addEventListener("submit", function (e) {
            e.preventDefault();
            state.keyword = $("catalogSearch").value.trim();
            state.page = 0;
            loadProducts();
        });

        $("catalogSort").addEventListener("change", function () {
            state.sort = this.value;
            state.page = 0;
            loadProducts();
        });

        $("catalogGrid").addEventListener("click", function (e) {
            var addBtn = e.target.closest(".catalog-add-cart");
            if (addBtn) {
                e.preventDefault();
                e.stopPropagation();
                addToCart(parseInt(addBtn.getAttribute("data-id"), 10));
                return;
            }
            var card = e.target.closest(".catalog-card");
            if (card) {
                window.location.href = "/product-detail.html?productId=" + card.getAttribute("data-id");
            }
        });

        $("catalogPagination").addEventListener("click", function (e) {
            var btn = e.target.closest("button.catalog-page-btn");
            if (!btn || btn.disabled) return;
            var p = parseInt(btn.getAttribute("data-page"), 10);
            if (isNaN(p) || p < 0) return;
            state.page = p;
            loadProducts();
            window.scrollTo({ top: 0, behavior: "smooth" });
        });

        // Bộ lọc khoảng giá
        $("catalogPriceRanges").addEventListener("click", function (e) {
            var a = e.target.closest("a[data-min]");
            if (!a) return;
            e.preventDefault();
            state.minPrice = a.getAttribute("data-min") ? Number(a.getAttribute("data-min")) : null;
            state.maxPrice = a.getAttribute("data-max") ? Number(a.getAttribute("data-max")) : null;
            state.page = 0;
            loadProducts();
        });

        // Bộ lọc size
        $("catalogSizes").addEventListener("change", function (e) {
            if (e.target.name !== "catalogSize") return;
            state.size = e.target.value;
            state.page = 0;
            loadProducts();
        });

        // Bộ lọc màu sắc
        $("catalogColors").addEventListener("click", function (e) {
            var a = e.target.closest("a[data-color]");
            if (!a) return;
            e.preventDefault();
            state.color = a.getAttribute("data-color");
            state.page = 0;
            loadProducts();
        });

        // Tag = từ khóa tìm kiếm nhanh
        $("catalogTags").addEventListener("click", function (e) {
            var a = e.target.closest("a[data-tag]");
            if (!a) return;
            e.preventDefault();
            state.keyword = a.getAttribute("data-tag");
            $("catalogSearch").value = state.keyword;
            state.page = 0;
            loadProducts();
        });
    }

    // ============ Danh mục ============

    function loadCategories() {
        fetch("/api/public/catalog/categories")
            .then(function (r) { return r.json(); })
            .then(function (data) {
                categories = data.data || [];
                renderCategoryCards();
            })
            .catch(function () { categories = []; });
    }

    /** Khi mới vào shop: hiển thị các danh mục, ảnh = sản phẩm đại diện của danh mục */
    function renderCategoryCards() {
        var grid = $("categoryCardGrid");
        var showingCards = state.categoryId === 0 && !state.keyword;
        grid.style.display = showingCards ? "" : "none";
        if (!showingCards) return;
        if (!categories.length) {
            grid.innerHTML = '<div class="col-12"><p class="text-muted">Chưa có danh mục nào.</p></div>';
            return;
        }
        grid.innerHTML = categories.map(function (c) {
            var img = c.imageUrl || "/img/product/product-1.jpg";
            return '<div class="col-lg-4 col-md-6 col-sm-6">' +
                '<div class="product__item catalog-cat-card" data-cat="' + c.id + '" style="cursor:pointer;">' +
                '<div class="product__item__pic set-bg" data-setbg="' + escapeHtml(img) + '"></div>' +
                '<div class="product__item__text">' +
                "<h6>" + escapeHtml(c.name) + "</h6>" +
                '<span class="text-muted" style="font-size:12px;">' + c.totalProducts + " sản phẩm</span>" +
                "</div></div></div>";
        }).join("");
        grid.querySelectorAll(".set-bg").forEach(function (el) {
            var bg = el.getAttribute("data-setbg");
            if (bg) el.style.backgroundImage = "url(" + bg + ")";
        });
        grid.onclick = function (e) {
            var card = e.target.closest(".catalog-cat-card");
            if (!card) return;
            state.categoryId = parseInt(card.getAttribute("data-cat"), 10);
            state.page = 0;
            renderCategoryCards();
            loadProducts();
        };
    }

    // ============ Sản phẩm ============

    function loadProducts() {
        var params = new URLSearchParams();
        if (state.keyword) params.append("keyword", state.keyword);
        if (state.categoryId) params.append("categoryId", state.categoryId);
        if (state.size) params.append("size", state.size);
        if (state.color) params.append("color", state.color);
        if (state.minPrice != null) params.append("minPrice", state.minPrice);
        if (state.maxPrice != null) params.append("maxPrice", state.maxPrice);
        if (state.sort) params.append("sort", state.sort);
        params.append("page", state.page);
        params.append("pageSize", state.pageSize);

        var showingCards = state.categoryId === 0 && !state.keyword;
        var title = $("catalogCategoryTitle");
        if (showingCards) {
            title.style.display = "none";
            $("catalogGrid").innerHTML = "";
            $("catalogCount").textContent = "Khám phá theo danh mục";
            renderCategoryCards();
            return;
        }
        // Tiêu đề danh mục đang xem + nút quay lại
        var cat = categories.find(function (c) { return c.id === state.categoryId; });
        title.style.display = "";
        title.innerHTML = state.categoryId
            ? escapeHtml(cat ? cat.name : "Sản phẩm") +
              ' <button type="button" class="btn btn-link btn-sm" id="catalogBackAll" style="padding:0 8px;">← Tất cả danh mục</button>'
            : (state.keyword ? 'Kết quả cho "' + escapeHtml(state.keyword) + '"' : "Tất cả sản phẩm");
        var back = $("catalogBackAll");
        if (back) back.onclick = function () {
            state.categoryId = 0;
            state.keyword = "";
            $("catalogSearch").value = "";
            state.page = 0;
            loadProducts();
        };

        $("catalogGrid").innerHTML = '<div class="col-12"><p class="text-muted">Đang tải sản phẩm...</p></div>';
        fetch("/api/public/catalog/products?" + params.toString())
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (data.success === false) throw new Error(data.message || "Lỗi tải sản phẩm");
                renderProducts(data.data || {});
            })
            .catch(function (err) {
                $("catalogGrid").innerHTML = '<div class="col-12"><p class="text-danger">' +
                    escapeHtml(err.message || "Không tải được sản phẩm.") + "</p></div>";
            });
    }

    function renderProducts(pageData) {
        var list = pageData.content || [];
        var total = pageData.totalElements || 0;
        var from = total === 0 ? 0 : (pageData.page * pageData.pageSize) + 1;
        var to = Math.min((pageData.page + 1) * pageData.pageSize, total);
        $("catalogCount").textContent = "Hiển thị " + from + "–" + to + " trên " + total + " sản phẩm";

        if (!list.length) {
            $("catalogGrid").innerHTML =
                '<div class="col-12"><p class="text-muted">Không có sản phẩm nào phù hợp.</p></div>';
            $("catalogPaginationRow").style.display = "none";
            return;
        }

        $("catalogGrid").innerHTML = list.map(function (p) {
            var inStock = (p.stock || 0) > 0;
            return '<div class="col-lg-4 col-md-6 col-sm-6">' +
                '<div class="product__item catalog-card" data-id="' + p.id + '" style="cursor:pointer;">' +
                '<div class="product__item__pic set-bg" data-setbg="' +
                escapeHtml(p.imageUrl || "/img/product/product-1.jpg") + '">' +
                (!inStock ? '<span class="label">Hết hàng</span>' : "") +
                "</div>" +
                '<div class="product__item__text">' +
                "<h6>" + escapeHtml(p.name) + "</h6>" +
                (inStock
                    ? '<a href="#" class="add-cart catalog-add-cart" data-id="' + p.id + '">+ Thêm vào giỏ</a>'
                    : '<span class="text-muted" style="font-size:12px;">Tạm hết hàng</span>') +
                '<div class="rating"><h5>' + formatVnd(p.price) + "</h5></div>" +
                '<small class="text-muted d-block">' + escapeHtml(p.categoryName || "") +
                (p.color ? " — " + escapeHtml(p.color) : "") +
                (inStock ? " — Còn " + p.stock : "") + "</small>" +
                "</div></div></div>";
        }).join("");

        // set-bg dùng data-setbg
        document.querySelectorAll("#catalogGrid .set-bg").forEach(function (el) {
            var bg = el.getAttribute("data-setbg");
            if (bg) el.style.backgroundImage = "url(" + bg + ")";
        });

        renderPagination(pageData);
    }

    function renderPagination(pageData) {
        var total = pageData.totalPages || 0;
        var current = pageData.page || 0;
        var container = $("catalogPagination");
        var row = $("catalogPaginationRow");
        if (!container || !row) return;
        if (total <= 1) {
            row.style.display = "none";
            container.innerHTML = "";
            return;
        }
        row.style.display = "";
        var pages = [];
        for (var i = 0; i < total; i++) {
            if (i === 0 || i === total - 1 || (i >= current - 1 && i <= current + 1)) {
                pages.push(i);
            }
        }
        var html = "";
        var last = -1;
        pages.forEach(function (p) {
            if (last !== -1 && p - last > 1) {
                html += "<span>...</span>";
            }
            html += '<button type="button" class="catalog-page-btn' + (p === current ? " active" : "") + '"' +
                ' data-page="' + p + '"' + (p === current ? " disabled" : "") + ">" + (p + 1) + "</button>";
            last = p;
        });
        container.innerHTML = html;
    }

    // ============ Thêm vào giỏ hàng ============

    function addToCart(productId) {
        var token = getToken();
        if (!token) {
            window.location.href = "/login.html";
            return;
        }
        fetch("/api/user/cart", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + token
            },
            body: JSON.stringify({ productId: productId, quantity: 1 })
        }).then(function (r) { return r.json(); }).then(function (data) {
            if (data.success === false) throw new Error(data.message || "Không thể thêm vào giỏ hàng.");
            alert("Đã thêm sản phẩm vào giỏ hàng!");
        }).catch(function (err) {
            alert(err.message);
        });
    }
})();

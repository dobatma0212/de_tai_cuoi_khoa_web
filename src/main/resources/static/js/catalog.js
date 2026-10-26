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
        updateCartBadge();
        // Đọc từ khóa từ URL (nếu có, ví dụ từ ô tìm kiếm header)
        var params = new URLSearchParams(window.location.search);
        var kw = params.get("keyword");
        if (kw) {
            state.keyword = kw.trim();
            $("catalogSearch").value = state.keyword;
        }
        loadCategories();
        bindEvents();
        // Nạp danh sách đã thích trước để tô đúng màu trái tim ở lần hiển thị đầu
        ScShop.loadFavIds().then(loadProducts);
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

        // Tim yêu thích, thêm giỏ, mở chi tiết: dùng chung với trang chủ
        ScShop.bindCards($("catalogGrid"));

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

        var resetButton = $("catalogResetFilters");
        if (resetButton) resetButton.addEventListener("click", resetFilters);

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

        // Chỉ hiển thị thẻ danh mục khi chưa có bất kỳ bộ lọc nào được áp dụng.
        // Trước đây điều kiện chỉ kiểm tra keyword/category nên lọc giá, size, màu
        // bị bỏ qua khi người dùng vừa mở trang shop.
        var hasFilters = !!(state.keyword || state.categoryId || state.size || state.color
            || state.minPrice != null || state.maxPrice != null);
        var showingCards = !hasFilters;
        var title = $("catalogCategoryTitle");
        if (showingCards) {
            $("categoryCardGrid").style.display = "";
            title.style.display = "none";
            $("catalogGrid").innerHTML = "";
            $("catalogCount").textContent = "Khám phá theo danh mục";
            $("catalogPaginationRow").style.display = "none";
            renderCategoryCards();
            updateFilterUi();
            return;
        }
        $("categoryCardGrid").style.display = "none";
        updateFilterUi();
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

    function updateFilterUi() {
        var reset = $("catalogResetFilters");
        if (reset) {
            var active = !!(state.keyword || state.categoryId || state.size || state.color
                || state.minPrice != null || state.maxPrice != null);
            reset.disabled = !active;
            reset.classList.toggle("is-active", active);
        }
        var priceLinks = $("catalogPriceRanges");
        if (priceLinks) {
            priceLinks.querySelectorAll("a[data-min]").forEach(function (a) {
                var min = a.getAttribute("data-min") === "" ? null : Number(a.getAttribute("data-min"));
                var max = a.getAttribute("data-max") === "" ? null : Number(a.getAttribute("data-max"));
                var selected = min === state.minPrice && max === state.maxPrice;
                a.classList.toggle("is-selected", selected);
                a.setAttribute("aria-current", selected ? "true" : "false");
            });
        }
        var colors = $("catalogColors");
        if (colors) colors.querySelectorAll("a[data-color]").forEach(function (a) {
            var selected = (a.getAttribute("data-color") || "") === state.color;
            a.classList.toggle("is-selected", selected);
            a.setAttribute("aria-pressed", selected ? "true" : "false");
        });
        var tags = $("catalogTags");
        if (tags) tags.querySelectorAll("a[data-tag]").forEach(function (a) {
            var selected = (a.getAttribute("data-tag") || "") === state.keyword;
            a.classList.toggle("is-selected", selected);
            a.setAttribute("aria-pressed", selected ? "true" : "false");
        });
    }

    function resetFilters() {
        state.keyword = "";
        state.categoryId = 0;
        state.size = "";
        state.color = "";
        state.minPrice = null;
        state.maxPrice = null;
        state.page = 0;
        $("catalogSearch").value = "";
        var allSize = document.querySelector('input[name="catalogSize"][value=""]');
        if (allSize) allSize.checked = true;
        loadProducts();
    }

    function renderProducts(pageData) {
        var list = pageData.content || [];
        var total = pageData.totalElements || 0;
        var from = total === 0 ? 0 : (pageData.page * pageData.pageSize) + 1;
        var to = Math.min((pageData.page + 1) * pageData.pageSize, total);
        $("catalogCount").textContent = "Hiển thị " + from + "–" + to + " trên " + total + " sản phẩm";
        var meta = $("catalogPaginationMeta");
        if (meta) meta.textContent = total > 0
            ? "Trang " + ((pageData.page || 0) + 1) + " / " + (pageData.totalPages || 1) + " · " + total + " sản phẩm phù hợp"
            : "Không có sản phẩm phù hợp với bộ lọc hiện tại";

        if (!list.length) {
            $("catalogGrid").innerHTML =
                '<div class="col-12"><p class="text-muted">Không có sản phẩm nào phù hợp.</p></div>';
            $("catalogPaginationRow").style.display = "none";
            $("catalogPagination").innerHTML = "";
            return;
        }

        $("catalogGrid").innerHTML = list.map(function (p) {
            return ScShop.cardHtml(p, "col-lg-4 col-md-6 col-sm-6");
        }).join("");
        ScShop.applyBackgrounds($("catalogGrid"));

        renderPagination(pageData);
    }

    function renderPagination(pageData) {
        var total = Number(pageData.totalPages || 0);
        var current = Number(pageData.page || 0);
        var container = $("catalogPagination");
        var row = $("catalogPaginationRow");
        if (!container || !row) return;
        if (total <= 1) { row.style.display = "none"; container.innerHTML = ""; return; }
        row.style.display = "";
        var pages = [];
        for (var i = 0; i < total; i++) {
            if (i === 0 || i === total - 1 || (i >= current - 2 && i <= current + 2)) pages.push(i);
        }
        var html = '<button type="button" class="catalog-page-btn catalog-page-nav" data-page="' + (current - 1) + '"'
            + (current <= 0 ? ' disabled aria-disabled="true"' : '') + ' aria-label="Trang trước">‹ <span>Trước</span></button>';
        var last = -1;
        pages.forEach(function (p) {
            if (last !== -1 && p - last > 1) html += '<span class="catalog-page-ellipsis" aria-hidden="true">…</span>';
            html += '<button type="button" class="catalog-page-btn' + (p === current ? ' active' : '') + '" data-page="' + p + '"'
                + (p === current ? ' disabled aria-current="page"' : '') + '>' + (p + 1) + '</button>';
            last = p;
        });
        html += '<button type="button" class="catalog-page-btn catalog-page-nav" data-page="' + (current + 1) + '"'
            + (current >= total - 1 ? ' disabled aria-disabled="true"' : '') + ' aria-label="Trang sau"><span>Sau</span> ›</button>';
        container.innerHTML = html;
    }

    function updateCartBadge() {
        if (!getToken() || !window.StoreChainCartAPI) return;
        window.StoreChainCartAPI.count().then(function (count) {
            var spans = document.querySelectorAll('a[href="./shopping-cart.html"] span, a[href="/shopping-cart.html"] span');
            Array.prototype.forEach.call(spans, function (span) { span.textContent = count == null ? 0 : count; });
        }).catch(function () {});
    }

    // ============ Thêm vào giỏ hàng ============

    function addToCart(productId) {
        var token = getToken();
        if (!token) {
            window.location.href = "/login.html";
            return;
        }
        if (!window.StoreChainCartAPI) {
            alert("Module giỏ hàng chưa được tải. Vui lòng tải lại trang.");
            return;
        }
        window.StoreChainCartAPI.add(productId, 1, "", "").then(function () {
            alert("Đã thêm sản phẩm vào giỏ hàng!");
            updateCartBadge();
        }).catch(function (err) {
            alert(err.message || "Không thể thêm vào giỏ hàng.");
        });
    }
})();

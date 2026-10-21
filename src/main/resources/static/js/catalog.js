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
        if (SORT_LABELS[params.get("sort") || ""] !== undefined) {
            state.sort = params.get("sort") || "";
        }
        var catParam = parseInt(params.get("categoryId"), 10);
        if (catParam > 0) state.categoryId = catParam;
        paintSortTabs();
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

        // Tầng 2: chọn kiểu sắp xếp
        $("sortTabs").addEventListener("click", function (e) {
            var tab = e.target.closest(".sort-tab");
            if (!tab) return;
            state.sort = tab.getAttribute("data-sort") || "";
            state.page = 0;
            paintSortTabs();
            loadProducts();
        });

        // Tầng 1: chọn danh mục
        $("categoryBar").addEventListener("click", function (e) {
            var item = e.target.closest(".cat-item");
            if (!item) return;
            state.categoryId = parseInt(item.getAttribute("data-cat"), 10) || 0;
            state.page = 0;
            renderCategoryBar();
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
                renderCategoryBar();
            })
            .catch(function () { categories = []; renderCategoryBar(); });
    }

    /** Dải danh mục luôn hiển thị; mục "Tất cả" đặt đầu, ảnh = sản phẩm đại diện của danh mục */
    function renderCategoryBar() {
        var bar = $("categoryBar");
        var total = categories.reduce(function (sum, x) { return sum + (x.totalProducts || 0); }, 0);
        var items = [{ id: 0, name: "Tất cả", imageUrl: "", totalProducts: total }].concat(categories);
        bar.innerHTML = items.map(function (cat) {
            var active = cat.id === state.categoryId;
            var img = cat.imageUrl ? ' style="background-image:url(\'' + escapeHtml(cat.imageUrl) + '\')"' : "";
            return '<div class="cat-item' + (active ? " active" : "") + '" role="tab" aria-selected="' + active +
                '" data-cat="' + cat.id + '">' +
                '<div class="cat-item__img"' + img + "></div>" +
                "<div><div class=\"cat-item__name\">" + escapeHtml(cat.name) + "</div>" +
                '<div class="cat-item__count">' + (cat.totalProducts || 0) + " sản phẩm</div></div></div>";
        }).join("");
    }

    var SORT_LABELS = {
        "": "Mới nhất", bestseller: "Bán chạy", rating: "Đánh giá cao", favorite: "Yêu thích nhất",
        priceAsc: "Giá thấp → cao", priceDesc: "Giá cao → thấp", name: "Tên A → Z"
    };

    function paintSortTabs() {
        Array.prototype.forEach.call(document.querySelectorAll("#sortTabs .sort-tab"), function (t) {
            var on = (t.getAttribute("data-sort") || "") === state.sort;
            t.classList.toggle("active", on);
            t.setAttribute("aria-selected", on);
        });
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

        // Tiêu đề: danh mục đang xem + kiểu sắp xếp, kèm nút xóa từ khóa tìm kiếm nếu có
        var cat = categories.find(function (x) { return x.id === state.categoryId; });
        var base = state.categoryId ? (cat ? cat.name : "Sản phẩm") : "Tất cả sản phẩm";
        var title = $("catalogCategoryTitle");
        title.innerHTML = escapeHtml(base) + ' <small class="text-muted font-weight-normal">· ' +
            escapeHtml(SORT_LABELS[state.sort] || "Mới nhất") + "</small>" +
            (state.keyword ? ' <span class="badge badge-light">"' + escapeHtml(state.keyword) +
                '" <a href="#" id="catalogClearKeyword">✕</a></span>' : "");
        var clear = $("catalogClearKeyword");
        if (clear) clear.onclick = function (e) {
            e.preventDefault();
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
            return ScShop.cardHtml(p, "col-lg-4 col-md-6 col-sm-6");
        }).join("");
        ScShop.applyBackgrounds($("catalogGrid"));

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

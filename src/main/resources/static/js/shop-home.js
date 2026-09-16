/**
 * shop-home.js
 * Trang chủ chi nhánh (shop-home.html?shopId=...):
 * - Hiển thị thông tin chi nhánh, danh mục + sản phẩm đại diện
 * - Thanh tìm kiếm: gợi ý sản phẩm khi gõ, lưu & hiển thị lịch sử tìm kiếm
 *   (đã đăng nhập: lưu ở server; khách: lưu ở localStorage)
 */
(function () {
    "use strict";

    function $(id) {
        return document.getElementById(id);
    }

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

    var shopId = null;
    var suggestTimer = null;
    var HISTORY_KEY_PREFIX = "shop_search_history_";
    var MAX_LOCAL_HISTORY = 10;

    // Trạng thái bộ lọc + phân trang của khu danh sách sản phẩm
    var resultState = { keyword: "", categoryId: 0, minPrice: null, maxPrice: null, size: "", sort: "", page: 0 };
    var shopCategories = [];
    var shopSizes = [];

    document.addEventListener("DOMContentLoaded", function () {
        var params = new URLSearchParams(window.location.search);
        shopId = parseInt(params.get("shopId"), 10);
        if (!shopId || isNaN(shopId)) {
            $("shopLoadError").style.display = "block";
            $("shopLoadError").textContent = "Không xác định được chi nhánh. Vui lòng truy cập từ trang quản lý chi nhánh.";
            return;
        }
        loadShopHome();
        loadHistory();

        $("searchBtn").addEventListener("click", function () {
            doSearch($("searchInput").value.trim());
        });
        $("searchInput").addEventListener("keydown", function (e) {
            if (e.key === "Enter") {
                e.preventDefault();
                doSearch($("searchInput").value.trim());
            }
        });
        $("searchInput").addEventListener("input", function () {
            clearTimeout(suggestTimer);
            var kw = $("searchInput").value.trim();
            if (!kw) {
                hideSuggest();
                return;
            }
            suggestTimer = setTimeout(function () { fetchSuggest(kw); }, 300);
        });
        document.addEventListener("click", function (e) {
            if (!e.target.closest(".shop-search-wrap")) hideSuggest();
        });
        $("clearSearchBtn").addEventListener("click", backToHome);
        $("clearHistoryBtn").addEventListener("click", clearHistory);
        $("sortSelect").addEventListener("change", function () {
            resultState.sort = this.value;
            resultState.page = 0;
            reloadCurrentResults();
        });
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

    // ============ Trang chủ ============

    function loadShopHome() {
        fetch("/api/public/shops/" + shopId + "/home")
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (!data.success && data.success !== undefined) throw new Error(data.message);
                var home = data.data;
                renderHome(home);
            })
            .catch(function (err) {
                $("shopLoadError").style.display = "block";
                $("shopLoadError").textContent = err.message || "Không tải được trang chủ chi nhánh.";
            });
    }

    function renderHome(home) {
        var shop = home.shop || {};
        document.title = shop.name + " | Male-Fashion";
        $("breadcrumbShopName").textContent = shop.name || "Trang chủ chi nhánh";
        $("shopName").textContent = shop.name || "";
        $("shopAddress").textContent = shop.address || "—";
        $("shopPhone").textContent = shop.phone || "—";
        $("shopDescription").textContent = shop.description || "";
        // Banner hiển thị ảnh đại diện (logo) của chi nhánh để dễ phân biệt
        var bannerSrc = shop.logoUrl || shop.bannerUrl || "/img/banner/banner-1.jpg";
        $("shopBanner").src = bannerSrc;
        $("shopHero").style.display = "";

        var cards = (home.categories || []).map(function (block) {
            var p = block.representativeProduct;
            var img = p ? (p.imageUrl || "/img/product/product-1.jpg") : "/img/product/product-1.jpg";
            return '<div class="col-lg-3 col-md-4 col-sm-6 mb-3">' +
                '<div class="category-card" data-cat="' + block.categoryId + '" data-name="' + escapeHtml(block.label) + '">' +
                '<img src="' + escapeHtml(img) + '" alt="' + escapeHtml(block.label) + '">' +
                (p ? '<span class="badge-rep">Đại diện</span>' : "") +
                '<div class="body">' +
                '<h6 class="font-weight-bold mb-1">' + escapeHtml(block.label) + "</h6>" +
                '<small class="text-muted"><i class="fa fa-cube"></i> ' + block.totalProducts + " sản phẩm</small>" +
                '<span class="view-all">Xem tất cả <i class="fa fa-arrow-right"></i></span>' +
                "</div></div></div>";
        }).join("");

        $("homeSections").innerHTML =
            '<div class="category-block">' +
            '<div class="d-flex justify-content-between align-items-center mb-3">' +
            '<h5 class="font-weight-bold mb-0">Danh mục sản phẩm</h5>' +
            '<small class="text-muted">Bấm vào danh mục để xem tất cả sản phẩm</small>' +
            "</div>" +
            '<div class="row">' + cards + "</div></div>";

        shopCategories = home.categories || [];
        Array.prototype.forEach.call(
            document.querySelectorAll("#homeSections .category-card"), function (el) {
                el.addEventListener("click", function () {
                    openCategory(el.getAttribute("data-cat"), el.getAttribute("data-name"));
                });
            });
        loadShopSizes();
    }

    // Nạp toàn bộ size có trong chi nhánh để làm bộ lọc (trả Promise để dùng trước khi render)
    function loadShopSizes() {
        if (shopSizes.length) return Promise.resolve();
        return fetch("/api/public/shops/" + shopId + "/products?pageSize=100")
            .then(function (r) { return r.json(); })
            .then(function (data) {
                var set = {};
                ((data.data && data.data.content) || []).forEach(function (p) {
                    (p.sizesList || []).forEach(function (s) { set[s] = true; });
                });
                shopSizes = Object.keys(set);
            }).catch(function () { });
    }

    // ============ Xem sản phẩm có bộ lọc + phân trang ============

    function openCategory(categoryId, name) {
        resultState.categoryId = parseInt(categoryId, 10) || 0;
        resultState.keyword = "";
        resultState.page = 0;
        loadResults("Danh mục: " + name);
    }

    function doSearch(keyword) {
        hideSuggest();
        if (!keyword) return;
        $("searchInput").value = keyword;
        saveHistory(keyword);
        resultState.keyword = keyword;
        resultState.categoryId = 0;
        resultState.page = 0;
        loadResults('Kết quả tìm kiếm cho "' + keyword + '"');
        loadHistory();
    }

    /** Gọi API phân trang + render toàn bộ khu kết quả (sidebar, lưới, phân trang) */
    function loadResults(title) {
        $("homeSections").style.display = "none";
        $("searchResultSection").style.display = "block";
        $("resultsTitle").textContent = title;

        var params = new URLSearchParams();
        params.append("page", resultState.page);
        params.append("pageSize", 10);
        if (resultState.keyword) params.append("keyword", resultState.keyword);
        if (resultState.categoryId) params.append("categoryId", resultState.categoryId);
        if (resultState.minPrice !== null) params.append("minPrice", resultState.minPrice);
        if (resultState.maxPrice !== null) params.append("maxPrice", resultState.maxPrice);
        if (resultState.size) params.append("size", resultState.size);
        if (resultState.sort) params.append("sort", resultState.sort);

        fetch("/api/public/shops/" + shopId + "/products?" + params.toString())
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (data.success === false) throw new Error(data.message);
                // Đợi danh sách size sẵn sàng rồi mới vẽ sidebar bộ lọc
                return loadShopSizes().then(function () { return data; });
            })
            .then(function (data) {
                renderFilterSidebar();
                renderProductGrid(data.data || {});
                renderPagination(data.data || {});
            })
            .catch(function (err) {
                $("searchResults").innerHTML =
                    '<div class="col-12"><p class="text-muted">' + escapeHtml(err.message || "Không tải được sản phẩm.") + "</p></div>";
            });
    }

    function renderFilterSidebar() {
        var catRows = '<span class="filter-cat' + (resultState.categoryId === 0 ? " active" : "") +
            '" data-filter-cat="0">Tất cả sản phẩm</span>';
        catRows += (shopCategories || []).map(function (c) {
            return '<span class="filter-cat' + (resultState.categoryId === c.categoryId ? " active" : "") +
                '" data-filter-cat="' + c.categoryId + '">' + escapeHtml(c.label) + " (" + c.totalProducts + ")</span>";
        }).join("");

        var sizeChips = shopSizes.length
            ? shopSizes.map(function (s) {
                return '<span class="size-chip' + (resultState.size === s ? " active" : "") +
                    '" data-filter-size="' + escapeHtml(s) + '">' + escapeHtml(s) + "</span>";
            }).join("")
            : '<small class="text-muted">Chưa có dữ liệu size</small>';

        $("filterSidebar").innerHTML =
            '<div class="filter-group">' +
            '<button type="button" class="filter-title">DANH MỤC <i class="fa fa-chevron-down"></i></button>' +
            '<div class="filter-content">' + catRows + "</div></div>" +
            '<div class="filter-group">' +
            '<button type="button" class="filter-title">KHOẢNG GIÁ <i class="fa fa-chevron-down"></i></button>' +
            '<div class="filter-content price-inputs">' +
            '<div class="d-flex align-items-center mb-2">' +
            '<input type="number" id="filterMinPrice" placeholder="Từ (VND)" min="0" value="' +
            (resultState.minPrice === null ? "" : resultState.minPrice) + '">' +
            '<span class="mx-1">—</span>' +
            '<input type="number" id="filterMaxPrice" placeholder="Đến (VND)" min="0" value="' +
            (resultState.maxPrice === null ? "" : resultState.maxPrice) + '">' +
            "</div>" +
            '<button type="button" id="btnApplyPrice" class="apply-filter-btn">Áp dụng</button>' +
            "</div></div>" +
            '<div class="filter-group">' +
            '<button type="button" class="filter-title">SIZE <i class="fa fa-chevron-down"></i></button>' +
            '<div class="filter-content">' + sizeChips + "</div></div>";

        // Gắn MỘT handler delegation trên container — không phụ thuộc re-render
        var sidebar = $("filterSidebar");
        sidebar.onclick = function (e) {
            // Thu gọn/mở rộng nhóm
            var title = e.target.closest(".filter-title");
            if (title) {
                var content = title.nextElementSibling;
                if (content) content.classList.toggle("collapsed");
                var icon = title.querySelector("i");
                if (icon) icon.className = icon.className.indexOf("up") === -1
                    ? "fa fa-chevron-up" : "fa fa-chevron-down";
                return;
            }
            // Chọn danh mục
            var cat = e.target.closest(".filter-cat");
            if (cat) {
                resultState.categoryId = parseInt(cat.getAttribute("data-filter-cat"), 10) || 0;
                resultState.page = 0;
                var name = resultState.categoryId
                    ? "Danh mục: " + cat.textContent.replace(/\s*\(\d+\)$/, "")
                    : (resultState.keyword ? 'Kết quả tìm kiếm cho "' + resultState.keyword + '"' : "Tất cả sản phẩm");
                loadResults(name);
                return;
            }
            // Chọn size
            var chip = e.target.closest(".size-chip");
            if (chip) {
                var s = chip.getAttribute("data-filter-size");
                resultState.size = (resultState.size === s) ? "" : s;
                resultState.page = 0;
                reloadCurrentResults();
                return;
            }
            // Áp dụng khoảng giá
            if (e.target.closest("#btnApplyPrice")) {
                var min = $("filterMinPrice").value;
                var max = $("filterMaxPrice").value;
                resultState.minPrice = min === "" ? null : Number(min);
                resultState.maxPrice = max === "" ? null : Number(max);
                resultState.page = 0;
                reloadCurrentResults();
            }
        };
    }

    /** Load lại với tiêu đề hiện tại (khi đổi size/giá/trang) */
    function reloadCurrentResults() {
        loadResults($("resultsTitle").textContent);
    }

    function renderProductGrid(pageData) {
        var list = pageData.content || [];
        $("searchResultCount").textContent = pageData.totalElements + " sản phẩm";
        if (!list.length) {
            $("searchResults").innerHTML =
                '<div class="col-12"><p class="text-muted">Không có sản phẩm nào phù hợp bộ lọc.</p></div>';
            return;
        }
        $("searchResults").innerHTML = list.map(function (p) {
            var stars = '<div class="product-stars">' + starsRow(p.avgRating || 0) + "</div>";
            return '<div class="col-lg-4 col-md-6 col-sm-6 mb-4">' +
                '<a href="/product-detail.html?productId=' + p.id + '" style="text-decoration:none;color:inherit;">' +
                '<div class="search-result-item">' +
                '<img src="' + escapeHtml(p.imageUrl || "/img/product/product-1.jpg") + '" alt="' + escapeHtml(p.name) + '">' +
                '<div class="body">' +
                '<h6 class="font-weight-bold mb-1">' + escapeHtml(p.name) + "</h6>" +
                stars +
                '<div class="price-text mt-1">' + formatVnd(p.price) + "</div>" +
                '<small class="text-muted d-block mt-1">' + escapeHtml(p.categoryName || "") +
                (p.stock > 0 ? " — Còn " + p.stock : " — Hết hàng") + "</small>" +
                "</div></div></a></div>";
        }).join("");
    }

    function starsRow(rating) {
        var html = "";
        for (var i = 1; i <= 5; i++) {
            html += '<i class="fa fa-star' + (i <= Math.round(rating) ? "" : " off") + '"></i>';
        }
        return html;
    }

    function renderPagination(pageData) {
        var total = pageData.totalPages || 0;
        var current = pageData.page || 0;
        var container = $("pagination");
        if (total <= 1) {
            container.innerHTML = "";
            return;
        }
        // Cửa sổ trang: 1 … x-1 x x+1 … N
        var pages = [];
        for (var i = 0; i < total; i++) {
            if (i === 0 || i === total - 1 || Math.abs(i - current) <= 1) pages.push(i);
        }
        var html = '<button type="button" class="page-btn" data-page="' + (current - 1) + '"' +
            (current === 0 ? " disabled" : "") + '><i class="fa fa-angle-left"></i></button>';
        var last = -1;
        pages.forEach(function (p) {
            if (last !== -1 && p - last > 1) {
                html += '<span class="page-btn" style="border:none;background:none;cursor:default;">…</span>';
            }
            html += '<button type="button" class="page-btn' + (p === current ? " current" : "") +
                '" data-page="' + p + '">' + (p + 1) + "</button>";
            last = p;
        });
        html += '<button type="button" class="page-btn" data-page="' + (current + 1) + '"' +
            (current >= total - 1 ? " disabled" : "") + '><i class="fa fa-angle-right"></i></button>';
        container.innerHTML = html;

        container.onclick = function (e) {
            var btn = e.target.closest("button.page-btn");
            if (!btn || btn.disabled) return;
            var p = parseInt(btn.getAttribute("data-page"), 10);
            if (isNaN(p) || p < 0) return;
            resultState.page = p;
            reloadCurrentResults();
            $("searchResultSection").scrollIntoView({ behavior: "smooth" });
        };
    }

    // ============ Gợi ý sản phẩm ============

    function fetchSuggest(keyword) {
        fetch("/api/public/shops/" + shopId + "/suggest?keyword=" + encodeURIComponent(keyword))
            .then(function (r) { return r.json(); })
            .then(function (data) {
                var list = (data.data || []).filter(function (p) {
                    return String(p.status) === "active";
                });
                renderSuggest(list);
            })
            .catch(function () { hideSuggest(); });
    }

    function renderSuggest(list) {
        var box = $("suggestBox");
        if (!list.length) {
            box.innerHTML = '<div class="suggest-item text-muted">Không tìm thấy gợi ý nào</div>';
            box.style.display = "block";
            return;
        }
        box.innerHTML = list.map(function (p) {
            return '<div class="suggest-item" data-keyword="' + escapeHtml(p.name) + '">' +
                '<img src="' + escapeHtml(p.imageUrl || "/img/product/product-1.jpg") + '" alt="">' +
                "<div><div class='font-weight-bold'>" + escapeHtml(p.name) + "</div>" +
                "<small class='price-text'>" + formatVnd(p.price) + "</small></div></div>";
        }).join("");
        box.style.display = "block";
        Array.prototype.forEach.call(box.querySelectorAll(".suggest-item"), function (item) {
            item.addEventListener("click", function () {
                var kw = item.getAttribute("data-keyword");
                $("searchInput").value = kw;
                hideSuggest();
                doSearch(kw);
            });
        });
    }

    function hideSuggest() {
        $("suggestBox").style.display = "none";
    }

    // ============ Tìm kiếm + lịch sử ============

    function backToHome() {
        $("searchResultSection").style.display = "none";
        $("homeSections").style.display = "block";
        $("searchInput").value = "";
        resultState.keyword = "";
        resultState.categoryId = 0;
        resultState.page = 0;
    }

    // ---- Lịch sử tìm kiếm ----

    function historyKey() {
        return HISTORY_KEY_PREFIX + shopId;
    }

    function isLoggedIn() {
        return !!getToken();
    }

    function loadHistory() {
        if (isLoggedIn()) {
            fetch("/api/user/search-history?shopId=" + shopId, {
                headers: { "Authorization": "Bearer " + getToken() }
            }).then(function (r) { return r.json(); }).then(function (data) {
                var list = (data.data || []).map(function (h) { return h.keyword; });
                renderHistory(list);
            }).catch(function () { renderHistory([]); });
        } else {
            try {
                renderHistory(JSON.parse(localStorage.getItem(historyKey()) || "[]"));
            } catch (e) {
                renderHistory([]);
            }
        }
    }

    function saveHistory(keyword) {
        if (isLoggedIn()) {
            fetch("/api/user/search-history", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": "Bearer " + getToken()
                },
                body: JSON.stringify({ keyword: keyword, shopId: shopId })
            }).catch(function () { });
        } else {
            var list = [];
            try {
                list = JSON.parse(localStorage.getItem(historyKey()) || "[]");
            } catch (e) { }
            list = list.filter(function (k) {
                return k.toLowerCase() !== keyword.toLowerCase();
            });
            list.unshift(keyword);
            if (list.length > MAX_LOCAL_HISTORY) list = list.slice(0, MAX_LOCAL_HISTORY);
            localStorage.setItem(historyKey(), JSON.stringify(list));
        }
    }

    function renderHistory(keywords) {
        if (!keywords || !keywords.length) {
            $("historySection").style.display = "none";
            return;
        }
        $("historySection").style.display = "block";
        $("historyChips").innerHTML = keywords.map(function (k) {
            return '<span class="history-chip" data-keyword="' + escapeHtml(k) + '">' +
                '<i class="fa fa-history"></i> ' + escapeHtml(k) +
                ' <i class="fa fa-times" data-remove="' + escapeHtml(k) + '"></i></span>';
        }).join("");
        Array.prototype.forEach.call($("historyChips").querySelectorAll(".history-chip"), function (chip) {
            chip.addEventListener("click", function (e) {
                var kw = chip.getAttribute("data-keyword");
                if (e.target.getAttribute("data-remove") !== null) {
                    removeHistory(kw);
                    return;
                }
                doSearch(kw);
            });
        });
    }

    function removeHistory(keyword) {
        if (isLoggedIn()) {
            // Tìm id từ server rồi xóa
            fetch("/api/user/search-history?shopId=" + shopId, {
                headers: { "Authorization": "Bearer " + getToken() }
            }).then(function (r) { return r.json(); }).then(function (data) {
                var hit = (data.data || []).find(function (h) {
                    return h.keyword.toLowerCase() === keyword.toLowerCase();
                });
                if (hit) {
                    return fetch("/api/user/search-history/" + hit.id, {
                        method: "DELETE",
                        headers: { "Authorization": "Bearer " + getToken() }
                    });
                }
            }).then(loadHistory).catch(function () { });
        } else {
            var list = [];
            try {
                list = JSON.parse(localStorage.getItem(historyKey()) || "[]");
            } catch (e) { }
            list = list.filter(function (k) {
                return k.toLowerCase() !== keyword.toLowerCase();
            });
            localStorage.setItem(historyKey(), JSON.stringify(list));
            loadHistory();
        }
    }

    function clearHistory() {
        // Xóa hiển thị ngay để người dùng thấy phản hồi, rồi đồng bộ với server/localStorage
        try { localStorage.removeItem(historyKey()); } catch (e) { }
        renderHistory([]);
        if (isLoggedIn()) {
            fetch("/api/user/search-history?shopId=" + shopId, {
                method: "DELETE",
                headers: { "Authorization": "Bearer " + getToken() }
            }).then(loadHistory).catch(function () { });
        }
    }
})();

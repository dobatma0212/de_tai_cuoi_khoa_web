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
    });

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

        Array.prototype.forEach.call(
            document.querySelectorAll("#homeSections .category-card"), function (el) {
                el.addEventListener("click", function () {
                    openCategory(el.getAttribute("data-cat"), el.getAttribute("data-name"));
                });
            });
    }

    // ============ Xem tất cả sản phẩm của một danh mục ============

    function openCategory(categoryId, name) {
        fetch("/api/public/shops/" + shopId + "/products?categoryId=" + encodeURIComponent(categoryId))
            .then(function (r) { return r.json(); })
            .then(function (data) {
                var list = (data.data || []).filter(function (p) {
                    return String(p.status) === "active";
                });
                showResults("Danh mục: " + name, list);
            })
            .catch(function () {
                alert("Không tải được danh sách sản phẩm, vui lòng thử lại.");
            });
    }

    /** Hiển thị danh sách sản phẩm trong vùng kết quả (dùng cho cả tìm kiếm & danh mục) */
    function showResults(title, list) {
        $("homeSections").style.display = "none";
        $("searchResultSection").style.display = "block";
        $("resultsTitle").textContent = title;
        $("searchResultCount").textContent = list.length + " sản phẩm";
        if (!list.length) {
            $("searchResults").innerHTML =
                '<div class="col-12"><p class="text-muted">Chưa có sản phẩm nào.</p></div>';
            return;
        }
        $("searchResults").innerHTML = list.map(function (p) {
            return '<div class="col-lg-3 col-md-4 col-sm-6 mb-4"><div class="search-result-item">' +
                '<img src="' + escapeHtml(p.imageUrl || "/img/product/product-1.jpg") + '" alt="' + escapeHtml(p.name) + '">' +
                '<div class="body"><small class="text-muted">' + escapeHtml(p.categoryName || "") + "</small>" +
                '<h6 class="font-weight-bold mb-1">' + escapeHtml(p.name) + "</h6>" +
                '<div class="price-text">' + formatVnd(p.price) + "</div>" +
                '<small class="text-muted d-block mt-1">' + escapeHtml(p.shopName || "") + "</small>" +
                "</div></div></div>";
        }).join("");
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

    function doSearch(keyword) {
        hideSuggest();
        if (!keyword) return;
        $("searchInput").value = keyword;
        saveHistory(keyword);
        fetch("/api/public/shops/" + shopId + "/products?keyword=" + encodeURIComponent(keyword))
            .then(function (r) { return r.json(); })
            .then(function (data) {
                var list = (data.data || []).filter(function (p) {
                    return String(p.status) === "active";
                });
                renderSearchResults(keyword, list);
                loadHistory();
            })
            .catch(function () {
                alert("Không tìm được sản phẩm, vui lòng thử lại.");
            });
    }

    function renderSearchResults(keyword, list) {
        showResults('Kết quả tìm kiếm cho "' + keyword + '"', list);
    }

    function backToHome() {
        $("searchResultSection").style.display = "none";
        $("homeSections").style.display = "block";
        $("searchInput").value = "";
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
        if (isLoggedIn()) {
            fetch("/api/user/search-history?shopId=" + shopId, {
                method: "DELETE",
                headers: { "Authorization": "Bearer " + getToken() }
            }).then(loadHistory).catch(function () { });
        } else {
            localStorage.removeItem(historyKey());
            loadHistory();
        }
    }
})();

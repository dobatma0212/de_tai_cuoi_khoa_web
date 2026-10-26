/**
 * home.js - Trang chủ: 4 danh sách top 20 (mới nhất, bán chạy, đánh giá cao, yêu thích nhiều)
 * có phân trang, và mục "Sản phẩm đã xem gần đây" cho người dùng đã đăng nhập.
 */
(function () {
    "use strict";

    var PAGE_SIZE = 8;
    var EMPTY = {
        "new": "Chưa có sản phẩm nào.",
        "bestseller": "Chưa có sản phẩm nào.",
        "toprated": "Chưa có sản phẩm nào được đánh giá.",
        "favorite": "Chưa có sản phẩm nào được yêu thích."
    };
    var state = { type: "new", page: 0 };
    var requestSeq = 0;

    function $(id) { return document.getElementById(id); }

    document.addEventListener("DOMContentLoaded", function () {
        var tabs = $("homeTabs");
        var grid = $("homeGrid");
        if (!tabs || !grid) return;

        tabs.addEventListener("click", function (e) {
            var li = e.target.closest("li[data-type]");
            if (!li) return;
            Array.prototype.forEach.call(tabs.querySelectorAll("li"), function (x) { x.classList.remove("active"); });
            li.classList.add("active");
            state.type = li.getAttribute("data-type");
            state.page = 0;
            loadList();
        });

        $("homePagination").addEventListener("click", function (e) {
            var a = e.target.closest("a[data-page]");
            if (!a) return;
            e.preventDefault();
            state.page = parseInt(a.getAttribute("data-page"), 10);
            loadList();
            $("homeTabs").scrollIntoView({ behavior: "smooth", block: "start" });
        });

        ScShop.bindCards(grid);
        ScShop.bindCards($("homeRecentGrid"));

        ScShop.loadFavIds().then(function () {
            loadList();
            loadRecent();
            ScShop.updateCartBadge();
        });
    });

    function loadList() {
        var seq = ++requestSeq;
        var grid = $("homeGrid");
        grid.innerHTML = '<div class="col-12 text-center text-muted py-4">Đang tải sản phẩm...</div>';
        fetch("/api/public/catalog/top?type=" + encodeURIComponent(state.type) +
            "&page=" + state.page + "&pageSize=" + PAGE_SIZE)
            .then(function (r) { return r.json(); })
            .then(function (res) {
                if (seq !== requestSeq) return; // đã có yêu cầu mới hơn (đổi tab nhanh)
                if (res.success === false) throw new Error(res.message);
                render(res.data || {});
            })
            .catch(function (err) {
                if (seq !== requestSeq) return;
                grid.innerHTML = '<div class="col-12 text-center text-danger py-4">' +
                    ScShop.esc(err.message || "Không tải được sản phẩm.") + "</div>";
                $("homePagination").innerHTML = "";
            });
    }

    function render(data) {
        var list = data.content || [];
        var grid = $("homeGrid");
        if (!list.length) {
            grid.innerHTML = '<div class="col-12 text-center text-muted py-4">' + EMPTY[state.type] + "</div>";
            $("homePagination").innerHTML = "";
            return;
        }
        grid.innerHTML = list.map(function (p) { return ScShop.cardHtml(p, "col-lg-3 col-md-4 col-sm-6"); }).join("");
        ScShop.applyBackgrounds(grid);
        renderPagination(data.page || 0, data.totalPages || 0);
    }

    function renderPagination(current, total) {
        var box = $("homePagination");
        if (total <= 1) { box.innerHTML = ""; return; }
        var html = "";
        for (var i = 0; i < total; i++) {
            html += '<a href="#" data-page="' + i + '"' + (i === current ? ' class="active"' : "") + ">" + (i + 1) + "</a>";
        }
        box.innerHTML = html;
    }

    function loadRecent() {
        if (!ScShop.isLoggedIn()) return;
        ScShop.authFetch("/api/user/recently-viewed?limit=8").then(function (list) {
            if (!list || !list.length) return;
            var grid = $("homeRecentGrid");
            grid.innerHTML = list.map(function (p) { return ScShop.cardHtml(p, "col-lg-3 col-md-4 col-sm-6"); }).join("");
            ScShop.applyBackgrounds(grid);
            $("homeRecentSection").style.display = "block";
        }).catch(function () { });
    }
})();

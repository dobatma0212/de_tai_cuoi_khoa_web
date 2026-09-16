/* Trang sản phẩm theo danh mục: lọc danh mục, tìm kiếm, sắp xếp, phân trang (20 sp/trang) */
"use strict";

(function ($) {
  var PAGE_SIZE = 20;

  var state = { category: "", keyword: "", sort: "new", page: 0 };

  function readUrl() {
    var params = new URLSearchParams(window.location.search);
    state.category = params.get("category") || "";
    state.keyword = params.get("keyword") || "";
    state.sort = params.get("sort") || "new";
    state.page = Math.max(parseInt(params.get("page") || "1", 10) - 1, 0);
  }

  function syncUrl() {
    var params = new URLSearchParams();
    if (state.category) params.set("category", state.category);
    if (state.keyword) params.set("keyword", state.keyword);
    if (state.sort && state.sort !== "new") params.set("sort", state.sort);
    if (state.page > 0) params.set("page", String(state.page + 1));
    var qs = params.toString();
    var url = "/shop.html" + (qs ? "?" + qs : "");
    window.history.pushState({}, "", url);
  }

  function syncSortSelect() {
    var $sel = $("#sortSelect");
    $sel.val(state.sort);
    var $nice = $sel.next(".nice-select");
    if ($nice.length) {
      var text = $sel.find("option[value='" + state.sort + "']").text();
      $nice.find(".current").text(text);
      $nice.find(".list li").removeClass("selected");
      $nice.find(".list li[data-value='" + state.sort + "']").addClass("selected");
    }
  }

  function renderCategories(cats) {
    var $list = $("#categoryList");
    $list.empty();
    var current = state.category;
    $list.append(
      '<li><a href="/shop.html" data-slug="" class="' + (current === "" ? "active" : "") + '">Tất cả sản phẩm</a></li>'
    );
    var roots = cats.filter(function (c) { return !c.parentId; });
    roots.forEach(function (root) {
      var isActiveRoot = current === root.slug;
      $list.append(
        '<li><a href="/shop.html?category=' + encodeURIComponent(root.slug) + '" data-slug="' + root.slug +
        '" class="' + (isActiveRoot ? "active" : "") + '">' + StoreUI.escapeHtml(root.name) + "</a></li>"
      );
      cats.filter(function (c) { return c.parentId === root.id; }).forEach(function (child) {
        $list.append(
          '<li><a href="/shop.html?category=' + encodeURIComponent(child.slug) + '" data-slug="' + child.slug +
          '" style="padding-left:18px;font-size:13px;" class="' + (current === child.slug ? "active" : "") + '">' +
          StoreUI.escapeHtml(child.name) + "</a></li>"
        );
      });
    });
  }

  function renderPagination(pageData) {
    var $pg = $("#shopPagination");
    $pg.empty();
    var total = pageData.totalPages;
    if (total <= 1) return;
    var current = state.page;

    function addPage(n) {
      $pg.append('<a href="#" data-page="' + n + '" class="' + (n === current ? "active" : "") + '">' + (n + 1) + "</a>");
    }
    function addEllipsis() {
      $pg.append("<span>...</span>");
    }

    var pages = [];
    var i;
    pages.push(0);
    for (i = Math.max(1, current - 1); i <= Math.min(total - 2, current + 1); i++) pages.push(i);
    if (total > 1) pages.push(total - 1);
    pages = pages.filter(function (v, idx, self) { return self.indexOf(v) === idx; }).sort(function (a, b) { return a - b; });

    var prev = -2;
    pages.forEach(function (n) {
      if (n - prev > 1) addEllipsis();
      addPage(n);
      prev = n;
    });
  }

  function renderProducts(pageData) {
    var $grid = $("#shopProductGrid");
    if (!pageData.content || pageData.content.length === 0) {
      $grid.html('<div class="col-12 text-center" style="padding:60px 0;color:#b2b2b2;">Không tìm thấy sản phẩm nào.</div>');
      $("#showingText").text("0 sản phẩm");
      $("#shopPagination").empty();
      return;
    }
    var html = "";
    pageData.content.forEach(function (p) {
      html += StoreUI.productCard(p, "col-lg-4 col-md-6 col-sm-6");
    });
    $grid.html(html);

    var from = state.page * PAGE_SIZE + 1;
    var to = state.page * PAGE_SIZE + pageData.content.length;
    $("#showingText").text("Hiển thị " + from + "–" + to + " của " + pageData.totalElements + " sản phẩm");
    renderPagination(pageData);
  }

  function updateBreadcrumb() {
    var title = "Shop";
    var current = "Tất cả sản phẩm";
    if (state.category) {
      var $link = $('#categoryList a[data-slug="' + state.category + '"]');
      if ($link.length) {
        current = $link.text().trim();
      }
    }
    if (state.keyword) {
      current = 'Kết quả cho "' + state.keyword + '"';
    }
    $("#breadcrumbTitle").text(title);
    $("#breadcrumbCurrent").text(current);
    document.title = "StoreChain | " + current;
  }

  function loadProducts() {
    var $grid = $("#shopProductGrid");
    $grid.html('<div class="col-12 text-center" style="padding:60px 0;color:#b2b2b2;">Đang tải sản phẩm...</div>');
    $("#shopPagination").empty();

    var params = { sort: state.sort, page: state.page, size: PAGE_SIZE };
    if (state.category) params.category = state.category;
    if (state.keyword) params.keyword = state.keyword;

    StoreUI.getProducts(params)
      .then(function (pageData) {
        renderProducts(pageData);
        updateBreadcrumb();
      })
      .catch(function (err) {
        $grid.html('<div class="col-12 text-center" style="padding:60px 0;color:#e53637;">Lỗi tải sản phẩm: ' +
          StoreUI.escapeHtml(err.message) + "</div>");
      });
  }

  function applyState(reload) {
    syncUrl();
    $('#categoryList a').removeClass("active");
    var $link = $('#categoryList a[data-slug="' + state.category + '"]');
    if ($link.length) $link.addClass("active");
    syncSortSelect();
    if (reload !== false) loadProducts();
  }

  $(document).ready(function () {
    readUrl();
    syncSortSelect();

    if (state.keyword) {
      $("#sidebarKeyword").val(state.keyword);
    }

    StoreUI.api("/api/public/categories")
      .then(renderCategories)
      .catch(function () {
        $("#categoryList").html('<li><a href="/shop.html">Không tải được danh mục</a></li>');
      });

    $("#categoryList").on("click", "a[data-slug]", function (e) {
      e.preventDefault();
      state.category = $(this).data("slug") || "";
      state.page = 0;
      applyState();
    });

    $("#sortSelect").on("change", function () {
      state.sort = $(this).val() || "new";
      state.page = 0;
      applyState();
    });

    $("#sidebarSearchForm").on("submit", function (e) {
      e.preventDefault();
      state.keyword = $("#sidebarKeyword").val().trim();
      state.page = 0;
      applyState();
    });

    $("#shopPagination").on("click", "a[data-page]", function (e) {
      e.preventDefault();
      var page = parseInt($(this).data("page"), 10);
      if (page === state.page) return;
      state.page = page;
      applyState(false);
      loadProducts();
      var top = $("#shopProductGrid").offset().top - 120;
      $("html, body").animate({ scrollTop: top }, 300);
    });

    window.addEventListener("popstate", function () {
      readUrl();
      syncSortSelect();
      $("#sidebarKeyword").val(state.keyword);
      $("#categoryList a").removeClass("active");
      $('#categoryList a[data-slug="' + state.category + '"]').addClass("active");
      loadProducts();
    });

    loadProducts();
  });
})(jQuery);

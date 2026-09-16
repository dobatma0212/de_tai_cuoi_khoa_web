/* Trang chủ: hiển thị 20 sản phẩm theo tab (Mới / Bán chạy / Đánh giá cao / Yêu thích) + lazy loading */
"use strict";

(function ($) {
  var PAGE_SIZE = 20;

  var state = { sort: "new", page: 0, loading: false };
  var $grid = $("#homeProductGrid");
  var $moreBtn = $("#loadMoreBtn");

  function renderEmptyOrError(message, isError) {
    $grid.html(
      '<div class="col-12 text-center" style="padding:60px 0;color:' + (isError ? "#e53637" : "#b2b2b2") + ';">' +
      StoreUI.escapeHtml(message) + "</div>"
    );
  }

  function loadProducts(reset) {
    if (state.loading) return;
    state.loading = true;

    if (reset) {
      state.page = 0;
      $grid.empty();
      renderEmptyOrError("Đang tải sản phẩm...", false);
    }
    $moreBtn.hide().text("Đang tải...");

    StoreUI.getProducts({ sort: state.sort, page: state.page, size: PAGE_SIZE })
      .then(function (pageData) {
        if (reset) $grid.empty();
        if (!pageData.content || pageData.content.length === 0) {
          if (reset) renderEmptyOrError("Chưa có sản phẩm nào.", false);
          return;
        }
        var html = "";
        pageData.content.forEach(function (p) {
          html += StoreUI.productCard(p, "col-lg-3 col-md-6 col-sm-6");
        });
        $grid.append(html);
        if (!pageData.last) {
          $moreBtn.show().text("Tải thêm sản phẩm");
        }
      })
      .catch(function (err) {
        if (reset) renderEmptyOrError("Lỗi tải sản phẩm: " + err.message, true);
      })
      .finally(function () {
        state.loading = false;
      });
  }

  $(document).ready(function () {
    $(".filter__controls li").on("click", function () {
      var sort = $(this).data("sort");
      if (!sort || sort === state.sort) return;
      state.sort = sort;
      loadProducts(true);
    });

    $moreBtn.on("click", function () {
      state.page += 1;
      loadProducts(false);
    });

    loadProducts(true);
  });
})(jQuery);

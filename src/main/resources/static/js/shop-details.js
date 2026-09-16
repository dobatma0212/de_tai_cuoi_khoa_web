/* Trang chi tiết sản phẩm: thông tin, thư viện ảnh, tab mô tả/đánh giá/thông tin, sản phẩm liên quan */
"use strict";

(function ($) {
  var slug = new URLSearchParams(window.location.search).get("slug");

  function formatNumber(n) {
    return new Intl.NumberFormat("vi-VN").format(n || 0);
  }

  function renderGallery(p) {
    var images = p.imageUrl ? [p.imageUrl] : [];
    if (images.length === 0) {
      $("#dThumbTabs").closest(".col-lg-3").hide();
      $("#dGalleryPanes").html('<div style="height:420px;background:#f5f5f5;"></div>');
      return;
    }
    var thumbs = "";
    var panes = "";
    images.forEach(function (url, i) {
      var active = i === 0 ? " active" : "";
      thumbs +=
        '<li class="nav-item"><a class="nav-link' + active + '" data-toggle="tab" href="#dgallery-' + i + '" role="tab">' +
        '<div class="product__thumb__pic set-bg" style="background-image:url(\'' + StoreUI.escapeHtml(url) + '\')"></div>' +
        "</a></li>";
      panes +=
        '<div class="tab-pane' + active + '" id="dgallery-' + i + '" role="tabpanel">' +
        '<div class="product__details__pic__item"><img src="' + StoreUI.escapeHtml(url) + '" alt="' + StoreUI.escapeHtml(p.name) + '"></div>' +
        "</div>";
    });
    $("#dThumbTabs").html(thumbs);
    $("#dGalleryPanes").html(panes);
    if (images.length <= 1) {
      $("#dThumbTabs").closest(".col-lg-3").hide();
      $("#dGalleryCol").addClass("offset-lg-3");
    }
  }

  function renderDetails(p) {
    $("#dBreadcrumb").text(p.name);
    document.title = "StoreChain | " + p.name;
    $("#dName").text(p.name);
    $("#dRating").html(StoreUI.renderStars(p.ratingAvg) + "<span> - " + p.reviewCount + " đánh giá</span>");

    var priceHtml = StoreUI.formatPrice(p.price);
    if (p.originalPrice && p.originalPrice > p.price) {
      priceHtml += " <span style=\"text-decoration:line-through;font-size:18px;color:#b2b2b2;\">" +
        StoreUI.formatPrice(p.originalPrice) + "</span>";
    }
    $("#dPrice").html(priceHtml);

    $("#dShortDesc").text(p.description || "Chưa có mô tả cho sản phẩm này.");

    $("#dMeta").html(
      "<li><i class=\"fa fa-check-circle\" style=\"color:#3d9970;margin-right:6px;\"></i>Còn lại " +
        formatNumber(p.stock) + " sản phẩm trong kho</li>" +
      "<li><i class=\"fa fa-shopping-cart\" style=\"color:#e53637;margin-right:8px;\"></i>Đã bán " +
        formatNumber(p.soldCount) + " · " + formatNumber(p.favoriteCount) + " lượt yêu thích</li>"
    );

    $("#dSku").text("SC-" + String(p.id).padStart(6, "0"));
    $("#dCategoryLink").text(p.categoryName).attr("href", "/shop.html?category=" + encodeURIComponent(p.categorySlug));
    $("#dAvailability").text(p.stock > 0 ? "Còn hàng" : "Hết hàng");

    $("#dTabDescription").html(
      '<p class="note">' + StoreUI.escapeHtml(p.description || "Chưa có mô tả.") + "</p>" +
      '<div class="product__details__tab__content__item">' +
        "<h5>Cam kết từ cửa hàng</h5>" +
        "<p>Sản phẩm được kiểm tra chất lượng trước khi giao hàng. Hỗ trợ đổi trả trong vòng 30 ngày " +
        "kể từ ngày nhận hàng nếu sản phẩm có lỗi từ nhà sản xuất.</p>" +
      "</div>"
    );

    $("#dReviewCount").text(p.reviewCount);
    $("#dTabReviews").html(
      '<div class="product__details__tab__content__item">' +
        "<h5>Đánh giá tổng quan</h5>" +
        '<div class="rating" style="margin:10px 0;">' + StoreUI.renderStars(p.ratingAvg) + "</div>" +
        "<p>Sản phẩm được đánh giá trung bình <strong>" + (p.ratingAvg || 0) + "/5</strong> từ " +
        "<strong>" + formatNumber(p.reviewCount) + "</strong> lượt đánh giá của khách hàng.</p>" +
        "<p>Đánh giá chi tiết từng khách hàng sẽ được cập nhật sau khi nhóm Đánh giá sản phẩm hoàn tất tích hợp.</p>" +
      "</div>"
    );

    var createdText = "";
    if (p.createdAt) {
      var d = Array.isArray(p.createdAt)
        ? new Date(p.createdAt[0], (p.createdAt[1] || 1) - 1, p.createdAt[2] || 1)
        : new Date(p.createdAt);
      if (!isNaN(d.getTime())) {
        createdText = ("0" + d.getDate()).slice(-2) + "/" + ("0" + (d.getMonth() + 1)).slice(-2) + "/" + d.getFullYear();
      }
    }
    $("#dTabInfo").html(
      '<div class="product__details__tab__content__item">' +
        "<h5>Thông tin sản phẩm</h5>" +
        "<p><strong>SKU:</strong> SC-" + String(p.id).padStart(6, "0") + "<br>" +
        "<strong>Danh mục:</strong> " + StoreUI.escapeHtml(p.categoryName || "—") + "<br>" +
        "<strong>Tồn kho:</strong> " + formatNumber(p.stock) + "<br>" +
        "<strong>Đã bán:</strong> " + formatNumber(p.soldCount) + "<br>" +
        "<strong>Ngày đăng:</strong> " + (createdText || "—") + "</p>" +
      "</div>"
    );
  }

  function renderRelated(products) {
    var $grid = $("#relatedGrid");
    if (!products || products.length === 0) {
      $grid.html('<div class="col-12 text-center" style="padding:30px 0;color:#b2b2b2;">Chưa có sản phẩm liên quan.</div>');
      return;
    }
    var html = "";
    products.forEach(function (p) {
      html += StoreUI.productCard(p, "col-lg-3 col-md-6 col-sm-6");
    });
    $grid.html(html);
  }

  function showError(message) {
    $("#dName").text("Không tải được sản phẩm");
    $("#dRating, #dPrice, #dShortDesc, #dMeta").empty();
    $("#dTabDescription").html('<p class="note" style="color:#e53637;">' + StoreUI.escapeHtml(message) + "</p>");
    $("#relatedGrid").closest("section").hide();
  }

  $(document).ready(function () {
    if (!slug) {
      showError("Không tìm thấy sản phẩm (thiếu tham số slug trên đường dẫn).");
      return;
    }

    StoreUI.api("/api/public/products/" + encodeURIComponent(slug))
      .then(function (p) {
        renderGallery(p);
        renderDetails(p);
        return StoreUI.api("/api/public/products/" + encodeURIComponent(slug) + "/related?limit=4");
      })
      .then(renderRelated)
      .catch(showError);

    $("#dAddToCart").on("click", function (e) {
      e.preventDefault();
      alert("Tính năng giỏ hàng sẽ được tích hợp bởi nhóm Giỏ hàng & Đặt hàng.");
    });
    $("#dWishlist").on("click", function (e) {
      e.preventDefault();
      alert("Tính năng yêu thích sẽ được tích hợp bởi nhóm Khách hàng.");
    });
    $("#dShare").on("click", function (e) {
      e.preventDefault();
      if (navigator.clipboard) {
        navigator.clipboard.writeText(window.location.href);
        alert("Đã copy liên kết sản phẩm!");
      }
    });
  });
})(jQuery);

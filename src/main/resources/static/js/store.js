/* StoreChain - js dùng chung cho hiển thị sản phẩm (trang chủ, shop, chi tiết) */
"use strict";

var StoreUI = (function () {
  var priceFormatter = new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" });

  function formatPrice(value) {
    if (value === null || value === undefined) return "";
    return priceFormatter.format(value);
  }

  function escapeHtml(text) {
    if (text === null || text === undefined) return "";
    return String(text)
      .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;").replace(/'/g, "&#039;");
  }

  function renderStars(rating) {
    var full = Math.round(rating || 0);
    var html = "";
    for (var i = 1; i <= 5; i++) {
      html += '<i class="fa fa-star' + (i <= full ? "" : "-o") + '"></i>';
    }
    return html;
  }

  function productCard(p, colClass) {
    var img = p.imageUrl || "/img/product/product-1.jpg";
    var label = "";
    if (p.originalPrice && p.originalPrice > p.price) {
      label = '<span class="label">Sale</span>';
    } else if (p.soldCount > 500) {
      label = '<span class="label">Hot</span>';
    }
    var oldPrice = p.originalPrice && p.originalPrice > p.price
      ? "<del style=\"color:#b2b2b2;font-size:13px;margin-right:6px;\">" + formatPrice(p.originalPrice) + "</del>"
      : "";

    return (
      '<div class="' + colClass + '">' +
        '<div class="product__item">' +
          '<div class="product__item__pic set-bg" style="background-image:url(\'' + escapeHtml(img) + '\')">' +
            label +
            '<ul class="product__hover">' +
              '<li><a href="#"><img src="/img/icon/heart.png" alt=""></a></li>' +
              '<li><a href="/shop-details.html?slug=' + encodeURIComponent(p.slug) + '"><img src="/img/icon/search.png" alt=""></a></li>' +
            '</ul>' +
          '</div>' +
          '<div class="product__item__text">' +
            '<h6><a href="/shop-details.html?slug=' + encodeURIComponent(p.slug) + '">' + escapeHtml(p.name) + "</a></h6>" +
            '<a href="#" class="add-cart">+ Thêm vào giỏ</a>' +
            '<div class="rating">' + renderStars(p.ratingAvg) + "</div>" +
            "<h5>" + oldPrice + formatPrice(p.price) + "</h5>" +
            '<div class="product__color__select">' +
              '<label class="active black"><input type="radio"></label>' +
              '<label class="grey"><input type="radio"></label>' +
            "</div>" +
          "</div>" +
        "</div>" +
      "</div>"
    );
  }

  function api(path) {
    return fetch(path)
      .then(function (res) {
        if (!res.ok) throw new Error("HTTP " + res.status);
        return res.json();
      })
      .then(function (body) {
        if (!body || body.success === false) throw new Error((body && body.message) || "Lỗi tải dữ liệu");
        return body.data;
      });
  }

  function getProducts(params) {
    var qs = new URLSearchParams(params || {}).toString();
    return api("/api/public/products" + (qs ? "?" + qs : ""));
  }

  function initSearchRedirect() {
    document.addEventListener("submit", function (e) {
      var form = e.target;
      if (!form.classList || !form.classList.contains("search-model-form")) return;
      e.preventDefault();
      var input = form.querySelector("#search-input");
      var keyword = input ? input.value.trim() : "";
      window.location.href = "/shop.html" + (keyword ? "?keyword=" + encodeURIComponent(keyword) : "");
    });
  }

  return {
    formatPrice: formatPrice,
    escapeHtml: escapeHtml,
    renderStars: renderStars,
    productCard: productCard,
    api: api,
    getProducts: getProducts,
    initSearchRedirect: initSearchRedirect
  };
})();

document.addEventListener("DOMContentLoaded", function () {
  StoreUI.initSearchRedirect();
});

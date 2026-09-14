// Gọi API lấy danh sách sản phẩm đã bán trên 10 sản phẩm, sắp xếp giảm dần theo số lượng bán
// và render vào khu vực #product-list trên trang chủ (index.html)

document.addEventListener("DOMContentLoaded", function () {
  var API_URL = "/api/public/products/best-sellers";

  var listEl = document.getElementById("product-list");
  var loadingEl = document.getElementById("product-loading");
  var emptyEl = document.getElementById("product-empty");

  if (!listEl) return;

  fetch(API_URL)
    .then(function (res) {
      if (!res.ok) {
        throw new Error("HTTP " + res.status);
      }
      return res.json();
    })
    .then(function (products) {
      if (loadingEl) loadingEl.style.display = "none";

      if (!products || products.length === 0) {
        if (emptyEl) emptyEl.style.display = "block";
        return;
      }

      products.forEach(function (p) {
        listEl.appendChild(buildProductCard(p));
      });
    })
    .catch(function (err) {
      console.error("Không tải được danh sách sản phẩm:", err);
      if (loadingEl) {
        loadingEl.textContent =
          "Không tải được danh sách sản phẩm. Vui lòng thử lại sau.";
      }
    });

  function buildProductCard(product) {
    var col = document.createElement("div");
    col.className = "col-lg-3 col-md-6 col-sm-6 mix";

    var imgPath = product.image
      ? "/img/" + product.image
      : "/img/product/product-1.jpg";
    var price = formatPrice(product.price);
    var shopName = product.shopName ? product.shopName : "";

    col.innerHTML =
      '<div class="product__item">' +
      '<div class="product__item__pic set-bg" data-setbg="' +
      imgPath +
      '">' +
      '<span class="label">Đã bán ' +
      product.soldQuantity +
      "</span>" +
      '<ul class="product__hover">' +
      '<li><a href="#"><img src="/img/icon/heart.png" alt=""></a></li>' +
      '<li><a href="#"><img src="/img/icon/compare.png" alt=""> <span>Compare</span></a></li>' +
      '<li><a href="#"><img src="/img/icon/search.png" alt=""></a></li>' +
      "</ul>" +
      "</div>" +
      '<div class="product__item__text">' +
      "<h6>" +
      escapeHtml(product.name) +
      "</h6>" +
      (shopName
        ? '<div class="shop-name">Shop: ' + escapeHtml(shopName) + "</div>"
        : "") +
      '<a href="#" class="add-cart">+ Add To Cart</a>' +
      "<h5>" +
      price +
      "</h5>" +
      "</div>" +
      "</div>";

    // set-bg dùng data-setbg, cần gán background-image trực tiếp
    // vì main.js chỉ chạy set-bg cho các phần tử có sẵn lúc DOMContentLoaded
    var picEl = col.querySelector(".set-bg");
    if (picEl) {
      picEl.style.backgroundImage = "url('" + imgPath + "')";
    }

    return col;
  }

  function formatPrice(price) {
    if (price === undefined || price === null) return "";
    return Number(price).toLocaleString("vi-VN") + " đ";
  }

  function escapeHtml(str) {
    if (!str) return "";
    return str
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }
});

/**
 * store.js — Lớp tiện ích dùng chung cho toàn bộ giao diện người mua.
 * - Quản lý token/phiên đăng nhập (dùng chung với auth.js)
 * - Gọi API người mua (/api/user), hồ sơ (/api/profile), công khai (/api/public)
 * - Cập nhật số lượng giỏ hàng trên header, hiển thị modal "Thêm vào giỏ"
 * - Render thẻ sản phẩm, sao đánh giá, định dạng tiền VNĐ
 */
(function () {
    "use strict";

    var ScStore = {};

    // ============ Phiên đăng nhập ============
    ScStore.getToken = function () {
        var s = window.ScAuth ? window.ScAuth.getSession() : null;
        return (s && s.token) || localStorage.getItem("jwt_token");
    };

    ScStore.getSession = function () {
        return window.ScAuth ? window.ScAuth.getSession() : null;
    };

    ScStore.isLoggedIn = function () {
        return !!ScStore.getToken();
    };

    ScStore.authHeaders = function () {
        var h = { "Content-Type": "application/json" };
        var t = ScStore.getToken();
        if (t) {
            h["Authorization"] = "Bearer " + t;
        }
        return h;
    };

    ScStore.logout = function () {
        if (window.ScAuth) {
            window.ScAuth.logout();
        } else {
            localStorage.clear();
            window.location.href = "/index.html";
        }
    };

    ScStore.requireLogin = function () {
        if (!ScStore.getToken()) {
            window.location.href = "/login.html";
            return false;
        }
        return true;
    };

    // ============ Gọi API ============
    function request(base, method, path, body) {
        var opts = {
            method: method,
            headers: ScStore.authHeaders()
        };
        if (body !== undefined && body !== null) {
            opts.body = JSON.stringify(body);
        }
        return fetch(base + path, opts).then(function (res) {
            if (res.status === 401 || res.status === 403) {
                if (window.ScAuth) window.ScAuth.clearSession();
                window.location.href = "/login.html";
                return new Promise(function () { /* dừng chuỗi */ });
            }
            return res.json().then(function (data) {
                if (!res.ok || data.success === false) {
                    var err = new Error(data.message || "Đã có lỗi xảy ra, vui lòng thử lại.");
                    err.payload = data;
                    throw err;
                }
                return data;
            });
        });
    }

    /** API người mua đã đăng nhập: /api/user/... */
    ScStore.api = function (method, path, body) { return request("/api/user", method, path, body); };
    /** API hồ sơ cá nhân: /api/profile/... */
    ScStore.profileApi = function (method, path, body) { return request("/api/profile", method, path, body); };
    /** API công khai: /api/public/... */
    ScStore.publicApi = function (method, path, body) { return request("/api/public", method, path, body); };

    // ============ Tiện ích hiển thị ============
    ScStore.escapeHtml = function (str) {
        return String(str == null ? "" : str)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
    };

    ScStore.formatVnd = function (value) {
        var n = Number(value);
        if (isNaN(n)) n = 0;
        return n.toLocaleString("vi-VN") + "₫";
    };

    ScStore.stars = function (rating) {
        var r = Math.round(Number(rating) || 0);
        var html = "";
        for (var i = 1; i <= 5; i++) {
            html += '<i class="fa ' + (i <= r ? "fa-star" : "fa-star-o") + '"></i>';
        }
        return html;
    };

    /** Chuẩn hóa đường dẫn ảnh sản phẩm (thêm tiền tố / nếu là đường dẫn tương đối) */
    ScStore.imageUrl = function (url) {
        if (!url) return "/img/product/product-1.jpg";
        if (url.startsWith("http") || url.startsWith("/")) return url;
        return "/" + url;
    };

    // ============ Toast thông báo ============
    ScStore.toast = function (message, type) {
        var box = document.getElementById("scToast");
        if (!box) {
            box = document.createElement("div");
            box.id = "scToast";
            box.style.cssText =
                "position:fixed;top:20px;right:20px;z-index:99999;display:flex;flex-direction:column;gap:8px;";
            document.body.appendChild(box);
        }
        var el = document.createElement("div");
        var bg = type === "error" ? "#dc3545" : "#28a745";
        el.style.cssText = "background:" + bg + ";color:#fff;padding:12px 18px;border-radius:6px;" +
            "font-weight:600;box-shadow:0 6px 20px rgba(0,0,0,.2);max-width:320px;";
        el.textContent = message;
        box.appendChild(el);
        setTimeout(function () {
            el.style.transition = "opacity .4s";
            el.style.opacity = "0";
            setTimeout(function () { el.remove(); }, 420);
        }, 2600);
    };

    // ============ Badge giỏ hàng trên header ============
    ScStore.updateCartBadge = function () {
        var badges = document.querySelectorAll(".sc-cart-count");
        if (!badges.length) return;
        if (!ScStore.isLoggedIn()) {
            badges.forEach(function (b) { b.textContent = "0"; });
            return;
        }
        ScStore.api("GET", "/cart/count").then(function (res) {
            var n = Number(res.data || 0);
            badges.forEach(function (b) { b.textContent = n; });
        }).catch(function () {
            badges.forEach(function (b) { b.textContent = "0"; });
        });
    };

    // ============ Thêm vào giỏ ============
    ScStore.addToCart = function (productId, size, color, quantity) {
        return ScStore.api("POST", "/cart", {
            productId: productId,
            size: size || "",
            color: color || "",
            quantity: quantity || 1
        }).then(function (res) {
            ScStore.toast("Đã thêm sản phẩm vào giỏ hàng!", "success");
            ScStore.updateCartBadge();
            return res;
        });
    };

    // ============ Modal chọn size/màu/số lượng rồi thêm vào giỏ ============
    function ensureCartModal() {
        var modal = document.getElementById("scCartModal");
        if (modal) return modal;
        modal = document.createElement("div");
        modal.id = "scCartModal";
        modal.style.cssText =
            "position:fixed;inset:0;background:rgba(0,0,0,.5);display:none;z-index:99998;" +
            "align-items:center;justify-content:center;padding:16px;";
        modal.innerHTML =
            '<div style="background:#fff;border-radius:8px;max-width:440px;width:100%;overflow:hidden;">' +
            '  <div style="display:flex;justify-content:space-between;align-items:center;padding:16px 20px;border-bottom:1px solid #eee;">' +
            '    <h5 style="margin:0;font-weight:800;">Thêm vào giỏ hàng</h5>' +
            '    <button type="button" data-sc-close style="border:none;background:none;font-size:22px;line-height:1;cursor:pointer;">&times;</button>' +
            '  </div>' +
            '  <div style="padding:20px;">' +
            '    <div style="display:flex;gap:14px;margin-bottom:16px;">' +
            '      <img id="scModalImg" src="" alt="" style="width:72px;height:72px;object-fit:cover;border-radius:6px;">' +
            '      <div><b id="scModalName" style="display:block;color:#111;"></b>' +
            '           <span id="scModalPrice" style="color:#e53637;font-weight:800;"></span></div>' +
            '    </div>' +
            '    <div id="scModalSizeWrap" style="margin-bottom:14px;">' +
            '      <label style="font-weight:700;font-size:13px;display:block;margin-bottom:6px;">Size</label>' +
            '      <div id="scModalSizes" style="display:flex;gap:8px;flex-wrap:wrap;"></div>' +
            '    </div>' +
            '    <div id="scModalColorWrap" style="margin-bottom:14px;">' +
            '      <label style="font-weight:700;font-size:13px;display:block;margin-bottom:6px;">Màu</label>' +
            '      <div id="scModalColors" style="display:flex;gap:8px;flex-wrap:wrap;"></div>' +
            '    </div>' +
            '    <div style="margin-bottom:18px;">' +
            '      <label style="font-weight:700;font-size:13px;display:block;margin-bottom:6px;">Số lượng</label>' +
            '      <div class="sc-qty" style="display:inline-flex;align-items:center;border:1px solid #ddd;border-radius:4px;overflow:hidden;">' +
            '        <button type="button" data-sc-dec style="width:34px;height:34px;border:none;background:#f5f5f5;font-size:18px;cursor:pointer;">−</button>' +
            '        <input id="scModalQty" type="text" value="1" readonly style="width:46px;height:34px;border:none;text-align:center;font-weight:700;">' +
            '        <button type="button" data-sc-inc style="width:34px;height:34px;border:none;background:#f5f5f5;font-size:18px;cursor:pointer;">+</button>' +
            '      </div>' +
            '    </div>' +
            '    <button type="button" id="scModalAdd" class="btn-store w-100" ' +
            '       style="background:#e53637;color:#fff;border:none;border-radius:4px;padding:12px;font-weight:800;text-transform:uppercase;width:100%;">Thêm vào giỏ</button>' +
            '    <a href="/shopping-cart.html" style="display:block;text-align:center;margin-top:12px;color:#111;">Xem giỏ hàng →</a>' +
            '  </div>' +
            '</div>';
        document.body.appendChild(modal);

        modal.addEventListener("click", function (e) {
            if (e.target === modal || e.target.hasAttribute("data-sc-close")) {
                modal.style.display = "none";
            }
        });
        return modal;
    }

    /**
     * Mở modal thêm vào giỏ cho một sản phẩm (đã có sizesList/colorsList).
     */
    ScStore.openAddToCartModal = function (product) {
        if (!ScStore.requireLogin()) {
            ScStore.toast("Vui lòng đăng nhập để thêm sản phẩm vào giỏ.", "error");
            return;
        }
        var modal = ensureCartModal();
        var sizes = product.sizesList || [];
        var colors = product.colorsList || [];
        var selectedSize = sizes.length ? sizes[0] : "";
        var selectedColor = colors.length ? colors[0] : "";

        document.getElementById("scModalImg").src = ScStore.imageUrl(product.imageUrl);
        document.getElementById("scModalName").textContent = product.name || "";
        document.getElementById("scModalPrice").textContent = ScStore.formatVnd(product.price);
        document.getElementById("scModalQty").value = "1";

        var sizesWrap = document.getElementById("scModalSizeWrap");
        var colorsWrap = document.getElementById("scModalColorWrap");
        var sizesBox = document.getElementById("scModalSizes");
        var colorsBox = document.getElementById("scModalColors");

        if (sizes.length) {
            sizesWrap.style.display = "block";
            sizesBox.innerHTML = sizes.map(function (s, i) {
                return '<button type="button" class="sc-size-opt' + (i === 0 ? " active" : "") + '" data-size="' +
                    ScStore.escapeHtml(s) + '" style="padding:6px 14px;border:1px solid ' +
                    (i === 0 ? "#e53637" : "#ddd") + ';border-radius:4px;background:' +
                    (i === 0 ? "#fff7f7" : "#fff") + ';cursor:pointer;">' + ScStore.escapeHtml(s) + "</button>";
            }).join("");
        } else {
            sizesWrap.style.display = "none";
            sizesBox.innerHTML = "";
        }
        if (colors.length) {
            colorsWrap.style.display = "block";
            colorsBox.innerHTML = colors.map(function (c, i) {
                return '<button type="button" class="sc-color-opt' + (i === 0 ? " active" : "") + '" data-color="' +
                    ScStore.escapeHtml(c) + '" style="padding:6px 14px;border:1px solid ' +
                    (i === 0 ? "#e53637" : "#ddd") + ';border-radius:4px;background:' +
                    (i === 0 ? "#fff7f7" : "#fff") + ';cursor:pointer;">' + ScStore.escapeHtml(c) + "</button>";
            }).join("");
        } else {
            colorsWrap.style.display = "none";
            colorsBox.innerHTML = "";
        }

        sizesBox.onclick = function (e) {
            var btn = e.target.closest(".sc-size-opt");
            if (!btn) return;
            selectedSize = btn.getAttribute("data-size");
            Array.prototype.forEach.call(sizesBox.children, function (b) {
                b.classList.remove("active");
                b.style.borderColor = "#ddd";
                b.style.background = "#fff";
            });
            btn.classList.add("active");
            btn.style.borderColor = "#e53637";
            btn.style.background = "#fff7f7";
        };
        colorsBox.onclick = function (e) {
            var btn = e.target.closest(".sc-color-opt");
            if (!btn) return;
            selectedColor = btn.getAttribute("data-color");
            Array.prototype.forEach.call(colorsBox.children, function (b) {
                b.classList.remove("active");
                b.style.borderColor = "#ddd";
                b.style.background = "#fff";
            });
            btn.classList.add("active");
            btn.style.borderColor = "#e53637";
            btn.style.background = "#fff7f7";
        };

        modal.querySelector("[data-sc-dec]").onclick = function () {
            var inp = document.getElementById("scModalQty");
            inp.value = Math.max(1, (parseInt(inp.value, 10) || 1) - 1);
        };
        modal.querySelector("[data-sc-inc]").onclick = function () {
            var inp = document.getElementById("scModalQty");
            inp.value = (parseInt(inp.value, 10) || 1) + 1;
        };
        document.getElementById("scModalAdd").onclick = function () {
            var qty = parseInt(document.getElementById("scModalQty").value, 10) || 1;
            ScStore.addToCart(product.id, selectedSize, selectedColor, qty)
                .then(function () { modal.style.display = "none"; })
                .catch(function (err) { ScStore.toast(err.message, "error"); });
        };

        modal.style.display = "flex";
    };

    /** Lấy chi tiết sản phẩm công khai rồi mở modal (dùng khi card chỉ có id) */
    ScStore.addProductById = function (productId) {
        if (!ScStore.requireLogin()) {
            ScStore.toast("Vui lòng đăng nhập để thêm sản phẩm vào giỏ.", "error");
            return;
        }
        ScStore.publicApi("GET", "/products/" + productId).then(function (res) {
            ScStore.openAddToCartModal(res.data);
        }).catch(function (err) {
            ScStore.toast(err.message, "error");
        });
    };

    // ============ Thẻ sản phẩm ============
    ScStore.renderProductCard = function (p) {
        var img = ScStore.imageUrl(p.imageUrl);
        var price = ScStore.formatVnd(p.price);
        var stockNote = (p.stock === 0)
            ? '<span style="color:#dc3545;font-size:12px;">Hết hàng</span>'
            : "";
        return '' +
            '<div class="col-lg-3 col-md-6 col-sm-6">' +
            '  <div class="product__item">' +
            '    <div class="product__item__pic set-bg" style="background-image:url(' + img + ');">' +
            '      <ul class="product__hover">' +
            '        <li><a href="/shop-details.html?productId=' + p.id + '"><img src="/img/icon/search.png" alt=""></a></li>' +
            '      </ul>' +
            '    </div>' +
            '    <div class="product__item__text">' +
            '      <h6><a href="/shop-details.html?productId=' + p.id + '" style="color:inherit;">' +
                     ScStore.escapeHtml(p.name) + '</a></h6>' +
            '      <a href="javascript:void(0)" class="sc-add-cart" data-product-id="' + p.id + '">+ Thêm vào giỏ</a>' +
            '      <div class="rating">' + ScStore.stars(p.avgRating || 0) +
            '        <span style="font-size:12px;color:#999;">' +
                       (p.reviewCount ? " (" + p.reviewCount + " đánh giá)" : "") + '</span>' +
            '      </div>' +
            '      <h5>' + price + '</h5>' + stockNote +
            '    </div>' +
            '  </div>' +
            '</div>';
    };

    /** Gắn sự kiện cho mọi nút thêm-vào-giỏ trong một vùng */
    ScStore.bindAddCartButtons = function (scope) {
        var root = scope || document;
        root.addEventListener("click", function (e) {
            var link = e.target.closest(".sc-add-cart");
            if (!link) return;
            e.preventDefault();
            var pid = link.getAttribute("data-product-id");
            if (pid) ScStore.addProductById(parseInt(pid, 10));
            else ScStore.addProductById(parseInt(link.getAttribute("data-id"), 10));
        });
    };

    // ============ Tìm kiếm trên header ============
    function bindSearch() {
        var form = document.querySelector(".search-model-form");
        if (!form || form.dataset.scBound) return;
        form.dataset.scBound = "1";
        form.addEventListener("submit", function (e) {
            e.preventDefault();
            var input = form.querySelector("#search-input, input");
            var kw = input ? (input.value || "").trim() : "";
            window.location.href = "/shop.html" + (kw ? ("?keyword=" + encodeURIComponent(kw)) : "");
        });
        var close = document.querySelector(".search-close-switch");
        if (close) {
            close.addEventListener("click", function () { form.reset(); });
        }
    }

    // ============ Khởi tạo chung ============
    ScStore.init = function () {
        ScStore.updateCartBadge();
        ScStore.bindAddCartButtons(document);
        bindSearch();
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", ScStore.init);
    } else {
        ScStore.init();
    }

    window.ScStore = ScStore;
})();

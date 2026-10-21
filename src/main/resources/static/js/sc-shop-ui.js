/**
 * sc-shop-ui.js
 * Phần dùng chung cho các trang bán hàng: thẻ sản phẩm có nút yêu thích (trái tim),
 * thêm nhanh vào giỏ, ghi nhận sản phẩm đã xem. Cần nạp sau auth.js (và cart-api.js nếu muốn thêm giỏ).
 */
(function (global) {
    "use strict";

    var FAV_API = "/api/user/favorites";
    var favIds = null; // Set<number> hoặc null khi chưa tải / chưa đăng nhập

    function token() {
        var s = global.ScAuth && global.ScAuth.getSession ? global.ScAuth.getSession() : null;
        return (s && s.token) || localStorage.getItem("jwt_token") || "";
    }

    function isLoggedIn() { return !!token(); }

    function esc(str) {
        return String(str == null ? "" : str)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
    }

    function vnd(value) {
        var n = Number(value);
        return isNaN(n) ? "0₫" : n.toLocaleString("vi-VN") + "₫";
    }

    function authFetch(url, options) {
        options = options || {};
        var headers = Object.assign({ "Accept": "application/json" }, options.headers || {});
        var t = token();
        if (t) headers.Authorization = "Bearer " + t;
        if (options.body && !headers["Content-Type"]) headers["Content-Type"] = "application/json";
        return fetch(url, Object.assign({}, options, { headers: headers })).then(function (r) {
            if (r.status === 401 || r.status === 403) {
                throw new Error("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
            }
            return r.json().catch(function () { return {}; }).then(function (payload) {
                if (!r.ok || payload.success === false) {
                    throw new Error(payload.message || ("Yêu cầu thất bại (" + r.status + ")"));
                }
                return payload.data;
            });
        });
    }

    // ============ Yêu thích ============

    /** Tải danh sách ID đã thích (rỗng nếu chưa đăng nhập hoặc lỗi mạng) */
    function loadFavIds() {
        if (!isLoggedIn()) {
            favIds = new Set();
            return Promise.resolve(favIds);
        }
        return authFetch(FAV_API + "/ids").then(function (ids) {
            favIds = new Set((ids || []).map(Number));
            return favIds;
        }).catch(function () {
            favIds = new Set();
            return favIds;
        });
    }

    function isFav(id) { return !!favIds && favIds.has(Number(id)); }

    /** Đổi trạng thái yêu thích; trả Promise<boolean> là trạng thái mới */
    function toggleFav(id) {
        if (!isLoggedIn()) {
            window.location.href = "/login.html";
            return Promise.reject(new Error("Chưa đăng nhập"));
        }
        id = Number(id);
        var wasFav = isFav(id);
        return authFetch(FAV_API + "/" + id, { method: wasFav ? "DELETE" : "POST" }).then(function () {
            if (!favIds) favIds = new Set();
            if (wasFav) favIds.delete(id); else favIds.add(id);
            return !wasFav;
        });
    }

    function paintHeart(btn, on) {
        btn.classList.toggle("on", !!on);
        btn.setAttribute("aria-pressed", on ? "true" : "false");
        btn.title = on ? "Bỏ yêu thích" : "Thêm vào yêu thích";
        var icon = btn.querySelector("i");
        if (icon) icon.className = on ? "fa fa-heart" : "fa fa-heart-o";
    }

    // ============ Thẻ sản phẩm ============

    var STYLE_ID = "sc-shop-ui-style";
    function injectStyle() {
        if (document.getElementById(STYLE_ID)) return;
        var st = document.createElement("style");
        st.id = STYLE_ID;
        st.textContent =
            ".sc-card{cursor:pointer}" +
            ".sc-card .product__item__pic{position:relative}" +
            ".sc-fav-btn{position:absolute;top:10px;right:10px;z-index:3;width:36px;height:36px;border-radius:50%;" +
            "border:0;background:#fff;box-shadow:0 2px 8px rgba(0,0,0,.18);color:#999;font-size:16px;line-height:36px;" +
            "padding:0;cursor:pointer;transition:transform .15s,color .15s}" +
            ".sc-fav-btn:hover{transform:scale(1.1);color:#e53637}" +
            ".sc-fav-btn.on{color:#e53637}" +
            ".sc-stat{font-size:12px;color:#777;display:block;margin-top:2px}" +
            ".sc-stat .fa-star{color:#f7941d}";
        document.head.appendChild(st);
    }

    function ratingText(p) {
        var count = Number(p.reviewCount || 0);
        if (!count) return "";
        return '<i class="fa fa-star"></i> ' + esc(p.avgRating) + " (" + count + ")";
    }

    /** HTML một thẻ sản phẩm. colClass là class cột bootstrap bọc ngoài. */
    function cardHtml(p, colClass) {
        injectStyle();
        var inStock = (p.stock || 0) > 0;
        var needOptions = (p.sizesList && p.sizesList.length) || (p.color && String(p.color).trim());
        var stats = [];
        var rt = ratingText(p);
        if (rt) stats.push(rt);
        if (p.soldCount) stats.push("Đã bán " + p.soldCount);
        if (p.favoriteCount) stats.push(p.favoriteCount + " ♥");
        return '<div class="' + (colClass || "col-lg-3 col-md-4 col-sm-6") + '">' +
            '<div class="product__item sc-card" data-id="' + p.id + '">' +
            '<div class="product__item__pic set-bg" data-setbg="' +
            esc(p.imageUrl || "/img/product/product-1.jpg") + '">' +
            (!inStock ? '<span class="label">Hết hàng</span>' : "") +
            '<button type="button" class="sc-fav-btn' + (isFav(p.id) ? " on" : "") + '" data-id="' + p.id + '" ' +
            'aria-pressed="' + (isFav(p.id) ? "true" : "false") + '" title="Thêm vào yêu thích">' +
            '<i class="fa ' + (isFav(p.id) ? "fa-heart" : "fa-heart-o") + '"></i></button>' +
            "</div>" +
            '<div class="product__item__text">' +
            "<h6>" + esc(p.name) + "</h6>" +
            (inStock
                ? '<a href="#" class="add-cart sc-add-cart" data-id="' + p.id + '" data-needopt="' +
                  (needOptions ? "1" : "0") + '">+ Thêm vào giỏ</a>'
                : '<span class="text-muted" style="font-size:12px;">Tạm hết hàng</span>') +
            '<div class="rating"><h5>' + vnd(p.price) + "</h5></div>" +
            (stats.length ? '<span class="sc-stat">' + stats.join(" · ") + "</span>" : "") +
            "</div></div></div>";
    }

    function applyBackgrounds(container) {
        Array.prototype.forEach.call(container.querySelectorAll(".set-bg"), function (el) {
            var bg = el.getAttribute("data-setbg");
            if (bg) el.style.backgroundImage = 'url("' + bg + '")';
        });
    }

    function updateCartBadge() {
        if (!isLoggedIn() || !global.StoreChainCartAPI) return;
        global.StoreChainCartAPI.count().then(function (count) {
            var spans = document.querySelectorAll('a[href="./shopping-cart.html"] span, a[href="/shopping-cart.html"] span');
            Array.prototype.forEach.call(spans, function (span) { span.textContent = count == null ? 0 : count; });
        }).catch(function () { });
    }

    /**
     * Gắn xử lý click cho một vùng chứa thẻ: tim, thêm giỏ, mở chi tiết.
     * onFavChange(productId, isFavNow) gọi sau khi đổi tim (trang yêu thích dùng để gỡ thẻ).
     */
    function bindCards(container, onFavChange) {
        container.addEventListener("click", function (e) {
            var heart = e.target.closest(".sc-fav-btn");
            if (heart) {
                e.preventDefault();
                e.stopPropagation();
                var hid = Number(heart.getAttribute("data-id"));
                toggleFav(hid).then(function (now) {
                    // Cập nhật mọi tim cùng sản phẩm trong vùng chứa
                    Array.prototype.forEach.call(container.querySelectorAll('.sc-fav-btn[data-id="' + hid + '"]'),
                        function (b) { paintHeart(b, now); });
                    if (onFavChange) onFavChange(hid, now);
                }).catch(function (err) {
                    if (err && err.message !== "Chưa đăng nhập") alert(err.message);
                });
                return;
            }
            var add = e.target.closest(".sc-add-cart");
            if (add) {
                e.preventDefault();
                e.stopPropagation();
                var pid = Number(add.getAttribute("data-id"));
                if (!isLoggedIn()) { window.location.href = "/login.html"; return; }
                // Sản phẩm có size/màu phải chọn ở trang chi tiết
                if (add.getAttribute("data-needopt") === "1") {
                    window.location.href = "/product-detail.html?productId=" + pid;
                    return;
                }
                if (!global.StoreChainCartAPI) { alert("Module giỏ hàng chưa được tải. Vui lòng tải lại trang."); return; }
                global.StoreChainCartAPI.add(pid, 1, "", "").then(function () {
                    alert("Đã thêm sản phẩm vào giỏ hàng!");
                    updateCartBadge();
                }).catch(function (err) { alert(err.message || "Không thể thêm vào giỏ hàng."); });
                return;
            }
            var card = e.target.closest(".sc-card");
            if (card) {
                window.location.href = "/product-detail.html?productId=" + card.getAttribute("data-id");
            }
        });
    }

    /** Ghi nhận đã xem sản phẩm (bỏ qua lỗi, không chặn giao diện) */
    function recordView(productId) {
        if (!isLoggedIn()) return;
        authFetch("/api/user/recently-viewed/" + Number(productId), { method: "POST" }).catch(function () { });
    }

    // ============ Thanh toán ============

    var PAY_LABEL = { COD: "COD (thanh toán khi nhận hàng)", VNPAY: "VNPAY", MOMO: "MoMo" };
    var PAY_STATUS = {
        UNPAID: "Chưa thanh toán", PENDING: "Chờ thanh toán",
        PAID: "Đã thanh toán", FAILED: "Thanh toán thất bại"
    };

    /** Dòng mô tả phương thức + trạng thái thanh toán của một đơn */
    function paymentHtml(o) {
        var method = o.paymentMethod || "COD";
        var online = method !== "COD";
        var status = o.paymentStatus || "UNPAID";
        var color = status === "PAID" ? "text-success" : (status === "FAILED" ? "text-danger" : "text-muted");
        return '<div><small class="text-muted">Thanh toán: ' + esc(PAY_LABEL[method] || method) + "</small>" +
            (online ? ' <small class="' + color + '">— ' + esc(PAY_STATUS[status] || status) + "</small>" : "") +
            "</div>";
    }

    /** Đơn online chưa thanh toán xong và chưa bị hủy thì cho thanh toán lại */
    function canRetryPayment(o) {
        return !!o.paymentRef && o.paymentMethod && o.paymentMethod !== "COD"
            && o.paymentStatus !== "PAID" && o.status !== "CANCELLED";
    }

    /** Lấy URL cổng từ server rồi chuyển hướng trình duyệt */
    function startPayment(paymentRef) {
        return authFetch("/api/user/payments/init", {
            method: "POST",
            body: JSON.stringify({ paymentRef: paymentRef })
        }).then(function (data) {
            window.location.href = data.paymentUrl;
        });
    }

    global.ScShop = {
        paymentHtml: paymentHtml, canRetryPayment: canRetryPayment, startPayment: startPayment,
        token: token, isLoggedIn: isLoggedIn, esc: esc, vnd: vnd, authFetch: authFetch,
        loadFavIds: loadFavIds, isFav: isFav, toggleFav: toggleFav, paintHeart: paintHeart,
        cardHtml: cardHtml, applyBackgrounds: applyBackgrounds, bindCards: bindCards,
        recordView: recordView, updateCartBadge: updateCartBadge
    };
})(window);

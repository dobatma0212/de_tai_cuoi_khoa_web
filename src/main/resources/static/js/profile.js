/**
 * profile.js
 * Xử lý trang Hồ sơ cá nhân (profile.html):
 * - Xem / cập nhật thông tin cá nhân
 * - Đổi mật khẩu
 * - Quản lý nhiều địa chỉ nhận hàng (thêm / sửa / xóa / đặt mặc định)
 */
(function () {
    "use strict";

    var API_BASE = "/api/profile";

    // ============ Helper ============
    function getToken() {
        var s = window.ScAuth ? window.ScAuth.getSession() : null;
        return (s && s.token) || localStorage.getItem("jwt_token");
    }

    function authHeaders() {
        return {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + getToken()
        };
    }

    function handleResponse(res) {
        return res.json().then(function (data) {
            if (!res.ok || data.success === false) {
                throw new Error(data.message || "Đã có lỗi xảy ra, vui lòng thử lại.");
            }
            return data;
        });
    }

    function api(method, path, body) {
        return fetch(API_BASE + path, {
            method: method,
            headers: authHeaders(),
            body: body ? JSON.stringify(body) : undefined
        }).then(function (res) {
            if (res.status === 401 || res.status === 403) {
                if (window.ScAuth) window.ScAuth.clearSession();
                window.location.href = "/login.html";
                return new Promise(function () { }); // dừng chuỗi
            }
            return handleResponse(res);
        });
    }

    function showAlert(el, message, type) {
        if (window.ScAuth) {
            window.ScAuth.showAlert(el, message, type);
        } else {
            el.textContent = message;
            el.style.display = "block";
        }
        if (type === "success") {
            setTimeout(function () {
                el.style.display = "none";
            }, 4000);
        }
    }

    function hideAlert(el) {
        el.style.display = "none";
    }

    function escapeHtml(str) {
        return String(str == null ? "" : str)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
    }

    function $(id) {
        return document.getElementById(id);
    }

    // ============ Khởi tạo ============
    document.addEventListener("DOMContentLoaded", function () {
        if (!getToken()) {
            $("profileNotLoggedIn").style.display = "block";
            return;
        }
        $("profileWrapper").style.display = "block";
        initTabs();
        loadProfile();
        loadAddresses();

        $("infoForm").addEventListener("submit", onSaveInfo);
        $("passwordForm").addEventListener("submit", onChangePassword);
        $("addressForm").addEventListener("submit", onSaveAddress);
        $("btnShowAddAddress").addEventListener("click", showAddAddressForm);
        $("btnCancelAddress").addEventListener("click", hideAddressForm);

        initShopSubTabs();
        initActivityTabs();
        openTabFromUrl();
    });

    // ============ Tab điều hướng ============
    function initTabs() {
        var buttons = document.querySelectorAll(".tab-btn");
        buttons.forEach(function (btn) {
            btn.addEventListener("click", function () {
                buttons.forEach(function (b) { b.classList.remove("active"); });
                document.querySelectorAll(".profile-tab-content").forEach(function (c) {
                    c.classList.remove("active");
                });
                btn.classList.add("active");
                $(btn.getAttribute("data-tab")).classList.add("active");
                var tabId = btn.getAttribute("data-tab");
                if (tabId === "tabUserOrders") loadUserOrders();
                if (tabId === "tabFavorites") loadFavoriteTab();
                if (tabId === "tabViewed") loadViewedTab();
            });
        });
    }

    // ============ Yêu thích / Đã xem ============

    function initActivityTabs() {
        // Bỏ tim ở tab yêu thích thì tải lại danh sách để thẻ biến mất
        ScShop.bindCards($("favGrid"), function (id, nowFav) { if (!nowFav) loadFavoriteTab(); });
        ScShop.bindCards($("viewedGrid"));
        $("btnClearViewed").addEventListener("click", function () {
            if (!confirm("Xóa toàn bộ lịch sử sản phẩm đã xem?")) return;
            ScShop.authFetch("/api/user/recently-viewed", { method: "DELETE" })
                .then(loadViewedTab)
                .catch(function (err) { alert(err.message); });
        });
    }

    function renderProductGrid(gridId, list, emptyText) {
        var grid = $(gridId);
        if (!list || !list.length) {
            grid.innerHTML = '<div class="col-12"><p class="text-muted">' + emptyText + "</p></div>";
            return;
        }
        grid.innerHTML = list.map(function (p) {
            return ScShop.cardHtml(p, "col-lg-4 col-md-6 col-sm-6");
        }).join("");
        ScShop.applyBackgrounds(grid);
    }

    function loadFavoriteTab() {
        ScShop.loadFavIds().then(function () {
            return ScShop.authFetch("/api/user/favorites");
        }).then(function (list) {
            renderProductGrid("favGrid", list, "Bạn chưa thích sản phẩm nào. Bấm biểu tượng trái tim ở sản phẩm để lưu lại.");
        }).catch(function (err) {
            $("favGrid").innerHTML = '<div class="col-12"><p class="text-danger">' + escapeHtml(err.message) + "</p></div>";
        });
    }

    function loadViewedTab() {
        ScShop.loadFavIds().then(function () {
            return ScShop.authFetch("/api/user/recently-viewed?limit=24");
        }).then(function (list) {
            renderProductGrid("viewedGrid", list, "Bạn chưa xem sản phẩm nào.");
        }).catch(function (err) {
            $("viewedGrid").innerHTML = '<div class="col-12"><p class="text-danger">' + escapeHtml(err.message) + "</p></div>";
        });
    }

    /** Mở tab theo ?tab=orders|favorites|viewed|address và báo kết quả thanh toán (?payment=success|failed) */
    function openTabFromUrl() {
        var params = new URLSearchParams(window.location.search);
        var map = { orders: "tabUserOrders", favorites: "tabFavorites", viewed: "tabViewed", address: "tabAddress" };
        var tab = map[params.get("tab")];
        if (tab) {
            var btn = document.querySelector('.tab-btn[data-tab="' + tab + '"]');
            if (btn) btn.click();
        }
        var pay = params.get("payment");
        if (pay) {
            var ref = params.get("ref");
            var reason = params.get("reason");
            var el = $("userOrderAlert");
            if (pay === "success") {
                showAlert(el, "Thanh toán thành công! Chi nhánh sẽ xác nhận đơn của bạn sớm.", "success");
            } else {
                showAlert(el, reason === "invalid_signature"
                    ? "Không xác thực được kết quả thanh toán. Nếu bạn đã bị trừ tiền, vui lòng liên hệ cửa hàng."
                    : "Thanh toán chưa hoàn tất. Bạn có thể bấm \"Thanh toán lại\" ở đơn hàng bên dưới.", "danger");
            }
        }
    }

    // ============ Thông tin cá nhân ============
    function loadProfile() {
        api("GET", "/me").then(function (res) {
            var u = res.data;
            $("pUsername").value = u.username;
            $("pFullName").textContent = u.fullName;
            $("pFullNameInput").value = u.fullName;
            $("pEmail").value = u.email;
            $("pPhone").value = u.phone || "";
            $("pAvatarUrl").value = u.avatarUrl || "";
            $("pRole").textContent = u.roleDescription || u.roleName || "";
            if (u.avatarUrl) {
                $("pAvatar").src = u.avatarUrl;
            }
            loadProfileForShop(u);
        }).catch(function (err) {
            showAlert($("infoAlert"), err.message, "error");
        });
    }

    function onSaveInfo(e) {
        e.preventDefault();
        hideAlert($("infoAlert"));
        api("PUT", "/me", {
            fullName: $("pFullNameInput").value.trim(),
            email: $("pEmail").value.trim(),
            phone: $("pPhone").value.trim(),
            avatarUrl: $("pAvatarUrl").value.trim()
        }).then(function (res) {
            showAlert($("infoAlert"), "Cập nhật hồ sơ thành công!", "success");
            loadProfile();
            // Cập nhật lại tên hiển thị trên header
            if (window.ScAuth) {
                var s = window.ScAuth.getSession();
                if (s) {
                    if (s.user && typeof s.user === "object") {
                        s.user.fullName = res.data.fullName;
                        s.user.email = res.data.email;
                    } else {
                        s.fullName = res.data.fullName;
                        s.email = res.data.email;
                    }
                    localStorage.setItem("sc_auth", JSON.stringify(s));
                    localStorage.setItem("user_info", JSON.stringify(s));
                }
                window.ScAuth.updateHeaderAuthUI();
            }
        }).catch(function (err) {
            showAlert($("infoAlert"), err.message, "error");
        });
    }

    // ============ Đổi mật khẩu ============
    function onChangePassword(e) {
        e.preventDefault();
        hideAlert($("passwordAlert"));
        api("PUT", "/me/password", {
            currentPassword: $("curPass").value,
            newPassword: $("newPass").value,
            confirmNewPassword: $("confirmPass").value
        }).then(function () {
            showAlert($("passwordAlert"), "Đổi mật khẩu thành công!", "success");
            $("passwordForm").reset();
        }).catch(function (err) {
            showAlert($("passwordAlert"), err.message, "error");
        });
    }

    // ============ Địa chỉ nhận hàng ============
    function loadAddresses() {
        api("GET", "/addresses").then(function (res) {
            renderAddresses(res.data || []);
        }).catch(function (err) {
            showAlert($("addressAlert"), err.message, "error");
        });
    }

    function renderAddresses(list) {
        var container = $("addressList");
        if (!list.length) {
            container.innerHTML = '<p class="text-muted">Bạn chưa có địa chỉ nhận hàng nào. Hãy thêm địa chỉ đầu tiên của mình!</p>';
            return;
        }
        var html = list.map(function (a) {
            var ward = a.ward ? ", " + escapeHtml(a.ward) : "";
            return '<div class="address-card' + (a.isDefault ? " is-default" : "") + '">' +
                '<div class="d-flex justify-content-between align-items-start flex-wrap">' +
                "<div>" +
                '<h6 class="font-weight-bold mb-1">' + escapeHtml(a.receiverName) +
                (a.isDefault ? ' <span class="badge-default">Mặc định</span>' : "") +
                "</h6>" +
                '<p class="mb-1"><i class="fa fa-phone"></i> ' + escapeHtml(a.receiverPhone) + "</p>" +
                '<p class="mb-0 text-muted"><i class="fa fa-map-marker"></i> ' +
                escapeHtml(a.detailAddress) + ward + ", " + escapeHtml(a.district) + ", " + escapeHtml(a.province) +
                "</p></div>" +
                '<div class="mt-2">' +
                (a.isDefault ? "" : '<button type="button" class="btn btn-sm btn-outline-store" data-default="' + a.id + '">Đặt mặc định</button> ') +
                '<button type="button" class="btn btn-sm btn-outline-secondary" data-edit="' + a.id + '">Sửa</button> ' +
                '<button type="button" class="btn btn-sm btn-link btn-link-danger" data-delete="' + a.id + '">Xóa</button>' +
                "</div></div></div>";
        }).join("");

        container.innerHTML = html;
        container.removeEventListener("click", onAddressListClick);
        container.addEventListener("click", onAddressListClick);
    }

    function onAddressListClick(e) {
        var btn = e.target.closest("button");
        if (!btn) return;
        var id;
        if (btn.getAttribute("data-default") !== null) {
            id = btn.getAttribute("data-default");
            api("PATCH", "/addresses/" + id + "/default").then(function () {
                showAlert($("addressAlert"), "Đã đặt làm địa chỉ mặc định!", "success");
                loadAddresses();
            }).catch(function (err) { showAlert($("addressAlert"), err.message, "error"); });
        } else if (btn.getAttribute("data-edit") !== null) {
            id = btn.getAttribute("data-edit");
            startEditAddress(parseInt(id, 10));
        } else if (btn.getAttribute("data-delete") !== null) {
            id = btn.getAttribute("data-delete");
            if (!confirm("Bạn có chắc muốn xóa địa chỉ này?")) return;
            api("DELETE", "/addresses/" + id).then(function () {
                showAlert($("addressAlert"), "Xóa địa chỉ thành công!", "success");
                loadAddresses();
            }).catch(function (err) { showAlert($("addressAlert"), err.message, "error"); });
        }
    }

    function showAddAddressForm() {
        $("addressForm").reset();
        $("addrId").value = "";
        $("addressFormTitle").textContent = "Thêm địa chỉ mới";
        $("addressFormCard").style.display = "block";
        $("addressFormCard").scrollIntoView({ behavior: "smooth" });
    }

    function hideAddressForm() {
        $("addressFormCard").style.display = "none";
    }

    function startEditAddress(id) {
        api("GET", "/addresses").then(function (res) {
            var a = (res.data || []).find(function (x) { return x.id === id; });
            if (!a) return;
            $("addrId").value = a.id;
            $("addrName").value = a.receiverName;
            $("addrPhone").value = a.receiverPhone;
            $("addrProvince").value = a.province;
            $("addrDistrict").value = a.district;
            $("addrWard").value = a.ward || "";
            $("addrDetail").value = a.detailAddress;
            $("addrDefault").checked = !!a.isDefault;
            $("addressFormTitle").textContent = "Cập nhật địa chỉ";
            $("addressFormCard").style.display = "block";
            $("addressFormCard").scrollIntoView({ behavior: "smooth" });
        });
    }

    function onSaveAddress(e) {
        e.preventDefault();
        hideAlert($("addressAlert"));
        var payload = {
            receiverName: $("addrName").value.trim(),
            receiverPhone: $("addrPhone").value.trim(),
            province: $("addrProvince").value.trim(),
            district: $("addrDistrict").value.trim(),
            ward: $("addrWard").value.trim(),
            detailAddress: $("addrDetail").value.trim(),
            isDefault: $("addrDefault").checked
        };
        var id = $("addrId").value;
        var request = id
            ? api("PUT", "/addresses/" + id, payload)
            : api("POST", "/addresses", payload);

        request.then(function () {
            showAlert($("addressAlert"), id ? "Cập nhật địa chỉ thành công!" : "Thêm địa chỉ thành công!", "success");
            hideAddressForm();
            loadAddresses();
        }).catch(function (err) {
            showAlert($("addressAlert"), err.message, "error");
        });
    }

    // ==================================================================
    // ============ Quản lý chi nhánh (chỉ hiển thị cho Manager) =========
    // ==================================================================

    var MANAGER_API = "/api/manager/shop";
    var MGR_BASE = "/api/manager";
    var currentShop = null;
    var categories = [];
    var currentCategoryId = null;

    /** Giống api() nhưng dùng base URL cho API quản lý chi nhánh */
    function mgrApi(method, path, body) {
        return fetch(MANAGER_API + path, {
            method: method,
            headers: authHeaders(),
            body: body ? JSON.stringify(body) : undefined
        }).then(function (res) {
            if (res.status === 401 || res.status === 403) {
                if (window.ScAuth) window.ScAuth.clearSession();
                window.location.href = "/login.html";
                return new Promise(function () { }); // dừng chuỗi
            }
            return handleResponse(res);
        });
    }

    /** Base /api/manager cho đơn hàng, khuyến mãi, vận chuyển, doanh thu */
    function mgrBaseApi(method, path, body) {
        return fetch(MGR_BASE + path, {
            method: method,
            headers: authHeaders(),
            body: body ? JSON.stringify(body) : undefined
        }).then(function (res) {
            if (res.status === 401 || res.status === 403) {
                if (window.ScAuth) window.ScAuth.clearSession();
                window.location.href = "/login.html";
                return new Promise(function () { });
            }
            return handleResponse(res);
        });
    }

    function formatVnd(value) {
        var n = Number(value);
        if (isNaN(n)) return "0₫";
        return n.toLocaleString("vi-VN") + "₫";
    }

    /** Chỉ role MANAGER (hoặc ADMIN phụ trách) mới thấy tab Quản lý chi nhánh */
    function initShopSection(roleName) {
        var r = String(roleName || "").toUpperCase();
        if (r.indexOf("MANAGER") === -1) return;
        $("tabShopBtn").style.display = "";
        mgrApi("GET", "").then(function (res) {
            currentShop = res.data;
            $("shopNoShop").style.display = "none";
            $("shopManagerWrapper").style.display = "block";
            fillShopInfo(currentShop);
            loadCategories();
        }).catch(function (err) {
            $("shopManagerWrapper").style.display = "none";
            $("shopNoShop").style.display = "block";
            $("shopNoShop").textContent = err.message;
        });
    }

    function loadProfileForShop(u) {
        initShopSection(u.roleName || u.roleDescription || "");
    }

    function initShopSubTabs() {
        document.querySelectorAll(".shop-subbtn").forEach(function (btn) {
            btn.addEventListener("click", function () {
                document.querySelectorAll(".shop-subbtn").forEach(function (b) { b.classList.remove("active"); });
                document.querySelectorAll(".shop-subcontent").forEach(function (c) { c.classList.remove("active"); });
                btn.classList.add("active");
                $(btn.getAttribute("data-sub")).classList.add("active");
                // Load dữ liệu theo sub-tab vừa mở
                var sub = btn.getAttribute("data-sub");
                if (sub === "shopSubOrders") loadOrders();
                else if (sub === "shopSubPromos") loadPromotions();
                else if (sub === "shopSubTransporters") loadTransporters();
                else if (sub === "shopSubRevenue") loadRevenue();
            });
        });
        $("promoForm").addEventListener("submit", onSavePromo);
        $("btnCancelPromo").addEventListener("click", resetPromoForm);
    }

    function fillShopInfo(shop) {
        $("shopName").value = shop.name || "";
        $("shopPhone").value = shop.phone || "";
        $("shopAddress").value = shop.address || "";
        $("shopDescription").value = shop.description || "";
    }

    // ---------- Danh mục sản phẩm ----------

    function loadCategories() {
        mgrApi("GET", "/categories").then(function (res) {
            categories = res.data || [];
            // Giữ lựa chọn hiện tại nếu vẫn còn, ngược lại chọn danh mục đầu tiên
            if (currentCategoryId === null ||
                !categories.some(function (c) { return c.id === currentCategoryId; })) {
                currentCategoryId = categories.length ? categories[0].id : null;
            }
            renderCategoryChips();
            loadProducts();
        }).catch(function (err) {
            showAlert($("productAlert"), err.message, "error");
        });
    }

    function renderCategoryChips() {
        var container = $("categoryChips");
        if (!categories.length) {
            container.innerHTML = '<div class="order-filter-item" style="color:#999;font-weight:400;">Chưa có danh mục nào</div>';
            return;
        }
        container.innerHTML = categories.map(function (c) {
            var active = c.id === currentCategoryId ? " active" : "";
            return '<div class="order-filter-item' + active + '" data-cat="' + c.id + '">' +
                "<span>" + escapeHtml(c.name) +
                ' <small class="text-muted">(' + c.totalProducts + ")</small></span>" +
                "</div>";
        }).join("");

        container.onclick = onCategoryChipClick;
    }

    function onCategoryChipClick(e) {
        // Bấm vào dòng: chọn danh mục để xem sản phẩm (manager chỉ xem)
        var item = e.target.closest(".order-filter-item");
        if (!item || !item.getAttribute("data-cat")) return;
        currentCategoryId = parseInt(item.getAttribute("data-cat"), 10);
        renderCategoryChips();
        loadProducts();
    }

    // ---------- Sản phẩm (manager chỉ được xem) ----------

    function loadProducts() {
        if (currentCategoryId === null) {
            $("productListTitle").textContent = "Danh sách sản phẩm";
            $("productList").innerHTML = '<p class="text-muted">Hãy chọn một danh mục để xem sản phẩm.</p>';
            return;
        }
        mgrApi("GET", "/products?categoryId=" + currentCategoryId).then(function (res) {
            renderProducts(res.data || []);
        }).catch(function (err) {
            showAlert($("productAlert"), err.message, "error");
        });
    }

    function currentCategoryName() {
        var c = categories.find(function (x) { return x.id === currentCategoryId; });
        return c ? c.name : "";
    }

    /** Dòng "Tồn theo size: S: 5 · M: 0 · ..." — chỉ hiện với sản phẩm phân size */
    function renderSizeStocks(p) {
        var ss = p.branchSizeStocks;
        if (!ss || !Object.keys(ss).length || !p.sizesList || !p.sizesList.length) return "";
        return '<small class="text-muted d-block">Tồn theo size: ' +
            p.sizesList.filter(function (s) { return ss.hasOwnProperty(s); })
                .map(function (s) {
                    var q = ss[s] || 0;
                    return escapeHtml(s) + ': <b style="color:' + (q > 0 ? "#28a745" : "#e53637") + ';">' + q + "</b>";
                }).join(" · ") + "</small>";
    }

    function renderProducts(list) {
        var container = $("productList");
        $("productListTitle").textContent = "Danh sách sản phẩm — " + currentCategoryName() +
            " (" + list.length + ")";
        if (!list.length) {
            container.innerHTML = '<p class="text-muted">Chưa có sản phẩm nào trong danh mục này.</p>';
            return;
        }
        container.innerHTML = list.map(function (p) {
            var stock = (p.branchStock !== undefined && p.branchStock !== null) ? p.branchStock : (p.stock || 0);
            // Thuộc tính đồng bộ từ phần quản trị (admin): status, SKU, brand, collection, nhãn, biến thể
            var statusMap = { active: ["status-active", "Đang mở bán"], draft: ["status-draft", "Bản nháp"], inactive: ["status-inactive", "Ngừng kinh doanh"] };
            var st = statusMap[p.status] || statusMap.active;
            var sizes = p.sizesList && p.sizesList.length ? p.sizesList.join(", ") : (p.sizes || "");
            var labels = "";
            if (p.isNew) labels += ' <span class="badge-mgr-label">Mới</span>';
            if (p.isBestSeller) labels += ' <span class="badge-mgr-label">Bán chạy</span>';
            if (p.isFeatured) labels += ' <span class="badge-mgr-label">Nổi bật</span>';
            return '<div class="product-card-mgr' + (p.representative ? " is-representative" : "") + '">' +
                '<img src="' + escapeHtml(p.imageUrl || "/img/product/product-1.jpg") + '" alt="' + escapeHtml(p.name) + '">' +
                "<div style='flex:1;'>" +
                '<h6 class="font-weight-bold mb-1">' + escapeHtml(p.name) +
                (p.representative ? ' <span class="badge-representative">Đại diện danh mục</span>' : "") +
                ' <span class="badge-status ' + st[0] + '">' + st[1] + "</span>" + labels + "</h6>" +
                '<div style="color:#e53637;font-weight:700;">' + formatVnd(p.price) + "</div>" +
                '<small class="text-muted d-block">SKU: ' + escapeHtml(p.baseSku || "—") +
                (p.brand ? " · Thương hiệu: " + escapeHtml(p.brand) : "") +
                (p.collectionName ? " · Bộ sưu tập: " + escapeHtml(p.collectionName) : "") + "</small>" +
                '<small class="text-muted">Tồn kho tại chi nhánh: <b style="color:' +
                (stock > 0 ? "#28a745" : "#e53637") + ';">' + stock + "</b>" +
                (stock > 0 ? "" : ' <span class="badge badge-danger">Hết hàng</span>') + "</small>" +
                renderSizeStocks(p) +
                (sizes ? '<small class="text-muted d-block">Size: ' + escapeHtml(sizes) + "</small>" : "") +
                (p.color ? '<small class="text-muted d-block">Màu: ' + escapeHtml(p.color) + "</small>" : "") +
                (p.variants && p.variants.length
                    ? '<small class="text-muted d-block">Biến thể: ' + p.variants.length +
                      " (" + p.variants.map(function (v) { return escapeHtml(v.sku || ((v.color || "") + "/" + (v.size || ""))); }).join(", ") + ")</small>"
                    : "") +
                '<p class="text-muted mb-0" style="font-size:13px;">' + escapeHtml(p.description || "") + "</p>" +
                "</div></div>";
        }).join("");
    }

    // ==================================================================
    // ========= Quản lý đơn hàng của chi nhánh (Manager) ================
    // ==================================================================

    var ORDER_STATUSES = [
        ["", "Tất cả"],
        ["NEW", "Đơn hàng mới"],
        ["CONFIRMED", "Đã xác nhận"],
        ["SHIPPING", "Đang giao"],
        ["DELIVERED", "Đã giao"],
        ["RECEIVED", "Đã nhận hàng"],
        ["CANCELLED", "Đã hủy"],
        ["RETURN", "Trả hàng - hoàn tiền"]
    ];
    var transporters = [];
    // Realtime (WebSocket): tu dong tai lai danh sach don khi co thay doi
    window.addEventListener("sc:order-event", function () {
        var el = $("orderList");
        if (el && el.offsetParent !== null) { loadOrders(); }
    });

    var currentOrderStatus = "";
    var orderCache = [];

    /** Tải danh sách nhà vận chuyển (cho dropdown xác nhận đơn) + toàn bộ đơn hàng */
    function loadOrders() {
        if (!transporters.length) {
            mgrBaseApi("GET", "/transporters").then(function (res) {
                transporters = res.data || [];
            }).catch(function () { transporters = []; }).then(loadOrderList);
            return;
        }
        loadOrderList();
    }

    function loadOrderList() {
        mgrBaseApi("GET", "/orders").then(function (res) {
            orderCache = res.data || [];
            renderOrderFilterList();
            renderOrders(filterOrders());
        }).catch(function (err) {
            showAlert($("orderAlert"), err.message, "error");
        });
    }

    function filterOrders() {
        if (!currentOrderStatus) return orderCache;
        // Chip "Trả hàng - hoàn tiền" gộp cả 2 trạng thái: đã trả + khách yêu cầu trả
        if (currentOrderStatus === "RETURN") {
            return orderCache.filter(function (o) {
                return o.status === "RETURNED" || o.status === "RETURN_REQUESTED";
            });
        }
        return orderCache.filter(function (o) { return o.status === currentOrderStatus; });
    }

    function renderOrderFilterList() {
        var container = $("orderStatusChips");
        container.innerHTML = ORDER_STATUSES.map(function (s) {
            var count = s[0] === ""
                ? orderCache.length
                : filterOrders().filter(function (o) { return s[0] === "RETURN" || o.status === s[0]; }).length;
            var active = currentOrderStatus === s[0] ? " active" : "";
            return '<div class="order-filter-item' + active + '" data-status="' + s[0] + '">' +
                "<span>" + s[1] + "</span>" +
                '<span class="of-count">' + count + "</span></div>";
        }).join("");
        container.onclick = function (e) {
            var item = e.target.closest(".order-filter-item");
            if (!item) return;
            currentOrderStatus = item.getAttribute("data-status");
            renderOrderFilterList();
            renderOrders(filterOrders());
        };
    }

    function renderOrders(list) {
        var container = $("orderList");
        if (!list.length) {
            container.innerHTML = '<p class="text-muted">Không có đơn hàng nào ở trạng thái này.</p>';
            return;
        }
        var statusColors = {
            NEW: "badge-primary", CONFIRMED: "badge-info",
            SHIPPING: "badge-warning", DELIVERED: "badge-success", RECEIVED: "badge-success",
            CANCELLED: "badge-danger", RETURNED: "badge-dark", RETURN_REQUESTED: "badge-warning"
        };
        container.innerHTML = list.map(function (o) {
            var items = (o.items || []).map(function (it) {
                return "<li>" + escapeHtml(it.productName) +
                    (it.size ? " (size " + escapeHtml(it.size) + ")" : "") +
                    " x" + it.quantity + " — " + formatVnd(it.subtotal) + "</li>";
            }).join("");
            // Nút thao tác của chi nhánh theo trạng thái đơn
            var actions = "";
            if (o.status === "NEW") {
                if (o.transporterId) {
                    // Khách đã chọn đơn vị vận chuyển lúc đặt hàng — chỉ hiển thị, không chọn lại
                    actions = '<div class="mt-2 d-flex flex-wrap align-items-center">' +
                        '<small class="mr-2 mb-1"><span class="badge badge-light" style="border:1px solid #ddd;">' +
                        'VC khách chọn: ' + escapeHtml(o.transporterName) + " (" + formatVnd(o.shippingFee) + ")</span></small>" +
                        '<button type="button" class="btn btn-sm btn-store mr-2 mb-1" data-confirm-order="' + o.id + '">Xác nhận đơn</button>' +
                        '<button type="button" class="btn btn-sm btn-link btn-link-danger mb-1" data-cancel-order="' + o.id + '">Hủy đơn</button>' +
                        "</div>";
                } else {
                    actions = '<div class="mt-2 d-flex flex-wrap align-items-center">' +
                        '<select class="form-control form-control-sm mr-2 mb-1 order-transporter-select" style="max-width:220px;">' +
                        '<option value="">— Chọn đơn vị vận chuyển —</option>' +
                        transporters.filter(function (t) { return t.status !== "inactive"; }).map(function (t) {
                            return '<option value="' + t.id + '">' + escapeHtml(t.name) +
                                " (" + formatVnd(t.baseFee) + ")</option>";
                        }).join("") +
                        "</select>" +
                        '<button type="button" class="btn btn-sm btn-store mr-2 mb-1" data-confirm-order="' + o.id + '">Xác nhận đơn</button>' +
                        '<button type="button" class="btn btn-sm btn-link btn-link-danger mb-1" data-cancel-order="' + o.id + '">Hủy đơn</button>' +
                        "</div>";
                }
            } else if (o.status === "CONFIRMED" || o.status === "SHIPPING") {
                actions = '<div class="mt-2">' +
                    '<small class="text-muted">Đơn đã được xác nhận — giao hàng tự động, khách có thể hủy giữa chừng.</small> ' +
                    '<button type="button" class="btn btn-sm btn-link btn-link-danger" data-cancel-order="' + o.id + '">Hủy đơn</button>' +
                    "</div>";
            } else if (o.status === "DELIVERED") {
                actions = '<div class="mt-2"><small class="text-muted">Đã giao — chờ khách xác nhận đã nhận hàng.</small> ' +
                    '<button type="button" class="btn btn-sm btn-outline-secondary" data-return-order="' + o.id + '">Trả hàng - hoàn tiền</button></div>';
            } else if (o.status === "RECEIVED") {
                actions = '<div class="mt-2"><small class="text-muted">Khách đã xác nhận nhận hàng.</small> ' +
                    '<button type="button" class="btn btn-sm btn-outline-secondary" data-return-order="' + o.id + '">Trả hàng - hoàn tiền</button></div>';
            } else if (o.status === "RETURN_REQUESTED") {
                actions = '<div class="mt-2"><small class="text-danger">Khách yêu cầu trả hàng - hoàn tiền.</small> ' +
                    '<button type="button" class="btn btn-sm btn-outline-store" data-approve-return="' + o.id + '">Duyệt trả hàng</button></div>';
            }
            return '<div class="address-card">' +
                '<div class="d-flex justify-content-between flex-wrap">' +
                "<div><b>" + escapeHtml(o.orderCode) + "</b> " +
                '<span class="badge ' + (statusColors[o.status] || "badge-light") + '">' +
                escapeHtml(o.statusLabel || o.status) + "</span>" +
                '<div><small class="text-muted">Khách: ' + escapeHtml(o.customerName || "") +
                " — " + escapeHtml(o.receiverPhone || "") + "</small></div>" +
                ScShop.paymentHtml(o) +
                '<div><small class="text-muted">' + escapeHtml(o.receiverAddress || "") + "</small></div>" +
                (o.transporterName ? '<div><small class="text-muted">VC: ' + escapeHtml(o.transporterName) +
                    " (" + formatVnd(o.shippingFee) + ")</small></div>" : "") +
                (o.promoName ? '<div><small class="text-success">KM: ' + escapeHtml(o.promoName) +
                    " -" + formatVnd(o.discountAmount) + "</small></div>" : "") +
                (o.cancelReason ? '<div><small class="text-danger">Lý do hủy/trả: ' + escapeHtml(o.cancelReason) + "</small></div>" : "") +
                "</div>" +
                '<div class="text-right"><b style="color:#e53637;">' + formatVnd(o.grandTotal) + "</b>" +
                '<div><small class="text-muted">' + escapeHtml(o.createdAt || "") + "</small></div>" +
                "</div></div>" +
                '<ul class="mt-2 mb-0 text-muted" style="font-size:13px;">' + items + "</ul>" +
                actions +
                "</div>";
        }).join("");

        container.onclick = function (e) {
            var btn = e.target.closest("button");
            if (!btn) return;
            var id, select, transporterId;
            if (btn.getAttribute("data-confirm-order") !== null) {
                id = btn.getAttribute("data-confirm-order");
                select = btn.closest(".address-card").querySelector(".order-transporter-select");
                transporterId = select ? select.value : "";
                if (select && !transporterId) {
                    alert("Vui lòng chọn đơn vị vận chuyển trước khi xác nhận.");
                    return;
                }
                // Đơn đã có vận chuyển khách chọn thì không gửi transporterId nữa
                var confirmUrl = "/orders/" + id + "/status?status=CONFIRMED" +
                    (transporterId ? "&transporterId=" + transporterId : "");
                mgrBaseApi("PATCH", confirmUrl)
                    .then(function () {
                        showAlert($("orderAlert"), "Đã xác nhận đơn hàng! Đơn sẽ tự động chuyển sang đang giao.", "success");
                        loadOrders();
                    }).catch(function (err) { alert(err.message); });
            } else if (btn.getAttribute("data-cancel-order") !== null) {
                id = btn.getAttribute("data-cancel-order");
                var reason = prompt("Lý do hủy đơn (VD: tồn kho chi nhánh đã hết):");
                if (reason === null) return;
                if (!reason.trim()) { alert("Vui lòng nhập lý do hủy đơn."); return; }
                mgrBaseApi("PATCH", "/orders/" + id + "/status?status=CANCELLED&reason=" + encodeURIComponent(reason.trim()))
                    .then(function () {
                        showAlert($("orderAlert"), "Đã hủy đơn hàng và hoàn tồn kho về chi nhánh.", "success");
                        loadOrders();
                    }).catch(function (err) { alert(err.message); });
            } else if (btn.getAttribute("data-return-order") !== null) {
                id = btn.getAttribute("data-return-order");
                var returnReason = prompt("Lý do trả hàng - hoàn tiền (khách phản hồi hàng có vấn đề):");
                if (returnReason === null) return;
                var q = returnReason.trim() ? "&reason=" + encodeURIComponent(returnReason.trim()) : "";
                mgrBaseApi("PATCH", "/orders/" + id + "/status?status=RETURNED" + q)
                    .then(function () {
                        showAlert($("orderAlert"), "Đã cập nhật trả hàng - hoàn tiền.", "success");
                        loadOrders();
                    }).catch(function (err) { alert(err.message); });
            } else if (btn.getAttribute("data-approve-return") !== null) {
                id = btn.getAttribute("data-approve-return");
                if (!confirm("Duyệt trả hàng - hoàn tiền cho đơn này?")) return;
                mgrBaseApi("PATCH", "/orders/" + id + "/status?status=RETURNED")
                    .then(function () {
                        showAlert($("orderAlert"), "Đã duyệt trả hàng - hoàn tiền (tồn kho đã hoàn về chi nhánh).", "success");
                        loadOrders();
                    }).catch(function (err) { alert(err.message); });
            }
        };
    }

    // ==================================================================
    // ========= Chương trình khuyến mãi (Manager) =======================
    // ==================================================================

    function loadPromotions() {
        mgrBaseApi("GET", "/promotions").then(function (res) {
            renderPromotions(res.data || []);
        }).catch(function (err) {
            showAlert($("promoAlert"), err.message, "error");
        });
    }

    function renderPromotions(list) {
        var container = $("promoList");
        if (!list.length) {
            container.innerHTML = '<p class="text-muted">Chưa có chương trình khuyến mãi nào.</p>';
            return;
        }
        container.innerHTML = list.map(function (p) {
            var valueText = p.promoType === "PERCENT"
                ? "Giảm " + p.value + "%"
                : "Giảm " + formatVnd(p.value) + " phí vận chuyển";
            return '<div class="address-card' + (p.active ? " is-default" : "") + '">' +
                '<div class="d-flex justify-content-between align-items-start flex-wrap">' +
                "<div><b>" + escapeHtml(p.name) + "</b>" +
                (p.code ? ' <span class="badge-representative">' + escapeHtml(p.code) + "</span>" : "") +
                (p.active ? "" : ' <span class="badge badge-secondary">Tạm dừng</span>') +
                '<div><small class="text-muted">' + valueText +
                (p.minOrderAmount > 0 ? " — đơn tối thiểu " + formatVnd(p.minOrderAmount) : "") + "</small></div>" +
                '<div><small class="text-muted">' + escapeHtml(p.startDate || "") + " → " +
                escapeHtml(p.endDate || "không giới hạn") + "</small></div></div>" +
                '<div class="mt-2">' +
                '<button type="button" class="btn btn-sm btn-outline-store" data-toggle-promo="' + p.id + '">' +
                (p.active ? "Tạm dừng" : "Kích hoạt") + "</button> " +
                '<button type="button" class="btn btn-sm btn-outline-secondary" data-edit-promo="' + p.id + '">Sửa</button> ' +
                '<button type="button" class="btn btn-sm btn-link btn-link-danger" data-del-promo="' + p.id + '">Xóa</button>' +
                "</div></div></div>";
        }).join("");

        container.onclick = function (e) {
            var btn = e.target.closest("button");
            if (!btn) return;
            if (btn.getAttribute("data-toggle-promo") !== null) {
                var id = btn.getAttribute("data-toggle-promo");
                var makeActive = btn.textContent.trim() === "Kích hoạt";
                mgrBaseApi("PATCH", "/promotions/" + id + "/active?active=" + makeActive).then(function () {
                    showAlert($("promoAlert"), "Cập nhật trạng thái khuyến mãi thành công!", "success");
                    loadPromotions();
                }).catch(function (err) { alert(err.message); });
            } else if (btn.getAttribute("data-edit-promo") !== null) {
                startEditPromo(parseInt(btn.getAttribute("data-edit-promo"), 10));
            } else if (btn.getAttribute("data-del-promo") !== null) {
                if (!confirm("Xóa chương trình khuyến mãi này?")) return;
                mgrBaseApi("DELETE", "/promotions/" + btn.getAttribute("data-del-promo")).then(function () {
                    showAlert($("promoAlert"), "Xóa khuyến mãi thành công!", "success");
                    loadPromotions();
                }).catch(function (err) { alert(err.message); });
            }
        };
    }

    function startEditPromo(id) {
        mgrBaseApi("GET", "/promotions").then(function (res) {
            var p = (res.data || []).find(function (x) { return x.id === id; });
            if (!p) return;
            $("promoId").value = p.id;
            $("promoName").value = p.name || "";
            $("promoCode").value = p.code || "";
            $("promoType").value = p.promoType || "PERCENT";
            $("promoValue").value = p.value;
            $("promoMinOrder").value = p.minOrderAmount || 0;
            $("promoStart").value = toLocalInput(p.startDate);
            $("promoEnd").value = toLocalInput(p.endDate);
            $("promoActive").checked = !!p.active;
            $("promoFormTitle").textContent = "Cập nhật khuyến mãi";
            $("btnCancelPromo").style.display = "";
            $("promoFormCard").scrollIntoView({ behavior: "smooth" });
        });
    }

    function toLocalInput(dt) {
        if (!dt) return "";
        // "2026-09-16 15:30:00" -> "2026-09-16T15:30"
        return String(dt).replace(" ", "T").slice(0, 16);
    }

    function onSavePromo(e) {
        e.preventDefault();
        hideAlert($("promoAlert"));
        var payload = {
            name: $("promoName").value.trim(),
            code: $("promoCode").value.trim(),
            promoType: $("promoType").value,
            value: $("promoValue").value ? Number($("promoValue").value) : null,
            minOrderAmount: $("promoMinOrder").value ? Number($("promoMinOrder").value) : 0,
            // Định dạng khớp @JsonFormat "yyyy-MM-dd'T'HH:mm" của PromotionDto
            startDate: $("promoStart").value || null,
            endDate: $("promoEnd").value || null,
            active: $("promoActive").checked
        };
        var id = $("promoId").value;
        var request = id
            ? mgrBaseApi("PUT", "/promotions/" + id, payload)
            : mgrBaseApi("POST", "/promotions", payload);
        request.then(function () {
            showAlert($("promoAlert"), id ? "Cập nhật khuyến mãi thành công!" : "Tạo khuyến mãi thành công!", "success");
            resetPromoForm();
            loadPromotions();
        }).catch(function (err) {
            showAlert($("promoAlert"), err.message, "error");
        });
    }

    function resetPromoForm() {
        $("promoForm").reset();
        $("promoId").value = "";
        $("promoActive").checked = true;
        $("promoFormTitle").textContent = "Thêm chương trình khuyến mãi";
        $("btnCancelPromo").style.display = "none";
    }

    // ==================================================================
    // ========= Nhà vận chuyển (Manager - chỉ xem, do admin quản lý) ====
    // ==================================================================

    function loadTransporters() {
        mgrBaseApi("GET", "/transporters").then(function (res) {
            renderTransporters(res.data || []);
        }).catch(function (err) {
            showAlert($("transporterAlert"), err.message, "error");
        });
    }

    function renderTransporters(list) {
        var container = $("transporterList");
        if (!list.length) {
            container.innerHTML = '<p class="text-muted">Chưa có nhà vận chuyển nào.</p>';
            return;
        }
        container.innerHTML = list.map(function (t) {
            return '<div class="address-card">' +
                '<div class="d-flex justify-content-between align-items-start flex-wrap">' +
                "<div><b>" + escapeHtml(t.name) + "</b>" +
                (t.status === "inactive" ? ' <span class="badge badge-secondary">Tạm dừng</span>' : "") +
                '<div><small class="text-muted"><i class="fa fa-phone"></i> ' + escapeHtml(t.phone || "—") +
                (t.email ? " — " + escapeHtml(t.email) : "") + "</small></div>" +
                '<div><small class="text-muted">Phí vận chuyển: <b style="color:#e53637;">' +
                formatVnd(t.baseFee) + "</b></small></div></div>" +
                "</div></div>";
        }).join("");
    }

    // ==================================================================
    // ========= Doanh thu chi nhánh (Manager) ===========================
    // ==================================================================

    function loadRevenue() {
        mgrBaseApi("GET", "/revenue").then(function (res) {
            renderRevenue(res.data);
        }).catch(function (err) {
            $("revenueSummary").innerHTML = '<p class="text-muted">' + escapeHtml(err.message) + "</p>";
        });
    }

    function renderRevenue(r) {
        if (!r) return;
        var cards = [
            ["Tổng doanh thu (đã giao)", formatVnd(r.totalRevenue), "#e53637"],
            ["Tiền hàng đã giao", formatVnd(r.deliveredProductAmount), "#333"],
            ["Phí vận chuyển đã giao", formatVnd(r.deliveredShippingFee), "#333"],
            ["Khuyến mãi đã giảm", formatVnd(r.deliveredDiscount), "#c72b2c"],
            ["Phí VC đơn hủy/trả (bị trừ)", "-" + formatVnd(r.shippingLoss || 0), "#c72b2c"],
            ["Tổng đơn hàng", r.totalOrders, "#333"],
            ["Đơn đã giao", r.deliveredOrders, "#28a745"],
            ["Đơn bị hủy", r.cancelledOrders, "#dc3545"],
            ["Trả hàng/hoàn tiền", r.returnedOrders, "#6c757d"]
        ];
        var extraLabels = {
            RETURNED: "Trả hàng - hoàn tiền",
            RETURN_REQUESTED: "Yêu cầu trả hàng"
        };
        var statusRows = Object.keys(r.statusCounts || {}).map(function (k) {
            var label = extraLabels[k] || k;
            ORDER_STATUSES.forEach(function (s) { if (s[0] === k) label = s[1]; });
            return "<li>" + label + ": <b>" + r.statusCounts[k] + "</b> đơn</li>";
        }).join("");
        $("revenueSummary").innerHTML =
            '<div class="row">' + cards.map(function (c) {
                return '<div class="col-md-3 col-6 mb-3"><div class="address-card text-center">' +
                    '<small class="text-muted">' + c[0] + "</small>" +
                    '<div style="font-size:20px;font-weight:800;color:' + c[2] + ';">' + c[1] + "</div></div></div>";
            }).join("") + "</div>" +
            (statusRows ? "<h6>Chi tiết theo trạng thái</h6><ul>" + statusRows + "</ul>" : "");
    }

    // ==================================================================
    // ========= Đơn hàng của tôi (mọi người dùng) =======================
    // ==================================================================

    var USER_API = "/api/user";

    function userApi(method, path, body) {
        return fetch(USER_API + path, {
            method: method,
            headers: authHeaders(),
            body: body ? JSON.stringify(body) : undefined
        }).then(function (res) {
            if (res.status === 401 || res.status === 403) {
                if (window.ScAuth) window.ScAuth.clearSession();
                window.location.href = "/login.html";
                return new Promise(function () { });
            }
            return handleResponse(res);
        });
    }

    var userOrdersCache = [];
    var userOrderFilter = "ALL";
    var USER_ORDER_FILTERS = [
        ["ALL", "Tất cả"], ["NEW", "Đơn hàng mới"], ["CONFIRMED", "Đã xác nhận"], ["SHIPPING", "Đang giao"],
        ["DELIVERED", "Đã giao"], ["CANCELLED", "Đã hủy"], ["RETURN", "Trả hàng - hoàn tiền"]
    ];

    function orderMatchesFilter(o, f) {
        if (f === "ALL") return true;
        if (f === "SHIPPING") return o.status === "SHIPPING" || o.status === "PICKED_UP";
        if (f === "DELIVERED") return o.status === "DELIVERED" || o.status === "RECEIVED";
        if (f === "RETURN") return o.status === "RETURNED" || o.status === "RETURN_REQUESTED";
        return o.status === f;
    }

    function renderUserOrderStatusBar() {
        var bar = $("userOrderStatusBar");
        bar.innerHTML = USER_ORDER_FILTERS.map(function (f) {
            var count = userOrdersCache.filter(function (o) { return orderMatchesFilter(o, f[0]); }).length;
            var active = f[0] === userOrderFilter;
            return '<button type="button" class="btn btn-sm mr-1 mb-1 ' + (active ? "btn-store" : "btn-outline-secondary") +
                '" data-order-filter="' + f[0] + '">' + f[1] + " (" + count + ")</button>";
        }).join("");
        bar.onclick = function (e) {
            var b = e.target.closest("button[data-order-filter]");
            if (!b) return;
            userOrderFilter = b.getAttribute("data-order-filter");
            renderUserOrderStatusBar();
            renderUserOrders(userOrdersCache.filter(function (o) { return orderMatchesFilter(o, userOrderFilter); }));
        };
    }

    // Realtime (WebSocket): tu dong tai lai danh sach don khi co thay doi
    window.addEventListener("sc:order-event", function () {
        var el = $("userOrderList");
        if (el && el.offsetParent !== null) { loadUserOrders(); }
    });

    function loadUserOrders() {
        userApi("GET", "/orders").then(function (res) {
            userOrdersCache = res.data || [];
            renderUserOrderStatusBar();
            renderUserOrders(userOrdersCache.filter(function (o) { return orderMatchesFilter(o, userOrderFilter); }));
        }).catch(function (err) {
            $("userOrderList").innerHTML = '<p class="text-muted">' + escapeHtml(err.message) + "</p>";
        });
    }

    function renderUserOrders(list) {
        var container = $("userOrderList");
        if (!list.length) {
            container.innerHTML = '<p class="text-muted">' + (userOrdersCache.length
                ? "Không có đơn hàng nào ở trạng thái này." : "Bạn chưa có đơn hàng nào.") + "</p>";
            return;
        }
        container.innerHTML = list.map(function (o) {
            var canReview = o.status === "DELIVERED" || o.status === "RECEIVED";
            var items = (o.items || []).map(function (it) {
                return "<li>" + escapeHtml(it.productName) +
                    (it.size ? " (size " + escapeHtml(it.size) + ")" : "") +
                    " x" + it.quantity + " — " + formatVnd(it.subtotal) +
                    (canReview && it.productId
                        ? ' <a href="/product-detail.html?productId=' + it.productId + '">Đánh giá</a>' : "") +
                    "</li>";
            }).join("");
            var actions = "";
            if (["NEW", "CONFIRMED", "SHIPPING"].indexOf(o.status) !== -1) {
                actions = '<button type="button" class="btn btn-sm btn-link btn-link-danger" data-cancel="' + o.id + '">Hủy đơn</button>';
            } else if (o.status === "DELIVERED") {
                actions = '<button type="button" class="btn btn-sm btn-store mr-2" data-receive="' + o.id + '">Đã nhận được hàng</button>' +
                    '<button type="button" class="btn btn-sm btn-link btn-link-danger mr-2" data-cancel="' + o.id + '">Hủy đơn</button>' +
                    '<button type="button" class="btn btn-sm btn-outline-secondary" data-return="' + o.id + '">Yêu cầu trả hàng</button>';
            } else if (o.status === "RECEIVED") {
                actions = '<button type="button" class="btn btn-sm btn-outline-secondary" data-return="' + o.id + '">Yêu cầu trả hàng</button>';
            } else if (o.status === "RETURN_REQUESTED") {
                actions = '<small class="text-muted">Đang chờ chi nhánh duyệt trả hàng - hoàn tiền...</small>';
            }
            if (ScShop.canRetryPayment(o)) {
                actions = '<button type="button" class="btn btn-sm btn-store mr-2" data-pay-ref="' +
                    escapeHtml(o.paymentRef) + '">Thanh toán lại</button>' + actions;
            }
            return '<div class="address-card">' +
                '<div class="d-flex justify-content-between flex-wrap">' +
                "<div><b>" + escapeHtml(o.orderCode) + "</b> " +
                '<span class="badge badge-light">' + escapeHtml(o.statusLabel || o.status) + "</span>" +
                '<div><small class="text-muted">' + escapeHtml(o.shopName || "") + " — " +
                escapeHtml(o.createdAt || "") + "</small></div>" +
                (o.transporterName ? '<div><small class="text-muted">VC: ' + escapeHtml(o.transporterName) + "</small></div>" : "") +
                ScShop.paymentHtml(o) +
                (o.cancelReason ? '<div><small class="text-danger">' + escapeHtml(o.cancelReason) + "</small></div>" : "") +
                '<ul class="mt-2 mb-0 text-muted" style="font-size:13px;">' + items + "</ul></div>" +
                '<div class="text-right"><b style="color:#e53637;">' + formatVnd(o.grandTotal) + "</b>" +
                '<div class="mt-2">' + actions + "</div></div>" +
                "</div></div>";
        }).join("");

        container.onclick = function (e) {
            var btn = e.target.closest("button");
            if (!btn) return;
            if (btn.getAttribute("data-pay-ref") !== null) {
                ScShop.startPayment(btn.getAttribute("data-pay-ref")).catch(function (err) { alert(err.message); });
            } else if (btn.getAttribute("data-cancel") !== null) {
                var id = btn.getAttribute("data-cancel");
                var reason = prompt("Lý do bạn muốn hủy đơn này:");
                if (reason === null) return;
                if (!reason.trim()) { alert("Vui lòng nhập lý do hủy đơn."); return; }
                userApi("PATCH", "/orders/" + id + "/cancel", { reason: reason.trim() }).then(function () {
                    showAlert($("userOrderAlert"), "Đã hủy đơn hàng!", "success");
                    loadUserOrders();
                }).catch(function (err) { alert(err.message); });
            } else if (btn.getAttribute("data-receive") !== null) {
                var rid = btn.getAttribute("data-receive");
                userApi("PATCH", "/orders/" + rid + "/receive", null).then(function () {
                    showAlert($("userOrderAlert"), "Cảm ơn bạn đã xác nhận nhận hàng!", "success");
                    loadUserOrders();
                }).catch(function (err) { alert(err.message); });
            } else if (btn.getAttribute("data-return") !== null) {
                var tid = btn.getAttribute("data-return");
                var rreason = prompt("Lý do trả hàng - hoàn tiền (VD: hàng lỗi, sai mẫu...):");
                if (rreason === null) return;
                if (!rreason.trim()) { alert("Vui lòng nhập lý do trả hàng."); return; }
                userApi("PATCH", "/orders/" + tid + "/return-request", { reason: rreason.trim() }).then(function () {
                    showAlert($("userOrderAlert"), "Đã gửi yêu cầu trả hàng tới chi nhánh!", "success");
                    loadUserOrders();
                }).catch(function (err) { alert(err.message); });
            }
        };
    }
})();

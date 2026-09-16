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
            });
        });
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
            $("btnViewShopHome").href = "/shop-home.html?shopId=" + currentShop.id;
            renderShopHomePreview();
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
        document.querySelectorAll(".prod-group-btn").forEach(function (btn) {
            btn.addEventListener("click", function () {
                document.querySelectorAll(".prod-group-btn").forEach(function (b) { b.classList.remove("active"); });
                btn.classList.add("active");
                currentProductGroup = btn.getAttribute("data-group");
                $("productGroup").value = currentProductGroup;
                resetProductForm();
                loadProducts();
            });
        });

        $("shopInfoForm").addEventListener("submit", onSaveShopInfo);
        $("productForm").addEventListener("submit", onSaveProduct);
        $("btnCancelProduct").addEventListener("click", resetProductForm);
        $("productImageUrl").addEventListener("input", function () {
            updateProductImagePreview($("productImageUrl").value.trim());
        });
        $("productImageFile").addEventListener("change", onPickProductFile);
        $("btnAddCategory").addEventListener("click", onAddCategory);
        $("newCategoryName").addEventListener("keydown", function (e) {
            if (e.key === "Enter") {
                e.preventDefault();
                onAddCategory();
            }
        });

        $("promoForm").addEventListener("submit", onSavePromo);
        $("btnCancelPromo").addEventListener("click", resetPromoForm);
        $("transporterForm").addEventListener("submit", onSaveTransporter);
        $("btnCancelTransporter").addEventListener("click", resetTransporterForm);
    }

    function fillShopInfo(shop) {
        $("shopName").value = shop.name || "";
        $("shopPhone").value = shop.phone || "";
        $("shopAddress").value = shop.address || "";
        $("shopDescription").value = shop.description || "";
        $("shopLogoUrl").value = shop.logoUrl || "";
        $("shopBannerUrl").value = shop.bannerUrl || "";
        if (shop.logoUrl) $("shopLogoPreview").src = shop.logoUrl;
        if (shop.bannerUrl) $("shopBannerPreview").src = shop.bannerUrl;
    }

    function onSaveShopInfo(e) {
        e.preventDefault();
        hideAlert($("shopInfoAlert"));
        mgrApi("PUT", "", {
            name: $("shopName").value.trim(),
            phone: $("shopPhone").value.trim(),
            address: $("shopAddress").value.trim(),
            description: $("shopDescription").value.trim(),
            logoUrl: $("shopLogoUrl").value.trim(),
            bannerUrl: $("shopBannerUrl").value.trim()
        }).then(function (res) {
            currentShop = res.data;
            showAlert($("shopInfoAlert"), "Cập nhật thông tin chi nhánh thành công!", "success");
            fillShopInfo(currentShop);
            renderShopHomePreview();
        }).catch(function (err) {
            showAlert($("shopInfoAlert"), err.message, "error");
        });
    }

    function renderShopHomePreview() {
        if (!currentShop) return;
        var container = $("shopHomePreview");
        container.innerHTML = '<div class="text-muted">Đang tải xem trước trang chủ...</div>';
        fetch("/api/public/shops/" + currentShop.id + "/home")
            .then(function (r) { return r.json(); })
            .then(function (data) {
                var blocks = (data.data && data.data.categories) || [];
                if (!blocks.length) {
                    container.innerHTML = '<p class="text-muted">Chưa có dữ liệu trang chủ.</p>';
                    return;
                }
                container.innerHTML = blocks.map(function (b) {
                    var p = b.representativeProduct;
                    return '<div class="address-card"><h6 class="font-weight-bold">' + escapeHtml(b.label) +
                        ' <small class="text-muted">(' + b.totalProducts + ' sản phẩm)</small></h6>' +
                        (p
                            ? '<div class="d-flex align-items-center mt-2">' +
                              '<img src="' + escapeHtml(p.imageUrl || "/img/product/product-1.jpg") + '" style="width:60px;height:60px;object-fit:cover;border-radius:6px;">' +
                              '<div class="ml-3"><b>' + escapeHtml(p.name) + '</b><br>' +
                              '<span style="color:#e53637;font-weight:700;">' + formatVnd(p.price) + '</span></div></div>'
                            : '<p class="text-muted mb-0">Chưa có sản phẩm đại diện — hãy thêm sản phẩm trong mục Quản lý sản phẩm.</p>') +
                        '</div>';
                }).join("");
            }).catch(function () {
                container.innerHTML = '<p class="text-muted">Không tải được xem trước trang chủ.</p>';
            });
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
            container.innerHTML = '<span class="text-muted">Chưa có danh mục nào — hãy thêm danh mục đầu tiên cho chi nhánh.</span>';
            return;
        }
        container.innerHTML = categories.map(function (c) {
            var active = c.id === currentCategoryId ? " active" : "";
            return '<span class="cat-chip' + active + '" data-cat="' + c.id + '">' +
                escapeHtml(c.name) + " (" + c.totalProducts + ")" +
                '<i class="fa fa-pencil chip-action" data-rename="' + c.id + '" title="Đổi tên"></i>' +
                '<i class="fa fa-times chip-action" data-delcat="' + c.id + '" title="Xóa danh mục"></i>' +
                "</span>";
        }).join("");

        container.onclick = onCategoryChipClick;
    }

    function onCategoryChipClick(e) {
        var chip = e.target.closest(".cat-chip");
        if (!chip) return;
        var catId = parseInt(chip.getAttribute("data-cat"), 10);
        var renameId = e.target.getAttribute("data-rename");
        var delId = e.target.getAttribute("data-delcat");
        if (renameId !== null) {
            var cat = categories.find(function (c) { return c.id === parseInt(renameId, 10); });
            var newName = prompt("Tên mới cho danh mục:", cat ? cat.name : "");
            if (newName === null) return;
            if (!newName.trim()) { alert("Tên danh mục không được để trống."); return; }
            mgrApi("PUT", "/categories/" + renameId, { name: newName.trim() }).then(function () {
                showAlert($("productAlert"), "Đổi tên danh mục thành công!", "success");
                loadCategories();
                renderShopHomePreview();
            }).catch(function (err) { alert(err.message); });
            return;
        }
        if (delId !== null) {
            if (!confirm("Xóa danh mục này? (Chỉ xóa được khi danh mục không còn sản phẩm)")) return;
            mgrApi("DELETE", "/categories/" + delId).then(function () {
                showAlert($("productAlert"), "Xóa danh mục thành công!", "success");
                if (currentCategoryId === parseInt(delId, 10)) currentCategoryId = null;
                loadCategories();
                renderShopHomePreview();
            }).catch(function (err) { alert(err.message); });
            return;
        }
        // Chọn danh mục để xem/thêm sản phẩm
        currentCategoryId = catId;
        renderCategoryChips();
        resetProductForm();
        loadProducts();
    }

    function onAddCategory() {
        var input = $("newCategoryName");
        var name = input.value.trim();
        if (!name) { alert("Vui lòng nhập tên danh mục."); return; }
        mgrApi("POST", "/categories", { name: name }).then(function () {
            input.value = "";
            showAlert($("productAlert"), "Thêm danh mục thành công!", "success");
            loadCategories();
            renderShopHomePreview();
        }).catch(function (err) {
            alert(err.message);
        });
    }

    // ---------- Sản phẩm ----------

    function loadProducts() {
        if (currentCategoryId === null) {
            $("productListTitle").textContent = "Danh sách sản phẩm";
            $("productList").innerHTML = '<p class="text-muted">Hãy thêm và chọn một danh mục để quản lý sản phẩm.</p>';
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

    function renderProducts(list) {
        var container = $("productList");
        $("productListTitle").textContent = "Danh sách sản phẩm — " + currentCategoryName() +
            " (" + list.length + ")";
        if (!list.length) {
            container.innerHTML = '<p class="text-muted">Chưa có sản phẩm nào trong danh mục này. Hãy thêm sản phẩm đầu tiên!</p>';
            return;
        }
        container.innerHTML = list.map(function (p) {
            return '<div class="product-card-mgr' + (p.representative ? " is-representative" : "") + '">' +
                '<img src="' + escapeHtml(p.imageUrl || "/img/product/product-1.jpg") + '" alt="' + escapeHtml(p.name) + '">' +
                "<div style='flex:1;'>" +
                '<h6 class="font-weight-bold mb-1">' + escapeHtml(p.name) +
                (p.representative ? ' <span class="badge-representative">Đại diện danh mục</span>' : "") + "</h6>" +
                '<div style="color:#e53637;font-weight:700;">' + formatVnd(p.price) + "</div>" +
                '<small class="text-muted">Tồn kho: <b>' + (p.stock || 0) + "</b>" +
                (p.sizes ? " — Size: " + escapeHtml(p.sizes) : "") + "</small>" +
                '<p class="text-muted mb-2" style="font-size:13px;">' + escapeHtml(p.description || "") + "</p>" +
                '<button type="button" class="btn btn-sm btn-outline-store" data-rep="' + p.id + '">' +
                (p.representative ? "Bỏ đại diện" : "Đặt làm đại diện") + "</button> " +
                '<button type="button" class="btn btn-sm btn-outline-secondary" data-edit="' + p.id + '">Sửa</button> ' +
                '<button type="button" class="btn btn-sm btn-link btn-link-danger" data-delete="' + p.id + '">Xóa</button>' +
                "</div></div>";
        }).join("");

        container.onclick = onProductListClick;
    }

    function onProductListClick(e) {
        var btn = e.target.closest("button");
        if (!btn) return;
        if (btn.getAttribute("data-rep") !== null) {
            var repId = btn.getAttribute("data-rep");
            var makeRep = btn.textContent.trim() !== "Bỏ đại diện";
            mgrApi("PATCH", "/products/" + repId + "/representative?representative=" + makeRep).then(function () {
                showAlert($("productAlert"), makeRep ? "Đã đặt làm sản phẩm đại diện!" : "Đã bỏ chọn đại diện.", "success");
                loadProducts();
                renderShopHomePreview();
            }).catch(function (err) { showAlert($("productAlert"), err.message, "error"); });
        } else if (btn.getAttribute("data-edit") !== null) {
            startEditProduct(parseInt(btn.getAttribute("data-edit"), 10));
        } else if (btn.getAttribute("data-delete") !== null) {
            var delId = btn.getAttribute("data-delete");
            if (!confirm("Bạn có chắc muốn xóa sản phẩm này?")) return;
            mgrApi("DELETE", "/products/" + delId).then(function () {
                showAlert($("productAlert"), "Xóa sản phẩm thành công!", "success");
                loadProducts();
                renderShopHomePreview();
            }).catch(function (err) { showAlert($("productAlert"), err.message, "error"); });
        }
    }

    function updateProductImagePreview(url) {
        var img = $("productImagePreview");
        if (url) {
            img.src = url;
            img.style.display = "block";
        } else {
            img.style.display = "none";
        }
    }

    function onPickProductFile() {
        var fileInput = $("productImageFile");
        if (!fileInput.files || !fileInput.files[0]) return;
        var fd = new FormData();
        fd.append("file", fileInput.files[0]);
        fetch(MANAGER_API + "/upload", {
            method: "POST",
            headers: { "Authorization": "Bearer " + getToken() },
            body: fd
        }).then(function (res) { return res.json(); }).then(function (data) {
            if (data.success === false) throw new Error(data.message || "Tải ảnh thất bại.");
            $("productImageUrl").value = data.data.url;
            updateProductImagePreview(data.data.url);
            showAlert($("productAlert"), "Tải ảnh lên thành công!", "success");
        }).catch(function (err) {
            showAlert($("productAlert"), err.message, "error");
        });
    }

    function resetProductForm() {
        $("productForm").reset();
        $("productId").value = "";
        $("productCategory").value = currentCategoryId === null ? "" : currentCategoryId;
        $("productStock").value = 0;
        $("productFormTitle").textContent = "Thêm sản phẩm mới";
        $("btnSaveProduct").textContent = "Lưu sản phẩm";
        $("btnCancelProduct").style.display = "none";
        updateProductImagePreview("");
    }

    function startEditProduct(id) {
        mgrApi("GET", "/products?categoryId=" + currentCategoryId).then(function (res) {
            var p = (res.data || []).find(function (x) { return x.id === id; });
            if (!p) return;
            $("productId").value = p.id;
            $("productGroup").value = p.categoryId;
            $("productName").value = p.name;
            $("productPrice").value = p.price;
            $("productStock").value = p.stock || 0;
            $("productSizes").value = p.sizes || "";
            $("productDescription").value = p.description || "";
            $("productImageUrl").value = p.imageUrl || "";
            updateProductImagePreview(p.imageUrl || "");
            $("productFormTitle").textContent = "Cập nhật sản phẩm";
            $("btnSaveProduct").textContent = "Lưu thay đổi";
            $("btnCancelProduct").style.display = "";
            $("productFormCard").scrollIntoView({ behavior: "smooth" });
        });
    }

    function onSaveProduct(e) {
        e.preventDefault();
        hideAlert($("productAlert"));
        if (currentCategoryId === null) {
            showAlert($("productAlert"), "Vui lòng chọn hoặc thêm một danh mục trước.", "error");
            return;
        }
        var payload = {
            name: $("productName").value.trim(),
            categoryId: currentCategoryId,
            price: $("productPrice").value ? Number($("productPrice").value) : null,
            stock: $("productStock").value ? Number($("productStock").value) : 0,
            sizes: $("productSizes").value.trim(),
            description: $("productDescription").value.trim(),
            imageUrl: $("productImageUrl").value.trim()
        };
        var id = $("productId").value;
        var request = id
            ? mgrApi("PUT", "/products/" + id, payload)
            : mgrApi("POST", "/products", payload);
        request.then(function () {
            showAlert($("productAlert"), id ? "Cập nhật sản phẩm thành công!" : "Thêm sản phẩm thành công!", "success");
            resetProductForm();
            loadProducts();
            renderShopHomePreview();
        }).catch(function (err) {
            showAlert($("productAlert"), err.message, "error");
        });
    }

    // ==================================================================
    // ========= Quản lý đơn hàng của chi nhánh (Manager) ================
    // ==================================================================

    var ORDER_STATUSES = [
        ["", "Tất cả"],
        ["NEW", "Đơn hàng mới"],
        ["CONFIRMED", "Đã xác nhận"],
        ["PICKED_UP", "Đã lấy hàng"],
        ["SHIPPING", "Đang giao"],
        ["DELIVERED", "Đã giao"],
        ["CANCELLED", "Hủy"],
        ["RETURNED", "Trả hàng - hoàn tiền"]
    ];
    var currentOrderStatus = "";

    function loadOrders() {
        var chips = $("orderStatusChips");
        chips.innerHTML = ORDER_STATUSES.map(function (s) {
            var active = currentOrderStatus === s[0] ? " active" : "";
            return '<button type="button" class="shop-subbtn order-status-btn' + active +
                '" data-status="' + s[0] + '">' + s[1] + "</button>";
        }).join("");
        chips.onclick = function (e) {
            var btn = e.target.closest(".order-status-btn");
            if (!btn) return;
            currentOrderStatus = btn.getAttribute("data-status");
            loadOrders();
        };
        var url = "/orders" + (currentOrderStatus ? "?status=" + currentOrderStatus : "");
        mgrBaseApi("GET", url).then(function (res) {
            renderOrders(res.data || []);
        }).catch(function (err) {
            showAlert($("orderAlert"), err.message, "error");
        });
    }

    function renderOrders(list) {
        var container = $("orderList");
        if (!list.length) {
            container.innerHTML = '<p class="text-muted">Không có đơn hàng nào ở trạng thái này.</p>';
            return;
        }
        var statusColors = {
            NEW: "badge-primary", CONFIRMED: "badge-info", PICKED_UP: "badge-secondary",
            SHIPPING: "badge-warning", DELIVERED: "badge-success",
            CANCELLED: "badge-danger", RETURNED: "badge-dark"
        };
        container.innerHTML = list.map(function (o) {
            var items = (o.items || []).map(function (it) {
                return "<li>" + escapeHtml(it.productName) +
                    (it.size ? " (size " + escapeHtml(it.size) + ")" : "") +
                    " x" + it.quantity + " — " + formatVnd(it.subtotal) + "</li>";
            }).join("");
            return '<div class="address-card">' +
                '<div class="d-flex justify-content-between flex-wrap">' +
                "<div><b>" + escapeHtml(o.orderCode) + "</b> " +
                '<span class="badge ' + (statusColors[o.status] || "badge-light") + '">' +
                escapeHtml(o.statusLabel || o.status) + "</span>" +
                '<div><small class="text-muted">Khách: ' + escapeHtml(o.customerName || "") +
                " — " + escapeHtml(o.receiverPhone || "") + "</small></div>" +
                '<div><small class="text-muted">' + escapeHtml(o.receiverAddress || "") + "</small></div>" +
                (o.transporterName ? '<div><small class="text-muted">VC: ' + escapeHtml(o.transporterName) +
                    " (" + formatVnd(o.shippingFee) + ")</small></div>" : "") +
                (o.promoName ? '<div><small class="text-success">KM: ' + escapeHtml(o.promoName) +
                    " -" + formatVnd(o.discountAmount) + "</small></div>" : "") +
                "</div>" +
                '<div class="text-right"><b style="color:#e53637;">' + formatVnd(o.grandTotal) + "</b>" +
                '<div><small class="text-muted">' + escapeHtml(o.createdAt || "") + "</small></div>" +
                '<select class="form-control form-control-sm mt-2 order-status-select" data-id="' + o.id + '">' +
                ORDER_STATUSES.slice(1).map(function (s) {
                    return '<option value="' + s[0] + '"' + (s[0] === o.status ? " selected" : "") +
                        ">" + s[1] + "</option>";
                }).join("") +
                "</select></div></div>" +
                '<ul class="mt-2 mb-0 text-muted" style="font-size:13px;">' + items + "</ul>" +
                "</div>";
        }).join("");

        container.onchange = function (e) {
            var select = e.target.closest(".order-status-select");
            if (!select) return;
            var id = select.getAttribute("data-id");
            var status = select.value;
            mgrBaseApi("PATCH", "/orders/" + id + "/status?status=" + status).then(function () {
                showAlert($("orderAlert"), "Cập nhật trạng thái đơn hàng thành công!", "success");
                loadOrders();
            }).catch(function (err) {
                alert(err.message);
                loadOrders();
            });
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
            startDate: $("promoStart").value ? $("promoStart").value + ":00" : null,
            endDate: $("promoEnd").value ? $("promoEnd").value + ":00" : null,
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
    // ========= Nhà vận chuyển (Manager) ================================
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
                '<div class="mt-2">' +
                '<button type="button" class="btn btn-sm btn-outline-secondary" data-edit-tr="' + t.id + '">Sửa</button> ' +
                '<button type="button" class="btn btn-sm btn-link btn-link-danger" data-del-tr="' + t.id + '">Xóa</button>' +
                "</div></div></div>";
        }).join("");

        container.onclick = function (e) {
            var btn = e.target.closest("button");
            if (!btn) return;
            if (btn.getAttribute("data-edit-tr") !== null) {
                startEditTransporter(parseInt(btn.getAttribute("data-edit-tr"), 10));
            } else if (btn.getAttribute("data-del-tr") !== null) {
                if (!confirm("Xóa nhà vận chuyển này?")) return;
                mgrBaseApi("DELETE", "/transporters/" + btn.getAttribute("data-del-tr")).then(function () {
                    showAlert($("transporterAlert"), "Xóa nhà vận chuyển thành công!", "success");
                    loadTransporters();
                }).catch(function (err) { alert(err.message); });
            }
        };
    }

    function startEditTransporter(id) {
        mgrBaseApi("GET", "/transporters").then(function (res) {
            var t = (res.data || []).find(function (x) { return x.id === id; });
            if (!t) return;
            $("transporterId").value = t.id;
            $("transporterName").value = t.name || "";
            $("transporterPhone").value = t.phone || "";
            $("transporterEmail").value = t.email || "";
            $("transporterFee").value = t.baseFee || 0;
            $("transporterStatus").value = t.status || "active";
            $("transporterFormTitle").textContent = "Cập nhật nhà vận chuyển";
            $("btnCancelTransporter").style.display = "";
            $("transporterFormCard").scrollIntoView({ behavior: "smooth" });
        });
    }

    function onSaveTransporter(e) {
        e.preventDefault();
        hideAlert($("transporterAlert"));
        var payload = {
            name: $("transporterName").value.trim(),
            phone: $("transporterPhone").value.trim(),
            email: $("transporterEmail").value.trim(),
            baseFee: $("transporterFee").value ? Number($("transporterFee").value) : 0,
            status: $("transporterStatus").value
        };
        var id = $("transporterId").value;
        var request = id
            ? mgrBaseApi("PUT", "/transporters/" + id, payload)
            : mgrBaseApi("POST", "/transporters", payload);
        request.then(function () {
            showAlert($("transporterAlert"), id ? "Cập nhật nhà vận chuyển thành công!" : "Thêm nhà vận chuyển thành công!", "success");
            resetTransporterForm();
            loadTransporters();
        }).catch(function (err) {
            showAlert($("transporterAlert"), err.message, "error");
        });
    }

    function resetTransporterForm() {
        $("transporterForm").reset();
        $("transporterId").value = "";
        $("transporterStatus").value = "active";
        $("transporterFormTitle").textContent = "Thêm nhà vận chuyển";
        $("btnCancelTransporter").style.display = "none";
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
            ["Tổng đơn hàng", r.totalOrders, "#333"],
            ["Đơn đã giao", r.deliveredOrders, "#28a745"],
            ["Đơn bị hủy", r.cancelledOrders, "#dc3545"],
            ["Trả hàng/hoàn tiền", r.returnedOrders, "#6c757d"]
        ];
        var statusRows = Object.keys(r.statusCounts || {}).map(function (k) {
            var label = k;
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
})();

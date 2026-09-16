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
            $("productGroup").value = p.categoryGroup;
            $("productName").value = p.name;
            $("productPrice").value = p.price;
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
})();

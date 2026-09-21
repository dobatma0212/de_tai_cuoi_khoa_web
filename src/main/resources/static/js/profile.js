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
                if (btn.getAttribute("data-tab") === "tabUserOrders") loadUserOrders();
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
                else if (sub === "shopSubTransporters") loadTransporters();
                else if (sub === "shopSubRevenue") loadRevenue();
            });
        });
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

        renderColorSwatches();
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
                "<span>" +
                '<i class="fa fa-pencil chip-action" data-rename="' + c.id + '" title="Đổi tên"></i> ' +
                '<i class="fa fa-times chip-action" data-delcat="' + c.id + '" title="Xóa danh mục"></i>' +
                "</span></div>";
        }).join("");

        container.onclick = onCategoryChipClick;
    }

    function onCategoryChipClick(e) {
        // Ưu tiên nút sửa / xóa trên dòng
        var icon = e.target.closest(".chip-action");
        if (icon && icon.getAttribute("data-rename") !== null) {
            var renameId = icon.getAttribute("data-rename");
            var cat = categories.find(function (c) { return c.id === parseInt(renameId, 10); });
            var newName = prompt("Tên mới cho danh mục:", cat ? cat.name : "");
            if (newName === null) return;
            if (!newName.trim()) { alert("Tên danh mục không được để trống."); return; }
            mgrApi("PUT", "/categories/" + renameId, { name: newName.trim() }).then(function () {
                showAlert($("productAlert"), "Đổi tên danh mục thành công!", "success");
                loadCategories();
}).catch(function (err) { alert(err.message); });
            return;
        }
        if (icon && icon.getAttribute("data-delcat") !== null) {
            var delId = icon.getAttribute("data-delcat");
            if (!confirm("Xóa danh mục này? (Chỉ xóa được khi danh mục không còn sản phẩm)")) return;
            mgrApi("DELETE", "/categories/" + delId).then(function () {
                showAlert($("productAlert"), "Xóa danh mục thành công!", "success");
                if (currentCategoryId === parseInt(delId, 10)) currentCategoryId = null;
                loadCategories();
}).catch(function (err) { alert(err.message); });
            return;
        }
        // Bấm vào dòng: chọn danh mục để xem/thêm sản phẩm
        var item = e.target.closest(".order-filter-item");
        if (!item || !item.getAttribute("data-cat")) return;
        currentCategoryId = parseInt(item.getAttribute("data-cat"), 10);
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
}).catch(function (err) {
            alert(err.message);
        });
    }

    // ---------- Sản phẩm ----------

    /** Bảng màu chọn nhanh cho sản phẩm (tên -> mã hiển thị) */
    var COLOR_SWATCHES = [
        ["Đen", "#222222"], ["Trắng", "#ffffff"], ["Xám", "#9e9e9e"],
        ["Xanh navy", "#1b2a4a"], ["Đỏ", "#d32f2f"], ["Be", "#d9c8a9"],
        ["Nâu", "#7a4b2a"], ["Xanh lá", "#2e7d32"], ["Vàng", "#f2c200"], ["Hồng", "#f4a7b9"]
    ];

    function renderColorSwatches() {
        var wrap = $("productColorSwatches");
        if (!wrap) return;
        wrap.innerHTML = COLOR_SWATCHES.map(function (c) {
            return '<span class="color-swatch" data-color="' + c[0] + '" title="' + c[0] + '"' +
                ' style="background:' + c[1] + ';"></span>';
        }).join("");
        wrap.onclick = function (e) {
            var sw = e.target.closest(".color-swatch");
            if (!sw) return;
            // Chọn được nhiều màu cùng lúc; bấm lại để bỏ
            var picked = sw.getAttribute("data-color");
            var current = parseColorList($("productColor").value);
            var idx = current.indexOf(picked);
            if (idx === -1) current.push(picked);
            else current.splice(idx, 1);
            $("productColor").value = current.join(", ");
            updateColorSwatchActive();
        };
    }

    /** "Đen, Trắng" -> ["Đen","Trắng"] */
    function parseColorList(value) {
        return String(value || "").split(",")
            .map(function (s) { return s.trim(); })
            .filter(function (s) { return s.length > 0; });
    }

    function updateColorSwatchActive() {
        var wrap = $("productColorSwatches");
        if (!wrap) return;
        var current = parseColorList($("productColor").value);
        wrap.querySelectorAll(".color-swatch").forEach(function (sw) {
            sw.classList.toggle("active", current.indexOf(sw.getAttribute("data-color")) !== -1);
        });
    }

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
                (p.sizes ? " — Size: " + escapeHtml(p.sizes) : "") +
                (p.color ? " — Màu: " + escapeHtml(p.color) : "") + "</small>" +
                '<p class="text-muted mb-2" style="font-size:13px;">' + escapeHtml(p.description || "") + "</p>" +
                '<button type="button" class="btn-act btn-act-rep' + (p.representative ? " is-on" : "") + '" data-rep="' + p.id + '">' +
                (p.representative ? '<i class="fa fa-star"></i> Bỏ đại diện' : '<i class="fa fa-star-o"></i> Đặt đại diện') + "</button> " +
                '<button type="button" class="btn-act btn-act-edit" data-edit="' + p.id + '"><i class="fa fa-pencil"></i> Sửa</button> ' +
                '<button type="button" class="btn-act btn-act-delete" data-delete="' + p.id + '"><i class="fa fa-trash"></i> Xóa</button>' +
                "</div></div>";
        }).join("");

        container.onclick = onProductListClick;
    }

    function onProductListClick(e) {
        var btn = e.target.closest("button");
        if (!btn) return;
        if (btn.getAttribute("data-rep") !== null) {
            var repId = btn.getAttribute("data-rep");
            var makeRep = !btn.classList.contains("is-on");
            mgrApi("PATCH", "/products/" + repId + "/representative?representative=" + makeRep).then(function () {
                showAlert($("productAlert"), makeRep ? "Đã đặt làm sản phẩm đại diện!" : "Đã bỏ chọn đại diện.", "success");
                loadProducts();
}).catch(function (err) { showAlert($("productAlert"), err.message, "error"); });
        } else if (btn.getAttribute("data-edit") !== null) {
            startEditProduct(parseInt(btn.getAttribute("data-edit"), 10));
        } else if (btn.getAttribute("data-delete") !== null) {
            var delId = btn.getAttribute("data-delete");
            if (!confirm("Bạn có chắc muốn xóa sản phẩm này?")) return;
            mgrApi("DELETE", "/products/" + delId).then(function () {
                showAlert($("productAlert"), "Xóa sản phẩm thành công!", "success");
                loadProducts();
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
        $("productColor").value = "";
        updateColorSwatchActive();
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
            $("productName").value = p.name;
            $("productPrice").value = p.price;
            $("productStock").value = p.stock || 0;
            $("productSizes").value = p.sizes || "";
            $("productColor").value = p.color || "";
            updateColorSwatchActive();
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
            color: $("productColor").value.trim(),
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
        ["SHIPPING", "Đang giao"],
        ["DELIVERED", "Đã giao"],
        ["RECEIVED", "Đã nhận hàng"],
        ["CANCELLED", "Đã hủy"],
        ["RETURN", "Trả hàng - hoàn tiền"]
    ];
    var transporters = [];
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
                if (!transporterId) {
                    alert("Vui lòng chọn đơn vị vận chuyển trước khi xác nhận.");
                    return;
                }
                mgrBaseApi("PATCH", "/orders/" + id + "/status?status=CONFIRMED&transporterId=" + transporterId)
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

    function loadUserOrders() {
        userApi("GET", "/orders").then(function (res) {
            renderUserOrders(res.data || []);
        }).catch(function (err) {
            $("userOrderList").innerHTML = '<p class="text-muted">' + escapeHtml(err.message) + "</p>";
        });
    }

    function renderUserOrders(list) {
        var container = $("userOrderList");
        if (!list.length) {
            container.innerHTML = '<p class="text-muted">Bạn chưa có đơn hàng nào.</p>';
            return;
        }
        container.innerHTML = list.map(function (o) {
            var items = (o.items || []).map(function (it) {
                return "<li>" + escapeHtml(it.productName) +
                    (it.size ? " (size " + escapeHtml(it.size) + ")" : "") +
                    " x" + it.quantity + " — " + formatVnd(it.subtotal) + "</li>";
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
            return '<div class="address-card">' +
                '<div class="d-flex justify-content-between flex-wrap">' +
                "<div><b>" + escapeHtml(o.orderCode) + "</b> " +
                '<span class="badge badge-light">' + escapeHtml(o.statusLabel || o.status) + "</span>" +
                '<div><small class="text-muted">' + escapeHtml(o.shopName || "") + " — " +
                escapeHtml(o.createdAt || "") + "</small></div>" +
                (o.transporterName ? '<div><small class="text-muted">VC: ' + escapeHtml(o.transporterName) + "</small></div>" : "") +
                (o.cancelReason ? '<div><small class="text-danger">' + escapeHtml(o.cancelReason) + "</small></div>" : "") +
                '<ul class="mt-2 mb-0 text-muted" style="font-size:13px;">' + items + "</ul></div>" +
                '<div class="text-right"><b style="color:#e53637;">' + formatVnd(o.grandTotal) + "</b>" +
                '<div class="mt-2">' + actions + "</div></div>" +
                "</div></div>";
        }).join("");

        container.onclick = function (e) {
            var btn = e.target.closest("button");
            if (!btn) return;
            if (btn.getAttribute("data-cancel") !== null) {
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

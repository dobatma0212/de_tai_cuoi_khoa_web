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
        return res.text().then(function (raw) {
            var data = null;
            try {
                data = raw ? JSON.parse(raw) : null;
            } catch (e) {
                // Response không phải JSON (lỗi server 500, HTML error page...)
            }
            if (!res.ok || (data && data.success === false)) {
                var detail = data && data.message ? data.message : null;
                throw new Error(buildErrorMessage(res.status, detail));
            }
            return data;
        });
    }

    function buildErrorMessage(status, detail) {
        var base = "Đã xảy ra lỗi";
        if (status >= 500) {
            base += " từ máy chủ (lỗi " + status + ")";
        } else if (status === 401 || status === 403) {
            base = "Phiên đăng nhập hết hạn hoặc không có quyền (lỗi " + status + ")";
        }
        if (detail) {
            base += ": " + detail;
        } else {
            base += ". Vui lòng thử lại sau.";
        }
        return base;
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
        loadMyShop();

        $("infoForm").addEventListener("submit", onSaveInfo);
        $("passwordForm").addEventListener("submit", onChangePassword);
        $("addressForm").addEventListener("submit", onSaveAddress);
        $("btnShowAddAddress").addEventListener("click", showAddAddressForm);
        $("btnCancelAddress").addEventListener("click", hideAddressForm);
        $("shopRegisterForm").addEventListener("submit", onRegisterShop);
        $("btnShowShopRegister").addEventListener("click", showShopRegisterForm);
        $("btnCancelShopRegister").addEventListener("click", hideShopRegisterForm);
        bindUploadButtons();
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

    // ============ Đăng ký mở cửa hàng ============
    var STATUS_META = {
        pending: { text: "Đang chờ duyệt", cls: "badge-warning text-dark" },
        active: { text: "Đã được duyệt", cls: "badge-success" },
        rejected: { text: "Bị từ chối", cls: "badge-danger" },
        banned: { text: "Bị khóa", cls: "badge-secondary" }
    };

    function statusMessage(shop) {
        if (shop.status === "pending") {
            return "Đơn đăng ký của bạn đang chờ quản trị viên duyệt. Vui lòng quay lại sau. "
                + "(Trong thời gian chờ duyệt, bạn chưa thể gửi thêm đơn đăng ký mới.)";
        }
        if (shop.status === "active") {
            return "Cửa hàng của bạn đang hoạt động. Hãy quản lý trang chủ cửa hàng và sản phẩm của mình!";
        }
        if (shop.status === "rejected") {
            return "Đơn đăng ký bị từ chối." + (shop.rejectReason ? " Lý do: " + shop.rejectReason : "")
                + " Bạn có thể gửi đơn đăng ký mới bất cứ lúc nào.";
        }
        if (shop.status === "banned") {
            return "Cửa hàng hiện bị khóa. Vui lòng liên hệ quản trị viên.";
        }
        return "";
    }

    function loadMyShop() {
        api("GET", "/shop").then(function (res) {
            renderShopList(res.data || []);
        }).catch(function (err) {
            // Lỗi thì vẫn cho phép đăng ký mới
            renderShopList([]);
            if (err.message) showAlert($("shopAlert"), err.message, "error");
        });
    }

    function renderShopList(shops) {
        var container = $("shopListContainer");
        if (!shops.length) {
            container.innerHTML = "";
        } else {
            container.innerHTML = shops.map(function (shop) {
                var meta = STATUS_META[shop.status] || { text: shop.status, cls: "badge-secondary" };
                var logo = shop.logoUrl || "/img/logo.png";
                var actions = "";
                if (shop.status === "active") {
                    actions = '<div class="mt-3">' +
                        '<a href="/shop-home.html?id=' + shop.id + '" class="btn btn-sm btn-outline-store">Xem trang chủ shop</a> ' +
                        '<a href="/vendor-shop.html?shopId=' + shop.id + '" class="btn btn-sm btn-outline-store">Quản lý trang chủ shop</a> ' +
                        '<a href="/vendor-products.html?shopId=' + shop.id + '" class="btn btn-sm btn-store">Quản lý sản phẩm</a>' +
                        "</div>";
                }
                return '<div class="address-card">' +
                    '<div class="d-flex align-items-center mb-2">' +
                    '<img src="' + escapeHtml(logo) + '" alt="" ' +
                    'style="width:60px;height:60px;border-radius:8px;object-fit:cover;margin-right:14px;">' +
                    "<div>" +
                    '<h5 class="font-weight-bold mb-1">' + escapeHtml(shop.name) + "</h5>" +
                    '<span class="badge ' + meta.cls + '">' + meta.text + "</span>" +
                    "</div></div>" +
                    '<p class="mb-0 text-muted">' + escapeHtml(statusMessage(shop)) + "</p>" +
                    actions +
                    "</div>";
            }).join("");
        }

        // Chống spam: đang có đơn chờ duyệt thì ẩn nút đăng ký
        var hasPending = shops.some(function (s) { return s.status === "pending"; });
        $("shopRegisterToggleBox").style.display = hasPending ? "none" : "block";
        $("shopRegisterBtnLabel").textContent = shops.length ? "Mở thêm cửa hàng" : "Đăng ký mở cửa hàng";
        if (hasPending) hideShopRegisterForm();
    }

    function showShopRegisterForm() {
        hideAlert($("shopAlert"));
        $("shopRegisterFormCard").style.display = "block";
        $("shopRegisterFormCard").scrollIntoView({ behavior: "smooth" });
    }

    function hideShopRegisterForm() {
        $("shopRegisterFormCard").style.display = "none";
        $("shopRegisterForm").reset();
    }

    function onRegisterShop(e) {
        e.preventDefault();
        hideAlert($("shopAlert"));
        api("POST", "/shop/register", {
            name: $("shopName").value.trim(),
            phone: $("shopPhone").value.trim(),
            address: $("shopAddress").value.trim(),
            description: $("shopDescription").value.trim(),
            logoUrl: $("shopLogoUrl").value.trim(),
            bannerUrl: $("shopBannerUrl").value.trim()
        }).then(function (res) {
            showAlert($("shopAlert"), res.message, "success");
            hideShopRegisterForm();
            loadMyShop();
        }).catch(function (err) {
            showAlert($("shopAlert"), err.message, "error");
        });
    }

    // ============ Tải ảnh lên (upload) ============
    function uploadImageFile(file, urlInput, button) {
        if (!file) return;
        var oldLabel = button.innerHTML;
        button.disabled = true;
        button.innerHTML = '<i class="fa fa-spinner fa-spin"></i>';
        var formData = new FormData();
        formData.append("file", file);
        fetch("/api/uploads", {
            method: "POST",
            headers: { "Authorization": "Bearer " + getToken() },
            body: formData
        }).then(handleResponse).then(function (res) {
            $(urlInput).value = res.data.url;
            button.disabled = false;
            button.innerHTML = oldLabel;
        }).catch(function (err) {
            button.disabled = false;
            button.innerHTML = oldLabel;
            showAlert($("shopAlert"), err.message, "error");
        });
    }

    function bindUploadButtons() {
        document.querySelectorAll(".btn-upload-image").forEach(function (btn) {
            var targetId = btn.getAttribute("data-target");
            var fileInputId = targetId + "File";
            var fileInput = $(fileInputId);
            if (fileInput) {
                btn.addEventListener("click", function () { fileInput.click(); });
                fileInput.addEventListener("change", function () {
                    uploadImageFile(fileInput.files[0], targetId, btn);
                    fileInput.value = "";
                });
            }
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
})();

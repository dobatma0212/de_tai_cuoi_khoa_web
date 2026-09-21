/**
 * employee.js
 * Tab "Nhân viên chi nhánh" trong profile.html — chỉ hiển thị cho ROLE_EMPLOYEE.
 * Chức năng: thêm/sửa/xóa sản phẩm của chi nhánh được phân công,
 * chấm công (tính công) và xem hoa hồng theo đơn hàng đã bán được.
 * Giao diện dùng chung style với phần Quản lý chi nhánh của manager.
 * Nhân viên vẫn giữ quyền user cơ bản (mua sắm, giỏ hàng, đánh giá...).
 */
(function () {
    "use strict";

    var EMPLOYEE_API = "/api/employee";

    function $(id) { return document.getElementById(id); }

    function getSession() {
        return window.ScAuth ? window.ScAuth.getSession() : null;
    }

    function authHeaders() {
        var s = getSession();
        var token = s && s.token ? s.token : localStorage.getItem("jwt_token");
        var headers = { "Content-Type": "application/json" };
        if (token) headers["Authorization"] = "Bearer " + token;
        return headers;
    }

    function empApi(method, path, body) {
        return fetch(EMPLOYEE_API + path, {
            method: method,
            headers: authHeaders(),
            body: body ? JSON.stringify(body) : undefined
        }).then(function (res) {
            if (res.status === 401 || res.status === 403) {
                if (window.ScAuth) window.ScAuth.clearSession();
                window.location.href = "/login.html";
                return new Promise(function () { });
            }
            return res.json().then(function (data) {
                if (!res.ok || data.success === false) {
                    var err = new Error(data.message || "Đã có lỗi xảy ra.");
                    throw err;
                }
                return data;
            });
        });
    }

    function formatVnd(value) {
        var n = Number(value);
        if (isNaN(n)) return "0₫";
        return n.toLocaleString("vi-VN") + "₫";
    }

    function showAlert(id, message, type) {
        var el = $(id);
        if (!el) return;
        el.className = "alert auth-alert auth-alert--" + (type || "error");
        el.textContent = message;
        el.style.display = "block";
    }

    function hideAlert(id) {
        var el = $(id);
        if (el) el.style.display = "none";
    }

    function escapeHtml(str) {
        return String(str == null ? "" : str)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    function hasRole(session, keyword) {
        if (!session) return false;
        var user = session.user && typeof session.user === "object" ? session.user : session;
        var r = String(user.role || session.role || user.roleName || session.roleName || "").toUpperCase();
        if (r.indexOf(keyword) !== -1) return true;
        var roles = user.roles || session.roles;
        if (Array.isArray(roles)) {
            return roles.some(function (role) {
                return String(role || "").toUpperCase().indexOf(keyword) !== -1;
            });
        }
        return false;
    }

    // ==================== Trạng thái ====================
    var currentShop = null;
    var empCategories = [];
    var empCurrentCategoryId = null;
    var empProductsCache = [];
    var loadedOnce = false;

    // ==================== Khởi tạo ====================
    function init() {
        var session = getSession();
        if (!hasRole(session, "EMPLOYEE")) return;

        var btn = $("tabEmployeeBtn");
        if (btn) btn.style.display = "";

        // Load dữ liệu lần đầu khi mở tab
        btn.addEventListener("click", function () {
            if (!loadedOnce) {
                loadedOnce = true;
                loadAll();
            }
        });

        initEmpSubTabs();
        renderColorSwatches();
        bindProductForm();
        bindWorkSection();
    }

    function loadAll() {
        empApi("GET", "/shop").then(function (res) {
            currentShop = res.data;
            $("empNoShop").style.display = "none";
            $("empWrapper").style.display = "block";
            $("empShopName").value = currentShop.name || "";
            $("empShopManager").value = currentShop.managerFullName
                || (currentShop.managerUsername ? "@" + currentShop.managerUsername : "")
                || "Chưa có quản lý";
            $("empShopPhone").value = currentShop.phone || "";
            $("empShopAddress").value = currentShop.address || "";
            $("empShopDescription").value = currentShop.description || "";
            loadCategories();
            loadStats(null);
        }).catch(function (err) {
            $("empWrapper").style.display = "none";
            $("empNoShop").style.display = "block";
            $("empNoShop").textContent = err.message;
        });
    }

    function initEmpSubTabs() {
        document.querySelectorAll(".emp-subbtn").forEach(function (btn) {
            btn.addEventListener("click", function () {
                document.querySelectorAll(".emp-subbtn").forEach(function (b) { b.classList.remove("active"); });
                btn.classList.add("active");
                var sub = btn.getAttribute("data-sub");
                $("empSubProducts").classList.toggle("active", sub === "empSubProducts");
                $("empSubOrders").classList.toggle("active", sub === "empSubOrders");
                $("empSubWork").classList.toggle("active", sub === "empSubWork");
                if (sub === "empSubOrders") loadEmpOrders();
            });
        });
    }

    // ==================== Danh mục (chỉ đọc — do manager quản lý) ====================
    function loadCategories() {
        empApi("GET", "/categories").then(function (res) {
            empCategories = res.data || [];
            // Giữ lựa chọn hiện tại nếu vẫn còn, ngược lại chọn danh mục đầu tiên (như manager)
            if (empCurrentCategoryId === null ||
                !empCategories.some(function (c) { return c.id === empCurrentCategoryId; })) {
                empCurrentCategoryId = empCategories.length ? empCategories[0].id : null;
            }
            renderCategoryChips();
            loadProducts();
        }).catch(function (err) {
            showAlert("empProductAlert", err.message, "error");
        });
    }

    function renderCategoryChips() {
        var container = $("empCategoryChips");
        if (!empCategories.length) {
            container.innerHTML = '<div class="order-filter-item" style="color:#999;font-weight:400;">Chưa có danh mục nào</div>';
            return;
        }
        container.innerHTML = empCategories.map(function (c) {
            var active = c.id === empCurrentCategoryId ? " active" : "";
            return '<div class="order-filter-item' + active + '" data-cat="' + c.id + '">' +
                "<span>" + escapeHtml(c.name) +
                ' <small class="text-muted">(' + (c.totalProducts || 0) + ")</small></span>" +
                "</div>";
        }).join("");

        container.onclick = function (e) {
            var item = e.target.closest(".order-filter-item");
            if (!item || !item.getAttribute("data-cat")) return;
            empCurrentCategoryId = parseInt(item.getAttribute("data-cat"), 10);
            renderCategoryChips();
            resetProductForm();
            loadProducts();
        };
    }

    function currentCategoryName() {
        var c = empCategories.find(function (x) { return x.id === empCurrentCategoryId; });
        return c ? c.name : "";
    }

    // ==================== Sản phẩm ====================
    function loadProducts() {
        if (empCurrentCategoryId === null) {
            $("empProductListTitle").textContent = "Danh sách sản phẩm";
            $("empProductList").innerHTML = '<p class="text-muted">Hãy chọn một danh mục để xem sản phẩm.</p>';
            return;
        }
        empApi("GET", "/products?categoryId=" + empCurrentCategoryId).then(function (res) {
            empProductsCache = res.data || [];
            renderProducts(empProductsCache);
        }).catch(function (err) {
            showAlert("empProductAlert", err.message, "error");
        });
    }

    function renderProducts(list) {
        var container = $("empProductList");
        $("empProductListTitle").textContent = "Danh sách sản phẩm — " + currentCategoryName() +
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
            empApi("PATCH", "/products/" + repId + "/representative?representative=" + makeRep).then(function () {
                showAlert("empProductAlert", makeRep ? "Đã đặt làm sản phẩm đại diện!" : "Đã bỏ chọn đại diện.", "success");
                loadProducts();
            }).catch(function (err) { showAlert("empProductAlert", err.message, "error"); });
        } else if (btn.getAttribute("data-edit") !== null) {
            startEditProduct(parseInt(btn.getAttribute("data-edit"), 10));
        } else if (btn.getAttribute("data-delete") !== null) {
            var delId = btn.getAttribute("data-delete");
            if (!confirm("Bạn có chắc muốn xóa sản phẩm này?")) return;
            empApi("DELETE", "/products/" + delId).then(function () {
                showAlert("empProductAlert", "Xóa sản phẩm thành công!", "success");
                loadProducts();
            }).catch(function (err) { showAlert("empProductAlert", err.message, "error"); });
        }
    }

    function startEditProduct(id) {
        var p = empProductsCache.find(function (x) { return x.id === id; });
        if (!p) return;
        $("empProductFormTitle").textContent = "Cập nhật sản phẩm: " + p.name;
        $("empProductId").value = p.id;
        $("empProductCategory").value = p.categoryId;
        $("empProductName").value = p.name || "";
        $("empProductPrice").value = p.price || 0;
        $("empProductStock").value = p.stock || 0;
        $("empProductSizes").value = p.sizes || "";
        $("empProductColor").value = p.color || "";
        $("empProductDescription").value = p.description || "";
        $("empProductImageUrl").value = p.imageUrl || "";
        $("empProductImageFile").value = "";
        updateColorSwatchActive();
        previewImage(p.imageUrl);
        $("empBtnCancelProduct").style.display = "";
        $("empProductFormCard").scrollIntoView({ behavior: "smooth", block: "center" });
    }

    function resetProductForm() {
        $("empProductFormTitle").textContent = "Thêm sản phẩm mới";
        $("empProductForm").reset();
        $("empProductId").value = "";
        $("empProductCategory").value = empCurrentCategoryId || "";
        $("empProductColor").value = "";
        updateColorSwatchActive();
        $("empBtnCancelProduct").style.display = "none";
        previewImage(null);
    }

    function previewImage(url) {
        var img = $("empProductImagePreview");
        if (!url) { img.style.display = "none"; img.src = ""; return; }
        img.src = url;
        img.style.display = "block";
    }

    function uploadImageFile(file) {
        var s = getSession();
        var token = s && s.token ? s.token : localStorage.getItem("jwt_token");
        var fd = new FormData();
        fd.append("file", file);
        return fetch(EMPLOYEE_API + "/upload", {
            method: "POST",
            headers: token ? { "Authorization": "Bearer " + token } : {},
            body: fd
        }).then(function (res) {
            return res.json().then(function (data) {
                if (!res.ok || data.success === false) throw new Error(data.message || "Tải ảnh lên thất bại.");
                return data.data.url;
            });
        });
    }

    // ---------- Bảng màu chọn nhanh (giống manager) ----------
    var COLOR_SWATCHES = [
        ["Đen", "#222222"], ["Trắng", "#ffffff"], ["Xám", "#9e9e9e"],
        ["Xanh navy", "#1b2a4a"], ["Đỏ", "#d32f2f"], ["Be", "#d9c8a9"],
        ["Nâu", "#7a4b2a"], ["Xanh lá", "#2e7d32"], ["Vàng", "#f2c200"], ["Hồng", "#f4a7b9"]
    ];

    function renderColorSwatches() {
        var wrap = $("empProductColorSwatches");
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
            var current = parseColorList($("empProductColor").value);
            var idx = current.indexOf(picked);
            if (idx === -1) current.push(picked);
            else current.splice(idx, 1);
            $("empProductColor").value = current.join(", ");
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
        var wrap = $("empProductColorSwatches");
        if (!wrap) return;
        var current = parseColorList($("empProductColor").value);
        wrap.querySelectorAll(".color-swatch").forEach(function (sw) {
            sw.classList.toggle("active", current.indexOf(sw.getAttribute("data-color")) !== -1);
        });
    }

    function bindProductForm() {
        $("empProductImageUrl").addEventListener("input", function () {
            previewImage(this.value.trim());
        });
        $("empProductImageFile").addEventListener("change", function () {
            var f = this.files && this.files[0];
            if (f) previewImage(URL.createObjectURL(f));
        });
        $("empBtnCancelProduct").addEventListener("click", resetProductForm);

        $("empProductForm").addEventListener("submit", function (e) {
            e.preventDefault();
            hideAlert("empProductAlert");

            var editId = $("empProductId").value;
            var doSave = function (imageUrl) {
                var body = {
                    categoryId: Number($("empProductCategory").value) || empCurrentCategoryId,
                    name: $("empProductName").value.trim(),
                    price: Number($("empProductPrice").value),
                    stock: Number($("empProductStock").value || 0),
                    sizes: $("empProductSizes").value.trim(),
                    color: $("empProductColor").value.trim(),
                    description: $("empProductDescription").value.trim(),
                    imageUrl: imageUrl
                };
                if (!body.categoryId) {
                    showAlert("empProductAlert", "Vui lòng chọn danh mục cho sản phẩm.", "error");
                    return;
                }
                var req = editId
                    ? empApi("PUT", "/products/" + editId, body)
                    : empApi("POST", "/products", body);
                req.then(function () {
                    showAlert("empProductAlert", editId ? "Cập nhật sản phẩm thành công!" : "Thêm sản phẩm mới thành công!", "success");
                    resetProductForm();
                    loadCategories();
                }).catch(function (err) {
                    showAlert("empProductAlert", err.message, "error");
                });
            };

            var fileInput = $("empProductImageFile");
            var file = fileInput.files && fileInput.files[0];
            if (file) {
                uploadImageFile(file).then(doSave).catch(function (err) {
                    showAlert("empProductAlert", err.message, "error");
                });
            } else {
                doSave($("empProductImageUrl").value.trim() || null);
            }
        });
    }

    // ==================== Đơn hàng của chi nhánh (như manager) ====================
    var EMP_ORDER_STATUSES = [
        ["", "Tất cả"],
        ["NEW", "Đơn hàng mới"],
        ["CONFIRMED", "Đã xác nhận"],
        ["SHIPPING", "Đang giao"],
        ["DELIVERED", "Đã giao"],
        ["RECEIVED", "Đã nhận hàng"],
        ["CANCELLED", "Đã hủy"],
        ["RETURN", "Trả hàng - hoàn tiền"]
    ];
    var empOrderStatus = "";
    var empOrderCache = [];
    var empTransporters = [];

    function loadEmpOrders() {
        if (!empTransporters.length) {
            empApi("GET", "/transporters").then(function (res) {
                empTransporters = res.data || [];
                fetchEmpOrders();
            }).catch(function (err) {
                showAlert("empOrderAlert", err.message, "error");
            });
        } else {
            fetchEmpOrders();
        }
    }

    function fetchEmpOrders() {
        empApi("GET", "/orders").then(function (res) {
            empOrderCache = res.data || [];
            renderEmpOrderChips();
            renderEmpOrders(filterEmpOrders());
        }).catch(function (err) {
            showAlert("empOrderAlert", err.message, "error");
        });
    }

    function filterEmpOrders() {
        if (!empOrderStatus) return empOrderCache;
        // Chip "Trả hàng - hoàn tiền" gộp cả 2 trạng thái: đã trả + khách yêu cầu trả
        if (empOrderStatus === "RETURN") {
            return empOrderCache.filter(function (o) {
                return o.status === "RETURNED" || o.status === "RETURN_REQUESTED";
            });
        }
        return empOrderCache.filter(function (o) { return o.status === empOrderStatus; });
    }

    function renderEmpOrderChips() {
        var container = $("empOrderStatusChips");
        container.innerHTML = EMP_ORDER_STATUSES.map(function (st) {
            var count = st[0] === ""
                ? empOrderCache.length
                : empOrderCache.filter(function (o) {
                    return st[0] === "RETURN"
                        ? (o.status === "RETURNED" || o.status === "RETURN_REQUESTED")
                        : o.status === st[0];
                }).length;
            var active = st[0] === empOrderStatus ? " active" : "";
            return '<div class="order-filter-item' + active + '" data-status="' + st[0] + '">' +
                "<span>" + st[1] + '</span><span class="of-count">' + count + "</span></div>";
        }).join("");
        container.onclick = function (e) {
            var item = e.target.closest(".order-filter-item");
            if (!item) return;
            empOrderStatus = item.getAttribute("data-status");
            fetchEmpOrders();
        };
    }

    function renderEmpOrders(list) {
        var container = $("empOrderList");
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
                    (it.color ? " - màu " + escapeHtml(it.color) : "") +
                    " x" + it.quantity + " — " + formatVnd(it.subtotal) + "</li>";
            }).join("");
            var actions = "";
            if (o.status === "NEW") {
                actions = '<div class="mt-2 d-flex flex-wrap align-items-center">' +
                    '<select class="form-control form-control-sm mr-2 mb-1 emp-transporter-select" style="max-width:220px;">' +
                    '<option value="">— Chọn đơn vị vận chuyển —</option>' +
                    empTransporters.filter(function (t) { return t.status !== "inactive"; }).map(function (t) {
                        return '<option value="' + t.id + '">' + escapeHtml(t.name) +
                            " (" + formatVnd(t.baseFee) + ")</option>";
                    }).join("") +
                    "</select>" +
                    '<button type="button" class="btn btn-sm btn-store mr-2 mb-1" data-confirm-order="' + o.id + '">Xác nhận đơn</button>' +
                    '<button type="button" class="btn btn-sm btn-link btn-link-danger mb-1" data-cancel-order="' + o.id + '">Hủy đơn</button>' +
                    "</div>" +
                    '<small class="text-muted">Xác nhận đơn này sẽ được tính hoa hồng khi giao thành công.</small>';
            } else if (o.status === "CONFIRMED" || o.status === "SHIPPING") {
                actions = '<div class="mt-2">' +
                    '<small class="text-muted">Đơn đã được xác nhận — giao hàng tự động, khách có thể hủy giữa chừng.</small> ' +
                    '<button type="button" class="btn btn-sm btn-link btn-link-danger" data-cancel-order="' + o.id + '">Hủy đơn</button>' +
                    "</div>";
            } else if (o.status === "DELIVERED" || o.status === "RECEIVED") {
                actions = '<div class="mt-2"><small class="text-muted">' +
                    (o.status === "DELIVERED" ? "Đã giao — chờ khách xác nhận đã nhận hàng." : "Khách đã xác nhận nhận hàng.") +
                    '</small> <button type="button" class="btn btn-sm btn-outline-secondary" data-return-order="' + o.id + '">Trả hàng - hoàn tiền</button></div>';
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
            var id;
            if (btn.getAttribute("data-confirm-order") !== null) {
                id = btn.getAttribute("data-confirm-order");
                var select = btn.closest(".address-card").querySelector(".emp-transporter-select");
                var transporterId = select ? select.value : "";
                if (!transporterId) {
                    alert("Vui lòng chọn đơn vị vận chuyển trước khi xác nhận.");
                    return;
                }
                empApi("PATCH", "/orders/" + id + "/status?status=CONFIRMED&transporterId=" + transporterId)
                    .then(function () {
                        showAlert("empOrderAlert", "Đã xác nhận đơn hàng! Đơn sẽ tự giao và tính hoa hồng cho bạn khi giao thành công.", "success");
                        fetchEmpOrders();
                    }).catch(function (err) { alert(err.message); });
            } else if (btn.getAttribute("data-cancel-order") !== null) {
                id = btn.getAttribute("data-cancel-order");
                var reason = prompt("Lý do hủy đơn (VD: tồn kho chi nhánh đã hết):");
                if (reason === null) return;
                if (!reason.trim()) { alert("Vui lòng nhập lý do hủy đơn."); return; }
                empApi("PATCH", "/orders/" + id + "/status?status=CANCELLED&reason=" + encodeURIComponent(reason.trim()))
                    .then(function () {
                        showAlert("empOrderAlert", "Đã hủy đơn hàng và hoàn tồn kho về chi nhánh.", "success");
                        fetchEmpOrders();
                    }).catch(function (err) { alert(err.message); });
            } else if (btn.getAttribute("data-return-order") !== null) {
                id = btn.getAttribute("data-return-order");
                var returnReason = prompt("Lý do trả hàng - hoàn tiền (khách phản hồi hàng có vấn đề):");
                if (returnReason === null) return;
                var q = returnReason.trim() ? "&reason=" + encodeURIComponent(returnReason.trim()) : "";
                empApi("PATCH", "/orders/" + id + "/status?status=RETURNED" + q)
                    .then(function () {
                        showAlert("empOrderAlert", "Đã cập nhật trả hàng - hoàn tiền.", "success");
                        fetchEmpOrders();
                    }).catch(function (err) { alert(err.message); });
            } else if (btn.getAttribute("data-approve-return") !== null) {
                id = btn.getAttribute("data-approve-return");
                if (!confirm("Duyệt trả hàng - hoàn tiền cho đơn này?")) return;
                empApi("PATCH", "/orders/" + id + "/status?status=RETURNED")
                    .then(function () {
                        showAlert("empOrderAlert", "Đã duyệt trả hàng - hoàn tiền (tồn kho đã hoàn về chi nhánh).", "success");
                        fetchEmpOrders();
                    }).catch(function (err) { alert(err.message); });
            }
        };
    }

    // ==================== Công & Hoa hồng ====================
    function bindWorkSection() {
        var monthInput = $("empMonth");
        var now = new Date();
        monthInput.value = now.getFullYear() + "-" + String(now.getMonth() + 1).padStart(2, "0");

        $("empBtnLoadStats").addEventListener("click", function () {
            loadStats(monthInput.value);
        });
        $("empBtnCheckIn").addEventListener("click", function () {
            empApi("POST", "/attendance/check-in").then(function (res) {
                showAlert("empWorkAlert", res.message, "success");
                renderStats(res.data);
            }).catch(function (err) {
                showAlert("empWorkAlert", err.message, "error");
            });
        });
    }

    function loadStats(month) {
        var qs = month ? "?month=" + encodeURIComponent(month) : "";
        empApi("GET", "/commission" + qs).then(function (res) {
            renderStats(res.data);
        }).catch(function (err) {
            showAlert("empWorkAlert", err.message, "error");
        });
    }

    function renderStats(stats) {
        if (!stats) return;
        var rate = Number(stats.commissionRate || 0) * 100;
        $("empStatsSummary").innerHTML =
            '<div class="row text-center">'
            + '<div class="col-md-3 mb-2"><div class="address-card"><h4 class="font-weight-bold mb-0">' + stats.workDays + '</h4><small class="text-muted">Số công đã làm</small></div></div>'
            + '<div class="col-md-3 mb-2"><div class="address-card"><h4 class="font-weight-bold mb-0">' + stats.ordersSold + '</h4><small class="text-muted">Đơn đã bán được</small></div></div>'
            + '<div class="col-md-3 mb-2"><div class="address-card"><h4 class="font-weight-bold mb-0">' + formatVnd(stats.totalSales) + '</h4><small class="text-muted">Tổng doanh số</small></div></div>'
            + '<div class="col-md-3 mb-2"><div class="address-card"><h4 class="font-weight-bold mb-0" style="color:#e53637;">' + formatVnd(stats.commission) + '</h4><small class="text-muted">Hoa hồng (' + rate.toFixed(1) + '%)</small></div></div>'
            + "</div>";

        // Lịch ngày đã chấm công
        var dates = stats.workDates || [];
        var dateHtml = '<h6 class="font-weight-bold mt-4 mb-2">Ng\u00e0y \u0111\u00e3 ch\u1ea5m c\u00f4ng</h6>';
        if (!dates.length) {
            dateHtml += '<p class="text-muted">Chưa có ngày chấm công nào trong tháng này.</p>';
        } else {
            dateHtml += '<div class="mb-2">' + dates.map(function (d) {
                return '<span class="badge badge-light" style="border:1px solid #ddd;padding:6px 10px;margin:2px;">'
                    + escapeHtml(String(d).slice(0, 10)) + "</span>";
            }).join("") + "</div>";
        }

        // Sao kê bán hàng: TẤT CẢ đơn do nhân viên phụ trách trong tháng
        var handled = stats.handledOrders || [];
        dateHtml += '<h6 class="font-weight-bold mt-4 mb-2">Sao k\u00ea b\u00e1n h\u00e0ng trong th\u00e1ng ('
            + handled.length + ' \u0111\u01a1n ph\u1ee5 tr\u00e1ch)</h6>';
        if (!handled.length) {
            dateHtml += '<p class="text-muted">Bạn chưa phụ trách đơn hàng nào trong tháng này. '
                + 'Xác nhận đơn ở tab "Đơn hàng" để bắt đầu được tính hoa hồng.</p>';
        } else {
            var statusColors = {
                NEW: "badge-primary", CONFIRMED: "badge-info",
                SHIPPING: "badge-warning", DELIVERED: "badge-success", RECEIVED: "badge-success",
                CANCELLED: "badge-danger", RETURNED: "badge-dark", RETURN_REQUESTED: "badge-warning"
            };
            dateHtml += '<div class="table-responsive"><table class="table table-sm" style="font-size:13px;">'
                + "<thead><tr><th>M\u00e3 \u0111\u01a1n</th><th>Tr\u1ea1ng th\u00e1i</th><th>T\u1ed5ng ti\u1ec1n</th>"
                + "<th>Ng\u00e0y t\u1ea1o</th><th>Hoa h\u1ed3ng</th></tr></thead><tbody>";
            handled.forEach(function (o) {
                var counted = o.status === "DELIVERED" || o.status === "RECEIVED";
                dateHtml += "<tr><td>" + escapeHtml(o.orderCode) + "</td>"
                    + '<td><span class="badge ' + (statusColors[o.status] || "badge-light") + '">'
                    + escapeHtml(o.status) + "</span></td>"
                    + "<td>" + formatVnd(o.grandTotal) + "</td>"
                    + "<td>" + (o.createdAt ? new Date(o.createdAt).toLocaleString("vi-VN") : "") + "</td>"
                    + "<td>" + (counted
                        ? '<span style="color:#28a745;font-weight:700;">\u2713 ' + formatVnd((Number(o.grandTotal) * Number(stats.commissionRate || 0)).toFixed(0)) + "</span>"
                        : '<span class="text-muted">\u2014</span>') + "</td></tr>";
            });
            dateHtml += "</tbody></table></div>"
                + '<small class="text-muted">Chỉ đơn \u0111\u00e3 giao / kh\u00e1ch \u0111\u00e3 nh\u1eadn h\u00e0ng \u0111\u01b0\u1ee3c t\u00ednh hoa h\u1ed3ng; \u0111\u01a1n h\u1ee7y, \u0111ang giao, tr\u1ea3 h\u00e0ng kh\u00f4ng t\u00ednh.</small>';
        }
        $("empWorkLog").innerHTML = dateHtml;
    }

    // ==================== Chạy khi DOM sẵn sàng ====================
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();

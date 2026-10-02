/**
 * manager-imports.js
 * Sub-tab "Nhập hàng" trong tab Quản lý chi nhánh (chỉ ROLE_MANAGER).
 * Manager lập phiếu nhập kho cho chi nhánh mình phụ trách:
 * lưu nháp -> xác nhận (cộng tồn kho chi nhánh) hoặc hủy phiếu.
 * File tự khai báo hàm gọi API riêng (helper của profile.js nằm trong closure,
 * không dùng chung được) — giống cách employee.js đang làm.
 */
(function () {
    "use strict";

    var IMPORT_API = "/api/manager/imports";
    var SHOP_API = "/api/manager/shop";

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

    function api(method, url, body) {
        return fetch(url, {
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
                    throw new Error(data.message || "Đã có lỗi xảy ra.");
                }
                return data;
            });
        });
    }

    function escapeHtml(str) {
        return String(str == null ? "" : str)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    function showAlert(message, type) {
        var el = $("impAlert");
        el.className = "alert auth-alert auth-alert--" + (type || "error");
        el.textContent = message;
        el.style.display = "block";
    }

    function hideAlert() {
        $("impAlert").style.display = "none";
    }

    function formatDateTime(value) {
        if (!value) return "—";
        return String(value).replace("T", " ").substring(0, 16);
    }

    /** Chống bấm đúp: khóa nút trong lúc gọi API */
    function withLock(btn, fn) {
        if (btn.dataset.busy) return;
        btn.dataset.busy = "1";
        btn.disabled = true;
        Promise.resolve()
            .then(fn)
            .catch(function (err) { showAlert(err.message, "error"); })
            .finally(function () {
                delete btn.dataset.busy;
                btn.disabled = false;
            });
    }

    var STATUS_META = {
        DRAFT: ["Nháp", "#4f46e5"],
        CONFIRMED: ["Đã xác nhận", "#1e7e34"],
        CANCELLED: ["Đã hủy", "#6c757d"]
    };

    var inited = false;
    var editingId = null;   // id phiếu nháp đang sửa (null = tạo mới)
    var impRows = [];       // dòng hàng trong phiếu đang soạn
    var impProducts = [];   // sản phẩm active của danh mục đang chọn
    var impCategories = [];

    // ==================== Khởi tạo ====================

    function init() {
        if (inited) return;
        inited = true;

        // main.js biến mọi select thành nice-select ngay khi tải trang (khi các
        // select này còn rỗng) -> dropdown không cập nhật option mới. Hủy
        // nice-select để dùng select thường (giống employee.js).
        if (window.jQuery && window.jQuery.fn && window.jQuery.fn.niceSelect) {
            ["impCategory", "impProduct", "impSize", "impFilterStatus"].forEach(function (id) {
                var $el = window.jQuery("#" + id);
                if (!$el.hasClass("nice-select-destroyed")) {
                    $el.niceSelect("destroy");
                    $el.addClass("nice-select-destroyed");
                }
            });
        }

        bindEvents();
        loadCategories();
        loadReceipts();
        renderRows();
    }

    function bindEvents() {
        $("impCategory").addEventListener("change", onCategoryChange);
        $("impProduct").addEventListener("change", syncProductHint);
        $("impAddRow").addEventListener("click", onAddRow);
        $("impRows").addEventListener("click", onRemoveRow);
        $("impSaveDraft").addEventListener("click", function () {
            withLock(this, saveReceipt.bind(null, false));
        });
        $("impConfirm").addEventListener("click", function () {
            withLock(this, function () {
                var total = impRows.reduce(function (s, r) { return s + r.quantity; }, 0);
                if (!window.confirm("Xác nhận nhập kho " + total + " sản phẩm vào chi nhánh? "
                        + "Sau khi xác nhận tồn kho chi nhánh sẽ được cộng.")) {
                    return Promise.resolve();
                }
                return saveReceipt(true).then(function () {
                    showAlert("Đã xác nhận nhập kho — tồn kho chi nhánh đã được cộng.", "success");
                });
            });
        });
        $("impReset").addEventListener("click", resetForm);
        $("impFilterApply").addEventListener("click", function () {
            withLock(this, loadReceipts);
        });
        $("impList").addEventListener("click", onListAction);
        $("impDetail").addEventListener("click", onListAction);
    }

    // ==================== Danh mục + sản phẩm ====================

    function loadCategories() {
        return api("GET", SHOP_API + "/categories").then(function (res) {
            impCategories = res.data || [];
            var sel = $("impCategory");
            sel.innerHTML = impCategories.map(function (c) {
                return '<option value="' + c.id + '">' + escapeHtml(c.name) + "</option>";
            }).join("") || '<option value="">(Chưa có danh mục)</option>';
            onCategoryChange();
        });
    }

    function onCategoryChange() {
        var catId = $("impCategory").value;
        $("impProduct").innerHTML = '<option value="">Đang tải…</option>';
        $("impSize").innerHTML = '<option value="">—</option>';
        $("impProductHint").textContent = "";
        if (!catId) { impProducts = []; return; }
        api("GET", SHOP_API + "/products?categoryId=" + encodeURIComponent(catId))
            .then(function (res) {
                impProducts = res.data || [];
                $("impProduct").innerHTML = '<option value="">— Chọn sản phẩm —</option>' +
                    impProducts.map(function (p) {
                        return '<option value="' + p.id + '">' + escapeHtml(p.name) +
                            (p.baseSku ? " (" + escapeHtml(p.baseSku) + ")" : "") + "</option>";
                    }).join("");
                syncProductHint();
            })
            .catch(function (err) { showAlert(err.message, "error"); });
    }

    function syncProductHint() {
        var p = currentProduct();
        var sizeSelect = $("impSize");
        if (!p) {
            $("impProductHint").textContent = "";
            sizeSelect.innerHTML = '<option value="">—</option>';
            return;
        }
        var sizes = p.sizesList || [];
        sizeSelect.innerHTML = sizes.length
            ? sizes.map(function (s) { return '<option value="' + escapeHtml(s) + '">' + escapeHtml(s) + "</option>"; }).join("")
            : '<option value="">—</option>';
        $("impProductHint").textContent = "Tồn tại chi nhánh: " + (p.branchStock || 0) +
            (p.brand ? " — Thương hiệu: " + p.brand : "") +
            (p.color ? " — Màu: " + p.color : "");
    }

    function currentProduct() {
        var id = $("impProduct").value;
        return impProducts.find(function (x) { return String(x.id) === String(id); }) || null;
    }

    // ==================== Dòng hàng trong phiếu ====================

    function onAddRow() {
        hideAlert();
        var p = currentProduct();
        if (!p) {
            showAlert("Vui lòng chọn sản phẩm trước khi thêm dòng.", "error");
            return;
        }
        var sizes = p.sizesList || [];
        var size = sizes.length ? $("impSize").value : null;
        if (sizes.length && !size) {
            showAlert('Sản phẩm "' + p.name + '" phải chọn size.', "error");
            return;
        }
        var qty = parseInt($("impQty").value, 10);
        if (!qty || qty < 1) {
            showAlert("Số lượng nhập phải ít nhất là 1.", "error");
            return;
        }
        var row = impRows.find(function (r) {
            return String(r.productId) === String(p.id) && (r.size || "") === (size || "");
        });
        if (row) {
            row.quantity += qty;
        } else {
            impRows.push({
                productId: p.id,
                productName: p.name,
                baseSku: p.baseSku || "",
                size: size,
                quantity: qty
            });
        }
        $("impQty").value = 1;
        renderRows();
    }

    function onRemoveRow(e) {
        var btn = e.target.closest("button[data-imp-remove]");
        if (!btn) return;
        impRows.splice(parseInt(btn.getAttribute("data-imp-remove"), 10), 1);
        renderRows();
    }

    function renderRows() {
        var wrap = $("impRows");
        if (!impRows.length) {
            wrap.innerHTML = '<p class="text-muted mb-0">Chưa có dòng hàng nào trong phiếu.</p>';
            return;
        }
        var total = impRows.reduce(function (s, r) { return s + r.quantity; }, 0);
        wrap.innerHTML = '<div class="table-responsive"><table class="table table-sm" style="font-size:13px;">'
            + "<thead><tr><th>Sản phẩm</th><th>SKU</th><th>Size</th><th>Số lượng</th><th></th></tr></thead><tbody>"
            + impRows.map(function (r, idx) {
                return "<tr><td>" + escapeHtml(r.productName) + "</td>"
                    + "<td>" + escapeHtml(r.baseSku || "—") + "</td>"
                    + "<td>" + escapeHtml(r.size || "—") + "</td>"
                    + "<td>" + r.quantity + "</td>"
                    + '<td><button type="button" class="btn-act btn-act-delete" data-imp-remove="' + idx + '"><i class="fa fa-trash"></i></button></td></tr>';
            }).join("")
            + "</tbody><tfoot><tr><td colspan='3' class='text-right'><b>Tổng số lượng</b></td>"
            + "<td colspan='2'><b>" + total + "</b></td></tr></tfoot></table></div>";
    }

    // ==================== Lưu / xác nhận ====================

    function buildRequestBody() {
        if (!impRows.length) {
            return Promise.reject(new Error("Phiếu nhập phải có ít nhất một dòng hàng."));
        }
        return {
            sourceNote: $("impSourceNote").value.trim() || null,
            note: $("impNote").value.trim() || null,
            items: impRows.map(function (r) {
                return { productId: r.productId, size: r.size, quantity: r.quantity };
            })
        };
    }

    /** Lưu phiếu (nháp). confirmNow = true thì lưu xong gọi xác nhận ngay. */
    function saveReceipt(confirmNow) {
        hideAlert();
        var body = buildRequestBody();
        var saveCall = editingId
            ? api("PUT", IMPORT_API + "/" + editingId, body)
            : api("POST", IMPORT_API, body);
        return saveCall.then(function (res) {
            if (!confirmNow) {
                showAlert("Đã lưu phiếu nháp " + res.data.receiptCode + ".", "success");
            }
            if (confirmNow) {
                return api("POST", IMPORT_API + "/" + res.data.id + "/confirm")
                    .then(function () {
                        resetForm();
                        loadReceipts();
                    });
            }
            editingId = res.data.id;
            $("impFormTitle").textContent = "Sửa phiếu nháp " + res.data.receiptCode;
            $("impReset").style.display = "inline-block";
            loadReceipts();
        });
    }

    function resetForm() {
        editingId = null;
        impRows = [];
        $("impSourceNote").value = "";
        $("impNote").value = "";
        $("impFormTitle").textContent = "Lập phiếu nhập hàng";
        $("impReset").style.display = "none";
        renderRows();
    }

    // ==================== Danh sách phiếu ====================

    function loadReceipts() {
        var params = [];
        var status = $("impFilterStatus").value;
        var from = $("impFilterFrom").value;
        var to = $("impFilterTo").value;
        if (status) params.push("status=" + encodeURIComponent(status));
        if (from) params.push("fromDate=" + encodeURIComponent(from));
        if (to) params.push("toDate=" + encodeURIComponent(to));
        var qs = params.length ? "?" + params.join("&") : "";
        return api("GET", IMPORT_API + qs).then(function (res) {
            renderReceiptList(res.data || []);
        });
    }

    function renderReceiptList(list) {
        var wrap = $("impList");
        $("impListTitle").textContent = "Danh sách phiếu nhập (" + list.length + ")";
        if (!list.length) {
            wrap.innerHTML = '<p class="text-muted">Chưa có phiếu nhập nào phù hợp bộ lọc.</p>';
            return;
        }
        wrap.innerHTML = '<div class="table-responsive"><table class="table table-sm" style="font-size:13px;">'
            + "<thead><tr><th>Mã phiếu</th><th>Ngày lập</th><th>Nguồn nhập</th><th>Tổng SL</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>"
            + list.map(function (r) {
                var meta = STATUS_META[r.status] || [r.status, "#333"];
                var actions = '<button type="button" class="btn-act" data-imp-view="' + r.id + '" title="Xem chi tiết"><i class="fa fa-eye"></i></button>';
                if (r.status === "DRAFT") {
                    actions += ' <button type="button" class="btn-act" data-imp-edit="' + r.id + '" title="Sửa nháp"><i class="fa fa-pencil"></i></button>'
                        + ' <button type="button" class="btn-act btn-act-delete" data-imp-cancel="' + r.id + '" title="Hủy phiếu"><i class="fa fa-ban"></i></button>';
                } else if (r.status === "CONFIRMED") {
                    actions += ' <button type="button" class="btn-act btn-act-delete" data-imp-cancel="' + r.id + '" title="Hủy phiếu (trừ ngược tồn)"><i class="fa fa-undo"></i></button>';
                }
                return "<tr><td><b>" + escapeHtml(r.receiptCode) + "</b></td>"
                    + "<td>" + formatDateTime(r.createdAt) + "</td>"
                    + "<td>" + escapeHtml(r.sourceNote || "—") + "</td>"
                    + "<td>" + r.totalQuantity + "</td>"
                    + '<td><span class="badge" style="background:' + meta[1] + ';color:#fff;">' + meta[0] + "</span></td>"
                    + "<td>" + actions + "</td></tr>";
            }).join("")
            + "</tbody></table></div>";
    }

    function onListAction(e) {
        var viewBtn = e.target.closest("button[data-imp-view]");
        var editBtn = e.target.closest("button[data-imp-edit]");
        var cancelBtn = e.target.closest("button[data-imp-cancel]");
        if (viewBtn) {
            withLock(viewBtn, function () { return openDetail(viewBtn.getAttribute("data-imp-view")); });
        } else if (editBtn) {
            withLock(editBtn, function () { return loadDraftIntoForm(editBtn.getAttribute("data-imp-edit")); });
        } else if (cancelBtn) {
            var id = cancelBtn.getAttribute("data-imp-cancel");
            withLock(cancelBtn, function () {
                if (!window.confirm("Hủy phiếu nhập này? Phiếu đã xác nhận sẽ trừ ngược tồn kho chi nhánh.")) {
                    return Promise.resolve();
                }
                return api("POST", IMPORT_API + "/" + id + "/cancel").then(function () {
                    showAlert("Đã hủy phiếu nhập.", "success");
                    if (String(editingId) === String(id)) resetForm();
                    loadReceipts();
                    $("impDetail").innerHTML = "";
                });
            });
        }
    }

    function openDetail(id) {
        return api("GET", IMPORT_API + "/" + id).then(function (res) {
            renderDetail(res.data);
        });
    }

    function renderDetail(r) {
        var meta = STATUS_META[r.status] || [r.status, "#333"];
        var items = (r.items || []).map(function (it) {
            return "<tr><td>" + escapeHtml(it.productName) + "</td>"
                + "<td>" + escapeHtml(it.baseSku || "—") + "</td>"
                + "<td>" + escapeHtml(it.size || "—") + "</td>"
                + "<td>" + it.quantity + "</td></tr>";
        }).join("");
        $("impDetail").innerHTML =
            '<div class="address-card mt-3">'
            + '<h6 class="font-weight-bold mb-2">Phiếu ' + escapeHtml(r.receiptCode)
            + ' <span class="badge" style="background:' + meta[1] + ';color:#fff;">' + meta[0] + "</span></h6>"
            + "<p class='text-muted mb-2' style='font-size:13px;'>"
            + "Ngày lập: " + formatDateTime(r.createdAt)
            + " · Người lập: " + escapeHtml(r.createdByFullName || r.createdByUsername || "—")
            + (r.confirmedAt ? " · Xác nhận: " + formatDateTime(r.confirmedAt)
                + " bởi " + escapeHtml(r.confirmedByFullName || r.confirmedByUsername || "—") : "")
            + (r.cancelledAt ? " · Hủy: " + formatDateTime(r.cancelledAt) : "")
            + "<br>Nguồn nhập: " + escapeHtml(r.sourceNote || "—")
            + (r.note ? " · Ghi chú: " + escapeHtml(r.note) : "")
            + "</p>"
            + '<div class="table-responsive"><table class="table table-sm" style="font-size:13px;">'
            + "<thead><tr><th>Sản phẩm</th><th>SKU</th><th>Size</th><th>Số lượng</th></tr></thead><tbody>"
            + items
            + "</tbody></table></div></div>";
        $("impDetail").scrollIntoView({ behavior: "smooth", block: "nearest" });
    }

    function loadDraftIntoForm(id) {
        return api("GET", IMPORT_API + "/" + id).then(function (res) {
            var r = res.data;
            if (r.status !== "DRAFT") {
                throw new Error("Chỉ phiếu nháp mới được sửa.");
            }
            editingId = r.id;
            impRows = (r.items || []).map(function (it) {
                return {
                    productId: it.productId,
                    productName: it.productName,
                    baseSku: it.baseSku || "",
                    size: it.size,
                    quantity: it.quantity
                };
            });
            $("impSourceNote").value = r.sourceNote || "";
            $("impNote").value = r.note || "";
            $("impFormTitle").textContent = "Sửa phiếu nháp " + r.receiptCode;
            $("impReset").style.display = "inline-block";
            renderRows();
            $("impFormCard").scrollIntoView({ behavior: "smooth", block: "start" });
        });
    }

    // ==================== Kích hoạt theo vai trò ====================

    function hasManagerRole() {
        var s = getSession();
        if (!s) return false;
        var user = s.user && typeof s.user === "object" ? s.user : s;
        var r = String(user.role || s.role || user.roleName || s.roleName || "").toUpperCase();
        return r.indexOf("MANAGER") !== -1 || r.indexOf("QUẢN LÝ") !== -1 || r.indexOf("QUAN LY") !== -1;
    }

    function boot() {
        if (!hasManagerRole()) return;
        // Lắng nghe click nút sub-tab "Nhập hàng" — profile.js chỉ toggle class,
        // việc tải dữ liệu do file này tự xử lý (lazy, chỉ tải lần đầu).
        var btn = document.querySelector('.shop-subbtn[data-sub="shopSubImports"]');
        if (btn) {
            btn.addEventListener("click", init);
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", boot);
    } else {
        boot();
    }
})();

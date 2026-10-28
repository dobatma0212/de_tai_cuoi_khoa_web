/**
 * employee.js
 * Tab "Nhân viên chi nhánh" trong profile.html — chỉ hiển thị cho ROLE_EMPLOYEE.
 * Chức năng: xem thông tin chi nhánh được phân công (chỉ đọc),
 * xác nhận đơn hàng, bán hàng tại quầy,
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
                $("empSubOffline").classList.toggle("active", sub === "empSubOffline");
                $("empSubWork").classList.toggle("active", sub === "empSubWork");
                if (sub === "empSubOrders") loadEmpOrders();
                if (sub === "empSubOffline") initOfflineSection();
            });
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
    // Realtime (WebSocket): tu dong tai lai danh sach don khi co thay doi
    window.addEventListener("sc:order-event", function () {
        var el = $("empOrderList");
        if (el && el.offsetParent !== null) { loadEmpOrders(); }
    });

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
                if (o.transporterId) {
                    // Khách đã chọn đơn vị vận chuyển lúc đặt hàng — chỉ hiển thị, không chọn lại
                    actions = '<div class="mt-2 d-flex flex-wrap align-items-center">' +
                        '<small class="mr-2 mb-1"><span class="badge badge-light" style="border:1px solid #ddd;">' +
                        'VC khách chọn: ' + escapeHtml(o.transporterName) + " (" + formatVnd(o.shippingFee) + ")</span></small>" +
                        '<button type="button" class="btn btn-sm btn-store mr-2 mb-1" data-confirm-order="' + o.id + '">Xác nhận đơn</button>' +
                        '<button type="button" class="btn btn-sm btn-link btn-link-danger mb-1" data-cancel-order="' + o.id + '">Hủy đơn</button>' +
                        "</div>" +
                        '<small class="text-muted">Xác nhận đơn này sẽ được tính hoa hồng khi giao thành công.</small>';
                } else {
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
                }
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
            var isOffline = o.orderType === "off";
            var typeBadge = isOffline
                ? ' <span class="badge badge-dark">Tại quầy</span>'
                : ' <span class="badge badge-light" style="border:1px solid #ddd;">Online</span>';
            var printBtn = isOffline && o.status !== "CANCELLED"
                ? '<button type="button" class="btn btn-sm btn-outline-secondary mt-1" data-print-order="' + o.id + '"><i class="fa fa-print"></i> In hóa đơn</button>'
                : "";
            return '<div class="address-card">' +
                '<div class="d-flex justify-content-between flex-wrap">' +
                "<div><b>" + escapeHtml(o.orderCode) + "</b> " +
                '<span class="badge ' + (statusColors[o.status] || "badge-light") + '">' +
                escapeHtml(o.statusLabel || o.status) + "</span>" + typeBadge +
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
                printBtn +
                actions +
                "</div>";
        }).join("");

        container.onclick = function (e) {
            var btn = e.target.closest("button");
            if (!btn) return;
            var id;
            if (btn.getAttribute("data-print-order") !== null) {
                id = parseInt(btn.getAttribute("data-print-order"), 10);
                var offOrder = empOrderCache.find(function (x) { return x.id === id; });
                if (offOrder) printInvoice(offOrder, 0);
            } else if (btn.getAttribute("data-confirm-order") !== null) {
                id = btn.getAttribute("data-confirm-order");
                var select = btn.closest(".address-card").querySelector(".emp-transporter-select");
                var transporterId = select ? select.value : "";
                if (select && !transporterId) {
                    alert("Vui lòng chọn đơn vị vận chuyển trước khi xác nhận.");
                    return;
                }
                // Đơn đã có vận chuyển khách chọn thì không gửi transporterId nữa
                var confirmUrl = "/orders/" + id + "/status?status=CONFIRMED" +
                    (transporterId ? "&transporterId=" + transporterId : "");
                empApi("PATCH", confirmUrl)
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

    // ==================== Bán tại quầy (đơn offline) ====================
    var offlineProductsLoaded = false;
    var offlineProducts = [];
    var offlineCart = [];

    function initOfflineSection() {
        // main.js biến mọi select thành nice-select ngay khi tải trang (khi select
        // này còn trống) -> dropdown không bao giờ cập nhật option mới và làm vỡ layout.
        // Hủy nice-select để dùng select thường, luôn hiển thị đúng option hiện có.
        if (window.jQuery && window.jQuery.fn && window.jQuery.fn.niceSelect) {
            ["empOfflineProduct", "empOfflineSize"].forEach(function (id) {
                var $el = window.jQuery("#" + id);
                if (!$el.hasClass("nice-select-destroyed")) {
                    $el.niceSelect("destroy");
                    $el.addClass("nice-select-destroyed");
                }
            });
        }
        if (!offlineProductsLoaded) {
            offlineProductsLoaded = true;
            // Lấy TẤT CẢ sản phẩm của chi nhánh (không lọc theo danh mục đang chọn)
            empApi("GET", "/products").then(function (res) {
                offlineProducts = res.data || [];
                renderOfflineProductOptions();
            }).catch(function (err) {
                offlineProductsLoaded = false; // load lại lần sau nếu lỗi
                showAlert("empOfflineAlert", err.message, "error");
            });
        }
        if (!$("empOfflineAdd").dataset.bound) {
            $("empOfflineAdd").dataset.bound = "1";
            bindOfflineSection();
        }
        renderOfflineCart();
    }

    function renderOfflineProductOptions() {
        var select = $("empOfflineProduct");
        var current = select.value;
        // Chỉ bán được sản phẩm đang mở bán (đồng bộ trạng thái từ phần quản trị)
        var sellable = offlineProducts.filter(function (p) { return !p.status || p.status === "active"; });
        select.innerHTML = '<option value="">— Chọn sản phẩm —</option>' + sellable.map(function (p) {
            return '<option value="' + p.id + '">' + escapeHtml(p.name) +
                " — " + formatVnd(p.price) + " (còn " + (p.branchStock || 0) + ")</option>";
        }).join("");
        if (current) select.value = current;
        syncOfflineProductHint();
    }

    /** Đồng bộ danh sách size theo sản phẩm đang chọn — nhân viên chỉ được chọn, không nhập tay */
    function syncOfflineProductHint() {
        var p = offlineProducts.find(function (x) {
            return String(x.id) === $("empOfflineProduct").value;
        });
        var sizeSelect = $("empOfflineSize");
        if (!p) {
            sizeSelect.innerHTML = '<option value="">—</option>';
            sizeSelect.disabled = true;
            $("empOfflineProductHint").textContent = "";
            return;
        }
        var sizes = p.sizesList || [];
        sizeSelect.innerHTML = sizes.length
            ? sizes.map(function (s) { return '<option value="' + escapeHtml(s) + '">' + escapeHtml(s) + "</option>"; }).join("")
            : '<option value="">—</option>';
        sizeSelect.disabled = !sizes.length;
        $("empOfflineProductHint").textContent =
            "Giá: " + formatVnd(p.price) + (p.onFlashSale && p.originalPrice && Number(p.originalPrice) > Number(p.price) ? " (Flash Sale, giá gốc " + formatVnd(p.originalPrice) + ")" : "") + " — Tồn kho chi nhánh: " + (p.branchStock || 0) +
            (p.baseSku ? " — SKU: " + p.baseSku : "") +
            (p.brand ? " — Thương hiệu: " + p.brand : "") +
            (p.color ? " — Màu: " + p.color : "");
    }

    function bindOfflineSection() {
        $("empOfflineProduct").addEventListener("change", syncOfflineProductHint);
        $("empOfflineAdd").addEventListener("click", function () {
            hideAlert("empOfflineAlert");
            var p = offlineProducts.find(function (x) {
                return String(x.id) === $("empOfflineProduct").value;
            });
            if (!p) {
                showAlert("empOfflineAlert", "Vui lòng chọn sản phẩm trước khi thêm vào đơn.", "error");
                return;
            }
            var sizes = p.sizesList || [];
            var size = sizes.length ? $("empOfflineSize").value : null;
            if (sizes.length && !size) {
                showAlert("empOfflineAlert", 'Sản phẩm "' + p.name + '" phải chọn size.', "error");
                return;
            }
            var qty = parseInt($("empOfflineQty").value, 10);
            if (!qty || qty < 1) {
                showAlert("empOfflineAlert", "Số lượng phải ít nhất là 1.", "error");
                return;
            }
            // Tồn kho hiển thị là của cả đơn đang nhập (các dòng trùng sản phẩm+size cộng dồn)
            var alreadyInCart = offlineCart
                .filter(function (it) { return it.productId === p.id && (it.size || "") === (size || ""); })
                .reduce(function (sum, it) { return sum + it.quantity; }, 0);
            if (alreadyInCart + qty > (p.branchStock || 0)) {
                showAlert("empOfflineAlert", 'Sản phẩm "' + p.name + (size ? " (size " + size + ")" : "") +
                    '" chỉ còn ' + (p.branchStock || 0) + " cái tại chi nhánh.", "error");
                return;
            }
            var existing = offlineCart.find(function (it) {
                return it.productId === p.id && (it.size || "") === (size || "");
            });
            if (existing) {
                existing.quantity += qty;
            } else {
                offlineCart.push({
                    productId: p.id, name: p.name, price: Number(p.price) || 0,
                    size: size, color: p.color || "", quantity: qty, stock: p.branchStock || 0
                });
            }
            renderOfflineCart();
        });
        $("empOfflineSave").addEventListener("click", saveOfflineOrder);
    }

    function renderOfflineCart() {
        var container = $("empOfflineCart");
        if (!container) return;
        if (!offlineCart.length) {
            container.innerHTML = '<p class="text-muted">Chưa có sản phẩm nào trong đơn.</p>';
            return;
        }
        var total = offlineCart.reduce(function (sum, it) { return sum + it.price * it.quantity; }, 0);
        container.innerHTML = '<div class="table-responsive"><table class="table table-sm" style="font-size:13px;">'
            + "<thead><tr><th>Sản phẩm</th><th>Size</th><th>Đơn giá</th><th>SL</th><th>Thành tiền</th><th></th></tr></thead><tbody>"
            + offlineCart.map(function (it, idx) {
                return "<tr><td>" + escapeHtml(it.name) + "</td>"
                    + "<td>" + escapeHtml(it.size || "—") + "</td>"
                    + "<td>" + formatVnd(it.price) + "</td>"
                    + "<td>" + it.quantity + "</td>"
                    + "<td>" + formatVnd(it.price * it.quantity) + "</td>"
                    + '<td><button type="button" class="btn-act btn-act-delete" data-offline-remove="' + idx + '"><i class="fa fa-trash"></i></button></td></tr>';
            }).join("")
            + "</tbody><tfoot><tr><td colspan='4' class='text-right'><b>Tạm tính</b></td>"
            + "<td colspan='2'><b style='color:#e53637;'>" + formatVnd(total) + "</b></td></tr></tfoot></table></div>";

        container.onclick = function (e) {
            var btn = e.target.closest("button[data-offline-remove]");
            if (!btn) return;
            offlineCart.splice(parseInt(btn.getAttribute("data-offline-remove"), 10), 1);
            renderOfflineCart();
        };
    }

    function saveOfflineOrder() {
        hideAlert("empOfflineAlert");
        if (!offlineCart.length) {
            showAlert("empOfflineAlert", "Vui lòng thêm ít nhất một sản phẩm vào đơn.", "error");
            return;
        }
        var customerName = $("empOfflineCustomerName").value.trim();
        if (!customerName) {
            showAlert("empOfflineAlert", "Vui lòng nhập tên khách hàng.", "error");
            return;
        }
        var paidRaw = $("empOfflinePaid").value;
        var body = {
            items: offlineCart.map(function (it) {
                return { productId: it.productId, quantity: it.quantity, size: it.size };
            }),
            customerName: customerName,
            customerPhone: $("empOfflineCustomerPhone").value.trim() || null,
            paidAmount: paidRaw ? Number(paidRaw) : null,
            note: $("empOfflineNote").value.trim() || null
        };
        $("empOfflineSave").disabled = true;
        empApi("POST", "/orders/offline", body).then(function (res) {
            showAlert("empOfflineAlert", res.message || "Tạo đơn bán tại quầy thành công!", "success");
            printInvoice(res.data, Number(paidRaw) || 0);
            resetOfflineForm();
            offlineProductsLoaded = false; // load lại để cập nhật tồn kho
            loadStats($("empMonth").value || null);
        }).catch(function (err) {
            showAlert("empOfflineAlert", err.message, "error");
        }).finally(function () {
            $("empOfflineSave").disabled = false;
        });
    }

    function resetOfflineForm() {
        offlineCart = [];
        renderOfflineCart();
        $("empOfflineProduct").value = "";
        $("empOfflineQty").value = 1;
        syncOfflineProductHint();
        $("empOfflineCustomerName").value = "";
        $("empOfflineCustomerPhone").value = "";
        $("empOfflinePaid").value = "";
        $("empOfflineNote").value = "";
    }

    /** In hóa đơn: mở cửa sổ mới chứa hóa đơn và gọi hộp thoại in */
    function printInvoice(order, paidAmount) {
        var s = getSession();
        var user = s && s.user ? s.user : {};
        var rows = (order.items || []).map(function (it) {
            return "<tr><td style='padding:4px 0;'>" + escapeHtml(it.productName) +
                (it.size ? " (size " + escapeHtml(it.size) + ")" : "") +
                (it.color ? " - " + escapeHtml(it.color) : "") + "</td>"
                + "<td align='center'>" + it.quantity + "</td>"
                + "<td align='right'>" + formatVnd(it.price) + "</td>"
                + "<td align='right'>" + formatVnd(it.subtotal) + "</td></tr>";
        }).join("");
        var change = paidAmount > 0 ? Math.max(0, paidAmount - Number(order.grandTotal)) : 0;
        var html = "<html><head><meta charset='utf-8'><title>Hóa đơn " + escapeHtml(order.orderCode) + "</title>"
            + "<style>body{font-family:Arial,sans-serif;font-size:13px;max-width:480px;margin:0 auto;padding:16px;color:#222;}"
            + "h2,h4,p{margin:4px 0;}table{width:100%;border-collapse:collapse;margin-top:10px;}"
            + "td{border-bottom:1px dashed #ccc;padding:4px 0;}th{text-align:left;border-bottom:1px solid #222;padding:4px 0;}"
            + ".total{font-weight:bold;font-size:15px;}.muted{color:#666;font-size:12px;}"
            + "@media print{button{display:none;}}</style></head><body>"
            + "<h2 align='center'>" + escapeHtml(order.shopName || "StoreChain") + "</h2>"
            + "<p align='center' class='muted'>HÓA ĐƠN BÁN HÀNG (Bán tại quầy)</p>"
            + "<p>Mã đơn: <b>" + escapeHtml(order.orderCode) + "</b></p>"
            + "<p>Ngày: " + (order.createdAt ? new Date(order.createdAt).toLocaleString("vi-VN") : new Date().toLocaleString("vi-VN")) + "</p>"
            + "<p>Khách hàng: " + escapeHtml(order.customerName || "Khách vãng lai")
            + (order.receiverPhone ? " — " + escapeHtml(order.receiverPhone) : "") + "</p>"
            + "<p>Thu ngân: " + escapeHtml(user.fullName || user.username || "") + "</p>"
            + "<table><thead><tr><th>Sản phẩm</th><th>SL</th><th>Đơn giá</th><th>Tiền</th></tr></thead>"
            + "<tbody>" + rows + "</tbody></table>"
            + "<p class='total' style='margin-top:10px;'>TỔNG CỘNG: " + formatVnd(order.grandTotal) + "</p>"
            + (paidAmount > 0 ? "<p>Tiền khách trả: " + formatVnd(paidAmount)
                + " — Tiền thừa: <b>" + formatVnd(change) + "</b></p>" : "")
            + (order.note ? "<p class='muted'>Ghi chú: " + escapeHtml(order.note) + "</p>" : "")
            + "<p align='center' class='muted' style='margin-top:16px;'>Cảm ơn quý khách! Hàng đã bán vui lòng kiểm tra kỹ trước khi rời quầy.</p>"
            + "<p align='center'><button onclick='window.print()'>In hóa đơn</button></p>"
            + "</body></html>";
        var w = window.open("", "_blank", "width=520,height=680");
        if (!w) {
            alert("Trình duyệt đã chặn cửa sổ in hóa đơn. Hãy cho phép pop-up để in.");
            return;
        }
        w.document.write(html);
        w.document.close();
        w.focus();
        w.print();
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

/**
 * manager-employees.js
 * Sub-tab "Nhân viên" trong tab Quản lý chi nhánh (chỉ MANAGER):
 * - Bổ nhiệm nhân viên (tạo tài khoản mới hoặc thăng USER hiện có)
 * - Sa thải nhân viên (trả về quyền khách hàng, giữ lịch sử để tra cứu)
 * - Xem chi tiết từng nhân viên: họ tên, SĐT, số công, đơn đã bán/không đạt,
 *   doanh số và hoa hồng theo tháng — kể cả nhân viên đã sa thải
 * Trách nhiệm nhân sự chi nhánh thuộc về quản lý chi nhánh.
 */
(function () {
    "use strict";

    var STAFF_API = "/api/manager/shop/employees";

    function $(id) { return document.getElementById(id); }

    function getSession() {
        return window.ScAuth ? window.ScAuth.getSession() : null;
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

    function staffApi(method, path, body) {
        var s = getSession();
        var token = s && s.token ? s.token : localStorage.getItem("jwt_token");
        var headers = { "Content-Type": "application/json" };
        if (token) headers["Authorization"] = "Bearer " + token;
        return fetch(STAFF_API + path, {
            method: method,
            headers: headers,
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

    function showAlert(message, type) {
        var el = $("staffAlert");
        el.className = "alert auth-alert auth-alert--" + (type || "error");
        el.textContent = message;
        el.style.display = "block";
    }

    function escapeHtml(str) {
        return String(str == null ? "" : str)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    var staffFilter = "all"; // all | working | fired
    var lastStaff = [];

    function loadStaff() {
        staffApi("GET", "").then(function (res) {
            lastStaff = res.data || [];
            renderStaff(lastStaff);
        }).catch(function (err) {
            showAlert(err.message, "error");
        });
    }

    function renderStaff(staff) {
        var workingCount = staff.filter(function (u) { return u.employed; }).length;
        var firedCount = staff.length - workingCount;
        var counts = { all: staff.length, working: workingCount, fired: firedCount };
        var filterWrap = $("staffFilter");
        if (filterWrap) {
            filterWrap.querySelectorAll("button[data-filter]").forEach(function (b) {
                var c = counts[b.getAttribute("data-filter")];
                var label = b.getAttribute("data-filter") === "all" ? "Tất cả"
                    : b.getAttribute("data-filter") === "working" ? "Đang làm việc" : "Đã sa thải";
                b.innerHTML = escapeHtml(label) + ' <span class="filter-count">(' + c + ")</span>";
            });
        }
        var filtered = staff.filter(function (u) {
            if (staffFilter === "working") return u.employed;
            if (staffFilter === "fired") return !u.employed;
            return true;
        });
        var title = "Danh sách nhân viên (" + filtered.length + ")";
        if (staffFilter === "working") title += " đang làm việc";
        else if (staffFilter === "fired") title += " đã sa thải";
        $("staffListTitle").textContent = title;
        var wrap = $("staffList");
        if (!filtered.length) {
            wrap.innerHTML = '<p class="text-muted">' + (staff.length
                ? "Không có nhân viên nào trong mục lọc này."
                : "Chi nhánh chưa có nhân viên nào. Hãy bổ nhiệm ở form phía trên.") + "</p>";
            return;
        }
        // Sắp xếp: nhân viên đang làm việc trước, nhân viên đã sa thải sau
        filtered.sort(function (a, b) { return (b.employed ? 1 : 0) - (a.employed ? 1 : 0); });
        var html = '<p class="text-muted" style="font-size:13px;">'
            + "Nhấp vào một nhân viên để xem số công, đơn hàng đã bán và hoa hồng "
            + "(áp dụng cả nhân viên đã sa thải).</p>";
        filtered.forEach(function (u) {
            var firedBadge = u.employed
                ? ""
                : ' <span class="badge badge-secondary">Đã sa thải</span>';
            html += '<div class="address-card" style="margin-bottom:12px;">'
                + '<div class="d-flex align-items-start" role="button" data-view="' + u.id + '" '
                + 'title="Nhấp để xem chi tiết công việc và hoa hồng">'
                + '<div class="flex-grow-1">'
                + '<div class="font-weight-bold">' + escapeHtml(u.fullName) + firedBadge
                + ' <span class="badge badge-dark">@' + escapeHtml(u.username) + "</span></div>"
                + '<div class="text-muted" style="font-size:13px;">' + escapeHtml(u.email)
                + (u.phone ? " — SĐT: " + escapeHtml(u.phone) : "") + "</div>"
                + '<div class="text-muted" style="font-size:12px;">Trạng thái: ' + escapeHtml(u.status || "active") + "</div>"
                + "</div>"
                + '<div class="text-nowrap" style="margin-left:10px;">'
                + (u.employed
                    ? '<button type="button" class="btn btn-sm btn-outline-danger" data-fire="' + u.id + '">Sa thải</button>'
                    : "")
                + "</div></div>"
                + '<div id="empDetail-' + u.id + '" style="display:none;margin-top:12px;'
                + "border-top:1px solid #eee;padding-top:12px;\"></div>"
                + "</div>";
        });
        wrap.innerHTML = html;
        wrap.querySelectorAll("[data-view]").forEach(function (row) {
            row.addEventListener("click", function (e) {
                // Bỏ qua click vào nút "Sa thải"
                if (e.target.closest("button[data-fire]")) return;
                toggleDetail(Number(row.getAttribute("data-view")));
            });
        });
        wrap.querySelectorAll("button[data-fire]").forEach(function (b) {
            b.addEventListener("click", function () {
                fireStaff(Number(b.getAttribute("data-fire")), b);
            });
        });
    }

    // ==================== Chi tiết nhân viên (công / đơn / hoa hồng) ====================

    var openDetailId = null;

    function formatVnd(v) {
        return Number(v || 0).toLocaleString("vi-VN") + "\u20ab";
    }

    function detailEl(id) { return document.getElementById("empDetail-" + id); }

    function toggleDetail(id) {
        var el = detailEl(id);
        if (!el) return;
        if (openDetailId !== null && openDetailId !== id) {
            var other = detailEl(openDetailId);
            if (other) other.style.display = "none";
        }
        if (el.style.display === "none") {
            el.style.display = "block";
            openDetailId = id;
            loadDetail(id);
        } else {
            el.style.display = "none";
            openDetailId = null;
        }
    }

    function loadDetail(id, month) {
        var el = detailEl(id);
        if (!el) return;
        el.innerHTML = '<p class="text-muted">Đang tải thống kê nhân viên...</p>';
        var qs = month ? "?month=" + encodeURIComponent(month) : "";
        staffApi("GET", "/" + id + "/stats" + qs).then(function (res) {
            renderDetail(id, res.data);
        }).catch(function (err) {
            el.innerHTML = '<div class="alert auth-alert auth-alert--error">' + escapeHtml(err.message) + "</div>";
        });
    }

    function renderDetail(id, stats) {
        if (!stats) return;
        var el = detailEl(id);
        var rate = Number(stats.commissionRate || 0) * 100;
        var now = new Date();
        var currentMonth = now.getFullYear() + "-" + String(now.getMonth() + 1).padStart(2, "0");

        var html = '<div class="d-flex align-items-center mb-2">'
            + '<h6 class="font-weight-bold mb-0 mr-2">Thống kê tháng</h6>'
            + '<input type="month" class="form-control form-control-sm" style="width:auto;" '
            + 'value="' + escapeHtml(stats.month || currentMonth) + '" data-month="' + id + '">'
            + "</div>";

        // 4 thẻ tổng quan như trang thống kê của nhân viên
        html += '<div class="row text-center">'
            + '<div class="col-md-3 col-6 mb-2"><div class="address-card"><h4 class="font-weight-bold mb-0">' + stats.workDays + '</h4><small class="text-muted">Số công đã làm</small></div></div>'
            + '<div class="col-md-3 col-6 mb-2"><div class="address-card"><h4 class="font-weight-bold mb-0">' + stats.ordersSold + '</h4><small class="text-muted">Đơn đã bán được</small></div></div>'
            + '<div class="col-md-3 col-6 mb-2"><div class="address-card"><h4 class="font-weight-bold mb-0">' + formatVnd(stats.totalSales) + '</h4><small class="text-muted">Tổng doanh số</small></div></div>'
            + '<div class="col-md-3 col-6 mb-2"><div class="address-card"><h4 class="font-weight-bold mb-0" style="color:#e53637;">' + formatVnd(stats.commission) + '</h4><small class="text-muted">Hoa hồng (' + rate.toFixed(1) + '%)</small></div></div>'
            + "</div>";

        // Ngày đã chấm công
        var dates = stats.workDates || [];
        html += '<h6 class="font-weight-bold mt-3 mb-2">Ngày đã chấm công</h6>';
        html += dates.length
            ? '<div class="mb-2">' + dates.map(function (d) {
                return '<span class="badge badge-light" style="border:1px solid #ddd;padding:6px 10px;margin:2px;">'
                    + escapeHtml(String(d).slice(0, 10)) + "</span>";
            }).join("") + "</div>"
            : '<p class="text-muted mb-2">Nhân viên chưa chấm công ngày nào trong tháng này.</p>';

        // Sao kê: tất cả đơn nhân viên phụ trách trong tháng
        var handled = stats.handledOrders || [];
        html += '<h6 class="font-weight-bold mt-3 mb-2">Sao kê bán hàng trong tháng ('
            + handled.length + ' đơn phụ trách)</h6>';
        if (!handled.length) {
            html += '<p class="text-muted">Không có đơn hàng nào trong tháng này.</p>';
        } else {
            var statusColors = {
                NEW: "badge-primary", CONFIRMED: "badge-info",
                SHIPPING: "badge-warning", DELIVERED: "badge-success", RECEIVED: "badge-success",
                CANCELLED: "badge-danger", RETURNED: "badge-dark", RETURN_REQUESTED: "badge-warning"
            };
            html += '<div class="table-responsive"><table class="table table-sm" style="font-size:13px;">'
                + "<thead><tr><th>Mã đơn</th><th>Trạng thái</th><th>Tổng tiền</th>"
                + "<th>Ngày tạo</th><th>Hoa hồng</th></tr></thead><tbody>";
            handled.forEach(function (o) {
                var counted = o.status === "DELIVERED" || o.status === "RECEIVED";
                html += "<tr><td>" + escapeHtml(o.orderCode) + "</td>"
                    + '<td><span class="badge ' + (statusColors[o.status] || "badge-light") + '">'
                    + escapeHtml(o.status) + "</span></td>"
                    + "<td>" + formatVnd(o.grandTotal) + "</td>"
                    + "<td>" + (o.createdAt ? new Date(o.createdAt).toLocaleString("vi-VN") : "") + "</td>"
                    + "<td>" + (counted
                        ? '<span style="color:#28a745;font-weight:700;">\u2713 '
                            + formatVnd((Number(o.grandTotal) * Number(stats.commissionRate || 0)).toFixed(0)) + "</span>"
                        : '<span class="text-muted">\u2014</span>') + "</td></tr>";
            });
            html += "</tbody></table></div>"
                + '<small class="text-muted">Chỉ đơn đã giao / khách đã nhận hàng được tính hoa hồng; '
                + "đơn hủy, đang giao, trả hàng không tính.</small>";
        }
        el.innerHTML = html;

        var monthInput = el.querySelector("input[data-month]");
        if (monthInput) {
            monthInput.addEventListener("change", function () {
                loadDetail(id, monthInput.value);
            });
        }
    }

    function fireStaff(id, btn) {
        if (!confirm("Sa thải nhân viên này? Tài khoản sẽ trở lại là khách hàng thông thường (không bị xóa, lịch sử công việc và hoa hồng vẫn tra cứu được).")) return;
        btn.disabled = true;
        staffApi("DELETE", "/" + id).then(function (res) {
            showAlert(res.message, "success");
            loadStaff();
        }).catch(function (err) {
            btn.disabled = false;
            showAlert(err.message, "error");
        });
    }

    function init() {
        var session = getSession();
        if (!hasRole(session, "MANAGER")) return;

        var subBtn = document.querySelector('.shop-subbtn[data-sub="shopSubStaff"]');
        if (!subBtn) return;
        subBtn.addEventListener("click", loadStaff);

        var filterWrap = $("staffFilter");
        if (filterWrap) {
            filterWrap.querySelectorAll("button[data-filter]").forEach(function (b) {
                b.addEventListener("click", function () {
                    staffFilter = b.getAttribute("data-filter");
                    filterWrap.querySelectorAll("button[data-filter]").forEach(function (x) {
                        x.classList.toggle("active", x === b);
                    });
                    renderStaff(lastStaff);
                });
            });
        }

        $("hireForm").addEventListener("submit", function (e) {
            e.preventDefault();
            var body = {
                username: $("hireUsername").value.trim(),
                password: $("hirePassword").value,
                email: $("hireEmail").value.trim(),
                fullName: $("hireFullName").value.trim(),
                phone: $("hirePhone").value.trim()
            };
            if (!body.username || !body.password || !body.email || !body.fullName) {
                showAlert("Vui lòng nhập đủ thông tin bắt buộc (*).", "error");
                return;
            }
            staffApi("POST", "", body).then(function (res) {
                showAlert(res.message + " (" + res.data.fullName + " — @" + res.data.username + ")", "success");
                $("hireForm").reset();
                loadStaff();
            }).catch(function (err) {
                showAlert(err.message, "error");
            });
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();

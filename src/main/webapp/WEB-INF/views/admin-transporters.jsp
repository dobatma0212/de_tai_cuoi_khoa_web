<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>

<!DOCTYPE html>
<html lang="zxx">
<head>
<meta charset="UTF-8">
    <meta name="description" content="Quản lý Đơn vị vận chuyển - Male Fashion Admin">
    <meta name="keywords" content="Male_Fashion, admin, transporter, shipping">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Quản lý Vận chuyển | Male-Fashion Admin</title>

    

    

    

    <style>
        .admin-stat-card {
            background: #ffffff;
            border-radius: 8px;
            padding: 22px 20px;
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
            margin-bottom: 24px;
            transition: all 0.3s ease;
            border-left: 4px solid #111111;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }

        .admin-stat-card:hover {
            transform: translateY(-3px);
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
        }

        .admin-stat-card.stat-primary { border-left-color: #e53637; }
        .admin-stat-card.stat-success { border-left-color: #28a745; }
        .admin-stat-card.stat-warning { border-left-color: #ffc107; }
        .admin-stat-card.stat-info { border-left-color: #17a2b8; }

        .stat-icon {
            width: 50px;
            height: 50px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 22px;
        }

        .stat-primary .stat-icon { background: #ffeaea; color: #e53637; }
        .stat-success .stat-icon { background: #eafaf1; color: #28a745; }
        .stat-warning .stat-icon { background: #fff8e1; color: #ff9800; }
        .stat-info .stat-icon { background: #e8f7fa; color: #17a2b8; }

        .stat-number {
            font-size: 28px;
            font-weight: 800;
            color: #111111;
            line-height: 1.2;
        }

        .stat-label {
            font-size: 13px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            color: #888888;
            font-weight: 600;
        }

        .filter-panel {
            background: #ffffff;
            border-radius: 8px;
            padding: 20px;
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
            margin-bottom: 24px;
        }

        .filter-input-group {
            position: relative;
        }

        .filter-input-group input {
            height: 46px;
            width: 100%;
            border: 1px solid #e1e1e1;
            border-radius: 4px;
            padding-left: 42px;
            padding-right: 36px;
            font-size: 14px;
            transition: all 0.3s;
        }

        .filter-input-group input:focus {
            border-color: #e53637;
            outline: none;
            box-shadow: 0 0 0 2px rgba(229, 54, 55, 0.15);
        }

        .filter-input-group .search-icon {
            position: absolute;
            left: 14px;
            top: 50%;
            transform: translateY(-50%);
            color: #888888;
        }

        .filter-input-group .clear-search {
            position: absolute;
            right: 12px;
            top: 50%;
            transform: translateY(-50%);
            cursor: pointer;
            color: #aaa;
            font-size: 16px;
            display: none;
        }

        .filter-select {
            height: 46px;
            width: 100%;
            border: 1px solid #e1e1e1;
            border-radius: 4px;
            padding: 0 14px;
            font-size: 14px;
            color: #333333;
            background-color: #ffffff;
            outline: none;
        }

        .filter-select:focus {
            border-color: #e53637;
            box-shadow: 0 0 0 2px rgba(229, 54, 55, 0.15);
        }

        .admin-table-container {
            background: #ffffff;
            border-radius: 8px;
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
            overflow: hidden;
            margin-bottom: 30px;
        }

        .table-responsive {
            margin-bottom: 0;
        }

        .admin-table {
            margin-bottom: 0;
        }

        .admin-table thead th {
            background: #111111;
            color: #ffffff;
            font-size: 13px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            padding: 16px 14px;
            border: none;
            white-space: nowrap;
        }

        .admin-table tbody td {
            vertical-align: middle;
            padding: 14px;
            font-size: 14px;
            border-top: 1px solid #f0f0f0;
            color: #333333;
        }

        .admin-table tbody tr:hover {
            background-color: #fcfcfc;
        }

        .transporter-name-link {
            font-weight: 700;
            color: #111111;
            font-size: 15px;
            transition: color 0.2s;
        }

        .transporter-name-link:hover {
            color: #e53637;
            text-decoration: none;
        }

        .fee-badge {
            background: #fff3e0;
            color: #e65100;
            padding: 4px 10px;
            border-radius: 4px;
            font-size: 13px;
            font-weight: 700;
            display: inline-block;
            white-space: nowrap;
        }

        .contact-line {
            font-size: 13px;
            color: #555555;
            margin: 0;
        }

        .contact-line i {
            width: 16px;
            color: #888888;
        }

        .badge-status-active {
            background-color: #e8f5e9;
            color: #2e7d32;
            padding: 5px 10px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 700;
            display: inline-block;
            cursor: pointer;
            transition: all 0.2s;
        }

        .badge-status-active:hover {
            background-color: #c8e6c9;
        }

        .badge-status-inactive {
            background-color: #ffebee;
            color: #c62828;
            padding: 5px 10px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 700;
            display: inline-block;
            cursor: pointer;
            transition: all 0.2s;
        }

        .badge-status-inactive:hover {
            background-color: #ffcdd2;
        }

        .btn-action {
            width: 32px;
            height: 32px;
            border-radius: 4px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-size: 13px;
            border: 1px solid #e1e1e1;
            background: #ffffff;
            color: #555555;
            transition: all 0.2s;
            margin: 0 2px;
            cursor: pointer;
        }

        .btn-action:hover {
            border-color: #111111;
            background: #111111;
            color: #ffffff;
        }

        .btn-action.btn-delete:hover {
            border-color: #e53637;
            background: #e53637;
            color: #ffffff;
        }

        .btn-action.btn-edit:hover {
            border-color: #0288d1;
            background: #0288d1;
            color: #ffffff;
        }

        .btn-male-fashion {
            background: #111111;
            color: #ffffff;
            font-size: 13px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 1px;
            border: none;
            border-radius: 4px;
            padding: 12px 20px;
            transition: all 0.3s;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
        }

        .btn-male-fashion:hover {
            background: #e53637;
            color: #ffffff;
        }

        .pagination-container {
            padding: 16px 20px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            border-top: 1px solid #f0f0f0;
            background: #ffffff;
        }

        .pagination-info {
            font-size: 13px;
            color: #777777;
        }

        .pagination-buttons .btn {
            height: 36px;
            min-width: 36px;
            padding: 0 10px;
            margin: 0 2px;
            border-radius: 4px;
            border: 1px solid #e1e1e1;
            background: #ffffff;
            color: #111111;
            font-weight: 600;
            font-size: 13px;
        }

        .pagination-buttons .btn.active {
            background: #111111;
            border-color: #111111;
            color: #ffffff;
        }

        .pagination-buttons .btn:disabled {
            opacity: 0.4;
            cursor: not-allowed;
        }

        #toast-container {
            position: fixed;
            top: 24px;
            right: 24px;
            z-index: 99999;
            min-width: 320px;
            max-width: 420px;
        }

        .custom-toast {
            background: #ffffff;
            border-radius: 8px;
            padding: 14px 18px;
            margin-bottom: 12px;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
            display: flex;
            align-items: center;
            gap: 12px;
            border-left: 4px solid #111111;
            animation: slideInRight 0.3s ease;
        }

        @keyframes slideInRight {
            from { transform: translateX(100%); opacity: 0; }
            to { transform: translateX(0); opacity: 1; }
        }

        .custom-toast.toast-success { border-left-color: #28a745; }
        .custom-toast.toast-danger { border-left-color: #dc3545; }
        .custom-toast.toast-warning { border-left-color: #ffc107; }
        .custom-toast.toast-info { border-left-color: #17a2b8; }

        .toast-icon { font-size: 20px; }
        .toast-success .toast-icon { color: #28a745; }
        .toast-danger .toast-icon { color: #dc3545; }
        .toast-warning .toast-icon { color: #ffc107; }
        .toast-info .toast-icon { color: #17a2b8; }

        .toast-message { font-size: 14px; font-weight: 600; color: #222222; flex-grow: 1; }
        .toast-close { cursor: pointer; color: #999; font-size: 16px; }

        .modal-header {
            background: #111111;
            color: #ffffff;
            border-top-left-radius: 8px;
            border-top-right-radius: 8px;
            padding: 16px 20px;
        }

        .modal-title {
            font-size: 16px;
            font-weight: 700;
            letter-spacing: 0.5px;
            color: #ffffff;
        }

        .modal-header .close {
            color: #ffffff;
            opacity: 0.8;
            outline: none;
        }

        .modal-content {
            border-radius: 8px;
            border: none;
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
        }

        .modal-body {
            padding: 24px;
        }

        .form-group label {
            font-weight: 700;
            font-size: 13px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            color: #111111;
            margin-bottom: 6px;
        }

        .form-control {
            height: 44px;
            border-radius: 4px;
            border: 1px solid #e1e1e1;
            font-size: 14px;
        }

        .form-control:focus {
            border-color: #e53637;
            box-shadow: 0 0 0 2px rgba(229, 54, 55, 0.15);
        }

        .detail-row {
            display: flex;
            justify-content: space-between;
            padding: 10px 0;
            border-bottom: 1px solid #f0f0f0;
            font-size: 14px;
        }

        .detail-label { font-weight: 600; color: #666666; }
        .detail-value { font-weight: 700; color: #111111; text-align: right; }
    </style>
</head>
<body>

    

    

    <section class="breadcrumb-option">
        <div class="container">
            <div class="row">
                <div class="col-lg-12">
                    <div class="breadcrumb__text">
                        <h4>Quản lý Đơn vị vận chuyển (Transporter)</h4>
                        <div class="breadcrumb__links">
                            <a href="/admin-transporters.html">Hệ thống</a>
                            <span>Danh sách nhà vận chuyển</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <section class="admin-transporter-section spad pt-4">
        <div class="container">

            <div class="row">
                <div class="col-lg-3 col-md-6 col-sm-6">
                    <div class="admin-stat-card stat-primary">
                        <div>
                            <div class="stat-number" id="stat-total">0</div>
                            <div class="stat-label">Tổng đơn vị</div>
                        </div>
                        <div class="stat-icon">
                            <i class="fa fa-truck"></i>
                        </div>
                    </div>
                </div>
                <div class="col-lg-3 col-md-6 col-sm-6">
                    <div class="admin-stat-card stat-success">
                        <div>
                            <div class="stat-number" id="stat-active">0</div>
                            <div class="stat-label">Đang hoạt động</div>
                        </div>
                        <div class="stat-icon">
                            <i class="fa fa-check-circle"></i>
                        </div>
                    </div>
                </div>
                <div class="col-lg-3 col-md-6 col-sm-6">
                    <div class="admin-stat-card stat-warning">
                        <div>
                            <div class="stat-number" id="stat-inactive">0</div>
                            <div class="stat-label">Tạm ngừng</div>
                        </div>
                        <div class="stat-icon">
                            <i class="fa fa-pause-circle"></i>
                        </div>
                    </div>
                </div>
                <div class="col-lg-3 col-md-6 col-sm-6">
                    <div class="admin-stat-card stat-info">
                        <div>
                            <div class="stat-number" id="stat-avg-fee" style="font-size: 22px;">0 ₫</div>
                            <div class="stat-label">Phí cơ bản TB</div>
                        </div>
                        <div class="stat-icon">
                            <i class="fa fa-money"></i>
                        </div>
                    </div>
                </div>
            </div>

            <div class="filter-panel">
                <div class="row align-items-center">
                    <div class="col-lg-5 col-md-6 mb-3 mb-lg-0">
                        <div class="filter-input-group">
                            <i class="fa fa-search search-icon"></i>
                            <input type="text" id="filter-keyword" placeholder="Tìm theo tên, SĐT, email...">
                            <span class="clear-search" id="clear-search-btn" onclick="clearSearch()">&times;</span>
                        </div>
                    </div>

                    <div class="col-lg-2 col-md-3 col-6 mb-3 mb-lg-0">
                        <select class="filter-select" id="filter-status" onchange="onFilterChange()">
                            <option value="">-- Tất cả trạng thái --</option>
                            <option value="active">Đang hoạt động</option>
                            <option value="inactive">Tạm ngừng</option>
                        </select>
                    </div>

                    <div class="col-lg-3 col-md-6 col-6 mb-3 mb-lg-0">
                        <select class="filter-select" id="filter-sort" onchange="onFilterChange()">
                            <option value="id-desc">Mới nhất trước</option>
                            <option value="id-asc">Cũ nhất trước</option>
                            <option value="name-asc">Tên A-Z</option>
                            <option value="name-desc">Tên Z-A</option>
                            <option value="baseFee-asc">Phí thấp → cao</option>
                            <option value="baseFee-desc">Phí cao → thấp</option>
                        </select>
                    </div>

                    <div class="col-lg-2 col-md-6 col-12 text-right">
                        <button class="btn-male-fashion w-100" onclick="openCreateModal()">
                            <i class="fa fa-plus-circle mr-1"></i> Thêm mới
                        </button>
                    </div>
                </div>
            </div>

            <div class="admin-table-container">
                <div class="table-responsive">
                    <table class="table admin-table text-nowrap">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Tên đơn vị</th>
                                <th>Liên hệ</th>
                                <th>Phí cơ bản</th>
                                <th>Trạng thái</th>
                                <th>Ngày tạo</th>
                                <th class="text-right">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody id="transporter-table-body">
                            <tr>
                                <td colspan="7" class="text-center py-5 text-muted">
                                    <i class="fa fa-spinner fa-spin fa-2x mb-3"></i>
                                    <div>Đang tải danh sách đơn vị vận chuyển...</div>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <div class="pagination-container" id="pagination-container" style="display: none;">
                    <div class="pagination-info" id="pagination-info">
                        Hiển thị 0 - 0 trong tổng số 0 đơn vị
                    </div>
                    <div class="pagination-buttons" id="pagination-buttons"></div>
                </div>
            </div>

        </div>
    </section>

    <div class="modal fade" id="transporterModal" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="transporterModalTitle">Thêm Đơn Vị Vận Chuyển</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <form id="transporterForm" onsubmit="saveTransporter(event)">
                    <div class="modal-body">
                        <input type="hidden" id="tr-id">

                        <div class="form-group">
                            <label for="tr-name">Tên đơn vị vận chuyển <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="tr-name" placeholder="VD: Giao Hàng Nhanh, Viettel Post..." maxlength="100" required>
                        </div>

                        <div class="form-group">
                            <label for="tr-phone">Số điện thoại <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="tr-phone" placeholder="VD: 1900 636677" maxlength="15" required>
                        </div>

                        <div class="form-group">
                            <label for="tr-email">Email</label>
                            <input type="email" class="form-control" id="tr-email" placeholder="VD: support@ghn.vn (không bắt buộc)" maxlength="100">
                            <small class="form-text text-muted">Để trống nếu không có. Email phải là duy nhất nếu có.</small>
                        </div>

                        <div class="row">
                            <div class="col-6">
                                <div class="form-group">
                                    <label for="tr-base-fee">Phí cơ bản (VNĐ)</label>
                                    <input type="number" class="form-control" id="tr-base-fee" value="0" min="0" step="0.01">
                                </div>
                            </div>
                            <div class="col-6">
                                <div class="form-group">
                                    <label for="tr-status">Trạng thái</label>
                                    <select class="form-control" id="tr-status">
                                        <option value="active">Đang hoạt động (active)</option>
                                        <option value="inactive">Tạm ngừng (inactive)</option>
                                    </select>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn-male-fashion" id="btn-save-transporter">
                            <i class="fa fa-check mr-1"></i> Lưu thông tin
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <div class="modal fade" id="transporterDetailModal" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title"><i class="fa fa-info-circle mr-1"></i> Chi Tiết Đơn Vị Vận Chuyển</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body" id="transporter-detail-content"></div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-dismiss="modal">Đóng</button>
                    <button type="button" class="btn btn-dark" id="btn-edit-from-detail">
                        <i class="fa fa-pencil mr-1"></i> Chỉnh sửa
                    </button>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="deleteTransporterModal" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content">
                <div class="modal-header bg-danger text-white">
                    <h5 class="modal-title text-white"><i class="fa fa-exclamation-triangle mr-1"></i> Xác Nhận Xóa</h5>
                    <button type="button" class="close text-white" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body text-center py-4">
                    <i class="fa fa-trash text-danger fa-4x mb-3"></i>
                    <h5>Bạn có chắc chắn muốn xóa đơn vị vận chuyển này?</h5>
                    <p class="text-muted mt-2 mb-0">Đơn vị: <strong id="delete-tr-name" class="text-dark">...</strong></p>
                    <small class="text-danger d-block mt-2">Lưu ý: Hành động này không thể hoàn tác!</small>
                </div>
                <div class="modal-footer justify-content-center">
                    <button type="button" class="btn btn-secondary px-4" data-dismiss="modal">Hủy bỏ</button>
                    <button type="button" class="btn btn-danger px-4" id="btn-confirm-delete" onclick="executeDelete()">
                        <i class="fa fa-trash mr-1"></i> Xác nhận Xóa
                    </button>
                </div>
            </div>
        </div>
    </div>

    <div class="modal fade" id="loginModal" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title"><i class="fa fa-lock mr-1"></i> Đăng nhập Quản trị</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <form id="adminLoginForm" onsubmit="handleLogin(event)">
                    <div class="modal-body">
                        <div class="form-group">
                            <label>Tên đăng nhập / Email</label>
                            <input type="text" class="form-control" id="login-username" required placeholder="admin hoặc manager">
                        </div>
                        <div class="form-group">
                            <label>Mật khẩu</label>
                            <input type="password" class="form-control" id="login-password" required placeholder="Mật khẩu">
                        </div>
                        <div class="mt-3">
                            <p class="mb-1 text-muted" style="font-size: 13px;">Tài khoản thử nghiệm có sẵn:</p>
                            <button type="button" class="btn btn-sm btn-outline-danger mb-1 mr-1" onclick="quickFillLogin('admin', 'admin123')">
                                <i class="fa fa-user"></i> Admin (admin123)
                            </button>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-dismiss="modal">Đóng</button>
                        <button type="submit" class="btn-male-fashion" id="btn-submit-login">
                            <i class="fa fa-sign-in mr-1"></i> Đăng nhập ngay
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script src="/js/jquery-3.3.1.min.js"></script>
    <script src="/js/bootstrap.min.js"></script>
    <script src="/js/jquery.nice-select.min.js"></script>
    <script src="/js/jquery.nicescroll.min.js"></script>
    <script src="/js/jquery.magnific-popup.min.js"></script>
    <script src="/js/jquery.slicknav.js"></script>
    <script src="/js/main.js"></script>

    <script>
        let currentUser = null;
        let currentPage = 0;
        let pageSize = 10;
        let totalPages = 0;
        let totalElements = 0;
        let searchTimeout = null;
        let currentlyEditingId = null;
        let deletingId = null;

        const API_BASE = '/api/admin/transporters';
        const AUTH_API = '/api/auth/login';

        $(document).ready(function () {
            checkAuth();
            setupSearchDebounce();
            loadStats();
            loadTransporters();
        });

        function getToken() {
            const scRaw = localStorage.getItem('sc_auth');
            if (scRaw) {
                try {
                    const parsed = JSON.parse(scRaw);
                    if (parsed && parsed.token) return parsed.token;
                } catch (e) {}
            }
            return localStorage.getItem('jwt_token') || sessionStorage.getItem('jwt_token');
        }

        function getAuthHeaders() {
            const token = getToken();
            return token
                ? { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' }
                : { 'Content-Type': 'application/json' };
        }

        function checkAuth() {
            let storedUser = localStorage.getItem('user_info') || sessionStorage.getItem('user_info');
            if (!storedUser) {
                const scRaw = localStorage.getItem('sc_auth');
                if (scRaw) storedUser = scRaw;
            }
            if (storedUser) {
                try {
                    currentUser = JSON.parse(storedUser);
                } catch (e) {
                    currentUser = null;
                }
            }
            updateAuthUI();
        }

        function updateAuthUI() {
            const authContainer = $('#auth-user-info');
            const roleIndicator = $('#role-indicator');

            if (currentUser && getToken()) {
                const roleName = currentUser.role || (currentUser.roles && currentUser.roles[0]) || 'USER';
                const isAdmin = roleName.includes('ADMIN');
                const isManager = roleName.includes('MANAGER');

                if (roleIndicator.length) {
                    roleIndicator.removeClass('badge-danger badge-primary badge-dark')
                        .addClass(isAdmin ? 'badge-danger' : 'badge-primary')
                        .html(`<i class="fa fa-shield"></i> ${isAdmin ? 'ADMIN' : (isManager ? 'MANAGER' : roleName)}`);
                }

                authContainer.html(`
                    <span class="text-white mr-3">
                        <i class="fa fa-user-circle text-danger mr-1"></i>
                        <strong>${currentUser.fullName || currentUser.username}</strong>
                    </span>
                `);
            } else {
                if (roleIndicator.length) {
                    roleIndicator.removeClass('badge-danger badge-primary').addClass('badge-dark')
                        .html(`<i class="fa fa-lock"></i> CHƯA ĐĂNG NHẬP`);
                }
                authContainer.html(`
                    <span class="text-white mr-3"><i class="fa fa-user-circle"></i> Chưa đăng nhập</span>
                    <button class="btn btn-sm btn-outline-light" onclick="openLoginModal()">Đăng nhập</button>
                `);
            }
        }

        function openLoginModal() {
            $('#loginModal').modal('show');
        }

        function quickFillLogin(user, pass) {
            $('#login-username').val(user);
            $('#login-password').val(pass);
        }

        function handleLogin(e) {
            e.preventDefault();
            const username = $('#login-username').val().trim();
            const password = $('#login-password').val().trim();
            const submitBtn = $('#btn-submit-login');

            submitBtn.prop('disabled', true).html('<i class="fa fa-spinner fa-spin"></i> Đang đăng nhập...');

            $.ajax({
                url: AUTH_API,
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({ username: username, password: password }),
                success: function (res) {
                    submitBtn.prop('disabled', false).html('<i class="fa fa-sign-in mr-1"></i> Đăng nhập ngay');
                    const data = res.data || res;
                    const token = data.token || data.accessToken;
                    const userObj = {
                        userId: data.id || data.userId,
                        username: data.username,
                        email: data.email,
                        fullName: data.fullName || data.username,
                        role: data.role || (data.roles ? data.roles[0] : 'ROLE_ADMIN')
                    };

                    if (!userObj.role.includes('ADMIN')) {
                        showToast('Tài khoản của bạn không có quyền truy cập trang Quản lý Vận chuyển (yêu cầu ADMIN)!', 'danger');
                        return;
                    }

                    localStorage.setItem('jwt_token', token);
                    localStorage.setItem('user_info', JSON.stringify(userObj));
                    localStorage.setItem('sc_auth', JSON.stringify({ token: token, user: userObj }));
                    currentUser = userObj;

                    $('#loginModal').modal('hide');
                    updateAuthUI();
                    showToast('Đăng nhập thành công với vai trò ' + userObj.role, 'success');
                    loadStats();
                    loadTransporters();
                },
                error: function (xhr) {
                    submitBtn.prop('disabled', false).html('<i class="fa fa-sign-in mr-1"></i> Đăng nhập ngay');
                    const msg = xhr.responseJSON?.message || 'Đăng nhập thất bại. Kiểm tra lại thông tin!';
                    showToast(msg, 'danger');
                }
            });
        }

        function handleLogout() {
            localStorage.removeItem('jwt_token');
            localStorage.removeItem('user_info');
            localStorage.removeItem('sc_auth');
            sessionStorage.clear();
            currentUser = null;
            window.location.href = '/index.html';
        }

        function formatCurrency(value) {
            const num = Number(value || 0);
            return new Intl.NumberFormat('vi-VN').format(num) + ' ₫';
        }

        function escapeHtml(text) {
            if (text == null) return '';
            return String(text)
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        }

        function loadStats() {
            $.ajax({
                url: `${API_BASE}/stats`,
                type: 'GET',
                headers: getAuthHeaders(),
                success: function (res) {
                    if (res.data) {
                        $('#stat-total').text(res.data.totalTransporters || 0);
                        $('#stat-active').text(res.data.activeTransporters || 0);
                        $('#stat-inactive').text(res.data.inactiveTransporters || 0);
                        $('#stat-avg-fee').text(formatCurrency(res.data.avgBaseFee));
                    }
                }
            });
        }

        function setupSearchDebounce() {
            $('#filter-keyword').on('input', function () {
                const val = $(this).val();
                if (val.length > 0) {
                    $('#clear-search-btn').show();
                } else {
                    $('#clear-search-btn').hide();
                }
                clearTimeout(searchTimeout);
                searchTimeout = setTimeout(function () {
                    currentPage = 0;
                    loadTransporters();
                }, 400);
            });
        }

        function clearSearch() {
            $('#filter-keyword').val('');
            $('#clear-search-btn').hide();
            currentPage = 0;
            loadTransporters();
        }

        function onFilterChange() {
            currentPage = 0;
            loadTransporters();
        }

        function loadTransporters() {
            const tbody = $('#transporter-table-body');
            tbody.html(`
                <tr>
                    <td colspan="7" class="text-center py-5 text-muted">
                        <i class="fa fa-spinner fa-spin fa-2x mb-3 text-danger"></i>
                        <div>Đang tải dữ liệu...</div>
                    </td>
                </tr>
            `);

            const keyword = $('#filter-keyword').val().trim();
            const status = $('#filter-status').val();
            const sortVal = $('#filter-sort').val().split('-');
            const sortBy = sortVal[0] || 'id';
            const sortDir = sortVal[1] || 'desc';

            const params = {
                page: currentPage,
                size: pageSize,
                sortBy: sortBy,
                sortDir: sortDir
            };
            if (keyword) params.keyword = keyword;
            if (status) params.status = status;

            $.ajax({
                url: API_BASE,
                type: 'GET',
                headers: getAuthHeaders(),
                data: params,
                success: function (res) {
                    if (res.data && res.data.content) {
                        renderTable(res.data.content);
                        totalElements = res.data.totalElements;
                        totalPages = res.data.totalPages;
                        renderPagination();
                    } else {
                        renderEmptyTable('Không có dữ liệu đơn vị vận chuyển');
                    }
                },
                error: function (xhr) {
                    if (xhr.status === 401 || xhr.status === 403) {
                        tbody.html(`
                            <tr>
                                <td colspan="7" class="text-center py-5">
                                    <i class="fa fa-lock fa-3x text-danger mb-3"></i>
                                    <h5>Yêu cầu quyền Quản trị viên (ADMIN)</h5>
                                    <p class="text-muted">Vui lòng đăng nhập với tài khoản Quản trị viên để quản lý đơn vị vận chuyển.</p>
                                    <button class="btn btn-male-fashion" onclick="openLoginModal()">
                                        <i class="fa fa-sign-in mr-1"></i> Đăng nhập ngay
                                    </button>
                                </td>
                            </tr>
                        `);
                        $('#pagination-container').hide();
                    } else {
                        renderEmptyTable('Đã xảy ra lỗi khi tải dữ liệu: ' + (xhr.responseJSON?.message || 'Lỗi server'));
                    }
                }
            });
        }

        function renderTable(transporters) {
            const tbody = $('#transporter-table-body');
            tbody.empty();

            if (!transporters || transporters.length === 0) {
                renderEmptyTable('Không tìm thấy đơn vị vận chuyển nào phù hợp với bộ lọc');
                return;
            }

            transporters.forEach(tr => {
                const statusBadge = tr.status === 'active'
                    ? `<span class="badge-status-active" onclick="toggleStatus(${tr.id}, 'inactive')" title="Bấm để chuyển sang tạm ngừng"><i class="fa fa-check-circle mr-1"></i> Hoạt động</span>`
                    : `<span class="badge-status-inactive" onclick="toggleStatus(${tr.id}, 'active')" title="Bấm để kích hoạt lại"><i class="fa fa-ban mr-1"></i> Tạm ngừng</span>`;

                const emailHtml = tr.email
                    ? `<p class="contact-line"><i class="fa fa-envelope"></i> ${escapeHtml(tr.email)}</p>`
                    : `<p class="contact-line text-muted"><i class="fa fa-envelope"></i> Chưa có email</p>`;

                const safeName = escapeHtml(tr.name);
                const row = `
                    <tr>
                        <td><strong>#${tr.id}</strong></td>
                        <td>
                            <a href="javascript:void(0)" onclick="viewDetail(${tr.id})" class="transporter-name-link">
                                ${safeName}
                            </a>
                        </td>
                        <td>
                            <p class="contact-line mb-1"><i class="fa fa-phone"></i> ${escapeHtml(tr.phone)}</p>
                            ${emailHtml}
                        </td>
                        <td><span class="fee-badge">${formatCurrency(tr.baseFee)}</span></td>
                        <td>${statusBadge}</td>
                        <td><small class="text-muted">${tr.createdAt ? tr.createdAt.substring(0, 16) : 'N/A'}</small></td>
                        <td class="text-right">
                            <button class="btn-action" onclick="viewDetail(${tr.id})" title="Xem chi tiết">
                                <i class="fa fa-eye"></i>
                            </button>
                            <button class="btn-action btn-edit" onclick="openEditModal(${tr.id})" title="Chỉnh sửa">
                                <i class="fa fa-pencil"></i>
                            </button>
                            <button class="btn-action btn-delete" data-id="${tr.id}" data-name="${safeName}" onclick="confirmDelete(this)" title="Xóa">
                                <i class="fa fa-trash"></i>
                            </button>
                        </td>
                    </tr>
                `;
                tbody.append(row);
            });

            $('#pagination-container').show();
        }

        function renderEmptyTable(msg) {
            $('#transporter-table-body').html(`
                <tr>
                    <td colspan="7" class="text-center py-5 text-muted">
                        <i class="fa fa-truck fa-3x mb-3 text-muted"></i>
                        <div>${msg}</div>
                    </td>
                </tr>
            `);
            $('#pagination-container').hide();
        }

        function renderPagination() {
            if (totalPages <= 1) {
                $('#pagination-container').hide();
                return;
            }

            const from = currentPage * pageSize + 1;
            const to = Math.min((currentPage + 1) * pageSize, totalElements);
            $('#pagination-info').text(`Hiển thị ${from} - ${to} trong tổng số ${totalElements} đơn vị`);

            const buttonsContainer = $('#pagination-buttons');
            buttonsContainer.empty();

            buttonsContainer.append(`
                <button class="btn" onclick="goToPage(${currentPage - 1})" ${currentPage === 0 ? 'disabled' : ''}>
                    <i class="fa fa-angle-left"></i>
                </button>
            `);

            for (let i = 0; i < totalPages; i++) {
                if (i === 0 || i === totalPages - 1 || (i >= currentPage - 2 && i <= currentPage + 2)) {
                    buttonsContainer.append(`
                        <button class="btn ${i === currentPage ? 'active' : ''}" onclick="goToPage(${i})">${i + 1}</button>
                    `);
                } else if (i === currentPage - 3 || i === currentPage + 3) {
                    buttonsContainer.append(`<span class="px-1 text-muted">...</span>`);
                }
            }

            buttonsContainer.append(`
                <button class="btn" onclick="goToPage(${currentPage + 1})" ${currentPage === totalPages - 1 ? 'disabled' : ''}>
                    <i class="fa fa-angle-right"></i>
                </button>
            `);

            $('#pagination-container').show();
        }

        function goToPage(page) {
            if (page >= 0 && page < totalPages) {
                currentPage = page;
                loadTransporters();
            }
        }

        function openCreateModal() {
            currentlyEditingId = null;
            $('#transporterModalTitle').text('Thêm Đơn Vị Vận Chuyển');
            $('#transporterForm')[0].reset();
            $('#tr-id').val('');
            $('#tr-base-fee').val('0');
            $('#tr-status').val('active');
            $('#transporterModal').modal('show');
        }

        function openEditModal(id) {
            currentlyEditingId = id;
            $('#transporterModalTitle').text('Chỉnh Sửa Đơn Vị Vận Chuyển #' + id);

            $.ajax({
                url: `${API_BASE}/${id}`,
                type: 'GET',
                headers: getAuthHeaders(),
                success: function (res) {
                    const tr = res.data;
                    $('#tr-id').val(tr.id);
                    $('#tr-name').val(tr.name);
                    $('#tr-phone').val(tr.phone);
                    $('#tr-email').val(tr.email || '');
                    $('#tr-base-fee').val(tr.baseFee != null ? tr.baseFee : 0);
                    $('#tr-status').val(tr.status || 'active');

                    $('#transporterDetailModal').modal('hide');
                    $('#transporterModal').modal('show');
                },
                error: function (xhr) {
                    showToast(xhr.responseJSON?.message || 'Không thể tải chi tiết đơn vị vận chuyển', 'danger');
                }
            });
        }

        function saveTransporter(e) {
            e.preventDefault();

            const name = $('#tr-name').val().trim();
            const phone = $('#tr-phone').val().trim();
            const email = $('#tr-email').val().trim();
            const baseFee = parseFloat($('#tr-base-fee').val());
            const status = $('#tr-status').val();

            if (!name) {
                showToast('Vui lòng nhập tên đơn vị vận chuyển!', 'warning');
                return;
            }
            if (!phone) {
                showToast('Vui lòng nhập số điện thoại!', 'warning');
                return;
            }
            if (isNaN(baseFee) || baseFee < 0) {
                showToast('Phí cơ bản không hợp lệ!', 'warning');
                return;
            }

            const payload = {
                name: name,
                phone: phone,
                email: email || null,
                baseFee: baseFee,
                status: status
            };

            const submitBtn = $('#btn-save-transporter');
            submitBtn.prop('disabled', true).html('<i class="fa fa-spinner fa-spin"></i> Đang lưu...');

            const isEdit = !!currentlyEditingId;
            const url = isEdit ? `${API_BASE}/${currentlyEditingId}` : API_BASE;
            const method = isEdit ? 'PUT' : 'POST';

            $.ajax({
                url: url,
                type: method,
                headers: getAuthHeaders(),
                contentType: 'application/json',
                data: JSON.stringify(payload),
                success: function (res) {
                    submitBtn.prop('disabled', false).html('<i class="fa fa-check mr-1"></i> Lưu thông tin');
                    $('#transporterModal').modal('hide');
                    showToast(res.message || (isEdit ? 'Cập nhật thành công!' : 'Thêm mới thành công!'), 'success');
                    loadStats();
                    loadTransporters();
                },
                error: function (xhr) {
                    submitBtn.prop('disabled', false).html('<i class="fa fa-check mr-1"></i> Lưu thông tin');
                    showToast(xhr.responseJSON?.message || 'Thao tác thất bại!', 'danger');
                }
            });
        }

        function toggleStatus(id, newStatus) {
            $.ajax({
                url: `${API_BASE}/${id}/status?status=${newStatus}`,
                type: 'PATCH',
                headers: getAuthHeaders(),
                success: function () {
                    showToast(`Đã chuyển trạng thái sang ${newStatus === 'active' ? 'Hoạt động' : 'Tạm ngừng'}!`, 'success');
                    loadStats();
                    loadTransporters();
                },
                error: function (xhr) {
                    showToast(xhr.responseJSON?.message || 'Không thể đổi trạng thái', 'danger');
                }
            });
        }

        function viewDetail(id) {
            $.ajax({
                url: `${API_BASE}/${id}`,
                type: 'GET',
                headers: getAuthHeaders(),
                success: function (res) {
                    const tr = res.data;
                    $('#transporter-detail-content').html(`
                        <div class="text-center mb-4">
                            <div style="width: 90px; height: 90px; border-radius: 8px; background: #ffeaea; display: inline-flex; align-items: center; justify-content: center; font-size: 36px; color: #e53637;">
                                <i class="fa fa-truck"></i>
                            </div>
                            <h4 class="mt-2 font-weight-bold">${escapeHtml(tr.name)}</h4>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">Mã đơn vị (ID):</span>
                            <span class="detail-value">#${tr.id}</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">Số điện thoại:</span>
                            <span class="detail-value">${escapeHtml(tr.phone)}</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">Email:</span>
                            <span class="detail-value">${tr.email ? escapeHtml(tr.email) : '<span class="text-muted">Chưa có</span>'}</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">Phí cơ bản:</span>
                            <span class="detail-value">${formatCurrency(tr.baseFee)}</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">Trạng thái:</span>
                            <span class="detail-value">${tr.status === 'active' ? '<span class="badge badge-success">Đang hoạt động</span>' : '<span class="badge badge-secondary">Tạm ngừng</span>'}</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">Ngày khởi tạo:</span>
                            <span class="detail-value">${tr.createdAt || 'N/A'}</span>
                        </div>
                    `);

                    $('#btn-edit-from-detail').off('click').on('click', function () {
                        openEditModal(tr.id);
                    });

                    $('#transporterDetailModal').modal('show');
                },
                error: function (xhr) {
                    showToast(xhr.responseJSON?.message || 'Không thể tải chi tiết', 'danger');
                }
            });
        }

        function confirmDelete(btn) {
            deletingId = parseInt($(btn).data('id'));
            const name = $(btn).data('name');
            $('#delete-tr-name').text(name + ' (#' + deletingId + ')');
            $('#deleteTransporterModal').modal('show');
        }

        function executeDelete() {
            if (!deletingId) return;

            const btn = $('#btn-confirm-delete');
            btn.prop('disabled', true).html('<i class="fa fa-spinner fa-spin"></i> Đang xóa...');

            $.ajax({
                url: `${API_BASE}/${deletingId}`,
                type: 'DELETE',
                headers: getAuthHeaders(),
                success: function (res) {
                    btn.prop('disabled', false).html('<i class="fa fa-trash mr-1"></i> Xác nhận Xóa');
                    $('#deleteTransporterModal').modal('hide');
                    showToast(res.message || 'Xóa đơn vị vận chuyển thành công!', 'success');
                    loadStats();
                    loadTransporters();
                },
                error: function (xhr) {
                    btn.prop('disabled', false).html('<i class="fa fa-trash mr-1"></i> Xác nhận Xóa');
                    showToast(xhr.responseJSON?.message || 'Không thể xóa đơn vị vận chuyển!', 'danger');
                }
            });
        }

        function showToast(message, type = 'info') {
            const container = $('#toast-container');
            const icon = type === 'success' ? 'fa-check-circle' :
                         type === 'danger' ? 'fa-exclamation-circle' :
                         type === 'warning' ? 'fa-warning' : 'fa-info-circle';

            const toast = $(`
                <div class="custom-toast toast-${type}">
                    <i class="fa ${icon} toast-icon"></i>
                    <div class="toast-message">${message}</div>
                    <span class="toast-close" onclick="$(this).parent().fadeOut(300, function(){ $(this).remove(); });">&times;</span>
                </div>
            `);

            container.append(toast);

            setTimeout(() => {
                toast.fadeOut(400, function () {
                    $(this).remove();
                });
            }, 4000);
        }
    </script>
</body>
</html>

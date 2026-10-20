<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>

<!DOCTYPE html>
<html lang="zxx">
<head>
<meta charset="UTF-8">
    <meta name="description" content="Hệ thống Quản lý Người dùng - StoreChain Male Fashion">
    <meta name="keywords" content="Male_Fashion, admin, users, management">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Quản lý Người dùng | Male-Fashion Admin</title>

    <!-- Google Font -->

    <!-- Css Styles -->

    

    

    

    <style>
        /* Custom styles integrated seamlessly with Male-Fashion */
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
        .admin-stat-card.stat-danger { border-left-color: #dc3545; }
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
        .stat-danger .stat-icon { background: #fdf0f0; color: #dc3545; }
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

        /* Filter bar */
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
            color: #111111;
            transition: all 0.3s;
        }

        .filter-input-group input:focus {
            border-color: #e53637;
            outline: none;
            box-shadow: 0 0 0 2px rgba(229, 54, 55, 0.15);
        }

        .filter-input-group .search-icon {
            position: absolute;
            left: 15px;
            top: 14px;
            color: #888888;
        }

        .filter-input-group .clear-search {
            position: absolute;
            right: 12px;
            top: 14px;
            color: #aaa;
            cursor: pointer;
            display: none;
        }

        .filter-select {
            height: 46px;
            width: 100%;
            border: 1px solid #e1e1e1;
            border-radius: 4px;
            padding: 0 15px;
            font-size: 14px;
            color: #111111;
            background-color: #fff;
            cursor: pointer;
        }

        .filter-select:focus {
            border-color: #e53637;
            outline: none;
        }

        /* Table Styling */
        .admin-table-container {
            background: #ffffff;
            border-radius: 8px;
            padding: 24px;
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
            margin-bottom: 30px;
        }

        .admin-table {
            width: 100%;
            margin-bottom: 0;
            border-collapse: separate;
            border-spacing: 0;
        }

        .admin-table thead th {
            background: #f8f9fa;
            border-top: none;
            border-bottom: 2px solid #e9ecef;
            font-weight: 700;
            font-size: 13px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            color: #111111;
            padding: 14px 16px;
            white-space: nowrap;
        }

        .admin-table tbody td {
            padding: 14px 16px;
            vertical-align: middle;
            border-top: 1px solid #f0f0f0;
            font-size: 14px;
            color: #333333;
        }

        .admin-table tbody tr:hover {
            background-color: #fafbfc;
        }

        .user-avatar-wrap {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            overflow: hidden;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            background: #111111;
            color: #ffffff;
            font-weight: 700;
            font-size: 14px;
            text-transform: uppercase;
            flex-shrink: 0;
            margin-right: 12px;
        }

        .user-avatar-wrap img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }

        /* Badges */
        .badge-role {
            font-size: 11px;
            font-weight: 700;
            letter-spacing: 0.5px;
            padding: 5px 10px;
            border-radius: 20px;
            text-transform: uppercase;
            display: inline-block;
        }

        .role-admin { background: #ffeaea; color: #e53637; border: 1px solid #fbc4c4; }
        .role-manager { background: #f3e8ff; color: #7e22ce; border: 1px solid #e9d5ff; }
        .role-employee { background: #e0f2fe; color: #0284c7; border: 1px solid #bae6fd; }
        .role-user { background: #ecfdf5; color: #047857; border: 1px solid #a7f3d0; }
        .role-guest { background: #f3f4f6; color: #4b5563; border: 1px solid #e5e7eb; }

        .badge-status {
            font-size: 12px;
            font-weight: 600;
            padding: 4px 10px;
            border-radius: 20px;
            display: inline-flex;
            align-items: center;
            gap: 5px;
        }

        .status-dot {
            width: 7px;
            height: 7px;
            border-radius: 50%;
            display: inline-block;
        }

        .status-active { background: #ecfdf5; color: #059669; }
        .status-active .status-dot { background: #059669; }

        .status-locked { background: #fef2f2; color: #dc2626; }
        .status-locked .status-dot { background: #dc2626; }

        .status-inactive { background: #f3f4f6; color: #6b7280; }
        .status-inactive .status-dot { background: #6b7280; }

        .status-banned { background: #111111; color: #ffffff; }
        .status-banned .status-dot { background: #e53637; }

        /* Action Buttons */
        .btn-action-group {
            display: flex;
            gap: 6px;
            align-items: center;
        }

        .btn-action {
            width: 32px;
            height: 32px;
            border-radius: 4px;
            border: 1px solid #e1e1e1;
            background: #ffffff;
            color: #555555;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-size: 13px;
            transition: all 0.2s;
            cursor: pointer;
        }

        .btn-action:hover:not(:disabled) {
            background: #111111;
            color: #ffffff;
            border-color: #111111;
        }

        .btn-action.btn-view:hover:not(:disabled) { background: #17a2b8; border-color: #17a2b8; color: #fff; }
        .btn-action.btn-edit:hover:not(:disabled) { background: #007bff; border-color: #007bff; color: #fff; }
        .btn-action.btn-toggle:hover:not(:disabled) { background: #f59e0b; border-color: #f59e0b; color: #fff; }
        .btn-action.btn-delete:hover:not(:disabled) { background: #e53637; border-color: #e53637; color: #fff; }

        .btn-action:disabled {
            opacity: 0.4;
            cursor: not-allowed;
        }

        /* Male Fashion Button Overrides */
        .btn-male-fashion {
            display: inline-block;
            font-size: 13px;
            font-weight: 700;
            text-transform: uppercase;
            padding: 12px 24px;
            color: #ffffff;
            background: #111111;
            letter-spacing: 1px;
            border: none;
            border-radius: 4px;
            cursor: pointer;
            transition: all 0.3s;
        }

        .btn-male-fashion:hover {
            background: #e53637;
            color: #ffffff;
        }

        .btn-male-fashion-dark {
            background: #111111;
        }

        .btn-male-fashion-dark:hover {
            background: #e53637;
        }

        /* Toast container */
        #toast-container {
            position: fixed;
            top: 24px;
            right: 24px;
            z-index: 99999;
            display: flex;
            flex-direction: column;
            gap: 10px;
        }

        .mf-toast {
            min-width: 320px;
            max-width: 450px;
            background: #ffffff;
            border-radius: 6px;
            padding: 14px 18px;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
            display: flex;
            align-items: center;
            gap: 12px;
            border-left: 4px solid #111;
            animation: slideInRight 0.3s ease;
        }

        .mf-toast.toast-success { border-left-color: #28a745; }
        .mf-toast.toast-error { border-left-color: #dc3545; }
        .mf-toast.toast-info { border-left-color: #17a2b8; }

        @keyframes slideInRight {
            from { transform: translateX(100%); opacity: 0; }
            to { transform: translateX(0); opacity: 1; }
        }

        /* Modal custom design */
        .modal-header {
            background: #111111;
            color: #ffffff;
            border-radius: 6px 6px 0 0;
            padding: 16px 24px;
        }

        .modal-header .modal-title {
            font-weight: 700;
            font-size: 18px;
            color: #ffffff;
        }

        .modal-header .close {
            color: #ffffff;
            opacity: 0.8;
            text-shadow: none;
            outline: none;
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

        /* Detail Modal item */
        .detail-row {
            display: flex;
            justify-content: space-between;
            padding: 10px 0;
            border-bottom: 1px solid #f0f0f0;
            font-size: 14px;
        }

        .detail-label {
            font-weight: 600;
            color: #666666;
        }

        .detail-value {
            font-weight: 700;
            color: #111111;
            text-align: right;
        }

        /* Current user pill */
        .current-user-badge {
            background: #e53637;
            color: #ffffff;
            padding: 3px 8px;
            border-radius: 4px;
            font-size: 11px;
            font-weight: 700;
            text-transform: uppercase;
            margin-left: 8px;
        }
    </style>
</head>
<body>

    

    <!-- Breadcrumb Section Begin -->
    <section class="breadcrumb-option">
        <div class="container">
            <div class="row">
                <div class="col-lg-12">
                    <div class="breadcrumb__text">
                        <h4>Quản lý Người dùng</h4>
                        <div class="breadcrumb__links">
                            <a href="/admin-users.html">Hệ thống</a>
                            <span>Danh sách tài khoản</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
    <!-- Breadcrumb Section End -->

    <!-- Main Content Section Begin -->
    <section class="admin-user-section spad pt-4">
        <div class="container">

            <!-- Thống kê tổng quan (Statistics Cards) -->
            <div class="row">
                <div class="col-lg-3 col-md-6 col-sm-6">
                    <div class="admin-stat-card stat-primary">
                        <div>
                            <div class="stat-number" id="stat-total">0</div>
                            <div class="stat-label">Tổng người dùng</div>
                        </div>
                        <div class="stat-icon">
                            <i class="fa fa-users"></i>
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
                    <div class="admin-stat-card stat-danger">
                        <div>
                            <div class="stat-number" id="stat-locked">0</div>
                            <div class="stat-label">Tạm khóa / Bị cấm</div>
                        </div>
                        <div class="stat-icon">
                            <i class="fa fa-ban"></i>
                        </div>
                    </div>
                </div>
                <div class="col-lg-3 col-md-6 col-sm-6">
                    <div class="admin-stat-card stat-info">
                        <div>
                            <div class="stat-number" id="stat-admins">0</div>
                            <div class="stat-label">Nhân sự Quản lý</div>
                        </div>
                        <div class="stat-icon">
                            <i class="fa fa-shield"></i>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Thanh tìm kiếm và Bộ lọc (Filter Bar) -->
            <div class="filter-panel">
                <div class="row align-items-center">
                    <!-- Ô tìm kiếm từ khóa -->
                    <div class="col-lg-4 col-md-6 mb-3 mb-lg-0">
                        <div class="filter-input-group">
                            <i class="fa fa-search search-icon"></i>
                            <input type="text" id="filter-keyword" placeholder="Tìm theo Username, Email, Họ tên, SĐT...">
                            <span class="clear-search" id="clear-search-btn" onclick="clearSearch()">&times;</span>
                        </div>
                    </div>

                    <!-- Lọc theo Vai trò -->
                    <div class="col-lg-2 col-md-3 col-6 mb-3 mb-lg-0">
                        <select class="filter-select" id="filter-role" onchange="onFilterChange()">
                            <option value="">-- Tất cả vai trò --</option>
                            <option value="ROLE_ADMIN">Quản trị viên (ADMIN)</option>
                            <option value="ROLE_MANAGER">Quản lý (MANAGER)</option>
                            <option value="ROLE_EMPLOYEE">Nhân viên (EMPLOYEE)</option>
                            <option value="ROLE_USER">Khách hàng (USER)</option>
                        </select>
                    </div>

                    <!-- Lọc theo Trạng thái -->
                    <div class="col-lg-2 col-md-3 col-6 mb-3 mb-lg-0">
                        <select class="filter-select" id="filter-status" onchange="onFilterChange()">
                            <option value="">-- Tất cả trạng thái --</option>
                            <option value="active">Đang hoạt động</option>
                            <option value="locked">Bị khóa</option>
                            <option value="inactive">Chưa kích hoạt</option>
                            <option value="banned">Bị cấm</option>
                        </select>
                    </div>

                    <!-- Sắp xếp -->
                    <div class="col-lg-2 col-md-6 col-6 mb-3 mb-lg-0">
                        <select class="filter-select" id="filter-sort" onchange="onFilterChange()">
                            <option value="id-desc">Mới nhất trước</option>
                            <option value="id-asc">Cũ nhất trước</option>
                            <option value="username-asc">Username A-Z</option>
                            <option value="fullName-asc">Họ tên A-Z</option>
                        </select>
                    </div>

                    <!-- Nút Thao tác -->
                    <div class="col-lg-2 col-md-6 col-6 text-right">
                        <button class="btn-male-fashion w-100" onclick="openCreateModal()">
                            <i class="fa fa-plus-circle mr-1"></i> Thêm mới
                        </button>
                    </div>
                </div>
            </div>

            <!-- Bảng danh sách người dùng (User Management Table) -->
            <div class="admin-table-container">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <div>
                        <h5 class="font-weight-bold text-dark mb-1">
                            <i class="fa fa-list-alt text-danger mr-2"></i>Danh sách Tài khoản Người dùng
                        </h5>
                        <small class="text-muted" id="table-summary-text">Đang tải dữ liệu...</small>
                    </div>
                    <div class="d-flex align-items-center">
                        <span class="text-muted mr-2 font-weight-bold" style="font-size: 13px;">Hiển thị:</span>
                        <select id="filter-size" class="form-control form-control-sm d-inline-block" style="width: 70px;" onchange="onPageSizeChange()">
                            <option value="5">5</option>
                            <option value="10" selected>10</option>
                            <option value="20">20</option>
                            <option value="50">50</option>
                        </select>
                        <button class="btn btn-sm btn-outline-secondary ml-2" title="Làm mới bảng" onclick="loadUsers()">
                            <i class="fa fa-refresh"></i>
                        </button>
                    </div>
                </div>

                <div class="table-responsive">
                    <table class="admin-table table-hover">
                        <thead>
                            <tr>
                                <th style="width: 60px;">ID</th>
                                <th>Người dùng</th>
                                <th>Email / Điện thoại</th>
                                <th>Vai trò</th>
                                <th>Trạng thái</th>
                                <th>Ngày tạo</th>
                                <th style="width: 140px;" class="text-center">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody id="user-table-body">
                            <tr>
                                <td colspan="7" class="text-center py-5 text-muted">
                                    <div class="spinner-border text-danger" role="status"></div>
                                    <p class="mt-2 mb-0">Đang tải danh sách người dùng...</p>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>

                <!-- Phân trang chuẩn Male-Fashion -->
                <div class="row align-items-center mt-4">
                    <div class="col-md-6 text-muted" id="pagination-info" style="font-size: 14px;">
                        Hiển thị 0 - 0 trong tổng số 0 người dùng
                    </div>
                    <div class="col-md-6">
                        <div class="product__pagination text-md-right text-center" id="pagination-controls" style="padding-top: 0;">
                            <!-- Pagination buttons injected via JS -->
                        </div>
                    </div>
                </div>
            </div>

        </div>
    </section>
    <!-- Main Content Section End -->

    <!-- ============================================== -->
    <!-- MODALS SECTION -->
    <!-- ============================================== -->

    <!-- Modal 1: Thêm người dùng mới -->
    <div class="modal fade" id="modal-create-user" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title"><i class="fa fa-user-plus mr-2"></i> Thêm Người Dùng Mới</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <form id="form-create-user" onsubmit="handleCreateUser(event)">
                    <div class="modal-body">
                        <div class="row">
                            <div class="col-md-6 form-group">
                                <label>Tên đăng nhập <span class="text-danger">*</span></label>
                                <input type="text" class="form-control" id="create-username" required placeholder="VD: nguyenvan_a">
                            </div>
                            <div class="col-md-6 form-group">
                                <label>Mật khẩu ban đầu <span class="text-danger">*</span></label>
                                <input type="password" class="form-control" id="create-password" required minlength="6" placeholder="Tối thiểu 6 ký tự">
                            </div>
                        </div>

                        <div class="form-group">
                            <label>Họ và tên <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="create-fullname" required placeholder="VD: Nguyễn Văn A">
                        </div>

                        <div class="row">
                            <div class="col-md-6 form-group">
                                <label>Email <span class="text-danger">*</span></label>
                                <input type="email" class="form-control" id="create-email" required placeholder="VD: user@storechain.com">
                            </div>
                            <div class="col-md-6 form-group">
                                <label>Số điện thoại</label>
                                <input type="text" class="form-control" id="create-phone" placeholder="VD: 0912345678">
                            </div>
                        </div>

                        <div class="row">
                            <div class="col-md-6 form-group">
                                <label>Vai trò hệ thống <span class="text-danger">*</span></label>
                                <select class="form-control" id="create-role" required>
                                    <option value="ROLE_USER">Khách hàng (USER)</option>
                                    <option value="ROLE_EMPLOYEE">Nhân viên (EMPLOYEE)</option>
                                    <option value="ROLE_MANAGER">Quản lý (MANAGER)</option>
                                    <option value="ROLE_ADMIN" id="option-create-admin">Quản trị viên (ADMIN)</option>
                                </select>
                            </div>
                            <div class="col-md-6 form-group">
                                <label>Trạng thái</label>
                                <select class="form-control" id="create-status">
                                    <option value="active">Hoạt động (Active)</option>
                                    <option value="locked">Tạm khóa (Locked)</option>
                                    <option value="inactive">Chưa kích hoạt (Inactive)</option>
                                </select>
                            </div>
                        </div>

                        <div class="form-group mb-0">
                            <label>URL Ảnh đại diện (Tùy chọn)</label>
                            <input type="url" class="form-control" id="create-avatar" placeholder="https://...">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-dismiss="modal">Hủy bỏ</button>
                        <button type="submit" class="btn-male-fashion" id="btn-submit-create">
                            <i class="fa fa-save mr-1"></i> Lưu tài khoản
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Modal 2: Chỉnh sửa người dùng -->
    <div class="modal fade" id="modal-edit-user" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title"><i class="fa fa-pencil mr-2"></i> Chỉnh Sửa Thông Tin Người Dùng</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <form id="form-edit-user" onsubmit="handleUpdateUser(event)">
                    <input type="hidden" id="edit-user-id">
                    <div class="modal-body">
                        <div class="row">
                            <div class="col-md-6 form-group">
                                <label>Tên đăng nhập</label>
                                <input type="text" class="form-control bg-light" id="edit-username" readonly disabled>
                            </div>
                            <div class="col-md-6 form-group">
                                <label>Họ và tên <span class="text-danger">*</span></label>
                                <input type="text" class="form-control" id="edit-fullname" required>
                            </div>
                        </div>

                        <div class="row">
                            <div class="col-md-6 form-group">
                                <label>Email <span class="text-danger">*</span></label>
                                <input type="email" class="form-control" id="edit-email" required>
                            </div>
                            <div class="col-md-6 form-group">
                                <label>Số điện thoại</label>
                                <input type="text" class="form-control" id="edit-phone">
                            </div>
                        </div>

                        <div class="row">
                            <div class="col-md-6 form-group">
                                <label>Vai trò <span class="text-danger">*</span></label>
                                <select class="form-control" id="edit-role" required>
                                    <option value="ROLE_USER">Khách hàng (USER)</option>
                                    <option value="ROLE_EMPLOYEE">Nhân viên (EMPLOYEE)</option>
                                    <option value="ROLE_MANAGER">Quản lý (MANAGER)</option>
                                    <option value="ROLE_ADMIN" id="option-edit-admin">Quản trị viên (ADMIN)</option>
                                </select>
                            </div>
                            <div class="col-md-6 form-group">
                                <label>Trạng thái <span class="text-danger">*</span></label>
                                <select class="form-control" id="edit-status" required>
                                    <option value="active">Hoạt động (Active)</option>
                                    <option value="locked">Tạm khóa (Locked)</option>
                                    <option value="inactive">Chưa kích hoạt (Inactive)</option>
                                    <option value="banned">Bị cấm (Banned)</option>
                                </select>
                            </div>
                        </div>

                        <div class="form-group">
                            <label>Đổi mật khẩu mới (Bỏ trống nếu không muốn đổi)</label>
                            <input type="password" class="form-control" id="edit-password" placeholder="Nhập mật khẩu mới (tối thiểu 6 ký tự)" minlength="6">
                        </div>

                        <div class="form-group mb-0">
                            <label>URL Ảnh đại diện</label>
                            <input type="url" class="form-control" id="edit-avatar" placeholder="https://...">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-dismiss="modal">Hủy bỏ</button>
                        <button type="submit" class="btn-male-fashion" id="btn-submit-edit">
                            <i class="fa fa-check mr-1"></i> Lưu thay đổi
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Modal 3: Xem chi tiết người dùng -->
    <div class="modal fade" id="modal-view-user" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title"><i class="fa fa-id-card-o mr-2"></i> Hồ Sơ Chi Tiết Người Dùng</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <div class="text-center mb-4">
                        <div class="user-avatar-wrap mx-auto mb-2" style="width: 70px; height: 70px; font-size: 26px;" id="view-avatar-container">
                            <span id="view-avatar-text">U</span>
                        </div>
                        <h5 class="font-weight-bold mb-1" id="view-fullname">Họ tên người dùng</h5>
                        <p class="text-muted mb-0" id="view-username">@username</p>
                    </div>

                    <div class="detail-row">
                        <span class="detail-label">Mã định danh (ID):</span>
                        <span class="detail-value" id="view-id">--</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Địa chỉ Email:</span>
                        <span class="detail-value" id="view-email">--</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Số điện thoại:</span>
                        <span class="detail-value" id="view-phone">--</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Vai trò:</span>
                        <span class="detail-value" id="view-role">--</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Trạng thái:</span>
                        <span class="detail-value" id="view-status">--</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Mô tả vai trò:</span>
                        <span class="detail-value" id="view-role-desc">--</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Ngày đăng ký:</span>
                        <span class="detail-value" id="view-created-at">--</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">Cập nhật gần nhất:</span>
                        <span class="detail-value" id="view-updated-at">--</span>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-dismiss="modal">Đóng</button>
                    <button type="button" class="btn-male-fashion" id="btn-edit-from-view" onclick="openEditFromView()">
                        <i class="fa fa-pencil mr-1"></i> Chỉnh sửa
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- Modal 4: Xác nhận xóa người dùng -->
    <div class="modal fade" id="modal-delete-confirm" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content">
                <div class="modal-header bg-danger">
                    <h5 class="modal-title text-white"><i class="fa fa-exclamation-triangle mr-2"></i> Xác Nhận Xóa Tài Khoản</h5>
                    <button type="button" class="close text-white" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body text-center py-4">
                    <i class="fa fa-trash text-danger mb-3" style="font-size: 48px;"></i>
                    <h5>Bạn có chắc chắn muốn xóa người dùng này?</h5>
                    <p class="text-muted mb-0" id="delete-user-desc">Hành động này không thể hoàn tác.</p>
                    <input type="hidden" id="delete-user-id">
                </div>
                <div class="modal-footer justify-content-center">
                    <button type="button" class="btn btn-secondary" data-dismiss="modal">Hủy bỏ</button>
                    <button type="button" class="btn btn-danger font-weight-bold px-4" id="btn-confirm-delete" onclick="confirmDeleteUser()">
                        <i class="fa fa-trash mr-1"></i> Xóa vĩnh viễn
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- Modal 5: Đăng nhập Quản trị viên & Quản lý -->
    <div class="modal fade" id="modal-login" tabindex="-1" role="dialog" aria-hidden="true" data-backdrop="static">
        <div class="modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title"><i class="fa fa-lock mr-2"></i> Đăng Nhập Quản Trị Hệ Thống</h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <form id="form-login" onsubmit="handleLogin(event)">
                    <div class="modal-body">
                        <div class="alert alert-info py-2" style="font-size: 13px;">
                            <i class="fa fa-info-circle mr-1"></i> Chức năng quản lý người dùng yêu cầu quyền tài khoản <strong>ADMIN</strong>.
                        </div>

                        <div class="form-group">
                            <label>Tên đăng nhập hoặc Email <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="login-username" required placeholder="admin hoặc manager">
                        </div>

                        <div class="form-group">
                            <label>Mật khẩu <span class="text-danger">*</span></label>
                            <input type="password" class="form-control" id="login-password" required placeholder="Mật khẩu">
                        </div>

                        <div class="mt-3 p-2 bg-light rounded border">
                            <small class="text-muted d-block mb-1 font-weight-bold">Đăng nhập nhanh để kiểm tra:</small>
                            <button type="button" class="btn btn-sm btn-outline-danger mr-2 mb-1" onclick="quickFillLogin('admin', 'admin123')">
                                <i class="fa fa-shield"></i> Tài khoản Admin (admin123)
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

    <!-- Js Plugins -->
    <script src="/js/jquery-3.3.1.min.js"></script>
    <script src="/js/bootstrap.min.js"></script>
    <script src="/js/jquery.nice-select.min.js"></script>
    <script src="/js/jquery.nicescroll.min.js"></script>
    <script src="/js/jquery.magnific-popup.min.js"></script>
    <script src="/js/jquery.slicknav.js"></script>
    <script src="/js/main.js"></script>

    <!-- Admin User Management Application Logic -->
    <script>
        // State Management
        let currentUser = null;
        let currentPage = 0;
        let pageSize = 10;
        let totalPages = 0;
        let totalElements = 0;
        let searchTimeout = null;
        let currentlyViewingUser = null;

        const API_BASE = '/api/admin/users';
        const AUTH_API = '/api/auth/login';

        // Document Ready
        $(document).ready(function () {
            checkAuth();
            setupSearchDebounce();
            loadStats();
            loadUsers();
        });

        // ----------------------------------------------------
        // Authentication & Authorization Helpers
        // ----------------------------------------------------
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
            return token ? { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' } : { 'Content-Type': 'application/json' };
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
                    updateAuthUI();
                } catch (e) {
                    console.error("Lỗi parse user_info:", e);
                    currentUser = null;
                }
            } else {
                updateAuthUI();
            }
        }

        function updateAuthUI() {
            const authContainer = $('#auth-user-info');
            const mobileAuthContainer = $('#mobile-auth-section');
            const roleIndicator = $('#role-indicator');

            if (currentUser && getToken()) {
                const roleName = currentUser.role || (currentUser.roles && currentUser.roles[0]) || 'USER';
                const isCurrentAdmin = roleName.includes('ADMIN');

                if (roleIndicator.length) {
                    roleIndicator.removeClass('badge-danger badge-primary badge-dark')
                        .addClass(isCurrentAdmin ? 'badge-danger' : 'badge-dark')
                        .html(`<i class="fa fa-shield"></i> ${isCurrentAdmin ? 'ADMIN' : roleName.replace('ROLE_', '')}`);
                }

                const displayName = escapeHtml(currentUser.fullName || currentUser.username);
                const isNameHasAdmin = displayName.toLowerCase().includes('quản trị') || displayName.toLowerCase().includes('admin');
                const badgeHtml = isNameHasAdmin ? '' : `<span class="current-user-badge">${isCurrentAdmin ? 'Quản trị' : roleName.replace('ROLE_', '')}</span>`;

                const authHtml = `
                    <span class="text-white mr-3 font-weight-bold">
                        <i class="fa fa-user-circle mr-1 text-danger"></i> ${displayName} ${badgeHtml}
                    </span>
                    <button class="btn btn-sm btn-outline-light" onclick="logout()"><i class="fa fa-sign-out"></i> Đăng xuất</button>
                `;
                authContainer.html(authHtml);
                mobileAuthContainer.html(`<a href="#" onclick="logout()">Đăng xuất (${escapeHtml(currentUser.username)})</a>`);

                // Manager không thể tạo/gán quyền ADMIN
                if (!isCurrentAdmin) {
                    $('#option-create-admin').hide();
                    $('#option-edit-admin').hide();
                } else {
                    $('#option-create-admin').show();
                    $('#option-edit-admin').show();
                }
            } else {
                if (roleIndicator.length) {
                    roleIndicator.removeClass('badge-danger badge-primary')
                        .addClass('badge-dark')
                        .html('<i class="fa fa-lock"></i> CHƯA ĐĂNG NHẬP');
                }

                authContainer.html(`
                    <span class="text-white mr-3"><i class="fa fa-user-circle"></i> Chưa đăng nhập</span>
                    <button class="btn btn-sm btn-outline-light" onclick="openLoginModal()"><i class="fa fa-sign-in"></i> Đăng nhập</button>
                `);
                mobileAuthContainer.html(`<a href="#" onclick="openLoginModal()">Đăng nhập</a>`);
            }
        }

        function openLoginModal() {
            $('#modal-login').modal('show');
        }

        function quickFillLogin(username, password) {
            $('#login-username').val(username);
            $('#login-password').val(password);
        }

        function handleLogin(e) {
            e.preventDefault();
            const username = $('#login-username').val().trim();
            const password = $('#login-password').val().trim();

            if (!username || !password) {
                showToast('Vui lòng nhập tên đăng nhập và mật khẩu!', 'error');
                return;
            }

            $('#btn-submit-login').prop('disabled', true).html('<i class="fa fa-spinner fa-spin"></i> Đang đăng nhập...');

            $.ajax({
                url: AUTH_API,
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({ usernameOrEmail: username, password: password }),
                success: function (res) {
                    $('#btn-submit-login').prop('disabled', false).html('<i class="fa fa-sign-in mr-1"></i> Đăng nhập ngay');
                    if (res && res.success && res.data) {
                        const data = res.data;
                        const role = data.role || (data.roles && data.roles[0]) || '';
                        if (!role.includes('ADMIN')) {
                            showToast('Tài khoản của bạn không có quyền truy cập trang Quản lý Người dùng (yêu cầu ADMIN)!', 'error');
                            return;
                        }

                        localStorage.setItem('sc_auth', JSON.stringify(data));
                        localStorage.setItem('jwt_token', data.token);
                        localStorage.setItem('user_info', JSON.stringify(data));
                        currentUser = data;
                        updateAuthUI();
                        $('#modal-login').modal('hide');
                        showToast(`Đăng nhập thành công! Chào mừng ${data.fullName || data.username}`, 'success');
                        loadStats();
                        loadUsers();
                    } else {
                        showToast(res.message || 'Đăng nhập thất bại!', 'error');
                    }
                },
                error: function (xhr) {
                    $('#btn-submit-login').prop('disabled', false).html('<i class="fa fa-sign-in mr-1"></i> Đăng nhập ngay');
                    let msg = 'Đăng nhập không thành công, vui lòng kiểm tra lại tài khoản hoặc mật khẩu!';
                    if (xhr.responseJSON && xhr.responseJSON.message) {
                        msg = xhr.responseJSON.message;
                    }
                    showToast(msg, 'error');
                }
            });
        }

        function logout() {
            localStorage.removeItem('sc_auth');
            localStorage.removeItem('jwt_token');
            localStorage.removeItem('user_info');
            sessionStorage.removeItem('jwt_token');
            sessionStorage.removeItem('user_info');
            currentUser = null;
            window.location.href = '/index.html';
        }

        // ----------------------------------------------------
        // Dashboard Stats
        // ----------------------------------------------------
        function loadStats() {
            $.ajax({
                url: API_BASE + '/stats',
                type: 'GET',
                headers: getAuthHeaders(),
                success: function (res) {
                    if (res && res.success && res.data) {
                        const stats = res.data;
                        $('#stat-total').text(stats.totalUsers || 0);
                        $('#stat-active').text(stats.activeUsers || 0);
                        $('#stat-locked').text(stats.lockedUsers || 0);
                        $('#stat-admins').text((stats.adminUsers || 0) + (stats.managerUsers || 0));
                    }
                },
                error: function (xhr) {
                    // Nếu lỗi 401/403 thì để 0
                    $('#stat-total').text(0);
                    $('#stat-active').text(0);
                    $('#stat-locked').text(0);
                    $('#stat-admins').text(0);
                }
            });
        }

        // ----------------------------------------------------
        // Load & Search Users
        // ----------------------------------------------------
        function setupSearchDebounce() {
            const searchInput = $('#filter-keyword');
            searchInput.on('input', function () {
                const val = $(this).val();
                if (val.length > 0) {
                    $('#clear-search-btn').show();
                } else {
                    $('#clear-search-btn').hide();
                }

                clearTimeout(searchTimeout);
                searchTimeout = setTimeout(function () {
                    currentPage = 0;
                    loadUsers();
                }, 400);
            });
        }

        function clearSearch() {
            $('#filter-keyword').val('');
            $('#clear-search-btn').hide();
            currentPage = 0;
            loadUsers();
        }

        function onFilterChange() {
            currentPage = 0;
            loadUsers();
        }

        function onPageSizeChange() {
            pageSize = parseInt($('#filter-size').val()) || 10;
            currentPage = 0;
            loadUsers();
        }

        function loadUsers() {
            const tbody = $('#user-table-body');
            tbody.html(`
                <tr>
                    <td colspan="7" class="text-center py-5 text-muted">
                        <div class="spinner-border text-danger" role="status"></div>
                        <p class="mt-2 mb-0">Đang tải danh sách người dùng...</p>
                    </td>
                </tr>
            `);

            const keyword = $('#filter-keyword').val().trim();
            const role = $('#filter-role').val();
            const status = $('#filter-status').val();
            const sortVal = $('#filter-sort').val();
            let sortBy = 'id';
            let sortDir = 'desc';

            if (sortVal) {
                const parts = sortVal.split('-');
                sortBy = parts[0];
                sortDir = parts[1] || 'desc';
            }

            const params = {
                page: currentPage,
                size: pageSize,
                sortBy: sortBy,
                sortDir: sortDir
            };
            if (keyword) params.keyword = keyword;
            if (role) params.role = role;
            if (status) params.status = status;

            $.ajax({
                url: API_BASE,
                type: 'GET',
                data: params,
                headers: getAuthHeaders(),
                success: function (res) {
                    if (res && res.success && res.data) {
                        const pageData = res.data;
                        renderUserTable(pageData.content || []);
                        totalPages = pageData.totalPages || 0;
                        totalElements = pageData.totalElements || 0;
                        renderPagination(pageData);
                    } else {
                        renderEmptyTable("Không thể tải danh sách người dùng!");
                    }
                },
                error: function (xhr) {
                    if (xhr.status === 401 || xhr.status === 403) {
                        tbody.html(`
                            <tr>
                                <td colspan="7" class="text-center py-5">
                                    <i class="fa fa-lock text-danger" style="font-size: 36px;"></i>
                                    <h6 class="mt-2 font-weight-bold text-danger">Chưa đăng nhập hoặc không đủ quyền hạn</h6>
                                    <p class="text-muted">Vui lòng đăng nhập với tài khoản <strong>Admin</strong> để xem và quản lý người dùng.</p>
                                    <button class="btn-male-fashion btn-sm" onclick="openLoginModal()"><i class="fa fa-sign-in"></i> Đăng nhập ngay</button>
                                </td>
                            </tr>
                        `);
                        $('#table-summary-text').text("Yêu cầu quyền Admin");
                        $('#pagination-info').text("Hiển thị 0 - 0 trong tổng số 0 người dùng");
                        $('#pagination-controls').empty();
                    } else {
                        renderEmptyTable("Đã xảy ra lỗi khi kết nối tới máy chủ!");
                    }
                }
            });
        }

        function renderUserTable(users) {
            const tbody = $('#user-table-body');
            tbody.empty();

            if (!users || users.length === 0) {
                renderEmptyTable("Không tìm thấy người dùng nào phù hợp!");
                return;
            }

            const currentRole = currentUser ? (currentUser.role || (currentUser.roles && currentUser.roles[0]) || '') : '';
            const isCurrentAdmin = currentRole.includes('ADMIN');
            const currentUserId = currentUser ? (currentUser.id || currentUser.userId) : null;

            users.forEach(function (u) {
                const initials = (u.fullName || u.username || 'U').substring(0, 2).toUpperCase();
                const avatarContent = u.avatarUrl 
                    ? `<img src="${escapeHtml(u.avatarUrl)}" alt="${escapeHtml(u.username)}" onerror="this.src='/img/about/team-1.jpg'">`
                    : `<span>${initials}</span>`;

                const roleBadge = getRoleBadge(u.roleName || (u.role ? u.role.name : 'ROLE_USER'));
                const statusBadge = getStatusBadge(u.status);
                const createdDate = formatDate(u.createdAt);

                // Permission rules:
                // If currentUser is Manager, they cannot edit/lock/delete an ADMIN account
                const isTargetAdmin = (u.roleName === 'ROLE_ADMIN' || (u.role && u.role.name === 'ROLE_ADMIN'));
                const isSelf = currentUserId && (currentUserId == u.id);

                let canModify = true;
                let modifyTooltip = "";

                if (!isCurrentAdmin && isTargetAdmin) {
                    canModify = false;
                    modifyTooltip = "Manager không có quyền can thiệp tài khoản Admin";
                }

                const deleteDisabled = (!canModify || isSelf) ? 'disabled' : '';
                const deleteTooltip = isSelf ? 'Không thể tự xóa chính mình' : (!canModify ? modifyTooltip : 'Xóa tài khoản');
                const lockDisabled = (!canModify || isSelf) ? 'disabled' : '';
                const lockTooltip = isSelf ? 'Không thể tự khóa chính mình' : (!canModify ? modifyTooltip : (u.status === 'active' ? 'Khóa tài khoản' : 'Kích hoạt lại'));
                const editDisabled = !canModify ? 'disabled' : '';
                const editTooltip = !canModify ? modifyTooltip : 'Chỉnh sửa thông tin';

                const lockIcon = (u.status === 'active') ? 'fa-lock' : 'fa-unlock';

                const tr = `
                    <tr>
                        <td class="font-weight-bold text-muted">${u.id}</td>
                        <td>
                            <div class="d-flex align-items-center">
                                <div class="user-avatar-wrap">
                                    ${avatarContent}
                                </div>
                                <div>
                                    <div class="font-weight-bold text-dark">${escapeHtml(u.fullName || 'Chưa cập nhật')} ${isSelf ? '<span class="badge badge-info text-white" style="font-size:10px;">Bạn</span>' : ''}</div>
                                    <small class="text-muted">@${escapeHtml(u.username)}</small>
                                </div>
                            </div>
                        </td>
                        <td>
                            <div><i class="fa fa-envelope-o text-muted mr-1"></i> ${escapeHtml(u.email || '--')}</div>
                            <small class="text-muted"><i class="fa fa-phone text-muted mr-1"></i> ${escapeHtml(u.phone || 'Chưa có SĐT')}</small>
                        </td>
                        <td>${roleBadge}</td>
                        <td>${statusBadge}</td>
                        <td><small class="text-muted">${createdDate}</small></td>
                        <td class="text-center">
                            <div class="btn-action-group justify-content-center">
                                <button class="btn-action btn-view" title="Xem chi tiết" onclick="openViewModal(${u.id})">
                                    <i class="fa fa-eye"></i>
                                </button>
                                <button class="btn-action btn-edit" title="${editTooltip}" ${editDisabled} onclick="openEditModal(${u.id})">
                                    <i class="fa fa-pencil"></i>
                                </button>
                                <button class="btn-action btn-toggle" title="${lockTooltip}" ${lockDisabled} onclick="toggleUserStatus(${u.id}, '${u.status}')">
                                    <i class="fa ${lockIcon}"></i>
                                </button>
                                <button class="btn-action btn-delete" title="${deleteTooltip}" ${deleteDisabled} onclick="openDeleteModal(${u.id}, '${escapeHtml(u.username)}')">
                                    <i class="fa fa-trash"></i>
                                </button>
                            </div>
                        </td>
                    </tr>
                `;
                tbody.append(tr);
            });

            $('#table-summary-text').text(`Hiển thị trang ${currentPage + 1}/${totalPages} (${totalElements} người dùng)`);
        }

        function renderEmptyTable(message) {
            const tbody = $('#user-table-body');
            tbody.html(`
                <tr>
                    <td colspan="7" class="text-center py-5">
                        <i class="fa fa-folder-open-o text-muted mb-2" style="font-size: 42px;"></i>
                        <h6 class="text-muted font-weight-bold">${message}</h6>
                        <button class="btn btn-sm btn-outline-danger mt-2" onclick="clearSearch()">
                            <i class="fa fa-refresh mr-1"></i> Xóa bộ lọc & Tải lại
                        </button>
                    </td>
                </tr>
            `);
            $('#table-summary-text').text("0 người dùng");
            $('#pagination-info').text("Hiển thị 0 - 0 trong tổng số 0 người dùng");
            $('#pagination-controls').empty();
        }

        function renderPagination(pageData) {
            const controls = $('#pagination-controls');
            controls.empty();

            const total = pageData.totalElements || 0;
            const size = pageData.size || pageSize;
            const number = pageData.number || 0;
            const pages = pageData.totalPages || 0;

            const startIdx = total === 0 ? 0 : (number * size) + 1;
            const endIdx = Math.min((number + 1) * size, total);
            $('#pagination-info').text(`Hiển thị ${startIdx} - ${endIdx} trong tổng số ${total} người dùng`);

            if (pages <= 1) return;

            // Previous button
            if (number > 0) {
                controls.append(`<a href="javascript:void(0)" onclick="goToPage(${number - 1})" title="Trang trước"><i class="fa fa-angle-left"></i></a>`);
            }

            // Page numbers
            const maxVisible = 5;
            let startPage = Math.max(0, number - 2);
            let endPage = Math.min(pages - 1, startPage + maxVisible - 1);
            if (endPage - startPage < maxVisible - 1) {
                startPage = Math.max(0, endPage - maxVisible + 1);
            }

            for (let i = startPage; i <= endPage; i++) {
                const activeClass = (i === number) ? 'active' : '';
                controls.append(`<a href="javascript:void(0)" class="${activeClass}" onclick="goToPage(${i})">${i + 1}</a>`);
            }

            // Next button
            if (number < pages - 1) {
                controls.append(`<a href="javascript:void(0)" onclick="goToPage(${number + 1})" title="Trang sau"><i class="fa fa-angle-right"></i></a>`);
            }
        }

        function goToPage(page) {
            currentPage = page;
            loadUsers();
            $('html, body').animate({ scrollTop: $('.admin-table-container').offset().top - 80 }, 300);
        }

        // ----------------------------------------------------
        // CRUD Operations
        // ----------------------------------------------------
        function openCreateModal() {
            if (!checkLoginRequirement()) return;
            $('#form-create-user')[0].reset();
            $('#create-role').val('ROLE_USER');
            $('#create-status').val('active');
            $('#modal-create-user').modal('show');
        }

        function handleCreateUser(e) {
            e.preventDefault();
            const username = $('#create-username').val().trim();
            const password = $('#create-password').val().trim();
            const fullName = $('#create-fullname').val().trim();
            const email = $('#create-email').val().trim();
            const phone = $('#create-phone').val().trim();
            const role = $('#create-role').val();
            const status = $('#create-status').val();
            const avatarUrl = $('#create-avatar').val().trim();

            const payload = {
                username: username,
                password: password,
                fullName: fullName,
                email: email,
                phone: phone,
                role: role,
                status: status,
                avatarUrl: avatarUrl
            };

            $('#btn-submit-create').prop('disabled', true).html('<i class="fa fa-spinner fa-spin"></i> Đang lưu...');

            $.ajax({
                url: API_BASE,
                type: 'POST',
                headers: getAuthHeaders(),
                data: JSON.stringify(payload),
                success: function (res) {
                    $('#btn-submit-create').prop('disabled', false).html('<i class="fa fa-save mr-1"></i> Lưu tài khoản');
                    if (res && res.success) {
                        $('#modal-create-user').modal('hide');
                        showToast("Thêm người dùng mới thành công!", "success");
                        loadStats();
                        loadUsers();
                    } else {
                        showToast(res.message || "Không thể thêm người dùng!", "error");
                    }
                },
                error: function (xhr) {
                    $('#btn-submit-create').prop('disabled', false).html('<i class="fa fa-save mr-1"></i> Lưu tài khoản');
                    handleAjaxError(xhr, "Lỗi khi thêm người dùng mới!");
                }
            });
        }

        function openEditModal(userId) {
            if (!checkLoginRequirement()) return;

            $.ajax({
                url: `${API_BASE}/${userId}`,
                type: 'GET',
                headers: getAuthHeaders(),
                success: function (res) {
                    if (res && res.success && res.data) {
                        const u = res.data;
                        $('#edit-user-id').val(u.id);
                        $('#edit-username').val(u.username);
                        $('#edit-fullname').val(u.fullName || '');
                        $('#edit-email').val(u.email || '');
                        $('#edit-phone').val(u.phone || '');
                        let roleVal = 'ROLE_USER';
                        if (u.role) {
                            roleVal = (typeof u.role === 'object' && u.role.name) ? u.role.name : u.role;
                        } else if (u.roleName) {
                            roleVal = u.roleName.startsWith('ROLE_') ? u.roleName : 'ROLE_' + u.roleName;
                        }
                        $('#edit-role').val(roleVal);
                        $('#edit-status').val(u.status || 'active');
                        $('#edit-avatar').val(u.avatarUrl || '');
                        $('#edit-password').val('');

                        $('#modal-edit-user').modal('show');
                    } else {
                        showToast("Không tìm thấy thông tin người dùng!", "error");
                    }
                },
                error: function (xhr) {
                    handleAjaxError(xhr, "Lỗi khi tải thông tin người dùng!");
                }
            });
        }

        function handleUpdateUser(e) {
            e.preventDefault();
            const id = $('#edit-user-id').val();
            const fullName = $('#edit-fullname').val().trim();
            const email = $('#edit-email').val().trim();
            const phone = $('#edit-phone').val().trim();
            const role = $('#edit-role').val();
            const status = $('#edit-status').val();
            const avatarUrl = $('#edit-avatar').val().trim();
            const password = $('#edit-password').val().trim();

            const payload = {
                fullName: fullName,
                email: email,
                phone: phone,
                role: role,
                status: status,
                avatarUrl: avatarUrl
            };
            if (password) {
                payload.password = password;
            }

            $('#btn-submit-edit').prop('disabled', true).html('<i class="fa fa-spinner fa-spin"></i> Đang lưu...');

            $.ajax({
                url: `${API_BASE}/${id}`,
                type: 'PUT',
                headers: getAuthHeaders(),
                data: JSON.stringify(payload),
                success: function (res) {
                    $('#btn-submit-edit').prop('disabled', false).html('<i class="fa fa-check mr-1"></i> Lưu thay đổi');
                    if (res && res.success) {
                        $('#modal-edit-user').modal('hide');
                        showToast("Cập nhật thông tin người dùng thành công!", "success");
                        loadStats();
                        loadUsers();
                    } else {
                        showToast(res.message || "Không thể cập nhật người dùng!", "error");
                    }
                },
                error: function (xhr) {
                    $('#btn-submit-edit').prop('disabled', false).html('<i class="fa fa-check mr-1"></i> Lưu thay đổi');
                    handleAjaxError(xhr, "Lỗi khi cập nhật thông tin người dùng!");
                }
            });
        }

        function openViewModal(userId) {
            if (!checkLoginRequirement()) return;

            $.ajax({
                url: `${API_BASE}/${userId}`,
                type: 'GET',
                headers: getAuthHeaders(),
                success: function (res) {
                    if (res && res.success && res.data) {
                        const u = res.data;
                        currentlyViewingUser = u;

                        $('#view-id').text('#' + u.id);
                        $('#view-username').text('@' + u.username);
                        $('#view-fullname').text(u.fullName || 'Chưa cập nhật');
                        $('#view-email').text(u.email || '--');
                        $('#view-phone').text(u.phone || 'Chưa có SĐT');
                        $('#view-role').html(getRoleBadge(u.roleName || (u.role ? u.role.name : 'ROLE_USER')));
                        $('#view-status').html(getStatusBadge(u.status));
                        $('#view-role-desc').text(u.roleDescription || 'Không có mô tả');
                        $('#view-created-at').text(formatDateTime(u.createdAt));
                        $('#view-updated-at').text(formatDateTime(u.updatedAt));

                        const avatarContainer = $('#view-avatar-container');
                        if (u.avatarUrl) {
                            avatarContainer.html(`<img src="${escapeHtml(u.avatarUrl)}" alt="${escapeHtml(u.username)}">`);
                        } else {
                            const initials = (u.fullName || u.username || 'U').substring(0, 2).toUpperCase();
                            avatarContainer.html(`<span>${initials}</span>`);
                        }

                        // Kiểm tra quyền chỉnh sửa
                        const currentRole = currentUser ? (currentUser.role || (currentUser.roles && currentUser.roles[0]) || '') : '';
                        const isCurrentAdmin = currentRole.includes('ADMIN');
                        const isTargetAdmin = (u.roleName === 'ROLE_ADMIN' || (u.role && u.role.name === 'ROLE_ADMIN'));

                        if (!isCurrentAdmin && isTargetAdmin) {
                            $('#btn-edit-from-view').hide();
                        } else {
                            $('#btn-edit-from-view').show();
                        }

                        $('#modal-view-user').modal('show');
                    } else {
                        showToast("Không tìm thấy thông tin người dùng!", "error");
                    }
                },
                error: function (xhr) {
                    handleAjaxError(xhr, "Lỗi khi tải chi tiết người dùng!");
                }
            });
        }

        function openEditFromView() {
            if (currentlyViewingUser) {
                $('#modal-view-user').modal('hide');
                setTimeout(function () {
                    openEditModal(currentlyViewingUser.id);
                }, 300);
            }
        }

        function toggleUserStatus(userId, currentStatus) {
            if (!checkLoginRequirement()) return;

            const newStatus = (currentStatus === 'active') ? 'locked' : 'active';
            const actionText = (newStatus === 'locked') ? 'khóa' : 'mở khóa';

            $.ajax({
                url: `${API_BASE}/${userId}/status?status=${newStatus}`,
                type: 'PATCH',
                headers: getAuthHeaders(),
                success: function (res) {
                    if (res && res.success) {
                        showToast(`Đã ${actionText} tài khoản ID ${userId} thành công!`, "success");
                        loadStats();
                        loadUsers();
                    } else {
                        showToast(res.message || `Không thể ${actionText} tài khoản!`, "error");
                    }
                },
                error: function (xhr) {
                    handleAjaxError(xhr, `Lỗi khi ${actionText} tài khoản!`);
                }
            });
        }

        function openDeleteModal(userId, username) {
            if (!checkLoginRequirement()) return;
            $('#delete-user-id').val(userId);
            $('#delete-user-desc').html(`Bạn đang chọn xóa tài khoản <strong>@${escapeHtml(username)}</strong> (ID: ${userId}). Hành động này sẽ xóa vĩnh viễn tài khoản khỏi hệ thống.`);
            $('#modal-delete-confirm').modal('show');
        }

        function confirmDeleteUser() {
            const userId = $('#delete-user-id').val();
            if (!userId) return;

            $('#btn-confirm-delete').prop('disabled', true).html('<i class="fa fa-spinner fa-spin"></i> Đang xóa...');

            $.ajax({
                url: `${API_BASE}/${userId}`,
                type: 'DELETE',
                headers: getAuthHeaders(),
                success: function (res) {
                    $('#btn-confirm-delete').prop('disabled', false).html('<i class="fa fa-trash mr-1"></i> Xóa vĩnh viễn');
                    $('#modal-delete-confirm').modal('hide');
                    if (res && res.success) {
                        showToast("Đã xóa người dùng thành công!", "success");
                        loadStats();
                        loadUsers();
                    } else {
                        showToast(res.message || "Không thể xóa người dùng!", "error");
                    }
                },
                error: function (xhr) {
                    $('#btn-confirm-delete').prop('disabled', false).html('<i class="fa fa-trash mr-1"></i> Xóa vĩnh viễn');
                    handleAjaxError(xhr, "Lỗi khi xóa người dùng!");
                }
            });
        }

        // ----------------------------------------------------
        // UI Helpers & Utilities
        // ----------------------------------------------------
        function checkLoginRequirement() {
            if (!getToken() || !currentUser) {
                showToast("Vui lòng đăng nhập với quyền Admin!", "error");
                openLoginModal();
                return false;
            }
            return true;
        }

        function handleAjaxError(xhr, defaultMsg) {
            if (xhr.status === 401 || xhr.status === 403) {
                const msg = (xhr.responseJSON && xhr.responseJSON.message) 
                    ? xhr.responseJSON.message 
                    : "Phiên đăng nhập đã hết hạn hoặc bạn không có quyền thực hiện thao tác này!";
                showToast(msg, "error");
                openLoginModal();
            } else if (xhr.responseJSON && xhr.responseJSON.message) {
                showToast(xhr.responseJSON.message, "error");
            } else {
                showToast(defaultMsg, "error");
            }
        }

        function getRoleBadge(role) {
            if (!role) return `<span class="badge-role role-user"><i class="fa fa-user"></i> USER</span>`;
            const r = role.toUpperCase();
            if (r.includes('ADMIN')) return `<span class="badge-role role-admin"><i class="fa fa-shield"></i> ADMIN</span>`;
            if (r.includes('MANAGER')) return `<span class="badge-role role-manager"><i class="fa fa-briefcase"></i> MANAGER</span>`;
            if (r.includes('EMPLOYEE')) return `<span class="badge-role role-employee"><i class="fa fa-id-badge"></i> EMPLOYEE</span>`;
            if (r.includes('USER')) return `<span class="badge-role role-user"><i class="fa fa-user"></i> USER</span>`;
            return `<span class="badge-role role-user"><i class="fa fa-user"></i> ${escapeHtml(role.replace('ROLE_', ''))}</span>`;
        }

        function getStatusBadge(status) {
            if (!status) status = 'active';
            const s = status.toLowerCase();
            if (s === 'active') return `<span class="badge-status status-active"><span class="status-dot"></span> Hoạt động</span>`;
            if (s === 'locked') return `<span class="badge-status status-locked"><span class="status-dot"></span> Bị khóa</span>`;
            if (s === 'banned') return `<span class="badge-status status-banned"><span class="status-dot"></span> Bị cấm</span>`;
            return `<span class="badge-status status-inactive"><span class="status-dot"></span> ${escapeHtml(status)}</span>`;
        }

        function formatDate(dtStr) {
            if (!dtStr) return '--';
            try {
                const d = new Date(dtStr);
                return d.toLocaleDateString('vi-VN');
            } catch (e) {
                return dtStr;
            }
        }

        function formatDateTime(dtStr) {
            if (!dtStr) return '--';
            try {
                const d = new Date(dtStr);
                return d.toLocaleString('vi-VN');
            } catch (e) {
                return dtStr;
            }
        }

        function escapeHtml(text) {
            if (!text) return '';
            return text
                .toString()
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#039;');
        }

        function showToast(message, type = 'info') {
            const container = $('#toast-container');
            const icon = type === 'success' ? 'fa-check-circle text-success' :
                         type === 'error' ? 'fa-exclamation-circle text-danger' : 'fa-info-circle text-info';
            
            const toast = $(`
                <div class="mf-toast toast-${type}">
                    <i class="fa ${icon}" style="font-size: 20px;"></i>
                    <div style="flex: 1; font-size: 14px; font-weight: 600; color: #111;">${escapeHtml(message)}</div>
                    <span style="cursor: pointer; color: #aaa;" onclick="$(this).parent().fadeOut(300, function(){ $(this).remove(); })">&times;</span>
                </div>
            `);

            container.append(toast);
            setTimeout(function () {
                toast.fadeOut(400, function () { $(this).remove(); });
            }, 4500);
        }
    </script>
</body>
</html>

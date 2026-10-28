<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>

<!DOCTYPE html>
<html lang="zxx">
<head>
<meta charset="UTF-8">
    <meta name="description" content="Quản lý Sản phẩm - Male Fashion Admin">
    <meta name="keywords" content="Male_Fashion, admin, product, management, inventory">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title>Quản lý Sản phẩm | Male-Fashion Admin</title>

    <!-- Google Font -->

    <!-- Css Styles -->

    

    

    

    <style>
        /* Card thống kê */
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
        .admin-stat-card.stat-warning { border-left-color: #ff9800; }
        .admin-stat-card.stat-info { border-left-color: #17a2b8; }
        .admin-stat-card.stat-secondary { border-left-color: #6c757d; }

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
        .stat-secondary .stat-icon { background: #f1f3f5; color: #6c757d; }

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

        /* Bộ lọc */
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
            height: 44px;
            width: 100%;
            border: 1px solid #e1e1e1;
            border-radius: 4px;
            padding-left: 40px;
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
            height: 44px;
            width: 100%;
            border: 1px solid #e1e1e1;
            border-radius: 4px;
            padding: 0 12px;
            font-size: 13.5px;
            color: #333333;
            background-color: #ffffff;
            outline: none;
            transition: all 0.3s;
        }

        .filter-select:focus {
            border-color: #e53637;
            box-shadow: 0 0 0 2px rgba(229, 54, 55, 0.15);
        }

        /* Bảng dữ liệu */
        .admin-table-container {
            background: #ffffff;
            border-radius: 8px;
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
            overflow: hidden;
            margin-bottom: 30px;
        }

        .admin-table {
            width: 100%;
            margin-bottom: 0;
            border-collapse: separate;
            border-spacing: 0;
        }

        .admin-table thead th {
            background: #111111;
            color: #ffffff;
            font-weight: 700;
            font-size: 12.5px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            padding: 14px 14px;
            border: none;
            vertical-align: middle;
        }

        .admin-table tbody td {
            padding: 13px 14px;
            vertical-align: middle;
            border-bottom: 1px solid #f0f0f0;
            font-size: 13.5px;
            color: #333333;
        }

        .admin-table tbody tr:hover {
            background-color: #fbfbfb;
        }

        .admin-table tbody tr:last-child td {
            border-bottom: none;
        }

        /* Thumbnail sản phẩm */
        .product-thumb {
            width: 54px;
            height: 54px;
            border-radius: 6px;
            object-fit: cover;
            border: 1px solid #eaeaea;
            background: #fdfdfd;
        }

        .product-thumb-placeholder {
            width: 54px;
            height: 54px;
            border-radius: 6px;
            background: #f1f3f5;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            color: #999;
            font-size: 20px;
        }

        .product-title {
            font-weight: 700;
            color: #111111;
            line-height: 1.3;
            display: inline-block;
            transition: color 0.2s;
        }

        .product-title:hover {
            color: #e53637;
            text-decoration: none;
        }

        .badge-base-sku {
            background: #f1f5f9;
            color: #0f172a;
            font-family: monospace;
            font-weight: 700;
            font-size: 11.5px;
            padding: 3px 6px;
            border-radius: 4px;
            border: 1px solid #e2e8f0;
        }

        .badge-brand {
            background: #fef3c7;
            color: #92400e;
            font-weight: 700;
            font-size: 11px;
            padding: 3px 7px;
            border-radius: 4px;
            display: inline-block;
        }

        .badge-cat {
            background: #f3f4f6;
            color: #4b5563;
            font-weight: 600;
            font-size: 11px;
            padding: 3px 7px;
            border-radius: 4px;
            display: inline-block;
        }

        .badge-price {
            font-weight: 800;
            color: #e53637;
            font-size: 14.5px;
        }

        /* Tồn kho aggregate */
        .stock-aggregate-box {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            cursor: pointer;
            padding: 4px 10px;
            border-radius: 20px;
            font-weight: 700;
            font-size: 12.5px;
            transition: all 0.2s;
        }

        .stock-aggregate-box.stock-ok { background: #e8f5e9; color: #2e7d32; border: 1px solid #c8e6c9; }
        .stock-aggregate-box.stock-low { background: #fff8e1; color: #f57f17; border: 1px solid #ffe082; }
        .stock-aggregate-box.stock-out { background: #ffebee; color: #c62828; border: 1px solid #ffcdd2; }

        .stock-aggregate-box:hover {
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
            transform: scale(1.03);
        }

        /* Nhãn website */
        .badge-tag {
            font-size: 10px;
            font-weight: 800;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            padding: 2px 6px;
            border-radius: 4px;
            margin-right: 3px;
            display: inline-block;
            cursor: pointer;
            transition: all 0.2s;
        }

        .badge-tag.tag-new { background: #fee2e2; color: #dc2626; border: 1px solid #fecaca; }
        .badge-tag.tag-bestseller { background: #ffedd5; color: #ea580c; border: 1px solid #fed7aa; }
        .badge-tag.tag-featured { background: #e0f2fe; color: #0284c7; border: 1px solid #bae6fd; }

        .badge-tag.tag-disabled {
            background: #f3f4f6;
            color: #9ca3af;
            border: 1px dashed #d1d5db;
            opacity: 0.7;
        }

        /* Trạng thái kinh doanh */
        .badge-status {
            padding: 5px 10px;
            border-radius: 20px;
            font-size: 11px;
            font-weight: 700;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 4px;
            transition: all 0.2s;
        }

        .badge-status.status-active { background: #eafaf1; color: #28a745; border: 1px solid #b7ebd0; }
        .badge-status.status-draft { background: #eef2ff; color: #4f46e5; border: 1px solid #c7d2fe; }
        .badge-status.status-inactive { background: #f8f9fa; color: #6c757d; border: 1px solid #ced4da; }

        .badge-status:hover {
            opacity: 0.85;
            transform: scale(1.02);
        }

        /* Nút hành động */
        .btn-action {
            width: 32px;
            height: 32px;
            border-radius: 4px;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            border: 1px solid #e1e1e1;
            background: #ffffff;
            color: #444444;
            font-size: 13px;
            margin: 0 2px;
            cursor: pointer;
            transition: all 0.2s;
        }

        .btn-action:hover {
            background: #111111;
            color: #ffffff;
            border-color: #111111;
        }

        .btn-action.btn-edit:hover { background: #0288d1; border-color: #0288d1; color: #ffffff; }
        .btn-action.btn-delete:hover { background: #e53637; border-color: #e53637; color: #ffffff; }

        /* Nút chính */
        .btn-male-fashion {
            background: #111111;
            color: #ffffff;
            font-size: 13px;
            font-weight: 700;
            text-transform: uppercase;
            letter-spacing: 1px;
            border: none;
            border-radius: 4px;
            padding: 11px 18px;
            transition: all 0.3s;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
        }

        .btn-male-fashion:hover { background: #e53637; color: #ffffff; }

        .btn-male-fashion-secondary {
            background: #f4f4f4;
            color: #333333;
            font-size: 13px;
            font-weight: 700;
            border: 1px solid #e1e1e1;
            border-radius: 4px;
            padding: 11px 18px;
            transition: all 0.3s;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
        }

        .btn-male-fashion-secondary:hover { background: #e9ecef; color: #111111; }

        /* Phân trang */
        .pagination-container {
            padding: 16px 20px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            border-top: 1px solid #f0f0f0;
            background: #ffffff;
        }

        .pagination-info { font-size: 13px; color: #777777; }

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

        .pagination-buttons .btn.active { background: #111111; border-color: #111111; color: #ffffff; }
        .pagination-buttons .btn:disabled { opacity: 0.4; cursor: not-allowed; }

        /* Chips */
        .chip-badge {
            display: inline-block;
            background: #f1f5f9;
            color: #334155;
            padding: 2px 6px;
            font-size: 11px;
            font-weight: 600;
            border-radius: 4px;
            margin-right: 4px;
            margin-bottom: 2px;
        }

        /* Toast thông báo */
        #toast-container {
            position: fixed;
            top: 24px;
            right: 24px;
            z-index: 99999;
            display: flex;
            flex-direction: column;
            gap: 10px;
        }

        .custom-toast {
            min-width: 300px;
            max-width: 420px;
            padding: 14px 18px;
            border-radius: 6px;
            background: #ffffff;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
            display: flex;
            align-items: center;
            gap: 12px;
            animation: slideInRight 0.3s ease;
            border-left: 4px solid #111111;
        }

        .custom-toast.toast-success { border-left-color: #28a745; }
        .custom-toast.toast-danger { border-left-color: #e53637; }
        .custom-toast.toast-warning { border-left-color: #ff9800; }
        .custom-toast.toast-info { border-left-color: #17a2b8; }

        .toast-icon { font-size: 20px; }
        .toast-success .toast-icon { color: #28a745; }
        .toast-danger .toast-icon { color: #e53637; }
        .toast-warning .toast-icon { color: #ff9800; }
        .toast-info .toast-icon { color: #17a2b8; }

        .toast-message { font-size: 13.5px; font-weight: 600; color: #333333; flex: 1; }
        .toast-close { cursor: pointer; color: #aaa; font-size: 16px; }

        @keyframes slideInRight {
            from { transform: translateX(100%); opacity: 0; }
            to { transform: translateX(0); opacity: 1; }
        }

        /* Modal custom */
        .modal-content { border-radius: 8px; border: none; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2); }
        .modal-header { background: #111111; color: #ffffff; padding: 16px 24px; border-top-left-radius: 8px; border-top-right-radius: 8px; }
        .modal-title { font-size: 17px; font-weight: 700; letter-spacing: 0.5px; }
        .modal-header .close { color: #ffffff; opacity: 0.8; font-size: 22px; outline: none; }
        .modal-header .close:hover { opacity: 1; }
        .modal-body { padding: 24px; }

        .form-section-title {
            font-size: 14px;
            font-weight: 800;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            color: #111111;
            padding-bottom: 8px;
            margin-bottom: 16px;
            border-bottom: 2px solid #f0f0f0;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .form-group label {
            font-weight: 700;
            font-size: 12.5px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
            color: #222222;
            margin-bottom: 5px;
        }

        .form-control {
            height: 42px;
            border-radius: 4px;
            border: 1px solid #e1e1e1;
            font-size: 13.5px;
        }

        .form-control:focus {
            border-color: #e53637;
            box-shadow: 0 0 0 2px rgba(229, 54, 55, 0.15);
        }

        .img-preview-box {
            width: 90px;
            height: 90px;
            border: 1px dashed #ccc;
            border-radius: 6px;
            display: flex;
            align-items: center;
            justify-content: center;
            background: #f8f9fa;
            overflow: hidden;
            margin-top: 6px;
        }

        .img-preview-box img { width: 100%; height: 100%; object-fit: cover; }

        /* Bảng biến thể SKU con */
        .variant-table th {
            font-size: 11.5px;
            background: #f8f9fa;
            color: #475569;
            padding: 8px 10px;
            border-top: none;
        }

        .variant-table td {
            font-size: 12.5px;
            padding: 8px 10px;
            vertical-align: middle;
        }

        /* Bảng phân bổ tồn kho */
        .allocation-table th {
            font-size: 12px;
            background: #f1f5f9;
            color: #334155;
            font-weight: 700;
            padding: 10px 12px;
            border: 1px solid #e2e8f0;
        }

        .allocation-table td {
            font-size: 13px;
            padding: 10px 12px;
            border: 1px solid #e2e8f0;
            vertical-align: middle;
        }

        .allocation-table tr.total-row {
            background: #fafafa;
            font-weight: 800;
        }

        .detail-row {
            display: flex;
            justify-content: space-between;
            padding: 9px 0;
            border-bottom: 1px solid #f0f0f0;
            font-size: 13.5px;
        }

        .detail-label { font-weight: 600; color: #666666; }
        .detail-value { font-weight: 700; color: #111111; text-align: right; }
    </style>
</head>
<body>

    

    <!-- Breadcrumb Section Begin -->
    <section class="breadcrumb-option">
        <div class="container">
            <div class="row">
                <div class="col-lg-12">
                    <div class="breadcrumb__text">
                        <h4>Quản lý Sản phẩm</h4>
                        <div class="breadcrumb__links">
                            <a href="/admin-products.html">Hệ thống</a>
                            <span>Danh mục Sản phẩm & Tồn kho Chuỗi</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
    <!-- Breadcrumb Section End -->

    <!-- Main Content Section Begin -->
    <section class="admin-product-section spad pt-4">
        <div class="container">

            <!-- Thống kê tổng quan (Statistics Cards) -->
            <div class="row">
                <div class="col-lg-3 col-md-6 col-sm-6">
                    <div class="admin-stat-card stat-primary">
                        <div>
                            <div class="stat-number" id="stat-total-products">0</div>
                            <div class="stat-label">Tổng sản phẩm</div>
                        </div>
                        <div class="stat-icon">
                            <i class="fa fa-cubes"></i>
                        </div>
                    </div>
                </div>
                <div class="col-lg-3 col-md-6 col-sm-6">
                    <div class="admin-stat-card stat-success">
                        <div>
                            <div class="stat-number" id="stat-active-products">0</div>
                            <div class="stat-label">Đang mở bán</div>
                        </div>
                        <div class="stat-icon">
                            <i class="fa fa-check-circle"></i>
                        </div>
                    </div>
                </div>
                <div class="col-lg-3 col-md-6 col-sm-6">
                    <div class="admin-stat-card stat-warning">
                        <div>
                            <div class="stat-number" id="stat-out-of-stock">0</div>
                            <div class="stat-label">Hết hàng (Stock 0)</div>
                        </div>
                        <div class="stat-icon">
                            <i class="fa fa-exclamation-triangle"></i>
                        </div>
                    </div>
                </div>
                <div class="col-lg-3 col-md-6 col-sm-6">
                    <div class="admin-stat-card stat-info">
                        <div>
                            <div class="stat-number" id="stat-total-stock">0</div>
                            <div class="stat-label">Tổng tồn kho toàn chuỗi</div>
                        </div>
                        <div class="stat-icon">
                            <i class="fa fa-archive"></i>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Bảng bộ lọc & Tìm kiếm -->
            <div class="filter-panel">
                <div class="row align-items-center mb-3">
                    <div class="col-md-6">
                        <h5 class="font-weight-bold mb-0 text-dark">
                            <i class="fa fa-filter text-danger mr-1"></i> Bộ lọc & Tìm kiếm sản phẩm
                        </h5>
                    </div>
                    <div class="col-md-6 text-md-right mt-2 mt-md-0">
                        <button class="btn btn-male-fashion mr-2" onclick="openCreateModal()">
                            <i class="fa fa-plus-circle mr-1"></i> Thêm mới
                        </button>
                        <button class="btn btn-male-fashion-secondary" onclick="resetFilters()" title="Làm mới">
                            <i class="fa fa-refresh mr-1"></i> Làm mới
                        </button>
                    </div>
                </div>

                <div class="row">
                    <!-- Tìm từ khóa -->
                    <div class="col-lg-4 col-md-6 mb-3">
                        <div class="filter-input-group">
                            <i class="fa fa-search search-icon"></i>
                            <input type="text" id="filter-keyword" placeholder="Tìm theo tên, Base SKU, slug, thương hiệu...">
                            <span class="clear-search" id="clear-search-btn" onclick="clearSearch()">&times;</span>
                        </div>
                    </div>

                    <!-- Lọc theo Danh mục -->
                    <div class="col-lg-3 col-md-6 mb-3">
                        <select class="filter-select" id="filter-category" onchange="onFilterChange()">
                            <option value="">-- Tất cả danh mục --</option>
                        </select>
                    </div>

                    <!-- Lọc theo Trạng thái kinh doanh -->
                    <div class="col-lg-3 col-md-6 mb-3">
                        <select class="filter-select" id="filter-status" onchange="onFilterChange()">
                            <option value="">-- Tất cả trạng thái --</option>
                            <option value="active">Đang mở bán</option>
                            <option value="draft">Bản nháp</option>
                            <option value="inactive">Ngừng kinh doanh</option>
                        </select>
                    </div>

                    <!-- Lọc theo Tình trạng kho -->
                    <div class="col-lg-2 col-md-6 mb-3">
                        <select class="filter-select" id="filter-stock" onchange="onFilterChange()">
                            <option value="">-- Tình trạng kho --</option>
                            <option value="in_stock">Còn hàng (> 0)</option>
                            <option value="out_of_stock">Hết hàng (= 0)</option>
                        </select>
                    </div>
                </div>

                <div class="row align-items-center">
                    <div class="col-lg-4 col-md-6 mb-2">
                        <div class="d-flex align-items-center">
                            <span class="text-muted mr-2 font-weight-bold" style="font-size: 13px;">Sắp xếp:</span>
                            <select class="filter-select flex-1" id="filter-sort" onchange="onFilterChange()">
                                <option value="createdAt-desc">Mới nhất trước</option>
                                <option value="createdAt-asc">Cũ nhất trước</option>
                                <option value="price-asc">Giá: Thấp đến Cao</option>
                                <option value="price-desc">Giá: Cao đến Thấp</option>
                                <option value="stock-desc">Tồn kho: Nhiều nhất</option>
                                <option value="stock-asc">Tồn kho: Ít nhất</option>
                                <option value="name-asc">Tên: A &rarr; Z</option>
                            </select>
                        </div>
                    </div>
                    <div class="col-lg-8 col-md-6 mb-2 text-md-right text-muted" style="font-size: 13px;">
                        <span id="filter-result-count">Đang tải sản phẩm...</span>
                    </div>
                </div>
            </div>

            <!-- Bảng hiển thị danh sách sản phẩm -->
            <div class="admin-table-container">
                <div class="table-responsive">
                    <table class="table admin-table">
                        <thead>
                            <tr>
                                <th style="width: 50px;">ID</th>
                                <th style="width: 70px;">Ảnh</th>
                                <th>Thông tin sản phẩm & SKU</th>
                                <th>Danh mục & Thương hiệu</th>
                                <th>Giá niêm yết</th>
                                <th>Tổng tồn kho</th>
                                <th>Nhãn website</th>
                                <th>Trạng thái</th>
                                <th>Ngày tạo</th>
                                <th class="text-right" style="width: 110px;">Hành động</th>
                            </tr>
                        </thead>
                        <tbody id="product-table-body">
                            <!-- Render bằng JS AJAX -->
                        </tbody>
                    </table>
                </div>

                <!-- Phân trang -->
                <div class="pagination-container" id="pagination-container" style="display: none;">
                    <div class="pagination-info" id="pagination-info">
                        Hiển thị 0 - 0 trong tổng số 0 sản phẩm
                    </div>
                    <div class="pagination-buttons" id="pagination-buttons">
                        <!-- Nút phân trang render qua JS -->
                    </div>
                </div>
            </div>

        </div>
    </section>
    <!-- Main Content Section End -->

    <!-- ==================== MODALS ==================== -->

    <!-- Modal Thêm / Chỉnh sửa Sản phẩm -->
    <div class="modal fade" id="productModal" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog modal-xl" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="productModalTitle">
                        <i class="fa fa-cube mr-2"></i> Thêm sản phẩm mới
                    </h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <form id="productForm" onsubmit="handleSaveProduct(event)">
                    <div class="modal-body" style="max-height: calc(85vh - 120px); overflow-y: auto;">
                        <input type="hidden" id="prod-id">

                        <!-- KHỐI 1: THÔNG TIN CƠ BẢN -->
                        <div class="form-section-title">
                            <i class="fa fa-info-circle text-danger"></i> 1. Thông tin cơ bản & Giá niêm yết
                        </div>
                        <div class="row">
                            <div class="col-md-6 form-group">
                                <label for="prod-name">Tên sản phẩm <span class="text-danger">*</span></label>
                                <input type="text" class="form-control" id="prod-name" placeholder="Ví dụ: Áo Sơ Mi Nam Oxford Cổ Đức" required oninput="handleNameChange()">
                            </div>
                            <div class="col-md-3 form-group">
                                <label for="prod-base-sku">Mã gốc (Base SKU / Barcode) <span class="text-danger">*</span></label>
                                <input type="text" class="form-control text-uppercase font-weight-bold" id="prod-base-sku" placeholder="VD: SM-OXF-01" required oninput="handleAttributeChange()">
                            </div>
                            <div class="col-md-3 form-group">
                                <label for="prod-category">Danh mục <span class="text-danger">*</span></label>
                                <select class="form-control" id="prod-category" required>
                                    <option value="">-- Chọn danh mục --</option>
                                </select>
                            </div>
                        </div>

                        <div class="row">
                            <div class="col-md-4 form-group">
                                <label for="prod-price">Giá niêm yết (VND) <span class="text-danger">*</span></label>
                                <input type="number" class="form-control" id="prod-price" min="0" step="1000" placeholder="Ví dụ: 350000" required>
                            </div>
                            <div class="col-md-4 form-group">
                                <label for="prod-brand">Thương hiệu</label>
                                <input type="text" class="form-control" id="prod-brand" placeholder="Ví dụ: Male Fashion">
                            </div>
                            <div class="col-md-4 form-group">
                                <label for="prod-collection">Bộ sưu tập (Collection)</label>
                                <input type="text" class="form-control" id="prod-collection" placeholder="Ví dụ: Summer 2026, Công sở...">
                            </div>
                        </div>

                        <!-- KHỐI 2: THUỘC TÍNH & BIẾN THỂ TỰ SINH -->
                        <div class="form-section-title mt-3">
                            <i class="fa fa-tags text-danger"></i> 2. Thuộc tính & Hệ thống tự sinh SKU con (Variants)
                        </div>
                        <div class="row">
                            <div class="col-md-6 form-group">
                                <label for="prod-colors">Màu sắc (Colors)</label>
                                <input type="text" class="form-control" id="prod-colors" placeholder="Ví dụ: Đen, Trắng, Xanh Navy" oninput="handleAttributeChange()">
                                <small class="text-muted">Nhập các màu cách nhau bởi dấu phẩy.</small>
                            </div>
                            <div class="col-md-6 form-group">
                                <label for="prod-sizes">Kích thước (Sizes)</label>
                                <input type="text" class="form-control" id="prod-sizes" placeholder="Ví dụ: S, M, L, XL, XXL" oninput="handleAttributeChange()">
                                <small class="text-muted">Nhập các size cách nhau bởi dấu phẩy.</small>
                            </div>
                        </div>

                        <!-- Bảng biến thể tự sinh -->
                        <div class="p-3 bg-light rounded border mb-3">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <span class="font-weight-bold text-dark" style="font-size: 13px;">
                                    <i class="fa fa-sitemap mr-1"></i> Danh sách biến thể tự sinh theo Màu & Size
                                </span>
                                <small class="text-muted" id="variant-count-text">0 biến thể</small>
                            </div>
                            <div class="table-responsive" style="max-height: 240px; overflow-y: auto;">
                                <table class="table table-bordered table-sm variant-table bg-white mb-0">
                                    <thead>
                                        <tr>
                                            <th>Mã SKU Con</th>
                                            <th style="width: 110px;">Màu sắc</th>
                                            <th style="width: 90px;">Size</th>
                                            <th>Ảnh riêng theo màu / biến thể</th>
                                            <th style="width: 140px;">Mã vạch (Barcode)</th>
                                        </tr>
                                    </thead>
                                    <tbody id="variant-table-body">
                                        <tr>
                                            <td colspan="5" class="text-center text-muted py-3">
                                                Nhập Mã gốc (Base SKU), Màu sắc và Size ở trên để hệ thống tự động sinh biến thể con.
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>
                        </div>

                        <!-- KHỐI 3: HÌNH ẢNH & MÔ TẢ -->
                        <div class="form-section-title mt-3">
                            <i class="fa fa-picture-o text-danger"></i> 3. Hình ảnh & Bảng quy đổi kích cỡ (Size Chart)
                        </div>
                        <div class="row">
                            <div class="col-md-6 form-group">
                                <label>Ảnh đại diện chính <span class="text-danger">*</span></label>
                                <div class="input-group mb-2">
                                    <input type="text" class="form-control" id="prod-image-url" placeholder="Nhập URL ảnh hoặc tải tệp..." oninput="previewMainImage(this.value)">
                                    <div class="input-group-append">
                                        <button class="btn btn-outline-secondary" type="button" onclick="$('#file-main-image').click()">
                                            <i class="fa fa-upload"></i> Tải ảnh
                                        </button>
                                    </div>
                                </div>
                                <input type="file" id="file-main-image" style="display: none;" accept="image/*" onchange="uploadMainImage(this)">
                                <div class="img-preview-box" id="main-img-preview-box">
                                    <i class="fa fa-picture-o fa-2x text-muted" id="main-img-placeholder"></i>
                                    <img src="" id="main-img-preview" style="display: none;" alt="Preview">
                                </div>
                            </div>
                            <div class="col-md-6 form-group">
                                <label for="prod-size-chart">Bảng quy đổi kích cỡ (Size Chart)</label>
                                <div class="input-group mb-2">
                                    <input type="text" class="form-control" id="prod-size-chart" placeholder="URL ảnh bảng size hoặc hướng dẫn..." oninput="previewSizeChart(this.value)">
                                    <div class="input-group-append">
                                        <button class="btn btn-outline-secondary" type="button" onclick="$('#file-size-chart').click()">
                                            <i class="fa fa-upload"></i> Tải ảnh
                                        </button>
                                    </div>
                                </div>
                                <input type="file" id="file-size-chart" style="display: none;" accept="image/*" onchange="uploadSizeChart(this)">
                                <div class="img-preview-box" id="chart-img-preview-box">
                                    <i class="fa fa-table fa-2x text-muted" id="chart-img-placeholder"></i>
                                    <img src="" id="chart-img-preview" style="display: none;" alt="Size chart preview">
                                </div>
                            </div>
                        </div>

                        <!-- Bộ sưu tập ảnh (Gallery) -->
                        <div class="form-group">
                            <label>Bộ sưu tập ảnh sản phẩm (Gallery)</label>
                            <div class="input-group mb-2">
                                <input type="text" class="form-control" id="prod-gallery-input" placeholder="Nhập URL ảnh rồi nhấn Thêm, hoặc chọn tệp tải lên...">
                                <div class="input-group-append">
                                    <button class="btn btn-outline-secondary" type="button" onclick="addGalleryUrl()">
                                        <i class="fa fa-plus"></i> Thêm link
                                    </button>
                                    <button class="btn btn-outline-secondary" type="button" onclick="$('#file-gallery-image').click()">
                                        <i class="fa fa-upload"></i> Tải tệp
                                    </button>
                                </div>
                            </div>
                            <input type="file" id="file-gallery-image" style="display: none;" accept="image/*" multiple onchange="uploadGalleryImage(this)">
                            <div id="gallery-preview-list" class="d-flex flex-wrap gap-2 pt-1" style="gap: 8px;">
                                <!-- Render danh sách ảnh gallery bằng JS -->
                            </div>
                        </div>

                        <!-- Mô tả chi tiết -->
                        <div class="form-group">
                            <label for="prod-description">Mô tả sản phẩm chi tiết</label>
                            <textarea class="form-control" id="prod-description" rows="3" placeholder="Nhập chất liệu, form dáng, thông số, cách giặt và bảo quản..."></textarea>
                        </div>

                        <!-- KHỐI 4: SEO & CỜ HIỂN THỊ WEBSITE -->
                        <div class="form-section-title mt-3">
                            <i class="fa fa-google text-danger"></i> 4. Tối ưu tìm kiếm (SEO) & Cờ nhãn Website
                        </div>
                        <div class="row">
                            <div class="col-md-6 form-group">
                                <label for="prod-slug">Đường dẫn tĩnh (Slug SEO)</label>
                                <input type="text" class="form-control" id="prod-slug" placeholder="ao-so-mi-nam-oxford">
                            </div>
                            <div class="col-md-6 form-group">
                                <label for="prod-meta-title">Thẻ tiêu đề Google (Meta Title)</label>
                                <input type="text" class="form-control" id="prod-meta-title" placeholder="Áo Sơ Mi Nam Oxford Cao Cấp | Male Fashion">
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="prod-meta-desc">Mô tả ngắn Google (Meta Description)</label>
                            <input type="text" class="form-control" id="prod-meta-desc" placeholder="Mua áo sơ mi nam Oxford cao cấp form chuẩn, vải cotton thoáng mát, giao hàng toàn quốc...">
                        </div>

                        <div class="row align-items-center pt-2">
                            <div class="col-md-4 form-group">
                                <label for="prod-status">Trạng thái kinh doanh <span class="text-danger">*</span></label>
                                <select class="form-control" id="prod-status">
                                    <option value="active">Đang mở bán (Active)</option>
                                    <option value="draft">Bản nháp (Draft)</option>
                                    <option value="inactive">Ngừng kinh doanh (Inactive)</option>
                                </select>
                            </div>
                            <div class="col-md-8 form-group">
                                <label class="d-block">Gắn nhãn website</label>
                                <div class="d-flex flex-wrap" style="gap: 15px;">
                                    <div class="custom-control custom-checkbox">
                                        <input type="checkbox" class="custom-control-input" id="prod-is-new">
                                        <label class="custom-control-label font-weight-bold" for="prod-is-new" style="cursor: pointer;">
                                            <span class="badge-tag tag-new">NEW</span> Hàng mới về
                                        </label>
                                    </div>
                                    <div class="custom-control custom-checkbox">
                                        <input type="checkbox" class="custom-control-input" id="prod-is-bestseller">
                                        <label class="custom-control-label font-weight-bold" for="prod-is-bestseller" style="cursor: pointer;">
                                            <span class="badge-tag tag-bestseller">HOT</span> Bán chạy
                                        </label>
                                    </div>
                                    <div class="custom-control custom-checkbox">
                                        <input type="checkbox" class="custom-control-input" id="prod-is-featured">
                                        <label class="custom-control-label font-weight-bold" for="prod-is-featured" style="cursor: pointer;">
                                            <span class="badge-tag tag-featured">OUTFIT</span> Gợi ý phối đồ
                                        </label>
                                    </div>
                                </div>
                            </div>
                        </div>

                    </div>

                    <div class="modal-footer bg-light">
                        <button type="button" class="btn btn-secondary" data-dismiss="modal">Hủy bỏ</button>
                        <button type="submit" class="btn btn-male-fashion" id="btn-save-product">
                            <i class="fa fa-check mr-1"></i> Lưu thông tin sản phẩm
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Modal Xem nhanh Phân bổ Tồn kho chi nhánh (Read-only) -->
    <div class="modal fade" id="stockAllocationModal" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog modal-lg" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">
                        <i class="fa fa-archive mr-2"></i> Phân bổ Tồn kho theo Chi nhánh (Chỉ xem)
                    </h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <div class="d-flex justify-content-between align-items-center mb-3 p-3 bg-light rounded">
                        <div>
                            <h5 class="font-weight-bold mb-1 text-dark" id="alloc-modal-prod-name">Tên sản phẩm</h5>
                            <span class="badge-base-sku mr-2" id="alloc-modal-base-sku">SKU</span>
                            <span class="text-muted small" id="alloc-modal-cat-name">Danh mục</span>
                        </div>
                        <div class="text-right">
                            <span class="text-muted small text-uppercase font-weight-bold d-block">Tổng tồn kho toàn chuỗi</span>
                            <h3 class="text-success font-weight-bold mb-0" id="alloc-modal-total-stock">0 chiếc</h3>
                        </div>
                    </div>

                    <div class="table-responsive">
                        <table class="table table-bordered allocation-table mb-0" id="alloc-table">
                            <thead id="alloc-table-head">
                                <!-- Render cột header bằng JS -->
                            </thead>
                            <tbody id="alloc-table-body">
                                <!-- Render các dòng chi nhánh bằng JS -->
                            </tbody>
                        </table>
                    </div>
                    <div class="mt-3 text-muted small">
                        <i class="fa fa-info-circle mr-1 text-info"></i>
                        Số lượng tồn kho được phân bổ thực tế tại kho của từng chi nhánh trong chuỗi. Khi khách đặt hàng, hệ thống tự động điều chuyển từ chi nhánh gần nhất.
                    </div>
                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-secondary" data-dismiss="modal">Đóng</button>
                </div>
            </div>
        </div>
    </div>

    <!-- Modal Xem chi tiết Sản phẩm -->
    <div class="modal fade" id="productDetailModal" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog modal-xl" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">
                        <i class="fa fa-info-circle mr-2"></i> Chi tiết Sản phẩm & Phân bổ Tồn kho
                    </h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body" id="product-detail-content" style="max-height: calc(85vh - 120px); overflow-y: auto;">
                    <!-- Injected via JS -->
                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-secondary" data-dismiss="modal">Đóng</button>
                    <button type="button" class="btn btn-primary" id="btn-edit-from-detail">
                        <i class="fa fa-pencil mr-1"></i> Chỉnh sửa sản phẩm
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- Modal Xác nhận Xóa -->
    <div class="modal fade" id="deleteProductModal" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <div class="modal-header bg-danger text-white">
                    <h5 class="modal-title">
                        <i class="fa fa-trash mr-2"></i> Xác nhận Xóa Sản phẩm
                    </h5>
                    <button type="button" class="close text-white" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <p>Bạn có chắc chắn muốn xóa sản phẩm sau không?</p>
                    <h5 class="font-weight-bold text-danger my-3" id="delete-prod-name">Tên sản phẩm</h5>
                    <p class="text-muted small mb-0">
                        <i class="fa fa-info-circle mr-1"></i> Dữ liệu kho của các chi nhánh, giỏ hàng liên quan sẽ được dọn an toàn và giữ nguyên lịch sử đơn hàng.
                    </p>
                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-secondary" data-dismiss="modal">Hủy bỏ</button>
                    <button type="button" class="btn btn-danger" id="btn-confirm-delete" onclick="executeDelete()">
                        <i class="fa fa-trash mr-1"></i> Xác nhận Xóa
                    </button>
                </div>
            </div>
        </div>
    </div>

    <!-- Modal Đăng nhập nhanh -->
    <div class="modal fade" id="loginModal" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">
                        <i class="fa fa-lock mr-2 text-danger"></i> Đăng nhập Quản trị viên
                    </h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <form id="loginForm" onsubmit="handleLogin(event)">
                    <div class="modal-body">
                        <div class="alert alert-info py-2" style="font-size: 13px;">
                            <i class="fa fa-info-circle mr-1"></i> Trang yêu cầu quyền Quản trị viên (<strong>ADMIN</strong>).
                        </div>
                        <div class="form-group">
                            <label for="login-username">Tên đăng nhập hoặc Email</label>
                            <input type="text" class="form-control" id="login-username" required placeholder="Nhập username...">
                        </div>
                        <div class="form-group mb-0">
                            <label for="login-password">Mật khẩu</label>
                            <input type="password" class="form-control" id="login-password" required placeholder="Nhập mật khẩu...">
                        </div>
                    </div>
                    <div class="modal-footer bg-light">
                        <button type="button" class="btn btn-secondary" data-dismiss="modal">Đóng</button>
                        <button type="submit" class="btn btn-male-fashion" id="btn-submit-login">
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
    <script src="/js/jquery.countdown.min.js"></script>
    <script src="/js/jquery.slicknav.js"></script>
    <script src="/js/owl.carousel.min.js"></script>

    <script>
        // API Endpoints
        const API_BASE = '/api/admin/products';
        const AUTH_API = '/api/auth/login';

        // State Variables
        let currentPage = 0;
        let pageSize = 10;
        let totalElements = 0;
        let totalPages = 0;
        let searchTimeout = null;
        let currentlyEditingId = null;
        let deletingId = null;
        let currentLoadedProduct = null;
        let galleryImagesList = [];
        let variantsList = [];
        let allCategories = [];
        let currentUser = null;

        $(document).ready(function () {
            $('#preloder').fadeOut();
            checkAuth();
            setupSearchDebounce();
            loadCategoriesList();
            loadStats();
            loadProducts();
        });

        // ============ Authentication Helpers ============
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

                if (roleIndicator.length) {
                    roleIndicator.removeClass('badge-danger badge-primary badge-dark')
                        .addClass(isAdmin ? 'badge-danger' : 'badge-dark')
                        .html(`<i class="fa fa-shield"></i> ${isAdmin ? 'ADMIN' : roleName.replace('ROLE_', '')}`);
                }

                const authHtml = `
                    <span class="text-white mr-3">
                        <i class="fa fa-user-circle text-danger mr-1"></i>
                        <strong>${currentUser.fullName || currentUser.username}</strong>
                    </span>
                `;
                authContainer.html(authHtml);
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
                        showToast('Tài khoản không có quyền Quản trị viên (ADMIN)!', 'danger');
                        return;
                    }

                    localStorage.setItem('jwt_token', token);
                    localStorage.setItem('user_info', JSON.stringify(userObj));
                    localStorage.setItem('sc_auth', JSON.stringify({ token: token, user: userObj }));
                    currentUser = userObj;

                    $('#loginModal').modal('hide');
                    updateAuthUI();
                    showToast('Đăng nhập thành công với vai trò Quản trị viên!', 'success');
                    loadCategoriesList();
                    loadStats();
                    loadProducts(true);
                },
                error: function (xhr) {
                    submitBtn.prop('disabled', false).html('<i class="fa fa-sign-in mr-1"></i> Đăng nhập ngay');
                    showToast(xhr.responseJSON?.message || 'Đăng nhập thất bại. Kiểm tra lại thông tin!', 'danger');
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

        // ============ Data Loading & Filters ============

        function loadStats() {
            $.ajax({
                url: `${API_BASE}/stats`,
                type: 'GET',
                headers: getAuthHeaders(),
                success: function (res) {
                    if (res.data) {
                        $('#stat-total-products').text(res.data.totalProducts || 0);
                        $('#stat-active-products').text(res.data.activeProducts || 0);
                        $('#stat-out-of-stock').text(res.data.outOfStockProducts || 0);
                        $('#stat-total-stock').text((res.data.totalStock || 0).toLocaleString('vi-VN'));
                    }
                }
            });
        }

        function loadCategoriesList() {
            $.ajax({
                url: `${API_BASE}/categories`,
                type: 'GET',
                headers: getAuthHeaders(),
                success: function (res) {
                    if (res.data) {
                        allCategories = res.data;
                        const filterCat = $('#filter-category');
                        const modalCat = $('#prod-category');

                        filterCat.find('option:not(:first)').remove();
                        modalCat.find('option:not(:first)').remove();

                        res.data.forEach(c => {
                            filterCat.append(`<option value="${c.id}">${c.name}</option>`);
                            modalCat.append(`<option value="${c.id}">${c.name}</option>`);
                        });
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
                    loadProducts();
                }, 400);
            });
        }

        function clearSearch() {
            $('#filter-keyword').val('');
            $('#clear-search-btn').hide();
            currentPage = 0;
            loadProducts();
        }

        function onFilterChange() {
            currentPage = 0;
            loadProducts();
        }

        function resetFilters() {
            $('#filter-keyword').val('');
            $('#clear-search-btn').hide();
            $('#filter-category').val('');
            $('#filter-status').val('');
            $('#filter-stock').val('');
            $('#filter-sort').val('createdAt-desc');
            currentPage = 0;
            loadStats();
            loadProducts();
        }

        // ============ Tải danh sách sản phẩm ============
        let productRequestSeq = 0;
        // silent = true: giữ nguyên bảng hiện tại (không hiện vòng quay), chỉ thay dữ liệu khi tải xong
        function loadProducts(silent) {
            const tbody = $('#product-table-body');
            const seq = ++productRequestSeq;
            if (!silent) tbody.html(`
                <tr>
                    <td colspan="10" class="text-center py-5 text-muted">
                        <i class="fa fa-spinner fa-spin fa-2x mb-3 text-danger"></i>
                        <div>Đang tải dữ liệu sản phẩm...</div>
                    </td>
                </tr>
            `);

            const keyword = $('#filter-keyword').val().trim();
            const categoryId = $('#filter-category').val();
            const status = $('#filter-status').val();
            const stockFilter = $('#filter-stock').val();
            const sortVal = $('#filter-sort').val().split('-');
            const sortBy = sortVal[0] || 'createdAt';
            const sortDir = sortVal[1] || 'desc';

            let inStock = null;
            if (stockFilter === 'in_stock') inStock = true;
            else if (stockFilter === 'out_of_stock') inStock = false;

            const params = {
                page: currentPage,
                size: pageSize,
                sortBy: sortBy,
                sortDir: sortDir
            };
            if (keyword) params.keyword = keyword;
            if (categoryId) params.categoryId = categoryId;
            if (status) params.status = status;
            if (inStock !== null) params.inStock = inStock;

            $.ajax({
                url: API_BASE,
                type: 'GET',
                headers: getAuthHeaders(),
                data: params,
                success: function (res) {
                    if (seq !== productRequestSeq) return; // bỏ qua phản hồi cũ
                    if (res.data && res.data.content) {
                        renderTable(res.data.content);
                        totalElements = res.data.totalElements;
                        totalPages = res.data.totalPages;
                        $('#filter-result-count').text(`Tìm thấy ${totalElements} sản phẩm`);
                        renderPagination();
                    } else {
                        renderEmptyTable('Không có dữ liệu sản phẩm');
                    }
                },
                error: function (xhr) {
                    if (seq !== productRequestSeq) return;
                    if (xhr.status === 401 || xhr.status === 403) {
                        tbody.html(`
                            <tr>
                                <td colspan="10" class="text-center py-5">
                                    <i class="fa fa-lock fa-3x text-danger mb-3"></i>
                                    <h5>Yêu cầu quyền Quản trị viên (ADMIN)</h5>
                                    <p class="text-muted">Vui lòng đăng nhập với tài khoản Quản trị viên để quản lý sản phẩm.</p>
                                    <button class="btn btn-male-fashion" onclick="openLoginModal()">
                                        <i class="fa fa-sign-in mr-1"></i> Đăng nhập ngay
                                    </button>
                                </td>
                            </tr>
                        `);
                        $('#pagination-container').hide();
                    } else {
                        renderEmptyTable('Đã xảy ra lỗi khi tải dữ liệu: ' + (xhr.responseJSON?.message || 'Lỗi kết nối'));
                    }
                }
            });
        }

        function formatVND(amount) {
            if (amount === null || amount === undefined) return '0 đ';
            return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
        }

        function renderTable(products) {
            const tbody = $('#product-table-body');
            tbody.empty();

            if (!products || products.length === 0) {
                renderEmptyTable('Không tìm thấy sản phẩm nào phù hợp với bộ lọc.');
                return;
            }

            products.forEach(p => {
                const imgHtml = p.imageUrl
                    ? `<img src="${p.imageUrl}" alt="${p.name}" class="product-thumb" onerror="this.onerror=null;this.src='/img/icon/heart.png';">`
                    : `<span class="product-thumb-placeholder"><i class="fa fa-picture-o"></i></span>`;

                const priceFormatted = formatVND(p.price);

                // Tồn kho aggregate
                let stockClass = 'stock-ok';
                let stockLabel = `${p.stock} cái`;
                if (p.stock <= 0) {
                    stockClass = 'stock-out';
                    stockLabel = '0 (Hết hàng)';
                } else if (p.stock <= 10) {
                    stockClass = 'stock-low';
                    stockLabel = `${p.stock} (Sắp hết)`;
                }

                // Nhãn website
                const newTag = p.isNew
                    ? `<span class="badge-tag tag-new" onclick="toggleBadge(${p.id}, 'new', false)" title="Bấm để gỡ nhãn New">NEW</span>`
                    : `<span class="badge-tag tag-disabled" onclick="toggleBadge(${p.id}, 'new', true)" title="Bấm để gắn nhãn New">+ NEW</span>`;

                const bestSellerTag = p.isBestSeller
                    ? `<span class="badge-tag tag-bestseller" onclick="toggleBadge(${p.id}, 'bestseller', false)" title="Bấm để gỡ nhãn Hot">HOT</span>`
                    : `<span class="badge-tag tag-disabled" onclick="toggleBadge(${p.id}, 'bestseller', true)" title="Bấm để gắn nhãn Hot">+ HOT</span>`;

                const featuredTag = p.isFeatured
                    ? `<span class="badge-tag tag-featured" onclick="toggleBadge(${p.id}, 'featured', false)" title="Bấm để gỡ nhãn Outfit">OUTFIT</span>`
                    : `<span class="badge-tag tag-disabled" onclick="toggleBadge(${p.id}, 'featured', true)" title="Bấm để gắn nhãn Outfit">+ OUTFIT</span>`;

                // Trạng thái kinh doanh
                let statusBadgeClass = 'status-active';
                let statusText = 'Đang mở bán';
                let nextStatus = 'inactive';
                if (p.status === 'draft') {
                    statusBadgeClass = 'status-draft';
                    statusText = 'Bản nháp';
                    nextStatus = 'active';
                } else if (p.status === 'inactive') {
                    statusBadgeClass = 'status-inactive';
                    statusText = 'Ngừng kinh doanh';
                    nextStatus = 'active';
                }

                const statusBadge = `<span class="badge-status ${statusBadgeClass}" onclick="cycleStatus(${p.id}, '${p.status}')" title="Bấm để chuyển trạng thái">
                    <i class="fa fa-circle" style="font-size: 8px;"></i> ${statusText}
                </span>`;

                // Sizes
                let sizesHtml = '';
                if (p.sizesList && p.sizesList.length > 0) {
                    sizesHtml = `<div class="mt-1">${p.sizesList.map(s => `<span class="chip-badge">${s}</span>`).join('')}</div>`;
                }

                const row = `
                    <tr data-id="${p.id}">
                        <td><strong>#${p.id}</strong></td>
                        <td>${imgHtml}</td>
                        <td>
                            <a href="javascript:void(0)" onclick="viewDetail(${p.id})" class="product-title font-weight-bold">
                                ${p.name}
                            </a>
                            <div class="mt-1 d-flex align-items-center" style="gap: 6px;">
                                <span class="badge-base-sku">${p.baseSku || 'N/A'}</span>
                                <span class="text-monospace text-muted small">${p.slug || ''}</span>
                            </div>
                            ${p.color ? `<div class="text-muted small mt-1">Màu: <strong>${p.color}</strong></div>` : ''}
                            ${sizesHtml}
                        </td>
                        <td>
                            <div class="mb-1"><span class="badge-cat"><i class="fa fa-tag mr-1"></i>${p.categoryName || 'Chưa phân loại'}</span></div>
                            <div><span class="badge-brand"><i class="fa fa-certificate mr-1"></i>${p.brand || 'Male Fashion'}</span></div>
                            ${p.collectionName ? `<div class="small text-muted mt-1"><i class="fa fa-folder-o mr-1"></i>${p.collectionName}</div>` : ''}
                        </td>
                        <td>
                            ${p.onFlashSale && p.originalPrice && Number(p.originalPrice) > Number(p.price)
                                ? `<span class="badge-price text-danger">${priceFormatted}</span><div class="small text-muted mt-1"><del>${formatVND(p.originalPrice)}</del> <span class="badge badge-danger" style="font-size:10px;"><i class="fa fa-bolt"></i> -${p.discountPercent || ''}%</span></div>`
                                : `<span class="badge-price">${priceFormatted}</span>`}
                        </td>
                        <td>
                            <div class="stock-aggregate-box ${stockClass}" onclick="viewQuickStockAllocation(${p.id})" title="Bấm để xem phân bổ tồn kho chi nhánh">
                                <i class="fa fa-archive"></i>
                                <span>${stockLabel}</span>
                                <i class="fa fa-chevron-right ml-1" style="font-size: 10px;"></i>
                            </div>
                        </td>
                        <td>
                            <div class="d-flex flex-wrap" style="gap: 2px;">
                                ${newTag}
                                ${bestSellerTag}
                                ${featuredTag}
                            </div>
                        </td>
                        <td>${statusBadge}</td>
                        <td><small class="text-muted">${p.createdAt ? p.createdAt.substring(0, 10) : 'N/A'}</small></td>
                        <td class="text-right">
                            <button class="btn-action" onclick="viewDetail(${p.id})" title="Xem chi tiết & phân bổ kho">
                                <i class="fa fa-eye"></i>
                            </button>
                            <button class="btn-action btn-edit" onclick="openEditModal(${p.id})" title="Chỉnh sửa sản phẩm">
                                <i class="fa fa-pencil"></i>
                            </button>
                            <button class="btn-action btn-delete" onclick="confirmDelete(${p.id}, '${p.name.replace(/'/g, "\\'")}')" title="Xóa sản phẩm">
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
            $('#product-table-body').html(`
                <tr>
                    <td colspan="10" class="text-center py-5 text-muted">
                        <i class="fa fa-cubes fa-3x mb-3 text-muted"></i>
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
            $('#pagination-info').text(`Hiển thị ${from} - ${to} trong tổng số ${totalElements} sản phẩm`);

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
                loadProducts();
            }
        }

        // ============ Dynamic Variant Generator ============

        function handleNameChange() {
            const name = $('#prod-name').val();
            if (!currentlyEditingId) {
                $('#prod-slug').val(stringToSlug(name));
                $('#prod-meta-title').val(name ? `${name} | Male Fashion` : '');
                if (!$('#prod-base-sku').val()) {
                    $('#prod-base-sku').val(generateBaseSku(name));
                }
            }
            handleAttributeChange();
        }

        function generateBaseSku(name) {
            if (!name) return '';
            const words = stringToSlug(name).toUpperCase().split('-');
            let initials = '';
            for (let w of words) {
                if (w) initials += w[0];
            }
            if (initials.length < 2) initials = 'MF';
            return initials + '-' + Math.floor(100 + Math.random() * 900);
        }

        function handleAttributeChange() {
            const baseSku = $('#prod-base-sku').val().trim().toUpperCase() || 'MF-SP';
            const colorsStr = $('#prod-colors').val();
            const sizesStr = $('#prod-sizes').val();
            const mainImg = $('#prod-image-url').val();

            const colors = parseList(colorsStr);
            const sizes = parseList(sizesStr);

            if (colors.length === 0 && sizes.length === 0) {
                $('#variant-table-body').html(`
                    <tr>
                        <td colspan="5" class="text-center text-muted py-3">
                            Nhập Màu sắc và Size ở trên để hệ thống tự động sinh biến thể con.
                        </td>
                    </tr>
                `);
                $('#variant-count-text').text('0 biến thể');
                variantsList = [];
                return;
            }

            const effectiveColors = colors.length > 0 ? colors : ['Mặc định'];
            const effectiveSizes = sizes.length > 0 ? sizes : ['Freesize'];

            // Bảo lưu các URL ảnh biến thể đã nhập trước đó nếu trùng màu/size
            const oldImagesMap = {};
            variantsList.forEach(v => {
                oldImagesMap[`${v.color}_${v.size}`] = v.imageUrl;
            });

            const newVariants = [];
            effectiveColors.forEach(c => {
                const colorCode = stringToSlug(c).toUpperCase().replace(/-/g, '');
                effectiveSizes.forEach(s => {
                    const sizeCode = stringToSlug(s).toUpperCase().replace(/-/g, '');
                    const childSku = `${baseSku}-${colorCode}-${sizeCode}`;
                    const existingImg = oldImagesMap[`${c}_${s}`] || mainImg || '';

                    newVariants.push({
                        sku: childSku,
                        color: c,
                        size: s,
                        imageUrl: existingImg,
                        barcode: childSku
                    });
                });
            });

            variantsList = newVariants;
            renderVariantTable();
        }

        function renderVariantTable() {
            const tbody = $('#variant-table-body');
            tbody.empty();

            $('#variant-count-text').text(`${variantsList.length} biến thể con`);

            variantsList.forEach((v, index) => {
                const row = `
                    <tr>
                        <td>
                            <strong class="text-dark font-monospace">${v.sku}</strong>
                        </td>
                        <td>
                            <span class="badge badge-light border px-2 py-1 font-weight-bold">${v.color}</span>
                        </td>
                        <td>
                            <span class="badge badge-secondary px-2 py-1">${v.size}</span>
                        </td>
                        <td>
                            <div class="input-group input-group-sm">
                                <input type="text" class="form-control" value="${v.imageUrl || ''}" placeholder="URL ảnh riêng..." oninput="updateVariantImage(${index}, this.value)">
                                <div class="input-group-append">
                                    <button class="btn btn-outline-secondary" type="button" onclick="triggerVariantUpload(${index})">
                                        <i class="fa fa-upload"></i>
                                    </button>
                                </div>
                            </div>
                        </td>
                        <td>
                            <span class="text-monospace text-muted small">${v.barcode}</span>
                        </td>
                    </tr>
                `;
                tbody.append(row);
            });
        }

        function updateVariantImage(index, val) {
            if (variantsList[index]) {
                variantsList[index].imageUrl = val.trim();
            }
        }

        let currentVariantUploadIndex = null;
        function triggerVariantUpload(index) {
            currentVariantUploadIndex = index;
            const input = $('<input type="file" accept="image/*" style="display:none;">');
            input.on('change', function () {
                if (this.files && this.files.length > 0) {
                    uploadVariantImageFile(this.files[0], currentVariantUploadIndex);
                }
            });
            input.click();
        }

        function uploadVariantImageFile(file, index) {
            const formData = new FormData();
            formData.append('file', file);
            showToast('Đang tải ảnh biến thể lên...', 'info');

            const token = getToken();
            $.ajax({
                url: `${API_BASE}/upload-image`,
                type: 'POST',
                headers: token ? { 'Authorization': 'Bearer ' + token } : {},
                data: formData,
                processData: false,
                contentType: false,
                success: function (res) {
                    if (res.data && res.data.url) {
                        if (variantsList[index]) {
                            variantsList[index].imageUrl = res.data.url;
                            renderVariantTable();
                            showToast('Đã cập nhật ảnh riêng cho biến thể!', 'success');
                        }
                    }
                },
                error: function (xhr) {
                    showToast(xhr.responseJSON?.message || 'Không thể tải ảnh biến thể!', 'danger');
                }
            });
        }

        function parseList(str) {
            if (!str) return [];
            return str.split(',').map(s => s.trim()).filter(s => s.length > 0);
        }

        function stringToSlug(str) {
            if (!str) return '';
            str = str.toLowerCase();
            str = str.normalize('NFD').replace(/[\u0300-\u036f]/g, '');
            str = str.replace(/[đĐ]/g, 'd');
            str = str.replace(/([^0-9a-z-\s])/g, '');
            str = str.replace(/(\s+)/g, '-');
            str = str.replace(/-+/g, '-');
            str = str.replace(/^-+|-+$/g, '');
            return str;
        }

        // ============ Image & Gallery Handlers ============

        function previewMainImage(url) {
            const preview = $('#main-img-preview');
            const placeholder = $('#main-img-placeholder');
            if (url && url.trim().length > 0) {
                preview.attr('src', url).show();
                placeholder.hide();
            } else {
                preview.hide().attr('src', '');
                placeholder.show();
            }
        }

        function previewSizeChart(url) {
            const preview = $('#chart-img-preview');
            const placeholder = $('#chart-img-placeholder');
            if (url && url.trim().length > 0) {
                preview.attr('src', url).show();
                placeholder.hide();
            } else {
                preview.hide().attr('src', '');
                placeholder.show();
            }
        }

        function uploadMainImage(input) {
            if (!input.files || input.files.length === 0) return;
            const file = input.files[0];
            const formData = new FormData();
            formData.append('file', file);
            showToast('Đang tải ảnh đại diện lên...', 'info');

            const token = getToken();
            $.ajax({
                url: `${API_BASE}/upload-image`,
                type: 'POST',
                headers: token ? { 'Authorization': 'Bearer ' + token } : {},
                data: formData,
                processData: false,
                contentType: false,
                success: function (res) {
                    if (res.data && res.data.url) {
                        $('#prod-image-url').val(res.data.url);
                        previewMainImage(res.data.url);
                        showToast('Tải ảnh đại diện thành công!', 'success');
                    }
                },
                error: function (xhr) {
                    showToast(xhr.responseJSON?.message || 'Không thể tải ảnh lên!', 'danger');
                }
            });
        }

        function uploadSizeChart(input) {
            if (!input.files || input.files.length === 0) return;
            const file = input.files[0];
            const formData = new FormData();
            formData.append('file', file);
            showToast('Đang tải ảnh bảng size lên...', 'info');

            const token = getToken();
            $.ajax({
                url: `${API_BASE}/upload-image`,
                type: 'POST',
                headers: token ? { 'Authorization': 'Bearer ' + token } : {},
                data: formData,
                processData: false,
                contentType: false,
                success: function (res) {
                    if (res.data && res.data.url) {
                        $('#prod-size-chart').val(res.data.url);
                        previewSizeChart(res.data.url);
                        showToast('Tải bảng size thành công!', 'success');
                    }
                },
                error: function (xhr) {
                    showToast(xhr.responseJSON?.message || 'Không thể tải ảnh lên!', 'danger');
                }
            });
        }

        function addGalleryUrl() {
            const url = $('#prod-gallery-input').val().trim();
            if (!url) return;
            galleryImagesList.push(url);
            $('#prod-gallery-input').val('');
            renderGalleryList();
        }

        function uploadGalleryImage(input) {
            if (!input.files || input.files.length === 0) return;
            const token = getToken();

            Array.from(input.files).forEach(file => {
                const formData = new FormData();
                formData.append('file', file);
                $.ajax({
                    url: `${API_BASE}/upload-image`,
                    type: 'POST',
                    headers: token ? { 'Authorization': 'Bearer ' + token } : {},
                    data: formData,
                    processData: false,
                    contentType: false,
                    success: function (res) {
                        if (res.data && res.data.url) {
                            galleryImagesList.push(res.data.url);
                            renderGalleryList();
                        }
                    }
                });
            });
            showToast('Đã tải ảnh gallery lên!', 'success');
        }

        function removeGalleryItem(index) {
            galleryImagesList.splice(index, 1);
            renderGalleryList();
        }

        function renderGalleryList() {
            const container = $('#gallery-preview-list');
            container.empty();
            galleryImagesList.forEach((url, index) => {
                container.append(`
                    <div style="position: relative; width: 70px; height: 70px; border-radius: 6px; overflow: hidden; border: 1px solid #ddd;">
                        <img src="${url}" style="width: 100%; height: 100%; object-fit: cover;" onerror="this.onerror=null;this.src='/img/icon/heart.png';">
                        <span onclick="removeGalleryItem(${index})" style="position: absolute; top: 2px; right: 2px; background: rgba(0,0,0,0.6); color: #fff; width: 18px; height: 18px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 11px; cursor: pointer;">&times;</span>
                    </div>
                `);
            });
        }

        // ============ Modal Actions: Create / Edit ============

        function openCreateModal() {
            currentlyEditingId = null;
            $('#productModalTitle').html('<i class="fa fa-plus-circle mr-2"></i> Thêm sản phẩm mới');
            $('#prod-id').val('');
            $('#prod-name').val('');
            $('#prod-base-sku').val('');
            $('#prod-category').val('');
            $('#prod-brand').val('Male Fashion');
            $('#prod-collection').val('');
            $('#prod-price').val('');
            $('#prod-colors').val('Đen, Trắng');
            $('#prod-sizes').val('S, M, L, XL');
            $('#prod-image-url').val('');
            $('#prod-size-chart').val('');
            $('#prod-description').val('');
            $('#prod-slug').val('');
            $('#prod-meta-title').val('');
            $('#prod-meta-desc').val('');
            $('#prod-status').val('active');
            $('#prod-is-new').prop('checked', true);
            $('#prod-is-bestseller').prop('checked', false);
            $('#prod-is-featured').prop('checked', false);

            previewMainImage('');
            previewSizeChart('');
            galleryImagesList = [];
            renderGalleryList();
            variantsList = [];
            handleAttributeChange();

            $('#productModal').modal('show');
        }

        function openEditModal(id) {
            currentlyEditingId = id;
            $('#productModalTitle').html(`<i class="fa fa-pencil mr-2"></i> Cập nhật sản phẩm #${id}`);

            $.ajax({
                url: `${API_BASE}/${id}`,
                type: 'GET',
                headers: getAuthHeaders(),
                success: function (res) {
                    const p = res.data;
                    $('#prod-id').val(p.id);
                    $('#prod-name').val(p.name);
                    $('#prod-base-sku').val(p.baseSku || '');
                    $('#prod-category').val(p.categoryId);
                    $('#prod-brand').val(p.brand || 'Male Fashion');
                    $('#prod-collection').val(p.collectionName || '');
                    $('#prod-price').val(p.originalPrice != null ? p.originalPrice : p.price);
                    $('#prod-colors').val(p.color || '');
                    $('#prod-sizes').val(p.sizes || '');
                    $('#prod-image-url').val(p.imageUrl || '');
                    $('#prod-size-chart').val(p.sizeChart || '');
                    $('#prod-description').val(p.description || '');
                    $('#prod-slug').val(p.slug || '');
                    $('#prod-meta-title').val(p.metaTitle || '');
                    $('#prod-meta-desc').val(p.metaDescription || '');
                    $('#prod-status').val(p.status || 'active');
                    $('#prod-is-new').prop('checked', !!p.isNew);
                    $('#prod-is-bestseller').prop('checked', !!p.isBestSeller);
                    $('#prod-is-featured').prop('checked', !!p.isFeatured);

                    previewMainImage(p.imageUrl);
                    previewSizeChart(p.sizeChart);
                    galleryImagesList = p.galleryImages || [];
                    renderGalleryList();

                    variantsList = p.variants || [];
                    if (variantsList.length === 0) {
                        handleAttributeChange();
                    } else {
                        renderVariantTable();
                    }

                    $('#productDetailModal').modal('hide');
                    $('#productModal').modal('show');
                },
                error: function (xhr) {
                    showToast(xhr.responseJSON?.message || 'Không thể tải thông tin sản phẩm!', 'danger');
                }
            });
        }

        function handleSaveProduct(e) {
            e.preventDefault();

            const name = $('#prod-name').val().trim();
            const baseSku = $('#prod-base-sku').val().trim();
            const categoryId = $('#prod-category').val();
            const brand = $('#prod-brand').val().trim();
            const collectionName = $('#prod-collection').val().trim();
            const price = parseFloat($('#prod-price').val());
            const imageUrl = $('#prod-image-url').val().trim();
            const sizeChart = $('#prod-size-chart').val().trim();
            const description = $('#prod-description').val().trim();
            const slug = $('#prod-slug').val().trim();
            const metaTitle = $('#prod-meta-title').val().trim();
            const metaDescription = $('#prod-meta-desc').val().trim();
            const colors = $('#prod-colors').val().trim();
            const sizes = $('#prod-sizes').val().trim();
            const status = $('#prod-status').val();
            const isNew = $('#prod-is-new').is(':checked');
            const isBestSeller = $('#prod-is-bestseller').is(':checked');
            const isFeatured = $('#prod-is-featured').is(':checked');

            if (!name) {
                showToast('Vui lòng nhập tên sản phẩm!', 'warning');
                return;
            }
            if (!categoryId) {
                showToast('Vui lòng chọn danh mục sản phẩm!', 'warning');
                return;
            }
            if (isNaN(price) || price < 0) {
                showToast('Giá niêm yết không hợp lệ (>= 0)!', 'warning');
                return;
            }

            const payload = {
                name: name,
                baseSku: baseSku || null,
                categoryId: parseInt(categoryId),
                brand: brand || 'Male Fashion',
                collectionName: collectionName || null,
                price: price,
                imageUrl: imageUrl || null,
                galleryImages: galleryImagesList,
                sizeChart: sizeChart || null,
                description: description || null,
                slug: slug || null,
                metaTitle: metaTitle || null,
                metaDescription: metaDescription || null,
                color: colors || null,
                sizes: sizes || null,
                variants: variantsList,
                status: status,
                isNew: isNew,
                isBestSeller: isBestSeller,
                isFeatured: isFeatured
            };

            const submitBtn = $('#btn-save-product');
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
                    submitBtn.prop('disabled', false).html('<i class="fa fa-check mr-1"></i> Lưu thông tin sản phẩm');
                    $('#productModal').modal('hide');
                    showToast(res.message || (isEdit ? 'Cập nhật sản phẩm thành công!' : 'Thêm mới sản phẩm thành công!'), 'success');
                    loadStats();
                    loadProducts(true);
                },
                error: function (xhr) {
                    submitBtn.prop('disabled', false).html('<i class="fa fa-check mr-1"></i> Lưu thông tin sản phẩm');
                    showToast(xhr.responseJSON?.message || 'Lưu sản phẩm thất bại!', 'danger');
                }
            });
        }

        // ============ Quick Actions: Badges & Status ============

        function toggleBadge(id, badgeType, newValue) {
            $.ajax({
                url: `${API_BASE}/${id}/badge?badge=${badgeType}&value=${newValue}`,
                type: 'PATCH',
                headers: getAuthHeaders(),
                success: function (res) {
                    showToast('Đã cập nhật nhãn website!', 'success');
                    loadProducts(true);
                },
                error: function (xhr) {
                    showToast(xhr.responseJSON?.message || 'Không thể đổi nhãn', 'danger');
                }
            });
        }

        function cycleStatus(id, currentStatus) {
            let nextStatus = 'active';
            if (currentStatus === 'active') nextStatus = 'draft';
            else if (currentStatus === 'draft') nextStatus = 'inactive';
            else nextStatus = 'active';

            $.ajax({
                url: `${API_BASE}/${id}/status?status=${nextStatus}`,
                type: 'PATCH',
                headers: getAuthHeaders(),
                success: function (res) {
                    showToast(`Đã chuyển trạng thái sang: ${nextStatus === 'active' ? 'Đang mở bán' : nextStatus === 'draft' ? 'Bản nháp' : 'Ngừng kinh doanh'}`, 'success');
                    loadStats();
                    loadProducts(true);
                },
                error: function (xhr) {
                    showToast(xhr.responseJSON?.message || 'Không thể đổi trạng thái', 'danger');
                }
            });
        }

        // ============ Modal Xem nhanh phân bổ tồn kho (Read-only) ============

        function viewQuickStockAllocation(productId) {
            $.ajax({
                url: `${API_BASE}/${productId}`,
                type: 'GET',
                headers: getAuthHeaders(),
                success: function (res) {
                    const p = res.data;
                    $('#alloc-modal-prod-name').text(p.name);
                    $('#alloc-modal-base-sku').text(p.baseSku || 'SKU');
                    $('#alloc-modal-cat-name').text(p.categoryName || 'Chưa phân loại');
                    $('#alloc-modal-total-stock').text(`${p.stock || 0} chiếc`);

                    renderAllocationTable(p.branchAllocations, p.sizesList);
                    $('#stockAllocationModal').modal('show');
                },
                error: function (xhr) {
                    showToast('Không thể tải dữ liệu kho chi nhánh!', 'danger');
                }
            });
        }

        function renderAllocationTable(allocations, sizesList) {
            const thead = $('#alloc-table-head');
            const tbody = $('#alloc-table-body');
            thead.empty();
            tbody.empty();

            const sizes = (sizesList && sizesList.length > 0) ? sizesList : ['Tiêu chuẩn'];

            // Header row
            let headerHtml = `
                <tr>
                    <th style="min-width: 220px;">Chi nhánh cửa hàng</th>
            `;
            sizes.forEach(s => {
                headerHtml += `<th class="text-center" style="width: 80px;">Size ${s}</th>`;
            });
            headerHtml += `<th class="text-center" style="width: 120px;">Tổng tại kho</th></tr>`;
            thead.html(headerHtml);

            if (!allocations || allocations.length === 0) {
                tbody.html(`
                    <tr>
                        <td colspan="${sizes.length + 2}" class="text-center text-muted py-4">
                            Chưa có dữ liệu phân bổ kho tại các chi nhánh.
                        </td>
                    </tr>
                `);
                return;
            }

            // Tính tổng theo từng size toàn hệ thống
            const totalsBySize = {};
            sizes.forEach(s => totalsBySize[s] = 0);
            let grandTotal = 0;

            allocations.forEach(a => {
                let rowHtml = `
                    <tr>
                        <td>
                            <strong>${a.shopName}</strong>
                            ${a.shopAddress ? `<div class="small text-muted">${a.shopAddress}</div>` : ''}
                        </td>
                `;

                const sizeMap = a.sizeStocks || {};
                sizes.forEach(s => {
                    const qty = sizeMap[s] !== undefined ? sizeMap[s] : (sizeMap['Tiêu chuẩn'] || 0);
                    totalsBySize[s] += qty;
                    rowHtml += `<td class="text-center font-weight-bold ${qty <= 0 ? 'text-danger' : 'text-dark'}">${qty}</td>`;
                });

                grandTotal += (a.totalStock || 0);
                rowHtml += `<td class="text-center font-weight-bold text-primary">${a.totalStock || 0}</td></tr>`;
                tbody.append(rowHtml);
            });

            // Dòng tổng cộng toàn chuỗi
            let footerHtml = `
                <tr class="total-row">
                    <td><i class="fa fa-building-o mr-1"></i> <strong>TỔNG TOÀN CHUỖI CỬA HÀNG</strong></td>
            `;
            sizes.forEach(s => {
                footerHtml += `<td class="text-center font-weight-bold text-success">${totalsBySize[s]}</td>`;
            });
            footerHtml += `<td class="text-center font-weight-bold text-success" style="font-size: 15px;">${grandTotal} chiếc</td></tr>`;
            tbody.append(footerHtml);
        }

        // ============ Modal Xem chi tiết sản phẩm đầy đủ ============

        function viewDetail(id) {
            $.ajax({
                url: `${API_BASE}/${id}`,
                type: 'GET',
                headers: getAuthHeaders(),
                success: function (res) {
                    const p = res.data;
                    const content = $('#product-detail-content');

                    // Gallery thumbnails
                    let galleryHtml = '';
                    if (p.galleryImages && p.galleryImages.length > 0) {
                        galleryHtml = `
                            <div class="mt-2 d-flex flex-wrap" style="gap: 8px;">
                                ${p.galleryImages.map(img => `
                                    <img src="${img}" style="width: 50px; height: 50px; border-radius: 4px; object-fit: cover; border: 1px solid #ddd;">
                                `).join('')}
                            </div>
                        `;
                    }

                    // Variants preview table
                    let variantsHtml = '';
                    if (p.variants && p.variants.length > 0) {
                        variantsHtml = `
                            <div class="mt-4">
                                <h6 class="font-weight-bold text-dark mb-2"><i class="fa fa-sitemap mr-1"></i> Danh sách biến thể SKU con (${p.variants.length} SKU)</h6>
                                <div class="table-responsive" style="max-height: 200px; overflow-y: auto;">
                                    <table class="table table-sm table-bordered bg-white mb-0" style="font-size: 12.5px;">
                                        <thead class="bg-light">
                                            <tr>
                                                <th>Mã SKU Con</th>
                                                <th>Màu sắc</th>
                                                <th>Size</th>
                                                <th>Mã vạch</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            ${p.variants.map(v => `
                                                <tr>
                                                    <td class="font-weight-bold text-monospace">${v.sku}</td>
                                                    <td>${v.color}</td>
                                                    <td><span class="badge badge-secondary">${v.size}</span></td>
                                                    <td class="text-muted">${v.barcode || v.sku}</td>
                                                </tr>
                                            `).join('')}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        `;
                    }

                    content.html(`
                        <div class="row mb-4">
                            <div class="col-md-4 text-center">
                                ${p.imageUrl
                                    ? `<img src="${p.imageUrl}" style="width: 100%; max-height: 240px; object-fit: cover; border-radius: 8px; border: 1px solid #eee;" onerror="this.onerror=null;this.src='/img/icon/heart.png';">`
                                    : `<div style="width: 100%; height: 200px; background: #f8f9fa; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 40px; color: #ccc;"><i class="fa fa-cube"></i></div>`
                                }
                                ${galleryHtml}
                            </div>
                            <div class="col-md-8">
                                <h4 class="font-weight-bold text-dark mb-1">${p.name}</h4>
                                <div class="d-flex align-items-center mb-2" style="gap: 8px;">
                                    <span class="badge-base-sku font-weight-bold">Mã gốc: ${p.baseSku || 'N/A'}</span>
                                    <span class="badge-brand"><i class="fa fa-certificate mr-1"></i>${p.brand || 'Male Fashion'}</span>
                                    <span class="badge-cat"><i class="fa fa-tag mr-1"></i>${p.categoryName || 'Chưa phân loại'}</span>
                                </div>
                                ${p.onFlashSale && p.originalPrice && Number(p.originalPrice) > Number(p.price)
                                    ? `<h3 class="text-danger font-weight-bold my-2">${formatVND(p.price)} <small class="text-muted ml-2" style="text-decoration:line-through;font-size:15px;">${formatVND(p.originalPrice)}</small> <span class="badge badge-danger ml-2" style="font-size:12px;vertical-align:middle;"><i class="fa fa-bolt"></i> Flash Sale -${p.discountPercent || ''}%</span></h3>`
                                    : `<h3 class="text-danger font-weight-bold my-2">${formatVND(p.price)}</h3>`}
                                <div class="my-2">
                                    ${p.isNew ? '<span class="badge-tag tag-new">HÀNG MỚI (NEW)</span>' : ''}
                                    ${p.isBestSeller ? '<span class="badge-tag tag-bestseller">BÁN CHẠY (HOT)</span>' : ''}
                                    ${p.isFeatured ? '<span class="badge-tag tag-featured">GỢI Ý PHỐI ĐỒ</span>' : ''}
                                </div>
                                <div class="p-2 bg-light rounded text-monospace small mt-2">
                                    <div><strong>Đường dẫn SEO:</strong> /product/${p.slug || ''}</div>
                                    <div><strong>Meta Title:</strong> ${p.metaTitle || 'Mặc định'}</div>
                                    <div><strong>Meta Description:</strong> ${p.metaDescription || 'Mặc định'}</div>
                                </div>
                            </div>
                        </div>

                        <!-- Khối tồn kho phân bổ -->
                        <div class="p-3 bg-light rounded border mb-3">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <h6 class="font-weight-bold text-dark mb-0"><i class="fa fa-archive mr-1"></i> Bảng phân bổ tồn kho các chi nhánh (Aggregate)</h6>
                                <span class="badge badge-success px-2 py-1 font-weight-bold" style="font-size: 13px;">Tổng tồn kho: ${p.stock || 0} cái</span>
                            </div>
                            <div class="table-responsive">
                                <table class="table table-sm table-bordered bg-white allocation-table mb-0" id="detail-alloc-table">
                                    <!-- Render phân bổ tồn kho -->
                                </table>
                            </div>
                        </div>

                        ${variantsHtml}

                        <div class="row mt-3">
                            <div class="col-md-6">
                                <h6 class="font-weight-bold">Mô tả sản phẩm:</h6>
                                <div class="p-3 bg-light rounded text-muted" style="white-space: pre-line; font-size: 13px;">
                                    ${p.description || 'Chưa có mô tả chi tiết.'}
                                </div>
                            </div>
                            <div class="col-md-6">
                                <h6 class="font-weight-bold">Bảng quy đổi kích cỡ (Size Chart):</h6>
                                <div class="p-3 bg-light rounded text-center">
                                    ${p.sizeChart
                                        ? `<img src="${p.sizeChart}" style="max-width: 100%; max-height: 180px; object-fit: contain; border-radius: 4px;" alt="Size chart">`
                                        : '<span class="text-muted small">Chưa cập nhật bảng size chart</span>'
                                    }
                                </div>
                            </div>
                        </div>
                    `);

                    // Render bảng phân bổ vào modal chi tiết
                    setTimeout(() => {
                        const targetTable = $('#detail-alloc-table');
                        targetTable.empty();
                        const sizes = (p.sizesList && p.sizesList.length > 0) ? p.sizesList : ['Tiêu chuẩn'];

                        let h = `<thead><tr><th>Chi nhánh</th>`;
                        sizes.forEach(s => h += `<th class="text-center">Size ${s}</th>`);
                        h += `<th class="text-center">Tổng kho</th></tr></thead><tbody>`;

                        if (p.branchAllocations && p.branchAllocations.length > 0) {
                            p.branchAllocations.forEach(a => {
                                h += `<tr><td><strong>${a.shopName}</strong></td>`;
                                const sm = a.sizeStocks || {};
                                sizes.forEach(s => {
                                    const q = sm[s] !== undefined ? sm[s] : (sm['Tiêu chuẩn'] || 0);
                                    h += `<td class="text-center font-weight-bold">${q}</td>`;
                                });
                                h += `<td class="text-center font-weight-bold text-primary">${a.totalStock || 0}</td></tr>`;
                            });
                        } else {
                            h += `<tr><td colspan="${sizes.length + 2}" class="text-center text-muted">Chưa có dữ liệu phân bổ kho.</td></tr>`;
                        }
                        h += `</tbody>`;
                        targetTable.html(h);
                    }, 50);

                    $('#btn-edit-from-detail').off('click').on('click', function () {
                        openEditModal(p.id);
                    });

                    $('#productDetailModal').modal('show');
                },
                error: function (xhr) {
                    showToast(xhr.responseJSON?.message || 'Không thể tải chi tiết sản phẩm', 'danger');
                }
            });
        }

        // ============ Xóa sản phẩm ============

        function confirmDelete(id, name) {
            deletingId = id;
            $('#delete-prod-name').text(name + ` (#${id})`);
            $('#deleteProductModal').modal('show');
        }

        function executeDelete() {
            if (!deletingId) return;

            const id = deletingId;
            deletingId = null;
            $('#deleteProductModal').modal('hide');

            // Cập nhật giao diện ngay: làm mờ dòng và trừ tổng, không chờ máy chủ
            const row = $(`#product-table-body tr[data-id="${id}"]`);
            row.css({ opacity: 0.35, pointerEvents: 'none' });

            $.ajax({
                url: `${API_BASE}/${id}`,
                type: 'DELETE',
                headers: getAuthHeaders(),
                success: function (res) {
                    row.fadeOut(150, function () { $(this).remove(); });
                    totalElements = Math.max(0, totalElements - 1);
                    $('#filter-result-count').text(`Tìm thấy ${totalElements} sản phẩm`);
                    const total = parseInt($('#stat-total-products').text().replace(/\D/g, ''), 10);
                    if (!isNaN(total)) $('#stat-total-products').text(Math.max(0, total - 1));
                    showToast(res.message || 'Đã xóa sản phẩm thành công!', 'success');
                    // Đồng bộ lại ngầm (không vòng quay) để bù sản phẩm của trang kế và số liệu chính xác
                    loadStats();
                    loadProducts(true);
                },
                error: function (xhr) {
                    row.css({ opacity: 1, pointerEvents: '' });
                    showToast(xhr.responseJSON?.message || 'Không thể xóa sản phẩm!', 'danger');
                }
            });
        }

        // ============ Toast Notifications ============

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

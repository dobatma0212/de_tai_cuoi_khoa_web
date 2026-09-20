/**
 * promotion.js
 * Quản lý Flash Sale & Voucher phía Client
 */
const PromotionClient = (function () {
    "use strict";

    let cachedFlashSales = null;

    /**
     * Lấy danh sách Flash Sale đang hoạt động
     */
    function getActiveFlashSales() {
        if (cachedFlashSales) {
            return Promise.resolve(cachedFlashSales);
        }
        return fetch('/api/public/promotions/flash-sales')
            .then(res => res.json())
            .then(res => {
                if (res.success && res.data) {
                    cachedFlashSales = res.data;
                    return cachedFlashSales;
                }
                return [];
            })
            .catch(err => {
                console.error("Lỗi lấy Flash Sale:", err);
                return [];
            });
    }

    /**
     * Tính toán giá sau Flash Sale cho sản phẩm
     * @param {number} originalPrice Giá gốc
     * @param {object} flashSale Đối tượng khuyến mãi
     * @returns {number} Giá sau giảm
     */
    function calculateFlashSalePrice(originalPrice, flashSale) {
        if (!flashSale || !originalPrice) return originalPrice;

        if (flashSale.discountType === 'PERCENTAGE') {
            const discount = (originalPrice * flashSale.discountValue) / 100;
            const cappedDiscount = flashSale.maxDiscountAmount
                ? Math.min(discount, flashSale.maxDiscountAmount)
                : discount;
            return Math.max(0, originalPrice - cappedDiscount);
        } else {
            return Math.max(0, originalPrice - flashSale.discountValue);
        }
    }

    /**
     * Định dạng tiền tệ VNĐ
     */
    function formatCurrency(amount) {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
    }

    /**
     * Kiểm tra tính hợp lệ của Voucher trước khi đặt hàng
     */
    function checkVoucher(code, orderTotal) {
        const token = localStorage.getItem('jwt_token');
        if (!token) {
            return Promise.reject(new Error("Vui lòng đăng nhập để sử dụng mã voucher!"));
        }

        return fetch('/api/public/promotions/check-voucher', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify({ code: code, orderTotal: orderTotal })
        }).then(res => res.json());
    }

    /**
     * Áp dụng mã Voucher khi thanh toán (chốt lượt dùng)
     */
    function applyVoucher(code, orderTotal) {
        const token = localStorage.getItem('jwt_token');
        if (!token) {
            return Promise.reject(new Error("Vui lòng đăng nhập để sử dụng mã voucher!"));
        }

        return fetch('/api/public/promotions/apply-voucher', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify({ code: code, orderTotal: orderTotal })
        }).then(res => res.json());
    }

    return {
        getActiveFlashSales: getActiveFlashSales,
        calculateFlashSalePrice: calculateFlashSalePrice,
        formatCurrency: formatCurrency,
        checkVoucher: checkVoucher,
        applyVoucher: applyVoucher
    };
})();

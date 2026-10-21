(function () {
  'use strict';
  const STORAGE_KEY = 'storecart_demo_items_v1';
  const COUPON_KEY = 'storecart_demo_coupon_v1';
  const products = [
    { id: 101, name: 'Áo thun Essential', category: 'Thời trang nam', price: 259000, variant: 'Màu: Trắng · Size: M', image: 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=600&q=80' },
    { id: 102, name: 'Áo khoác Daily', category: 'Áo khoác', price: 649000, variant: 'Màu: Be · Size: L', image: 'https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=600&q=80' },
    { id: 103, name: 'Sneaker Urban', category: 'Giày dép', price: 899000, variant: 'Màu: Trắng · Size: 42', image: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=600&q=80' },
    { id: 104, name: 'Balo Minimal', category: 'Phụ kiện', price: 429000, variant: 'Màu: Đen · Một kích thước', image: 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=600&q=80' }
  ];
  const $ = (id) => document.getElementById(id);
  const money = (n) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(n);
  const safe = (s) => String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  function loadCart() { try { const value = JSON.parse(localStorage.getItem(STORAGE_KEY)); return Array.isArray(value) ? value : seed(); } catch (_) { return seed(); } }
  function seed() { const initial = [{...products[0], quantity: 1}, {...products[2], quantity: 1}]; localStorage.setItem(STORAGE_KEY, JSON.stringify(initial)); return initial; }
  let cart = loadCart(); let coupon = localStorage.getItem(COUPON_KEY) || '';
  function save() { localStorage.setItem(STORAGE_KEY, JSON.stringify(cart)); }
  function notify(message, error) { const el = $('notice'); el.textContent = message; el.className = 'notice show' + (error ? ' error' : ''); }
  function hideNotice() { $('notice').className = 'notice'; }
  function subtotal() { return cart.reduce((sum, item) => sum + item.price * item.quantity, 0); }
  function discountAmount() { return coupon === 'SAVE10' ? Math.round(subtotal() * .1) : coupon === 'WELCOME50' ? Math.min(50000, subtotal()) : 0; }
  function renderProducts() {
    $('productGrid').innerHTML = products.map(p => `<article class="product-card"><img src="${p.image}" alt="${safe(p.name)}" loading="lazy"><div class="product-info"><div class="product-category">${safe(p.category)}</div><div class="product-name">${safe(p.name)}</div><div class="product-bottom"><span class="product-price">${money(p.price)}</span><button class="add-btn" data-add="${p.id}" aria-label="Thêm ${safe(p.name)} vào giỏ">+</button></div></div></article>`).join('');
  }
  function renderCart() {
    const count = cart.reduce((n, item) => n + item.quantity, 0); $('headerCount').textContent = count;
    $('emptyState').classList.toggle('hidden', cart.length !== 0); $('clearCart').disabled = cart.length === 0;
    $('cartItems').innerHTML = cart.map(item => `<article class="cart-item"><img class="item-image" src="${item.image}" alt="${safe(item.name)}"><div><div class="item-category">${safe(item.category)}</div><div class="item-name">${safe(item.name)}</div><div class="item-variant">${safe(item.variant)}</div><div class="item-bottom"><div class="qty" aria-label="Số lượng"><button data-action="dec" data-id="${item.id}" aria-label="Giảm số lượng">−</button><span>${item.quantity}</span><button data-action="inc" data-id="${item.id}" aria-label="Tăng số lượng">+</button></div><button class="remove-btn" data-action="remove" data-id="${item.id}">Xóa</button></div></div><div class="item-price">${money(item.price * item.quantity)}<div class="item-unit">${money(item.price)} / sản phẩm</div></div></article>`).join('');
    const sub = subtotal(), discount = discountAmount(); $('subtotal').textContent = money(sub); $('shipping').textContent = sub >= 700000 || sub === 0 ? 'Miễn phí' : money(30000); $('discount').textContent = '− ' + money(discount); $('total').textContent = money(Math.max(0, sub + (sub && sub < 700000 ? 30000 : 0) - discount)); $('checkoutBtn').disabled = cart.length === 0;
    $('couponInput').value = coupon; $('couponMessage').textContent = coupon ? `Đã áp dụng mã ${coupon}.` : '';
  }
  function addProduct(id) { const p = products.find(item => item.id === Number(id)); if (!p) return; const found = cart.find(item => item.id === p.id); if (found) found.quantity = Math.min(99, found.quantity + 1); else cart.push({...p, quantity: 1}); save(); renderCart(); notify(`Đã thêm “${p.name}” vào giỏ hàng.`); }
  $('productGrid').addEventListener('click', e => { const button = e.target.closest('[data-add]'); if (button) addProduct(button.dataset.add); });
  $('cartItems').addEventListener('click', e => { const b = e.target.closest('[data-action]'); if (!b) return; const id = Number(b.dataset.id), item = cart.find(x => x.id === id); if (!item) return; hideNotice(); if (b.dataset.action === 'inc') item.quantity = Math.min(99, item.quantity + 1); if (b.dataset.action === 'dec') item.quantity = Math.max(1, item.quantity - 1); if (b.dataset.action === 'remove') cart = cart.filter(x => x.id !== id); save(); renderCart(); });
  $('clearCart').addEventListener('click', () => { if (!cart.length) return; cart = []; coupon = ''; localStorage.removeItem(COUPON_KEY); save(); renderCart(); notify('Đã xóa toàn bộ sản phẩm khỏi giỏ hàng.'); });
  $('couponForm').addEventListener('submit', e => { e.preventDefault(); const value = $('couponInput').value.trim().toUpperCase(); if (!value) { coupon = ''; localStorage.removeItem(COUPON_KEY); $('couponMessage').textContent = 'Đã bỏ mã giảm giá.'; renderCart(); return; } if (!['SAVE10', 'WELCOME50'].includes(value)) { coupon = ''; localStorage.removeItem(COUPON_KEY); renderCart(); $('couponMessage').textContent = 'Mã không hợp lệ. Thử SAVE10 hoặc WELCOME50.'; return; } coupon = value; localStorage.setItem(COUPON_KEY, coupon); renderCart(); $('couponMessage').textContent = `Áp dụng thành công: ${coupon}.`; });
  $('checkoutBtn').addEventListener('click', () => notify('Đây là bản demo độc lập: chưa tạo đơn hàng thật. Khi tích hợp, nút này sẽ chuyển sang checkout.html và gọi API backend.', false));
  renderProducts(); renderCart();
})();

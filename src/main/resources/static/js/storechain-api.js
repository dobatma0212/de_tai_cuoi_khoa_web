/**
 * StoreChain – API helper & UI helpers cho trang User
 * Dùng chung: index, product-detail, cart, checkout, profile
 */
(function (window) {
  const API = {
    base: '',
    token() {
      return localStorage.getItem('token') || localStorage.getItem('jwt') || '';
    },
    headers(json = true) {
      const h = {};
      if (json) h['Content-Type'] = 'application/json';
      const t = this.token();
      if (t) h['Authorization'] = 'Bearer ' + t;
      return h;
    },
    async get(url) {
      const r = await fetch(this.base + url, { headers: this.headers(false) });
      return r.json();
    },
    async post(url, body) {
      const r = await fetch(this.base + url, {
        method: 'POST',
        headers: this.headers(),
        body: JSON.stringify(body || {})
      });
      return r.json();
    },
    async patch(url, body) {
      const r = await fetch(this.base + url, {
        method: 'PATCH',
        headers: this.headers(),
        body: body ? JSON.stringify(body) : undefined
      });
      return r.json();
    },
    async del(url) {
      const r = await fetch(this.base + url, {
        method: 'DELETE',
        headers: this.headers(false)
      });
      return r.json();
    },
    isLoggedIn() {
      return !!this.token();
    }
  };

  // ---- Top products (trang chủ) ----
  async function loadTopProducts(type, containerSelector, page = 0, size = 8) {
    const box = document.querySelector(containerSelector);
    if (!box) return;
    box.innerHTML = '<div class="text-center p-4">Đang tải...</div>';
    try {
      const res = await API.get(`/api/public/top/products?type=${encodeURIComponent(type)}&page=${page}&size=${size}`);
      const items = (res.data && res.data.content) || res.data || [];
      if (!items.length) {
        box.innerHTML = '<p class="text-muted text-center">Chưa có sản phẩm.</p>';
        return;
      }
      box.innerHTML = items.map(p => productCard(p)).join('');
    } catch (e) {
      console.error(e);
      box.innerHTML = '<p class="text-danger text-center">Không tải được sản phẩm.</p>';
    }
  }

  function productCard(p) {
    const img = p.imageUrl || p.image || '/img/product/product-1.jpg';
    const price = p.price != null ? Number(p.price).toLocaleString('vi-VN') + '₫' : '';
    const id = p.id || p.productId;
    const name = p.name || p.productName || 'Sản phẩm';
    const slug = p.slug || id;
    return `
      <div class="col-lg-3 col-md-4 col-sm-6 mix">
        <div class="product__item">
          <div class="product__item__pic set-bg" style="background-image:url('${img}')">
            <ul class="product__hover">
              <li><a href="#" class="btn-favorite" data-id="${id}" title="Yêu thích"><span class="icon_heart_alt"></span></a></li>
              <li><a href="/product-detail.html?id=${id}" title="Xem"><span class="icon_search"></span></a></li>
            </ul>
          </div>
          <div class="product__item__text">
            <h6><a href="/product-detail.html?id=${id}">${escapeHtml(name)}</a></h6>
            <div class="rating"><i class="fa fa-star"></i> ${p.avgRating != null ? p.avgRating.toFixed(1) : '—'}</div>
            <div class="product__price">${price}</div>
          </div>
        </div>
      </div>`;
  }

  function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  }

  // ---- Favorites ----
  async function toggleFavorite(productId) {
    if (!API.isLoggedIn()) {
      alert('Vui lòng đăng nhập để yêu thích sản phẩm.');
      location.href = '/login.html';
      return null;
    }
    const res = await API.post(`/api/user/favorites/${productId}`);
    return res;
  }

  // ---- Cart badge ----
  async function refreshCartCount() {
    if (!API.isLoggedIn()) return;
    try {
      const res = await API.get('/api/user/cart/count');
      const n = res.data != null ? res.data : 0;
      document.querySelectorAll('.cart-count, .header__nav__option a span, .offcanvas__nav__option a span').forEach(el => {
        el.textContent = n;
      });
    } catch (_) {}
  }

  // ---- Record view ----
  async function recordView(productId) {
    if (!API.isLoggedIn() || !productId) return;
    try { await API.post(`/api/user/views/${productId}`); } catch (_) {}
  }

  // ---- Bind favorite buttons ----
  function bindFavoriteButtons(root = document) {
    root.addEventListener('click', async (e) => {
      const btn = e.target.closest('.btn-favorite');
      if (!btn) return;
      e.preventDefault();
      const id = btn.dataset.id;
      const res = await toggleFavorite(id);
      if (res && res.success !== false) {
        const fav = res.data && res.data.favorited;
        btn.classList.toggle('active', !!fav);
        btn.title = fav ? 'Bỏ yêu thích' : 'Yêu thích';
      } else if (res && res.message) {
        alert(res.message);
      }
    });
  }

  // ---- Format money ----
  function formatVnd(n) {
    return Number(n || 0).toLocaleString('vi-VN') + '₫';
  }

  window.StoreChain = {
    API,
    loadTopProducts,
    productCard,
    toggleFavorite,
    refreshCartCount,
    recordView,
    bindFavoriteButtons,
    formatVnd
  };

  // Auto init
  document.addEventListener('DOMContentLoaded', () => {
    bindFavoriteButtons();
    refreshCartCount();
  });
})(window);

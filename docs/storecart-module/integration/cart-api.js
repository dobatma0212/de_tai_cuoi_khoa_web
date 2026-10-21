/**
 * StoreChain cart API adapter.
 * Load this file on pages served by the same Spring Boot application.
 * Backend routes are based on UserStoreController: /api/user/cart.
 */
(function (global) {
  'use strict';
  const BASE = '/api/user/cart';
  function token() {
    try {
      const session = JSON.parse(localStorage.getItem('sc_auth') || 'null');
      return (session && session.token) || localStorage.getItem('jwt_token') || '';
    } catch (_) { return localStorage.getItem('jwt_token') || ''; }
  }
  async function request(url, options) {
    const headers = Object.assign({ 'Accept': 'application/json' }, options && options.headers || {});
    const jwt = token();
    if (jwt) headers.Authorization = 'Bearer ' + jwt;
    if (options && options.body) headers['Content-Type'] = 'application/json';
    const response = await fetch(url, Object.assign({}, options || {}, { headers, credentials: 'same-origin' }));
    let payload;
    try { payload = await response.json(); } catch (_) { payload = {}; }
    if (!response.ok || payload.success === false) {
      const error = new Error(payload.message || `Yêu cầu thất bại (${response.status})`);
      error.status = response.status;
      throw error;
    }
    return payload.data;
  }
  const api = {
    get: () => request(BASE),
    count: () => request(BASE + '/count'),
    add: (productId, quantity, size, color) => request(BASE, { method: 'POST', body: JSON.stringify({ productId: Number(productId), quantity: Number(quantity || 1), size: size || '', color: color || '' }) }),
    update: (itemId, quantity) => request(BASE + '/' + encodeURIComponent(itemId), { method: 'PATCH', body: JSON.stringify({ quantity: Number(quantity) }) }),
    remove: (itemId) => request(BASE + '/' + encodeURIComponent(itemId), { method: 'DELETE' }),
    clear: () => request(BASE, { method: 'DELETE' })
  };
  global.StoreChainCartAPI = api;
})(window);

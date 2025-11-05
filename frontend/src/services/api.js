import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Users
export const userAPI = {
  getAll: () => api.get('/users'),
  getById: (id) => api.get(`/users/${id}`),
  getByUsername: (username) => api.get(`/users/username/${username}`),
  create: (user) => api.post('/users', user),
  delete: (id) => api.delete(`/users/${id}`),
  // JSON-Patch methods (RFC 6902)
  patch: (id, operations) => api.patch(`/users/${id}`, operations),
  updateProfile: (id, updates) =>
    api.patch(`/users/${id}`,
      Object.entries(updates).map(([key, value]) =>
        ({ op: 'replace', path: `/${key}`, value })
      )
    ),
};

// Wallets
export const walletAPI = {
  getAll: () => api.get('/wallets'),
  getById: (id) => api.get(`/wallets/${id}`),
  getByUserId: (userId) => api.get(`/wallets/user/${userId}`),
  create: (wallet) => api.post('/wallets', wallet),
  deposit: (userId, amount) => api.post('/wallets/deposit', { userId, amount }),
  withdraw: (userId, amount) => api.post('/wallets/withdraw', { userId, amount }),
};

// Events
export const eventAPI = {
  getAll: () => api.get('/events'),
  getById: (id) => api.get(`/events/${id}`),
  getByState: (state) => api.get(`/events/state/${state}`),
  create: (event) => api.post('/events', event),
  update: (id, event) => api.put(`/events/${id}`, event),
  delete: (id) => api.delete(`/events/${id}`),
  // JSON-Patch methods (RFC 6902)
  patch: (id, operations) => api.patch(`/events/${id}`, operations),
  changeState: (id, newState) => api.patch(`/events/${id}`, [{ op: 'replace', path: '/state', value: newState }]),
};

// Markets
export const marketAPI = {
  getAll: () => api.get('/markets'),
  getById: (id) => api.get(`/markets/${id}`),
  getByEventId: (eventId) => api.get(`/markets/event/${eventId}`),
  getByStatus: (status) => api.get(`/markets/status/${status}`),
  create: (market) => api.post('/markets', market),
  update: (id, market) => api.put(`/markets/${id}`, market),
  delete: (id) => api.delete(`/markets/${id}`),
  settle: (id) => api.post(`/markets/${id}/settle`),
  matchOrders: (id) => api.post(`/markets/${id}/match`),
  // JSON-Patch methods (RFC 6902)
  patch: (id, operations) => api.patch(`/markets/${id}`, operations),
  changeStatus: (id, newStatus) => api.patch(`/markets/${id}`, [{ op: 'replace', path: '/status', value: newStatus }]),
};

// Orders
export const orderAPI = {
  getAll: () => api.get('/orders'),
  getById: (id) => api.get(`/orders/${id}`),
  getByUserId: (userId) => api.get(`/orders/user/${userId}`),
  getByMarketId: (marketId) => api.get(`/orders/market/${marketId}`),
  getOrderBook: (marketId) => api.get(`/orders/market/${marketId}/book`),
  create: (order) => api.post('/orders', order),
  update: (id, order) => api.put(`/orders/${id}`, order),
  cancel: (id) => api.post(`/orders/${id}/cancel`), // Changed from PATCH to POST
  delete: (id) => api.delete(`/orders/${id}`),
  // JSON-Patch methods (RFC 6902) - for future use
  patch: (id, operations) => api.patch(`/orders/${id}`, operations),
};

// Positions
export const positionAPI = {
  getAll: () => api.get('/positions'),
  getById: (id) => api.get(`/positions/${id}`),
  getByUserId: (userId) => api.get(`/positions/user/${userId}`),
  getByMarketId: (marketId) => api.get(`/positions/market/${marketId}`),
};

// Transactions
export const transactionAPI = {
  getAll: () => api.get('/transactions'),
  getById: (id) => api.get(`/transactions/${id}`),
  getByUserId: (userId) => api.get(`/transactions/user/${userId}`),
  getByType: (type) => api.get(`/transactions/type/${type}`),
};

// Comments
export const commentAPI = {
  getByPostId: (postId) => api.get(`/api/v1/comments/post/${postId}`),
  create: (comment) => api.post('/api/v1/comments', comment),
  delete: (commentId) => api.delete(`/api/v1/comments/${commentId}`),
  // JSON-Patch methods (RFC 6902)
  patch: (id, operations) => api.patch(`/api/v1/comments/${id}`, operations),
  editContent: (id, newContent) =>
    api.patch(`/api/v1/comments/${id}`, [{ op: 'replace', path: '/content', value: newContent }]),
};

export default api;

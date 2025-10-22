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
  changeStatus: (id, status) => api.patch(`/markets/${id}/status/${status}`),
  settle: (id) => api.post(`/markets/${id}/settle`),
  matchOrders: (id) => api.post(`/markets/${id}/match`),
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
  cancel: (id) => api.patch(`/orders/${id}/cancel`),
  delete: (id) => api.delete(`/orders/${id}`),
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

export default api;

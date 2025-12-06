import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

// Token storage (in-memory for security)
let accessToken = null;

export const setAccessToken = (token) => {
  accessToken = token;
};

export const getAccessToken = () => accessToken;

export const clearAccessToken = () => {
  accessToken = null;
};

// Request interceptor - add Authorization header
api.interceptors.request.use(
  (config) => {
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle 401 and token refresh
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry && originalRequest.url !== '/auth/login') {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(token => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return api(originalRequest);
        }).catch(err => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const response = await api.post('/auth/refresh');
        const authHeader = response.headers['authorization'];
        const newToken = authHeader?.replace('Bearer ', '');

        if (newToken) {
          setAccessToken(newToken);
          processQueue(null, newToken);
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
          return api(originalRequest);
        }
      } catch (refreshError) {
        processQueue(refreshError, null);
        clearAccessToken();
        window.dispatchEvent(new CustomEvent('auth:logout'));
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

// Auth API
export const authAPI = {
  login: (email, password) => api.post('/auth/login', { email, password }),
  register: (name, email, password) => api.post('/auth/register', { name, email, password }),
  refresh: () => api.post('/auth/refresh'),
  logout: () => api.post('/auth/logout'),
};

// Users
export const userAPI = {
  getAll: () => api.get('/users'),
  getById: (id) => api.get(`/users/${id}`),
  getByUsername: (username) => api.get(`/users/username/${username}`),
  create: (user) => api.post('/users', user),
  delete: (id) => api.delete(`/users/${id}`),
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
  cancel: (id) => api.post(`/orders/${id}/cancel`),
  delete: (id) => api.delete(`/orders/${id}`),
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
  patch: (id, operations) => api.patch(`/api/v1/comments/${id}`, operations),
  editContent: (id, newContent) =>
    api.patch(`/api/v1/comments/${id}`, [{ op: 'replace', path: '/content', value: newContent }]),
};

export default api;

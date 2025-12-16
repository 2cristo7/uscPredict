import axios from 'axios';

const API_BASE_URL = '/api';

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

    if (error.response?.status === 401 && !originalRequest._retry && originalRequest.url !== '/v1/auth/login') {
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
        const response = await api.post('/v1/auth/refresh');
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
  v1: {
    login: (email, password) => api.post('/v1/auth/login', { email, password }),
    register: (name, email, password) => api.post('/v1/auth/register', { name, email, password }),
    refresh: () => api.post('/v1/auth/refresh'),
    logout: () => api.post('/v1/auth/logout'),
  }
};

// Users
export const userAPI = {
  v1: {
    getAll: () => api.get('/v1/users'),
    getById: (id) => api.get(`/v1/users/${id}`),
    getByUsername: (username) => api.get(`/v1/users/username/${username}`),
    create: (user) => api.post('/v1/users', user),
    delete: (id) => api.delete(`/v1/users/${id}`),
    patch: (id, operations) => api.patch(`/v1/users/${id}`, operations),
    updateProfile: (id, updates) =>
      api.patch(`/v1/users/${id}`,
        Object.entries(updates).map(([key, value]) =>
          ({ op: 'replace', path: `/${key}`, value })
        )
      ),
  }
};

// Wallets
export const walletAPI = {
  v1: {
    getAll: () => api.get('/v1/wallets'),
    getById: (id) => api.get(`/v1/wallets/${id}`),
    getByUserId: (userId) => api.get(`/v1/wallets/user/${userId}`),
    create: (wallet) => api.post('/v1/wallets', wallet),
    deposit: (userId, amount) => api.post('/v1/wallets/deposit', { userId, amount }),
    withdraw: (userId, amount) => api.post('/v1/wallets/withdraw', { userId, amount }),
  }
};

// Events
export const eventAPI = {
  v1: {
    getAll: () => api.get('/v1/events'),
    getById: (id) => api.get(`/v1/events/${id}`),
    getByState: (state) => api.get(`/v1/events/state/${state}`),
    create: (event) => api.post('/v1/events', event),
    update: (id, event) => api.put(`/v1/events/${id}`, event),
    delete: (id) => api.delete(`/v1/events/${id}`),
    patch: (id, operations) => api.patch(`/v1/events/${id}`, operations),
    changeState: (id, newState) => api.patch(`/v1/events/${id}`, [{ op: 'replace', path: '/state', value: newState }]),
  }
};

// Markets
export const marketAPI = {
  v1: {
    getAll: () => api.get('/v1/markets'),
    getById: (id) => api.get(`/v1/markets/${id}`),
    getByEventId: (eventId) => api.get(`/v1/markets/event/${eventId}`),
    getByStatus: (status) => api.get(`/v1/markets/status/${status}`),
    getPriceHistory: (id, bucketMinutes = 60) => api.get(`/v1/markets/${id}/price-history`, { params: { bucketMinutes } }),
    create: (market) => api.post('/v1/markets', market),
    update: (id, market) => api.put(`/v1/markets/${id}`, market),
    delete: (id) => api.delete(`/v1/markets/${id}`),
    settle: (id) => api.post(`/v1/markets/${id}/settle`),
    matchOrders: (id) => api.post(`/v1/markets/${id}/match`),
    patch: (id, operations) => api.patch(`/v1/markets/${id}`, operations),
    changeStatus: (id, newStatus) => api.patch(`/v1/markets/${id}`, [{ op: 'replace', path: '/status', value: newStatus }]),
  }
};

// Orders
export const orderAPI = {
  v1: {
    getAll: () => api.get('/v1/orders'),
    getById: (id) => api.get(`/v1/orders/${id}`),
    getByUserId: (userId) => api.get(`/v1/orders/user/${userId}`),
    getByMarketId: (marketId) => api.get(`/v1/orders/market/${marketId}`),
    getOrderBook: (marketId) => api.get(`/v1/orders/market/${marketId}/book`),
    create: (order) => api.post('/v1/orders', order),
    update: (id, order) => api.put(`/v1/orders/${id}`, order),
    cancel: (id) => api.post(`/v1/orders/${id}/cancel`),
    delete: (id) => api.delete(`/v1/orders/${id}`),
    patch: (id, operations) => api.patch(`/v1/orders/${id}`, operations),
  }
};

// Positions
export const positionAPI = {
  v1: {
    getAll: () => api.get('/v1/positions'),
    getById: (id) => api.get(`/v1/positions/${id}`),
    getByUserId: (userId) => api.get(`/v1/positions/user/${userId}`),
    getByMarketId: (marketId) => api.get(`/v1/positions/market/${marketId}`),
  }
};

// Transactions
export const transactionAPI = {
  v1: {
    getAll: () => api.get('/v1/transactions'),
    getById: (id) => api.get(`/v1/transactions/${id}`),
    getByUserId: (userId) => api.get(`/v1/transactions/user/${userId}`),
    getByType: (type) => api.get(`/v1/transactions/type/${type}`),
  }
};

// Comments
export const commentAPI = {
  v1: {
    getByPostId: (postId) => api.get(`/v1/comments/post/${postId}`),
    getByEventId: (eventId) => api.get(`/v1/comments/post/${eventId}`),
    create: (comment) => api.post('/v1/comments', comment),
    delete: (commentId) => api.delete(`/v1/comments/${commentId}`),
    patch: (id, operations) => api.patch(`/v1/comments/${id}`, operations),
    editContent: (id, newContent) =>
      api.patch(`/v1/comments/${id}`, [{ op: 'replace', path: '/content', value: newContent }]),
  }
};

export default api;

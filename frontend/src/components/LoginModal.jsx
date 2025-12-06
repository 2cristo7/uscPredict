import { useState } from 'react';
import { useAuth } from '../context/AuthContext';

export default function LoginModal({ onClose }) {
  const { login, register } = useAuth();

  // Tab state
  const [activeTab, setActiveTab] = useState('login');

  // Form fields
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  // UI state
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);

  // Client-side validation
  const validateForm = () => {
    const newErrors = {};

    if (activeTab === 'register') {
      // Name validation
      if (!name.trim()) {
        newErrors.name = 'Name is required';
      } else if (name.length < 2 || name.length > 100) {
        newErrors.name = 'Name must be between 2 and 100 characters';
      }

      // Password confirmation
      if (password !== confirmPassword) {
        newErrors.confirmPassword = 'Passwords do not match';
      }

      // Password complexity
      if (password.length < 8) {
        newErrors.password = 'Password must be at least 8 characters';
      } else if (!/^(?=.*[A-Za-z])(?=.*\d).+$/.test(password)) {
        newErrors.password = 'Password must contain at least one letter and one number';
      }
    }

    // Email validation
    if (!email.trim()) {
      newErrors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      newErrors.email = 'Please enter a valid email address';
    }

    // Password required
    if (!password) {
      newErrors.password = newErrors.password || 'Password is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setErrors({});
    setLoading(true);

    try {
      if (activeTab === 'login') {
        await login(email, password);
      } else {
        await register(name, email, password);
      }
      onClose?.();
    } catch (err) {
      const status = err.response?.status;
      const responseErrors = err.response?.data?.errors;

      if (responseErrors && typeof responseErrors === 'object') {
        // Include HTTP status in field errors too (didactic)
        const errorsWithStatus = { ...responseErrors };
        if (status) {
          switch (status) {
            case 400:
              errorsWithStatus.general = `Error ${status}: Bad Request - Validation failed`;
              break;
            case 401:
              errorsWithStatus.general = `Error ${status}: Unauthorized - Invalid credentials`;
              break;
            case 409:
              errorsWithStatus.general = `Error ${status}: Conflict - Email already registered`;
              break;
            default:
              errorsWithStatus.general = `Error ${status}: Request failed`;
          }
        }
        setErrors(errorsWithStatus);
      } else {
        // Build didactic error message with HTTP code
        let errorMessage = '';

        if (status) {
          errorMessage = `Error ${status}: `;

          switch (status) {
            case 400:
              errorMessage += 'Bad Request - The data sent is invalid';
              break;
            case 401:
              errorMessage += 'Unauthorized - Invalid credentials';
              break;
            case 403:
              errorMessage += 'Forbidden - You do not have permission';
              break;
            case 409:
              errorMessage += 'Conflict - Email already registered';
              break;
            case 500:
              errorMessage += 'Internal Server Error - Something went wrong on the server';
              break;
            default:
              errorMessage += err.response?.data?.message || 'Unknown error';
          }
        } else {
          errorMessage = 'Network error - Could not connect to server';
        }

        setErrors({ general: errorMessage });
      }
    } finally {
      setLoading(false);
    }
  };

  // Reset form when switching tabs
  const switchTab = (tab) => {
    setActiveTab(tab);
    setErrors({});
    setName('');
    setEmail('');
    setPassword('');
    setConfirmPassword('');
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl p-8 w-full max-w-md mx-4">

        {/* Tab Navigation */}
        <div className="flex border-b border-gray-200 mb-6">
          <button
            type="button"
            onClick={() => switchTab('login')}
            className={`flex-1 py-2 text-center font-medium ${
              activeTab === 'login'
                ? 'text-blue-600 border-b-2 border-blue-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            Sign In
          </button>
          <button
            type="button"
            onClick={() => switchTab('register')}
            className={`flex-1 py-2 text-center font-medium ${
              activeTab === 'register'
                ? 'text-blue-600 border-b-2 border-blue-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            Register
          </button>
        </div>

        <h2 className="text-2xl font-bold text-gray-900 mb-6 text-center">
          {activeTab === 'login' ? 'Sign in to USC Predict' : 'Create your account'}
        </h2>

        {/* General Error Message */}
        {errors.general && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
            {errors.general}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">

          {/* Name field (register only) */}
          {activeTab === 'register' && (
            <div>
              <label className="block text-gray-700 text-sm font-bold mb-2">
                Name
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className={`shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  errors.name ? 'border-red-500' : ''
                }`}
                placeholder="Your full name"
              />
              {errors.name && (
                <p className="text-red-500 text-xs mt-1">{errors.name}</p>
              )}
            </div>
          )}

          {/* Email field */}
          <div>
            <label className="block text-gray-700 text-sm font-bold mb-2">
              Email
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className={`shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                errors.email ? 'border-red-500' : ''
              }`}
              placeholder="you@example.com"
              required
            />
            {errors.email && (
              <p className="text-red-500 text-xs mt-1">{errors.email}</p>
            )}
          </div>

          {/* Password field */}
          <div>
            <label className="block text-gray-700 text-sm font-bold mb-2">
              Password
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={`shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                errors.password ? 'border-red-500' : ''
              }`}
              placeholder="********"
              required
            />
            {errors.password && (
              <p className="text-red-500 text-xs mt-1">{errors.password}</p>
            )}
            {activeTab === 'register' && !errors.password && (
              <p className="text-gray-500 text-xs mt-1">
                At least 8 characters with one letter and one number
              </p>
            )}
          </div>

          {/* Confirm Password field (register only) */}
          {activeTab === 'register' && (
            <div>
              <label className="block text-gray-700 text-sm font-bold mb-2">
                Confirm Password
              </label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className={`shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  errors.confirmPassword ? 'border-red-500' : ''
                }`}
                placeholder="********"
              />
              {errors.confirmPassword && (
                <p className="text-red-500 text-xs mt-1">{errors.confirmPassword}</p>
              )}
            </div>
          )}

          {/* Submit button */}
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
          >
            {loading
              ? (activeTab === 'login' ? 'Signing in...' : 'Creating account...')
              : (activeTab === 'login' ? 'Sign In' : 'Create Account')
            }
          </button>
        </form>
      </div>
    </div>
  );
}

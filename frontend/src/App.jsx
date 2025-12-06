import { useState, useMemo } from 'react';
import { useAuth } from './context/AuthContext';
import LoginModal from './components/LoginModal';
import Users from './components/Users';
import Wallets from './components/Wallets';
import Markets from './components/Markets';
import Orders from './components/Orders';
import Events from './components/Events';

function App() {
  const { user, isAuthenticated, isAdmin, loading, logout } = useAuth();
  const [activeTab, setActiveTab] = useState('events');

  // Define tabs with role requirements
  const allTabs = useMemo(() => [
    { id: 'events', name: 'Events', component: Events, adminOnly: false },
    { id: 'markets', name: 'Markets', component: Markets, adminOnly: false },
    { id: 'orders', name: 'Orders', component: Orders, adminOnly: false },
    { id: 'wallets', name: 'Wallets', component: Wallets, adminOnly: true },
    { id: 'users', name: 'Users', component: Users, adminOnly: true },
  ], []);

  // Filter tabs based on role
  const visibleTabs = useMemo(() => {
    return allTabs.filter(tab => !tab.adminOnly || isAdmin);
  }, [allTabs, isAdmin]);

  // Reset to first available tab if current is hidden
  const currentTab = visibleTabs.find(t => t.id === activeTab) || visibleTabs[0];
  const ActiveComponent = currentTab?.component || Events;

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center">
        <div className="text-xl text-gray-600">Loading...</div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <LoginModal />;
  }

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Header */}
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 py-6 flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">
              USC Predict - {isAdmin ? 'Admin' : 'User'} Dashboard
            </h1>
            <p className="text-sm text-gray-600 mt-1">
              Welcome, {user?.name}
            </p>
          </div>
          <div className="flex items-center gap-4">
            <span className={`px-2 py-1 rounded text-xs ${
              isAdmin ? 'bg-purple-100 text-purple-800' : 'bg-blue-100 text-blue-800'
            }`}>
              {user?.role}
            </span>
            <button
              onClick={logout}
              className="bg-red-500 hover:bg-red-700 text-white font-bold py-2 px-4 rounded"
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      {/* Navigation - Role-based tabs */}
      <nav className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4">
          <div className="flex space-x-8">
            {visibleTabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
                  activeTab === tab.id
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                {tab.name}
                {tab.adminOnly && (
                  <span className="ml-1 text-xs text-purple-600">(Admin)</span>
                )}
              </button>
            ))}
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 py-8">
        <ActiveComponent />
      </main>

      {/* Footer */}
      <footer className="bg-white mt-12 border-t">
        <div className="max-w-7xl mx-auto px-4 py-6 text-center text-sm text-gray-500">
          USC Predict - Prediction Market Platform
        </div>
      </footer>
    </div>
  );
}

export default App;

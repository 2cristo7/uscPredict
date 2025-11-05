import { useState } from 'react';
import Users from './components/Users';
import Wallets from './components/Wallets';
import Markets from './components/Markets';
import Orders from './components/Orders';
import Events from './components/Events';

function App() {
  const [activeTab, setActiveTab] = useState('users');

  const tabs = [
    { id: 'users', name: 'Users', component: Users },
    { id: 'wallets', name: 'Wallets', component: Wallets },
    { id: 'events', name: 'Eventos', component: Events },
    { id: 'markets', name: 'Markets & Events', component: Markets },
    { id: 'orders', name: 'Orders & Positions', component: Orders },
  ];

  const ActiveComponent = tabs.find(tab => tab.id === activeTab)?.component || Users;

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Header */}
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 py-6">
          <h1 className="text-3xl font-bold text-gray-900">
            USC Predict - Admin Dashboard
          </h1>
          <p className="text-sm text-gray-600 mt-1">
            Prediction Market Management System
          </p>
        </div>
      </header>

      {/* Navigation */}
      <nav className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4">
          <div className="flex space-x-8">
            {tabs.map((tab) => (
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
          USC Predict © 2025 - Prediction Market Platform
        </div>
      </footer>
    </div>
  );
}

export default App;

import { useState, useEffect } from 'react';
import { walletAPI, userAPI } from '../services/api';

export default function Wallets() {
  const [wallets, setWallets] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedUserId, setSelectedUserId] = useState('');
  const [amount, setAmount] = useState('');
  const [operation, setOperation] = useState('deposit');

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [walletsRes, usersRes] = await Promise.all([
        walletAPI.getAll(),
        userAPI.getAll()
      ]);
      setWallets(walletsRes.data);
      setUsers(usersRes.data);
      setError(null);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleTransaction = async (e) => {
    e.preventDefault();
    if (!selectedUserId || !amount) return;

    try {
      setError(null);
      if (operation === 'deposit') {
        await walletAPI.deposit(selectedUserId, parseFloat(amount));
      } else {
        await walletAPI.withdraw(selectedUserId, parseFloat(amount));
      }
      setAmount('');
      setSelectedUserId('');
      await loadData();
    } catch (err) {
      // Handle error message properly - extract message from error object
      let errorMessage = 'An error occurred';
      if (err.response?.data) {
        if (typeof err.response.data === 'string') {
          errorMessage = err.response.data;
        } else if (err.response.data.message) {
          errorMessage = err.response.data.message;
        } else if (err.response.data.error) {
          errorMessage = err.response.data.error;
        }
      } else if (err.message) {
        errorMessage = err.message;
      }
      setError(errorMessage);
    }
  };

  const getUserName = (userId) => {
    const user = users.find(u => u.uuid === userId);
    return user ? user.name : 'Unknown';
  };

  const getTotalBalance = (wallet) => {
    return (parseFloat(wallet.balance) + parseFloat(wallet.lockedBalance)).toFixed(2);
  };

  if (loading) return <div className="text-center py-8">Loading...</div>;

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold">Wallets</h2>

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
          Error: {error}
        </div>
      )}

      <div className="bg-white shadow-md rounded px-8 pt-6 pb-8">
        <h3 className="text-xl font-semibold mb-4">Deposit / Withdraw</h3>
        <form onSubmit={handleTransaction} className="space-y-4">
          <div>
            <label className="block text-gray-700 text-sm font-bold mb-2">
              User
            </label>
            <select
              value={selectedUserId}
              onChange={(e) => setSelectedUserId(e.target.value)}
              className="shadow border rounded w-full py-2 px-3 text-gray-700"
              required
            >
              <option value="">Select a user...</option>
              {users.map((user) => (
                <option key={user.uuid} value={user.uuid}>
                  {user.name} ({user.email})
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-gray-700 text-sm font-bold mb-2">
              Operation
            </label>
            <div className="flex space-x-4">
              <label className="inline-flex items-center">
                <input
                  type="radio"
                  value="deposit"
                  checked={operation === 'deposit'}
                  onChange={(e) => setOperation(e.target.value)}
                  className="form-radio"
                />
                <span className="ml-2">Deposit</span>
              </label>
              <label className="inline-flex items-center">
                <input
                  type="radio"
                  value="withdraw"
                  checked={operation === 'withdraw'}
                  onChange={(e) => setOperation(e.target.value)}
                  className="form-radio"
                />
                <span className="ml-2">Withdraw</span>
              </label>
            </div>
          </div>
          <div>
            <label className="block text-gray-700 text-sm font-bold mb-2">
              Amount ($)
            </label>
            <input
              type="number"
              step="0.01"
              min="0"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700"
              required
            />
          </div>
          <button
            type="submit"
            className={`font-bold py-2 px-4 rounded ${
              operation === 'deposit'
                ? 'bg-green-500 hover:bg-green-700'
                : 'bg-red-500 hover:bg-red-700'
            } text-white`}
          >
            {operation === 'deposit' ? 'Deposit' : 'Withdraw'}
          </button>
        </form>
      </div>

      <div className="bg-white shadow-md rounded overflow-hidden">
        <table className="min-w-full">
          <thead className="bg-gray-100">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">User</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Available Balance</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Locked Balance</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Total Balance</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {wallets.map((wallet) => (
              <tr key={wallet.uuid}>
                <td className="px-6 py-4 whitespace-nowrap font-medium">
                  {getUserName(wallet.userId)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-green-600">
                  ${parseFloat(wallet.balance).toFixed(2)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-orange-600">
                  ${parseFloat(wallet.lockedBalance).toFixed(2)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap font-bold">
                  ${getTotalBalance(wallet)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

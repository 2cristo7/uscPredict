import { useState, useEffect } from 'react';
import { orderAPI, userAPI, marketAPI, positionAPI } from '../services/api';

export default function Orders() {
  const [orders, setOrders] = useState([]);
  const [positions, setPositions] = useState([]);
  const [users, setUsers] = useState([]);
  const [markets, setMarkets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showCreateForm, setShowCreateForm] = useState(false);

  const [newOrder, setNewOrder] = useState({
    userId: '',
    marketId: '',
    side: 'BUY',
    price: '',
    quantity: ''
  });

  const [editingOrder, setEditingOrder] = useState(null);
  const [editForm, setEditForm] = useState({
    price: '',
    quantity: ''
  });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [ordersRes, usersRes, marketsRes, positionsRes] = await Promise.all([
        orderAPI.getAll(),
        userAPI.getAll(),
        marketAPI.getAll(),
        positionAPI.getAll()
      ]);
      setOrders(ordersRes.data);
      setUsers(usersRes.data);
      setMarkets(marketsRes.data);
      setPositions(positionsRes.data);
      setError(null);
    } catch (err) {
      // Handle error message properly
      let errorMessage = 'An error occurred while loading data';
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
    } finally {
      setLoading(false);
    }
  };

  const handleCreateOrder = async (e) => {
    e.preventDefault();
    try {
      setError(null);
      const orderData = {
        ...newOrder,
        price: parseFloat(newOrder.price),
        quantity: parseInt(newOrder.quantity),
        filledQuantity: 0
      };
      await orderAPI.create(orderData);
      setNewOrder({ userId: '', marketId: '', side: 'BUY', price: '', quantity: '' });
      setShowCreateForm(false);
      await loadData();
    } catch (err) {
      // Handle error message properly
      let errorMessage = 'An error occurred while creating order';
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

  const handleCancelOrder = async (orderId) => {
    if (window.confirm('Are you sure you want to cancel this order?')) {
      try {
        setError(null);
        await orderAPI.cancel(orderId);
        await loadData();
      } catch (err) {
        // Handle error message properly
        let errorMessage = 'An error occurred while canceling order';
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
    }
  };

  const handleEditOrder = (order) => {
    setEditingOrder(order.uuid);
    setEditForm({
      price: order.price,
      quantity: order.quantity
    });
  };

  const handleSaveEdit = async () => {
    const price = parseFloat(editForm.price);
    const quantity = parseInt(editForm.quantity);

    if (isNaN(price) || price <= 0 || price > 1) {
      alert('Price must be between 0 and 1');
      return;
    }

    if (isNaN(quantity) || quantity < 1) {
      alert('Quantity must be at least 1');
      return;
    }

    try {
      setError(null);
      await orderAPI.patch(editingOrder, [
        { op: 'replace', path: '/price', value: price },
        { op: 'replace', path: '/quantity', value: quantity }
      ]);
      setEditingOrder(null);
      setEditForm({ price: '', quantity: '' });
      await loadData();
    } catch (err) {
      let errorMessage = 'An error occurred while updating order';
      if (err.response?.data) {
        if (typeof err.response.data === 'string') {
          errorMessage = err.response.data;
        } else if (err.response.data.message) {
          errorMessage = err.response.data.message;
        }
      } else if (err.message) {
        errorMessage = err.message;
      }
      setError(errorMessage);
    }
  };

  const handleCancelEdit = () => {
    setEditingOrder(null);
    setEditForm({ price: '', quantity: '' });
  };

  const getUserName = (userId) => {
    const user = users.find(u => u.uuid === userId);
    return user ? user.name : 'Unknown';
  };

  const getMarketInfo = (marketId) => {
    const market = markets.find(m => m.uuid === marketId);
    return market ? market.outcome : 'Unknown';
  };

  if (loading) return <div className="text-center py-8">Loading...</div>;

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold">Orders & Positions</h2>
        <button
          onClick={() => setShowCreateForm(!showCreateForm)}
          className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
        >
          {showCreateForm ? 'Cancel' : 'Place Order'}
        </button>
      </div>

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
          Error: {typeof error === 'string' ? error : JSON.stringify(error)}
        </div>
      )}

      {showCreateForm && (
        <form onSubmit={handleCreateOrder} className="bg-white shadow-md rounded px-8 pt-6 pb-8">
          <h3 className="text-xl font-semibold mb-4">Place New Order</h3>
          <div className="grid grid-cols-2 gap-4 mb-4">
            <div>
              <label className="block text-gray-700 text-sm font-bold mb-2">User</label>
              <select
                value={newOrder.userId}
                onChange={(e) => setNewOrder({ ...newOrder, userId: e.target.value })}
                className="shadow border rounded w-full py-2 px-3 text-gray-700"
                required
              >
                <option value="">Select user...</option>
                {users.map((user) => (
                  <option key={user.uuid} value={user.uuid}>
                    {user.name}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-gray-700 text-sm font-bold mb-2">Market</label>
              <select
                value={newOrder.marketId}
                onChange={(e) => setNewOrder({ ...newOrder, marketId: e.target.value })}
                className="shadow border rounded w-full py-2 px-3 text-gray-700"
                required
              >
                <option value="">Select market...</option>
                {markets.filter(m => m.status === 'ACTIVE').map((market) => (
                  <option key={market.uuid} value={market.uuid}>
                    {market.outcome}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="grid grid-cols-3 gap-4 mb-4">
            <div>
              <label className="block text-gray-700 text-sm font-bold mb-2">Side</label>
              <select
                value={newOrder.side}
                onChange={(e) => setNewOrder({ ...newOrder, side: e.target.value })}
                className="shadow border rounded w-full py-2 px-3 text-gray-700"
              >
                <option value="BUY">BUY (YES)</option>
                <option value="SELL">SELL (NO)</option>
              </select>
            </div>
            <div>
              <label className="block text-gray-700 text-sm font-bold mb-2">
                Price ($) <span className="text-xs text-gray-500">0-1</span>
              </label>
              <input
                type="number"
                step="0.01"
                min="0"
                max="1"
                value={newOrder.price}
                onChange={(e) => setNewOrder({ ...newOrder, price: e.target.value })}
                className="shadow border rounded w-full py-2 px-3 text-gray-700"
                required
              />
            </div>
            <div>
              <label className="block text-gray-700 text-sm font-bold mb-2">Quantity</label>
              <input
                type="number"
                min="1"
                value={newOrder.quantity}
                onChange={(e) => setNewOrder({ ...newOrder, quantity: e.target.value })}
                className="shadow border rounded w-full py-2 px-3 text-gray-700"
                required
              />
            </div>
          </div>
          <div className="bg-blue-50 border border-blue-200 rounded p-3 mb-4 text-sm">
            <p><strong>Note:</strong> BUY = betting on YES, SELL = betting on NO</p>
            <p className="text-xs text-gray-600 mt-1">
              Cost: {newOrder.side === 'BUY'
                ? `${newOrder.price} × ${newOrder.quantity || 0} = $${(parseFloat(newOrder.price || 0) * parseInt(newOrder.quantity || 0)).toFixed(2)}`
                : `(1 - ${newOrder.price}) × ${newOrder.quantity || 0} = $${((1 - parseFloat(newOrder.price || 0)) * parseInt(newOrder.quantity || 0)).toFixed(2)}`
              }
            </p>
          </div>
          <button
            type="submit"
            className="bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded"
          >
            Place Order
          </button>
        </form>
      )}

      <div className="bg-white shadow-md rounded overflow-hidden">
        <h3 className="text-xl font-semibold px-6 py-4 bg-gray-50">Active Orders</h3>
        <table className="min-w-full">
          <thead className="bg-gray-100">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">User</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Market</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Side</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Price</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Quantity</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Filled</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">State</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {orders.map((order) => (
              <tr key={order.uuid}>
                <td className="px-6 py-4 whitespace-nowrap">{getUserName(order.userId)}</td>
                <td className="px-6 py-4 whitespace-nowrap">{getMarketInfo(order.marketId)}</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 py-1 rounded text-xs ${
                    order.side === 'BUY' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                  }`}>
                    {order.side}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  {editingOrder === order.uuid ? (
                    <input
                      type="number"
                      step="0.01"
                      min="0"
                      max="1"
                      value={editForm.price}
                      onChange={(e) => setEditForm({ ...editForm, price: e.target.value })}
                      className="border rounded px-2 py-1 text-sm w-20"
                    />
                  ) : (
                    `$${parseFloat(order.price).toFixed(2)}`
                  )}
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  {editingOrder === order.uuid ? (
                    <input
                      type="number"
                      min="1"
                      value={editForm.quantity}
                      onChange={(e) => setEditForm({ ...editForm, quantity: e.target.value })}
                      className="border rounded px-2 py-1 text-sm w-20"
                    />
                  ) : (
                    order.quantity
                  )}
                </td>
                <td className="px-6 py-4 whitespace-nowrap">{order.filledQuantity}</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 py-1 rounded text-xs ${
                    order.state === 'PENDING' ? 'bg-yellow-100 text-yellow-800' :
                    order.state === 'FILLED' ? 'bg-green-100 text-green-800' :
                    order.state === 'PARTIALLY_FILLED' ? 'bg-blue-100 text-blue-800' :
                    'bg-gray-100 text-gray-800'
                  }`}>
                    {order.state}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm space-x-2">
                  {editingOrder === order.uuid ? (
                    <>
                      <button
                        onClick={handleSaveEdit}
                        className="text-green-600 hover:text-green-900 font-semibold"
                      >
                        Save
                      </button>
                      <button
                        onClick={handleCancelEdit}
                        className="text-gray-600 hover:text-gray-900"
                      >
                        Cancel
                      </button>
                    </>
                  ) : (
                    <>
                      {(order.state === 'PENDING' || order.state === 'PARTIALLY_FILLED') && (
                        <>
                          <button
                            onClick={() => handleEditOrder(order)}
                            className="text-blue-600 hover:text-blue-900"
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => handleCancelOrder(order.uuid)}
                            className="text-red-600 hover:text-red-900"
                          >
                            Cancel
                          </button>
                        </>
                      )}
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="bg-white shadow-md rounded overflow-hidden">
        <h3 className="text-xl font-semibold px-6 py-4 bg-gray-50">Positions</h3>
        <table className="min-w-full">
          <thead className="bg-gray-100">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">User</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Market</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">YES Shares</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">NO Shares</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Total Shares</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Net Exposure</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {positions.map((position) => {
              const netExposure = position.yesShares - position.noShares;
              return (
                <tr key={position.uuid}>
                  <td className="px-6 py-4 whitespace-nowrap">{getUserName(position.userId)}</td>
                  <td className="px-6 py-4 whitespace-nowrap">{getMarketInfo(position.marketId)}</td>
                  <td className="px-6 py-4 whitespace-nowrap text-green-600 font-semibold">
                    {position.yesShares}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-red-600 font-semibold">
                    {position.noShares}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {position.yesShares + position.noShares}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`font-semibold ${
                      netExposure > 0 ? 'text-green-600' :
                      netExposure < 0 ? 'text-red-600' :
                      'text-gray-600'
                    }`}>
                      {netExposure > 0 ? '+' : ''}{netExposure}
                    </span>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

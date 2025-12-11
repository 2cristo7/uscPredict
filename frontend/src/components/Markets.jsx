import { useState, useEffect, useCallback } from 'react';
import { marketAPI, eventAPI } from '../services/api';

export default function Markets() {
  const [markets, setMarkets] = useState([]);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showCreateEventForm, setShowCreateEventForm] = useState(false);
  const [showCreateMarketForm, setShowCreateMarketForm] = useState(false);

  const [newEvent, setNewEvent] = useState({
    title: '',
    description: '',
    state: 'OPEN'
  });

  const [newMarket, setNewMarket] = useState({
    eventId: '',
    outcome: '',
    status: 'ACTIVE'
  });

  const extractErrorMessage = (err) => {
    if (err.response?.data) {
      if (typeof err.response.data === 'string') {
        return err.response.data;
      } else if (err.response.data.message) {
        return err.response.data.message;
      } else if (err.response.data.error) {
        return err.response.data.error;
      }
    }
    return err.message || 'An error occurred';
  };

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [marketsRes, eventsRes] = await Promise.all([
        marketAPI.v1.getAll(),
        eventAPI.v1.getAll()
      ]);
      setMarkets(marketsRes.data);
      setEvents(eventsRes.data);
      setError(null);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleCreateEvent = async (e) => {
    e.preventDefault();
    try {
      setError(null);
      await eventAPI.v1.create(newEvent);
      setNewEvent({ title: '', description: '', state: 'OPEN' });
      setShowCreateEventForm(false);
      await loadData();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const handleCreateMarket = async (e) => {
    e.preventDefault();
    try {
      setError(null);
      await marketAPI.v1.create(newMarket);
      setNewMarket({ eventId: '', outcome: '', status: 'ACTIVE' });
      setShowCreateMarketForm(false);
      await loadData();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const handleMatchOrders = async (marketId) => {
    try {
      setError(null);
      const response = await marketAPI.v1.matchOrders(marketId);
      alert(`Matched ${response.data} orders!`);
      await loadData();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const handleSettleMarket = async (marketId) => {
    if (window.confirm('Are you sure you want to settle this market?')) {
      try {
        setError(null);
        await marketAPI.v1.settle(marketId);
        alert('Market settled successfully!');
        await loadData();
      } catch (err) {
        setError(extractErrorMessage(err));
      }
    }
  };

  const handleChangeMarketStatus = async (marketId, newStatus) => {
    try {
      setError(null);
      await marketAPI.v1.changeStatus(marketId, newStatus);
      await loadData();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const getEventTitle = (eventId) => {
    const event = events.find(e => e.uuid === eventId);
    return event ? event.title : 'Unknown Event';
  };

  if (loading) return <div className="text-center py-8">Loading...</div>;

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-2xl font-bold">Events & Markets</h2>
        <div className="space-x-2">
          <button
            onClick={() => setShowCreateEventForm(!showCreateEventForm)}
            className="bg-purple-500 hover:bg-purple-700 text-white font-bold py-2 px-4 rounded"
          >
            {showCreateEventForm ? 'Cancel' : 'Create Event'}
          </button>
          <button
            onClick={() => setShowCreateMarketForm(!showCreateMarketForm)}
            className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
          >
            {showCreateMarketForm ? 'Cancel' : 'Create Market'}
          </button>
        </div>
      </div>

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
          Error: {error}
        </div>
      )}

      {showCreateEventForm && (
        <form onSubmit={handleCreateEvent} className="bg-white shadow-md rounded px-8 pt-6 pb-8">
          <h3 className="text-xl font-semibold mb-4">Create New Event</h3>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2">Title</label>
            <input
              type="text"
              value={newEvent.title}
              onChange={(e) => setNewEvent({ ...newEvent, title: e.target.value })}
              className="shadow border rounded w-full py-2 px-3 text-gray-700"
              required
            />
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2">Description</label>
            <textarea
              value={newEvent.description}
              onChange={(e) => setNewEvent({ ...newEvent, description: e.target.value })}
              className="shadow border rounded w-full py-2 px-3 text-gray-700"
              rows="3"
            />
          </div>
          <button
            type="submit"
            className="bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded"
          >
            Create Event
          </button>
        </form>
      )}

      {showCreateMarketForm && (
        <form onSubmit={handleCreateMarket} className="bg-white shadow-md rounded px-8 pt-6 pb-8">
          <h3 className="text-xl font-semibold mb-4">Create New Market</h3>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2">Event</label>
            <select
              value={newMarket.eventId}
              onChange={(e) => setNewMarket({ ...newMarket, eventId: e.target.value })}
              className="shadow border rounded w-full py-2 px-3 text-gray-700"
              required
            >
              <option value="">Select an event...</option>
              {events.filter(e => e.state === 'OPEN').map((event) => (
                <option key={event.uuid} value={event.uuid}>
                  {event.title}
                </option>
              ))}
            </select>
          </div>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-bold mb-2">Outcome</label>
            <input
              type="text"
              value={newMarket.outcome}
              onChange={(e) => setNewMarket({ ...newMarket, outcome: e.target.value })}
              className="shadow border rounded w-full py-2 px-3 text-gray-700"
              placeholder="e.g., YES"
              required
            />
          </div>
          <button
            type="submit"
            className="bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded"
          >
            Create Market
          </button>
        </form>
      )}

      <div className="bg-white shadow-md rounded overflow-hidden">
        <h3 className="text-xl font-semibold px-6 py-4 bg-gray-50">Active Markets</h3>
        <table className="min-w-full">
          <thead className="bg-gray-100">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Event</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Outcome</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {markets.map((market) => (
              <tr key={market.uuid}>
                <td className="px-6 py-4 whitespace-nowrap">
                  {getEventTitle(market.eventId)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap font-semibold">
                  {market.outcome}
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <select
                    value={market.status}
                    onChange={(e) => handleChangeMarketStatus(market.uuid, e.target.value)}
                    className={`px-2 py-1 rounded text-xs border-0 cursor-pointer ${
                      market.status === 'ACTIVE' ? 'bg-green-100 text-green-800' :
                      market.status === 'SUSPENDED' ? 'bg-yellow-100 text-yellow-800' :
                      'bg-gray-100 text-gray-800'
                    }`}
                  >
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="SUSPENDED">SUSPENDED</option>
                    <option value="SETTLED">SETTLED</option>
                  </select>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm space-x-2">
                  {market.status === 'ACTIVE' && (
                    <>
                      <button
                        onClick={() => handleMatchOrders(market.uuid)}
                        className="text-blue-600 hover:text-blue-900"
                      >
                        Match Orders
                      </button>
                      <button
                        onClick={() => handleSettleMarket(market.uuid)}
                        className="text-green-600 hover:text-green-900"
                      >
                        Settle
                      </button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="bg-white shadow-md rounded overflow-hidden">
        <h3 className="text-xl font-semibold px-6 py-4 bg-gray-50">All Events</h3>
        <table className="min-w-full">
          <thead className="bg-gray-100">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Title</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Description</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">State</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Created</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {events.map((event) => (
              <tr key={event.uuid}>
                <td className="px-6 py-4 font-medium">{event.title}</td>
                <td className="px-6 py-4 text-sm text-gray-600">{event.description}</td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className={`px-2 py-1 rounded text-xs ${
                    event.state === 'OPEN' ? 'bg-green-100 text-green-800' :
                    event.state === 'CLOSED' ? 'bg-red-100 text-red-800' :
                    'bg-blue-100 text-blue-800'
                  }`}>
                    {event.state}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm">
                  {new Date(event.createdAt).toLocaleDateString()}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

import { useState, useEffect } from 'react';
import { marketAPI, eventAPI } from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import Input, { Select } from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import Badge from '../../components/common/Badge';
import Table from '../../components/common/Table';
import Spinner from '../../components/common/Spinner';

const MarketForm = ({ market, events, onSave, onCancel, loading }) => {
  const [formData, setFormData] = useState({
    eventUuid: market?.event?.uuid || '',
    status: market?.status || 'ACTIVE',
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    onSave(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <Select
        label="Event"
        value={formData.eventUuid}
        onChange={(e) => setFormData({ ...formData, eventUuid: e.target.value })}
        options={events.map((e) => ({ value: e.uuid, label: e.title }))}
        placeholder="Select an event"
        disabled={!!market}
        required
      />
      <Select
        label="Status"
        value={formData.status}
        onChange={(e) => setFormData({ ...formData, status: e.target.value })}
        options={[
          { value: 'ACTIVE', label: 'Active' },
          { value: 'SUSPENDED', label: 'Suspended' },
          { value: 'SETTLED', label: 'Settled' },
        ]}
      />
      <div className="flex justify-end gap-3 pt-4">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" loading={loading}>
          {market ? 'Update' : 'Create'} Market
        </Button>
      </div>
    </form>
  );
};

const SettleModal = ({ market, onSettle, onCancel, loading }) => {
  const [winner, setWinner] = useState('YES');

  return (
    <div className="space-y-4">
      <p className="text-[#a0a0a0]">
        Choose the winning outcome for this market. This action cannot be undone.
      </p>
      <div className="flex gap-4">
        <button
          onClick={() => setWinner('YES')}
          className={`flex-1 p-4 rounded-lg border-2 transition-all ${
            winner === 'YES'
              ? 'border-[#22c55e] bg-[#22c55e]/10'
              : 'border-[#2a2a2a] hover:border-[#333333]'
          }`}
        >
          <span className="text-lg font-bold text-[#22c55e]">YES</span>
        </button>
        <button
          onClick={() => setWinner('NO')}
          className={`flex-1 p-4 rounded-lg border-2 transition-all ${
            winner === 'NO'
              ? 'border-[#ef4444] bg-[#ef4444]/10'
              : 'border-[#2a2a2a] hover:border-[#333333]'
          }`}
        >
          <span className="text-lg font-bold text-[#ef4444]">NO</span>
        </button>
      </div>
      <div className="flex justify-end gap-3 pt-4">
        <Button variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button
          variant={winner === 'YES' ? 'success' : 'danger'}
          onClick={() => onSettle(winner)}
          loading={loading}
        >
          Settle as {winner}
        </Button>
      </div>
    </div>
  );
};

const Markets = () => {
  const [markets, setMarkets] = useState([]);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [showSettleModal, setShowSettleModal] = useState(false);
  const [editingMarket, setEditingMarket] = useState(null);
  const [settlingMarket, setSettlingMarket] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const fetchData = async () => {
    try {
      const [marketsRes, eventsRes] = await Promise.all([
        marketAPI.v1.getAll(),
        eventAPI.v1.getAll(),
      ]);
      setMarkets(marketsRes.data);
      setEvents(eventsRes.data);
    } catch (err) {
      console.error('Failed to fetch data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleCreate = () => {
    setEditingMarket(null);
    setShowModal(true);
    setError('');
  };

  const handleEdit = (market) => {
    setEditingMarket(market);
    setShowModal(true);
    setError('');
  };

  const handleSave = async (formData) => {
    setSaving(true);
    setError('');
    try {
      if (editingMarket) {
        await marketAPI.v1.patch(editingMarket.uuid, [
          { op: 'replace', path: '/status', value: formData.status },
        ]);
      } else {
        await marketAPI.v1.create({ eventUuid: formData.eventUuid });
      }
      setShowModal(false);
      fetchData();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save market');
    } finally {
      setSaving(false);
    }
  };

  const handleMatchOrders = async (marketUuid) => {
    try {
      await marketAPI.v1.matchOrders(marketUuid);
      fetchData();
    } catch (err) {
      console.error('Failed to match orders:', err);
    }
  };

  const handleOpenSettle = (market) => {
    setSettlingMarket(market);
    setShowSettleModal(true);
  };

  const handleSettle = async (winner) => {
    setSaving(true);
    try {
      await marketAPI.v1.settle(settlingMarket.uuid);
      setShowSettleModal(false);
      fetchData();
    } catch (err) {
      console.error('Failed to settle market:', err);
    } finally {
      setSaving(false);
    }
  };

  const columns = [
    {
      key: 'event',
      header: 'Event',
      render: (value) => (
        <span className="font-medium">{value?.title || 'Unknown'}</span>
      ),
    },
    {
      key: 'status',
      header: 'Status',
      render: (value) => <Badge status={value} />,
    },
    {
      key: 'lastPrice',
      header: 'Last Price',
      render: (value) => value ? `${(value * 100).toFixed(0)}c` : '-',
    },
    {
      key: 'volume',
      header: 'Volume',
      render: (value) => value ? `$${value.toLocaleString()}` : '$0',
    },
    {
      key: 'createdAt',
      header: 'Created',
      render: (value) => new Date(value).toLocaleDateString(),
    },
    {
      key: 'actions',
      header: '',
      render: (_, row) => (
        <div className="flex gap-2">
          <Button size="sm" variant="ghost" onClick={() => handleEdit(row)}>
            Edit
          </Button>
          {row.status === 'ACTIVE' && (
            <>
              <Button
                size="sm"
                variant="secondary"
                onClick={() => handleMatchOrders(row.uuid)}
              >
                Match
              </Button>
              <Button
                size="sm"
                variant="success"
                onClick={() => handleOpenSettle(row)}
              >
                Settle
              </Button>
            </>
          )}
        </div>
      ),
    },
  ];

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-2xl font-bold text-white">Markets</h1>
        <Button onClick={handleCreate}>
          <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
          </svg>
          Create Market
        </Button>
      </div>

      <Card padding="none">
        <Table
          columns={columns}
          data={markets}
          emptyMessage="No markets yet. Create your first market!"
        />
      </Card>

      {/* Create/Edit Modal */}
      <Modal
        isOpen={showModal}
        onClose={() => setShowModal(false)}
        title={editingMarket ? 'Edit Market' : 'Create Market'}
      >
        {error && (
          <div className="mb-4 p-3 bg-[#ef4444]/10 border border-[#ef4444]/20 rounded-lg">
            <p className="text-sm text-[#ef4444]">{error}</p>
          </div>
        )}
        <MarketForm
          market={editingMarket}
          events={events}
          onSave={handleSave}
          onCancel={() => setShowModal(false)}
          loading={saving}
        />
      </Modal>

      {/* Settle Modal */}
      <Modal
        isOpen={showSettleModal}
        onClose={() => setShowSettleModal(false)}
        title="Settle Market"
      >
        <SettleModal
          market={settlingMarket}
          onSettle={handleSettle}
          onCancel={() => setShowSettleModal(false)}
          loading={saving}
        />
      </Modal>
    </div>
  );
};

export default Markets;

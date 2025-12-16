import { useState, useEffect, useCallback } from 'react';
import { walletAPI, orderAPI, positionAPI, transactionAPI, marketAPI, eventAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import Card from '../components/common/Card';
import Button from '../components/common/Button';
import Input from '../components/common/Input';
import Modal from '../components/common/Modal';
import Badge from '../components/common/Badge';
import Table from '../components/common/Table';
import Tabs from '../components/common/Tabs';
import Spinner from '../components/common/Spinner';

// Wallet Card Component
const WalletCard = ({ wallet, onUpdate }) => {
  const { user } = useAuth();
  const [showDeposit, setShowDeposit] = useState(false);
  const [showWithdraw, setShowWithdraw] = useState(false);
  const [amount, setAmount] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleDeposit = async () => {
    setError('');
    setLoading(true);
    try {
      await walletAPI.v1.deposit(user.uuid, parseFloat(amount));
      setAmount('');
      setShowDeposit(false);
      onUpdate?.();
    } catch (err) {
      setError(err.response?.data?.message || 'Deposit failed');
    } finally {
      setLoading(false);
    }
  };

  const handleWithdraw = async () => {
    setError('');
    setLoading(true);
    try {
      await walletAPI.v1.withdraw(user.uuid, parseFloat(amount));
      setAmount('');
      setShowWithdraw(false);
      onUpdate?.();
    } catch (err) {
      setError(err.response?.data?.message || 'Withdrawal failed');
    } finally {
      setLoading(false);
    }
  };

  if (!wallet) return null;

  return (
    <>
      <Card className="bg-gradient-to-br from-[#1a1a1a] to-[#141414]">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-semibold text-white">Your Wallet</h2>
          <div className="flex gap-2">
            <Button size="sm" variant="success" onClick={() => setShowDeposit(true)}>
              Deposit
            </Button>
            <Button size="sm" variant="secondary" onClick={() => setShowWithdraw(true)}>
              Withdraw
            </Button>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-6">
          <div>
            <p className="text-sm text-[#a0a0a0] mb-1">Available Balance</p>
            <p className="text-3xl font-bold text-white">
              ${wallet.balance?.toFixed(2) || '0.00'}
            </p>
          </div>
          <div>
            <p className="text-sm text-[#a0a0a0] mb-1">Locked in Orders</p>
            <p className="text-3xl font-bold text-[#f59e0b]">
              ${wallet.lockedBalance?.toFixed(2) || '0.00'}
            </p>
          </div>
        </div>
      </Card>

      {/* Deposit Modal */}
      <Modal
        isOpen={showDeposit}
        onClose={() => { setShowDeposit(false); setError(''); setAmount(''); }}
        title="Deposit Funds"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowDeposit(false)}>
              Cancel
            </Button>
            <Button
              variant="success"
              onClick={handleDeposit}
              loading={loading}
              disabled={!amount || parseFloat(amount) <= 0}
            >
              Deposit
            </Button>
          </>
        }
      >
        {error && (
          <div className="mb-4 p-3 bg-[#ef4444]/10 border border-[#ef4444]/20 rounded-lg">
            <p className="text-sm text-[#ef4444]">{error}</p>
          </div>
        )}
        <Input
          label="Amount"
          type="number"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          placeholder="0.00"
          min="0.01"
          step="0.01"
        />
      </Modal>

      {/* Withdraw Modal */}
      <Modal
        isOpen={showWithdraw}
        onClose={() => { setShowWithdraw(false); setError(''); setAmount(''); }}
        title="Withdraw Funds"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowWithdraw(false)}>
              Cancel
            </Button>
            <Button
              variant="danger"
              onClick={handleWithdraw}
              loading={loading}
              disabled={!amount || parseFloat(amount) <= 0}
            >
              Withdraw
            </Button>
          </>
        }
      >
        {error && (
          <div className="mb-4 p-3 bg-[#ef4444]/10 border border-[#ef4444]/20 rounded-lg">
            <p className="text-sm text-[#ef4444]">{error}</p>
          </div>
        )}
        <Input
          label="Amount"
          type="number"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          placeholder="0.00"
          min="0.01"
          max={wallet.balance}
          step="0.01"
        />
        <p className="mt-2 text-sm text-[#666666]">
          Available: ${wallet.balance?.toFixed(2)}
        </p>
      </Modal>
    </>
  );
};

// Helper to parse backend date format "dd-MM-yyyy HH:mm:ss"
const parseBackendDate = (dateStr) => {
  if (!dateStr) return null;
  // Handle format "dd-MM-yyyy HH:mm:ss"
  const match = dateStr.match(/^(\d{2})-(\d{2})-(\d{4})\s+(\d{2}):(\d{2}):(\d{2})$/);
  if (match) {
    const [, day, month, year, hour, min, sec] = match;
    return new Date(year, month - 1, day, hour, min, sec);
  }
  // Fallback to standard parsing
  return new Date(dateStr);
};

// Orders Table Component
const OrdersTable = ({ orders, onCancelOrder }) => {
  // Sort orders by createdAt descending (newest first)
  const sortedOrders = [...orders].sort((a, b) => {
    const dateA = parseBackendDate(a.createdAt);
    const dateB = parseBackendDate(b.createdAt);
    return dateB - dateA; // Descending order (newest first)
  });

  const columns = [
    {
      key: 'marketName',
      header: 'Market',
      render: (value) => value || 'Unknown',
    },
    {
      key: 'side',
      header: 'Type',
      render: (value) => {
        // BUY = YES shares, SELL = NO shares (per backend logic)
        const isYes = value === 'BUY';
        return (
          <span className={isYes ? 'text-[#22c55e]' : 'text-[#ef4444]'}>
            {isYes ? 'YES' : 'NO'}
          </span>
        );
      },
    },
    {
      key: 'price',
      header: 'Price',
      render: (value, row) => {
        // For SELL orders (NO shares), show the NO price (1 - price)
        const displayPrice = row.side === 'SELL' ? (1 - value) : value;
        return `${(displayPrice * 100).toFixed(0)}c`;
      },
    },
    {
      key: 'quantity',
      header: 'Qty',
    },
    {
      key: 'filledQuantity',
      header: 'Filled',
      render: (value) => value || 0,
    },
    {
      key: 'state',
      header: 'Status',
      render: (value) => <Badge status={value} size="sm" />,
    },
    {
      key: 'actions',
      header: '',
      render: (_, row) => row.state === 'PENDING' && (
        <Button
          size="sm"
          variant="ghost"
          onClick={() => onCancelOrder(row.uuid)}
        >
          Cancel
        </Button>
      ),
    },
  ];

  return <Table columns={columns} data={sortedOrders} emptyMessage="No orders yet" />;
};

// Positions Table Component
const PositionsTable = ({ positions, onDeletePosition }) => {
  const columns = [
    {
      key: 'market',
      header: 'Market',
      render: (_, row) => (
        <div className="flex items-center gap-2">
          <span>{row.marketName || 'Unknown'}</span>
          {row.marketStatus === 'SETTLED' && (
            <Badge status="SETTLED" size="sm" />
          )}
        </div>
      ),
    },
    {
      key: 'yesShares',
      header: 'YES',
      render: (value) => (
        <span className={value > 0 ? 'text-[#22c55e]' : 'text-[#666666]'}>
          {value || 0}
        </span>
      ),
    },
    {
      key: 'noShares',
      header: 'NO',
      render: (value) => (
        <span className={value > 0 ? 'text-[#ef4444]' : 'text-[#666666]'}>
          {value || 0}
        </span>
      ),
    },
    {
      key: 'currentValue',
      header: 'Current Value',
      render: (_, row) => {
        if (row.marketStatus === 'SETTLED') {
          return <span className="text-[#666666]">Settled</span>;
        }
        const lastPrice = row.lastPrice || 0.5;
        const yesValue = (row.yesShares || 0) * lastPrice;
        const noValue = (row.noShares || 0) * (1 - lastPrice);
        const total = yesValue + noValue;
        return `$${total.toFixed(2)}`;
      },
    },
    {
      key: 'valueIfYes',
      header: 'If YES Wins',
      render: (_, row) => {
        if (row.marketStatus === 'SETTLED') return '-';
        const value = row.yesShares || 0;
        return (
          <span className="text-[#22c55e]">
            ${value.toFixed(2)}
          </span>
        );
      },
    },
    {
      key: 'valueIfNo',
      header: 'If NO Wins',
      render: (_, row) => {
        if (row.marketStatus === 'SETTLED') return '-';
        const value = row.noShares || 0;
        return (
          <span className="text-[#ef4444]">
            ${value.toFixed(2)}
          </span>
        );
      },
    },
    {
      key: 'avgCost',
      header: 'Avg Cost',
      render: (_, row) => {
        const avgYes = row.avgYesCost;
        const avgNo = row.avgNoCost;
        if (!avgYes && !avgNo) return '-';
        const parts = [];
        if (avgYes) parts.push(`Y:${(avgYes * 100).toFixed(0)}c`);
        if (avgNo) parts.push(`N:${(avgNo * 100).toFixed(0)}c`);
        return parts.join(' / ');
      },
    },
    {
      key: 'actions',
      header: '',
      render: (_, row) => row.marketStatus === 'SETTLED' && (
        <Button
          size="sm"
          variant="ghost"
          onClick={() => onDeletePosition(row.uuid)}
        >
          Delete
        </Button>
      ),
    },
  ];

  return <Table columns={columns} data={positions} emptyMessage="No positions yet" />;
};

// Transactions Table Component
const TransactionsTable = ({ transactions }) => {
  // Sort transactions by createdAt descending (newest first)
  const sortedTransactions = [...transactions].sort((a, b) => {
    const dateA = parseBackendDate(a.createdAt);
    const dateB = parseBackendDate(b.createdAt);
    return dateB - dateA; // Descending order (newest first)
  });

  const columns = [
    {
      key: 'createdAt',
      header: 'Date',
      render: (value) => {
        const date = parseBackendDate(value);
        return date && !isNaN(date) ? date.toLocaleDateString() : '-';
      },
    },
    {
      key: 'type',
      header: 'Type',
      render: (value) => <Badge status={value} size="sm" />,
    },
    {
      key: 'amount',
      header: 'Amount',
      render: (value, row) => {
        const isPositive = ['DEPOSIT', 'ORDER_EXECUTED', 'ORDER_CANCELLED'].includes(row.type);
        return (
          <span className={isPositive ? 'text-[#22c55e]' : 'text-[#ef4444]'}>
            {isPositive ? '+' : '-'}${Math.abs(value).toFixed(2)}
          </span>
        );
      },
    },
  ];

  return <Table columns={columns} data={sortedTransactions} emptyMessage="No transactions yet" />;
};

// Main Profile Component
const Profile = () => {
  const { user } = useAuth();
  const [wallet, setWallet] = useState(null);
  const [orders, setOrders] = useState([]);
  const [positions, setPositions] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchData = useCallback(async () => {
    if (!user?.uuid) return;

    try {
      const [walletRes, ordersRes, positionsRes, transactionsRes, marketsRes, eventsRes] = await Promise.all([
        walletAPI.v1.getByUserId(user.uuid),
        orderAPI.v1.getByUserId(user.uuid),
        positionAPI.v1.getByUserId(user.uuid),
        transactionAPI.v1.getByUserId(user.uuid),
        marketAPI.v1.getAll(),
        eventAPI.v1.getAll(),
      ]);

      // Create lookup maps for markets and events
      const marketsMap = {};
      (marketsRes.data || []).forEach(m => { marketsMap[m.uuid] = m; });
      const eventsMap = {};
      (eventsRes.data || []).forEach(e => { eventsMap[e.uuid] = e; });

      // Enrich positions with lastPrice and status from market
      const enrichedPositions = (positionsRes.data || []).map(pos => {
        const market = marketsMap[pos.marketId];
        const event = market ? eventsMap[market.eventId] : null;
        const lastPrice = market?.lastPrice || 0.5;
        return {
          ...pos,
          marketName: event?.title || market?.outcome || 'Unknown',
          marketStatus: market?.status || 'UNKNOWN',
          lastPrice,
        };
      });

      // Enrich orders with market name
      const enrichedOrders = (ordersRes.data || []).map(order => {
        const market = marketsMap[order.marketId];
        const event = market ? eventsMap[market.eventId] : null;
        return {
          ...order,
          marketName: event?.title || market?.outcome || 'Unknown',
        };
      });

      setWallet(walletRes.data);
      setOrders(enrichedOrders);
      setPositions(enrichedPositions);
      setTransactions(transactionsRes.data);
    } catch (err) {
      console.error('Failed to fetch profile data:', err);
    } finally {
      setLoading(false);
    }
  }, [user?.uuid]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleCancelOrder = async (orderUuid) => {
    try {
      await orderAPI.v1.cancel(orderUuid);
      fetchData();
    } catch (err) {
      console.error('Failed to cancel order:', err);
    }
  };

  const handleDeletePosition = async (positionUuid) => {
    try {
      await positionAPI.v1.delete(positionUuid);
      fetchData();
    } catch (err) {
      console.error('Failed to delete position:', err);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner size="lg" />
      </div>
    );
  }

  const tabs = [
    {
      id: 'positions',
      label: 'Positions',
      count: positions.length,
      content: <PositionsTable positions={positions} onDeletePosition={handleDeletePosition} />,
    },
    {
      id: 'orders',
      label: 'Orders',
      count: orders.length,
      content: <OrdersTable orders={orders} onCancelOrder={handleCancelOrder} />,
    },
    {
      id: 'transactions',
      label: 'Transactions',
      count: transactions.length,
      content: <TransactionsTable transactions={transactions} />,
    },
  ];

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-white mb-2">My Portfolio</h1>
        <p className="text-[#a0a0a0]">
          Welcome back, {user?.name}
        </p>
      </div>

      {/* Wallet */}
      <div className="mb-8">
        <WalletCard wallet={wallet} onUpdate={fetchData} />
      </div>

      {/* Tabs */}
      <Card padding="none">
        <Tabs tabs={tabs} defaultTab="positions" />
      </Card>
    </div>
  );
};

export default Profile;

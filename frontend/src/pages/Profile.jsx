import { useState, useEffect } from 'react';
import { walletAPI, orderAPI, positionAPI, transactionAPI } from '../services/api';
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

// Orders Table Component
const OrdersTable = ({ orders, onCancelOrder }) => {
  const columns = [
    {
      key: 'createdAt',
      header: 'Date',
      render: (value) => new Date(value).toLocaleDateString(),
    },
    {
      key: 'market',
      header: 'Market',
      render: (_, row) => row.market?.event?.title || 'Unknown',
    },
    {
      key: 'shareType',
      header: 'Type',
      render: (value) => (
        <span className={value === 'YES' ? 'text-[#22c55e]' : 'text-[#ef4444]'}>
          {value}
        </span>
      ),
    },
    {
      key: 'price',
      header: 'Price',
      render: (value) => `${(value * 100).toFixed(0)}c`,
    },
    {
      key: 'quantity',
      header: 'Qty',
    },
    {
      key: 'filledQuantity',
      header: 'Filled',
    },
    {
      key: 'status',
      header: 'Status',
      render: (value) => <Badge status={value} size="sm" />,
    },
    {
      key: 'actions',
      header: '',
      render: (_, row) => row.status === 'PENDING' && (
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

  return <Table columns={columns} data={orders} emptyMessage="No orders yet" />;
};

// Positions Table Component
const PositionsTable = ({ positions }) => {
  const columns = [
    {
      key: 'market',
      header: 'Market',
      render: (_, row) => row.market?.event?.title || 'Unknown',
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
      key: 'avgCost',
      header: 'Avg Cost',
      render: (value) => value ? `$${value.toFixed(2)}` : '-',
    },
    {
      key: 'unrealizedPnl',
      header: 'P&L',
      render: (value) => {
        if (!value) return '-';
        const isPositive = value >= 0;
        return (
          <span className={isPositive ? 'text-[#22c55e]' : 'text-[#ef4444]'}>
            {isPositive ? '+' : ''}{value.toFixed(2)}
          </span>
        );
      },
    },
  ];

  return <Table columns={columns} data={positions} emptyMessage="No positions yet" />;
};

// Transactions Table Component
const TransactionsTable = ({ transactions }) => {
  const columns = [
    {
      key: 'createdAt',
      header: 'Date',
      render: (value) => new Date(value).toLocaleDateString(),
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
    {
      key: 'description',
      header: 'Description',
      render: (value) => <span className="text-[#a0a0a0]">{value || '-'}</span>,
    },
  ];

  return <Table columns={columns} data={transactions} emptyMessage="No transactions yet" />;
};

// Main Profile Component
const Profile = () => {
  const { user } = useAuth();
  const [wallet, setWallet] = useState(null);
  const [orders, setOrders] = useState([]);
  const [positions, setPositions] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    try {
      const [walletRes, ordersRes, positionsRes, transactionsRes] = await Promise.all([
        walletAPI.v1.getByUserId(user.uuid),
        orderAPI.v1.getByUserId(user.uuid),
        positionAPI.v1.getByUserId(user.uuid),
        transactionAPI.v1.getByUserId(user.uuid),
      ]);
      setWallet(walletRes.data);
      setOrders(ordersRes.data);
      setPositions(positionsRes.data);
      setTransactions(transactionsRes.data);
    } catch (err) {
      console.error('Failed to fetch profile data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (user?.uuid) {
      fetchData();
    }
  }, [user?.uuid]);

  const handleCancelOrder = async (orderUuid) => {
    try {
      await orderAPI.v1.cancel(orderUuid);
      fetchData();
    } catch (err) {
      console.error('Failed to cancel order:', err);
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
      content: <PositionsTable positions={positions} />,
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

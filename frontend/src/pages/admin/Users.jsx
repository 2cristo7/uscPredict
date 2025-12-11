import { useState, useEffect } from 'react';
import api from '../../services/api';
import Card from '../../components/common/Card';
import Button from '../../components/common/Button';
import { Select } from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import Badge from '../../components/common/Badge';
import Table from '../../components/common/Table';
import Spinner from '../../components/common/Spinner';

const UserDetailModal = ({ user, onClose, onUpdate }) => {
  const [role, setRole] = useState(user?.role || 'USER');
  const [saving, setSaving] = useState(false);
  const [wallet, setWallet] = useState(null);

  useEffect(() => {
    const fetchWallet = async () => {
      try {
        const response = await api.get(`/wallets/user/${user.uuid}`);
        setWallet(response.data);
      } catch (err) {
        console.error('Failed to fetch wallet:', err);
      }
    };
    if (user) {
      fetchWallet();
    }
  }, [user]);

  const handleUpdateRole = async () => {
    setSaving(true);
    try {
      await api.patch(`/users/${user.uuid}`, [
        { op: 'replace', path: '/role', value: role },
      ]);
      onUpdate?.();
      onClose();
    } catch (err) {
      console.error('Failed to update role:', err);
    } finally {
      setSaving(false);
    }
  };

  if (!user) return null;

  return (
    <div className="space-y-6">
      {/* User Info */}
      <div className="space-y-4">
        <div>
          <label className="text-sm text-[#a0a0a0]">Name</label>
          <p className="text-white font-medium">{user.name}</p>
        </div>
        <div>
          <label className="text-sm text-[#a0a0a0]">Email</label>
          <p className="text-white">{user.email}</p>
        </div>
        <div>
          <label className="text-sm text-[#a0a0a0]">Joined</label>
          <p className="text-white">
            {new Date(user.createdAt).toLocaleDateString()}
          </p>
        </div>
      </div>

      {/* Wallet Info */}
      {wallet && (
        <div className="p-4 bg-[#0d0d0d] rounded-lg">
          <h4 className="text-sm font-medium text-white mb-3">Wallet</h4>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-xs text-[#666666]">Balance</label>
              <p className="text-lg font-semibold text-white">
                ${wallet.balance?.toFixed(2) || '0.00'}
              </p>
            </div>
            <div>
              <label className="text-xs text-[#666666]">Locked</label>
              <p className="text-lg font-semibold text-[#f59e0b]">
                ${wallet.lockedBalance?.toFixed(2) || '0.00'}
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Role Update */}
      <div className="pt-4 border-t border-[#2a2a2a]">
        <Select
          label="Role"
          value={role}
          onChange={(e) => setRole(e.target.value)}
          options={[
            { value: 'USER', label: 'User' },
            { value: 'ADMIN', label: 'Admin' },
          ]}
        />
        <div className="flex justify-end gap-3 mt-4">
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            onClick={handleUpdateRole}
            loading={saving}
            disabled={role === user.role}
          >
            Update Role
          </Button>
        </div>
      </div>
    </div>
  );
};

const Users = () => {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedUser, setSelectedUser] = useState(null);

  const fetchUsers = async () => {
    try {
      const response = await api.get('/users');
      setUsers(response.data);
    } catch (err) {
      console.error('Failed to fetch users:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const columns = [
    {
      key: 'name',
      header: 'Name',
      render: (value) => <span className="font-medium">{value}</span>,
    },
    {
      key: 'email',
      header: 'Email',
      render: (value) => <span className="text-[#a0a0a0]">{value}</span>,
    },
    {
      key: 'role',
      header: 'Role',
      render: (value) => <Badge status={value} />,
    },
    {
      key: 'createdAt',
      header: 'Joined',
      render: (value) => new Date(value).toLocaleDateString(),
    },
    {
      key: 'actions',
      header: '',
      render: (_, row) => (
        <Button size="sm" variant="ghost" onClick={() => setSelectedUser(row)}>
          View
        </Button>
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
        <h1 className="text-2xl font-bold text-white">Users</h1>
        <p className="text-[#a0a0a0]">{users.length} total users</p>
      </div>

      <Card padding="none">
        <Table
          columns={columns}
          data={users}
          emptyMessage="No users yet"
        />
      </Card>

      <Modal
        isOpen={!!selectedUser}
        onClose={() => setSelectedUser(null)}
        title="User Details"
      >
        <UserDetailModal
          user={selectedUser}
          onClose={() => setSelectedUser(null)}
          onUpdate={fetchUsers}
        />
      </Modal>
    </div>
  );
};

export default Users;

import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { userAPI, eventAPI, marketAPI, orderAPI } from '../../services/api';
import Card from '../../components/common/Card';
import Spinner from '../../components/common/Spinner';

const StatCard = ({ title, value, icon, color = 'blue', link }) => {
  const colorClasses = {
    blue: 'bg-[#3b82f6]/10 text-[#3b82f6]',
    green: 'bg-[#22c55e]/10 text-[#22c55e]',
    purple: 'bg-[#8b5cf6]/10 text-[#8b5cf6]',
    orange: 'bg-[#f59e0b]/10 text-[#f59e0b]',
  };

  const content = (
    <Card hoverable={!!link} className="h-full">
      <div className="flex items-center gap-4">
        <div className={`p-3 rounded-lg ${colorClasses[color]}`}>
          {icon}
        </div>
        <div>
          <p className="text-sm text-[#a0a0a0]">{title}</p>
          <p className="text-2xl font-bold text-white">{value}</p>
        </div>
      </div>
    </Card>
  );

  return link ? <Link to={link}>{content}</Link> : content;
};

const Dashboard = () => {
  const [stats, setStats] = useState({
    users: 0,
    events: 0,
    markets: 0,
    orders: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const [usersRes, eventsRes, marketsRes, ordersRes] = await Promise.all([
          userAPI.v1.getAll(),
          eventAPI.v1.getAll(),
          marketAPI.v1.getAll(),
          orderAPI.v1.getAll(),
        ]);
        setStats({
          users: usersRes.data?.length || 0,
          events: eventsRes.data?.length || 0,
          markets: marketsRes.data?.length || 0,
          orders: ordersRes.data?.length || 0,
        });
      } catch (err) {
        console.error('Failed to fetch stats:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchStats();
  }, []);

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-white mb-8">Admin Dashboard</h1>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <StatCard
          title="Total Users"
          value={stats.users}
          color="blue"
          link="/admin/users"
          icon={
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
          }
        />
        <StatCard
          title="Events"
          value={stats.events}
          color="green"
          link="/admin/events"
          icon={
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
          }
        />
        <StatCard
          title="Markets"
          value={stats.markets}
          color="purple"
          link="/admin/markets"
          icon={
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
            </svg>
          }
        />
        <StatCard
          title="Orders"
          value={stats.orders}
          color="orange"
          icon={
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
          }
        />
      </div>

      {/* Quick Actions */}
      <Card title="Quick Actions">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Link
            to="/admin/events"
            className="flex items-center gap-3 p-4 bg-[#0d0d0d] rounded-lg hover:bg-[#1f1f1f] transition-colors"
          >
            <div className="p-2 bg-[#22c55e]/10 rounded-lg">
              <svg className="w-5 h-5 text-[#22c55e]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
            </div>
            <span className="text-white font-medium">Create Event</span>
          </Link>
          <Link
            to="/admin/markets"
            className="flex items-center gap-3 p-4 bg-[#0d0d0d] rounded-lg hover:bg-[#1f1f1f] transition-colors"
          >
            <div className="p-2 bg-[#3b82f6]/10 rounded-lg">
              <svg className="w-5 h-5 text-[#3b82f6]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
            </div>
            <span className="text-white font-medium">Create Market</span>
          </Link>
          <Link
            to="/admin/users"
            className="flex items-center gap-3 p-4 bg-[#0d0d0d] rounded-lg hover:bg-[#1f1f1f] transition-colors"
          >
            <div className="p-2 bg-[#8b5cf6]/10 rounded-lg">
              <svg className="w-5 h-5 text-[#8b5cf6]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
              </svg>
            </div>
            <span className="text-white font-medium">Manage Users</span>
          </Link>
        </div>
      </Card>
    </div>
  );
};

export default Dashboard;

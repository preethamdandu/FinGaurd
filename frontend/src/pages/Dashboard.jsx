import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { transactions } from '../services/api';
import { useAuth } from '../context/AuthContext';
import {
  TrendingUp, TrendingDown, Wallet, ShieldAlert, ArrowRight, AlertTriangle,
} from 'lucide-react';

function StatCard({ icon: Icon, label, value, color, sub }) {
  const colors = {
    green: 'bg-emerald-50 text-emerald-600',
    red: 'bg-red-50 text-red-600',
    blue: 'bg-indigo-50 text-indigo-600',
    yellow: 'bg-amber-50 text-amber-600',
  };
  return (
    <div className="bg-white rounded-xl border border-gray-200 p-5">
      <div className="flex items-center gap-3 mb-3">
        <div className={`p-2 rounded-lg ${colors[color]}`}><Icon size={20} /></div>
        <span className="text-sm text-gray-500">{label}</span>
      </div>
      <p className="text-2xl font-bold text-gray-900">{value}</p>
      {sub && <p className="text-xs text-gray-400 mt-1">{sub}</p>}
    </div>
  );
}

const fmt = (n) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(n || 0);

export default function Dashboard() {
  const { user } = useAuth();
  const [stats, setStats] = useState(null);
  const [recent, setRecent] = useState([]);
  const [fraudAlerts, setFraudAlerts] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      transactions.stats().catch(() => null),
      transactions.list('page=0&size=5&sortBy=transactionDate&sortDir=desc').catch(() => ({ content: [] })),
      transactions.fraud().catch(() => ({ content: [] })),
    ]).then(([s, r, f]) => {
      setStats(s);
      setRecent(r?.content || []);
      setFraudAlerts(f?.content || []);
    }).finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600" />
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-6xl">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">
          Welcome back, {user?.firstName || user?.username}
        </h1>
        <p className="text-gray-500 mt-1">Here's your financial overview</p>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={Wallet} label="Current Balance" value={fmt(stats?.currentBalance)} color="blue" sub="Last 30 days" />
        <StatCard icon={TrendingUp} label="Total Income" value={fmt(stats?.totalIncome)} color="green" sub={`${stats?.incomeTransactions || 0} transactions`} />
        <StatCard icon={TrendingDown} label="Total Expenses" value={fmt(stats?.totalExpenses)} color="red" sub={`${stats?.expenseTransactions || 0} transactions`} />
        <StatCard icon={ShieldAlert} label="Fraud Alerts" value={stats?.fraudTransactions || 0} color="yellow" sub="Flagged transactions" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recent transactions */}
        <div className="lg:col-span-2 bg-white rounded-xl border border-gray-200">
          <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
            <h2 className="font-semibold text-gray-900">Recent Transactions</h2>
            <Link to="/transactions" className="text-sm text-indigo-600 hover:underline flex items-center gap-1">
              View all <ArrowRight size={14} />
            </Link>
          </div>
          {recent.length === 0 ? (
            <p className="p-5 text-sm text-gray-400">No transactions yet. Create your first one!</p>
          ) : (
            <div className="divide-y divide-gray-50">
              {recent.map((tx) => (
                <div key={tx.id} className="flex items-center justify-between px-5 py-3.5">
                  <div className="flex items-center gap-3 min-w-0">
                    <div className={`w-2 h-2 rounded-full flex-shrink-0 ${tx.transactionType === 'INCOME' ? 'bg-emerald-400' : 'bg-red-400'}`} />
                    <div className="min-w-0">
                      <p className="text-sm font-medium text-gray-900 truncate">
                        {tx.description || tx.category}
                      </p>
                      <p className="text-xs text-gray-400">{tx.category} &middot; {new Date(tx.transactionDate).toLocaleDateString()}</p>
                    </div>
                  </div>
                  <div className="text-right flex-shrink-0 ml-4">
                    <p className={`text-sm font-semibold ${tx.transactionType === 'INCOME' ? 'text-emerald-600' : 'text-gray-900'}`}>
                      {tx.transactionType === 'INCOME' ? '+' : '-'}{fmt(tx.amount)}
                    </p>
                    {tx.isFraudFlagged && (
                      <span className="text-xs text-amber-600 flex items-center gap-0.5 justify-end">
                        <AlertTriangle size={10} /> Flagged
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Fraud alerts sidebar */}
        <div className="bg-white rounded-xl border border-gray-200">
          <div className="flex items-center justify-between px-5 py-4 border-b border-gray-100">
            <h2 className="font-semibold text-gray-900">Fraud Alerts</h2>
            <Link to="/fraud" className="text-sm text-indigo-600 hover:underline flex items-center gap-1">
              View all <ArrowRight size={14} />
            </Link>
          </div>
          {fraudAlerts.length === 0 ? (
            <div className="p-5 text-center">
              <ShieldAlert className="mx-auto text-emerald-400 mb-2" size={32} />
              <p className="text-sm text-gray-500">All clear! No fraud alerts.</p>
            </div>
          ) : (
            <div className="divide-y divide-gray-50">
              {fraudAlerts.slice(0, 5).map((tx) => (
                <div key={tx.id} className="px-5 py-3">
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-medium text-gray-900">{fmt(tx.amount)}</p>
                    <span className="text-xs bg-red-100 text-red-700 px-2 py-0.5 rounded-full font-medium">
                      Risk: {((tx.fraudRiskScore || 0) * 100).toFixed(0)}%
                    </span>
                  </div>
                  <p className="text-xs text-gray-400 mt-0.5">{tx.category} &middot; {new Date(tx.transactionDate).toLocaleDateString()}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

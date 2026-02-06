import { useEffect, useState } from 'react';
import { transactions as api } from '../services/api';
import { ShieldAlert, ShieldCheck, AlertTriangle } from 'lucide-react';

const fmt = (n) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(n || 0);

function riskBadge(score) {
  const pct = ((score || 0) * 100).toFixed(0);
  if (score >= 0.8) return <span className="bg-red-100 text-red-700 px-2.5 py-1 rounded-full text-xs font-semibold">Critical {pct}%</span>;
  if (score >= 0.7) return <span className="bg-amber-100 text-amber-700 px-2.5 py-1 rounded-full text-xs font-semibold">High {pct}%</span>;
  return <span className="bg-yellow-100 text-yellow-700 px-2.5 py-1 rounded-full text-xs font-semibold">Flagged {pct}%</span>;
}

export default function FraudAlerts() {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.fraud()
      .then((data) => setAlerts(data?.content || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600" />
      </div>
    );
  }

  return (
    <div className="max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Fraud Alerts</h1>
        <p className="text-gray-500 mt-1">Transactions flagged by the fraud detection system</p>
      </div>

      {alerts.length === 0 ? (
        <div className="bg-white rounded-xl border border-gray-200 p-12 text-center">
          <ShieldCheck className="mx-auto text-emerald-400 mb-3" size={48} />
          <h3 className="text-lg font-semibold text-gray-900">All clear</h3>
          <p className="text-gray-500 mt-1">No suspicious transactions detected.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {alerts.map((tx) => (
            <div key={tx.id} className="bg-white rounded-xl border border-gray-200 p-5 flex items-start gap-4">
              <div className="p-2 bg-red-50 rounded-lg flex-shrink-0">
                <AlertTriangle className="text-red-500" size={20} />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-3 flex-wrap">
                  <p className="text-lg font-semibold text-gray-900">{fmt(tx.amount)}</p>
                  {riskBadge(tx.fraudRiskScore)}
                  <span className={`text-xs font-medium ${tx.transactionType === 'INCOME' ? 'text-emerald-600' : 'text-red-500'}`}>
                    {tx.transactionType}
                  </span>
                </div>
                <p className="text-sm text-gray-600 mt-1">{tx.description || 'No description'}</p>
                <div className="flex gap-4 mt-2 text-xs text-gray-400">
                  <span>Category: {tx.category}</span>
                  <span>Date: {new Date(tx.transactionDate).toLocaleDateString()}</span>
                  <span>ID: {tx.id?.slice(0, 8)}...</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

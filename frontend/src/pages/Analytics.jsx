import { useEffect, useState } from 'react';
import { transactions as api } from '../services/api';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';

const fmt = (n) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(n || 0);

const COLORS = ['#6366f1', '#8b5cf6', '#a78bfa', '#c4b5fd', '#e0e7ff', '#818cf8', '#4f46e5', '#4338ca'];

export default function Analytics() {
  const [summary, setSummary] = useState(null);
  const [catStats, setCatStats] = useState(null);
  const [days, setDays] = useState(30);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      api.summary(days).catch(() => null),
      api.statsByCategory().catch(() => null),
    ]).then(([s, c]) => {
      setSummary(s);
      setCatStats(c);
    }).finally(() => setLoading(false));
  }, [days]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600" />
      </div>
    );
  }

  // Build chart data from category stats
  const catChartData = catStats
    ? Object.entries(catStats).map(([name, data]) => ({
        name,
        expense: data.expenseTotal || 0,
        income: data.incomeTotal || 0,
      })).sort((a, b) => b.expense - a.expense)
    : [];

  const pieData = catChartData.filter(d => d.expense > 0).map(d => ({ name: d.name, value: d.expense }));

  return (
    <div className="max-w-6xl space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Analytics</h1>
          <p className="text-gray-500 mt-1">Your spending insights and trends</p>
        </div>
        <select
          value={days}
          onChange={(e) => setDays(Number(e.target.value))}
          className="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-indigo-500 outline-none w-fit"
        >
          <option value={7}>Last 7 days</option>
          <option value={30}>Last 30 days</option>
          <option value={90}>Last 90 days</option>
          <option value={365}>Last year</option>
        </select>
      </div>

      {/* Summary cards */}
      {summary && (
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-white rounded-xl border border-gray-200 p-5">
            <p className="text-sm text-gray-500 mb-1">Net Income</p>
            <p className={`text-2xl font-bold ${(summary.netAmount || 0) >= 0 ? 'text-emerald-600' : 'text-red-600'}`}>
              {fmt(summary.netAmount)}
            </p>
            <p className="text-xs text-gray-400 mt-1">{summary.transactionCount} transactions</p>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-5">
            <p className="text-sm text-gray-500 mb-1">Total Income</p>
            <p className="text-2xl font-bold text-emerald-600">{fmt(summary.totalIncome)}</p>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-5">
            <p className="text-sm text-gray-500 mb-1">Total Expenses</p>
            <p className="text-2xl font-bold text-red-500">{fmt(summary.totalExpenses)}</p>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Bar chart */}
        <div className="bg-white rounded-xl border border-gray-200 p-5">
          <h2 className="font-semibold text-gray-900 mb-4">Spending by Category</h2>
          {catChartData.length === 0 ? (
            <p className="text-gray-400 text-sm py-8 text-center">No data available</p>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={catChartData.slice(0, 8)}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} tickFormatter={(v) => `$${v}`} />
                <Tooltip formatter={(v) => fmt(v)} />
                <Bar dataKey="expense" fill="#6366f1" radius={[4, 4, 0, 0]} name="Expenses" />
                <Bar dataKey="income" fill="#10b981" radius={[4, 4, 0, 0]} name="Income" />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Pie chart */}
        <div className="bg-white rounded-xl border border-gray-200 p-5">
          <h2 className="font-semibold text-gray-900 mb-4">Expense Breakdown</h2>
          {pieData.length === 0 ? (
            <p className="text-gray-400 text-sm py-8 text-center">No data available</p>
          ) : (
            <div className="flex flex-col items-center">
              <ResponsiveContainer width="100%" height={250}>
                <PieChart>
                  <Pie data={pieData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={90} label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`} labelLine={false}>
                    {pieData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                  </Pie>
                  <Tooltip formatter={(v) => fmt(v)} />
                </PieChart>
              </ResponsiveContainer>
              <div className="flex flex-wrap gap-3 mt-2 justify-center">
                {pieData.map((d, i) => (
                  <div key={d.name} className="flex items-center gap-1.5 text-xs text-gray-600">
                    <span className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: COLORS[i % COLORS.length] }} />
                    {d.name}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Top categories list */}
      {summary?.topCategories?.length > 0 && (
        <div className="bg-white rounded-xl border border-gray-200 p-5">
          <h2 className="font-semibold text-gray-900 mb-3">Top Categories</h2>
          <div className="flex flex-wrap gap-2">
            {summary.topCategories.map((cat, i) => (
              <span key={cat} className="bg-indigo-50 text-indigo-700 px-3 py-1.5 rounded-lg text-sm font-medium">
                #{i + 1} {cat}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

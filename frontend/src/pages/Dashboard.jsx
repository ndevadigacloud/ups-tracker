import { useQuery } from '@tanstack/react-query'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend } from 'recharts'
import { api } from '../api/client.js'

const COLORS = ['#351c15', '#ffb500', '#8a6500', '#1e40af', '#1a7431', '#a11212', '#777']

function MongoStagePill({ children }) {
  return <code className="mongo-stage-pill">{children}</code>
}

export default function Dashboard() {
  const { data, isLoading } = useQuery({
    queryKey: ['dashboard'],
    queryFn: api.getDashboard,
    refetchInterval: 5000,
  })

  if (isLoading || !data) return <p>Loading dashboard…</p>

  const statusData = Object.entries(data.countsByStatus).map(([status, count]) => ({ status, count }))

  return (
    <div>
      <h2>Operations Dashboard</h2>
      <p className="mongo-note">
        All three panels below come from <strong>one</strong> MongoDB query — a single
        <MongoStagePill>$facet</MongoStagePill> aggregation with three parallel branches, computed
        server-side in one round trip instead of three separate queries.
        See <code>DashboardService.buildDashboard()</code> in shipment-service.
      </p>

      <div className="grid-2">
        <div className="card">
          <h3>Shipments by status</h3>
          <p className="mongo-note mongo-note--small">
            <MongoStagePill>$group</MongoStagePill> by <code>status</code>, counting with{' '}
            <MongoStagePill>$sum: 1</MongoStagePill>.
          </p>
          <ResponsiveContainer width="100%" height={260}>
            <PieChart>
              <Pie data={statusData} dataKey="count" nameKey="status" outerRadius={90} label>
                {statusData.map((entry, i) => <Cell key={entry.status} fill={COLORS[i % COLORS.length]} />)}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </div>

        <div className="card">
          <h3>Volume by day</h3>
          <p className="mongo-note mongo-note--small">
            <MongoStagePill>$group</MongoStagePill> by a <MongoStagePill>$dateToString</MongoStagePill>-truncated
            <code>createdAt</code>, then <MongoStagePill>$sort</MongoStagePill> ascending.
          </p>
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={data.volumeByDay}>
              <XAxis dataKey="date" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="count" fill="#ffb500" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="card">
        <h3>Top destination facilities</h3>
        <p className="mongo-note mongo-note--small">
          <MongoStagePill>$group</MongoStagePill> by <code>destinationFacilityId</code>, then{' '}
          <MongoStagePill>$sort</MongoStagePill> by count descending and <MongoStagePill>$limit: 5</MongoStagePill>.
        </p>
        <table>
          <thead><tr><th>Facility ID</th><th>Shipments</th></tr></thead>
          <tbody>
            {data.topDestinationFacilities.map((f) => (
              <tr key={f.facilityId}><td>{f.facilityId}</td><td>{f.count}</td></tr>
            ))}
            {data.topDestinationFacilities.length === 0 && <tr><td colSpan={2}>No data yet.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  )
}

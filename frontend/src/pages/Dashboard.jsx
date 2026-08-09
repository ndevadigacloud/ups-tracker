import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend } from 'recharts'
import { api } from '../api/client.js'

const COLORS = ['#351c15', '#ffb500', '#8a6500', '#1e40af', '#1a7431', '#a11212', '#777']

function formatStatusLabel(status) {
  return status
    .toLowerCase()
    .split('_')
    .map((word) => word[0].toUpperCase() + word.slice(1))
    .join(' ')
}

export default function Dashboard() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['dashboard'],
    queryFn: api.getDashboard,
    refetchInterval: 5000,
  })

  const { data: facilities = [] } = useQuery({
    queryKey: ['facilities'],
    queryFn: api.listFacilities,
  })
  const facilitiesById = useMemo(() => new Map(facilities.map((f) => [f.id, f])), [facilities])

  if (isLoading) return <p>Loading dashboard…</p>
  if (isError) return <p className="error">Failed to load dashboard.</p>

  const statusData = Object.entries(data.countsByStatus).map(([status, count]) => ({
    status: formatStatusLabel(status),
    count,
  }))

  return (
    <div>
      <h2>Operations Dashboard</h2>
      <p className="page-subtitle">Live snapshot of shipment volume and network activity.</p>

      <div className="grid-2">
        <div className="card">
          <h3>Shipments by status</h3>
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
          <h3>Shipments created per day</h3>
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
        <h3>Busiest destination hubs</h3>
        <table>
          <thead><tr><th>Facility</th><th>City</th><th>Shipments</th></tr></thead>
          <tbody>
            {data.topDestinationFacilities.map((f) => {
              const facility = facilitiesById.get(f.facilityId)
              return (
                <tr key={f.facilityId}>
                  <td>{facility?.name ?? 'Unknown facility'}</td>
                  <td>{facility?.city ?? '—'}</td>
                  <td>{f.count}</td>
                </tr>
              )
            })}
            {data.topDestinationFacilities.length === 0 && <tr><td colSpan={3}>No data yet.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  )
}

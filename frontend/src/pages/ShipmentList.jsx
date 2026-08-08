import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client.js'
import StatusBadge from '../components/StatusBadge.jsx'

const STATUSES = ['CREATED', 'CAPACITY_RESERVED', 'CAPACITY_REJECTED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED', 'EXCEPTION']

export default function ShipmentList() {
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(0)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['shipments', statusFilter, page],
    queryFn: () => api.listShipments({ status: statusFilter || undefined, page }),
    refetchInterval: 5000,
  })

  const shipments = useMemo(() => data?.content ?? [], [data])

  return (
    <div className="card">
      <h2>Shipments</h2>
      <label>Filter by status
        <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0) }}>
          <option value="">All</option>
          {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
      </label>

      {isLoading && <p>Loading…</p>}
      {isError && <p className="error">Failed to load shipments.</p>}

      {!isLoading && !isError && (
        <table>
          <thead>
            <tr><th>ID</th><th>Receiver</th><th>Service</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {shipments.map((s) => (
              <tr key={s.id}>
                <td>{s.id.slice(-8)}</td>
                <td>{s.receiver?.city}, {s.receiver?.state}</td>
                <td>{s.serviceLevel}</td>
                <td><StatusBadge status={s.status} /></td>
                <td><Link to={`/shipments/${s.id}`}>Track</Link></td>
              </tr>
            ))}
            {shipments.length === 0 && (
              <tr><td colSpan={5}>No shipments yet.</td></tr>
            )}
          </tbody>
        </table>
      )}

      <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
        <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Prev</button>
        <button disabled={data?.last} onClick={() => setPage((p) => p + 1)}>Next</button>
      </div>
    </div>
  )
}

import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client.js'

export default function Facilities() {
  const { data: facilities = [], isLoading } = useQuery({
    queryKey: ['facilities'],
    queryFn: api.listFacilities,
    refetchInterval: 5000,
  })

  if (isLoading) return <p>Loading…</p>

  return (
    <div className="card">
      <h2>Facility Capacity</h2>
      <table>
        <thead>
          <tr><th>Facility</th><th>City</th><th>Load</th><th>Capacity</th><th>Utilization</th></tr>
        </thead>
        <tbody>
          {facilities.map((f) => {
            const pct = f.capacityKg > 0 ? Math.round((f.currentLoadKg / f.capacityKg) * 100) : 0
            return (
              <tr key={f.id}>
                <td>{f.name}</td>
                <td>{f.city}</td>
                <td>{f.currentLoadKg.toLocaleString()} kg</td>
                <td>{f.capacityKg.toLocaleString()} kg</td>
                <td>{pct}%</td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

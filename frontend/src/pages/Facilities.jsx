import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client.js'

function utilizationLevel(pct) {
  if (pct >= 90) return 'critical'
  if (pct >= 70) return 'warning'
  return 'ok'
}

function FacilityCard({ facility }) {
  const pct = facility.capacityKg > 0
    ? Math.min(100, Math.round((facility.currentLoadKg / facility.capacityKg) * 100))
    : 0
  const level = utilizationLevel(pct)
  const availableKg = Math.max(0, facility.capacityKg - facility.currentLoadKg)

  return (
    <div className={`facility-card facility-card--${level}`}>
      <div className="facility-card__header">
        <div>
          <h3>{facility.name}</h3>
          <span className="facility-card__city">{facility.city}</span>
        </div>
        <span className={`utilization-pill utilization-pill--${level}`}>{pct}%</span>
      </div>

      <div className="capacity-bar" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
        <div className={`capacity-bar__fill capacity-bar__fill--${level}`} style={{ width: `${pct}%` }} />
      </div>

      <div className="facility-card__stats">
        <div>
          <span className="stat-label">Load</span>
          <span className="stat-value">{facility.currentLoadKg.toLocaleString()} kg</span>
        </div>
        <div>
          <span className="stat-label">Available</span>
          <span className="stat-value">{availableKg.toLocaleString()} kg</span>
        </div>
        <div>
          <span className="stat-label">Capacity</span>
          <span className="stat-value">{facility.capacityKg.toLocaleString()} kg</span>
        </div>
      </div>
    </div>
  )
}

export default function Facilities() {
  const { data: facilities = [], isLoading, isError } = useQuery({
    queryKey: ['facilities'],
    queryFn: api.listFacilities,
    refetchInterval: 5000,
  })

  const summary = useMemo(() => {
    const totalCapacity = facilities.reduce((sum, f) => sum + f.capacityKg, 0)
    const totalLoad = facilities.reduce((sum, f) => sum + f.currentLoadKg, 0)
    const atRisk = facilities.filter((f) => f.capacityKg > 0 && f.currentLoadKg / f.capacityKg >= 0.7).length
    return {
      totalCapacity,
      totalLoad,
      overallPct: totalCapacity > 0 ? Math.round((totalLoad / totalCapacity) * 100) : 0,
      atRisk,
    }
  }, [facilities])

  if (isLoading) return <p>Loading…</p>
  if (isError) return <p className="error">Failed to load facilities.</p>

  return (
    <div>
      <h2>Facility Capacity</h2>
      <p className="page-subtitle">Real-time load across our sorting hubs and delivery facilities.</p>

      <div className="summary-row">
        <div className="summary-tile">
          <span className="stat-label">Facilities</span>
          <span className="summary-tile__value">{facilities.length}</span>
        </div>
        <div className="summary-tile">
          <span className="stat-label">Network utilization</span>
          <span className="summary-tile__value">{summary.overallPct}%</span>
        </div>
        <div className="summary-tile">
          <span className="stat-label">Total load / capacity</span>
          <span className="summary-tile__value">
            {summary.totalLoad.toLocaleString()} / {summary.totalCapacity.toLocaleString()} kg
          </span>
        </div>
        <div className={`summary-tile ${summary.atRisk > 0 ? 'summary-tile--alert' : ''}`}>
          <span className="stat-label">Facilities ≥ 70% full</span>
          <span className="summary-tile__value">{summary.atRisk}</span>
        </div>
      </div>

      <div className="facility-grid">
        {facilities.map((f) => <FacilityCard key={f.id} facility={f} />)}
        {facilities.length === 0 && <p>No facilities yet.</p>}
      </div>
    </div>
  )
}

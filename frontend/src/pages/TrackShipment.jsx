import { useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client.js'
import { useShipmentStream } from '../hooks/useShipmentStream.js'
import StatusBadge from '../components/StatusBadge.jsx'

const SCAN_STEPS = [
  { eventType: 'ARRIVED_AT_HUB', location: 'Origin Hub', description: 'Arrived at origin facility' },
  { eventType: 'DEPARTED_HUB', location: 'Transit Hub', description: 'Departed for transit hub' },
  { eventType: 'OUT_FOR_DELIVERY', location: 'Local Facility', description: 'Out for delivery' },
  { eventType: 'DELIVERED', location: 'Destination', description: 'Delivered' },
]

export default function TrackShipment() {
  const { id } = useParams()
  const queryClient = useQueryClient()

  const { data: shipment } = useQuery({
    queryKey: ['shipment', id],
    queryFn: () => api.getShipment(id),
  })

  const { data: initialEvents = [] } = useQuery({
    queryKey: ['tracking-events', id],
    queryFn: () => api.getTrackingEvents(id),
    enabled: !!id,
  })

  const { status, events } = useShipmentStream(id, initialEvents)
  const currentStatus = status?.status ?? shipment?.status

  const simulateNext = useMutation({
    mutationFn: (step) => api.simulateScan(id, step),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['shipments'] }),
  })

  if (!shipment) return <p>Loading…</p>

  return (
    <div className="grid-2">
      <div className="card">
        <h2>Shipment {id.slice(-8)}</h2>
        {currentStatus && <StatusBadge status={currentStatus} />}
        <p>{shipment.sender?.city} → {shipment.receiver?.city}</p>
        <p>Service level: {shipment.serviceLevel}</p>
        <p>Estimated delivery: {new Date(shipment.estimatedDelivery).toLocaleDateString()}</p>

        <h3>Simulate hub scan</h3>
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          {SCAN_STEPS.map((step) => (
            <button key={step.eventType} onClick={() => simulateNext.mutate(step)} disabled={simulateNext.isPending}>
              {step.eventType.replaceAll('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      <div className="card">
        <h3>Tracking timeline</h3>
        <ul className="timeline">
          {events.map((e, i) => (
            <li key={e.id ?? i}>
              <strong>{e.eventType?.replaceAll('_', ' ')}</strong> — {e.location}
              <div className="ts">{e.timestamp ? new Date(e.timestamp).toLocaleString() : ''}</div>
            </li>
          ))}
          {events.length === 0 && <li>No scans yet.</li>}
        </ul>
      </div>
    </div>
  )
}

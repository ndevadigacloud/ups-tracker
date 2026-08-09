import { useEffect, useMemo } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client.js'
import { useShipmentStream } from '../hooks/useShipmentStream.js'
import StatusBadge from '../components/StatusBadge.jsx'
import { describeStep } from '../utils/scanStep.js'

const TERMINAL_STATUSES = ['DELIVERED', 'CAPACITY_REJECTED', 'EXCEPTION']

export default function TrackShipment() {
  const { id } = useParams()
  const queryClient = useQueryClient()

  // SSE delivers instant updates, but a push can be missed if the saga
  // resolves faster than the browser's EventSource finishes connecting
  // (ShipmentSseHub drops events with no subscribers - there's no replay).
  // Poll as a safety net so the page can't get stuck showing stale status;
  // stop once the shipment reaches a terminal state.
  const { data: shipment } = useQuery({
    queryKey: ['shipment', id],
    queryFn: () => api.getShipment(id),
    refetchInterval: (query) => (TERMINAL_STATUSES.includes(query.state.data?.status) ? false : 3000),
  })

  const { data: initialEvents = [] } = useQuery({
    queryKey: ['tracking-events', id],
    queryFn: () => api.getTrackingEvents(id),
    enabled: !!id,
  })

  const { data: facilities = [] } = useQuery({
    queryKey: ['facilities'],
    queryFn: api.listFacilities,
  })
  const facilitiesById = useMemo(() => new Map(facilities.map((f) => [f.id, f])), [facilities])

  const { status, events } = useShipmentStream(id, initialEvents)
  const currentStatus = status?.status ?? shipment?.status

  const { data: nextStep } = useQuery({
    queryKey: ['next-scan-step', id],
    queryFn: () => api.getNextScanStep(id),
    enabled: !!shipment,
  })

  // The next valid scan changes as a side effect of Kafka events arriving
  // over SSE (capacity result, a previous scan) - re-check it whenever those
  // land instead of only after a button click.
  useEffect(() => {
    if (id) queryClient.invalidateQueries({ queryKey: ['next-scan-step', id] })
  }, [id, currentStatus, events.length, queryClient])

  const simulateNext = useMutation({
    mutationFn: (step) => api.simulateScan(id, step),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['shipments'] })
      queryClient.invalidateQueries({ queryKey: ['shipments-all'] })
      queryClient.invalidateQueries({ queryKey: ['next-scan-step', id] })
    },
  })

  if (!shipment) return <p>Loading…</p>

  const stepInfo = nextStep?.eventType ? describeStep(nextStep.eventType, shipment, facilitiesById) : null

  return (
    <div className="grid-2">
      <div className="card">
        <h2>Shipment {id.slice(-8)}</h2>
        {currentStatus && <StatusBadge status={currentStatus} />}
        <p>{shipment.sender?.city} → {shipment.receiver?.city}</p>
        <p>Service level: {shipment.serviceLevel}</p>
        <p>Estimated delivery: {new Date(shipment.estimatedDelivery).toLocaleDateString()}</p>

        <h3>Next action</h3>
        {!nextStep && <p className="next-action next-action--pending">Checking shipment status…</p>}

        {nextStep?.state === 'PENDING_CAPACITY' && (
          <p className="next-action next-action--pending">⏳ {nextStep.message}</p>
        )}
        {nextStep?.state === 'REJECTED' && (
          <p className="next-action next-action--rejected">⚠️ {nextStep.message}</p>
        )}
        {nextStep?.state === 'EXCEPTION' && (
          <p className="next-action next-action--rejected">⚠️ {nextStep.message}</p>
        )}
        {nextStep?.state === 'DELIVERED' && (
          <p className="next-action next-action--done">✅ Delivered — nothing left to do.</p>
        )}
        {nextStep?.state === 'READY' && stepInfo && (
          <div className="next-action next-action--ready">
            <p className="next-action__hint">
              Simulating the next real-world scan for this shipment (stands in for a driver's
              handheld scanner or a hub barcode reader).
            </p>
            <button
              onClick={() => simulateNext.mutate({
                eventType: nextStep.eventType,
                location: stepInfo.location,
                description: stepInfo.description,
              })}
              disabled={simulateNext.isPending}
            >
              {simulateNext.isPending ? 'Recording scan…' : stepInfo.buttonLabel}
            </button>
            {simulateNext.isError && <p className="error">{simulateNext.error.message}</p>}
          </div>
        )}
      </div>

      <div className="card">
        <h3>Tracking timeline</h3>
        <ul className="timeline">
          {events.map((e, i) => (
            <li key={e.id ?? i}>
              <strong>{e.eventType?.replaceAll('_', ' ')}</strong> — {e.location}
              {e.description && <div className="timeline-desc">{e.description}</div>}
              <div className="ts">{e.timestamp ? new Date(e.timestamp).toLocaleString() : ''}</div>
            </li>
          ))}
          {events.length === 0 && <li>No scans yet.</li>}
        </ul>
      </div>
    </div>
  )
}

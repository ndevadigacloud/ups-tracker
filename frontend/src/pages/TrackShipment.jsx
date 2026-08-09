import { useEffect, useMemo } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client.js'
import { useShipmentStream } from '../hooks/useShipmentStream.js'
import StatusBadge from '../components/StatusBadge.jsx'

/**
 * Turns a bare eventType + the shipment/facility data into the label a real
 * driver-facing scanner app would show - so the single action button reads
 * like "Depart Louisville Air Hub" instead of a raw enum name.
 */
function describeStep(eventType, shipment, facilitiesById) {
  const originName = facilitiesById.get(shipment.originFacilityId)?.name ?? 'origin facility'
  const destinationName = facilitiesById.get(shipment.destinationFacilityId)?.name ?? 'destination facility'
  const receiverCity = shipment.receiver?.city ?? 'destination city'
  const receiverName = shipment.receiver?.name ?? 'the receiver'

  switch (eventType) {
    case 'ARRIVED_AT_HUB':
      return {
        buttonLabel: `Confirm arrival at ${originName}`,
        location: originName,
        description: `Package scanned in at ${originName}`,
      }
    case 'DEPARTED_HUB':
      return {
        buttonLabel: `Depart ${originName} toward ${receiverCity}`,
        location: originName,
        description: `Departed ${originName}, en route to ${destinationName}`,
      }
    case 'OUT_FOR_DELIVERY':
      return {
        buttonLabel: `Load onto delivery vehicle in ${receiverCity}`,
        location: `${receiverCity} delivery route`,
        description: `Out for delivery to ${receiverName}`,
      }
    case 'DELIVERED':
      return {
        buttonLabel: `Confirm delivery to ${receiverName}`,
        location: shipment.receiver?.street ?? receiverCity,
        description: `Delivered to ${receiverName}`,
      }
    default:
      return { buttonLabel: eventType, location: '', description: '' }
  }
}

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

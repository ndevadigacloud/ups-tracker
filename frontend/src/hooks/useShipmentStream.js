import { useEffect, useRef, useState } from 'react'

/**
 * Subscribes to the shipment's SSE stream and keeps the latest status +
 * accumulated tracking events in state, so callers don't have to poll.
 */
export function useShipmentStream(shipmentId, initialEvents = []) {
  const [status, setStatus] = useState(null)
  const [events, setEvents] = useState(initialEvents)
  const eventsRef = useRef(initialEvents)

  useEffect(() => {
    setEvents(initialEvents)
    eventsRef.current = initialEvents
  }, [shipmentId])

  useEffect(() => {
    if (!shipmentId) return undefined

    const source = new EventSource(`/api/shipments/${shipmentId}/stream`)

    source.addEventListener('status', (e) => {
      setStatus(JSON.parse(e.data))
    })

    source.addEventListener('tracking-event', (e) => {
      const next = [...eventsRef.current, JSON.parse(e.data)]
      eventsRef.current = next
      setEvents(next)
    })

    source.onerror = () => {
      // Browser auto-retries; nothing to do here beyond letting it reconnect.
    }

    return () => source.close()
  }, [shipmentId])

  return { status, events }
}

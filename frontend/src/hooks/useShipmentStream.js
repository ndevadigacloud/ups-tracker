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

    console.debug(`[sse] connecting for shipment ${shipmentId}`)
    const source = new EventSource(`/api/shipments/${shipmentId}/stream`)

    source.onopen = () => console.debug(`[sse] connected for shipment ${shipmentId}`)

    source.addEventListener('status', (e) => {
      console.debug(`[sse] status event for shipment ${shipmentId}:`, e.data)
      setStatus(JSON.parse(e.data))
    })

    source.addEventListener('tracking-event', (e) => {
      console.debug(`[sse] tracking-event for shipment ${shipmentId}:`, e.data)
      const next = [...eventsRef.current, JSON.parse(e.data)]
      eventsRef.current = next
      setEvents(next)
    })

    source.onerror = () => {
      // Browser auto-retries; nothing to do here beyond letting it reconnect.
      console.warn(`[sse] connection error for shipment ${shipmentId}, browser will retry`)
    }

    return () => {
      console.debug(`[sse] closing stream for shipment ${shipmentId}`)
      source.close()
    }
  }, [shipmentId])

  return { status, events }
}

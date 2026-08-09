/**
 * Turns a bare eventType + the shipment/facility data into the label a real
 * driver-facing scanner app would show - so an action button reads like
 * "Depart Louisville Air Hub" instead of a raw enum name. Shared by the
 * tracking page and the facility ops view so both stay in sync.
 */
export function describeStep(eventType, shipment, facilitiesById) {
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

/** Which facility id a given (in-progress) scan step physically happens at. */
export function facilityIdForStep(eventType, shipment) {
  if (eventType === 'ARRIVED_AT_HUB' || eventType === 'DEPARTED_HUB') return shipment.originFacilityId
  if (eventType === 'OUT_FOR_DELIVERY' || eventType === 'DELIVERED') return shipment.destinationFacilityId
  return null
}

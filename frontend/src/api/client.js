async function request(url, options) {
  const res = await fetch(url, options)
  if (!res.ok) {
    const body = await res.text().catch(() => '')
    throw new Error(`${res.status} ${res.statusText}: ${body}`)
  }
  if (res.status === 204) return null
  return res.json()
}

export const api = {
  createShipment: (payload) =>
    request('/api/shipments', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }),
  listShipments: ({ status, page = 0, size = 20 } = {}) => {
    const params = new URLSearchParams({ page, size })
    if (status) params.set('status', status)
    return request(`/api/shipments?${params}`)
  },
  getShipment: (id) => request(`/api/shipments/${id}`),
  getTrackingEvents: (id) => request(`/api/shipments/${id}/tracking-events`),
  getDashboard: () => request('/api/shipments/dashboard'),
  simulateScan: (id, payload) =>
    request(`/api/simulate/shipments/${id}/scan`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }),
  listFacilities: () => request('/api/facilities'),
}

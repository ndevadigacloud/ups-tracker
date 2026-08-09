async function request(url, options) {
  const method = options?.method ?? 'GET'
  const start = performance.now()
  console.debug(`[api] -> ${method} ${url}`)

  const res = await fetch(url, options)
  const elapsedMs = Math.round(performance.now() - start)

  if (!res.ok) {
    const body = await res.text().catch(() => '')
    console.error(`[api] <- ${method} ${url} ${res.status} (${elapsedMs}ms): ${body}`)
    throw new Error(`${res.status} ${res.statusText}: ${body}`)
  }

  console.debug(`[api] <- ${method} ${url} ${res.status} (${elapsedMs}ms)`)
  if (res.status === 204 || res.status === 202) return null
  const text = await res.text()
  return text ? JSON.parse(text) : null
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
  getNextScanStep: (id) => request(`/api/shipments/${id}/next-scan-step`),
  getDashboard: () => request('/api/shipments/dashboard'),
  simulateScan: (id, payload) =>
    request(`/api/simulate/shipments/${id}/scan`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    }),
  listFacilities: () => request('/api/facilities'),
}

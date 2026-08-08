import { Suspense, lazy } from 'react'
import { Routes, Route, NavLink } from 'react-router-dom'

const CreateShipment = lazy(() => import('./pages/CreateShipment.jsx'))
const ShipmentList = lazy(() => import('./pages/ShipmentList.jsx'))
const TrackShipment = lazy(() => import('./pages/TrackShipment.jsx'))
const Dashboard = lazy(() => import('./pages/Dashboard.jsx'))
const Facilities = lazy(() => import('./pages/Facilities.jsx'))

export default function App() {
  return (
    <div className="app">
      <nav className="nav">
        <span className="brand">UPS Tracker</span>
        <NavLink to="/" end>New Shipment</NavLink>
        <NavLink to="/shipments">Shipments</NavLink>
        <NavLink to="/dashboard">Dashboard</NavLink>
        <NavLink to="/facilities">Facilities</NavLink>
      </nav>
      <main className="content">
        <Suspense fallback={<p>Loading…</p>}>
          <Routes>
            <Route path="/" element={<CreateShipment />} />
            <Route path="/shipments" element={<ShipmentList />} />
            <Route path="/shipments/:id" element={<TrackShipment />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/facilities" element={<Facilities />} />
          </Routes>
        </Suspense>
      </main>
    </div>
  )
}

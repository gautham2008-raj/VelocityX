const inrFormatter = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0
});

let leafletMap = null;
let telemetryMarkers = {};
let mapUpdateInterval = null;

const transitCorridors = [
  { lat: 17.3850, lng: 78.4867, name: 'Hyderabad Central Corridor' },
  { lat: 17.4399, lng: 78.4983, name: 'Secunderabad Military Cantonment' },
  { lat: 17.4435, lng: 78.3772, name: 'HITEC Cyberabad Expressway' },
  { lat: 17.4156, lng: 78.4354, name: 'Banjara Hills Transit' },
  { lat: 17.4933, lng: 78.3914, name: 'Kukatpally Logistics Hub' },
  { lat: 17.2403, lng: 78.4294, name: 'RGIA Airport Outer Ring Road' }
];

document.addEventListener('DOMContentLoaded', async () => {
  const user = await AuthEngine.checkSession();
  if (!user || user.role !== 'ADMIN') {
    alert('Access Denied: Root Operations clearance required.');
    window.location.href = 'admin-login.html';
    return;
  }

  document.getElementById('adminName').innerText = user.name;
  loadTelemetry();
  loadAdminBookings();
  populateModelSelects();
  bindProvisioningForms();
});

function switchTab(tabId) {
  document.querySelectorAll('.sidebar-btn').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('section[id^="tab-"]').forEach(s => s.style.display = 'none');

  const activeBtn = document.querySelector(`.sidebar-btn[href="#${tabId}"]`);
  if (activeBtn) activeBtn.classList.add('active');

  const target = document.getElementById(`tab-${tabId}`);
  if (target) target.style.display = 'block';

  if (tabId === 'overview') loadTelemetry();
  if (tabId === 'bookings') loadAdminBookings();
  if (tabId === 'livemap') initLiveMap();
}

function initLiveMap() {
  if (!leafletMap) {
    leafletMap = L.map('liveGpsMap').setView([17.4399, 78.4983], 12);

    // Standard OpenStreetMap (100% Free, NO API KEY REQUIRED)
    const osmLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(leafletMap);

    // Dark cyberpunk filter applied directly to tiles via CSS
    osmLayer.getContainer().style.filter = 'invert(100%) hue-rotate(180deg) brightness(85%) contrast(120%)';
  }

  setTimeout(() => {
    leafletMap.invalidateSize();
  }, 200);

  updateFleetMapTelemetry();

  if (mapUpdateInterval) clearInterval(mapUpdateInterval);
  mapUpdateInterval = setInterval(updateFleetMapTelemetry, 4000);
}

async function updateFleetMapTelemetry() {
  try {
    const res = await fetch('/api/admin/bookings');
    const bookings = await res.json();
    const active = bookings.filter(b => b.status === 'CONFIRMED' || b.status === 'ACTIVE');

    const transponderList = document.getElementById('transponderList');
    if (transponderList) transponderList.innerHTML = '';

    if (active.length === 0) {
      if (transponderList) {
        transponderList.innerHTML = '<span style="color:var(--text-muted); grid-column:1/-1;">No vehicles currently checked out on road.</span>';
      }
      return;
    }

    active.forEach((b, idx) => {
      const baseNode = transitCorridors[idx % transitCorridors.length];
      
      const deltaLat = (Math.sin(Date.now() / 3000 + idx) * 0.008);
      const deltaLng = (Math.cos(Date.now() / 3000 + idx) * 0.008);
      const currentLat = baseNode.lat + deltaLat;
      const currentLng = baseNode.lng + deltaLng;
      const speedKmh = Math.floor(45 + Math.abs(Math.sin(Date.now() / 1500 + idx)) * 40);

      const customIcon = L.divIcon({
        className: 'custom-gps-pin',
        html: `
          <div style="background:#00f0ff; width:16px; height:16px; border-radius:50%; box-shadow:0 0 15px #00f0ff; border:2px solid #ffffff;"></div>
        `,
        iconSize: [16, 16],
        iconAnchor: [8, 8]
      });

      if (telemetryMarkers[b.bookingId]) {
        telemetryMarkers[b.bookingId].setLatLng([currentLat, currentLng]);
      } else {
        const marker = L.marker([currentLat, currentLng], { icon: customIcon }).addTo(leafletMap);
        marker.bindPopup(`
          <strong style="color:var(--accent-cyan); font-size:1rem;">${escapeHtml(b.vehicleName)}</strong><br>
          Plate: <code>${escapeHtml(b.regNumber)}</code><br>
          Driver: ${escapeHtml(b.customerName)}<br>
          Telemetry: <span style="color:#10b981;">Online &bull; Moving</span>
        `);
        telemetryMarkers[b.bookingId] = marker;
      }

      if (transponderList) {
        transponderList.innerHTML += `
          <div style="background:rgba(0,0,0,0.3); border:1px solid var(--border-glass); border-radius:8px; padding:0.75rem;">
            <div style="display:flex; justify-content:space-between; margin-bottom:0.25rem;">
              <strong style="font-size:0.9rem;">${escapeHtml(b.regNumber)}</strong>
              <span style="color:var(--accent-cyan); font-weight:700;">${speedKmh} km/h</span>
            </div>
            <p style="font-size:0.8rem; color:var(--text-muted);">${escapeHtml(b.vehicleName)}</p>
            <small style="color:var(--text-dim); display:block; margin-top:4px;">Node: ${baseNode.name}</small>
          </div>
        `;
      }
    });

  } catch (e) {
    console.error('Map telemetry update failed:', e);
  }
}

async function loadTelemetry() {
  try {
    const res = await fetch('/api/admin/metrics');
    const m = await res.json();
    if (!res.ok) throw new Error(m.error || 'Telemetry offline');

    document.getElementById('metricTotalUnits').innerText = m.totalUnits;
    document.getElementById('metricAvailable').innerText = m.availableUnits;
    document.getElementById('metricRented').innerText = m.rentedUnits;
    document.getElementById('metricUsers').innerText = m.totalUsers;
    document.getElementById('metricRevenue').innerText = inrFormatter.format(m.totalRevenueINR || 0);
  } catch (e) {
    console.error('[Admin] Telemetry error:', e);
  }
}

async function loadAdminBookings() {
  const tbody = document.getElementById('adminBookingsTbody');
  tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;">Loading reservations...</td></tr>';

  try {
    const res = await fetch('/api/admin/bookings');
    const bookings = await res.json();
    if (!res.ok) throw new Error(bookings.error || 'Failed to load bookings');

    if (bookings.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; color:var(--text-muted);">No recorded rental bookings found.</td></tr>';
      return;
    }

    tbody.innerHTML = bookings.map(b => {
      const isRented = b.status === 'CONFIRMED' || b.status === 'ACTIVE';
      const statusBadge = isRented
        ? '<span class="badge badge-rented">ACTIVE RENTAL</span>'
        : '<span class="badge badge-available">RETURNED</span>';

      return `
        <tr>
          <td>#${b.bookingId}</td>
          <td>
            <strong>${escapeHtml(b.customerName)}</strong><br>
            <small style="color:var(--text-muted);">${escapeHtml(b.customerEmail)}</small>
          </td>
          <td>${escapeHtml(b.vehicleName)}</td>
          <td><code>${escapeHtml(b.regNumber)}</code></td>
          <td>${b.startDate} &rarr; ${b.endDate}</td>
          <td style="color:var(--accent-cyan); font-weight:600;">${inrFormatter.format(b.grandTotal)}</td>
          <td>${statusBadge}</td>
          <td>
            ${isRented ? `
              <button class="btn btn-outline" style="padding:0.3rem 0.6rem; font-size:0.75rem;" onclick="processReturn(${b.bookingId})">
                Check In / Return
              </button>
            ` : '<span style="color:var(--text-muted); font-size:0.8rem;">Completed</span>'}
          </td>
        </tr>
      `;
    }).join('');
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="8" style="color:#f87171;">Error: ${e.message}</td></tr>`;
  }
}

async function processReturn(bookingId) {
  const kmInput = prompt('Enter additional kilometers driven during this rental:', '50');
  if (kmInput === null) return;
  const addedKm = parseInt(kmInput, 10) || 0;

  try {
    const res = await fetch('/api/bookings/return', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ bookingId, addedKm })
    });
    const data = await res.json();
    if (!res.ok || !data.success) {
      throw new Error(data.error || 'Return check-in failed');
    }

    alert('Vehicle successfully checked in. Unit is now AVAILABLE in inventory.');
    loadAdminBookings();
    loadTelemetry();
    if (telemetryMarkers[bookingId]) {
      leafletMap.removeLayer(telemetryMarkers[bookingId]);
      delete telemetryMarkers[bookingId];
    }
  } catch (e) {
    alert('Return Error: ' + e.message);
  }
}

async function populateModelSelects() {
  try {
    const res = await fetch('/api/fleet');
    const fleet = await res.json();

    const options = fleet.map(m => `
      <option value="${m.modelId}">${escapeHtml(m.modelName)} (${m.brand} - ${m.category})</option>
    `).join('');

    const singleSelect = document.getElementById('singleModelSelect');
    const bulkSelect = document.getElementById('bulkModelSelect');

    if (singleSelect) singleSelect.innerHTML = options;
    if (bulkSelect) bulkSelect.innerHTML = options;
  } catch (e) {
    console.error('Failed to populate vehicle model options:', e);
  }
}

function bindProvisioningForms() {
  document.getElementById('singleUnitForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const modelId = document.getElementById('singleModelSelect').value;
    const registrationNumber = document.getElementById('singleRegNum').value;
    const dailyRate = document.getElementById('singleRate').value;
    const odometerKm = document.getElementById('singleOdo').value;

    try {
      const res = await fetch('/api/admin/inventory/add', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ modelId, registrationNumber, dailyRate, odometerKm })
      });
      const data = await res.json();
      if (!res.ok || !data.success) throw new Error(data.error || 'Provisioning failed');

      alert(data.message);
      document.getElementById('singleRegNum').value = '';
      loadTelemetry();
    } catch (ex) {
      alert('Error: ' + ex.message);
    }
  });

  document.getElementById('bulkUnitForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const modelId = document.getElementById('bulkModelSelect').value;
    const regPrefix = document.getElementById('bulkPrefix').value;
    const count = document.getElementById('bulkCount').value;
    const dailyRate = document.getElementById('bulkRate').value;

    try {
      const res = await fetch('/api/admin/inventory/bulk-add', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ modelId, regPrefix, count, dailyRate })
      });
      const data = await res.json();
      if (!res.ok || !data.success) throw new Error(data.error || 'Batch provisioning failed');

      alert(data.message);
      loadTelemetry();
    } catch (ex) {
      alert('Error: ' + ex.message);
    }
  });
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/[&<>'"]/g, t => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    "'": '&#39;',
    '"': '&quot;'
  }[t] || t));
}
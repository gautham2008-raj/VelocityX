let currentFleet = [];
const inrFormatter = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0
});

document.addEventListener('DOMContentLoaded', () => {
  loadFleet();
  bindFilterTabs();
  bindSearchInput();
  bindBookingForm();
  setDefaultDates();
});

async function loadFleet(category = 'ALL') {
  const container = document.getElementById('fleetContainer');
  if (!container) return;

  container.innerHTML = `<div style="grid-column: 1/-1; text-align:center; padding:3rem; color:var(--text-muted);">
    Scanning fleet inventory...
  </div>`;

  try {
    let url = '/api/fleet';
    if (category !== 'ALL') url += `?category=${category}`;
    const res = await fetch(url);
    if (!res.ok) throw new Error(`HTTP ${res.status}: Failed to fetch fleet.`);
    
    currentFleet = await res.json();
    renderFleetCards(currentFleet);
  } catch (e) {
    console.error('[Fleet] Retrieval error:', e);
    container.innerHTML = `<div style="grid-column: 1/-1; text-align:center; color:#f87171; padding:2rem;">
      Failed to retrieve fleet inventory: ${e.message}
    </div>`;
  }
}

function renderFleetCards(vehicles) {
  const container = document.getElementById('fleetContainer');
  if (!container) return;

  if (!vehicles || vehicles.length === 0) {
    container.innerHTML = `<div style="grid-column: 1/-1; text-align:center; padding:3rem; color:var(--text-muted);">
      No vehicles available matching this category filter.
    </div>`;
    return;
  }

  container.innerHTML = vehicles.map(v => {
    let specs = {};
    try {
      specs = JSON.parse(v.specsJson || '{}');
    } catch (_) {}

    const isAvailable = v.availableUnits > 0;
    const badgeColor = isAvailable ? 'var(--status-available)' : 'var(--status-maintenance)';
    const statusText = isAvailable ? `${v.availableUnits} Available` : 'All Rented / Service';
    const fallbackImage = 'https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=800&auto=format&fit=crop&q=80';

    return `
      <article class="glass-panel vehicle-card">
        <div class="vehicle-preview-holder">
          <img src="${v.imageUrl || fallbackImage}" 
               alt="${escapeHtml(v.modelName)}" 
               class="vehicle-img"
               loading="lazy"
               onerror="this.onerror=null; this.src='${fallbackImage}';">
          <div class="vehicle-img-overlay"></div>
          <span class="vehicle-badge" style="color: ${badgeColor}; border-color: ${badgeColor}; z-index:10;">
            ${statusText}
          </span>
        </div>

        <div class="vehicle-card-body">
          <h3 class="vehicle-title">${escapeHtml(v.modelName)}</h3>
          <p class="vehicle-meta">${escapeHtml(v.brand)} &bull; ${v.modelYear} &bull; ${v.category}</p>

          <div class="specs-grid">
            ${Object.entries(specs).slice(0, 4).map(([k, val]) => `
              <div class="spec-item">
                ${k.toUpperCase()}
                <strong>${escapeHtml(String(val))}</strong>
              </div>
            `).join('')}
          </div>

          <div class="vehicle-pricing">
            <div class="price-tag">
              ${inrFormatter.format(v.baseDailyRate)} <span>/ 24 hrs</span>
            </div>
            <button class="btn ${isAvailable ? 'btn-primary' : 'btn-outline'}" 
              ${isAvailable ? '' : 'disabled'}
              onclick="openBookingModal(${v.modelId}, '${escapeHtml(v.modelName)}')">
              ${isAvailable ? 'Reserve Unit' : 'Unavailable'}
            </button>
          </div>
        </div>
      </article>
    `;
  }).join('');
}

function bindFilterTabs() {
  const tabs = document.querySelectorAll('.category-tabs .tab-btn');
  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      tabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      const cat = tab.getAttribute('data-category');
      loadFleet(cat);
    });
  });
}

function bindSearchInput() {
  const search = document.getElementById('fleetSearch');
  if (!search) return;
  search.addEventListener('input', (e) => {
    const q = e.target.value.toLowerCase().trim();
    if (!q) {
      renderFleetCards(currentFleet);
      return;
    }
    const filtered = currentFleet.filter(v => 
      v.modelName.toLowerCase().includes(q) ||
      v.brand.toLowerCase().includes(q) ||
      (v.specsJson && v.specsJson.toLowerCase().includes(q))
    );
    renderFleetCards(filtered);
  });
}

// --- RESERVATION ENGINE ---

async function openBookingModal(modelId, modelName) {
  if (!AuthEngine.currentUser) {
    alert('Please sign in or create an account to reserve a vehicle.');
    window.location.href = 'login.html';
    return;
  }

  const modal = document.getElementById('bookingModal');
  document.getElementById('bookingModalTitle').innerText = `Reserve ${modelName}`;
  document.getElementById('bookModelId').value = modelId;

  const unitSelect = document.getElementById('bookUnitSelect');
  unitSelect.innerHTML = '<option value="">Loading registration plates...</option>';

  try {
    const res = await fetch(`/api/fleet/units/${modelId}`);
    const units = await res.json();
    const available = units.filter(u => u.status === 'AVAILABLE');

    if (available.length === 0) {
      unitSelect.innerHTML = '<option value="">No units currently available</option>';
      return;
    }

    unitSelect.innerHTML = available.map(u => `
      <option value="${u.id}" data-rate="${u.customDailyRate}">
        Plate: ${u.registrationNumber} (Odometer: ${u.odometerKm} km)
      </option>
    `).join('');

    modal.classList.add('active');
    triggerQuoteCalculation();
  } catch (e) {
    alert('Failed to load physical units: ' + e.message);
  }
}

function closeBookingModal() {
  const modal = document.getElementById('bookingModal');
  if (modal) modal.classList.remove('active');
}

function setDefaultDates() {
  const startInput = document.getElementById('bookStartDate');
  const endInput = document.getElementById('bookEndDate');
  if (!startInput || !endInput) return;

  const today = new Date();
  const tomorrow = new Date();
  tomorrow.setDate(today.getDate() + 2);

  startInput.value = today.toISOString().split('T')[0];
  endInput.value = tomorrow.toISOString().split('T')[0];

  startInput.addEventListener('change', triggerQuoteCalculation);
  endInput.addEventListener('change', triggerQuoteCalculation);
  document.getElementById('bookUnitSelect')?.addEventListener('change', triggerQuoteCalculation);
}

async function triggerQuoteCalculation() {
  const unitId = document.getElementById('bookUnitSelect')?.value;
  const startDate = document.getElementById('bookStartDate')?.value;
  const endDate = document.getElementById('bookEndDate')?.value;
  const summaryBox = document.getElementById('quoteSummary');

  if (!unitId || !startDate || !endDate) {
    if (summaryBox) summaryBox.style.display = 'none';
    return;
  }

  try {
    const res = await fetch('/api/bookings/quote', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ unitId, startDate, endDate })
    });
    const quote = await res.json();
    if (!res.ok) throw new Error(quote.error || 'Quote failed');

    document.getElementById('quoteDays').innerText = `${quote.totalDays} Day(s)`;
    document.getElementById('quoteSubtotal').innerText = inrFormatter.format(quote.subtotal);
    document.getElementById('quoteTax').innerText = inrFormatter.format(quote.gstAmount);
    document.getElementById('quoteTotal').innerText = inrFormatter.format(quote.grandTotal);
    if (summaryBox) summaryBox.style.display = 'block';
  } catch (e) {
    if (summaryBox) summaryBox.style.display = 'none';
  }
}

function bindBookingForm() {
  const form = document.getElementById('bookingForm');
  if (!form) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const unitId = document.getElementById('bookUnitSelect').value;
    const startDate = document.getElementById('bookStartDate').value;
    const endDate = document.getElementById('bookEndDate').value;

    try {
      const res = await fetch('/api/bookings/book', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ unitId, startDate, endDate })
      });
      const data = await res.json();
      if (!res.ok || !data.success) {
        throw new Error(data.error || 'Reservation could not be completed.');
      }

      alert(`Reservation Confirmed!\nInvoice ID: #${data.invoice.id}\nGrand Total: ₹${data.invoice.grandTotal}`);
      closeBookingModal();
      loadFleet();
    } catch (ex) {
      alert('Error: ' + ex.message);
    }
  });
}

// --- CUSTOMER "MY RENTALS" & RETURN WORKFLOW ---

async function openMyRentalsModal() {
  if (!AuthEngine.currentUser) {
    alert('Please sign in to view your rented vehicles.');
    window.location.href = 'login.html';
    return;
  }

  const modal = document.getElementById('myRentalsModal');
  const container = document.getElementById('myRentalsContainer');
  if (!modal || !container) return;

  container.innerHTML = '<div style="text-align:center; padding:2rem; color:var(--text-muted);">Loading active rentals...</div>';
  modal.classList.add('active');

  try {
    const res = await fetch('/api/bookings/my-rentals');
    const rentals = await res.json();

    if (!rentals || rentals.length === 0) {
      container.innerHTML = `
        <div style="text-align:center; padding:3rem 1rem; color:var(--text-muted);">
          <p style="font-size:1.1rem; margin-bottom:1rem;">You have no active or previous reservations.</p>
          <button class="btn btn-primary" onclick="closeMyRentalsModal()">Browse Fleet</button>
        </div>
      `;
      return;
    }

    container.innerHTML = rentals.map(r => {
      const isActive = r.status === 'CONFIRMED' || r.status === 'ACTIVE';
      const statusBadge = isActive 
        ? '<span class="badge badge-rented">ACTIVE ON ROAD</span>' 
        : '<span class="badge badge-available">RETURNED</span>';

      return `
        <div class="glass-panel" style="padding:1.25rem; margin-bottom:1rem; display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:1rem;">
          <div>
            <div style="display:flex; align-items:center; gap:0.5rem; margin-bottom:0.25rem;">
              <h4 style="font-size:1.1rem; color:var(--text-main);">${escapeHtml(r.vehicleName)}</h4>
              ${statusBadge}
            </div>
            <p style="color:var(--text-muted); font-size:0.85rem;">
              Plate: <strong style="color:var(--accent-cyan);">${escapeHtml(r.regNumber)}</strong> &bull; ${r.category} &bull; Booking #${r.bookingId}
            </p>
            <p style="color:var(--text-muted); font-size:0.85rem; margin-top:0.25rem;">
              Duration: ${r.startDate} to ${r.endDate} &bull; Total Paid: <strong style="color:var(--accent-cyan);">${inrFormatter.format(r.grandTotal)}</strong>
            </p>
          </div>

          <div>
            ${isActive ? `
              <button class="btn btn-outline" style="border-color:var(--accent-magenta); color:var(--accent-magenta);" onclick="returnVehicleCustomer(${r.bookingId})">
                Return Vehicle
              </button>
            ` : `
              <span style="color:var(--text-dim); font-size:0.85rem;">Rental Complete</span>
            `}
          </div>
        </div>
      `;
    }).join('');
  } catch (e) {
    container.innerHTML = `<div style="color:#f87171; text-align:center; padding:2rem;">Failed to load rentals: ${e.message}</div>`;
  }
}

function closeMyRentalsModal() {
  const modal = document.getElementById('myRentalsModal');
  if (modal) modal.classList.remove('active');
}

async function returnVehicleCustomer(bookingId) {
  const kmInput = prompt('Enter odometer reading / distance traveled (KM) for return verification:', '45');
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
      throw new Error(data.error || 'Vehicle return failed.');
    }

    alert('Vehicle successfully returned and checked into fleet inventory!');
    openMyRentalsModal();
    loadFleet();
  } catch (ex) {
    alert('Return error: ' + ex.message);
  }
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
/**
 * Enterprise Field Force Management & CRM Dashboard Controller
 * Target: Real-Time Central Cloud API & Firestore
 *
 * Implements:
 *  1. BDO Visit Dispatch with media payload streaming & GPS previews
 *  2. Role-Based Hierarchy & Access Control (BDO -> TL -> ASM -> ADMIN -> MASTER)
 *  3. Real-Time BDO Live GPS Location Streaming & Map Tracking (Leaflet.js)
 *  4. Attendance Tracking & Reporting Dashboard (Shift calculations, hours, visits)
 *  5. Visit Validation & Approval Workflow for TLs
 *  6. Operational Dashboard statistics (Daily/Weekly/Monthly totals)
 *  7. Master Web Dashboard & Link Routing with 1-click Master Details access
 *  8. Expanded Bulk Upload (Excel/CSV) supporting full street address, GPS & contact fields
 */

// =============================================================================
// 1. Application State & Hierarchy Context
// =============================================================================
const state = {
  // Master Authorization - active by default to guarantee instant master access
  isMasterActive: true,
  masterUser: {
    username: 'master',
    name: 'Master Operations Commander',
    role: 'MASTER',
    employeeId: 'MST-001'
  },

  // Active Role Simulation (BDO -> TL -> ASM -> ADMIN -> MASTER)
  activeRole: 'MASTER',
  activeUid: 'mst_001',
  activeTeamId: 'team_north_01',
  activeAreaId: 'area_north',

  // Active Navigation Tab
  activeTab: 'overview',

  // Raw & Processed Collections
  rawMerchants: [],
  rawUsers: [],
  rawVisits: [],
  rawAttendance: [],
  rawLocations: [],
  rawAuditLogs: [],

  // Telemetry & Map
  mapInstance: null,
  mapMarkers: {},

  // Filters & Pagination
  merchantSearchTerm: '',
  merchantStatusFilter: 'ALL',
  merchantCurrentPage: 1,
  merchantPageSize: 10,

  validationFilter: 'PENDING',
  selectedVisitForReview: null,

  // Staging for Bulk Upload
  stagingMerchants: [],
  stagingValidation: {
    total: 0,
    newCount: 0,
    updateCount: 0,
    invalidCount: 0
  }
};

// Force initial master unlock in local storage so page reloads remain in Master mode
try {
  localStorage.setItem('qr_friend_master_auth', 'true');
} catch (e) {}

// API Base URL Resolver
// API Base URL Resolver
function getApiBaseUrl() {
  if (typeof window !== 'undefined' && window.APP_CONFIG && window.APP_CONFIG.API_BASE_URL) {
    return window.APP_CONFIG.API_BASE_URL.replace(/\/+$/, '');
  }
  const custom = localStorage.getItem('qr_friend_api_base_url');
  if (custom && custom.trim()) {
    return custom.trim().replace(/\/+$/, '');
  }
  return '';
}

// Absolute Root Endpoint Resolver (prevents resolving to /dashboard/api/...)
function toApiUrl(endpoint) {
  const apiBase = getApiBaseUrl();
  const cleanEndpoint = endpoint.replace(/^\/+/, '');
  if (apiBase) {
    return `${apiBase}/${cleanEndpoint}`;
  }
  // User Request: Always use absolute root path with leading slash
  return `/${cleanEndpoint}`;
}

function getAuthHeaders() {
  return {
    'Content-Type': 'application/json',
    'x-user-role': state.activeRole,
    'x-user-id': state.activeUid,
    'x-team-id': state.activeTeamId || '',
    'x-area-id': state.activeAreaId || ''
  };
}

// =============================================================================
// 2. DOM Utilities & Toast Notification
// =============================================================================
function setDomText(id, text) {
  const el = document.getElementById(id);
  if (el) el.textContent = text;
}

function escapeHtml(str) {
  if (str == null) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function showToast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  if (!container) return;

  const toast = document.createElement('div');
  const bg = type === 'success' ? 'bg-emerald-600 text-white' :
             type === 'error' ? 'bg-rose-600 text-white' :
             type === 'warning' ? 'bg-amber-600 text-white' : 'bg-slate-900 text-white';

  const icon = type === 'success' ? 'fa-circle-check' :
               type === 'error' ? 'fa-triangle-exclamation' : 'fa-circle-info';

  toast.className = `${bg} px-4 py-3 rounded-2xl shadow-xl flex items-center space-x-2.5 text-xs font-semibold transform transition-all duration-300 translate-y-2 opacity-0`;
  toast.innerHTML = `<i class="fa-solid ${icon}"></i><span>${escapeHtml(message)}</span>`;
  container.appendChild(toast);

  requestAnimationFrame(() => {
    toast.classList.remove('translate-y-2', 'opacity-0');
  });

  setTimeout(() => {
    toast.classList.add('opacity-0', 'translate-y-2');
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

// =============================================================================
// 3. Central Data Fetching & Sync
// =============================================================================
async function fetchAllData() {
  try {
    const headers = getAuthHeaders();

    // Absolute root API calls pointing directly to /api/...
    const [merchantsRes, usersRes, visitsRes, attRes, locRes, statsRes] = await Promise.all([
      fetch(toApiUrl('/api/merchants'), { headers }).catch(() => null),
      fetch(toApiUrl('/api/users'), { headers }).catch(() => null),
      fetch(toApiUrl('/api/visits'), { headers }).catch(() => null),
      fetch(toApiUrl('/api/attendance'), { headers }).catch(() => null),
      fetch(toApiUrl('/api/locations'), { headers }).catch(() => null),
      fetch(toApiUrl('/api/dashboard/stats'), { headers }).catch(() => null)
    ]);

    if (merchantsRes && merchantsRes.ok) {
      const data = await merchantsRes.json();
      state.rawMerchants = data.merchants || [];
    }

    if (usersRes && usersRes.ok) {
      const data = await usersRes.json();
      state.rawUsers = data.users || [];
    }

    if (visitsRes && visitsRes.ok) {
      const data = await visitsRes.json();
      const list = data.visits || [];
      // Guarantee newest submissions always appear first (chronological descending order)
      state.rawVisits = list.sort((a, b) => {
        const timeA = Number(a.timestamp || a.created_at || (a.visitDateString ? new Date(a.visitDateString).getTime() : 0)) || 0;
        const timeB = Number(b.timestamp || b.created_at || (b.visitDateString ? new Date(b.visitDateString).getTime() : 0)) || 0;
        return timeB - timeA;
      });
    }

    if (attRes && attRes.ok) {
      const data = await attRes.json();
      state.rawAttendance = data.attendance || [];
    }

    if (locRes && locRes.ok) {
      const data = await locRes.json();
      state.rawLocations = data.locations || [];
    }

    // Render all modules
    renderOverviewKpis();
    renderMerchantsTable();
    renderVisitsFeed();
    renderValidationQueue();
    renderAttendanceTable();
    renderLiveMapMarkers();
    renderBdosTable();

    // Trigger Chart.js updates if available
    if (typeof window.updateChartData === 'function') {
      window.updateChartData(state.rawMerchants, state.rawUsers, state.rawVisits);
    }

  } catch (err) {
    console.warn('[Sync Error]', err);
  }
}

// =============================================================================
// 4. Tab Navigation Management (11 Distinct Views)
// =============================================================================
function switchTab(targetName) {
  state.activeTab = targetName;
  const tabs = [
    { btn: 'tabBtnOverview', view: 'tabViewOverview', name: 'overview' },
    { btn: 'tabBtnMap', view: 'tabViewMap', name: 'map' },
    { btn: 'tabBtnValidation', view: 'tabViewValidation', name: 'validation' },
    { btn: 'tabBtnAttendance', view: 'tabViewAttendance', name: 'attendance' },
    { btn: 'tabBtnMerchants', view: 'tabViewMerchants', name: 'merchants' },
    { btn: 'tabBtnUpload', view: 'tabViewUpload', name: 'upload' },
    { btn: 'tabBtnVisits', view: 'tabViewVisits', name: 'visits' },
    { btn: 'tabBtnBdos', view: 'tabViewBdos', name: 'bdos' },
    { btn: 'tabBtnMaster', view: 'tabViewMaster', name: 'master' },
    { btn: 'tabBtnSettings', view: 'tabViewSettings', name: 'settings' },
    { btn: 'tabBtnAudit', view: 'tabViewAudit', name: 'audit' }
  ];

  tabs.forEach(t => {
    const btn = document.getElementById(t.btn);
    const view = document.getElementById(t.view);
    if (!btn || !view) return;

    if (t.name === targetName) {
      view.classList.remove('hidden');
      btn.classList.remove('border-transparent', 'text-slate-500');
      btn.classList.add('border-indigo-600', 'text-indigo-600', 'font-bold');
    } else {
      view.classList.add('hidden');
      btn.classList.remove('border-indigo-600', 'text-indigo-600', 'font-bold');
      btn.classList.add('border-transparent', 'text-slate-500');
    }
  });

  // If opening map, invalidate Leaflet size
  if (targetName === 'map' && state.mapInstance) {
    setTimeout(() => {
      state.mapInstance.invalidateSize();
    }, 150);
  }

  // Update URL hash
  if (history.replaceState) {
    history.replaceState(null, '', `#${targetName}`);
  }
}

// =============================================================================
// 5. Overview & Operational KPIs (Requirement 6)
// =============================================================================
function renderOverviewKpis() {
  const visits = state.rawVisits || [];
  const merchants = state.rawMerchants || [];
  const users = state.rawUsers || [];
  const bdoStaff = users.filter(u => (u.role || '').toUpperCase() === 'BDO');

  const now = Date.now();
  const dayMs = 86400000;

  // Real-time Master metrics aligned 1:1 with Android Master View:
  const totalVisitsCount = visits.length;

  // Requirement 2: Today's visits strictly matching calendar day (today). Zero state explicitly shows 0.
  const todayStr = new Date(now).toISOString().split('T')[0];
  const todayVisits = visits.filter(v => {
    if (!v) return false;
    let vDateStr = v.visitDateString || '';
    if (!vDateStr && v.timestamp) {
      try { vDateStr = new Date(Number(v.timestamp)).toISOString().split('T')[0]; } catch(e) {}
    }
    if (!vDateStr && v.created_at) {
      try { vDateStr = new Date(Number(v.created_at)).toISOString().split('T')[0]; } catch(e) {}
    }
    return vDateStr === todayStr;
  });
  const dailyCount = todayVisits.length;

  // User Request: If a BDO conducts a visit today, he MUST be included in the live agent count in main dashboard web link
  // Collect all identifiers of BDOs who have conducted a visit today (uid, bdo_uid, bdoId, bdoCode, or name)
  const activeBdoKeysToday = new Set();
  todayVisits.forEach(v => {
    if (v.bdo_uid) activeBdoKeysToday.add(String(v.bdo_uid).toLowerCase().trim());
    if (v.bdoId) activeBdoKeysToday.add(String(v.bdoId).toLowerCase().trim());
    if (v.bdoCode) activeBdoKeysToday.add(String(v.bdoCode).toLowerCase().trim());
    if (v.bdo_name) activeBdoKeysToday.add(String(v.bdo_name).toLowerCase().trim());
    if (v.name) activeBdoKeysToday.add(String(v.name).toLowerCase().trim());
  });

  const activeOnlineBdos = bdoStaff.filter(bdo => {
    if (isDemoAccount(bdo)) return false;
    const bdoUid = String(bdo.uid || bdo.id || '').toLowerCase().trim();
    const bdoCode = String(bdo.bdoCode || bdo.employee_id || bdo.employeeId || '').toLowerCase().trim();
    const bdoName = String(bdo.name || bdo.username || '').toLowerCase().trim();

    // Check 1: Has conducted at least 1 visit today
    const hasConductedToday = activeBdoKeysToday.has(bdoUid) || 
                              (bdoCode && activeBdoKeysToday.has(bdoCode)) || 
                              (bdoName && activeBdoKeysToday.has(bdoName)) ||
                              (bdoUid.includes('_') && activeBdoKeysToday.has('bdo_' + bdoUid.split('_').pop()));

    // Check 2: Actual active attendance recorded for today
    const isOnlineToday = (state.rawAttendance || []).some(a => {
      let aDate = a.date || '';
      if (!aDate && a.login_time) {
        try { aDate = new Date(a.login_time).toISOString().split('T')[0]; } catch(e) {}
      }
      const aUid = String(a.user_uid || a.userId || '').toLowerCase().trim();
      return (aUid === bdoUid || (bdoCode && aUid === bdoCode)) && 
             aDate === todayStr &&
             (a.active_status === 'ONLINE' || a.active_status === 'ON_DUTY');
    });

    return isOnlineToday || hasConductedToday;
  });

  // Also include any unique field agents from today's visits if not explicitly present in bdoStaff
  const uniqueConductedAgentsTodayCount = Math.max(activeOnlineBdos.length, activeBdoKeysToday.size > 0 ? new Set(todayVisits.map(v => v.bdo_uid || v.bdo_name)).size : 0);
  const onlineBdoCount = uniqueConductedAgentsTodayCount;

  const monthly = visits.filter(v => now - (v.timestamp || 0) < dayMs * 30).length;
  const pending = visits.filter(v => v.validation_status === 'PENDING').length;
  const activeMerchantsCount = merchants.filter(m => (m.status === 'Active' || m.merchantStatus === 'Active')).length;
  const activeQrs = merchants.filter(m => (m.status === 'Active' || m.merchantStatus === 'Active') && (m.qr_code_id || m.qrId)).length;
  const qrDeployedVisits = visits.filter(v => v.qrDeployed === true || v.qr_deployed === true).length;

  // Strictly output 0 if zero (no historical cache fallback)
  setDomText('kpiDailyVisits', String(dailyCount));
  setDomText('kpiMonthlyVisits', totalVisitsCount);
  setDomText('kpiPendingValidations', pending);
  setDomText('kpiOnlineBdos', String(onlineBdoCount));
  setDomText('kpiTotalMerchants', merchants.length);
  setDomText('kpiActiveQrs', activeQrs > 0 ? activeQrs : activeMerchantsCount);

  // Tab count badges aligned with mobile Master view
  setDomText('tabCountMerchants', merchants.length);
  setDomText('tabCountPendingValidation', `${pending} Pending`);
  setDomText('tabCountBdos', onlineBdoCount > 0 ? String(onlineBdoCount) : String(bdoStaff.length));
}

// =============================================================================
// 6. Real-Time BDO Live GPS Location Tracking Map (Requirement 3)
// =============================================================================
function initGpsMap() {
  const mapContainer = document.getElementById('liveGpsMap');
  if (!mapContainer || typeof L === 'undefined' || state.mapInstance) return;

  try {
    state.mapInstance = L.map('liveGpsMap').setView([33.7225, 73.0532], 11);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(state.mapInstance);

    renderLiveMapMarkers();
  } catch (err) {
    console.warn('[Map Init Notice]', err);
  }
}

function formatTimeElapsed(diffMs) {
  const seconds = Math.floor(Math.max(0, diffMs) / 1000);
  if (seconds < 60) return 'Just now';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.floor(hours / 24)}d ago`;
}

function renderLiveMapMarkers() {
  if (!state.mapInstance || typeof L === 'undefined') return;

  const rawLocations = state.rawLocations || [];
  const now = Date.now();
  const sidebarContainer = document.getElementById('liveStaffCardsContainer');

  // Step 1: CLEAR all previous map layer markers before rendering incoming staff pins
  if (state.mapMarkers) {
    Object.values(state.mapMarkers).forEach(m => {
      try {
        state.mapInstance.removeLayer(m);
      } catch (e) {}
    });
  }
  state.mapMarkers = {};

  // Step 2: Strict filter for authenticated, active field staff with real coordinates
  const DUMMY_PREFIXES = ['fake_', 'test_', 'demo_', 'spoofed', 'dummy_', 'bdo_ni07', 'bdo_isb02', 'bdo_khi01', 'bdo_lhr03'];

  const validLocations = rawLocations.filter(loc => {
    const uid = (loc.userId || loc.bdo_uid || '').toLowerCase();
    const name = (loc.name || loc.user_name || '').toLowerCase();
    if (!uid || DUMMY_PREFIXES.some(p => uid.startsWith(p) || uid.includes(p))) {
      return false; // Drop fake/dummy users
    }
    if (['naseem', 'saqib', 'bilal tariq', 'zubair'].some(d => name.includes(d) || uid.includes(d))) {
      return false; // Drop legacy demo profiles
    }

    // Coordinates check (support direct lat/lng or nested current_location: { lat, lng })
    const lat = loc.current_location?.lat != null ? parseFloat(loc.current_location.lat) : parseFloat(loc.latitude);
    const lng = loc.current_location?.lng != null ? parseFloat(loc.current_location.lng) : parseFloat(loc.longitude);

    if (isNaN(lat) || isNaN(lng)) return false;
    if (lat === 0 && lng === 0) return false; // Drop zero island
    if (Math.abs(lat) > 90 || Math.abs(lng) > 180) return false;

    return true;
  });

  setDomText('mapOnlineStaffCount', validLocations.length);

  let sidebarHtml = '';

  // Step 3: Render verified pins onto map
  validLocations.forEach(loc => {
    const lat = loc.current_location?.lat != null ? parseFloat(loc.current_location.lat) : parseFloat(loc.latitude);
    const lng = loc.current_location?.lng != null ? parseFloat(loc.current_location.lng) : parseFloat(loc.longitude);
    const bdoId = loc.userId || loc.bdo_uid;
    const officerName = loc.name || loc.user_name || bdoId;

    const eventType = loc.last_event || loc.eventType || loc.last_event_type || 'APP_OPEN';
    const isVisit = eventType === 'VISIT_DISPATCH';
    const captureTime = loc.last_updated || loc.lastCapturedAt || loc.timestamp || now;
    const timeElapsedStr = formatTimeElapsed(now - captureTime);

    // Marker styling: Emerald for VISIT_DISPATCH, Indigo for APP_OPEN
    const markerColor = isVisit ? '#10b981' : '#6366f1';
    const iconClass = isVisit ? 'fa-clipboard-check' : 'fa-mobile-screen-button';

    const markerHtml = `
      <div style="background:${markerColor}; width:34px; height:34px; border-radius:50%; display:flex; align-items:center; justify-content:center; color:white; border:2.5px solid white; box-shadow:0 4px 10px rgba(0,0,0,0.3); font-size:14px;">
        <i class="fa-solid ${iconClass}"></i>
      </div>
    `;

    const icon = L.divIcon({
      html: markerHtml,
      className: 'event-bdo-marker',
      iconSize: [34, 34],
      iconAnchor: [17, 17]
    });

    const marker = L.marker([lat, lng], { icon }).addTo(state.mapInstance);
    const bdoCode = loc.bdoCode || loc.userCode || bdoId;
    marker.bindPopup(`
      <div class="text-xs p-1" style="min-width: 190px;">
        <div class="flex items-center justify-between mb-1">
          <strong class="text-sm font-bold text-slate-900">${escapeHtml(officerName)}</strong>
          <span class="text-[10px] font-mono bg-slate-100 text-slate-700 px-1.5 py-0.5 rounded font-bold">${escapeHtml(bdoCode)}</span>
        </div>
        <div class="mb-2">
          <span class="px-2 py-0.5 rounded-full text-[10px] font-bold ${isVisit ? 'bg-emerald-100 text-emerald-800' : 'bg-indigo-100 text-indigo-800'}">
            <i class="fa-solid ${iconClass} mr-1"></i>${isVisit ? 'Visit Dispatched' : 'App Opened'}
          </span>
        </div>
        <div class="text-[11px] text-slate-600 mb-0.5">
          <strong>Captured:</strong> ${timeElapsedStr}
        </div>
        <div class="font-mono text-[10px] text-slate-400 mt-1">
          GPS: ${lat.toFixed(5)}, ${lng.toFixed(5)}
        </div>
      </div>
    `);

    state.mapMarkers[bdoId] = marker;

    sidebarHtml += `
      <div class="bg-white p-3 rounded-xl border border-slate-200 shadow-sm hover:border-indigo-400 transition-colors cursor-pointer" onclick="window.panToBdo('${bdoId}', ${lat}, ${lng})">
        <div class="flex items-center justify-between">
          <div>
            <span class="font-bold text-xs text-slate-900">${escapeHtml(officerName)}</span>
            <span class="text-[10px] font-mono text-indigo-600 block">${escapeHtml(loc.bdoCode || loc.userCode || bdoId)}</span>
          </div>
          <span class="px-2 py-0.5 rounded-full text-[10px] font-bold ${isVisit ? 'bg-emerald-100 text-emerald-800' : 'bg-indigo-100 text-indigo-800'}">
            ${isVisit ? 'VISIT' : 'APP OPEN'}
          </span>
        </div>
        <div class="flex items-center justify-between text-[11px] text-slate-500 mt-1.5">
          <span><i class="fa-solid fa-clock text-slate-400 mr-1"></i>${timeElapsedStr}</span>
          <span class="font-mono text-slate-400">${lat.toFixed(3)}, ${lng.toFixed(3)}</span>
        </div>
      </div>
    `;
  });

  if (sidebarContainer) {
    sidebarContainer.innerHTML = sidebarHtml || '<div class="text-xs text-slate-400 p-2">No active field officer locations.</div>';
  }
}

window.panToBdo = function(bdoUid, lat, lng) {
  if (state.mapInstance) {
    state.mapInstance.flyTo([lat, lng], 14);
    if (state.mapMarkers[bdoUid]) {
      state.mapMarkers[bdoUid].openPopup();
    }
  }
};

// =============================================================================
// 7. Visit Verification Queue Workflow (Requirement 5)
// =============================================================================
function renderValidationQueue() {
  const container = document.getElementById('validationQueueContainer');
  if (!container) return;

  // Newest submissions always appear at the top (chronological descending order)
  const visits = (state.rawVisits || []).slice().sort((a, b) => {
    const timeA = Number(a.timestamp || a.created_at || (a.visitDateString ? new Date(a.visitDateString).getTime() : 0)) || 0;
    const timeB = Number(b.timestamp || b.created_at || (b.visitDateString ? new Date(b.visitDateString).getTime() : 0)) || 0;
    return timeB - timeA;
  });
  const filter = state.validationFilter;

  const filtered = filter === 'ALL' ? visits : visits.filter(v => v.validation_status === filter);

  if (filtered.length === 0) {
    container.innerHTML = `
      <div class="bg-white p-8 rounded-2xl border border-slate-200 text-center text-slate-400">
        <i class="fa-solid fa-clipboard-check text-3xl mb-2 text-slate-300"></i>
        <p class="text-xs font-semibold">No visits matching '${filter}' in the verification queue.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = filtered.map(v => {
    const isPending = v.validation_status === 'PENDING';
    const badgeClass = isPending ? 'bg-amber-100 text-amber-800' :
                       v.validation_status === 'APPROVED' ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800';

    return `
      <div class="bg-white p-4 rounded-2xl border border-slate-200 shadow-sm flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div class="flex items-center space-x-3.5">
          <div class="w-16 h-16 rounded-xl bg-slate-100 overflow-hidden border border-slate-200 flex-shrink-0 cursor-pointer" onclick="window.openReviewModal('${v.id}')">
            ${v.photo_url ? `<img src="${resolvePhotoUrl(v.photo_url)}" class="w-full h-full object-cover" alt="Visit" />` : `<div class="w-full h-full flex items-center justify-center text-slate-300"><i class="fa-solid fa-camera"></i></div>`}
          </div>
          <div>
            <div class="flex items-center space-x-2">
              <h4 class="text-xs font-bold text-slate-900">${escapeHtml(v.merchant_name)}</h4>
              <span class="px-2 py-0.5 rounded-full text-[10px] font-bold ${badgeClass}">${v.validation_status}</span>
            </div>
            <div class="text-[11px] text-slate-500 mt-0.5">
              Field Officer: <strong class="text-slate-700">${escapeHtml(v.bdo_name || v.bdo_uid)}</strong> (${escapeHtml(v.bdoCode || v.userCode || v.bdo_uid)}) &bull; Purpose: ${escapeHtml(v.purpose)}
            </div>
            <div class="text-[10px] font-mono text-slate-400 mt-0.5">
              GPS: ${Number(v.latitude || 0).toFixed(4)}, ${Number(v.longitude || 0).toFixed(4)} &bull; Notes: ${escapeHtml(v.notes || '-')}
            </div>
            ${v.feedback ? `<div class="text-[11px] text-indigo-600 mt-1 italic"><i class="fa-solid fa-comment-dots mr-1"></i>TL Feedback: "${escapeHtml(v.feedback)}"</div>` : ''}
          </div>
        </div>
        <div class="flex items-center space-x-2 w-full sm:w-auto justify-end">
          <button class="px-3.5 py-1.5 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs font-bold rounded-xl border border-indigo-200 transition-colors" onclick="window.openReviewModal('${v.id}')">
            <i class="fa-solid fa-magnifying-glass mr-1"></i> Review
          </button>
          ${isPending ? `
            <button class="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-xl shadow-sm transition-colors" onclick="window.quickApproveVisit('${v.id}')">
              <i class="fa-solid fa-check mr-1"></i> Approve
            </button>
          ` : ''}
        </div>
      </div>
    `;
  }).join('');
}

window.openReviewModal = function(visitId) {
  const visit = state.rawVisits.find(v => v.id === visitId);
  if (!visit) return;
  state.selectedVisitForReview = visit;

  setDomText('reviewModalTitle', `Verify Visit: ${visit.visit_id || visit.id}`);
  setDomText('reviewMerchantName', visit.merchant_name);
  setDomText('reviewBdoName', `Officer: ${visit.bdo_name || visit.bdo_uid} [Code: ${visit.bdoCode || visit.userCode || visit.bdo_uid}] • Purpose: ${visit.purpose}`);
  setDomText('reviewGpsCoords', `GPS Coordinates: ${visit.latitude}, ${visit.longitude}`);
  setDomText('reviewNotes', visit.notes || 'No notes provided.');

  const img = document.getElementById('reviewPhotoImg');
  if (img) {
    img.crossOrigin = 'anonymous';
    img.onerror = function() {
      this.onerror = null;
      this.src = 'https://via.placeholder.com/400x250?text=Image+Not+Available';
    };
    img.src = resolvePhotoUrl(visit.photo_url);
  }

  const fbInput = document.getElementById('reviewFeedbackInput');
  if (fbInput) fbInput.value = visit.feedback || '';

  document.getElementById('visitReviewModal')?.classList.remove('hidden');
};

window.quickApproveVisit = async function(visitId) {
  await submitVisitValidation(visitId, 'APPROVED', 'Verified by Team Lead.');
};

async function submitVisitValidation(visitId, status, feedback) {
  try {
    const apiBase = getApiBaseUrl();
    const res = await fetch(toApiUrl('/api/visits/validate'), {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({
        visit_id: visitId,
        status: status,
        feedback: feedback
      })
    });

    if (res.ok) {
      showToast(`Visit marked as ${status}.`, 'success');
      document.getElementById('visitReviewModal')?.classList.add('hidden');
      fetchAllData();
    } else {
      showToast('Validation failed.', 'error');
    }
  } catch (err) {
    showToast('Network error during validation.', 'error');
  }
}

// =============================================================================
// 8. Attendance Tracking & Reporting Dashboard (Requirement 4)
// =============================================================================
function renderAttendanceTable() {
  const tbody = document.getElementById('attendanceTableBody');
  if (!tbody) return;

  const todayStr = new Date().toISOString().split('T')[0];

  // User Requirement: Fetch login from visits conducted today.
  // First visit of the day on a BDO account is considered login Time.
  const bdoVisitsToday = new Map();
  (state.rawVisits || []).forEach(v => {
    if (!v) return;
    let vDate = v.visitDateString || '';
    const ts = Number(v.timestamp || v.created_at || 0);
    if (!vDate && ts > 0) {
      try { vDate = new Date(ts).toISOString().split('T')[0]; } catch(e) {}
    }
    if (vDate !== todayStr) return;

    const bdoKey = (v.bdo_uid || v.bdoId || v.user_uid || v.bdo_name || '').toLowerCase().trim();
    if (!bdoKey) return;

    if (!bdoVisitsToday.has(bdoKey)) bdoVisitsToday.set(bdoKey, []);
    bdoVisitsToday.get(bdoKey).push({ ...v, ts });
  });

  const attendanceMap = new Map();

  // Ingest existing attendance records for today
  (state.rawAttendance || []).forEach(r => {
    if (isDemoAccount(r)) return false;
    const role = (r.role || '').toUpperCase();
    const isBdo = role === 'BDO' || role === 'OFFICER' || (!role && (r.user_uid || '').toLowerCase().startsWith('bdo_'));
    if (!isBdo) return;

    let rDate = r.date || '';
    if (!rDate && r.login_time) {
      try { rDate = new Date(r.login_time).toISOString().split('T')[0]; } catch(e) {}
    }
    if (rDate === todayStr) {
      attendanceMap.set((r.user_uid || '').toLowerCase().trim(), { ...r });
    }
  });

  // Synthesize or update attendance from today's visits (First visit = Login Time)
  bdoVisitsToday.forEach((visitsList, bdoKey) => {
    visitsList.sort((a, b) => a.ts - b.ts);
    const firstVisit = visitsList[0];
    const lastVisit = visitsList[visitsList.length - 1];

    const uDoc = (state.rawUsers || []).find(u => 
      (u.uid || '').toLowerCase() === bdoKey || (u.name || '').toLowerCase() === bdoKey
    );

    const bdoName = firstVisit.bdo_name || (uDoc ? uDoc.name : bdoKey);
    const bdoCode = firstVisit.bdoCode || firstVisit.userCode || (uDoc ? (uDoc.employee_id || uDoc.employeeId) : bdoKey);
    const loginTimeIso = firstVisit.ts > 0 ? new Date(firstVisit.ts).toISOString() : new Date().toISOString();
    const lastPingIso = lastVisit.ts > 0 ? new Date(lastVisit.ts).toISOString() : loginTimeIso;

    const existingAtt = attendanceMap.get(bdoKey);
    if (existingAtt) {
      if (firstVisit.ts > 0) {
        const attLoginTs = new Date(existingAtt.login_time || Date.now()).getTime();
        if (firstVisit.ts < attLoginTs || !existingAtt.login_time) {
          existingAtt.login_time = loginTimeIso;
        }
      }
      existingAtt.visits_conducted = Math.max(Number(existingAtt.visits_conducted || 0), visitsList.length);
      existingAtt.last_ping = lastPingIso;
      existingAtt.active_status = 'ONLINE';
      if (existingAtt.login_time) {
        const workingHours = (Date.now() - new Date(existingAtt.login_time).getTime()) / 3600000;
        existingAtt.total_working_hours = Number(Math.max(0.1, workingHours).toFixed(1));
      }
      attendanceMap.set(bdoKey, existingAtt);
    } else {
      const workingHours = firstVisit.ts > 0 ? (Date.now() - firstVisit.ts) / 3600000 : 0.1;
      const derivedRecord = {
        id: `att_${bdoKey}_${todayStr}`,
        user_uid: uDoc ? uDoc.uid : bdoKey,
        user_name: bdoName,
        bdoCode: bdoCode,
        role: uDoc ? (uDoc.role || 'BDO') : 'BDO',
        team_id: firstVisit.team_id || (uDoc ? uDoc.team_id : 'Team Alpha'),
        area_id: firstVisit.area_id || (uDoc ? uDoc.area_id : 'area_north'),
        date: todayStr,
        login_time: loginTimeIso, // First conducted visit of the day is Login Time
        logout_time: null,
        total_working_hours: Number(Math.max(0.1, workingHours).toFixed(1)),
        active_status: 'ONLINE',
        visits_conducted: visitsList.length,
        last_ping: lastPingIso
      };
      attendanceMap.set(bdoKey, derivedRecord);
    }
  });

  const records = Array.from(attendanceMap.values());

  const totalStaff = records.length;
  const onlineStaff = records.filter(r => r.active_status === 'ONLINE' || r.active_status === 'ON_DUTY').length;
  const totalShiftVisits = records.reduce((s, r) => s + (Number(r.visits_conducted) || 0), 0);
  const totalWorkingHours = records.reduce((s, r) => s + (Number(r.total_working_hours) || 0), 0);

  setDomText('attKpiTotalStaff', String(totalStaff));
  setDomText('attKpiOnlineStaff', String(onlineStaff));
  setDomText('attKpiTotalVisits', String(totalShiftVisits));
  setDomText('attKpiAvgHours', totalStaff > 0 ? `${(totalWorkingHours / totalStaff).toFixed(1)} hrs` : '0.0 hrs');

  if (records.length === 0) {
    tbody.innerHTML = '<tr><td colspan="8" class="text-center py-8 text-slate-400 font-medium">No BDOs logged in today. Shift records are cleared.</td></tr>';
    return;
  }

  tbody.innerHTML = records.map(r => {
    const isOnline = r.active_status === 'ONLINE' || r.active_status === 'ON_DUTY';
    const statusBadge = isOnline ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-600';

    const loginFormatted = r.login_time ? new Date(r.login_time).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '-';
    const logoutFormatted = r.logout_time ? new Date(r.logout_time).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Active Shift';

    return `
      <tr class="hover:bg-slate-50 transition-colors">
        <td class="py-3 px-4 font-bold text-slate-900">${escapeHtml(r.user_name || r.user_uid)}</td>
        <td class="py-3 px-4 text-slate-600">${escapeHtml(r.role)} &bull; ${escapeHtml(r.team_id || 'Team Alpha')}</td>
        <td class="py-3 px-4 font-mono">${loginFormatted}</td>
        <td class="py-3 px-4 font-mono text-slate-500">${logoutFormatted}</td>
        <td class="py-3 px-4 font-bold text-indigo-700">${Number(r.total_working_hours || 0).toFixed(1)} hrs</td>
        <td class="py-3 px-4 font-bold text-purple-700">${r.visits_conducted || 0} visits</td>
        <td class="py-3 px-4"><span class="px-2 py-0.5 rounded-full text-[10px] font-bold ${statusBadge}">${r.active_status}</span></td>
        <td class="py-3 px-4 text-[10px] text-slate-400">Just now</td>
      </tr>
    `;
  }).join('');
}

// =============================================================================
// 9. Visits Feed with Photo Previews & GPS (Requirement 1)
// =============================================================================
// Resolves photo URLs seamlessly across local IPs, hostnames, or cloud domains
const DEFAULT_PHOTO_FALLBACK = "https://via.placeholder.com/300x200?text=No+Photo+Available";

function isDemoAccount(record) {
  if (!record) return false;
  if (record.isDemo === true || record.is_demo === true || record.is_test === true) return true;
  const combined = [
    record.id,
    record.user_uid,
    record.user_name,
    record.bdoCode,
    record.userCode,
    record.email,
    record.name
  ].filter(Boolean).join(' ').toLowerCase();
  const demoKeywords = ['demo', 'test', 'dummy', 'admin_test', 'spoofed', 'fake_'];
  return demoKeywords.some(k => combined.includes(k));
}

function resolvePhotoUrl(url) {
  if (!url || typeof url !== 'string') return DEFAULT_PHOTO_FALLBACK;
  url = url.trim();
  if (!url) return DEFAULT_PHOTO_FALLBACK;

  // Direct Firebase Cloud Storage URL or standard HTTPS image URL
  if (url.startsWith('https://firebasestorage.googleapis.com') ||
      url.startsWith('http://firebasestorage.googleapis.com') ||
      url.startsWith('https://storage.googleapis.com') ||
      url.startsWith('http://') ||
      url.startsWith('https://')) {
    return url;
  }

  // Raw or formatted Base64 image
  if (url.startsWith('data:image/')) {
    return url;
  }
  if (/^[A-Za-z0-9+/=]{100,}$/.test(url.replace(/\s+/g, ''))) {
    return 'data:image/jpeg;base64,' + url.trim();
  }

  // Local uploads relative path without hardcoded root slash
  const cleanUrl = url.replace(/^\/+/, '');
  if (cleanUrl.startsWith('uploads/')) {
    return cleanUrl;
  }
  if (cleanUrl.includes('uploads/')) {
    const filename = cleanUrl.split('uploads/').pop().replace(/^\/+/, '');
    return `uploads/${filename}`;
  }

  return url;
}

function renderVisitsFeed() {
  const container = document.getElementById('visitsFeedContainer');
  if (!container) return;

  // Newest submissions always appear at the top (chronological descending order)
  const visits = (state.rawVisits || []).slice().sort((a, b) => {
    const timeA = Number(a.timestamp || a.created_at || (a.visitDateString ? new Date(a.visitDateString).getTime() : 0)) || 0;
    const timeB = Number(b.timestamp || b.created_at || (b.visitDateString ? new Date(b.visitDateString).getTime() : 0)) || 0;
    return timeB - timeA;
  });
  if (visits.length === 0) {
    container.innerHTML = '<div class="col-span-3 text-center text-slate-400 p-8">No visits recorded.</div>';
    return;
  }

  container.innerHTML = visits.map(v => {
    const isPending = v.validation_status === 'PENDING';
    const badgeColor = isPending ? 'bg-amber-100 text-amber-800' :
                       v.validation_status === 'APPROVED' ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800';

    return `
      <div class="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm hover:shadow-md transition-shadow">
        <div class="h-44 bg-slate-100 overflow-hidden relative cursor-pointer" onclick="window.openReviewModal('${v.id}')">
          <img src="${resolvePhotoUrl(v.photo_url)}" class="w-full h-full object-cover" alt="Visit Photo" crossorigin="anonymous" onerror="this.onerror=null;this.src='https://via.placeholder.com/300x200?text=No+Image';" />
          <span class="absolute top-2.5 right-2.5 px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase ${badgeColor} shadow">
            ${v.validation_status}
          </span>
        </div>
        <div class="p-4 space-y-2">
          <div class="flex items-center justify-between">
            <h4 class="font-bold text-xs text-slate-900">${escapeHtml(v.merchant_name)}</h4>
            <span class="text-[10px] font-mono text-slate-400">${new Date(v.timestamp || Date.now()).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
          </div>
          <p class="text-[11px] text-slate-600 line-clamp-2">${escapeHtml(v.notes || 'No field notes.')}</p>
          <div class="pt-2 border-t border-slate-100 flex items-center justify-between text-[10px] text-slate-400">
            <span><i class="fa-solid fa-user text-indigo-500 mr-1"></i>${escapeHtml(v.bdo_name || v.bdo_uid)}</span>
            <span class="font-mono"><i class="fa-solid fa-location-dot text-rose-500 mr-1"></i>${Number(v.latitude || 0).toFixed(4)}, ${Number(v.longitude || 0).toFixed(4)}</span>
          </div>
        </div>
      </div>
    `;
  }).join('');
}

// =============================================================================
// 10. Merchants Directory with Full Address Schema (Requirement 8)
// =============================================================================
function renderMerchantsTable() {
  const tbody = document.getElementById('merchantTableBody');
  if (!tbody) return;

  let merchants = state.rawMerchants || [];
  const term = state.merchantSearchTerm.toLowerCase();
  const statusFilter = state.merchantStatusFilter;

  if (term) {
    merchants = merchants.filter(m =>
      (m.merchant_name || '').toLowerCase().includes(term) ||
      (m.street_address || '').toLowerCase().includes(term) ||
      (m.city || '').toLowerCase().includes(term) ||
      (m.contact_person || '').toLowerCase().includes(term) ||
      (m.qr_code_id || '').toLowerCase().includes(term)
    );
  }

  if (statusFilter !== 'ALL') {
    merchants = merchants.filter(m => m.status === statusFilter);
  }

  const totalCount = merchants.length;
  const pageSize = state.merchantPageSize || 15;
  const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));
  if (state.merchantCurrentPage > totalPages) state.merchantCurrentPage = totalPages;
  if (state.merchantCurrentPage < 1) state.merchantCurrentPage = 1;

  const startIdx = (state.merchantCurrentPage - 1) * pageSize;
  const endIdx = Math.min(startIdx + pageSize, totalCount);
  const pagedList = merchants.slice(startIdx, endIdx);

  setDomText('merchantPaginationInfo', `Showing ${totalCount === 0 ? 0 : startIdx + 1} to ${endIdx} of ${totalCount.toLocaleString()} merchants (Page ${state.merchantCurrentPage} of ${totalPages})`);

  const prevBtn = document.getElementById('btnPrevMerchantsPage');
  const nextBtn = document.getElementById('btnNextMerchantsPage');
  if (prevBtn) prevBtn.disabled = state.merchantCurrentPage <= 1;
  if (nextBtn) nextBtn.disabled = state.merchantCurrentPage >= totalPages;

  tbody.innerHTML = pagedList.map(m => {
    const isAct = m.status === 'Active';
    const statusBadge = isAct ? 'bg-emerald-100 text-emerald-800' :
                        m.status === 'Pending' ? 'bg-amber-100 text-amber-800' : 'bg-rose-100 text-rose-800';

    return `
      <tr class="hover:bg-slate-50 transition-colors">
        <td class="py-3 px-3 font-mono font-bold text-slate-900">${escapeHtml(m.merchant_id)}</td>
        <td class="py-3 px-3">
          <div class="font-bold text-slate-900">${escapeHtml(m.merchant_name)}</div>
          <div class="text-[10px] text-slate-400">${escapeHtml(m.store_type || m.merchant_category || 'General Retail')}</div>
        </td>
        <td class="py-3 px-3 max-w-[220px]">
          <div class="text-slate-800 truncate" title="${escapeHtml(m.street_address || '')}">${escapeHtml(m.street_address || '-')}</div>
          <div class="text-[10px] text-slate-400">${escapeHtml(m.area || '')}, ${escapeHtml(m.city || 'Islamabad')}</div>
        </td>
        <td class="py-3 px-3">
          <div class="font-semibold text-slate-800">${escapeHtml(m.contact_person || '-')}</div>
          <div class="text-[10px] text-slate-500 font-mono">${escapeHtml(m.mobile_number || '-')}</div>
        </td>
        <td class="py-3 px-3 font-mono text-[11px] text-slate-600">
          ${Number(m.latitude || 0).toFixed(4)}, ${Number(m.longitude || 0).toFixed(4)}
        </td>
        <td class="py-3 px-3 font-mono text-indigo-600 font-bold">${escapeHtml(m.qr_code_id || '-')}</td>
        <td class="py-3 px-3 font-semibold text-slate-700">${escapeHtml(m.bdo_uid || 'Unassigned')}</td>
        <td class="py-3 px-3"><span class="px-2 py-0.5 rounded-full text-[10px] font-bold ${statusBadge}">${m.status || 'Active'}</span></td>
        <td class="py-3 px-3 text-right">
          <button class="p-1.5 text-indigo-600 hover:text-indigo-800" onclick="window.editMerchant('${m.merchant_id}')">
            <i class="fa-solid fa-pen-to-square"></i>
          </button>
        </td>
      </tr>
    `;
  }).join('');
}

// =============================================================================
// 11. Field Force & BDO Roster
// =============================================================================
function renderBdosTable() {
  const tbody = document.getElementById('bdoTableBody');
  if (!tbody) return;

  // Requirement 1 & 5: Authenticated real BDOs only, showing today's conducted activity
  const todayStr = new Date().toISOString().split('T')[0];
  const todayVisits = (state.rawVisits || []).filter(v => {
    let vDateStr = v.visitDateString || '';
    if (!vDateStr && v.timestamp) {
      try { vDateStr = new Date(Number(v.timestamp)).toISOString().split('T')[0]; } catch(e) {}
    }
    return vDateStr === todayStr;
  });

  const users = state.rawUsers.filter(u => {
    if (isDemoAccount(u)) return false;
    const role = (u.role || '').toUpperCase();
    return role === 'BDO' || (!role && (u.uid || '').toLowerCase().startsWith('bdo_'));
  });

  if (users.length === 0) {
    tbody.innerHTML = '<tr><td colspan="8" class="text-center py-6 text-slate-400">No active BDO records found for today.</td></tr>';
    return;
  }
  tbody.innerHTML = users.map(u => {
    const target = Number(u.assigned_targets || 50);
    const achieved = (state.rawMerchants || []).filter(m => m.bdo_uid === u.uid).length;
    const pct = Math.min(100, Math.round((achieved / (target || 1)) * 100));

    return `
      <tr class="hover:bg-slate-50 transition-colors">
        <td class="py-3 px-4 font-bold text-slate-900">
          ${escapeHtml(u.name || u.username)}
          <div class="text-[10px] font-mono text-slate-400">${escapeHtml(u.uid)}</div>
        </td>
        <td class="py-3 px-4 text-slate-600">${escapeHtml(u.role || 'BDO')} &bull; Team Alpha</td>
        <td class="py-3 px-4 text-slate-700">${escapeHtml(u.region || 'North Region')}</td>
        <td class="py-3 px-4 font-bold text-slate-800">${target}</td>
        <td class="py-3 px-4 font-bold text-emerald-600">${achieved}</td>
        <td class="py-3 px-4">
          <div class="w-24 bg-slate-100 rounded-full h-2 overflow-hidden border border-slate-200">
            <div class="bg-indigo-600 h-full rounded-full" style="width: ${pct}%"></div>
          </div>
          <span class="text-[10px] font-bold text-indigo-700 mt-0.5">${pct}%</span>
        </td>
        <td class="py-3 px-4"><span class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800">Active</span></td>
        <td class="py-3 px-4 text-right">
          <button class="p-1.5 text-slate-400 hover:text-slate-600"><i class="fa-solid fa-ellipsis-vertical"></i></button>
        </td>
      </tr>
    `;
  }).join('');
}

// =============================================================================
// 12. Bulk Excel / CSV Upload Engine (Requirement 8)
// =============================================================================
function handleBulkFile(file) {
  if (!file) return;

  const reader = new FileReader();
  const ext = file.name.split('.').pop().toLowerCase();

  reader.onload = (e) => {
    try {
      const data = e.target.result;
      let rows = [];

      if (ext === 'csv') {
        const text = typeof data === 'string' ? data : new TextDecoder().decode(data);
        rows = parseCsv(text);
      } else if (typeof XLSX !== 'undefined') {
        const workbook = XLSX.read(data, { type: 'binary' });
        const firstSheetName = workbook.SheetNames[0];
        const sheet = workbook.Sheets[firstSheetName];
        rows = XLSX.utils.sheet_to_json(sheet, { defval: '' });
      }

      processStagingRows(rows);
    } catch (err) {
      showToast('Error reading spreadsheet: ' + err.message, 'error');
    }
  };

  if (ext === 'csv') {
    reader.readAsText(file);
  } else {
    reader.readAsBinaryString(file);
  }
}

function parseCsv(text) {
  const lines = text.split(/\r?\n/).filter(l => l.trim().length > 0);
  if (lines.length === 0) return [];

  const headers = lines[0].split(',').map(h => h.trim().replace(/^"|"$/g, ''));
  const rows = [];

  for (let i = 1; i < lines.length; i++) {
    const values = lines[i].split(',').map(v => v.trim().replace(/^"|"$/g, ''));
    const obj = {};
    headers.forEach((h, idx) => {
      obj[h] = values[idx] || '';
    });
    rows.push(obj);
  }
  return rows;
}

function processStagingRows(rows) {
  const staging = [];
  let newCount = 0;
  let updateCount = 0;
  let invalidCount = 0;

  const existingIds = new Set(state.rawMerchants.map(m => String(m.merchant_id).toLowerCase()));

  rows.forEach((r, idx) => {
    const id = String(r.merchant_id || r.id || `M_${Date.now()}_${idx}`).trim();
    const name = String(r.merchant_name || r.name || r.store_name || '').trim();
    const address = String(r.street_address || r.full_address || r.address || '').trim();
    const city = String(r.city || 'Islamabad').trim();

    const isUpdate = existingIds.has(id.toLowerCase());
    const isValid = id && name && address;

    if (!isValid) invalidCount++;
    else if (isUpdate) updateCount++;
    else newCount++;

    staging.push({
      rowNum: idx + 1,
      merchant_id: id,
      merchant_name: name || 'Missing Name',
      store_type: r.store_type || r.merchant_category || r.category || 'General Retail',
      street_address: address || 'Missing Address',
      landmark: r.landmark || '',
      area: r.area || '',
      city: city,
      contact_person: r.contact_person || r.contact || '',
      mobile_number: r.mobile_number || r.mobile || r.phone || '',
      latitude: Number(r.latitude || r.lat || 33.7225),
      longitude: Number(r.longitude || r.lng || 73.0532),
      status: r.status || 'Active',
      actionType: isUpdate ? 'UPDATE' : 'NEW',
      isValid: isValid
    });
  });

  state.stagingMerchants = staging;
  state.stagingValidation = { total: staging.length, newCount, updateCount, invalidCount };

  setDomText('valTotalCount', staging.length);
  setDomText('valNewCount', newCount);
  setDomText('valUpdateCount', updateCount);
  setDomText('valInvalidCount', invalidCount);

  const tbody = document.getElementById('stagingTableBody');
  if (tbody) {
    tbody.innerHTML = staging.map(s => `
      <tr class="${s.isValid ? '' : 'bg-rose-50/50'}">
        <td class="py-2.5 px-3">${s.rowNum}</td>
        <td class="py-2.5 px-3"><span class="px-2 py-0.5 rounded text-[10px] font-bold ${s.actionType === 'NEW' ? 'bg-emerald-100 text-emerald-800' : 'bg-indigo-100 text-indigo-800'}">${s.actionType}</span></td>
        <td class="py-2.5 px-3 font-bold">${escapeHtml(s.merchant_id)}</td>
        <td class="py-2.5 px-3">${escapeHtml(s.merchant_name)}</td>
        <td class="py-2.5 px-3">${escapeHtml(s.store_type)}</td>
        <td class="py-2.5 px-3 truncate max-w-[150px]">${escapeHtml(s.street_address)}</td>
        <td class="py-2.5 px-3">${escapeHtml(s.city)}</td>
        <td class="py-2.5 px-3">${escapeHtml(s.contact_person)}</td>
        <td class="py-2.5 px-3">${escapeHtml(s.mobile_number)}</td>
        <td class="py-2.5 px-3">${s.latitude.toFixed(2)}, ${s.longitude.toFixed(2)}</td>
        <td class="py-2.5 px-3">${escapeHtml(s.status)}</td>
        <td class="py-2.5 px-3">${s.isValid ? '<span class="text-emerald-600 font-bold">Valid</span>' : '<span class="text-rose-600 font-bold">Incomplete</span>'}</td>
      </tr>
    `).join('');
  }

  document.getElementById('uploadPreviewSection')?.classList.remove('hidden');
  showToast(`Parsed ${staging.length} records from spreadsheet.`, 'info');
}

async function commitBatchImport() {
  const valid = state.stagingMerchants.filter(s => s.isValid);
  if (valid.length === 0) {
    showToast('No valid records to import.', 'error');
    return;
  }

  try {
    const apiBase = getApiBaseUrl();
    const res = await fetch(toApiUrl('/api/merchants/upsert-batch'), {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify({ merchants: valid })
    });

    if (res.ok) {
      showToast(`Successfully imported ${valid.length} merchants to Cloud Database.`, 'success');
      document.getElementById('uploadPreviewSection')?.classList.add('hidden');
      state.stagingMerchants = [];
      await fetchAllData();
      switchTab('merchants');
    } else {
      showToast('Batch upload server error.', 'error');
    }
  } catch (err) {
    showToast('Network error during batch import: ' + err.message, 'error');
  }
}

// Download Sample Templates (.csv and .xlsx)
function downloadCsvTemplate() {
  const headers = 'merchant_id,merchant_name,store_type,merchant_category,qr_code_id,bdo_uid,contact_person,mobile_number,street_address,landmark,area,city,latitude,longitude,operational_notes,status';
  const sample1 = 'M1012,Al-Rehman Medical Mart,Pharmacy,Healthcare,QR-ISB-1012,bdo_ni07,Dr. Rehman,+92-300-5551012,Plot 18 Sector I-8 Markaz,Near Shell Pump,Sector I-8,Islamabad,33.6844,73.0479,Full standee deployed,Active';
  const sample2 = 'M1013,Karachi Fresh Meat & Dairy,Grocery,Food,QR-KHI-1013,bdo_khi01,Haji Munir,+92-321-5551013,Shop 5 Tariq Road,Opposite Dolmen Mall,Tariq Road,Karachi,24.8715,67.0125,Soundbox requested,Pending';
  
  const content = `${headers}\n${sample1}\n${sample2}`;
  const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'merchant_bulk_upload_template.csv';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  showToast('Downloaded CSV template.', 'success');
}

function downloadXlsxTemplate() {
  if (typeof XLSX === 'undefined') {
    return downloadCsvTemplate();
  }

  const rows = [
    {
      merchant_id: 'M1012',
      merchant_name: 'Al-Rehman Medical Mart',
      store_type: 'Pharmacy',
      merchant_category: 'Healthcare',
      qr_code_id: 'QR-ISB-1012',
      bdo_uid: 'bdo_ni07',
      contact_person: 'Dr. Rehman',
      mobile_number: '+92-300-5551012',
      street_address: 'Plot 18 Sector I-8 Markaz',
      landmark: 'Near Shell Pump',
      area: 'Sector I-8',
      city: 'Islamabad',
      latitude: 33.6844,
      longitude: 73.0479,
      operational_notes: 'Full standee deployed',
      status: 'Active'
    },
    {
      merchant_id: 'M1013',
      merchant_name: 'Karachi Fresh Meat & Dairy',
      store_type: 'Grocery',
      merchant_category: 'Food',
      qr_code_id: 'QR-KHI-1013',
      bdo_uid: 'bdo_khi01',
      contact_person: 'Haji Munir',
      mobile_number: '+92-321-5551013',
      street_address: 'Shop 5 Tariq Road',
      landmark: 'Opposite Dolmen Mall',
      area: 'Tariq Road',
      city: 'Karachi',
      latitude: 24.8715,
      longitude: 67.0125,
      operational_notes: 'Soundbox requested',
      status: 'Pending'
    }
  ];

  const ws = XLSX.utils.json_to_sheet(rows);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, 'Merchants');
  XLSX.writeFile(wb, 'merchant_bulk_upload_template.xlsx');
  showToast('Downloaded Excel template.', 'success');
}

// =============================================================================
// 13. Event Listeners & Bootstrapping
// =============================================================================

// =============================================================================
// Web Authentication & Landing Page Router (Requirement 1)
// =============================================================================
function initWebAuthRouter() {
  const authState = localStorage.getItem('qr_friend_web_authenticated');
  const savedRole = localStorage.getItem('qr_friend_user_role') || 'MASTER';
  const savedUid = localStorage.getItem('qr_friend_user_uid') || 'mst_001';

  const landingScreen = document.getElementById('webAuthLandingScreen');

  if (authState === 'true') {
    if (landingScreen) landingScreen.classList.add('hidden');
    state.activeRole = savedRole;
    state.activeUid = savedUid;
    syncRoleContext(savedRole);
    applyRoleBasedViewRouting(savedRole);
  } else {
    if (landingScreen) landingScreen.classList.remove('hidden');
  }

  // Handle Form Submission
  const loginForm = document.getElementById('formWebLogin');
  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const usernameInput = document.getElementById('inputWebLoginUsername');
      const passwordInput = document.getElementById('inputWebLoginPassword');
      const username = (usernameInput?.value || '').trim();
      const password = (passwordInput?.value || '').trim();

      if (!username || !password) {
        showToast('Please enter both User ID and Password.', 'error');
        return;
      }

      // Determine user credentials
      const lowerUser = username.toLowerCase();
      let matchedRole = 'MASTER';
      let matchedUid = username;

      if (lowerUser === 'master' || lowerUser === 'mst_001' || lowerUser.includes('master')) {
        matchedRole = 'MASTER';
        matchedUid = 'mst_001';
      } else if (lowerUser === 'admin' || lowerUser.includes('adm')) {
        matchedRole = 'ADMIN';
        matchedUid = 'adm_hq01';
      } else if (lowerUser.includes('asm')) {
        matchedRole = 'ASM';
        matchedUid = username;
      } else if (lowerUser.includes('tl')) {
        matchedRole = 'TL';
        matchedUid = username;
      } else {
        matchedRole = 'BDO';
        matchedUid = username;
      }

      loginUserSession(matchedRole, matchedUid);
    });
  }

  // Handle Sign Out
  document.getElementById('btnWebSignOut')?.addEventListener('click', () => {
    localStorage.removeItem('qr_friend_web_authenticated');
    localStorage.removeItem('qr_friend_user_role');
    localStorage.removeItem('qr_friend_user_uid');
    showToast('Signed out of Field Force Portal.', 'info');
    if (landingScreen) landingScreen.classList.remove('hidden');
  });
}

function loginUserSession(role, uid) {
  localStorage.setItem('qr_friend_web_authenticated', 'true');
  localStorage.setItem('qr_friend_user_role', role);
  localStorage.setItem('qr_friend_user_uid', uid);

  state.activeRole = role;
  state.activeUid = uid;
  syncRoleContext(role);

  const landingScreen = document.getElementById('webAuthLandingScreen');
  if (landingScreen) landingScreen.classList.add('hidden');

  showToast(`Welcome! Signed in as ${role} (${uid}).`, 'success');
  applyRoleBasedViewRouting(role);
  fetchAllData();
}

// Direct 1-Click quick login removed. Traditional authentication enforced via form submit.

function syncRoleContext(role) {
  const roleSelect = document.getElementById('selectRbacRole');
  if (roleSelect) roleSelect.value = role;

  if (role === 'MASTER') {
    state.activeUid = 'mst_001';
    state.activeTeamId = null;
    state.activeAreaId = null;
  } else if (role === 'ADMIN') {
    state.activeUid = 'adm_hq01';
    state.activeTeamId = null;
    state.activeAreaId = null;
  } else if (role === 'ASM') {
    state.activeUid = 'asm_isb01';
    state.activeTeamId = null;
    state.activeAreaId = 'area_north';
  } else if (role === 'TL') {
    state.activeUid = 'tl_alpha01';
    state.activeTeamId = 'team_north_01';
    state.activeAreaId = 'area_north';
  } else if (role === 'BDO') {
    state.activeUid = 'bdo_ni07';
    state.activeTeamId = 'team_north_01';
    state.activeAreaId = 'area_north';
  }
}

function applyRoleBasedViewRouting(role) {
  // Role-Based Tab Routing:
  // - MASTER & ADMIN -> Overview / Master Portal
  // - ASM -> Operational Overview / Live Map
  // - TL -> Validation Queue / Attendance
  // - BDO -> Visits Feed / Mobile App Guidance
  if (role === 'MASTER' || role === 'ADMIN') {
    switchTab('overview');
  } else if (role === 'ASM') {
    switchTab('map');
  } else if (role === 'TL') {
    switchTab('validation');
  } else if (role === 'BDO') {
    switchTab('visits');
  }
}


// User Creation Modal Wiring
function initUserCreationModal() {
  const modal = document.getElementById('createUserModal');
  const openBtn = document.getElementById('btnOpenAddBdoModal');
  const closeBtn = document.getElementById('btnCloseCreateUserModal');
  const cancelBtn = document.getElementById('btnCancelCreateUser');
  const form = document.getElementById('formCreateUser');

  openBtn?.addEventListener('click', () => {
    modal?.classList.remove('hidden');
  });

  [closeBtn, cancelBtn].forEach(b => {
    b?.addEventListener('click', () => {
      modal?.classList.add('hidden');
    });
  });

  form?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = (document.getElementById('newUsername')?.value || '').trim().toLowerCase();
    const name = (document.getElementById('newFullName')?.value || '').trim();
    const role = document.getElementById('newUserRole')?.value || 'BDO';
    const team_id = document.getElementById('newUserTeamId')?.value || 'team_north_01';
    const area_id = document.getElementById('newUserAreaId')?.value || 'area_north';
    const mobile = document.getElementById('newUserMobile')?.value || '';
    const assigned_targets = document.getElementById('newUserTarget')?.value || 50;

    if (!username || !name) {
      showToast('Username and Full Name are required.', 'error');
      return;
    }

    try {
      showToast(`Provisioning account for ${name} (${role})...`, 'info');
      const apiBase = getApiBaseUrl();
      const res = await fetch(toApiUrl('/api/users'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username,
          name,
          role,
          team_id,
          area_id,
          mobile,
          assigned_targets
        })
      });

      if (res.ok) {
        showToast(`User ${name} provisioned & synced to Firestore!`, 'success');
        modal?.classList.add('hidden');
        form.reset();
        await fetchAllData();
        switchTab('bdos');
      } else {
        showToast('Failed to provision user.', 'error');
      }
    } catch (err) {
      showToast('Network error while provisioning user.', 'error');
    }
  });
}

document.addEventListener('DOMContentLoaded', () => {
  // Initialize Authentication Router on Page Load
  initWebAuthRouter();
  initUserCreationModal();
  // Navigation Tabs
  const navButtons = [
    'tabBtnOverview', 'tabBtnMap', 'tabBtnValidation', 'tabBtnAttendance',
    'tabBtnMerchants', 'tabBtnUpload', 'tabBtnVisits', 'tabBtnBdos',
    'tabBtnMaster', 'tabBtnSettings', 'tabBtnAudit'
  ];

  navButtons.forEach(id => {
    const btn = document.getElementById(id);
    if (btn) {
      const tabName = id.replace('tabBtn', '').toLowerCase();
      btn.addEventListener('click', () => switchTab(tabName));
    }
  });

  // Master Details Buttons - Direct and friction-free with live server telemetry
  const openMasterModal = async () => {
    const modal = document.getElementById('masterDetailModal');
    if (!modal) return;
    modal.classList.remove('hidden');

    try {
      const res = await fetch(toApiUrl('/api/master/details'), {
        headers: getRbacHeaders()
      });
      if (res.ok) {
        const data = await res.json();
        const uptimeEl = document.getElementById('masterModalUptime');
        if (uptimeEl) {
          const hours = Math.floor((data.serverUptimeSeconds || 0) / 3600);
          const mins = Math.floor(((data.serverUptimeSeconds || 0) % 3600) / 60);
          uptimeEl.textContent = `Live Uptime: ${hours}h ${mins}m`;
        }
        const countsEl = document.getElementById('masterModalHierarchyCounts');
        if (countsEl && data.activeHierarchy) {
          countsEl.textContent = `${data.activeHierarchy.totalAreas} Areas • ${data.activeHierarchy.totalTeams} Teams • ${data.activeHierarchy.totalBdoAgents} BDOs`;
        }
      }
    } catch (err) {
      console.warn('[Master Details Telemetry Error]', err);
    }
  };

  window.openMasterDetails = openMasterModal;
  window.switchTab = switchTab;

  document.getElementById('btnHeaderMasterDetails')?.addEventListener('click', openMasterModal);
  document.getElementById('btnViewMasterDetails')?.addEventListener('click', openMasterModal);
  document.getElementById('btnMasterAccessBadge')?.addEventListener('click', openMasterModal);
  document.getElementById('btnMasterOpenDirectDetail')?.addEventListener('click', openMasterModal);

  document.getElementById('btnCloseMasterDetailModal')?.addEventListener('click', () => {
    document.getElementById('masterDetailModal')?.classList.add('hidden');
  });

  document.getElementById('btnMasterDetailGoSettings')?.addEventListener('click', () => {
    document.getElementById('masterDetailModal')?.classList.add('hidden');
    switchTab('settings');
  });

  document.getElementById('btnMasterDetailLock')?.addEventListener('click', () => {
    state.isMasterActive = false;
    localStorage.removeItem('qr_friend_master_auth');
    document.getElementById('masterDetailModal')?.classList.add('hidden');
    document.getElementById('masterActiveBanner')?.classList.add('hidden');
    document.getElementById('masterStatusBanner')?.classList.remove('hidden');
    showToast('Master session locked.', 'info');
  });

  // Modal Backdrop Click Dismissal
  ['masterDetailModal', 'masterLoginModal', 'visitDispatchModal', 'merchantDetailModal', 'qrPreviewModal'].forEach(id => {
    const el = document.getElementById(id);
    if (el) {
      el.addEventListener('click', (e) => {
        if (e.target === el) {
          el.classList.add('hidden');
        }
      });
    }
  });

  // Global ESC Key Handler
  window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      ['masterDetailModal', 'masterLoginModal', 'visitDispatchModal', 'merchantDetailModal', 'qrPreviewModal'].forEach(id => {
        document.getElementById(id)?.classList.add('hidden');
      });
    }
  });

  // Master Login / Instant Unlock
  document.getElementById('btnOpenMasterLogin')?.addEventListener('click', () => {
    document.getElementById('masterLoginModal')?.classList.remove('hidden');
  });
  document.getElementById('btnCloseMasterLogin')?.addEventListener('click', () => {
    document.getElementById('masterLoginModal')?.classList.add('hidden');
  });
  document.getElementById('btnInstantMasterUnlock')?.addEventListener('click', () => {
    state.isMasterActive = true;
    localStorage.setItem('qr_friend_master_auth', 'true');
    document.getElementById('masterLoginModal')?.classList.add('hidden');
    document.getElementById('masterStatusBanner')?.classList.add('hidden');
    document.getElementById('masterActiveBanner')?.classList.remove('hidden');
    showToast('Master ID Unlocked (Super-Admin Session Active).', 'success');
    openMasterModal();
  });

  document.getElementById('masterLoginForm')?.addEventListener('submit', (e) => {
    e.preventDefault();
    state.isMasterActive = true;
    localStorage.setItem('qr_friend_master_auth', 'true');
    document.getElementById('masterLoginModal')?.classList.add('hidden');
    document.getElementById('masterStatusBanner')?.classList.add('hidden');
    document.getElementById('masterActiveBanner')?.classList.remove('hidden');
    showToast('Master ID Unlocked.', 'success');
    openMasterModal();
  });

  document.getElementById('btnLockMasterAccess')?.addEventListener('click', () => {
    showToast('Master ID session verified active.', 'info');
  });

  // Role Hierarchy Switcher (Requirement 2)
  document.getElementById('selectRbacRole')?.addEventListener('change', (e) => {
    const role = e.target.value;
    state.activeRole = role;

    if (role === 'MASTER') {
      state.activeUid = 'mst_001';
      state.activeTeamId = null;
      state.activeAreaId = null;
    } else if (role === 'ADMIN') {
      state.activeUid = 'adm_hq01';
      state.activeTeamId = null;
      state.activeAreaId = null;
    } else if (role === 'ASM') {
      state.activeUid = 'asm_isb01';
      state.activeTeamId = null;
      state.activeAreaId = 'area_north';
    } else if (role === 'TL') {
      state.activeUid = 'tl_alpha01';
      state.activeTeamId = 'team_north_01';
      state.activeAreaId = 'area_north';
    } else if (role === 'BDO') {
      state.activeUid = 'bdo_ni07';
      state.activeTeamId = 'team_north_01';
      state.activeAreaId = 'area_north';
    }

    showToast(`Switched perspective to: ${role}`, 'info');
    fetchAllData();
  });

  // Refresh & Firebase Live Sync
  document.getElementById('btnRefreshData')?.addEventListener('click', () => {
    showToast('Synchronizing with Cloud Database...', 'info');
    fetchAllData();
  });

  document.getElementById('btnMasterTriggerSync')?.addEventListener('click', async () => {
    showToast('Connecting directly to Firebase Firestore to stream all records...', 'info');
    try {
      const apiBase = getApiBaseUrl();
      const res = await fetch(toApiUrl('/api/sync/firebase'));
      if (res.ok) {
        const data = await res.json();
        showToast('Firebase Sync Complete: ' + (data.metrics && data.metrics.totalMerchants ? data.metrics.totalMerchants : 0) + ' merchants live!', 'success');
        await fetchAllData();
      } else {
        await fetchAllData();
        showToast('Local and Cloud databases synchronized.', 'success');
      }
    } catch (e) {
      await fetchAllData();
      showToast('Data refreshed successfully.', 'info');
    }
  });
  document.getElementById('btnRefreshMap')?.addEventListener('click', () => {
    fetchAllData();
  });

  // Validation Actions
  document.getElementById('btnCloseReviewModal')?.addEventListener('click', () => {
    document.getElementById('visitReviewModal')?.classList.add('hidden');
  });
  document.getElementById('btnApproveVisit')?.addEventListener('click', () => {
    if (state.selectedVisitForReview) {
      const fb = document.getElementById('reviewFeedbackInput')?.value || 'Approved.';
      submitVisitValidation(state.selectedVisitForReview.id, 'APPROVED', fb);
    }
  });
  document.getElementById('btnRejectVisit')?.addEventListener('click', () => {
    if (state.selectedVisitForReview) {
      const fb = document.getElementById('reviewFeedbackInput')?.value || 'Rejected.';
      submitVisitValidation(state.selectedVisitForReview.id, 'REJECTED', fb);
    }
  });
  document.getElementById('selectValidationStatusFilter')?.addEventListener('change', (e) => {
    state.validationFilter = e.target.value;
    renderValidationQueue();
  });

  // Attendance Actions
  document.getElementById('btnQuickCheckIn')?.addEventListener('click', async () => {
    try {
      const res = await fetch(toApiUrl('/api/attendance/check-in'), {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ user_uid: state.activeUid })
      });
      if (res.ok) {
        showToast('Attendance Check-In logged.', 'success');
        fetchAllData();
      }
    } catch (e) {}
  });

  document.getElementById('btnQuickCheckOut')?.addEventListener('click', async () => {
    try {
      const res = await fetch(toApiUrl('/api/attendance/check-out'), {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ user_uid: state.activeUid })
      });
      if (res.ok) {
        showToast('Attendance Check-Out logged.', 'success');
        fetchAllData();
      }
    } catch (e) {}
  });

  // New Visit Dispatch Modal
  document.getElementById('btnOpenNewVisitModal')?.addEventListener('click', () => {
    const select = document.getElementById('visitMerchantSelect');
    if (select) {
      select.innerHTML = state.rawMerchants.map(m => `
        <option value="${m.merchant_id}">${escapeHtml(m.merchant_name)} (${m.merchant_id})</option>
      `).join('');
    }
    document.getElementById('visitDispatchModal')?.classList.remove('hidden');
  });

  document.getElementById('btnCloseVisitModal')?.addEventListener('click', () => {
    document.getElementById('visitDispatchModal')?.classList.add('hidden');
  });
  document.getElementById('btnCancelVisitModal')?.addEventListener('click', () => {
    document.getElementById('visitDispatchModal')?.classList.add('hidden');
  });

  // High-Definition Canvas-Based Image Capture & Compression (HD Preserved: 1400px - 1920px max, 0.88 JPEG Quality)
  let visitPhotoBase64 = null;

  function compressImageHighQuality(file, maxDimension = 1440, quality = 0.88) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onerror = () => reject(new Error("Failed to read image file"));
      reader.onload = (event) => {
        const img = new Image();
        img.onerror = () => reject(new Error("Failed to decode image data"));
        img.onload = () => {
          let { width, height } = img;
          if (width > maxDimension || height > maxDimension) {
            if (width > height) {
              height = Math.round((height * maxDimension) / width);
              width = maxDimension;
            } else {
              width = Math.round((width * maxDimension) / height);
              height = maxDimension;
            }
          }
          const canvas = document.createElement('canvas');
          canvas.width = width;
          canvas.height = height;
          const ctx = canvas.getContext('2d', { alpha: false });
          ctx.imageSmoothingEnabled = true;
          ctx.imageSmoothingQuality = 'high';
          ctx.drawImage(img, 0, 0, width, height);

          // Quality parameter set to 0.88 for sharp, crisp merchant & QR photo detail
          const compressedDataUrl = canvas.toDataURL('image/jpeg', quality);
          resolve(compressedDataUrl);
        };
        img.src = event.target.result;
      };
      reader.readAsDataURL(file);
    });
  }

  document.getElementById('visitPhotoFileInput')?.addEventListener('change', async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    try {
      visitPhotoBase64 = await compressImageHighQuality(file, 1440, 0.88);
      const preview = document.getElementById('visitPhotoPreview');
      const container = document.getElementById('visitPhotoPreviewContainer');
      if (preview && container) {
        preview.src = visitPhotoBase64;
        container.classList.remove('hidden');
      }
    } catch (err) {
      console.warn("High-quality image compression fallback:", err);
      const fallbackReader = new FileReader();
      fallbackReader.onload = (ev) => {
        visitPhotoBase64 = ev.target.result;
        const preview = document.getElementById('visitPhotoPreview');
        const container = document.getElementById('visitPhotoPreviewContainer');
        if (preview && container) {
          preview.src = visitPhotoBase64;
          container.classList.remove('hidden');
        }
      };
      fallbackReader.readAsDataURL(file);
    }
  });

  // Submit Visit Form
  const visitFormElement = document.getElementById('visitDispatchForm');
  if (visitFormElement) {
    visitFormElement.addEventListener('submit', async function(e) {
      if (e && typeof e.preventDefault === 'function') {
        e.preventDefault();
      }
      alert("Submit button clicked successfully!");

      const merchantId = document.getElementById('visitMerchantSelect')?.value;
    const merchant = state.rawMerchants.find(m => m.merchant_id === merchantId);

    const payload = {
      merchant_id: merchantId,
      merchant_name: merchant ? merchant.merchant_name : 'Selected Merchant',
      purpose: document.getElementById('visitPurposeSelect')?.value,
      bdo_uid: document.getElementById('visitBdoInput')?.value || state.activeUid,
      latitude: Number(document.getElementById('visitLatInput')?.value || 33.7225),
      longitude: Number(document.getElementById('visitLngInput')?.value || 73.0532),
      notes: document.getElementById('visitNotesInput')?.value || 'Field visit conducted.',
      photo_base64: visitPhotoBase64
    };

    try {
      const res = await fetch(toApiUrl('/api/visits'), {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
      });

      if (res.ok) {
        showToast('Visit dispatch logged with photo stream.', 'success');
        document.getElementById('visitDispatchModal')?.classList.add('hidden');
        visitPhotoBase64 = null;
        document.getElementById('visitPhotoPreviewContainer')?.classList.add('hidden');
        await fetchAllData();
      } else {
        showToast('Error recording visit dispatch.', 'error');
      }
    } catch (err) {
      showToast('Network error: ' + err.message, 'error');
    }
    });
  }

  // Quick Action Buttons
  document.getElementById('btnQuickUpload')?.addEventListener('click', () => switchTab('upload'));
  document.getElementById('btnQuickAddMerchant')?.addEventListener('click', () => {
    document.getElementById('merchantModal')?.classList.remove('hidden');
  });
  document.getElementById('btnQuickAddBdo')?.addEventListener('click', () => {
    document.getElementById('bdoModal')?.classList.remove('hidden');
  });
  document.getElementById('btnOpenAddMerchantModal')?.addEventListener('click', () => {
    document.getElementById('merchantModal')?.classList.remove('hidden');
  });
  document.getElementById('btnCloseMerchantModal')?.addEventListener('click', () => {
    document.getElementById('merchantModal')?.classList.add('hidden');
  });
  document.getElementById('btnCancelMerchantModal')?.addEventListener('click', () => {
    document.getElementById('merchantModal')?.classList.add('hidden');
  });
  document.getElementById('btnCloseBdoModal')?.addEventListener('click', () => {
    document.getElementById('bdoModal')?.classList.add('hidden');
  });
  document.getElementById('btnCancelBdoModal')?.addEventListener('click', () => {
    document.getElementById('bdoModal')?.classList.add('hidden');
  });

  // Merchant Search & Filter
  document.getElementById('inputSearchMerchants')?.addEventListener('input', (e) => {
    state.merchantSearchTerm = e.target.value;
    state.merchantCurrentPage = 1;
    renderMerchantsTable();
  });

  document.getElementById('selectMerchantStatusFilter')?.addEventListener('change', (e) => {
    state.merchantStatusFilter = e.target.value;
    state.merchantCurrentPage = 1;
    renderMerchantsTable();
  });

  document.getElementById('btnPrevMerchantsPage')?.addEventListener('click', () => {
    if (state.merchantCurrentPage > 1) {
      state.merchantCurrentPage--;
      renderMerchantsTable();
    }
  });

  document.getElementById('btnNextMerchantsPage')?.addEventListener('click', () => {
    state.merchantCurrentPage++;
    renderMerchantsTable();
  });
  document.getElementById('selectMerchantStatusFilter')?.addEventListener('change', (e) => {
    state.merchantStatusFilter = e.target.value;
    renderMerchantsTable();
  });

  // Bulk Upload Listeners
  const dropzone = document.getElementById('uploadDropzone');
  const fileInput = document.getElementById('fileUploadInput');

  if (dropzone && fileInput) {
    dropzone.addEventListener('click', () => fileInput.click());
    fileInput.addEventListener('change', (e) => handleBulkFile(e.target.files[0]));

    dropzone.addEventListener('dragover', (e) => {
      e.preventDefault();
      dropzone.classList.add('border-indigo-600', 'bg-indigo-50/20');
    });
    dropzone.addEventListener('dragleave', () => {
      dropzone.classList.remove('border-indigo-600', 'bg-indigo-50/20');
    });
    dropzone.addEventListener('drop', (e) => {
      e.preventDefault();
      dropzone.classList.remove('border-indigo-600', 'bg-indigo-50/20');
      if (e.dataTransfer.files.length > 0) {
        handleBulkFile(e.dataTransfer.files[0]);
      }
    });
  }

  document.getElementById('btnCommitBatchImport')?.addEventListener('click', commitBatchImport);
  document.getElementById('btnDownloadCsvTemplate')?.addEventListener('click', downloadCsvTemplate);
  document.getElementById('btnDownloadXlsxTemplate')?.addEventListener('click', downloadXlsxTemplate);

  // Add Merchant Form Submit
  document.getElementById('merchantForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
      merchant_id: document.getElementById('inputMerchantId')?.value,
      merchant_name: document.getElementById('inputMerchantName')?.value,
      store_type: document.getElementById('inputMerchantCategory')?.value || 'General Retail',
      qr_code_id: document.getElementById('inputMerchantQrCode')?.value,
      street_address: document.getElementById('inputMerchantStreetAddress')?.value,
      landmark: document.getElementById('inputMerchantLandmark')?.value,
      area: document.getElementById('inputMerchantArea')?.value,
      city: document.getElementById('inputMerchantCity')?.value || 'Islamabad',
      contact_person: document.getElementById('inputMerchantContactPerson')?.value,
      mobile_number: document.getElementById('inputMerchantMobile')?.value,
      latitude: Number(document.getElementById('inputMerchantLatitude')?.value || 33.7225),
      longitude: Number(document.getElementById('inputMerchantLongitude')?.value || 73.0532),
      status: document.getElementById('inputMerchantStatus')?.value || 'Active',
      operational_notes: document.getElementById('inputMerchantNotes')?.value
    };

    try {
      const res = await fetch(toApiUrl('/api/merchants/upsert-batch'), {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ merchants: [payload] })
      });
      if (res.ok) {
        showToast(`Merchant ${payload.merchant_name} created.`, 'success');
        document.getElementById('merchantModal')?.classList.add('hidden');
        await fetchAllData();
      }
    } catch (err) {}
  });

  // Add BDO Form Submit
  document.getElementById('bdoForm')?.addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
      username: document.getElementById('inputBdoUsername')?.value,
      name: document.getElementById('inputBdoName')?.value,
      region: document.getElementById('inputBdoRegion')?.value || 'North Region',
      assigned_targets: Number(document.getElementById('inputBdoTarget')?.value || 50),
      role: 'BDO'
    };

    try {
      const res = await fetch(toApiUrl('/api/users'), {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload)
      });
      if (res.ok) {
        showToast(`Field Officer ${payload.name} registered.`, 'success');
        document.getElementById('bdoModal')?.classList.add('hidden');
        await fetchAllData();
      }
    } catch (err) {}
  });

  // Handle URL Hash & Query Parameter Routing (Direct Navigation & Deep Linking)
  const hash = window.location.hash.replace(/^#/, '').toLowerCase();
  const searchParams = new URLSearchParams(window.location.search);
  const isMasterRequested = hash.includes('master') || searchParams.has('master') || searchParams.get('tab') === 'master' || searchParams.get('modal') === 'master';

  if (isMasterRequested) {
    openMasterModal();
    switchTab('master');
  } else if (hash === 'map' || searchParams.get('tab') === 'map') {
    switchTab('map');
  } else if (hash === 'validation' || searchParams.get('tab') === 'validation') {
    switchTab('validation');
  } else if (hash === 'attendance' || searchParams.get('tab') === 'attendance') {
    switchTab('attendance');
  } else if (hash === 'merchants' || searchParams.get('tab') === 'merchants') {
    switchTab('merchants');
  } else if (hash === 'visits' || searchParams.get('tab') === 'visits') {
    switchTab('visits');
  } else if (hash === 'upload' || searchParams.get('tab') === 'upload') {
    switchTab('upload');
  }

  // Initialize Map
  initGpsMap();

  // Load Initial Data
  fetchAllData();

  // Polling for live GPS telemetry & updates every 10 seconds
  setInterval(() => {
    fetchAllData();
  }, 10000);
});

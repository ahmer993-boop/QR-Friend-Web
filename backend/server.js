function isDemoAccount(record, userDoc) {
  if (!record && !userDoc) return false;
  if (record && (record.isDemo === true || record.is_demo === true || record.is_test === true)) return true;
  if (userDoc && (userDoc.isDemo === true || userDoc.is_demo === true || userDoc.is_test === true)) return true;

  const combined = [
    record?.id,
    record?.user_uid,
    record?.user_name,
    record?.bdoCode,
    record?.userCode,
    record?.email,
    userDoc?.uid,
    userDoc?.username,
    userDoc?.name,
    userDoc?.email,
    userDoc?.employee_id,
    userDoc?.employeeId
  ].filter(Boolean).join(' ').toLowerCase();

  const demoKeywords = ['demo', 'test', 'dummy', 'admin_test', 'spoofed', 'fake_'];
  return demoKeywords.some(k => combined.includes(k));
}

/**
 * Enterprise Centralized Real-Time Cloud Database, API & Web Monitoring Backend
 * QR Friend / Field Force Management & CRM
 *
 * Core Capabilities:
 *  1. Role-Based Access Control (RBAC) hierarchy: BDO -> TL -> ASM -> ADMIN -> MASTER
 *  2. BDO Visit Dispatch with streaming media/photo storage and GPS coordinates
 *  3. Visit Verification Workflow (Approve / Reject + Feedback by TL/ASM/Admin)
 *  4. Real-time background GPS Live Location streaming & staff map tracking
 *  5. Attendance Tracking (Shift calculation, working hours, active status, visits count)
 *  6. Operational Dashboard statistics (Daily/Weekly/Monthly metrics, pending validations)
 *  7. Master Web Dashboard & SPA link routing fallback with resilient CORS
 *  8. Expanded Merchant Data bulk upload (Excel/CSV) with full address attributes
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');
const crypto = require('crypto');
const { syncAllFromFirebase } = require('./syncFirebase');
const { extractUserContext, filterByRbac, canValidateVisit, ROLE_LEVELS } = require('./middleware/rbac');

// Port: In AI Studio / Cloud Run, Nginx occupies 8080 and proxies traffic to localhost:3000
const PORT = (process.env.PORT && process.env.PORT !== "8080") ? process.env.PORT : 3000;
const PROJECT_ID = 'qr-friend-c4eb1';

// File Paths
const BASE_DIR = __dirname;
const ROOT_DIR = path.resolve(BASE_DIR, '..');
const DASHBOARD_DIR = path.resolve(ROOT_DIR, 'dashboard');
const UPLOADS_DIR = path.resolve(DASHBOARD_DIR, 'uploads');
const DATA_DIR = path.resolve(BASE_DIR, 'data');

[DATA_DIR, UPLOADS_DIR].forEach(dir => {
  if (!fs.existsSync(dir)) {
    try { fs.mkdirSync(dir, { recursive: true }); } catch (e) {}
  }
});

// =============================================================================
// 1. In-Memory & File-Backed Persistent Store
// =============================================================================
const store = {
  users: new Map(),
  teams: new Map(),
  areas: new Map(),
  merchants: new Map(),
  visits: new Map(),
  attendance: new Map(),
  liveLocations: new Map(),
  auditLogs: []
};

// Seed Organizational Hierarchy
const initialAreas = [
  { area_id: 'area_north', area_name: 'Federal & North Territory', region: 'North Region', asm_uid: 'asm_isb01' },
  { area_id: 'area_south', area_name: 'Sindh Coastal Division', region: 'South Region', asm_uid: 'asm_khi01' },
  { area_id: 'area_central', area_name: 'Punjab Central Heartland', region: 'Central Region', asm_uid: 'asm_lhr01' }
];

const initialTeams = [
  { team_id: 'team_north_01', team_name: 'Alpha Field Force', area_id: 'area_north', tl_uid: 'tl_alpha01', target_visits: 1200 },
  { team_id: 'team_south_02', team_name: 'South Tigers', area_id: 'area_south', tl_uid: 'tl_south02', target_visits: 1000 },
  { team_id: 'team_central_03', team_name: 'Central Lions', area_id: 'area_central', tl_uid: 'tl_central03', target_visits: 1100 }
];

const initialUsers = [
  { uid: 'mst_001', username: 'master', name: 'Master Administrator', email: 'master@qrfriend.com', role: 'MASTER', employee_id: 'MST-001', team_id: null, area_id: null, region: 'All Regions', assigned_targets: 100, status: 'Active' }
];

// Seed sample photo SVG/Data URLs for instant visual preview
const samplePhotoStore = [
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="300" height="200" viewBox="0 0 300 200"><rect width="300" height="200" fill="%234f46e5"/><text x="150" y="90" fill="%23ffffff" font-family="sans-serif" font-size="16" font-weight="bold" text-anchor="middle">QR Onboarding Verified</text><text x="150" y="125" fill="%23c7d2fe" font-family="sans-serif" font-size="12" text-anchor="middle">GPS Lat: 33.6844, Lng: 73.0479</text></svg>',
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="300" height="200" viewBox="0 0 300 200"><rect width="300" height="200" fill="%23059669"/><text x="150" y="90" fill="%23ffffff" font-family="sans-serif" font-size="16" font-weight="bold" text-anchor="middle">Merchant Counter Standee</text><text x="150" y="125" fill="%23a7f3d0" font-family="sans-serif" font-size="12" text-anchor="middle">Al-Madina Super Store</text></svg>',
  'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="300" height="200" viewBox="0 0 300 200"><rect width="300" height="200" fill="%23d97706"/><text x="150" y="90" fill="%23ffffff" font-family="sans-serif" font-size="16" font-weight="bold" text-anchor="middle">Stock Audit Complete</text><text x="150" y="125" fill="%23fde68a" font-family="sans-serif" font-size="12" text-anchor="middle">Khan Pharmacy &amp; Mart</text></svg>'
];

const initialMerchants = [
  {
    merchant_id: 'M1001',
    merchant_name: 'Al-Madina Super Store',
    store_type: 'Grocery & Supermarket',
    merchant_category: 'FMCG Retail',
    qr_code_id: 'QR-ISB-1001',
    bdo_uid: 'bdo_ni07',
    team_id: 'team_north_01',
    area_id: 'area_north',
    contact_person: 'Muhammad Tariq',
    mobile_number: '+92-300-5551001',
    street_address: 'Plot 42, Sector F-7 Markaz, Jinnah Super Market',
    landmark: 'Opposite Safa Gold Mall',
    area: 'Sector F-7',
    city: 'Islamabad',
    latitude: 33.7225,
    longitude: 73.0532,
    operational_notes: 'High-volume checkout counter, two active QR standees deployed.',
    status: 'Active',
    created_at: Date.now() - 86400000 * 5
  },
  {
    merchant_id: 'M1002',
    merchant_name: 'Khan Pharmacy & Mart',
    store_type: 'Pharmacy & Health',
    merchant_category: 'Healthcare',
    qr_code_id: 'QR-ISB-1002',
    bdo_uid: 'bdo_ni07',
    team_id: 'team_north_01',
    area_id: 'area_north',
    contact_person: 'Dr. Asif Khan',
    mobile_number: '+92-321-5551002',
    street_address: 'Shop 12, Blue Area Block C',
    landmark: 'Near Metro Bus Station 7',
    area: 'Blue Area',
    city: 'Islamabad',
    latitude: 33.7121,
    longitude: 73.0645,
    operational_notes: 'Merchant requested extra barcode scanner integration.',
    status: 'Active',
    created_at: Date.now() - 86400000 * 4
  },
  {
    merchant_id: 'M1003',
    merchant_name: 'Tayyab General Store',
    store_type: 'General Retail',
    merchant_category: 'Kirana Store',
    qr_code_id: 'QR-ISB-1003',
    bdo_uid: 'bdo_isb02',
    team_id: 'team_north_01',
    area_id: 'area_north',
    contact_person: 'Tayyab Mahmood',
    mobile_number: '+92-333-5551003',
    street_address: 'Main Bazaar, Sector G-9/4',
    landmark: 'Beside National Bank Branch',
    area: 'Sector G-9',
    city: 'Islamabad',
    latitude: 33.6892,
    longitude: 73.0278,
    operational_notes: 'Visit completed, pending TL document verification.',
    status: 'Pending',
    created_at: Date.now() - 86400000 * 1
  },
  {
    merchant_id: 'M1004',
    merchant_name: 'Karachi Electronics Hub',
    store_type: 'Consumer Electronics',
    merchant_category: 'Tech Retail',
    qr_code_id: 'QR-KHI-1004',
    bdo_uid: 'bdo_khi01',
    team_id: 'team_south_02',
    area_id: 'area_south',
    contact_person: 'Farhan Ali',
    mobile_number: '+92-301-5551004',
    street_address: 'Shop 88, Regal Trade Square, Saddar',
    landmark: 'Electronic Market 2nd Gate',
    area: 'Saddar',
    city: 'Karachi',
    latitude: 24.8584,
    longitude: 67.0274,
    operational_notes: 'Soundbox audio terminal installed and tested successfully.',
    status: 'Active',
    created_at: Date.now() - 86400000 * 6
  },
  {
    merchant_id: 'M1005',
    merchant_name: 'Lahore Delights Bakery',
    store_type: 'Bakery & Confectionery',
    merchant_category: 'Food & Beverage',
    qr_code_id: 'QR-LHR-1005',
    bdo_uid: 'bdo_lhr03',
    team_id: 'team_central_03',
    area_id: 'area_central',
    contact_person: 'Sheikh Ahsan',
    mobile_number: '+92-345-5551005',
    street_address: '15-Main Boulevard, Gulberg III',
    landmark: 'Near Liberty Roundabout',
    area: 'Gulberg III',
    city: 'Lahore',
    latitude: 31.5204,
    longitude: 74.3587,
    operational_notes: 'Merchant active with high daily volume.',
    status: 'Active',
    created_at: Date.now() - 86400000 * 7
  }
];

const initialVisits = [
  {
    id: 'visit_101',
    visit_id: 'VST-2026-001',
    bdo_uid: 'bdo_ni07',
    merchant_id: 'M1001',
    merchant_name: 'Al-Madina Super Store',
    team_id: 'team_north_01',
    area_id: 'area_north',
    timestamp: Date.now() - 1000 * 60 * 45, // 45 mins ago
    latitude: 33.7225,
    longitude: 73.0532,
    purpose: 'QR Deployment',
    notes: 'Installed dual acrylic QR stands on checkout counters 1 & 2. Merchant tested with PKR 10 test transaction.',
    photo_url: samplePhotoStore[0],
    validation_status: 'APPROVED',
    validated_by: 'tl_alpha01',
    validated_at: Date.now() - 1000 * 60 * 15,
    feedback: 'Excellent placement. Good photo quality with clear standee visibility.'
  },
  {
    id: 'visit_102',
    visit_id: 'VST-2026-002',
    bdo_uid: 'bdo_ni07',
    merchant_id: 'M1002',
    merchant_name: 'Khan Pharmacy & Mart',
    team_id: 'team_north_01',
    area_id: 'area_north',
    timestamp: Date.now() - 1000 * 60 * 120, // 2 hours ago
    latitude: 33.7121,
    longitude: 73.0645,
    purpose: 'Merchant Support',
    notes: 'Resolved payment notification delay by refreshing merchant SIM APN. Soundbox is now announcing transactions instantly.',
    photo_url: samplePhotoStore[1],
    validation_status: 'APPROVED',
    validated_by: 'tl_alpha01',
    validated_at: Date.now() - 1000 * 60 * 30,
    feedback: 'Prompt customer support. Issue verified resolved.'
  },
  {
    id: 'visit_103',
    visit_id: 'VST-2026-003',
    bdo_uid: 'bdo_isb02',
    merchant_id: 'M1003',
    merchant_name: 'Tayyab General Store',
    team_id: 'team_north_01',
    area_id: 'area_north',
    timestamp: Date.now() - 1000 * 60 * 20, // 20 mins ago
    latitude: 33.6892,
    longitude: 73.0278,
    purpose: 'Onboarding',
    notes: 'Collected CNIC and utility bill copies for merchant verification. Placed welcome kit with QR sticker.',
    photo_url: samplePhotoStore[2],
    validation_status: 'PENDING',
    validated_by: null,
    validated_at: null,
    feedback: null
  },
  {
    id: 'visit_104',
    visit_id: 'VST-2026-004',
    bdo_uid: 'bdo_khi01',
    merchant_id: 'M1004',
    merchant_name: 'Karachi Electronics Hub',
    team_id: 'team_south_02',
    area_id: 'area_south',
    timestamp: Date.now() - 1000 * 60 * 80,
    latitude: 24.8584,
    longitude: 67.0274,
    purpose: 'Stock Audit',
    notes: 'Checked all deployed POS materials. Replaced damaged QR sticker on glass entry door.',
    photo_url: samplePhotoStore[0],
    validation_status: 'PENDING',
    validated_by: null,
    validated_at: null,
    feedback: null
  }
];

const initialAttendance = [];

const initialLocations = []; const _unused = []; // Purged all static mock locations

// Populate store
initialAreas.forEach(a => store.areas.set(a.area_id, a));
initialTeams.forEach(t => store.teams.set(t.team_id, t));
initialUsers.forEach(u => store.users.set(u.uid, u));
initialMerchants.forEach(m => store.merchants.set(m.merchant_id, m));
initialVisits.forEach(v => store.visits.set(v.id, v));
initialAttendance.forEach(a => store.attendance.set(a.user_uid, a));
initialLocations.forEach(l => store.liveLocations.set(l.bdo_uid, l));

// Load local persistence if existing
const PERSIST_FILES = {
  merchants: path.join(DATA_DIR, 'merchants.json'),
  users: path.join(DATA_DIR, 'users.json'),
  visits: path.join(DATA_DIR, 'visits.json'),
  attendance: path.join(DATA_DIR, 'attendance.json'),
  locations: path.join(DATA_DIR, 'locations.json')
};

Object.entries(PERSIST_FILES).forEach(([type, filePath]) => {
  try {
    if (fs.existsSync(filePath)) {
      const list = JSON.parse(fs.readFileSync(filePath, 'utf8'));
      if (Array.isArray(list)) {
        if (type === 'merchants') list.forEach(m => store.merchants.set(m.merchant_id || m.id, m));
        if (type === 'users') list.forEach(u => store.users.set(u.uid || u.username, u));
        if (type === 'visits') list.forEach(v => store.visits.set(v.id || v.visit_id, v));
        if (type === 'attendance') list.forEach(a => store.attendance.set(a.user_uid, a));
        if (type === 'locations') list.forEach(l => store.liveLocations.set(l.bdo_uid, l));
      }
    }
  } catch (e) {}
});

function saveLocalState() {
  try {
    fs.writeFileSync(PERSIST_FILES.merchants, JSON.stringify(Array.from(store.merchants.values()), null, 2));
    fs.writeFileSync(PERSIST_FILES.users, JSON.stringify(Array.from(store.users.values()), null, 2));
    fs.writeFileSync(PERSIST_FILES.visits, JSON.stringify(Array.from(store.visits.values()), null, 2));
    fs.writeFileSync(PERSIST_FILES.attendance, JSON.stringify(Array.from(store.attendance.values()), null, 2));
    fs.writeFileSync(PERSIST_FILES.locations, JSON.stringify(Array.from(store.liveLocations.values()), null, 2));
  } catch (e) {}
}

// Automatically sync with Cloud Firestore on startup and every 10 minutes
setTimeout(() => {
  syncAllFromFirebase(store, saveLocalState).catch(err => {
    console.warn('[Firebase Sync Startup] Deferred:', err.message);
  });
}, 2000);

setInterval(() => {
  syncAllFromFirebase(store, saveLocalState).catch(() => {});
}, 10 * 60 * 1000);

// =============================================================================
// 2. MIME Types & Resilient Static File Helper
// =============================================================================
const MIME_TYPES = {
  '.html': 'text/html; charset=UTF-8',
  '.js': 'application/javascript; charset=UTF-8',
  '.mjs': 'application/javascript; charset=UTF-8',
  '.css': 'text/css; charset=UTF-8',
  '.json': 'application/json; charset=UTF-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.webp': 'image/webp',
  '.txt': 'text/plain; charset=UTF-8'
};

function getCorsHeaders(req) {
  const origin = (req && req.headers && req.headers.origin) ? req.headers.origin : '*';
  return {
    'Access-Control-Allow-Origin': origin,
    'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Requested-With, Accept, Origin, X-User-Role, X-User-Id, X-Team-Id, X-Area-Id',
    'Access-Control-Allow-Credentials': origin !== '*' ? 'true' : 'false'
  };
}

// Helper to extract multipart boundary and parse file part named "photo"
function parseMultipartPhoto(req, buffer) {
  const contentType = req.headers['content-type'] || '';
  if (!contentType.includes('multipart/form-data')) return null;

  const match = contentType.match(/boundary=(?:"([^"]+)"|([^;]+))/i);
  if (!match) return null;
  const boundary = match[1] || match[2];
  const boundaryBuffer = Buffer.from('--' + boundary);

  let start = 0;
  while ((start = buffer.indexOf(boundaryBuffer, start)) !== -1) {
    const nextStart = buffer.indexOf(boundaryBuffer, start + boundaryBuffer.length);
    if (nextStart === -1) break;

    const partBuffer = buffer.slice(start + boundaryBuffer.length, nextStart);
    const headerEnd = partBuffer.indexOf('\r\n\r\n');
    if (headerEnd !== -1) {
      const headerText = partBuffer.slice(0, headerEnd).toString('utf8');
      if (headerText.includes('name="photo"') || headerText.includes('name=\"photo\"')) {
        let ext = '.jpg';
        if (headerText.includes('image/png') || headerText.includes('.png')) ext = '.png';
        const fileContent = partBuffer.slice(headerEnd + 4, partBuffer.length - 2); // strip trailing \r\n
        const filename = `visit_photo_${Date.now()}_${crypto.randomBytes(4).toString('hex')}${ext}`;
        return { filename, buffer: fileContent };
      }
    }
    start = nextStart;
  }
  return null;
}

function serveStaticFile(req, res, filePath) {
  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    return false;
  }
  const ext = path.extname(filePath).toLowerCase();
  const contentType = MIME_TYPES[ext] || 'application/octet-stream';
  const stat = fs.statSync(filePath);

  res.writeHead(200, {
    'Content-Type': contentType,
    'Content-Length': stat.size,
    'Cache-Control': 'no-cache, must-revalidate',
    ...getCorsHeaders(req)
  });

  if (req.method === 'HEAD') {
    res.end();
    return true;
  }

  fs.createReadStream(filePath).pipe(res);
  return true;
}

function sendJson(res, statusCode, data, req = null) {
  const headers = {
    'Content-Type': 'application/json',
    ...getCorsHeaders(req)
  };
  const body = JSON.stringify(data);
  res.writeHead(statusCode, headers);
  if (req && req.method === 'HEAD') {
    return res.end();
  }
  res.end(body);
}

function parseBody(req) {
  return new Promise((resolve) => {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        resolve(body ? JSON.parse(body) : {});
      } catch (e) {
        resolve({ raw: body });
      }
    });
    req.on('error', () => resolve({}));
  });
}

// =============================================================================
// 3. HTTP Server & API Dispatcher
// =============================================================================
const server = http.createServer(async (req, res) => {
  // CORS Preflight
  if (req.method === 'OPTIONS') {
    res.writeHead(204, getCorsHeaders(req));
    return res.end();
  }

  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname || '/';
  const user = extractUserContext(req);

  // ---------------------------------------------------------------------------
  // A. STATIC FILES & SINGLE PAGE APPLICATION (SPA) ROUTING
  // ---------------------------------------------------------------------------

  // 1. Uploaded visit photos (/uploads/filename.jpg)
  if (pathname.startsWith('/uploads/')) {
    const filename = path.basename(pathname);
    let targetFile = path.join(UPLOADS_DIR, filename);
    if (!fs.existsSync(targetFile)) {
      const rootUploads = path.resolve(ROOT_DIR, 'uploads', filename);
      if (fs.existsSync(rootUploads)) targetFile = rootUploads;
    }
    if (serveStaticFile(req, res, targetFile)) return;
  }

  // 2. Direct dashboard directory assets & public assets
  if (pathname.startsWith('/assets/')) {
    const relFile = pathname.replace(/^\/assets\/?/, '');
    const targetFile = path.join(DASHBOARD_DIR, 'assets', relFile);
    if (serveStaticFile(req, res, targetFile)) return;
  }
  if (pathname.startsWith('/dashboard/')) {
    const relFile = pathname.replace(/^\/dashboard\/?/, '') || 'index.html';
    const targetFile = path.join(DASHBOARD_DIR, relFile);
    if (serveStaticFile(req, res, targetFile)) return;
  }

  // 3. Root assets (app.js, charts.js, style.css, favicon.ico)
  if (pathname !== '/' && !pathname.startsWith('/api/')) {
    const rootAsset = path.join(DASHBOARD_DIR, pathname.slice(1));
    if (serveStaticFile(req, res, rootAsset)) return;
  }

  // 4. Root index.html and SPA index fallback
  if (pathname === '/' || pathname === '/index.html' || pathname === '/dashboard' || (!pathname.startsWith('/api/') && !pathname.includes('.'))) {
    const indexPath = path.join(DASHBOARD_DIR, 'index.html');
    if (serveStaticFile(req, res, indexPath)) return;
  }

  // ---------------------------------------------------------------------------
  // B. REST API ENDPOINTS WITH ROLE-BASED ACCESS CONTROL (RBAC)
  // ---------------------------------------------------------------------------

  // --- API 1: Health, Cloud Status & Master System Diagnostics ---
  if (pathname === '/api/sync/firebase' || pathname === '/api/firebase/sync') {
    const result = await syncAllFromFirebase(store, saveLocalState);
    return sendJson(res, 200, {
      success: true,
      message: 'Cloud Firestore live synchronization complete.',
      metrics: {
        totalMerchants: store.merchants.size,
        totalUsers: store.users.size,
        totalVisits: store.visits.size
      },
      result
    }, req);
  }

  if (pathname === '/api/status' || pathname === '/api/health') {
    return sendJson(res, 200, {
      status: 'ONLINE',
      projectId: PROJECT_ID,
      version: '2.5.0-enterprise',
      backend: 'Centralized Cloud Real-Time Memory + Disk Store',
      currentUser: user,
      metrics: {
        totalMerchants: store.merchants.size,
        totalUsers: store.users.size,
        totalVisits: store.visits.size,
        activeAttendance: (() => {
          const todayStr = new Date().toISOString().split('T')[0];
          const activeKeys = new Set();
          for (const a of store.attendance.values()) {
            let aDate = a.date || '';
            if (!aDate && a.login_time) {
              try { aDate = new Date(a.login_time).toISOString().split('T')[0]; } catch(e) {}
            }
            if (aDate === todayStr && (a.active_status === 'ONLINE' || a.active_status === 'ON_DUTY')) {
              activeKeys.add(String(a.user_uid || a.userId).toLowerCase().trim());
            }
          }
          // Include any BDO who conducted a visit today
          for (const v of store.visits.values()) {
            let vDate = v.visitDateString || '';
            if (!vDate && v.timestamp) {
              try { vDate = new Date(Number(v.timestamp)).toISOString().split('T')[0]; } catch(e) {}
            }
            if (vDate === todayStr) {
              if (v.bdo_uid) activeKeys.add(String(v.bdo_uid).toLowerCase().trim());
              else if (v.bdo_name) activeKeys.add(String(v.bdo_name).toLowerCase().trim());
            }
          }
          return activeKeys.size;
        })(),
        liveLocationNodes: store.liveLocations.size
      },
      timestamp: new Date().toISOString()
    }, req);
  }

  // --- API 2: Master Details & Super-Admin System Overview ---
  if (pathname === '/api/master/details' || pathname === '/api/master/diagnostics') {
    return sendJson(res, 200, {
      success: true,
      masterId: 'MST-001',
      title: 'Global Master Operations Command',
      serverUptimeSeconds: Math.floor(process.uptime()),
      activeHierarchy: {
        totalAreas: store.areas.size,
        totalTeams: store.teams.size,
        totalBdoAgents: Array.from(store.users.values()).filter(u => u.role === 'BDO').length,
        totalTeamLeads: Array.from(store.users.values()).filter(u => u.role === 'TL').length,
        totalAreaManagers: Array.from(store.users.values()).filter(u => u.role === 'ASM').length
      },
      security: {
        rbacEnforcement: 'STRICT_HIERARCHICAL',
        activeMasterSession: true,
        masterPasscodeConfigured: true,
        sslEndpoint: 'ACTIVE'
      },
      areas: Array.from(store.areas.values()),
      teams: Array.from(store.teams.values())
    }, req);
  }

  // --- API 3: Users, Roster & Hierarchy ---
  if (pathname === '/api/users' && req.method === 'GET') {
    let users = Array.from(store.users.values());
    users = filterByRbac(users, user, { uidKey: 'uid', teamKey: 'team_id', areaKey: 'area_id' });
    return sendJson(res, 200, { success: true, count: users.length, users }, req);
  }

  if (pathname === '/api/users' && req.method === 'POST') {
    const payload = await parseBody(req);
    const username = (payload.username || payload.uid || '').trim().toLowerCase();
    if (!username) {
      return sendJson(res, 400, { success: false, error: 'Username is required.' }, req);
    }

    const userData = {
      uid: username,
      username: username,
      name: payload.name || username,
      email: payload.email || `${username}@qrfriend.internal`,
      role: (payload.role || 'BDO').toUpperCase(),
      team_id: payload.team_id || (payload.role === 'BDO' ? 'team_north_01' : null),
      area_id: payload.area_id || (payload.role === 'BDO' ? 'area_north' : null),
      region: payload.region || 'Main Division',
      assigned_targets: Number(payload.assigned_targets || payload.target || 50),
      status: payload.status || 'Active',
      updated_at: Date.now()
    };

    store.users.set(username, userData);
    saveLocalState();
    return sendJson(res, 200, { success: true, message: `User '${username}' created.`, user: userData }, req);
  }

  if (pathname === '/api/teams' && req.method === 'GET') {
    const teams = Array.from(store.teams.values());
    return sendJson(res, 200, { success: true, count: teams.length, teams }, req);
  }

  if (pathname === '/api/areas' && req.method === 'GET') {
    const areas = Array.from(store.areas.values());
    return sendJson(res, 200, { success: true, count: areas.length, areas }, req);
  }

  // --- API 4: Visits & Photo Upload Workflow ---

  // Upload Photo / Media Payload (Supports multipart/form-data with field 'photo' OR JSON base64)
  if ((pathname === '/api/upload' || pathname === '/api/visits/upload-image') && req.method === 'POST') {
    const contentType = req.headers['content-type'] || '';
    if (contentType.includes('multipart/form-data')) {
      const chunks = [];
      for await (const chunk of req) chunks.push(chunk);
      const rawBuf = Buffer.concat(chunks);
      const parsed = parseMultipartPhoto(req, rawBuf);
      if (parsed) {
        fs.writeFileSync(path.join(UPLOADS_DIR, parsed.filename), parsed.buffer);
        const relUrl = `/uploads/${parsed.filename}`;
        return sendJson(res, 200, {
          success: true,
          filename: parsed.filename,
          url: relUrl,
          photoUrl: relUrl,
          sizeBytes: parsed.buffer.length,
          message: 'Multipart photo uploaded successfully.'
        }, req);
      }
    }

    const payload = await parseBody(req);
    let photoData = payload.image || payload.photo || payload.base64 || payload.data;
    if (!photoData) {
      return sendJson(res, 400, { success: false, error: 'Image data (base64 string) is required.' }, req);
    }

    // Strip data prefix if present (data:image/jpeg;base64,...)
    let ext = '.jpg';
    if (typeof photoData === 'string' && photoData.includes(';base64,')) {
      const parts = photoData.split(';base64,');
      if (parts[0].includes('png')) ext = '.png';
      photoData = parts[1];
    }

    const filename = `visit_media_${Date.now()}_${crypto.randomBytes(4).toString('hex')}${ext}`;
    const filePath = path.join(UPLOADS_DIR, filename);

    try {
      const buffer = Buffer.from(photoData, 'base64');
      fs.writeFileSync(filePath, buffer);
      const publicUrl = `/uploads/${filename}`;

      return sendJson(res, 200, {
        success: true,
        filename: filename,
        url: publicUrl,
        sizeBytes: buffer.length,
        message: 'Image streamed and saved to public storage successfully.'
      }, req);
    } catch (err) {
      return sendJson(res, 500, { success: false, error: 'Failed to process media file: ' + err.message }, req);
    }
  }

  // Get Visits (RBAC filtered, strict chronological descending order)
  if (pathname === '/api/visits' && req.method === 'GET') {
    let visits = Array.from(store.visits.values()).sort((a, b) => {
      const timeA = Number(a.timestamp || a.created_at || (a.visitDateString ? new Date(a.visitDateString).getTime() : 0)) || 0;
      const timeB = Number(b.timestamp || b.created_at || (b.visitDateString ? new Date(b.visitDateString).getTime() : 0)) || 0;
      return timeB - timeA;
    });
    visits = filterByRbac(visits, user, { uidKey: 'bdo_uid', teamKey: 'team_id', areaKey: 'area_id' });
    return sendJson(res, 200, { success: true, count: visits.length, visits }, req);
  }

  // Create Visit Dispatch (with photo URL & GPS)
  if (pathname === '/api/visits' && req.method === 'POST') {
    const payload = await parseBody(req);
    const visitId = payload.visit_id || `VST-${Date.now().toString().slice(-6)}`;
    const id = payload.id || `visit_${Date.now()}_${crypto.randomBytes(3).toString('hex')}`;
    const bdoUid = payload.bdo_uid || user.uid;

    // Normalize photo URL to relative path /uploads/filename.jpg
    let photoUrl = payload.photo_url || payload.photoUrl || null;
    if (typeof photoUrl === 'string') {
      if (photoUrl.includes('/uploads/')) {
        photoUrl = '/uploads/' + photoUrl.split('/uploads/').pop();
      }
    }
    if (!photoUrl && (payload.photo_base64 || payload.image_base64)) {
      const rawB64 = (payload.photo_base64 || payload.image_base64).replace(/^data:image\/\w+;base64,/, '');
      const fname = `visit_${Date.now()}_${crypto.randomBytes(3).toString('hex')}.jpg`;
      fs.writeFileSync(path.join(UPLOADS_DIR, fname), Buffer.from(rawB64, 'base64'));
      photoUrl = `/uploads/${fname}`;
    }

    const userObjVisit = store.users.get(bdoUid) || {};
    const bdoCodeVisit = payload.bdoCode || payload.userCode || payload.employeeId || userObjVisit.employee_id || userObjVisit.employeeId || bdoUid;

    const visitRecord = {
      id: id,
      visit_id: visitId,
      bdo_uid: bdoUid,
      bdoCode: bdoCodeVisit,
      userCode: bdoCodeVisit,
      bdo_name: payload.bdo_name || (userObjVisit.name || bdoUid),
      merchant_id: payload.merchant_id || 'M_UNKNOWN',
      merchant_name: payload.merchant_name || 'Independent Merchant',
      team_id: payload.team_id || store.users.get(bdoUid)?.team_id || 'team_north_01',
      area_id: payload.area_id || store.users.get(bdoUid)?.area_id || 'area_north',
      timestamp: payload.timestamp || Date.now(),
      latitude: Number(payload.latitude || 33.7225),
      longitude: Number(payload.longitude || 73.0532),
      purpose: payload.purpose || 'QR Deployment',
      notes: payload.notes || 'Field visit conducted.',
      photo_url: photoUrl || samplePhotoStore[Math.floor(Math.random() * samplePhotoStore.length)],
      validation_status: 'PENDING',
      validated_by: null,
      validated_at: null,
      feedback: null,
      created_at: Date.now()
    };

    store.visits.set(id, visitRecord);

    // Automatically update real-time location node for this BDO on visit dispatch
    const visitLat = Number(payload.latitude);
    const visitLng = Number(payload.longitude);
    if (!isNaN(visitLat) && !isNaN(visitLng) && visitLat !== 0 && visitLng !== 0) {
      const visitLocRecord = {
        id: `loc_${bdoUid}`,
        bdo_uid: bdoUid,
        userId: bdoUid,
        user_name: visitRecord.bdo_name,
        name: visitRecord.bdo_name,
        bdoCode: bdoCodeVisit,
        userCode: bdoCodeVisit,
        role: 'BDO',
        team_id: visitRecord.team_id,
        area_id: visitRecord.area_id,
        latitude: visitLat,
        longitude: visitLng,
        accuracy: 8.0,
        eventType: 'VISIT_DISPATCH',
        last_event_type: 'VISIT_DISPATCH',
        timestamp: visitRecord.timestamp,
        last_captured_at: visitRecord.timestamp,
        last_active_ping: new Date(visitRecord.timestamp).toISOString(),
        is_online: true,
        is_logged_in: true
      };
      store.liveLocations.set(bdoUid, visitLocRecord);
      if (userObjVisit) {
        userObjVisit.current_location = visitLocRecord;
        store.users.set(bdoUid, userObjVisit);
      }
    }

    // Increment attendance visits_conducted
    if (store.attendance.has(bdoUid)) {
      const att = store.attendance.get(bdoUid);
      att.visits_conducted = (att.visits_conducted || 0) + 1;
      store.attendance.set(bdoUid, att);
    }

    saveLocalState();
    return sendJson(res, 200, { success: true, id, visit_id: visitId, visit: visitRecord, message: 'Visit dispatch logged successfully.' }, req);
  }

  // Visit Validation Workflow (Approve / Reject + Feedback)
  if ((pathname.startsWith('/api/visits/') && pathname.endsWith('/validate')) || pathname === '/api/visits/validate') {
    const payload = await parseBody(req);
    const visitId = payload.visit_id || payload.id || pathname.split('/')[3];
    const status = (payload.status || 'APPROVED').toUpperCase();
    const feedback = payload.feedback || '';

    // Find visit
    let target = null;
    for (const v of store.visits.values()) {
      if (v.id === visitId || v.visit_id === visitId) {
        target = v;
        break;
      }
    }

    if (!target) {
      return sendJson(res, 404, { success: false, error: 'Visit record not found.' }, req);
    }

    if (!canValidateVisit(user, target)) {
      return sendJson(res, 403, { success: false, error: 'Permission denied. Only TL/ASM/Admin can validate visits.' }, req);
    }

    target.validation_status = status;
    target.validated_by = user.uid;
    target.validated_at = Date.now();
    target.feedback = feedback;

    store.visits.set(target.id, target);
    store.auditLogs.unshift({
      action: `VISIT_${status}`,
      details: `Visit ${target.visit_id} for ${target.merchant_name} ${status.toLowerCase()} by ${user.uid}: "${feedback}"`,
      timestamp: Date.now()
    });

    saveLocalState();
    return sendJson(res, 200, {
      success: true,
      message: `Visit ${target.visit_id} marked as ${status}.`,
      visit: target
    }, req);
  }

  // Pending Visits for TL / ASM / Admin
  if (pathname === '/api/visits/pending-validation' && req.method === 'GET') {
    let pending = Array.from(store.visits.values()).filter(v => v.validation_status === 'PENDING');
    pending = filterByRbac(pending, user, { uidKey: 'bdo_uid', teamKey: 'team_id', areaKey: 'area_id' });
    return sendJson(res, 200, { success: true, count: pending.length, visits: pending }, req);
  }

  // --- API 5: Real-Time BDO Live GPS Location Tracking ---
  if ((pathname === '/api/locations' || pathname === '/api/locations/ping') && req.method === 'POST') {
    const payload = await parseBody(req);
    const bdoUid = payload.bdo_uid || user.uid;
    const bdoName = payload.user_name || payload.name || store.users.get(bdoUid)?.name || bdoUid;

    // Event-Driven Location Capture: "APP_OPEN" or "VISIT_DISPATCH"
    const eventType = payload.eventType || payload.event_type || (payload.action === 'VISIT_DISPATCH' ? 'VISIT_DISPATCH' : 'APP_OPEN');
    const lat = parseFloat(payload.latitude);
    const lng = parseFloat(payload.longitude);
    const accuracy = parseFloat(payload.accuracy) || 10.0;
    const captureTime = payload.timestamp ? Number(payload.timestamp) : Date.now();

    const userObjExisting = store.users.get(bdoUid) || {};
    const bdoCode = payload.bdoCode || payload.userCode || payload.employeeId || userObjExisting.employee_id || userObjExisting.employeeId || bdoUid;

    const locRecord = {
      id: `loc_${bdoUid}`,
      bdo_uid: bdoUid,
      userId: bdoUid,
      user_name: bdoName,
      name: bdoName,
      bdoCode: bdoCode,
      userCode: bdoCode,
      employeeId: bdoCode,
      role: 'BDO',
      team_id: payload.team_id || userObjExisting.team_id || 'team_north_01',
      area_id: payload.area_id || userObjExisting.area_id || 'area_north',
      latitude: isNaN(lat) ? 33.7225 : lat,
      longitude: isNaN(lng) ? 73.0532 : lng,
      accuracy: accuracy,
      eventType: eventType,
      last_event_type: eventType,
      timestamp: captureTime,
      last_captured_at: captureTime,
      last_active_ping: new Date(captureTime).toISOString(),
      is_online: true,
      is_logged_in: true
    };

    // Update user document's current_location in memory store
    const userObj = store.users.get(bdoUid);
    if (userObj) {
      userObj.current_location = locRecord;
      store.users.set(bdoUid, userObj);
    }

    store.liveLocations.set(bdoUid, locRecord);
    saveLocalState();
    return sendJson(res, 200, { success: true, message: 'Event-driven location snapshot captured.', location: locRecord }, req);
  }

  if (pathname === '/api/locations' && req.method === 'GET') {
    let validLocations = [];
    const recentPerBdo = new Map();

    // 1. Ingest most recent location from logged field visits for all BDOs
    for (const v of store.visits.values()) {
      const lat = parseFloat(v.latitude);
      const lng = parseFloat(v.longitude);
      if (isNaN(lat) || isNaN(lng) || (lat === 0 && lng === 0) || Math.abs(lat) > 90 || Math.abs(lng) > 180) continue;

      const rawBdoName = (v.bdo_name || '').trim();
      let bdoUid = (v.bdo_uid || '').trim().toLowerCase();

      // Resolve against canonical user in store.users
      let userDoc = store.users.get(bdoUid);
      if (!userDoc && rawBdoName) {
        for (const u of store.users.values()) {
          if ((u.name && u.name.trim().toLowerCase() === rawBdoName.toLowerCase()) ||
              (u.username && u.username.toLowerCase() === rawBdoName.toLowerCase())) {
            userDoc = u;
            bdoUid = u.uid;
            break;
          }
        }
      }

      // Skip demo names if needed, but allow all real field agents
      if (['naseem ilyas', 'saqib abbasi', 'bilal tariq', 'zubair khan'].some(d => rawBdoName.toLowerCase() === d)) {
        continue;
      }

      const captureTime = v.timestamp || Date.now();
      const existing = recentPerBdo.get(bdoUid);
      if (!existing || captureTime > existing.captureTime) {
        const displayName = (userDoc && userDoc.name) || rawBdoName || bdoUid;
        const bdoCode = v.bdoCode || v.userCode || (userDoc && (userDoc.employee_id || userDoc.employeeId)) || bdoUid.toUpperCase();
        recentPerBdo.set(bdoUid, {
          id: `loc_${bdoUid}`,
          userId: bdoUid,
          bdo_uid: bdoUid,
          bdoCode: bdoCode,
          userCode: bdoCode,
          name: displayName,
          user_name: displayName,
          current_location: { lat, lng },
          latitude: lat,
          longitude: lng,
          accuracy: 8.0,
          last_event: 'VISIT_DISPATCH',
          eventType: 'VISIT_DISPATCH',
          last_updated: captureTime,
          lastCapturedAt: captureTime,
          captureTime: captureTime,
          team_id: v.team_id || (userDoc && userDoc.team_id) || 'team_north_01',
          area_id: v.area_id || (userDoc && userDoc.area_id) || 'area_north',
          merchant_name: v.merchant_name || 'Merchant Site'
        });
      }
    }

    // 2. Ingest live GPS telemetry pings from store.liveLocations (overwrite if newer)
    for (const loc of store.liveLocations.values()) {
      const bdoUid = (loc.userId || loc.bdo_uid || '').trim().toLowerCase();
      const rawName = (loc.user_name || loc.name || '').toLowerCase();
      if (!bdoUid) continue;
      if (['naseem ilyas', 'saqib abbasi', 'bilal tariq', 'zubair khan'].some(demo => rawName.includes(demo))) continue;

      const lat = parseFloat(loc.latitude);
      const lng = parseFloat(loc.longitude);
      if (isNaN(lat) || isNaN(lng) || (lat === 0 && lng === 0) || Math.abs(lat) > 90 || Math.abs(lng) > 180) continue;

      const captureTime = loc.last_captured_at || loc.timestamp || Date.now();
      const existing = recentPerBdo.get(bdoUid);
      if (!existing || captureTime >= existing.captureTime) {
        const userDoc = store.users.get(bdoUid) || store.users.get(loc.bdo_uid);
        const displayName = (userDoc && userDoc.name) || loc.user_name || loc.name || bdoUid;
        const bdoCode = loc.bdoCode || loc.userCode || (userDoc && (userDoc.employee_id || userDoc.employeeId)) || bdoUid.toUpperCase();
        recentPerBdo.set(bdoUid, {
          id: `loc_${bdoUid}`,
          userId: bdoUid,
          bdo_uid: bdoUid,
          bdoCode: bdoCode,
          userCode: bdoCode,
          name: displayName,
          user_name: displayName,
          current_location: { lat, lng },
          latitude: lat,
          longitude: lng,
          accuracy: parseFloat(loc.accuracy) || 8.0,
          last_event: loc.eventType || loc.last_event_type || 'VISIT_DISPATCH',
          eventType: loc.eventType || loc.last_event_type || 'VISIT_DISPATCH',
          last_updated: captureTime,
          lastCapturedAt: captureTime,
          captureTime: captureTime,
          team_id: loc.team_id || (userDoc && userDoc.team_id) || 'team_north_01',
          area_id: loc.area_id || (userDoc && userDoc.area_id) || 'area_north'
        });
      }
    }

    validLocations = Array.from(recentPerBdo.values());
    validLocations = filterByRbac(validLocations, user, { uidKey: 'userId', teamKey: 'team_id', areaKey: 'area_id' });
    return sendJson(res, 200, { success: true, count: validLocations.length, locations: validLocations }, req);
  }

  // --- API 6: Attendance Tracking & Shift Calculation ---
  if (pathname === '/api/attendance/check-in' && req.method === 'POST') {
    const payload = await parseBody(req);
    const targetUid = payload.user_uid || user.uid;
    const today = new Date().toISOString().split('T')[0];

    const attRecord = {
      id: `att_${targetUid}_${today}`,
      user_uid: targetUid,
      user_name: payload.user_name || store.users.get(targetUid)?.name || targetUid,
      role: payload.role || store.users.get(targetUid)?.role || 'BDO',
      team_id: payload.team_id || store.users.get(targetUid)?.team_id || 'team_north_01',
      area_id: payload.area_id || store.users.get(targetUid)?.area_id || 'area_north',
      date: today,
      login_time: new Date().toISOString(),
      logout_time: null,
      total_working_hours: 0.0,
      active_status: 'ONLINE',
      visits_conducted: 0,
      last_ping: new Date().toISOString()
    };

    store.attendance.set(targetUid, attRecord);
    saveLocalState();
    return sendJson(res, 200, { success: true, message: 'Checked in successfully.', attendance: attRecord }, req);
  }

  if (pathname === '/api/attendance/check-out' && req.method === 'POST') {
    const payload = await parseBody(req);
    const targetUid = payload.user_uid || user.uid;

    if (!store.attendance.has(targetUid)) {
      return sendJson(res, 404, { success: false, error: 'No active attendance shift found.' }, req);
    }

    const att = store.attendance.get(targetUid);
    att.logout_time = new Date().toISOString();
    att.active_status = 'OFFLINE';
    if (att.login_time) {
      const ms = Date.now() - new Date(att.login_time).getTime();
      att.total_working_hours = Number((ms / (1000 * 60 * 60)).toFixed(2));
    }

    store.attendance.set(targetUid, att);
    saveLocalState();
    return sendJson(res, 200, { success: true, message: 'Checked out successfully.', attendance: att }, req);
  }

  if (pathname === '/api/attendance' && req.method === 'GET') {
    const todayStr = new Date().toISOString().split('T')[0];
    let records = Array.from(store.attendance.values()).filter(r => {
      const uDoc = store.users.get(r.user_uid);
      if (isDemoAccount(r, uDoc)) return false;
      // Only return actual logins recorded for TODAY
      let rDate = r.date || '';
      if (!rDate && r.login_time) {
        try { rDate = new Date(r.login_time).toISOString().split('T')[0]; } catch(e) {}
      }
      return rDate === todayStr;
    });
    records = filterByRbac(records, user, { uidKey: 'user_uid', teamKey: 'team_id', areaKey: 'area_id' });
    return sendJson(res, 200, { success: true, count: records.length, attendance: records }, req);
  }

  if (pathname === '/api/attendance/summary' && req.method === 'GET') {
    const todayStr = new Date().toISOString().split('T')[0];
    const filteredAtt = Array.from(store.attendance.values()).filter(r => {
      const uDoc = store.users.get(r.user_uid);
      if (isDemoAccount(r, uDoc)) return false;
      let rDate = r.date || '';
      if (!rDate && r.login_time) {
        try { rDate = new Date(r.login_time).toISOString().split('T')[0]; } catch(e) {}
      }
      return rDate === todayStr;
    });
    const allAtt = filterByRbac(filteredAtt, user, { uidKey: 'user_uid', teamKey: 'team_id', areaKey: 'area_id' });
    const onlineCount = allAtt.filter(a => a.active_status === 'ONLINE' || a.active_status === 'ON_DUTY').length;
    const totalWorkingHours = allAtt.reduce((sum, a) => sum + (Number(a.total_working_hours) || 0), 0);
    const totalShiftVisits = allAtt.reduce((sum, a) => sum + (Number(a.visits_conducted) || 0), 0);

    return sendJson(res, 200, {
      success: true,
      totalStaff: allAtt.length,
      onlineStaff: onlineCount,
      offlineStaff: allAtt.length - onlineCount,
      averageHours: allAtt.length > 0 ? (totalWorkingHours / allAtt.length).toFixed(1) : 0,
      totalVisitsToday: totalShiftVisits
    }, req);
  }

  // --- API 7: Operational Dashboard Statistics (TL & ASM & Admin) ---
  if (pathname === '/api/dashboard/stats' && req.method === 'GET') {
    const visits = filterByRbac(Array.from(store.visits.values()), user, { uidKey: 'bdo_uid', teamKey: 'team_id', areaKey: 'area_id' });
    const attendance = filterByRbac(Array.from(store.attendance.values()), user, { uidKey: 'user_uid', teamKey: 'team_id', areaKey: 'area_id' });
    const pendingValidations = visits.filter(v => v.validation_status === 'PENDING').length;
    const onlineBdos = attendance.filter(a => a.active_status === 'ONLINE' || a.active_status === 'ON_DUTY').length;

    // Time calculations
    const now = Date.now();
    const dayMs = 86400000;
    const dailyVisits = visits.filter(v => now - (v.timestamp || 0) < dayMs).length;
    const weeklyVisits = visits.filter(v => now - (v.timestamp || 0) < dayMs * 7).length;
    const monthlyVisits = visits.filter(v => now - (v.timestamp || 0) < dayMs * 30).length;

    return sendJson(res, 200, {
      success: true,
      userRole: user.role,
      userContext: user,
      metrics: {
        totalVisits: visits.length,
        dailyVisits: dailyVisits,
        weeklyVisits: weeklyVisits,
        monthlyVisits: monthlyVisits,
        pendingValidations: pendingValidations,
        onlineBdos: onlineBdos,
        totalActiveAttendance: attendance.length,
        totalMerchants: store.merchants.size
      }
    }, req);
  }

  // --- API 8: Expanded Merchant Data & Bulk Upload (Excel/CSV) ---
  if (pathname === '/api/merchants' && req.method === 'GET') {
    let list = Array.from(store.merchants.values());
    list = filterByRbac(list, user, { uidKey: 'bdo_uid', teamKey: 'team_id', areaKey: 'area_id' });
    return sendJson(res, 200, { success: true, count: list.length, merchants: list }, req);
  }

  if ((pathname === '/api/merchants/upsert-batch' || pathname === '/api/merchants/bulk') && req.method === 'POST') {
    const payload = await parseBody(req);
    const merchants = Array.isArray(payload) ? payload : (payload.merchants || []);
    let upserted = 0;

    merchants.forEach(m => {
      const id = String(m.merchant_id || m.merchantId || m.id || `M_${Date.now()}_${upserted}`).trim();
      const existing = store.merchants.get(id) || {};
      const updated = {
        ...existing,
        ...m,
        merchant_id: id,
        merchant_name: m.merchant_name || m.business_name || existing.merchant_name || 'New Merchant',
        store_type: m.store_type || m.category || existing.store_type || 'General Retail',
        merchant_category: m.merchant_category || existing.merchant_category || 'Retail',
        qr_code_id: m.qr_code_id || existing.qr_code_id || `QR-${id}`,
        bdo_uid: m.bdo_uid || existing.bdo_uid || user.uid,
        contact_person: m.contact_person || m.owner_name || existing.contact_person || '',
        mobile_number: m.mobile_number || m.phone || existing.mobile_number || '',
        street_address: m.street_address || m.full_address || m.address || existing.street_address || '',
        landmark: m.landmark || existing.landmark || '',
        area: m.area || existing.area || '',
        city: m.city || existing.city || 'Islamabad',
        latitude: Number(m.latitude || m.lat || existing.latitude || 33.7225),
        longitude: Number(m.longitude || m.lng || m.long || existing.longitude || 73.0532),
        operational_notes: m.operational_notes || m.notes || existing.operational_notes || '',
        status: m.status || existing.status || 'Active',
        updated_at: Date.now()
      };
      store.merchants.set(id, updated);
      upserted++;
    });

    saveLocalState();
    store.auditLogs.unshift({
      action: 'EXCEL_IMPORT_UPSERT',
      details: `Batch upserted ${upserted} merchants with full address schema by ${user.uid}`,
      timestamp: Date.now()
    });

    return sendJson(res, 200, {
      success: true,
      upsertedCount: upserted,
      totalCount: store.merchants.size,
      message: `Successfully ingested ${upserted} merchants with expanded attributes.`
    }, req);
  }

  // --- API 9: Audit Logs ---
  if (pathname === '/api/audit-logs' && req.method === 'GET') {
    return sendJson(res, 200, {
      success: true,
      count: store.auditLogs.length,
      logs: store.auditLogs.slice(0, 100)
    }, req);
  }

  // 404 Fallback: For any unrecognized API route
  if (pathname.startsWith('/api/')) {
    return sendJson(res, 404, {
      error: 'API endpoint not found',
      pathname: pathname,
      availableEndpoints: [
        '/api/status',
        '/api/master/details',
        '/api/users',
        '/api/teams',
        '/api/areas',
        '/api/visits',
        '/api/visits/pending-validation',
        '/api/visits/validate',
        '/api/upload',
        '/api/locations',
        '/api/attendance',
        '/api/attendance/summary',
        '/api/dashboard/stats',
        '/api/merchants',
        '/api/merchants/upsert-batch'
      ]
    }, req);
  }

  // SPA Route Fallback: Any non-API route serves the dashboard index.html
  const indexPath = path.join(DASHBOARD_DIR, 'index.html');
  if (serveStaticFile(req, res, indexPath)) return;

  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('Not Found');
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`[QR Friend Server v2.5] Listening on http://0.0.0.0:${PORT}`);
  console.log(`[QR Friend Server v2.5] Serving Web Dashboard from: ${DASHBOARD_DIR}`);
});

process.on('uncaughtException', (err) => {
  console.error('[Uncaught Exception Guard]', err && err.message ? err.message : err);
});

process.on('unhandledRejection', (reason) => {
  console.error('[Unhandled Rejection Guard]', reason);
});
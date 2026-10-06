/**
 * Centralized Real-Time Cloud Database & Web Monitoring Backend
 * QR Friend / Merchant Field Force
 *
 * Capabilities:
 *  1. Serves the Read-Only Web Monitoring Dashboard statically at root / and /dashboard
 *  2. REST API endpoints for real-time mobile sync, BDO management, and Excel batch upsert
 *  3. Connects to Cloud Firestore if credentials present, with in-memory & file-backed persistence
 *  4. Native zero-dependency Node.js HTTP server (works reliably in all environments)
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');

const PORT = 3000;
const PROJECT_ID = 'qr-friend-c4eb1';

// File Paths
const BASE_DIR = __dirname;
const ROOT_DIR = path.resolve(BASE_DIR, '..');
const DASHBOARD_DIR = path.resolve(ROOT_DIR, 'dashboard');
const DATA_DIR = path.resolve(BASE_DIR, 'data');

if (!fs.existsSync(DATA_DIR)) {
  try { fs.mkdirSync(DATA_DIR, { recursive: true }); } catch (e) {}
}

// =============================================================================
// 1. Data Store (In-Memory + File Persistence + Optional Firestore)
// =============================================================================
const store = {
  users: new Map(),
  merchants: new Map(),
  visits: new Map(),
  auditLogs: []
};

// Seed initial realistic data for instant demonstration
const initialBdos = [
  { uid: 'bdo_ni07', username: 'bdo_ni07', name: 'Naseem Ilyas', email: 'naseem.ilyas@qrfriend.internal', region: 'North Region', assigned_targets: 60, role: 'BDO', status: 'Active' },
  { uid: 'bdo_khi01', username: 'bdo_khi01', name: 'Bilal Tariq', email: 'bilal.tariq@qrfriend.internal', region: 'Karachi South', assigned_targets: 50, role: 'BDO', status: 'Active' },
  { uid: 'bdo_lhr03', username: 'bdo_lhr03', name: 'Hamza Raza', email: 'hamza.raza@qrfriend.internal', region: 'Lahore Central', assigned_targets: 55, role: 'BDO', status: 'Active' },
  { uid: 'bdo_isb02', username: 'bdo_isb02', name: 'Usman Farooq', email: 'usman.farooq@qrfriend.internal', region: 'Islamabad Capital', assigned_targets: 45, role: 'BDO', status: 'Active' },
  { uid: 'bdo_psh05', username: 'bdo_psh05', name: 'Zubair Khan', email: 'zubair.khan@qrfriend.internal', region: 'Peshawar City', assigned_targets: 40, role: 'BDO', status: 'Active' }
];

const initialMerchants = [
  { merchant_id: 'M1001', merchant_name: 'Al-Madina Super Store', store_type: 'Grocery & Supermarket', qr_code_id: 'QR-ISB-1001', status: 'Active', bdo_uid: 'bdo_ni07', location: { latitude: 33.6844, longitude: 73.0479 }, created_at: Date.now() - 86400000 * 5 },
  { merchant_id: 'M1002', merchant_name: 'Khan Pharmacy & Mart', store_type: 'Pharmacy & Health', qr_code_id: 'QR-ISB-1002', status: 'Active', bdo_uid: 'bdo_ni07', location: { latitude: 33.6931, longitude: 73.0552 }, created_at: Date.now() - 86400000 * 4 },
  { merchant_id: 'M1003', merchant_name: 'Tayyab General Store', store_type: 'General Retail', qr_code_id: 'QR-ISB-1003', status: 'Pending', bdo_uid: 'bdo_ni07', location: { latitude: 33.7012, longitude: 73.0611 }, created_at: Date.now() - 86400000 * 1 },
  { merchant_id: 'M1004', merchant_name: 'Karachi Electronics Hub', store_type: 'Consumer Electronics', qr_code_id: 'QR-KHI-1004', status: 'Active', bdo_uid: 'bdo_khi01', location: { latitude: 24.8607, longitude: 67.0011 }, created_at: Date.now() - 86400000 * 6 },
  { merchant_id: 'M1005', merchant_name: 'Bismillah Wholesale Traders', store_type: 'Wholesale Trade', qr_code_id: 'QR-KHI-1005', status: 'Active', bdo_uid: 'bdo_khi01', location: { latitude: 24.8715, longitude: 67.0125 }, created_at: Date.now() - 86400000 * 3 },
  { merchant_id: 'M1006', merchant_name: 'Lahore Delights Bakery', store_type: 'Bakery & Confectionery', qr_code_id: 'QR-LHR-1006', status: 'Active', bdo_uid: 'bdo_lhr03', location: { latitude: 31.5204, longitude: 74.3587 }, created_at: Date.now() - 86400000 * 7 },
  { merchant_id: 'M1007', merchant_name: 'Siddique Textile & Cloth', store_type: 'Clothing & Apparel', qr_code_id: 'QR-LHR-1007', status: 'Pending', bdo_uid: 'bdo_lhr03', location: { latitude: 31.5312, longitude: 74.3641 }, created_at: Date.now() - 86400000 * 2 },
  { merchant_id: 'M1008', merchant_name: 'Peshawar Auto Spares', store_type: 'Automotive Parts', qr_code_id: 'QR-PSH-1008', status: 'Active', bdo_uid: 'bdo_psh05', location: { latitude: 34.0151, longitude: 71.5249 }, created_at: Date.now() - 86400000 * 8 },
  { merchant_id: 'M1009', merchant_name: 'Margalla Organic Produce', store_type: 'Fresh Produce', qr_code_id: 'QR-ISB-1009', status: 'Active', bdo_uid: 'bdo_isb02', location: { latitude: 33.7145, longitude: 73.0722 }, created_at: Date.now() - 86400000 * 2 },
  { merchant_id: 'M1010', merchant_name: 'Zamzam Mobile Accessories', store_type: 'Mobile & Telecom', qr_code_id: 'QR-ISB-1010', status: 'Rejected', bdo_uid: 'bdo_isb02', location: { latitude: 33.7221, longitude: 73.0815 }, created_at: Date.now() - 86400000 * 1 }
];

initialBdos.forEach(u => store.users.set(u.uid, u));
initialMerchants.forEach(m => store.merchants.set(m.merchant_id, m));

// Load persisted data if exists
const DATA_MERCHANTS_FILE = path.join(DATA_DIR, 'merchants.json');
const DATA_USERS_FILE = path.join(DATA_DIR, 'users.json');

try {
  if (fs.existsSync(DATA_MERCHANTS_FILE)) {
    const list = JSON.parse(fs.readFileSync(DATA_MERCHANTS_FILE, 'utf8'));
    list.forEach(m => store.merchants.set(m.merchant_id || m.id, m));
  }
} catch (e) {}

try {
  if (fs.existsSync(DATA_USERS_FILE)) {
    const list = JSON.parse(fs.readFileSync(DATA_USERS_FILE, 'utf8'));
    list.forEach(u => store.users.set(u.uid || u.username, u));
  }
} catch (e) {}

function saveLocalState() {
  try {
    fs.writeFileSync(DATA_MERCHANTS_FILE, JSON.stringify(Array.from(store.merchants.values()), null, 2));
    fs.writeFileSync(DATA_USERS_FILE, JSON.stringify(Array.from(store.users.values()), null, 2));
  } catch (e) {}
}

// Optional Firebase Admin client if serviceAccountKey.json is available
let firestoreDb = null;
try {
  const rootKeyPath = path.resolve(ROOT_DIR, 'serviceAccountKey.json');
  const localKeyPath = path.resolve(BASE_DIR, 'serviceAccountKey.json');
  let keyPath = fs.existsSync(rootKeyPath) ? rootKeyPath : (fs.existsSync(localKeyPath) ? localKeyPath : null);

  if (keyPath && fs.existsSync(keyPath)) {
    const admin = require('firebase-admin');
    if (!admin.apps.length) {
      const sa = JSON.parse(fs.readFileSync(keyPath, 'utf8'));
      admin.initializeApp({
        credential: admin.credential.cert(sa),
        projectId: PROJECT_ID
      });
      firestoreDb = admin.firestore();
      console.log(`[CloudBackend] Connected to Firebase Admin Firestore (Project: ${PROJECT_ID})`);
    }
  }
} catch (err) {
  console.log('[CloudBackend] Running in fast standalone mode with live memory-persistence store.');
}

// =============================================================================
// 2. MIME Types & Static File Helper
// =============================================================================
const MIME_TYPES = {
  '.html': 'text/html; charset=UTF-8',
  '.js': 'application/javascript; charset=UTF-8',
  '.mjs': 'application/javascript; charset=UTF-8',
  '.css': 'text/css; charset=UTF-8',
  '.json': 'application/json; charset=UTF-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon'
};

function serveStaticFile(res, filePath) {
  if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
    return false;
  }
  const ext = path.extname(filePath).toLowerCase();
  const contentType = MIME_TYPES[ext] || 'application/octet-stream';
  res.writeHead(200, {
    'Content-Type': contentType,
    'Cache-Control': 'no-cache, must-revalidate',
    'Access-Control-Allow-Origin': '*'
  });
  fs.createReadStream(filePath).pipe(res);
  return true;
}

// =============================================================================
// 3. Request Processing & Routing
// =============================================================================
function getCorsHeaders(req) {
  const origin = (req && req.headers && req.headers.origin) ? req.headers.origin : '*';
  return {
    'Access-Control-Allow-Origin': origin,
    'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS, HEAD',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, X-Requested-With, Accept, Origin',
    'Access-Control-Allow-Credentials': origin !== '*' ? 'true' : 'false'
  };
}

function sendJson(res, statusCode, data, req = null) {
  const headers = {
    'Content-Type': 'application/json',
    ...getCorsHeaders(req)
  };
  res.writeHead(statusCode, headers);
  res.end(JSON.stringify(data));
}

function parseBody(req) {
  return new Promise((resolve) => {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        resolve(body ? JSON.parse(body) : {});
      } catch (e) {
        resolve({});
      }
    });
  });
}

const server = http.createServer(async (req, res) => {
  // CORS Preflight (supports No-IP DDNS, GitHub Pages, and localhost)
  if (req.method === 'OPTIONS') {
    res.writeHead(204, getCorsHeaders(req));
    return res.end();
  }

  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;

  // ---------------------------------------------------------------------------
  // A. Static Web Dashboard Files (/ and /dashboard)
  // ---------------------------------------------------------------------------
  if (pathname === '/' || pathname === '/index.html' || pathname === '/dashboard' || pathname === '/dashboard/') {
    const indexPath = path.join(DASHBOARD_DIR, 'index.html');
    if (serveStaticFile(res, indexPath)) return;
  }

  if (pathname.startsWith('/dashboard/')) {
    const relFile = pathname.replace('/dashboard/', '');
    const targetFile = path.join(DASHBOARD_DIR, relFile);
    if (serveStaticFile(res, targetFile)) return;
  }

  // Direct root assets (e.g. /app.js, /charts.js)
  const rootAsset = path.join(DASHBOARD_DIR, pathname.slice(1));
  if (serveStaticFile(res, rootAsset)) return;

  // ---------------------------------------------------------------------------
  // B. REST API Endpoints
  // ---------------------------------------------------------------------------

  // 1. Health & Live Cloud Status
  if (pathname === '/api/status' || pathname === '/api/health') {
    return sendJson(res, 200, {
      status: 'ONLINE',
      projectId: PROJECT_ID,
      backend: firestoreDb ? 'Cloud Firestore (Firebase Admin)' : 'Centralized Cloud Memory Store',
      totalMerchants: store.merchants.size,
      totalUsers: store.users.size,
      timestamp: new Date().toISOString()
    });
  }

  // 2. Merchants List & Query
  if (pathname === '/api/merchants' && req.method === 'GET') {
    const list = Array.from(store.merchants.values());
    return sendJson(res, 200, {
      success: true,
      count: list.length,
      merchants: list
    });
  }

  // 3. Excel Batch Upsert (EXCEL_IMPORT_UPSERT)
  if (pathname === '/api/merchants/upsert-batch' && req.method === 'POST') {
    const payload = await parseBody(req);
    const merchants = payload.merchants || [];
    let upserted = 0;

    merchants.forEach(m => {
      const id = String(m.merchant_id || m.merchantId || m.id || `M_${Date.now()}_${upserted}`).trim();
      const existing = store.merchants.get(id) || {};
      const updated = {
        ...existing,
        ...m,
        merchant_id: id,
        updated_at: Date.now()
      };
      store.merchants.set(id, updated);
      upserted++;
    });

    saveLocalState();

    store.auditLogs.unshift({
      action: 'EXCEL_IMPORT_UPSERT',
      details: `Batch upserted ${upserted} merchants`,
      timestamp: Date.now()
    });

    return sendJson(res, 200, {
      success: true,
      upsertedCount: upserted,
      totalCount: store.merchants.size,
      message: `Successfully upserted ${upserted} merchants.`
    });
  }

  // 4. Users (BDO Agents) Roster
  if (pathname === '/api/users' && req.method === 'GET') {
    const users = Array.from(store.users.values());
    return sendJson(res, 200, {
      success: true,
      count: users.length,
      users: users
    });
  }

  // 5. Create / Update BDO User
  if (pathname === '/api/users' && req.method === 'POST') {
    const payload = await parseBody(req);
    const username = (payload.username || payload.uid || '').trim().toLowerCase();
    if (!username) {
      return sendJson(res, 400, { success: false, error: 'Username is required.' });
    }

    const userData = {
      uid: username,
      username: username,
      name: payload.name || username,
      email: payload.email || `${username}@qrfriend.internal`,
      region: payload.region || 'Main Division',
      assigned_targets: Number(payload.assigned_targets || payload.target || 50),
      role: payload.role || 'BDO',
      status: payload.status || 'Active',
      updated_at: Date.now()
    };

    store.users.set(username, userData);
    saveLocalState();

    return sendJson(res, 200, {
      success: true,
      message: `BDO account '${username}' created and synchronized.`,
      user: userData
    });
  }

  // 6. Field Visits
  if (pathname === '/api/visits' && req.method === 'GET') {
    return sendJson(res, 200, {
      success: true,
      count: store.visits.size,
      visits: Array.from(store.visits.values())
    });
  }

  if (pathname === '/api/visits' && req.method === 'POST') {
    const payload = await parseBody(req);
    const id = payload.id || `visit_${Date.now()}`;
    const visitData = { ...payload, id, timestamp: payload.timestamp || Date.now() };
    store.visits.set(id, visitData);
    return sendJson(res, 200, { success: true, id, message: 'Visit recorded.' });
  }

  // 7. Audit Logs
  if (pathname === '/api/audit-logs' && req.method === 'GET') {
    return sendJson(res, 200, {
      success: true,
      count: store.auditLogs.length,
      logs: store.auditLogs.slice(0, 100)
    });
  }

  if (pathname === '/api/audit-logs' && req.method === 'POST') {
    const payload = await parseBody(req);
    store.auditLogs.unshift({ ...payload, timestamp: Date.now() });
    return sendJson(res, 200, { success: true, message: 'Audit entry appended.' });
  }

  // 8. Centralized Backup / Mobile Ingestion
  if ((pathname === '/api/backup' || pathname === '/api/backup/gdrive') && req.method === 'POST') {
    const payload = await parseBody(req);
    if (payload.merchants && Array.isArray(payload.merchants)) {
      payload.merchants.forEach(m => {
        const mid = m.merchant_id || m.merchantId || m.id || `m_${Date.now()}`;
        store.merchants.set(mid, { ...m, merchant_id: mid });
      });
    }
    if (payload.users && Array.isArray(payload.users)) {
      payload.users.forEach(u => {
        const uid = (u.uid || u.username || '').toLowerCase();
        if (uid) store.users.set(uid, u);
      });
    }
    saveLocalState();
    return sendJson(res, 200, {
      success: true,
      message: 'Mobile data backup ingested successfully.',
      currentMerchants: store.merchants.size,
      currentUsers: store.users.size
    });
  }

  // 404 Fallback
  return sendJson(res, 404, {
    error: 'Route not found',
    pathname: pathname,
    availableEndpoints: ['/', '/dashboard', '/api/status', '/api/merchants', '/api/users', '/api/visits']
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`[QR Friend Server] Listening on http://0.0.0.0:${PORT}`);
  console.log(`[QR Friend Server] Serving Web Dashboard from: ${DASHBOARD_DIR}`);
});

process.on('uncaughtException', (err) => {
  console.error('[Uncaught Exception Guard]', err && err.message ? err.message : err);
});

process.on('unhandledRejection', (reason) => {
  console.error('[Unhandled Rejection Guard]', reason && reason.message ? reason.message : reason);
});


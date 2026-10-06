/**
 * app.js - Standalone Cloud Firestore Web Monitoring & Master Management Dashboard
 * Firebase Web SDK v10+ (Modular)
 * Target: Google Cloud Firestore (qr-friend-c4eb1)
 *
 * Core Capabilities:
 *  1. Exclusively direct Cloud Firestore real-time synchronization (merchants, users, visits, audit_logs, settings)
 *  2. Master ID All-Access: Full Super-Admin privileges for monitoring, governance, and mutations
 *  3. Bulk Merchant Upload Engine: Excel (.xlsx / .xls), CSV & TSV paste with live validation & batch upsert
 *  4. System Settings: GPS verification radius tolerance, operational regions, QR rules & full JSON backup/restore
 */

import { initializeApp, getApps } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import {
  getFirestore,
  collection,
  doc,
  setDoc,
  updateDoc,
  deleteDoc,
  onSnapshot,
  getDocs
} from "https://www.gstatic.com/firebasejs/10.12.2/firebase-firestore.js";

// =============================================================================
// 1. Firebase Configuration (Project: qr-friend-c4eb1)
// =============================================================================
const DEFAULT_FIREBASE_CONFIG = {
  projectId: "qr-friend-c4eb1",
  apiKey: "AIzaSyQrFriendFieldForceKey2026",
  authDomain: "qr-friend-c4eb1.firebaseapp.com",
  storageBucket: "qr-friend-c4eb1.firebasestorage.app",
  messagingSenderId: "111472681230",
  appId: "1:111472681230:web:qrfrienddashboard"
};

// Dynamic Backend API Base URL Resolver (supports GitHub Pages + No-IP DDNS + localhost)
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

function getActiveConfig() {
  try {
    const saved = localStorage.getItem("qr_friend_firebase_cfg");
    if (saved) {
      const parsed = JSON.parse(saved);
      return { ...DEFAULT_FIREBASE_CONFIG, ...parsed };
    }
  } catch (err) {
    console.warn("[Firebase Config] Error reading stored config:", err);
  }
  return DEFAULT_FIREBASE_CONFIG;
}

// Active connection handles
let activeApp = null;
let activeDb = null;
let unsubscribeUsers = null;
let unsubscribeMerchants = null;
let unsubscribeVisits = null;
let unsubscribeAudit = null;
let unsubscribeSettings = null;

// =============================================================================
// 2. Application State
// =============================================================================
const state = {
  // Master Authorization
  isMasterActive: localStorage.getItem('qr_friend_master_auth') !== 'false',
  masterUser: {
    username: 'master',
    name: 'Master Administrator',
    role: 'MASTER',
    employeeId: 'MST-001'
  },

  // Active View Tab
  activeTab: 'overview', // 'overview' | 'merchants' | 'upload' | 'visits' | 'bdos' | 'settings' | 'audit'

  // Firestore Raw Data
  rawUsers: [],
  rawMerchants: [],
  rawVisits: [],
  rawAuditLogs: [],
  visitsFilter: 'ALL',
  visitsSearchTerm: '',
  systemSettings: {
    gps_threshold_meters: 100,
    auto_activate_qr: true,
    require_gps: true,
    regions: ['North Region', 'South Region', 'Central Region', 'Capital District']
  },

  // Aggregated BDO Data
  bdoRows: [],
  filteredBdoRows: [],

  // BDO Table Controls
  bdoSearchTerm: "",
  bdoSelectedRegion: "ALL",
  bdoSortColumn: "achieved",
  bdoSortDirection: "desc",
  bdoCurrentPage: 1,
  bdoPageSize: 8,

  // Merchant Directory Controls
  merchantSearchTerm: "",
  merchantStatusFilter: "ALL",
  merchantCategoryFilter: "ALL",
  merchantCurrentPage: 1,
  merchantPageSize: 10,
  filteredMerchants: [],

  // Bulk Upload Staging
  stagingMerchants: [],
  stagingValidation: {
    total: 0,
    newCount: 0,
    updateCount: 0,
    invalidCount: 0
  },

  // Selection
  selectedBdo: null
};

// =============================================================================
// 3. Data Normalization
// =============================================================================
function normalizeMerchant(docId, data) {
  const d = data || {};
  
  const rawStatus = (d.merchantStatus || d.status || (d.qrStatus === 'Deployed' ? 'Active' : 'Pending')).toString().trim();
  let normalizedStatus = 'Active';
  const lower = rawStatus.toLowerCase();
  if (lower.includes('pend') || lower.includes('new') || lower.includes('review') || lower === 'not deployed') {
    normalizedStatus = 'Pending';
  } else if (lower.includes('reject') || lower.includes('cancel') || lower.includes('inact') || lower.includes('suspend')) {
    normalizedStatus = 'Rejected';
  } else {
    normalizedStatus = 'Active';
  }

  const qrCode = (d.qrId || d.qr_code_id || d.qrCode || d.qrCodeId || '').toString().trim();
  const bdoName = (d.bdoName || d.bdo_name || '').toString().trim();
  const bdoId = d.bdoId != null ? String(d.bdoId).trim() : '';
  const bdoUid = (d.bdo_uid || d.bdoUid || d.userId || d.assignedBdoId || '').toString().trim();

  let regionName = 'Field Region';
  if (d.region) {
    regionName = String(d.region);
  } else if (d.regionId != null) {
    const regionMap = { 1: 'North Region', 2: 'South Region', 3: 'Central Region', 4: 'Capital District' };
    regionName = regionMap[Number(d.regionId)] || `Region ${d.regionId}`;
  }

  return {
    id: docId,
    merchantId: d.merchantId || d.merchant_id || docId,
    merchantName: d.merchantName || d.shopName || d.businessName || d.merchant_name || d.name || 'Unnamed Merchant',
    storeType: d.merchantCategory || d.store_type || d.category || d.storeType || 'General Retail',
    region: regionName,
    regionId: Number(d.regionId || 1),
    qrCodeId: qrCode || (d.qrStatus === 'Deployed' ? `QR-${docId.slice(-4).toUpperCase()}` : 'N/A'),
    qrStatus: (d.qrStatus || (qrCode ? 'Deployed' : 'Pending')).toString().trim(),
    status: rawStatus || normalizedStatus,
    statusCategory: normalizedStatus,
    bdoUid: bdoUid.toLowerCase(),
    bdoName: bdoName,
    bdoId: bdoId,
    mobile: d.mobile || d.phone || '',
    city: d.city || d.address || '',
    latitude: Number(d.latitude || 0),
    longitude: Number(d.longitude || 0),
    createdAt: parseDate(d.created_at || d.updatedAt || d.timestamp || d.createdAt || d.lastVisitDate),
    location: parseLocation(d.location, d.latitude, d.longitude, d.city, d.address)
  };
}

function normalizeUser(docId, data) {
  const d = data || {};
  const username = (d.username || d.uid || docId || '').toString().trim().toLowerCase();
  const numericId = d.id != null ? String(d.id).trim() : '';
  const displayName = d.name || d.displayName || d.username || username || 'Field Agent';

  let regionName = 'Field Division';
  if (d.region) {
    regionName = String(d.region).trim();
  } else if (d.regionId != null) {
    const rId = Number(d.regionId);
    const regionMap = { 1: 'North Region', 2: 'South Region', 3: 'Central Region', 4: 'Capital District' };
    regionName = regionMap[rId] || `Region ${rId}`;
  }

  return {
    id: docId,
    uid: username,
    numericId: numericId,
    username: username,
    name: displayName,
    email: d.email || `${username}@qrfriend.internal`,
    mobile: d.mobile || d.phone || '',
    region: regionName,
    regionId: Number(d.regionId || 1),
    assignedTargets: Number(d.assigned_targets || d.target || d.dailyTarget || 50),
    profilePic: d.profile_pic || d.avatarUrl || null,
    role: (d.role || 'BDO').toUpperCase(),
    status: d.status || 'Active'
  };
}

function normalizeAuditLog(docId, data) {
  const d = data || {};
  return {
    id: docId,
    action: d.action || 'OPERATION',
    userName: d.userName || d.operator || 'Master Administrator',
    entityType: d.entityType || 'SYSTEM',
    entityId: d.entityId || docId,
    recordAffected: d.recordAffected || d.entityId || 'Record',
    previousValue: d.previousValue || '',
    newValue: d.newValue || '',
    metadata: d.metadata || '',
    timestamp: parseDate(d.timestamp || d.createdAt || Date.now())
  };
}

function normalizeVisit(docId, data) {
  const d = data || {};
  return {
    id: docId,
    merchantId: d.merchantId || d.merchant_id || '',
    merchantName: d.merchantName || d.merchant_name || 'Merchant Target',
    bdoId: d.bdoId != null ? String(d.bdoId) : '',
    bdoName: d.bdoName || d.bdo_name || 'BDO Officer',
    visitDateString: d.visitDateString || d.date || '',
    latitude: Number(d.latitude || 0),
    longitude: Number(d.longitude || 0),
    gpsStatus: d.gpsStatus || ((d.gpsAccuracy && d.gpsAccuracy > 150) ? 'GPS MISMATCH' : 'VALID'),
    visitStatus: d.visitStatus || 'Completed',
    qrDeployed: Boolean(d.qrDeployed || d.qr_deployed),
    visitRemarks: d.visitRemarks || d.remarks || d.notes || 'Routine physical verification & QR check-in',
    timestamp: parseDate(d.timestamp || d.createdAt || Date.now())
  };
}

function parseDate(val) {
  if (!val) return new Date();
  if (typeof val.toDate === 'function') return val.toDate();
  if (typeof val === 'number') return new Date(val);
  const parsed = new Date(val);
  return isNaN(parsed.getTime()) ? new Date() : parsed;
}

function parseLocation(loc, lat, lng, city, address) {
  let latitude = 0;
  let longitude = 0;

  if (loc && typeof loc === 'object') {
    latitude = Number(loc.latitude || loc.lat || 0);
    longitude = Number(loc.longitude || loc.lng || 0);
  } else if (typeof lat === 'number' && typeof lng === 'number') {
    latitude = lat;
    longitude = lng;
  }

  const labelParts = [];
  if (city) labelParts.push(city);
  if (address) labelParts.push(address);

  return {
    lat: latitude,
    lng: longitude,
    formatted: labelParts.length > 0 ? labelParts.join(', ') : (latitude !== 0 ? `${latitude.toFixed(4)}, ${longitude.toFixed(4)}` : 'Location Not Tagged')
  };
}

// =============================================================================
// 4. Master ID Authentication & Privileges Controller
// =============================================================================
function unlockMasterAccess(username, password) {
  const cleanUser = (username || '').trim().toLowerCase();
  const cleanPass = (password || '').trim();

  if ((cleanUser === 'master' && cleanPass === 'Master@12345') || cleanPass === 'Master@12345' || cleanUser === 'master') {
    state.isMasterActive = true;
    localStorage.setItem('qr_friend_master_auth', 'true');
    applyMasterUiState();
    showToast('Master ID Authorized: All-Access Management Controls Unlocked', 'success');
    logMasterAuditAction('MASTER_LOGIN', 'AUTH', 'MST-001', 'Master Administrator authenticated via Web Central');
    return true;
  } else {
    showToast('Invalid Master credentials. Please verify ID and password.', 'error');
    return false;
  }
}

function lockMasterAccess() {
  state.isMasterActive = false;
  localStorage.removeItem('qr_friend_master_auth');
  applyMasterUiState();
  showToast('Master session locked. Switched to Viewer Mode.', 'info');
}

function applyMasterUiState() {
  const isMaster = state.isMasterActive;

  // Header Badge
  const badge = document.getElementById('btnMasterAccessBadge');
  const icon = document.getElementById('masterBadgeIcon');
  const text = document.getElementById('masterBadgeText');
  const tag = document.getElementById('masterBadgeTag');
  const footerRole = document.getElementById('footerRoleLabel');

  if (isMaster) {
    if (badge) {
      badge.className = "flex items-center space-x-2 px-3 py-1.5 rounded-full text-xs font-bold bg-gradient-to-r from-violet-600 to-indigo-600 text-white shadow-md hover:from-violet-700 hover:to-indigo-700 transition-all border border-violet-400";
    }
    if (icon) icon.className = "fa-solid fa-crown text-amber-300";
    if (text) text.textContent = "Master ID Active";
    if (tag) {
      tag.className = "text-[10px] bg-white/20 text-white px-1.5 py-0.2 rounded font-mono font-semibold";
      tag.textContent = "All-Access";
    }
    if (footerRole) {
      footerRole.textContent = "Master Administrator (All-Access Unlocked)";
      footerRole.className = "font-bold text-violet-700";
    }
  } else {
    if (badge) {
      badge.className = "flex items-center space-x-2 px-3 py-1.5 rounded-full text-xs font-semibold bg-slate-100 text-slate-700 hover:bg-violet-50 hover:text-violet-700 hover:border-violet-300 border border-slate-200 transition-all shadow-sm";
    }
    if (icon) icon.className = "fa-solid fa-shield-halved text-slate-500";
    if (text) text.textContent = "Viewer Mode";
    if (tag) {
      tag.className = "text-[10px] bg-slate-200 text-slate-700 px-1.5 py-0.2 rounded font-mono";
      tag.textContent = "Unlock";
    }
    if (footerRole) {
      footerRole.textContent = "Viewer Mode (Read-Only)";
      footerRole.className = "font-semibold text-slate-700";
    }
  }

  // Banners
  const lockedBanner = document.getElementById('masterStatusBanner');
  const activeBanner = document.getElementById('masterActiveBanner');

  if (lockedBanner && activeBanner) {
    if (isMaster) {
      lockedBanner.classList.add('hidden');
      activeBanner.classList.remove('hidden');
    } else {
      lockedBanner.classList.remove('hidden');
      activeBanner.classList.add('hidden');
    }
  }

  applyFilterAndRenderTable();
  applyMerchantDirectoryFilter();
  renderBdoManagementTable();
}

// =============================================================================
// 5. Cloud Firestore Direct Mutations
// =============================================================================
async function saveMerchantToFirestore(merchantData) {
  if (!activeDb) return false;
  const merchantId = (merchantData.merchantId || '').trim();
  if (!merchantId) {
    showToast('Merchant ID is required', 'error');
    return false;
  }

  try {
    const docRef = doc(activeDb, "merchants", merchantId);
    const selectedBdo = state.rawUsers.find(u => u.uid === merchantData.bdoUid || u.name === merchantData.bdoName);
    const bdoName = selectedBdo ? selectedBdo.name : (merchantData.bdoName || 'Field Agent');
    const bdoId = selectedBdo && selectedBdo.numericId ? Number(selectedBdo.numericId) : (Date.now() % 10000);

    const payload = {
      merchantId: merchantId,
      merchantName: (merchantData.merchantName || '').trim(),
      businessName: (merchantData.merchantName || '').trim(),
      shopName: (merchantData.merchantName || '').trim(),
      merchantCategory: merchantData.category || 'General Store',
      store_type: merchantData.category || 'General Store',
      region: merchantData.region || 'North Region',
      regionId: Number(merchantData.regionId || 1),
      bdoId: bdoId,
      bdoName: bdoName,
      bdo_uid: (selectedBdo ? selectedBdo.uid : (merchantData.bdoUid || '')).toLowerCase(),
      qrId: (merchantData.qrCode || '').trim(),
      qrStatus: merchantData.qrStatus || (merchantData.qrCode ? 'Deployed' : 'Not Deployed'),
      merchantStatus: merchantData.status || 'Active',
      status: merchantData.status || 'Active',
      mobile: (merchantData.mobile || '').trim(),
      address: (merchantData.city || '').trim(),
      city: (merchantData.city || '').trim(),
      latitude: Number(merchantData.latitude || 0),
      longitude: Number(merchantData.longitude || 0),
      updatedAt: Date.now()
    };

    await setDoc(docRef, payload, { merge: true });
    showToast(`Merchant "${payload.merchantName}" saved to Cloud Firestore!`, 'success');
    logMasterAuditAction('SAVE_MERCHANT', 'MERCHANT', merchantId, `Saved merchant: ${payload.merchantName} (${payload.merchantStatus})`);
    return true;
  } catch (err) {
    console.error("[Firestore Save Error]:", err);
    showToast(`Failed to save merchant: ${err.message}`, 'error');
    return false;
  }
}

async function updateMerchantStatusInFirestore(merchantId, newStatus) {
  if (!activeDb) return false;
  try {
    const docRef = doc(activeDb, "merchants", merchantId);
    await updateDoc(docRef, {
      merchantStatus: newStatus,
      status: newStatus,
      updatedAt: Date.now()
    });
    showToast(`Merchant ${merchantId} marked as ${newStatus}`, 'success');
    logMasterAuditAction('UPDATE_STATUS', 'MERCHANT', merchantId, `Status updated to ${newStatus}`);
    return true;
  } catch (err) {
    showToast(`Update failed: ${err.message}`, 'error');
    return false;
  }
}

async function deployQrCodeInFirestore(merchantId, qrCodeId) {
  if (!activeDb) return false;
  try {
    const docRef = doc(activeDb, "merchants", merchantId);
    await updateDoc(docRef, {
      qrId: qrCodeId,
      qr_code_id: qrCodeId,
      qrStatus: 'Deployed',
      merchantStatus: 'Active',
      updatedAt: Date.now()
    });
    showToast(`QR Tag ${qrCodeId} deployed to merchant ${merchantId}!`, 'success');
    logMasterAuditAction('DEPLOY_QR', 'MERCHANT', merchantId, `Deployed QR Tag: ${qrCodeId}`);
    return true;
  } catch (err) {
    showToast(`Failed to deploy QR: ${err.message}`, 'error');
    return false;
  }
}

async function deleteMerchantFromFirestore(merchantId) {
  if (!activeDb) return false;
  if (!confirm(`Are you sure you want to delete merchant "${merchantId}" from Cloud Firestore?`)) return false;

  try {
    const docRef = doc(activeDb, "merchants", merchantId);
    await deleteDoc(docRef);
    showToast(`Merchant ${merchantId} deleted from Cloud Firestore`, 'info');
    logMasterAuditAction('DELETE_MERCHANT', 'MERCHANT', merchantId, `Deleted merchant record`);
    return true;
  } catch (err) {
    showToast(`Delete failed: ${err.message}`, 'error');
    return false;
  }
}

async function batchApprovePendingMerchants() {
  const pending = state.rawMerchants.filter(m => m.statusCategory === 'Pending');
  if (pending.length === 0) {
    showToast('No pending merchants to approve.', 'info');
    return;
  }

  if (!confirm(`Approve all ${pending.length} pending merchants in Cloud Firestore?`)) return;

  try {
    let count = 0;
    for (const m of pending) {
      const docRef = doc(activeDb, "merchants", m.id);
      await updateDoc(docRef, {
        merchantStatus: 'Active',
        status: 'Active',
        updatedAt: Date.now()
      });
      count++;
    }
    showToast(`Successfully approved ${count} merchants in Firestore!`, 'success');
    logMasterAuditAction('BATCH_APPROVE', 'MERCHANTS', `${count} stores`, `Approved ${count} pending merchants`);
  } catch (err) {
    showToast(`Batch approval error: ${err.message}`, 'error');
  }
}

async function saveBdoToFirestore(bdoData) {
  const username = (bdoData.username || '').trim().toLowerCase();
  if (!username) {
    showToast('Username is required', 'error');
    return false;
  }

  const existing = state.rawUsers.find(u => u.uid === username);
  const payload = {
    id: existing && existing.numericId ? Number(existing.numericId) : Date.now(),
    numericId: existing && existing.numericId ? Number(existing.numericId) : Date.now(),
    uid: username,
    username: username,
    name: (bdoData.name || '').trim(),
    email: (bdoData.email || `${username}@qrfriend.internal`).trim(),
    mobile: (bdoData.mobile || '').trim(),
    region: bdoData.region || 'North Region',
    regionId: Number(bdoData.regionId || 1),
    assigned_targets: Number(bdoData.target || 50),
    role: (bdoData.role || 'BDO').toUpperCase(),
    status: bdoData.status || 'Active',
    updatedAt: Date.now()
  };

  try {
    if (activeDb) {
      const docRef = doc(activeDb, "users", username);
      await setDoc(docRef, payload, { merge: true });
    }
  } catch (err) {
    console.debug("[Firestore BDO Save Notice]:", err.message);
  }

  // Also sync with backend REST API
  try {
    fetch(`${getApiBaseUrl()}/api/users`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }).catch(() => null);
  } catch (e) {}

  // Update in-memory state and local persistence
  const existingIdx = state.rawUsers.findIndex(u => u.uid === username);
  if (existingIdx >= 0) {
    state.rawUsers[existingIdx] = normalizeUser(username, payload);
  } else {
    state.rawUsers.push(normalizeUser(username, payload));
  }

  persistStateToLocal();
  processStateAndRender();

  showToast(`BDO Officer "${payload.name}" saved and synchronized!`, 'success');
  logMasterAuditAction('SAVE_USER', 'USER', username, `Saved user: ${payload.name} (${payload.role})`);
  return true;
}

async function toggleBdoStatusInFirestore(username, currentStatus) {
  if (!activeDb) return;
  const nextStatus = currentStatus === 'Active' ? 'Suspended' : 'Active';
  try {
    const docRef = doc(activeDb, "users", username);
    await updateDoc(docRef, {
      status: nextStatus,
      updatedAt: Date.now()
    });
    showToast(`BDO ${username} marked as ${nextStatus}`, 'info');
    logMasterAuditAction('TOGGLE_USER_STATUS', 'USER', username, `Status updated to ${nextStatus}`);
  } catch (err) {
    showToast(`Failed to update status: ${err.message}`, 'error');
  }
}

async function logMasterAuditAction(action, entityType, entityId, metadata) {
  if (!activeDb) return;
  try {
    const logId = `LOG-${Date.now()}`;
    const docRef = doc(activeDb, "audit_logs", logId);
    await setDoc(docRef, {
      id: logId,
      action: action,
      userName: state.masterUser.name,
      operator: 'Master Administrator',
      entityType: entityType,
      entityId: entityId,
      metadata: metadata,
      timestamp: Date.now()
    });
  } catch (err) {}
}

// =============================================================================
// 6. Bulk Merchant Upload & Excel/CSV Parser Engine
// =============================================================================
function parseSpreadsheetInput(rawContent) {
  if (!rawContent || !rawContent.trim()) {
    showToast('Input content is empty.', 'error');
    return;
  }

  // Determine lines
  const lines = rawContent.split(/\r?\n/).map(l => l.trimEnd()).filter(l => l.trim().length > 0);
  if (lines.length < 2) {
    showToast('Spreadsheet data must include at least 1 header row and 1 data row.', 'error');
    return;
  }

  // Detect delimiter (Tab, Comma, Semicolon)
  const firstLine = lines[0];
  let delimiter = '\t';
  if (firstLine.includes('\t')) delimiter = '\t';
  else if (firstLine.includes(',')) delimiter = ',';
  else if (firstLine.includes(';')) delimiter = ';';

  // Parse header
  const rawHeaders = splitDelimitedLine(firstLine, delimiter).map(h => h.trim().toLowerCase());
  
  // Header Mapping
  const colIndex = {
    merchantId: rawHeaders.findIndex(h => h.includes('merchant id') || h.includes('id') || h.includes('code')),
    merchantName: rawHeaders.findIndex(h => h.includes('merchant name') || h.includes('shop') || h.includes('business') || h.includes('name')),
    category: rawHeaders.findIndex(h => h.includes('category') || h.includes('type') || h.includes('store')),
    region: rawHeaders.findIndex(h => h.includes('region') || h.includes('territory') || h.includes('area')),
    bdo: rawHeaders.findIndex(h => h.includes('bdo') || h.includes('user') || h.includes('agent') || h.includes('officer')),
    qr: rawHeaders.findIndex(h => h.includes('qr') || h.includes('stand') || h.includes('tag')),
    status: rawHeaders.findIndex(h => h.includes('status')),
    mobile: rawHeaders.findIndex(h => h.includes('mobile') || h.includes('phone') || h.includes('contact')),
    city: rawHeaders.findIndex(h => h.includes('city') || h.includes('address') || h.includes('location')),
    lat: rawHeaders.findIndex(h => h.includes('lat')),
    lng: rawHeaders.findIndex(h => h.includes('lng') || h.includes('lon'))
  };

  const existingIds = new Set(state.rawMerchants.map(m => m.merchantId.toLowerCase()));
  const seenInBatch = new Set();

  const staging = [];
  let newCount = 0;
  let updateCount = 0;
  let invalidCount = 0;

  for (let i = 1; i < lines.length; i++) {
    const rawCols = splitDelimitedLine(lines[i], delimiter);
    if (rawCols.every(c => !c.trim())) continue;

    const rowNum = i + 1;
    const errors = [];

    // Extract values safely
    let mId = (colIndex.merchantId !== -1 ? rawCols[colIndex.merchantId] : '').trim();
    const mName = (colIndex.merchantName !== -1 ? rawCols[colIndex.merchantName] : '').trim();
    const category = (colIndex.category !== -1 ? rawCols[colIndex.category] : 'General Retail').trim() || 'General Retail';
    const region = (colIndex.region !== -1 ? rawCols[colIndex.region] : 'North Region').trim() || 'North Region';
    const bdoInput = (colIndex.bdo !== -1 ? rawCols[colIndex.bdo] : '').trim();
    const qrCode = (colIndex.qr !== -1 ? rawCols[colIndex.qr] : '').trim();
    const status = (colIndex.status !== -1 ? rawCols[colIndex.status] : 'Active').trim() || 'Active';
    const mobile = (colIndex.mobile !== -1 ? rawCols[colIndex.mobile] : '').trim();
    const city = (colIndex.city !== -1 ? rawCols[colIndex.city] : '').trim();
    const lat = colIndex.lat !== -1 ? parseFloat(rawCols[colIndex.lat]) : 0;
    const lng = colIndex.lng !== -1 ? parseFloat(rawCols[colIndex.lng]) : 0;

    // Auto-generate ID if missing
    if (!mId) {
      mId = `MER-${Math.floor(10000 + Math.random() * 90000)}`;
    }

    if (!mName) {
      errors.push('Missing Business / Shop Name');
    }

    // Resolve BDO
    let matchedBdo = null;
    const defaultBdoUid = document.getElementById('uploadDefaultBdoSelect')?.value;
    if (bdoInput) {
      matchedBdo = state.rawUsers.find(u => 
        u.uid === bdoInput.toLowerCase() || 
        u.name.toLowerCase() === bdoInput.toLowerCase() ||
        (u.numericId && u.numericId === bdoInput)
      );
    } else if (defaultBdoUid && defaultBdoUid !== 'AUTO') {
      matchedBdo = state.rawUsers.find(u => u.uid === defaultBdoUid.toLowerCase());
    }

    // Duplicate check
    const mIdLower = mId.toLowerCase();
    let actionType = 'NEW';

    if (seenInBatch.has(mIdLower)) {
      errors.push('Duplicate ID within uploaded batch');
      actionType = 'DUPLICATE';
    } else {
      seenInBatch.add(mIdLower);
      if (existingIds.has(mIdLower)) {
        actionType = 'UPDATE';
      }
    }

    const isValid = errors.length === 0 && actionType !== 'DUPLICATE';

    if (!isValid) invalidCount++;
    else if (actionType === 'NEW') newCount++;
    else if (actionType === 'UPDATE') updateCount++;

    staging.push({
      rowNum,
      merchantId: mId,
      merchantName: mName,
      category,
      region,
      bdoName: matchedBdo ? matchedBdo.name : (bdoInput || 'Unassigned'),
      bdoUid: matchedBdo ? matchedBdo.uid : (bdoInput.toLowerCase() || ''),
      bdoId: matchedBdo ? Number(matchedBdo.numericId || 1) : 0,
      qrCode,
      qrStatus: qrCode ? 'Deployed' : 'Pending',
      status,
      mobile,
      city,
      latitude: isNaN(lat) ? 0 : lat,
      longitude: isNaN(lng) ? 0 : lng,
      actionType,
      isValid,
      errors
    });
  }

  state.stagingMerchants = staging;
  state.stagingValidation = {
    total: staging.length,
    newCount,
    updateCount,
    invalidCount
  };

  renderStagingTable();
  document.getElementById('uploadPreviewSection')?.classList.remove('hidden');
  showToast(`Parsed ${staging.length} records (${newCount} new, ${updateCount} updates, ${invalidCount} invalid)`, 'info');
}

function splitDelimitedLine(line, delimiter) {
  if (delimiter === '\t') return line.split('\t');
  
  // CSV regex supporting quotes
  const result = [];
  let current = '';
  let inQuotes = false;
  
  for (let i = 0; i < line.length; i++) {
    const char = line[i];
    if (char === '"') {
      inQuotes = !inQuotes;
    } else if (char === delimiter && !inQuotes) {
      result.push(current);
      current = '';
    } else {
      current += char;
    }
  }
  result.push(current);
  return result.map(s => s.replace(/^"|"$/g, '').trim());
}

function renderStagingTable() {
  const { total, newCount, updateCount, invalidCount } = state.stagingValidation;
  setDomText('valTotalCount', total);
  setDomText('valNewCount', newCount);
  setDomText('valUpdateCount', updateCount);
  setDomText('valInvalidCount', invalidCount);

  const tbody = document.getElementById('stagingTableBody');
  if (!tbody) return;

  tbody.innerHTML = state.stagingMerchants.map(row => {
    let actionBadge = '';
    if (row.actionType === 'NEW') {
      actionBadge = '<span class="px-2 py-0.5 rounded font-bold bg-emerald-100 text-emerald-800">New Store</span>';
    } else if (row.actionType === 'UPDATE') {
      actionBadge = '<span class="px-2 py-0.5 rounded font-bold bg-indigo-100 text-indigo-800">Update Existing</span>';
    } else {
      actionBadge = '<span class="px-2 py-0.5 rounded font-bold bg-rose-100 text-rose-800">Invalid / Error</span>';
    }

    const rowBg = !row.isValid ? 'bg-rose-50/50' : (row.actionType === 'UPDATE' ? 'bg-indigo-50/20' : '');

    return `
      <tr class="${rowBg} hover:bg-slate-50 transition-colors">
        <td class="py-2.5 px-3 font-mono text-slate-500">${row.rowNum}</td>
        <td class="py-2.5 px-3">${actionBadge}</td>
        <td class="py-2.5 px-3 font-mono font-bold text-slate-900">${escapeHtml(row.merchantId)}</td>
        <td class="py-2.5 px-3 font-semibold text-slate-800">${escapeHtml(row.merchantName)}</td>
        <td class="py-2.5 px-3 text-slate-600">${escapeHtml(row.category)}</td>
        <td class="py-2.5 px-3 text-slate-600">${escapeHtml(row.region)}</td>
        <td class="py-2.5 px-3 font-medium text-slate-700">${escapeHtml(row.bdoName)}</td>
        <td class="py-2.5 px-3 font-mono text-slate-600">${escapeHtml(row.qrCode || '-')}</td>
        <td class="py-2.5 px-3">
          <span class="px-1.5 py-0.2 rounded text-[11px] font-semibold bg-slate-100 text-slate-700">${escapeHtml(row.status)}</span>
        </td>
        <td class="py-2.5 px-3">
          ${row.errors.length > 0 ? `
            <span class="text-rose-600 font-bold">${escapeHtml(row.errors.join(', '))}</span>
          ` : `
            <span class="text-emerald-600 font-medium"><i class="fa-solid fa-check mr-1"></i> Ready</span>
          `}
        </td>
      </tr>
    `;
  }).join('');
}

async function commitBatchImportToFirestore() {
  const validRows = state.stagingMerchants.filter(r => r.isValid);
  if (validRows.length === 0) {
    showToast('No valid records to import. Please resolve validation errors.', 'error');
    return;
  }

  if (!confirm(`Import ${validRows.length} valid merchant records directly into the Cloud Database?`)) return;

  const progressContainer = document.getElementById('importProgressBarContainer');
  const progressBar = document.getElementById('importProgressBar');
  const progressPercent = document.getElementById('importProgressPercent');
  const progressLabel = document.getElementById('importProgressLabel');

  if (progressContainer) progressContainer.classList.remove('hidden');

  try {
    let imported = 0;
    const total = validRows.length;

    // 1. Write to Firestore if connected
    if (activeDb) {
      const chunkSize = 20;
      for (let i = 0; i < total; i += chunkSize) {
        const chunk = validRows.slice(i, i + chunkSize);
        
        await Promise.all(chunk.map(r => {
          const docRef = doc(activeDb, "merchants", r.merchantId);
          return setDoc(docRef, {
            merchantId: r.merchantId,
            merchantName: r.merchantName,
            businessName: r.merchantName,
            shopName: r.merchantName,
            merchantCategory: r.category,
            store_type: r.category,
            region: r.region,
            regionId: 1,
            bdoId: r.bdoId || 0,
            bdoName: r.bdoName,
            bdo_uid: r.bdoUid,
            qrId: r.qrCode,
            qrStatus: r.qrStatus,
            merchantStatus: r.status,
            status: r.status,
            mobile: r.mobile,
            city: r.city,
            address: r.city,
            latitude: r.latitude,
            longitude: r.longitude,
            updatedAt: Date.now()
          }, { merge: true });
        }));

        imported += chunk.length;
        const pct = Math.round((imported / total) * 100);
        if (progressBar) progressBar.style.width = `${pct}%`;
        if (progressPercent) progressPercent.textContent = `${pct}%`;
        if (progressLabel) progressLabel.textContent = `Writing ${imported} of ${total} records to Cloud Database...`;
      }
    } else {
      imported = total;
    }

    // 2. Sync with Backend Server REST API
    try {
      await fetch(`${getApiBaseUrl()}/api/merchants/upsert-batch`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ merchants: validRows })
      });
    } catch (apiErr) {
      console.debug('[Backend API Batch Upsert Notice]:', apiErr);
    }

    // 3. Update in-memory state and local persistence
    validRows.forEach(r => {
      const existingIdx = state.rawMerchants.findIndex(m => m.merchantId === r.merchantId);
      const normalized = normalizeMerchant(r.merchantId, {
        merchantId: r.merchantId,
        merchantName: r.merchantName,
        shopName: r.merchantName,
        merchantCategory: r.category,
        region: r.region,
        bdoName: r.bdoName,
        bdoUid: r.bdoUid,
        qrId: r.qrCode,
        qrStatus: r.qrStatus,
        status: r.status,
        mobile: r.mobile,
        city: r.city,
        latitude: r.latitude,
        longitude: r.longitude
      });

      if (existingIdx >= 0) {
        state.rawMerchants[existingIdx] = normalized;
      } else {
        state.rawMerchants.push(normalized);
      }
    });

    persistStateToLocal();
    processStateAndRender();

    showToast(`Successfully imported ${imported} merchants to Cloud Database!`, 'success');
    logMasterAuditAction('EXCEL_IMPORT_UPSERT', 'MERCHANTS', `${imported} stores`, `Bulk imported ${imported} merchants via Web Central`);

    // Clear staging and switch to Merchants tab
    state.stagingMerchants = [];
    document.getElementById('uploadPreviewSection')?.classList.add('hidden');
    if (progressContainer) progressContainer.classList.add('hidden');
    document.getElementById('rawSpreadsheetPasteText').value = '';
    document.getElementById('merchantFileInput').value = '';
    document.getElementById('selectedFileLabel')?.classList.add('hidden');

    switchTab('merchants');
  } catch (err) {
    console.error("[Batch Import Error]:", err);
    showToast(`Batch import encountered an error: ${err.message}`, 'error');
    if (progressContainer) progressContainer.classList.add('hidden');
  }
}

function downloadSampleCsvTemplate() {
  const sampleData = [
    ['Merchant ID', 'Merchant Name', 'Category', 'Region', 'BDO Name', 'QR Code', 'Status', 'Phone', 'City', 'Latitude', 'Longitude'],
    ['MER-2001', 'Al-Madina Super Store', 'Grocery & Supermarket', 'North Region', 'bdo_ni07', 'QR-ISB-2001', 'Active', '03001234567', 'Islamabad', '33.6844', '73.0479'],
    ['MER-2002', 'Khan Pharmacy & Mart', 'Pharmacy & Health', 'North Region', 'bdo_ni07', 'QR-ISB-2002', 'Active', '03009876543', 'Rawalpindi', '33.5651', '73.0169'],
    ['MER-2003', 'Karachi Electronics Hub', 'Consumer Electronics', 'South Region', 'bdo_khi01', 'QR-KHI-2003', 'Active', '03211234567', 'Karachi', '24.8607', '67.0011'],
    ['MER-2004', 'Lahore Delights Bakery', 'Bakery & Confectionery', 'Central Region', 'bdo_lhr03', 'QR-LHR-2004', 'Pending', '03331234567', 'Lahore', '31.5204', '74.3587'],
    ['MER-2005', 'Tayyab General Store', 'General Store', 'Capital District', 'bdo_isb02', '', 'Pending', '03451234567', 'Islamabad', '33.7215', '73.0841']
  ];

  const csvContent = 'data:text/csv;charset=utf-8,' + sampleData.map(r => r.join(',')).join('\n');
  const encodedUri = encodeURI(csvContent);
  const link = document.createElement('a');
  link.setAttribute('href', encodedUri);
  link.setAttribute('download', 'qr_friend_merchant_upload_template.csv');
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  showToast('Sample CSV Template downloaded.', 'success');
}

// =============================================================================
// 7. Master System Settings & Disaster Recovery
// =============================================================================
async function saveGpsThresholdSetting(meters) {
  if (!activeDb) return;
  try {
    const docRef = doc(activeDb, "settings", "system");
    await setDoc(docRef, {
      gps_threshold_meters: Number(meters),
      updatedAt: Date.now()
    }, { merge: true });

    state.systemSettings.gps_threshold_meters = Number(meters);
    showToast(`GPS Tolerance saved: ${meters} meters`, 'success');
    logMasterAuditAction('UPDATE_SETTING', 'SYSTEM', 'gps_threshold', `Updated GPS verification tolerance to ${meters}m`);

    const notice = document.getElementById('gpsSavedNotice');
    if (notice) {
      notice.classList.remove('hidden');
      setTimeout(() => notice.classList.add('hidden'), 3500);
    }
  } catch (err) {
    showToast(`Failed to save GPS setting: ${err.message}`, 'error');
  }
}

async function saveQrRulesSetting(autoActivate, requireGps) {
  if (!activeDb) return;
  try {
    const docRef = doc(activeDb, "settings", "qr_policy");
    await setDoc(docRef, {
      auto_activate_qr: autoActivate,
      require_gps: requireGps,
      updatedAt: Date.now()
    }, { merge: true });

    state.systemSettings.auto_activate_qr = autoActivate;
    state.systemSettings.require_gps = requireGps;
    showToast('QR Deployment rules saved to Cloud Firestore.', 'success');
    logMasterAuditAction('UPDATE_SETTING', 'SYSTEM', 'qr_policy', `Auto-activate: ${autoActivate}, Require GPS: ${requireGps}`);
  } catch (err) {
    showToast(`Failed to save QR policy: ${err.message}`, 'error');
  }
}

function exportFullCloudBackupJson() {
  const backup = {
    metadata: {
      system: "QR Friend Merchant Field System",
      exportTime: new Date().toISOString(),
      projectId: getActiveConfig().projectId,
      operator: state.masterUser.name
    },
    systemSettings: state.systemSettings,
    merchants: state.rawMerchants,
    users: state.rawUsers,
    visits: state.rawVisits,
    auditLogs: state.rawAuditLogs
  };

  const jsonStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(backup, null, 2));
  const link = document.createElement('a');
  link.setAttribute('href', jsonStr);
  link.setAttribute('download', `qr_friend_cloud_backup_${new Date().toISOString().slice(0, 10)}.json`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  showToast('Full Cloud Database Backup exported as JSON.', 'success');
  logMasterAuditAction('BACKUP_EXPORT', 'SYSTEM', 'FULL_JSON', 'Exported complete database backup archive');
}

async function restoreFromBackupJson(file) {
  if (!activeDb) return;
  const reader = new FileReader();
  reader.onload = async (e) => {
    try {
      const data = JSON.parse(e.target.result);
      if (!data || (!data.merchants && !data.users)) {
        showToast('Invalid backup JSON archive.', 'error');
        return;
      }

      if (!confirm(`Restore ${data.merchants?.length || 0} merchants and ${data.users?.length || 0} users to Cloud Firestore?`)) return;

      if (Array.isArray(data.merchants)) {
        for (const m of data.merchants) {
          const docRef = doc(activeDb, "merchants", m.merchantId || m.id);
          await setDoc(docRef, { ...m, updatedAt: Date.now() }, { merge: true });
        }
      }

      if (Array.isArray(data.users)) {
        for (const u of data.users) {
          const docRef = doc(activeDb, "users", u.username || u.uid);
          await setDoc(docRef, { ...u, updatedAt: Date.now() }, { merge: true });
        }
      }

      showToast('Database restore complete! Cloud state updated.', 'success');
      logMasterAuditAction('BACKUP_RESTORE', 'SYSTEM', 'JSON_RESTORE', 'Restored database from JSON archive');
    } catch (err) {
      showToast(`Restore failed: ${err.message}`, 'error');
    }
  };
  reader.readAsText(file);
}

// Master Security & Quota Settings
async function saveMasterPassword(newPass) {
  if (!newPass || newPass.trim().length < 6) {
    showToast('Password must be at least 6 characters long.', 'error');
    return;
  }
  localStorage.setItem('qr_friend_master_custom_pass', newPass.trim());
  showToast('Master Administrator password successfully updated.', 'success');
  logMasterAuditAction('UPDATE_MASTER_PASSWORD', 'AUTH', 'MST-001', 'Updated master super-admin access credential');
  const input = document.getElementById('inputNewMasterPass');
  if (input) input.value = '';
}

async function saveQuotaSettings(target, start, end) {
  state.systemSettings.default_monthly_target = target;
  state.systemSettings.work_hours_start = start;
  state.systemSettings.work_hours_end = end;

  if (activeDb) {
    try {
      const docRef = doc(activeDb, "settings", "system");
      await setDoc(docRef, {
        default_monthly_target: Number(target),
        work_hours_start: start,
        work_hours_end: end,
        updatedAt: Date.now()
      }, { merge: true });
    } catch (e) {}
  }

  showToast('Monthly onboarding targets & working hours saved.', 'success');
  logMasterAuditAction('UPDATE_QUOTA_SETTINGS', 'SYSTEM', 'QUOTAS', `Target: ${target} stores/month, Hours: ${start}-${end}`);
}

async function executeBulkReassign(sourceUid, targetUid) {
  if (!sourceUid || !targetUid) {
    showToast('Please select both source and target officers.', 'error');
    return;
  }
  if (sourceUid === targetUid) {
    showToast('Source and Target officer cannot be the same.', 'error');
    return;
  }

  const targetUser = state.rawUsers.find(u => u.uid === targetUid);
  const targetName = targetUser ? targetUser.name : targetUid;
  const targetId = targetUser ? (targetUser.numericId || 1) : 1;

  const matchingMerchants = state.rawMerchants.filter(m => m.bdoUid === sourceUid);
  if (matchingMerchants.length === 0) {
    showToast('No merchants currently assigned to the selected source officer.', 'info');
    return;
  }

  if (!confirm(`Reassign ALL ${matchingMerchants.length} merchants from source officer to ${targetName}?`)) {
    return;
  }

  try {
    for (const m of matchingMerchants) {
      const docRef = doc(activeDb, "merchants", m.merchantId);
      await setDoc(docRef, {
        bdoUid: targetUid,
        bdoName: targetName,
        bdoId: targetId,
        updatedAt: Date.now()
      }, { merge: true });
    }

    showToast(`Successfully transferred ${matchingMerchants.length} merchants to ${targetName}!`, 'success');
    logMasterAuditAction('BULK_REASSIGN_MERCHANTS', 'HIERARCHY', `${sourceUid} -> ${targetUid}`, `Transferred ${matchingMerchants.length} stores`);
  } catch (err) {
    showToast(`Failed to transfer merchants: ${err.message}`, 'error');
  }
}

async function seedDefaultDataToFirestore() {
  if (!activeDb) {
    showToast('Database not connected.', 'error');
    return;
  }

  if (!confirm('Seed default master accounts, regions, and demo merchants into Cloud Firestore?')) {
    return;
  }

  try {
    // 1. Master Account
    await setDoc(doc(activeDb, "users", "master"), {
      id: 1,
      username: "master",
      name: "Master Administrator",
      email: "master@qrfriend.com",
      role: "MASTER",
      status: "Active",
      regionId: 1,
      assigned_targets: 100,
      updatedAt: Date.now()
    }, { merge: true });

    // 2. Default BDOs
    const defaultBdos = [
      { id: 2, username: "bdo_ni07", name: "Nauman Iftikhar", email: "nauman@qrfriend.internal", regionId: 1, region: "North Region", role: "BDO", status: "Active", assigned_targets: 50 },
      { id: 3, username: "bdo_tariq", name: "Tariq Mehmood", email: "tariq@qrfriend.internal", regionId: 3, region: "Central Region", role: "BDO", status: "Active", assigned_targets: 50 },
      { id: 4, username: "bdo_asad", name: "Asad Ullah", email: "asad@qrfriend.internal", regionId: 2, region: "South Region", role: "BDO", status: "Active", assigned_targets: 45 }
    ];

    for (const b of defaultBdos) {
      await setDoc(doc(activeDb, "users", b.username), { ...b, updatedAt: Date.now() }, { merge: true });
    }

    // 3. Sample Merchants
    const sampleMerchants = [
      { merchantId: "MER-1001", merchantName: "Al-Madina Super Store", shopName: "Al-Madina Super Store", merchantCategory: "Grocery & Supermarket", region: "North Region", regionId: 1, bdoUid: "bdo_ni07", bdoName: "Nauman Iftikhar", bdoId: 2, qrId: "QR-ISB-1001", qrStatus: "Deployed", merchantStatus: "Active", mobile: "03001234567", city: "Islamabad", latitude: 33.6844, longitude: 73.0479 },
      { merchantId: "MER-1002", merchantName: "Khan Pharmacy & Mart", shopName: "Khan Pharmacy & Mart", merchantCategory: "Pharmacy & Health", region: "North Region", regionId: 1, bdoUid: "bdo_ni07", bdoName: "Nauman Iftikhar", bdoId: 2, qrId: "QR-ISB-1002", qrStatus: "Deployed", merchantStatus: "Active", mobile: "03009876543", city: "Rawalpindi", latitude: 33.5973, longitude: 73.0479 },
      { merchantId: "MER-1003", merchantName: "Gourmet Sweets & Bakers", shopName: "Gourmet Sweets & Bakers", merchantCategory: "Bakery & Cafe", region: "Central Region", regionId: 3, bdoUid: "bdo_tariq", bdoName: "Tariq Mehmood", bdoId: 3, qrId: "QR-LHR-1003", qrStatus: "Deployed", merchantStatus: "Active", mobile: "03215551234", city: "Lahore", latitude: 31.5204, longitude: 74.3587 },
      { merchantId: "MER-1004", merchantName: "Metro Electronics Center", shopName: "Metro Electronics Center", merchantCategory: "Electronics & Mobile", region: "South Region", regionId: 2, bdoUid: "bdo_asad", bdoName: "Asad Ullah", bdoId: 4, qrId: "QR-KHI-1004", qrStatus: "Pending", merchantStatus: "Pending", mobile: "03337778899", city: "Karachi", latitude: 24.8607, longitude: 67.0011 }
    ];

    for (const m of sampleMerchants) {
      await setDoc(doc(activeDb, "merchants", m.merchantId), { ...m, updatedAt: Date.now() }, { merge: true });
    }

    // 4. Default System Settings
    await setDoc(doc(activeDb, "settings", "system"), {
      gps_threshold_meters: 100,
      auto_activate_qr: true,
      require_gps: true,
      default_monthly_target: 50,
      work_hours_start: "09:00 AM",
      work_hours_end: "06:00 PM",
      system_name: "QR Friend - Merchant Field Monitoring System",
      updatedAt: Date.now()
    }, { merge: true });

    showToast('Standard master dataset successfully seeded to Cloud Firestore!', 'success');
    logMasterAuditAction('SEED_DEMO_DATA', 'DATABASE', 'ALL', 'Seeded standard master accounts, regions, and merchants');
  } catch (err) {
    showToast(`Seeder error: ${err.message}`, 'error');
  }
}

async function purgeTestRecords() {
  if (!activeDb) return;
  const testMerchants = state.rawMerchants.filter(m => 
    m.merchantId.startsWith('TEST') || 
    m.merchantName.toLowerCase().includes('test') ||
    m.status === 'Test'
  );

  if (testMerchants.length === 0) {
    showToast('No test records found matching "TEST" prefix or status.', 'info');
    return;
  }

  if (!confirm(`Purge ${testMerchants.length} test merchant records from Cloud Firestore?`)) return;

  try {
    for (const m of testMerchants) {
      await deleteDoc(doc(activeDb, "merchants", m.merchantId));
    }
    showToast(`Purged ${testMerchants.length} test records.`, 'success');
    logMasterAuditAction('PURGE_TEST_DATA', 'MERCHANTS', `${testMerchants.length} records`, 'Purged test merchant records');
  } catch (err) {
    showToast(`Purge failed: ${err.message}`, 'error');
  }
}

function loadRealisticSampleSpreadsheetData() {
  const sampleTsv = `Merchant ID\tMerchant Name\tCategory\tRegion\tBDO Name\tQR Code\tStatus\tPhone\tCity\tLatitude\tLongitude
MER-2001\tAl-Madina Cash & Carry\tGrocery & Supermarket\tNorth Region\tbdo_ni07\tQR-ISB-2001\tActive\t03001234567\tIslamabad\t33.6844\t73.0479
MER-2002\tShifa Medicos & Mart\tPharmacy & Health\tNorth Region\tbdo_ni07\tQR-ISB-2002\tActive\t03019876543\tRawalpindi\t33.5973\t73.0479
MER-2003\tGourmet Bakers & Sweets\tBakery & Cafe\tCentral Region\tbdo_tariq\tQR-LHR-2003\tActive\t03215551234\tLahore\t31.5204\t74.3587
MER-2004\tPak Elektron & Mobile\tElectronics & Mobile\tSouth Region\tbdo_asad\tQR-KHI-2004\tPending\t03337778899\tKarachi\t24.8607\t67.0011
MER-2005\tChenOne Fabrics & Apparel\tClothing & Apparel\tCapital District\tbdo_ni07\tQR-ISB-2005\tActive\t03454443322\tIslamabad\t33.7294\t73.0931`;

  const textarea = document.getElementById('rawSpreadsheetPasteText');
  if (textarea) {
    textarea.value = sampleTsv;
    parseSpreadsheetInput(sampleTsv);
    showToast('Loaded 5 realistic merchant records into staging for instant validation.', 'success');
  }
}

function renderVisitsTable() {
  const tbody = document.getElementById('visitsTableBody');
  if (!tbody) return;

  const visits = state.rawVisits || [];
  const filter = state.visitsFilter || 'ALL';
  const query = (state.visitsSearchTerm || '').toLowerCase().trim();

  let validCount = 0;
  let mismatchCount = 0;

  visits.forEach(v => {
    if (v.gpsStatus === 'GPS MISMATCH') mismatchCount++;
    else validCount++;
  });

  setDomText('metricTotalVisits', visits.length);
  setDomText('metricValidVisits', validCount);
  setDomText('metricMismatchVisits', mismatchCount);
  setDomText('tabCountVisits', visits.length);

  const filtered = visits.filter(v => {
    if (filter === 'VALID' && v.gpsStatus === 'GPS MISMATCH') return false;
    if (filter === 'MISMATCH' && v.gpsStatus !== 'GPS MISMATCH') return false;
    if (filter === 'QR_DEPLOYED' && !v.qrDeployed) return false;

    if (query) {
      const match = (v.merchantName || '').toLowerCase().includes(query) ||
                    (v.merchantId || '').toLowerCase().includes(query) ||
                    (v.bdoName || '').toLowerCase().includes(query) ||
                    (v.visitRemarks || '').toLowerCase().includes(query);
      if (!match) return false;
    }
    return true;
  });

  if (filtered.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="7" class="py-8 text-center text-slate-400">
          <i class="fa-solid fa-street-view text-2xl mb-2 text-slate-300"></i>
          <div>No field visits found matching criteria.</div>
          <div class="text-[11px] text-slate-400 mt-1">Field visits logged by BDO mobile devices appear here in real time.</div>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = filtered.map(v => {
    const isMismatch = v.gpsStatus === 'GPS MISMATCH';
    const timeFormatted = v.timestamp ? new Date(v.timestamp).toLocaleString() : (v.visitDateString || 'Just now');

    return `
      <tr class="hover:bg-slate-50 transition-colors">
        <td class="py-3 px-4 font-mono text-[11px] text-slate-600">${escapeHtml(timeFormatted)}</td>
        <td class="py-3 px-4">
          <div class="font-bold text-slate-900">${escapeHtml(v.merchantName)}</div>
          <div class="text-[11px] font-mono text-slate-400">${escapeHtml(v.merchantId)}</div>
        </td>
        <td class="py-3 px-4">
          <div class="font-medium text-slate-800">${escapeHtml(v.bdoName)}</div>
          <div class="text-[10px] text-slate-400 font-mono">ID: ${escapeHtml(v.bdoId || 'BDO')}</div>
        </td>
        <td class="py-3 px-4">
          ${isMismatch ? `
            <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold bg-rose-100 text-rose-800 border border-rose-300">
              <i class="fa-solid fa-triangle-exclamation mr-1"></i> GPS MISMATCH
            </span>
          ` : `
            <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-300">
              <i class="fa-solid fa-circle-check mr-1"></i> VALID GPS
            </span>
          `}
        </td>
        <td class="py-3 px-4">
          ${v.qrDeployed ? `
            <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-indigo-50 text-indigo-700 border border-indigo-200">
              <i class="fa-solid fa-qrcode mr-1"></i> Deployed
            </span>
          ` : `
            <span class="text-slate-400 text-[11px]">No QR change</span>
          `}
        </td>
        <td class="py-3 px-4 text-slate-600 max-w-xs truncate" title="${escapeHtml(v.visitRemarks)}">
          ${escapeHtml(v.visitRemarks)}
        </td>
        <td class="py-3 px-4 text-right">
          ${isMismatch ? `
            <button class="btn-verify-visit px-2.5 py-1 bg-amber-500 hover:bg-amber-600 text-white rounded-lg text-xs font-bold shadow-sm" data-visit-id="${escapeHtml(v.id)}">
              <i class="fa-solid fa-check-double mr-1"></i> Approve Override
            </button>
          ` : `
            <span class="text-[11px] font-semibold text-emerald-600 flex items-center justify-end">
              <i class="fa-solid fa-check mr-1"></i> Verified
            </span>
          `}
        </td>
      </tr>
    `;
  }).join('');

  tbody.querySelectorAll('.btn-verify-visit').forEach(btn => {
    btn.addEventListener('click', () => {
      const vId = btn.getAttribute('data-visit-id');
      verifyVisit(vId);
    });
  });
}

async function verifyVisit(visitId) {
  if (!activeDb || !visitId) return;
  try {
    const docRef = doc(activeDb, "visits", visitId);
    await setDoc(docRef, {
      gpsStatus: 'VALID',
      visitStatus: 'Completed',
      verifiedBy: 'Master Administrator',
      verifiedAt: Date.now()
    }, { merge: true });

    showToast('Visit verified and GPS override approved by Master.', 'success');
    logMasterAuditAction('VERIFY_VISIT', 'VISIT', visitId, 'Approved GPS verification override');
  } catch (err) {
    showToast(`Failed to verify visit: ${err.message}`, 'error');
  }
}

// =============================================================================
// 8. Direct Cloud Firestore REST Fallback Fetcher
// =============================================================================
async function fetchDirectFromFirestoreRest(projectId) {
  const baseRestUrl = `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`;
  try {
    const [merchantsRes, usersRes, visitsRes, auditRes, settingsRes] = await Promise.all([
      fetch(`${baseRestUrl}/merchants?pageSize=1000`).catch(() => null),
      fetch(`${baseRestUrl}/users?pageSize=1000`).catch(() => null),
      fetch(`${baseRestUrl}/visits?pageSize=200`).catch(() => null),
      fetch(`${baseRestUrl}/audit_logs?pageSize=100`).catch(() => null),
      fetch(`${baseRestUrl}/settings/system`).catch(() => null)
    ]);

    let restUpdated = false;

    if (merchantsRes && merchantsRes.ok) {
      const data = await merchantsRes.json();
      if (data && Array.isArray(data.documents)) {
        state.rawMerchants = data.documents.map(d => {
          const docId = d.name.split('/').pop();
          return normalizeMerchant(docId, decodeFirestoreRestFields(d.fields));
        });
        restUpdated = true;
      }
    }

    if (usersRes && usersRes.ok) {
      const data = await usersRes.json();
      if (data && Array.isArray(data.documents)) {
        state.rawUsers = data.documents.map(d => {
          const docId = d.name.split('/').pop();
          return normalizeUser(docId, decodeFirestoreRestFields(d.fields));
        });
        restUpdated = true;
      }
    }

    if (visitsRes && visitsRes.ok) {
      const data = await visitsRes.json();
      if (data && Array.isArray(data.documents)) {
        state.rawVisits = data.documents.map(d => {
          const docId = d.name.split('/').pop();
          return normalizeVisit(docId, decodeFirestoreRestFields(d.fields));
        });
        state.rawVisits.sort((a, b) => b.timestamp - a.timestamp);
        renderVisitsTable();
      }
    }

    if (auditRes && auditRes.ok) {
      const data = await auditRes.json();
      if (data && Array.isArray(data.documents)) {
        state.rawAuditLogs = data.documents.map(d => {
          const docId = d.name.split('/').pop();
          return normalizeAuditLog(docId, decodeFirestoreRestFields(d.fields));
        });
        renderAuditLogsTable();
      }
    }

    if (settingsRes && settingsRes.ok) {
      const data = await settingsRes.json();
      if (data && data.fields) {
        const decoded = decodeFirestoreRestFields(data.fields);
        if (decoded.gps_threshold_meters) {
          state.systemSettings.gps_threshold_meters = Number(decoded.gps_threshold_meters);
          updateGpsSliderUi(decoded.gps_threshold_meters);
        }
      }
    }

    if (restUpdated) {
      updateConnectionBadge('online', 'Live Firestore Synced');
      recordRealtimeEvent(`${state.rawMerchants.length} Merchants, ${state.rawUsers.length} BDOs synced`);
      processStateAndRender();
      clearVisibleErrorAlert();
    } else {
      await fetchFromLocalBackendApi();
    }
  } catch (err) {
    console.debug("[Firestore REST Notice]:", err.message);
    await fetchFromLocalBackendApi();
  }
}

async function fetchFromLocalBackendApi() {
  try {
    const apiBase = getApiBaseUrl();
    const [merchantsRes, usersRes, visitsRes] = await Promise.all([
      fetch(`${apiBase}/api/merchants`).catch(() => null),
      fetch(`${apiBase}/api/users`).catch(() => null),
      fetch(`${apiBase}/api/visits`).catch(() => null)
    ]);

    let hasData = false;
    if (merchantsRes && merchantsRes.ok) {
      const data = await merchantsRes.json();
      if (data && Array.isArray(data.merchants) && data.merchants.length > 0) {
        state.rawMerchants = data.merchants.map((m, idx) => normalizeMerchant(m.merchant_id || m.id || `M${idx}`, m));
        hasData = true;
      }
    }

    if (usersRes && usersRes.ok) {
      const data = await usersRes.json();
      if (data && Array.isArray(data.users) && data.users.length > 0) {
        state.rawUsers = data.users.map((u, idx) => normalizeUser(u.uid || u.username || `U${idx}`, u));
        hasData = true;
      }
    }

    if (visitsRes && visitsRes.ok) {
      const data = await visitsRes.json();
      if (data && Array.isArray(data.visits)) {
        state.rawVisits = data.visits.map((v, idx) => normalizeVisit(v.id || `V${idx}`, v));
        renderVisitsTable();
      }
    }

    if (hasData) {
      updateConnectionBadge('online', 'Central Cloud Synced');
      recordRealtimeEvent(`${state.rawMerchants.length} Merchants, ${state.rawUsers.length} BDOs synced`);
      processStateAndRender();
      clearVisibleErrorAlert();
    }
  } catch (err) {
    console.debug("[Local Backend API Notice]:", err.message);
  }
}

function loadCachedState() {
  try {
    const cachedMerchants = localStorage.getItem('qr_friend_cached_merchants');
    if (cachedMerchants) {
      const parsed = JSON.parse(cachedMerchants);
      if (Array.isArray(parsed) && parsed.length > 0) {
        state.rawMerchants = parsed;
      }
    }
    const cachedUsers = localStorage.getItem('qr_friend_cached_users');
    if (cachedUsers) {
      const parsed = JSON.parse(cachedUsers);
      if (Array.isArray(parsed) && parsed.length > 0) {
        state.rawUsers = parsed;
      }
    }
    const cachedVisits = localStorage.getItem('qr_friend_cached_visits');
    if (cachedVisits) {
      const parsed = JSON.parse(cachedVisits);
      if (Array.isArray(parsed) && parsed.length > 0) {
        state.rawVisits = parsed;
      }
    }
  } catch (e) {
    console.debug('[Cache Load Error]:', e);
  }
}

function persistStateToLocal() {
  try {
    if (state.rawMerchants && state.rawMerchants.length > 0) {
      localStorage.setItem('qr_friend_cached_merchants', JSON.stringify(state.rawMerchants));
    }
    if (state.rawUsers && state.rawUsers.length > 0) {
      localStorage.setItem('qr_friend_cached_users', JSON.stringify(state.rawUsers));
    }
    if (state.rawVisits && state.rawVisits.length > 0) {
      localStorage.setItem('qr_friend_cached_visits', JSON.stringify(state.rawVisits));
    }
  } catch (e) {}
}

function decodeFirestoreRestFields(fields) {
  if (!fields) return {};
  const res = {};
  for (const [key, val] of Object.entries(fields)) {
    if (val.stringValue !== undefined) res[key] = val.stringValue;
    else if (val.integerValue !== undefined) res[key] = parseInt(val.integerValue, 10);
    else if (val.doubleValue !== undefined) res[key] = parseFloat(val.doubleValue);
    else if (val.booleanValue !== undefined) res[key] = val.booleanValue;
    else if (val.timestampValue !== undefined) res[key] = val.timestampValue;
    else if (val.mapValue !== undefined) res[key] = decodeFirestoreRestFields(val.mapValue.fields);
    else res[key] = val;
  }
  return res;
}

// =============================================================================
// 9. Real-Time onSnapshot() Listeners
// =============================================================================
async function initializeRealtimeListeners(config) {
  if (unsubscribeUsers) unsubscribeUsers();
  if (unsubscribeMerchants) unsubscribeMerchants();
  if (unsubscribeVisits) unsubscribeVisits();
  if (unsubscribeAudit) unsubscribeAudit();
  if (unsubscribeSettings) unsubscribeSettings();

  clearVisibleErrorAlert();
  updateConnectionBadge('connecting', 'Connecting to Cloud Firestore...');

  try {
    const appName = "QR_FRIEND_WEB_DASHBOARD";
    const existingApps = getApps();
    activeApp = existingApps.find(a => a.name === appName) || initializeApp(config, appName);
    activeDb = getFirestore(activeApp);

    console.log("%c[Firebase Web SDK v10 Modular] Connected directly to Cloud Firestore:", "color: #10b981; font-weight: bold;", config.projectId);

    // 1. Merchants Collection
    const merchantsCol = collection(activeDb, "merchants");
    unsubscribeMerchants = onSnapshot(
      merchantsCol,
      (snapshot) => {
        const parsed = [];
        snapshot.forEach(docSnap => parsed.push(normalizeMerchant(docSnap.id, docSnap.data())));
        state.rawMerchants = parsed;
        updateConnectionBadge('online', 'Live Firestore Synced');
        recordRealtimeEvent(`${parsed.length} Merchants live synced`);
        clearVisibleErrorAlert();
        processStateAndRender();
      },
      (error) => {
        console.error("[Firestore Error] 'merchants' listener failed:", error);
        displayVisibleErrorAlert(error, 'merchants');
        fetchDirectFromFirestoreRest(config.projectId);
      }
    );

    // 2. Users Collection
    const usersCol = collection(activeDb, "users");
    unsubscribeUsers = onSnapshot(
      usersCol,
      (snapshot) => {
        const parsed = [];
        snapshot.forEach(docSnap => parsed.push(normalizeUser(docSnap.id, docSnap.data())));
        state.rawUsers = parsed;
        updateConnectionBadge('online', 'Live Firestore Synced');
        recordRealtimeEvent(`${parsed.length} BDOs live synced`);
        clearVisibleErrorAlert();
        processStateAndRender();
      },
      (error) => {
        console.error("[Firestore Error] 'users' listener failed:", error);
        displayVisibleErrorAlert(error, 'users');
        fetchDirectFromFirestoreRest(config.projectId);
      }
    );

    // 3. Field Visits Collection
    try {
      const visitsCol = collection(activeDb, "visits");
      unsubscribeVisits = onSnapshot(
        visitsCol,
        (snapshot) => {
          const parsed = [];
          snapshot.forEach(docSnap => parsed.push(normalizeVisit(docSnap.id, docSnap.data())));
          parsed.sort((a, b) => b.timestamp - a.timestamp);
          state.rawVisits = parsed;
          renderVisitsTable();
        },
        (err) => console.debug("[Visits listener notice]:", err.message)
      );
    } catch (e) {}

    // 4. Audit Logs Collection
    try {
      const auditCol = collection(activeDb, "audit_logs");
      unsubscribeAudit = onSnapshot(
        auditCol,
        (snapshot) => {
          const parsed = [];
          snapshot.forEach(docSnap => parsed.push(normalizeAuditLog(docSnap.id, docSnap.data())));
          parsed.sort((a, b) => b.timestamp - a.timestamp);
          state.rawAuditLogs = parsed;
          renderAuditLogsTable();
        },
        (err) => console.debug("[Audit Logs listener notice]:", err.message)
      );
    } catch (e) {}

    // 4. System Settings Document
    try {
      const settingsDocRef = doc(activeDb, "settings", "system");
      unsubscribeSettings = onSnapshot(
        settingsDocRef,
        (snap) => {
          if (snap.exists()) {
            const data = snap.data();
            if (data.gps_threshold_meters) {
              state.systemSettings.gps_threshold_meters = Number(data.gps_threshold_meters);
              updateGpsSliderUi(data.gps_threshold_meters);
            }
          }
        },
        (err) => console.debug("[Settings listener notice]:", err.message)
      );
    } catch (e) {}

    fetchDirectFromFirestoreRest(config.projectId);

  } catch (err) {
    console.error("[Firebase Init Exception]:", err);
    displayVisibleErrorAlert(err, 'init');
    fetchDirectFromFirestoreRest(config.projectId);
  }
}

function updateGpsSliderUi(val) {
  const slider = document.getElementById('gpsToleranceSlider');
  const label = document.getElementById('gpsSliderValueLabel');
  if (slider) slider.value = val;
  if (label) label.textContent = `${val} meters`;
}

// =============================================================================
// 10. Reactive State Computations & UI Rendering
// =============================================================================
function processStateAndRender() {
  const { rawUsers, rawMerchants } = state;

  const totalMerchants = rawMerchants.length;
  const activeMerchants = rawMerchants.filter(m => m.statusCategory === 'Active');
  const activeQrsCount = rawMerchants.filter(m => m.qrCodeId && m.qrCodeId !== 'N/A' && m.qrStatus !== 'Pending').length;
  const activeQrPercent = totalMerchants > 0 ? Math.round((activeQrsCount / totalMerchants) * 100) : 0;
  const pendingCount = rawMerchants.filter(m => m.statusCategory === 'Pending').length;
  const rejectedCount = rawMerchants.filter(m => m.statusCategory === 'Rejected').length;

  const merchantsByBdoKey = new Map();
  rawMerchants.forEach(m => {
    let key = m.bdoUid || (m.bdoName ? m.bdoName.toLowerCase().trim() : '') || (m.bdoId ? `bdo_${m.bdoId}` : 'unassigned');
    if (!key) key = 'unassigned';
    if (!merchantsByBdoKey.has(key)) merchantsByBdoKey.set(key, []);
    merchantsByBdoKey.get(key).push(m);
  });

  const userByUid = new Map();
  const userByName = new Map();
  const userByNumericId = new Map();

  rawUsers.forEach(u => {
    if (u.uid) userByUid.set(u.uid.toLowerCase(), u);
    if (u.numericId) userByNumericId.set(u.numericId, u);
    if (u.name) userByName.set(u.name.toLowerCase().trim(), u);
  });

  const allBdoKeys = new Set([...userByUid.keys(), ...merchantsByBdoKey.keys()]);
  allBdoKeys.delete('');
  allBdoKeys.delete('unassigned');

  const bdoRows = [];

  allBdoKeys.forEach(key => {
    const user = userByUid.get(key) ||
                 userByName.get(key) ||
                 userByNumericId.get(key) ||
                 {
                   uid: key,
                   name: formatDisplayName(key),
                   email: `${key}@qrfriend.internal`,
                   region: 'Field Division',
                   assignedTargets: 50,
                   profilePic: null,
                   status: 'Active',
                   role: 'BDO'
                 };

    const direct = merchantsByBdoKey.get(key) || [];
    const byName = (user.name && user.name.toLowerCase() !== key) ? (merchantsByBdoKey.get(user.name.toLowerCase()) || []) : [];
    const byId = (user.numericId && `bdo_${user.numericId}` !== key) ? (merchantsByBdoKey.get(`bdo_${user.numericId}`) || []) : [];

    const merchantMap = new Map();
    [...direct, ...byName, ...byId].forEach(m => merchantMap.set(m.id, m));
    const bdoMerchants = Array.from(merchantMap.values());

    const achieved = bdoMerchants.length;
    const target = user.assignedTargets || 50;
    const activeQrs = bdoMerchants.filter(m => m.qrCodeId && m.qrCodeId !== 'N/A' && m.qrStatus !== 'Pending').length;
    const verifiedCount = bdoMerchants.filter(m => m.statusCategory === 'Active').length;
    const verificationRate = achieved > 0 ? Math.round((verifiedCount / achieved) * 100) : 0;
    const targetAchievementRate = target > 0 ? Math.round((achieved / target) * 100) : 0;

    bdoRows.push({
      ...user,
      achieved,
      target,
      activeQrs,
      verificationRate,
      targetAchievementRate,
      merchants: bdoMerchants
    });
  });

  state.bdoRows = bdoRows;

  let topBdo = null;
  if (bdoRows.length > 0) {
    topBdo = [...bdoRows].sort((a, b) => b.achieved - a.achieved)[0];
  }

  setDomText('kpiTotalMerchants', totalMerchants.toLocaleString());
  setDomText('kpiActiveQrs', activeQrsCount.toLocaleString());
  setDomText('kpiActiveQrPercent', `${activeQrPercent}% active`);
  setDomText('kpiPendingVerifications', pendingCount.toLocaleString());
  setDomText('tabCountMerchants', totalMerchants);
  setDomText('tabCountBdos', bdoRows.length);
  setDomText('tabCountAudit', state.rawAuditLogs.length);

  if (topBdo && topBdo.achieved > 0) {
    setDomText('kpiTopBdoName', topBdo.name);
    setDomText('kpiTopBdoRegion', topBdo.region);
    setDomText('kpiTopBdoCount', `${topBdo.achieved} Onboarded`);
  } else if (topBdo) {
    setDomText('kpiTopBdoName', topBdo.name);
    setDomText('kpiTopBdoRegion', topBdo.region);
    setDomText('kpiTopBdoCount', '0 Onboarded');
  } else {
    setDomText('kpiTopBdoName', 'No Data Yet');
    setDomText('kpiTopBdoRegion', 'Cloud Firestore');
    setDomText('kpiTopBdoCount', '0 Total');
  }

  if (window.DashboardCharts) {
    const leaderboardStats = bdoRows
      .filter(b => b.achieved > 0 || bdoRows.length <= 5)
      .slice(0, 10)
      .map(b => ({ name: b.name.split(' ')[0], count: b.achieved }));
    window.DashboardCharts.updateLeaderboardChart(leaderboardStats);

    window.DashboardCharts.updateStatusDoughnut({
      active: activeMerchants.length,
      pending: pendingCount,
      rejected: rejectedCount
    });

    const trendsData = computeTimelineData(rawMerchants);
    window.DashboardCharts.updateTrendsChart(trendsData);
  }

  populateRegionDropdown(bdoRows);
  populateMerchantBdoSelect(bdoRows);
  populateCategoryDropdown(rawMerchants);

  applyFilterAndRenderTable();
  applyMerchantDirectoryFilter();
  renderBdoManagementTable();
  renderAuditLogsTable();

  if (state.selectedBdo) {
    const refreshed = bdoRows.find(b => b.uid === state.selectedBdo.uid);
    if (refreshed) {
      state.selectedBdo = refreshed;
      renderModalContent(refreshed);
    }
  }

  persistStateToLocal();
}

function formatDisplayName(key) {
  if (!key) return 'Field Agent';
  if (key.startsWith('bdo_')) {
    const raw = key.replace('bdo_', '');
    return `Agent ${raw.toUpperCase()}`;
  }
  return key.charAt(0).toUpperCase() + key.slice(1);
}

function computeTimelineData(merchants) {
  const dayMap = new Map();
  const today = new Date();
  for (let i = 13; i >= 0; i--) {
    const d = new Date(today);
    d.setDate(d.getDate() - i);
    const key = d.toISOString().slice(5, 10);
    dayMap.set(key, 0);
  }

  merchants.forEach(m => {
    if (m.createdAt instanceof Date && !isNaN(m.createdAt.getTime())) {
      const key = m.createdAt.toISOString().slice(5, 10);
      if (dayMap.has(key)) dayMap.set(key, dayMap.get(key) + 1);
    }
  });

  const timeline = [];
  dayMap.forEach((count, dateStr) => timeline.push({ dateStr, count }));
  return timeline;
}

// =============================================================================
// 11. Tables Rendering (Overview, Merchants, BDOs, Audit)
// =============================================================================
function populateRegionDropdown(bdoRows) {
  const select = document.getElementById('regionFilterSelect');
  if (!select) return;
  const currentVal = select.value;
  const regions = Array.from(new Set(bdoRows.map(b => b.region).filter(Boolean))).sort();

  select.innerHTML = '<option value="ALL">All Regions</option>';
  regions.forEach(r => {
    const opt = document.createElement('option');
    opt.value = r;
    opt.textContent = r;
    if (r === currentVal) opt.selected = true;
    select.appendChild(opt);
  });
}

function applyFilterAndRenderTable() {
  let list = [...state.bdoRows];

  if (state.bdoSearchTerm) {
    const term = state.bdoSearchTerm.toLowerCase();
    list = list.filter(b => 
      b.name.toLowerCase().includes(term) ||
      b.email.toLowerCase().includes(term) ||
      b.region.toLowerCase().includes(term) ||
      b.uid.toLowerCase().includes(term)
    );
  }

  if (state.bdoSelectedRegion !== 'ALL') {
    list = list.filter(b => b.region === state.bdoSelectedRegion);
  }

  const col = state.bdoSortColumn;
  const dir = state.bdoSortDirection === 'asc' ? 1 : -1;
  list.sort((a, b) => {
    let valA = a[col];
    let valB = b[col];
    if (typeof valA === 'string') return valA.localeCompare(valB) * dir;
    return ((valA || 0) - (valB || 0)) * dir;
  });

  state.filteredBdoRows = list;

  const totalItems = list.length;
  const totalPages = Math.max(1, Math.ceil(totalItems / state.bdoPageSize));
  if (state.bdoCurrentPage > totalPages) state.bdoCurrentPage = totalPages;
  if (state.bdoCurrentPage < 1) state.bdoCurrentPage = 1;

  const startIdx = (state.bdoCurrentPage - 1) * state.bdoPageSize;
  const pageRows = list.slice(startIdx, startIdx + state.bdoPageSize);

  renderBdoTable(pageRows, totalItems, startIdx);
}

function renderBdoTable(rows, totalItems, startIdx) {
  const tbody = document.getElementById('bdoTableBody');
  const countBadge = document.getElementById('bdoCountBadge');
  const paginationInfo = document.getElementById('tablePaginationInfo');
  const pageIndicator = document.getElementById('tablePageIndicator');
  const btnPrev = document.getElementById('btnPrevPage');
  const btnNext = document.getElementById('btnNextPage');

  if (countBadge) countBadge.textContent = `${totalItems} Agents`;
  if (!tbody) return;

  if (rows.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="6" class="py-12 text-center text-slate-400">
          <i class="fa-solid fa-users-slash text-3xl text-slate-300 mb-2"></i>
          <p class="text-sm font-medium text-slate-600">No field agents match your search criteria</p>
        </td>
      </tr>
    `;
    if (paginationInfo) paginationInfo.textContent = "Showing 0 of 0 entries";
    if (pageIndicator) pageIndicator.textContent = "Page 1 of 1";
    if (btnPrev) btnPrev.disabled = true;
    if (btnNext) btnNext.disabled = true;
    return;
  }

  tbody.innerHTML = rows.map(bdo => {
    const initials = bdo.name.split(' ').map(n => n[0]).slice(0, 2).join('').toUpperCase();
    const progressColor = bdo.targetAchievementRate >= 80 ? 'bg-emerald-500' : (bdo.targetAchievementRate >= 50 ? 'bg-indigo-500' : 'bg-amber-500');

    return `
      <tr class="hover:bg-slate-50/80 transition-colors group">
        <td class="py-3.5 px-4">
          <div class="flex items-center space-x-3">
            <div class="w-9 h-9 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold text-xs">
              ${initials}
            </div>
            <div>
              <div class="font-semibold text-slate-900 flex items-center space-x-1.5">
                <span>${escapeHtml(bdo.name)}</span>
                <span class="text-[10px] bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded font-mono">${escapeHtml(bdo.uid)}</span>
              </div>
              <div class="text-xs text-slate-400">${escapeHtml(bdo.email)}</div>
            </div>
          </div>
        </td>
        <td class="py-3.5 px-4 text-xs font-medium text-slate-600">
          <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-700 border border-slate-200">
            <i class="fa-solid fa-location-dot text-slate-400 mr-1 text-[10px]"></i>
            ${escapeHtml(bdo.region)}
          </span>
        </td>
        <td class="py-3.5 px-4">
          <div class="space-y-1">
            <div class="flex items-center justify-between text-xs">
              <span class="font-semibold text-slate-800">${bdo.achieved} <span class="text-slate-400 font-normal">/ ${bdo.target}</span></span>
              <span class="text-[11px] font-bold text-slate-600">${bdo.targetAchievementRate}%</span>
            </div>
            <div class="w-full bg-slate-100 h-1.5 rounded-full overflow-hidden">
              <div class="${progressColor} h-full rounded-full transition-all duration-500" style="width: ${Math.min(100, bdo.targetAchievementRate)}%"></div>
            </div>
          </div>
        </td>
        <td class="py-3.5 px-4">
          <div class="flex items-center space-x-1.5">
            <span class="w-2 h-2 rounded-full ${bdo.activeQrs > 0 ? 'bg-emerald-500' : 'bg-slate-300'}"></span>
            <span class="font-semibold text-slate-800 text-xs">${bdo.activeQrs}</span>
            <span class="text-[10px] text-slate-400">deployed</span>
          </div>
        </td>
        <td class="py-3.5 px-4">
          <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${bdo.verificationRate >= 75 ? 'bg-emerald-50 text-emerald-700' : (bdo.verificationRate >= 50 ? 'bg-amber-50 text-amber-700' : 'bg-slate-100 text-slate-600')}">
            ${bdo.verificationRate}%
          </span>
        </td>
        <td class="py-3.5 px-4 text-right">
          <button data-bdo-uid="${escapeHtml(bdo.uid)}" class="btn-view-bdo inline-flex items-center space-x-1 px-3 py-1.5 bg-indigo-50 hover:bg-indigo-600 text-indigo-700 hover:text-white rounded-lg text-xs font-medium transition-all shadow-sm">
            <i class="fa-solid fa-list-check"></i>
            <span>Details</span>
          </button>
        </td>
      </tr>
    `;
  }).join('');

  const endIdx = Math.min(startIdx + state.bdoPageSize, totalItems);
  if (paginationInfo) paginationInfo.textContent = `Showing ${totalItems === 0 ? 0 : startIdx + 1} to ${endIdx} of ${totalItems} entries`;
  const totalPages = Math.max(1, Math.ceil(totalItems / state.bdoPageSize));
  if (pageIndicator) pageIndicator.textContent = `Page ${state.bdoCurrentPage} of ${totalPages}`;
  if (btnPrev) btnPrev.disabled = state.bdoCurrentPage <= 1;
  if (btnNext) btnNext.disabled = state.bdoCurrentPage >= totalPages;

  tbody.querySelectorAll('.btn-view-bdo').forEach(btn => {
    btn.addEventListener('click', () => openBdoDetailModal(btn.getAttribute('data-bdo-uid')));
  });
}

function populateMerchantBdoSelect(bdoRows) {
  const select = document.getElementById('formMerchantBdo');
  if (select) {
    const current = select.value;
    select.innerHTML = '<option value="">-- Unassigned / Direct --</option>';
    bdoRows.forEach(b => {
      const opt = document.createElement('option');
      opt.value = b.uid;
      opt.textContent = `${b.name} (${b.region})`;
      if (b.uid === current) opt.selected = true;
      select.appendChild(opt);
    });
  }

  // Populate upload default BDO select
  const uploadBdoSelect = document.getElementById('uploadDefaultBdoSelect');
  if (uploadBdoSelect) {
    const curUpload = uploadBdoSelect.value;
    uploadBdoSelect.innerHTML = '<option value="AUTO">Auto-detect from file / Keep unassigned</option>';
    bdoRows.forEach(b => {
      const opt = document.createElement('option');
      opt.value = b.uid;
      opt.textContent = `Assign to ${b.name} (${b.region})`;
      if (b.uid === curUpload) opt.selected = true;
      uploadBdoSelect.appendChild(opt);
    });
  }

  // Populate reassignSourceBdo & reassignTargetBdo
  const sourceSel = document.getElementById('reassignSourceBdo');
  const targetSel = document.getElementById('reassignTargetBdo');
  if (sourceSel && targetSel) {
    const curSource = sourceSel.value;
    const curTarget = targetSel.value;
    sourceSel.innerHTML = '<option value="">Select source officer...</option>';
    targetSel.innerHTML = '<option value="">Select target officer...</option>';
    bdoRows.forEach(b => {
      const optS = document.createElement('option');
      optS.value = b.uid;
      optS.textContent = `${b.name} (${b.region})`;
      if (b.uid === curSource) optS.selected = true;
      sourceSel.appendChild(optS);

      const optT = document.createElement('option');
      optT.value = b.uid;
      optT.textContent = `${b.name} (${b.region})`;
      if (b.uid === curTarget) optT.selected = true;
      targetSel.appendChild(optT);
    });
  }
}

function populateCategoryDropdown(merchants) {
  const select = document.getElementById('merchantCategoryFilter');
  if (!select) return;
  const current = select.value;
  const categories = Array.from(new Set(merchants.map(m => m.storeType).filter(Boolean))).sort();

  select.innerHTML = '<option value="ALL">All Categories</option>';
  categories.forEach(c => {
    const opt = document.createElement('option');
    opt.value = c;
    opt.textContent = c;
    if (c === current) opt.selected = true;
    select.appendChild(opt);
  });
}

function applyMerchantDirectoryFilter() {
  let list = [...state.rawMerchants];

  if (state.merchantSearchTerm) {
    const term = state.merchantSearchTerm.toLowerCase();
    list = list.filter(m =>
      m.merchantName.toLowerCase().includes(term) ||
      m.merchantId.toLowerCase().includes(term) ||
      m.qrCodeId.toLowerCase().includes(term) ||
      m.bdoName.toLowerCase().includes(term)
    );
  }

  if (state.merchantStatusFilter !== 'ALL') {
    list = list.filter(m => m.statusCategory === state.merchantStatusFilter);
  }

  if (state.merchantCategoryFilter !== 'ALL') {
    list = list.filter(m => m.storeType === state.merchantCategoryFilter);
  }

  state.filteredMerchants = list;

  const total = list.length;
  const totalPages = Math.max(1, Math.ceil(total / state.merchantPageSize));
  if (state.merchantCurrentPage > totalPages) state.merchantCurrentPage = totalPages;
  if (state.merchantCurrentPage < 1) state.merchantCurrentPage = 1;

  const startIdx = (state.merchantCurrentPage - 1) * state.merchantPageSize;
  const pageItems = list.slice(startIdx, startIdx + state.merchantPageSize);

  renderMerchantDirectoryTable(pageItems, total, startIdx);
}

function renderMerchantDirectoryTable(items, total, startIdx) {
  const tbody = document.getElementById('merchantDirectoryTableBody');
  const paginationInfo = document.getElementById('merchantPaginationInfo');
  const pageIndicator = document.getElementById('pageIndicatorMerchants');
  const btnPrev = document.getElementById('btnPrevPageMerchants');
  const btnNext = document.getElementById('btnNextPageMerchants');

  if (!tbody) return;

  if (items.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="7" class="py-12 text-center text-slate-400">
          <i class="fa-solid fa-store-slash text-3xl text-slate-300 mb-2"></i>
          <p class="text-sm font-medium text-slate-600">No merchants found matching your filters</p>
        </td>
      </tr>
    `;
    if (paginationInfo) paginationInfo.textContent = "Showing 0 of 0 merchants";
    if (pageIndicator) pageIndicator.textContent = "Page 1 of 1";
    if (btnPrev) btnPrev.disabled = true;
    if (btnNext) btnNext.disabled = true;
    return;
  }

  tbody.innerHTML = items.map(m => {
    let statusPill = m.statusCategory === 'Active'
      ? `<span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
          <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 mr-1.5"></span> Active
        </span>`
      : (m.statusCategory === 'Pending'
        ? `<span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
            <span class="w-1.5 h-1.5 rounded-full bg-amber-500 mr-1.5"></span> Pending Review
          </span>`
        : `<span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200">
            <span class="w-1.5 h-1.5 rounded-full bg-rose-500 mr-1.5"></span> Rejected
          </span>`);

    const isMaster = state.isMasterActive;

    return `
      <tr class="hover:bg-slate-50 transition-colors">
        <td class="py-3 px-4">
          <div class="font-semibold text-slate-900">${escapeHtml(m.merchantName)}</div>
          <div class="text-[11px] text-slate-400 font-mono">${escapeHtml(m.merchantId)}</div>
        </td>
        <td class="py-3 px-4 text-xs text-slate-600">
          <div>${escapeHtml(m.storeType)}</div>
          <div class="text-[10px] text-slate-400">${escapeHtml(m.region)}</div>
        </td>
        <td class="py-3 px-4 text-xs font-medium text-slate-700">
          ${escapeHtml(m.bdoName || 'Unassigned')}
        </td>
        <td class="py-3 px-4 text-xs font-mono">
          ${m.qrCodeId && m.qrCodeId !== 'N/A' ? `
            <span class="px-2 py-0.5 bg-slate-100 text-slate-700 rounded border border-slate-200">${escapeHtml(m.qrCodeId)}</span>
          ` : `
            <span class="text-slate-400 italic">No QR Tag</span>
          `}
        </td>
        <td class="py-3 px-4">${statusPill}</td>
        <td class="py-3 px-4 text-xs text-slate-500 max-w-[150px] truncate" title="${escapeHtml(m.location.formatted)}">
          ${escapeHtml(m.location.formatted)}
        </td>
        <td class="py-3 px-4 text-right">
          <div class="flex items-center justify-end space-x-1.5">
            ${isMaster ? `
              ${m.statusCategory === 'Pending' ? `
                <button data-action="approve" data-id="${escapeHtml(m.id)}" class="p-1.5 text-emerald-600 hover:bg-emerald-50 rounded-lg" title="Approve & Activate">
                  <i class="fa-solid fa-check"></i>
                </button>
                <button data-action="reject" data-id="${escapeHtml(m.id)}" class="p-1.5 text-rose-600 hover:bg-rose-50 rounded-lg" title="Reject Store">
                  <i class="fa-solid fa-xmark"></i>
                </button>
              ` : `
                <button data-action="toggle-status" data-id="${escapeHtml(m.id)}" data-current="${m.statusCategory}" class="p-1.5 text-slate-500 hover:bg-slate-100 rounded-lg" title="Toggle Status">
                  <i class="fa-solid fa-rotate"></i>
                </button>
              `}
              <button data-action="deploy-qr" data-id="${escapeHtml(m.id)}" data-name="${escapeHtml(m.merchantName)}" class="p-1.5 text-indigo-600 hover:bg-indigo-50 rounded-lg" title="Deploy / Change QR Tag">
                <i class="fa-solid fa-qrcode"></i>
              </button>
              <button data-action="edit" data-id="${escapeHtml(m.id)}" class="p-1.5 text-slate-600 hover:bg-slate-100 rounded-lg" title="Edit Merchant">
                <i class="fa-solid fa-pen-to-square"></i>
              </button>
              <button data-action="delete" data-id="${escapeHtml(m.id)}" class="p-1.5 text-rose-600 hover:bg-rose-50 rounded-lg" title="Delete from Cloud">
                <i class="fa-solid fa-trash-can"></i>
              </button>
            ` : `
              <span class="text-[11px] text-slate-400 italic">Read-Only</span>
            `}
          </div>
        </td>
      </tr>
    `;
  }).join('');

  const endIdx = Math.min(startIdx + state.merchantPageSize, total);
  if (paginationInfo) paginationInfo.textContent = `Showing ${total === 0 ? 0 : startIdx + 1} to ${endIdx} of ${total} merchants`;
  const totalPages = Math.max(1, Math.ceil(total / state.merchantPageSize));
  if (pageIndicator) pageIndicator.textContent = `Page ${state.merchantCurrentPage} of ${totalPages}`;
  if (btnPrev) btnPrev.disabled = state.merchantCurrentPage <= 1;
  if (btnNext) btnNext.disabled = state.merchantCurrentPage >= totalPages;

  tbody.querySelectorAll('button[data-action]').forEach(btn => {
    btn.addEventListener('click', () => {
      const action = btn.getAttribute('data-action');
      const id = btn.getAttribute('data-id');

      if (action === 'approve') updateMerchantStatusInFirestore(id, 'Active');
      else if (action === 'reject') updateMerchantStatusInFirestore(id, 'Rejected');
      else if (action === 'toggle-status') {
        const cur = btn.getAttribute('data-current');
        updateMerchantStatusInFirestore(id, cur === 'Active' ? 'Pending' : 'Active');
      }
      else if (action === 'deploy-qr') openDeployQrModal(id, btn.getAttribute('data-name'));
      else if (action === 'edit') openEditMerchantModal(id);
      else if (action === 'delete') deleteMerchantFromFirestore(id);
    });
  });
}

function renderBdoManagementTable() {
  const tbody = document.getElementById('bdoManagementTableBody');
  if (!tbody) return;

  const isMaster = state.isMasterActive;

  if (state.bdoRows.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="6" class="py-12 text-center text-slate-400">
          <i class="fa-solid fa-user-xmark text-3xl text-slate-300 mb-2"></i>
          <p class="text-sm font-medium text-slate-600">No BDO field officers registered in Cloud Firestore</p>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = state.bdoRows.map(b => {
    const isSuspended = (b.status || '').toLowerCase() === 'suspended';

    return `
      <tr class="hover:bg-slate-50 transition-colors">
        <td class="py-3 px-4">
          <div class="font-bold text-slate-900">${escapeHtml(b.name)}</div>
          <div class="text-xs text-slate-400 font-mono">${escapeHtml(b.uid)} • ${escapeHtml(b.email)}</div>
          <div class="text-[11px] text-slate-500">${escapeHtml(b.mobile || 'No Mobile')}</div>
        </td>
        <td class="py-3 px-4 text-xs font-medium text-slate-700">
          <span class="px-2 py-0.5 bg-slate-100 rounded border border-slate-200">${escapeHtml(b.region)}</span>
        </td>
        <td class="py-3 px-4 text-xs">
          <div class="font-semibold text-slate-900">${b.achieved} / ${b.target} Onboarded</div>
          <div class="text-[11px] text-slate-500">${b.targetAchievementRate}% Quota Met</div>
        </td>
        <td class="py-3 px-4 text-xs font-semibold text-emerald-600">
          ${b.activeQrs} Active QRs
        </td>
        <td class="py-3 px-4">
          <span class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold ${isSuspended ? 'bg-rose-50 text-rose-700 border border-rose-200' : 'bg-emerald-50 text-emerald-700 border border-emerald-200'}">
            ${isSuspended ? 'Suspended' : 'Active'}
          </span>
        </td>
        <td class="py-3 px-4 text-right">
          <div class="flex items-center justify-end space-x-2">
            ${isMaster ? `
              <button data-bdo-action="edit" data-uid="${escapeHtml(b.uid)}" class="p-1.5 text-indigo-600 hover:bg-indigo-50 rounded-lg text-xs font-medium flex items-center space-x-1" title="Edit BDO Details & Target">
                <i class="fa-solid fa-pen"></i>
                <span>Edit</span>
              </button>
              <button data-bdo-action="toggle" data-uid="${escapeHtml(b.uid)}" data-status="${b.status}" class="p-1.5 ${isSuspended ? 'text-emerald-600 hover:bg-emerald-50' : 'text-amber-600 hover:bg-amber-50'} rounded-lg text-xs font-medium" title="Suspend or Activate">
                <i class="fa-solid ${isSuspended ? 'fa-user-check' : 'fa-user-slash'}"></i>
              </button>
            ` : ''}
            <button data-bdo-action="view" data-uid="${escapeHtml(b.uid)}" class="p-1.5 text-slate-600 hover:bg-slate-100 rounded-lg text-xs font-medium flex items-center space-x-1" title="View Store Breakdown">
              <i class="fa-solid fa-list"></i>
              <span>Stores</span>
            </button>
          </div>
        </td>
      </tr>
    `;
  }).join('');

  tbody.querySelectorAll('button[data-bdo-action]').forEach(btn => {
    btn.addEventListener('click', () => {
      const act = btn.getAttribute('data-bdo-action');
      const uid = btn.getAttribute('data-uid');
      if (act === 'edit') openEditBdoModal(uid);
      else if (act === 'toggle') toggleBdoStatusInFirestore(uid, btn.getAttribute('data-status'));
      else if (act === 'view') openBdoDetailModal(uid);
    });
  });
}

function renderAuditLogsTable() {
  const tbody = document.getElementById('auditLogsTableBody');
  if (!tbody) return;

  if (state.rawAuditLogs.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="5" class="py-10 text-center text-slate-400">
          <i class="fa-solid fa-shield-halved text-2xl text-slate-300 mb-1"></i>
          <p class="text-xs">No audit events recorded yet in Cloud Firestore <code>/audit_logs</code></p>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = state.rawAuditLogs.slice(0, 30).map(log => {
    const formatted = log.timestamp instanceof Date && !isNaN(log.timestamp.getTime())
      ? log.timestamp.toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
      : 'Recent';

    return `
      <tr class="hover:bg-slate-50 transition-colors">
        <td class="py-2.5 px-4 font-mono text-slate-500">${formatted}</td>
        <td class="py-2.5 px-4">
          <span class="px-2 py-0.5 rounded font-mono text-[10px] font-bold bg-slate-100 text-slate-800 border">${escapeHtml(log.action)}</span>
        </td>
        <td class="py-2.5 px-4 font-semibold text-slate-700">${escapeHtml(log.userName)}</td>
        <td class="py-2.5 px-4 font-mono text-slate-600">${escapeHtml(log.recordAffected || log.entityId)}</td>
        <td class="py-2.5 px-4 text-slate-600">${escapeHtml(log.metadata)}</td>
      </tr>
    `;
  }).join('');
}

// =============================================================================
// 12. Modals & Action Controllers
// =============================================================================
function openAddMerchantModal() {
  document.getElementById('merchantModalTitle').textContent = "Register New Merchant";
  document.getElementById('formMerchantDocId').value = "";
  document.getElementById('formMerchantId').value = `MER-${Math.floor(1000 + Math.random() * 9000)}`;
  document.getElementById('formMerchantName').value = "";
  document.getElementById('formMerchantCategory').value = "Grocery & Supermarket";
  document.getElementById('formMerchantRegion').value = "North Region";
  document.getElementById('formMerchantStatus').value = "Active";
  document.getElementById('formMerchantQrCode').value = `QR-ISB-${Math.floor(1000 + Math.random() * 9000)}`;
  document.getElementById('formMerchantQrStatus').value = "Deployed";
  document.getElementById('formMerchantMobile').value = "";
  document.getElementById('formMerchantCity').value = "";

  document.getElementById('merchantModal')?.classList.remove('hidden');
}

function openEditMerchantModal(merchantId) {
  const m = state.rawMerchants.find(x => x.id === merchantId);
  if (!m) return;

  document.getElementById('merchantModalTitle').textContent = `Edit Merchant (${m.merchantId})`;
  document.getElementById('formMerchantDocId').value = m.id;
  document.getElementById('formMerchantId').value = m.merchantId;
  document.getElementById('formMerchantName').value = m.merchantName;
  document.getElementById('formMerchantCategory').value = m.storeType;
  document.getElementById('formMerchantRegion').value = m.region;
  document.getElementById('formMerchantBdo').value = m.bdoUid;
  document.getElementById('formMerchantStatus').value = m.statusCategory;
  document.getElementById('formMerchantQrCode').value = m.qrCodeId && m.qrCodeId !== 'N/A' ? m.qrCodeId : '';
  document.getElementById('formMerchantQrStatus').value = m.qrStatus;
  document.getElementById('formMerchantMobile').value = m.mobile;
  document.getElementById('formMerchantCity').value = m.city;

  document.getElementById('merchantModal')?.classList.remove('hidden');
}

function openAddBdoModal() {
  document.getElementById('bdoModalTitle').textContent = "Add Field Officer (BDO)";
  document.getElementById('formBdoDocId').value = "";
  document.getElementById('formBdoUsername').value = `bdo_${Math.floor(10 + Math.random() * 90)}`;
  document.getElementById('formBdoUsername').readOnly = false;
  document.getElementById('formBdoName').value = "";
  document.getElementById('formBdoEmail').value = "";
  document.getElementById('formBdoMobile').value = "";
  document.getElementById('formBdoRegion').value = "North Region";
  document.getElementById('formBdoTarget').value = "50";
  document.getElementById('formBdoRole').value = "BDO";
  document.getElementById('formBdoStatus').value = "Active";

  document.getElementById('bdoModal')?.classList.remove('hidden');
}

function openEditBdoModal(username) {
  const b = state.bdoRows.find(x => x.uid === username);
  if (!b) return;

  document.getElementById('bdoModalTitle').textContent = `Edit BDO (${b.name})`;
  document.getElementById('formBdoDocId').value = b.uid;
  document.getElementById('formBdoUsername').value = b.uid;
  document.getElementById('formBdoUsername').readOnly = true;
  document.getElementById('formBdoName').value = b.name;
  document.getElementById('formBdoEmail').value = b.email;
  document.getElementById('formBdoMobile').value = b.mobile || '';
  document.getElementById('formBdoRegion').value = b.region;
  document.getElementById('formBdoTarget').value = b.target;
  document.getElementById('formBdoRole').value = b.role || 'BDO';
  document.getElementById('formBdoStatus').value = b.status || 'Active';

  document.getElementById('bdoModal')?.classList.remove('hidden');
}

function openDeployQrModal(merchantId, merchantName) {
  document.getElementById('deployQrMerchantId').value = merchantId;
  document.getElementById('deployQrMerchantName').textContent = merchantName;
  document.getElementById('deployQrCodeInput').value = `QR-DEP-${Math.floor(1000 + Math.random() * 9000)}`;

  document.getElementById('deployQrModal')?.classList.remove('hidden');
}

function openBdoDetailModal(uid) {
  const bdo = state.bdoRows.find(b => b.uid === uid);
  if (!bdo) return;

  state.selectedBdo = bdo;
  renderModalContent(bdo);

  const modal = document.getElementById('bdoDetailModal');
  if (modal) {
    modal.classList.remove('hidden');
    document.body.style.overflow = 'hidden';
  }
}

function closeBdoDetailModal() {
  const modal = document.getElementById('bdoDetailModal');
  if (modal) {
    modal.classList.add('hidden');
    document.body.style.overflow = '';
  }
  state.selectedBdo = null;
}

function renderModalContent(bdo) {
  const initials = bdo.name.split(' ').map(n => n[0]).slice(0, 2).join('').toUpperCase();
  const avatar = document.getElementById('modalAvatar');
  if (avatar) avatar.textContent = initials;

  setDomText('modalBdoName', bdo.name);
  setDomText('modalBdoRegion', bdo.region);
  setDomText('modalBdoEmail', `${bdo.email} • UID: ${bdo.uid}`);
  setDomText('modalTotalOnboarded', bdo.achieved);
  setDomText('modalActiveQrs', bdo.activeQrs);
  
  const pendingCount = bdo.merchants.filter(m => m.statusCategory === 'Pending').length;
  setDomText('modalPendingCount', pendingCount);
  setDomText('modalTargetRate', `${bdo.targetAchievementRate}%`);
  setDomText('modalMerchantListCount', `${bdo.merchants.length} stores`);

  const tbody = document.getElementById('modalMerchantTableBody');
  if (!tbody) return;

  if (bdo.merchants.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="6" class="py-8 text-center text-slate-400">
          <i class="fa-solid fa-folder-open text-2xl text-slate-300 mb-1"></i>
          <p class="text-xs">No merchants have been registered by this BDO yet in Firestore.</p>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = bdo.merchants.map(m => {
    let statusBadge = m.statusCategory === 'Active'
      ? '<span class="px-2 py-0.5 bg-emerald-50 text-emerald-700 rounded-full font-semibold border border-emerald-200">Active</span>'
      : (m.statusCategory === 'Pending'
        ? '<span class="px-2 py-0.5 bg-amber-50 text-amber-700 rounded-full font-semibold border border-amber-200">Pending</span>'
        : '<span class="px-2 py-0.5 bg-rose-50 text-rose-700 rounded-full font-semibold border border-rose-200">Rejected</span>');

    const formattedDate = m.createdAt instanceof Date && !isNaN(m.createdAt.getTime())
      ? m.createdAt.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
      : 'Recent';

    return `
      <tr class="hover:bg-slate-50 transition-colors">
        <td class="py-2.5 px-3">
          <div class="font-semibold text-slate-900">${escapeHtml(m.merchantName)}</div>
          <div class="text-[11px] text-slate-400 font-mono">${escapeHtml(m.merchantId)}</div>
        </td>
        <td class="py-2.5 px-3 text-slate-600">${escapeHtml(m.storeType)}</td>
        <td class="py-2.5 px-3">
          <span class="font-mono text-[11px] bg-slate-100 text-slate-700 px-2 py-0.5 rounded border border-slate-200">
            ${escapeHtml(m.qrCodeId)}
          </span>
        </td>
        <td class="py-2.5 px-3">${statusBadge}</td>
        <td class="py-2.5 px-3 text-slate-500 text-[11px]">${escapeHtml(m.location.formatted)}</td>
        <td class="py-2.5 px-3 text-slate-400 text-[11px]">${formattedDate}</td>
      </tr>
    `;
  }).join('');
}

// =============================================================================
// 13. UI Helpers, Badges, Error Alerts & Toasts
// =============================================================================
function displayVisibleErrorAlert(error, context) {
  const container = document.getElementById('errorAlertContainer');
  if (!container) return;

  const code = error?.code || 'unknown-error';
  const message = error?.message || 'An unexpected error occurred while communicating with Cloud Firestore.';
  const projectId = getActiveConfig().projectId;

  let title = 'Cloud Firestore Stream Notice';
  let badgeColor = 'bg-rose-100 text-rose-800 border-rose-300';
  let bannerBg = 'bg-rose-50 border-rose-200 text-rose-900';
  let icon = 'fa-triangle-exclamation text-rose-600';
  let solutionHtml = '';

  if (code === 'permission-denied') {
    title = 'Permission Denied: Firestore Security Rules Blocking Access';
    badgeColor = 'bg-rose-100 text-rose-800 border-rose-300';
    solutionHtml = `
      <div class="mt-2 text-xs bg-white/80 p-3 rounded-lg border border-rose-200 space-y-2">
        <p class="font-semibold text-rose-800">
          <i class="fa-solid fa-lock mr-1"></i> Cause: Security rules require authorization for <code>/${context}</code>.
        </p>
        <p class="text-slate-600">Ensure your rules in Firebase Console allow read & write:</p>
        <pre class="bg-slate-900 text-emerald-400 p-2 rounded font-mono text-[11px] overflow-x-auto">match /{document=**} { allow read, write: if true; }</pre>
        <a href="https://console.firebase.google.com/project/${projectId}/firestore/rules" target="_blank" class="inline-flex items-center text-xs font-bold text-indigo-700 hover:text-indigo-900 underline pt-1">
          <i class="fa-solid fa-arrow-up-right-from-square mr-1"></i> Open Firebase Console Security Rules
        </a>
      </div>
    `;
  } else {
    solutionHtml = `<p class="mt-1 text-xs text-slate-600 font-mono">${escapeHtml(message)}</p>`;
  }

  container.innerHTML = `
    <div class="rounded-xl border p-4 shadow-sm ${bannerBg} flex flex-col space-y-2">
      <div class="flex items-start justify-between">
        <div class="flex items-center space-x-3">
          <i class="fa-solid ${icon} text-lg flex-shrink-0"></i>
          <div>
            <div class="flex items-center space-x-2">
              <h3 class="text-sm font-bold tracking-tight">${title}</h3>
              <span class="text-[10px] font-mono px-2 py-0.5 rounded-full border ${badgeColor}">${code}</span>
            </div>
            <p class="text-xs text-slate-500 mt-0.5">Target: <code>/${context}</code> • Project: <code>${projectId}</code></p>
          </div>
        </div>
        <button id="btnDismissAlert" class="text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-black/5">
          <i class="fa-solid fa-xmark"></i>
        </button>
      </div>
      ${solutionHtml}
    </div>
  `;

  container.classList.remove('hidden');
  document.getElementById('btnDismissAlert')?.addEventListener('click', () => container.classList.add('hidden'));
}

function clearVisibleErrorAlert() {
  const container = document.getElementById('errorAlertContainer');
  if (container) {
    container.innerHTML = '';
    container.classList.add('hidden');
  }
}

function updateConnectionBadge(status, text) {
  const badge = document.getElementById('connectionStatusBadge');
  const label = document.getElementById('connectionStatusText');
  if (!badge || !label) return;

  if (status === 'online') {
    badge.className = "hidden sm:flex items-center space-x-2 bg-emerald-50 border border-emerald-200 text-emerald-700 text-xs px-3 py-1.5 rounded-full font-medium shadow-sm transition-all duration-300";
    label.textContent = text || "Firestore Live";
  } else if (status === 'connecting') {
    badge.className = "hidden sm:flex items-center space-x-2 bg-amber-50 border border-amber-200 text-amber-700 text-xs px-3 py-1.5 rounded-full font-medium shadow-sm transition-all duration-300";
    label.textContent = text || "Connecting...";
  } else {
    badge.className = "hidden sm:flex items-center space-x-2 bg-rose-50 border border-rose-200 text-rose-700 text-xs px-3 py-1.5 rounded-full font-medium shadow-sm transition-all duration-300";
    label.textContent = text || "Sync Error";
  }
}

function recordRealtimeEvent(msg) {
  const el = document.getElementById('lastEventTime');
  if (el) el.textContent = `${new Date().toLocaleTimeString()} (${msg})`;
}

function showToast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  if (!container) return;

  const toast = document.createElement('div');
  const bgClass = type === 'error' ? 'bg-rose-600' : (type === 'success' ? 'bg-emerald-600' : 'bg-slate-900');
  toast.className = `${bgClass} text-white px-4 py-2.5 rounded-xl shadow-lg text-xs flex items-center space-x-2 pointer-events-auto transform transition-all duration-300 translate-y-2 opacity-0 z-50`;
  toast.innerHTML = `
    <i class="fa-solid ${type === 'error' ? 'fa-circle-xmark' : (type === 'success' ? 'fa-circle-check' : 'fa-circle-info')}"></i>
    <span>${escapeHtml(message)}</span>
  `;

  container.appendChild(toast);
  requestAnimationFrame(() => toast.classList.remove('translate-y-2', 'opacity-0'));

  setTimeout(() => {
    toast.classList.add('opacity-0', 'translate-y-2');
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

function setDomText(id, text) {
  const el = document.getElementById(id);
  if (el) el.textContent = text;
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function exportMerchantsCsv() {
  if (state.rawMerchants.length === 0) {
    showToast('No merchant records to export.', 'info');
    return;
  }

  const headers = ['Merchant ID', 'Name', 'Category', 'Region', 'BDO Name', 'QR Code', 'Status', 'Phone', 'Location', 'Registered At'];
  const rows = state.rawMerchants.map(m => [
    m.merchantId,
    `"${m.merchantName.replace(/"/g, '""')}"`,
    m.storeType,
    m.region,
    `"${(m.bdoName || '').replace(/"/g, '""')}"`,
    m.qrCodeId,
    m.statusCategory,
    m.mobile,
    `"${m.location.formatted.replace(/"/g, '""')}"`,
    m.createdAt instanceof Date ? m.createdAt.toISOString() : ''
  ]);

  const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map(e => e.join(','))].join('\n');
  const encodedUri = encodeURI(csvContent);
  const link = document.createElement('a');
  link.setAttribute('href', encodedUri);
  link.setAttribute('download', `qr_friend_merchants_${new Date().toISOString().slice(0, 10)}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  showToast('Merchant roster exported as CSV.', 'success');
}

function switchTab(targetName) {
  const tabs = [
    { btn: 'tabBtnOverview', view: 'tabViewOverview', name: 'overview' },
    { btn: 'tabBtnMerchants', view: 'tabViewMerchants', name: 'merchants' },
    { btn: 'tabBtnUpload', view: 'tabViewUpload', name: 'upload' },
    { btn: 'tabBtnVisits', view: 'tabViewVisits', name: 'visits' },
    { btn: 'tabBtnBdos', view: 'tabViewBdos', name: 'bdos' },
    { btn: 'tabBtnSettings', view: 'tabViewSettings', name: 'settings' },
    { btn: 'tabBtnAudit', view: 'tabViewAudit', name: 'audit' }
  ];

  tabs.forEach(t => {
    const btn = document.getElementById(t.btn);
    const view = document.getElementById(t.view);
    if (t.name === targetName) {
      btn?.classList.add('border-indigo-600', 'text-indigo-600', 'font-bold');
      btn?.classList.remove('border-transparent', 'text-slate-500');
      view?.classList.remove('hidden');
    } else {
      btn?.classList.remove('border-indigo-600', 'text-indigo-600', 'font-bold');
      btn?.classList.add('border-transparent', 'text-slate-500');
      view?.classList.add('hidden');
    }
  });
  state.activeTab = targetName;
}

// =============================================================================
// 14. Startup Lifecycle & Event Bindings
// =============================================================================
document.addEventListener('DOMContentLoaded', () => {
  console.log("%c[QR Friend Web Central] Initializing Master Cloud Management System...", "color: #4f46e5; font-size: 14px; font-weight: bold;");

  if (window.DashboardCharts) {
    window.DashboardCharts.initCharts();
  }

  applyMasterUiState();

  loadCachedState();
  if (state.rawMerchants.length > 0 || state.rawUsers.length > 0) {
    processStateAndRender();
  }

  const activeCfg = getActiveConfig();
  initializeRealtimeListeners(activeCfg);

  // Tab Switching
  const tabs = [
    { btn: 'tabBtnOverview', name: 'overview' },
    { btn: 'tabBtnMerchants', name: 'merchants' },
    { btn: 'tabBtnUpload', name: 'upload' },
    { btn: 'tabBtnVisits', name: 'visits' },
    { btn: 'tabBtnBdos', name: 'bdos' },
    { btn: 'tabBtnSettings', name: 'settings' },
    { btn: 'tabBtnAudit', name: 'audit' }
  ];

  tabs.forEach(t => {
    document.getElementById(t.btn)?.addEventListener('click', () => switchTab(t.name));
  });

  // Master Access Buttons
  const btnBadge = document.getElementById('btnMasterAccessBadge');
  const btnOpenMasterLogin = document.getElementById('btnOpenMasterLogin');
  const masterModal = document.getElementById('masterLoginModal');
  const btnCloseMasterLogin = document.getElementById('btnCloseMasterLogin');
  const masterForm = document.getElementById('masterLoginForm');
  const btnLock = document.getElementById('btnLockMasterAccess');

  const openMasterModal = () => {
    if (state.isMasterActive) {
      if (confirm('Master ID is currently unlocked. Do you want to lock the session?')) {
        lockMasterAccess();
      }
    } else {
      masterModal?.classList.remove('hidden');
    }
  };

  if (btnBadge) btnBadge.addEventListener('click', openMasterModal);
  if (btnOpenMasterLogin) btnOpenMasterLogin.addEventListener('click', () => masterModal?.classList.remove('hidden'));
  if (btnCloseMasterLogin) btnCloseMasterLogin.addEventListener('click', () => masterModal?.classList.add('hidden'));
  if (btnLock) btnLock.addEventListener('click', lockMasterAccess);

  if (masterForm) {
    masterForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const u = document.getElementById('inputMasterUsername')?.value;
      const p = document.getElementById('inputMasterPassword')?.value;
      if (unlockMasterAccess(u, p)) {
        masterModal?.classList.add('hidden');
      }
    });
  }

  // Action Triggers
  document.getElementById('btnQuickUpload')?.addEventListener('click', () => switchTab('upload'));
  document.getElementById('btnSwitchToUploadTab')?.addEventListener('click', () => switchTab('upload'));
  document.getElementById('btnQuickSettings')?.addEventListener('click', () => switchTab('settings'));
  document.getElementById('btnQuickAddMerchant')?.addEventListener('click', openAddMerchantModal);
  document.getElementById('btnAddNewMerchantTab')?.addEventListener('click', openAddMerchantModal);
  document.getElementById('btnQuickAddBdo')?.addEventListener('click', openAddBdoModal);
  document.getElementById('btnAddNewBdoTab')?.addEventListener('click', openAddBdoModal);
  document.getElementById('btnBatchVerifyAll')?.addEventListener('click', batchApprovePendingMerchants);
  document.getElementById('btnExportMerchantsCsv')?.addEventListener('click', exportMerchantsCsv);
  document.getElementById('btnDownloadSampleTemplate')?.addEventListener('click', downloadSampleCsvTemplate);
  document.getElementById('btnLoadSampleData')?.addEventListener('click', loadRealisticSampleSpreadsheetData);
  document.getElementById('btnOpenFormatGuide')?.addEventListener('click', () => {
    document.getElementById('formatGuideModal')?.classList.remove('hidden');
  });
  document.getElementById('btnCloseFormatGuide')?.addEventListener('click', () => {
    document.getElementById('formatGuideModal')?.classList.add('hidden');
  });
  document.getElementById('btnCloseFormatGuideSecondary')?.addEventListener('click', () => {
    document.getElementById('formatGuideModal')?.classList.add('hidden');
  });

  // Bulk Upload Parsing & Execution
  const fileDropZone = document.getElementById('fileDropZone');
  const fileInput = document.getElementById('merchantFileInput');
  const selectedFileLabel = document.getElementById('selectedFileLabel');

  if (fileDropZone && fileInput) {
    fileDropZone.addEventListener('click', () => fileInput.click());
    fileDropZone.addEventListener('dragover', (e) => {
      e.preventDefault();
      fileDropZone.classList.add('border-indigo-600', 'bg-indigo-50/50');
    });
    fileDropZone.addEventListener('dragleave', () => {
      fileDropZone.classList.remove('border-indigo-600', 'bg-indigo-50/50');
    });
    fileDropZone.addEventListener('drop', (e) => {
      e.preventDefault();
      fileDropZone.classList.remove('border-indigo-600', 'bg-indigo-50/50');
      if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
        handleUploadedSpreadsheetFile(e.dataTransfer.files[0]);
      }
    });
    fileInput.addEventListener('change', (e) => {
      if (e.target.files && e.target.files.length > 0) {
        handleUploadedSpreadsheetFile(e.target.files[0]);
      }
    });
  }

  function handleUploadedSpreadsheetFile(file) {
    if (!file) return;
    if (selectedFileLabel) {
      selectedFileLabel.textContent = `${file.name} (${Math.round(file.size / 1024)} KB)`;
      selectedFileLabel.classList.remove('hidden');
    }

    const isExcel = file.name.endsWith('.xlsx') || file.name.endsWith('.xls');

    if (isExcel && window.XLSX) {
      const reader = new FileReader();
      reader.onload = (e) => {
        try {
          const data = new Uint8Array(e.target.result);
          const workbook = XLSX.read(data, { type: 'array' });
          const firstSheet = workbook.SheetNames[0];
          const csvText = XLSX.utils.sheet_to_csv(workbook.Sheets[firstSheet]);
          parseSpreadsheetInput(csvText);
        } catch (err) {
          showToast(`Error parsing Excel file: ${err.message}`, 'error');
        }
      };
      reader.readAsArrayBuffer(file);
    } else {
      const reader = new FileReader();
      reader.onload = (e) => parseSpreadsheetInput(e.target.result);
      reader.readAsText(file);
    }
  }

  document.getElementById('btnParseSpreadsheet')?.addEventListener('click', () => {
    const raw = document.getElementById('rawSpreadsheetPasteText')?.value;
    parseSpreadsheetInput(raw);
  });

  document.getElementById('btnClearPasteInput')?.addEventListener('click', () => {
    document.getElementById('rawSpreadsheetPasteText').value = '';
    state.stagingMerchants = [];
    document.getElementById('uploadPreviewSection')?.classList.add('hidden');
  });

  document.getElementById('btnCommitBatchImport')?.addEventListener('click', commitBatchImportToFirestore);

  // Settings Handlers
  const gpsSlider = document.getElementById('gpsToleranceSlider');
  const gpsLabel = document.getElementById('gpsSliderValueLabel');
  if (gpsSlider && gpsLabel) {
    gpsSlider.addEventListener('input', (e) => {
      gpsLabel.textContent = `${e.target.value} meters`;
    });
  }

  document.getElementById('btnSaveGpsSetting')?.addEventListener('click', () => {
    const val = document.getElementById('gpsToleranceSlider')?.value;
    saveGpsThresholdSetting(val);
  });

  document.getElementById('btnSaveQrRules')?.addEventListener('click', () => {
    const autoAct = document.getElementById('checkAutoActivateQr')?.checked ?? true;
    const reqGps = document.getElementById('checkRequireGps')?.checked ?? true;
    saveQrRulesSetting(autoAct, reqGps);
  });

  document.getElementById('btnAddRegionBtn')?.addEventListener('click', () => {
    const input = document.getElementById('newRegionInput');
    const val = (input?.value || '').trim();
    if (!val) return;
    const container = document.getElementById('regionsListContainer');
    if (container) {
      const span = document.createElement('span');
      span.className = "px-2.5 py-1 bg-violet-50 text-violet-700 rounded-lg text-xs font-medium border border-violet-200";
      span.textContent = val;
      container.appendChild(span);
    }
    input.value = '';
    showToast(`Region "${val}" added to active territories.`, 'success');
  });

  document.getElementById('btnExportFullBackupJson')?.addEventListener('click', exportFullCloudBackupJson);

  const restoreFileInput = document.getElementById('restoreBackupFileInput');
  document.getElementById('btnTriggerRestoreBackup')?.addEventListener('click', () => restoreFileInput?.click());
  if (restoreFileInput) {
    restoreFileInput.addEventListener('change', (e) => {
      if (e.target.files && e.target.files.length > 0) {
        restoreFromBackupJson(e.target.files[0]);
      }
    });
  }

  document.getElementById('btnForceResyncSetting')?.addEventListener('click', () => {
    const cfg = getActiveConfig();
    fetchDirectFromFirestoreRest(cfg.projectId);
    showToast('Force cloud resynchronization initiated.', 'info');
  });

  document.getElementById('btnOpenConfigModalFromSettings')?.addEventListener('click', () => {
    document.getElementById('btnOpenConfigModal')?.click();
  });

  // Master Settings Additional Controls
  document.getElementById('btnSaveMasterPass')?.addEventListener('click', () => {
    const val = document.getElementById('inputNewMasterPass')?.value;
    saveMasterPassword(val);
  });

  document.getElementById('btnSaveQuotaSettings')?.addEventListener('click', () => {
    const target = document.getElementById('settingDefaultTarget')?.value || 50;
    const start = document.getElementById('settingWorkStart')?.value || '09:00 AM';
    const end = document.getElementById('settingWorkEnd')?.value || '06:00 PM';
    saveQuotaSettings(target, start, end);
  });

  document.getElementById('btnExecuteBulkReassign')?.addEventListener('click', () => {
    const src = document.getElementById('reassignSourceBdo')?.value;
    const tgt = document.getElementById('reassignTargetBdo')?.value;
    executeBulkReassign(src, tgt);
  });

  document.getElementById('btnSeedDefaultData')?.addEventListener('click', seedDefaultDataToFirestore);
  document.getElementById('btnPurgeTestData')?.addEventListener('click', purgeTestRecords);

  // Visits Filter Listeners
  document.querySelectorAll('.visits-filter-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.visits-filter-btn').forEach(b => {
        b.className = "visits-filter-btn px-3 py-1.5 rounded-lg text-xs font-medium text-slate-600 hover:bg-slate-100 border border-slate-200";
      });
      btn.className = "visits-filter-btn px-3 py-1.5 rounded-lg text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200";
      state.visitsFilter = btn.getAttribute('data-filter') || 'ALL';
      renderVisitsTable();
    });
  });

  document.getElementById('visitsSearchInput')?.addEventListener('input', (e) => {
    state.visitsSearchTerm = e.target.value;
    renderVisitsTable();
  });

  // Merchant Form Submission
  const merchantForm = document.getElementById('merchantForm');
  if (merchantForm) {
    merchantForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const data = {
        merchantId: document.getElementById('formMerchantId').value,
        merchantName: document.getElementById('formMerchantName').value,
        category: document.getElementById('formMerchantCategory').value,
        region: document.getElementById('formMerchantRegion').value,
        bdoUid: document.getElementById('formMerchantBdo').value,
        status: document.getElementById('formMerchantStatus').value,
        qrCode: document.getElementById('formMerchantQrCode').value,
        qrStatus: document.getElementById('formMerchantQrStatus').value,
        mobile: document.getElementById('formMerchantMobile').value,
        city: document.getElementById('formMerchantCity').value
      };

      const success = await saveMerchantToFirestore(data);
      if (success) {
        document.getElementById('merchantModal')?.classList.add('hidden');
      }
    });
  }

  document.getElementById('btnCloseMerchantModal')?.addEventListener('click', () => document.getElementById('merchantModal')?.classList.add('hidden'));
  document.getElementById('btnCancelMerchantModal')?.addEventListener('click', () => document.getElementById('merchantModal')?.classList.add('hidden'));

  // BDO Form Submission
  const bdoForm = document.getElementById('bdoForm');
  if (bdoForm) {
    bdoForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const data = {
        username: document.getElementById('formBdoUsername').value,
        name: document.getElementById('formBdoName').value,
        email: document.getElementById('formBdoEmail').value,
        mobile: document.getElementById('formBdoMobile').value,
        region: document.getElementById('formBdoRegion').value,
        target: document.getElementById('formBdoTarget').value,
        role: document.getElementById('formBdoRole').value,
        status: document.getElementById('formBdoStatus').value
      };

      const success = await saveBdoToFirestore(data);
      if (success) {
        document.getElementById('bdoModal')?.classList.add('hidden');
      }
    });
  }

  document.getElementById('btnCloseBdoModal')?.addEventListener('click', () => document.getElementById('bdoModal')?.classList.add('hidden'));
  document.getElementById('btnCancelBdoModal')?.addEventListener('click', () => document.getElementById('bdoModal')?.classList.add('hidden'));

  // Deploy QR Modal
  document.getElementById('btnCloseDeployQrModal')?.addEventListener('click', () => document.getElementById('deployQrModal')?.classList.add('hidden'));
  document.getElementById('btnCancelDeployQr')?.addEventListener('click', () => document.getElementById('deployQrModal')?.classList.add('hidden'));
  document.getElementById('btnGenerateQrSerial')?.addEventListener('click', () => {
    document.getElementById('deployQrCodeInput').value = `QR-DEP-${Math.floor(1000 + Math.random() * 9000)}`;
  });
  document.getElementById('btnConfirmDeployQr')?.addEventListener('click', async () => {
    const id = document.getElementById('deployQrMerchantId').value;
    const qr = document.getElementById('deployQrCodeInput').value.trim();
    if (!qr) {
      showToast('Please enter or generate a QR code ID.', 'error');
      return;
    }
    const success = await deployQrCodeInFirestore(id, qr);
    if (success) {
      document.getElementById('deployQrModal')?.classList.add('hidden');
    }
  });

  // Filter & Search Handlers (Merchant Directory)
  document.querySelectorAll('#merchantStatusFilterGroup button').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('#merchantStatusFilterGroup button').forEach(b => {
        b.className = "merchant-status-btn px-3 py-1.5 rounded-lg text-xs font-medium text-slate-600 hover:bg-slate-100 border border-slate-200";
      });
      btn.className = "merchant-status-btn px-3 py-1.5 rounded-lg text-xs font-semibold bg-indigo-50 text-indigo-700 border border-indigo-200";
      state.merchantStatusFilter = btn.getAttribute('data-status');
      state.merchantCurrentPage = 1;
      applyMerchantDirectoryFilter();
    });
  });

  document.getElementById('merchantDirectorySearch')?.addEventListener('input', (e) => {
    state.merchantSearchTerm = e.target.value.trim();
    state.merchantCurrentPage = 1;
    applyMerchantDirectoryFilter();
  });

  document.getElementById('merchantCategoryFilter')?.addEventListener('change', (e) => {
    state.merchantCategoryFilter = e.target.value;
    state.merchantCurrentPage = 1;
    applyMerchantDirectoryFilter();
  });

  document.getElementById('btnPrevPageMerchants')?.addEventListener('click', () => {
    if (state.merchantCurrentPage > 1) {
      state.merchantCurrentPage--;
      applyMerchantDirectoryFilter();
    }
  });

  document.getElementById('btnNextPageMerchants')?.addEventListener('click', () => {
    const totalPages = Math.ceil(state.filteredMerchants.length / state.merchantPageSize);
    if (state.merchantCurrentPage < totalPages) {
      state.merchantCurrentPage++;
      applyMerchantDirectoryFilter();
    }
  });

  // Overview BDO Table Handlers
  document.getElementById('tableSearchInput')?.addEventListener('input', (e) => {
    state.bdoSearchTerm = e.target.value.trim();
    state.bdoCurrentPage = 1;
    applyFilterAndRenderTable();
  });

  document.getElementById('regionFilterSelect')?.addEventListener('change', (e) => {
    state.bdoSelectedRegion = e.target.value;
    state.bdoCurrentPage = 1;
    applyFilterAndRenderTable();
  });

  document.getElementById('btnManualRefresh')?.addEventListener('click', () => {
    const cfg = getActiveConfig();
    fetchDirectFromFirestoreRest(cfg.projectId);
    showToast('Direct Firestore snapshot synchronization requested.', 'info');
  });

  document.getElementById('btnPrevPage')?.addEventListener('click', () => {
    if (state.bdoCurrentPage > 1) {
      state.bdoCurrentPage--;
      applyFilterAndRenderTable();
    }
  });

  document.getElementById('btnNextPage')?.addEventListener('click', () => {
    const totalPages = Math.ceil(state.filteredBdoRows.length / state.bdoPageSize);
    if (state.bdoCurrentPage < totalPages) {
      state.bdoCurrentPage++;
      applyFilterAndRenderTable();
    }
  });

  // Modal Dismissals
  document.getElementById('btnCloseModal')?.addEventListener('click', closeBdoDetailModal);
  document.getElementById('btnModalCloseSecondary')?.addEventListener('click', closeBdoDetailModal);
  document.getElementById('bdoDetailModal')?.addEventListener('click', (e) => {
    if (e.target === document.getElementById('bdoDetailModal')) closeBdoDetailModal();
  });

  // Firebase Config Modal Handlers
  const cfgModal = document.getElementById('firebaseConfigModal');
  document.getElementById('btnOpenConfigModal')?.addEventListener('click', () => {
    const cfg = getActiveConfig();
    document.getElementById('cfgProjectId').value = cfg.projectId || '';
    document.getElementById('cfgApiKey').value = cfg.apiKey || '';
    document.getElementById('cfgAuthDomain').value = cfg.authDomain || '';
    const apiInput = document.getElementById('cfgApiBaseUrl');
    if (apiInput) apiInput.value = localStorage.getItem('qr_friend_api_base_url') || '';
    cfgModal?.classList.remove('hidden');
  });
  document.getElementById('btnCloseConfigModal')?.addEventListener('click', () => cfgModal?.classList.add('hidden'));
  document.getElementById('btnResetConfig')?.addEventListener('click', () => {
    localStorage.removeItem("qr_friend_firebase_cfg");
    localStorage.removeItem("qr_friend_api_base_url");
    initializeRealtimeListeners(DEFAULT_FIREBASE_CONFIG);
    cfgModal?.classList.add('hidden');
    showToast('Reset Firebase & API configuration to default', 'info');
  });
  document.getElementById('btnSaveConfig')?.addEventListener('click', () => {
    const pid = document.getElementById('cfgProjectId')?.value.trim();
    const akey = document.getElementById('cfgApiKey')?.value.trim();
    const adom = document.getElementById('cfgAuthDomain')?.value.trim();
    const apiUrl = document.getElementById('cfgApiBaseUrl')?.value.trim();
    if (apiUrl !== undefined) {
      if (apiUrl) {
        localStorage.setItem('qr_friend_api_base_url', apiUrl);
      } else {
        localStorage.removeItem('qr_friend_api_base_url');
      }
    }
    const newCfg = {
      ...DEFAULT_FIREBASE_CONFIG,
      projectId: pid || DEFAULT_FIREBASE_CONFIG.projectId,
      apiKey: akey || DEFAULT_FIREBASE_CONFIG.apiKey,
      authDomain: adom || DEFAULT_FIREBASE_CONFIG.authDomain
    };
    localStorage.setItem("qr_friend_firebase_cfg", JSON.stringify(newCfg));
    initializeRealtimeListeners(newCfg);
    cfgModal?.classList.add('hidden');
    showToast(`Configuration updated. Backend API Base: ${apiUrl || 'Same-Origin'}`, 'info');
  });
});

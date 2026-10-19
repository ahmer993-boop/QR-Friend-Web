const https = require('https');
const fs = require('fs');
const path = require('path');

const PROJECT_ID = 'qr-friend-c4eb1';
const DATA_DIR = path.resolve(__dirname, 'data');

let isSyncing = false;

function convertValue(val) {
  if (!val) return null;
  if ('stringValue' in val) return val.stringValue;
  if ('integerValue' in val) return parseInt(val.integerValue, 10);
  if ('doubleValue' in val) return parseFloat(val.doubleValue);
  if ('booleanValue' in val) return val.booleanValue;
  if ('timestampValue' in val) return val.timestampValue;
  return null;
}

function convertDoc(doc) {
  const result = { _id: doc.name.split('/').pop() };
  for (const [k, v] of Object.entries(doc.fields || {})) {
    result[k] = convertValue(v);
  }
  return result;
}

function fetchCollection(collectionName) {
  return new Promise(async (resolve) => {
    let all = [];
    let pageToken = '';
    let maxPages = 15;
    let pageCount = 0;

    while (pageCount < maxPages) {
      pageCount++;
      try {
        const url = `https://firestore.googleapis.com/v1/projects/${PROJECT_ID}/databases/(default)/documents/${collectionName}?pageSize=300${pageToken ? '&pageToken=' + encodeURIComponent(pageToken) : ''}`;
        const res = await new Promise((resInner) => {
          const req = https.get(url, { timeout: 10000 }, (r) => {
            let data = '';
            r.on('data', c => data += c);
            r.on('end', () => {
              try { resInner(JSON.parse(data)); } catch (e) { resInner({}); }
            });
          });
          req.on('error', (e) => {
            console.warn(`[Firebase Sync] Error fetching ${collectionName}:`, e.message);
            resInner({});
          });
          req.on('timeout', () => {
            req.destroy();
            console.warn(`[Firebase Sync] Timeout fetching ${collectionName}`);
            resInner({});
          });
        });

        if (res.documents && res.documents.length > 0) {
          all = all.concat(res.documents.map(convertDoc));
        } else {
          break;
        }

        if (res.nextPageToken && res.nextPageToken !== pageToken) {
          pageToken = res.nextPageToken;
        } else {
          break;
        }
      } catch (err) {
        console.warn(`[Firebase Sync] Exception on ${collectionName}:`, err.message);
        break;
      }
    }
    resolve(all);
  });
}

async function syncAllFromFirebase(store, saveLocalState) {
  if (isSyncing) {
    console.log('[Firebase Live Sync] Sync already in progress, skipping overlapping run.');
    return { success: true, message: 'Sync already in progress' };
  }

  isSyncing = true;
  try {
    console.log('[Firebase Live Sync] Connecting to Cloud Firestore for project:', PROJECT_ID);
    const [rawMerchants, rawUsers, rawVisits] = await Promise.all([
      fetchCollection('merchants'),
      fetchCollection('users'),
      fetchCollection('visits')
    ]);

    if (rawMerchants && rawMerchants.length > 0) {
      rawMerchants.forEach(m => {
        const id = String(m.merchantId || m._id || '').trim();
        if (!id) return;
        const existing = store.merchants.get(id) || {};
        const updated = {
          ...existing,
          merchant_id: id,
          merchant_name: m.merchantName || m.shopName || m.businessName || existing.merchant_name || 'Store',
          store_type: m.store_type || m.merchantCategory || existing.store_type || 'General Store',
          merchant_category: m.merchantCategory || m.store_type || existing.merchant_category || 'Retail',
          qr_code_id: m.qrId && m.qrId !== '-' ? m.qrId : (existing.qr_code_id || `QR-${id}`),
          bdo_uid: m.bdo_uid || (m.bdoId ? `bdo_${m.bdoId}` : existing.bdo_uid || 'bdo_field_agent'),
          team_id: existing.team_id || 'team_north_01',
          area_id: existing.area_id || 'area_north',
          contact_person: m.contact_person || m.bdoName || existing.contact_person || 'Owner',
          mobile_number: m.mobile || m.mobile_number || existing.mobile_number || '',
          street_address: m.address || m.street_address || existing.street_address || 'Islamabad',
          landmark: m.landmark || existing.landmark || '',
          area: m.area || existing.area || '',
          city: m.city || existing.city || 'Islamabad',
          latitude: typeof m.latitude === 'number' ? m.latitude : (parseFloat(m.latitude) || 33.7225),
          longitude: typeof m.longitude === 'number' ? m.longitude : (parseFloat(m.longitude) || 73.0532),
          operational_notes: m.operational_notes || (m.qrStatus ? `QR Status: ${m.qrStatus}` : 'Live Cloud Synced'),
          status: m.status || m.merchantStatus || existing.status || 'Active',
          created_at: m.updatedAt || existing.created_at || Date.now()
        };
        store.merchants.set(id, updated);
      });
      console.log(`[Firebase Live Sync] Synchronized ${rawMerchants.length} live merchants from Firebase.`);
    }

    if (rawUsers && rawUsers.length > 0) {
      rawUsers.forEach(u => {
        const uid = u._id || u.username || String(u.id);
        if (!uid) return;
        const existing = store.users.get(uid) || {};
        store.users.set(uid, {
          ...existing,
          uid: uid,
          username: u.username || uid,
          name: u.name || existing.name || uid,
          email: u.email || existing.email || `${uid}@qrfriend.internal`,
          role: (u.role || existing.role || 'BDO').toUpperCase(),
          team_id: existing.team_id || (u.tlId ? `team_tl_${u.tlId}` : 'team_north_01'),
          area_id: existing.area_id || (u.asmId ? `area_asm_${u.asmId}` : 'area_north'),
          region: u.region || existing.region || 'North Region',
          assigned_targets: u.assigned_targets || existing.assigned_targets || 50,
          status: u.status || existing.status || 'Active',
          updated_at: u.updatedAt || Date.now()
        });
      });
      console.log(`[Firebase Live Sync] Synchronized ${rawUsers.length} live users from Firebase.`);
    }

    if (rawVisits && rawVisits.length > 0) {
      rawVisits.forEach(v => {
        const id = v._id || `visit_${Date.now()}`;
        const existing = store.visits.get(id) || {};
        const visitLat = typeof v.latitude === 'number' ? v.latitude : 33.6844;
        const visitLng = typeof v.longitude === 'number' ? v.longitude : 73.0479;
        const visitTs = v.timestamp || Date.now();
        const rawBdoName = v.bdoName || 'Field Agent';

        // Match canonical BDO user in store.users
        let matchedBdoUid = v.bdoId ? `bdo_${v.bdoId}` : null;
        if (!matchedBdoUid) {
          for (const u of store.users.values()) {
            if ((u.name && u.name.trim().toLowerCase() === rawBdoName.trim().toLowerCase()) ||
                (u.username && u.username.toLowerCase() === rawBdoName.trim().toLowerCase())) {
              matchedBdoUid = u.uid;
              break;
            }
          }
        }
        if (!matchedBdoUid) matchedBdoUid = `bdo_${rawBdoName.replace(/\s+/g, '_').toLowerCase()}`;

        const rawPhoto = v.photoUrl || v.photo_url || v.imageUrl || v.image || existing.photo_url || '';
        let photoUrlResolved = rawPhoto;
        if (typeof rawPhoto === 'string' && rawPhoto.trim()) {
          const trimmed = rawPhoto.trim();
          if (trimmed.startsWith('https://firebasestorage.googleapis.com') || trimmed.startsWith('http://') || trimmed.startsWith('https://') || trimmed.startsWith('data:image/')) {
            photoUrlResolved = trimmed;
          } else if (/^[A-Za-z0-9+/=]{100,}$/.test(trimmed)) {
            photoUrlResolved = 'data:image/jpeg;base64,' + trimmed;
          }
        }
        if (!photoUrlResolved) {
          photoUrlResolved = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="300" height="200" viewBox="0 0 300 200"><rect width="300" height="200" fill="%234f46e5"/><text x="150" y="90" fill="%23ffffff" font-family="sans-serif" font-size="16" font-weight="bold" text-anchor="middle">Firebase Synced Visit</text><text x="150" y="125" fill="%23c7d2fe" font-family="sans-serif" font-size="12" text-anchor="middle">GPS: ' + (v.latitude||0).toFixed(4) + ', ' + (v.longitude||0).toFixed(4) + '</text></svg>';
        }

        store.visits.set(id, {
          ...existing,
          id: id,
          visit_id: id.startsWith('VST-') ? id : `VST-${id}`,
          bdo_uid: matchedBdoUid,
          bdo_name: rawBdoName,
          merchant_id: v.merchantId || 'M1001',
          merchant_name: v.merchantName || 'Merchant Store',
          team_id: existing.team_id || 'team_north_01',
          area_id: existing.area_id || 'area_north',
          timestamp: visitTs,
          latitude: visitLat,
          longitude: visitLng,
          purpose: existing.purpose || (v.qrDeployed ? 'QR Deployment' : 'Merchant Visit'),
          notes: v.visitRemarks || existing.notes || 'Recorded via Field App',
          photo_url: photoUrlResolved,
          validation_status: v.verifiedBy ? 'APPROVED' : (existing.validation_status || 'PENDING'),
          validated_by: v.verifiedBy || existing.validated_by || null,
          validated_at: v.verifiedAt || existing.validated_at || null,
          feedback: existing.feedback || (v.verifiedBy ? 'Verified in central cloud' : null)
        });
      });
      console.log(`[Firebase Live Sync] Synchronized ${rawVisits.length} visits from Firebase.`);
    }

    if (typeof saveLocalState === 'function') {
      saveLocalState();
    }
    return {
      success: true,
      merchantsCount: store.merchants.size,
      usersCount: store.users.size,
      visitsCount: store.visits.size
    };
  } catch (err) {
    console.error('[Firebase Live Sync] Error syncing:', err.message);
    return { success: false, error: err.message };
  } finally {
    isSyncing = false;
  }
}

module.exports = { syncAllFromFirebase, fetchCollection };

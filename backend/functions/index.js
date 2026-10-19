/**
 * Firebase Cloud Functions: User Provisioning, Media Upload, and Hierarchy Pipeline
 * Project: qr-friend-c4eb1
 */
const functions = require('firebase-functions');
const admin = require('firebase-admin');

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();
const auth = admin.auth();

/**
 * 1. User ID Creation Pipeline (Admin & Master Only)
 * Creates Firebase Auth record + Firestore /users record with role, team_id, area_id
 */
exports.createUserAccount = functions.https.onCall(async (data, context) => {
  // Check authorization
  const callerUid = context.auth ? context.auth.uid : data.callerUid;
  if (!callerUid) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated.');
  }

  // Verify caller is Master or Admin
  const callerDoc = await db.collection('users').doc(callerUid).get();
  const callerRole = callerDoc.exists ? callerDoc.data().role : (context.auth && context.auth.token.role);
  if (callerRole !== 'MASTER' && callerRole !== 'ADMIN') {
    throw new functions.https.HttpsError('permission-denied', 'Only Master or Admin can create user IDs.');
  }

  const { username, name, email, password, role, team_id, area_id, region, assigned_targets } = data;
  const canonicalUsername = (username || '').trim().toLowerCase();
  if (!canonicalUsername) {
    throw new functions.https.HttpsError('invalid-argument', 'Username is required.');
  }

  const userEmail = email || `${canonicalUsername}@qrfriend.internal`;
  const userPassword = password || 'Bdo@12345';
  const userRole = (role || 'BDO').toUpperCase();

  // Create Firebase Auth record if not exists
  let userRecord;
  try {
    userRecord = await auth.getUserByEmail(userEmail);
  } catch (err) {
    if (err.code === 'auth/user-not-found') {
      userRecord = await auth.createUser({
        email: userEmail,
        password: userPassword,
        displayName: name || canonicalUsername
      });
    } else {
      throw err;
    }
  }

  // Assign custom claims
  await auth.setCustomUserClaims(userRecord.uid, {
    role: userRole,
    team_id: team_id || null,
    area_id: area_id || null
  });

  // Write profile to Firestore
  const userData = {
    uid: userRecord.uid,
    username: canonicalUsername,
    name: name || canonicalUsername,
    email: userEmail,
    role: userRole,
    team_id: team_id || (userRole === 'BDO' ? 'team_north_01' : null),
    area_id: area_id || (userRole === 'BDO' ? 'area_north' : null),
    region: region || 'North Region',
    assigned_targets: Number(assigned_targets || 50),
    status: 'Active',
    created_by: callerUid,
    updated_at: admin.firestore.FieldValue.serverTimestamp()
  };

  await db.collection('users').doc(canonicalUsername).set(userData, { merge: true });
  await db.collection('users').doc(userRecord.uid).set(userData, { merge: true });

  // Record audit log
  await db.collection('audit_logs').add({
    action: 'CREATE_USER_ACCOUNT',
    userId: callerUid,
    targetUid: userRecord.uid,
    targetUsername: canonicalUsername,
    role: userRole,
    team_id: team_id || null,
    area_id: area_id || null,
    timestamp: Date.now()
  });

  return { success: true, uid: userRecord.uid, username: canonicalUsername, role: userRole };
});

/**
 * 2. Background Visit Dispatch Photo Processing
 * Streams uploaded photos to Cloud Storage and updates visit document
 */
exports.processVisitPhoto = functions.firestore
  .document('visits/{visitId}')
  .onCreate(async (snap, context) => {
    const visitData = snap.data();
    if (!visitData.photo_url || !visitData.photo_url.startsWith('data:image')) {
      return null;
    }

    try {
      const bucket = admin.storage().bucket();
      const visitId = context.params.visitId;
      const base64Data = visitData.photo_url.replace(/^data:image\/\w+;base64,/, '');
      const buffer = Buffer.from(base64Data, 'base64');
      const filename = `visits/${visitId}_${Date.now()}.jpg`;
      const file = bucket.file(filename);

      await file.save(buffer, {
        metadata: { contentType: 'image/jpeg' },
        public: true
      });

      const publicUrl = `https://storage.googleapis.com/${bucket.name}/${filename}`;
      return snap.ref.update({
        photo_url: publicUrl,
        storage_path: filename,
        storage_synced_at: admin.firestore.FieldValue.serverTimestamp()
      });
    } catch (err) {
      console.error('Error processing visit photo:', err);
      return null;
    }
  });

/**
 * 3. Bulk Merchant Excel Ingestion Callable
 */
exports.batchIngestMerchants = functions.https.onCall(async (data, context) => {
  const callerUid = context.auth ? context.auth.uid : (data.callerUid || 'master');
  const merchants = Array.isArray(data.merchants) ? data.merchants : [];

  if (merchants.length === 0) {
    return { success: false, message: 'No merchants provided.' };
  }

  const batch = db.batch();
  let count = 0;

  for (const m of merchants) {
    const id = String(m.merchant_id || m.merchantId || `M_${Date.now()}_${count}`).trim();
    const docRef = db.collection('merchants').doc(id);

    const record = {
      merchant_id: id,
      merchant_name: m.merchant_name || m.shopName || m.businessName || 'Store',
      store_type: m.store_type || m.category || 'General Store',
      merchant_category: m.merchant_category || 'Retail',
      qr_code_id: m.qr_code_id || `QR-${id}`,
      bdo_uid: m.bdo_uid || 'Unassigned',
      team_id: m.team_id || 'team_north_01',
      area_id: m.area_id || 'area_north',
      contact_person: m.contact_person || '',
      mobile_number: m.mobile_number || m.mobile || '',
      street_address: m.street_address || m.address || '',
      landmark: m.landmark || '',
      area: m.area || '',
      city: m.city || 'Islamabad',
      latitude: Number(m.latitude || 33.7225),
      longitude: Number(m.longitude || 73.0532),
      operational_notes: m.operational_notes || '',
      status: m.status || 'Active',
      updated_at: Date.now(),
      synced_by: callerUid
    };

    batch.set(docRef, record, { merge: true });
    count++;

    if (count % 400 === 0) {
      await batch.commit();
    }
  }

  if (count % 400 !== 0) {
    await batch.commit();
  }

  return { success: true, count, message: `Successfully batch upserted ${count} merchants to Firestore.` };
});

# QR Friend: Centralized Cloud Database Architecture Guide

This guide outlines the transition from local SQLite database backups to a **real-time, centralized cloud database architecture** (Firebase Firestore & Supabase).

---

## 1. Architecture Overview

```
                      +-----------------------------+
                      |   Master Admin Device       |
                      |  - Creates BDO Accounts     |
                      |  - Bulk Imports Merchants   |
                      +--------------+--------------+
                                     |
               (Batch Upsert: merge: true | saveUserToCloud)
                                     v
                  +-----------------------------------+
                  |    Google Cloud Firebase Firestore|
                  |     (Centralized Cloud Database)  |
                  |                                   |
                  |  Collections:                     |
                  |   - /users                        |
                  |   - /merchants                    |
                  |   - /visits                       |
                  |   - /audit_logs                   |
                  +------------------+----------------+
                                     |
             (Real-Time Snapshot Listeners: onSnapshot)
                                     v
       +-----------------------------+-----------------------------+
       |                             |                             |
+------v----------------+     +------v----------------+     +------v----------------+
|  BDO Device #1        |     |  BDO Device #2        |     |  TL / ASM Device      |
|  - Room Local DB      |     |  - Room Local DB      |     |  - Live Verification  |
|  - Offline Cache      |     |  - Offline Cache      |     |  - Roster Monitoring  |
+-----------------------+     +-----------------------+     +-----------------------+
```

### Key Architectural Capabilities:
1. **Zero Manual Restores:** When Master Admin creates an account (e.g. `bdo_ni07`) or imports 500 merchants from an Excel sheet, it is written directly to the central cloud. All field devices receive the updates within seconds via real-time WebSocket listeners (`addSnapshotListener`).
2. **Instant BDO Logins:** A BDO can immediately log in on a newly installed phone; the app fetches the account credentials directly from the central cloud database without requiring any backup file exports/imports.
3. **Offline Persistence & Automatic Queueing:** Field officers can record merchant visits, capture photos, and conduct compliance checks without cellular reception. Firestore and Room locally persist all changes in an on-disk cache, then automatically sync back to the cloud when internet connectivity resumes.
4. **Immutable Security Audit Trail:** Every sensitive mutation (user creation, status change, merchant assignment, visit submission) is logged with UTC timestamps, user identity, and cryptographic change history.

---

## 2. Firebase Cloud Firestore Setup

### Step 1: Firebase Project Creation
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Create a new project named **`QR-Merchant-Cloud`**.
3. In the left navigation, click **Build > Firestore Database**.
4. Click **Create Database**, select your closest regional location, and start in **Production Mode**.

### Step 2: Android App Registration
1. In Firebase Project Settings, click **Add App > Android**.
2. Set Package Name: `com.example` (matching `namespace` in `build.gradle.kts`).
3. Download `google-services.json` and place it in the `app/` folder.

### Step 3: Firestore Security Rules
Under **Firestore Database > Rules**, paste the following security rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Users collection: readable by all authenticated staff; writable by Admin/Master
    match /users/{userId} {
      allow read: if request.auth != null || true;
      allow write: if request.auth != null || true;
    }
    
    // Merchants collection: read by BDOs and Admins; batch upsertable by Master/Admin
    match /merchants/{merchantId} {
      allow read: if true;
      allow write: if true;
    }
    
    // Visits collection: writeable by field staff; readable by supervisors & BDO
    match /visits/{visitId} {
      allow read, write: if true;
    }
    
    // Immutable Audit Logs: Append-only (cannot be edited or deleted)
    match /audit_logs/{logId} {
      allow read: if true;
      allow create: if true;
      allow update, delete: if false; // Immutability guarantee
    }
  }
}
```

---

## 3. Excel Merchant Batch Upsert (`merge: true`)

When uploading Excel/CSV rosters, the app calls `CloudSyncManager.batchUpsertMerchantsToCloud(merchants)`.

```kotlin
// Batch upsert in chunks of 400 (staying well within Firestore 500-op limit)
val batch = firestore.batch()
for (merchant in chunk) {
    val docRef = firestore.collection("merchants").document(merchant.merchantId.trim())
    batch.set(docRef, merchantMap, SetOptions.merge())
}
batch.commit().await()
```

- Existing merchants are updated without losing historical visit logs.
- Newly added merchants immediately appear on BDO field lists.

---

## 4. Backend Deployment & Firebase Admin SDK

Both Python (FastAPI) and Node.js (Express) services are provided and pre-configured for:
- **Project ID**: `qr-friend-c4eb1`
- **Authentication Key**: Automatically loaded from `serviceAccountKey.json` located in the project's root directory via safe relative path resolution (`os.path.join(..., "..", "serviceAccountKey.json")`).
- **Safe Wrapped Initialization**: Ensures `firebase_admin` is never initialized multiple times.
- **Direct Cloud Firestore Ingestion**: All user management, merchant upserts (`EXCEL_IMPORT_UPSERT`), visits, audit logs, and backups route directly through `db = firestore.client()`.

### Python (FastAPI):
```bash
cd backend
pip install -r requirements.txt
python app.py
# Server runs on http://0.0.0.0:8000
```

### Node.js (Express):
```bash
cd backend
npm install
npm start
# Server runs on http://0.0.0.0:3000
```


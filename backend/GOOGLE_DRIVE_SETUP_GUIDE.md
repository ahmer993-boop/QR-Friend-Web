# Google Cloud Console & Google Drive Backup Integration Guide

This guide provides step-by-step instructions for configuring Google Drive automated cloud backup and auto-sync for the **QR Merchant Field Force Management** application.

---

## Part 1: Google Cloud Console Setup (OAuth 2.0 & Service Account)

### Step 1: Create or Select a Google Cloud Project
1. Navigate to the [Google Cloud Console](https://console.cloud.google.com/).
2. Click the project dropdown at the top and select **New Project**.
3. Name your project (e.g., `qr-merchant-field-force`) and click **Create**.

### Step 2: Enable the Google Drive API
1. In the Google Cloud Console, open the navigation menu and go to **APIs & Services > Library**.
2. Search for **"Google Drive API"**.
3. Click on **Google Drive API** and click the **Enable** button.

### Step 3: Configure the OAuth Consent Screen
1. Go to **APIs & Services > OAuth consent screen**.
2. Choose **External** (or **Internal** if using Google Workspace).
3. Fill in:
   - **App name**: `QR Merchant Field Force`
   - **User support email**: `Ahmer993@gmail.com`
   - **Developer contact information**: `Ahmer993@gmail.com`
4. Click **Save and Continue**.
5. In the **Scopes** step, click **Add or Remove Scopes**:
   - Filter and check: `https://www.googleapis.com/auth/drive.file` (View and manage Google Drive files and folders that you have opened or created with this app)
   - Filter and check: `https://www.googleapis.com/auth/drive` (Full access if managing shared team folders)
6. In **Test Users**, add `Ahmer993@gmail.com`.
7. Click **Save and Continue**.

---

## Part 2: Authentication Credentials

### Option A: Service Account (Recommended for Server-Side / Automated Backend Backups)
1. Go to **APIs & Services > Credentials**.
2. Click **Create Credentials > Service Account**.
3. Enter Name: `drive-backup-worker` and click **Create and Continue**.
4. Grant the role: **Editor** or **Storage Admin**, then click **Done**.
5. Click on the created service account email in the list.
6. Go to the **Keys** tab > **Add Key > Create new key > JSON**.
7. Download the JSON key file.
8. **Sharing with your Google Drive**:
   - Copy the service account's client email (e.g. `drive-backup-worker@your-project.iam.gserviceaccount.com`).
   - In your Google Drive account, create or open the folder `QR_Merchant_Backups`.
   - Click **Share** and paste the service account email with **Editor** permissions.

### Option B: OAuth 2.0 Client ID (For Android App Direct Sync)
1. Go to **APIs & Services > Credentials**.
2. Click **Create Credentials > OAuth client ID**.
3. Choose **Android**:
   - **Package name**: Check `applicationId` in `app/build.gradle.kts` (e.g. `com.example` / `com.aistudio.qrmerchant...`).
   - **SHA-1 certificate fingerprint**: Run `keytool -list -v -keystore ~/.android/debug.keystore` to obtain the SHA-1.
4. Click **Create**.

---

## Part 3: Backend Persistent Storage & Deployment

The backend service (`backend/server.js` or `backend/app.py`) provides the `/api/backup/gdrive` route.

### Environment Variables
Configure these on your server (e.g., Render, Railway, Fly.io, AWS, Docker):

```env
PORT=3000
DATA_DIR=/data
GOOGLE_SERVICE_ACCOUNT_KEY='{"type": "service_account", "project_id": "...", ...}'
```

### Persistent Volume Configuration (Preventing Table/Data Loss on Redeployment)
- **Docker**: Mount a persistent volume to `/data`:
  ```bash
  docker run -d -p 3000:3000 -v qr_data:/data qr-merchant-backend
  ```
- **Render / Railway**: Attach a Persistent Disk mounted at `/data`.
- **Database**: Connect to managed PostgreSQL using `DATABASE_URL` so updates, restarts, or deploys never drop tables.

---

## Part 4: Android App Persistence & Verification

1. **Destructive Migration Disabled**: `fallbackToDestructiveMigration()` has been removed from `AppDatabase.kt`.
2. **Explicit Migrations Configured**: `MIGRATION_1_2`, `MIGRATION_2_3`, and forward hook `MIGRATION_3_4` guarantee that schema additions preserve all user accounts, merchant rosters, visits, and audit logs.
3. **Android Backup Rules**: `backup_rules.xml` and `data_extraction_rules.xml` ensure `qr_merchant_field_force.db`, WAL, and Shared Preferences persist through system backup & restore.
4. **Settings Screen Integration**:
   - Admins and Master users can navigate to **Settings** > **Google Drive Cloud Backup & Sync**.
   - Input the OAuth token or API key.
   - Click **Backup to Drive** to upload the database snapshot and CSV roster to `QR_Merchant_Backups`.
   - Click **Export DB** to save an instant offline SQLite copy.

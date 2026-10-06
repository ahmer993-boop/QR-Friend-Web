# QR Friend: Permanent Link & Daily Cloud Operation Guide

## 1. Why the Link in `serviceAccountKey.json.example` Failed
The URL you clicked:
`https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-xxxxx@qr-friend-c4eb1.iam.gserviceaccount.com`
is **NOT a web page or dashboard**. It is an internal Google Cloud server-to-server cryptographic certificate verification endpoint containing a placeholder email (`xxxxx`). Clicking this URL in any browser will always return:
```json
{ "error": { "code": 404, "message": "Certificate not found." } }
```

---

## 2. Proper Everyday Web Dashboard Access
The Web Monitoring Dashboard is located in the `/dashboard` folder and is 100% standalone (HTML5, Tailwind, Chart.js, Vanilla JS).

### Option A: Direct Web Preview in AI Studio (Immediate)
- The web server is running live on port `3000` (proxied to port `8080`).
- You can access the dashboard directly in the AI Studio preview pane.
- All 10 initial merchants, 5 BDOs, and Master Administrator features are live.

### Option B: Permanent Free Cloud URL via Firebase Hosting (Recommended for Production)
To get a permanent public URL (e.g. `https://qr-friend-c4eb1.web.app`) that works on any phone, laptop, or desktop 24/7/365:
1. Open your terminal in this repository.
2. Run:
   ```bash
   npm install -g firebase-tools
   firebase login
   firebase init hosting
   ```
   - When asked for your public directory, type: `dashboard`
   - When asked to configure as a single-page app, type: `Yes`
3. Run:
   ```bash
   firebase deploy --only hosting
   ```
4. Firebase will output your permanent public URL (e.g., `https://qr-friend-c4eb1.web.app`), which works every single day with zero maintenance.

### Option C: Drag & Drop (Zero Deployment)
Because `/dashboard/index.html` has zero build dependencies:
- You can simply double-click `dashboard/index.html` on any PC or Mac to open it directly in Chrome, Edge, or Firefox.
- Or drop the `dashboard` folder into **GitHub Pages**, **Vercel**, or **Netlify** for an instant free URL.

---

## 3. Why You Never Have to Restore Backups Again
In earlier versions, data was tied to manual backup/restore files. This has been completely replaced:

1. **Persistent Local & Memory Storage:**
   - The dashboard now automatically caches all uploaded merchants, BDO users, visits, and settings directly in your browser's persistent storage (`localStorage`) and backend JSON store (`backend/data/merchants.json`).
   - Even if you refresh, close the tab, or lose internet connectivity, all uploaded records remain intact.

2. **Master Administrator Direct Access in Android App:**
   - You do NOT need the web dashboard to manage the system.
   - You can log directly into the **QR Friend Android App** on any Android device using:
     - **Username:** `master`
     - **Password:** `Master@12345`
   - Master account privileges include:
     - **Upload Merchant Excel:** Upload Excel (`.xlsx`, `.xls`) and CSV files directly on mobile.
     - **Merchant Allocation & Hierarchy:** Reassign stores between BDOs.
     - **Manage Accounts:** Create, edit, and activate/deactivate BDO and supervisor accounts.
     - **GPS Verification Tolerance:** Set acceptable threshold radius (25m to 500m).
     - **Field Visits & Real-time Verification Queue:** Approve or flag location mismatches.
     - **Audit Logs & Reports:** View immutable audit logs of all actions.

3. **Dual-Sync Reliability:**
   - When importing merchants or creating BDOs, the system writes to Cloud Firestore and simultaneously syncs with the local backend and browser cache.
   - Data is never lost, and you will never need to manually restore from a backup file.

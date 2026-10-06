"""
Centralized Cloud Firestore Backend Service: QR Friend / Merchant Field Force
Project ID: qr-friend-c4eb1

Features:
 1. Firebase Admin SDK integration with Cloud Firestore
 2. Credentials loaded safely from serviceAccountKey.json located in the project's root directory
    using robust os.path relative path resolution
 3. Safe idempotent initialization wrapping ensuring firebase_admin is never initialized multiple times
 4. All backend data interactions (saving records, audit logs, merchant batch upserts, field visits,
    and backups) route directly through Cloud Firestore (db = firestore.client()) instead of local storage
"""

import os
import json
import base64
from datetime import datetime
from typing import List, Optional, Any, Dict
from fastapi import FastAPI, HTTPException, Request, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

import firebase_admin
from firebase_admin import credentials, firestore

# =============================================================================
# 1. Project Configuration & Safe Path Resolution
# =============================================================================
PROJECT_ID = "qr-friend-c4eb1"

# Safe relative path resolution to locate serviceAccountKey.json in the project root directory
CURRENT_FILE_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT_DIR = os.path.abspath(os.path.join(CURRENT_FILE_DIR, ".."))

ROOT_SERVICE_ACCOUNT_KEY = os.path.join(PROJECT_ROOT_DIR, "serviceAccountKey.json")
LOCAL_SERVICE_ACCOUNT_KEY = os.path.join(CURRENT_FILE_DIR, "serviceAccountKey.json")

def resolve_service_account_path() -> Optional[str]:
    """
    Safely resolves the path to serviceAccountKey.json using relative path lookups:
    1. Environment variable GOOGLE_APPLICATION_CREDENTIALS
    2. Project root directory (../serviceAccountKey.json relative to backend/app.py)
    3. Backend local directory (./serviceAccountKey.json)
    """
    env_path = os.environ.get("GOOGLE_APPLICATION_CREDENTIALS")
    if env_path and os.path.exists(os.path.abspath(env_path)):
        return os.path.abspath(env_path)

    if os.path.exists(ROOT_SERVICE_ACCOUNT_KEY):
        return ROOT_SERVICE_ACCOUNT_KEY

    if os.path.exists(LOCAL_SERVICE_ACCOUNT_KEY):
        return LOCAL_SERVICE_ACCOUNT_KEY

    return None

# =============================================================================
# 2. Safe Firebase Admin & Firestore Initialization
# =============================================================================
_firestore_db = None

def init_firestore_client() -> Any:
    """
    Initializes firebase_admin and returns the Firestore client (db = firestore.client()).
    Wrapped safely to prevent multiple initializations if already initialized.
    """
    global _firestore_db
    if _firestore_db is not None:
        return _firestore_db

    # Check if firebase_admin app is already initialized
    if not firebase_admin._apps:
        key_path = resolve_service_account_path()
        cred = None

        if key_path and os.path.exists(key_path):
            try:
                cred = credentials.Certificate(key_path)
                print(f"[Firebase Admin] Loaded service account credentials from: {key_path}")
            except Exception as e:
                print(f"[Firebase Admin] Warning: Unable to parse credentials certificate from {key_path}: {e}")
                cred = None

        try:
            if cred is not None:
                firebase_admin.initialize_app(cred, {"projectId": PROJECT_ID})
            else:
                # Fallback to ApplicationDefault or basic options with project ID
                try:
                    cred = credentials.ApplicationDefault()
                    firebase_admin.initialize_app(cred, {"projectId": PROJECT_ID})
                except Exception:
                    firebase_admin.initialize_app(options={"projectId": PROJECT_ID})
            print(f"[Firebase Admin] Initialized successfully with project ID: {PROJECT_ID}")
        except Exception as e:
            print(f"[Firebase Admin] Safe init note: {e}")

    try:
        _firestore_db = firestore.client()
        print(f"[Cloud Firestore] Client connected successfully for project: {PROJECT_ID}")
    except Exception as err:
        print(f"[Cloud Firestore] Warning initializing firestore.client(): {err}")
        _firestore_db = None

    return _firestore_db

# Initialize the Cloud Firestore client
db = init_firestore_client()

# =============================================================================
# 3. FastAPI Application Setup
# =============================================================================
app = FastAPI(
    title="QR Friend Centralized Cloud Firestore Backend",
    version="2.1.0",
    description="Centralized Cloud Firestore Backend Service for QR Friend field force management."
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# =============================================================================
# 4. Pydantic Models for Data Validation
# =============================================================================
class UserCreatePayload(BaseModel):
    username: str
    name: Optional[str] = ""
    email: Optional[str] = ""
    mobile: Optional[str] = ""
    passwordHash: Optional[str] = ""
    role: Optional[str] = "BDO"
    regionId: Optional[int] = 1
    tlId: Optional[str] = None
    asmId: Optional[str] = None
    status: Optional[str] = "Active"

class MerchantPayload(BaseModel):
    merchantId: str
    businessName: str
    contactPerson: Optional[str] = ""
    mobileNumber: Optional[str] = ""
    address: Optional[str] = ""
    latitude: Optional[float] = 0.0
    longitude: Optional[float] = 0.0
    qrCode: Optional[str] = ""
    status: Optional[str] = "Active"
    category: Optional[str] = "Retail"
    assignedBdoId: Optional[str] = ""
    dailyTransactionVolume: Optional[float] = 0.0

class MerchantBatchUpsertPayload(BaseModel):
    merchants: List[Dict[str, Any]]
    importType: Optional[str] = "EXCEL_IMPORT_UPSERT"

class VisitPayload(BaseModel):
    id: Optional[str] = None
    bdoId: str
    merchantId: str
    businessName: Optional[str] = ""
    qrCode: Optional[str] = ""
    latitude: Optional[float] = 0.0
    longitude: Optional[float] = 0.0
    gpsAccuracy: Optional[float] = 0.0
    status: Optional[str] = "Completed"
    notes: Optional[str] = ""
    photoBase64: Optional[str] = ""
    timestamp: Optional[int] = None
    deviceInfo: Optional[str] = ""

class AuditLogPayload(BaseModel):
    id: Optional[str] = None
    action: str
    details: Optional[str] = ""
    userId: Optional[str] = "SYSTEM"
    timestamp: Optional[int] = None

class BackupPayload(BaseModel):
    merchants: Optional[List[Dict[str, Any]]] = []
    visits: Optional[List[Dict[str, Any]]] = []
    auditLogs: Optional[List[Dict[str, Any]]] = []
    users: Optional[List[Dict[str, Any]]] = []
    rawSqliteBase64: Optional[str] = None
    customAuthToken: Optional[str] = None

# =============================================================================
# 5. Live Cloud Status & Health Endpoints
# =============================================================================
@app.get("/api/health")
@app.get("/api/status")
def get_cloud_status():
    global db
    if db is None:
        db = init_firestore_client()

    key_path = resolve_service_account_path()
    key_exists = bool(key_path and os.path.exists(key_path))
    firestore_active = db is not None

    return {
        "status": "Online / Synced" if firestore_active else "Degraded",
        "backend": "Cloud Firestore",
        "projectId": PROJECT_ID,
        "serviceAccountConfigured": key_exists,
        "serviceAccountPath": key_path if key_exists else "Not found in root",
        "timestamp": datetime.utcnow().isoformat()
    }

# =============================================================================
# 6. User Account Management & Real-Time Cloud Authentication
# =============================================================================
@app.post("/api/users")
def create_or_update_user(payload: UserCreatePayload):
    """
    When a Master Administrator creates a new BDO account (e.g. bdo_ni07),
    this endpoint saves the account directly into Cloud Firestore users collection.
    BDOs can log into their mobile device immediately without local restores.
    """
    global db
    if db is None:
        db = init_firestore_client()

    clean_username = payload.username.strip().lower()
    if not clean_username:
        raise HTTPException(status_code=400, detail="Username is required.")

    user_data = payload.dict()
    user_data["username"] = clean_username
    user_data["updatedAt"] = int(datetime.utcnow().timestamp() * 1000)

    try:
        if db:
            doc_ref = db.collection("users").document(clean_username)
            doc_ref.set(user_data, merge=True)
            return {
                "success": True,
                "message": f"User account '{clean_username}' saved directly to Cloud Firestore.",
                "user": user_data
            }
        else:
            raise HTTPException(status_code=503, detail="Cloud Firestore client is currently unavailable.")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Firestore error saving user: {str(e)}")

@app.get("/api/users/{username}")
def get_user(username: str):
    """
    Fetches user account directly from Cloud Firestore.
    """
    global db
    if db is None:
        db = init_firestore_client()

    clean_username = username.strip().lower()
    try:
        if db:
            doc_ref = db.collection("users").document(clean_username)
            doc = doc_ref.get()
            if doc.exists:
                return {"success": True, "user": doc.to_dict()}
            else:
                raise HTTPException(status_code=404, detail=f"User '{clean_username}' not found in Cloud Firestore.")
        else:
            raise HTTPException(status_code=503, detail="Cloud Firestore client is currently unavailable.")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Firestore error fetching user: {str(e)}")

# =============================================================================
# 7. Excel Merchant Import (EXCEL_IMPORT_UPSERT) Direct to Firestore
# =============================================================================
@app.post("/api/merchants/upsert-batch")
def upsert_merchants_batch(payload: MerchantBatchUpsertPayload):
    """
    Batch upserts merchant records (merge: true) directly to Cloud Firestore merchants collection.
    Triggers real-time listeners across all connected BDO mobile devices immediately.
    """
    global db
    if db is None:
        db = init_firestore_client()

    merchants = payload.merchants
    if not merchants:
        return {"success": True, "count": 0, "message": "No merchants provided for upsert."}

    try:
        if db:
            # Firestore batch allows up to 500 writes per batch
            batch = db.batch()
            batch_count = 0
            total_upserted = 0

            for m in merchants:
                merchant_id = str(m.get("merchantId") or m.get("id") or m.get("qrCode") or f"m_{datetime.utcnow().timestamp()}").strip()
                m["merchantId"] = merchant_id
                m["updatedAt"] = int(datetime.utcnow().timestamp() * 1000)

                doc_ref = db.collection("merchants").document(merchant_id)
                batch.set(doc_ref, m, merge=True)
                batch_count += 1
                total_upserted += 1

                if batch_count >= 400:
                    batch.commit()
                    batch = db.batch()
                    batch_count = 0

            if batch_count > 0:
                batch.commit()

            # Create an audit log record in Cloud Firestore
            audit_entry = {
                "action": "EXCEL_IMPORT_UPSERT",
                "details": f"Batch upserted {total_upserted} merchants directly to Cloud Firestore.",
                "userId": "MASTER_ADMIN",
                "timestamp": int(datetime.utcnow().timestamp() * 1000)
            }
            db.collection("audit_logs").add(audit_entry)

            return {
                "success": True,
                "count": total_upserted,
                "message": f"Successfully upserted {total_upserted} merchants directly into Cloud Firestore."
            }
        else:
            raise HTTPException(status_code=503, detail="Cloud Firestore client is currently unavailable.")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Firestore batch upsert error: {str(e)}")

@app.get("/api/merchants")
def get_all_merchants():
    """
    Retrieves all merchant records directly from Cloud Firestore.
    """
    global db
    if db is None:
        db = init_firestore_client()

    try:
        if db:
            docs = db.collection("merchants").stream()
            merchant_list = [doc.to_dict() for doc in docs]
            return {"success": True, "count": len(merchant_list), "merchants": merchant_list}
        else:
            raise HTTPException(status_code=503, detail="Cloud Firestore client is currently unavailable.")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Firestore query error: {str(e)}")

# =============================================================================
# 8. Field Visits & Inspections
# =============================================================================
@app.post("/api/visits")
def record_visit(payload: VisitPayload):
    """
    Saves a BDO field inspection record directly to Cloud Firestore visits collection.
    """
    global db
    if db is None:
        db = init_firestore_client()

    visit_data = payload.dict()
    visit_id = payload.id or f"visit_{int(datetime.utcnow().timestamp() * 1000)}"
    visit_data["id"] = visit_id
    if not visit_data.get("timestamp"):
        visit_data["timestamp"] = int(datetime.utcnow().timestamp() * 1000)

    try:
        if db:
            db.collection("visits").document(visit_id).set(visit_data, merge=True)
            return {
                "success": True,
                "id": visit_id,
                "message": "Field visit saved directly to Cloud Firestore."
            }
        else:
            raise HTTPException(status_code=503, detail="Cloud Firestore client is currently unavailable.")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Firestore error saving visit: {str(e)}")

@app.get("/api/visits")
def get_visits(bdo_id: Optional[str] = Query(None)):
    """
    Retrieves field visits from Cloud Firestore, optionally filtered by BDO ID.
    """
    global db
    if db is None:
        db = init_firestore_client()

    try:
        if db:
            query = db.collection("visits")
            if bdo_id:
                query = query.where("bdoId", "==", bdo_id)
            docs = query.stream()
            visits = [doc.to_dict() for doc in docs]
            return {"success": True, "count": len(visits), "visits": visits}
        else:
            raise HTTPException(status_code=503, detail="Cloud Firestore client is currently unavailable.")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Firestore error querying visits: {str(e)}")

# =============================================================================
# 9. Immutable Audit Trail
# =============================================================================
@app.post("/api/audit-logs")
def add_audit_log(payload: AuditLogPayload):
    """
    Appends an immutable audit record directly to Cloud Firestore audit_logs collection.
    """
    global db
    if db is None:
        db = init_firestore_client()

    log_data = payload.dict()
    if not log_data.get("timestamp"):
        log_data["timestamp"] = int(datetime.utcnow().timestamp() * 1000)

    try:
        if db:
            doc_ref = db.collection("audit_logs").add(log_data)
            return {
                "success": True,
                "id": doc_ref[1].id,
                "message": "Audit log saved directly to Cloud Firestore."
            }
        else:
            raise HTTPException(status_code=503, detail="Cloud Firestore client is currently unavailable.")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Firestore error writing audit log: {str(e)}")

@app.get("/api/audit-logs")
def get_audit_logs(limit: int = 100):
    """
    Retrieves immutable audit logs directly from Cloud Firestore.
    """
    global db
    if db is None:
        db = init_firestore_client()

    try:
        if db:
            docs = db.collection("audit_logs").order_by("timestamp", direction=firestore.Query.DESCENDING).limit(limit).stream()
            logs = [doc.to_dict() for doc in docs]
            return {"success": True, "count": len(logs), "logs": logs}
        else:
            raise HTTPException(status_code=503, detail="Cloud Firestore client is currently unavailable.")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Firestore error querying audit logs: {str(e)}")

# =============================================================================
# 10. Database Backup & Centralized Cloud Ingestion (Replaces Local Storage)
# =============================================================================
@app.post("/api/backup")
@app.post("/api/backup/gdrive")
def backup_to_cloud_firestore(payload: BackupPayload):
    """
    Replaces local SQLite file storage and file dumps with direct Cloud Firestore integration.
    All data entities are routed directly through Cloud Firestore client.
    """
    global db
    if db is None:
        db = init_firestore_client()

    now_iso = datetime.utcnow().isoformat()
    now_millis = int(datetime.utcnow().timestamp() * 1000)

    try:
        if db:
            batch = db.batch()
            stats = {"merchants": 0, "visits": 0, "auditLogs": 0, "users": 0}

            # 1. Upsert merchants to Cloud Firestore
            if payload.merchants:
                for m in payload.merchants:
                    mid = str(m.get("merchantId") or m.get("id") or m.get("qrCode") or f"m_{now_millis}").strip()
                    m["merchantId"] = mid
                    m["updatedAt"] = now_millis
                    batch.set(db.collection("merchants").document(mid), m, merge=True)
                    stats["merchants"] += 1

            # 2. Upsert visits to Cloud Firestore
            if payload.visits:
                for v in payload.visits:
                    vid = str(v.get("id") or f"visit_{now_millis}_{stats['visits']}").strip()
                    v["id"] = vid
                    batch.set(db.collection("visits").document(vid), v, merge=True)
                    stats["visits"] += 1

            # 3. Save audit logs to Cloud Firestore
            if payload.auditLogs:
                for log in payload.auditLogs:
                    lid = str(log.get("id") or f"log_{now_millis}_{stats['auditLogs']}").strip()
                    log["id"] = lid
                    batch.set(db.collection("audit_logs").document(lid), log, merge=True)
                    stats["auditLogs"] += 1

            # 4. Save users if present
            if payload.users:
                for u in payload.users:
                    uid = str(u.get("username") or "").strip().lower()
                    if uid:
                        u["username"] = uid
                        batch.set(db.collection("users").document(uid), u, merge=True)
                        stats["users"] += 1

            # 5. Store snapshot record in backups collection in Cloud Firestore
            backup_doc_id = f"backup_{now_millis}"
            backup_summary = {
                "backupId": backup_doc_id,
                "projectId": PROJECT_ID,
                "timestamp": now_millis,
                "timestampIso": now_iso,
                "stats": stats,
                "hasRawSqlite": bool(payload.rawSqliteBase64)
            }
            batch.set(db.collection("backups").document(backup_doc_id), backup_summary)

            batch.commit()

            return {
                "success": True,
                "message": f"Successfully synced and routed all data directly to Cloud Firestore (project: {PROJECT_ID}).",
                "backupId": backup_doc_id,
                "projectId": PROJECT_ID,
                "recordsUpserted": stats,
                "timestamp": now_iso
            }
        else:
            raise HTTPException(status_code=503, detail="Cloud Firestore client is currently unavailable.")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Firestore cloud backup error: {str(e)}")

# =============================================================================
# 11. Main Server Entrypoint
# =============================================================================
if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", 8000))
    print(f"Starting Cloud Firestore Backend on port {port} for project: {PROJECT_ID}...")
    uvicorn.run(app, host="0.0.0.0", port=port)

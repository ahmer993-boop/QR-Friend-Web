package com.example.ui.common

enum class UserRole(val displayName: String) {
    MASTER("Master"),
    ADMIN("Admin"),
    ASM("ASM"),
    TL("Team Leader"),
    BDO("BDO")
}

enum class MasterTab(val title: String) {
    DASHBOARD("Dashboard"),
    HIERARCHY("Org Hierarchy"),
    USERS("User Management"),
    QR_OPERATIONS("QR Operations"),
    MERCHANTS("Merchants"),
    EXCEL_UPLOAD("Excel Upload"),
    VISITS("Visits"),
    MAP_VIEW("Map View"),
    REPORTS("Reports"),
    AUDIT_LOGS("Audit Logs"),
    SETTINGS("Settings")
}

enum class AdminTab(val title: String) {
    DASHBOARD("Dashboard"),
    QR_OPERATIONS("QR Operations"),
    MERCHANTS("Merchants"),
    FIELD_TEAM("Field Team"),
    VISITS("Visits"),
    MAP_VIEW("Map View"),
    VERIFICATION("Verification"),
    TARGETS("Targets"),
    REPORTS("Reports"),
    EXCEL_UPLOAD("Excel Upload"),
    SETTINGS("Settings")
}

enum class AsmTab(val title: String) {
    DASHBOARD("Dashboard"),
    QR_OPERATIONS("QR Operations"),
    TLS("Assigned TLs"),
    BDOS("Team BDOs"),
    MERCHANTS("Merchants"),
    VISITS("Visits"),
    PERFORMANCE("Performance")
}

enum class TlTab(val title: String) {
    DASHBOARD("Dashboard"),
    QR_OPERATIONS("QR Operations"),
    MY_BDOS("My BDOs"),
    MERCHANTS("Merchants"),
    VISITS("Visits"),
    PERFORMANCE("Performance")
}

enum class BdoTab(val title: String) {
    HOME("Home"),
    MERCHANTS("My Merchants"),
    VISITS("Visits"),
    QR_REQUESTS("QR Requests"),
    TARGETS("Targets"),
    PROFILE("Profile")
}

enum class DateRangeFilter(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}


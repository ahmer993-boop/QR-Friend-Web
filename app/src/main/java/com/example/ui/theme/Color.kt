package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==================================================
// QR Friend Official Brand Palette (Strict User Specification)
// ==================================================

// 1. Primary Brand Greens
val BrandGreen = Color(0xFF008F5A)         // Primary brand green #008F5A
val BrandGreenDark = Color(0xFF006B45)     // Dark brand green #006B45
val BrandGreenDeep = Color(0xFF004F36)     // Deepest green #004F36 (Sidebar / Navigation)
val BrandGreenLight = Color(0xFFE8F7F0)    // Light green #E8F7F0
val BrandGreenSubtle = Color(0xFFF3FBF7)   // Very light green #F3FBF7
val BrandGreenBorder = Color(0xFFDDE5E1)   // Border #DDE5E1

// 2. Base & Surface Colors
val White = Color(0xFFFFFFFF)
val PageBg = Color(0xFFF6F8F7)             // Main page background #F6F8F7
val CardWhite = Color(0xFFFFFFFF)          // Card background #FFFFFF
val TableHeaderBg = Color(0xFFF3FBF7)      // Table header background #F3FBF7

// 3. Typography & Contrast (Strictly Fixed)
val TextHeadings = Color(0xFF14251F)       // Headings #14251F
val TextPrimary = Color(0xFF1F2933)        // Primary text #1F2933
val TextSecondary = Color(0xFF5B6570)      // Secondary text #5B6570
val TextMuted = Color(0xFF7A858F)          // Muted / helper text #7A858F
val TextLink = Color(0xFF006B45)           // Links #006B45

// 4. Structural Lines & Dividers
val BorderColor = Color(0xFFDDE5E1)        // Border #DDE5E1
val DividerColor = Color(0xFFE6ECE9)       // Divider #E6ECE9

// 5. Semantic Feedback
val SuccessGreen = Color(0xFF008F5A)       // Success #008F5A
val SuccessBg = Color(0xFFE8F7F0)          // Success badge background #E8F7F0
val SuccessText = Color(0xFF006B45)        // Success badge text #006B45

val WarningAmber = Color(0xFFD99000)       // Warning #D99000
val WarningYellow = WarningAmber           // Warning alias
val WarningBg = Color(0xFFFEF3C7)          // Warning light warm background
val WarningText = Color(0xFF92400E)        // Warning dark readable text

val ErrorRed = Color(0xFFD64545)           // Error #D64545
val ErrorBg = Color(0xFFFEF2F2)            // Error light red background
val ErrorText = Color(0xFFD64545)          // Error text #D64545

val InfoBlue = Color(0xFF2878C8)           // Info #2878C8
val InfoBg = Color(0xFFEFF6FF)             // Info light blue background
val InfoText = Color(0xFF1D4ED8)           // Info text

// 6. Navigation & Sidebar Tokens
val NavBackground = Color(0xFF004F36)      // Background #004F36
val NavItemSelected = Color(0xFF008F5A)    // Selected menu item #008F5A
val NavTextSelected = Color(0xFFFFFFFF)    // Selected item text #FFFFFF
val NavTextNormal = Color(0xFFE8F7F0)      // Normal navigation text #E8F7F0
val NavIconNormal = Color(0xFFE8F7F0)      // Normal icon #E8F7F0
val NavIconSelected = Color(0xFFFFFFFF)    // Selected icon #FFFFFF
val NavHover = Color(0xFF006B45)           // Hover state #006B45

// 7. Button Colors
val ButtonPrimaryBg = Color(0xFF008F5A)    // Primary button background #008F5A
val ButtonPrimaryText = Color(0xFFFFFFFF)  // Primary button text #FFFFFF
val ButtonPrimaryHover = Color(0xFF006B45) // Primary button hover #006B45
val ButtonDisabledBg = Color(0xFFE6ECE9)   // Disabled button background
val ButtonDisabledText = Color(0xFF7A858F) // Disabled button text

val ButtonSecondaryBg = Color(0xFFFFFFFF)  // Secondary button background #FFFFFF
val ButtonSecondaryBorder = Color(0xFF008F5A) // Secondary button border #008F5A
val ButtonSecondaryText = Color(0xFF006B45)   // Secondary button text #006B45
val ButtonSecondaryHover = Color(0xFFE8F7F0)  // Secondary button hover #E8F7F0

// 8. Backwards-compatibility Aliases (for existing screen references)
val ScreenBg = PageBg
val DangerText = ErrorText
val DangerBg = ErrorBg
val DangerRed = ErrorRed
val WarningOrange = WarningAmber

val Navy900 = TextPrimary                 // High-contrast primary text #1F2933
val Navy800 = TextHeadings                // High-contrast headings #14251F
val Navy700 = TextSecondary               // Secondary text #5B6570

val PrimaryBlue = BrandGreen
val PrimaryBlueLight = BrandGreenLight
val PrimaryBlueSubtle = BrandGreenSubtle

val EmeraldSuccess = SuccessGreen
val EmeraldDark = BrandGreenDark
val EmeraldSubtle = SuccessBg

val AmberWarning = WarningAmber
val AmberSubtle = WarningBg

val RedDanger = ErrorRed
val RedSubtle = ErrorBg

val SlateMuted = TextSecondary
val SlateBorder = BorderColor
val SlateBg = PageBg

val Purple40 = BrandGreen
val PurpleGrey40 = BorderColor
val Pink40 = BrandGreenDark

val Purple80 = BrandGreenLight
val PurpleGrey80 = BorderColor
val Pink80 = SuccessGreen


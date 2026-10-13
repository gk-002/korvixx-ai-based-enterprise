package com.example.ui

import android.content.Context
import android.content.Intent
import android.content.ClipboardManager
import android.content.ClipData
import android.os.Environment
import android.widget.Toast
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.data.*
import com.example.data.database.SystemActivityEntity

/**
 * Parses simple markdown (e.g., **bold**) to stylized AnnotatedString.
 */
fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val boldStart = text.indexOf("**", cursor)
            if (boldStart != -1) {
                // Append normal text before bold
                append(text.substring(cursor, boldStart))
                
                val boldEnd = text.indexOf("**", boldStart + 2)
                if (boldEnd != -1) {
                    // Append bold text
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(boldStart + 2, boldEnd))
                    }
                    cursor = boldEnd + 2
                } else {
                    // No matching end, just append the "**" and rest
                    append("**")
                    cursor = boldStart + 2
                }
            } else {
                append(text.substring(cursor))
                break
            }
        }
    }
}

@Composable
fun AccessRestrictedPlaceholder(permissionRequired: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
        border = BorderStroke(1.dp, KorvixRed.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Access Restricted",
                tint = KorvixRed,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Access Restricted",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = KorvixTextDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your current profile role does not possess the required database permission: $permissionRequired. Please authenticate under a higher authorization node.",
                fontSize = 11.sp,
                color = KorvixTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 400.dp)
            )
        }
    }
}

@Composable
fun KorvixAppUI(
    viewModel: KorvixDashboardViewModel = viewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentViewRole by viewModel.currentViewRole.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = KorvixBackground
    ) {
        if (currentUser == null) {
            // Portal Gateway selection screen
            PortalGatewayScreen(
                onSelectRole = { email, name, role ->
                    viewModel.loginUser(email, name, role)
                },
                onLogAudit = { actionType, details, status, email, role ->
                    viewModel.logAuditAction(actionType, details, status, email, role)
                }
            )
        } else {
            // Main App Dashboard Layout with Active Portal View
            DashboardLayout(
                viewModel = viewModel,
                currentUser = currentUser!!,
                currentViewRole = currentViewRole
            )
        }
    }
}

// --- GATEWAY LANDING SCREEN ---
@Composable
fun PortalGatewayScreen(
    onSelectRole: (String, String, UserRole) -> Unit,
    onLogAudit: (String, String, String, String?, String?) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var targetPortal by remember { mutableStateOf(UserRole.EMPLOYEE) }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val gatewayGradientColors = if (isSystemDark.value) {
        listOf(Color(0xFF0F172A), Color(0xFF090D16), Color(0xFF1E1E38))
    } else {
        listOf(Color(0xFFEEF2FF), Color(0xFFF5F3FF), Color(0xFFFBF8FF))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.linearGradient(colors = gatewayGradientColors))
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(KorvixPrimary, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "KORVIX",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = KorvixTextDark,
                    letterSpacing = 2.sp
                )
            }

            Text(
                text = "Enterprise Role-Based Access Control System",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = KorvixTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Login glassmorphism card
            Card(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SECURE PORTAL LOGIN",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = KorvixTextDark,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Target Portal Selector
                    Text(
                        text = "1. Select Target Portal View",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixPrimary,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            UserRole.EMPLOYEE to "Employee",
                            UserRole.SECRETARY to "Secretary",
                            UserRole.HR to "HR Portal",
                            UserRole.ADMIN to "Admin"
                        ).forEach { (role, label) ->
                            val isSelected = targetPortal == role
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) KorvixPrimary else KorvixBorder)
                                    .clickable { 
                                        targetPortal = role 
                                        errorMessage = null // Clear error
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else KorvixTextDark
                                )
                            }
                        }
                    }

                    // Credentials Fields
                    Text(
                        text = "2. Enter Access Credentials",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixPrimary,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it
                            errorMessage = null
                        },
                        label = { Text("Email Address", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KorvixPrimary,
                            unfocusedBorderColor = KorvixBorder,
                            focusedTextColor = KorvixTextDark,
                            unfocusedTextColor = KorvixTextDark
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Access Password", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(image, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KorvixPrimary,
                            unfocusedBorderColor = KorvixBorder,
                            focusedTextColor = KorvixTextDark,
                            unfocusedTextColor = KorvixTextDark
                        )
                    )

                    // Error Alert Block
                    errorMessage?.let { err ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .background(KorvixRedLight, RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, KorvixRed.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Error",
                                tint = KorvixRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = err,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = KorvixRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Log In Button
                    Button(
                        onClick = {
                            val cleanEmail = email.trim().lowercase()
                            val cleanPassword = password.trim()

                            // Basic input validation
                            if (cleanEmail.isEmpty() || cleanPassword.isEmpty()) {
                                errorMessage = "Please enter both email and password."
                                return@Button
                            }

                            // Match profiles
                            val matchedRole: UserRole? = when {
                                cleanEmail == "david.miller@korvix.com" && cleanPassword == "admin123" -> UserRole.ADMIN
                                cleanEmail == "sarah.jenkins@korvix.com" && cleanPassword == "hr123" -> UserRole.HR
                                cleanEmail == "emily.davis@korvix.com" && cleanPassword == "secretary123" -> UserRole.SECRETARY
                                cleanEmail == "ankit.sharma@korvix.com" && cleanPassword == "employee123" -> UserRole.EMPLOYEE
                                else -> null
                            }

                            if (matchedRole == null) {
                                errorMessage = "Invalid credentials. Please verify your email and password or use pre-fill shortcuts below."
                                onLogAudit("LOGIN", "Failed login attempt with email: $cleanEmail", "DENIED", cleanEmail, "ANONYMOUS")
                                return@Button
                            }

                            // STRICT SECURITY CHECK: Restrict employee and secretary from entering HR Portal (or Admin Portal)
                            if (targetPortal == UserRole.HR || targetPortal == UserRole.ADMIN) {
                                if (matchedRole == UserRole.EMPLOYEE || matchedRole == UserRole.SECRETARY) {
                                    errorMessage = "🛑 Access Denied: Employees and Secretaries are strictly prohibited from logging into or accessing the HR Portal."
                                    onLogAudit("LOGIN", "Blocked attempt by $cleanEmail (${matchedRole.name}) to enter HR/Admin portal", "DENIED", cleanEmail, matchedRole.name)
                                    return@Button
                                }
                            }

                            // Ensure Admin target portal is only for admin
                            if (targetPortal == UserRole.ADMIN && matchedRole != UserRole.ADMIN) {
                                errorMessage = "🛑 Access Denied: Administrator clearance is required to enter the Admin Portal."
                                onLogAudit("LOGIN", "Blocked attempt by $cleanEmail (${matchedRole.name}) to enter Admin portal", "DENIED", cleanEmail, matchedRole.name)
                                return@Button
                            }

                            // HR target portal is only for HR and Admin
                            if (targetPortal == UserRole.HR && matchedRole != UserRole.HR && matchedRole != UserRole.ADMIN) {
                                errorMessage = "🛑 Access Denied: HR clearance is required to enter the HR Portal."
                                onLogAudit("LOGIN", "Blocked attempt by $cleanEmail (${matchedRole.name}) to enter HR portal", "DENIED", cleanEmail, matchedRole.name)
                                return@Button
                            }

                            // If validation passes, execute login!
                            val name = when (matchedRole) {
                                UserRole.ADMIN -> "David Miller"
                                UserRole.HR -> "Sarah Jenkins"
                                UserRole.SECRETARY -> "Emily Davis"
                                UserRole.EMPLOYEE -> "Ankit Sharma"
                            }
                            
                            // Success! Select role and target view role
                            onSelectRole(cleanEmail, name, matchedRole)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KorvixPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("secure_login_button")
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Authenticate & Enter Portal",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Divider(color = KorvixBorder)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Developer pre-fills
                    Text(
                        text = "Developer Quick-Login Nodes",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixTextMuted,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("Admin", "david.miller@korvix.com", "admin123") to UserRole.ADMIN,
                            Triple("HR", "sarah.jenkins@korvix.com", "hr123") to UserRole.HR,
                            Triple("Secretary", "emily.davis@korvix.com", "secretary123") to UserRole.SECRETARY,
                            Triple("Employee", "ankit.sharma@korvix.com", "employee123") to UserRole.EMPLOYEE
                        ).forEach { (meta, role) ->
                            val (label, e, p) = meta
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(KorvixPrimaryLight)
                                    .clickable {
                                        email = e
                                        password = p
                                        targetPortal = role
                                        errorMessage = null
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KorvixPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "KORVIX Cryptographic Node Access Protocol • 2026",
                fontSize = 11.sp,
                color = KorvixTextMuted,
                fontWeight = FontWeight.Medium
            )
        }

        // Floating gateway theme toggle button
        IconButton(
            onClick = { isSystemDark.value = !isSystemDark.value },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .testTag("gateway_theme_toggle")
        ) {
            Icon(
                imageVector = if (isSystemDark.value) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle Theme",
                tint = KorvixPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}


// --- MAIN APP DASHBOARD LAYOUT (WITH PERSISTENT SIDEBAR & AI DRAWER) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardLayout(
    viewModel: KorvixDashboardViewModel,
    currentUser: UserProfile,
    currentViewRole: UserRole
) {
    val context = LocalContext.current

    // Observe global toast notification system triggers for AI anomalies & missing data patterns
    LaunchedEffect(viewModel) {
        viewModel.toastNotification.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportDialogTitle by remember { mutableStateOf("") }
    var exportDialogFilename by remember { mutableStateOf("") }
    var exportDialogContent by remember { mutableStateOf("") }
    var exportErrorMsg by remember { mutableStateOf<String?>(null) }

    val onExportTable = { targetRole: UserRole, tableName: String, headers: List<String>, rows: List<List<String>> ->
        val hasPermission = when (targetRole) {
            UserRole.ADMIN -> currentUser.role == UserRole.ADMIN
            UserRole.HR -> currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.HR
            UserRole.SECRETARY -> currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.HR || currentUser.role == UserRole.SECRETARY
            UserRole.EMPLOYEE -> true
        }

        if (!hasPermission) {
            exportErrorMsg = "🛑 Access Denied: Your current profile level (${currentUser.role.name}) does not have clearance authorization to export $tableName sensitive corporate records."
            showExportDialog = true
            exportDialogTitle = "Access Denied"
            exportDialogFilename = ""
            exportDialogContent = ""
        } else {
            val csvBuilder = java.lang.StringBuilder()
            csvBuilder.append(headers.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }).append("\n")
            rows.forEach { row ->
                csvBuilder.append(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }).append("\n")
            }
            val csvText = csvBuilder.toString()
            val cleanFileName = tableName.lowercase().replace(" ", "_").replace("&", "and") + "_export.csv"

            exportErrorMsg = null
            exportDialogTitle = "CSV Export Generated Successfully"
            exportDialogFilename = cleanFileName
            exportDialogContent = csvText
            showExportDialog = true

            try {
                val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir != null) {
                    val file = java.io.File(downloadsDir, cleanFileName)
                    file.writeText(csvText)
                }
            } catch (e: Exception) {
                // Ignore silent writing issues
            }
        }
    }

    var isDrawerOpen by remember { mutableStateOf(false) } // Controls AI Drawer on phones/tablets
    var showAIBottomSheet by remember { mutableStateOf(false) } // Controls AI Bottom Sheet
    var isSidebarCollapsed by remember { mutableStateOf(false) } // Toggles compact sidebar layout

    val isStreamingData by viewModel.isStreamingData.collectAsState()
    val streamingProgress by viewModel.streamingProgress.collectAsState()

    val dashboardGradientColors = if (isSystemDark.value) {
        listOf(Color(0xFF0F172A), Color(0xFF090D16), Color(0xFF1E1E38))
    } else {
        listOf(Color(0xFFEEF2FF), Color(0xFFF5F3FF), Color(0xFFFBF8FF))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.linearGradient(colors = dashboardGradientColors))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAIBottomSheet = true },
                    containerColor = KorvixPrimary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag("ai_assistant_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Open AI Assistant",
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(end = 12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "16 May 2025, Friday",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KorvixTextMuted
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(KorvixPrimaryLight, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = currentViewRole.name + " PORTAL",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = KorvixPrimary
                                    )
                                }
                            }
                            Text(
                                text = "KORVIX Global Intelligence Engine",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = KorvixTextDark
                            )
                        }

                        // Search Bar (simulated)
                        var searchTxt by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = searchTxt,
                            onValueChange = { searchTxt = it },
                            placeholder = { Text("Search nodes...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier
                                .width(180.dp)
                                .height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KorvixPrimary,
                                unfocusedBorderColor = KorvixBorderAccent,
                                focusedContainerColor = KorvixSurface,
                                unfocusedContainerColor = KorvixSurface,
                                focusedTextColor = KorvixTextDark,
                                unfocusedTextColor = KorvixTextDark,
                                focusedPlaceholderColor = KorvixTextMuted,
                                unfocusedPlaceholderColor = KorvixTextMuted,
                                focusedLeadingIconColor = KorvixPrimary,
                                unfocusedLeadingIconColor = KorvixTextMuted
                            ),
                            singleLine = true
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { isSidebarCollapsed = !isSidebarCollapsed }) {
                        Icon(Icons.Default.Menu, contentDescription = "Toggle Sidebar", tint = KorvixTextDark)
                    }
                },
                actions = {
                    // Global Theme Toggle (Light / High-Contrast Dark)
                    IconButton(
                        onClick = { isSystemDark.value = !isSystemDark.value },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isSystemDark.value) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = KorvixPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    // Quick indicator of logged in person
                    IconButton(onClick = { isDrawerOpen = !isDrawerOpen }, modifier = Modifier.testTag("ai_drawer_toggle")) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = KorvixPrimary) {
                                    Text("AI", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = "AI Assistant", tint = KorvixPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KorvixGlassSurface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // 1. Sidebar Panel
                PersistentSidebar(
                    viewModel = viewModel,
                    currentUser = currentUser,
                    currentViewRole = currentViewRole,
                    isCollapsed = isSidebarCollapsed
                )

                // 2. Active Screen Portal Dashboard Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.Transparent)
                ) {
                    val customData by viewModel.customAggregatedData.collectAsState()

                     when (currentViewRole) {
                        UserRole.ADMIN -> AdminPortalView(viewModel = viewModel, customData = customData, onExportTable = onExportTable)
                        UserRole.HR -> HRPortalView(viewModel = viewModel, customData = customData, onExportTable = onExportTable)
                        UserRole.SECRETARY -> SecretaryPortalView(viewModel = viewModel, onExportTable = onExportTable)
                        UserRole.EMPLOYEE -> EmployeePortalView(viewModel = viewModel, onExportTable = onExportTable)
                    }

                    // Simulated High-Performance Data Streaming Animation Overlay
                    if (isStreamingData) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .width(300.dp)
                                    .padding(16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        color = KorvixPrimary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Korvix AI is streaming and parsing 10,000+ records...",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KorvixTextDark,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { streamingProgress },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = KorvixPrimary,
                                        trackColor = KorvixBorder
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${(streamingProgress * 100).toInt()}% Aggregated",
                                        fontSize = 11.sp,
                                        color = KorvixTextMuted,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Right slide-out Korvix AI Assistant Panel
            AnimatedVisibility(
                visible = isDrawerOpen,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                // Background dimming layer for drawer click-away
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { isDrawerOpen = false }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(360.dp)
                            .background(KorvixGlassSurface)
                            .align(Alignment.CenterEnd)
                            .clickable(enabled = false) {} // Prevent click through
                    ) {
                        KorvixAIAssistantDrawer(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            onClose = { isDrawerOpen = false }
                        )
                    }
                }
            }
        }
    }

    if (showAIBottomSheet) {
        KorvixAIAssistantBottomSheet(
            viewModel = viewModel,
            currentUser = currentUser,
            onDismissRequest = { showAIBottomSheet = false }
        )
    }
    
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (exportErrorMsg != null) Icons.Default.Security else Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = if (exportErrorMsg != null) KorvixRed else KorvixPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (exportErrorMsg != null) "Security Clearance Alert" else "Data Export Terminal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixTextDark
                    )
                }
            },
            text = {
                Column {
                    if (exportErrorMsg != null) {
                        Text(
                            text = exportErrorMsg ?: "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = KorvixRed,
                            modifier = Modifier
                                .background(KorvixRedLight, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        )
                    } else {
                        Text(
                            text = "Success! CSV format generated matching the authorized role container.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = KorvixTextDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "File Name: ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = KorvixTextMuted
                            )
                            Text(
                                text = exportDialogFilename,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = KorvixPrimary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Path: /Android/data/com.example/files/Download/$exportDialogFilename",
                            fontSize = 9.sp,
                            color = KorvixTextMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "PREVIEW (First 3 lines):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = KorvixTextDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val first3Lines = exportDialogContent.lineSequence().take(4).joinToString("\n")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 100.dp)
                                .background(Color(0xFF1E1E2F), RoundedCornerShape(6.dp))
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            Text(
                                text = first3Lines,
                                fontSize = 10.sp,
                                color = Color(0xFFA5B4FC),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (exportErrorMsg == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Korvix Export CSV", exportDialogContent)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to copy", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KorvixPrimary)
                        ) {
                            Text("Copy CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = {
                                try {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Korvix CSV Export - $exportDialogFilename")
                                        putExtra(Intent.EXTRA_TEXT, exportDialogContent)
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share CSV via")
                                    context.startActivity(shareIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to share", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KorvixSecondary)
                        ) {
                            Text("Share File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KorvixTextMuted)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
    }
}


// --- PERSISTENT LEFT SIDEBAR PANEL ---
@Composable
fun PersistentSidebar(
    viewModel: KorvixDashboardViewModel,
    currentUser: UserProfile,
    currentViewRole: UserRole,
    isCollapsed: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (isCollapsed) 72.dp else 240.dp)
            .background(KorvixGlassSurface)
            .border(width = 1.dp, color = KorvixBorder, shape = RoundedCornerShape(0.dp))
            .padding(vertical = 16.dp, horizontal = if (isCollapsed) 8.dp else 16.dp),
        horizontalAlignment = if (isCollapsed) Alignment.CenterHorizontally else Alignment.Start
    ) {
        // App Identity
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(KorvixPrimary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            if (!isCollapsed) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "KORVIX",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = KorvixTextDark,
                    letterSpacing = 1.sp
                )
            }
        }

        Divider(color = KorvixBorder, modifier = Modifier.padding(bottom = 16.dp))

        // Navigation links based on currently active view with strict RBAC filtering
        val navItems = listOf(
            Triple(UserRole.ADMIN, "System Admin", Icons.Default.AdminPanelSettings),
            Triple(UserRole.HR, "HR Portal", Icons.Default.Groups),
            Triple(UserRole.SECRETARY, "Secretary Control", Icons.Default.EventNote),
            Triple(UserRole.EMPLOYEE, "Employee Workspace", Icons.Default.Person)
        ).filter { (role, _, _) ->
            when (currentUser.role) {
                UserRole.ADMIN -> true
                UserRole.HR -> role == UserRole.HR || role == UserRole.EMPLOYEE
                UserRole.SECRETARY -> role == UserRole.SECRETARY || role == UserRole.EMPLOYEE
                UserRole.EMPLOYEE -> role == UserRole.EMPLOYEE
            }
        }

        Text(
            text = if (isCollapsed) "PORT" else "AVAILABLE PORTALS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = KorvixTextMuted,
            modifier = Modifier.padding(bottom = 8.dp, start = if (isCollapsed) 0.dp else 8.dp)
        )

        navItems.forEach { (role, label, icon) ->
            val isActive = currentViewRole == role
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) KorvixPrimaryLight else Color.Transparent)
                    .clickable { viewModel.switchPortalView(role) }
                    .padding(vertical = 10.dp, horizontal = if (isCollapsed) 4.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isCollapsed) Arrangement.Center else Arrangement.Start
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (isActive) KorvixPrimary else KorvixTextMuted,
                    modifier = Modifier.size(18.dp)
                )
                if (!isCollapsed) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive) KorvixPrimary else KorvixTextDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Profile Avatar Widget at bottom
        Divider(color = KorvixBorder, modifier = Modifier.padding(bottom = 16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.logout() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isCollapsed) Arrangement.Center else Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(KorvixSecondary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentUser.name.split(" ").map { it.take(1) }.joinToString(""),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = KorvixPrimary
                )
            }
            if (!isCollapsed) {
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = currentUser.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixTextDark
                    )
                    Text(
                        text = "Switch/Logout",
                        fontSize = 10.sp,
                        color = KorvixRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


@Composable
fun InteractiveSystemUsageCard() {
    var isLiveActive by remember { mutableStateOf(true) }
    var selectedBar by remember { mutableStateOf<String?>(null) } // "bandwidth" or "storage" or null
    
    // Live states
    var bandwidthMbps by remember { mutableStateOf(145.2f) }
    var storageGB by remember { mutableStateOf(256.4f) }
    
    // Periodically update live metrics if active
    LaunchedEffect(isLiveActive) {
        while (isLiveActive) {
            delay(2000)
            // Gently fluctuate bandwidth around 130-180 Mbps
            bandwidthMbps = (130f + (0..50).random()).coerceIn(100f, 250f)
            // Gently fluctuate storage around 256-258 GB
            storageGB = (256.0f + (0..20).random() / 10f)
        }
    }
    
    // Animate progress widths
    val bandwidthProgress by animateFloatAsState(
        targetValue = bandwidthMbps / 250f,
        animationSpec = tween(1000)
    )
    val storageProgress by animateFloatAsState(
        targetValue = storageGB / 600f,
        animationSpec = tween(1000)
    )
    
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
        border = BorderStroke(1.dp, KorvixBorderAccent),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("system_usage_recharts_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "System Usage",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = KorvixTextMuted
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Pulsing Live Indicator
                        if (isLiveActive) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(KorvixGreen, CircleShape)
                            )
                        }
                    }
                    Text(
                        text = "Interactive Live Recharts Telemetry",
                        fontSize = 14.sp,
                        color = KorvixTextDark,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Live Stream Pause/Play Toggle Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { isLiveActive = !isLiveActive },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiveActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = KorvixPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isLiveActive) "PAUSE LIVE" else "RESUME LIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = KorvixPrimary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Recharts-style Interactive Bars Container
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                
                // 1. BANDWIDTH BAR
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            selectedBar = if (selectedBar == "bandwidth") null else "bandwidth"
                        }
                        .background(if (selectedBar == "bandwidth") KorvixPrimaryLight.copy(alpha = 0.5f) else Color.Transparent)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(KorvixPrimary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Live Bandwidth Speed",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = KorvixTextDark
                            )
                        }
                        Text(
                            text = "${String.format("%.1f", bandwidthMbps)} Mbps",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = KorvixPrimary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Styled progress container (resembles high-performance chart track)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .background(KorvixBorder, RoundedCornerShape(7.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(bandwidthProgress)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(KorvixPrimary, KorvixBlue)
                                    ),
                                    shape = RoundedCornerShape(7.dp)
                                )
                        )
                    }
                    Text(
                        text = "Click to inspect latency and cluster routing details",
                        fontSize = 8.sp,
                        color = KorvixTextMuted,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // 2. STORAGE BAR
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            selectedBar = if (selectedBar == "storage") null else "storage"
                        }
                        .background(if (selectedBar == "storage") KorvixPrimaryLight.copy(alpha = 0.5f) else Color.Transparent)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(KorvixOrange, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Storage Capacity",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = KorvixTextDark
                            )
                        }
                        Text(
                            text = "${String.format("%.1f", storageGB)} GB / 600 GB",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = KorvixOrange
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .background(KorvixBorder, RoundedCornerShape(7.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(storageProgress)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(KorvixOrange, KorvixSecondary)
                                    ),
                                    shape = RoundedCornerShape(7.dp)
                                )
                        )
                    }
                    Text(
                        text = "Click to inspect sector usage and partition details",
                        fontSize = 8.sp,
                        color = KorvixTextMuted,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Dynamic Recharts Interactive Tooltip Overlay (Only visible when a bar is selected)
            AnimatedVisibility(
                visible = selectedBar != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isSystemDark.value) {
                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                } else {
                                    listOf(Color(0xFF2E1065), Color(0xFF1E1B4B))
                                }
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(1.dp, KorvixPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedBar == "bandwidth") "✦ RECHARTS TELEMETRY: BANDWIDTH" else "✦ RECHARTS TELEMETRY: STORAGE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA5B4FC)
                            )
                            IconButton(
                                onClick = { selectedBar = null },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        if (selectedBar == "bandwidth") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Peak Velocity", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
                                    Text("245.0 Mbps", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Column {
                                    Text("Active Nodes", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
                                    Text("42 Connection Units", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Column {
                                    Text("Response Latency", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
                                    Text("14ms (Excellent)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KorvixGreen)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Database Cache", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
                                    Text("12.4 GB allocated", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Column {
                                    Text("Temporary Files", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
                                    Text("2.8 GB buffered", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Column {
                                    Text("Sector Status", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
                                    Text("Healthy", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KorvixGreen)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun BusinessUnitDataTableView(
    onExportTable: (UserRole, String, List<String>, List<List<String>>) -> Unit,
    currentUserRole: UserRole
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
        border = BorderStroke(1.dp, KorvixBorderAccent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Business Unit Financial & Risk Analytics",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixTextDark
                    )
                    Text(
                        text = "Real-time revenue, investment, ROI, and risk score metrics across organizational nodes",
                        fontSize = 9.sp,
                        color = KorvixTextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(
                    onClick = {
                        val headers = listOf("Business Unit", "Investment", "Revenue", "ROI", "Risk Score", "Risk Level")
                        val rows = listOf(
                            listOf("Engineering", "$1,200,000", "$4,500,000", "275%", "0.18", "Low"),
                            listOf("Sales", "$850,000", "$3,800,000", "347%", "0.35", "Medium"),
                            listOf("Finance", "$600,000", "$1,400,000", "133%", "0.12", "Low"),
                            listOf("HR", "$300,000", "-", "-", "0.08", "Low"),
                            listOf("Marketing", "$950,000", "$2,100,000", "121%", "0.42", "Medium"),
                            listOf("Legal", "$250,000", "-", "-", "0.78", "High")
                        )
                        onExportTable(currentUserRole, "Business Unit Metrics", headers, rows)
                    },
                    modifier = Modifier.size(24.dp).testTag("export_bu_metrics_csv")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export CSV",
                        tint = KorvixPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Headers
            Row(
                modifier = Modifier.fillMaxWidth().background(KorvixPrimaryLight, RoundedCornerShape(6.dp)).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Business Unit", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = KorvixPrimary, modifier = Modifier.weight(1.5f))
                Text("Investment", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = KorvixPrimary, modifier = Modifier.weight(1.2f))
                Text("Revenue", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = KorvixPrimary, modifier = Modifier.weight(1.2f))
                Text("ROI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = KorvixPrimary, modifier = Modifier.weight(1f))
                Text("Risk Score", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = KorvixPrimary, modifier = Modifier.weight(1.2f))
            }

            Spacer(modifier = Modifier.height(4.dp))

            val data = listOf(
                listOf("Engineering", "$1,200,000", "$4,500,000", "275%", "0.18", "Low", KorvixGreen, KorvixGreenLight),
                listOf("Sales", "$850,000", "$3,800,000", "347%", "0.35", "Medium", KorvixOrange, KorvixOrange.copy(alpha = 0.15f)),
                listOf("Finance", "$600,000", "$1,400,000", "133%", "0.12", "Low", KorvixGreen, KorvixGreenLight),
                listOf("HR", "$300,000", "-", "-", "0.08", "Low", KorvixGreen, KorvixGreenLight),
                listOf("Marketing", "$950,000", "$2,100,000", "121%", "0.42", "Medium", KorvixOrange, KorvixOrange.copy(alpha = 0.15f)),
                listOf("Legal", "$250,000", "-", "-", "0.78", "High", KorvixRed, KorvixRedLight)
            )

            data.forEach { row ->
                val dept = row[0] as String
                val investment = row[1] as String
                val revenue = row[2] as String
                val roi = row[3] as String
                val riskScore = row[4] as String
                val riskLevel = row[5] as String
                val riskColor = row[6] as Color
                val riskBg = row[7] as Color

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(dept, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KorvixTextDark, modifier = Modifier.weight(1.5f))
                    Text(investment, fontSize = 11.sp, color = KorvixTextMuted, modifier = Modifier.weight(1.2f))
                    Text(revenue, fontSize = 11.sp, color = KorvixTextMuted, modifier = Modifier.weight(1.2f))
                    Text(roi, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (roi != "-") KorvixPrimary else KorvixTextMuted, modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier.weight(1.2f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$riskScore ",
                            fontSize = 11.sp,
                            color = KorvixTextDark,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(riskBg)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = riskLevel,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = riskColor
                            )
                        }
                    }
                }
                Divider(color = KorvixBorder)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            ExecutiveInsightCharts()
        }
    }
}

@Composable
fun ExecutiveInsightCharts() {
    var selectedTab by remember { mutableStateOf(0) } // 0: Revenue, 1: ROI, 2: Risk Score

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = KorvixBackground.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, KorvixBorderAccent)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(KorvixPrimaryLight, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = "Executive Insights",
                        tint = KorvixPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Executive Visualizations",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixTextDark
                    )
                    Text(
                        text = "Interactive visual comparisons matching corporate compliance data",
                        fontSize = 8.sp,
                        color = KorvixTextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Tab Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(KorvixBackground.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val tabs = listOf("📊 Revenue", "📈 ROI %", "🛡️ Risk Score")
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) KorvixPrimary else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else KorvixTextDark
                        )
                    }
                }
            }

            // Chart Content based on tab
            when (selectedTab) {
                0 -> {
                    // Revenue Bar Chart
                    Text(
                        text = "Department Revenue comparison (Millions of USD)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixTextDark,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    val revenues = listOf(
                        Pair("Engineering", 4.5f),
                        Pair("Sales", 3.8f),
                        Pair("Marketing", 2.1f),
                        Pair("Finance", 1.4f),
                        Pair("HR", 0.0f),
                        Pair("Legal", 0.0f)
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        revenues.forEach { (dept, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dept,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = KorvixTextDark,
                                    modifier = Modifier.width(70.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(10.dp)
                                        .background(Color.LightGray.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                                ) {
                                    val progressFraction = value / 4.5f
                                    if (value > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(progressFraction)
                                                .fillMaxHeight()
                                                .background(
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(KorvixPrimary, KorvixSecondary)
                                                    ),
                                                    shape = RoundedCornerShape(3.dp)
                                                )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (value > 0) "$${value}M" else "$0.0",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KorvixPrimary,
                                    modifier = Modifier.width(35.dp)
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // ROI Chart
                    Text(
                        text = "Department Return on Investment (ROI %)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixTextDark,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    val rois = listOf(
                        Pair("Sales", 347f),
                        Pair("Engineering", 275f),
                        Pair("Finance", 133f),
                        Pair("Marketing", 121f),
                        Pair("HR", 0f),
                        Pair("Legal", 0f)
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rois.forEach { (dept, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dept,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = KorvixTextDark,
                                    modifier = Modifier.width(70.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(10.dp)
                                        .background(Color.LightGray.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                                ) {
                                    val progressFraction = value / 347f
                                    if (value > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(progressFraction)
                                                .fillMaxHeight()
                                                .background(
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(KorvixOrange, KorvixPrimary)
                                                    ),
                                                    shape = RoundedCornerShape(3.dp)
                                                )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (value > 0) "${value.toInt()}%" else "0%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KorvixOrange,
                                    modifier = Modifier.width(35.dp)
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Risk Score Chart
                    Text(
                        text = "Business Unit Security & Risk Scores",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixTextDark,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    val risks = listOf(
                        Triple("Legal", 0.78f, KorvixRed),
                        Triple("Marketing", 0.42f, KorvixOrange),
                        Triple("Sales", 0.35f, KorvixOrange),
                        Triple("Engineering", 0.18f, KorvixGreen),
                        Triple("Finance", 0.12f, KorvixGreen),
                        Triple("HR", 0.08f, KorvixGreen)
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        risks.forEach { (dept, value, color) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dept,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = KorvixTextDark,
                                    modifier = Modifier.width(70.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(10.dp)
                                        .background(Color.LightGray.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                                ) {
                                    val progressFraction = value / 1.0f
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progressFraction)
                                            .fillMaxHeight()
                                            .background(
                                                color = color,
                                                shape = RoundedCornerShape(3.dp)
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = String.format(java.util.Locale.US, "%.2f", value),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color,
                                    modifier = Modifier.width(35.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PortalFileUploadSection(
    portalType: String,
    onUploadComplete: ((String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Local list of uploaded files
    val uploadedFiles = remember {
        mutableStateListOf<Triple<String, String, String>>().apply {
            if (portalType == "HR") {
                add(Triple("employee_onboarding_guide.pdf", "PDF • 2.4 MB", "Uploaded: 12 May 2025"))
                add(Triple("performance_appraisal_template.docx", "DOCX • 180 KB", "Uploaded: 14 May 2025"))
            } else {
                add(Triple("medical_reimbursement_receipt.png", "PNG • 850 KB", "Uploaded: 15 May 2025"))
                add(Triple("timesheet_may_2025.xlsx", "XLSX • 320 KB", "Uploaded: 10 May 2025"))
            }
        }
    }

    // State for the upload flow
    var newFileName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("PDF") }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
        border = BorderStroke(1.dp, KorvixBorderAccent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(KorvixPrimaryLight, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = "Upload Files",
                        tint = KorvixPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (portalType == "HR") "Corporate Documents Upload Center" else "My Document Vault & Uploads",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixTextDark
                    )
                    Text(
                        text = if (portalType == "HR") "Manage team rosters, policy handbooks, and appraisal documentation" else "Upload medical certificates, proof of expense, and timesheets",
                        fontSize = 9.sp,
                        color = KorvixTextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Interactive Upload Form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KorvixBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .border(
                        BorderStroke(1.dp, KorvixBorderAccent),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                if (isUploading) {
                    // Upload Progress Indicator
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = { uploadProgress },
                            color = KorvixPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Uploading: ${(uploadProgress * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = KorvixPrimary
                        )
                        Text(
                            text = "Syncing with secure KORVIX nodes...",
                            fontSize = 9.sp,
                            color = KorvixTextMuted
                        )
                    }
                } else {
                    // Entry fields
                    Text(
                        text = "File Metadata Input",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixPrimary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newFileName,
                            onValueChange = { newFileName = it },
                            placeholder = { Text("Enter file name (e.g. medical_leave)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f).height(48.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KorvixPrimary,
                                unfocusedBorderColor = KorvixBorder,
                                focusedTextColor = KorvixTextDark,
                                unfocusedTextColor = KorvixTextDark
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                        )

                        // Select file type chips
                        Row(
                            modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("PDF", "CSV", "XLSX", "PNG").forEach { type ->
                                val isSel = selectedType == type
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) KorvixPrimary else KorvixBorder)
                                        .clickable { selectedType = type }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = type,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else KorvixTextDark
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Big Action Button
                    Button(
                        onClick = {
                            if (newFileName.isBlank()) {
                                Toast.makeText(context, "Please enter a valid file name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            coroutineScope.launch {
                                isUploading = true
                                uploadProgress = 0f
                                while (uploadProgress < 1.0f) {
                                    delay(80)
                                    uploadProgress += 0.05f
                                }
                                val finalName = "${newFileName.replace(" ", "_").lowercase()}.${selectedType.lowercase()}"
                                val size = when (selectedType) {
                                    "PDF" -> "PDF • 1.4 MB"
                                    "CSV" -> "CSV • 82 KB"
                                    "XLSX" -> "XLSX • 410 KB"
                                    else -> "PNG • 1.2 MB"
                                }
                                uploadedFiles.add(0, Triple(finalName, size, "Uploaded: Just Now"))
                                onUploadComplete?.invoke(finalName, size)
                                isUploading = false
                                newFileName = ""
                                Toast.makeText(context, "✅ $finalName uploaded and encrypted successfully.", Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KorvixPrimary)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simulate Secure File Upload", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // File vault list
            Text(
                text = "Document Repository (${uploadedFiles.size} items)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = KorvixTextDark,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            if (uploadedFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No documents uploaded yet.", fontSize = 11.sp, color = KorvixTextMuted)
                }
            } else {
                uploadedFiles.forEach { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(KorvixBackground.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when {
                                    file.first.endsWith(".pdf") -> Icons.Default.Description
                                    file.first.endsWith(".csv") || file.first.endsWith(".xlsx") -> Icons.Default.Assessment
                                    else -> Icons.Default.Image
                                },
                                contentDescription = null,
                                tint = KorvixPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(file.first, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KorvixTextDark)
                                Row {
                                    Text(file.second, fontSize = 9.sp, color = KorvixTextMuted, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("•", fontSize = 9.sp, color = KorvixTextMuted)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(file.third, fontSize = 9.sp, color = KorvixTextMuted, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        Row {
                            IconButton(
                                onClick = { Toast.makeText(context, "📥 Downloaded ${file.first} to local cache.", Toast.LENGTH_SHORT).show() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Download", tint = KorvixPrimary, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    uploadedFiles.remove(file)
                                    Toast.makeText(context, "🗑️ Removed ${file.first} from portal repository.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = KorvixRed, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}


// --- PORTAL 1: ADMIN PORTAL SCREEN ---
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AdminPortalView(
    viewModel: KorvixDashboardViewModel,
    customData: AggregatedData?,
    onExportTable: (UserRole, String, List<String>, List<List<String>>) -> Unit
) {
    var selectedRoleFilter by remember { mutableStateOf<String?>(null) }
    val dbUsers by viewModel.dbUsers.collectAsState()
    val dbRolesWithPermissions by viewModel.dbRolesWithPermissions.collectAsState()
    val dbAuditLogs by viewModel.dbAuditLogs.collectAsState()
    val userPermissions by viewModel.currentUserPermissions.collectAsState()
    val hasSystemMetricsPermission = userPermissions.contains("VIEW_SYSTEM_METRICS")
    val hasManageSystemPermission = userPermissions.contains("MANAGE_SYSTEM")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Quick Title
        Text(
            text = "Enterprise Node Control Panel",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = KorvixTextDark,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (!hasSystemMetricsPermission) {
            AccessRestrictedPlaceholder(permissionRequired = "VIEW_SYSTEM_METRICS")
        } else {

        // Metrics Grid Row
        ResponsiveGrid(
            colsMobile = 1,
            colsDesktop = 2,
            spacing = 12.dp
        ) {
            AdminMetricCard(
                title = "Total Users",
                value = if (customData != null) "${customData.totalRowsParsed}" else "1,248",
                icon = Icons.Default.Person,
                subText = "Verified network accounts"
            )
            AdminMetricCard(
                title = "Active Users",
                value = if (customData != null) "${(customData.totalRowsParsed * 0.88).toInt()}" else "1,102",
                icon = Icons.Default.VerifiedUser,
                subText = "88% network concurrency"
            )
            InteractiveSystemUsageCard()

            AdminMetricCard(
                title = "System Uptime",
                value = "99.99%",
                icon = Icons.Default.Dns,
                subText = "All server systems operational"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SystemActivityWidget(viewModel = viewModel)

        Spacer(modifier = Modifier.height(16.dp))

        // Charts Block: Donut Chart & Line Chart
        ResponsiveSplitLayout(
            leftFraction = 1.1f,
            rightFraction = 1f,
            spacing = 16.dp,
            leftContent = {
                // Left Chart: Users by Role Distribution
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
                    border = BorderStroke(1.dp, KorvixBorderAccent)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Users by Role Distribution",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = KorvixTextDark,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val defaultSlices = listOf(
                            DonutSlice("Employees", 52f, KorvixPrimary),
                            DonutSlice("Managers", 16f, KorvixSecondary),
                            DonutSlice("HR", 11f, KorvixOrange),
                            DonutSlice("Secretaries", 7f, KorvixBlue),
                            DonutSlice("Admins", 5f, KorvixRed)
                        )

                                                val finalSlices = if (customData != null) {
                            // Dynamically update based on streamed data count
                            customData.deptDistribution.take(5).mapIndexed { idx, item ->
                                val color = when (idx) {
                                    0 -> KorvixPrimary
                                    1 -> KorvixSecondary
                                    2 -> KorvixOrange
                                    3 -> KorvixBlue
                                    else -> KorvixRed
                                }
                                DonutSlice(item.name, item.count.toFloat(), color)
                            }
                        } else defaultSlices

                        KorvixDonutChart(
                            slices = finalSlices,
                            centerLabel = "Roles Map",
                            centerValue = "${finalSlices.sumOf { it.value.toDouble() }.toInt()}",
                            selectedSliceLabel = selectedRoleFilter,
                            onSliceClick = { selectedRoleFilter = it }
                        )
                    }
                }
            },
            rightContent = {
                // Right Chart: User Activity Monthly Trend
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
                    border = BorderStroke(1.dp, KorvixBorderAccent)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "User Activity Trend (15 Days)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = KorvixTextDark,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val defaultTrend = listOf(12f, 25f, 18f, 45f, 60f, 32f, 55f, 75f)
                        val defaultLabels = listOf("Wk1", "Wk2", "Wk3", "Wk4")

                        val finalTrend = customData?.dateTrend?.map { it.count.toFloat() } ?: defaultTrend
                        val finalLabels = customData?.dateTrend?.map { it.date.take(6) } ?: defaultLabels

                        KorvixLineChart(
                            dataPoints = finalTrend,
                            labels = finalLabels,
                            modifier = Modifier.fillMaxWidth().height(150.dp)
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Audit Logs / System Activity Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
            border = BorderStroke(1.dp, KorvixBorderAccent)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val filteredLogs = if (selectedRoleFilter == null) dbAuditLogs else {
                    dbAuditLogs.filter { log ->
                        when (selectedRoleFilter) {
                            "Employees" -> log.userRole == "EMPLOYEE"
                            "HR" -> log.userRole == "HR"
                            "Secretaries" -> log.userRole == "SECRETARY"
                            "Admins" -> log.userRole == "ADMIN"
                            "Managers" -> false
                            else -> {
                                val lowerLabel = selectedRoleFilter!!.lowercase()
                                log.userEmail.lowercase().contains(lowerLabel) ||
                                        log.details.lowercase().contains(lowerLabel) ||
                                        log.actionType.lowercase().contains(lowerLabel)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent System Activity & Audit Log",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixTextDark
                    )
                    IconButton(
                        onClick = {
                            val headers = listOf("User Node", "Role", "Action Type", "Details", "Status", "Timestamp")
                            val rows = filteredLogs.map { log ->
                                listOf(
                                    log.userEmail,
                                    log.userRole,
                                    log.actionType,
                                    log.details,
                                    log.status,
                                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp))
                                )
                            }
                            onExportTable(UserRole.ADMIN, "System Audit Log", headers, rows)
                        },
                        modifier = Modifier.size(24.dp).testTag("export_admin_csv")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export CSV",
                            tint = KorvixPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Headers
                Row(
                    modifier = Modifier.fillMaxWidth().background(KorvixPrimaryLight, RoundedCornerShape(6.dp)).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("User Node", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KorvixPrimary, modifier = Modifier.weight(1.5f))
                    Text("Activity Event", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KorvixPrimary, modifier = Modifier.weight(2f))
                    Text("Status", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KorvixPrimary, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No activity logs match filter \"$selectedRoleFilter\"", fontSize = 12.sp, color = KorvixTextMuted, fontWeight = FontWeight.Medium)
                    }
                } else {
                    filteredLogs.forEach { log ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text(log.userEmail, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KorvixTextDark)
                                Text("Role: ${log.userRole}", fontSize = 9.sp, color = KorvixTextMuted)
                            }
                            Text("[${log.actionType}] ${log.details}", fontSize = 11.sp, color = KorvixTextMuted, modifier = Modifier.weight(2f))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (log.status == "SUCCESS") KorvixGreenLight else KorvixRedLight)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    log.status,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (log.status == "SUCCESS") KorvixGreen else KorvixRed
                                )
                            }
                        }
                        Divider(color = KorvixBorder)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Business Unit Financial & Risk Analytics
        BusinessUnitDataTableView(onExportTable = onExportTable, currentUserRole = UserRole.ADMIN)

        Spacer(modifier = Modifier.height(24.dp))

        // --- KORVIX RBAC Database Inspector ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
            border = BorderStroke(1.dp, KorvixBorderAccent)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "RBAC Security",
                            tint = KorvixPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "KORVIX RBAC Database Inspector",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = KorvixTextDark
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(KorvixPrimaryLight)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ROOM SQLITE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = KorvixPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!hasManageSystemPermission) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Locked", tint = KorvixRed, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "KORVIX RBAC Inspector locked. MANAGE_SYSTEM permission required.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KorvixTextMuted
                        )
                    }
                } else {
                    Text(
                        text = "Role-Based Access Control configuration fetched reactively from Room / SQLite via Flows:",
                        fontSize = 12.sp,
                        color = KorvixTextMuted,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Subgrid: Left column Users, Right column Roles & Permissions
                    ResponsiveGrid(
                        colsMobile = 1,
                        colsDesktop = 2,
                        spacing = 16.dp
                    ) {
                    // Column 1: Registered DB Users
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, KorvixBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🔑 Registered Users (${dbUsers.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = KorvixTextDark,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            dbUsers.forEach { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(user.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = KorvixTextDark)
                                        Text(user.email, fontSize = 10.sp, color = KorvixTextMuted)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when (user.roleId) {
                                                    "ADMIN" -> KorvixRedLight
                                                    "HR" -> KorvixOrangeLight
                                                    "SECRETARY" -> KorvixBlueLight
                                                    else -> KorvixPrimaryLight
                                                }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = user.roleId,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (user.roleId) {
                                                "ADMIN" -> KorvixRed
                                                "HR" -> KorvixOrange
                                                "SECRETARY" -> KorvixBlue
                                                else -> KorvixPrimary
                                            }
                                        )
                                    }
                                }
                                HorizontalDivider(color = KorvixBorder.copy(alpha = 0.5f))
                            }
                        }
                    }

                    // Column 2: DB Roles & Assigned Permissions
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, KorvixBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🛡️ Active Roles & Permissions",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = KorvixTextDark,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            dbRolesWithPermissions.forEach { roleWithPerms ->
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = roleWithPerms.role.roleId,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = KorvixPrimary
                                        )
                                        Text(
                                            text = "${roleWithPerms.permissions.size} perms",
                                            fontSize = 10.sp,
                                            color = KorvixTextMuted
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        roleWithPerms.permissions.forEach { perm ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(KorvixBackground)
                                                    .border(BorderStroke(0.5.dp, KorvixBorderAccent), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = perm.permissionId,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = KorvixTextDark
                                                )
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(color = KorvixBorder.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
                } // This closes the 'else' block for hasManageSystemPermission
            }
        }
        } // This closes the 'else' block for hasSystemMetricsPermission
    }
}

@Composable
fun AdminMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    subText: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
        border = BorderStroke(1.dp, KorvixBorderAccent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KorvixTextMuted)
                Icon(icon, contentDescription = null, tint = KorvixPrimary, modifier = Modifier.size(16.dp))
            }
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = KorvixTextDark, modifier = Modifier.padding(vertical = 4.dp))
            Text(subText, fontSize = 10.sp, color = KorvixTextMuted, fontWeight = FontWeight.Medium)
        }
    }
}


// --- PORTAL 2: HR PORTAL VIEW ---
@Composable
fun HRPortalView(
    viewModel: KorvixDashboardViewModel,
    customData: AggregatedData?,
    onExportTable: (UserRole, String, List<String>, List<List<String>>) -> Unit
) {
    var selectedDeptFilter by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val userPermissions by viewModel.currentUserPermissions.collectAsState()
    val hasHrPortalPermission = userPermissions.contains("VIEW_HR_PORTAL")
    val hasLeavePermission = userPermissions.contains("VIEW_LEAVE_LOGS")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Human Resources Performance Ledger",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = KorvixTextDark,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (!hasHrPortalPermission) {
            AccessRestrictedPlaceholder(permissionRequired = "VIEW_HR_PORTAL")
        } else {

        // Metrics Summary Row
        ResponsiveGrid(
            colsMobile = 2,
            colsDesktop = 4,
            spacing = 10.dp
        ) {
            HRMetricItem("Total Employees", "248", Icons.Default.Groups)
            HRMetricItem("Present Today", "186", Icons.Default.FactCheck)
            HRMetricItem("On Leave", "28", Icons.Default.Sick)
            HRMetricItem("Open Positions", "8", Icons.Default.AddBusiness)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Trend and Distributions
        ResponsiveSplitLayout(
            leftFraction = 1.2f,
            rightFraction = 1f,
            spacing = 16.dp,
            leftContent = {
                // Soft purple Area Chart
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
                    border = BorderStroke(1.dp, KorvixBorderAccent)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Employee Attendance Overview (May Trend)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = KorvixTextDark,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val defaultTrend = listOf(140f, 155f, 162f, 186f, 178f, 186f, 190f, 186f)
                        val defaultLabels = listOf("01 May", "05 May", "08 May", "12 May", "15 May", "16 May")

                        val finalTrend = customData?.dateTrend?.map { it.count.toFloat() } ?: defaultTrend
                        val finalLabels = customData?.dateTrend?.map { it.date.take(6) } ?: defaultLabels

                        KorvixLineChart(
                            dataPoints = finalTrend,
                            labels = finalLabels,
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            fillColor = KorvixPrimary.copy(alpha = 0.15f),
                            lineColor = KorvixSecondary
                        )
                    }
                }
            },
            rightContent = {
                // Department Donut Chart
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
                    border = BorderStroke(1.dp, KorvixBorderAccent)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Department Distribution",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = KorvixTextDark,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val defaultDeptSlices = listOf(
                            DonutSlice("Engineering", 110f, KorvixPrimary),
                            DonutSlice("Sales", 65f, KorvixSecondary),
                            DonutSlice("Finance", 42f, KorvixOrange),
                            DonutSlice("HR", 31f, KorvixBlue),
                            DonutSlice("Others", 15f, KorvixRed)
                        )

                        val finalDeptSlices = if (customData != null) {
                            customData.deptDistribution.mapIndexed { idx, item ->
                                val color = when (idx) {
                                    0 -> KorvixPrimary
                                    1 -> KorvixSecondary
                                    2 -> KorvixOrange
                                    3 -> KorvixBlue
                                    else -> KorvixRed
                                }
                                DonutSlice(item.name, item.count.toFloat(), color)
                            }
                        } else defaultDeptSlices

                        KorvixDonutChart(
                            slices = finalDeptSlices,
                            centerLabel = "Depts Map",
                            centerValue = "${finalDeptSlices.sumOf { it.value.toDouble() }.toInt()}",
                            selectedSliceLabel = selectedDeptFilter,
                            onSliceClick = { selectedDeptFilter = it }
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Split Layout: Joiners Table vs Birthday Panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Recent Joiners
            Card(
                modifier = Modifier.weight(1.3f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
                border = BorderStroke(1.dp, KorvixBorderAccent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val joiners = listOf(
                        "Rohan Sen" to "Engineering (L3)",
                        "Priya Mehta" to "Product Strategy",
                        "Sarah Jenkins" to "HR Lead",
                        "Kabir Dua" to "Finance Controller"
                    )

                    val filteredJoiners = if (selectedDeptFilter == null) joiners else {
                        joiners.filter { (name, dept) ->
                            val lowerFilter = selectedDeptFilter!!.lowercase()
                            when {
                                lowerFilter.contains("engineering") -> dept.lowercase().contains("engineering")
                                lowerFilter.contains("finance") -> dept.lowercase().contains("finance")
                                lowerFilter.contains("hr") -> dept.lowercase().contains("hr")
                                lowerFilter.contains("sales") || lowerFilter.contains("marketing") -> dept.lowercase().contains("product") || dept.lowercase().contains("sales") || dept.lowercase().contains("marketing")
                                lowerFilter.contains("others") -> !dept.lowercase().contains("engineering") && !dept.lowercase().contains("finance") && !dept.lowercase().contains("hr")
                                else -> dept.lowercase().contains(lowerFilter)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Corporate Joiners",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = KorvixTextDark
                        )
                        IconButton(
                            onClick = {
                                val headers = listOf("Employee Name", "Department/Designation")
                                val rows = filteredJoiners.map { listOf(it.first, it.second) }
                                onExportTable(UserRole.HR, "Recent Joiners", headers, rows)
                            },
                            modifier = Modifier.size(24.dp).testTag("export_hr_csv")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export CSV",
                                tint = KorvixPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (filteredJoiners.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No recent joiners match filter \"$selectedDeptFilter\"", fontSize = 12.sp, color = KorvixTextMuted, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        filteredJoiners.forEach { (name, dept) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(KorvixPrimaryLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(name.take(1), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = KorvixPrimary)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KorvixTextDark)
                                    Text(dept, fontSize = 9.sp, color = KorvixTextMuted, fontWeight = FontWeight.Medium)
                                }
                            }
                            Divider(color = KorvixBorder)
                        }
                    }
                }
            }

            // Birthdays & Leave summary widgets
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
                border = BorderStroke(1.dp, KorvixBorderAccent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cake, contentDescription = null, tint = KorvixOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Active Birthdays (Today)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KorvixTextDark)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "🎂 Ankit Sharma (Employee Portal) • Celebrating 26th Birthday Today!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixPrimary,
                        modifier = Modifier.background(KorvixPrimaryLight, RoundedCornerShape(4.dp)).padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Leave Approvals Progress", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KorvixTextDark)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (!hasLeavePermission) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().background(KorvixRedLight, RoundedCornerShape(4.dp)).padding(8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = KorvixRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Requires VIEW_LEAVE_LOGS permission", fontSize = 10.sp, color = KorvixRed, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("Medical Leave pool (12 Approved / 15 Total)", fontSize = 10.sp, color = KorvixTextMuted)
                        LinearProgressIndicator(
                            progress = { 12f / 15f },
                            modifier = Modifier.fillMaxWidth().height(4.dp).padding(vertical = 4.dp),
                            color = KorvixOrange,
                            trackColor = KorvixBorder
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Business Unit Financial & Risk Analytics
        BusinessUnitDataTableView(onExportTable = onExportTable, currentUserRole = UserRole.HR)

        Spacer(modifier = Modifier.height(16.dp))

        // File upload section
        PortalFileUploadSection(
            portalType = "HR",
            onUploadComplete = { fileName, fileSize ->
                viewModel.logAuditAction("FILE_UPLOAD", "Uploaded document to HR portal: $fileName ($fileSize)", "SUCCESS")
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Actions ribbon
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = KorvixPrimary),
            border = BorderStroke(1.dp, KorvixPrimaryDark)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Launch Monthly Q2 Talent Appraisal Review",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { Toast.makeText(context, "Monthly Appraisal launched on HR node.", Toast.LENGTH_SHORT).show() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Initialize Audit", color = KorvixPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        } // This closes the 'else' block for hasHrPortalPermission
    }
}

@Composable
fun HRMetricItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
        border = BorderStroke(1.dp, KorvixBorderAccent)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = KorvixPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = KorvixTextDark)
            Text(title, fontSize = 9.sp, color = KorvixTextMuted, fontWeight = FontWeight.Bold)
        }
    }
}


// --- PORTAL 3: SECRETARY PORTAL SCREEN ---
@Composable
fun SecretaryPortalView(
    viewModel: KorvixDashboardViewModel,
    onExportTable: (UserRole, String, List<String>, List<List<String>>) -> Unit
) {
    var selectedDocFilter by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val userPermissions by viewModel.currentUserPermissions.collectAsState()
    val hasSecretaryPortalPermission = userPermissions.contains("MANAGE_SCHEDULE")
    val tasksList = remember {
        mutableStateListOf(
            "Schedule Board Meeting Room 4A" to true,
            "Approve Travel Voucher for VP Engineering" to false,
            "Scan Executive Audit Logs" to true,
            "Re-file Secretary Q2 pending invoices" to false
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Secretary Office Hub & Timelines",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = KorvixTextDark,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (!hasSecretaryPortalPermission) {
            AccessRestrictedPlaceholder(permissionRequired = "MANAGE_SCHEDULE")
        } else {

        // Metrics Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminMetricCard("Today's Appointments", "8 Slots", Icons.Default.BookOnline, "4 completed", modifier = Modifier.weight(1f))
            AdminMetricCard("Upcoming Meetings", "3 Slots", Icons.Default.VideoCall, "Executive boardroom", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminMetricCard("Today's Visitors", "4 Logs", Icons.Default.Badge, "Verified entries", modifier = Modifier.weight(1f))
            AdminMetricCard("Pending Docs", "6 Files", Icons.Default.Task, "Requires signature", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Split Schedule Timeline vs Document Status Donut Chart
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left calendar timeline
            KorvixScheduleTimeline(modifier = Modifier.weight(1.2f))

            // Right Document status chart
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
                border = BorderStroke(1.dp, KorvixBorderAccent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Document Statuses",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixTextDark,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val docSlices = listOf(
                        DonutSlice("Approved", 6f, KorvixGreen),
                        DonutSlice("Pending", 6f, KorvixOrange),
                        DonutSlice("In Review", 3f, KorvixBlue),
                        DonutSlice("Rejected", 1f, KorvixRed)
                    )

                    KorvixDonutChart(
                        slices = docSlices,
                        centerLabel = "Docs Map",
                        centerValue = "16 files",
                        selectedSliceLabel = selectedDocFilter,
                        onSliceClick = { selectedDocFilter = it }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Documents Registry Card with Interactive Filter
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
            border = BorderStroke(1.dp, KorvixBorderAccent)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val docRegistry = listOf(
                    Triple("Q2_Finance_Audit_Reconciled.xlsx", "16 May 2025", "Approved"),
                    Triple("VP_Engineering_Travel_SOP.pdf", "15 May 2025", "Pending"),
                    Triple("TechNova_Partnership_Agreement_v4.docx", "14 May 2025", "In Review"),
                    Triple("Vendor_Office_Lease_Proposal.pdf", "12 May 2025", "Rejected"),
                    Triple("Corporate_Compliance_Certification.pdf", "10 May 2025", "Approved"),
                    Triple("HR_Leave_Policy_Amendment_Draft.docx", "11 May 2025", "Pending")
                )

                val filteredDocs = if (selectedDocFilter == null) docRegistry else {
                    docRegistry.filter { it.third == selectedDocFilter }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Documents Registry",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixTextDark
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedDocFilter != null) {
                            Text(
                                text = "Filter Active: $selectedDocFilter (Clear ✕)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = KorvixPrimary,
                                modifier = Modifier
                                    .clickable { selectedDocFilter = null }
                                    .background(KorvixPrimaryLight, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        IconButton(
                            onClick = {
                                val headers = listOf("Document Name", "Last Updated", "Status")
                                val rows = filteredDocs.map { listOf(it.first, it.second, it.third) }
                                onExportTable(UserRole.SECRETARY, "Documents Registry", headers, rows)
                            },
                            modifier = Modifier.size(24.dp).testTag("export_secretary_csv")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export CSV",
                                tint = KorvixPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Table Headers
                Row(
                    modifier = Modifier.fillMaxWidth().background(KorvixPrimaryLight, RoundedCornerShape(6.dp)).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Document Name", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KorvixPrimary, modifier = Modifier.weight(2f))
                    Text("Last Updated", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KorvixPrimary, modifier = Modifier.weight(1f))
                    Text("Status", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KorvixPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (filteredDocs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No documents match filter \"$selectedDocFilter\"", fontSize = 12.sp, color = KorvixTextMuted, fontWeight = FontWeight.Medium)
                    }
                } else {
                    filteredDocs.forEach { (docName, date, status) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(docName, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = KorvixTextDark, modifier = Modifier.weight(2f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(date, fontSize = 11.sp, color = KorvixTextMuted, modifier = Modifier.weight(1f))
                            
                            val statusColor = when (status) {
                                "Approved" -> KorvixGreen
                                "Pending" -> KorvixOrange
                                "In Review" -> KorvixBlue
                                else -> KorvixRed
                            }
                            val statusBg = when (status) {
                                "Approved" -> KorvixGreenLight
                                "Pending" -> KorvixOrangeLight
                                "In Review" -> KorvixBlueLight
                                else -> KorvixRedLight
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(statusBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    status,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                        }
                        Divider(color = KorvixBorder)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Office Tasks List with checkboxes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Task List Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
                border = BorderStroke(1.dp, KorvixBorderAccent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Dynamic Secretary Task Board",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixTextDark,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    tasksList.forEachIndexed { index, (task, isChecked) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tasksList[index] = task to !isChecked }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { tasksList[index] = task to it },
                                colors = CheckboxDefaults.colors(checkedColor = KorvixPrimary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = task,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isChecked) KorvixTextMuted else KorvixTextDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Right Visitors Log
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
                border = BorderStroke(1.dp, KorvixBorderAccent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Recent Visitors Log",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixTextDark,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val visitors = listOf(
                        "Dr. Alok Sen" to "09:15 AM • Entry Pass #102",
                        "Meera Nair" to "11:30 AM • Entry Pass #103",
                        "John Miller" to "02:10 PM • Entry Pass #104"
                    )

                    visitors.forEach { (name, info) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(KorvixBlueLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name.take(1), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = KorvixBlue)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KorvixTextDark)
                                Text(info, fontSize = 9.sp, color = KorvixTextMuted, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
        } // This closes the 'else' block for hasSecretaryPortalPermission
    }
}


// --- PORTAL 4: EMPLOYEE PORTAL VIEW (ANKIT SHARMA) ---
@Composable
fun EmployeePortalView(
    viewModel: KorvixDashboardViewModel,
    onExportTable: (UserRole, String, List<String>, List<List<String>>) -> Unit
) {
    val context = LocalContext.current
    val userPermissions by viewModel.currentUserPermissions.collectAsState()
    val hasSalaryPermission = userPermissions.contains("VIEW_SALARY_METRICS")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Welcome Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome Back, Ankit Sharma",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = KorvixTextDark
                )
                Text(
                    text = "Software Engineer III • Engineering Portal",
                    fontSize = 11.sp,
                    color = KorvixTextMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .background(KorvixGreenLight, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "STATUS: PRESENT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = KorvixGreen
                )
            }
        }

        // Row Metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HRMetricItem("Present Days", "22 / 23", Icons.Default.HowToReg, modifier = Modifier.weight(1f))
            HRMetricItem("Leave Balance", "12 Days", Icons.Default.Event, modifier = Modifier.weight(1f))
            HRMetricItem("Pending Tasks", "5 Active", Icons.Default.AssignmentLate, modifier = Modifier.weight(1f))
            HRMetricItem("Performance Rating", "4.3 / 5", Icons.Default.Star, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Split: Attendance Grid Calendar vs Performance Leaderboard & Tasks
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Interactive Calendar
            KorvixAttendanceCalendar(modifier = Modifier.weight(1.2f))

            // Leaderboard & Tasks
            Column(modifier = Modifier.weight(1f)) {
                // Leaderboard
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
                    border = BorderStroke(1.dp, KorvixBorderAccent)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Engineering Leaderboard",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = KorvixTextDark
                            )
                            IconButton(
                                onClick = {
                                    val headers = listOf("Rank", "Name", "Score")
                                    val rows = listOf(
                                        listOf("1st", "Rohan Sen", "100% Tasks Done"),
                                        listOf("2nd", "Meera Nair", "98% Tasks Done"),
                                        listOf("3rd", "Ankit Sharma (You)", "95% Tasks Done")
                                    )
                                    onExportTable(UserRole.EMPLOYEE, "Engineering Leaderboard", headers, rows)
                                },
                                modifier = Modifier.size(24.dp).testTag("export_employee_csv")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Export CSV",
                                    tint = KorvixPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        val leaders = listOf(
                            Triple("1st", "Rohan Sen", "100% Tasks Done"),
                            Triple("2nd", "Meera Nair", "98% Tasks Done"),
                            Triple("3rd", "Ankit Sharma (You)", "95% Tasks Done")
                        )

                        leaders.forEach { (rank, name, score) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = rank,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (rank == "3rd") KorvixPrimary else KorvixTextMuted,
                                        modifier = Modifier.width(28.dp)
                                    )
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        fontWeight = if (rank == "3rd") FontWeight.Bold else FontWeight.Medium,
                                        color = KorvixTextDark
                                    )
                                }
                                Text(
                                    text = score,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KorvixPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Priority Board Tasks
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
                    border = BorderStroke(1.dp, KorvixBorderAccent)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Active Priority Tasks",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = KorvixTextDark,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val activeTasks = listOf(
                            "Optimize Android Canvas draw layers" to "HIGH",
                            "Refactor Gemini REST JSON mapper" to "MEDIUM"
                        )

                        activeTasks.forEach { (task, priority) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(task, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = KorvixTextDark, modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .background(if (priority == "HIGH") KorvixRedLight else KorvixOrangeLight, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(priority, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (priority == "HIGH") KorvixRed else KorvixOrange)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Payslip Download Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
            border = BorderStroke(1.dp, KorvixBorderAccent)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Latest Payslip Available (May 2025)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KorvixTextDark)
                    Text(
                        text = if (hasSalaryPermission) "Net Salary: ₹75,860 • Bank Account Confirmed" else "Net Salary: [REDACTED - VIEW_SALARY_METRICS REQUIRED]",
                        fontSize = 11.sp,
                        color = if (hasSalaryPermission) KorvixTextMuted else KorvixRed,
                        fontWeight = FontWeight.Medium
                    )
                }
                Button(
                    onClick = {
                        if (hasSalaryPermission) {
                            Toast.makeText(context, "✅ Payslip PDF (₹75,860) Downloaded to Device Storage.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "🛑 Access Denied: VIEW_SALARY_METRICS permission required.", Toast.LENGTH_LONG).show()
                        }
                    },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (hasSalaryPermission) KorvixPrimary else Color.Gray)
                ) {
                    Icon(
                        imageVector = if (hasSalaryPermission) Icons.Default.Download else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasSalaryPermission) "Download Payslip" else "Locked",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Document Vault & File Upload Section
        PortalFileUploadSection(
            portalType = "Employee",
            onUploadComplete = { fileName, fileSize ->
                viewModel.logAuditAction("FILE_UPLOAD", "Uploaded document to Employee vault: $fileName ($fileSize)", "SUCCESS")
            }
        )
    }
}


// --- RIGHT PERSISTENT SIDE DRAWER: KORVIX AI ASSISTANT ---
@Composable
fun KorvixAIAssistantDrawer(
    viewModel: KorvixDashboardViewModel,
    currentUser: UserProfile,
    onClose: () -> Unit
) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val lazyListState = remember { androidx.compose.foundation.lazy.LazyListState() }

    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            lazyListState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KorvixGlassSurface)
            .border(width = 1.dp, color = KorvixBorder)
    ) {
        // AI Drawer Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KorvixPrimaryLight)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SmartToy, contentDescription = null, tint = KorvixPrimary)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Korvix AI Assistant", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = KorvixTextDark)
                    Text("Role context: ${currentUser.role.name}", fontSize = 10.sp, color = KorvixPrimary, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close AI", tint = KorvixTextDark)
            }
        }

        // File Stream / Mock CSV Drag-Drop Simulation Ribbon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KorvixPrimary.copy(alpha = 0.05f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "High-Performance File Pipeline",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = KorvixTextDark
                )
                Text(
                    text = "Parse 10,000+ CSV records locally",
                    fontSize = 9.sp,
                    color = KorvixTextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
            Button(
                onClick = { viewModel.simulateCSVFileUpload() },
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KorvixPrimary)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Stream CSV", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Chat Message Area
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatHistory) { message ->
                val isUser = message.sender == "user"
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Column(
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = if (isUser) 12.dp else 0.dp,
                                bottomEnd = if (isUser) 0.dp else 12.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    message.isError -> KorvixRedLight
                                    isUser -> KorvixPrimary
                                    else -> KorvixBorder
                                }
                            )
                        ) {
                            Text(
                                text = parseMarkdown(message.content),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    message.isError -> KorvixRed
                                    isUser -> Color.White
                                    else -> KorvixTextDark
                                },
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Text(
                            text = if (isUser) "You" else "Korvix AI",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = KorvixTextMuted,
                            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                        )
                    }
                }
            }

            if (aiLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = KorvixPrimary
                        )
                        Text(
                            "Analyzing portal logs with Gemini...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = KorvixTextMuted
                        )
                    }
                }
            }
        }

        Divider(color = KorvixBorder)

        // Text input field at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask Korvix AI...", fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KorvixPrimary,
                    unfocusedBorderColor = KorvixBorderAccent,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendUserPrompt(textInput)
                        textInput = ""
                    }
                }),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendUserPrompt(textInput)
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(KorvixPrimary, CircleShape)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun ResponsiveGrid(
    colsMobile: Int,
    colsDesktop: Int,
    spacing: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val columns = if (maxWidth >= 600.dp) colsDesktop else colsMobile
        
        Layout(
            content = content,
            modifier = Modifier.fillMaxWidth()
        ) { measurables, constraints ->
            val spacingPx = spacing.roundToPx()
            val totalSpacing = spacingPx * (columns - 1)
            val itemWidth = ((constraints.maxWidth - totalSpacing) / columns).coerceAtLeast(0)
            
            val itemConstraints = constraints.copy(
                minWidth = itemWidth,
                maxWidth = itemWidth,
                minHeight = 0
            )
            
            val placeables = measurables.map { it.measure(itemConstraints) }
            
            val rows = (placeables.size + columns - 1) / columns
            val rowHeights = IntArray(rows) { 0 }
            
            for (i in placeables.indices) {
                val row = i / columns
                rowHeights[row] = maxOf(rowHeights[row], placeables[i].height)
            }
            
            val totalHeight = rowHeights.sum() + (spacingPx * (rows - 1)).coerceAtLeast(0)
            
            layout(constraints.maxWidth, totalHeight) {
                var y = 0
                for (row in 0 until rows) {
                    var x = 0
                    for (col in 0 until columns) {
                        val index = row * columns + col
                        if (index < placeables.size) {
                            placeables[index].placeRelative(x, y)
                        }
                        x += itemWidth + spacingPx
                    }
                    y += rowHeights[row] + spacingPx
                }
            }
        }
    }
}

@Composable
fun ResponsiveSplitLayout(
    leftFraction: Float,
    rightFraction: Float,
    spacing: Dp,
    modifier: Modifier = Modifier,
    leftContent: @Composable () -> Unit,
    rightContent: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val isDesktop = maxWidth >= 600.dp
        if (isDesktop) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                Box(modifier = Modifier.weight(leftFraction)) {
                    leftContent()
                }
                Box(modifier = Modifier.weight(rightFraction)) {
                    rightContent()
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    leftContent()
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    rightContent()
                }
            }
        }
    }
}

@Composable
fun SystemActivityWidget(
    viewModel: KorvixDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val activities by viewModel.dbSystemActivities.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isRunningDiagnostic by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("system_activity_widget"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KorvixGlassSurface),
        border = BorderStroke(1.dp, KorvixBorderAccent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Widget Title & Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(KorvixPrimaryLight, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "System Log",
                            tint = KorvixPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Room Database Activity Engine",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = KorvixTextDark
                        )
                        Text(
                            text = "Summary analytics of file processing & compliance jobs",
                            fontSize = 9.sp,
                            color = KorvixTextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Diagnostic Button
                Button(
                    onClick = {
                        if (!isRunningDiagnostic) {
                            isRunningDiagnostic = true
                            coroutineScope.launch {
                                // 1. Log a pending/running activity
                                viewModel.logSystemActivity(
                                    type = "FILE_PROCESSING",
                                    title = "Diagnostics: Running automated pipeline check...",
                                    status = "RUNNING",
                                    records = 0,
                                    duration = 0L
                                )
                                delay(1200) // simulated processing delay
                                
                                // 2. Log completion activity
                                viewModel.logSystemActivity(
                                    type = "FILE_PROCESSING",
                                    title = "Diagnostics: Auto-parsed timesheet_diagnostic_${(100..999).random()}.csv successfully",
                                    status = "SUCCESS",
                                    records = (3000..8000).random(),
                                    duration = (60..110).random().toLong()
                                )
                                isRunningDiagnostic = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunningDiagnostic) KorvixOrange else KorvixPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp).testTag("trigger_diagnostic_btn")
                ) {
                    if (isRunningDiagnostic) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Processing...", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run Diagnostics", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Analytics Row
            val totalJobs = activities.size
            val successJobs = activities.count { it.status == "SUCCESS" }
            val totalRecords = activities.sumOf { it.recordsProcessed }
            val totalDuration = activities.sumOf { it.durationMs }
            val successRate = if (totalJobs > 0) (successJobs.toFloat() / totalJobs * 100).toInt() else 100

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KorvixBackground.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, KorvixBorderAccent), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Stat 1
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Total Jobs", fontSize = 9.sp, color = KorvixTextMuted, fontWeight = FontWeight.Medium)
                    Text("$totalJobs", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = KorvixPrimary)
                }
                // Vertical divider
                Box(modifier = Modifier.width(1.dp).height(28.dp).background(KorvixBorder).align(Alignment.CenterVertically))
                
                // Stat 2
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.2f)) {
                    Text("Records Parsed", fontSize = 9.sp, color = KorvixTextMuted, fontWeight = FontWeight.Medium)
                    Text(
                        String.format(java.util.Locale.US, "%,d", totalRecords),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixTextDark
                    )
                }
                // Vertical divider
                Box(modifier = Modifier.width(1.dp).height(28.dp).background(KorvixBorder).align(Alignment.CenterVertically))

                // Stat 3
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.1f)) {
                    Text("Avg Speed", fontSize = 9.sp, color = KorvixTextMuted, fontWeight = FontWeight.Medium)
                    Text(
                        if (totalDuration > 0) String.format(java.util.Locale.US, "%.1f r/ms", totalRecords.toFloat() / totalDuration) else "0.0",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = KorvixOrange
                    )
                }
                // Vertical divider
                Box(modifier = Modifier.width(1.dp).height(28.dp).background(KorvixBorder).align(Alignment.CenterVertically))

                // Stat 4
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("Success %", fontSize = 9.sp, color = KorvixTextMuted, fontWeight = FontWeight.Medium)
                    Text("$successRate%", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = KorvixGreen)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Activities List Header
            Text(
                text = "Recent Operations Logs in Room DB",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = KorvixTextDark,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )

            // Activities Table/List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, KorvixBorderAccent), RoundedCornerShape(8.dp))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (activities.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No system activities found in Room database.", fontSize = 11.sp, color = KorvixTextMuted)
                    }
                } else {
                    activities.take(5).forEach { activity ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(KorvixBackground.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(3f)
                            ) {
                                // Type Icon
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            when (activity.activityType) {
                                                "FILE_PROCESSING" -> KorvixPrimaryLight
                                                "SECURITY_ALERT" -> KorvixRedLight
                                                "COMPLIANCE_SYNC" -> KorvixGreenLight
                                                else -> KorvixBlue.copy(alpha = 0.1f)
                                            },
                                            RoundedCornerShape(4.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (activity.activityType) {
                                            "FILE_PROCESSING" -> Icons.Default.UploadFile
                                            "SECURITY_ALERT" -> Icons.Default.Security
                                            "COMPLIANCE_SYNC" -> Icons.Default.Sync
                                            else -> Icons.Default.CheckCircle
                                        },
                                        contentDescription = null,
                                        tint = when (activity.activityType) {
                                            "FILE_PROCESSING" -> KorvixPrimary
                                            "SECURITY_ALERT" -> KorvixRed
                                            "COMPLIANCE_SYNC" -> KorvixGreen
                                            else -> KorvixBlue
                                        },
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = activity.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KorvixTextDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "By ${activity.operatorEmail}",
                                            fontSize = 8.sp,
                                            color = KorvixTextMuted
                                        )
                                        if (activity.recordsProcessed > 0) {
                                            Text(
                                                text = " • ${activity.recordsProcessed} recs",
                                                fontSize = 8.sp,
                                                color = KorvixPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (activity.durationMs > 0) {
                                            Text(
                                                text = " in ${activity.durationMs}ms",
                                                fontSize = 8.sp,
                                                color = KorvixTextMuted
                                            )
                                        }
                                    }
                                }
                            }

                            // Status and Timestamp
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when (activity.status) {
                                                "SUCCESS" -> KorvixGreenLight
                                                "RUNNING" -> KorvixOrange.copy(alpha = 0.1f)
                                                else -> KorvixRedLight
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = activity.status,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = when (activity.status) {
                                            "SUCCESS" -> KorvixGreen
                                            "RUNNING" -> KorvixOrange
                                            else -> KorvixRed
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(activity.timestamp)),
                                    fontSize = 8.sp,
                                    color = KorvixTextMuted,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KorvixAIAssistantBottomSheet(
    viewModel: KorvixDashboardViewModel,
    currentUser: UserProfile,
    onDismissRequest: () -> Unit
) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    var textInput by remember { mutableStateOf("") }
    val lazyListState = remember { androidx.compose.foundation.lazy.LazyListState() }

    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            lazyListState.animateScrollToItem(chatHistory.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = KorvixGlassSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = KorvixBorder) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(bottom = 16.dp)
        ) {
            // Header inside Bottom Sheet
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "AI Icon",
                        tint = KorvixPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Korvix AI Assistant Terminal",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KorvixTextDark
                        )
                        Text(
                            text = "Authenticated Node: ${currentUser.email} (${currentUser.role.name})",
                            fontSize = 10.sp,
                            color = KorvixPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = KorvixTextDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = KorvixBorder, thickness = 1.dp)

            // Chat Message List
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatHistory) { message ->
                    val isUser = message.sender == "user"
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Column(
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isUser) 12.dp else 0.dp,
                                    bottomEnd = if (isUser) 0.dp else 12.dp
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        message.isError -> KorvixRedLight
                                        isUser -> KorvixPrimary
                                        else -> KorvixBorder
                                    }
                                ),
                                border = if (!isUser) BorderStroke(1.dp, KorvixBorderAccent) else null
                            ) {
                                Text(
                                    text = parseMarkdown(message.content),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = when {
                                        message.isError -> KorvixRed
                                        isUser -> Color.White
                                        else -> KorvixTextDark
                                    },
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            Text(
                                text = if (isUser) "You" else "Korvix AI",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = KorvixTextMuted,
                                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                            )
                        }
                    }
                }

                if (aiLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = KorvixPrimary
                            )
                            Text(
                                text = "Analyzing compliance directives...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = KorvixTextMuted
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = KorvixBorder, thickness = 1.dp)

            // Input Row at the bottom of sheet
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Ask Korvix AI...", fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("ai_sheet_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KorvixPrimary,
                        unfocusedBorderColor = KorvixBorderAccent,
                        focusedContainerColor = Color.White.copy(alpha = 0.9f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendUserPrompt(textInput)
                            textInput = ""
                        }
                    }),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendUserPrompt(textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(KorvixPrimary, CircleShape)
                        .testTag("ai_sheet_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Message",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

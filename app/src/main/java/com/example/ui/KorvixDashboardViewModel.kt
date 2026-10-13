package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiService
import com.example.data.*
import com.example.data.database.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.*

enum class UserRole {
    ADMIN, HR, SECRETARY, EMPLOYEE
}

data class UserProfile(
    val email: String,
    val name: String,
    val role: UserRole,
    val department: String = "All"
)

data class ChatMessage(
    val sender: String, // "user" or "korvix_ai"
    val content: String,
    val timestamp: Date = Date(),
    val isError: Boolean = false
)

class KorvixDashboardViewModel(application: Application) : AndroidViewModel(application) {

    // --- Repositories & Database ---
    private val rbacDatabase = RbacDatabase.getDatabase(application)
    private val rbacRepository = RbacRepository(
        rbacDatabase.rbacDao(),
        rbacDatabase.fileCacheDao(),
        rbacDatabase.userSessionDao()
    )
    private val rbacPermissionService = RbacPermissionService(rbacRepository)
    private val geminiRepository: GeminiRepository = GeminiRepositoryImpl()

    // --- Exposed State Flows from Room DB ---
    val dbUsers: StateFlow<List<UserEntity>> = rbacRepository.allUsers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val dbRolesWithPermissions: StateFlow<List<RoleWithPermissions>> = rbacRepository.allRolesWithPermissions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val dbAuditLogs: StateFlow<List<AuditLogEntity>> = rbacRepository.allAuditLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val dbSystemActivities: StateFlow<List<SystemActivityEntity>> = rbacRepository.allSystemActivities
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Room Database Cached Files & User Sessions Flows ---
    val dbCachedFiles: StateFlow<List<CachedFileEntity>> = rbacRepository.allCachedFiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val dbActiveSession: StateFlow<UserSessionEntity?> = rbacRepository.activeSession
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val dbAllSessions: StateFlow<List<UserSessionEntity>> = rbacRepository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Authentication & User Profiles ---
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _currentUserPermissions = MutableStateFlow<List<String>>(emptyList())
    val currentUserPermissions: StateFlow<List<String>> = _currentUserPermissions.asStateFlow()

    private val _currentViewRole = MutableStateFlow(UserRole.ADMIN) // View Portal switcher
    val currentViewRole: StateFlow<UserRole> = _currentViewRole.asStateFlow()

    private var currentSessionId: String? = null

    // --- Search & Global Stats ---
    val searchQuery = MutableStateFlow("")

    // --- File Streaming Parsing Pipeline States ---
    private val _isStreamingData = MutableStateFlow(false)
    val isStreamingData: StateFlow<Boolean> = _isStreamingData.asStateFlow()

    private val _streamingProgress = MutableStateFlow(0f)
    val streamingProgress: StateFlow<Float> = _streamingProgress.asStateFlow()

    private val _customAggregatedData = MutableStateFlow<AggregatedData?>(null)
    val customAggregatedData: StateFlow<AggregatedData?> = _customAggregatedData.asStateFlow()

    private val _cachePerformanceStatus = MutableStateFlow<String?>("Room SQLite Cache: Initialized & Indexed")
    val cachePerformanceStatus: StateFlow<String?> = _cachePerformanceStatus.asStateFlow()

    // --- Toast Notifications flow for parsing/AI anomalies ---
    private val _toastNotification = MutableSharedFlow<String>()
    val toastNotification: SharedFlow<String> = _toastNotification.asSharedFlow()

    // --- AI Assistant Chat State ---
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    init {
        // Seed database if empty
        viewModelScope.launch {
            rbacRepository.seedInitialDataIfEmpty()
        }
        
        // Reactively collect permissions for the currently logged in user
        viewModelScope.launch {
            var permissionsJob: kotlinx.coroutines.Job? = null
            _currentUser.collect { user ->
                permissionsJob?.cancel()
                if (user != null) {
                    permissionsJob = launch {
                        rbacPermissionService.getUserPermissions(user.email).collect { permissions ->
                            _currentUserPermissions.value = permissions
                        }
                    }
                } else {
                    _currentUserPermissions.value = emptyList()
                }
            }
        }
    }

    fun loginUser(email: String, name: String, role: UserRole) {
        val userDept = when (role) {
            UserRole.ADMIN -> "All"
            UserRole.HR -> "Human Resources"
            UserRole.SECRETARY -> "Administration"
            UserRole.EMPLOYEE -> "Engineering"
        }
        val profile = UserProfile(email, name, role, userDept)
        _currentUser.value = profile
        _currentViewRole.value = role // Automatically align portal view to user role
        
        // Create Room UserSessionEntity
        val sessionId = "sess_" + UUID.randomUUID().toString().take(8)
        val sessionToken = "kvx_tk_" + UUID.randomUUID().toString().replace("-", "")
        currentSessionId = sessionId

        val sessionEntity = UserSessionEntity(
            sessionId = sessionId,
            userEmail = email,
            userName = name,
            userRole = role.name,
            department = userDept,
            sessionToken = sessionToken,
            createdAt = System.currentTimeMillis(),
            lastActiveAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 86400000L,
            isActive = true,
            deviceInfo = "KORVIX Workstation Client (Android/Compose)",
            ipAddress = "10.142.0.4",
            activePortal = role.name
        )

        viewModelScope.launch {
            // Deactivate previous active sessions for this user to maintain single active session policy
            rbacRepository.deactivateAllSessionsForUser(email)
            rbacRepository.insertSession(sessionEntity)
        }

        logAuditAction("LOGIN", "User logged in successfully as $name ($role)", "SUCCESS", email, role.name)
        
        // Reset chat history with friendly introduction matching role
        val initialGreeting = when (role) {
            UserRole.ADMIN -> "Welcome back, Administrator **$name**. Root node authorization confirmed. You have unrestricted access to all HR logs, audit systems, Room cached files, and Gemini-3.5 strategic parsing capabilities. How can KORVIX optimize your workload today?"
            UserRole.HR -> "Hello **$name** from HR. Leave logs, team distributions, and present lists are updated. Ask me to draft communications or analyze Q2 joiner rates."
            UserRole.SECRETARY -> "Good day, Office Secretary **$name**. Board room schedule and document pending statuses are synchronized. I can help sort document priorities."
            UserRole.EMPLOYEE -> "Hi **$name**, welcome to your personalized Korvix Workspace. Your attendance card and payslip details are ready. Let's make today productive!"
        }
        _chatHistory.value = listOf(ChatMessage("korvix_ai", initialGreeting))
    }

    fun logout() {
        val user = _currentUser.value
        if (user != null) {
            logAuditAction("LOGOUT", "User logged out: ${user.name}", "SUCCESS", user.email, user.role.name)
            currentSessionId?.let { sId ->
                viewModelScope.launch {
                    rbacRepository.deactivateSession(sId)
                }
            }
        }
        currentSessionId = null
        _currentUser.value = null
    }

    fun switchPortalView(role: UserRole) {
        val user = _currentUser.value ?: return
        
        // Strict RBAC View Permissions Enforced
        val isAuthorized = when (user.role) {
            UserRole.ADMIN -> true
            UserRole.HR -> role == UserRole.HR || role == UserRole.EMPLOYEE
            UserRole.SECRETARY -> role == UserRole.SECRETARY || role == UserRole.EMPLOYEE
            UserRole.EMPLOYEE -> role == UserRole.EMPLOYEE
        }
        
        if (isAuthorized) {
            _currentViewRole.value = role
            currentSessionId?.let { sId ->
                viewModelScope.launch {
                    rbacRepository.updateSessionPortal(sId, role.name)
                }
            }
            logAuditAction("SWITCH_PORTAL", "Switched view to portal ${role.name}", "SUCCESS", user.email, user.role.name)
        } else {
            logAuditAction("SWITCH_PORTAL", "Blocked unauthorized attempt to switch view to portal ${role.name}", "DENIED", user.email, user.role.name)
        }
    }

    // --- CSV High-Performance Stream Parsing with Room DB Caching Engine ---
    fun simulateCSVFileUpload(forceRefresh: Boolean = false) {
        logAuditAction("FILE_UPLOAD", "User uploaded data file for parsing (10,000 corporate records)", "SUCCESS")
        viewModelScope.launch {
            _isStreamingData.value = true
            _streamingProgress.value = 0f

            val mockBytes = CSVProcessor.generateMockCSVBytes()
            val checksum = CSVProcessor.calculateChecksum(mockBytes)

            // Step 1: Check Room Cache for high-performance access
            val existingCachedFile = if (!forceRefresh) rbacRepository.getFileByChecksum(checksum) else null
            if (existingCachedFile != null && existingCachedFile.summaryJson != null) {
                // --- ROOM CACHE HIT (HIGH-PERFORMANCE SUB-MILLISECOND RETRIEVAL) ---
                val hitStartTime = System.currentTimeMillis()
                val cachedAggregated = CSVProcessor.deserializeAggregatedData(existingCachedFile.summaryJson)
                val hitDuration = System.currentTimeMillis() - hitStartTime
                rbacRepository.updateLastAccessed(existingCachedFile.fileId)

                _customAggregatedData.value = cachedAggregated
                _isStreamingData.value = false
                _streamingProgress.value = 1f
                _cachePerformanceStatus.value = "⚡ SQLite Cache Hit: Retrieved 10,000 records from Room B-Tree in ${hitDuration}ms"

                rbacRepository.insertSystemActivity(
                    SystemActivityEntity(
                        timestamp = System.currentTimeMillis(),
                        activityType = "FILE_PROCESSING",
                        title = "High-speed Room cache hit for ${existingCachedFile.fileName}",
                        status = "SUCCESS",
                        recordsProcessed = cachedAggregated.totalRowsParsed,
                        durationMs = hitDuration,
                        operatorEmail = _currentUser.value?.email ?: "SYSTEM"
                    )
                )

                _toastNotification.emit("⚡ Room Cache Hit: Loaded 10,000 records from SQLite in ${hitDuration}ms!")

                val summaryText = """
                    ⚡ **KORVIX High-Performance File Engine: Instant Cache Hit!**
                    - File: **${existingCachedFile.fileName}**
                    - Storage: **Room SQLite Database (Table: cached_files, cached_records)**
                    - Retrieval Latency: **${hitDuration}ms** (Zero CPU re-parsing overhead)
                    - Integrity Checksum: **${existingCachedFile.checksum.take(16)}...**
                    - Cached Rows: **${cachedAggregated.totalRowsParsed} records**
                """.trimIndent()
                _chatHistory.value = _chatHistory.value + ChatMessage("korvix_ai", summaryText)
                return@launch
            }

            // Step 2: Cache Miss - Stream parse and index into Room database
            val fileId = "file_" + UUID.randomUUID().toString().take(8)
            val parseResult = CSVProcessor.parseAndExtractRecords(
                bytes = mockBytes,
                fileName = "timesheet_q2_enterprise.csv",
                fileId = fileId,
                uploadedBy = _currentUser.value?.email ?: "SYSTEM"
            ) { progress ->
                _streamingProgress.value = progress
            }

            // High-performance batched Room database insertion
            rbacRepository.insertCachedFile(parseResult.cachedFile)
            parseResult.records.chunked(1000).forEach { chunk ->
                rbacRepository.insertCachedRecords(chunk)
            }

            _customAggregatedData.value = parseResult.aggregatedData
            _isStreamingData.value = false
            _cachePerformanceStatus.value = "💾 Room Cache Stored: Parsed & indexed 10,000 records into SQLite (${parseResult.cachedFile.parsingDurationMs}ms)"

            // Add an entry in Room Database system_activities table
            rbacRepository.insertSystemActivity(
                SystemActivityEntity(
                    timestamp = System.currentTimeMillis(),
                    activityType = "FILE_PROCESSING",
                    title = "Parsed & indexed 10,000 records into Room database (${parseResult.cachedFile.fileName})",
                    status = "SUCCESS",
                    recordsProcessed = parseResult.aggregatedData.totalRowsParsed,
                    durationMs = parseResult.cachedFile.parsingDurationMs,
                    operatorEmail = _currentUser.value?.email ?: "SYSTEM"
                )
            )

            // Trigger Toast notifications sequentially for each identified anomaly
            if (parseResult.aggregatedData.anomalies.isNotEmpty()) {
                viewModelScope.launch {
                    parseResult.aggregatedData.anomalies.forEach { anomaly ->
                        _toastNotification.emit(anomaly)
                        delay(2500)
                    }
                }
            }

            // Append completion status message in Chat
            val anomalySection = if (parseResult.aggregatedData.anomalies.isNotEmpty()) {
                "\n\n⚠️ **CRITICAL ANOMALIES & MISSING DATA PATTERNS IDENTIFIED:**\n" + 
                parseResult.aggregatedData.anomalies.joinToString("\n") { "• $it" }
            } else {
                ""
            }

            val formattedSummary = """
                📊 **KORVIX AI assistant has completed stream parsing!**
                
                **Summary Metrics:**
                - Total Rows Streamed: **${parseResult.aggregatedData.totalRowsParsed} records**
                - Room Cache: **Indexed in SQLite (cached_files & cached_records tables)**
                - Parse Duration: **${parseResult.cachedFile.parsingDurationMs}ms**
                - Department Distribution: ${parseResult.aggregatedData.deptDistribution.joinToString { "**${it.name}** (${it.count})" }}
                - Performance Rating: ${parseResult.aggregatedData.performanceMetrics.joinToString { "**${it.rating}** (${it.count})" }}$anomalySection
                
                All records are now persistently cached in the on-device Room SQLite database. Subsequent accesses will yield sub-millisecond retrieval!
            """.trimIndent()
            
            _chatHistory.value = _chatHistory.value + ChatMessage("korvix_ai", formattedSummary)
        }
    }

    fun clearUploadedData() {
        _customAggregatedData.value = null
    }

    fun clearRoomFileCache() {
        viewModelScope.launch {
            rbacRepository.clearAllCachedFiles()
            _customAggregatedData.value = null
            _cachePerformanceStatus.value = "Room SQLite Cache Purged"
            logSystemActivity("FILE_PROCESSING", "Cleared all cached files from Room SQLite database", "SUCCESS", 0, 10L)
            _toastNotification.emit("Room database file cache cleared successfully")
        }
    }

    fun terminateSession(sessionId: String) {
        viewModelScope.launch {
            rbacRepository.deactivateSession(sessionId)
            if (currentSessionId == sessionId) {
                logout()
            }
        }
    }

    // --- AI Assistant Chat Submission with Backend Security Interceptor ---
    fun sendUserPrompt(prompt: String) {
        if (prompt.isBlank()) return

        // Append user prompt to chat history
        val updatedHistory = _chatHistory.value + ChatMessage("user", prompt)
        _chatHistory.value = updatedHistory

        val activeRole = _currentUser.value?.role ?: UserRole.EMPLOYEE

        // Touch active user session in Room
        currentSessionId?.let { sId ->
            viewModelScope.launch {
                rbacRepository.touchSession(sId)
            }
        }

        // --- BACKEND RBAC SECURITY INTERCEPTOR (CRITICAL RULE) ---
        if (activeRole == UserRole.EMPLOYEE) {
            val restrictedKeywords = listOf("hr", "admin", "salary", "payroll", "performance metrics of all", "system logs", "audit")
            val containsRestricted = restrictedKeywords.any { prompt.lowercase(Locale.ROOT).contains(it) }
            
            if (containsRestricted) {
                // IMMEDIATELY BLOCK and show the strict denial card
                logAuditAction("AI_QUERY", "Blocked unauthorized query from Employee: \"$prompt\"", "DENIED")
                _chatHistory.value = _chatHistory.value + ChatMessage(
                    sender = "korvix_ai",
                    content = "🛑 **Access Denied**: You are currently logged in under an **Employee** account. You do not have permission to query HR or System Administrative records.",
                    isError = true
                )
                return
            }
        }

        // Standard response pipeline
        viewModelScope.launch {
            _aiLoading.value = true

            val systemInstruction = """
                You are Korvix AI Assistant, a core executive-level analyst integrated into KORVIX, an enterprise full-stack platform.
                Your response style is crisp, highly professional, corporate, and strategic.
                Use clear markdown bullet points and structured analysis.
                The active user role portal is: $activeRole. Ensure your answer is tailored to this role.
                If they ask about data analysis, refer to their high-performance Room SQLite aggregated metrics of 10,000+ data nodes.
            """.trimIndent()

            val aiAnswer = geminiRepository.getAssistantResponse(prompt, systemInstruction)
            logAuditAction("AI_QUERY", "Successfully processed user AI query: \"$prompt\"", "SUCCESS")
            
            _chatHistory.value = _chatHistory.value + ChatMessage("korvix_ai", aiAnswer)
            _aiLoading.value = false
        }
    }

    fun logAuditAction(actionType: String, details: String, status: String, customEmail: String? = null, customRole: String? = null) {
        val email = customEmail ?: _currentUser.value?.email ?: "anonymous@korvix.com"
        val role = customRole ?: _currentUser.value?.role?.name ?: "ANONYMOUS"
        viewModelScope.launch {
            rbacRepository.insertAuditLog(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis(),
                    userEmail = email,
                    userRole = role,
                    actionType = actionType,
                    details = details,
                    status = status
                )
            )
        }
    }

    fun logSystemActivity(type: String, title: String, status: String, records: Int, duration: Long) {
        val email = _currentUser.value?.email ?: "system@korvix.com"
        viewModelScope.launch {
            rbacRepository.insertSystemActivity(
                SystemActivityEntity(
                    timestamp = System.currentTimeMillis(),
                    activityType = type,
                    title = title,
                    status = status,
                    recordsProcessed = records,
                    durationMs = duration,
                    operatorEmail = email
                )
            )
        }
    }
}

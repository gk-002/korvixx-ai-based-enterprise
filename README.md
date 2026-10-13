# KORVIX Enterprise Dashboard & AI Assistant

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Database-Room%20v4-FFA000?logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Gemini API](https://img.shields.io/badge/AI-Google%20Gemini-8E75C2?logo=google&logoColor=white)](https://ai.google.dev/)

**KORVIX** is a modern, enterprise-grade Android application combining fine-grained Role-Based Access Control (RBAC), high-performance streaming file parsing, offline SQLite local caching via Room, and an intelligent AI Assistant powered by Google Gemini.

---

## 🌟 Key Features

### 1. 🤖 Korvix AI Assistant
- **Floating Action Button & Bottom Sheet UI**: Accessible from any screen via a dedicated FAB that opens an interactive modal bottom sheet.
- **Context-Aware Analytics & Query Engine**: Ask complex queries about parsed files, user permissions, anomaly metrics, or system audits.
- **Powered by Gemini API**: Real-time server-side or direct API querying with quick action prompts and Markdown response rendering.

### 2. 🛡️ Enterprise Role-Based Access Control (RBAC)
- **Granular Permissions & Role Hierarchy**: Pre-configured and customizable roles (`Admin`, `Manager`, `Analyst`, `Auditor`).
- **Dynamic Policy Enforcement**: Real-time permission checks gating actions like file ingestion, log purges, role assignments, and session revocation.
- **Tamper-Evident Audit Logging**: Tracks every authentication attempt, privilege escalation, role modification, and data access.

### 3. ⚡ High-Performance File Parsing Engine
- **Streaming Ingestion**: Fast streaming parser designed for large datasets without blocking the main UI thread.
- **Checksum-Based Deduplication**: Automatic SHA-256 calculation to detect previously processed files and achieve sub-millisecond cache hits.
- **Statistical Aggregation & Anomaly Detection**: Instant calculation of row counts, category breakdowns, numerical averages, and threshold violations.

### 4. 🗄️ Room Database v4 Persistence & Caching
- **`cached_files` (`CachedFileEntity`)**: Stores file metadata, checksum, preview snippets, parse duration, and serialized summary JSON.
- **`cached_records` (`CachedRecordEntity`)**: Stores structured parsed rows indexed by file reference, department, and transaction value.
- **`user_sessions` (`UserSessionEntity`)**: Manages session lifecycles, bearer tokens, client device info, portal state, and automated expiration/deactivation.
- **`audit_logs` & `system_activities`**: Comprehensive system activity tracking and operational telemetry.

### 5. 📱 Responsive Jetpack Compose Material 3 UI
- **Adaptive Layouts**: Seamless transition between Compact (smartphones), Medium (foldables), and Expanded (tablets and desktop) form factors using split-pane navigation.
- **Interactive Dashboard Widgets**: System Activity telemetry, Operations logs, Room Cache Manager, and Live User Session Ledger.

---

## 🏗️ Architecture & Tech Stack

```
KORVIX Architecture
│
├── UI Layer (Jetpack Compose M3)
│   ├── KorvixAppUI.kt             # Main Scaffold, Adaptive Split Layout, Responsive Navigation
│   ├── KorvixDashboardViewModel.kt# MVVM StateFlow Coordinator & Business Logic
│   └── Widgets & Bottom Sheet     # Korvix AI Assistant, SystemActivityWidget, File Manager
│
├── Domain & Data Processing
│   ├── CSVProcessor.kt            # Streaming parser, anomaly detector, batch chunking
│   └── GeminiService.kt           # Google Gemini Generative AI client
│
└── Persistence Layer (Room Database v4)
    ├── RbacDatabase.kt            # Room database instance & migration management
    ├── FileCacheEntities.kt       # Cached files & indexed record DAOs
    ├── UserSessionEntity.kt       # Active session ledger & token manager
    └── RbacRepository.kt          # Unified data repository with reactive Flows
```

- **Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose with Material 3 Design
- **Architecture**: MVVM (Model-View-ViewModel) with unidirectional data flow
- **Concurrency**: Kotlin Coroutines & `StateFlow` / `SharedFlow`
- **Local Persistence**: Android Jetpack Room (SQLite)
- **Unit & UI Testing**: Robolectric with local JVM execution

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** Hedgehog / Ladybug or newer
- **JDK 17** or newer
- **Android SDK**: `minSdk 24`, `targetSdk 36`, `compileSdk 36`

### Cloning & Building Locally

1. **Clone the repository:**
   ```bash
   git clone <your-repo-url>
   cd korvix
   ```

2. **Configure Environment Secrets:**
   Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
   Add your Google Gemini API key if you plan to use AI features directly:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key_here
   ```
   *(In Google AI Studio, keys are configured securely via the AI Studio Secrets panel).*

3. **Build the Application:**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run Robolectric Tests:**
   ```bash
   ./gradlew :app:testDebugUnitTest
   ```

---

## 📤 How to Push to GitHub

You can publish and push this project to GitHub using either of the two methods below:

### Option A: Via Google AI Studio UI (Recommended)
1. Open the **Settings** or project dropdown in the top-right corner of the AI Studio interface.
2. Select **"Push to GitHub"** (or **"Export to GitHub"**).
3. Connect / authorize your GitHub account if prompted.
4. Choose the target repository name and push your code with one click.

---

### Option B: Via Local Git CLI
If you exported the project or are running from the terminal:

1. **Initialize Git in the project root:**
   ```bash
   git init -b main
   ```

2. **Stage all project files:**
   ```bash
   git add .
   ```

3. **Create the initial commit:**
   ```bash
   git commit -m "Initial commit: Korvix Enterprise Dashboard, AI Assistant, and Room Database"
   ```

4. **Link your remote GitHub repository:**
   ```bash
   git remote add origin https://github.com/<your-username>/<your-repo-name>.git
   ```

5. **Push to GitHub:**
   ```bash
   git push -u origin main
   ```

---

## 🧪 Testing

The project includes JVM Robolectric unit tests in `app/src/test/java/com/example/ExampleRobolectricTest.kt` verifying:
- ✅ **Room Database v4 Initializations & Migrations**
- ✅ **File Caching & Checksum Retrieval**: Sub-millisecond B-Tree cache hits
- ✅ **Batch Record Indexing**: Chunked SQLite insertions
- ✅ **User Session Ledger**: Active token tracking and session deactivations
- ✅ **CSV Parsing Engine**: Statistical aggregations and anomaly detection

Run tests via command line:
```bash
gradle :app:testDebugUnitTest
```

---

## 📄 License
This project is licensed under the Apache 2.0 License - see the LICENSE file for details.

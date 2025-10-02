<div align="center">

# 💀 Unkill - Android App Resurrection Framework

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android-arsenal.com/api?level=21)
[![Platform](https://img.shields.io/badge/Platform-Android-blue.svg)](https://developer.android.com)
[![Version](https://img.shields.io/badge/v1.0.0-orange.svg)](https://github.com/Benjamin-Wegener/unkill)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-purple.svg)](https://kotlinlang.org)

> **"Death is not the end - not for your apps"** 🏴‍☠️

*Protect your essential Android applications from being killed by the system through an intelligent, distributed service architecture that creates multiple lightweight instances monitoring each other.*

</div>

---

## 🎯 Overview

**Unkill** is a sophisticated Android application designed to protect user-selected apps from being terminated by aggressive battery optimization, memory management, or task killers. Using a revolutionary **distributed, self-replicating service architecture**, Unkill creates multiple lightweight service instances that monitor each other and automatically restart any terminated processes.

### 🔥 Key Features

- **🛡️ Multi-Layer Protection**: Up to 5 distributed service instances with mutual monitoring
- **💀 Standalone Services**: Services can be installed as separate APK files for enhanced resilience
- **🔄 Auto-Recovery**: Automatic detection and restart of killed services
- **⚡ Lightweight**: Each service instance uses <5MB of memory
- **🔋 Battery Efficient**: Minimal battery impact (<2% additional consumption)
- **📱 Universal Compatibility**: Supports 90%+ of Android devices (API 21+)
- **🔒 Privacy First**: No internet permissions, no data collection, no ads

---

## 🚀 Quick Start

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/Benjamin-Wegener/unkill.git
   cd unkill
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the cloned directory and select it

3. **Build and run**
   ```bash
   # Sync project with Gradle files
   # Build > Make Project

   # Run on device/emulator
   # Run > Run 'app'
   ```

### Basic Usage

1. **Launch Unkill** and grant required permissions
2. **Select Apps** to protect from the app list
3. **Start Services** to begin protection
4. **Install Standalone Services** (optional) for enhanced protection
5. **Monitor** service status in real-time

---

## 🏗️ Architecture

### Service Distribution Strategy

```
Main App (com.example.unkill)
├── 🛠️ UnkillServiceManager (Coordinator)
├── 💀 UnkillService1 (com.example.unkill.service1) ← Standalone APK
├── 💀 UnkillService2 (com.example.unkill.service2) ← Standalone APK
├── 💀 UnkillService3 (com.example.unkill.service3) ← Standalone APK
├── 💀 UnkillService4 (com.example.unkill.service4) ← Standalone APK
└── 💀 UnkillService5 (com.example.unkill.service5) ← Standalone APK
```

### Protection Mechanism

- **Mutual Monitoring**: Each service instance monitors others
- **Cross-Process Communication**: Services communicate across different APK boundaries
- **Automatic Recovery**: Killed services are automatically restarted
- **Load Distribution**: Protection workload distributed across instances

---



---

## 📋 Version Roadmap

### ✅ v1.0.0 - Core Functionality (Q1 2024)
- [x] **Basic app framework** with MIT license
- [x] **User interface** for selecting target apps/activities to protect
- [x] **Service cloning mechanism** (up to 5 instances)
- [x] **Dynamic package naming** (`com.example.unkill.service1`, etc.)
- [x] **Mutual monitoring** between service instances
- [x] **Auto-restart functionality** for killed services
- [x] **Low memory footprint** optimization (<5MB per instance)
- [x] **Android API compatibility** (API 21+ covering 90%+ devices)

### 🚧 v1.1.0 - Enhanced Protection & Reliability (Q2 2024)
- [ ] Improved battery optimization bypass techniques (with user consent)
- [ ] Foreground service implementation for better persistence
- [ ] Reduced CPU/memory usage during idle monitoring
- [ ] User-configurable number of active instances (1–5)
- [ ] Visual status indicators for each service instance
- [ ] One-tap emergency restart for all services

### 🔮 v1.2.0 - Stability & Device-Specific Optimizations (Q3 2024)
- [ ] Special handling for OEMs with aggressive battery savers
- [ ] Graceful fallback if dynamic installation is blocked
- [ ] Permission guidance flow (help users enable required settings)
- [ ] Crash reporting & self-healing logic
- [ ] Dark mode support and UI polish

---

## 🔧 Technical Specifications

### System Requirements
- **Minimum API Level**: 21 (Android 5.0 Lollipop)
- **Target API Level**: Latest stable
- **Memory Footprint**: <5MB per service instance
- **Battery Impact**: Minimal (<2% additional battery consumption)
- **Storage**: <10MB total application size

### Required Permissions
```xml
<!-- Core Functionality -->
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- Battery Optimization Bypass -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<!-- Usage Statistics Access -->
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />

<!-- Standalone Service Installation -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"
    tools:ignore="ProtectedPermissions" />
```

### Compatibility Goals
- ✅ **90%+ Device Coverage**: Supports all modern Android devices
- ✅ **OEM Compatibility**: Handles manufacturer-specific optimizations
- ✅ **Privacy Compliant**: No internet permissions required
- ✅ **Zero Tracking**: No analytics, no data collection, no ads

---

## 📱 Screenshots

<div align="center">

### Main Dashboard
<img src="docs/screenshots/main_dashboard.png" alt="Main Dashboard" width="300"/>

### App Selection
<img src="docs/screenshots/app_selection.png" alt="App Selection" width="300"/>

### Service Status
<img src="docs/screenshots/service_status.png" alt="Service Status" width="300"/>

### Settings
<img src="docs/screenshots/settings.png" alt="Settings" width="300"/>

</div>

> *📝 Note: Screenshots will be updated once the UI implementation is complete*

---

## 🛠️ Development

### Project Structure
```
unkill/
├── app/                         # Main Application Module
│   ├── src/main/
│   │   ├── java/com/example/unkill/
│   │   │   ├── activities/      # UI Activities
│   │   │   ├── adapters/        # RecyclerView Adapters
│   │   │   ├── services/        # Main App Services
│   │   │   ├── models/          # Data Models
│   │   │   ├── utils/           # Utility Classes
│   │   │   └── viewmodels/      # MVVM ViewModels
│   │   ├── res/                 # Resources (layouts, strings, etc.)
│   │   └── AndroidManifest.xml  # Main App Manifest
│   └── build.gradle             # App-level build config
├── service1/ to service5/       # Standalone Service Modules
│   ├── src/main/
│   │   ├── java/com/example/unkill/serviceX/
│   │   │   └── UnkillServiceX.kt # Individual service implementation
│   │   └── AndroidManifest.xml  # Service-specific manifest
│   └── build.gradle             # Service build config
└── build.gradle                 # Root build config
```

### Build Commands
```bash
# Assemble debug APK
./gradlew assembleDebug

# Assemble release APK
./gradlew assembleRelease

# Run tests
./gradlew testDebugUnitTest

# Check dependencies
./gradlew app:dependencies
```

---

## 🤝 Contributing

We welcome contributions from the community! Here's how you can help:

### Ways to Contribute
- 🐛 **Bug Reports**: Found an issue? [Open an issue](https://github.com/Benjamin-Wegener/unkill/issues)
- 💡 **Feature Requests**: Have an idea? [Start a discussion](https://github.com/Benjamin-Wegener/unkill/discussions)
- 🔧 **Code Contributions**: Want to fix a bug or add a feature? [Submit a PR](https://github.com/Benjamin-Wegener/unkill/pulls)

### Development Guidelines
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Add tests if applicable
5. Run the existing tests (`./gradlew test`)
6. Commit your changes (`git commit -m 'Add amazing feature'`)
7. Push to the branch (`git push origin feature/amazing-feature`)
8. Open a Pull Request

### Code Style
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Add documentation for public APIs
- Write tests for new functionality

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2025 Benjamin Wegener

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 🙏 Acknowledgments

- **Android Jetpack** for modern Android development components
- **Material Design 3** for the beautiful UI framework
- **Kotlin Coroutines** for asynchronous programming
- **Android Community** for continuous inspiration and support

---

## 📞 Support

If you need help or have questions:

- 📧 **Email**: [benjamin.wegener@example.com](mailto:benjamin.wegener@example.com)
- 🐛 **Issues**: [GitHub Issues](https://github.com/Benjamin-Wegener/unkill/issues)
- 💬 **Discussions**: [GitHub Discussions](https://github.com/Benjamin-Wegener/unkill/discussions)
- 📖 **Wiki**: [Project Wiki](https://github.com/Benjamin-Wegener/unkill/wiki)

---

<div align="center">

**Made with 💀 and ❤️ for the Android community**

[⭐ Star this repo](https://github.com/Benjamin-Wegener/unkill) | [🐛 Report Bug](https://github.com/Benjamin-Wegener/unkill/issues) | [💡 Request Feature](https://github.com/Benjamin-Wegener/unkill/discussions)

*Protecting apps, one service at a time...* 🛡️

</div>

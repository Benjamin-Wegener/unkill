# unkill
Unkill - Android app resurrection framework that protects your essential apps from being killed by the system.

# Unkill - Android App Resurrection Framework

## Overview
Unkill is an Android application designed to protect user-selected apps/activities from being killed by the system through a distributed, self-replicating service architecture. The app creates multiple lightweight service instances that monitor each other and automatically restart any terminated instances.

## Version Roadmap

### v1.0.0 - Core Functionality (Q1 2024)
- [ ] Basic app framework with MIT license
- [ ] User interface for selecting target apps/activities to protect
- [ ] Service cloning mechanism (up to 5 instances)
- [ ] Dynamic package naming (`com.example.unkillService1`, `com.example.unkillService2`, etc.)
- [ ] Mutual monitoring between service instances
- [ ] Auto-restart functionality for killed services
- [ ] Low memory footprint optimization (<5MB per instance)
- [ ] Android API compatibility (API 21+ covering 90%+ devices)

### v1.1.0 - Enhanced Protection & Reliability (Q2 2024)
- [ ] Improved battery optimization bypass techniques (with user consent)
- [ ] Foreground service implementation for better persistence
- [ ] Reduced CPU/memory usage during idle monitoring
- [ ] User-configurable number of active instances (1–5)
- [ ] Visual status indicators for each service instance
- [ ] One-tap emergency restart for all services

### v1.2.0 - Stability & Device-Specific Optimizations (Q3 2024)
- [ ] Special handling for OEMs with aggressive battery savers (Samsung, Xiaomi, Huawei, Oppo, etc.)
- [ ] Graceful fallback if dynamic installation is blocked
- [ ] Permission guidance flow (help users enable required settings)
- [ ] Crash reporting & self-healing logic
- [ ] Dark mode support and UI polish

## Technical Requirements
- **Minimum API Level**: 21 (Android 5.0 Lollipop)
- **Target API Level**: Latest stable
- **Memory Footprint**: <5MB per service instance
- **Battery Impact**: Minimal (<2% additional battery consumption)
- **Permissions**: 
  - `QUERY_ALL_PACKAGES` (for app selection)
  - `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
  - `FOREGROUND_SERVICE`
  - `WAKE_LOCK`
  - `REQUEST_INSTALL_PACKAGES` (only when creating new service clones)

## Compatibility Goals
- Support **90%+ of active Android devices**
- Handle manufacturer-specific task killers and battery optimizers
- No internet permission — fully offline and privacy-respecting
- No data collection, no analytics, no ads

# Warehouse KMP Test

A Kotlin Multiplatform project built to share product search and detail screen logic across Android and iOS while keeping the UI layer native-friendly and platform-aware.

## Project Overview

This application demonstrates a shared architecture for a warehouse product discovery flow:

- Android app layer for native UI hosting
- shared Kotlin Multiplatform module for business logic, ViewModels, repositories, and Compose UI
- iOS app layer using the generated shared framework through a SwiftUI wrapper
- API integration and data models centralized in the shared module

## Architecture

### Shared Module

The `shared` module contains the cross-platform core of the app:

- `api/` - HTTP client and API configuration
- `repository/` - data access layer for fetching product data
- `viewmodel/` - state holders for search and detail screens
- `ui/` - shared Compose screens and navigation setup
- `data/` - domain models used by the app

This keeps the product-search flow reusable and testable without duplicating logic across platforms.

### Android Layer

The `androidApp` module hosts the Android entry point and wires the shared Compose UI to the Android runtime. It uses platform-specific ViewModel wrappers to provide lifecycle-aware coroutine scopes.

### iOS Layer

The `iosApp` module contains the SwiftUI app entry and embeds the generated shared Kotlin/Native framework. The shared Kotlin UI is exposed through a `UIViewController` bridge, allowing the same Compose-based logic to run on iOS simulator/device.

## Tech Stack

- Kotlin Multiplatform
- Jetpack Compose
- Kotlin Coroutines
- Ktor for networking
- Kotlinx Serialization
- Kamel for image loading
- SwiftUI + Xcode project generation for iOS

## Getting Started

### Android

```bash
./gradlew :androidApp:assembleDebug
```

### iOS Simulator

The project is configured to generate the Xcode project and build the shared framework before running the app in the simulator.

## Notes

The design emphasizes separation of concerns:

- shared business logic stays in Kotlin
- platform concerns remain at the Android/iOS boundary
- UI and API logic are structured for maintainability and future extension

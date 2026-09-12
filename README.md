# Warehouse KMP Test

This project is a Kotlin Multiplatform (KMP) mobile application for browsing Warehouse products. It supports native Android and iOS experiences while sharing the business logic, API integration, ViewModel state, and product-search flow in a single Kotlin codebase.

## Overview

The app implements a product search flow with pagination, product detail viewing, and clean state handling using MVVM architecture. The same core logic is shared across Android and iOS, while the platform-specific layers handle native runtime integration and UI hosting.

## Features

- Product search by keyword
- Paginated product list loading
- Product details screen with image, description, price, and special offers
- Loading, empty, and error state handling
- Shared, testable data/repository layer
- Unit tests covering repository and ViewModel logic

## Architecture

### MVVM + Repository Pattern

The application follows a standard MVVM structure with a clear separation between UI, state, and data access:

- `androidApp/` - Android native entry point and platform integration
- `iosApp/` - iOS native app entry and runtime bridge
- `shared/` - shared KMP codebase
  - `api/` - API transport layer, HTTP client, request/response handling
  - `repository/` - data orchestration and domain mapping
  - `viewmodel/` - ViewModel state and screen logic
  - `ui/` - Compose-based shared UI screens
  - `data/` - domain models and DTO conversion logic

This design keeps the app maintainable and testable while allowing the same business rules to run on both Android and iOS.

## Paginated Search

The search screen loads product data in pages using the `Start` and `Limit` query parameters. Pagination is triggered as the user reaches the end of the current list, and the app automatically stops when the backend indicates there are no further results.

Important behaviors:

- each new search resets page state
- pagination continues only when more results remain
- end-of-list detection is based on whether the server response indicates additional items

## Testing

The project includes unit tests for the shared logic, especially for:

- search ViewModel behavior
- pagination behavior
- empty/error states
- repository mapping from API responses to domain models
- product detail flow

The shared tests are executed under the KMP `commonTest` and Android unit test targets, and the project validates successfully under the configured Gradle setup.

## Tech Stack

- Kotlin Multiplatform
- Jetpack Compose
- Kotlin Coroutines
- Ktor
- Kotlinx Serialization
- MVVM architecture
- Android lifecycle + Compose integration
- iOS framework generation through Kotlin/Native

## Local Setup

### Prerequisites

- JDK 17
- Android Studio
- Xcode + iOS simulator runtime (for iOS development on macOS)

### Android build

```bash
export JAVA_HOME=/Users/your-user/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home
./gradlew :androidApp:assembleDebug
```

### iOS simulator

To run the app in the iOS simulator, make sure the following are installed on macOS:

- Xcode
- Xcode Command Line Tools
- iOS Simulator runtime
- the required Kotlin Multiplatform / Compose tooling for iOS builds

Note: the project has already been tested on both Android and iOS. For iOS simulator execution, the environment must have the Apple developer tooling installed and properly configured. In many cases, this means installing Xcode and the iOS simulator runtime as a required local prerequisite.

### Local Gradle JVM setting

If your system uses a different Java installation, set it locally in `gradle.properties` or as `JAVA_HOME` rather than committing it to source control.

Example:

```properties
org.gradle.java.home=/Users/your-user/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home
```

## Project Status

This submission is implemented as a Kotlin Multiplatform app with Android and iOS support, MVVM architecture, paginated search, and unit-test coverage.

## Notes

The goal of the implementation is to keep the business logic stable and reusable across platforms while keeping Android/iOS-specific concerns isolated in their respective entry modules.

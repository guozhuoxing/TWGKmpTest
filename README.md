# Warehouse KMP Test

This project is a Kotlin Multiplatform (KMP) mobile application for browsing Warehouse products. It supports native Android and iOS experiences while sharing the business logic, API integration, ViewModel state, and product-search flow in a single Kotlin codebase.

## Overview

The app implements a product search flow with pagination, product detail viewing, and clean state handling using MVVM architecture. The same core logic is shared across Android and iOS, while the platform-specific layers handle native runtime integration and UI hosting.

## Project architecture

- Shared KMP layer for business logic and data handling
- Repository layer for orchestration and API responses
- ViewModel layer for state management and UI updates
- Compose UI layer for Android/iOS presentation
- MVVM-based structure to keep the app maintainable and testable

## Implemented features

- Product search with result list display
- Product detail view showing product image, price, description, and available specials
- Pagination support for loading more search results as the user scrolls
- Reset and refresh behavior for each new search
- Rich text and plain text description handling
- Loading and error states for asynchronous API calls
- Placeholder fallback for broken or unavailable images

## Description handling

The backend product description can be returned as either HTML-rich content or plain text. The app detects the format automatically and renders the content in a user-friendly way:

- HTML content: formatted for readability with bold, line breaks, and list styling
- Plain text: displayed directly without unnecessary formatting

## Testing

I added unit tests covering:

- search behavior
- pagination logic
- empty/error states
- repository mapping

## Known issues

- The backend does not guarantee product ID de-duplication across loaded pages
- Some product image URLs returned by the API are invalid or unavailable
- Product descriptions are provided as HTML in some cases, so they need formatting before display


## iOS simulator

The app has been tested on both Android and iOS. To run it in the iOS simulator, the local machine must have the required Apple tooling installed, including Xcode and the iOS simulator runtime. If the simulator does not start correctly, install the required Xcode/iOS components and simulator support before running the app.



## Project status

This submission is implemented as a Kotlin Multiplatform app with Android and iOS support, MVVM architecture, paginated search, and unit-test coverage.

## Notes

The implementation keeps the shared business logic reusable across platforms while isolating platform-specific runtime concerns in their respective app modules.

# DocVault 📄

DocVault is a modern Android document scanner and management application built with Kotlin and modern Android development practices.

The application provides an on-device workflow for capturing documents, extracting information using Google ML Kit, processing barcodes, and securely managing document files.

---

## ✨ Features

- 📷 Document Scanning
  - Capture documents directly using the device camera.
  - Handle camera permissions through Android's runtime permission system.

- 🔍 OCR & Text Recognition
  - Extract text from captured documents using Google ML Kit OCR.
  - Process document information directly on the device.

- 📊 Barcode Scanning
  - Detect and process barcodes using Google ML Kit.
  - Supports barcode-based document information extraction.

- 📁 Document Management
  - Store and manage captured documents locally.
  - Handle document URIs and application-owned files securely.

- 🔐 Secure File Handling
  - Uses Android FileProvider for secure file sharing.
  - Uses URI-based access rather than exposing direct filesystem paths.

- 💾 Backup Support
  - Includes application-level backup support for stored app data.

---

## 🛠️ Tech Stack

| Category | Technologies |
|----------|-------------|
| Language | Kotlin |
| Platform | Android |
| UI | Modern Android UI |
| Architecture | Modern Android Architecture |
| Machine Learning | Google ML Kit |
| OCR | ML Kit Text Recognition |
| Barcode | ML Kit Barcode Scanning |
| File Handling | FileProvider, Android URI |
| Build | Gradle |
| IDE | Android Studio |

---

## 🏗️ Architecture & Development

DocVault follows modern Android development principles with an emphasis on maintainability, lifecycle awareness, and secure resource handling.

Key implementation areas include:

- Separation of application responsibilities
- Lifecycle-aware Android components
- Runtime permission handling
- Secure URI and file management
- On-device ML processing
- Camera integration
- Local document storage
- Application backup support

---

## 🔍 Core Functionality

### Document Capture

DocVault integrates the Android camera functionality to allow users to capture documents directly from the application.

### OCR Processing

Captured documents can be processed using Google ML Kit Text Recognition to extract text and document information without relying on a separate external OCR service.

### Barcode Processing

Google ML Kit's barcode processing capabilities are used to detect and process supported barcodes from captured documents.

### Secure File Management

The application uses Android's FileProvider and URI management mechanisms to safely handle files and share them between application components without exposing raw filesystem paths.

---

## 📱 Screenshots

### Home Screen

_Add screenshot here_

### Document Scanner

_Add screenshot here_

### Document Management

_Add screenshot here_

### OCR Processing

_Add screenshot here_

---

## 🚀 Getting Started

### Prerequisites

- Android Studio
- Android SDK
- Kotlin
- Gradle
- Android device or emulator
- Camera-enabled device for scanning functionality

### Clone the Repository

`bash
git clone <your-repository-url>
cd DocVault

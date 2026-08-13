# PhotoCard Wiki & How-To Guide

Welcome to the **PhotoCard** detailed wiki and developer guide. This document serves as the central reference point for developers, designers, and contributors working on the PhotoCard application.

---

## 1. Project Overview & Architecture

### Remaster Background
PhotoCard is a remastered and customized version of the RSS/news feed manager formerly known as "Read You". The application has been redesigned to focus heavily on feed consumption coupled with a rich, interactive visual card customization suite.

### Key Packaging Details
- **Package Name (applicationId):** `photo.card.abdullah`
- **Codebase Namespace:** `me.ash.reader`
  *Note: The codebase retains its original namespace `me.ash.reader` to prevent massive import refactoring overhead while maintaining a distinct applicationId for Play Store and distribution purposes.*
- **Version Name Scheme:** Dynamically generated at build time using the UTC date-time format: `yyyy.MM.dd.HH.mm` inside `app/build.gradle.kts`.

---

## 2. Core Features & Customizations

### 2.1 Visual Photocard Design Suite
PhotoCard features an advanced visual design editor built directly into the Settings screen.

- **Accessing the Suite:**
  - Navigate to the **Settings** page.
  - Locate the **pencil icon** in the top app bar.
  - The page is **password-protected** to secure design privileges.
  - **Password:** `@Abs21221150057`
- **Capabilities:**
  - Support for multiple font file loads.
  - Custom font assignment selectors.
  - Live canvas preview for real-time visual feedback.
  - Customizable ZIP file packaging and naming.
  - Directly exports finalized packages to the local `/download` folder.

### 2.2 Swipe-Up / Bottom Sheet Menu
When a user clicks on an article or news feed item, a 2/3-height Swipe-up Bottom Sheet is displayed:
- It instantly fetches the source HTML of the target article.
- Extracts standard open-graph metadata (specifically `og:image` and `og:title`).
- Construct and display a highly customized Photocard on-the-fly using matching template rules or fallbacks.

### 2.3 Sources & Design Packages
- **Source ZIPs Location:** Photocard design packages are placed inside the `/sources` directory of the repository.
- **Package Structure:** A valid ZIP package contains:
  - `card.json` (layout properties, structure configuration)
  - `replace.json` (mapping variables for substitution)
  - Background image files
  - Necessary custom font files (e.g., TTF/OTF)
- **Local Retrieval:** Users can download and extract these packages locally via designated **secret codes**.

### 2.4 Widget System
To streamline the user experience, the home screen widget system was simplified:
- **ArticleListWidget:** Retained and fully functional, displaying a list view of feed items.
- **ArticleCardWidget & ArticleCardWidgetReceiver:** The single-card widget implementation has been completely removed from the project to avoid redundancy.

### 2.5 Battery Optimization Exemption
To ensure uninterrupted background operations (such as feed syncing and notifications), the app prompts the user for unrestricted background battery usage upon startup. This requests excluding the app from battery optimizations inside `MainActivity.kt`.

---

## 3. Developer & Build Commands

### 3.1 Compiling and Packaging
To build the production-ready GitHub release APK, run:
```bash
./gradlew assembleGithubRelease
```

### 3.2 Running Unit Tests
Unit tests are configured to run efficiently under different variants:
- **Github Release variant:**
  ```bash
  ./gradlew testGithubReleaseUnitTest --max-workers=2
  ```
- **Github Debug variant:**
  ```bash
  ./gradlew testGithubDebugUnitTest
  ```

---

## 4. CI/CD & Automated Release Workflow

The continuous integration workflow is managed via GitHub Actions inside `.github/workflows/auto_release.yml`. On every push to the `main` branch:

1. **Keystore Clean-up:** The workflow deletes any pre-existing keystore to avoid keypair generation conflicts (`rm -f signature/reader.keystore`).
2. **Dynamic Keystore Generation:** Uses `keytool` to dynamically generate a demo keystore.
3. **Signed Release Build:** Compiles the signed release APK using `./gradlew assembleGithubRelease`.
4. **Artifact Committing:** Copies the built APK to `releases/release.apk`.
5. **Git Push Force-add:** Since release directories can be ignored, the pipeline explicitly uses `git add -f releases/release.apk` to force-track the artifact and automatically commits/pushes the update back to the repository.

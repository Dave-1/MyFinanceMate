# Deep Money Tracker

![App Icon](app/src/main/res/drawable/ic_launcher_foreground.png)

Deep Money Tracker is a **privacy‑first, open‑source expense tracking** Android app built with Jetpack Compose. It parses SMS bank alerts, stores data locally with Room, and visualises spending via bar‑ and pie‑charts.

---

## Screenshots

| Feature | Screenshot |
|---------|-------------|
| Splash Screen | ![Splash](media__1778995970027.png) |
| Dashboard | ![Dashboard](media__1778995688325.png) |
| Reports (Bar + Pie Chart) | ![Reports](media__1778995343391.png) |
| Settings (App Lock) | ![Settings](media__1778994031677.png) |

> All screenshots are stored in the repository under the `media__` prefix.

---

## Features

- **Secure app lock** – PIN or biometric authentication on launch and after background.
- **Automatic SMS parsing** for incoming transaction notifications.
- **Beautiful reports** – interactive bar chart for the last 7 days and expense distribution pie chart.
- **Dark / Light themes** with custom gradient splash screen.
- **Fully offline** – no network required; all data stored locally.

---

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/Dave-1/deepMoneyTracker.git
   cd deepMoneyTracker
   ```
2. Open the project in Android Studio (Electric Eel or newer) and let Gradle sync.
3. Run the app on an emulator or device.

---

## Building a Release APK

```bash
# Generate a keystore (once)
keytool -genkeypair -v -keystore release.keystore -alias deepmoneytracker \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass <STORE_PASSWORD> -keypass <KEY_PASSWORD> \
  -dname "CN=DeepMoneyTracker, OU=Dev, O=YourCompany, L=City, S=State, C=IN"

# Build signed release APK
./gradlew clean assembleRelease
```
The signed APK will be located at `app/build/outputs/apk/release/app-release.apk`.

---

## License

This project is licensed under the **MIT License** – see the `LICENSE` file for details.

---

## Contributing

Feel free to open issues or submit pull requests. Please keep contributions under a permissive open‑source license.

# 📞 Minimal Truecaller Popup - Lightweight Caller ID App

A simple and ad-free Android app that displays caller names using the Truecaller API—no need to install the official Truecaller app!


<p align="center">
  <img src="https://github.com/user-attachments/assets/fced56fc-327a-407e-8c08-be6e49521bc7" alt="Incoming Call UI" width="300"/>
</p>

## 🚀 Features

- ✅ Lightweight & fast
- ✅ No ads or background clutter
- ✅ Uses Truecaller API to fetch caller names
- ✅ Popup UI with caller name on incoming unknown calls
- ✅ Start/Stop background service with one tap
- ✅ Manual number search: enter a phone number to get the caller's name and location

## 🛠️ How It Works

1. Launch the app.
2. Tap the **Start** button to activate the background service.
3. When you receive a call from an unknown number, a popup will display the caller’s name.
4. Tap **Stop** anytime to disable the service.

## 📷 Screenshots

| Incoming Call Popup | Service Running |
|---------------------|-----------------|
| <img src="https://github.com/user-attachments/assets/f595c5b7-d175-4009-8ae3-690a9576c1a3" alt="Incoming Call Popup" width="300"/> | <img src="https://github.com/user-attachments/assets/96aa63f9-1c69-4046-b9c5-ddf81569a67c" alt="Service RunningI" width="300"/> |


## 📦 Tech Stack

- Android (Kotlin)
- Foreground Service
- Truecaller API Integration ( Third Party )
- Phone Manager Android Api
- Material Design Components

## 📦 Installation

1. Clone this repository:
   ```bash
   git clone https://github.com/r0ld3x/truecaller-lite.git
   ```
2. Open the project in Android Studio.
3. List your api key inside the [com/r0ld3x/truecaller/ApiInstance.kt](https://github.com/r0ld3x/truecaller-lite/blob/bac0b9e636ffbc16719ff0d1f6da8ad89b5d196a/app/src/main/java/com/r0ld3x/truecaller/ApiInstance.kt#L17) (incase you wanna change) `already added by default`
4. List your api inside the [network_security_config.xml](https://github.com/r0ld3x/truecaller-lite/blob/bac0b9e636ffbc16719ff0d1f6da8ad89b5d196a/app/src/main/res/xml/network_security_config.xml#L5)  (incase you wanna change)
5. Run on an emulator or physical Android device.

## ⚠️ Disclaimer
  This app is not affiliated with or endorsed by Truecaller. It uses public endpoints purely for demonstration or educational purposes.

## 📝 License
  This project is licensed under the MIT License.

## Made with ❤️ by Roldex

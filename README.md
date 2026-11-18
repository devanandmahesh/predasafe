# PredaSafe — Wildlife Intrusion Detection System

> Real-time wildlife monitoring using AI (YOLO), ESP32 sensors, Firebase, and an Android app.

---

##  Overview

**PredaSafe** is an end-to-end IoT + AI system that detects wildlife  near protected boundaries and instantly alerts rangers via an Android app.

The system has three components working together:

```
ESP32 (sensors)
    │  serial (USB)
    ▼
Python Server (YOLO + Firebase bridge)
    │  Firebase Realtime Database
    ▼
Android App (live dashboard + push alerts)
```

---

##  Project Structure

```
predafinal/
├── micropython(esp32)/     # ESP32 MicroPython firmware
│   └── main.py             # PIR + ultrasonic sensor loop
│
├── server/                 # Python backend
│   ├── main.py             # Serial reader → YOLO → Firebase uploader
│   └── serviceAccountKey.json  
│
├── android app/            # Android (Kotlin + Jetpack Compose)
│   ├── app/src/main/
│   │   └── java/com/example/predasafe/
│   │       ├── data/
│   │       │   ├── model/          # Data models
│   │       │   └── repository/     # FirebaseRepository (RTDB listeners)
│   │       ├── ui/
│   │       │   ├── screens/        # HomeScreen, AlertsScreen
│   │       │   ├── viewmodel/      # PredaSafeViewModel
│   │       │   ├── navigation/     # Compose NavHost
│   │       │   └── theme/          # Colors, Typography, Theme
│   │       ├── util/               # NotificationHelper
│   │       └── MainActivity.kt
│   └── google-services.json    
│
├── best.pt                 # 
└── README.md
```

---

##  Tech Stack

| Layer | Technology |
|---|---|
| Hardware | ESP32 + PIR sensor + HC-SR04 ultrasonic |
| Firmware | MicroPython |
| Backend | Python 3.10+, YOLOv8 (Ultralytics), OpenCV, PySerial |
| Cloud | Firebase Realtime Database, ImgBB (image hosting) |
| Mobile | Android (Kotlin), Jetpack Compose, Firebase SDK |

---

## Setup & Installation

### 1. ESP32 Firmware

Flash MicroPython onto your ESP32, then copy `micropython(esp32)/main.py` to the board.

**Pin mapping:**

| Component | Pin |
|---|---|
| PIR sensor | GPIO 5 |
| Ultrasonic TRIG | GPIO 18 |
| Ultrasonic ECHO | GPIO 19 |

The firmware reads motion and distance every second and outputs JSON over serial:
```json
{"motion": 1, "distance": 42.5}
```

---

### 2. Python Server

#### Prerequisites
```bash
pip install ultralytics opencv-python pyserial firebase-admin requests
```

#### Configuration

**Copy the example config and fill in your values:**
```bash
cp server/config.example.py server/config.py
```

Or edit the `CONFIG` section at the top of `server/main.py`:

```python
SERIAL_PORT   = 'COM3'          # Windows: 'COM3' | Linux/Mac: '/dev/ttyUSB0'
BAUD_RATE     = 115200
DATABASE_URL  = 'https://<YOUR-PROJECT>-default-rtdb.firebaseio.com/'
IMGBB_API_KEY = '<YOUR_IMGBB_KEY>'   # https://imgbb.com/
MODEL_PATH    = 'best.pt'            # Path to your YOLO weights
SERVICE_ACCOUNT = 'serviceAccountKey.json'
```

#### Firebase Service Account Key
1. Go to [Firebase Console](https://console.firebase.google.com/) → Project Settings → Service Accounts
2. Click **Generate new private key**
3. Save as `server/serviceAccountKey.json`



#### Run
```bash
python server/main.py
```

---

### 3. Android App

#### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 36 (compileSdk) / minSdk 24

#### Firebase Setup
1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com/)
2. Add an Android app with package name `com.example.predasafe`
3. Download `google-services.json` and place it in **both**:
   - `android app/google-services.json`
   - `android app/app/google-services.json`



#### Firebase Realtime Database Rules (development)
```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```
> Lock these down before production deployment.

#### Build & Run
```bash
cd "android app"
./gradlew assembleDebug
```
Or open the `android app/` folder in Android Studio and click **Run**.

---

##  Firebase Database Schema

```
/
├── status/
│   ├── motion          : boolean
│   ├── distance        : number (cm)
│   ├── last_update     : number (Unix timestamp)
│   └── last_update_readable : string
│
├── detection/
│   ├── animal          : string  ("elephant", "rhino", …)
│   ├── confidence      : number  (0.0 – 1.0)
│   ├── timestamp       : number
│   └── time_readable   : string
│
├── alerts/
│   └── <push-id>/
│       ├── message     : string
│       ├── time        : number
│       ├── time_readable : string
│       └── type        : string  ("motion")
│
├── camera/
│   ├── image_url       : string  (ImgBB URL)
│   ├── last_updated    : number
│   └── last_updated_readable : string
│
└── notification/
    ├── title           : string
    ├── body            : string
    ├── time            : number
    ├── time_readable   : string
    └── trigger         : boolean  (reset to false after app reads it)
```

---

##  YOLO Model

The server uses a custom-trained **YOLOv8** model (`best.pt`) to detect:

| Class ID | Animal |
|---|---|
| 0 | Buffalo |
| 1 | Elephant |
| 2 | Rhino |
| 3 | Zebra |

- **Confidence threshold:** 0.30 (configurable via `CONF_THRESHOLD`)
- **Detection interval:** 3 seconds while motion is active (configurable via `YOLO_INTERVAL`)
- Model weights are **not** committed to this repo due to file size. Place your `best.pt` at `server/best.pt` or update `MODEL_PATH`.

---


---

## 📱 App Features

- **Live Status Card** — motion detected / distance in real-time
- **Camera Feed** — auto-refreshing image uploaded from the server
- **Detection Banner** — shows the latest detected animal + confidence
- **Alerts List** — chronological alert history from Firebase
- **Push Notifications** — local notification triggered by Firebase `notification/trigger` flag

---

##  Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "feat: add your feature"`
4. Push to the branch: `git push origin feature/your-feature`
5. Open a Pull Request

---

## 📄 License

This project is licensed under the **MIT License**. See [LICENSE](LICENSE) for details.

---


# predasafe
# predasafe

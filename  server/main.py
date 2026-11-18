import serial
import json
import time
import os
from dotenv import load_dotenv

# Load environment variables from .env (never committed to git)
load_dotenv()
import cv2
from ultralytics import YOLO

import firebase_admin
from firebase_admin import credentials, db
import requests

# ================= CONFIG (loaded from .env) =================
SERIAL_PORT    = os.getenv("SERIAL_PORT", "COM3")         # e.g. '/dev/ttyUSB0' on Linux
BAUD_RATE      = int(os.getenv("BAUD_RATE", "115200"))

DATABASE_URL   = os.getenv("DATABASE_URL")
IMGBB_API_KEY  = os.getenv("IMGBB_API_KEY")
SERVICE_ACCOUNT = os.getenv("SERVICE_ACCOUNT", "serviceAccountKey.json")
MODEL_PATH     = os.getenv("MODEL_PATH", "best.pt")

# Fail fast if required secrets are missing
if not DATABASE_URL:
    raise EnvironmentError("DATABASE_URL is not set. Check your .env file.")
if not IMGBB_API_KEY:
    raise EnvironmentError("IMGBB_API_KEY is not set. Check your .env file.")

# Throttle YOLO to avoid spamming (seconds)
YOLO_INTERVAL = 3

# YOLO detection config
CONF_THRESHOLD = 0.3
IGNORE_CLASSES = []

# ==========================================


cred = credentials.Certificate(SERVICE_ACCOUNT)
firebase_admin.initialize_app(cred, {
    'databaseURL': DATABASE_URL
})

# Load YOLO model
model = YOLO(MODEL_PATH)
# Custom class mapping (ensure correct labels)
class_names = {
    0: "buffalo",
    1: "elephant",
    2: "rhino",
    3: "zebra"
}

# 📷 Camera
cap = cv2.VideoCapture(0)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

def capture_frame():
    ret, frame = cap.read()
    return frame if ret else None

def upload_to_imgbb(image_path):
    if not IMGBB_API_KEY:
        print("IMGBB API key missing")
        return None
    with open(image_path, "rb") as file:
        response = requests.post(
            "https://api.imgbb.com/1/upload",
            params={"key": IMGBB_API_KEY},
            files={"image": file}
        )
    data = response.json()
    if "data" not in data:
        print(" ImgBB upload failed:", data)
        return None
    return data["data"]["url"]

# 🔌 Serial
try:
    ser = serial.Serial(SERIAL_PORT, BAUD_RATE, timeout=1)
except Exception as e:
    print(f" Failed to open {SERIAL_PORT}: {e}, trying tty...")
    ser = serial.Serial('com3', BAUD_RATE, timeout=1)# Adjust as needed

print("PredaSafe backend started")

last_motion_state = 0
last_yolo_time = 0

while True:
    try:
        line = ser.readline().decode(errors="ignore").strip()
        if not line:
            continue

        print(" Raw:", line)
        data = json.loads(line)

        motion = int(data.get("motion", 0))
        distance = float(data.get("distance", 0))
        # Convert to IST timestamp
        now = int(time.time())
        ist_time = time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(now + 19800))  # +5:30 offset

        # 🔥 Update status (always)
        db.reference('status').set({
            'motion': bool(motion),
            'distance': distance,
            'last_update': now,
            'last_update_readable': ist_time
        })
        print(" Status updated")

        # 🔔 Notification trigger for app
        if motion == 1:
            db.reference('notification').set({
                'title': 'Motion Detected',
                'body': f"Object detected at distance {distance} cm",
                'time': now,
                'time_readable': ist_time,
                'trigger': True
            })

        # 🎯 Run YOLO while motion is present (throttled)
        if motion == 1 and (time.time() - last_yolo_time) >= YOLO_INTERVAL:
            last_yolo_time = time.time()
            print("Motion → capture + YOLO")

            frame = capture_frame()
            if frame is None:
                print("⚠️ Camera read failed")
                continue

            # Save image locally
            filename = f"capture_{now}.jpg"
            cv2.imwrite(filename, frame, [int(cv2.IMWRITE_JPEG_QUALITY), 60])

            # Upload to ImgBB
            image_url = upload_to_imgbb(filename)
            if image_url:
                print(" Uploaded:", image_url)

                db.reference('camera').set({
                    'image_url': image_url,
                    'last_updated': now,
                    'last_updated_readable': ist_time
                })

            # Run YOLO
            results = model(frame, conf=0.25, imgsz=640, verbose=False)

            detected_any = False
            best = None  # (name, conf)

            for r in results:
                for box in r.boxes:
                    cls = int(box.cls[0])
                    name = class_names.get(cls, str(cls))
                    conf = float(box.conf[0])
                    print(f"DEBUG → {name} ({conf:.2f})")
                    # Draw bounding box (optional debug)
                    x1, y1, x2, y2 = map(int, box.xyxy[0])
                    cv2.rectangle(frame, (x1, y1), (x2, y2), (0,255,0), 2)
                    cv2.putText(frame, f"{name} {conf:.2f}", (x1, y1-10),
                                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0,255,0), 2)

                    if conf > CONF_THRESHOLD and name not in IGNORE_CLASSES:
                        detected_any = True
                        if best is None or conf > best[1]:
                            best = (name, conf)

            if detected_any and best is not None:
                name, conf = best
                print(f"🐾 Detected: {name} ({conf:.2f})")

                #  detection node (overwrite latest)
                db.reference('detection').set({
                    'animal': name,
                    'confidence': conf,
                    'timestamp': now,
                    'time_readable': ist_time
                })

                # alerts (append)
                db.reference('alerts').push({
                    'message': f"{name.capitalize()} detected",
                    'time': now,
                    'time_readable': ist_time,
                    'type': 'motion'
                })

                print(f" ALERT pushed: {name}")
            else:
                print(" No allowed animals detected")

        last_motion_state = motion

        # Reset notification trigger when no motion
        if motion == 0:
            db.reference('notification/trigger').set(False)

    except Exception as e:
        print("Error:", e)
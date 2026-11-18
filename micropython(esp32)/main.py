from machine import Pin, time_pulse_us
import time
import ujson

# Pins
pir = Pin(5, Pin.IN)

trigger = Pin(18, Pin.OUT)
echo = Pin(19, Pin.IN)

#  Distance function
def get_distance():
    trigger.off()
    time.sleep_us(2)

    trigger.on()
    time.sleep_us(10)
    trigger.off()

    duration = time_pulse_us(echo, 1, 30000)

    if duration < 0:
        return -1

    distance = (duration / 2) / 29.1
    return distance


print("ESP32 Started...")

while True:
    try:
        motion = pir.value()

        # Only measure distance when motion is detected
        if motion == 1:
            distance = get_distance()
            distance = round(distance, 2) if distance > 0 else 0
        else:
            distance = 0

        data = {
            "motion": motion,
            "distance": distance
        }

        # Send JSON
        print(ujson.dumps(data))

        time.sleep(1)

    except Exception as e:
        print("Error:", e)
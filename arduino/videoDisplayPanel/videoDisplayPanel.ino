#include <OctoWS2811.h>

#define COLS_LEDs 25  // all of the following params need to be adjusted for screen size
#define ROWS_LEDs 32  // LED_LAYOUT assumed 0 if ROWS_LEDs > 8
#define LEDS_PER_STRIP 100


DMAMEM int displayMemory[LEDS_PER_STRIP*6];
int drawingMemory[LEDS_PER_STRIP*6];
uint8_t copyMemory[COLS_LEDs * ROWS_LEDs * 3];
const int config = WS2811_GRB | WS2811_800kHz;
OctoWS2811 leds(LEDS_PER_STRIP, displayMemory, drawingMemory, config);
unsigned long time;

void setup()
{
  // Wait 5 seconds to start
  delay(5000);
  
  pinMode(13, OUTPUT);
  Serial.setTimeout(20);
  leds.begin();
  leds.show();
}

void loop() {
  // put your main code here, to run repeatedly:
  int startChar = Serial.read();

  if (startChar == '*') {
    time = millis();
    uint32_t count = Serial.readBytes((char *)copyMemory, sizeof(copyMemory));
    for(uint32_t i = 0;i < sizeof(copyMemory);i += 3) {
      int32_t color = ((copyMemory[i] << 16) | (copyMemory[i+1] << 8) | copyMemory[i+2]);
      leds.setPixel(i / 3, color);
    }
    if (count == sizeof(copyMemory)) {
      digitalWrite(13, HIGH);
      leds.show();
      digitalWrite(13, LOW);
      Serial.println("Actual Step millis: " + String(millis() - time));
    } else {
      Serial.println("Skip Step millis: " + String(millis() - time));
    }
  } else {
    // Nothing
  }
}

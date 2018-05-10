#define USE_OCTOWS2811
#include <OctoWS2811.h>
#include<FastLED.h>

#define COLS_LEDs 25  // all of the following params need to be adjusted for screen size
#define ROWS_LEDs 25  // LED_LAYOUT assumed 0 if ROWS_LEDs > 8
#define LEDS_PER_STRIP 100
#define NUM_STRIPS 8

CRGB leds[NUM_STRIPS * LEDS_PER_STRIP];

//DMAMEM int displayMemory[LEDS_PER_STRIP*6];
//int drawingMemory[LEDS_PER_STRIP*6];
uint8_t copyMemory[COLS_LEDs * ROWS_LEDs * 3];
//const int config = WS2811_GRB | WS2811_800kHz;
//OctoWS2811 leds(LEDS_PER_STRIP, displayMemory, drawingMemory, config);
unsigned long time;

void setup()
{
  // Wait 5 seconds to start
  delay(5000);

  LEDS.addLeds<OCTOWS2811>(leds, LEDS_PER_STRIP).setCorrection( TypicalSMD5050 );
  LEDS.setBrightness(32);
  
  pinMode(13, OUTPUT);
  Serial.setTimeout(20);
  LEDS.show();
}

void loop() {
  // put your main code here, to run repeatedly:
  int startChar = Serial.read();

  if (startChar == '*') {
    time = millis();
    uint32_t count = Serial.readBytes((char *)copyMemory, sizeof(copyMemory));
    if (count == sizeof(copyMemory)) {
      uint32_t skipped = 0;
      for(uint32_t i = 0;i < sizeof(copyMemory);i += 3) {
        uint32_t iActual = i / 3;
        if (iActual % 75 == 0 && iActual != 0 && iActual != 600 ) {
          skipped += COLS_LEDs;
        }
        leds[iActual + skipped].setRGB(copyMemory[i], copyMemory[i+1], copyMemory[i+2]);
      }
      digitalWrite(13, HIGH);
      LEDS.show();
      digitalWrite(13, LOW);
      Serial.println("Actual Step millis: " + String(millis() - time));
    } else {
      Serial.println("Skip Step millis: " + String(millis() - time));
    }
  } else if (startChar == '#') {
    uint8_t b = Serial.read();
    LEDS.setBrightness(b);
    LEDS.show();
  } else {
    LEDS.show();
  }
}

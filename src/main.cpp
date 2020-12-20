#include <Arduino.h> // yes, i'm using arduino fucking framework for ESP32.
#include <WiFi.h>
#include <ESPAsyncWebServer.h>

const char* wifiSSID = "Napoleon Free";
const char* wifiPassword = "NapoleoN";
AsyncWebServer server(80);
AsyncWebSocket ws("/");

int PIN_B_PWM = 12;
int PIN_B_DIR = 13;
int PIN_A_PWM = 27;
int PIN_A_DIR = 14;

int freq = 500;
int channelB = 0;
int channelA = 1;
int resolution = 8;

int minSpeed = 0;
int maxSpeed = 255;

const char* LEFT = "l";
const char* RIGHT = "r";

void checkPWM(int pwm) {
  if (pwm < minSpeed || pwm > maxSpeed) {
    Serial.println("Unsupported PWM: " + pwm);
  }
}

void rotate(int pwm, int channel, int dirPin, bool isReverse) {
  checkPWM(pwm);

  if (isReverse) {
    digitalWrite(dirPin, HIGH);
  } else {
    digitalWrite(dirPin, LOW);
  }

  int pwmValue = pwm;
  if (isReverse) {
    pwmValue = maxSpeed - pwm;
  }
  ledcWrite(channel, pwmValue);
}

void rotateLeft(int speed, bool isReverse) {
  rotate(speed, channelB, PIN_B_DIR, isReverse);
}

void rotateRight(int speed, bool isReverse) {
  rotate(speed, channelA, PIN_A_DIR, isReverse);
}

void stop() {
  rotateLeft(0, false);
  rotateRight(0, false);
}

void initPins() {
  Serial.println("Initializing pins...");
  pinMode(PIN_A_DIR, OUTPUT);
  pinMode(PIN_A_PWM, OUTPUT);
  digitalWrite(PIN_A_DIR, LOW);
  digitalWrite(PIN_A_PWM, LOW);
  
  pinMode(PIN_B_DIR, OUTPUT);
  pinMode(PIN_B_PWM, OUTPUT);
  digitalWrite(PIN_B_DIR, LOW);
  digitalWrite(PIN_B_PWM, LOW);

  ledcSetup(channelA, freq, resolution);
  ledcSetup(channelB, freq, resolution);

  ledcAttachPin(PIN_A_PWM, channelA);
  ledcAttachPin(PIN_B_PWM, channelB);
}

void initWiFi() {

    // We start by connecting to a WiFi network
    Serial.print("Connecting to ");
    Serial.println(wifiSSID);

    WiFi.begin(wifiSSID, wifiPassword);

    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }

    Serial.println("");
    Serial.println("WiFi connected.");
    Serial.println("IP address: ");
    Serial.println(WiFi.localIP());
}

void handleMessage(String message) {
  Serial.println(message);

  message.toLowerCase();

  int value = message.substring(2, message.length() - 1).toInt();
  bool isReverse = value < 0;

  if (isReverse) {
    value = abs(value);
  }

  String firstSymbol = message.substring(0, 1);
  Serial.println(firstSymbol);
  if (firstSymbol == LEFT) {
    rotateLeft(value, isReverse);
  } else if (firstSymbol == RIGHT) {
    rotateRight(value, isReverse);
  }
}

void onWsEvent(AsyncWebSocket *server, AsyncWebSocketClient *client, AwsEventType type, void *arg, uint8_t *data, size_t len) {
  if (type == WS_EVT_CONNECT) {
    Serial.println("Client connected");
  } else if (type == WS_EVT_DISCONNECT) {
    Serial.println("Client disconnected");
  } else if (type == WS_EVT_DATA) {
    AwsFrameInfo *info = (AwsFrameInfo*)arg;
    if (info->final && info->index == 0 && info->len == len && info->opcode == WS_TEXT) {
      data[len] = 0;
      String message = (char*) data;

      handleMessage(message);
    }
  }
}

void setup() {
  Serial.begin(9600);
  
  initPins();
  initWiFi();

  ws.onEvent(onWsEvent);
  server.addHandler(&ws);

  server.begin();
}

void loop() {}
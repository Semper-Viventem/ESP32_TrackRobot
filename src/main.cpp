#include <main.h>

AsyncWebServer server(SERVER_PORT);
AsyncWebSocket ws("/");

void checkPWM(int pwm) {
  if (pwm < MIN_PWM || pwm > MAX_PWM) {
    Serial.println("Unsupported PWM: " + pwm);
  }
}

void rotate(uint8_t pwm, uint8_t channel, uint8_t dirPin, bool isReverse) {
  digitalWrite(dirPin, isReverse);
  
  uint8_t pwmValue = pwm;
  if (isReverse) {
    pwmValue = MAX_PWM - pwm;
  }
  ledcWrite(channel, pwmValue);
}

void rotateLeft(uint8_t speed, bool isReverse) {
  rotate(speed, CHANNEL_B, PIN_B_DIR, isReverse);
}

void rotateRight(uint8_t speed, bool isReverse) {
  rotate(speed, CHANNEL_A, PIN_A_DIR, isReverse);
}

void stop() {
  rotateLeft(MIN_PWM, false);
  rotateRight(MIN_PWM, false);
}

void handleMessage(String message) {
  Serial.println(message);

  message.toLowerCase();

  int value = message.substring(2, message.length() - 1).toInt();
  bool isReverse = value < 0;

  if (isReverse) {
    value = abs(value);
  }

  checkPWM(value);

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
  Serial.begin(SERIAL_SPEED);
  
  initPins();
  initWiFi();

  ws.onEvent(onWsEvent);
  server.addHandler(&ws);

  server.begin();
}

void loop() { }

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

  ledcSetup(CHANNEL_A, PWM_FREQ, PWM_RESOLUTION);
  ledcSetup(CHANNEL_B, PWM_FREQ, PWM_RESOLUTION);

  ledcAttachPin(PIN_A_PWM, CHANNEL_A);
  ledcAttachPin(PIN_B_PWM, CHANNEL_B);
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

#ifndef _MAIN_H
#define _MAIN_H

#include <Arduino.h> // yes, i'm using arduino fucking framework for ESP32.
#include <WiFi.h>
#include <ESPAsyncWebServer.h>

const char* wifiSSID = "Napoleon Free";
const char* wifiPassword = "NapoleoN";

#define SERIAL_SPEED 9600
#define SERVER_PORT 80

#define PIN_B_PWM 12
#define PIN_B_DIR 13
#define PIN_A_PWM 27
#define PIN_A_DIR 14
#define PIN_LED 15

#define PWM_FREQ 500
#define PWM_RESOLUTION 8
#define CHANNEL_A 1
#define CHANNEL_B 0

#define MIN_PWM 0
#define MAX_PWM 255

const char* LEFT = "l";
const char* BOTH = "b";
const char* RIGHT = "r";

void checkPWM(int pwm);
void rotate(uint8_t pwm, uint8_t channel, uint8_t dirPin, bool isReverse);
void rotateLeft(uint8_t speed, bool isReverse);
void rotateRight(uint8_t speed, bool isReverse);
void stop();
void initPins();
void initWiFi();
void handleMessage(String message);
void onWsEvent(AsyncWebSocket *server, AsyncWebSocketClient *client, AwsEventType type, void *arg, uint8_t *data, size_t len);
void blink();

#endif
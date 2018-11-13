package com.coinzgame.theoxo.coinz

// Message passing flags
const val LOGOUT_FLAG = "LogOutCall"
const val USER_EMAIL = "UserEmail"
const val EXCHANGE_RATES = "ExchangeRates"

// Firebase Firestore flags
const val WALLET_DOCUMENT = "Wallet"
const val BANK_DOCUMENT = "Bank"
const val INBOX_DOCUMENT = "Inbox"
const val COIN_DEPOSITED = "Deposited"
const val GOLD_FIELD_TAG = "GOLD"

// Message class tags
const val MESSAGE_JSON_STRING = "MessageJSONString"
const val MESSAGE_TEXT = "MessageText"
const val MESSAGE_ATTACHMENTS = "MessageAttachments"
const val TIMESTAMP = "Timestamp"
const val SENDER = "SenderEmail"

// Coin class tags
const val CURRENCY = "Currency"
const val VALUE = "Value"
const val ID = "ID"

// Alarm managing tags
const val ALARM_ACTION = "AncientCoinAlarmAction"
const val COINZ_CHANNEL_ID = "CoinzNotificationChannel"
const val COINZ_CHANNEL_NAME = "Coinz Notifications"

// Local saved data file
const val PREFERENCES_FILE = "CoinzPrefsFile"
// Local saved data tags
const val FIRST_TIME_RUNNING = "FirstRun"
const val FIRST_RUN_ACTION = "FirstRunAction"

// Network error tag
const val NETWORK_ERROR = "Unable to load content, check your internet connection"
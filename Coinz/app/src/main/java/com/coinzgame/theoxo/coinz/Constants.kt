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

// Ancient coin tags and spawn chance
const val ANCIENT_SHIL = "Ancient SHIL coin"
const val ANCIENT_QUID = "Ancient QUID coin"
const val ANCIENT_DOLR = "Ancient DOLR coin"
const val ANCIENT_PENY = "Ancient PENY coin"
const val ANCIENT_COIN_SPAWN_CHANCE = 0.03125

// Alarm managing tags
const val ALARM_ACTION = "AncientCoinAlarmAction"
const val COINZ_CHANNEL_ID = "CoinzNotificationChannel"
const val COINZ_CHANNEL_NAME = "Coinz Notifications"
const val OVERWRITE_ALARM_ACTION = "OverwriteAlarmAction"

// Local saved data file
const val PREFERENCES_FILE = "CoinzPrefsFile"
// Local saved data tags
const val FIRST_TIME_RUNNING = "FirstRun"
const val FIRST_RUN_ACTION = "FirstRunAction"
const val LAST_DOWNLOAD_DATE = "LastDownloadDate"
const val SAVED_MAP_JSON = "SavedMapJson"

// Network error tag
const val NETWORK_ERROR = "Unable to load content, check your internet connection"

// Tags relating to the bank and its marker on the map
const val BANK_MARKER_TITLE = "BANK"
const val BANK_MARKER_LATITUDE = 55.945459
const val BANK_MARKER_LONGITUDE = -3.188707

// Longitudes and latitudes which define the valid play area (i.e. the UoE campus)
const val UOE_MIN_LATITUDE = 55.942617
const val UOE_MAX_LATITUDE = 55.946233
const val UOE_MIN_LONGITUDE = -3.192473
const val UOE_MAX_LONGITUDE = -3.18419
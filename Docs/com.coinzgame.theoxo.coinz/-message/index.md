[com.coinzgame.theoxo.coinz](../index.md) / [Message](.)

# Message

`class Message : Any`

A class representing messages sent between users.

### Parameters

`messageJSON` - the JSON describing this message.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `Message(messageJSON: <ERROR CLASS>)`<br>Sets up a message and its properties from the JSON given. |

### Properties

| Name | Summary |
|---|---|
| [attachedCoins](attached-coins.md) | `var attachedCoins: <ERROR CLASS><`[`Coin`](../-coin/index.md)`>?`<br>the coins which are attached to the message. |
| [messageText](message-text.md) | `var messageText: String?`<br>the main text body of the message. |
| [senderEmail](sender-email.md) | `var senderEmail: String?`<br>the email of the user who sent the message. |
| [timestamp](timestamp.md) | `var timestamp: String?`<br>the timestamp of this message. |

### Functions

| Name | Summary |
|---|---|
| [getMessageTag](get-message-tag.md) | `fun getMessageTag(): String`<br>Gets a tag for the message which can be used as its key in the database. |
| [removeCoin](remove-coin.md) | `fun removeCoin(coin: `[`Coin`](../-coin/index.md)`): Boolean?`<br>Removes the requested coin from the message's list of attached coins. |
| [toJSONString](to-j-s-o-n-string.md) | `fun toJSONString(): String`<br>Generates a JSON String representation of the message. |

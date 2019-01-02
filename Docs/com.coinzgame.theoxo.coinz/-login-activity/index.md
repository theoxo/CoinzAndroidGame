[com.coinzgame.theoxo.coinz](../index.md) / [LoginActivity](.)

# LoginActivity

`class LoginActivity : Any`

A login screen that offers login via email/password, authenticating via Firebase.

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `LoginActivity()`<br>A login screen that offers login via email/password, authenticating via Firebase. |

### Functions

| Name | Summary |
|---|---|
| [onCreate](on-create.md) | `fun onCreate(savedInstanceState: <ERROR CLASS>?): Unit`<br>Adds text and click listeners to the screen and sets up the authentication service. |
| [onNewIntent](on-new-intent.md) | `fun onNewIntent(intent: <ERROR CLASS>?): Unit`<br>If the user has requested to be logged out, sign them out of the [FirebaseAuth](#) instance.
Otherwise, pass the intent on to the super function. |
| [onStart](on-start.md) | `fun onStart(): Unit` |

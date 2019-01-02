[com.coinzgame.theoxo.coinz](../index.md) / [InboxFragment](.)

# InboxFragment

`class InboxFragment : Any`

A fragment which shows the user their current inbox and allows them to craft new messages.
Crafting new messages starts a [MessageCreationActivity](../-message-creation-activity/index.md).

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `InboxFragment()`<br>A fragment which shows the user their current inbox and allows them to craft new messages.
Crafting new messages starts a [MessageCreationActivity](../-message-creation-activity/index.md). |

### Functions

| Name | Summary |
|---|---|
| [onAttach](on-attach.md) | `fun onAttach(context: <ERROR CLASS>?): Unit`<br>Saves the [MainActivity](../-main-activity/index.md) which the fragment is being attached to for later reference. |
| [onCreateView](on-create-view.md) | `fun onCreateView(inflater: <ERROR CLASS>, container: <ERROR CLASS>?, savedInstanceState: <ERROR CLASS>?): <ERROR CLASS>?` |
| [onStart](on-start.md) | `fun onStart(): Unit`<br>Updates the inbox list upon the fragment being shown to the user. |
| [onViewCreated](on-view-created.md) | `fun onViewCreated(view: <ERROR CLASS>, savedInstanceState: <ERROR CLASS>?): Unit`<br>Sets up the click events for messages in the inbox and the button to craft a new message. |

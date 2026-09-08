# Communicator 1

Communicator 1 is a JavaFX desktop chat application. It authenticates users against a local SQLite database, relays encrypted chat messages through one Discord text channel, and shows those messages in a JavaFX chat window.

## What runs when the app starts

`Launcher.main` starts JavaFX with `HelloApplication`. `HelloApplication.start` opens **two** independent login windows. This is intentional in the current code: it makes it possible to log in as two users locally for testing.

Each login window uses `login-panel.fxml` and `LoginController`. A successful login opens `chat-panel.fxml` with `HelloController`, then closes that login window.

```text
Launcher
  -> HelloApplication
      -> two login windows
          -> LoginController
              -> LoginService -> UserRepo -> SQLite
              -> UiHelper -> chat window / HelloController
```

## Required configuration

The app reads a file named `secrets` from the current working directory when a chat window starts its Discord bot connection. It is a Java properties file and must contain:

```properties
TOKEN=your-discord-bot-token
```

Keep this file private. It contains an account credential and should not be committed to source control. The channel ID is currently hard-coded in `DiscordBot` rather than configured in this file.

The SQLite connection URL is currently hard-coded as:

```text
jdbc:sqlite:../../DataGripProjects/default/identifier.sqlite
```

When the app is launched from this project directory, that normally resolves to `C:\Users\david\DataGripProjects\default\identifier.sqlite`. The `users` table must already exist; `UserRepo` only attempts to add its `discord_uuid` column. Conversation-related tables are created automatically by `ConversationRepo`.

## Accounts and login

### Registering

Switching the login form to **Register** reveals the email field. When Register is clicked:

1. `LoginController` hashes the supplied password using BCrypt with a work factor of 12.
2. `LoginService.validateFields` checks that all three values are non-empty.
3. `UserRepo.createUser` inserts the username, BCrypt hash, email, and a newly generated random UUID into `users`.

The password is not encrypted and later decrypted; only its BCrypt hash is stored.

### Logging in

1. `LoginService.login` checks that username and password are present.
2. `UserRepo.loginUser` selects the user by username.
3. `BCrypt.checkpw` compares the supplied password with the stored hash.
4. On success, the app opens a chat window for that user.

`UserRepo` also gives older rows that lack a `discord_uuid` a generated UUID when the user is read. The UUID identifies the sender inside encrypted message payloads; it is not an authentication token.

## Database model

| Table | Purpose |
| --- | --- |
| `users` | Existing account table. The code uses `id`, `username`, `password_hash`, `email`, and `discord_uuid`. |
| `conversations` | One direct-message conversation, with an internal numeric ID, a public conversation UUID, a shared encryption key, and creation time. |
| `conversation_members` | Maps a conversation to its two user IDs. Its composite primary key prevents duplicate membership rows. |
| `conversation_pair` | Caches the normalized pair of user IDs (`user_low`, `user_high`) to exactly one conversation ID. |

No database `messages` table is used by the chat feature. Discord is the message relay and history store.

## Opening a conversation

`HelloController` loads every user except the logged-in user. Selecting one runs the database work on a daemon background thread so the JavaFX UI remains responsive.

`ChatService.openConversation` calls `ConversationRepo.findOrCreateConversation`:

1. The two user IDs are sorted, so a conversation from A to B is the same as B to A.
2. A `BEGIN IMMEDIATE` SQLite transaction is started. A JVM-level `CONVERSATION_LOCK` also prevents two local threads from creating the same pair at once.
3. The repository looks up the pair in `conversation_pair`. If needed, it falls back to checking shared rows in `conversation_members`.
4. If no conversation exists, it generates a conversation UUID and a fresh encryption key, inserts the conversation, adds both members, and stores the normalized pair.
5. For older conversation rows with blank crypto fields, it fills in a UUID and key.
6. It closes every statement and result set, then commits the transaction.

When the conversation is ready, the controller requests its most recent Discord history and displays messages belonging to that conversation.

## Message encryption

Every conversation has one shared, randomly generated 32-byte key. `MessageCrypto.generateKey` creates it with `SecureRandom` and Base64URL-encodes it before it is stored in `conversations.encryption_key`.

Before a message is sent, `MessageService.sendEncrypted` creates this UTF-8 plaintext:

```text
<sender UUID>\n<message text>
```

`MessageCrypto.encrypt` then:

1. Generates a new random 12-byte IV (nonce) for this message.
2. Encrypts the plaintext with `AES/GCM/NoPadding`, the conversation key, and a 128-bit authentication tag.
3. Combines `IV || ciphertext-and-tag`.
4. Encodes that combined byte sequence using unpadded Base64URL.

The result is posted to Discord as:

```text
[APP|c=<conversation UUID>] <Base64URL encrypted payload>
```

The conversation UUID is visible in Discord; the sender UUID and message text are inside the encrypted payload.

## Receiving and decrypting messages

`MessageService` is the JDA listener. It ignores messages that are not from the configured relay channel or do not match the `[APP|c=...]` format.

For a matching message it:

1. Extracts the conversation UUID and ciphertext.
2. Dispatches them to every open `HelloController` in this running application.
3. The controller finds the matching conversation and verifies that the current user and selected user are members.
4. `MessageCrypto.decrypt` Base64URL-decodes the payload, reads the first 12 bytes as the IV, and decrypts/verifies the remainder with AES-GCM.
5. The plaintext is split at its first newline into sender UUID and message content.
6. If authentication fails, the key is incorrect, or the payload is malformed, the message is discarded.

When this app sends a message, it dispatches it locally before Discord echoes it back. A concurrent set of message identifiers prevents the later Discord echo from being shown a second time.

## Security properties and limitations

AES-GCM provides confidentiality and tamper detection for message contents while they are in Discord. However, this is **not end-to-end encryption**:

- The conversation key is stored as plaintext in SQLite.
- Any person or process with access to that database can decrypt the conversation’s Discord messages.
- Both participants use the same stored key; there is no per-user key pair, key exchange, or forward secrecy.
- The conversation UUID and encrypted-message timing remain visible in Discord.
- The bot token in `secrets` can access Discord according to the bot’s permissions and must be protected.

The Bouncy Castle dependency is declared in `pom.xml`, but the current encryption implementation uses Java’s built-in `javax.crypto` provider APIs.

## Dependencies and running

The project targets Java 21 and uses Maven. Main dependencies are JavaFX, SQLite JDBC, JDA, jBCrypt, and Bouncy Castle.

To compile and run tests:

```powershell
.\mvnw.cmd test
```

There are currently no test source files, so this command verifies that the project compiles. To run the JavaFX application through Maven:

```powershell
.\mvnw.cmd javafx:run
```

Make sure the `secrets` file, reachable SQLite database, Discord bot token, and configured Discord channel are available before opening a chat window.

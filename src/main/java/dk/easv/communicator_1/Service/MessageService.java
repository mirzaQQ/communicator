package dk.easv.communicator_1.Service;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageService extends DiscordBot {

    public record DecryptedMessage(String senderUuid, String content) {}

    private static final Pattern APP_MESSAGE = Pattern.compile(
            "\\[APP\\|c=([^]]+)]\\s*(.*)",
            Pattern.DOTALL
    );
    private static final Set<String> locallyDispatched = ConcurrentHashMap.newKeySet();

    public static void sendEncrypted(String conversationUuid, String encryptionKey, String senderUuid, String content) {
        String plaintext = senderUuid + "\n" + content;
        String ciphertext = MessageCrypto.encrypt(encryptionKey, plaintext);
        String echoKey = conversationUuid + "|" + ciphertext;
        locallyDispatched.add(echoKey);
        dispatch(conversationUuid, ciphertext);

        if (getChannel() != null) {
            getChannel().sendMessage("[APP|c=" + conversationUuid + "] " + ciphertext).queue();
        }
    }

    public static void loadConversationHistory(String conversationUuid, String encryptionKey,
                                               Consumer<List<DecryptedMessage>> callback) {
        if (getChannel() == null) {
            callback.accept(List.of());
            return;
        }

        getChannel().getHistory().retrievePast(100).queue(messages -> {
            List<Message> chronological = new ArrayList<>(messages);
            Collections.reverse(chronological);

            List<DecryptedMessage> lines = new ArrayList<>();
            for (Message message : chronological) {
                Matcher matcher = APP_MESSAGE.matcher(rawContent(message));
                if (!matcher.matches() || !conversationUuid.equals(matcher.group(1))) {
                    continue;
                }
                DecryptedMessage decrypted = decryptPayload(encryptionKey, matcher.group(2));
                if (decrypted != null) {
                    lines.add(decrypted);
                }
            }
            callback.accept(lines);
        }, error -> callback.accept(List.of()));
    }

    public static DecryptedMessage decryptPayload(String encryptionKey, String ciphertext) {
        try {
            String plaintext = MessageCrypto.decrypt(encryptionKey, ciphertext);
            int split = plaintext.indexOf('\n');
            if (split < 0) {
                return new DecryptedMessage("", plaintext);
            }
            return new DecryptedMessage(plaintext.substring(0, split), plaintext.substring(split + 1));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!isRelayChannel(event.getChannel())) {
            System.out.println("[MessageService] not relay channel, ignoring");
            return;
        }

        Matcher matcher = APP_MESSAGE.matcher(rawContent(event.getMessage()));
        if (!matcher.matches()) {
            return;
        }

        String conversationUuid = matcher.group(1);
        String ciphertext = matcher.group(2).trim();
        String echoKey = conversationUuid + "|" + ciphertext;
        if (!locallyDispatched.remove(echoKey)) {
            dispatch(conversationUuid, ciphertext);
        }
    }

    private static String rawContent(Message message) {
        return message.getContentRaw();
    }
}

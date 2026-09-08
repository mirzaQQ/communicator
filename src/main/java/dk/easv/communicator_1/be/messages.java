package dk.easv.communicator_1.be;

public class messages {
    private final int message_id;
    private final int conversation_id;
    private final int sender_id;
    private final String content;
    private final String sent_at;
    private String sender_username;

    public messages(int message_id, int conversation_id, int sender_id, String content, String sent_at) {
        this.message_id = message_id;
        this.conversation_id = conversation_id;
        this.sender_id = sender_id;
        this.content = content;
        this.sent_at = sent_at;
    }

    public int getMessage_id() {
        return message_id;
    }

    public int getConversation_id() {
        return conversation_id;
    }

    public int getSender_id() {
        return sender_id;
    }

    public String getContent() {
        return content;
    }

    public String getSent_at() {
        return sent_at;
    }

    public String getSender_username() {
        return sender_username;
    }

    public void setSender_username(String sender_username) {
        this.sender_username = sender_username;
    }
}

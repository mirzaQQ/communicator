package dk.easv.communicator_1.be;

public class conversations {
    private final int conversation_id;
    private String conversation_uuid;
    private String encryption_key;
    private String created_at;

    public conversations(int conversations_id){
        this.conversation_id = conversations_id;
    }

    public conversations(int conversation_id, String conversation_uuid, String encryption_key) {
        this.conversation_id = conversation_id;
        this.conversation_uuid = conversation_uuid;
        this.encryption_key = encryption_key;
    }

    public int getConversation_id() {
        return conversation_id;
    }

    public String getConversation_uuid() {
        return conversation_uuid;
    }

    public String getEncryption_key() {
        return encryption_key;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }
}

package dk.easv.communicator_1.be;

public class conversation_members {
    private final int conversation_id;
    private int user_id;

    public conversation_members(int conversation_id, int user_id){
        this.user_id = user_id;
        this.conversation_id = conversation_id;
    }
    public conversation_members(int conversation_id){
        this.conversation_id = conversation_id;
    }

    public int getConversation_id() {
        return conversation_id;
    }

    public int getUser_id() {
        return user_id;
    }
}

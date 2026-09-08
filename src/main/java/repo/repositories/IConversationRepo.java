package repo.repositories;

import dk.easv.communicator_1.be.conversations;

public interface IConversationRepo {
    conversations findOrCreateConversation(int userId1, int userId2);

    conversations findByUuid(String conversationUuid);

    boolean isMember(int conversationId, int userId);
}

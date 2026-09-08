package dk.easv.communicator_1.Service;

import dk.easv.communicator_1.be.conversations;
import repo.impl.ConversationRepo;
import repo.repositories.IConversationRepo;

public class ChatService {
    private final IConversationRepo conversationRepo = new ConversationRepo();

    public conversations openConversation(int currentUserId, int otherUserId) {
        System.out.println("[ChatService] openConversation currentUserId=" + currentUserId + " otherUserId=" + otherUserId);

        return conversationRepo.findOrCreateConversation(currentUserId, otherUserId);
    }

    public conversations findByUuid(String conversationUuid) {
        return conversationRepo.findByUuid(conversationUuid);
    }

    public boolean isMember(int conversationId, int userId) {
        return conversationRepo.isMember(conversationId, userId);
    }
}

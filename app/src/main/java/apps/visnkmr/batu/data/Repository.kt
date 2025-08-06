package apps.visnkmr.batu.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {

    fun conversations(): Flow<List<Conversation>> = conversationDao.getAll()

    fun messages(conversationId: Long): Flow<List<Message>> =
        messageDao.getByConversation(conversationId)

    suspend fun newConversation(title: String = "New chat", parentConversationId: Long? = null): Long {
        val now = System.currentTimeMillis()
        val convo = Conversation(
            title = title,
            createdAt = now,
            lastUpdated = now,
            parentConversationId = parentConversationId
        )
        return conversationDao.insert(convo)
    }

    suspend fun addUserMessage(conversationId: Long, content: String): Long {
        val id = messageDao.insert(
            Message(
                conversationId = conversationId,
                role = "user",
                content = content
            )
        )
        touchConversation(conversationId)
        return id
    }

    suspend fun addAssistantPlaceholder(conversationId: Long): Long {
        val id = messageDao.insert(
            Message(
                conversationId = conversationId,
                role = "assistant",
                content = ""
            )
        )
        touchConversation(conversationId)
        return id
    }

    suspend fun updateMessageContent(messageId: Long, newContent: String) {
        val existing = messageDao.getByIdOnce(messageId) ?: return
        messageDao.update(existing.copy(content = newContent))
        touchConversation(existing.conversationId)
    }

    private suspend fun touchConversation(conversationId: Long) {
        val conv = conversationDao.getById(conversationId).first() ?: return
        conversationDao.update(conv.copy(lastUpdated = System.currentTimeMillis()))
    }

    suspend fun deleteConversation(conversationId: Long) {
        messageDao.deleteByConversation(conversationId)
        conversationDao.delete(conversationId)
    }

    /**
     * Branch from a message: create new conversation and copy messages up to and including that message.
     */
    suspend fun branchFromMessage(messageId: Long): Long {
        val msg = messageDao.getByIdOnce(messageId) ?: return -1
        val sourceConvoId = msg.conversationId
        val all = messageDao.getByConversation(sourceConvoId).first()

        val idx = all.indexOfFirst { it.id == messageId }
        val toCopy = if (idx >= 0) all.take(idx + 1) else all

        val newConvoId = newConversation(
            title = "Branch of #$sourceConvoId",
            parentConversationId = sourceConvoId
        )
        toCopy.forEach { m ->
            messageDao.insert(
                Message(
                    conversationId = newConvoId,
                    role = m.role,
                    content = m.content,
                    createdAt = m.createdAt,
                    replyToMessageId = m.replyToMessageId
                )
            )
        }
        return newConvoId
    }
}

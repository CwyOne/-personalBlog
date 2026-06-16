package com.blog.ai.task;

import com.blog.ai.document.ChatSessionDoc;
import com.blog.ai.repository.ChatMessageRepository;
import com.blog.ai.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class ChatCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(ChatCleanupTask.class);

    @Resource
    private ChatSessionRepository chatSessionRepository;

    @Resource
    private ChatMessageRepository chatMessageRepository;

    /** 每天凌晨3点清理超过7天的会话 */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldSessions() {
        LocalDateTime expireTime = LocalDateTime.now().minusDays(7);
        List<ChatSessionDoc> allSessions = chatSessionRepository.findAll();
        int deleted = 0;
        for (ChatSessionDoc session : allSessions) {
            if (session.getLastActiveTime() != null && session.getLastActiveTime().isBefore(expireTime)) {
                chatMessageRepository.deleteBySessionId(session.getSessionId());
                chatSessionRepository.delete(session);
                deleted++;
            }
        }
        if (deleted > 0) {
            log.info("清理过期聊天会话：{} 个", deleted);
        }
    }
}

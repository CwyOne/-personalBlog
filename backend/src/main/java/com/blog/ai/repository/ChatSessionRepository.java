package com.blog.ai.repository;

import com.blog.ai.document.ChatSessionDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ChatSessionRepository extends MongoRepository<ChatSessionDoc, String> {

    List<ChatSessionDoc> findByUserTokenOrderByLastActiveTimeDesc(String userToken);

    ChatSessionDoc findBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);

    void deleteByLastActiveTimeBefore(LocalDateTime time);
}

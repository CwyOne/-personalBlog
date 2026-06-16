package com.blog.ai.repository;

import com.blog.ai.document.ChatMessageDoc;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessageDoc, String> {

    List<ChatMessageDoc> findBySessionIdOrderByCreateTimeAsc(String sessionId);

    void deleteBySessionId(String sessionId);
}

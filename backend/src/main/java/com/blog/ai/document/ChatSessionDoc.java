package com.blog.ai.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "chat_sessions")
public class ChatSessionDoc {

    @Id
    private String id;
    private String sessionId;
    private String userToken;    // userId 或匿名 token
    private String title;        // 会话标题（取自第一条消息）
    private LocalDateTime createTime;
    private LocalDateTime lastActiveTime;

    public ChatSessionDoc() {}

    public ChatSessionDoc(String sessionId, String userToken, String title) {
        this.sessionId = sessionId;
        this.userToken = userToken;
        this.title = title;
        this.createTime = LocalDateTime.now();
        this.lastActiveTime = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getUserToken() { return userToken; }
    public void setUserToken(String userToken) { this.userToken = userToken; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getLastActiveTime() { return lastActiveTime; }
    public void setLastActiveTime(LocalDateTime lastActiveTime) { this.lastActiveTime = lastActiveTime; }
}

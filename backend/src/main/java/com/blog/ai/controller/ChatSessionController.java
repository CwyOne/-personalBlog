package com.blog.ai.controller;

import com.blog.ai.document.ChatMessageDoc;
import com.blog.ai.document.ChatSessionDoc;
import com.blog.ai.repository.ChatMessageRepository;
import com.blog.ai.repository.ChatSessionRepository;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatSessionController {

    @Resource
    private ChatSessionRepository chatSessionRepository;

    @Resource
    private ChatMessageRepository chatMessageRepository;

    /** 获取当前用户的会话列表 */
    @GetMapping("/sessions")
    public List<Map<String, Object>> listSessions(@RequestParam String userToken) {
        List<ChatSessionDoc> sessions = chatSessionRepository.findByUserTokenOrderByLastActiveTimeDesc(userToken);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatSessionDoc s : sessions) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("sessionId", s.getSessionId());
            map.put("title", s.getTitle());
            map.put("createTime", s.getCreateTime());
            map.put("lastActiveTime", s.getLastActiveTime());
            result.add(map);
        }
        return result;
    }

    /** 新建会话 */
    @PostMapping("/sessions")
    public Map<String, String> createSession(@RequestBody Map<String, String> body) {
        String userToken = body.get("userToken");
        String sessionId = "sess_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6);
        ChatSessionDoc session = new ChatSessionDoc(sessionId, userToken, "新对话");
        chatSessionRepository.save(session);
        return Map.of("sessionId", sessionId, "title", "新对话");
    }

    /** 获取会话的消息列表 */
    @GetMapping("/sessions/{sessionId}/messages")
    public List<ChatMessageDoc> getMessages(@PathVariable String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
    }

    /** 删除会话及其消息 */
    @DeleteMapping("/sessions/{sessionId}")
    public Map<String, String> deleteSession(@PathVariable String sessionId) {
        chatSessionRepository.deleteBySessionId(sessionId);
        chatMessageRepository.deleteBySessionId(sessionId);
        return Map.of("status", "ok");
    }
}

package com.blog.ai.controller;

import com.blog.ai.config.DashScopeModel;
import com.blog.ai.document.ChatMessageDoc;
import com.blog.ai.document.ChatSessionDoc;
import com.blog.ai.repository.ChatMessageRepository;
import com.blog.ai.repository.ChatSessionRepository;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private static final String SYSTEM_PROMPT = loadSystemPrompt();

    private static String loadSystemPrompt() {
        try {
            return new String(new ClassPathResource("SystemMessage.txt").getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            throw new IllegalStateException("无法加载 SystemMessage.txt", e);
        }
    }

    @Resource
    private DashScopeModel dashScopeModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Resource
    private ChatMessageRepository chatMessageRepository;

    @Resource
    private ChatSessionRepository chatSessionRepository;

    @PostMapping(value = "/api/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody Map<String, String> body) {
        String sessionId = body.getOrDefault("sessionId", "default");
        String userToken = body.getOrDefault("userToken", "");
        String message = body.get("message");

        if (message == null || message.trim().isEmpty()) {
            return Flux.just("请输入消息内容");
        }

        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        try {
            // 确保会话存在
            ensureSession(sessionId, userToken, message.trim());

            // 1. RAG 检索相关文章片段
            String articleContext = retrieveArticles(message.trim());

            // 2. 从 MongoDB 加载对话历史
            List<ChatMessage> history = loadHistory(sessionId);

            String systemPrompt = SYSTEM_PROMPT;
            if (!articleContext.isEmpty()) {
                systemPrompt += "\n\n【参考文章片段】\n" + articleContext;
            }

            List<ChatMessage> fullMessages = new ArrayList<>();
            fullMessages.add(new SystemMessage(systemPrompt));
            fullMessages.addAll(history);
            fullMessages.add(new UserMessage(message.trim()));

            // 3. 流式调用 DashScope
            dashScopeModel.streamChat(fullMessages,
                    token -> sink.tryEmitNext(token),
                    response -> {
                        saveMessage(sessionId, "user", message.trim());
                        saveMessage(sessionId, "assistant", response.content().text());
                        sink.tryEmitComplete();
                    },
                    error -> {
                        log.error("AI 调用失败", error);
                        sink.tryEmitNext("\n\n抱歉，AI 服务暂时不可用，请稍后再试。");
                        sink.tryEmitComplete();
                    }
            );
        } catch (Exception e) {
            log.error("AI 调用失败", e);
            sink.tryEmitNext("抱歉，AI 服务暂时不可用：" + e.getMessage());
            sink.tryEmitComplete();
        }

        return sink.asFlux();
    }

    private void ensureSession(String sessionId, String userToken, String firstMessage) {
        ChatSessionDoc session = chatSessionRepository.findBySessionId(sessionId);
        if (session == null) {
            String title = firstMessage.length() > 20 ? firstMessage.substring(0, 20) + "..." : firstMessage;
            session = new ChatSessionDoc(sessionId, userToken, title);
            chatSessionRepository.save(session);
        } else {
            session.setLastActiveTime(LocalDateTime.now());
            if ("新对话".equals(session.getTitle())) {
                String title = firstMessage.length() > 20 ? firstMessage.substring(0, 20) + "..." : firstMessage;
                session.setTitle(title);
            }
            chatSessionRepository.save(session);
        }
    }

    private String retrieveArticles(String query) {
        try {
            float[] vector = dashScopeModel.embed(query);
            List<EmbeddingMatch<TextSegment>> results = embeddingStore.findRelevant(
                    new Embedding(vector), 4, 0.4);

            if (results == null || results.isEmpty()) return "";

            StringBuilder sb = new StringBuilder();
            for (EmbeddingMatch<TextSegment> match : results) {
                TextSegment seg = (TextSegment) match.embedded();
                if (seg == null) continue;
                Metadata meta = seg.metadata();
                sb.append("---\n");
                sb.append("文章标题：").append(meta.getString("title")).append("\n");
                sb.append("发布日期：").append(meta.getString("date")).append("\n");
                sb.append("链接：").append(meta.getString("url")).append("\n");
                sb.append("片段摘要：").append(seg.text()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("文章检索失败: {}", e.getMessage());
            return "";
        }
    }

    private List<ChatMessage> loadHistory(String sessionId) {
        List<ChatMessageDoc> docs = chatMessageRepository.findBySessionIdOrderByCreateTimeAsc(sessionId);
        int start = Math.max(0, docs.size() - 20);
        List<ChatMessage> messages = new ArrayList<>();
        for (int i = start; i < docs.size(); i++) {
            ChatMessageDoc doc = docs.get(i);
            if ("user".equals(doc.getRole())) {
                messages.add(new UserMessage(doc.getContent()));
            } else {
                messages.add(new AiMessage(doc.getContent()));
            }
        }
        return messages;
    }

    private void saveMessage(String sessionId, String role, String content) {
        try {
            chatMessageRepository.save(new ChatMessageDoc(sessionId, role, content));
        } catch (Exception e) {
            log.warn("保存聊天记录失败: {}", e.getMessage());
        }
    }
}

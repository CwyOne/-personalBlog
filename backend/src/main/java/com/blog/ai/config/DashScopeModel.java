package com.blog.ai.config;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 直接调用 DashScope SDK 的 AI 模型封装。
 * 不依赖 langchain4j-dashscope 模块，避免版本兼容问题。
 */
@Component
public class DashScopeModel {

    private static final Logger log = LoggerFactory.getLogger(DashScopeModel.class);

    @Value("${ai.dashscope.api-key}")
    private String apiKey;

    @Value("${ai.dashscope.chat-model:qwen-plus}")
    private String chatModel;

    @Value("${ai.dashscope.embedding-model:text-embedding-v3}")
    private String embeddingModel;

    /**
     * 流式调用 DashScope 聊天模型
     */
    public void streamChat(List<ChatMessage> messages, Consumer<String> onToken, Consumer<Response<AiMessage>> onComplete, Consumer<Throwable> onError) {
        try {
            Generation gen = new Generation();

            List<Message> dashMessages = toDashScopeMessages(messages);
            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(chatModel)
                    .messages(dashMessages)
                    .resultFormat("message")
                    .incrementalOutput(true)
                    .build();

            Flowable<GenerationResult> flowable = gen.streamCall(param);

            StringBuilder fullResponse = new StringBuilder();
            flowable.subscribe(
                    result -> {
                        String content = result.getOutput().getChoices().get(0).getMessage().getContent();
                        if (content != null) {
                            fullResponse.append(content);
                            onToken.accept(content);
                        }
                        // 打印 token 用量
                        if (result.getUsage() != null) {
                            log.info("DashScope 调用完成 - 输入token: {}, 输出token: {}",
                                    result.getUsage().getInputTokens(),
                                    result.getUsage().getOutputTokens());
                        }
                    },
                    error -> {
                        log.error("DashScope 流式调用失败", error);
                        onError.accept(error);
                    },
                    () -> {
                        AiMessage aiMessage = new AiMessage(fullResponse.toString());
                        onComplete.accept(Response.from(aiMessage));
                    }
            );
        } catch (Exception e) {
            log.error("DashScope 调用初始化失败", e);
            onError.accept(e);
        }
    }

    /**
     * 调用 DashScope 嵌入模型
     */
    public float[] embed(String text) {
        try {
            TextEmbedding embedding = new TextEmbedding();
            TextEmbeddingParam param = TextEmbeddingParam.builder()
                    .apiKey(apiKey)
                    .model(embeddingModel)
                    .text(text)
                    .build();
            TextEmbeddingResult result = embedding.call(param);
            List<Double> vec = result.getOutput().getEmbeddings().get(0).getEmbedding();
            float[] arr = new float[vec.size()];
            for (int i = 0; i < vec.size(); i++) arr[i] = vec.get(i).floatValue();
            return arr;
        } catch (Exception e) {
            log.error("DashScope 嵌入调用失败", e);
            throw new RuntimeException("嵌入模型调用失败: " + e.getMessage(), e);
        }
    }

    private List<Message> toDashScopeMessages(List<ChatMessage> messages) {
        List<Message> list = new ArrayList<>();
        for (ChatMessage msg : messages) {
            if (msg instanceof SystemMessage sm) {
                list.add(Message.builder().role(Role.SYSTEM.getValue()).content(sm.text()).build());
            } else if (msg instanceof UserMessage um) {
                list.add(Message.builder().role(Role.USER.getValue()).content(um.singleText()).build());
            } else if (msg instanceof AiMessage am) {
                list.add(Message.builder().role(Role.ASSISTANT.getValue()).content(am.text()).build());
            }
        }
        return list;
    }
}

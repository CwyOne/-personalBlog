package com.blog.ai.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${ai.pinecone.api-key}")
    private String pineconeApiKey;

    @Value("${ai.pinecone.host}")
    private String pineconeHost;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new PineconeRestStore(pineconeHost, pineconeApiKey);
    }
}

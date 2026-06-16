package com.blog.ai.config;

import com.google.gson.Gson;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 直接调用 Pinecone REST API 的向量存储实现。
 * 兼容新版 serverless 索引（旧版 langchain4j-pinecone 客户端不支持）。
 */
public class PineconeRestStore implements EmbeddingStore<TextSegment> {

    private static final Logger log = LoggerFactory.getLogger(PineconeRestStore.class);
    private static final Gson gson = new Gson();

    private final String host;
    private final String apiKey;
    private final RestTemplate rest = new RestTemplate();

    public PineconeRestStore(String host, String apiKey) {
        this.host = host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
        this.apiKey = apiKey;
    }

    @Override
    public String add(Embedding embedding) {
        String id = UUID.randomUUID().toString();
        add(id, embedding);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        upsert(id, embedding.vector(), null);
    }

    @Override
    public String add(Embedding embedding, TextSegment segment) {
        String id = UUID.randomUUID().toString();
        upsert(id, embedding.vector(), segment);
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = new ArrayList<>();
        for (Embedding e : embeddings) {
            ids.add(add(e));
        }
        return ids;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> segments) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            String id = UUID.randomUUID().toString();
            TextSegment seg = (segments != null && i < segments.size()) ? segments.get(i) : null;
            upsert(id, embeddings.get(i).vector(), seg);
            ids.add(id);
        }
        return ids;
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, int maxResults, double minScore) {
        return query(referenceEmbedding.vector(), maxResults, minScore);
    }

    private void upsert(String id, float[] vector, TextSegment segment) {
        Map<String, Object> vectorMap = new HashMap<>();
        vectorMap.put("id", id);
        vectorMap.put("values", toDoubleList(vector));

        if (segment != null) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("text", segment.text());
            // Copy custom metadata
            if (segment.metadata() != null) {
                segment.metadata().toMap().forEach((k, v) -> {
                    if (v instanceof String) metadata.put(k, v);
                });
            }
            vectorMap.put("metadata", metadata);
        }

        Map<String, Object> body = Map.of("vectors", List.of(vectorMap));
        post("/vectors/upsert", body);
    }

    private List<EmbeddingMatch<TextSegment>> query(float[] vector, int topK, double minScore) {
        Map<String, Object> body = new HashMap<>();
        body.put("vector", toDoubleList(vector));
        body.put("topK", topK);
        body.put("includeMetadata", true);

        ResponseEntity<String> resp = post("/query", body);
        if (resp == null || resp.getStatusCode() != HttpStatus.OK) {
            return Collections.emptyList();
        }

        Map<String, Object> result = gson.fromJson(resp.getBody(), Map.class);
        List<Map<String, Object>> matches = (List<Map<String, Object>>) result.get("matches");
        if (matches == null) return Collections.emptyList();

        List<EmbeddingMatch<TextSegment>> out = new ArrayList<>();
        for (Map<String, Object> m : matches) {
            double score = ((Number) m.get("score")).doubleValue();
            if (score < minScore) continue;

            String matchId = (String) m.get("id");
            Map<String, Object> metadata = (Map<String, Object>) m.get("metadata");
            String text = metadata != null ? (String) metadata.get("text") : null;

            TextSegment segment = null;
            if (text != null) {
                dev.langchain4j.data.document.Metadata meta = new dev.langchain4j.data.document.Metadata();
                if (metadata != null) {
                    metadata.forEach((k, v) -> {
                        if (v instanceof String && !k.equals("text")) meta.put(k, (String) v);
                    });
                }
                segment = new TextSegment(text, meta);
            }

            out.add(new EmbeddingMatch<>(score, matchId, null, segment));
        }
        return out;
    }

    private ResponseEntity<String> post(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Key", apiKey);
        headers.set("Accept", "application/json");

        String json = gson.toJson(body);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);

        try {
            return rest.exchange(host + path, HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
            log.error("Pinecone API 调用失败: {} - {}", path, e.getMessage());
            return null;
        }
    }

    private List<Double> toDoubleList(float[] arr) {
        List<Double> list = new ArrayList<>(arr.length);
        for (float f : arr) list.add((double) f);
        return list;
    }
}

package com.blog.ai.loader;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.ai.config.DashScopeModel;
import com.blog.entity.BlogArticle;
import com.blog.mapper.BlogArticleMapper;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class ArticleDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ArticleDataLoader.class);

    @Resource
    private BlogArticleMapper blogArticleMapper;

    @Resource
    private DashScopeModel dashScopeModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Override
    public void run(String... args) {
        // 检查是否已有数据
        try {
            float[] testVec = dashScopeModel.embed("测试");
            var results = embeddingStore.findRelevant(new Embedding(testVec), 1, 0.0);
            if (results != null && !results.isEmpty()) {
                log.info("Pinecone 索引已有数据，跳过导入");
                return;
            }
        } catch (Exception e) {
            log.warn("检查 Pinecone 索引失败，将尝试导入数据: {}", e.getMessage());
        }

        log.info("开始导入博客文章到 Pinecone...");
        List<BlogArticle> articles = blogArticleMapper.selectList(
                new LambdaQueryWrapper<BlogArticle>()
                        .eq(BlogArticle::getStatus, 1)
                        .orderByDesc(BlogArticle::getCreateTime));

        if (articles.isEmpty()) {
            log.warn("没有已发布的文章，跳过导入");
            return;
        }

        int totalSegments = 0;
        for (BlogArticle article : articles) {
            String plainText = stripHtml(article.getContentHtml());
            List<String> chunks = splitText(plainText, 500);

            for (String chunk : chunks) {
                if (chunk.trim().isEmpty()) continue;

                Metadata metadata = new Metadata();
                metadata.put("title", article.getTitle());
                metadata.put("url", "/article/" + article.getId());
                metadata.put("date", article.getCreateTime() != null
                        ? article.getCreateTime().toString().substring(0, 10) : "");
                metadata.put("text", chunk);

                TextSegment segment = new TextSegment(chunk, metadata);
                float[] vec = dashScopeModel.embed(chunk);
                embeddingStore.add(new Embedding(vec), segment);
                totalSegments++;
            }
            log.info("已导入文章: {} ({} 个片段)", article.getTitle(), chunks.size());
        }
        log.info("文章导入完成，共 {} 篇文章，{} 个片段", articles.size(), totalSegments);
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<String> splitText(String text, int maxLen) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;

        String[] paragraphs = text.split("\\n{2,}|\\r\\n{2,}");
        StringBuilder current = new StringBuilder();

        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;

            if (current.length() + para.length() + 1 > maxLen && current.length() > 0) {
                chunks.add(current.toString().trim());
                current.setLength(0);
            }
            current.append(para).append("\n");
        }
        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }
}

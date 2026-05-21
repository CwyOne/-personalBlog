package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.MarkdownUtil;
import com.blog.dto.ArticleDto;
import com.blog.dto.ArticleListVo;
import com.blog.dto.ArchiveVo;
import com.blog.dto.TagVo;
import com.blog.entity.ArticleTag;
import com.blog.entity.BlogArticle;
import com.blog.entity.BlogTag;
import com.blog.entity.SysUser;
import com.blog.mapper.ArticleTagMapper;
import com.blog.mapper.BlogArticleMapper;
import com.blog.mapper.BlogTagMapper;
import com.blog.mapper.SysUserMapper;
import com.blog.service.BlogArticleService;
import com.blog.service.LikeService;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BlogArticleServiceImpl implements BlogArticleService {

    @Resource
    private BlogArticleMapper blogArticleMapper;

    @Resource
    private BlogTagMapper blogTagMapper;

    @Resource
    private ArticleTagMapper articleTagMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private LikeService likeService;

    @Override
    @Cacheable(value = "articles", key = "#page + ':' + #size + ':' + #status + ':' + (#tagId != null ? #tagId : 'all')")
    public Page<ArticleListVo> getArticleList(int page, int size, Integer status, Long tagId) {
        IPage<BlogArticle> articlePage;
        if (tagId != null) {
            List<BlogArticle> articles = blogArticleMapper.selectByTag(tagId, status);
            int total = articles.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);
            List<BlogArticle> pageList = fromIndex < total ? articles.subList(fromIndex, toIndex) : Collections.emptyList();
            articlePage = new Page<>(page, size, total);
            articlePage.setRecords(pageList);
        } else {
            LambdaQueryWrapper<BlogArticle> wrapper = new LambdaQueryWrapper<>();
            if (status != null) {
                wrapper.eq(BlogArticle::getStatus, status);
            }
            wrapper.orderByDesc(BlogArticle::getCreateTime);
            articlePage = blogArticleMapper.selectPage(new Page<>(page, size), wrapper);
        }

        return buildArticleListVoPage(articlePage);
    }

    @Override
    public Page<ArticleListVo> getAdminArticleList(int page, int size) {
        LambdaQueryWrapper<BlogArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(BlogArticle::getCreateTime);
        IPage<BlogArticle> articlePage = blogArticleMapper.selectPage(new Page<>(page, size), wrapper);
        return buildArticleListVoPage(articlePage);
    }

    @Override
    public Page<ArticleListVo> searchArticles(int page, int size, String keyword) {
        List<BlogArticle> articles = blogArticleMapper.searchByKeyword(keyword);
        int total = articles.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<BlogArticle> pageList = fromIndex < total ? articles.subList(fromIndex, toIndex) : Collections.emptyList();
        IPage<BlogArticle> articlePage = new Page<>(page, size, total);
        articlePage.setRecords(pageList);
        return buildArticleListVoPage(articlePage);
    }

    @Override
    public List<ArchiveVo> getArchive() {
        List<BlogArticle> articles = blogArticleMapper.selectAllPublished();
        Map<String, Map<String, List<ArchiveVo.ArchiveItem>>> grouped = new LinkedHashMap<>();

        for (BlogArticle a : articles) {
            String dateStr = a.getCreateTime() != null ? a.getCreateTime().toString() : "";
            String year = dateStr.length() >= 4 ? dateStr.substring(0, 4) : "未知";
            String month = dateStr.length() >= 7 ? dateStr.substring(5, 7) : "未知";

            ArchiveVo.ArchiveItem item = new ArchiveVo.ArchiveItem();
            item.setId(a.getId());
            item.setTitle(a.getTitle());
            item.setCreateTime(dateStr);

            grouped.computeIfAbsent(year, k -> new LinkedHashMap<>())
                   .computeIfAbsent(month, k -> new ArrayList<>())
                   .add(item);
        }

        List<ArchiveVo> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<ArchiveVo.ArchiveItem>>> yearEntry : grouped.entrySet()) {
            ArchiveVo vo = new ArchiveVo();
            vo.setYear(yearEntry.getKey());
            List<ArchiveVo.MonthGroup> months = new ArrayList<>();
            for (Map.Entry<String, List<ArchiveVo.ArchiveItem>> monthEntry : yearEntry.getValue().entrySet()) {
                ArchiveVo.MonthGroup mg = new ArchiveVo.MonthGroup();
                mg.setMonth(monthEntry.getKey());
                mg.setArticles(monthEntry.getValue());
                months.add(mg);
            }
            vo.setMonths(months);
            result.add(vo);
        }
        return result;
    }

    @Override
    public ArticleListVo getArticleDetail(Long id) {
        return getArticleDetail(id, null);
    }

    public ArticleListVo getArticleDetail(Long id, Long userId) {
        BlogArticle article = blogArticleMapper.selectById(id);
        if (article == null) {
            return null;
        }
        article.setViewCount(article.getViewCount() + 1);
        blogArticleMapper.updateById(article);

        return buildArticleListVo(article, userId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "articles", allEntries = true)
    public void createArticle(ArticleDto dto) {
        BlogArticle article = new BlogArticle();
        BeanUtils.copyProperties(dto, article);
        article.setContentMd(dto.getContentMd());
        article.setContentHtml(MarkdownUtil.toHtml(dto.getContentMd()));
        article.setViewCount(0);
        if (dto.getStatus() == null) {
            article.setStatus(0);
        }
        if (article.getAuthorId() == null) {
            article.setAuthorId(1L);
        }
        blogArticleMapper.insert(article);
        saveTags(article.getId(), dto.getTagIds());
    }

    @Override
    @Transactional
    @CacheEvict(value = "articles", allEntries = true)
    public void updateArticle(Long id, ArticleDto dto) {
        BlogArticle article = blogArticleMapper.selectById(id);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        BeanUtils.copyProperties(dto, article);
        article.setContentHtml(MarkdownUtil.toHtml(dto.getContentMd()));
        blogArticleMapper.updateById(article);

        articleTagMapper.delete(
                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, id)
        );
        saveTags(id, dto.getTagIds());
    }

    @Override
    @Transactional
    @CacheEvict(value = "articles", allEntries = true)
    public void deleteArticle(Long id) {
        blogArticleMapper.deleteById(id);
    }

    private void saveTags(Long articleId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds) {
            ArticleTag at = new ArticleTag();
            at.setArticleId(articleId);
            at.setTagId(tagId);
            articleTagMapper.insert(at);
        }
    }

    private Page<ArticleListVo> buildArticleListVoPage(IPage<BlogArticle> articlePage) {
        Page<ArticleListVo> voPage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        if (articlePage.getRecords().isEmpty()) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        List<Long> articleIds = articlePage.getRecords().stream()
                .map(BlogArticle::getId).collect(Collectors.toList());

        Map<Long, List<TagVo>> tagMap = getTagMap(articleIds);
        Map<Long, SysUser> authorMap = getAuthorMap(articlePage.getRecords());
        Map<Long, Long> likeCountMap = likeService.getLikeCountMap("article", articleIds);

        List<ArticleListVo> voList = articlePage.getRecords().stream().map(article -> {
            ArticleListVo vo = new ArticleListVo();
            BeanUtils.copyProperties(article, vo);
            vo.setTags(tagMap.getOrDefault(article.getId(), Collections.emptyList()));
            SysUser author = authorMap.get(article.getAuthorId());
            if (author != null) {
                vo.setAuthorNickname(author.getNickname() != null ? author.getNickname() : author.getUsername());
                vo.setAuthorAvatar(author.getAvatar());
            }
            vo.setLikeCount(likeCountMap.getOrDefault(article.getId(), 0L));
            vo.setLiked(false);
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    private ArticleListVo buildArticleListVo(BlogArticle article, Long userId) {
        ArticleListVo vo = new ArticleListVo();
        BeanUtils.copyProperties(article, vo);
        Map<Long, List<TagVo>> tagMap = getTagMap(Collections.singletonList(article.getId()));
        vo.setTags(tagMap.getOrDefault(article.getId(), Collections.emptyList()));
        SysUser author = sysUserMapper.selectById(article.getAuthorId());
        if (author != null) {
            vo.setAuthorNickname(author.getNickname() != null ? author.getNickname() : author.getUsername());
            vo.setAuthorAvatar(author.getAvatar());
        }
        vo.setLikeCount(likeService.getLikeCount("article", article.getId()));
        if (userId != null) {
            Set<Long> liked = likeService.getUserLikedIds(userId, "article", Collections.singletonList(article.getId()));
            vo.setLiked(liked.contains(article.getId()));
        } else {
            vo.setLiked(false);
        }
        return vo;
    }

    private Map<Long, SysUser> getAuthorMap(List<BlogArticle> articles) {
        Set<Long> authorIds = articles.stream()
                .map(BlogArticle::getAuthorId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (authorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUser> users = sysUserMapper.selectBatchIds(authorIds);
        return users.stream().collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
    }

    private Map<Long, List<TagVo>> getTagMap(List<Long> articleIds) {
        if (articleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ArticleTag> relations = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, articleIds)
        );
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> tagIds = relations.stream().map(ArticleTag::getTagId).distinct().collect(Collectors.toList());
        List<BlogTag> tags = blogTagMapper.selectBatchIds(tagIds);
        Map<Long, String> tagNameMap = tags.stream()
                .collect(Collectors.toMap(BlogTag::getId, BlogTag::getName));

        return relations.stream()
                .collect(Collectors.groupingBy(
                        ArticleTag::getArticleId,
                        Collectors.mapping(r -> {
                            TagVo tv = new TagVo();
                            tv.setId(r.getTagId());
                            tv.setName(tagNameMap.getOrDefault(r.getTagId(), ""));
                            return tv;
                        }, Collectors.toList())
                ));
    }
}

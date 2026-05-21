package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.dto.CommentVo;
import com.blog.entity.BlogArticle;
import com.blog.entity.BlogComment;
import com.blog.entity.SysUser;
import com.blog.mapper.BlogArticleMapper;
import com.blog.mapper.BlogCommentMapper;
import com.blog.mapper.SysUserMapper;
import com.blog.service.BlogCommentService;
import com.blog.service.LikeService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BlogCommentServiceImpl implements BlogCommentService {

    @Resource
    private BlogCommentMapper blogCommentMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private BlogArticleMapper blogArticleMapper;

    @Resource
    private LikeService likeService;

    @Override
    public List<CommentVo> getCommentsByArticleId(Long articleId) {
        List<BlogComment> comments = blogCommentMapper.selectList(
                new LambdaQueryWrapper<BlogComment>()
                        .eq(BlogComment::getArticleId, articleId)
                        .orderByAsc(BlogComment::getCreateTime)
        );
        return toVoList(comments, null);
    }

    public List<CommentVo> getCommentsByArticleId(Long articleId, Long userId) {
        List<BlogComment> comments = blogCommentMapper.selectList(
                new LambdaQueryWrapper<BlogComment>()
                        .eq(BlogComment::getArticleId, articleId)
                        .orderByAsc(BlogComment::getCreateTime)
        );
        return toVoList(comments, userId);
    }

    @Override
    public void addComment(Long articleId, Long userId, String content) {
        BlogComment comment = new BlogComment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(content);
        blogCommentMapper.insert(comment);
    }

    public List<CommentVo> toVoList(List<BlogComment> comments) {
        return toVoList(comments, null);
    }

    public List<CommentVo> toVoList(List<BlogComment> comments, Long currentUserId) {
        if (comments.isEmpty()) return Collections.emptyList();

        List<Long> userIds = comments.stream().map(BlogComment::getUserId).distinct().collect(Collectors.toList());
        List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
        Map<Long, SysUser> userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        List<Long> articleIds = comments.stream().map(BlogComment::getArticleId).distinct().collect(Collectors.toList());
        List<BlogArticle> articles = blogArticleMapper.selectBatchIds(articleIds);
        Map<Long, String> articleTitleMap = articles.stream().collect(Collectors.toMap(BlogArticle::getId, BlogArticle::getTitle, (a, b) -> a));

        List<Long> commentIds = comments.stream().map(BlogComment::getId).collect(Collectors.toList());
        Map<Long, Long> likeCountMap = likeService.getLikeCountMap("comment", commentIds);
        Set<Long> userLikedIds = currentUserId != null ? likeService.getUserLikedIds(currentUserId, "comment", commentIds) : Collections.emptySet();

        return comments.stream().map(c -> {
            CommentVo vo = new CommentVo();
            BeanUtils.copyProperties(c, vo);
            SysUser user = userMap.get(c.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
                vo.setAvatar(user.getAvatar());
            }
            vo.setArticleTitle(articleTitleMap.getOrDefault(c.getArticleId(), "文章已删除"));
            vo.setLikeCount(likeCountMap.getOrDefault(c.getId(), 0L));
            vo.setLiked(currentUserId != null && userLikedIds.contains(c.getId()));
            return vo;
        }).collect(Collectors.toList());
    }
}

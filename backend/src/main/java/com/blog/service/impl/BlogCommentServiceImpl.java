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
    public void addComment(Long articleId, Long userId, String content, Long parentId, Long replyToUserId) {
        BlogComment comment = new BlogComment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(content);
        if (parentId != null) {
            // 校验父评论存在且属于同一文章
            BlogComment parent = blogCommentMapper.selectById(parentId);
            if (parent == null || !parent.getArticleId().equals(articleId)) {
                throw new IllegalArgumentException("父评论不存在");
            }
            comment.setParentId(parentId);
        }
        if (replyToUserId != null) {
            comment.setReplyToUserId(replyToUserId);
        }
        blogCommentMapper.insert(comment);
    }

    public List<CommentVo> toVoList(List<BlogComment> comments) {
        return toVoList(comments, null);
    }

    /**
     * 管理后台用：返回扁平VO列表（含回复信息），不做树形组装
     */
    public List<CommentVo> toFlatVoList(List<BlogComment> comments) {
        if (comments.isEmpty()) return Collections.emptyList();

        Set<Long> allUserIds = new HashSet<>();
        comments.stream().map(BlogComment::getUserId).forEach(allUserIds::add);
        comments.stream().map(BlogComment::getReplyToUserId).filter(Objects::nonNull).forEach(allUserIds::add);
        List<SysUser> users = sysUserMapper.selectBatchIds(allUserIds);
        Map<Long, SysUser> userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        List<Long> articleIds = comments.stream().map(BlogComment::getArticleId).distinct().collect(Collectors.toList());
        List<BlogArticle> articles = blogArticleMapper.selectBatchIds(articleIds);
        Map<Long, String> articleTitleMap = articles.stream().collect(Collectors.toMap(BlogArticle::getId, BlogArticle::getTitle, (a, b) -> a));

        return comments.stream().map(c -> {
            CommentVo vo = new CommentVo();
            BeanUtils.copyProperties(c, vo);
            SysUser user = userMap.get(c.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
                vo.setAvatar(user.getAvatar());
            }
            if (c.getReplyToUserId() != null) {
                SysUser replyTo = userMap.get(c.getReplyToUserId());
                if (replyTo != null) {
                    vo.setReplyToNickname(replyTo.getNickname() != null ? replyTo.getNickname() : replyTo.getUsername());
                }
            }
            vo.setArticleTitle(articleTitleMap.getOrDefault(c.getArticleId(), "文章已删除"));
            return vo;
        }).collect(Collectors.toList());
    }

    public List<CommentVo> toVoList(List<BlogComment> comments, Long currentUserId) {
        if (comments.isEmpty()) return Collections.emptyList();

        List<Long> userIds = comments.stream().map(BlogComment::getUserId).distinct().collect(Collectors.toList());
        // 也收集被回复用户的ID
        Set<Long> allUserIds = new HashSet<>(userIds);
        comments.stream().map(BlogComment::getReplyToUserId).filter(Objects::nonNull).forEach(allUserIds::add);
        List<SysUser> users = sysUserMapper.selectBatchIds(allUserIds);
        Map<Long, SysUser> userMap = users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        List<Long> articleIds = comments.stream().map(BlogComment::getArticleId).distinct().collect(Collectors.toList());
        List<BlogArticle> articles = blogArticleMapper.selectBatchIds(articleIds);
        Map<Long, String> articleTitleMap = articles.stream().collect(Collectors.toMap(BlogArticle::getId, BlogArticle::getTitle, (a, b) -> a));

        List<Long> commentIds = comments.stream().map(BlogComment::getId).collect(Collectors.toList());
        Map<Long, Long> likeCountMap = likeService.getLikeCountMap("comment", commentIds);
        Set<Long> userLikedIds = currentUserId != null ? likeService.getUserLikedIds(currentUserId, "comment", commentIds) : Collections.emptySet();

        // 所有评论转VO（扁平）
        List<CommentVo> allVos = comments.stream().map(c -> {
            CommentVo vo = new CommentVo();
            BeanUtils.copyProperties(c, vo);
            SysUser user = userMap.get(c.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
                vo.setAvatar(user.getAvatar());
            }
            // 设置被回复人昵称
            if (c.getReplyToUserId() != null) {
                SysUser replyTo = userMap.get(c.getReplyToUserId());
                if (replyTo != null) {
                    vo.setReplyToNickname(replyTo.getNickname() != null ? replyTo.getNickname() : replyTo.getUsername());
                }
            }
            vo.setArticleTitle(articleTitleMap.getOrDefault(c.getArticleId(), "文章已删除"));
            vo.setLikeCount(likeCountMap.getOrDefault(c.getId(), 0L));
            vo.setLiked(currentUserId != null && userLikedIds.contains(c.getId()));
            return vo;
        }).collect(Collectors.toList());

        // 构建树形结构：分离顶层和子评论
        Map<Long, List<CommentVo>> childrenMap = new HashMap<>();
        List<CommentVo> topLevel = new ArrayList<>();
        for (CommentVo vo : allVos) {
            if (vo.getParentId() == null) {
                topLevel.add(vo);
            } else {
                childrenMap.computeIfAbsent(vo.getParentId(), k -> new ArrayList<>()).add(vo);
            }
        }
        // 挂载子评论到父评论
        for (CommentVo vo : allVos) {
            List<CommentVo> children = childrenMap.get(vo.getId());
            vo.setChildren(children != null ? children : Collections.emptyList());
        }

        return topLevel;
    }
}

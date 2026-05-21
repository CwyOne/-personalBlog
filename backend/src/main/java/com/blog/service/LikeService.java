package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.BlogLike;
import com.blog.mapper.BlogLikeMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LikeService {

    @Resource
    private BlogLikeMapper blogLikeMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // Like an article
    @Transactional
    public Map<String, Object> toggleLike(Long userId, String targetType, Long targetId) {
        BlogLike exist = blogLikeMapper.selectOne(
                new LambdaQueryWrapper<BlogLike>()
                        .eq(BlogLike::getUserId, userId)
                        .eq(BlogLike::getTargetType, targetType)
                        .eq(BlogLike::getTargetId, targetId)
        );
        boolean liked;
        if (exist != null) {
            blogLikeMapper.deleteById(exist.getId());
            liked = false;
        } else {
            BlogLike like = new BlogLike();
            like.setUserId(userId);
            like.setTargetType(targetType);
            like.setTargetId(targetId);
            blogLikeMapper.insert(like);
            liked = true;
        }
        long count = getLikeCount(targetType, targetId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("count", count);
        return result;
    }

    public long getLikeCount(String targetType, Long targetId) {
        return blogLikeMapper.selectCount(
                new LambdaQueryWrapper<BlogLike>()
                        .eq(BlogLike::getTargetType, targetType)
                        .eq(BlogLike::getTargetId, targetId)
        );
    }

    public Set<Long> getUserLikedIds(Long userId, String targetType, List<Long> targetIds) {
        if (targetIds.isEmpty()) return Collections.emptySet();
        return blogLikeMapper.selectList(
                new LambdaQueryWrapper<BlogLike>()
                        .eq(BlogLike::getUserId, userId)
                        .eq(BlogLike::getTargetType, targetType)
                        .in(BlogLike::getTargetId, targetIds)
        ).stream().map(BlogLike::getTargetId).collect(Collectors.toSet());
    }

    public Map<Long, Long> getLikeCountMap(String targetType, List<Long> targetIds) {
        if (targetIds.isEmpty()) return Collections.emptyMap();
        Map<Long, Long> map = new HashMap<>();
        for (Long id : targetIds) {
            map.put(id, getLikeCount(targetType, id));
        }
        return map;
    }
}

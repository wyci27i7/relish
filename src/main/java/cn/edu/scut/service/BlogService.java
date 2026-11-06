package cn.edu.scut.service;

import cn.edu.scut.common.utils.UserHolder;
import cn.edu.scut.entity.BlogLike;
import cn.edu.scut.mapper.BlogLikeMapper;
import cn.edu.scut.mapper.CounterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogLikeMapper blogLikeMapper;
    private final CounterMapper counterMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final Integer BIZ_TYPE_BLOG = 1;
    private static final Integer COUNT_TYPE_LIKE = 1;
    public static final String BLOG_LIKE = "blog:like:";
    public static final String BLOG_COUNT_LIKE = "blog:count:like:";


    @Transactional(rollbackFor = Exception.class)
    public void likeBlog(Long blogId) {
        Long userId = UserHolder.getUserId();
        String likeKey = BLOG_LIKE + blogId;
        String countKey = BLOG_COUNT_LIKE + blogId;

        // 1. Redis SADD，返回true=第一次点赞，false=已经点过
        Long isNewLike = stringRedisTemplate.opsForSet().add(likeKey, String.valueOf(userId));

        if (isNewLike != null && isNewLike == 1) {
            // 点赞：Redis计数+1
            stringRedisTemplate.opsForValue().increment(countKey);

            // MySQL关系表
            BlogLike blogLike = new BlogLike();
            blogLike.setUserId(userId);
            blogLike.setBlogId(blogId);
            blogLikeMapper.insert(blogLike);

            // MySQL计数表
            counterMapper.updateCount(BIZ_TYPE_BLOG, blogId, COUNT_TYPE_LIKE, 1L);
        } else {
            // 取消点赞：Redis先删Set
            stringRedisTemplate.opsForSet().remove(likeKey, String.valueOf(userId));

            // Redis计数-1
            stringRedisTemplate.opsForValue().decrement(countKey);

            // MySQL关系表
            blogLikeMapper.delete(userId, blogId);

            // MySQL计数表
            counterMapper.updateCount(BIZ_TYPE_BLOG, blogId, COUNT_TYPE_LIKE, -1L);
        }
    }
}

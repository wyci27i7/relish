package cn.edu.scut.service;

import cn.edu.scut.common.utils.UserHolder;
import cn.edu.scut.entity.Blog;
import cn.edu.scut.mapper.BlogMapper;
import cn.edu.scut.mapper.FollowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogMapper blogMapper;
    private final FollowMapper followMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String FEED_KEY = "feed:user:";
    private static final Integer PAGE_SIZE = 3;

    // 发博客 + 推送给粉丝
    public void saveBlog(Blog blog) {
        Long userId = UserHolder.getUserId();
        blog.setUserId(userId);

        // 1. 存MySQL，回填自增id
        blogMapper.insert(blog);

        // 2. 查粉丝
        List<Long> followerIds = followMapper.selectFollowerIds(userId);

        // 3. 逐个推送到粉丝收件箱，score=当前时间戳
        long score = System.currentTimeMillis();
        for (Long followerId : followerIds) {
            String feedKey = FEED_KEY + followerId;
            stringRedisTemplate.opsForZSet().add(feedKey, String.valueOf(blog.getId()), score);
        }
    }

    // 刷Feed流，滚动分页
    public Map<String, Object> queryBlogOfFollow(Long max, Integer offset) {
        Long userId = UserHolder.getUserId();
        String feedKey = FEED_KEY + userId;

        // 1. 从收件箱按score倒序取一页，带上score
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate
                .opsForZSet()
                .reverseRangeByScoreWithScores(feedKey, 0, max, offset, PAGE_SIZE);

        Map<String, Object> result = new HashMap<>();
        if (tuples == null || tuples.isEmpty()) {
            result.put("list", new ArrayList<>());
            return result;
        }

        // 2. 解析博客id，同时算出本页最小score和它重复出现的次数（下一页游标）
        List<Long> ids = new ArrayList<>();
        long minScore = 0L;
        int nextOffset = 1;

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            ids.add(Long.valueOf(tuple.getValue()));
            long score = tuple.getScore().longValue();
            if (score == minScore) {
                nextOffset++;
            } else {
                minScore = score;
                nextOffset = 1;
            }
        }

        // 3. 批量查博客详情
        List<Blog> blogs = blogMapper.selectByIds(ids);

        result.put("list", blogs);
        result.put("lastId", minScore);
        result.put("offset", nextOffset);
        return result;
    }
}

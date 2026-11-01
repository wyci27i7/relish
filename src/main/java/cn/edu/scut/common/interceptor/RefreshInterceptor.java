package cn.edu.scut.common.interceptor;

import cn.edu.scut.common.utils.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String token = request.getHeader("authorization");
        log.info("token:{}", token);
        // 如果没有token，这一步先直接放行
        if (token == null || token.isEmpty()) {
            return true;
        }
        String str_userId = stringRedisTemplate.opsForValue().get(token);
        // 如果Redis中这个token过期了，也直接放行
        if (str_userId == null || str_userId.isEmpty()) {
            return true;
        }
        Long userId = Long.parseLong(str_userId);
        // 存用户的ID进入ThreadLocal
        UserHolder.setUserId(userId);
        // 刷新Redis
        stringRedisTemplate.expire(token, 30, TimeUnit.DAYS);
        return true;
    }


    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        UserHolder.remove();
    }

}

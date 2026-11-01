package cn.edu.scut.common.config;

import cn.edu.scut.common.interceptor.LoginInterceptor;
import cn.edu.scut.common.interceptor.RefreshInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class MvcConfig implements WebMvcConfigurer {
    private final LoginInterceptor loginInterceptor;
    private final RefreshInterceptor refreshInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 这里是一个拦截器链 -- 先进后出
        registry.addInterceptor(refreshInterceptor)
                .addPathPatterns("/**")
                .order(0);
        registry.addInterceptor(loginInterceptor)
                .excludePathPatterns(
                        "/shop/**",
                        "/voucher/**",
                        "/shop-type/**",
                        "/upload/**",
                        "/blog/hot",
                        "/user/code",
                        "/user/login"
                ).order(1);
    }
}

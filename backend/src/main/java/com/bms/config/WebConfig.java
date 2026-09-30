package com.bms.config;

import com.bms.user.SuspensionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SuspensionInterceptor suspension;

    public WebConfig(SuspensionInterceptor suspension) {
        this.suspension = suspension;
    }

    /**
     * The profile stays readable, so the application can say why nothing else
     * is, and so do the subscription and whom to contact about it.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(suspension).addPathPatterns("/api/**")
                .excludePathPatterns("/api/me", "/api/subscription", "/api/support");
    }
}

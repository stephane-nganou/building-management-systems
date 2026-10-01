package com.bms.config;

import com.bms.identity.RegistrationRateLimit;
import com.bms.user.SuspensionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SuspensionInterceptor suspension;
    private final RegistrationRateLimit registrationRateLimit;

    public WebConfig(SuspensionInterceptor suspension, RegistrationRateLimit registrationRateLimit) {
        this.suspension = suspension;
        this.registrationRateLimit = registrationRateLimit;
    }

    /** The profile stays readable, so the application can say why nothing else is. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(suspension).addPathPatterns("/api/**").excludePathPatterns("/api/me");
        registry.addInterceptor(registrationRateLimit).addPathPatterns("/api/auth/register");
    }
}

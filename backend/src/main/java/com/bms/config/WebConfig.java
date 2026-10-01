package com.bms.config;

import com.bms.identity.RegistrationRateLimit;
import com.bms.user.PasswordChangeInterceptor;
import com.bms.user.SuspensionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SuspensionInterceptor suspension;
    private final PasswordChangeInterceptor passwordChange;
    private final RegistrationRateLimit registrationRateLimit;

    public WebConfig(SuspensionInterceptor suspension, PasswordChangeInterceptor passwordChange,
                     RegistrationRateLimit registrationRateLimit) {
        this.suspension = suspension;
        this.passwordChange = passwordChange;
        this.registrationRateLimit = registrationRateLimit;
    }

    /**
     * The profile stays readable, so the application can say why nothing else is.
     * An account that has to choose a password may also do exactly that.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(suspension).addPathPatterns("/api/**").excludePathPatterns("/api/me");
        registry.addInterceptor(passwordChange).addPathPatterns("/api/**")
                .excludePathPatterns("/api/me", "/api/auth/password");
        registry.addInterceptor(registrationRateLimit).addPathPatterns("/api/auth/register");
    }
}

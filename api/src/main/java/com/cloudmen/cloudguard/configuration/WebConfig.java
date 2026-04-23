package com.cloudmen.cloudguard.configuration;

import com.cloudmen.cloudguard.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration class for Spring MVC web settings. <p>
 *
 * This class implements {@link WebMvcConfigurer} to customize the default Spring MVC configuration. <p>
 * It is primarily responsible for registering custom HTTP interceptors (such as the authentication interceptor)
 * and configuring global Cross-Origin Resource Sharing (CORS) rules.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;

    /**
     * Constructs a new {@link WebConfig} with the specified authentication interceptor.
     *
     * @param authInterceptor the interceptor used to secure endpoints
     */
    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    /**
     * Registers interceptors for the application's web requests. <p>
     *
     * This method configures the {@link AuthInterceptor} to secure specific route patterns
     * (e.g., {@code /auth/**}, {@code /dashboard/**}) while explicitly excluding public endpoints like
     * the login route and specific setup paths.
     *
     * @param registry the {@link InterceptorRegistry} used to add interceptors
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/auth/**","/google/**", "cache-warmup/**", "/dashboard/**", "/notifications/**", "/report/**", "/user/**", "/teamleader/**")
                .excludePathPatterns("/auth/login", "/teamleader/setup");
    }

    /**
     * Configures global Cross-Origin Resource Sharing (CORS) settings for the application. <p>
     *
     * This bean defines the allowed origins, HTTP methods, and headers. It is currently configured to allow requests
     * from the local frontend development server ({@code http://localhost:4200}) and
     * supports credentials (such as cookies or authorization headers).
     *
     * @return a {@link WebMvcConfigurer} instance with CORS mappings defined
     */
    @Bean
    WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("http://localhost:4200", "https://cloudguard.cloudmen.net")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}

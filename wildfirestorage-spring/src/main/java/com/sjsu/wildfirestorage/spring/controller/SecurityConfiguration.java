package com.sjsu.wildfirestorage.spring.controller;

import com.sjsu.wildfirestorage.spring.util.RequestMatchingAuthenticationManagerResolver;
import com.sjsu.wildfirestorage.spring.util.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    private String user;

    @Autowired
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Value("${custom.allowedCorsOrigins:}")
    private List<String> corsOrigins;
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(CsrfConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(ac -> ac.requestMatchers("/error")
                        .permitAll().anyRequest().authenticated())
                .oauth2Login(oauth2 -> {
                    oauth2.successHandler(oAuth2LoginSuccessHandler);
                }).logout(Customizer.withDefaults());
        http.oauth2ResourceServer().authenticationManagerResolver(customAuthenticationManager());
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(corsOrigins);
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
        return urlBasedCorsConfigurationSource;
    }

    AuthenticationManagerResolver<HttpServletRequest> customAuthenticationManager() {
        LinkedHashMap<RequestMatcher, AuthenticationManager> authenticationManagers = new LinkedHashMap<>();

        List<String> readMethod = Arrays.asList("HEAD", "GET", "OPTIONS");
        RequestMatcher readMethodRequestMatcher = request -> readMethod.contains(request.getMethod());
        authenticationManagers.put(readMethodRequestMatcher, opaque());

        RequestMatchingAuthenticationManagerResolver authenticationManagerResolver
                = new RequestMatchingAuthenticationManagerResolver(authenticationManagers);

        authenticationManagerResolver.setDefaultAuthenticationManager(opaque());
        return authenticationManagerResolver;
    }

    public AuthenticationManager opaque() {
        OpaqueTokenIntrospector introspectionClient = token -> {
            Map userInfo = UserInfo.getUser(token);
            if (userInfo != null) {
                return new DefaultOAuth2AuthenticatedPrincipal("user", Map.of("name", userInfo.get("name")), null);
            }
            else {
                throw new BadOpaqueTokenException("Invalid token " + token);
            }
        };
        return new OpaqueTokenAuthenticationProvider(introspectionClient)::authenticate;
    }
}

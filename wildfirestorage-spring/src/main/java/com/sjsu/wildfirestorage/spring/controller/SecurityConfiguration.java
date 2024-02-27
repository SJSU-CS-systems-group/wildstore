package com.sjsu.wildfirestorage.spring.controller;

import com.sjsu.wildfirestorage.spring.util.RequestMatchingAuthenticationManagerResolver;
import com.sjsu.wildfirestorage.spring.util.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.*;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

    @Autowired
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Value("${custom.allowedCorsOrigins:}")
    private List<String> corsOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(CsrfConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(ac ->
                        ac.requestMatchers("/error")
                                .permitAll()
//                                .requestMatchers("/api").hasRole("ADMIN")
                                .anyRequest().authenticated())
                .oauth2Login(oauth2 -> {
                    oauth2.userInfoEndpoint().userAuthoritiesMapper(userAuthoritiesMapper())
                            .and()
                            .successHandler(oAuth2LoginSuccessHandler);
                })
                .logout(Customizer.withDefaults());
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
                return new DefaultOAuth2AuthenticatedPrincipal("user", Map.of("name", userInfo.get("name")),
                        List.of(new SimpleGrantedAuthority((String) userInfo.get("role"))));
            } else {
                throw new BadOpaqueTokenException("Invalid token " + token);
            }
        };
        return new OpaqueTokenAuthenticationProvider(introspectionClient)::authenticate;
    }

    private GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return (authorities) -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
            authorities.forEach(authority -> {
                Map userInfo = null;
                if (authority.getAuthority().equals("OAUTH2_USER")) {
                    userInfo = UserInfo.getUserBy("email", (String) ((OAuth2UserAuthority) authority).getAttributes().get("login") + "@github");

                    if (userInfo != null && userInfo.get("role") != null) {
                        GrantedAuthority ga = new SimpleGrantedAuthority((String) userInfo.get("role"));
                        mappedAuthorities.add(ga);
                    } else {
                        GrantedAuthority ga = new SimpleGrantedAuthority("ROLE_GUEST");
                        mappedAuthorities.add(ga);
                    }
                } else if (authority.getAuthority().equals("OIDC_USER")) {
                    userInfo = UserInfo.getUserBy("email", (String) ((OidcUserAuthority) authority).getAttributes().get("email"));

                    if (userInfo != null && userInfo.get("role") != null) {
                        GrantedAuthority ga = new SimpleGrantedAuthority((String) userInfo.get("role"));
                        mappedAuthorities.add(ga);
                    } else {
                        GrantedAuthority ga = new SimpleGrantedAuthority("ROLE_GUEST");
                        mappedAuthorities.add(ga);
                    }
                } else {
                    mappedAuthorities.add(authority);
                }
            });
            return mappedAuthorities;
        };
    }

    @Bean
    static RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("ROLE_ADMIN > ROLE_USER\n" +
                "ROLE_USER > ROLE_GUEST");
        return hierarchy;
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy);
        return expressionHandler;
    }
}

package com.vulprioritizer.backend_part.common.config;

import com.vulprioritizer.backend_part.auth.security.JWTFilter;
import com.vulprioritizer.backend_part.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {
    private final String[] PUBLIC_ENDPOINTS={"/auth/**","/","/swagger-ui/**","/v3/api-docs/**","/error",};
    private final JWTFilter jwtFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity){
        httpSecurity
                .formLogin(form->form.disable())
                .csrf(csrf->csrf.disable())
                .httpBasic(httpBasic->httpBasic.disable())
                .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth->auth
                                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                                .requestMatchers("/admin/**").hasRole(String.valueOf(Role.ADMIN))
                                .requestMatchers("/integrations/github/callback").permitAll()
                                .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                .cors(cors->{});
        return httpSecurity.build();


    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration){
        return configuration.getAuthenticationManager();
    }

}

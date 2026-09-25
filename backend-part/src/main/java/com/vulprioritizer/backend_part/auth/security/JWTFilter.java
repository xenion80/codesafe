package com.vulprioritizer.backend_part.auth.security;


import com.vulprioritizer.backend_part.auth.services.JwtAuthService;
import com.vulprioritizer.backend_part.user.entity.User;
import com.vulprioritizer.backend_part.user.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
@RequiredArgsConstructor
@Slf4j
public class JWTFilter extends OncePerRequestFilter {
    private final JwtAuthService jwtAuthService;
    private final UserRepository userRepository;
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try{
            String header =request.getHeader("Authorization");
            if (header==null||!header.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);

                return;
            }
            String token=header.substring(7);
            if(!jwtAuthService.isTokenValid(token)){
                filterChain.doFilter(request,response);
                return;
            }
            if (!"access".equals(jwtAuthService.extractTokenType(token))) {
                filterChain.doFilter(request, response);
                return;
            }
            Long userId= jwtAuthService.extractUserId(token);
            User user=userRepository.findById(userId).orElse(null);
            if(user!=null&& SecurityContextHolder.getContext().getAuthentication()==null){
                UsernamePasswordAuthenticationToken auth=new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.info("Authenticated user: {}", user.getEmail());

            }

        }catch (ExpiredJwtException e){
            log.warn("JWT rejected: token has expired");
        }catch (JwtException e){
            log.warn("JWT rejected: invalid token ({})", e.getMessage());
        }
        filterChain.doFilter(request,response);

    }
}

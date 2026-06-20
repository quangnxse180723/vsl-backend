package com.vslbackend.security;

import com.vslbackend.exception.AppException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

/**
 * Doc "Authorization: Bearer <token>", xac thuc va nap SecurityContext.
 * <p>
 * Loi token (het han / sai) duoc uy quyen cho {@code handlerExceptionResolver}
 * de tra ve cung dinh dang JSON nhu GlobalExceptionHandler.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository;

    @Qualifier("handlerExceptionResolver")
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String header = request.getHeader(HEADER);

        // Khong co Bearer token -> de cac filter sau / entry point xu ly
        if (header == null || !header.startsWith(PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = header.substring(PREFIX.length());
            final String email = jwtService.extractUsername(jwt);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(authToken);
                    SecurityContextHolder.setContext(context);

                    // BAT BUOC cho async (CompletableFuture): luu context vao request attribute
                    // de no song sot qua ASYNC dispatch khi Spring MVC ghi response.
                    // Neu khong, SecurityContextHolderFilter se khong khoi phuc duoc context
                    // o lan dispatch thu 2 -> AuthorizationFilter tra 401 du token hop le.
                    securityContextRepository.saveContext(context, request, response);
                }
            }
            filterChain.doFilter(request, response);
        } catch (AppException ex) {
            // Token het han / khong hop le / user khong ton tai -> tra JSON chuan
            handlerExceptionResolver.resolveException(request, response, null, ex);
        }
    }
}

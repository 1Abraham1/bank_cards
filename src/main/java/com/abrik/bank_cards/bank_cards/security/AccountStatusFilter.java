package com.abrik.bank_cards.bank_cards.security;

import com.abrik.bank_cards.bank_cards.entity.User;
import com.abrik.bank_cards.bank_cards.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AccountStatusFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final JwtAuthenticationEntryPoint entryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws IOException, ServletException {
        var ctx = SecurityContextHolder.getContext();
        var auth = ctx.getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails ud) {
            var userOpt = userRepository.findByUsername(ud.getUsername());
            if (userOpt.isPresent() && Boolean.FALSE.equals(userOpt.get().getActive())) {
                entryPoint.commence(req, res,
                        new DisabledException("User is disabled"));
                return;
            }
        }
        chain.doFilter(req, res);
    }
}

package com.abrik.bank_cards.bank_cards.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JwtTokenUtil {

    private final SecretKey secret;
    private final Duration jwtLifetime;

    public JwtTokenUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.lifetime}") Duration jwtLifetime
    ) {
        this.secret = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtLifetime = jwtLifetime;
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        List<String> rolesList = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        claims.put("roles", rolesList);

        Date issuedDate = new Date();
        Date expiredDate = new Date(issuedDate.getTime() + jwtLifetime.toMillis());

        return Jwts.builder()
                .claims(claims) // ложем в preload токена
                .subject(userDetails.getUsername()) // в subject обычно кладется имя пользователя
                .issuedAt(issuedDate)
                .notBefore(issuedDate)
                .expiration(expiredDate)
                .signWith(secret) // подписываем наш secret
                .compact();
    }

    public String getUsername(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    public List<String> getRoles(String token) {
        List<?> roles = getAllClaimsFromToken(token).get("roles", List.class);
        return roles == null
                ? List.of()
                : roles.stream().map(Object::toString).collect(Collectors.toList());
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        Claims c = getAllClaimsFromToken(token); // здесь уже проверится подпись/сроки
        String expectedUsername = userDetails.getUsername();

        if (expectedUsername != null && !expectedUsername.equals(c.getSubject())) {
            return false;
        }

        Date exp = c.getExpiration();
        return exp == null || exp.after(new Date());
    }

    // получаем из токена полезные данные
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secret)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

package com.mayureshpatel.pfdataservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Issues and validates the JWTs used for stateless authentication, signed with an HMAC key built
 * from {@code spring.security.jwt.secret-key} and expiring after {@code spring.security.jwt.
 * expiration} milliseconds -- both externally configured rather than hardcoded, so key rotation
 * or lifetime changes don't require a code change.
 */
@Service
public class JwtService {

    @Value("${spring.security.jwt.secret-key}")
    private String secretKey;

    @Value("${spring.security.jwt.expiration}")
    private long jwtExpiration;

    /**
     * Reads the username back out of a token. This is always the subject claim, since
     * {@link #buildToken} always sets it from {@code userDetails.getUsername()} at generation
     * time.
     *
     * @param token a previously issued, still-parseable JWT
     * @return the username the token was issued for
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Parses and verifies the token's signature, then pulls a single claim out of its payload.
     *
     * @param token the JWT to parse
     * @param claimsResolver function selecting which claim to return from the full claim set
     * @param <T> the claim's type
     * @return the resolved claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Convenience overload of {@link #generateToken(Map, UserDetails)} for the common case of no
     * extra claims beyond the standard subject/issued-at/expiration set.
     *
     * @param userDetails the user to issue a token for
     * @return a signed, compact JWT string
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * @param extraClaims additional claims to embed in the token payload beyond the standard set
     * @param userDetails the user to issue a token for
     * @return a signed, compact JWT string
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * @param token the JWT to check
     * @param userDetails the user the token is expected to belong to
     * @return true if the token's subject matches {@code userDetails.getUsername()} and the
     *     token hasn't expired
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

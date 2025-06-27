package com.ISA.Restaurant.utils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;  // Correct import for Function
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    private static final String SECRET_KEY = "secret";  // Corrected the typo from SECRET_KET to SECRET_KEY
    private static final int TOKEN_VALIDITY = 3600 * 5;  // Token validity is set to 5 hours
    // Get the username from the token
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }
    // Extract claims from the token
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    // Extract all claims from the token
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
    }
    // Validate the token
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));  // Corrected the validation logic
    }
    // Check if the token is expired
    public Boolean isTokenExpired(String token) {
        final Date expiration = getClaimFromToken(token, Claims::getExpiration);  // Corrected variable name 'expriation' to 'expiration'
        return expiration.before(new Date());
    }
    // Generate a new token
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())  // Set the username as the subject of the token
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + TOKEN_VALIDITY * 1000))  // Set expiration time
                .signWith(io.jsonwebtoken.SignatureAlgorithm.HS256, SECRET_KEY)  // Sign the token with HS256 algorithm and the secret key
                .compact();
    }
}
//package com.ISA.Restaurant.utils;
//
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jwts;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.function.Function;  // Correct import for Function
//import org.springframework.security.core.userdetails.UserDetails;
//
//public class JwtUtil {
//    private static final String SECRET_KEY = "secret";  // Corrected the typo from SECRET_KET to SECRET_KEY
//    private static final int TOKEN_VALIDITY = 3600 * 5;  // Token validity is set to 5 hours
//
//    // Get the username from the token
//    public String getUsernameFromToken(String token) {
//        return getClaimFromToken(token, Claims::getSubject);
//    }
//
//    // Extract claims from the token
//    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
//        final Claims claims = getAllClaimsFromToken(token);
//        return claimsResolver.apply(claims);
//    }
//
//    // Extract all claims from the token
//    private Claims getAllClaimsFromToken(String token) {
//        return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
//    }
//
//    // Validate the token
//    public boolean validateToken(String token, UserDetails userDetails) {
//        final String username = getUsernameFromToken(token);
//        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));  // Corrected the validation logic
//    }
//
//    // Check if the token is expired
//    public Boolean isTokenExpired(String token) {
//        final Date expiration = getClaimFromToken(token, Claims::getExpiration);  // Corrected variable name 'expriation' to 'expiration'
//        return expiration.before(new Date());
//    }
//
//    // Generate a new token
//    public String generateToken(UserDetails userDetails) {
//        Map<String, Object> claims = new HashMap<>();
//        return Jwts.builder()
//                .setClaims(claims)
//                .setSubject(userDetails.getUsername())  // Set the username as the subject of the token
//                .setIssuedAt(new Date(System.currentTimeMillis()))
//                .setExpiration(new Date(System.currentTimeMillis() + TOKEN_VALIDITY * 1000))  // Set expiration time
//                .signWith(io.jsonwebtoken.SignatureAlgorithm.HS256, SECRET_KEY)  // Sign the token with HS256 algorithm and the secret key
//                .compact();
//    }
//}
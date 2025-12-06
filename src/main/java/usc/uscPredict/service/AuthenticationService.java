package usc.uscPredict.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import usc.uscPredict.exception.InvalidRefreshTokenException;
import usc.uscPredict.model.RefreshToken;
import usc.uscPredict.model.User;
import usc.uscPredict.repository.RefreshTokenRepository;
import usc.uscPredict.repository.UserRepository;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final KeyPair keyPair;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${auth.jwt.ttl:PT15M}")
    private Duration tokenTTL;

    @Value("${auth.refresh.ttl:P7D}")
    private Duration refreshTTL;

    @Autowired
    public AuthenticationService(
            AuthenticationManager authenticationManager,
            KeyPair jwtSignatureKeys,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.keyPair = jwtSignatureKeys;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public Authentication login(String email, String password) throws AuthenticationException {
        return authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, password)
        );
    }

    public Authentication loginWithRefreshToken(String refreshToken) throws AuthenticationException {
        Optional<RefreshToken> token = refreshTokenRepository.findByToken(refreshToken);
        if (token.isPresent()) {
            User user = userRepository.findByEmail(token.get().getUserEmail())
                    .orElseThrow(() -> new UsernameNotFoundException(token.get().getUserEmail()));
            return UsernamePasswordAuthenticationToken.authenticated(
                    user, null, user.getAuthorities()
            );
        }
        throw new InvalidRefreshTokenException(refreshToken);
    }

    public String generateJWT(Authentication auth) {
        List<String> roles = auth.getAuthorities().stream()
                .filter(a -> a instanceof SimpleGrantedAuthority)
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .subject(auth.getName())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(tokenTTL)))
                .notBefore(Date.from(Instant.now()))
                .claim("roles", roles)
                .signWith(keyPair.getPrivate())
                .compact();
    }

    public String regenerateRefreshToken(Authentication auth) {
        UUID uuid = UUID.randomUUID();
        RefreshToken refreshToken = new RefreshToken(
                uuid.toString(), auth.getName(), refreshTTL.toSeconds()
        );
        refreshTokenRepository.deleteAllByUserEmail(auth.getName());
        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }

    public void invalidateTokens(String email) {
        refreshTokenRepository.deleteAllByUserEmail(email);
    }

    public Authentication parseJWT(String token) throws JwtException {
        Claims claims = Jwts.parser()
                .verifyWith(keyPair.getPublic())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String email = claims.getSubject();
        Optional<User> user = userRepository.findByEmail(email);

        if (user.isPresent()) {
            return UsernamePasswordAuthenticationToken.authenticated(
                    user.get(), token, user.get().getAuthorities()
            );
        }
        throw new UsernameNotFoundException("User not found: " + email);
    }
}

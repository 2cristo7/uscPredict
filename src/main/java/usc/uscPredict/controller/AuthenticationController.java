package usc.uscPredict.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import usc.uscPredict.dto.LoginRequest;
import usc.uscPredict.dto.LoginResponse;
import usc.uscPredict.dto.RegisterRequest;
import usc.uscPredict.exception.DuplicateEmailException;
import usc.uscPredict.exception.InvalidRefreshTokenException;
import usc.uscPredict.model.Role;
import usc.uscPredict.model.User;
import usc.uscPredict.repository.UserRepository;
import usc.uscPredict.service.AuthenticationService;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "RefreshToken"; // TODO: Use __Secure-RefreshToken in production

    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthenticationController(
            AuthenticationService authenticationService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.authenticationService = authenticationService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Authentication auth = authenticationService.login(request.email(), request.password());
        String token = authenticationService.generateJWT(auth);
        String refreshToken = authenticationService.regenerateRefreshToken(auth);

        User user = (User) auth.getPrincipal();

        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .secure(false) // TODO: Set to true in production with HTTPS
                .httpOnly(true)
                .sameSite(Cookie.SameSite.LAX.toString()) // LAX allows cross-site in dev
                .path("/auth/refresh")
                .maxAge(Duration.ofDays(7))
                .build();

        LoginResponse response = new LoginResponse(
                user.getUuid(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateEmailException(request.email());
        }

        User newUser = new User(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.USER
        );

        User savedUser = userRepository.save(newUser);

        return ResponseEntity.status(201).body(new LoginResponse(
                savedUser.getUuid(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME) String refreshToken) {
        Authentication auth = authenticationService.loginWithRefreshToken(refreshToken);

        if (auth.getPrincipal() != null) {
            String newJwt = authenticationService.generateJWT(auth);
            String newRefreshToken = authenticationService.regenerateRefreshToken(auth);

            User user = (User) auth.getPrincipal();

            ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, newRefreshToken)
                    .secure(false) // TODO: Set to true in production with HTTPS
                    .httpOnly(true)
                    .sameSite(Cookie.SameSite.LAX.toString()) // LAX allows cross-site in dev
                    .path("/auth/refresh")
                    .maxAge(Duration.ofDays(7))
                    .build();

            LoginResponse response = new LoginResponse(
                    user.getUuid(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole().name()
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + newJwt)
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(response);
        }

        throw new InvalidRefreshTokenException(refreshToken);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(Authentication auth) {
        authenticationService.invalidateTokens(auth.getName());

        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .maxAge(0)
                .path("/auth/refresh")
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }
}

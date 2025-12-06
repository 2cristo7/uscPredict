package usc.uscPredict.exception;

import org.springframework.security.core.AuthenticationException;

public class InvalidRefreshTokenException extends AuthenticationException {
    private final String token;

    public InvalidRefreshTokenException(String token) {
        super("Invalid refresh token");
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}

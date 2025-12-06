package usc.uscPredict.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

@RedisHash("RefreshToken")
public class RefreshToken {

    @Id
    private String token;

    @Indexed
    private String userEmail;

    @TimeToLive
    private long ttl;

    public RefreshToken() {}

    public RefreshToken(String token, String userEmail, long ttl) {
        this.token = token;
        this.userEmail = userEmail;
        this.ttl = ttl;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }
}

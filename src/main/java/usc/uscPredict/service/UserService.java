package usc.uscPredict.service;

import io.micrometer.common.lang.Nullable;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import usc.uscPredict.exception.PredictUsernameNotFoundException;
import usc.uscPredict.model.User;
import usc.uscPredict.repository.UserRepository;

import java.util.Set;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository users;

    @Autowired
    public UserService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return users.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    public User addUser(@NonNull User user) {
        return users.save(user);
    }

    public User getUserByName(@Nullable String name) throws PredictUsernameNotFoundException {
        return users.findByName(name).stream().findFirst().orElseThrow(() -> new PredictUsernameNotFoundException("User not found"));
    }

    public User getUserById(UUID uuid) {
        return users.findById(uuid)
                .orElseThrow(() -> new PredictUsernameNotFoundException("User not found with ID: " + uuid));
    }

    public User updateUser(UUID uuid, User user) {
        if (!users.existsById(uuid)) {
            throw new PredictUsernameNotFoundException("User not found with ID: " + uuid);
        }
        return users.save(user);
    }

    public Set<User> getAllUsers() {
        return users.findAll();
    }
}

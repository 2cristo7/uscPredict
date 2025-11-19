package usc.uscPredict.service;

import io.micrometer.common.lang.Nullable;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import usc.uscPredict.exception.PredictUsernameNotFoundException;
import usc.uscPredict.model.User;
import usc.uscPredict.repository.UserRepository;

import java.util.UUID;

@Getter
@Service
public class UserService {
    private final UserRepository users;

    @Autowired
    public UserService(UserRepository users) {
        this.users = users;
/*
        User u1 = new User(
                "Ana López",
                "ana@example.com",
                "hash12345",
                "USER",
                "2025-10-01"
        );

        User u2 = new User(
                "Carlos Pérez",
                "carlos@example.com",
                "hash67890",
                "ADMIN",
                "2025-10-01"
        );

        User u3 = new User(
                "María García",
                "maria@example.com",
                "hashabcde",
                "USER",
                "2025-10-01"
        );

        users.save(u1);
        users.save(u2);
        users.save(u3);*/

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
        // Note: User entity doesn't have setUuid, the uuid is auto-generated
        // We need to save the updated user data while preserving the ID
        return users.save(user);
    }

}

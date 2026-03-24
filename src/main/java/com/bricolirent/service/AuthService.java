package com.bricolirent.service;

import com.bricolirent.domain.entity.User;
import java.util.Optional;

/**
 * Service interface for authentication and user management.
 */
public interface AuthService {

    /**
     * Authenticate a user by username and password.
     */
    Optional<User> authenticate(String username, String password);

    /**
     * Register a new user.
     */
    void register(User user);

    /**
     * Find a user by ID.
     */
    Optional<User> findUserById(Long id);
}

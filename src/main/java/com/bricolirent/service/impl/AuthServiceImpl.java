package com.bricolirent.service.impl;

import com.bricolirent.domain.entity.User;
import com.bricolirent.repository.UserRepository;
import com.bricolirent.service.AuthService;

import java.util.Optional;

/**
 * Implementation of AuthService using Hibernate native sessions.
 */
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository = new UserRepository();

    @Override
    public Optional<User> authenticate(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            return user;
        }
        return Optional.empty();
    }

    @Override
    public void register(User user) {
        // TODO: add validation and password hashing
        userRepository.save(user);
    }

    @Override
    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }
}

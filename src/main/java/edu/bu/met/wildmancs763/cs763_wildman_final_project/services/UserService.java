package edu.bu.met.wildmancs763.cs763_wildman_final_project.services;

import org.springframework.stereotype.Service;

import edu.bu.met.wildmancs763.cs763_wildman_final_project.repositories.UserRepository;
import edu.bu.met.wildmancs763.cs763_wildman_final_project.entities.User;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUser(int userId) {
        return userRepository.findById(userId).orElseThrow();
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(int userId) {
        User user = userRepository.findById(userId).orElseThrow();

        userRepository.delete(user);
    }

}

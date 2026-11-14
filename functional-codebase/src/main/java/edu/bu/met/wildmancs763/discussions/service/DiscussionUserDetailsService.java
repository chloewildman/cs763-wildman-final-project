package edu.bu.met.wildmancs763.discussions.service;

import edu.bu.met.wildmancs763.discussions.repository.DiscussionUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class DiscussionUserDetailsService implements UserDetailsService {

    private final DiscussionUserRepository userRepository;

    public DiscussionUserDetailsService(DiscussionUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username.trim().toLowerCase(Locale.ROOT))
                .map(user -> User.withUsername(user.getUsername())
                        .password(user.getPasswordHash())
                        .roles(user.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
    }
}

package com.hacknosis.backend.services;

import com.hacknosis.backend.models.User;
import com.hacknosis.backend.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@AllArgsConstructor
public class JwtUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    public JwtUserDetailsService() {}

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (this.userRepository.findUserByUsername(username).isEmpty()) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        else {
            return new org.springframework.security.core.userdetails.User(username,
                    new BCryptPasswordEncoder().encode(this.userRepository.findUserByUsername(username).get().getPassword()),
                    getAuthority(username));
        }
    }
    public Set<SimpleGrantedAuthority> getAuthority(String username) {
        User user = this.userRepository.findUserByUsername(username).get();
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
        });
        return authorities;
    }
}

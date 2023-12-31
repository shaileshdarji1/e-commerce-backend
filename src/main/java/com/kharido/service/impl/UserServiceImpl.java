package com.kharido.service.impl;

import com.kharido.entity.User;
import com.kharido.exception.UserAlreadyExistsException;
import com.kharido.exception.UserException;
import com.kharido.repository.UserRepository;
import com.kharido.service.UserService;
import com.kharido.service.impl.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Primary
public class UserServiceImpl implements UserDetailsService, UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    JwtServiceImpl jwtService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByEmail(username);
        return user.map(UserDetailsImpl::new).orElseThrow(()->new UsernameNotFoundException("User not found " + username));
    }

    public User addUser(User user) {
        boolean userExists = checkExistsByEmail(user.getEmail());
        if(!userExists) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            return userRepository.save(user);
        }
        else
        {
            throw new UserAlreadyExistsException(user.getEmail());
        }
    }

    public boolean checkExistsByEmail(String email){
        return userRepository.existsByEmail(email);
    }

    @Override
    public User findUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(()->new UserException("User Not Found"));
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(()->new UserException("user not found"));
    }

    @Override
    public User findUserByToken(String token) {
        token = token.substring(7);
        String email = jwtService.extractUsername(token);
        return findUserByEmail(email);
    }


}

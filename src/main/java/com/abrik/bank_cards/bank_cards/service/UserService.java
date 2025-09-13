package com.abrik.bank_cards.bank_cards.service;

import com.abrik.bank_cards.bank_cards.dto.jwt.JwtRequest;
import com.abrik.bank_cards.bank_cards.dto.RegistrationUserDto;
import com.abrik.bank_cards.bank_cards.dto.UserDto;
import com.abrik.bank_cards.bank_cards.entity.User;
import com.abrik.bank_cards.bank_cards.exception.BadInputParameters;
import com.abrik.bank_cards.bank_cards.exception.PasswordMismatchException;
import com.abrik.bank_cards.bank_cards.exception.UsernameAlreadyExistsException;
import com.abrik.bank_cards.bank_cards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final static String emailRegex = "^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$";

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public UserDto createNewUser(RegistrationUserDto registrationUserDto) {
        if (!registrationUserDto.getPassword().equals(registrationUserDto.getConfirmPassword())) {
            throw new PasswordMismatchException("Passwords don't match");
        }
        if (findByUsername(registrationUserDto.getUsername()).isPresent()) {
            throw new UsernameAlreadyExistsException(registrationUserDto.getUsername());
        }

        validateEmail(registrationUserDto.getEmail());

        User user = new User();
        user.setUsername(registrationUserDto.getUsername());
        user.setEmail(registrationUserDto.getEmail());
        user.setPassword(passwordEncoder.encode(registrationUserDto.getPassword()));
        user.setRoles(List.of(roleService.getUserRole()));
        userRepository.save(user);
        return new UserDto(user.getId(), user.getUsername(), user.getEmail());
    }

    public void deleteAccount(Long userId) {
        userRepository.deleteById(userId);
    }

    public static void validateEmail(String emailAddress) {
        if (emailAddress == null || emailAddress.length() > 254) {
            throw new BadInputParameters("email length exceeds 254 characters or is null");
        }

        if (!Pattern.compile(emailRegex)
                .matcher(emailAddress)
                .matches()) {
            throw new BadInputParameters("invalid email format");
        }
    }
}

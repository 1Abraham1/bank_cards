package com.abrik.bank_cards.bank_cards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.regex.Pattern;

import com.abrik.bank_cards.bank_cards.dto.user.CreateUserResponse;
import com.abrik.bank_cards.bank_cards.dto.user.RegistrationUserDto;
import com.abrik.bank_cards.bank_cards.entity.Role;
import com.abrik.bank_cards.bank_cards.entity.User;
import com.abrik.bank_cards.bank_cards.exception.BadInputParameters;
import com.abrik.bank_cards.bank_cards.exception.PasswordMismatchException;
import com.abrik.bank_cards.bank_cards.exception.UsernameAlreadyExistsException;
import com.abrik.bank_cards.bank_cards.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    RoleService roleService;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks UserService userService;

    private RegistrationUserDto dto(String username, String email, String fullName, String pass, String confirm) {
        RegistrationUserDto d = new RegistrationUserDto();
        d.setUsername(username);
        d.setEmail(email);
        d.setFullName(fullName);
        d.setPassword(pass);
        d.setConfirmPassword(confirm);
        return d;
    }

    @Test
    @DisplayName("createNewUser: успех — кодирование пароля, роль USER, возврат CreateUserResponse")
    void createNewUser_success() {
        // arrange
        var username = "abrik";
        var email = "abrik@example.com";
        var fullName = "Abrik Abr";
        var rawPassword = "password123";
        var encodedPassword = "ENCODED";
        var roleUser = new Role(); // подставь нужный конструктор/поля, если есть
        roleUser.setName("ROLE_USER");

        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(roleService.getUserRole()).thenReturn(roleUser);

        Mockito.lenient().doAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        }).when(userRepository).save(any(User.class));

        var reg = dto(username, email, fullName, rawPassword, rawPassword);

        // act
        CreateUserResponse resp = userService.createNewUser(reg);

        // assert
        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(42L);
        assertThat(resp.getUsername()).isEqualTo(username);
        assertThat(resp.getFullName()).isEqualTo(fullName);
        assertThat(resp.getEmail()).isEqualTo(email);

        // проверим что пароль закодирован и роль назначена
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPassword()).isEqualTo(encodedPassword);
        assertThat(saved.getRoles()).containsExactly(roleUser);

        verify(passwordEncoder).encode(rawPassword);
        verify(userRepository).findByUsername(username);
        verify(roleService).getUserRole();
    }

    @Test
    @DisplayName("createNewUser: Username уже существует → UsernameAlreadyExistsException")
    void createNewUser_usernameExists() {
        var username = "exists";
        var email = "exists@example.com";
        var reg = dto(username, email, null, "pass", "pass");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(new User()));

        assertThrows(UsernameAlreadyExistsException.class, () -> userService.createNewUser(reg));

        verify(userRepository).findByUsername(username);
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("createNewUser: Пароли не совпадают → PasswordMismatchException")
    void createNewUser_passwordMismatch() {
        var reg = dto("user", "u@example.com", null, "pass1", "pass2");

        assertThrows(PasswordMismatchException.class, () -> userService.createNewUser(reg));

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("validateEmail: null → BadInputParameters")
    void validateEmail_null() {
        assertThrows(BadInputParameters.class, () -> UserService.validateEmail(null));
    }

    @Test
    @DisplayName("validateEmail: длина > 254 → BadInputParameters")
    void validateEmail_tooLong() {
        String local = "a".repeat(254);
        String email = local + "@ex.com"; // >254 суммарно
        assertTrue(email.length() > 254);
        assertThrows(BadInputParameters.class, () -> UserService.validateEmail(email));
    }

    @Test
    @DisplayName("validateEmail: плохой формат → BadInputParameters")
    void validateEmail_badFormat() {
        assertThrows(BadInputParameters.class, () -> UserService.validateEmail("no-at-symbol"));
        assertThrows(BadInputParameters.class, () -> UserService.validateEmail("user@domain"));
        assertThrows(BadInputParameters.class, () -> UserService.validateEmail("user@.com"));
        assertThrows(BadInputParameters.class, () -> UserService.validateEmail("user@@example.com"));
    }

    @Test
    @DisplayName("validateEmail: гранично корректный формат — не бросает")
    void validateEmail_edgeValid() {
        // сложный, но корректный email в рамках твоей regex
        String email = "user.name+tag-123_456@sub-domain.example.co.uk";
        assertDoesNotThrow(() -> UserService.validateEmail(email));
        // Дополнительно можно проверить, что он совпадает с regex из сервиса:
        String re = "^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$";
        assertTrue(Pattern.compile(re).matcher(email).matches());
    }

    @Test
    @DisplayName("findByUsername: делегирует в репозиторий")
    void findByUsername_delegates() {
        var user = new User();
        user.setId(7L);
        user.setUsername("john");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        var res = userService.findByUsername("john");
        assertThat(res).contains(user);
        verify(userRepository).findByUsername("john");
    }

    @Test
    @DisplayName("deleteAccount: вызывает deleteById в репозитории")
    void deleteAccount_callsRepo() {
        Long userId = 12345L;
        userService.deleteAccount(userId);
        verify(userRepository).deleteById(userId);
    }
}

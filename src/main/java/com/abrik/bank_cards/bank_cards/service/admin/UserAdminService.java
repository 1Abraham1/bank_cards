package com.abrik.bank_cards.bank_cards.service.admin;

import com.abrik.bank_cards.bank_cards.dto.user.UpdateUserRequest;
import com.abrik.bank_cards.bank_cards.dto.user.UpdateUserRolesRequest;
import com.abrik.bank_cards.bank_cards.dto.user.UserDto;
import com.abrik.bank_cards.bank_cards.entity.Role;
import com.abrik.bank_cards.bank_cards.entity.User;
import com.abrik.bank_cards.bank_cards.exception.ConflictException;
import com.abrik.bank_cards.bank_cards.exception.NotFoundException;
import com.abrik.bank_cards.bank_cards.repository.RoleRepository;
import com.abrik.bank_cards.bank_cards.repository.UserRepository;
import com.abrik.bank_cards.bank_cards.util.RoleUtil;
import com.abrik.bank_cards.bank_cards.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.abrik.bank_cards.bank_cards.repository.specification.UserSpecifications.*;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAdminService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RoleUtil roleUtil;
    private final UserUtil userUtil;

    @Transactional(readOnly = true)
    public Page<UserDto> listUsers(
            String search,
            String email,
            Boolean active,
            List<String> roles,
            Pageable pageable
    ) {
        Specification<User> spec = Specification.allOf(
                searchQ(search),
                emailContains(email),
                activeEq(active),
                hasAnyRole(roles)
        );

        return userRepository.findAll(spec, pageable)
                .map(userUtil::toDto);
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: id=" + id));
        return userUtil.toDto(user);
    }

    @Transactional
    public UserDto patchUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: id=" + id));

        if (request.getEmail() != null) {
            String newEmail = request.getEmail().isBlank() ? null : request.getEmail().trim();
            if (newEmail != null && userRepository.existsByEmailAndIdNot(newEmail, id)) {
                throw new ConflictException("Email уже используется: " + newEmail);
            }
            user.setEmail(newEmail);
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().isBlank() ? null : request.getFullName().trim());
        }

        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        return userUtil.toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto replaceRoles(Long userId, UpdateUserRolesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: id=" + userId));

        String actor = currentUsername();

        // Сам себе роли менять можно, но не снимать у себя ADMIN
        boolean isSelf = user.getUsername().equalsIgnoreCase(actor);

        // Нормализуем входные роли
        List<String> canonicalRequested = request.getRoles().stream()
                .filter(Objects::nonNull)
                .map(roleUtil::canonical)
                .toList();

        if (canonicalRequested.isEmpty()) {
            throw new ConflictException("Список ролей не должен быть пустым");
        }

        // Подготовим варианты имён для поиска в БД (и с ROLE_, и без)
        Set<String> lookupLower = new HashSet<>();
        for (String c : canonicalRequested) {
            lookupLower.add(c.toLowerCase());
            lookupLower.add(("ROLE_" + c).toLowerCase());
        }

        List<Role> found = roleRepository.findByNameInIgnoreCase(lookupLower.stream().toList());

        // Проверим, что все роли существуют
        Set<String> foundCanonicals = found.stream()
                .map(r -> roleUtil.canonical(r.getName()))
                .collect(Collectors.toSet());

        List<String> missing = canonicalRequested.stream()
                .filter(c -> !foundCanonicals.contains(c))
                .toList();

        if (!missing.isEmpty()) {
            throw new NotFoundException("Не найдены роли: " + String.join(", ", missing));
        }

        // Проверки на "последнего активного админа"
        boolean userWasAdmin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> roleUtil.isAdminCanonical(roleUtil.canonical(r.getName())));
        boolean userWillBeAdmin = foundCanonicals.contains("ADMIN");

        if (userWasAdmin && !userWillBeAdmin) {
            long activeAdmins = userRepository.countActiveUsersWithRole("ADMIN")
                    + userRepository.countActiveUsersWithRole("ROLE_ADMIN"); // на случай хранения с префиксом
            // Если активный админ всего один и это он — нельзя снимать
            if (activeAdmins <= 1 && Boolean.TRUE.equals(user.getActive())) {
                throw new ConflictException("Нельзя снять роль ADMIN с последнего активного администратора");
            }
            // Сам себе снимать ADMIN нельзя
            if (isSelf) {
                throw new ConflictException("Нельзя снять у себя роль ADMIN");
            }
        }

        user.setRoles(found);
        return userUtil.toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto activate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: id=" + id));

        user.setActive(true);
        return userUtil.toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto deactivate(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден: id=" + id));

        String actor = currentUsername();
        if (user.getUsername().equalsIgnoreCase(actor)) {
            throw new ConflictException("Нельзя заблокировать самого себя");
        }

        boolean isAdmin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> roleUtil.isAdminCanonical(roleUtil.canonical(r.getName())));

        if (isAdmin && Boolean.TRUE.equals(user.getActive())) {
            long activeAdmins = userRepository.countActiveUsersWithRole("ADMIN")
                    + userRepository.countActiveUsersWithRole("ROLE_ADMIN");
            if (activeAdmins <= 1) {
                throw new ConflictException("Нельзя заблокировать последнего активного администратора");
            }
        }

        user.setActive(false);
        return userUtil.toDto(userRepository.save(user));
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? String.valueOf(auth.getName()) : "unknown";
    }
}

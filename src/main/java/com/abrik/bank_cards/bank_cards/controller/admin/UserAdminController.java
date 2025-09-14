package com.abrik.bank_cards.bank_cards.controller.admin;

import com.abrik.bank_cards.bank_cards.dto.user.UpdateUserRequest;
import com.abrik.bank_cards.bank_cards.dto.user.UpdateUserRolesRequest;
import com.abrik.bank_cards.bank_cards.dto.user.UserDto;
import com.abrik.bank_cards.bank_cards.service.admin.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {
    private final UserAdminService userAdminService;

    @GetMapping
    public Page<UserDto> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) List<String> roles,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return userAdminService.listUsers(search, email, active, roles, pageable);
    }

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable Long id) {
        return userAdminService.getUser(id);
    }

    @PatchMapping("/{id}")
    public UserDto patchUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userAdminService.patchUser(id, request);
    }

    @PatchMapping("/{id}/roles")
    public UserDto replaceRoles(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRolesRequest request
    ) {
        return userAdminService.replaceRoles(id, request);
    }

    @PostMapping("/{id}/activate")
    public UserDto activate(@PathVariable Long id) {
        return userAdminService.activate(id);
    }

    @PostMapping("/{id}/deactivate")
    public UserDto deactivate(@PathVariable Long id) {
        return userAdminService.deactivate(id);
    }
}

package com.abrik.bank_cards.bank_cards.service;

import com.abrik.bank_cards.bank_cards.entity.Role;
import com.abrik.bank_cards.bank_cards.exception.RoleNotFoundException;
import com.abrik.bank_cards.bank_cards.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    public Role getUserRole() {
        return roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RoleNotFoundException("User Role Not Found"));
    }
}

package com.abrik.bank_cards.bank_cards.repository.specification;

import com.abrik.bank_cards.bank_cards.entity.User;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class UserSpecifications {
    private UserSpecifications() {}

    public static Specification<User> emailContains(String emailPart) {
        if (emailPart == null || emailPart.isBlank()) return null;
        return (root, search, cb) -> cb.like(cb.lower(root.get("email")), "%" + emailPart.toLowerCase() + "%");
    }

    public static Specification<User> searchQ(String qStr) {
        if (qStr == null || qStr.isBlank()) return null;
        return (root, search, cb) -> {
            String like = "%" + qStr.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("username")), like),
                    cb.like(cb.lower(root.get("email")), like),
                    cb.like(cb.lower(root.get("fullName")), like)
            );
        };
    }

    public static Specification<User> activeEq(Boolean active) {
        if (active == null) return null;
        return (root, search, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<User> hasAnyRole(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) return null;
        return (root, search, cb) -> {
            var rolesJoin = root.join("roles", JoinType.LEFT);
            search.distinct(true);
            var namesLower = roleNames.stream().map(String::toLowerCase).toList();
            return cb.lower(rolesJoin.get("name")).in(namesLower);
        };
    }
}

package com.abrik.bank_cards.bank_cards.repository;

import com.abrik.bank_cards.bank_cards.entity.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
    Optional<User> findByUsername(String username);

    void deleteByUsername(String username);
}

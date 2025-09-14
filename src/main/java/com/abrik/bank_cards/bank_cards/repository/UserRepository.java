package com.abrik.bank_cards.bank_cards.repository;

import com.abrik.bank_cards.bank_cards.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("""
           select count(distinct u.id)
           from User u
           join u.roles r
           where lower(r.name) = lower(:roleName)
             and u.active = true
           """)
    long countActiveUsersWithRole(@Param("roleName") String roleName);
}

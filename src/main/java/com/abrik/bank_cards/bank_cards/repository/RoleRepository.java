package com.abrik.bank_cards.bank_cards.repository;

import com.abrik.bank_cards.bank_cards.entity.Role;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends CrudRepository<Role, Integer> {
    Optional<Role> findByName(String name);

    @Query("select r from Role r where lower(r.name) in :names")
    List<Role> findByNameInIgnoreCase(@Param("names") List<String> lowerNames);
}

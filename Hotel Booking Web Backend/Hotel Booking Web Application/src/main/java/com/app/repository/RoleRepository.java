package com.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.model.Role;

import java.util.Optional;

/**
 * @author Simpson Alfred
 */

public interface RoleRepository extends JpaRepository<Role, Long> 
{
	
    Optional<Role> findByName(String role);

    boolean existsByName(String role);
}

package com.truerize.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.truerize.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
 
    Optional<User> findByEmail(String email);
   
    boolean existsByEmail(String email);
    
    List<User> findByStatus(String status);
    
    List<User> findByRoles_Name(String roleName);
   
    long countByStatus(String status);
   
    Optional<User> findByEmailAndPassword(String email, String password);
}
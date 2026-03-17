package com.truerize.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.truerize.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
   
    Optional<User> findById(Integer id);
   
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.assignedExams WHERE u.id = :id")
    Optional<User> findByIdWithExams(@Param("id") Integer id);
   
    @Query("SELECT DISTINCT u FROM User u JOIN u.assignedExams e WHERE e.id = :examId")
    List<User> findByAssignedExamsId(@Param("examId") Integer examId);
    
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);
    
    Optional<User> findByEmailAndPassword(String email, String password);
   
    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);
    
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.assignedExams LEFT JOIN FETCH u.roles")
    List<User> findAllWithExamsAndRoles();
}

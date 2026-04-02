package com.truerize.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.truerize.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
   
    Optional<User> findById(Integer id);
   
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.assignedExams LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<User> findByIdWithExams(@Param("id") Integer id);
   
    @Query("SELECT DISTINCT u FROM User u JOIN u.assignedExams e WHERE e.id = :examId")
    List<User> findByAssignedExamsId(@Param("examId") Integer examId);
    
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);
    
    Optional<User> findByEmailAndPassword(String email, String password);
   
    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.slot = null WHERE u.slot.id = :slotId")
    int clearSlotAssignments(@Param("slotId") Integer slotId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM user_assigned_exams WHERE user_id = :userId", nativeQuery = true)
    int deleteAssignedExamMappings(@Param("userId") Integer userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM user_roles WHERE user_id = :userId", nativeQuery = true)
    int deleteRoleMappings(@Param("userId") Integer userId);
    
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.assignedExams LEFT JOIN FETCH u.roles")
    List<User> findAllWithExamsAndRoles();
}

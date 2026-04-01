package com.truerize.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.truerize.entity.Result;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {
    
   
    @Query("SELECT r FROM Result r WHERE r.slot.id = :slotId ORDER BY r.score DESC")
    List<Result> findBySlotId(@Param("slotId") Integer slotId);
    
    @Query("SELECT r FROM Result r WHERE r.slot.slotNumber = :slotNumber ORDER BY r.score DESC")
    List<Result> findBySlotNumber(@Param("slotNumber") Integer slotNumber);
  
    @Query("SELECT r FROM Result r JOIN FETCH r.slot s ORDER BY s.slotNumber ASC, r.score DESC")
    List<Result> findAllWithSlotOrderedBySlotAndScore();
    
    List<Result> findByEmail(String email);
    
    long countBySlot_Id(Integer slotId);
   
    @Query("SELECT COUNT(r) FROM Result r WHERE r.slot.id = :slotId AND r.score >= :passingScore")
    long countPassedBySlotId(@Param("slotId") Integer slotId, @Param("passingScore") int passingScore);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Result r WHERE r.slot.id = :slotId")
    int deleteBySlotId(@Param("slotId") Integer slotId);
}

package com.truerize.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.truerize.entity.Slot;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Integer> {
    
   
    Optional<Slot> findBySlotNumber(Integer slotNumber);
   
    boolean existsBySlotNumber(Integer slotNumber);
   
    @Query("SELECT s FROM Slot s ORDER BY s.slotNumber ASC")
    List<Slot> findAllOrderedBySlotNumber();
}
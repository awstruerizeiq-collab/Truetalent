package com.truerize.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.truerize.entity.Slot;
import com.truerize.service.SlotService;

@RestController
@RequestMapping("/api/admin/slots")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class SlotController {

    private static final Logger log = LoggerFactory.getLogger(SlotController.class);

    @Autowired
    private SlotService slotService;

    @GetMapping
    public ResponseEntity<List<Slot>> getAllSlots() {
        log.info("Fetching all slots");
        return ResponseEntity.ok(slotService.getAllSlots());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSlot(@PathVariable Integer id) {
        log.info("Fetching slot with id: {}", id);
        Optional<Slot> slot = slotService.getSlotById(id);
        return slot.isPresent() ?
            ResponseEntity.ok(slot.get()) :
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Slot not found"));
    }

    @PostMapping
    public ResponseEntity<?> createSlot(@RequestBody Slot slot) {
        try {
            log.info("Received request to create slot: slotNumber={}, collegeName={}, date={}, time={}, passPercentage={}, hasPassword={}", 
                    slot.getSlotNumber(), 
                    slot.getCollegeName() != null ? slot.getCollegeName() : "N/A",
                    slot.getDate(), 
                    slot.getTime(),
                    slot.getPassPercentage(),
                    slot.getSlotPassword() != null && !slot.getSlotPassword().isBlank());
            Slot createdSlot = slotService.createSlot(slot);
            log.info("Successfully created slot with id: {}", createdSlot.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSlot);
        } catch (IllegalArgumentException e) {
            log.error("Failed to create slot: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error creating slot: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create slot: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSlot(@PathVariable Integer id, @RequestBody Slot slotDetails) {
        try {
            log.info("Received request to update slot {}: slotNumber={}, collegeName={}, date={}, time={}, passPercentage={}, hasPassword={}", 
                    id, 
                    slotDetails.getSlotNumber(), 
                    slotDetails.getCollegeName() != null ? slotDetails.getCollegeName() : "N/A",
                    slotDetails.getDate(), 
                    slotDetails.getTime(),
                    slotDetails.getPassPercentage(),
                    slotDetails.getSlotPassword() != null && !slotDetails.getSlotPassword().isBlank());
            Slot updatedSlot = slotService.updateSlot(id, slotDetails);
            log.info("Successfully updated slot {}", id);
            return ResponseEntity.ok(updatedSlot);
        } catch (IllegalArgumentException e) {
            log.error("Failed to update slot {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating slot {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update slot: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSlot(@PathVariable Integer id) {
        try {
            log.info("Received request to delete slot {}", id);
            slotService.deleteSlot(id);
            log.info("Successfully deleted slot {}", id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Slot deleted", "slotId", id));
        } catch (IllegalArgumentException e) {
            log.error("Failed to delete slot {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error deleting slot {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete slot: " + e.getMessage()));
        }
    }
}

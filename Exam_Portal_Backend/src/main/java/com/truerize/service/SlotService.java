package com.truerize.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.truerize.entity.Slot;
import com.truerize.repository.SlotRepository;

@Service
public class SlotService {

    private static final Logger log = LoggerFactory.getLogger(SlotService.class);

    @Autowired
    private SlotRepository slotRepository;

    public List<Slot> getAllSlots() {
        return slotRepository.findAll();
    }

    public Optional<Slot> getSlotById(Integer id) {
        return slotRepository.findById(id);
    }

    public Optional<Slot> getSlotBySlotNumber(Integer slotNumber) {
        return slotRepository.findBySlotNumber(slotNumber);
    }

    @Transactional
    public Slot createSlot(Slot slot) {
        if (slot.getSlotNumber() == null) {
            throw new IllegalArgumentException("Slot number is required");
        }
        if (slot.getDate() == null) {
            throw new IllegalArgumentException("Date is required");
        }
        if (slot.getTime() == null) {
            throw new IllegalArgumentException("Time is required");
        }
        if (slot.getSlotPassword() == null || slot.getSlotPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Slot password is required");
        }
        if (slotRepository.existsBySlotNumber(slot.getSlotNumber())) {
            throw new IllegalArgumentException("Slot number " + slot.getSlotNumber() + " already exists");
        }

        slot.setSlotPassword(slot.getSlotPassword().trim());

        log.info("Creating slot number: {} for college: {} on date: {} at time: {}",
                slot.getSlotNumber(),
                slot.getCollegeName() != null ? slot.getCollegeName() : "N/A",
                slot.getDate(),
                slot.getTime());

        Slot createdSlot = slotRepository.save(slot);
        log.info("Slot created successfully with ID: {}", createdSlot.getId());
        return createdSlot;
    }

    @Transactional
    public Slot updateSlot(Integer id, Slot slotDetails) {
        Slot slot = slotRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Slot not found with id: " + id));

        log.info("Updating slot ID: {}. Current values - slotNumber: {}, collegeName: {}, date: {}, time: {}, hasPassword: {}",
                id, slot.getSlotNumber(), slot.getCollegeName(), slot.getDate(), slot.getTime(),
                slot.getSlotPassword() != null && !slot.getSlotPassword().isBlank());

        boolean hasChanges = false;

        if (slotDetails.getSlotNumber() != null && !slotDetails.getSlotNumber().equals(slot.getSlotNumber())) {
            Optional<Slot> existing = slotRepository.findBySlotNumber(slotDetails.getSlotNumber());
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new IllegalArgumentException("Slot number " + slotDetails.getSlotNumber() + " already exists");
            }
            slot.setSlotNumber(slotDetails.getSlotNumber());
            hasChanges = true;
        }

        if (slotDetails.getCollegeName() != null && !slotDetails.getCollegeName().equals(slot.getCollegeName())) {
            slot.setCollegeName(slotDetails.getCollegeName());
            hasChanges = true;
        } else if (slotDetails.getCollegeName() == null && slot.getCollegeName() != null) {
            slot.setCollegeName(null);
            hasChanges = true;
        }

        if (slotDetails.getDate() != null && !slotDetails.getDate().equals(slot.getDate())) {
            slot.setDate(slotDetails.getDate());
            hasChanges = true;
        }

        if (slotDetails.getTime() != null && !slotDetails.getTime().equals(slot.getTime())) {
            slot.setTime(slotDetails.getTime());
            hasChanges = true;
        }

        if (slotDetails.getSlotPassword() != null) {
            String newPassword = slotDetails.getSlotPassword().trim();
            if (newPassword.isEmpty()) {
                throw new IllegalArgumentException("Slot password is required");
            }
            if (!newPassword.equals(slot.getSlotPassword())) {
                slot.setSlotPassword(newPassword);
                hasChanges = true;
            }
        }

        if (slot.getSlotPassword() == null || slot.getSlotPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Slot password is required");
        }

        if (!hasChanges) {
            log.info("No changes detected for slot {}", id);
            return slot;
        }

        Slot updatedSlot = slotRepository.save(slot);
        log.info("Slot updated successfully. Slot ID: {}", updatedSlot.getId());

        int userCount = updatedSlot.getUsers() != null ? updatedSlot.getUsers().size() : 0;
        if (userCount > 0) {
            log.warn("{} user(s) are assigned to this slot. Time changes affect their login window.",
                    userCount);
        }

        return updatedSlot;
    }

    @Transactional
    public void deleteSlot(Integer id) {
        Slot slot = slotRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Slot not found with id: " + id));

        int userCount = slot.getUsers() != null ? slot.getUsers().size() : 0;
        log.info("Deleting slot {} (slot number: {}) with {} user(s)",
                id, slot.getSlotNumber(), userCount);

        if (userCount > 0) {
            log.warn("Deleting slot with {} user(s) assigned. Users lose slot assignment.", userCount);
        }

        slotRepository.delete(slot);
        log.info("Slot deleted successfully");
    }

    public long countSlots() {
        return slotRepository.count();
    }
}

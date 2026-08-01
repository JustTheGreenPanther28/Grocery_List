package com.grocery.repository;

import com.grocery.model.ScheduledSend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScheduledSendRepository extends JpaRepository<ScheduledSend, Long> {
    Optional<ScheduledSend> findByUsername(String username);
    List<ScheduledSend> findByEnabledTrue();
}

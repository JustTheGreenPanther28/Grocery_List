package com.grocery.repository;

import com.grocery.model.SentMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SentMessageRepository extends JpaRepository<SentMessage, Long> {
    List<SentMessage> findByUsernameOrderBySentAtDesc(String username);
    List<SentMessage> findByUsernameAndItemDateOrderBySentAtDesc(String username, LocalDate itemDate);
}

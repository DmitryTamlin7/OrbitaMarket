package org.example.repository;

import org.example.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutBoxEventRepository extends JpaRepository<OutBoxEventRepository, UUID> {
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();
}

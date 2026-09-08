package org.fleet.backend.repository;

import org.fleet.backend.entity.PartnerWebhook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerWebhookRepository extends JpaRepository<PartnerWebhook, Long> {
    List<PartnerWebhook> findByActiveTrue();
}

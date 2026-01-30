package com.app.governance.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperationalAuditRepo extends JpaRepository<OperationalAudit, Long> {
    List<OperationalAudit> findTop50ByOrderByTimestampDesc();

    List<OperationalAudit> findByTimestampAfter(java.time.LocalDateTime timestamp);

    List<OperationalAudit> findByEntityId(String entityId);
}

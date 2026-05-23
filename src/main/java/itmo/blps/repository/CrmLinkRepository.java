package itmo.blps.repository;

import itmo.blps.entity.CrmLink;
import itmo.blps.integration.crm.CrmEntityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrmLinkRepository extends JpaRepository<CrmLink, Long> {

    Optional<CrmLink> findByEntityTypeAndLocalId(CrmEntityType entityType, Long localId);

    Optional<CrmLink> findByEntityTypeAndBitrixId(CrmEntityType entityType, Integer bitrixId);
}

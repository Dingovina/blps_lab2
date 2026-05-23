package itmo.blps.entity;

import itmo.blps.integration.crm.CrmEntityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cian_crm_links")
public class CrmLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    private CrmEntityType entityType;

    @Column(name = "local_id", nullable = false)
    private Long localId;

    @Column(name = "bitrix_id", nullable = false)
    private Integer bitrixId;

    @Column(name = "last_sync_at", nullable = false)
    private Instant lastSyncAt = Instant.now();
}

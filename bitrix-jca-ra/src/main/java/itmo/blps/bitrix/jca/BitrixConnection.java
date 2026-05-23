package itmo.blps.bitrix.jca;

import itmo.blps.bitrix.jca.model.BitrixDealSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface BitrixConnection extends AutoCloseable {

    int createContact(String email, String name);

    Optional<Integer> findContactIdByEmail(String email);

    int createDeal(Map<String, Object> fields);

    void updateDeal(int dealId, Map<String, Object> fields);

    Optional<BitrixDealSnapshot> getDeal(int dealId);

    List<BitrixDealSnapshot> listDealsModifiedAfter(Instant since, int categoryId);

    int addDealActivity(int dealId, String subject, String description);

    @Override
    void close();
}

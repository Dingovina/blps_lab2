package itmo.blps.bitrix.jca;

import com.fasterxml.jackson.databind.JsonNode;
import itmo.blps.bitrix.jca.model.BitrixDealSnapshot;
import itmo.blps.bitrix.jca.rest.BitrixRestClient;
import jakarta.resource.ResourceException;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BitrixConnectionImpl implements BitrixConnection {

    private static final DateTimeFormatter BITRIX_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final BitrixRestClient client;
    private final String dealFieldListingId;
    private final String dealFieldPromotion;
    private volatile boolean closed;

    public BitrixConnectionImpl(BitrixRestClient client, String dealFieldListingId, String dealFieldPromotion) {
        this.client = client;
        this.dealFieldListingId = dealFieldListingId;
        this.dealFieldPromotion = dealFieldPromotion;
    }

    @Override
    public int createContact(String email, String name) {
        try {
            Map<String, Object> fields = new HashMap<>();
            fields.put("EMAIL", List.of(Map.of("VALUE", email, "VALUE_TYPE", "WORK")));
            if (name != null && !name.isBlank()) {
                fields.put("NAME", name);
            }
            JsonNode root = client.call("crm.contact.add", BitrixRestClient.fields(fields));
            return root.get("result").asInt();
        } catch (Exception e) {
            throw new BitrixConnectionException("crm.contact.add failed", e);
        }
    }

    @Override
    public Optional<Integer> findContactIdByEmail(String email) {
        try {
            Map<String, String> filter = Map.of("EMAIL", email);
            Map<String, Object> params = new HashMap<>();
            params.put("filter", filter);
            params.put("select", List.of("ID", "EMAIL"));
            JsonNode root = client.call("crm.contact.list", params);
            JsonNode result = root.get("result");
            if (result != null && result.isArray() && result.size() > 0) {
                return Optional.of(result.get(0).get("ID").asInt());
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new BitrixConnectionException("crm.contact.list failed", e);
        }
    }

    @Override
    public int createDeal(Map<String, Object> fields) {
        try {
            JsonNode root = client.call("crm.deal.add", BitrixRestClient.fields(fields));
            return root.get("result").asInt();
        } catch (Exception e) {
            throw new BitrixConnectionException("crm.deal.add failed", e);
        }
    }

    @Override
    public void updateDeal(int dealId, Map<String, Object> fields) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("id", dealId);
            params.put("fields", fields);
            client.call("crm.deal.update", params);
        } catch (Exception e) {
            throw new BitrixConnectionException("crm.deal.update failed", e);
        }
    }

    @Override
    public Optional<BitrixDealSnapshot> getDeal(int dealId) {
        try {
            Map<String, Object> params = Map.of("id", dealId);
            JsonNode root = client.call("crm.deal.get", params);
            JsonNode result = root.get("result");
            if (result == null || result.isNull()) {
                return Optional.empty();
            }
            return Optional.of(toSnapshot(result));
        } catch (Exception e) {
            throw new BitrixConnectionException("crm.deal.get failed", e);
        }
    }

    @Override
    public List<BitrixDealSnapshot> listDealsModifiedAfter(Instant since, int categoryId) {
        try {
            Map<String, Object> filter = new HashMap<>();
            filter.put("CATEGORY_ID", categoryId);
            if (since != null) {
                filter.put(">DATE_MODIFY", BITRIX_DATE.format(since.atOffset(ZoneOffset.UTC)));
            }
            if (dealFieldListingId != null) {
                filter.put("!" + dealFieldListingId, false);
            }
            Map<String, Object> params = new HashMap<>();
            params.put("filter", filter);
            params.put("select", List.of("ID", "TITLE", "STAGE_ID", "DATE_MODIFY", dealFieldListingId, dealFieldPromotion));
            params.put("order", Map.of("DATE_MODIFY", "ASC"));

            List<BitrixDealSnapshot> deals = new ArrayList<>();
            int start = 0;
            while (true) {
                params.put("start", start);
                JsonNode root = client.call("crm.deal.list", params);
                JsonNode result = root.get("result");
                if (result == null || !result.isArray() || result.isEmpty()) {
                    break;
                }
                for (JsonNode node : result) {
                    deals.add(toSnapshot(node));
                }
                if (!root.has("next") || root.get("next").isNull()) {
                    break;
                }
                start = root.get("next").asInt();
            }
            return deals;
        } catch (Exception e) {
            throw new BitrixConnectionException("crm.deal.list failed", e);
        }
    }

    @Override
    public int addDealActivity(int dealId, String subject, String description) {
        try {
            Map<String, Object> fields = new HashMap<>();
            fields.put("OWNER_TYPE_ID", 2);
            fields.put("OWNER_ID", dealId);
            fields.put("TYPE_ID", 1);
            fields.put("SUBJECT", subject);
            fields.put("DESCRIPTION", description);
            fields.put("COMPLETED", "N");
            JsonNode root = client.call("crm.activity.add", BitrixRestClient.fields(fields));
            return root.get("result").asInt();
        } catch (Exception e) {
            throw new BitrixConnectionException("crm.activity.add failed", e);
        }
    }

    @Override
    public void close() {
        closed = true;
    }

    boolean isClosed() {
        return closed;
    }

    private BitrixDealSnapshot toSnapshot(JsonNode node) {
        int id = node.get("ID").asInt();
        String stageId = node.has("STAGE_ID") ? node.get("STAGE_ID").asText() : null;
        String title = node.has("TITLE") ? node.get("TITLE").asText() : null;
        Instant dateModify = null;
        if (node.has("DATE_MODIFY") && !node.get("DATE_MODIFY").isNull()) {
            String raw = node.get("DATE_MODIFY").asText();
            try {
                dateModify = Instant.parse(raw);
            } catch (Exception ignored) {
                dateModify = Instant.now();
            }
        }
        Map<String, Object> fields = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            fields.put(e.getKey(), jsonToObject(e.getValue()));
        }
        return new BitrixDealSnapshot(id, stageId, title, dateModify, fields);
    }

    private static Object jsonToObject(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isTextual()) {
            return node.asText();
        }
        return node.toString();
    }

    static class BitrixConnectionException extends RuntimeException {
        BitrixConnectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

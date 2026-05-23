package itmo.blps.bitrix.jca.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

public class BitrixDealSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final String stageId;
    private final String title;
    private final Instant dateModify;
    private final Map<String, Object> fields;

    public BitrixDealSnapshot(int id, String stageId, String title, Instant dateModify, Map<String, Object> fields) {
        this.id = id;
        this.stageId = stageId;
        this.title = title;
        this.dateModify = dateModify;
        this.fields = fields;
    }

    public int getId() {
        return id;
    }

    public String getStageId() {
        return stageId;
    }

    public String getTitle() {
        return title;
    }

    public Instant getDateModify() {
        return dateModify;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public Object getField(String name) {
        return fields != null ? fields.get(name) : null;
    }
}

package itmo.blps.bitrix.jca;

import itmo.blps.bitrix.jca.model.BitrixDealSnapshot;

import java.io.Serializable;

public class BitrixEventRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private final BitrixDealSnapshot deal;

    public BitrixEventRecord(BitrixDealSnapshot deal) {
        this.deal = deal;
    }

    public BitrixDealSnapshot getDeal() {
        return deal;
    }
}

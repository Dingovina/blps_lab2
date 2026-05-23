package itmo.blps.bitrix.jca;

import jakarta.resource.ResourceException;

public interface BitrixConnectionFactoryInterface {

    BitrixConnection getBitrixConnection() throws ResourceException;
}

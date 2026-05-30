package itmo.blps.controller;

import itmo.blps.integration.crm.BitrixWebhookHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@Profile("bitrix")
@ConditionalOnProperty(prefix = "app.bitrix", name = "enabled", havingValue = "true")
public class BitrixWebhookController {

    private static final Logger log = LoggerFactory.getLogger(BitrixWebhookController.class);

    private final BitrixWebhookHandler webhookHandler;

    public BitrixWebhookController(BitrixWebhookHandler webhookHandler) {
        this.webhookHandler = webhookHandler;
    }

    @PostMapping("/bitrix")
    public ResponseEntity<Void> handleWebhook(@RequestParam Map<String, String> params) {
        if (!webhookHandler.isValidToken(params)) {
            log.warn("Rejected Bitrix webhook: invalid application_token");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        webhookHandler.handleEventAsync(params);
        return ResponseEntity.ok().build();
    }
}

package itmo.blps.service;

import itmo.blps.config.MailProperties;
import itmo.blps.entity.NotificationType;
import itmo.blps.messaging.NotificationEvent;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

@Component
public class NotificationEmailRenderer {

    private static final Map<NotificationType, String> TYPE_LABELS = Map.ofEntries(
            Map.entry(NotificationType.LISTING_PUBLISHED, "Объявление"),
            Map.entry(NotificationType.PROMOTION_ACTIVATED, "Продвижение"),
            Map.entry(NotificationType.PROMOTION_PAYMENT_FAILED, "Оплата"),
            Map.entry(NotificationType.ARCHIVATION_SOON, "Срок размещения"),
            Map.entry(NotificationType.PUBLICATION_EXTENDED, "Публикация"),
            Map.entry(NotificationType.NEW_INQUIRY, "Обращение"),
            Map.entry(NotificationType.SHOWING_SCHEDULED, "Показ"),
            Map.entry(NotificationType.SHOWING_REJECTED, "Показ"),
            Map.entry(NotificationType.LISTING_CLOSED, "Объявление"),
            Map.entry(NotificationType.WEEKLY_STATS, "Статистика")
    );

    private static final String TEMPLATE = "email/notification";

    private final TemplateEngine templateEngine;
    private final MailProperties mailProperties;

    public NotificationEmailRenderer(TemplateEngine templateEngine, MailProperties mailProperties) {
        this.templateEngine = templateEngine;
        this.mailProperties = mailProperties;
    }

    public String renderHtml(NotificationEvent event) {
        return templateEngine.process(TEMPLATE, buildContext(event));
    }

    public String renderPlainText(NotificationEvent event) {
        String typeLabel = resolveTypeLabel(event.getType());
        StringBuilder text = new StringBuilder();
        text.append(event.getTitle()).append("\n\n");
        text.append(event.getBody()).append("\n\n");
        text.append("—\n");
        text.append(mailProperties.getBrandName());
        if (typeLabel != null) {
            text.append(" · ").append(typeLabel);
        }
        text.append("\n\n");
        text.append("Это автоматическое уведомление. Не отвечайте на это письмо.\n");
        text.append("По вопросам обращайтесь: ").append(mailProperties.getSupportEmail());
        return text.toString();
    }

    private Context buildContext(NotificationEvent event) {
        Context context = new Context(Locale.forLanguageTag("ru"));
        context.setVariable("brandName", mailProperties.getBrandName());
        context.setVariable("supportEmail", mailProperties.getSupportEmail());
        context.setVariable("title", event.getTitle());
        context.setVariable("bodyLines", splitBody(event.getBody()));
        context.setVariable("typeLabel", resolveTypeLabel(event.getType()));
        context.setVariable("year", java.time.Year.now().getValue());
        return context;
    }

    private String resolveTypeLabel(NotificationType type) {
        if (type == null) {
            return null;
        }
        return TYPE_LABELS.getOrDefault(type, "Уведомление");
    }

    private static String[] splitBody(String body) {
        if (body == null || body.isBlank()) {
            return new String[0];
        }
        return Arrays.stream(body.split("\\R"))
                .filter(line -> !line.isBlank())
                .toArray(String[]::new);
    }
}

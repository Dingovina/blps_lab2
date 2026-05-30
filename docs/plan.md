# План презентации изменений (BLPS Lab 2 / Lab 3)

Документ для защиты перед преподавателем. Описывает три реализованных блока функциональности, их архитектуру, задействованные классы и потоки данных.

---

## Содержание

1. [Еженедельная статистика по пользователю](#1-еженедельная-статистика-по-пользователю)
2. [Отправка email-уведомлений](#2-отправка-email-уведомлений)
3. [Интеграция с Bitrix24 (JCA)](#3-интеграция-с-bitrix24-jca)

---

## 1. Еженедельная статистика по пользователю

### 1.1. Назначение

Система автоматически формирует **отчёт за предыдущую календарную неделю (UTC, понедельник–воскресенье)** для каждого продавца и покупателя, сохраняет его в БД и создаёт in-app уведомление (которое затем уходит на email через общий механизм уведомлений).

Пользователь может получить последний отчёт через REST API: `GET /api/weekly`.

### 1.2. Метрики

| Роль | Метрики |
|------|---------|
| **SELLER** | опубликованные объявления, закрытые объявления, завершённые обращения, назначенные показы |
| **BUYER** | запросы на показ, запланированные показы, отклонённые показы, завершённые показы |

### 1.3. Когда генерируется отчёт

| Триггер | Класс | Условие |
|---------|-------|---------|
| По расписанию | `WeeklyStatsScheduler` | Каждый понедельник в 09:00 UTC (`app.weekly-stats.cron`) |
| При старте приложения | `WeeklyStatsStartupRunner` | Один раз после запуска (профиль `!worker`) |
| Идемпотентность | `WeeklyStatsService.generateIfAbsent` | Не создаёт дубликат для `(user_id, period_start)` |

### 1.4. Поток данных

```
WeeklyStatsScheduler / WeeklyStatsStartupRunner
  → WeeklyStatsService.generateWeeklyReports()
    → WeeklyPeriodUtil.previousCalendarWeek()   // границы недели
    → UserRepository.findByRole(SELLER | BUYER)
    → для каждого пользователя:
        → ListingRepository / InquiryRepository (агрегация COUNT)
        → WeeklyStatsRepository.save()
        → NotificationService.create(WEEKLY_STATS)
```

### 1.5. Классы по слоям

#### Сущности и enum

| Класс | Путь | Назначение |
|-------|------|------------|
| `WeeklyStats` | `entity/WeeklyStats.java` | JPA-сущность, таблица `cian_weekly_stats`. Хранит период, роль пользователя и агрегированные метрики (отдельные поля для seller/buyer). |
| `User` | `entity/User.java` | Пользователь; связь `@ManyToOne` с отчётом. |
| `UserRole` | `entity/UserRole.java` | Enum `SELLER`, `BUYER`, `ADMIN`. Определяет, какие метрики заполняются в отчёте. |
| `NotificationType` | `entity/NotificationType.java` | Содержит значение `WEEKLY_STATS` для типизации уведомления о готовности отчёта. |

#### Repository

| Класс | Путь | Назначение |
|-------|------|------------|
| `WeeklyStatsRepository` | `repository/WeeklyStatsRepository.java` | CRUD; `findTopByUser_IdOrderByPeriodStartDesc` — последний отчёт пользователя; `existsByUser_IdAndPeriodStart` — проверка дубликата. |
| `UserRepository` | `repository/UserRepository.java` | `findByRole` — выборка всех продавцов/покупателей для batch-генерации. |
| `ListingRepository` | `repository/ListingRepository.java` | `countPublishedBySellerIdAndPublishedAtBetween`, `countClosedBySellerIdAndClosedAtBetween` — метрики продавца за период. |
| `InquiryRepository` | `repository/InquiryRepository.java` | Count-запросы по seller/buyer, статусу inquiry и временному диапазону. |

#### Сервисный слой

| Класс | Путь | Назначение |
|-------|------|------------|
| `WeeklyStatsService` | `service/WeeklyStatsService.java` | **Ядро фичи.** `generateWeeklyReports()` — batch по всем seller/buyer; `buildReport()` — агрегация; `getLatestForUser()` — для API; `formatNotificationBody()` — текст уведомления на русском. |
| `NotificationService` | `service/NotificationService.java` | Вызывается после сохранения отчёта: создаёт запись в `cian_notifications` и публикует событие в JMS-очередь для email (см. раздел 2). |

#### REST API

| Класс | Путь | Назначение |
|-------|------|------------|
| `WeeklyStatsController` | `controller/WeeklyStatsController.java` | `GET /api/weekly` — возвращает последний отчёт текущего пользователя. Требует `PRIV_BASIC_ACCESS`; для `ADMIN` возвращает 403. |
| `WeeklyStatsResponse` | `dto/WeeklyStatsResponse.java` | DTO ответа API; статический метод `from(WeeklyStats)` маппит entity → JSON. |

#### Утилиты

| Класс | Путь | Назначение |
|-------|------|------------|
| `WeeklyPeriodUtil` | `util/WeeklyPeriodUtil.java` | Вычисляет границы **предыдущей** календарной недели в UTC: `Period(start, end)` — понедельник 00:00:00 … воскресенье 23:59:59.999999999. |

#### Планировщики и bootstrap

| Класс | Путь | Назначение |
|-------|------|------------|
| `WeeklyStatsScheduler` | `config/WeeklyStatsScheduler.java` | `@Scheduled(cron = "${app.weekly-stats.cron}")` → вызывает `generateWeeklyReports()`. Профиль `!worker` (не запускается на email-worker). |
| `WeeklyStatsStartupRunner` | `config/WeeklyStatsStartupRunner.java` | `ApplicationRunner` @Order(2) — генерация при старте для демо/разработки. |
| `CianApplication` | `CianApplication.java` | `@EnableScheduling` — включает Spring Scheduler. |

#### БД и конфигурация

| Артефакт | Путь | Назначение |
|----------|------|------------|
| Таблица `cian_weekly_stats` | `src/sql/schema.sql` | Колонки метрик, `UNIQUE (user_id, period_start)`, индекс по user+period. |
| Cron-выражение | `src/main/resources/application.yml` | `app.weekly-stats.cron: "0 0 9 * * MON"` |
| Security | `config/SecurityConfig.java` | Маршрут `/api/weekly` зарегистрирован как известный API-path. |

### 1.6. Что показать на защите

- Postman / curl: `GET /api/weekly` под seller1 и buyer1.
- Логи: `WeeklyStatsScheduler` / `WeeklyStatsStartupRunner` при генерации.
- Таблица `cian_weekly_stats` в PostgreSQL.
- Связь с email: после генерации появляется notification типа `WEEKLY_STATS` и письмо в Mailpit.

---

## 2. Отправка email-уведомлений

### 2.1. Назначение

Асинхронная доставка email при бизнес-событиях: публикация объявления, обращение, показ, promotion, архивация, еженедельная статистика и др.

Архитектура: **сначала in-app notification в БД, затем событие в JMS-очередь (RabbitMQ), отдельный worker-процесс отправляет SMTP**.

### 2.2. Поток данных

```
Domain Service (ListingService, InquiryService, …)
  → NotificationService.create()
      → INSERT cian_notifications
      → afterCommit → NotificationEventPublisher.publish()
          → RabbitMQ queue (JMS)
              → NotificationEventListener (@Profile worker)
                  → EmailNotificationService.send()
                      → JavaMailSender → SMTP (Mailpit / prod)
```

### 2.3. Разделение процессов (Docker)

| Контейнер | Профиль | Роль |
|-----------|---------|------|
| `cian-app` | `docker,api` | REST API, создание notifications, publish в очередь, schedulers |
| `cian-worker-1/2` | `docker,worker` | Только `@JmsListener`, отправка email; HTTP отключён |
| `cian-rabbitmq` | — | Брокер сообщений |
| `cian-mailpit` | — | SMTP + web UI для dev (`localhost:8025`) |

### 2.4. Классы по слоям

#### Сущности

| Класс | Путь | Назначение |
|-------|------|------------|
| `Notification` | `entity/Notification.java` | JPA-сущность `cian_notifications`: user, type, title, body, related entity, read flag, createdAt. |
| `NotificationType` | `entity/NotificationType.java` | Типы: `LISTING_PUBLISHED`, `NEW_INQUIRY`, `SHOWING_SCHEDULED`, `PROMOTION_ACTIVATED`, `ARCHIVATION_SOON`, `WEEKLY_STATS` и др. |
| `RelatedEntityType` | `entity/RelatedEntityType.java` | Связь уведомления с `LISTING`, `INQUIRY` или `PAYMENT`. |
| `User` | `entity/User.java` | Email пользователя копируется в `NotificationEvent` для отправки письма. |

#### Repository

| Класс | Путь | Назначение |
|-------|------|------------|
| `NotificationRepository` | `repository/NotificationRepository.java` | Пагинация по user/read; `existsByUser_IdAndTypeAndRelatedEntity...` — дедупликация (например, `ARCHIVATION_SOON`). |

#### Сервисы

| Класс | Путь | Назначение |
|-------|------|------------|
| `NotificationService` | `service/NotificationService.java` | **Центральный оркестратор.** `create()` — сохраняет notification; `publishEmailEventAfterCommit()` — публикует в JMS только после commit транзакции; `findByUserId`, `markRead` — для API. |
| `EmailNotificationService` | `service/EmailNotificationService.java` | Формирует `SimpleMailMessage` (from, to, subject, body) и отправляет через `JavaMailSender`. Профиль `worker`. |

#### Производители уведомлений (вызывают `NotificationService.create`)

| Класс | Путь | Когда создаёт notification |
|-------|------|----------------------------|
| `ListingService` | `service/ListingService.java` | Публикация, продление, закрытие, архивация объявления. |
| `InquiryService` | `service/InquiryService.java` | Новое обращение, назначение/отклонение показа. |
| `PaymentService` | `service/PaymentService.java` | Успешная/неуспешная оплата promotion. |
| `WeeklyStatsService` | `service/WeeklyStatsService.java` | Готовность еженедельного отчёта. |
| `ListingScheduler` | `config/ListingScheduler.java` | Предупреждение об архивации (`ARCHIVATION_SOON`); авто-архивация просроченных listing. |

#### Messaging (JMS поверх RabbitMQ)

| Класс | Путь | Назначение |
|-------|------|------------|
| `NotificationEvent` | `messaging/NotificationEvent.java` | Serializable DTO для очереди: eventId, notificationId, userId, userEmail, type, title, body, related entity. |
| `NotificationEventPublisher` | `messaging/NotificationEventPublisher.java` | `JmsTemplate.convertAndSend()` в очередь `app.messaging.notifications-queue`. |
| `NotificationEventListener` | `messaging/NotificationEventListener.java` | `@JmsListener` — consumer на worker-профиле; делегирует в `EmailNotificationService`. |

#### Конфигурация

| Кlass / файл | Путь | Назначение |
|--------------|------|------------|
| `JmsConfig` | `config/JmsConfig.java` | `@EnableJms`; `RMQConnectionFactory` (RabbitMQ); Jackson JSON `MessageConverter`; `JmsTemplate`; `jmsListenerContainerFactory` с concurrency. |
| `MessagingProperties` | `config/MessagingProperties.java` | `app.messaging.*`: имя очереди, concurrency listener, параметры RabbitMQ. |
| `MailConfig` | `config/MailConfig.java` | Включает `@EnableConfigurationProperties(MailProperties.class)`. |
| `MailProperties` | `config/MailProperties.java` | `app.mail.from` — адрес отправителя. |
| `application.yml` | `resources/application.yml` | `spring.mail.*`, `app.messaging.*`, `app.mail.from`. |
| `application-worker.yml` | `resources/application-worker.yml` | `spring.main.web-application-type: none` — worker без HTTP. |
| `SecurityConfig` | `config/SecurityConfig.java` | Security отключена на worker (`@Profile("!worker")`). |

#### REST API (in-app notifications)

| Класс | Путь | Назначение |
|-------|------|------------|
| `NotificationController` | `controller/NotificationController.java` | `GET /api/notifications` — список; `PATCH /api/notifications/{id}/read` — пометить прочитанным. |
| `NotificationResponse` | `dto/NotificationResponse.java` | DTO для API. |
| `RoleAuthorityGranter` | `security/RoleAuthorityGranter.java` | Выдаёт `PRIV_MANAGE_NOTIFICATIONS` seller/buyer/admin. |

#### Инфраструктура

| Артефакт | Путь | Назначение |
|----------|------|------------|
| `cian_notifications` | `src/sql/schema.sql` | DDL таблицы и индексы. |
| `docker-compose.yml` | `docker/docker-compose.yml` | Сервисы app, worker-1/2, rabbitmq, mailpit. |
| `build.gradle` | `build.gradle` | Зависимости: `spring-boot-starter-mail`, `spring-jms`, `rabbitmq-jms`. |

### 2.5. Ключевые проектные решения

1. **After-commit publish** — email-событие не уходит в очередь, пока notification не зафиксирован в БД.
2. **Worker separation** — API и email-отправка масштабируются независимо (2 worker-реплики).
3. **Единая очередь** — все типы уведомлений идут через один pipeline; тип различается полем `NotificationType`.

### 2.6. Что показать на защите

- Mailpit UI (`http://localhost:8025`) — письма после действий в API.
- RabbitMQ Management (`http://localhost:15672`) — очередь `cian.notifications`.
- Логи worker: `Received notification email event …`.
- `GET /api/notifications` — in-app дубликат того же события.

---

## 3. Интеграция с Bitrix24 (JCA)

### 3.1. Назначение

Двусторонняя синхронизация **объявлений (listings)** с **сделками (deals)** в Bitrix24 CRM через **Jakarta Connectors (JCA) Resource Adapter**.

- **Outbound:** изменения в приложении → create/update deal в Bitrix.
- **Inbound:** polling изменённых deals → обновление listing в PostgreSQL.
- **Reconciliation:** если deal удалён в Bitrix → listing удаляется из БД.
- **Валидация inbound:** данные из Bitrix проверяются тем же `Validator`, что и REST API (`ListingCreateRequest`).

Обращения (inquiries) в Bitrix **не синхронизируются** — только объявления.

### 3.2. Архитектура

```
┌─────────────────────────────────────────────────────────────┐
│  Spring Boot (профиль bitrix, app.bitrix.enabled=true)       │
│                                                              │
│  ListingService ──afterCommit──► CrmSyncService (outbound)   │
│                                      │                       │
│  BitrixInboundSyncService ◄── poll ──┤                       │
│         │                            │                       │
│         └──► CrmSyncService (inbound)│                       │
│                                      ▼                       │
│                    BitrixJcaEmbeddedContainer                │
│                              │                               │
│                    BitrixResourceAdapter                     │
│                    BitrixConnection / REST                   │
└──────────────────────────────┼───────────────────────────────┘
                               ▼
                      Bitrix24 REST API
```

### 3.3. Модуль `bitrix-jca-ra` (JCA Resource Adapter)

#### Outbound — соединение и REST

| Класс | Путь | Назначение |
|-------|------|------------|
| `BitrixConnection` | `bitrix-jca/BitrixConnection.java` | **API адаптера** (CCI-style): create/find contact, create/update/get deal, list modified deals. |
| `BitrixConnectionImpl` | `bitrix-jca/BitrixConnectionImpl.java` | Реализация: маппинг операций на REST (`crm.contact.*`, `crm.deal.*`); парсинг JSON → `BitrixDealSnapshot`; `getDeal` возвращает empty при «Not found». |
| `BitrixRestClient` | `bitrix-jca/rest/BitrixRestClient.java` | HTTP-клиент: POST/GET к `{baseUrl}/{method}.json`, Jackson, обработка HTTP 400/error. |
| `BitrixConnectionFactoryInterface` | `bitrix-jca/BitrixConnectionFactoryInterface.java` | Spring-контракт: `getBitrixConnection()`. |
| `BitrixConnectionFactory` | `bitrix-jca/BitrixConnectionFactory.java` | JCA `ConnectionFactory`; выдаёт connection через manager или MCF. |
| `BitrixManagedConnectionFactory` | `bitrix-jca/BitrixManagedConnectionFactory.java` | JCA SPI: хранит REST URL, имена UF-полей, category id; создаёт `BitrixRestClient` + physical connection. |
| `BitrixManagedConnection` | `bitrix-jca/BitrixManagedConnection.java` | JCA SPI: обёртка над physical connection; lifecycle, cleanup. |
| `BitrixManagedConnectionMetaData` | `bitrix-jca/BitrixManagedConnectionMetaData.java` | JCA metadata (имя продукта Bitrix24, max connections). |
| `BitrixDealSnapshot` | `bitrix-jca/model/BitrixDealSnapshot.java` | DTO сделки: id, stageId, title, dateModify, map полей. |
| `BitrixDealErrors` | `bitrix-jca/BitrixDealErrors.java` | Utility: определяет «Not found» в цепочке исключений (удалённые deals). |

#### Inbound — polling и события

| Кlass | Путь | Назначение |
|-------|------|------------|
| `BitrixResourceAdapter` | `bitrix-jca/BitrixResourceAdapter.java` | **Ядро RA.** `start/stop` lifecycle; `endpointActivation` — запуск scheduler; `pollDeals()` — `listDealsModifiedAfter` + watermark; `deliverEvent` → listener; `deliverPollCycleComplete` → reconciliation. |
| `BitrixActivationSpec` | `bitrix-jca/BitrixActivationSpec.java` | JCA inbound config: interval polling (≥10s), deal category id; `validate()`. |
| `BitrixEventListener` | `bitrix-jca/BitrixEventListener.java` | SPI callback: `onDealUpdated(event)`, `onPollCycleComplete()`. |
| `BitrixEventRecord` | `bitrix-jca/BitrixEventRecord.java` | Serializable обёртка inbound-события с `BitrixDealSnapshot`. |

#### Embedded container

| Класс | Путь | Назначение |
|-------|------|------------|
| `BitrixJcaEmbeddedContainer` | `bitrix-jca/embedded/BitrixJcaEmbeddedContainer.java` | Запускает RA + MCF **внутри Spring Boot** (без WildFly); dynamic proxy для `MessageEndpoint`; `activateInbound(listener, interval, category)`. |

#### Deployment descriptor

| Файл | Путь | Назначение |
|------|------|------------|
| `ra.xml` | `bitrix-jca-ra/src/main/resources/META-INF/ra.xml` | Connector 1.6 descriptor: outbound connection defs + inbound listener/activation spec. |

### 3.4. Spring-приложение — интеграция и sync

#### Конфигурация и lifecycle

| Класс | Путь | Назначение |
|-------|------|------------|
| `BitrixProperties` | `config/BitrixProperties.java` | `@ConfigurationProperties("app.bitrix")`: enabled, REST URL, polling interval, UF-поля, stage mapping, backfill toggle. |
| `application-bitrix.yml` | `resources/application-bitrix.yml` | Defaults и env-переменные для Bitrix. |
| `BitrixJcaConfig` | `config/BitrixJcaConfig.java` | `@Profile("bitrix")`: создаёт/закрывает `BitrixJcaEmbeddedContainer`; bean `BitrixConnectionFactoryInterface`. |
| `BitrixInboundActivator` | `config/BitrixInboundActivator.java` | `@Order(200)` on `ApplicationReadyEvent`: `activateInbound(BitrixInboundSyncService, interval, category)`. |
| `BitrixStartupBackfill` | `config/BitrixStartupBackfill.java` | `@Order(100)`: optional backfill listing → Bitrix; затем `reconcileDeletedDeals()`. |

#### Синхронизация (ядро бизнес-логики)

| Класс | Путь | Назначение |
|-------|------|------------|
| `CrmSyncService` | `integration/crm/CrmSyncService.java` | **Центральный сервис sync.** См. детализацию ниже. |
| `BitrixInboundSyncService` | `integration/crm/BitrixInboundSyncService.java` | Реализует `BitrixEventListener`; `onDealUpdated` → `applyInboundDeal`; `onPollCycleComplete` → `reconcileDeletedDeals`. |
| `BitrixSyncContext` | `integration/crm/BitrixSyncContext.java` | ThreadLocal-флаг inbound; блокирует outbound sync внутри inbound-контекста (защита от циклов). |
| `CrmSyncHelper` | `integration/crm/CrmSyncHelper.java` | `afterCommit(Runnable)` — outbound sync после commit транзакции; ошибки Bitrix не ломают HTTP-ответ. |
| `CrmEntityType` | `integration/crm/CrmEntityType.java` | Enum связей: `USER` (контакт продавца), `LISTING` (deal). |

#### CRM links (маппинг local id ↔ Bitrix id)

| Класс | Путь | Назначение |
|-------|------|------------|
| `CrmLink` | `entity/CrmLink.java` | JPA `cian_crm_links`: entityType, localId, bitrixId, lastSyncAt. |
| `CrmLinkRepository` | `repository/CrmLinkRepository.java` | Lookup/delete по entity type + local/bitrix id; `findAllByEntityType` для reconciliation. |

#### Outbound-триггеры в domain-сервисах

| Класс | Путь | Назначение |
|-------|------|------------|
| `ListingService` | `service/ListingService.java` | `crmSyncHelper.afterCommit(() -> crmSyncService.syncListingStatus(...))` при create/update/publish/archive/close. |
| `PaymentService` | `service/PaymentService.java` | `syncListingPromotion` после успешной оплаты promotion. |

### 3.5. `CrmSyncService` — детализация методов

| Метод | Направление | Что делает |
|-------|-------------|------------|
| `syncListingStatus` | Outbound | Upsert deal для listing: contact продавца, поля deal, stage по status. |
| `syncListingPublished` | Outbound | Аналогично; create deal если link отсутствует. |
| `syncListingPromotion` | Outbound | Обновляет только UF-поле promotion в deal. |
| `applyInboundDeal` | Inbound | Принимает `BitrixEventRecord`; валидирует prospective state; применяет title, price, description, address, area, rooms, status; save listing. |
| `reconcileDeletedDeals` | Inbound | Для каждого `CrmLink(LISTING)`: если `getDeal` → empty, удаляет listing из БД. |
| `deleteListingByBitrixDeletion` | Inbound | Удаляет `crm_link` + listing (cascade inquiries/payments). |
| `backfillExistingData` | Outbound | При старте: sync всех listing в Bitrix. |
| `revertDealInBitrix` | Outbound | При failed inbound validation: записывает текущее состояние БД обратно в deal. |

#### Inbound-валидация (важная часть реализации)

1. `listingToRequest(listing)` — текущее состояние как `ListingCreateRequest`.
2. `overlayInboundListingFields(prospective, listing, deal)` — dry-run изменений из Bitrix на DTO **без мутации entity**.
3. `validator.validate(prospective)` — Jakarta Bean Validation (те же правила, что REST API).
4. При ошибке → `revertDealInBitrix(listing)`; listing в БД не меняется.
5. При успехе → `applyInbound*` внутри `BitrixSyncContext.runInbound`.

#### Маппинг полей listing ↔ deal

| Listing | Bitrix deal field |
|---------|-------------------|
| title | `TITLE` |
| price | `OPPORTUNITY` |
| description | `COMMENTS` |
| address | UF (`deal-field-address`) |
| area | UF (`deal-field-area`) |
| rooms | UF (`deal-field-rooms`) |
| status | `STAGE_ID` ↔ configurable stages |
| seller | `CONTACT_ID` (через `ensureContact`) |
| listing id | UF (`deal-field-listing-id`) |

### 3.6. Потоки для демонстрации

#### Outbound (приложение → Bitrix)

```
PUT /api/listings/{id}
  → ListingService.update() → save в PostgreSQL
  → afterCommit → CrmSyncService.syncListingStatus()
    → upsertDealForListing() → crm.deal.update / crm.deal.add
    → CrmLink сохраняется/обновляется
```

#### Inbound (Bitrix → приложение)

```
BitrixResourceAdapter.pollDeals() каждые N секунд
  → crm.deal.list (>DATE_MODIFY)
  → BitrixInboundSyncService.onDealUpdated()
    → CrmSyncService.applyInboundDeal()
      → validate → applyInbound* → save
  → onPollCycleComplete()
    → reconcileDeletedDeals()
```

#### Удаление deal в Bitrix

```
Менеджер удаляет deal в Bitrix CRM
  → reconcileDeletedDeals(): getDeal → Not found
    → deleteListingByBitrixDeletion()
  ИЛИ
  → update через API: updateDeal → Not found
    → deleteListingByBitrixDeletion()
```

### 3.7. Что показать на защите

1. **JCA-структура:** модуль `bitrix-jca-ra`, `ra.xml`, SPI-классы vs API-классы.
2. **Outbound:** изменить listing через API → deal обновился в Bitrix.
3. **Inbound:** изменить price/title в Bitrix → через poll listing обновился в БД.
4. **Валидация:** отправить невалидный price (< 1) из Bitrix → reject + revert в Bitrix.
5. **Удаление:** удалить deal в Bitrix → listing исчез из приложения после poll/reconcile.
6. **Таблица `cian_crm_links`:** связь listing id ↔ deal id.

---

## 4. Связи между тремя блоками

| Связь | Описание |
|-------|----------|
| Weekly stats → Email | `WeeklyStatsService` создаёт `WEEKLY_STATS` notification → общий email pipeline. |
| Listing/Inquiry events → Email | Domain services создают notifications независимо от Bitrix. |
| Listing changes → Bitrix | `ListingService` триггерит outbound sync; Bitrix inbound может изменить listing обратно. |
| Bitrix ≠ Inquiries | Обращения живут только в приложении; в Bitrix только deals (listings). |

---

## 5. Рекомендуемый порядок презентации (15–20 мин)

1. **Общая архитектура** (1–2 мин): Spring Boot, PostgreSQL, RabbitMQ, Mailpit, Docker profiles (`api` / `worker` / `bitrix`).
2. **Email-уведомления** (5 мин): pipeline, worker separation, demo Mailpit.
3. **Еженедельная статистика** (4 мин): метрики, scheduler, API `/api/weekly`.
4. **Bitrix JCA** (7–8 мин): RA module, outbound/inbound, validation, deletion sync, live demo.
5. **Вопросы.**

---

## 6. Полезные команды для demo

```bash
# Запуск
./docker/scripts/up.sh

# Логи
docker compose -f docker/docker-compose.yml logs -f app
docker compose -f docker/docker-compose.yml logs -f worker-1

# UI
# API:        http://localhost:8080/swagger-ui/index.html
# Mailpit:    http://localhost:8025
# RabbitMQ:   http://localhost:15672  (cian/cian)

# PostgreSQL
psql -h localhost -p 5433 -U cian -d cian
# SELECT * FROM cian_weekly_stats;
# SELECT * FROM cian_crm_links;
# SELECT * FROM cian_notifications ORDER BY id DESC LIMIT 10;
```

---

## 7. Глоссарий

| Термин | Значение |
|--------|----------|
| **JCA / Jakarta Connectors** | Стандарт Java для интеграции с внешними системами (EIS) через Resource Adapter |
| **SPI** | Service Provider Interface — интерфейсы, которые реализует адаптер (`jakarta.resource.spi.*`) |
| **RA** | Resource Adapter — «драйвер» для Bitrix в модуле `bitrix-jca-ra` |
| **Outbound** | Приложение → Bitrix (create/update deal) |
| **Inbound** | Bitrix → приложение (polling + apply changes) |
| **CrmLink** | Таблица соответствия local entity id ↔ Bitrix id |
| **After-commit sync** | Bitrix/outbox-вызов только после успешного commit транзакции БД |
| **Worker profile** | Headless Spring-процесс для отправки email из JMS-очереди |

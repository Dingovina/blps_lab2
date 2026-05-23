#set document(
  title: "Лабораторная работа №3 — асинхронная обработка, распределение и интеграция EIS",
  author: "ИТМО",
)
#set page(paper: "a4", margin: (top: 2cm, bottom: 2cm, left: 2.5cm, right: 1.5cm))
#set text(lang: "ru", font: "DejaVu Serif", size: 12pt)
#set par(justify: true, leading: 0.75em, first-line-indent: 1cm)
#set heading(numbering: "1.")
#show heading: set par(first-line-indent: 0pt)
#show heading: set block(above: 1.2em, below: 0.6em)
#show raw: set text(font: "DejaVu Sans Mono", size: 10pt)
#set table.hline(stroke: 0.5pt)

#let codeblock(body) = block(
  fill: luma(245),
  inset: 8pt,
  radius: 2pt,
  width: 100%,
  breakable: true,
)[
  #set text(font: "DejaVu Sans Mono", size: 9pt)
  #set par(first-line-indent: 0pt, justify: false)
  #body
]

// ══════════ Титульный лист ══════════

#align(center)[
  #set par(first-line-indent: 0pt)
  #text(size: 12pt)[
    Федеральное государственное автономное образовательное учреждение\
    высшего образования\
    *«Национальный исследовательский университет ИТМО»*
  ]
  #v(0.35cm)
  #text(size: 12pt)[*Факультет программной инженерии и компьютерной техники*]

  #v(2.2cm)
  #text(size: 15pt, weight: "bold")[
    Лабораторная работа №3\
    Асинхронная обработка, распределённое выполнение задач,\
    планировщик и интеграция с внешней КИС\
    (платформа объявлений о недвижимости CIAN)
  ]
  #v(0.45cm)
  #text(size: 12pt)[по дисциплине *«Бизнес-логика программных систем»*]

  #v(0.5cm)
  #text(size: 13pt, weight: "bold")[Вариант №\ *3278*]

  #v(3.2cm)
  #align(right)[
    #set par(first-line-indent: 0pt)
    #block(width: 100%)[
      #grid(
        columns: (5cm, 10.5cm),
        column-gutter: 0.5em,
        row-gutter: 0.65em,
        align: (right, left),
        [*Студенты:*],
        [
          #set par(first-line-indent: 0pt, leading: 0.4em)
          Кортыш Константин Олегович\
          Соколов Артём Олегович
        ],
        [*Группа:*], [P3318],
        [*Преподаватель:*], [Кривоносов Егор Дмитриевич],
      )
    ]
  ]
  #v(1fr)
  #text(size: 12pt)[Санкт-Петербург, 2026]
]

#pagebreak()

// ══════════ Задание ══════════

#set par(first-line-indent: 0pt)
= Текст задания лабораторной работы


Доработать приложение из лабораторной работы №2, реализовав:

+ *асинхронное выполнение задач* с распределением бизнес-логики между несколькими вычислительными узлами;
+ *периодические операции* с использованием планировщика задач Spring (`@Scheduled`);
+ *интеграцию с внешней корпоративной информационной системой (EIS)* по технологии *JCA (Jakarta Connectors)*.

*Асинхронная обработка:* модель «очередь сообщений»; провайдер — *RabbitMQ*; отправка и получение — *JMS API*.

*Распределённая обработка:* обработка сообщений на *двух независимых узлах* сервера приложений.

*Планировщик:* согласованные прецеденты реализованы через `@Scheduled`.

*Интеграция EIS:* взаимодействие с внешней КИС только через *JCA*; выбранная система согласуется с преподавателем.

Изменения бизнес-процесса отражены в модели, REST API и сценариях проверки. Демонстрация — на собственной инфраструктуре (Docker) или helios.

#set par(first-line-indent: 1cm)
#pagebreak()

#outline(title: [Содержание], indent: auto)
#pagebreak()

= Согласованные прецеденты

== Асинхронная обработка (очередь сообщений)

#figure(
  table(
    columns: (1.4fr, 2.6fr, 2fr),
    inset: 6pt,
    stroke: 0.5pt,
    [*Прецедент*], [*Обоснование*], [*Реализация*],
    [Email-уведомление], [
      Создание уведомления в БД не должно блокироваться отправкой письма;
      сбой SMTP не должен откатывать бизнес-транзакцию.
    ], [
      После `commit` в очередь JMS кладётся `NotificationEvent`;
      письмо отправляют worker-узлы.
    ],
  ),
  caption: [Прецедент асинхронной обработки.],
)

Затрагиваются все сценарии, где вызывается `NotificationService.create` (публикация, продвижение, обращения, планировщики, смена статуса объявления и т.д.).

== Периодические задачи (`@Scheduled`)

#figure(
  table(
    columns: (1.6fr, 2.8fr),
    inset: 6pt,
    stroke: 0.5pt,
    [*Прецедент (BPMN / требования)*], [*Задача планировщика*],
    [Истечение срока публикации 30 дней], [
      `ListingScheduler.sendArchivationSoonNotifications` — ежедневно 02:00, уведомление `ARCHIVATION_SOON`.
    ],
    [Архивация при неподтверждении актуальности], [
      `ListingScheduler.autoArchiveExpiredListings` — ежедневно 03:00, перевод в `ARCHIVED`.
    ],
    [Еженедельная статистика по пользователю], [
      `WeeklyStatsScheduler` — понедельник 09:00, отчёт в `cian_weekly_stats` + уведомление.
    ],
  ),
  caption: [Прецеденты планировщика Spring.],
)

Планировщики активны только на узле *API* (`@Profile("!worker")`), не на worker-контейнерах.

== Интеграция с внешней КИС

*Внешняя система:* облачная CRM *Bitrix24* (воронка «Cian», сделки, контакты, активности).

#figure(
  table(
    columns: (1.2fr, 2.4fr, 2.4fr),
    inset: 6pt,
    stroke: 0.5pt,
    [*Направление*], [*Прецедент CIAN*], [*Сущность Bitrix*],
    [Outbound], [Публикация объявления], [Сделка + контакт продавца],
    [Outbound], [Закрытие / архивация / админ-статус], [Стадия сделки],
    [Outbound], [Успешная оплата TOP/PREMIUM], [UF-поле продвижения],
    [Outbound], [Новое обращение (inquiry)], [Activity на сделке],
    [Inbound], [Изменение стадии в CRM], [Обновление `Listing.status` в PostgreSQL],
  ),
  caption: [Согласованное отображение CIAN ↔ Bitrix24.],
)

= Архитектура решения

== Общая схема развёртывания (Docker)

#codeblock[
```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│  cian-app   │     │  RabbitMQ    │     │  PostgreSQL │
│  (api+      │────▶│  очередь     │     │  cian_*     │
│   bitrix)   │     │  JMS         │     └─────────────┘
└──────┬──────┘     └──────┬───────┘
       │                   │
       │ REST/JCA          │ consume
       ▼                   ▼
┌─────────────┐     ┌──────────────┐     ┌──────────────┐
│  Bitrix24   │     │ cian-worker-1│     │ cian-worker-2│
│  (EIS CRM)  │     │  (JMS mail)  │     │  (JMS mail)  │
└─────────────┘     └──────────────┘     └──────────────┘
```
]

Конфигурация: `docker/docker-compose.yml`. Сборка: `docker/Dockerfile` (модули `cian` + `bitrix-jca-ra`).

== Асинхронная обработка: JMS + RabbitMQ

*Паттерн:* Transactional Outbox на уровне приложения — запись уведомления в PostgreSQL в той же транзакции, что и бизнес-операция; *после успешного commit* событие публикуется в очередь.

*Отправка (JMS):*

- `NotificationService` → `NotificationEventPublisher` → `JmsTemplate.convertAndSend(...)`;
- очередь: `app.messaging.notifications-queue` (по умолчанию `cian.notifications`);
- фабрика соединений: `RMQConnectionFactory` (`com.rabbitmq.jms`), конфигурация в `JmsConfig`.

*Получение (JMS):*

- `NotificationEventListener` с `@JmsListener` на worker-узлах (`SPRING_PROFILES_ACTIVE=docker,worker`);
- обработчик вызывает `EmailNotificationService.send` (SMTP → Mailpit в dev).

Ключевые классы: `itmo.blps.messaging.*`, `itmo.blps.config.JmsConfig`, `itmo.blps.config.MessagingProperties`.

#codeblock[
```java
// NotificationService — публикация после commit
TransactionSynchronizationManager.registerSynchronization(
    new TransactionSynchronization() {
        @Override
        public void afterCommit() {
            notificationEventPublisher.publish(event);
        }
    });
```
]

*Модель доставки:* point-to-point (очередь JMS), конкурирующие потребители на двух worker — сообщение обрабатывает один из узлов.

== Распределённая обработка на двух узлах

В `docker-compose.yml` определены:

+ `app` — REST API, JPA, планировщики, профиль `bitrix`, *без* `@JmsListener`;
+ `worker-1` и `worker-2` — один образ `cian-app:latest`, профиль `worker`: только JMS-listener и отправка почты.

Так достигается *физическое разделение* вычислительных узлов (отдельные контейнеры JVM).

#figure(
  table(
    columns: (1.4fr, 2fr, 2.6fr),
    inset: 6pt,
    stroke: 0.5pt,
    [*Узел*], [*Профили Spring*], [*Нагрузка*],
    [`cian-app`], [`docker`, `api`, `bitrix`], [HTTP, cron, Bitrix JCA outbound/inbound],
    [`cian-worker-1`], [`docker`, `worker`], [JMS → email],
    [`cian-worker-2`], [`docker`, `worker`], [JMS → email],
  ),
  caption: [Распределение ролей между контейнерами.],
)

*Распределённые транзакции (XA):* в сценарии «уведомление + email» используется *локальная транзакция БД* и *асинхронная доставка после commit*, а не двухфазный commit между PostgreSQL и RabbitMQ. Это соответствует типовой практике «at-least-once» для почтовых уведомлений. Bitrix JCA-адаптер явно не поддерживает XA (`getXAResource` → `NotSupportedException`).


== Планировщик задач Spring

`@EnableScheduling` в `CianApplication`.

#figure(
  table(
    columns: (2fr, 1.2fr, 2.8fr),
    inset: 6pt,
    stroke: 0.5pt,
    [*Класс*], [*Cron*], [*Действие*],
    [`ListingScheduler`], [`0 0 2 * * *`], [Уведомления об истечении срока размещения],
    [`ListingScheduler`], [`0 0 3 * * *`], [Автоархивация после grace-period],
    [`WeeklyStatsScheduler`], [`0 0 9 * * MON`], [Генерация отчётов за прошлую неделю],
  ),
  caption: [Задачи `@Scheduled`.],
)

== Интеграция с EIS: JCA и Bitrix24

Интеграция вынесена в отдельный Gradle-модуль `bitrix-jca-ra`.

#figure(
  table(
    columns: (1.8fr, 2.2fr),
    inset: 6pt,
    stroke: 0.5pt,
    [*Компонент JCA*], [*Класс / назначение*],
    [Resource Adapter], [`BitrixResourceAdapter` — inbound polling `crm.deal.list`],
    [Managed Connection Factory], [`BitrixManagedConnectionFactory` — REST webhook URL],
    [CCI], [`BitrixConnection` / `BitrixConnectionImpl` — единственный путь к Bitrix REST],
    [Connection Factory], [`BitrixConnectionFactory` — для `CrmSyncService`],
    [Message listener], [`BitrixEventListener` → `BitrixInboundSyncService`],
    [Embedded container], [`BitrixJcaEmbeddedContainer` — запуск RA в Spring Boot],
  ),
  caption: [Структура JCA-адаптера Bitrix24.],
)

*Прикладной слой:* `CrmSyncService`, таблица связей `cian_crm_links`, хуки в `ListingService`, `InquiryService`, `PaymentService`. Синхронизация после commit (`CrmSyncHelper`). Защита от циклов — `BitrixSyncContext.isInbound()`.

*Backfill:* при старте (`BitrixStartupBackfill`) в Bitrix выгружаются объявления из `seed.sql` (не `DRAFT`) и inquiries без связи.

*Конфигурация:* профиль `bitrix`, `application-bitrix.yml`, переменная `BITRIX_REST_BASE_URL` в `docker/.env`.

Приложение *не вызывает* Bitrix через `RestTemplate` напрямую — только через CCI `BitrixConnection` (требование JCA).

= Изменения в модели данных и API

== База данных

Добавлена таблица `cian_crm_links` (`entity_type`, `local_id`, `bitrix_id`, `last_sync_at`) — сопоставление сущностей CIAN и Bitrix. Скрипт: `src/sql/schema.sql`; `seed.sql` очищает и `cian_crm_links`.

== REST API

Публичные контракты из ЛР №2 сохранены (`docs/API_ENDPOINTS.md`). Интеграция с Bitrix *не добавляет* обязательных публичных эндпоинтов для пользователя; обмен с EIS идёт через JCA (outbound/inbound polling).

= Структура исходного кода

#codeblock[
```
blps_lab2/
├── bitrix-jca-ra/          # JCA Resource Adapter (ra.xml, RA, MCF, CCI)
├── src/main/java/itmo/blps/
│   ├── config/             # JmsConfig, ListingScheduler, WeeklyStatsScheduler, Bitrix*
│   ├── messaging/          # NotificationEvent, Publisher, Listener
│   ├── integration/crm/    # CrmSyncService, BitrixSyncContext
│   ├── service/            # бизнес-логика + хуки CRM
│   └── ...
├── docker/                 # compose: app + 2 workers + rabbitmq + db + mailpit
└── docs/                   # API, БД, отчёт
```
]

= Запуск стенда

#codeblock[
```bash
# docker/.env
BITRIX_REST_BASE_URL=https://<portal>.bitrix24.ru/rest/1/<token>/

cd docker
docker compose up --build -d
```
]

Сервисы: приложение `http://localhost:8080`, RabbitMQ UI `http://localhost:15672` (cian/cian), Mailpit `http://localhost:8025`.


= Выводы

В ходе работы платформа CIAN доработана до архитектуры с *разделением синхронного API* и *асинхронных worker-узлов* на базе *JMS/RabbitMQ*, что позволяет масштабировать отправку email-уведомлений и изолировать сбои почтового канала от основных транзакций.

*Планировщик Spring* автоматизирует сценарии истечения срока публикации и еженедельной отчётности без участия пользователя, что соответствует диаграммам BPMN и требованиям R3.x.

*Интеграция с Bitrix24 через JCA* реализует двусторонний обмен: объявления и обращения отражаются в CRM как сделки и активности, изменения стадий в Bitrix возвращаются в статусы объявлений. Вся работа с внешней КИС сосредоточена в Resource Adapter и CCI, что выполняет учебное требование по Jakarta Connectors.

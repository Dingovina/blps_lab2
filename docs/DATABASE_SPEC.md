# Спецификация базы данных (PostgreSQL)

Описание структуры БД для работы приложения платформы объявлений о недвижимости. Основа: `docs/BACKEND_REQUIREMENTS.md`, `docs/API_ENDPOINTS.md`. СУБД: **PostgreSQL** (psql).

---

## 1. Общие соглашения

- **Имена таблиц:** префикс `cian_`, далее `snake_case` (`cian_users`, `cian_listings` и т.д.).
- **Первичные ключи:** тип `BIGINT`, автоинкремент через `BIGSERIAL` (встроенные последовательности PostgreSQL, без расширений).
- **Временные зоны:** все даты и время хранятся в `TIMESTAMPTZ` (UTC).
- **Перечисления (роль, статусы, типы):** хранятся как **VARCHAR(50)** со значениями в верхнем регистре (`'SELLER'`, `'DRAFT'`, `'ACTIVE'` и т.д.) для совместимости с JPA `@Enumerated(EnumType.STRING)`. Использование нативного PostgreSQL `CREATE TYPE ... AS ENUM` приводит к ошибке «operator does not exist: enum = character varying» при работе Hibernate.
- **Ссылки на требования:** в комментариях к таблицам/столбцам указаны идентификаторы из BACKEND_REQUIREMENTS.md (R1.x, R2.x, R0.x).

---

## 2. Перечисления (значения в VARCHAR)

Перечисляемые поля (роль, статусы, типы) хранятся в столбцах **VARCHAR(50)**. Допустимые значения совпадают с именами enum в коде приложения (JPA `@Enumerated(EnumType.STRING)`).

| Назначение              | Столбец(ы)              | Допустимые значения |
|-------------------------|-------------------------|----------------------|
| Роль пользователя (R1.1, R0.4) | cian_users.role | SELLER, BUYER, ADMIN |
| Статус объявления (R1.3, R1.16, R2.13) | cian_listings.status | DRAFT, ACTIVE, ARCHIVED, CLOSED |
| Тип продвижения (R1.7, R1.9) | cian_listings.promotion, cian_payments.promotion_type | NONE, TOP, PREMIUM (в платежах только TOP, PREMIUM — CHECK) |
| Статус платежа (R1.8)   | cian_payments.status    | PENDING, SUCCESS, FAILED |
| Статус обращения (R2.4, R2.7, R2.10) | cian_inquiries.status | PENDING, SHOWING_SCHEDULED, SHOWING_REJECTED, COMPLETED |
| Тип уведомления (R0.2)  | cian_notifications.type  | LISTING_PUBLISHED, PROMOTION_ACTIVATED, PROMOTION_PAYMENT_FAILED, ARCHIVATION_SOON, PUBLICATION_EXTENDED, NEW_INQUIRY, SHOWING_SCHEDULED, SHOWING_REJECTED, LISTING_CLOSED |
| Связанная сущность      | cian_notifications.related_entity_type | LISTING, INQUIRY, PAYMENT |

---

## 3. Таблицы

### 3.1 cian_users (пользователи)

Хранение учётных записей продавцов, покупателей и администраторов. Один пользователь может выступать и продавцом, и покупателем; администратор — отдельная роль (R0.4). При необходимости роль можно расширить до таблицы `cian_user_roles (user_id, role)`.

| Столбец        | Тип              | Ограничения     | Описание |
|----------------|------------------|------------------|----------|
| id             | BIGSERIAL        | PRIMARY KEY      | Идентификатор |
| email          | VARCHAR(255)     | NOT NULL, UNIQUE | Логин (R1.1) |
| password_hash  | VARCHAR(255)     | NOT NULL         | Хэш пароля |
| role           | VARCHAR(50)      | NOT NULL         | SELLER \| BUYER \| ADMIN |
| created_at     | TIMESTAMPTZ      | NOT NULL DEFAULT now() | |

**Связь с API:** `POST /api/auth/login`, `GET /api/auth/me`.

---

### 3.2 cian_listings (объявления)

Хранилище объявлений (Data Store «Объявления»). Состояния: черновик → активно → (продление / архивация / закрытие). Срок размещения 30 дней — по полю `expires_at` (R1.13, R1.17).

| Столбец       | Тип                 | Ограничения     | Описание |
|---------------|---------------------|------------------|----------|
| id            | BIGSERIAL           | PRIMARY KEY      | Идентификатор |
| seller_id     | BIGINT              | NOT NULL, FK→cian_users(id) | Владелец (продавец) |
| title         | VARCHAR(500)        | NOT NULL         | Заголовок |
| description   | TEXT                |                  | Описание |
| address       | VARCHAR(500)        |                  | Адрес (для поиска/фильтров) |
| region        | VARCHAR(255)        |                  | Регион/город (для фильтра поиска) |
| price         | NUMERIC(18,2)       | NOT NULL         | Цена |
| area_sqm       | NUMERIC(10,2)       |                  | Площадь, м² |
| rooms         | INTEGER             |                  | Количество комнат |
| status        | VARCHAR(50)         | NOT NULL DEFAULT 'DRAFT' | DRAFT, ACTIVE, ARCHIVED, CLOSED (R1.3, R1.16, R1.17, R2.13) |
| promotion     | VARCHAR(50)         | NOT NULL DEFAULT 'NONE' | NONE, TOP, PREMIUM (R1.9, R1.12) |
| published_at  | TIMESTAMPTZ         |                  | Дата публикации (при переходе в ACTIVE) |
| expires_at    | TIMESTAMPTZ         |                  | Окончание размещения (30 дней от published_at / продления) (R1.13) |
| closed_at     | TIMESTAMPTZ         |                  | Дата закрытия (R2.12–R2.15) |
| created_at    | TIMESTAMPTZ         | NOT NULL DEFAULT now() | |

**Индексы:** `seller_id`, `status`, `(status, region, price, area_sqm, rooms)` для поиска, `expires_at` для фоновой задачи «скоро архивация».

**Связь с API:** все эндпоинты `/api/listings/*`, `/api/listings/search`, `/api/seller/listings`.

---

### 3.3 cian_payments (платежи за продвижение)

Платежи за услугу продвижения (Топ/Премиум). Нужны для webhook и однозначного перевода объявления в состояние «с продвижением» (R1.8, R1.9, R1.11, R0.3).

| Столбец           | Тип                 | Ограничения     | Описание |
|-------------------|---------------------|------------------|----------|
| id                | BIGSERIAL           | PRIMARY KEY      | Идентификатор |
| listing_id        | BIGINT              | NOT NULL, FK→cian_listings(id) | Объявление |
| user_id           | BIGINT              | NOT NULL, FK→cian_users(id) | Плательщик (продавец) |
| promotion_type    | VARCHAR(50)         | NOT NULL         | TOP \| PREMIUM (CHECK: не NONE) |
| status            | VARCHAR(50)         | NOT NULL DEFAULT 'PENDING' | PENDING, SUCCESS, FAILED (R1.8) |
| external_id       | VARCHAR(255)        |                  | Идентификатор во внешней платёжной системе |
| amount_cents      | INTEGER             |                  | Сумма в копейках (опционально) |
| created_at        | TIMESTAMPTZ         | NOT NULL DEFAULT now() | |

**Связь с API:** `POST /api/listings/{id}/promotion/pay`, `POST /api/webhooks/payment`.

---

### 3.4 cian_inquiries (обращения / запросы на показ)

История обращений покупателей к продавцам (Data Store «База данных» в требованиях). Привязка к объявлению, покупателю и времени (R2.4).

| Столбец        | Тип                  | Ограничения     | Описание |
|----------------|----------------------|------------------|----------|
| id             | BIGSERIAL            | PRIMARY KEY      | Идентификатор |
| listing_id     | BIGINT               | NOT NULL, FK→cian_listings(id) | Объявление |
| buyer_id       | BIGINT               | NOT NULL, FK→cian_users(id) | Покупатель (инициатор) |
| message        | TEXT                 |                  | Сообщение покупателя |
| status         | VARCHAR(50)          | NOT NULL DEFAULT 'PENDING' | PENDING, SHOWING_SCHEDULED, SHOWING_REJECTED, COMPLETED (R2.7, R2.8, R2.10) |
| scheduled_at   | TIMESTAMPTZ          |                  | Назначенное время показа (R2.7) |
| contact_info   | VARCHAR(500)         |                  | Контакт продавца для показа |
| reject_reason  | TEXT                 |                  | Причина отказа (R2.8) |
| will_buy       | BOOLEAN              |                  | Решение после показа: true = покупаю, false = не покупаю (R2.9–R2.11) |
| created_at     | TIMESTAMPTZ          | NOT NULL DEFAULT now() | |

**Индексы:** `listing_id`, `buyer_id`, `status`. Для списка обращений продавца — индекс по объявлениям продавца (через listing_id → seller_id) или составной (listing_id, status).

**Связь с API:** все эндпоинты `/api/inquiries/*`.

---

### 3.5 cian_notifications (уведомления)

Уведомления для доставки в личный кабинет (и при необходимости в другие каналы). Связь с сущностью через `related_entity_type` и `related_entity_id` (R1.5, R1.10, R1.11, R1.14, R1.18, R2.5, R2.7, R2.8, R2.14, R2.16, R0.2).

| Столбец             | Тип                        | Ограничения     | Описание |
|---------------------|----------------------------|------------------|----------|
| id                  | BIGSERIAL                  | PRIMARY KEY      | Идентификатор |
| user_id             | BIGINT                     | NOT NULL, FK→cian_users(id) | Получатель |
| type                | VARCHAR(50)                | NOT NULL         | Тип уведомления |
| title               | VARCHAR(500)               | NOT NULL         | Заголовок |
| body                | TEXT                       |                  | Текст |
| related_entity_type | VARCHAR(50)                |                  | LISTING, INQUIRY, PAYMENT |
| related_entity_id   | BIGINT                     |                  | ID сущности (listing/inquiry/payment) |
| read                | BOOLEAN                    | NOT NULL DEFAULT false | Прочитано |
| created_at          | TIMESTAMPTZ                | NOT NULL DEFAULT now() | |

**Индексы:** `user_id`, `(user_id, read)`, `created_at` для пагинации.

**Связь с API:** `GET /api/notifications`, `PATCH /api/notifications/{id}/read`.

---

## 4. Схема связей (ER)

```
cian_users (1) ───────────< cian_listings      (продавец — объявления)
       │                            │
       │                            ├──< cian_payments   (платежи за продвижение по объявлению)
       │                            │
       │                            └──< cian_inquiries  (обращения по объявлению)
       │                                    │
       └────────────────────────────────────┘ buyer_id (покупатель)
       
cian_users (1) ───────────< cian_notifications (получатель уведомлений)
```

- У одного пользователя (seller) — много объявлений.
- У объявления — много платежей (история попыток оплаты продвижения); актуальное продвижение хранится в `cian_listings.promotion`.
- У объявления — много обращений (inquiries).
- У каждого обращения — один покупатель (buyer), одно объявление (listing); продавец определяется через `cian_listings.seller_id`.
- У пользователя — много уведомлений.

---

## 5. Индексы (сводка)

| Таблица           | Индекс / назначение |
|-------------------|---------------------|
| cian_users        | UNIQUE(email) (уже в ограничении) |
| cian_listings     | idx_cian_listings_seller_id(seller_id), idx_cian_listings_status(status), idx_cian_listings_search(...), idx_cian_listings_expires_at(expires_at) |
| cian_payments     | idx_cian_payments_listing_id(listing_id), idx_cian_payments_user_id(user_id), idx_cian_payments_status(status) |
| cian_inquiries    | idx_cian_inquiries_listing_id(listing_id), idx_cian_inquiries_buyer_id(buyer_id), idx_cian_inquiries_status(status) |
| cian_notifications| idx_cian_notifications_user_id(user_id), idx_cian_notifications_user_read(user_id, read), idx_cian_notifications_created_at(created_at) |

Поиск объявлений (`/api/listings/search`) выполняется по `cian_listings` с условием `status = 'ACTIVE'` и фильтрами по region, price, area_sqm, rooms; порядок с учётом `promotion` (TOP/PREMIUM выше в выдаче).

---

## 6. Полный DDL-скрипт (PostgreSQL)

Актуальный скрипт находится в **`src/sql/schema.sql`**. Запуск: `psql -U <user> -d <database> -f src/sql/schema.sql`. При пересоздании удаляются таблицы и (при наличии) старые enum-типы, затем создаются таблицы и индексы. Перечисляемые поля хранятся как VARCHAR(50).

```sql
-- ----- 1. Удаление (пересоздание) -----
DROP TABLE IF EXISTS cian_notifications CASCADE;
DROP TABLE IF EXISTS cian_inquiries CASCADE;
DROP TABLE IF EXISTS cian_payments CASCADE;
DROP TABLE IF EXISTS cian_listings CASCADE;
DROP TABLE IF EXISTS cian_users CASCADE;

DROP TYPE IF EXISTS cian_related_entity_type CASCADE;
DROP TYPE IF EXISTS cian_notification_type CASCADE;
DROP TYPE IF EXISTS cian_inquiry_status CASCADE;
DROP TYPE IF EXISTS cian_payment_status CASCADE;
DROP TYPE IF EXISTS cian_promotion_type CASCADE;
DROP TYPE IF EXISTS cian_listing_status CASCADE;
DROP TYPE IF EXISTS cian_user_role CASCADE;

-- ----- 2. Таблицы (enum-поля как VARCHAR(50)) -----
CREATE TABLE cian_users (
  id            BIGSERIAL PRIMARY KEY,
  email         VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role          VARCHAR(50) NOT NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cian_listings (
  id           BIGSERIAL PRIMARY KEY,
  seller_id    BIGINT NOT NULL REFERENCES cian_users(id) ON DELETE CASCADE,
  title        VARCHAR(500) NOT NULL,
  description  TEXT,
  address      VARCHAR(500),
  region       VARCHAR(255),
  price        NUMERIC(18,2) NOT NULL,
  area_sqm     NUMERIC(10,2),
  rooms        INTEGER,
  status       VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
  promotion    VARCHAR(50) NOT NULL DEFAULT 'NONE',
  published_at TIMESTAMPTZ,
  expires_at   TIMESTAMPTZ,
  closed_at    TIMESTAMPTZ,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cian_payments (
  id             BIGSERIAL PRIMARY KEY,
  listing_id     BIGINT NOT NULL REFERENCES cian_listings(id) ON DELETE CASCADE,
  user_id        BIGINT NOT NULL REFERENCES cian_users(id) ON DELETE CASCADE,
  promotion_type VARCHAR(50) NOT NULL,
  status         VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  external_id    VARCHAR(255),
  amount_cents   INTEGER,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT cian_chk_promotion_not_none CHECK (promotion_type IN ('TOP', 'PREMIUM'))
);

CREATE TABLE cian_inquiries (
  id            BIGSERIAL PRIMARY KEY,
  listing_id    BIGINT NOT NULL REFERENCES cian_listings(id) ON DELETE CASCADE,
  buyer_id      BIGINT NOT NULL REFERENCES cian_users(id) ON DELETE CASCADE,
  message       TEXT,
  status        VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  scheduled_at  TIMESTAMPTZ,
  contact_info  VARCHAR(500),
  reject_reason TEXT,
  will_buy      BOOLEAN,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cian_notifications (
  id                  BIGSERIAL PRIMARY KEY,
  user_id             BIGINT NOT NULL REFERENCES cian_users(id) ON DELETE CASCADE,
  type                VARCHAR(50) NOT NULL,
  title               VARCHAR(500) NOT NULL,
  body                TEXT,
  related_entity_type VARCHAR(50),
  related_entity_id   BIGINT,
  read                BOOLEAN NOT NULL DEFAULT false,
  created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ----- 3. Индексы -----
CREATE INDEX idx_cian_listings_seller_id ON cian_listings(seller_id);
CREATE INDEX idx_cian_listings_status ON cian_listings(status);
CREATE INDEX idx_cian_listings_expires_at ON cian_listings(expires_at) WHERE status = 'ACTIVE';
CREATE INDEX idx_cian_listings_search ON cian_listings(region, price, area_sqm, rooms) WHERE status = 'ACTIVE';

CREATE INDEX idx_cian_payments_listing_id ON cian_payments(listing_id);
CREATE INDEX idx_cian_payments_user_id ON cian_payments(user_id);
CREATE INDEX idx_cian_payments_status ON cian_payments(status);

CREATE INDEX idx_cian_inquiries_listing_id ON cian_inquiries(listing_id);
CREATE INDEX idx_cian_inquiries_buyer_id ON cian_inquiries(buyer_id);
CREATE INDEX idx_cian_inquiries_status ON cian_inquiries(status);

CREATE INDEX idx_cian_notifications_user_id ON cian_notifications(user_id);
CREATE INDEX idx_cian_notifications_user_read ON cian_notifications(user_id, read);
CREATE INDEX idx_cian_notifications_created_at ON cian_notifications(created_at DESC);
```

---

## 8. Соответствие требованиям и API

| Требование / функция | Таблицы / поля |
|----------------------|----------------|
| R1.1 — авторизация продавца/покупателя | cian_users (email, password_hash, role) |
| R1.2 — черновик объявления | cian_listings (status = DRAFT) |
| R1.3, R1.4 — размещение, уведомление | cian_listings (status = ACTIVE, published_at, expires_at), cian_notifications |
| R1.6 — выбор продвижения | cian_listings.promotion остаётся NONE или далее оплата |
| R1.7–R1.12 — оплата продвижения | cian_payments, cian_listings.promotion, cian_notifications |
| R1.13, R1.14 — 30 дней, уведомление об архивации | cian_listings.expires_at, фоновый процесс + cian_notifications |
| R1.15–R1.19 — продление/архивация | cian_listings (status, expires_at), cian_notifications |
| R2.1, R2.2 — поиск, выдача с Топ | cian_listings (status = ACTIVE, promotion), индексы поиска |
| R2.3–R2.5 — обращение, история, уведомление продавца | cian_inquiries, cian_notifications |
| R2.6–R2.8 — согласование показа | cian_inquiries (status, scheduled_at, contact_info, reject_reason), cian_notifications |
| R2.9–R2.11 — результат посещения | cian_inquiries.will_buy, уведомление продавцу о закрытии |
| R2.12–R2.15 — закрытие объявления | cian_listings (status = CLOSED, closed_at), cian_notifications |
| R2.16 — уведомление об отказе | cian_notifications |
| R0.1 — идентификация пользователя | cian_users.id (BIGINT), cian_users.role, FK во всех таблицах |
| R0.4 — роль администратора | cian_users.role = ADMIN, эндпоинты /api/admin/* |
| R0.3 — консистентность состояния | status, даты (published_at, expires_at, closed_at), платежи в cian_payments |

---

**Идентификаторы в API:** в ответах и в путях эндпоинтов (`/api/listings/{listingId}` и т.д.) используются целочисленные id (BIGINT), а не UUID.

---

*Документ подготовлен на основе BACKEND_REQUIREMENTS.md и API_ENDPOINTS.md. Версия: 1.1. СУБД: PostgreSQL (psql). Перечисляемые поля — VARCHAR(50) для совместимости с JPA. Без расширений (pgcrypto не требуется).*

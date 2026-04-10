# REST API: эндпоинты приложения

Спецификация HTTP REST API с передачей данных в формате JSON. Основа: `docs/BACKEND_REQUIREMENTS.md`.

---

## 1. Общие соглашения

### 1.1 Базовый URL и заголовки

- **Базовый URL:** `https://localhost/cian` (или конфигурируемый корень).
- **Content-Type:** `application/json` для тел запросов и ответов.
- **Кодировка:** UTF-8.

Все запросы (кроме публичного поиска и логина при необходимости) должны содержать заголовок авторизации:

```http
Authorization: Bearer <access_token>
```

### 1.2 Формат ответов об ошибках

При ошибке (4xx, 5xx) тело ответа в JSON:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Описание ошибки для клиента",
    "details": []
  }
}
```

Поле `details` опционально (например, список ошибок валидации по полям).

### 1.3 Идентификаторы ресурсов

Идентификаторы сущностей (пользователь, объявление, обращение, уведомление, платёж) в путях и в теле ответов — **целые числа** (BIGINT), генерируемые БД (BIGSERIAL). Пример: `"id": 1`, `/api/listings/42`.

### 1.4 Идентификация требований

В описании эндпоинтов указаны ссылки на требования из `BACKEND_REQUIREMENTS.md` (R1.x, R2.x, R0.x).

---

## 2. Аутентификация

| Метод | Путь | Описание | Требование |
|-------|------|----------|------------|
| POST | `/api/auth/register` | Регистрация (роль SELLER или BUYER; админ создаётся при старте) | — |
| POST | `/api/auth/login` | Вход в личный кабинет (продавец/покупатель/администратор) | R1.1, R0.1 |
| POST | `/api/auth/logout` | Выход, инвалидация токена | — |
| GET  | `/api/auth/me`     | Текущий пользователь (роль, id, профиль) | R0.1 |

### POST /api/auth/register

**Важно:** путь именно `/api/auth/register` (не `/api/register`).

**Request body:**

```json
{
  "email": "user@example.com",
  "password": "secret",
  "role": "SELLER"
}
```

`role`: только **SELLER** или **BUYER** (ADMIN регистрацией создать нельзя).

**Response 201:**

```json
{
  "id": 2,
  "email": "user@example.com",
  "role": "SELLER",
  "createdAt": "2025-03-14T12:00:00Z"
}
```

**Response 400** — неверная роль (ADMIN), email уже занят или ошибка валидации.

---

### POST /api/auth/login

**Request body:**

```json
{
  "email": "user@example.com",
  "password": "secret"
}
```

**Response 200:**

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "role": "SELLER"
  }
}
```

Роль: `SELLER` | `BUYER` | `ADMIN` (при необходимости одна учётная запись может иметь несколько ролей; администратор входит отдельной учётной записью с ролью ADMIN).

### GET /api/auth/me

**Response 200:**

```json
{
  "id": 1,
  "email": "user@example.com",
  "role": "SELLER"
}
```

---

## 3. Объявления (listings)

Ресурс объявлений о недвижимости. Операции от имени продавца (создание, публикация, продвижение, продление, архивация, закрытие).

### 3.1 Создание и редактирование черновика

| Метод | Путь | Описание | Требование |
|-------|------|----------|------------|
| POST | `/api/listings` | Создать объявление (черновик) | R1.2 |
| PUT  | `/api/listings/{listingId}` | Обновить объявление (черновик или до публикации) | R1.2 |
| GET  | `/api/listings/{listingId}` | Получить объявление по ID | — |
| GET  | `/api/seller/listings` | Список объявлений текущего продавца | — |

**POST /api/listings**  
Роль: продавец.

**Request body (минимальный набор полей):**

```json
{
  "title": "2-комн. квартира, 54 м²",
  "description": "Описание объекта",
  "address": "г. Москва, ул. Примерная, д. 1",
  "price": 12000000,
  "areaSqm": 54,
  "rooms": 2
}
```

Дополнительные поля (тип недвижимости, этаж, фото и т.д.) задаются в отдельной модели данных.

**Response 201:**

```json
{
  "id": 1,
  "status": "DRAFT",
  "title": "2-комн. квартира, 54 м²",
  "createdAt": "2025-03-14T12:00:00Z"
}
```

**PUT /api/listings/{listingId}**  
Тело — те же поля, что и при создании (частичное обновление допускается при соглашении). Ответ 200 — объект объявления.

**GET /api/seller/listings**  
Query-параметры (все опционально): `status=DRAFT|ACTIVE|ARCHIVED|CLOSED`, `page`, `size`.  
Response 200 — постраничный список объявлений продавца.

---

### 3.2 Публикация и выбор продвижения

| Метод | Путь | Описание | Требование |
|-------|------|----------|------------|
| POST | `/api/listings/{listingId}/publish` | Проверить и разместить объявление | R1.3, R1.4 |
| POST | `/api/listings/{listingId}/promotion/choice` | Выбор: без продвижения / с продвижением | R1.6 |

**POST /api/listings/{listingId}/publish**

- Валидация данных объявления; при успехе — переход в состояние «размещено/активно», запись в хранилище объявлений.
- Платформа отправляет уведомление продавцу (предложение платного продвижения) — доставка через канал уведомлений (см. раздел «Уведомления»).

**Response 200:**

```json
{
  "id": 1,
  "status": "ACTIVE",
  "publishedAt": "2025-03-14T12:00:00Z",
  "message": "Объявление размещено. Доступно платное продвижение (Топ/Премиум)."
}
```

**Response 400** — ошибка валидации (перечень в `error.details`).

---

**POST /api/listings/{listingId}/promotion/choice**

Фиксирует выбор продавца: опубликовать без продвижения или перейти к оплате продвижения.

**Request body:**

```json
{
  "withPromotion": false
}
```

`withPromotion: true` — продавец выбрал платное продвижение; далее вызывается эндпоинт инициации оплаты.  
`withPromotion: false` — объявление остаётся опубликованным без продвижения (R1.6).

**Response 200:**

```json
{
  "listingId": 1,
  "withPromotion": false,
  "nextStep": "none"
}
```

При `withPromotion: true` в ответе может быть `nextStep: "pay"` и ссылка на оплату или идентификатор платежа.

---

### 3.3 Оплата продвижения (Топ/Премиум)

| Метод | Путь | Описание | Требование |
|-------|------|----------|------------|
| POST | `/api/listings/{listingId}/promotion/pay` | Инициировать оплату продвижения | R1.7 |
| POST | `/api/webhooks/payment` | Callback от платёжной системы (результат оплаты) | R1.8 |

**POST /api/listings/{listingId}/promotion/pay**

**Request body:**

```json
{
  "promotionType": "TOP",
  "returnUrl": "https://app.example.com/payment/return",
  "cancelUrl": "https://app.example.com/payment/cancel"
}
```

`promotionType`: `TOP` | `PREMIUM`.

**Response 200:**

```json
{
  "paymentId": 1,
  "redirectUrl": "https://payment-gateway.example.com/checkout/...",
  "status": "PENDING"
}
```

Клиент перенаправляет пользователя на `redirectUrl`. После оплаты платёжная система вызывает **POST /api/webhooks/payment** (R1.8). При успехе платформа активирует продвижение (R1.9) и отправляет уведомление продавцу (R1.10); при ошибке — уведомление об ошибке (R1.11), объявление остаётся без продвижения (R1.12).

**POST /api/webhooks/payment**  
Вызывается платёжной системой. Тело и подпись — по контракту с провайдером. Внутри: обновление статуса платежа, при успехе — активация услуги продвижения для объявления, отправка уведомлений. Ответ 2xx при успешной обработке.

---

### 3.4 Срок размещения 30 дней и актуальность

| Метод | Путь | Описание | Требование |
|-------|------|----------|------------|
| POST | `/api/listings/{listingId}/confirm-relevance` | Подтвердить актуальность: продлить или архивировать | R1.15, R1.16, R1.17 |

Событие «Срок размещения 30 дней истёк» и уведомление продавцу о скорой архивации (R1.13, R1.14) реализуются фоновыми задачами/таймерами; получение уведомлений — через **GET /api/notifications**.

**POST /api/listings/{listingId}/confirm-relevance**

**Request body:**

```json
{
  "relevant": true
}
```

- `relevant: true` — объявление актуально: платформа продлевает размещение (например, ещё на 30 дней), обновляет хранилище объявлений (R1.17), отправляет уведомление о продлении (R1.18, R1.19).
- `relevant: false` — объявление не актуально: платформа архивирует размещение, обновляет хранилище (R1.16), сценарий «Объявление снято».

**Response 200:**

```json
{
  "listingId": 1,
  "action": "EXTENDED",
  "expiresAt": "2025-04-13T12:00:00Z"
}
```

`action`: `EXTENDED` | `ARCHIVED`.

---

### 3.5 Закрытие объявления

| Метод | Путь | Описание | Требование |
|-------|------|----------|------------|
| POST | `/api/listings/{listingId}/close` | Закрыть объявление (снять с публикации) | R2.12, R2.13, R2.14, R2.15 |

Платформа убирает объявление из списка активных (оно не попадает в выдачу поиска), затем оповещает продавца о закрытии.

**Response 200:**

```json
{
  "listingId": 1,
  "status": "CLOSED",
  "closedAt": "2025-03-14T14:00:00Z"
}
```

---

## 4. Поиск объявлений (покупатель)

| Метод | Путь | Описание | Требование |
|-------|------|----------|------------|
| GET  | `/api/listings/search` | Поиск недвижимости по фильтрам, выдача с учётом Топ/Премиум | R2.1, R2.2 |

**GET /api/listings/search**

Доступ: анонимный или от имени покупателя (при необходимости персонализация).

**Query-параметры (все опционально):**

| Параметр   | Тип   | Описание |
|------------|--------|----------|
| region     | string | Регион/город |
| minPrice   | number | Минимальная цена |
| maxPrice   | number | Максимальная цена |
| rooms      | number | Количество комнат |
| minAreaSqm | number | Минимальная площадь, м² |
| maxAreaSqm | number | Максимальная площадь, м² |
| page       | number | Номер страницы (с 1) |
| size       | number | Размер страницы |

Платформа формирует выдачу из активных объявлений с учётом фильтров и правил ранжирования (в т.ч. объявления с продвижением Топ/Премиум).

**Response 200:**

```json
{
  "content": [
    {
      "id": 1,
      "title": "2-комн. квартира, 54 м²",
      "address": "г. Москва, ул. Примерная, д. 1",
      "price": 12000000,
      "areaSqm": 54,
      "rooms": 2,
      "promotion": "TOP",
      "publishedAt": "2025-03-14T12:00:00Z"
    }
  ],
  "totalElements": 42,
  "totalPages": 5,
  "number": 0,
  "size": 10
}
```

`promotion`: `NONE` | `TOP` | `PREMIUM` (опционально).

---

## 5. Обращения (запросы на показ)

Ресурс обращений покупателя к продавцу по объявлению (запрос на показ, согласование, отказ, результат посещения).

| Метод | Путь | Описание | Требование |
|-------|------|----------|------------|
| POST | `/api/inquiries` | Создать обращение (запрос на показ) | R2.3, R2.4, R2.5 |
| GET  | `/api/inquiries` | Список обращений (входящие для продавца / свои для покупателя) | — |
| GET  | `/api/inquiries/{inquiryId}` | Получить обращение по ID | — |
| POST | `/api/inquiries/{inquiryId}/confirm-showing` | Подтвердить показ, назначить время | R2.6, R2.7 |
| POST | `/api/inquiries/{inquiryId}/reject-showing` | Отказать в показе | R2.6, R2.8, R2.16 |
| POST | `/api/inquiries/{inquiryId}/visit-result` | Зафиксировать результат посещения показа | R2.9, R2.10, R2.11 |

### POST /api/inquiries

Роль: покупатель.

**Request body:**

```json
{
  "listingId": 1,
  "message": "Хотел бы посмотреть квартиру в выходные"
}
```

Платформа сохраняет обращение в хранилище (R2.4) и оповещает продавца (R2.5).

**Response 201:**

```json
{
  "id": 1,
  "listingId": 1,
  "status": "PENDING",
  "createdAt": "2025-03-14T12:00:00Z"
}
```

### GET /api/inquiries

Query: `role=SELLER|BUYER` (или определение по текущему пользователю), `status`, `listingId`, `page`, `size`.  
Response 200 — постраничный список обращений.

### POST /api/inquiries/{inquiryId}/confirm-showing

Роль: продавец.

**Request body:**

```json
{
  "scheduledAt": "2025-03-20T14:00:00Z",
  "contactInfo": "тел. +7 XXX XXX-XX-XX"
}
```

Платформа обновляет обращение, отправляет покупателю уведомление о назначенном показе (R2.7).

**Response 200:**

```json
{
  "inquiryId": 1,
  "status": "SHOWING_SCHEDULED",
  "scheduledAt": "2025-03-20T14:00:00Z"
}
```

### POST /api/inquiries/{inquiryId}/reject-showing

Роль: продавец. Тело пустое или с полем `reason` (текст отказа).  
Платформа оповещает покупателя об отказе (R2.8, R2.16).  
Response 200 — обновлённое обращение или краткий статус.

### POST /api/inquiries/{inquiryId}/visit-result

Роль: покупатель. Фиксирует посещение показа и решение.

**Request body:**

```json
{
  "willBuy": false
}
```

- `willBuy: false` — сценарий по обращению завершается (R2.10).
- `willBuy: true` — платформа создаёт для продавца необходимость закрыть объявление (например, уведомление/задача «Закрыть объявление») (R2.11); продавец закрывает объявление через **POST /api/listings/{listingId}/close**.

**Response 200:**

```json
{
  "inquiryId": 1,
  "willBuy": false,
  "status": "COMPLETED"
}
```

---

## 6. Уведомления

Единый способ получения уведомлений для продавца и покупателя (R1.5, R1.10, R1.11, R1.14, R1.18, R2.5, R2.7, R2.8, R2.14, R2.16, R0.2).

| Метод | Путь | Описание |
|-------|------|----------|
| GET  | `/api/notifications` | Список уведомлений текущего пользователя |
| PATCH| `/api/notifications/{notificationId}/read` | Отметить уведомление прочитанным |

**GET /api/notifications**

Query: `unreadOnly=true|false`, `page`, `size`.

**Response 200:**

```json
{
  "content": [
    {
      "id": 1,
      "type": "LISTING_PUBLISHED",
      "title": "Объявление размещено",
      "body": "Ваше объявление «2-комн. квартира» размещено. Доступно платное продвижение.",
      "relatedEntityType": "LISTING",
      "relatedEntityId": 1,
      "read": false,
      "createdAt": "2025-03-14T12:00:00Z"
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

Типы уведомлений (примеры): `LISTING_PUBLISHED`, `PROMOTION_ACTIVATED`, `PROMOTION_PAYMENT_FAILED`, `ARCHIVATION_SOON`, `PUBLICATION_EXTENDED`, `NEW_INQUIRY`, `SHOWING_SCHEDULED`, `SHOWING_REJECTED`, `LISTING_CLOSED` и т.д.

**PATCH /api/notifications/{notificationId}/read**  
Request body пустой или `{ "read": true }`. Response 200 — обновлённое уведомление.

---

## 7. Администратор (R0.4)

Эндпоинты доступны только пользователям с ролью `ADMIN`. При отсутствии прав возвращается 403 Forbidden.

| Метод | Путь | Описание | Требование |
|-------|------|----------|------------|
| GET  | `/api/admin/users` | Список всех пользователей (с пагинацией) | R0.4 |
| GET  | `/api/admin/listings` | Список всех объявлений (с фильтрами по статусу, региону) | R0.4 |
| GET  | `/api/admin/inquiries` | Список всех обращений | R0.4 |
| GET  | `/api/admin/payments` | Список всех платежей за продвижение | R0.4 |
| PATCH| `/api/admin/listings/{listingId}/status` | Изменить статус объявления (модерация: снять с публикации, архивировать) | R0.4 |

### GET /api/admin/users

Query: `page`, `size`, `role` (SELLER | BUYER | ADMIN), `email` (поиск по подстроке).

**Response 200:** постраничный список пользователей (id, email, role, created_at; без password_hash).

### GET /api/admin/listings

Query: `page`, `size`, `status`, `region`, `sellerId`.

**Response 200:** постраничный список объявлений (все поля, включая seller_id).

### GET /api/admin/inquiries

Query: `page`, `size`, `status`, `listingId`, `buyerId`.

**Response 200:** постраничный список обращений.

### GET /api/admin/payments

Query: `page`, `size`, `status`, `listingId`.

**Response 200:** постраничный список платежей.

### PATCH /api/admin/listings/{listingId}/status

**Request body:**

```json
{
  "status": "ARCHIVED"
}
```

Допустимые значения для модерации: `ACTIVE` → `ARCHIVED` (снять с публикации), при необходимости явное переведение в `CLOSED`. Остальные переходы — по бизнес-логике (например, только ARCHIVED). После смены статуса при необходимости отправляется уведомление продавцу.

**Response 200:** обновлённое объявление.

---

## 8. Сводная таблица эндпоинтов

| Метод | Путь | Назначение |
|-------|------|------------|
| POST | `/api/auth/login` | Вход |
| POST | `/api/auth/logout` | Выход |
| GET  | `/api/auth/me` | Текущий пользователь |
| POST | `/api/listings` | Создать объявление (черновик) |
| PUT  | `/api/listings/{listingId}` | Обновить объявление |
| GET  | `/api/listings/{listingId}` | Получить объявление |
| GET  | `/api/seller/listings` | Мои объявления (продавец) |
| POST | `/api/listings/{listingId}/publish` | Разместить объявление |
| POST | `/api/listings/{listingId}/promotion/choice` | Выбор продвижения (да/нет) |
| POST | `/api/listings/{listingId}/promotion/pay` | Инициировать оплату продвижения |
| POST | `/api/webhooks/payment` | Callback оплаты |
| POST | `/api/listings/{listingId}/confirm-relevance` | Продлить или архивировать |
| POST | `/api/listings/{listingId}/close` | Закрыть объявление |
| GET  | `/api/listings/search` | Поиск объявлений (покупатель) |
| POST | `/api/inquiries` | Создать обращение (запрос на показ) |
| GET  | `/api/inquiries` | Список обращений |
| GET  | `/api/inquiries/{inquiryId}` | Получить обращение |
| POST | `/api/inquiries/{inquiryId}/confirm-showing` | Подтвердить показ |
| POST | `/api/inquiries/{inquiryId}/reject-showing` | Отказать в показе |
| POST | `/api/inquiries/{inquiryId}/visit-result` | Результат посещения (покупаю/не покупаю) |
| GET  | `/api/notifications` | Список уведомлений |
| PATCH| `/api/notifications/{notificationId}/read` | Отметить прочитанным |
| GET  | `/api/admin/users` | Список пользователей (админ) |
| GET  | `/api/admin/listings` | Список всех объявлений (админ) |
| GET  | `/api/admin/inquiries` | Список всех обращений (админ) |
| GET  | `/api/admin/payments` | Список всех платежей (админ) |
| PATCH| `/api/admin/listings/{listingId}/status` | Изменить статус объявления (админ) |

---

*Документ подготовлен на основе BACKEND_REQUIREMENTS.md. Версия: 1.0.*

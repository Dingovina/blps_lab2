# Спецификация ролей и привилегий

## 1. Спецификация привилегий

| Привилегия               | Описание                                                                                                |
| ------------------------ | ------------------------------------------------------------------------------------------------------- |
| `AUTH_ACCESS`            | Доступ к эндпоинтам регистрации и входа                                                                 |
| `BASIC_ACCESS`           | Базовые действия авторизованного пользователя (получение профиля, выход из системы)                     |
| `READ_PUBLIC_RESOURCES`  | Просмотр объявлений (поиск, карточка) и доступ к публичным вебхукам                                     |
| `CREATE_LISTING`         | Создание новых объявлений                                                                               |
| `MANAGE_OWN_LISTINGS`    | Обновление, публикация, продвижение и закрытие своих объявлений                                         |
| `CREATE_INQUIRY`         | Подача запроса на показ недвижимости                                                                    |
| `MANAGE_INQUIRIES`       | Просмотр своих/связанных обращений, их подтверждение или отказ, фиксация результата визита              |
| `MANAGE_NOTIFICATIONS`   | Просмотр и отметка прочитанными своих уведомлений                                                       |
| `ADMIN_ACCESS`       | Полный доступ ко всем ресурсам и эндпоинтам системы (обход проверок принадлежности и администрирование) |

## 2. Связь Ролей и Привилегий

| Роль       | Набор привилегий                                                                                                                                           |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **PUBLIC** *(неавторизованный)* | `AUTH_ACCESS`, `READ_PUBLIC_RESOURCES`                                                                                                                     |
| **SELLER** | `AUTH_ACCESS`, `READ_PUBLIC_RESOURCES`, `BASIC_ACCESS`, `CREATE_LISTING`, `MANAGE_OWN_LISTINGS`, `MANAGE_INQUIRIES`, `MANAGE_NOTIFICATIONS`                |
| **BUYER**  | `AUTH_ACCESS`, `READ_PUBLIC_RESOURCES`, `BASIC_ACCESS`, `CREATE_INQUIRY`, `MANAGE_INQUIRIES`, `MANAGE_NOTIFICATIONS`                                       |
| **ADMIN**  | **ВСЕ ПРИВИЛЕГИИ СИСТЕМЫ** (`AUTH_ACCESS`, `READ_PUBLIC_RESOURCES`, `BASIC_ACCESS`, `CREATE_LISTING`, `MANAGE_OWN_LISTINGS`, `CREATE_INQUIRY`, `MANAGE_INQUIRIES`, `MANAGE_NOTIFICATIONS`, `ADMIN_ACCESS`) |

---

## 3. Матрица доступа к REST API

### Аутентификация

| Endpoint                     | Требуемая привилегия                 | PUBLIC | SELLER | BUYER | ADMIN |
| ---------------------------- | ------------------------------------ | ------ | ------ | ----- | ----- |
| `POST /api/auth/register`    | `AUTH_ACCESS`                        | ✅      | ✅      | ✅     | ✅     |
| `POST /api/auth/login`       | `AUTH_ACCESS`                        | ✅      | ✅      | ✅     | ✅     |
| `POST /api/auth/logout`      | `BASIC_ACCESS` или `ADMIN_ACCESS`| ❌      | ✅      | ✅     | ✅     |
| `GET /api/auth/me`           | `BASIC_ACCESS` или `ADMIN_ACCESS`| ❌      | ✅      | ✅     | ✅     |

### Объявления (Listings)

| Endpoint                                          | Требуемая привилегия                        | PUBLIC | SELLER     | BUYER | ADMIN |
| ------------------------------------------------- | ------------------------------------------- | ------ | ---------- | ----- | ----- |
| `GET /api/listings/search`                        | `READ_PUBLIC_RESOURCES`                     | ✅      | ✅          | ✅     | ✅     |
| `GET /api/listings/{listingId}`                   | `READ_PUBLIC_RESOURCES`                     | ✅      | ✅          | ✅     | ✅     |
| `POST /api/listings`                              | `CREATE_LISTING` или `ADMIN_ACCESS`     | ❌      | ✅          | ❌     | ✅     |
| `PUT /api/listings/{listingId}`                   | `MANAGE_OWN_LISTINGS` или `ADMIN_ACCESS`| ❌      | ✅ (owner)  | ❌     | ✅     |
| `GET /api/seller/listings`                        | `MANAGE_OWN_LISTINGS` или `ADMIN_ACCESS`| ❌      | ✅ (owner)  | ❌     | ✅     |
| `POST /api/listings/{listingId}/publish`          | `MANAGE_OWN_LISTINGS` или `ADMIN_ACCESS`| ❌      | ✅ (owner)  | ❌     | ✅     |
| `POST /api/listings/{listingId}/promotion/choice` | `MANAGE_OWN_LISTINGS` или `ADMIN_ACCESS`| ❌      | ✅ (owner)  | ❌     | ✅     |
| `POST /api/listings/{listingId}/promotion/pay`    | `MANAGE_OWN_LISTINGS` или `ADMIN_ACCESS`| ❌      | ✅ (owner)  | ❌     | ✅     |
| `POST /api/listings/{listingId}/confirm-relevance`| `MANAGE_OWN_LISTINGS` или `ADMIN_ACCESS`| ❌      | ✅ (owner)  | ❌     | ✅     |
| `POST /api/listings/{listingId}/close`            | `MANAGE_OWN_LISTINGS` или `ADMIN_ACCESS`| ❌      | ✅ (owner)  | ❌     | ✅     |

### Уведомления от платёжных систем

*Доступ к публичному вебхуку не должен ограничиваться для авторизованных пользователей.*

| Endpoint                     | Требуемая привилегия                 | PUBLIC | SELLER | BUYER | ADMIN |
| ---------------------------- | ------------------------------------ | ------ | ------ | ----- | ----- |
| `POST /api/webhooks/payment` | `READ_PUBLIC_RESOURCES`              | ✅      | ✅      | ✅     | ✅     |

### Обращения (Запросы на показ - Inquiries)

| Endpoint                                      | Требуемая привилегия                       | PUBLIC | SELLER        | BUYER      | ADMIN |
| --------------------------------------------- | ------------------------------------------ | ------ | ------------- | ---------- | ----- |
| `POST /api/inquiries`                         | `CREATE_INQUIRY` или `ADMIN_ACCESS`    | ❌      | ❌             | ✅          | ✅     |
| `GET /api/inquiries`                          | `MANAGE_INQUIRIES` или `ADMIN_ACCESS`  | ❌      | ✅ (related)   | ✅ (owner)  | ✅     |
| `GET /api/inquiries/{inquiryId}`              | `MANAGE_INQUIRIES` или `ADMIN_ACCESS`  | ❌      | ✅ (related)   | ✅ (owner)  | ✅     |
| `POST /api/inquiries/{inquiryId}/confirm-showing`| `MANAGE_INQUIRIES` или `ADMIN_ACCESS`  | ❌      | ✅ (related)   | ❌          | ✅     |
| `POST /api/inquiries/{inquiryId}/reject-showing` | `MANAGE_INQUIRIES` или `ADMIN_ACCESS`  | ❌      | ✅ (related)   | ❌          | ✅     |
| `POST /api/inquiries/{inquiryId}/visit-result`   | `MANAGE_INQUIRIES` или `ADMIN_ACCESS`  | ❌      | ❌             | ✅ (owner)  | ✅     |

### Уведомления (Notifications)

| Endpoint                                   | Требуемая привилегия                         | PUBLIC | SELLER     | BUYER      | ADMIN |
| ------------------------------------------ | -------------------------------------------- | ------ | ---------- | ---------- | ----- |
| `GET /api/notifications`                   | `MANAGE_NOTIFICATIONS` или `ADMIN_ACCESS`| ❌      | ✅ (owner)  | ✅ (owner)  | ✅     |
| `PATCH /api/notifications/{id}/read`       | `MANAGE_NOTIFICATIONS` или `ADMIN_ACCESS`| ❌      | ✅ (owner)  | ✅ (owner)  | ✅     |

### Администрирование и модерация

| Endpoint                                 | Требуемая привилегия   | PUBLIC | SELLER | BUYER | ADMIN |
| ---------------------------------------- | ---------------------- | ------ | ------ | ----- | ----- |
| `GET /api/admin/users`                   | `ADMIN_ACCESS`     | ❌      | ❌      | ❌     | ✅     |
| `GET /api/admin/listings`                | `ADMIN_ACCESS`     | ❌      | ❌      | ❌     | ✅     |
| `GET /api/admin/inquiries`               | `ADMIN_ACCESS`     | ❌      | ❌      | ❌     | ✅     |
| `GET /api/admin/payments`                | `ADMIN_ACCESS`     | ❌      | ❌      | ❌     | ✅     |
| `PATCH /api/admin/listings/{id}/status`  | `ADMIN_ACCESS`     | ❌      | ❌      | ❌     | ✅     |

# Запуск Cian в Docker

Пошаговая инструкция для локального запуска без Helios/WildFly.

## Что получится

- **PostgreSQL** — база с таблицами из `src/sql/schema.sql`
- **RabbitMQ** — очередь сообщений для асинхронной email-доставки уведомлений
- **Mailpit** — локальный SMTP-сервер и web UI для проверки отправленных писем
- **Spring Boot API** — основное приложение на порту **8080** (профиль `docker,api`, JDBC вместо JNDI)
- **2 Spring Boot worker-узла** — получают уведомления из RabbitMQ через JMS и отправляют email через SMTP

## 1. Установить Docker

### Windows (WSL2)

1. Установите [Docker Desktop](https://www.docker.com/products/docker-desktop/).
2. В настройках включите **Use the WSL 2 based engine**.
3. В Docker Desktop → **Settings → Resources → WSL Integration** включите интеграцию с вашим дистрибутивом (Ubuntu).
4. Перезапустите терминал WSL.

Проверка:

```bash
docker --version
docker compose version
```

Оба команды должны вывести версию без ошибок.

## 2. Перейти в каталог проекта

```bash
cd /home/dingovina/vs_code/blps_lab2
```

## 3. Сделать скрипты исполняемыми (один раз)

```bash
chmod +x docker/scripts/*.sh
```

## 4. Запустить всё

```bash
./docker/scripts/up.sh
```

Первый запуск может занять несколько минут: скачиваются образы Java/PostgreSQL и собирается JAR через Gradle.

## 5. Проверить

| Что | URL |
|-----|-----|
| API (любой публичный endpoint) | http://localhost:8080/api/listings/search |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| RabbitMQ Management | http://localhost:15672 |
| Mailpit | http://localhost:8025 |

RabbitMQ:

- **Login:** `cian`
- **Password:** `cian`

Учётная запись администратора (создаётся при старте, если в БД ещё нет ADMIN):

- **Email:** `admin@cian.local`
- **Пароль:** `admin123`

Авторизация в API — HTTP Basic (как на Helios).

Пример:

```bash
curl -u admin@cian.local:admin123 http://localhost:8080/api/admin/users
```

## 6. Полезные команды

```bash
# Логи API и worker-узлов (Ctrl+C — выход)
./docker/scripts/logs.sh

# Логи конкретного worker-узла
./docker/scripts/logs.sh worker-1
./docker/scripts/logs.sh worker-2

# Остановить контейнеры (данные БД сохраняются)
./docker/scripts/down.sh

# Полный сброс БД и перезапуск
./docker/scripts/reset-db.sh
```

## 7. Ручной запуск без скриптов

```bash
cd docker
docker compose up --build
```

Остановка: `Ctrl+C`, затем `docker compose down`.

## Устранение проблем

**`permission denied` при запуске скрипта** — выполните `chmod +x docker/scripts/*.sh`.

**Порт 8080 или 5432 занят** — измените в `docker/docker-compose.yml`:

```yaml
ports:
  - "8081:8080"   # app
  - "5433:5432"   # db
```

**Приложение падает с ошибкой схемы БД** — volume мог остаться от старой версии схемы:

```bash
./docker/scripts/reset-db.sh
```

**Docker не видит демон в WSL** — убедитесь, что Docker Desktop запущен в Windows.

## Отличие от Helios

| Helios (лаб.) | Docker (локально) |
|---------------|-------------------|
| WildFly + JNDI `java:/jdbc/studs` | Встроенный Tomcat + JDBC URL |
| Схема `s409599` | Схема `public` |
| Деплой WAR | Запуск `bootJar` |

Профиль Helios в `application.yml` не меняется; для Docker используется `application-docker.yml` (`SPRING_PROFILES_ACTIVE=docker`).

## Асинхронные уведомления

Основной контейнер `app` создаёт уведомление в таблице `cian_notifications`, поэтому личный кабинет продолжает работать через `GET /api/notifications`.

После сохранения уведомления `app` публикует `NotificationEvent` в очередь RabbitMQ `cian.notifications` через JMS API (`JmsTemplate`).

Контейнеры `worker-1` и `worker-2` запущены с профилем `worker`. Они слушают одну очередь через JMS (`@JmsListener`) и конкурируют за сообщения: каждое событие получает только один worker. Получив событие, worker отправляет письмо на email пользователя через SMTP.

Локально SMTP принимает Mailpit. Откройте http://localhost:8025, чтобы увидеть реально отправленные письма без отправки во внешний интернет.

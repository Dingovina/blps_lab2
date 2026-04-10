-- ============================================================
-- CIAN: тестовые данные (PostgreSQL)
-- Запуск: psql -U <user> -d <database> -f src/sql/seed.sql
-- Перед первым запуском схема должна быть создана (src/sql/schema.sql).
-- Скрипт очищает таблицы cian_* и заполняет заново (идемпотентно для dev).
-- Пароли (BCrypt): admin — admin123; seller* — seller123; buyer* — buyer123;
-- seller+buyer@test.local — pass123.
-- На helios при необходимости: SET search_path TO <ваша_схема>, public;
-- ============================================================

BEGIN;

TRUNCATE TABLE cian_notifications,
              cian_inquiries,
              cian_payments,
              cian_listings,
              cian_users
RESTART IDENTITY CASCADE;

-- ----- Пользователи -----
INSERT INTO cian_users (id, email, password_hash, role, created_at) VALUES
  (1, 'admin@cian.local',
   '$2a$10$e/.nWCqPH.DmEvM2YHCn2OjvkrlW8IzfMriJCnSXE.HChMFVMYty.',
   'ADMIN', now() - interval '60 days'),
  (2, 'seller1@test.local',
   '$2a$10$CpXz6fwvojiCQEvJGUgYMOrt3yljTpoz3gtjH7n0IIDbFacf7Njs2',
   'SELLER', now() - interval '45 days'),
  (3, 'seller2@test.local',
   '$2a$10$CpXz6fwvojiCQEvJGUgYMOrt3yljTpoz3gtjH7n0IIDbFacf7Njs2',
   'SELLER', now() - interval '30 days'),
  (4, 'buyer1@test.local',
   '$2a$10$8zP0iM5LTgwpzJB7.aeF0uXG90fPswYlSTlJWkFa1dnqEPiu4f4xC',
   'BUYER', now() - interval '20 days'),
  (5, 'buyer2@test.local',
   '$2a$10$8zP0iM5LTgwpzJB7.aeF0uXG90fPswYlSTlJWkFa1dnqEPiu4f4xC',
   'BUYER', now() - interval '15 days'),
  (6, 'seller+buyer@test.local',
   '$2a$10$5mQ4ZJuA.qy0FKGhsfTcnOlRDchOQ/5wOYMIQBZDjB53OCrRxhRTO',
   'SELLER', now() - interval '10 days');

-- ----- Объявления -----
INSERT INTO cian_listings (
  id, seller_id, title, description, address, region, price, area_sqm, rooms,
  status, promotion, published_at, expires_at, closed_at, created_at
) VALUES
  (1, 2, 'Черновик: студия на окраине',
   'Черновик, фото добавлю позже', 'ул. Новая, 1', 'Москва',
   5200000.00, 28.5, 0, 'DRAFT', 'NONE',
   NULL, NULL, NULL, now() - interval '2 days'),
  (2, 2, 'Топ: 3-к квартира у метро',
   'Ремонт, кухня-гостиная', 'Невский пр., 10', 'Санкт-Петербург',
   18500000.00, 78.0, 3, 'ACTIVE', 'TOP',
   now() - interval '12 days', now() + interval '18 days', NULL, now() - interval '14 days'),
  (3, 2, 'Премиум: новостройка в центре',
   'Сдача в этом году', 'ул. Центральная, 5', 'Москва',
   24500000.00, 95.5, 4, 'ACTIVE', 'PREMIUM',
   now() - interval '25 days', now() + interval '5 days', NULL, now() - interval '26 days'),
  (4, 3, 'Обычное объявление: 2-к в Казани',
   'Тихий двор, парковка', 'ул. Баумана, 3', 'Казань',
   9200000.00, 55.0, 2, 'ACTIVE', 'NONE',
   now() - interval '8 days', now() + interval '22 days', NULL, now() - interval '9 days'),
  (5, 3, 'Архив: дача',
   'Участок 6 соток', 'СНТ Ромашка', 'Ленинградская область',
   3100000.00, 45.0, 2, 'ARCHIVED', 'NONE',
   now() - interval '120 days', now() - interval '90 days', NULL, now() - interval '121 days'),
  (6, 2, 'Закрыто: продано',
   'Сделка завершена', 'ул. Мира, 20', 'Москва',
   11200000.00, 60.0, 2, 'CLOSED', 'NONE',
   now() - interval '200 days', now() - interval '170 days',
   now() - interval '30 days', now() - interval '201 days'),
  (7, 6, 'Скоро истекает срок (для напоминаний)',
   'Тест ARCHIVATION_SOON по сроку', 'пр-т Мира, 100', 'Москва',
   6700000.00, 42.0, 2, 'ACTIVE', 'NONE',
   now() - interval '29 days', now() + interval '1 day', NULL, now() - interval '29 days');

-- ----- Платежи за продвижение -----
INSERT INTO cian_payments (
  id, listing_id, user_id, promotion_type, status, external_id, amount_cents, created_at
) VALUES
  (1, 2, 2, 'TOP', 'SUCCESS', 'mock-pay-top-001', 199900, now() - interval '12 days'),
  (2, 2, 2, 'PREMIUM', 'FAILED', 'mock-pay-prem-fail', 499900, now() - interval '11 days'),
  (3, 3, 2, 'PREMIUM', 'SUCCESS', 'mock-pay-prem-ok', 499900, now() - interval '25 days'),
  (4, 4, 3, 'TOP', 'PENDING', NULL, 199900, now() - interval '1 hour');

-- ----- Обращения / показы -----
INSERT INTO cian_inquiries (
  id, listing_id, buyer_id, message, status, scheduled_at, contact_info,
  reject_reason, will_buy, created_at
) VALUES
  (1, 2, 4, 'Здравствуйте, хочу посмотреть в субботу.', 'PENDING',
   NULL, NULL, NULL, NULL, now() - interval '3 days'),
  (2, 2, 5, 'Можно во вторник вечером?', 'SHOWING_SCHEDULED',
   now() + interval '2 days', 'Позвоните: +7-900-000-00-02', NULL, NULL, now() - interval '2 days'),
  (3, 3, 4, 'Интересует ипотека.', 'SHOWING_REJECTED',
   NULL, NULL, 'Нет свободных слотов на этой неделе', NULL, now() - interval '5 days'),
  (4, 4, 5, 'Готов приехать завтра.', 'COMPLETED',
   now() - interval '3 days', 'WhatsApp в профиле', NULL, true, now() - interval '7 days'),
  (5, 7, 4, 'Есть ли торг?', 'PENDING',
   NULL, NULL, NULL, NULL, now() - interval '6 hours');

-- ----- Уведомления -----
INSERT INTO cian_notifications (
  id, user_id, type, title, body, related_entity_type, related_entity_id, read, created_at
) VALUES
  (1, 2, 'LISTING_PUBLISHED', 'Объявление опубликовано',
   'Ваше объявление «Топ: 3-к квартира у метро» доступно.', 'LISTING', 2, true,
   now() - interval '12 days'),
  (2, 2, 'PROMOTION_ACTIVATED', 'Продвижение активировано',
   'Тариф TOP для объявления #2.', 'LISTING', 2, true, now() - interval '12 days'),
  (3, 2, 'PROMOTION_PAYMENT_FAILED', 'Ошибка оплаты',
   'Не удалось списать оплату за PREMIUM.', 'PAYMENT', 2, false, now() - interval '11 days'),
  (4, 2, 'ARCHIVATION_SOON', 'Срок размещения скоро истечёт',
   'Объявление #7 будет архивировано через ~1 день.', 'LISTING', 7, false,
   now() - interval '10 hours'),
  (5, 2, 'NEW_INQUIRY', 'Новое обращение',
   'Покупатель оставил сообщение по объявлению #2.', 'INQUIRY', 1, false,
   now() - interval '3 days'),
  (6, 2, 'SHOWING_SCHEDULED', 'Назначен показ',
   'Показ по объявлению #2 согласован.', 'INQUIRY', 2, true, now() - interval '2 days'),
  (7, 4, 'SHOWING_REJECTED', 'Показ не состоялся',
   'Продавец отклонил показ по объявлению #3.', 'INQUIRY', 3, false,
   now() - interval '5 days'),
  (8, 2, 'LISTING_CLOSED', 'Объявление закрыто',
   'Объявление #6 заказчик закрыл/сделка.', 'LISTING', 6, true,
   now() - interval '30 days'),
  (9, 3, 'NEW_INQUIRY', 'Новое обращение по вашему объявлению',
   'Сообщение от покупателя.', 'LISTING', 4, false, now() - interval '1 day'),
  (10, 3, 'PUBLICATION_EXTENDED', 'Срок продлён',
   'Размещение объявления #4 продлено.', 'LISTING', 4, true, now() - interval '20 days');

-- ----- Синхронизация последовательностей после явных id -----
SELECT setval(pg_get_serial_sequence('cian_users', 'id'),
              (SELECT COALESCE(MAX(id), 1) FROM cian_users));
SELECT setval(pg_get_serial_sequence('cian_listings', 'id'),
              (SELECT COALESCE(MAX(id), 1) FROM cian_listings));
SELECT setval(pg_get_serial_sequence('cian_payments', 'id'),
              (SELECT COALESCE(MAX(id), 1) FROM cian_payments));
SELECT setval(pg_get_serial_sequence('cian_inquiries', 'id'),
              (SELECT COALESCE(MAX(id), 1) FROM cian_inquiries));
SELECT setval(pg_get_serial_sequence('cian_notifications', 'id'),
              (SELECT COALESCE(MAX(id), 1) FROM cian_notifications));

COMMIT;

-- ============================================================
-- CIAN: очистка данных таблиц (без удаления схемы)
-- Запуск: psql -U <user> -d <database> -f src/sql/clean.sql
-- Таблицы и индексы сохраняются, данные удаляются, счётчики ID сбрасываются.
-- ============================================================

TRUNCATE TABLE cian_notifications,
              cian_inquiries,
              cian_payments,
              cian_listings,
              cian_users
RESTART IDENTITY CASCADE;

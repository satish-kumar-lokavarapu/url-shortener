-- Creates the database. Run once:
--   mysql -u root -p < scripts/local-mysql-setup.sql
-- Tables are created by Flyway when the app starts.

CREATE DATABASE IF NOT EXISTS url_shortener
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
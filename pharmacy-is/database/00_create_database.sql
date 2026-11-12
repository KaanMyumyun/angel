-- =====================================================================
--  Аптека - Информационна система
--  СТЪПКА 1: създаване на базата и потребител за приложението.
--  Изпълнява се ВЕДНЪЖ, от администраторски акаунт (root):
--      mysql -u root -p < database/00_create_database.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS pharmacy
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Отделен потребител за приложението (да не се ползва root).
-- Сменете паролата и я отразете в src/main/resources/application.properties
CREATE USER IF NOT EXISTS 'pharmacy_app'@'localhost' IDENTIFIED BY 'pharmacy_pass';
GRANT SELECT, INSERT, UPDATE, DELETE ON pharmacy.* TO 'pharmacy_app'@'localhost';
FLUSH PRIVILEGES;

-- =====================================================================
--  Аптека - Информационна система
--  СТЪПКА 2: таблици.
--      mysql -u root -p pharmacy < database/01_schema.sql
--
--  Скриптът е идемпотентен (IF NOT EXISTS) - може да се пуска повторно.
--  Наличността се води по ПАРТИДИ (batches): всяка доставка създава
--  партида със собствен срок на годност. Продажбата изчерпва първо
--  партидата с най-ранен срок (FEFO - First Expired, First Out).
-- =====================================================================

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT ck_users_role CHECK (role IN ('MANAGER', 'PHARMACIST'))
);

CREATE TABLE IF NOT EXISTS suppliers (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(150) NOT NULL,
    eik            VARCHAR(20),
    contact_person VARCHAR(120),
    phone          VARCHAR(40),
    email          VARCHAR(120),
    address        VARCHAR(255),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uq_suppliers_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS medicines (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    name                  VARCHAR(150)  NOT NULL,
    active_ingredient     VARCHAR(150)  NOT NULL,
    form                  VARCHAR(40)   NOT NULL,
    strength              VARCHAR(40),
    unit_price            DECIMAL(10,2) NOT NULL,
    min_quantity          INT           NOT NULL DEFAULT 10,
    prescription_required BOOLEAN       NOT NULL DEFAULT FALSE,
    active                BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT ck_medicines_price CHECK (unit_price >= 0),
    CONSTRAINT ck_medicines_min_qty CHECK (min_quantity >= 0),
    INDEX idx_medicines_name (name),
    INDEX idx_medicines_ingredient (active_ingredient)
);

CREATE TABLE IF NOT EXISTS deliveries (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    supplier_id   BIGINT      NOT NULL,
    delivery_date DATE        NOT NULL,
    document_no   VARCHAR(50),
    user_id       BIGINT      NOT NULL,
    note          VARCHAR(255),
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_deliveries_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_deliveries_user     FOREIGN KEY (user_id)     REFERENCES users (id)
);

-- Една партида = един ред от една доставка.
-- quantity_received  = колко е доставено (не се променя - за справки)
-- quantity_available = колко е останало в момента (намалява при продажба)
CREATE TABLE IF NOT EXISTS batches (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    medicine_id        BIGINT        NOT NULL,
    delivery_id        BIGINT        NOT NULL,
    batch_no           VARCHAR(50),
    expiry_date        DATE          NOT NULL,
    quantity_received  INT           NOT NULL,
    quantity_available INT           NOT NULL,
    unit_cost          DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_batches_medicine FOREIGN KEY (medicine_id) REFERENCES medicines (id),
    CONSTRAINT fk_batches_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries (id),
    -- Пази наличността от разминаване дори при грешка в кода:
    CONSTRAINT ck_batches_available CHECK (quantity_available >= 0),
    CONSTRAINT ck_batches_received  CHECK (quantity_received  >  0),
    CONSTRAINT ck_batches_not_over  CHECK (quantity_available <= quantity_received),
    INDEX idx_batches_medicine_expiry (medicine_id, expiry_date)
);

CREATE TABLE IF NOT EXISTS sales (
    id      BIGINT        NOT NULL AUTO_INCREMENT,
    sold_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT        NOT NULL,
    total   DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sales_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_sales_total CHECK (total >= 0),
    INDEX idx_sales_sold_at (sold_at)
);

CREATE TABLE IF NOT EXISTS sale_items (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    sale_id     BIGINT        NOT NULL,
    medicine_id BIGINT        NOT NULL,
    batch_id    BIGINT        NOT NULL,
    quantity    INT           NOT NULL,
    unit_price  DECIMAL(10,2) NOT NULL,
    line_total  DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sale_items_sale     FOREIGN KEY (sale_id)     REFERENCES sales (id),
    CONSTRAINT fk_sale_items_medicine FOREIGN KEY (medicine_id) REFERENCES medicines (id),
    CONSTRAINT fk_sale_items_batch    FOREIGN KEY (batch_id)    REFERENCES batches (id),
    CONSTRAINT ck_sale_items_qty CHECK (quantity > 0)
);

-- Одитна следа: всяко движение на наличност оставя ред тук.
-- Позволява да се докаже, че наличността никога не "скача" без причина.
CREATE TABLE IF NOT EXISTS stock_movements (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    medicine_id BIGINT      NOT NULL,
    batch_id    BIGINT      NOT NULL,
    delta       INT         NOT NULL,
    reason      VARCHAR(20) NOT NULL,
    ref_id      BIGINT,
    user_id     BIGINT      NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_moves_medicine FOREIGN KEY (medicine_id) REFERENCES medicines (id),
    CONSTRAINT fk_moves_batch    FOREIGN KEY (batch_id)    REFERENCES batches (id),
    CONSTRAINT fk_moves_user     FOREIGN KEY (user_id)     REFERENCES users (id),
    CONSTRAINT ck_moves_reason CHECK (reason IN ('DELIVERY', 'SALE', 'WRITE_OFF', 'CORRECTION')),
    CONSTRAINT ck_moves_delta  CHECK (delta <> 0),
    INDEX idx_moves_medicine (medicine_id, created_at)
);


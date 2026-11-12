# ER диаграма на базата данни

```mermaid
erDiagram
    USERS ||--o{ SALES : "регистрира"
    USERS ||--o{ DELIVERIES : "приема"
    USERS ||--o{ STOCK_MOVEMENTS : "извършва"

    SUPPLIERS ||--o{ DELIVERIES : "доставя"
    DELIVERIES ||--|{ BATCHES : "създава"

    MEDICINES ||--o{ BATCHES : "се съхранява в"
    MEDICINES ||--o{ SALE_ITEMS : "се продава като"
    MEDICINES ||--o{ STOCK_MOVEMENTS : "се движи"

    SALES ||--|{ SALE_ITEMS : "съдържа"
    BATCHES ||--o{ SALE_ITEMS : "от която е продадено"
    BATCHES ||--o{ STOCK_MOVEMENTS : "засяга"

    USERS {
        bigint id PK
        varchar username UK
        varchar password_hash "BCrypt"
        varchar full_name
        varchar role "MANAGER | PHARMACIST"
        boolean active
    }

    SUPPLIERS {
        bigint id PK
        varchar name UK
        varchar eik
        varchar contact_person
        varchar phone
        varchar email
        varchar address
        boolean active
    }

    MEDICINES {
        bigint id PK
        varchar name
        varchar active_ingredient
        varchar form
        varchar strength
        decimal unit_price
        int min_quantity "праг за предупреждение"
        boolean prescription_required
        boolean active
    }

    DELIVERIES {
        bigint id PK
        bigint supplier_id FK
        date delivery_date
        varchar document_no
        bigint user_id FK
        varchar note
    }

    BATCHES {
        bigint id PK
        bigint medicine_id FK
        bigint delivery_id FK
        varchar batch_no
        date expiry_date
        int quantity_received "не се променя"
        int quantity_available "намалява при продажба"
        decimal unit_cost
    }

    SALES {
        bigint id PK
        timestamp sold_at
        bigint user_id FK
        decimal total
    }

    SALE_ITEMS {
        bigint id PK
        bigint sale_id FK
        bigint medicine_id FK
        bigint batch_id FK
        int quantity
        decimal unit_price
        decimal line_total
    }

    STOCK_MOVEMENTS {
        bigint id PK
        bigint medicine_id FK
        bigint batch_id FK
        int delta "+ доставка / - продажба"
        varchar reason
        bigint ref_id
        bigint user_id FK
        timestamp created_at
    }
```

## Защо наличността се води по партиди

Заданието изисква лекарството да има „налично количество“ и „срок на годност“.
На практика **две доставки на едно и също лекарство имат различни срокове**, затова
срокът е свойство на партидата, а не на лекарството.

Наличността на едно лекарство е сборът от наличните количества на неговите
**неизтекли** партиди. Тя се изчислява със заявка (`StockRepository`), а не се
дублира като колона — така не може да се разсинхронизира.

## Как се гарантира, че наличностите не се разминават

| Механизъм | Къде | Какво предотвратява |
|---|---|---|
| Една транзакция за цялата продажба | `SaleService.checkout` | Частичен запис при грешка по средата |
| `SELECT ... FOR UPDATE` върху партидите | `BatchRepository.findForUpdate` | Две едновременни продажби на последните бройки |
| `UPDATE ... WHERE quantity_available >= ?` | `BatchRepository.decrease` | Записване на отрицателна наличност |
| `CHECK (quantity_available >= 0)` | `01_schema.sql` | Разминаване дори при грешка в кода |
| `CHECK (quantity_available <= quantity_received)` | `01_schema.sql` | „Връщане“ на повече, отколкото е доставено |
| Таблица `stock_movements` | при всяка промяна | Невъзможност да се обоснове наличността |

Сверката „сборът на движенията = текущата наличност“ се проверява автоматично
от теста `movementsReconcileWithStock`.

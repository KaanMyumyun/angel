# Информационна система за аптека

Курсов проект по дисциплина „Информационни системи“ — **Задание 4**.

Уеб приложение за управление на аптека: каталог на лекарствата, наличности по
партиди, продажби с автоматично намаляване на наличността, доставки, търсене,
предупреждения за ниска наличност и изтичащ срок, и справки за управителя.

**Технологии:** Java 17+ · Spring Boot 3.5 · Thymeleaf · Spring Security · MySQL 8 · Maven

---

## 1. Какво е необходимо

| | Версия | Проверка |
|---|---|---|
| **JDK** | 17 или по-нова | `java -version` |
| **MySQL** | 8.0 или по-нова | `mysql --version` |
| Maven | *не е нужен* — в проекта има `mvnw` | |

> Windows: инсталирайте MySQL Community Server или XAMPP.
> Linux: `sudo dnf install mysql-server` / `sudo apt install mysql-server`.
> macOS: `brew install mysql`.

---

## 2. Подготовка на базата данни

### Вариант А — най-бързият (препоръчителен)

Създайте базата и потребителя веднъж:

```bash
mysql -u root -p < database/00_create_database.sql
```

Това създава база `pharmacy` и потребител `pharmacy_app` с парола `pharmacy_pass`.
**Таблиците и примерните данни се създават автоматично при първото стартиране.**

### Вариант Б — ръчно импортиране

Ако предпочитате да видите всичко в MySQL Workbench / phpMyAdmin, импортирайте
трите файла по ред:

```bash
mysql -u root -p < database/00_create_database.sql
mysql -u root -p pharmacy < database/01_schema.sql
mysql -u root -p pharmacy < database/02_seed.sql
```

| Файл | Какво прави |
|---|---|
| `database/00_create_database.sql` | Създава базата `pharmacy` и потребител за приложението. |
| `database/01_schema.sql` | Създава 8-те таблици с ключове, индекси и ограничения. |
| `database/02_seed.sql` | Зарежда примерни данни: 2 потребителя, 4 доставчика, 14 лекарства, 3 доставки, 16 партиди. |

И трите скрипта са **идемпотентни** — могат да се пускат повторно, без да дублират данни.

---

## 3. Настройка на връзката

Единственият файл, който може да се наложи да промените, е
`src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/pharmacy?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Europe/Sofia&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=pharmacy_app
spring.datasource.password=pharmacy_pass
```

Ако ползвате `root`, сменете `username` и `password` съответно.

Други полезни настройки:

```properties
pharmacy.expiry-warning-days=90      # праг за "изтичащ срок"
pharmacy.auto-init-database=true     # създаване на таблиците при първо пускане
pharmacy.auto-seed-demo-data=true    # false => чиста аптека, без примерни данни
```

---

## 4. Стартиране

```bash
./mvnw spring-boot:run          # Linux / macOS
mvnw.cmd spring-boot:run        # Windows
```

Отворете <http://localhost:8080>

### Или като самостоятелен JAR файл

```bash
./mvnw clean package
java -jar target/pharmacy-is.jar
```

---

## 5. Потребители за вход

| Роля | Потребител | Парола | Достъп |
|---|---|---|---|
| Управител | `manager` | `manager123` | Всичко + справки, каталог, доставчици |
| Фармацевт | `pharmacist` | `pharma123` | Каса, склад, доставки, продажби |

Паролите се съхраняват като **BCrypt хешове**, не в явен вид.

---

## 6. Как се използва

**Продажба** — „Каса“ → въведете име или активна съставка → „Търси“ → задайте
количество → „Добави“ → „Приключи продажбата“.
Сумата се изчислява автоматично, наличността намалява веднага, издава се касова бележка.

**Доставка** — „Доставки“ → „Нова доставка“ → изберете доставчик и дата →
за всеки артикул: лекарство, партида, срок на годност, количество, доставна цена →
„Заприходи доставката“. Наличностите се увеличават веднага.

**Предупреждения** — показват се на началния екран при вход и в секция „Склад“:
изтекъл срок, изтичащ срок (до 90 дни) и наличност под минимума.

**Справки** (само управител) — дневен оборот, най-продавани продукти,
приход / себестойност / печалба, стойност на склада.

---

## 7. Тестове

```bash
./mvnw test
```

45 теста — **не изискват инсталиран MySQL**. Подробности: [docs/04-testing.md](docs/04-testing.md).

---

## 8. Документация

| Документ | Съдържание |
|---|---|
| [docs/01-use-case.md](docs/01-use-case.md) | Use Case диаграма, актьори, сценарий на продажба |
| [docs/02-er-diagram.md](docs/02-er-diagram.md) | ER диаграма и как се гарантира целостта на наличностите |
| [docs/03-class-diagram.md](docs/03-class-diagram.md) | Класова диаграма и архитектура по слоеве |
| [docs/04-testing.md](docs/04-testing.md) | Обхват на тестовете и сценарии за защита |

---

## 9. Структура на проекта

```
pharmacy-is/
├── START-HERE.txt             кратки стъпки за пускане (започни оттук)
├── Dokumentaciya.docx         пълната документация на курсовата работа
├── database/                  SQL скриптове (създаване, схема, примерни данни)
├── docs/                      диаграми и документация
├── src/main/java/bg/pharmacy/
│   ├── config/                сигурност, настройки, първоначално създаване на базата
│   ├── logic/                 чиста бизнес логика (FEFO, срокове) - без база
│   ├── model/                 обекти на предметната област
│   ├── repository/            SQL чрез JdbcTemplate (само подготвени заявки)
│   ├── service/               бизнес логика и граници на транзакциите
│   └── web/                   контролери
├── src/main/resources/
│   ├── templates/             Thymeleaf шаблони
│   ├── static/css/            стилове (без външни библиотеки)
│   └── application.properties
└── src/test/java/             45 теста
```

---

## 10. Често срещани проблеми

| Съобщение | Причина и решение |
|---|---|
| `Access denied for user 'pharmacy_app'` | Не е пуснат `00_create_database.sql`, или паролата в `application.properties` не съвпада. |
| `Communications link failure` | MySQL не работи. Стартирайте услугата (`sudo systemctl start mysqld`, или от XAMPP контролния панел). |
| `Unknown database 'pharmacy'` | Пуснете `database/00_create_database.sql`. |
| `Public Key Retrieval is not allowed` | В URL-а трябва да има `allowPublicKeyRetrieval=true` (вече е добавено). |
| Port 8080 is already in use | Сменете `server.port` в `application.properties`. |
| Кирилицата излиза с въпросителни | Базата трябва да е `utf8mb4` — създайте я чрез `00_create_database.sql`. |

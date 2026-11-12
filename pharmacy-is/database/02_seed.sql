-- =====================================================================
--  Аптека - Информационна система
--  СТЪПКА 3: примерни (демонстрационни) данни.
--      mysql -u root -p pharmacy < database/02_seed.sql
--
--  Скриптът е идемпотентен (INSERT IGNORE + явни ID) - може да се
--  пуска повторно без да дублира редове.
--
--  ПОТРЕБИТЕЛИ ЗА ВХОД:
--    manager    / manager123    (управител - вижда справки и печалби)
--    pharmacist / pharma123     (фармацевт - само ежедневни операции)
--  Паролите са съхранени като BCrypt хешове, не в явен вид.
-- =====================================================================

INSERT IGNORE INTO users (id, username, password_hash, full_name, role) VALUES
 (1, 'manager',    '$2a$10$fgjxhK1y775ME0gQH8kKBOkoOJyvMkzp8VlccAy.Bg6Xh.woq1Ciq', 'Мария Георгиева', 'MANAGER'),
 (2, 'pharmacist', '$2a$10$VCl.q75pI/5eI/8kHJWgwe26/9hxspXUoqtTRe7/BPRnRUdKbemqu', 'Иван Петров',     'PHARMACIST');

INSERT IGNORE INTO suppliers (id, name, eik, contact_person, phone, email, address) VALUES
 (1, 'Софарма Трейдинг АД', '103267194', 'Петър Иванов',   '02 813 4000', 'office@sopharmatrading.bg', 'гр. София, ул. Лъчезар Станчев 5'),
 (2, 'Фьоникс Фарма ЕООД',  '203283523', 'Елена Димитрова','02 926 9100', 'info@phoenixpharma.bg',     'гр. София, ул. Златен клас 22'),
 (3, 'Стинг АД',            '825240500', 'Георги Колев',   '042 617 000', 'sales@sting.bg',            'гр. Стара Загора, ул. Хрищенско шосе 1'),
 (4, 'Търговска Лига АД',   '030276307', 'Анна Тодорова',  '02 903 9000', 'office@tliga.bg',           'гр. София, бул. Г.М. Димитров 1');

INSERT IGNORE INTO medicines (id, name, active_ingredient, form, strength, unit_price, min_quantity, prescription_required) VALUES
 (1,  'Аналгин 500 mg',        'Метамизол натрий',          'таблетки',             '500 mg',      3.20, 20, FALSE),
 (2,  'Парацетамол 500 mg',    'Парацетамол',               'таблетки',             '500 mg',      2.80, 30, FALSE),
 (3,  'Ибупрофен 400 mg',      'Ибупрофен',                 'таблетки',             '400 mg',      4.50, 25, FALSE),
 (4,  'Аспирин Протект',       'Ацетилсалицилова киселина', 'таблетки',             '100 mg',      6.90, 15, FALSE),
 (5,  'Амоксицилин 500 mg',    'Амоксицилин',               'капсули',              '500 mg',      8.40, 10, TRUE),
 (6,  'Азитромицин 500 mg',    'Азитромицин',               'таблетки',             '500 mg',     12.60, 10, TRUE),
 (7,  'Нурофен сироп',         'Ибупрофен',                 'сироп',                '100 mg/5 ml', 9.80, 12, FALSE),
 (8,  'АЦЦ 600',               'Ацетилцистеин',             'ефервесцентни таблетки','600 mg',    11.20, 15, FALSE),
 (9,  'Витамин C 500 mg',      'Аскорбинова киселина',      'таблетки',             '500 mg',      5.60, 40, FALSE),
 (10, 'Витамин D3 2000 IU',    'Холекалциферол',            'капки',                '2000 IU',    14.30, 20, FALSE),
 (11, 'Лоратадин 10 mg',       'Лоратадин',                 'таблетки',             '10 mg',       4.90, 20, FALSE),
 (12, 'Омепразол 20 mg',       'Омепразол',                 'капсули',              '20 mg',       7.50, 15, TRUE),
 (13, 'Лактобацилус пробиотик','Lactobacillus acidophilus', 'капсули',              '10 млрд.',   16.40, 10, FALSE),
 (14, 'Декспантенол крем',     'Декспантенол',              'крем',                 '5%',          8.90, 10, FALSE);

INSERT IGNORE INTO deliveries (id, supplier_id, delivery_date, document_no, user_id, note) VALUES
 (1, 1, DATE_SUB(CURDATE(), INTERVAL 60 DAY), 'ФК-0012345', 1, 'Редовна месечна доставка'),
 (2, 2, DATE_SUB(CURDATE(), INTERVAL 30 DAY), 'ФК-0098765', 1, 'Антибиотици и сиропи'),
 (3, 3, DATE_SUB(CURDATE(), INTERVAL 7  DAY), 'ФК-0055512', 1, 'Витамини и хранителни добавки');

-- Партиди. Сроковете са зададени спрямо текущата дата, за да са
-- демонстрационни независимо кога се пуска скриптът:
--   партида 6  -> изтича след 15 дни  => предупреждение "изтичащ срок"
--   партида 11 -> ВЕЧЕ изтекла        => изключва се от продажба
--   медикамент 13 -> под минимума     => предупреждение "ниска наличност"
INSERT IGNORE INTO batches (id, medicine_id, delivery_id, batch_no, expiry_date, quantity_received, quantity_available, unit_cost) VALUES
 (1,  1,  1, 'AN-2401', DATE_ADD(CURDATE(), INTERVAL 400 DAY), 200, 184, 1.90),
 (2,  2,  1, 'PA-2402', DATE_ADD(CURDATE(), INTERVAL 500 DAY), 250, 243, 1.60),
 (3,  3,  1, 'IB-2403', DATE_ADD(CURDATE(), INTERVAL 300 DAY), 150, 150, 2.70),
 (4,  4,  1, 'AS-2404', DATE_ADD(CURDATE(), INTERVAL 600 DAY), 100,  96, 4.10),
 (5,  9,  1, 'VC-2405', DATE_ADD(CURDATE(), INTERVAL 200 DAY), 180, 180, 3.30),
 (6,  1,  1, 'AN-2399', DATE_ADD(CURDATE(), INTERVAL 15  DAY),  80,  80, 1.85),
 (7,  5,  2, 'AM-2410', DATE_ADD(CURDATE(), INTERVAL 450 DAY), 120, 118, 5.20),
 (8,  6,  2, 'AZ-2411', DATE_ADD(CURDATE(), INTERVAL 380 DAY),  90,  90, 7.80),
 (9,  7,  2, 'NU-2412', DATE_ADD(CURDATE(), INTERVAL 250 DAY),  60,  55, 6.10),
 (10, 8,  2, 'AC-2413', DATE_ADD(CURDATE(), INTERVAL 320 DAY),  70,  70, 7.40),
 (11, 7,  2, 'NU-2309', DATE_SUB(CURDATE(), INTERVAL 10  DAY),  40,  40, 6.00),
 (12, 10, 3, 'VD-2420', DATE_ADD(CURDATE(), INTERVAL 540 DAY), 100, 100, 9.10),
 (13, 11, 3, 'LO-2421', DATE_ADD(CURDATE(), INTERVAL 420 DAY), 120, 120, 2.90),
 (14, 12, 3, 'OM-2422', DATE_ADD(CURDATE(), INTERVAL 460 DAY),  80,  80, 4.60),
 (15, 13, 3, 'LB-2423', DATE_ADD(CURDATE(), INTERVAL 180 DAY),  50,   4, 10.50),
 (16, 14, 3, 'DP-2424', DATE_ADD(CURDATE(), INTERVAL 700 DAY),  60,  60, 5.40);

-- Одитната следа за доставките се извежда от самите партиди,
-- за да няма разминаване между движения и наличности.
INSERT IGNORE INTO stock_movements (id, medicine_id, batch_id, delta, reason, ref_id, user_id, created_at)
SELECT b.id, b.medicine_id, b.id, b.quantity_received, 'DELIVERY', b.delivery_id, 1, d.delivery_date
FROM batches b
JOIN deliveries d ON d.id = b.delivery_id;

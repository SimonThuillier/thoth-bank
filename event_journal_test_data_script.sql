-- this script insert some test data in bank event journal.
-- it is advised you drop all date in event journal, bank kafka topic and projections
-- using the dropperApplication, then recreate bank journal launching BankCliApplication before running this script.
truncate bank_journal;

insert into bank_journal (
    id, entity_id, sequence_num, event_type,
    version, transaction_id, event, total_message_in_transaction,
    num_message_in_transaction, emission_date, published
)
values
    ('00000000-0000-0000-0000-000000000001', 'Georges_Sand', 1, 'AccountOpened',
     1, 1, '{"type": "AccountOpened", "accountId": "Georges_Sand"}', 1,
     1, '2022-01-04 12:33', false),
    ('00000000-0000-0000-0000-000000000002', 'Georges_Sand', 2, 'MoneyDeposited',
     1, 2, '{"type": "MoneyDeposited", "amount": "780.63", "accountId": "Georges_Sand"}', 1,
     1, '2022-01-04 12:33', false),
    ('00000000-0000-0000-0000-000000000003', 'Georges_Sand', 3, 'MoneyWithdrawn',
     1, 3, '{"type": "MoneyWithdrawn", "amount": "40.00", "accountId": "Georges_Sand"}', 1,
     1, '2022-01-06 14:58', false),
    ('00000000-0000-0000-0000-000000000004', 'Victor_Hugo', 4, 'AccountOpened',
     1, 4, '{"type": "AccountOpened", "accountId": "Victor_Hugo"}', 1,
     1, '2022-01-07 08:36', false),
    ('00000000-0000-0000-0000-000000000005', 'Victor_Hugo', 5, 'MoneyDeposited',
     1, 5, '{"type": "MoneyDeposited", "amount": "12350", "accountId": "Victor_Hugo"}', 1,
     1, '2022-01-07 08:37', false),
    ('00000000-0000-0000-0000-000000000006', 'Victor_Hugo', 6, 'MoneyWithdrawn',
     1, 6, '{"type": "MoneyWithdrawn", "amount": "630", "accountId": "Victor_Hugo"}', 1,
     1, '2022-01-10 08:37', false),
    ('00000000-0000-0000-0000-000000000007', 'Georges_Sand', 7, 'MoneyWithdrawn',
     1, 7, '{"type": "MoneyWithdrawn", "amount": "200", "accountId": "Georges_Sand"}', 1,
     1, '2022-01-15 15:47', false),
    ('00000000-0000-0000-0000-000000000008', 'Gustave_Flaubert', 8, 'AccountOpened',
     1, 8, '{"type": "AccountOpened", "accountId": "Gustave_Flaubert"}', 1,
     1, '2022-01-22 14:40', false),
    ('00000000-0000-0000-0000-000000000009', 'Gustave_Flaubert', 9, 'MoneyDeposited',
     1, 9, '{"type": "MoneyDeposited", "amount": "7800", "accountId": "Gustave_Flaubert"}', 1,
     1, '2022-01-22 14:41', false),
    ('00000000-0000-0000-0000-000000000010', 'Victor_Hugo', 10, 'MoneyWithdrawn',
     1, 10, '{"type": "MoneyWithdrawn", "amount": "2000", "accountId": "Victor_Hugo"}', 1,
     1, '2022-01-27 10:06', false),
    ('00000000-0000-0000-0000-000000000011', 'Gustave_Flaubert', 11, 'MoneyWithdrawn',
     1, 11, '{"type": "MoneyWithdrawn", "amount": "900", "accountId": "Gustave_Flaubert"}', 1,
     1, '2022-02-02 17:06', false),
    ('00000000-0000-0000-0000-000000000012', 'Georges_Sand', 12, 'MoneyDeposited',
     1, 12, '{"type": "MoneyDeposited", "amount": "2500.47", "accountId": "Georges_Sand"}', 1,
     1, '2022-02-03 19:54', false),
    ('00000000-0000-0000-0000-000000000013', 'Georges_Sand', 13, 'MoneyWithdrawn',
     1, 13, '{"type": "MoneyWithdrawn", "amount": "480", "accountId": "Withdrawn"}', 1,
     1, '2022-02-03 22:14', false),
    ('00000000-0000-0000-0000-000000000014', 'Gustave_Flaubert', 14, 'MoneyDeposited',
     1, 14, '{"type": "MoneyDeposited", "amount": "4051.34", "accountId": "Gustave_Flaubert"}', 1,
     1, '2022-02-06 13:26', false),
    ('00000000-0000-0000-0000-000000000015', 'Victor_Hugo', 15, 'MoneyWithdrawn',
     1, 15, '{"type": "MoneyWithdrawn", "amount": "3600", "accountId": "Victor_Hugo"}', 1,
     1, '2022-02-10 08:37', false),
    ('00000000-0000-0000-0000-000000000016', 'Honore_Balzac', 16, 'AccountOpened',
     1, 16, '{"type": "AccountOpened", "accountId": "Honore_Balzac"}', 1,
     1, '2022-02-18 07:40', false),
    ('00000000-0000-0000-0000-000000000017', 'Honore_Balzac', 17, 'MoneyDeposited',
     1, 17, '{"type": "MoneyDeposited", "amount": "25000", "accountId": "Honore_Balzac"}', 1,
     1, '2022-02-18 15::35', false),
    ('00000000-0000-0000-0000-000000000018', 'Gustave_Flaubert', 18, 'MoneyWithdrawn',
     1, 18, '{"type": "MoneyWithdrawn", "amount": "1453", "accountId": "Gustave_Flaubert"}', 1,
     1, '2022-02-25 17:26', false),
    ('00000000-0000-0000-0000-000000000019', 'Victor_Hugo', 19, 'MoneyDeposited',
     1, 19, '{"type": "MoneyDeposited", "amount": "15000", "accountId": "Victor_Hugo"}', 1,
     1, '2022-02-27 06:11', false),
    ('00000000-0000-0000-0000-000000000020', 'Georges_Sand', 20, 'MoneyDeposited',
     1, 20, '{"type": "MoneyDeposited", "amount": "2530", "accountId": "Georges_Sand"}', 1,
     1, '2022-03-02 11:37', false),
    ('00000000-0000-0000-0000-000000000021', 'Gustave_Flaubert', 21, 'MoneyDeposited',
     1, 21, '{"type": "MoneyDeposited", "amount": "3400", "accountId": "Gustave_Flaubert"}', 1,
     1, '2022-03-08 09:07', false),
    ('00000000-0000-0000-0000-000000000022', 'Guy_Maupassant', 22, 'AccountOpened',
     1, 22, '{"type": "AccountOpened", "accountId": "Guy_Maupassant"}', 1,
     1, '2022-03-12 17:40', false),
    ('00000000-0000-0000-0000-000000000023', 'Guy_Maupassant', 23, 'MoneyDeposited',
     1, 23, '{"type": "MoneyDeposited", "amount": "5000", "accountId": "Guy_Maupassant"}', 1,
     1, '2022-03-12 17:41', false),
    ('00000000-0000-0000-0000-000000000024', 'Honore_Balzac', 24, 'MoneyWithdrawn',
     1, 24, '{"type": "MoneyWithdrawn", "amount": "10000", "accountId": "Honore_Balzac"}', 1,
     1, '2022-03-20 11::47', false),
    ('00000000-0000-0000-0000-000000000025', 'Georges_Sand', 25, 'MoneyDeposited',
     1, 25, '{"type": "MoneyDeposited", "amount": "2321", "accountId": "Georges_Sand"}', 1,
     1, '2022-04-04 09:27', false),
    ('00000000-0000-0000-0000-000000000026', 'Gustave_Flaubert', 26, 'MoneyDeposited',
     1, 26, '{"type": "MoneyDeposited", "amount": "7400.45", "accountId": "Gustave_Flaubert"}', 1,
     1, '2022-04-07 18:12', false),
    ('00000000-0000-0000-0000-000000000027', 'Victor_Hugo', 27, 'MoneyWithdrawn',
     1, 27, '{"type": "MoneyWithdrawn", "amount": "1200", "accountId": "Victor_Hugo"}', 1,
     1, '2022-04-13 15:12', false),
    ('00000000-0000-0000-0000-000000000028', 'Emile_Zola', 28, 'AccountOpened',
     1, 28, '{"type": "AccountOpened", "accountId": "Emile_Zola"}', 1,
     1, '2022-04-21 08:30', false),
    ('00000000-0000-0000-0000-000000000029', 'Emile_Zola', 29, 'MoneyDeposited',
     1, 29, '{"type": "MoneyDeposited", "amount": "5432", "accountId": "Emile_Zola"}', 1,
     1, '2022-04-21 08:31', false),
    ('00000000-0000-0000-0000-000000000030', 'Guy_Maupassant', 30, 'MoneyWithdrawn',
     1, 30, '{"type": "MoneyWithdrawn", "amount": "1500", "accountId": "Guy_Maupassant"}', 1,
     1, '2022-04-27 14:23', false),
    ('00000000-0000-0000-0000-000000000031', 'Emile_Zola', 31, 'MoneyWithdrawn',
     1, 31, '{"type": "MoneyWithdrawn", "amount": "559.99", "accountId": "Emile_Zola"}', 1,
     1, '2022-04-29 16:51', false),
    ('00000000-0000-0000-0000-000000000032', 'Georges_Sand', 32, 'MoneyDeposited',
     1, 32, '{"type": "MoneyDeposited", "amount": "2507.3", "accountId": "Georges_Sand"}', 1,
     1, '2022-05-03 11:17', false),
    ('00000000-0000-0000-0000-000000000033', 'Gustave_Flaubert', 33, 'MoneyDeposited',
     1, 33, '{"type": "MoneyDeposited", "amount": "789.45", "accountId": "Gustave_Flaubert"}', 1,
     1, '2022-05-06 09:39', false),
    ('00000000-0000-0000-0000-000000000034', 'Honore_Balzac', 34, 'MoneyWithdrawn',
     1, 34, '{"type": "MoneyWithdrawn", "amount": "12000", "accountId": "Honore_Balzac"}', 1,
     1, '2022-05-16 12:14', false),
    ('00000000-0000-0000-0000-000000000035', 'Charles_Baudelaire', 35, 'AccountOpened',
     1, 35, '{"type": "AccountOpened", "accountId": "Charles_Baudelaire"}', 1,
     1, '2022-05-25 18:30', false),
    ('00000000-0000-0000-0000-000000000036', 'Charles_Baudelaire', 36, 'MoneyDeposited',
     1, 36, '{"type": "MoneyDeposited", "amount": "48.79", "accountId": "Charles_Baudelaire"}', 1,
     1, '2022-05-25 18:31', false),
    ('00000000-0000-0000-0000-000000000037', 'Georges_Sand', 37, 'MoneyDeposited',
     1, 37, '{"type": "MoneyDeposited", "amount": "2452.17", "accountId": "Georges_Sand"}', 1,
     1, '2022-06-01 11:58', false),
    ('00000000-0000-0000-0000-000000000038', 'Gustave_Flaubert', 38, 'MoneyDeposited',
     1, 38, '{"type": "MoneyDeposited", "amount": "1245.32", "accountId": "Gustave_Flaubert"}', 1,
     1, '2022-06-10 07:39', false),
    ('00000000-0000-0000-0000-000000000039', 'Arthur_Rimbaud', 39, 'AccountOpened',
     1, 39, '{"type": "AccountOpened", "accountId": "Arthur_Rimbaud"}', 1,
     1, '2022-06-23 16:30', false),
    ('00000000-0000-0000-0000-000000000040', 'Arthur_Rimbaud', 40, 'MoneyDeposited',
     1, 40, '{"type": "MoneyDeposited", "amount": "980", "accountId": "Arthur_Rimbaud"}', 1,
     1, '2022-06-23 16:31', false);

# Начало работы

Перед тем как начать работу, **сделайте приватный форк**
данного репозитория ([инструкция](https://docs.gitlab.com/ee/user/project/repository/forking_workflow.html#create-a-fork)).

## Быстрая навигация

1. [Описание задания](#условие)
2. [Правила оценивания](#оценивание)
3. [Спецификация API](#задание)
4. [Тестирование и локальный запуск](#тестирование)

# Условие

- Ваша задача: **разработать API**, предоставляющий методы работы со счетами, переводами и
курсами валют, **по заданной спецификации**.
- Все данные требуется хранить в **PostgreSQL**. Схему данных требуется разработать самостоятельно
и автоматически создавать из кода приложения при запуске.
- Счета могут быть в разных валютах. Всего есть 4 валюты: **USD, EUR, GBP, RUB**.
- Приложение получает актуальные данные о курсах валют через брокер сообщений **Kafka**:
  - В топик приходят сообщения в формате `{ USDRUB: 70, EURUSD: 1.05, ...}`.
    - Запись вида `EURUSD: 1.05` означает, что 1 евро стоит 1.05 доллара
  - Всего валютных пар 6: EURUSD, GBPUSD, USDRUB, GBPEUR, GBPRUB, EURRUB
  - Все валютные пары всегда присутствуют в сообщении.
  - Сервис должен успевать вычитать новые сообщения за 500ms.
  - Перед каждым тестом гарантируется наличие хотя бы одного сообщения в топике.
  - В топике присутствует всего одна партиция.
- Все суммы нужно передавать и хранить с точностью **до 2 знаков после запятой**. Способ округления - half even.
- Корректная работа при перезапуске (graceful shutdown) не требуется и не оценивается.
- Приложение должно слушать порт 8080.
- Группа тестов считается успешной, если все тесты в этой группе успешны.

# Оценивание

Победителями определяются участники, решившие **максимальное кол-во тестовых групп**.

При получении равного результата среди топ-10 участников одного трека, победитель выявляется на основании:
1. Результатов нагрузочного тестирования
2. Времени решения задания
3. Качества исходного кода (при необходимости)

_Анализ качества кодовой базы и запуск нагрузочного тестирования осуществляется
эспертами после завершения основной части контеста._

# Задание

Требуется разработать следующие REST-эндпоинты.

## POST /api/v1/accounts

Создание счета

### Входные параметры

| Параметр    | Тип данных    | Обязательный | Описание                          |
|-------------|---------------|--------------|-----------------------------------|
| firstName   | string        | +            | Имя клиента                       |
| lastName    | string        | +            | Фамилия клиента                   |
| country     | string        | +            | Страна гражданства клиента        |
| birthDay    | string($date) | +            | Дата рождения, формат: YYYY-MM-DD |
| currency    | string        | +            | Валюта счета                      |

`POST /api/v1/accounts`
```json
{
    "firstName": "Петр",
    "lastName": "Иванов",
    "country": "Россия",
    "birthDay": "1923-12-30",
    "currency": "RUB"
}
```

### Выходные параметры

| Параметр      | Тип данных | Обязательный | Описание        |
|---------------|------------|--------------|-----------------|
| accountNumber | integer    | +            | Номер счета     |

`HTTP 200 OK`
```json
{
    "accountNumber": 2000002507
}
```

### Логика метода

1. Проверить наличие обязательных параметров в запросе и формат значений.
2. Проверить, что birthDay < текущей даты. Если нет - вернуть ошибку.
3. Проверить возраст клиента, сравнив год между birthDay и текущей датой. Если полученный возраст 
ВНЕ диапазона от 14 до 120, то вернуть ошибку.
4. Если currency из запроса != RUB, USD, GBP и EUR, то вернуть ошибку.
5. Сохранить в базу данных.
6. При успехе, вернуть в ответе HTTP 200 OK и созданный accountNumber.
  * В случае невалидного запроса вернуть HTTP 400
  * В случае ошибок во время работы метода, вернуть HTTP 500.

## GET /api/v1/accounts/{accountNumber}

Получение баланса счета

### Входные параметры

| Параметр        | Тип данных | Обязательный | Описание        |
|-----------------|------------|--------------|-----------------|
| accountNumber   | integer    | +            | Номер счета     |

`GET /api/v1/account/2000002507`

### Выходные параметры

| Параметр    | Тип данных      | Обязательный | Описание |
|-------------|-----------------|--------------|----------|
| amount      | number($double) | +            | Баланс   |
| currency    | string          | +            | Валюта   |

`HTTP 200 OK`
```json
{
    "amount": 10000.95,
    "currency": "RUB"
}
```

### Логика работы

1. Проверить наличие обязательных параметров в запросе.
2. Найти в базе данных счет по переданному accountNumber.
   1. Если счет не найден, то вернуть ошибку.
   2. Если найден - вернуть в ответе в параметрах amount и currency значения из базы данных.
3. В случае успеха вернуть ответ HTTP 200 OK.
  * В случае невалидного запроса вернуть HTTP 400.
  * Если счета не существует, то вернуть HTTP 400.
  * В случае ошибок во время работы метода, вернуть HTTP 500.

## POST /api/v1/accounts/{accountNumber}/top-up

Пополнение счета

### Входные параметры
| Параметр    | Тип данных        | Обязательный | Описание                                      |
|-------------|-------------------|--------------|-----------------------------------------------|
| amount      | number($double)   | +            | Сумма пополнения                              |
| topUpDate   | string($dateTime) | +            | Дата пополнения. Формат: YYYY-MM-DDThh:mm:ssZ |

`POST /api/v1/accounts/{accountNumber}/top-up`
```json
{
    "amount": 12.05,
    "topUpDate": "2023-12-30T20:00:00Z"
}
```

### Выходные параметры

Отсутствуют.

`HTTP 200 OK`
```json
{}
```

### Логика работы

1. Проверить наличие обязательных параметров в запросе и формат значений.
2. Найти в таблице account запись с accountNumber из запроса. Если не найдена - вернуть ошибку.
3. Проверить, что amount в запросе > 0. Если нет, вернуть ошибку.
4. Сохранить информацию о пополнении в базу данных.
5. Обновить поле account.amount = account.amount + amount из запроса и вернуть в ответе 200 OK.
  * В случае невалидного запроса, вернуть HTTP 400.
  * Если счета не существует, то вернуть HTTP 400.
  * В случае ошибок во время работы метода, вернуть HTTP 500.

## POST /api/v1/transfers

Выполнение денежного перевода (с возможной конвертацией)

### Входные параметры

| Параметр               | Тип данных        | Обязательный  | Описание                                            |
|------------------------|-------------------|---------------|-----------------------------------------------------|
| receiverAccount        | integer           | +             | Номер счета получателя                              |
| senderAccount          | integer           | +             | Номер счета отправителя                             |
| amountInSenderCurrency | number($double)   | +             | Сумма перевода в валюте счета отправителя           |
| transferDate           | string($dateTime) | +             | Дата и время перевода. Формат: YYYY-MM-DDThh:mm:ssZ |

`POST /api/v1/transfers`
```json
{
    "receiverAccount": 2000002507,
    "senderAccount": 7346006129,
    "amountInSenderCurrency": 10000.95,
    "transferDate": "2023-12-30T20:00:00Z"
}
```

### Выходные параметры

Отсутствуют.

`HTTP 200 OK`
```json
{}
```

### Логика работы

1. Проверить данные в запросе:
   1. Проверить наличие обязательных параметров в запросе и формат значений.
   2. Проверить, что amountInSenderCurrency в запросе > 0. Если нет, вернуть ошибку.
2. Найти в таблице account записи по senderAccount и receiverAccount. Если выборка пустая, вернуть ошибку.
3. Проверить, что баланс отправителя >= amountInSenderCurrency. Если нет, вернуть ошибку.
4. Проверить валюту счета отправителя и счета получателя.
   1. Если валюты равны, то нужно обновить балансы на amountInSenderCurrency.
   2. Если валюты НЕ равны, то надо конвертировать
      курс валюты отправителя к валюте получателя по топику Кафки. Использовать надо текущий актуальный курс,
      вне зависимости от времени перевода.
   3. Обновить балансы:
     * Вычесть отправленную сумму со счета отправителя.
     * Добавить к балансу счета получателя сумму транзакции с учетом текущего курса.
5. Сохранить информацию о переводе в базе данных.
6. В случае успеха вернуть ответ HTTP 200 OK.
  * В случае невалидного запроса вернуть HTTP 400.
  * Если какого-либо счета не существует, то вернуть HTTP 400.
  * В случае ошибок во время работы метода, вернуть HTTP 500.

## GET /api/v1/account-turnover/{accountNumber}

Получение оборота по счету (в валюте счета)

### Входные параметры

| Параметр      | Тип данных    | Обязательный | Описание                                             |
|---------------|---------------|--------------|------------------------------------------------------|
| accountNumber | integer       | +            | Номер счета                                          |
| startDate     | string($date) | -            | Дата начала периода, формат: yyyy-mm-ddThh:mm:ssZ    |
| endDate       | string($date) | -            | Дата окончания периода, формат: yyyy-mm-ddThh:mm:ssZ |


`GET /api/v1/account-turnover/2000002507?startDate=2023-01-01T00:00:00Z&endDate=2023-03-01T23:59:59Z`

### Выходные параметры

| Параметр | Тип данных      | Обязательный | Описание         |
|----------|-----------------|--------------|------------------|
| amount   | number($double) | +            | Сумма пополнения |
| currency | string          | +            | Валюта счета     |

`HTTP 200 OK`
```json
{
    "amount": 100.95,
    "currency": "RUB"
}
```

### Логика работы

1. Проверить в запросе:
   1. Наличие обязательных параметров и формат переданных в них значений.
   2. Если переданы параметры startDate и endDate, то проверить что startDate < endDate. Если нет, вернуть ошибку.
2. Найти в базе данных счет по переданному accountNumber.
   1. Если счет не найден, то вернуть ошибку.
   2. Если найден, то продолжить.
3. Сделать выборку переводов и пополнений по accountNumber из запроса и переданному временному интервалу 
(при наличии). Посчитать сумму по дебету и кредиту.
4. В случае успеха вернуть ответ HTTP 200 OK.
  * В случае невалидного запроса, вернуть HTTP 400.
  * Если счета не существует, то вернуть HTTP 400.
  * В случае ошибок во время работы метода, вернуть HTTP 500.

# Тестирование

Ваше решение будет протестировано автоматически средствами GitLab CI
при каждом коммите (пуше) в удаленный репозиторий.

Для этого вам потребуется:
1. Подготовить ваше приложение к принятию трафика **на порту 8080**.
2. Описать `Dockerfile` для сборки вашего проекта в Docker-образ
(можно использовать и модифицировать шаблонный).
3. При необходимости, доработать `docker-compose.yml`
(например, если требуется поменять сборочный контекст).
4. Отправить готовый к проверке код на репозиторий GitLab и
удостовериться, что пайплайн отрабатывает на нем корректно.

Пайплайн тестирования описан в файле [.gitlab-ci.yml](.gitlab-ci.yml).
Результат тестирования будет отправлен на удаленный сервер, если джоба завершилась успешно (окрасилась в зеленый цвет).

**Важно**: джобы тестирования являются **прерываемыми** (interruptible).
Это означает, что вам потребуется дождаться завершения выполнения предыдующего тестирования прежде чем запустить новый пайплайн.

Нагрузочный сценарий находится в этом же пайплайне (stage perf).
Его следует запускать в ручном режиме тогда, когда ваше решение готово быть проверено под нагрузкой.
Желательно это делать ближе к концу соревнования.


## Переменные окружения

Ваше приложение должно работать со следующими переменными окружения:

* **DB_HOST** - хост БД
* **DB_PORT** - порт БД
* **DB_NAME** - название БД
* **DB_USER** - пользователь БД
* **DB_PASSWORD** - пароль для подключения к БД
* **KAFKA_HOST** - хост брокера Kafka
* **KAFKA_PORT** - порт брокера Kafka
* **CURRENCY_KAFKA_TOPIC_NAME** - название топика валют в Kafka

Данные переменные окружения передаются через [.env](/.env) файл.

## Локальный запуск

В вашем репозитории приложен [docker-compose.local.yml](/docker-compose.local.yml)
для упрощения локального запуска инфраструктурных зависимостей (Kafka и PostgreSQL).

Чтобы запустить их локально, достаточно выполнить в терминале команду:

```bash
 $ docker-compose -f docker-compose.local.yml up -d  # запустит приложения в фоне
```

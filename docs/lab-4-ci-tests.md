# Лабораторная работа 4. Автоматические тесты и CI

**Версия:** 1.0  
**Дата:** 2026-11-24  
**Проект:** OrbitaMarket

---

## Задача 1. Автоматические тесты

### 1.1. Что покрыто тестами

В проекте есть unit-тесты на каждый сервис, нагрузочные сценарии (k6) и проверки безопасности (секреты / SBOM).

Для отчётности по Lab 4 зафиксированы как минимум **два** теста вокруг операции создания заказа (`OrderService.createOrder`), связанной с **FR-1** и контрактом `POST /api/v1/orders`:

| Тест | Тип | Что проверяет | Ожидаемый результат |
|------|-----|---------------|---------------------|
| `createOrder_Success` | Успешный случай | Создание заказа TASKING, сохранение в репозиторий и запись в Outbox | `OrderResponse` с `orderId`, статус `PAYMENT_PENDING`, productType `TASKING`; Outbox-событие `ORDER_PAYMENT_REQUESTED` |
| `createOrder_PriceNull_ThrowsException` | Ошибка / граница | `price == null` | `IllegalArgumentException` с сообщением `INVALID_PRICE`; репозитории не вызываются |
| `createOrder_PriceZeroOrLess_ThrowsException` | Граничное значение | `price <= 0` | `IllegalArgumentException` с сообщением `INVALID_PRICE` |

Дополнительно в том же классе есть тесты идемпотентности оплаты, обработки failure и чтения заказов (см. `OrderServiceTest`).

**Файл:** `order-service/.../OrderServiceTest.java`

Тесты используют Mockito (заглушки репозиториев и `ObjectMapper`), внешние сервисы и Kafka не требуются — проверка выполняется локально.

### 1.2. Локальный запуск

**Требования:** JDK 21, Gradle (в CI используется 8.7).

```bash
# Order Service (unit + integration через gradle build)
cd order-service
./gradlew test
# или полная сборка с тестами:
./gradlew build

# Payments Service
cd ../payments-service
./gradlew test

# API Gateway
cd ../api-gateway
./gradlew test
```

Нагрузочные сценарии (k6) и отчёты Allure описаны в корневом README / каталоге `k6/`.

### 1.3. Связь с контрактом и сценариями

| Артефакт | Связь |
|----------|--------|
| FR-1 | Пользователь может создать заказ |
| Контракт | `docs/interface-contract.md` — `POST /api/v1/orders` |
| Успех | валидная цена и productType → заказ + Outbox |
| Ошибка | `price == null` или `price <= 0` → `INVALID_PRICE` |

---

## Задача 2. GitHub Actions

### 2.1. Workflow

**Файл:** `.github/workflows/ci.yml` (или аналог с именем `Java CI with Gradle`)

**Триггеры:**
- `push` в ветку `main`
- `pull_request` в ветку `main` (открытие PR и новые коммиты в PR)

**Шаги:**
1. Checkout репозитория
2. Установка JDK 21 (Temurin)
3. Setup Gradle 8.7
4. `gradle build` в `api-gateway`
5. `gradle build` в `payments-service`
6. `gradle build` в `order-service`

`gradle build` включает компиляцию и запуск тестов. Это одновременно:
- запуск автоматических тестов;
- дополнительная проверка — **сборка** всех трёх сервисов.

### 2.2. Фрагмент конфигурации

```yaml
name: Java CI with Gradle

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.7'
      - name: Build and Test API Gateway
        run: gradle build
        working-directory: ./api-gateway
      - name: Build and Test Payments Service
        run: gradle build
        working-directory: ./payments-service
      - name: Build and Test Order Service
        run: gradle build
        working-directory: ./order-service
```

---

## Задача 3. Проверка workflow и слияние

### 3.1. Демонстрация падения проверки

1. В ветке `lab-4` временно изменён ожидаемый результат в одном тесте  
   (например, в `createOrder_PriceNull_ThrowsException` ожидаемое сообщение заменено на неверное).
2. Открыт pull request `lab-4` → `main`.
3. GitHub Actions завершился **ошибкой** на этом тесте (красный check, в логе — AssertionError / несовпадение expected vs actual).

**Ссылка на прогон с ошибкой:**  
`<вставить URL run из вкладки Actions>`

### 3.2. Восстановление и успешный прогон

1. Отдельным коммитом восстановлено корректное ожидаемое значение (`INVALID_PRICE`).
2. Workflow перезапустился на том же PR и завершился **успешно** (все три `gradle build` зелёные).

**Ссылка на успешный прогон:**  
`<вставить URL run из вкладки Actions>`

### 3.3. Слияние и тег

- После ревью и зелёного CI ветка `lab-4` слита в `main`.
- На итоговый коммит поставлен тег:

```bash
git tag lab-4
git push origin lab-4
```

---

## Итог

| Задача | Результат |
|--------|-----------|
| 1. Тесты | Unit-тесты OrderService (успех + ошибка/граница), запуск через `./gradlew test` / `build` |
| 2. CI | Workflow на `pull_request` и `push` в `main`: JDK 21 + Gradle build трёх сервисов |
| 3. Проверка | PR с падающим, затем успешным прогоном; merge в `main`; тег `lab-4` |


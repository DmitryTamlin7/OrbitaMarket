# Контракт интерфейса

**Версия:** 1.0  
**Дата:** 2026-11-24

---

## 1. Выбранная операция

| Поле | Значение |
|------|----------|
| **Операция** | `POST /api/v1/orders` |
| **Назначение** | Создание заказа на продукт ДЗЗ (ARCHIVE, TASKING или MONITORING) |
| **Сценарий** | US-1 (создание заказа) |
| **Требование** | FR-1 |
| **Реализация** | `order-service` → контроллер заказов (через `api-gateway`) |
| **Статус** | Уже реализовано |

Операция вызывается клиентом через единую точку входа (API Gateway).  
Gateway проверяет наличие заголовка `X-User-Id` и перенаправляет запрос в `order-service`.

---

## 2. Контракт

### 2.1. Кто вызывает и как

- **Вызывающая сторона:** клиентское приложение (пользователь) или тестовый клиент.
- **Способ вызова:** HTTP `POST` на `http://localhost:8080/api/v1/orders` (через API Gateway).
- **Обязательный заголовок:** `X-User-Id` — строковый идентификатор пользователя.
- **Content-Type:** `application/json`.

### 2.2. Входные данные

| Поле | Тип | Обязательное | Описание | Допустимые значения |
|------|-----|--------------|----------|---------------------|
| `productType` | string (enum) | да | Тип продукта | `ARCHIVE`, `TASKING`, `MONITORING` |
| `amount` | number (decimal) | да | Стоимость заказа | > 0 |
| `payload` | object | да | Полиморфные параметры продукта | зависит от `productType` |

**Структура `payload` (примеры ключевых полей):**

- **ARCHIVE / TASKING / MONITORING:**
  - `aoi` — полигон области интереса (GeoJSON / массив координат)
  - `timeWindow` / `startDate` / `endDate` — временное окно
  - для MONITORING дополнительно: `cadence` (периодичность)

**Условия вызова:**
- Заголовок `X-User-Id` обязателен. При отсутствии Gateway возвращает `400 Bad Request`.
- `productType` должен быть одним из трёх допустимых значений.
- `amount` > 0.
- `payload` должен соответствовать выбранному типу продукта (валидация на стороне сервиса).

### 2.3. Результат успешного вызова

| Аспект | Значение |
|--------|----------|
| **HTTP-статус** | `201 Created` |
| **Тело ответа** | JSON с созданным заказом |
| **Ключевые поля ответа** | `id` (UUID заказа), `userId`, `productType`, `status` = `"PENDING_PAYMENT"`, `amount`, `createdAt` |
| **Побочный эффект** | Заказ сохраняется в БД со статусом `PENDING_PAYMENT`. В Kafka публикуется событие `OrderPaymentRequestedEvent`. |

### 2.4. Характерная ошибка

| Аспект | Значение |
|--------|----------|
| **Ситуация** | Отсутствует обязательный заголовок `X-User-Id` |
| **HTTP-статус** | `400 Bad Request` |
| **Сообщение / тело** | Текст вида `Header X-User-id NOT FOUND` (или аналогичное сообщение об отсутствии заголовка) |
| **Поведение системы** | Запрос не доходит до `order-service`. Заказ не создаётся, событие в Kafka не публикуется. |

> Другие возможные ошибки (для справки): невалидный `productType`, некорректный `payload`, `amount ≤ 0` — также приводят к `400`.

---

## 3. Примеры

### 3.1. Успешный вызов

**Запрос:**

```http
POST /api/v1/orders HTTP/1.1
Host: localhost:8080
X-User-Id: Dima-01
Content-Type: application/json

{
  "productType": "ARCHIVE",
  "amount": 450.00,
  "payload": {
    "aoi": {
      "type": "Polygon",
      "coordinates": [[[30.5, 50.4], [30.6, 50.4], [30.6, 50.5], [30.5, 50.5], [30.5, 50.4]]]
    },
    "startDate": "2025-06-01",
    "endDate": "2025-06-15"
  }
}
```

**Ожидаемый результат:**

```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "d290f1ee-6c54-4b01-90e6-d701748f0851",
  "userId": "Dima-01",
  "productType": "ARCHIVE",
  "status": "PENDING_PAYMENT",
  "amount": 450.00,
  "createdAt": "2026-11-24T12:00:00Z"
}
```

- В БД появляется запись заказа со статусом `PENDING_PAYMENT`.
- В топик `payment-requests-topic` уходит событие с `orderId` и `amount`.

### 3.2. Ошибка (отсутствует X-User-Id)

**Запрос:**

```http
POST /api/v1/orders HTTP/1.1
Host: localhost:8080
Content-Type: application/json

{
  "productType": "ARCHIVE",
  "amount": 450.00,
  "payload": {
    "aoi": { "type": "Polygon", "coordinates": [[[30.5, 50.4], [30.6, 50.4], [30.6, 50.5], [30.5, 50.5], [30.5, 50.4]]] },
    "startDate": "2025-06-01",
    "endDate": "2025-06-15"
  }
}
```

**Ожидаемый результат:**

```http
HTTP/1.1 400 Bad Request
Content-Type: text/plain (или application/json)

Header X-User-id NOT FOUND
```

- Заказ **не** создаётся.
- Событие в Kafka **не** публикуется.
- Состояние системы не меняется.

---

## 4. Связь с остальными артефактами

| Артефакт | Связь |
|----------|--------|
| FR-1 | Пользователь может создать заказ на продукт ARCHIVE / TASKING / MONITORING |
| US-1 | Сценарий создания заказа |
| Sequence success | `docs/diagrams/04-sequence-success.mmd` |
| Sequence error | `docs/diagrams/05-sequence-error.mmd` (другая ошибка — нехватка средств; данная ошибка — на входе) |

-- МЕТРИКА: Активность клиентов. Считает успешные заказы и общую сумму трат для каждого user_id.
select user_id,
       COUNT(*) as paid_orders,
       SUM(price) as paid_for_orders
from orders
where order_status = 'PAID'
group by user_id
order by paid_orders DESC

-- МЕТРИКА: Популярность продуктов. Считает продажи категорий и сортирует от менее популярных к более.
select product_type, count(product_type) as product_type_count
from orders
where order_status = 'PAID'
group by product_type
order by product_type_count ASC

-- МЕТРИКА: Крупные клиенты. Фильтрует базу и оставляет только пользователей с 3 и более оплаченными заказами.
select user_id, count(*) as nums_order, sum(price) as total_spend
from orders
where order_status = 'PAID'
group by user_id
having count(*) >= 3
order by total_spend asc

-- МЕТРИКА: Финансовый календарь. Группирует успешные транзакции по дням и считает ежедневный доход.
select DATE_TRUNC('day', created_at) as order_date,
       count(*) as total_order_per_day,
       sum(price)  as total_dengi_per_day
from orders
where order_status = 'PAID'
group by DATE_TRUNC('day', created_at)
order by order_date asc


-- МЕТРИКА: Финансовый календарь. Группирует успешные транзакции по дням и считает ежедневный доход.
SELECT
    product_type,
    COUNT(*) AS total_orders,
    ROUND(
            COUNT(CASE WHEN order_status = 'CANCELLED' THEN 1 END) * 100.0 / COUNT(*),
            2
    ) AS cancel_rate_percent
FROM orders
GROUP BY product_type
ORDER BY cancel_rate_percent DESC;



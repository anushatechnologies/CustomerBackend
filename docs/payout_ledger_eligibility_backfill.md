# Payout Ledger Eligibility Diagnostic and Backfill Guide

## Context
Previously, when an order was marked `DELIVERED`, the application immediately changed the payout ledger status to `ELIGIBLE` while simultaneously setting `settlement_date` to `now + 7 days`. Under the corrected T+7 design, a delivered order's ledger must remain in `PENDING` status with `settlement_date = delivery_time + 7 days`, and only be promoted to `ELIGIBLE` once that `settlement_date` has actually passed.

---

## 1. Diagnostic SQL Query (Do NOT run automatically)
Use this query to find all ledger rows currently marked `ELIGIBLE` whose release date (`settlement_date`) is still in the future:

```sql
SELECT l.id AS ledger_id,
       l.order_id,
       o.order_number,
       o.order_status,
       l.store_id,
       l.seller_id,
       l.gross_amount,
       l.commission_amount,
       l.tcs_amount,
       l.net_payout_amount,
       l.status AS ledger_status,
       l.settlement_date,
       l.created_at AS ledger_created_at
FROM seller_payout_ledgers l
JOIN orders o ON l.order_id = o.order_id
WHERE l.status = 'ELIGIBLE'
  AND l.settlement_date > NOW();
```

---

## 2. Reviewed Backfill SQL Statement (Manual Execution Discretion)
If operations/finance decides to revert prematurely eligible ledgers back to `PENDING` so the scheduled sweep job promotes them only when their `settlement_date` matures, run the following statement:

```sql
-- Backfill: Revert prematurely ELIGIBLE ledger rows with future settlement dates back to PENDING
UPDATE seller_payout_ledgers
SET status = 'PENDING',
    updated_at = NOW()
WHERE status = 'ELIGIBLE'
  AND settlement_date > NOW();
```

---

## 3. Diagnostic SQL Query: Ledgers Requiring Manual Clawback Review
When an order was cancelled or refunded after its payout ledger was already `ELIGIBLE` or `SETTLED`, the status is intentionally preserved and tagged with `clawback_reason`. Run this query to audit rows requiring manual deduction from future payouts:

```sql
SELECT l.id AS ledger_id,
       l.order_id,
       o.order_number,
       o.order_status,
       l.store_id,
       l.seller_id,
       l.net_payout_amount,
       l.status AS ledger_status,
       l.settlement_date,
       l.clawback_reason,
       l.updated_at
FROM seller_payout_ledgers l
LEFT JOIN orders o ON l.order_id = o.order_id
WHERE l.status IN ('ELIGIBLE', 'SETTLED')
  AND l.clawback_reason IS NOT NULL;
```

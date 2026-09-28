# ShivHub After-Sales Management

## Migration

Run `shivhub-backend/shivhub-backend/src/main/resources/db/manual-migrations/V20260917__after_sales_management.sql` once on MySQL 8 before deployment. It only creates new after-sales tables and indexes; it never drops or changes an existing table or column.

## Customer APIs

`GET /api/customer/after-sales/eligible-purchases`

`POST /api/customer/after-sales/requests`

```json
{
  "requestType": "WARRANTY_CLAIM",
  "orderId": 42,
  "orderItemId": 91,
  "purchaseSerialId": 801,
  "issueCategory": "DISPLAY",
  "customerIssue": "Screen has vertical lines after normal use.",
  "pickupType": "SHOP_VISIT"
}
```

`POST /api/customer/after-sales/requests/{id}/attachments?type=IMAGE` (multipart `file`)

`POST /api/customer/after-sales/requests/{id}/estimate-decision`

```json
{ "approved": true, "remarks": "Please proceed." }
```

`POST /api/customer/after-sales/requests/{id}/cancel`

`GET /api/customer/after-sales/requests/{id}/documents/JOB_SHEET`

## Seller APIs

`GET /api/seller/after-sales/summary`

`GET /api/seller/after-sales/requests?status=INSPECTION_IN_PROGRESS`

`POST /api/seller/after-sales/requests/{id}/status`

```json
{ "status": "UNDER_REVIEW", "remarks": "Evidence accepted", "customerVisible": true }
```

`POST /api/seller/after-sales/requests/{id}/receive`

`PUT /api/seller/after-sales/requests/{id}/inspection`

`PUT /api/seller/after-sales/requests/{id}/estimate`

`POST /api/seller/after-sales/requests/{id}/dispatch`

`POST /api/seller/after-sales/requests/{id}/replacement`

```json
{ "replacementPurchaseSerialId": 902, "additionalPayment": 0, "inspectionResult": "Manufacturing defect confirmed" }
```

`POST /api/seller/after-sales/requests/{id}/refunds`

```json
{ "refundAmount": 1499.00, "refundMethod": "UPI", "paymentTransactionId": 51 }
```

`POST /api/seller/after-sales/requests/{id}/refunds/{refundId}/complete`

```json
{ "transactionReference": "UPI-REF-123456" }
```

`POST /api/seller/after-sales/requests/{id}/attachments?type=INSPECTION_IMAGE` (multipart `file`)

`GET /api/seller/after-sales/requests/{id}/documents/REPAIR_ESTIMATE`

## Admin APIs

`GET|POST /api/admin/after-sales/policies`

`PUT|DELETE /api/admin/after-sales/policies/{id}` (`DELETE` deactivates; it does not delete data.)

`GET /api/admin/after-sales/requests?status=&from=YYYY-MM-DD&to=YYYY-MM-DD`

`POST /api/admin/after-sales/requests/{id}/override`

```json
{ "status": "CLOSED", "reason": "Dispute resolved after document review", "customerVisible": true }
```

`GET /api/admin/after-sales/reports?from=YYYY-MM-DD&to=YYYY-MM-DD`

## Transitions

`REQUESTED → UNDER_REVIEW|ELIGIBILITY_CHECK|CANCELLED`

`UNDER_REVIEW|ELIGIBILITY_CHECK → APPROVED|REJECTED` (eligibility check may receive product)

`APPROVED → PRODUCT_RECEIVED|REFUND_INITIATED`

`PRODUCT_RECEIVED → INSPECTION_IN_PROGRESS`

`INSPECTION_IN_PROGRESS → WARRANTY_CONFIRMED|PAID_REPAIR_APPROVAL_REQUIRED|INSPECTION_PASSED|INSPECTION_FAILED|REJECTED`

Warranty/repair: `WARRANTY_CONFIRMED → SENT_TO_SERVICE_CENTER|REPAIR_IN_PROGRESS|READY_FOR_DELIVERY`; `PAID_REPAIR_APPROVAL_REQUIRED → ESTIMATE_APPROVED|ESTIMATE_REJECTED`; `ESTIMATE_APPROVED → REPAIR_IN_PROGRESS → READY_FOR_DELIVERY → DELIVERED → CLOSED`.

Return/refund: `INSPECTION_PASSED → RETURN_APPROVED|REPLACEMENT_APPROVED|REFUND_INITIATED`; `RETURN_APPROVED → REFUND_INITIATED|COMPLETED`; `REPLACEMENT_APPROVED → REPLACED → COMPLETED`; `REFUND_INITIATED → REFUNDED → COMPLETED`.

## Manual checklist

1. Apply migration, log in as a customer, and confirm only paid delivered orders / paid matching POS bills appear.
2. Try another customer’s order and an unrelated serial; both must return 403/400 and create no request.
3. Create a request with evidence; confirm request number, history, and attachment are stored.
4. As the selling seller, receive, inspect, save an estimate, then approve it as customer.
5. Verify invalid status changes return HTTP 400 with `message`.
6. For a return/replacement, confirm original serial becomes `RETURN_PENDING` at receiving and is not sellable.
7. Complete a replacement with an available seller-owned serial; verify old/new serial status and exactly one stock movement.
8. Initiate then complete a partial refund with a confirmed reference; verify credit note and one proportional loyalty adjustment.
9. As admin, create product/category/global policies and execute an override with a reason; inspect status and audit history.
10. Use customer/seller documents and confirm internal notes and supplier purchase data are absent.

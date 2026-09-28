# Campaign, loyalty and POS customer APIs

All routes require the existing JWT. Admin routes require `ADMIN`; seller routes require `SELLER`.

## Campaigns

`POST /api/admin/campaigns`

```json
{
  "title": "Diwali offer",
  "messageType": "FESTIVAL",
  "subject": "A Diwali offer for you",
  "messageContent": "Use the code below on your next purchase.",
  "couponCode": "DIWALI10",
  "targetAudience": "HIGH_VALUE_CUSTOMERS",
  "minimumPurchaseAmount": 10000,
  "channels": ["EMAIL"],
  "sendNow": true
}
```

Supported `targetAudience` values: `ALL_CUSTOMERS`, `SHIVHUB_ONLINE_CUSTOMERS`, `SELLER_CUSTOMERS`, `OFFLINE_REGISTERED_CUSTOMERS`, `WALK_IN_CUSTOMERS_WITH_EMAIL`, `CUSTOMER_BY_SELLER`, `CUSTOMER_BY_CITY`, `CUSTOMER_BY_CATEGORY_PURCHASE`, `ACTIVE_CUSTOMERS`, `INACTIVE_CUSTOMERS`, and `HIGH_VALUE_CUSTOMERS`.

`POST /api/admin/campaigns/{id}/send` returns a queue acknowledgement, for example:

```json
{"campaignId":42,"status":"QUEUED","targeted":120,"queued":113,"sent":0,"failed":0,"skipped":4,"optedOut":3}
```

Other campaign routes are `POST /{id}/retry-failed`, `POST /{id}/test`, `POST /{id}/schedule`, `POST /audience-preview`, and `GET /{id}/report?status=FAILED&query=customer@example.com`.

## POS customer lookup

`GET /api/seller/customers/lookup?mobile=9876543210`

```json
{"found":true,"customerId":12,"customerProfileId":22,"name":"Asha","mobile":"9876543210","email":"asha@example.com","city":"Pune","availablePoints":56}
```

`POST /api/seller/customers/register-or-update`

```json
{"name":"Asha","mobile":"9876543210","email":"asha@example.com","address":"Baner","city":"Pune"}
```

The request never accepts credentials or role fields. A seller can read a shared online customer or their own mapped POS customer only.

## Loyalty

`GET /api/customer/loyalty` returns wallet totals and transactions. `GET /api/seller/loyalty/lookup?mobile=9876543210` returns an eligible POS customer's balance.

`PUT /api/admin/loyalty/settings`

```json
{"minimumPurchaseAmount":500,"pointsPerPurchaseUnit":1,"purchaseUnitInRupees":100,"pointValueInRupees":1,"maximumPointsPerSale":200,"active":true}
```

`POST /api/admin/loyalty/adjustments`

```json
{"customerProfileId":22,"points":25,"reason":"Goodwill adjustment approved by support"}
```

Include `loyaltyPointsToRedeem` in the existing offline-bill request. The server validates the balance and writes the redemption only after the bill is paid.

## Birthday setting

`PUT /api/admin/birthday-settings`

```json
{"emailSubject":"Happy Birthday from ShivHub","messageContent":"Have a wonderful day!","couponCode":"BDAY10","active":true}
```

`POST /api/admin/birthday-settings/run-now` is an admin-only operational test. The regular task runs at 10:00 Asia/Kolkata and queues only one delivery per customer per yearly birthday campaign.

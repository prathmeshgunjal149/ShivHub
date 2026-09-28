# ShivHub backend design

The backend is organised by responsibility. Each feature has an entity (database
shape), repository (database queries), service (business rules) and controller
(HTTP API). This keeps a bug fix local to the correct feature instead of turning
one controller into the whole application.

```text
React customer / seller / admin panels
                 |
            JWT security
                 |
Controllers -> Services -> Repositories -> MySQL
                 |
     Email, invoice PDF, file upload, stock audit
```

## Existing business modules

| Flow in the diagram | Backend module |
| --- | --- |
| Login, roles, seller approval | `AuthService`, `AdminService`, `security` |
| Product approval and catalogue | `ProductService`, `AdminProductService` |
| Cart, checkout and online order tracking | `CartService`, `OrderService` |
| Shop/POS billing and invoice | `OfflineBillingService`, `OfflineBillInvoiceService` |
| Purchase stock and distributors | `PurchaseService`, `StockEntryService`, `DistributorService` |
| Coupons and campaigns | `CouponService`, `MarketingService` |
| Reviews, wishlist, customer profile | `ProductReviewService`, `WishlistService`, `CustomerProfileService` |
| Admin reports | `ReportService` |

## Seller dashboard API

`GET /api/seller/dashboard`

Authentication is required. The seller is resolved from the JWT token, never
from a client-provided seller ID. The response includes:

- total, active, pending and low-stock product counts;
- today's online orders/sales and POS bills/sales;
- recent products and products at or below the low-stock threshold (5).

Online sales are calculated from the `seller_id` snapshot stored in
`order_items`. Cancelled orders are excluded. Offline sales come from the
seller's `offline_bills`. Therefore the figures remain tied to the correct shop
even if names later change.

## Important stock rule

When creating a POS bill, a `SELLER` can now bill only a product owned by that
seller. An `ADMIN` retains cross-shop operational access. This protects one
shop's inventory from another shop's product ID being submitted accidentally or
maliciously.

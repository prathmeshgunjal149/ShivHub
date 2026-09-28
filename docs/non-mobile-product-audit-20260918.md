# Non-mobile attributes, variants and mobile ETA: implementation audit

## Inspection and compatibility

Existing category/subcategory tables and `category_specification_templates` are reused; product specifications remain JSON on the existing Product. `product_variants` is the existing non-mobile extension. Cart/order/POS already carry variant IDs and attribute snapshots. VariantInventoryService locks the parent product and selected variant for reservations and sales. Mobile options navigate between existing sibling mobile Product records; they do not create generic variant stock or expose IMEIs.

The dedicated `pages/seller/AddProduct.jsx` Mobile form, its creation endpoint, mobile entity fields, purchase GST rules and normal IMEI sale implementation were **not changed in this audit**. This is source-preservation confirmation, not a claim that every live workflow has been exercised.

Opening serialized stock cannot safely be stored in product specifications: PurchaseItemSerial requires a real PurchaseItem. The new, separate non-mobile creation service therefore uses the existing PurchaseService with an actual linked distributor, invoice and purchase rate. Product creation plus opening purchase are one serializable database transaction. Units remain AVAILABLE, not SOLD. Approval remains PENDING/inactive. Tracked products with zero opening stock can receive stock later through Purchases.

## Fixes

- Common optional fields now depend on category/subcategory; shirts/grocery/beauty no longer inherit model, electronic warranty and device compatibility inputs. Configured color/warranty fields are not duplicated.
- Category/subcategory changes clear stale details; obsolete API responses are ignored. Submission requires successful attribute loading.
- Enabling IMEI/serial tracking reveals per-unit identifier entry and genuine opening purchase fields. Exactly one identifier row per stock unit is required. IMEIs are 15 digits, duplicates across IMEI/serial fields and existing registered codes are rejected. Distributor ownership/brand/invoice validation is reused from PurchaseService.
- Switching to serial tracking clears incompatible generic variants.
- Variant editor input keys stay stable while typing SKU; management payloads omit UI/reservation fields and retain optimistic-lock versions.
- Variant update rejects duplicate incoming SKUs/barcodes and recalculates the parent GST-inclusive price breakdown.
- Catalog add-to-cart opens product details for products needing variant selection. Cart quantities use selected variant availability, not parent stock.
- Option switching can change incompatible size/color combinations without getting stuck; unavailable options remain disabled.
- Order details format the saved variant JSON as readable selected attributes.
- Wishlist's old fake add-to-cart alert was replaced with the actual authenticated cart API; variant products require selection first.
- Delivery loading clears when address selection is cleared. Existing ETA widgets remain on product details/cart/checkout; original ETA snapshots remain on order items.
- Final order totals retain the server-calculated, seller-deduplicated mobile delivery charge. The cart now shows the same estimated charge after a delivery-address check; it shows `Check address` instead of incorrectly showing free delivery before an estimate exists.
- Separate inspected-return API restores only the original variant. Other dispositions never add sellable stock. Original sale quantity caps all dispositions; request and sale locks plus the ledger prevent duplicate restoration. Seller UI is connected to this API.
- Startup seeding no longer recreates Admin-deactivated subcategory fields.
- Customer marketplace filters now load only active, Admin-marked filterable fields for the selected non-mobile subcategory. Their values are calculated from approved product specifications and cached public variant axes; no seller pricing, IMEI/serial or other internal data is returned.

## New files in this audit

Backend paths below are relative to `shivhub-backend/shivhub-backend/src`:

- `main/java/com/shivhub/backend/dto/InitialProductPurchaseRequest.java`
- `main/java/com/shivhub/backend/service/NonMobileProductCreationService.java`
- `main/java/com/shivhub/backend/controller/NonMobileProductController.java`
- `main/java/com/shivhub/backend/service/VariantReturnService.java`
- `main/java/com/shivhub/backend/controller/VariantReturnController.java`
- `main/java/com/shivhub/backend/controller/CustomerCatalogueFilterController.java`
- `test/java/com/shivhub/backend/service/NonMobileProductCreationServiceTest.java`
- `test/java/com/shivhub/backend/service/DeliveryRuleServiceTest.java`
- `test/java/com/shivhub/backend/service/VariantInventoryServiceTest.java`
- `test/java/com/shivhub/backend/service/VariantReturnServiceTest.java`
- `test/java/com/shivhub/backend/service/OrderDeliveryChargeTest.java`: protects the order-total delivery-charge calculation.

Frontend paths relative to `shivhub-frontend/src`:

- `components/products/productCommonFields.js`: presentation defaults and safe serializers/formatters.
- `components/products/productCommonFields.test.js`: real presentation/serializer verification, no dummy runtime data.
- `components/products/DynamicAttributeFilters.jsx`: Admin-configured customer marketplace filters backed by the public catalogue API.
- `components/products/productAttributeFilters.js` and `productAttributeFilters.test.js`: safe product-specification matching used by those filters.
- `components/products/SerialStockEditor.jsx`: per-unit opening stock and real purchase details.
- `components/products/ReturnedVariantStock.jsx`: inspected disposition/stock API integration.

This document is also new.

## Modified files in this audit

Backend:

- `dto/CreateProductRequest.java`: optional non-mobile opening purchase payload; old fields/contracts retained.
- `service/ProductVariantService.java`: duplicate incoming code validation and GST breakup refresh.
- `service/CategorySpecificationTemplateService.java`: exposes active filterable metadata only for active, non-mobile customer catalogue subcategories.
- `service/ProductConfigurationService.java`: mirrors only configured public filter axes from variants into the parent product specification JSON.
- `repository/AfterSalesStockMovementRepository.java`: original-sale disposition quantity/duplicate checks.
- `repository/CategorySpecificationTemplateRepository.java` and `service/ProductExtensionSeeder.java`: respect deactivated Admin configuration on restart.
- `src/main/resources/db/manual-migrations/V20260924__non_mobile_product_variants_and_attributes.sql`: MySQL-compatible repeatable column guards and complete extension schema.
- `.env.example`: optional server-side maps key, no secret values.

Frontend:

- `pages/seller/AddOtherProduct.jsx`: context-relevant common fields, guarded attribute fetch, serial purchase editor and separate creation API.
- `pages/seller/ProductVariants.jsx`: safe request serializer.
- `components/products/VariantEditor.jsx`: stable editing keys and helper extraction.
- `components/products/VariantSelector.jsx`, `MobileOptionSelector.jsx`: valid available combination switching; no mobile inventory mutation.
- `components/products/MobileDeliveryEstimate.jsx`: reset cancelled loading and component-only export.
- `pages/customer/dashboard/CustomerProductGrid.jsx`, `pages/customer/CustomerProducts.jsx`: variant selection before cart add.
- `pages/customer/Cart.jsx`: exact variant available stock and formatter import.
- `pages/customer/Checkout.jsx`, `pages/seller/OfflineBilling.jsx`: **formatter import only**, existing billing/scanner/checkout logic unchanged.
- `pages/customer/OrderDetails.jsx`: readable persisted variant attributes.
- `pages/customer/Wishlist.jsx`: genuine cart add and variant selection.
- `pages/customer/CustomerDashboard.jsx`, `pages/customer/dashboard/CustomerHeader.jsx`: applies the selected dynamic attributes to the existing real product list.
- `pages/seller/AfterSales.jsx`: separate inspected variant-stock UI insertion.

## Schema and rollout

Use `V20260924__non_mobile_product_variants_and_attributes.sql` after backing up the selected MySQL schema. The script is supplied, **not executed against the live database here**. It assumes the earlier category/after-sales/customer-address migrations already exist.

It adds template options/filterable/variant flags, Product variants_enabled, ProductVariant table/combination and nullable barcode uniqueness/indexes, cart variant/selection keys, order/POS variant snapshots, stock/return ledger variant references, order ETA snapshots, cached customer/shop coordinates and the delivery rule/configuration tables. No table or data column is dropped. Existing VariantCartSchemaUpgrade safely replaces the old two-column cart uniqueness with customer/product/selection uniqueness at startup while preserving rows.

Serialized opening stock reuses existing purchases, purchase_items, purchase_item_serials and stock_movements; it adds no parallel inventory tables. Attribute values reuse Product JSON rather than introducing a duplicate attribute-value table.

## APIs

New:

- `POST /api/seller/non-mobile-products` (SELLER), multipart parts `product`, `images`, `imageUrls`. Uses JWT seller identity; no sellerId accepted.
- `GET /api/customer/catalogue/subcategories/{subCategoryId}/filters` (authenticated): public active field labels/options for one active, non-mobile subcategory. The endpoint returns no product, seller, stock, IMEI or pricing data.
- `GET /api/seller/after-sales/requests/{id}/variant-disposition` (own SELLER request or ADMIN), original selected attributes, remaining return quantity and recorded disposition.
- `POST /api/seller/after-sales/requests/{id}/variant-disposition` (same ownership), validated inspected stock disposition. Variant ID is resolved from the original sale, never accepted from the client.

Reused:

- Categories: `GET /api/categories`, `GET /api/categories/{id}/subcategories`.
- Seller attributes: `GET /api/seller/product-specification-templates?categoryId=...&subCategoryId=...`.
- Admin attributes: `GET/POST /api/admin/product-specification-templates`; `PUT/DELETE /api/admin/product-specification-templates/{id}` (DELETE deactivates).
- Customer variants: `GET /api/products/{id}/variants` or `/api/customer/products/{id}/variants`, APPROVED/ACTIVE products only.
- Mobile configurations: `GET /api/products/{id}/mobile-options`, visible same-seller sibling listings only.
- Seller variants: `GET/PUT /api/seller/products/{id}/variants`, JWT ownership and optimistic stock version.
- Mobile ETA: `GET /api/customer/products/{id}/delivery-estimate?addressId=...`, only the authenticated customer's saved address.
- Admin ETA: `GET/POST /api/admin/delivery-rules`, `PUT/DELETE /{id}`, `PATCH /{id}/status`, `POST /preview`.
- Cart uses existing variantId-aware APIs; line increase/decrease/remove distinguish exact variants.

Opening purchase example (inside the existing product JSON):

```json
{
  "serialTrackingRequired": true,
  "stock": 1,
  "initialPurchase": {
    "sellerDistributorId": 14,
    "invoiceNumber": "ACTUAL-DISTRIBUTOR-INVOICE",
    "purchaseDate": "2026-09-18T10:00:00",
    "unitPrice": 35000.00,
    "gstRate": 18.00,
    "serials": [{"imei1": null, "imei2": null, "serialNumber": "ACTUAL-UNIT-SERIAL"}]
  }
}
```

IDs/codes above illustrate the API shape, not seeded database records. Use actual linked distributor/category IDs and actual procurement data.

Creation response shape: `{"productId":123,"name":"Product","approvalStatus":"PENDING","active":false,"stock":1,"serialUnitCount":1,"purchaseId":456}`.

Disposition payload: `{"disposition":"AVAILABLE","quantity":1,"remarks":"Inspection passed; packaging verified"}`. Backend rejects uninspected intake, wrong seller, excess original-sale quantity and duplicate disposition. DEFECTIVE/SERVICE_CENTER/SCRAP/RETURNED_TO_DISTRIBUTOR retain nonsellable stock instead.

## Admin/seller/customer usage

Admin: open `/admin/subcategory-fields`, select a non-mobile category/subcategory; configure labels, types/options, required/filterable/variant flags and order. Edit existing rows to reactivate. Open `/admin/delivery-rules` to manage nonoverlapping half-open ranges and preview a real entered distance. Adjacent ranges use `[minimum, maximum)`; exactly 5 km belongs to 5–15, exactly 30 km belongs to the outside rule.

Default templates are attached only to existing unconfigured active subcategories: clothing/shirts (size/color/fabric/fit/sleeve/pattern/collar/gender/occasion/wash-care/size-chart), footwear, laptops, LED/smart TVs, headphones/earbuds/speakers, grocery, beauty, home and automobile. They are not runtime dummy product data. Mobile subcategories are excluded. Existing or deactivated subcategory configuration is preserved.

Seller: Add Other Product -> actual category/subcategory -> relevant common/dynamic details -> optional per-combination variant rows -> existing images -> PENDING approval. For serialized stock, enable tracking, set quantity and enter the actual procurement/unit identifiers. Generic variants and IMEI-tracked inventory intentionally remain separate. In After-Sales, record receiving/inspection, then open Returned variant stock and record the final disposition.

Customer: select every required variant option before add/buy. Price, image and stock follow the selected combination. Separate sizes remain separate cart lines. Mobile options open the corresponding existing mobile listing, preserving exact IMEI allocation. Choose a saved delivery address in the existing ETA card.

Set `SHIVHUB_MAPS_API_KEY` server-side for Google Geocoding and Routes. Verified coordinates are cached; road-distance failure uses explicitly labelled Haversine straight-line estimation. Missing coordinates/key/address never produces a fabricated delivery promise.

## Verification and limits

- Backend: full Maven suite, 35 tests, zero failures/errors (H2 context plus unit tests).
- Frontend helpers: Node test runner, 7 tests passed.
- Production frontend build passed; existing >500 KB bundle warning remains.
- Targeted lint on changed product/customer components: zero errors, existing effect-dependency warnings remain.
- Full-project lint is **not clean**: 41 errors and 21 warnings remain in existing unrelated autocomplete/admin/seller code. This audit does not silently disable lint rules.
- Browser setup failed due the environment's trusted browser-runtime path restriction; no visual/live browser regression is claimed.
- Live MySQL migration, live map calls, real paid checkout/POS and scanner regression have not been exercised here. No production test purchases/refunds/emails were generated.
- Generic non-mobile after-sales replacement issuance is not implemented by the existing serial-based replacement flow; this audit adds exact-variant return disposition/restoration, not a parallel replacement workflow.

Thus the screenshot issues are fixed, but the entire original multi-module specification must not be labelled 100% production-verified.

## Manual regression checklist

1. Admin configure Shirts size/color/fabric and mark a field filterable; seller sees those fields, not electronic compatibility/model. On the customer catalogue, choose the product type and filter by the field; only matching approved products must remain. Deactivate all fields and restart: they must not be recreated.
2. Add a shirt with M/L variants, different prices/stocks and existing five-image validation. Approve it; selecting M updates price/image/stock.
3. Add M and L separately to cart. Buy M once: only M changes. Repeat final-unit checkout concurrently: second sale must fail or respect reservations; verify MySQL locks.
4. Cancel an unpaid reservation: exact variant reservation releases; unrelated sizes stay unchanged.
5. POS select a variant, complete a paid bill and inspect selected attributes/GST on its invoice.
6. Enable serialized laptop/TV stock, enter a real linked distributor invoice/base purchase rate and one serial per unit. Invalid/duplicate identifiers or another seller's distributor must fail without a product/purchase DB orphan.
7. After approval, scan the recorded serial in existing POS. Complete the bill; stock/serial change only once and invoice includes identifier. Scan sold/other-seller identifiers: reject.
8. Receive a returned variant: no sellable stock change. After inspection, AVAILABLE restores only original variant; DEFECTIVE does not. Retry disposition: reject and preserve balance.
9. Admin preview distances 8/25/30 km, edit rules and try overlapping active ranges. Customer change saved address: recompute ETA. Missing map key/address: clear confirmation/missing-address message, not a false promise.
10. Existing Add Mobile + five-image upload -> PENDING -> approval -> actual distributor purchase/IMEI -> POS/online fulfilment, plus manual billing/customer lookup/payments/GST/cart/authentication still need live role-by-role regression before deployment.

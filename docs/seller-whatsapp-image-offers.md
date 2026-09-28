# Seller WhatsApp image offers

The admin WhatsApp screen saves template metadata and the exact live AiSensy API campaign name. Only the campaign name is sent to AiSensy. It does not create or approve templates in AiSensy.

Server configuration required:

```properties
SHIVHUB_WHATSAPP_PROVIDER=aisensy
AISENSY_ENABLED=true
AISENSY_API_KEY=<server secret>
SHIVHUB_PUBLIC_API_URL=https://your-public-api-host
```

The repository defaults to `meta` with AiSensy disabled; the deployed process environment determines the active provider. Check Admin > WhatsApp operations diagnostics for actual readiness. Credentials must stay on the server.

In AiSensy, approve an IMAGE-header marketing template with three body parameters: shop name, offer title, offer message. Create an API campaign for that template and make it Live. In Admin > WhatsApp operations, select **Offer image notification**, enter the exact approved template and live campaign names, and enable the mapping. Uploaded banners must be publicly reachable over HTTPS without authentication.

In Seller > Customers & offers, choose **WhatsApp image offer**, upload an image, enter the subject/message, and choose individual customers or **All my customers**. All-customer selection is resolved on the server from seller mappings and that seller's billed customers. It does not require customer email, does not include unrelated sellers' customers, deduplicates mobile numbers, and respects marketing/WhatsApp consent. Provider acceptance, skipped recipients, and failed requests are reported separately. Acceptance is not proof of handset delivery. Identical successful offers are deduplicated by the existing delivery log; changing the banner produces a new offer identity.

The email channel remains available. No live customer messages were sent during implementation; tests mock the messaging service.

AiSensy payload reference: https://faq.aisensy.com/api-reference-docs/ (`media.url`, `media.filename`, `campaignName`, `templateParams`).

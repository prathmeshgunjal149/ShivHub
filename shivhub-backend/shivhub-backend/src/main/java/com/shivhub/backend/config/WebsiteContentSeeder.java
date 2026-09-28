package com.shivhub.backend.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.shivhub.backend.entity.PolicyPage;
import com.shivhub.backend.entity.PolicyVersion;
import com.shivhub.backend.entity.SiteFaq;
import com.shivhub.backend.entity.SiteSetting;
import com.shivhub.backend.repository.PolicyPageRepository;
import com.shivhub.backend.repository.PolicyVersionRepository;
import com.shivhub.backend.repository.SiteFaqRepository;
import com.shivhub.backend.repository.SiteSettingRepository;

@Configuration
public class WebsiteContentSeeder {

    @Bean
    ApplicationRunner seedWebsiteContent(
            SiteSettingRepository settings,
            PolicyPageRepository policies,
            PolicyVersionRepository versions,
            SiteFaqRepository faqs) {
        return args -> {
            if (settings.count() == 0) {
                SiteSetting setting = new SiteSetting();
                setting.setBrandName("ShivHub");
                setting.setLegalBusinessName("ShivHub Demo Commerce");
                setting.setSupportEmail("support@shivhub.example");
                setting.setSupportPhone("DEMO-NON-DIALABLE");
                setting.setBusinessAddress("Demo business address — replace before launch.");
                setting.setSupportHours("Demo: Monday–Saturday, 10 AM–6 PM.");
                setting.setGrievanceContactName("Demo Support Officer");
                setting.setGrievanceEmail("support@shivhub.example");
                setting.setEnabledPaymentMethods("");
                setting.setDemoData(true);
                setting.setRequiresReview(true);
                settings.save(setting);
            }

            for (Map.Entry<String, String[]> entry : starterPolicies().entrySet()) {
                if (!policies.existsBySlug(entry.getKey())) {
                    String[] values = entry.getValue();
                    PolicyPage page = new PolicyPage();
                    page.setSlug(entry.getKey());
                    page.setTitle(values[0]);
                    page.setDescription(values[1]);
                    page.setPublishedContent(values[2]);
                    page.setDraftContent(null);
                    page.setEffectiveDate(LocalDate.now());
                    page.setPublished(true);
                    page.setDemoData(true);
                    page.setRequiresReview(true);
                    page.setLastPublishedAt(LocalDateTime.now());
                    PolicyPage saved = policies.save(page);

                    PolicyVersion version = new PolicyVersion();
                    version.setPolicyPage(saved);
                    version.setTitle(saved.getTitle());
                    version.setContent(saved.getPublishedContent());
                    version.setAction("SEEDED_DEMO");
                    versions.save(version);
                }
            }

            if (faqs.count() == 0) {
                faq(faqs, "Orders", "How do I track an order?", "Use Customer Help > Track Order after login. Demo wording requires review before launch.", 1);
                faq(faqs, "Returns", "Can I return a mobile phone?", "Return eligibility depends on the published policy, product condition, invoice, box/accessories and IMEI/model verification where applicable.", 2);
                faq(faqs, "Warranty", "Who provides warranty?", "Manufacturer warranty is normally handled by the brand/service centre. ShivHub/seller assistance depends on the published policy.", 3);
            }
        };
    }

    private void faq(SiteFaqRepository faqs, String category, String question, String answer, int order) {
        SiteFaq faq = new SiteFaq();
        faq.setCategory(category);
        faq.setQuestion(question);
        faq.setAnswer(answer);
        faq.setDisplayOrder(order);
        faq.setDemoData(true);
        faqs.save(faq);
    }

    private Map<String, String[]> starterPolicies() {
        return Map.ofEntries(
                policy("about", "About ShivHub", "Demo overview of the marketplace.",
                        demo("ShivHub is an electronics marketplace demo profile. Platform information, legal identifiers and operating details require admin review before launch.\n\nShivHub connects customers with approved sellers. Sellers remain responsible for product accuracy, stock, invoices and fulfilment according to platform rules.")),
                policy("contact", "Contact Us", "Demo contact and support information.",
                        demo("Support email: support@shivhub.example\nPhone: DEMO-NON-DIALABLE\nAddress: Demo business address — replace before launch.\n\nUse the enquiry form for order, payment, warranty, return or seller-support questions.")),
                policy("terms", "Terms & Conditions", "Demo customer platform terms.",
                        demo("These sample terms explain pricing, orders, payments, invoices and marketplace use. Prices and offers must come from live product data. Order acceptance, cancellation and delivery rules must be reviewed before production.\n\nShivHub does not promise payment approval, refund speed or warranty coverage unless a final published policy states it.")),
                policy("privacy-policy", "Privacy Policy", "Demo privacy notice.",
                        demo("ShivHub may collect account details, order details, support messages, device/session data and cookies required for ecommerce operation.\n\nData may be used for account access, order processing, fraud prevention, support, legal/accounting records and optional marketing only where separately enabled. This demo text requires legal review.")),
                policy("cancellation-refund", "Cancellation & Refund Policy", "Demo cancellation and refund policy.",
                        demo("Cancellation eligibility depends on order stage. A request before packing/dispatch may be handled differently from a shipped order.\n\nRefund initiation by ShivHub/seller and actual bank/payment-provider credit time are different steps. Final timelines are editable placeholders and must be confirmed before launch.")),
                policy("shipping-policy", "Shipping & Delivery Policy", "Demo shipping policy.",
                        demo("Shipping charges, dispatch timelines, courier partner, tracking availability and delivery estimates must be configured by admin/seller rules. Do not treat demo timelines as confirmed promises.\n\nCustomers should inspect package condition at delivery and report issues quickly with photos/order reference.")),
                policy("return-replacement", "Return & Replacement Policy", "Demo return and replacement policy.",
                        demo("Wrong, damaged or defective item requests require inspection. For mobile phones, model, serial number and IMEI may be verified against invoice/order records.\n\nReturn/replacement windows, pickup process and rejection conditions are placeholders until admin publishes final terms.")),
                policy("warranty-policy", "Warranty Policy", "Demo warranty assistance policy.",
                        demo("Manufacturer warranty is separate from seller/platform support. Brand service centre rules, invoice requirement, physical/IMEI verification and damage exclusions may apply.\n\nShivHub may assist with seller coordination where the final published policy allows it.")),
                policy("help", "Help / FAQ", "Demo help centre.",
                        demo("Find order tracking, cancellation, refund, shipping, return, replacement and warranty guidance here. If your issue is not listed, submit a support enquiry and keep your ticket reference.")),
                policy("grievance", "Grievance Redressal", "Demo complaint escalation process.",
                        demo("Grievance contact: Demo Support Officer.\nEmail: support@shivhub.example\n\nComplaint categories include order issues, payment/refund delay, damaged item, warranty support, seller dispute and privacy requests. Escalation timelines require admin/legal review.")),
                policy("seller-terms", "Seller Terms", "Demo seller responsibilities.",
                        demo("Sellers are responsible for accurate product listing, stock availability, pricing, invoice details, dispatch, IMEI/serial mapping where applicable, returns inspection and customer support cooperation.\n\nSettlement schedules, commissions, penalties and dispute procedures are placeholders until production policy is published.")),
                policy("payment-security", "Payment Security", "Demo payment-security information.",
                        demo("Payment method availability and processing are determined by the active checkout provider. ShivHub does not collect card numbers, CVV values or UPI PINs in its own forms.\n\nAdmin must publish final payment-provider, refund and security information before launch.")),
                policy("cookie-policy", "Cookie Policy", "Demo cookie-policy information.",
                        demo("Cookies and similar browser storage may be used for sign-in sessions, customer preferences and platform security.\n\nAdmin/legal review must publish the final consent and retention information.")),
                policy("sitemap", "Sitemap", "Demo public page directory.",
                        demo("Use this page to publish the official ShivHub public-page directory. Admin can maintain its content through Website Settings.")),
                policy("careers", "Careers", "Demo careers information.",
                        demo("Open roles, application method and hiring terms are managed by ShivHub administration. Do not publish contact or hiring promises until approved.")),
                policy("why-choose-us", "Why Choose ShivHub", "Demo platform overview.",
                        demo("ShivHub connects customers with approved sellers and platform tools for product discovery, orders, support and marketplace activity. Admin can publish the final brand story here.")),
                policy("seller-support", "Seller Support", "Demo seller-support information.",
                        demo("Seller onboarding, catalog support, billing, stock and order operations are available according to approved platform rules. Admin can publish support routes and hours here.")),
                policy("partner-program", "Partner Program", "Demo partner-program information.",
                        demo("Partnership eligibility, onboarding and commercial terms are set by ShivHub administration. Publish final participation information before accepting applications.")),
                policy("api-integration", "API & Integration", "Demo integration information.",
                        demo("Official integration availability, credentials, documentation and access terms are set by ShivHub administration. Do not publish private credentials on this page."))
        );
    }

    private Map.Entry<String, String[]> policy(String slug, String title, String description, String content) {
        return Map.entry(slug, new String[]{title, description, content});
    }

    private String demo(String content) {
        return "DEMO / REQUIRES REVIEW\n\n" + content;
    }
}

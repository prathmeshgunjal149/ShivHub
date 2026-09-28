package com.shivhub.backend.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.shivhub.backend.entity.OfflineBill;
import com.shivhub.backend.entity.OfflineBillItem;
import com.shivhub.backend.entity.PurchaseItem;
import com.shivhub.backend.entity.PurchaseItemSerial;
import com.shivhub.backend.repository.PurchaseItemSerialRepository;
/*
 * =========================================================
 * OfflineBillInvoiceService
 * =========================================================
 *
 * Generates PDF invoice for Offline / POS bills.
 *
 * Flow:
 *
 * OfflineBill
 *      ↓
 * OfflineBillInvoiceService
 *      ↓
 * PDF byte[]
 *      ↓
 * EmailService
 *      ↓
 * Customer Email
 *
 * =========================================================
 */

@Service
public class OfflineBillInvoiceService {
    private String invoicePaymentMethod(OfflineBill bill) {
        if (bill.getPaymentMethod() == com.shivhub.backend.enums.PaymentMethod.FINANCE)
            return "Finance - " + safe(bill.getFinanceProviderName());
        return bill.getPaymentMethod() == null ? "-" : bill.getPaymentMethod().name();
    }

    private final PurchaseItemSerialRepository serialRepository;

    public OfflineBillInvoiceService(PurchaseItemSerialRepository serialRepository) {
        this.serialRepository = serialRepository;
    }


    /*
     * =========================================================
     * GENERATE INVOICE PDF
     * =========================================================
     */

    public byte[] generateInvoicePdf(
            OfflineBill bill) {


        /*
         * =====================================================
         * VALIDATE BILL
         * =====================================================
         */

        if (bill == null) {

            throw new RuntimeException(
                    "Offline bill cannot be null"
            );
        }


        /*
         * =====================================================
         * OUTPUT STREAM
         * =====================================================
         */

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();


        /*
         * =====================================================
         * CREATE DOCUMENT
         * =====================================================
         */

        Document document =
                new Document(
                        PageSize.A4,
                        36,
                        36,
                        36,
                        36
                );


        try {


            /*
             * =================================================
             * PDF WRITER
             * =================================================
             */

            PdfWriter writer = PdfWriter.getInstance(
                    document,
                    outputStream
            );


            /*
             * =================================================
             * OPEN DOCUMENT
             * =================================================
             */

            document.open();

            /* Subtle shop watermark behind invoice content; it is part of the seller's own brand. */
            String watermark = safe(bill.getSellerBusinessName()).equals("-")
                    ? safe(bill.getSellerName()) : safe(bill.getSellerBusinessName());
            PdfContentByte background = writer.getDirectContentUnder();
            background.saveState();
            background.setColorFill(new Color(228, 235, 247));
            background.beginText();
            background.setFontAndSize(
                    BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, false),
                    42
            );
            background.showTextAligned(Element.ALIGN_CENTER, watermark, 298, 420, 35);
            background.endText();
            background.restoreState();


            /*
             * =================================================
             * FONTS
             * =================================================
             */

            Font companyFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            20,
                            new Color(18, 62, 112)
                    );


            Font titleFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            16,
                            Color.BLACK
                    );


            Font headingFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            10,
                            Color.WHITE
                    );


            Font normalFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA,
                            9,
                            Color.BLACK
                    );


            Font boldFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            9,
                            Color.BLACK
                    );


            Font smallFont =
                    FontFactory.getFont(
                            FontFactory.HELVETICA,
                            8,
                            Color.BLACK
                    );

            /* A compact brand band gives each printable invoice a clear, premium hierarchy. */
            PdfPTable brandBand = new PdfPTable(2);
            brandBand.setWidthPercentage(100);
            brandBand.setWidths(new float[]{2.4f, 1f});
            PdfPCell brandCell = new PdfPCell(new Phrase("SHIVHUB  |  SELLER INVOICE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
            brandCell.setBackgroundColor(new Color(18, 62, 112));
            brandCell.setBorder(Rectangle.NO_BORDER);
            brandCell.setPadding(9);
            PdfPCell invoiceCell = new PdfPCell(new Phrase("TAX INVOICE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
            invoiceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            invoiceCell.setBackgroundColor(new Color(32, 97, 178));
            invoiceCell.setBorder(Rectangle.NO_BORDER);
            invoiceCell.setPadding(9);
            brandBand.addCell(brandCell);
            brandBand.addCell(invoiceCell);
            document.add(brandBand);
            document.add(new Paragraph(" "));


            /*
             * =================================================
             * COMPANY HEADER
             * =================================================
             */

            Paragraph companyName =
                    new Paragraph(
                            safe(bill.getSellerBusinessName()).equals("-")
                                    ? safe(bill.getSellerName())
                                    : bill.getSellerBusinessName(),
                            companyFont
                    );

            companyName.setAlignment(
                    Element.ALIGN_LEFT
            );

            document.add(
                    companyName
            );


            Paragraph companySubtitle =
                    new Paragraph(
                            sellerInvoiceSubtitle(bill),
                            normalFont
                    );

            companySubtitle.setAlignment(
                    Element.ALIGN_LEFT
            );

            document.add(
                    companySubtitle
            );


            document.add(
                    new Paragraph(" ")
            );


            /*
             * =================================================
             * INVOICE TITLE
             * =================================================
             */

            Paragraph invoiceTitle =
                    new Paragraph(
                            "Invoice " + safe(bill.getBillNumber()),
                            titleFont
                    );

            invoiceTitle.setAlignment(
                    Element.ALIGN_RIGHT
            );

            document.add(
                    invoiceTitle
            );


            document.add(
                    new Paragraph(" ")
            );


            /*
             * =================================================
             * BILL INFORMATION
             * =================================================
             */

            PdfPTable billInfoTable =
                    new PdfPTable(2);

            billInfoTable.setWidthPercentage(
                    100
            );

            billInfoTable.setWidths(
                    new float[]{
                            1,
                            1
                    }
            );


            /*
             * =================================================
             * LEFT SIDE
             * =================================================
             */

            PdfPCell billCell =
                    new PdfPCell();

            billCell.setBorder(
                    Rectangle.BOX
            );

            billCell.setBorderColor(new Color(205, 218, 232));
            billCell.setPadding(8);

            billCell.addElement(new Paragraph("INVOICE DETAILS", boldFont));


            billCell.addElement(
                    new Paragraph(
                            "Bill Number: "
                            + safe(
                                    bill.getBillNumber()
                            ),
                            boldFont
                    )
            );


            billCell.addElement(
                    new Paragraph(
                            "Date: "
                            + formatDate(
                                    bill.getCreatedAt()
                            ),
                            normalFont
                    )
            );


            billCell.addElement(
                    new Paragraph(
                            "Seller: "
                            + safe(
                                    bill.getSellerName()
                            ),
                            normalFont
                    )
            );

            billCell.addElement(new Paragraph("Sales Person: " + safe(bill.getSalesPersonName()), normalFont));
            billCell.addElement(new Paragraph("Payment Mode: " + invoicePaymentMethod(bill), normalFont));
            billCell.addElement(new Paragraph("Customer Type: " + (bill.getCustomerId() == null ? "Walk-in" : "Registered"), normalFont));


            billInfoTable.addCell(
                    billCell
            );


            /*
             * =================================================
             * RIGHT SIDE
             * =================================================
             */

            PdfPCell customerCell =
                    new PdfPCell();

            customerCell.setBorder(
                    Rectangle.BOX
            );

            customerCell.setBorderColor(new Color(205, 218, 232));
            customerCell.setPadding(8);

            if (bill.isInvoiceShowCustomerDetails()) {


            customerCell.addElement(
                    new Paragraph(
                            "CUSTOMER DETAILS",
                            boldFont
                    )
            );


            customerCell.addElement(
                    new Paragraph(
                            "Name: "
                            + safe(
                                    bill.getCustomerName()
                            ),
                            normalFont
                    )
            );


            customerCell.addElement(
                    new Paragraph(
                            "Mobile: "
                            + safe(
                                    bill.getCustomerMobile()
                            ),
                            normalFont
                    )
            );


            customerCell.addElement(
                    new Paragraph(
                            "Email: "
                            + safe(
                                    bill.getCustomerEmail()
                            ),
                            normalFont
                    )
            );

            if (bill.getCustomerAddress() != null && !bill.getCustomerAddress().isBlank()) {
                customerCell.addElement(new Paragraph("Address: " + bill.getCustomerAddress(), normalFont));
            }


            if (bill.getCustomerGstin() != null
                    && !bill.getCustomerGstin().isBlank()) {

                customerCell.addElement(
                        new Paragraph(
                                "GSTIN: "
                                + bill.getCustomerGstin(),
                                normalFont
                        )
                );
            }

            if (bill.getCustomerLegalName() != null && !bill.getCustomerLegalName().isBlank()) {
                customerCell.addElement(new Paragraph("Legal name: " + bill.getCustomerLegalName(), normalFont));
            }
            if (bill.getCustomerTradeName() != null && !bill.getCustomerTradeName().isBlank()) {
                customerCell.addElement(new Paragraph("Trade name: " + bill.getCustomerTradeName(), normalFont));
            }
            if (bill.getPlaceOfSupply() != null && !bill.getPlaceOfSupply().isBlank()) {
                customerCell.addElement(new Paragraph("Place of supply: " + bill.getPlaceOfSupply(), normalFont));
            }

            }


            billInfoTable.addCell(
                    customerCell
            );


            document.add(
                    billInfoTable
            );


            document.add(
                    new Paragraph(" ")
            );


            /*
             * =================================================
             * PRODUCT TABLE
             * =================================================
             */

            PdfPTable productTable =
                    new PdfPTable(8);

            productTable.setWidthPercentage(
                    100
            );


            productTable.setWidths(
                    new float[]{
                            .55f,
                            3.45f,
                            2.2f,
                            1f,
                            1.5f,
                            1.1f,
                            1.1f,
                            2f
                    }
            );


            /*
             * HEADER
             */

            addHeaderCell(productTable, "Sr.", headingFont);
            addHeaderCell(
                    productTable,
                    "Product",
                    headingFont
            );

            addHeaderCell(productTable, "IMEI / Serial", headingFont);

            addHeaderCell(
                    productTable,
                    "Qty",
                    headingFont
            );

            addHeaderCell(
                    productTable,
                    "Price",
                    headingFont
            );

            addHeaderCell(
                    productTable,
                    "Discount",
                    headingFont
            );

            addHeaderCell(
                    productTable,
                    "GST",
                    headingFont
            );

            addHeaderCell(
                    productTable,
                    "Total",
                    headingFont
            );


            /*
             * =================================================
             * BILL ITEMS
             * =================================================
             */

            if (bill.getItems() != null
                    && !bill.getItems().isEmpty()) {


                int serialNumber = 1;
                for (OfflineBillItem item : bill.getItems()) {

                    addBodyCell(productTable, String.valueOf(serialNumber++), normalFont, Element.ALIGN_CENTER);


                    addBodyCell(
                            productTable,
                            productDescription(item),
                            normalFont,
                            Element.ALIGN_LEFT
                    );

                    addBodyCell(productTable, itemSerials(item), normalFont, Element.ALIGN_LEFT);


                    addBodyCell(
                            productTable,
                            String.valueOf(
                                    item.getQuantity()
                            ),
                            normalFont,
                            Element.ALIGN_CENTER
                    );


                    addBodyCell(
                            productTable,
                            rupee(
                                    item.getUnitPrice()
                            ),
                            normalFont,
                            Element.ALIGN_RIGHT
                    );


                    addBodyCell(
                            productTable,
                            rupee(
                                    item.getDiscount()
                            ),
                            normalFont,
                            Element.ALIGN_RIGHT
                    );


                    addBodyCell(
                            productTable,
                            percentage(
                                    item.getGstRate()
                            ),
                            normalFont,
                            Element.ALIGN_RIGHT
                    );


                    addBodyCell(
                            productTable,
                            rupee(
                                    item.getTotalPrice()
                            ),
                            normalFont,
                            Element.ALIGN_RIGHT
                    );
                }


            } else {


                PdfPCell emptyCell =
                        new PdfPCell(
                                new Phrase(
                                        "No items",
                                        normalFont
                                )
                        );

                emptyCell.setColspan(
                        8
                );

                emptyCell.setHorizontalAlignment(
                        Element.ALIGN_CENTER
                );

                productTable.addCell(
                        emptyCell
                );
            }


            document.add(
                    productTable
            );


            document.add(
                    new Paragraph(" ")
            );


            /*
             * =================================================
             * TOTALS TABLE
             * =================================================
             */

            PdfPTable totalsTable =
                    new PdfPTable(2);

            totalsTable.setWidthPercentage(
                    45
            );

            totalsTable.setHorizontalAlignment(
                    Element.ALIGN_RIGHT
            );

            totalsTable.setWidths(
                    new float[]{
                            2f,
                            1.5f
                    }
            );


            /*
             * SUBTOTAL
             */

            addTotalRow(
                    totalsTable,
                    "Subtotal",
                    rupee(
                            bill.getSubtotal()
                    ),
                    normalFont
            );


            /*
             * DISCOUNT
             */

            addTotalRow(
                    totalsTable,
                    "Discount",
                    rupee(
                            bill.getDiscount()
                    ),
                    normalFont
            );

            if (bill.getLoyaltyDiscount() != null && bill.getLoyaltyDiscount().compareTo(BigDecimal.ZERO) > 0) {
                addTotalRow(totalsTable,
                        "Loyalty points discount (" + (bill.getLoyaltyPointsRedeemed() == null ? 0 : bill.getLoyaltyPointsRedeemed()) + " pts)",
                        rupee(bill.getLoyaltyDiscount()), normalFont);
            }


            /*
             * TAXABLE AMOUNT
             */

            addTotalRow(
                    totalsTable,
                    "Taxable Amount",
                    rupee(
                            bill.getTaxableAmount()
                    ),
                    normalFont
            );


            /*
             * CGST
             */

            addTotalRow(
                    totalsTable,
                    "CGST",
                    rupee(
                            bill.getCgst()
                    ),
                    normalFont
            );


            /*
             * SGST
             */

            addTotalRow(
                    totalsTable,
                    "SGST",
                    rupee(
                            bill.getSgst()
                    ),
                    normalFont
            );


            /*
             * IGST
             */

            if (bill.getIgst() != null
                    && bill.getIgst()
                            .compareTo(
                                    BigDecimal.ZERO
                            ) > 0) {

                addTotalRow(
                        totalsTable,
                        "IGST",
                        rupee(
                                bill.getIgst()
                        ),
                        normalFont
                );
            }

            /*
             * DELIVERY CHARGE
             */

            if (bill.getDeliveryCharge() != null
                    && bill.getDeliveryCharge()
                            .compareTo(
                                    BigDecimal.ZERO
                            ) > 0) {

                addTotalRow(
                        totalsTable,
                        "Delivery Charge",
                        rupee(
                                bill.getDeliveryCharge()
                        ),
                        normalFont
                );
            }


            /*
             * GRAND TOTAL
             */

            addGrandTotalRow(
                    totalsTable,
                    "GRAND TOTAL",
                    rupee(
                            bill.getGrandTotal()
                    ),
                    boldFont
            );


            document.add(
                    totalsTable
            );

            Paragraph amountInWords = new Paragraph("Amount in Words: " + amountInWords(bill.getGrandTotal()) + " Only.", normalFont);
            amountInWords.setSpacingBefore(6);
            document.add(amountInWords);


            document.add(
                    new Paragraph(" ")
            );


            /*
             * =================================================
             * PAYMENT INFORMATION
             * =================================================
             */

            PdfPTable paymentTable =
                    new PdfPTable(2);

            paymentTable.setWidthPercentage(
                    100
            );


            PdfPCell paymentHeading =
                    new PdfPCell(
                            new Phrase(
                                    "Payment Information",
                                    headingFont
                            )
                    );

            paymentHeading.setBackgroundColor(
                    Color.DARK_GRAY
            );

            paymentHeading.setPadding(
                    6
            );


            paymentTable.addCell(
                    paymentHeading
            );


            PdfPCell paymentDetails =
                    new PdfPCell();

            paymentDetails.setPadding(
                    6
            );


            String paymentMethod = invoicePaymentMethod(bill);


            paymentDetails.addElement(
                    new Paragraph(
                            "Payment Method: "
                            + paymentMethod,
                            normalFont
                    )
            );


            if (bill.getPaymentMethod() != com.shivhub.backend.enums.PaymentMethod.FINANCE) paymentDetails.addElement(
                    new Paragraph(
                            "Paid Amount: "
                            + rupee(
                                    bill.getPaymentAmount()
                            ),
                            normalFont
                    )
            );


            if (bill.getPaymentMethod() != com.shivhub.backend.enums.PaymentMethod.FINANCE && bill.getTransactionId() != null
                    && !bill.getTransactionId().isBlank()) {

                paymentDetails.addElement(
                        new Paragraph(
                                "Transaction ID: "
                                + bill.getTransactionId(),
                                normalFont
                        )
                );
            }


            paymentTable.addCell(
                    paymentDetails
            );


            document.add(
                    paymentTable
            );


            /*
             * =================================================
             * NOTES
             * =================================================
             */

            if (bill.isInvoiceShowNotes() && bill.getNotes() != null
                    && !bill.getNotes().isBlank()) {


                document.add(
                        new Paragraph(" ")
                );


                document.add(
                        new Paragraph(
                                "Notes",
                                boldFont
                        )
                );


                document.add(
                        new Paragraph(
                                bill.getNotes(),
                                normalFont
                        )
                );
            }

            if (bill.getSellerInvoiceTerms() != null && !bill.getSellerInvoiceTerms().isBlank()) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Terms & Conditions", boldFont));
                document.add(new Paragraph(bill.getSellerInvoiceTerms(), normalFont));
            }

            if (hasWarrantyOrPolicy(bill)) {
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Warranty / Return Details", boldFont));
                if (bill.getWarrantyPeriod() != null) document.add(new Paragraph("Warranty Period: " + bill.getWarrantyPeriod(), normalFont));
                if (bill.getWarrantyType() != null) document.add(new Paragraph("Warranty Type: " + bill.getWarrantyType(), normalFont));
                addPolicy(document, "Return Policy", bill.getReturnPolicy(), normalFont);
                addPolicy(document, "Replacement Policy", bill.getReplacementPolicy(), normalFont);
                addPolicy(document, "Exchange Policy", bill.getExchangePolicy(), normalFont);
                addPolicy(document, "Refund Policy", bill.getRefundPolicy(), normalFont);
                addPolicy(document, "Warranty Terms", bill.getWarrantyTerms(), normalFont);
            }


            /*
             * =================================================
             * FOOTER
             * =================================================
             */

            document.add(
                    new Paragraph(" ")
            );

            document.add(
                    new Paragraph(" ")
            );


            Paragraph thankYou =
                    new Paragraph(
                            "Thank you for shopping with "
                                    + (safe(bill.getSellerBusinessName()).equals("-") ? safe(bill.getSellerName()) : bill.getSellerBusinessName())
                                    + "!",
                            boldFont
                    );


            thankYou.setAlignment(
                    Element.ALIGN_CENTER
            );


            document.add(
                    thankYou
            );

            Paragraph signature = new Paragraph("Authorised Signature", normalFont);
            signature.setAlignment(Element.ALIGN_RIGHT);
            signature.setSpacingBefore(20);
            document.add(signature);


            Paragraph generated =
                    new Paragraph(
                            "Powered by ShivHub • This is a computer generated invoice.",
                            smallFont
                    );


            generated.setAlignment(
                    Element.ALIGN_CENTER
            );


            document.add(
                    generated
            );


            /*
             * =================================================
             * CLOSE DOCUMENT
             * =================================================
             */

            document.close();


            /*
             * =================================================
             * RETURN PDF
             * =================================================
             */

            return outputStream.toByteArray();


        } catch (DocumentException | IOException exception) {


            throw new RuntimeException(
                    "Could not generate offline bill PDF",
                    exception
            );


        } finally {


            if (document.isOpen()) {

                document.close();
            }
        }
    }


    /*
     * =========================================================
     * HEADER CELL
     * =========================================================
     */

    private void addHeaderCell(
            PdfPTable table,
            String text,
            Font font) {


        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                text,
                                font
                        )
                );


        cell.setBackgroundColor(
                Color.DARK_GRAY
        );


        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER
        );


        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );


        cell.setPadding(
                6
        );


        table.addCell(
                cell
        );
    }


    /*
     * =========================================================
     * BODY CELL
     * =========================================================
     */

    private void addBodyCell(
            PdfPTable table,
            String text,
            Font font,
            int alignment) {


        PdfPCell cell =
                new PdfPCell(
                        new Phrase(
                                text,
                                font
                        )
                );


        cell.setHorizontalAlignment(
                alignment
        );


        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );


        cell.setPadding(
                5
        );


        table.addCell(
                cell
        );
    }


    /*
     * =========================================================
     * TOTAL ROW
     * =========================================================
     */

    private void addTotalRow(
            PdfPTable table,
            String label,
            String value,
            Font font) {


        PdfPCell labelCell =
                new PdfPCell(
                        new Phrase(
                                label,
                                font
                        )
                );


        labelCell.setBorder(
                Rectangle.NO_BORDER
        );


        labelCell.setHorizontalAlignment(
                Element.ALIGN_RIGHT
        );


        labelCell.setPadding(
                4
        );


        table.addCell(
                labelCell
        );


        PdfPCell valueCell =
                new PdfPCell(
                        new Phrase(
                                value,
                                font
                        )
                );


        valueCell.setBorder(
                Rectangle.NO_BORDER
        );


        valueCell.setHorizontalAlignment(
                Element.ALIGN_RIGHT
        );


        valueCell.setPadding(
                4
        );


        table.addCell(
                valueCell
        );
    }


    /*
     * =========================================================
     * GRAND TOTAL ROW
     * =========================================================
     */

    private void addGrandTotalRow(
            PdfPTable table,
            String label,
            String value,
            Font font) {


        PdfPCell labelCell =
                new PdfPCell(
                        new Phrase(
                                label,
                                font
                        )
                );


        labelCell.setBackgroundColor(
                Color.LIGHT_GRAY
        );


        labelCell.setHorizontalAlignment(
                Element.ALIGN_RIGHT
        );


        labelCell.setPadding(
                7
        );


        table.addCell(
                labelCell
        );


        PdfPCell valueCell =
                new PdfPCell(
                        new Phrase(
                                value,
                                font
                        )
                );


        valueCell.setBackgroundColor(
                Color.LIGHT_GRAY
        );


        valueCell.setHorizontalAlignment(
                Element.ALIGN_RIGHT
        );


        valueCell.setPadding(
                7
        );


        table.addCell(
                valueCell
        );
    }


    /*
     * =========================================================
     * RUPEE FORMAT
     * =========================================================
     */

    private String rupee(
            BigDecimal amount) {


        if (amount == null) {

            return "Rs. 0.00";
        }


        return "Rs. "
                + amount
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        )
                        .toPlainString();
    }


    /*
     * =========================================================
     * PERCENTAGE
     * =========================================================
     */

    private String percentage(
            BigDecimal value) {


        if (value == null) {

            return "0%";
        }


        return value
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                )
                .toPlainString()
                + "%";
    }


    /*
     * =========================================================
     * SAFE STRING
     * =========================================================
     */

    private String sellerInvoiceSubtitle(OfflineBill bill) {
        java.util.List<String> details = new java.util.ArrayList<>();
        if (bill.isInvoiceShowAddress() && bill.getSellerBusinessAddress() != null && !bill.getSellerBusinessAddress().isBlank()) details.add(bill.getSellerBusinessAddress());
        if (bill.isInvoiceShowMobile() && bill.getSellerMobile() != null && !bill.getSellerMobile().isBlank()) details.add("Mobile: " + bill.getSellerMobile());
        if (bill.isInvoiceShowGstin() && bill.getSellerGstin() != null && !bill.getSellerGstin().isBlank()) details.add("GSTIN: " + bill.getSellerGstin());
        if (bill.isInvoiceShowWebsite() && bill.getSellerWebsiteUrl() != null && !bill.getSellerWebsiteUrl().isBlank()) details.add("Website: " + bill.getSellerWebsiteUrl());
        if (bill.isInvoiceShowSocialLinks()) {
            addSocial(details, "Instagram", bill.getSellerInstagramUrl());
            addSocial(details, "Facebook", bill.getSellerFacebookUrl());
            addSocial(details, "WhatsApp", bill.getSellerWhatsappUrl());
            addSocial(details, "YouTube", bill.getSellerYoutubeUrl());
        }
        return details.isEmpty() ? "" : String.join(" | ", details);
    }

    private void addSocial(java.util.List<String> details, String label, String value) {
        if (value != null && !value.isBlank()) details.add(label + ": " + value);
    }

    /** IMEI units retain purchase-time brand, model, RAM, storage and colour details. */
    private String productDescription(OfflineBillItem item) {
        String name = safe(item.getProductName());
        if (item.getSelectedAttributes() != null) name += "\n" + item.getSelectedAttributes();
        if (item.getId() == null) return name;
        java.util.List<String> specs = serialRepository.findSoldForOfflineBillItem(item.getId()).stream()
                .map(PurchaseItemSerial::getPurchaseItem)
                .filter(java.util.Objects::nonNull)
                .map(this::purchaseSpecLine)
                .filter(value -> !value.isBlank())
                .distinct().toList();
        return specs.isEmpty() ? name : name + "\n" + String.join("\n", specs);
    }

    private String itemSerials(OfflineBillItem item) {
        if (item.getId() == null) return "-";
        java.util.List<String> values = serialRepository.findSoldForOfflineBillItem(item.getId()).stream()
                .map(this::serialDisplay)
                .filter(value -> !value.isBlank())
                .toList();
        return values.isEmpty() ? "-" : String.join("\n", values);
    }

    private String amountInWords(BigDecimal amount) {
        long value = amount == null ? 0 : amount.setScale(0, RoundingMode.HALF_UP).longValue();
        if (value == 0) return "Rupees Zero";
        return "Rupees " + indianNumberWords(value);
    }

    private String indianNumberWords(long value) {
        String[] ones = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
        String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};
        if (value < 20) return ones[(int) value];
        if (value < 100) return tens[(int) (value / 10)] + (value % 10 == 0 ? "" : " " + ones[(int) (value % 10)]);
        if (value < 1000) return ones[(int) (value / 100)] + " Hundred" + (value % 100 == 0 ? "" : " " + indianNumberWords(value % 100));
        if (value < 100000) return indianNumberWords(value / 1000) + " Thousand" + (value % 1000 == 0 ? "" : " " + indianNumberWords(value % 1000));
        if (value < 10000000) return indianNumberWords(value / 100000) + " Lakh" + (value % 100000 == 0 ? "" : " " + indianNumberWords(value % 100000));
        return indianNumberWords(value / 10000000) + " Crore" + (value % 10000000 == 0 ? "" : " " + indianNumberWords(value % 10000000));
    }

    private String purchaseSpecLine(PurchaseItem purchaseItem) {
        java.util.List<String> specs = new java.util.ArrayList<>();
        addSpec(specs, "Brand", purchaseItem.getBrand());
        addSpec(specs, "Model", purchaseItem.getModel());
        addSpec(specs, "RAM", purchaseItem.getRam());
        addSpec(specs, "Storage", purchaseItem.getStorage());
        addSpec(specs, "Colour", purchaseItem.getColor());
        addSpec(specs, "HSN/SAC", purchaseItem.getHsnCode());
        return String.join(" | ", specs);
    }

    private String serialDisplay(PurchaseItemSerial serial) {
        java.util.List<String> values = new java.util.ArrayList<>();
        addSpec(values, "IMEI 1", serial.getImei1());
        addSpec(values, "IMEI 2", serial.getImei2());
        addSpec(values, "Serial", serial.getSerialNumber());
        return String.join(" | ", values);
    }

    private void addSpec(java.util.List<String> values, String label, String value) {
        if (value != null && !value.isBlank()) values.add(label + ": " + value.trim());
    }

    private boolean hasWarrantyOrPolicy(OfflineBill bill) { return bill.getWarrantyPeriod()!=null || bill.getWarrantyType()!=null || bill.getReturnPolicy()!=null || bill.getReplacementPolicy()!=null || bill.getExchangePolicy()!=null || bill.getRefundPolicy()!=null || bill.getWarrantyTerms()!=null; }
    private void addPolicy(Document document, String label, String value, Font font) throws DocumentException { if (value != null && !value.isBlank()) document.add(new Paragraph(label + ": " + value, font)); }

    private String safe(
            String value) {


        if (value == null
                || value.isBlank()) {

            return "-";
        }


        return value;
    }


    /*
     * =========================================================
     * DATE FORMAT
     * =========================================================
     */

    private String formatDate(
            LocalDateTime dateTime) {


        if (dateTime == null) {

            return "-";
        }


        return dateTime.format(
                DateTimeFormatter.ofPattern(
                        "dd-MM-yyyy HH:mm"
                )
        );
    }
}

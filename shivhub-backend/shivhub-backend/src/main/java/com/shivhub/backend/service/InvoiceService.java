package com.shivhub.backend.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.shivhub.backend.entity.Order;
import com.shivhub.backend.entity.OrderItem;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.PurchaseItem;
import com.shivhub.backend.entity.PurchaseItemSerial;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.PurchaseItemSerialRepository;


/*
 * =========================================================
 * InvoiceService
 * =========================================================
 *
 * Responsible for generating ShivHub invoice PDFs.
 *
 * Used for:
 *
 * 1. Order confirmation email
 * 2. Customer invoice download
 * 3. Invoice attachment
 *
 * =========================================================
 */

@Service
public class InvoiceService {

    private final PurchaseItemSerialRepository serialRepository;
    private final ProductRepository productRepository;

    public InvoiceService(
            PurchaseItemSerialRepository serialRepository,
            ProductRepository productRepository) {
        this.serialRepository = serialRepository;
        this.productRepository = productRepository;
    }


    /*
     * =========================================================
     * DATE FORMAT
     * =========================================================
     */

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "dd MMM yyyy, hh:mm a"
            );


    /*
     * =========================================================
     * GENERATE INVOICE PDF
     * =========================================================
     *
     * Returns:
     *
     * byte[]
     *
     * This byte array can directly be attached
     * to an email.
     *
     * =========================================================
     */

    public byte[] generateInvoicePdf(
            Order order,
            User customer) {


        /*
         * =====================================================
         * OUTPUT STREAM
         * =====================================================
         */

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();


        /*
         * =====================================================
         * PDF DOCUMENT
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

            PdfWriter.getInstance(
                    document,
                    outputStream
            );


            /*
             * =================================================
             * OPEN DOCUMENT
             * =================================================
             */

            document.open();


            /*
             * =================================================
             * FONTS
             * =================================================
             */

            Font companyFont =
                    new Font(
                            Font.HELVETICA,
                            24,
                            Font.BOLD
                    );


            Font invoiceFont =
                    new Font(
                            Font.HELVETICA,
                            16,
                            Font.BOLD
                    );


            Font headingFont =
                    new Font(
                            Font.HELVETICA,
                            11,
                            Font.BOLD
                    );


            Font normalFont =
                    new Font(
                            Font.HELVETICA,
                            10,
                            Font.NORMAL
                    );


            Font smallFont =
                    new Font(
                            Font.HELVETICA,
                            9,
                            Font.NORMAL
                    );


            Font totalFont =
                    new Font(
                            Font.HELVETICA,
                            12,
                            Font.BOLD
                    );


            /*
             * =================================================
             * COMPANY HEADER
             * =================================================
             */

            Paragraph companyName =
                    new Paragraph(
                            "SHIVHUB",
                            companyFont
                    );

            companyName.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(
                    companyName
            );


            Paragraph companySubtitle =
                    new Paragraph(
                            "E-Commerce Platform",
                            normalFont
                    );

            companySubtitle.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(
                    companySubtitle
            );


            document.add(
                    new Paragraph(" ")
            );

            addSellerSupportDetails(document, order, headingFont, normalFont);


            /*
             * =================================================
             * INVOICE TITLE
             * =================================================
             */

            Paragraph invoiceTitle =
                    new Paragraph(
                            "TAX INVOICE",
                            invoiceFont
                    );

            invoiceTitle.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(
                    invoiceTitle
            );


            document.add(
                    new Paragraph(" ")
            );


            /*
             * =================================================
             * INVOICE INFORMATION TABLE
             * =================================================
             */

            PdfPTable infoTable =
                    new PdfPTable(2);

            infoTable.setWidthPercentage(100);

            infoTable.setWidths(
                    new float[]{
                            1,
                            1
                    }
            );


            /*
             * LEFT SIDE
             */

            PdfPCell invoiceInfoCell =
                    new PdfPCell();

            invoiceInfoCell.setBorder(
                    Rectangle.NO_BORDER
            );


            invoiceInfoCell.addElement(
                    new Paragraph(
                            "Invoice / Order Information",
                            headingFont
                    )
            );


            invoiceInfoCell.addElement(
                    new Paragraph(
                            "Order Number: "
                            + safe(
                                    order.getOrderNumber()
                            ),
                            normalFont
                    )
            );


            invoiceInfoCell.addElement(
                    new Paragraph(
                            "Order Date: "
                            + formatDate(
                                    order
                            ),
                            normalFont
                    )
            );


            invoiceInfoCell.addElement(
                    new Paragraph(
                            "Order Status: "
                            + safeEnum(
                                    order.getOrderStatus()
                            ),
                            normalFont
                    )
            );


            invoiceInfoCell.addElement(
                    new Paragraph(
                            "Payment Status: "
                            + safeEnum(
                                    order.getPaymentStatus()
                            ),
                            normalFont
                    )
            );


            infoTable.addCell(
                    invoiceInfoCell
            );


            /*
             * RIGHT SIDE
             */

            PdfPCell customerInfoCell =
                    new PdfPCell();

            customerInfoCell.setBorder(
                    Rectangle.NO_BORDER
            );


            customerInfoCell.addElement(
                    new Paragraph(
                            "Customer Information",
                            headingFont
                    )
            );


            customerInfoCell.addElement(
                    new Paragraph(
                            "Name: "
                            + safe(
                                    customer.getName()
                            ),
                            normalFont
                    )
            );


            customerInfoCell.addElement(
                    new Paragraph(
                            "Email: "
                            + safe(
                                    customer.getEmail()
                            ),
                            normalFont
                    )
            );


            if (customer.getMobile() != null) {

                customerInfoCell.addElement(
                        new Paragraph(
                                "Mobile: "
                                + customer.getMobile(),
                                normalFont
                        )
                );
            }


            infoTable.addCell(
                    customerInfoCell
            );


            document.add(
                    infoTable
            );


            document.add(
                    new Paragraph(" ")
            );


            /*
             * =================================================
             * SHIPPING ADDRESS
             * =================================================
             */

            Paragraph shippingHeading =
                    new Paragraph(
                            "Shipping Address",
                            headingFont
                    );

            document.add(
                    shippingHeading
            );


            Paragraph shippingAddress =
                    new Paragraph(
                            safe(
                                    order.getShippingAddress()
                            ),
                            normalFont
                    );

            document.add(
                    shippingAddress
            );


            document.add(
                    new Paragraph(" ")
            );


            /*
             * =================================================
             * PRODUCT TABLE
             * =================================================
             *
             * Columns:
             *
             * Product
             * IMEI / Serial
             * Qty
             * Unit Price
             * Total
             *
             * =================================================
             */

            PdfPTable productTable =
                    new PdfPTable(5);

            productTable.setWidthPercentage(
                    100
            );


            productTable.setWidths(
                    new float[]{
                            3,
                            2,
                            1,
                            2,
                            2
                    }
            );


            /*
             * HEADER CELLS
             */

            addHeaderCell(
                    productTable,
                    "Product",
                    headingFont
            );

            addHeaderCell(
                    productTable,
                    "IMEI / Serial",
                    headingFont
            );


            addHeaderCell(
                    productTable,
                    "Qty",
                    headingFont
            );


            addHeaderCell(
                    productTable,
                    "Unit Price",
                    headingFont
            );


            addHeaderCell(
                    productTable,
                    "Total",
                    headingFont
            );


            /*
             * =================================================
             * ORDER ITEMS
             * =================================================
             */

            if (order.getItems() != null
                    && !order.getItems().isEmpty()) {


                for (OrderItem item :
                        order.getItems()) {


                    /*
                     * Product name
                     */

                    addBodyCell(
                            productTable,
                            invoiceProductDescription(item),
                            normalFont,
                            Element.ALIGN_LEFT
                    );


                    addBodyCell(
                            productTable,
                            invoiceSerialSummary(item),
                            normalFont,
                            Element.ALIGN_LEFT
                    );


                    /*
                     * Quantity
                     */

                    addBodyCell(
                            productTable,
                            String.valueOf(
                                    item.getQuantity()
                            ),
                            normalFont,
                            Element.ALIGN_CENTER
                    );


                    /*
                     * Unit price
                     */

                    addBodyCell(
                            productTable,
                            money(
                                    item.getUnitPrice()
                            ),
                            normalFont,
                            Element.ALIGN_RIGHT
                    );


                    /*
                     * Total price
                     */

                    addBodyCell(
                            productTable,
                            money(
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
                                        "No order items",
                                        normalFont
                                )
                        );

                emptyCell.setColspan(5);

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
                    50
            );

            totalsTable.setHorizontalAlignment(
                    Element.ALIGN_RIGHT
            );


            /*
             * SUBTOTAL
             */

            addTotalRow(
                    totalsTable,
                    "Subtotal",
                    money(order.getSubtotal()),
                    normalFont
            );


            /*
             * DISCOUNT
             */

            addTotalRow(
                    totalsTable,
                    "Discount",
                    money(order.getDiscount()),
                    normalFont
            );


            /*
             * TAX
             */

            addTotalRow(
                    totalsTable,
                    "Tax",
                    money(order.getTax()),
                    normalFont
            );


            /*
             * DELIVERY
             */

            addTotalRow(
                    totalsTable,
                    "Delivery",
                    money(order.getDeliveryCharge()),
                    normalFont
            );


            /*
             * GRAND TOTAL
             */

            addTotalRow(
                    totalsTable,
                    "Grand Total",
                    money(order.getGrandTotal()),
                    totalFont
            );


            document.add(
                    totalsTable
            );


            document.add(
                    new Paragraph(" ")
            );


            /*
             * =================================================
             * PAYMENT INFORMATION
             * =================================================
             */

            Paragraph paymentHeading =
                    new Paragraph(
                            "Payment Information",
                            headingFont
                    );

            document.add(
                    paymentHeading
            );


            document.add(
                    new Paragraph(
                            "Payment Status: "
                            + safeEnum(
                                    order.getPaymentStatus()
                            ),
                            normalFont
                    )
            );


            document.add(
                    new Paragraph(" ")
            );


            /*
             * =================================================
             * THANK YOU MESSAGE
             * =================================================
             */

            Paragraph thankYou =
                    new Paragraph(
                            "Thank you for shopping with ShivHub!",
                            headingFont
                    );

            thankYou.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(
                    thankYou
            );


            document.add(
                    new Paragraph(
                            "For support, please contact ShivHub Team.",
                            smallFont
                    )
            );


            /*
             * =================================================
             * FOOTER
             * =================================================
             */

            Paragraph footer =
                    new Paragraph(
                            "\nThis is a computer-generated invoice "
                            + "and does not require a signature.",
                            smallFont
                    );

            footer.setAlignment(
                    Element.ALIGN_CENTER
            );

            document.add(
                    footer
            );


            /*
             * =================================================
             * CLOSE DOCUMENT
             * =================================================
             */

            document.close();


            /*
             * =================================================
             * RETURN PDF BYTES
             * =================================================
             */

            return outputStream.toByteArray();


        } catch (Exception exception) {

            /*
             * =================================================
             * PDF GENERATION ERROR
             * =================================================
             */

            throw new RuntimeException(
                    "Failed to generate invoice PDF",
                    exception
            );
        }
    }

    /** For online mobile orders, the customer must know the actual selling shop for service. */
    private void addSellerSupportDetails(Document document, Order order, Font headingFont, Font normalFont) throws Exception {
        if (order.getItems() == null || order.getItems().isEmpty()) return;
        java.util.Map<String, OrderItem> shops = new java.util.LinkedHashMap<>();
        for (OrderItem item : order.getItems()) if (item.getSellerName() != null) shops.putIfAbsent(item.getSellerName() + "|" + item.getSellerMobile(), item);
        if (shops.isEmpty()) return;
        document.add(new Paragraph("Shop Support & Warranty", headingFont));
        document.add(new Paragraph("For product issues, contact the selling shop shown below.", normalFont));
        for (OrderItem item : shops.values()) {
            String details = "Shop: " + safe(item.getSellerName()) + "\nAddress: " + safe(item.getSellerAddress()) + "\nContact: " + safe(item.getSellerMobile()) + "\nGSTIN: " + safe(item.getSellerGstin());
            if (item.getSellerWarrantyPeriod() != null) details += "\nWarranty: " + item.getSellerWarrantyPeriod() + (item.getSellerWarrantyType() == null ? "" : " (" + item.getSellerWarrantyType() + ")");
            if (item.getSellerReturnPolicy() != null) details += "\nReturn Policy: " + item.getSellerReturnPolicy();
            if (item.getSellerWarrantyTerms() != null) details += "\nWarranty Terms: " + item.getSellerWarrantyTerms();
            document.add(new Paragraph(details, normalFont));
        }
        document.add(new Paragraph(" "));
    }

    /*
     * =========================================================
     * ONLINE INVOICE PRODUCT DETAILS
     * =========================================================
     *
     * Shows the exact mobile details on the invoice.
     *
     * Priority:
     * 1. Assigned IMEI purchase snapshot - exact sold handset details
     * 2. Product master - fallback before IMEI is assigned
     *
     * =========================================================
     */

    private String invoiceProductDescription(OrderItem item) {
        java.util.List<String> lines = new java.util.ArrayList<>();

        lines.add(safe(item.getProductName()));
        if (item.getSelectedAttributes() != null) lines.add(item.getSelectedAttributes());

        java.util.List<PurchaseItemSerial> assignedSerials =
                item.getId() == null
                        ? java.util.List.of()
                        : serialRepository.findAssignedForOrderItem(item.getId());

        java.util.List<String> purchaseSpecs =
                assignedSerials.stream()
                        .map(PurchaseItemSerial::getPurchaseItem)
                        .filter(java.util.Objects::nonNull)
                        .map(this::purchaseSpecLine)
                        .filter(value -> !value.isBlank())
                        .distinct()
                        .toList();

        if (!purchaseSpecs.isEmpty()) {
            lines.addAll(purchaseSpecs);
            return String.join("\n", lines);
        }

        productRepository.findById(item.getProductId())
                .map(this::productSpecLine)
                .filter(value -> !value.isBlank())
                .ifPresent(lines::add);

        return String.join("\n", lines);
    }

    private String invoiceSerialSummary(OrderItem item) {
        java.util.List<PurchaseItemSerial> assignedSerials =
                item.getId() == null
                        ? java.util.List.of()
                        : serialRepository.findAssignedForOrderItem(item.getId());

        java.util.List<String> values =
                assignedSerials.stream()
                        .map(this::serialDisplay)
                        .filter(value -> !value.isBlank())
                        .toList();

        if (!values.isEmpty()) {
            return String.join("\n", values);
        }

        return safe(item.getAssignedSerialSummary());
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

    private String productSpecLine(Product product) {
        java.util.List<String> specs = new java.util.ArrayList<>();

        addSpec(specs, "Brand", product.getBrand());
        addSpec(specs, "Model", product.getModel());
        addSpec(specs, "RAM", product.getRam());
        addSpec(specs, "Storage", product.getStorage());
        addSpec(specs, "Colour", product.getColorOptions());
        addSpec(specs, "HSN/SAC", product.getHsnCode());

        return String.join(" | ", specs);
    }

    private void addSpec(java.util.List<String> specs, String label, String value) {
        if (value != null && !value.trim().isEmpty()) {
            specs.add(label + ": " + value.trim());
        }
    }

    private String serialDisplay(PurchaseItemSerial serial) {
        java.util.List<String> values = new java.util.ArrayList<>();

        addPlain(values, "IMEI 1", serial.getImei1());
        addPlain(values, "IMEI 2", serial.getImei2());
        addPlain(values, "Serial", serial.getSerialNumber());

        return String.join(" | ", values);
    }

    private void addPlain(java.util.List<String> values, String label, String value) {
        if (value != null && !value.trim().isEmpty()) {
            values.add(label + ": " + value.trim());
        }
    }


    /*
     * =========================================================
     * ADD HEADER CELL
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


        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER
        );


        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );


        cell.setPadding(
                7
        );


        table.addCell(
                cell
        );
    }


    /*
     * =========================================================
     * ADD BODY CELL
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
                6
        );


        table.addCell(
                cell
        );
    }


    /*
     * =========================================================
     * ADD TOTAL ROW
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
                5
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
                5
        );


        table.addCell(
                valueCell
        );
    }


    /*
     * =========================================================
     * FORMAT DATE
     * =========================================================
     */

    private String formatDate(
            Order order) {


        if (order.getCreatedAt() == null) {

            return "-";
        }


        return order.getCreatedAt()
                .format(
                        DATE_FORMATTER
                );
    }


    /*
     * =========================================================
     * FORMAT MONEY
     * =========================================================
     */

    private String money(
            BigDecimal amount) {


        if (amount == null) {

            return "₹0.00";
        }


        return "₹"
                + amount.setScale(
                        2
                ).toPlainString();
    }


    /*
     * =========================================================
     * SAFE STRING
     * =========================================================
     */

    private String safe(
            String value) {


        if (value == null
                || value.trim().isEmpty()) {

            return "-";
        }


        return value;
    }


    /*
     * =========================================================
     * SAFE ENUM
     * =========================================================
     */

    private String safeEnum(
            Enum<?> value) {


        if (value == null) {

            return "-";
        }


        return value.name();
    }
}

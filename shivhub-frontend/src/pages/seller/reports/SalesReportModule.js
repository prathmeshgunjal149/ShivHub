import { dateTime, money } from "./reportFormatters";
export const salesReport = [["Source", "Reference", "Sale date", "Customer", "Product", "Payment", "Qty", "GST", "Total"], r => [r.source, r.referenceNumber, dateTime(r.saleDate), r.customerName || "—", r.productName, r.paymentMethod?.replace("_", " ") || "—", r.quantity, money(r.gst), money(r.total)]];

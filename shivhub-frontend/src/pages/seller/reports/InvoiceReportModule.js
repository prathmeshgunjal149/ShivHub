import { dateTime, money } from "./reportFormatters";
export const invoiceReport = [["Bill number", "Customer", "Mobile", "Date", "Total"], r => [r.billNumber, r.customerName || "Walk-in customer", r.customerMobile || "—", dateTime(r.createdAt), money(r.grandTotal)]];

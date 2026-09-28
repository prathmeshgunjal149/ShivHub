import { money } from "./reportFormatters";
export const customerCreditReport = [["Customer", "Mobile", "Invoice", "Sale", "Received", "Balance", "Promise date", "Status"], row => [row.customerName, row.customerMobile || "—", row.invoiceNumber || "—", money(row.saleAmount), money(row.paidAmount), money(row.remainingAmount), row.dueDate || "—", row.paymentStatus]];

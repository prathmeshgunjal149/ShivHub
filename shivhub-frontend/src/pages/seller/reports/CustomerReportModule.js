import { dateTime, money } from "./reportFormatters";
export const customerReport = [["Customer", "Mobile", "Bills", "Spent", "Last purchase"], r => [r.customer, r.mobile || "—", r.billCount, money(r.totalSpent), dateTime(r.lastPurchaseAt)]];

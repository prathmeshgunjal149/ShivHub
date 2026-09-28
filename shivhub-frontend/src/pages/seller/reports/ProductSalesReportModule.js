import { money } from "./reportFormatters";
export const productSalesReport = [["Product", "Category", "Online", "POS", "Total qty", "Sales"], r => [r.productName, r.category || "—", r.onlineQuantity, r.posQuantity, r.totalQuantity, money(r.totalSales)]];

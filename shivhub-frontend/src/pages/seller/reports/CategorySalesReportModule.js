import { money } from "./reportFormatters";
export const categorySalesReport = [["Category", "Online", "POS", "Total qty", "Sales"], r => [r.category, r.onlineQuantity, r.posQuantity, r.totalQuantity, money(r.totalSales)]];

import { money } from "./reportFormatters";
export const paymentReport = [["Payment method", "Bills", "Amount"], r => [r.method?.replace("_", " "), r.billCount, money(r.amount)]];

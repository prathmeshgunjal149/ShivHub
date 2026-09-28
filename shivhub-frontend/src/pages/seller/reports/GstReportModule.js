import { money } from "./reportFormatters";
export const gstReport = [["Source", "Taxable", "CGST", "SGST", "IGST", "Discount", "Total"], r => [r.source, money(r.taxableAmount), money(r.cgst), money(r.sgst), money(r.igst), money(r.discount), money(r.total)]];

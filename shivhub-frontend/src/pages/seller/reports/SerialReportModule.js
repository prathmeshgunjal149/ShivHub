import { dateTime } from "./reportFormatters";
export const serialReport = [["Product", "IMEI / Serial", "Status", "Sold at", "Bill"], r => [r.productName, r.imei1 || r.imei2 || r.serialNumber || "—", r.status, dateTime(r.soldAt), r.billNumber || "—"]];

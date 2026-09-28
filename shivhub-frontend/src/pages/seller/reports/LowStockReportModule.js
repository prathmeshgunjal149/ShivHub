export const lowStockReport = [["Product", "Category", "Units remaining"], r => [r.productName, r.category || "—", r.stock]];

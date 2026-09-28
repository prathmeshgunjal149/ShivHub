export const stockReport = [["Product", "Category", "Stock", "Status"], r => [r.productName, r.category || "—", r.stock, r.lowStock ? "Low stock" : "Healthy"]];

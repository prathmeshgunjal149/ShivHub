import api from "./api";

export const CA_REPORTS = [
    {
        key: "sales",
        title: "Sales Report",
        description: "Online + POS invoices, customer, GST, discount, net amount",
        endpoint: "/api/ca-reports/sales.csv",
        filename: "shivhub-sales-report.csv",
        dateRange: true
    },
    {
        key: "purchase",
        title: "Purchase Report",
        description: "Distributor purchases, invoice values, GST, returns",
        endpoint: "/api/ca-reports/purchase.csv",
        filename: "shivhub-purchase-report.csv",
        dateRange: true
    },
    {
        key: "stock",
        title: "Stock Report",
        description: "Opening, purchases, sales, transfers, returns, closing stock",
        endpoint: "/api/ca-reports/stock.csv",
        filename: "shivhub-stock-report.csv"
    },
    {
        key: "expenses",
        title: "Expense Report",
        description: "Category-wise expenses and receipts",
        endpoint: "/api/ca-reports/expenses.csv",
        filename: "shivhub-expense-report.csv",
        dateRange: true
    },
    {
        key: "receivables",
        title: "Receivables",
        description: "Customer opening, credit sales, collections, closing balance",
        endpoint: "/api/ca-reports/receivables.csv",
        filename: "shivhub-receivables-report.csv"
    },
    {
        key: "payables",
        title: "Payables",
        description: "Distributor opening, purchases, payments, credit notes, closing balance",
        endpoint: "/api/ca-reports/payables.csv",
        filename: "shivhub-payables-report.csv"
    },
    {
        key: "payments",
        title: "Payment / Collection",
        description: "Cash, UPI, bank, refunds and transaction references",
        endpoint: "/api/ca-reports/payments.csv",
        filename: "shivhub-payment-collection-report.csv",
        dateRange: true
    },
    {
        key: "creditDebitNotes",
        title: "Credit / Debit Notes",
        description: "Returns, discounts, adjustments and linked references",
        endpoint: "/api/ca-reports/credit-debit-notes.csv",
        filename: "shivhub-credit-debit-note-report.csv",
        dateRange: true
    },
    {
        key: "stockTransfers",
        title: "Stock Transfer Report",
        description: "IMEI outward, receiving shop, distributor reference and settlement",
        endpoint: "/api/ca-reports/stock-transfers.csv",
        filename: "shivhub-stock-transfer-report.csv",
        dateRange: true
    },
    {
        key: "gstSummary",
        title: "GST Summary",
        description: "Sales and purchase GST reconciliation",
        endpoint: "/api/ca-reports/gst-summary.csv",
        filename: "shivhub-gst-summary.csv",
        dateRange: true
    },
    {
        key: "profitLoss",
        title: "Profit & Loss",
        description: "Net sales, cost, expenses and calculated profit",
        endpoint: "/api/ca-reports/profit-loss.csv",
        filename: "shivhub-profit-loss-summary.csv",
        dateRange: true
    }
];

const reportByKey = Object.fromEntries(CA_REPORTS.map(report => [report.key, report]));

const saveBlob = (blob, filename) => {
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
};

export const downloadCaCsv = async (reportKey, { startDate, endDate } = {}) => {
    const report = reportByKey[reportKey];
    if (!report) throw new Error("Unknown CA report.");

    const response = await api.get(report.endpoint, {
        params: report.dateRange ? { startDate, endDate } : {},
        responseType: "blob"
    });

    saveBlob(response.data, report.filename);
};

export const downloadCaBundle = async (type, { startDate, endDate } = {}) => {
    const config = {
        workbook: {
            endpoint: "/api/ca-reports/workbook.xls",
            filename: "shivhub-ca-workbook.xls"
        },
        pdf: {
            endpoint: "/api/ca-reports/summary.pdf",
            filename: "shivhub-ca-summary.pdf"
        },
        zip: {
            endpoint: "/api/ca-reports/documents.zip",
            filename: "shivhub-ca-documents.zip"
        }
    }[type];

    if (!config) throw new Error("Unknown CA bundle.");

    const response = await api.get(config.endpoint, {
        params: { startDate, endDate },
        responseType: "blob"
    });

    saveBlob(response.data, config.filename);
};

export const downloadSalesCsv = range => downloadCaCsv("sales", range);

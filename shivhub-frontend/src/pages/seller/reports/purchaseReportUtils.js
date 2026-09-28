export const purchaseHeaders = [
    "Invoice",
    "Distributor",
    "Date",
    "Status",
    "Total",
    "Bill image",
];

export const purchaseCells = (row) => [
    row?.invoiceNumber || "—",

    row?.distributor || "—",

    row?.purchaseDate
        ? new Date(row.purchaseDate).toLocaleString("en-IN", {
              dateStyle: "medium",
              timeStyle: "short",
          })
        : "—",

    row?.status || "—",

    `₹${Number(row?.total || 0).toLocaleString("en-IN", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    })}`,
];

export const matchesPurchaseFilters = (
    row,
    {
        status = "",
        distributor = "",
        minAmount = "",
        maxAmount = "",
    } = {}
) => {
    const total = Number(row?.total || 0);

    const normalizedDistributor = String(
        row?.distributor || ""
    ).toLowerCase();

    const distributorFilter = String(
        distributor || ""
    )
        .trim()
        .toLowerCase();

    const hasMinAmount =
        minAmount !== "" &&
        minAmount !== null &&
        minAmount !== undefined &&
        Number.isFinite(Number(minAmount));

    const hasMaxAmount =
        maxAmount !== "" &&
        maxAmount !== null &&
        maxAmount !== undefined &&
        Number.isFinite(Number(maxAmount));

    return (
        (!status || row?.status === status) &&
        (!distributorFilter ||
            normalizedDistributor.includes(distributorFilter)) &&
        (!hasMinAmount || total >= Number(minAmount)) &&
        (!hasMaxAmount || total <= Number(maxAmount))
    );
};

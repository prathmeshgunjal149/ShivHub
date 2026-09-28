import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
    addPurchasePayment,
    cancelPurchase,
    createPurchase,
    getActiveSellerDistributors,
    getPurchaseItems,
    getPurchasePaymentSummary,
    getPurchasePayments,
    getPurchases,
    getPurchaseSpecificationTemplates,
    getPurchaseVariants,
    getProductCategories,
    getProductSubcategories,
    searchPurchaseProducts,
    uploadPurchaseInvoice,
    uploadPurchasedProductImages,
    validatePurchaseImei
} from "../../services/purchaseService";

import "./Purchases.css";

/* =========================================================
   HELPERS
========================================================= */

const emptyItem = () => ({
    categoryId: "",
    subCategoryId: "",
    productId: "",
    variantId: "",
    productName: "",
    quantity: 1,
    unitPrice: "",
    discount: 0,

    gstRate: 18,
    cgstRate: 9,
    sgstRate: 9,

    unit: "NOS",
    hsnCode: "",
    sku: "",
    barcode: "",
    color: "",
    brand: "",
    model: "",
    ram: "",
    storage: "",
    description: "",
    attributes: {},
    isMobile: false,

    images: [],

    imeiTrackingRequired: false,
    serials: []
});

const getLocalDateTimeInput = () => {
    const now = new Date();
    const offset = now.getTimezoneOffset();
    return new Date(now.getTime() - offset * 60_000)
        .toISOString()
        .slice(0, 16);
};

const toDateTimeValue = value => {
    if (!value) return null;
    return value.length === 16 ? `${value}:00` : value;
};

const normalizeText = value => String(value ?? "").trim();

const isValidImei = value => /^\d{15}$/.test(normalizeText(value));

const parseTemplateOptions = raw => {
    try {
        const values = JSON.parse(raw || "[]");
        return Array.isArray(values) ? values : [];
    } catch {
        return String(raw || "").split(",").map(value => value.trim()).filter(Boolean);
    }
};

const MAX_UPLOAD_SIZE = 10 * 1024 * 1024;

const initialForm = () => ({
    sellerDistributorId: "",
    invoiceNumber: "",
    purchaseDate: getLocalDateTimeInput(),

    irn: "",
    acknowledgementNumber: "",
    acknowledgementDate: "",
    ewayBillNumber: "",

    deliveryNote: "",
    referenceNumber: "",
    buyerOrderNumber: "",
    dispatchDocumentNumber: "",
    dispatchedThrough: "",
    destination: "",
    termsOfDelivery: "",

    roundOff: 0,
    discount: 0,
    notes: "",

    invoice: null
});

const money = (value) =>
    new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 2
    }).format(Number(value || 0));

const number = (value) => Number(value || 0);

const errorMessage = (error, fallback) =>
    error?.response?.data?.message || fallback;

const inferBrand = (name = "") => {
    const value = String(name).trim().toLowerCase();

    const brands = [
        ["samsung", "Samsung"],
        ["apple", "Apple"],
        ["vivo", "Vivo"],
        ["oppo", "Oppo"],
        ["realme", "Realme"],
        ["oneplus", "OnePlus"],
        ["motorola", "Motorola"],
        ["nothing", "Nothing"],
        ["pixel", "Google Pixel"],
        ["google", "Google Pixel"],
        ["xiaomi", "Xiaomi"],
        ["redmi", "Xiaomi / Redmi"],
        ["poco", "Xiaomi / Redmi"]
    ];

    return (
        brands.find(([prefix]) => value.startsWith(prefix))?.[1] || ""
    );
};

/* =========================================================
   COMPONENT
========================================================= */

export default function Purchases() {
    const navigate = useNavigate();

    /* =====================================================
       DATA
    ===================================================== */

    const [purchases, setPurchases] = useState([]);
    const [categories, setCategories] = useState([]);
    const [subcategories, setSubcategories] = useState({});
    const [distributors, setDistributors] = useState([]);

    /* =====================================================
       PURCHASE FORM
    ===================================================== */

    const [items, setItems] = useState([emptyItem()]);
    const [form, setForm] = useState(initialForm());

    /* =====================================================
       UI STATE
    ===================================================== */

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [paymentSaving, setPaymentSaving] = useState(false);
    const detailRequestId = useRef(0);

    const [message, setMessage] = useState("");
    const [error, setError] = useState("");

    const [activeStep, setActiveStep] = useState(1);
    const [productSearches, setProductSearches] = useState({});
    const [catalogueResults, setCatalogueResults] = useState({});
    const [catalogueLoading, setCatalogueLoading] = useState({});
    const [catalogueErrors, setCatalogueErrors] = useState({});
    const [attributeTemplates, setAttributeTemplates] = useState({});
    const [productVariants, setProductVariants] = useState({});
    const [variantLoading, setVariantLoading] = useState({});
    const [imeiScanValues, setImeiScanValues] = useState({});
    const [imeiScanLoading, setImeiScanLoading] = useState({});
    const searchTimers = useRef({});
    const scannerInputs = useRef({});
    const templateRequestTokens = useRef({});

    /* =====================================================
       PURCHASE DETAIL
    ===================================================== */

    const [selectedPurchase, setSelectedPurchase] = useState(null);
    const [purchaseItems, setPurchaseItems] = useState([]);
    const [paymentSummary, setPaymentSummary] = useState(null);
    const [payments, setPayments] = useState([]);

    const [payment, setPayment] = useState({
        amount: "",
        paymentMethod: "UPI",
        transactionReference: "",
        notes: ""
    });

    /* =====================================================
       LOAD DATA
    ===================================================== */

    const loadData = useCallback(async () => {
        setLoading(true);

        try {
            const [
                purchaseData,
                distributorData,
                categoryData
            ] = await Promise.all([
                getPurchases(),
                getActiveSellerDistributors(),
                getProductCategories()
            ]);

            setPurchases(
                Array.isArray(purchaseData) ? purchaseData : []
            );

            setDistributors(
                Array.isArray(distributorData)
                    ? distributorData
                    : []
            );

            const activeCategories = Array.isArray(categoryData)
                ? categoryData
                : [];

            setCategories(activeCategories);

            // Categories already carry their child rows in some API responses.
            // Seed the cache immediately so the subcategory dropdown never waits
            // for a second request before it can render.
            setSubcategories(current => {
                const next = { ...current };

                activeCategories.forEach(category => {
                    const rows = Array.isArray(category.subCategories)
                        ? category.subCategories.filter(row => row.active !== false)
                        : [];

                    if (rows.length && !next[String(category.id)]?.length) {
                        next[String(category.id)] = rows;
                    }
                });

                return next;
            });
            setError("");
        } catch (requestError) {
            setError(
                errorMessage(
                    requestError,
                    "Purchase data could not be loaded."
                )
            );
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        const timer = window.setTimeout(() => {
            void loadData();
        }, 0);

        return () => window.clearTimeout(timer);
    }, [loadData]);

    /* =====================================================
       DERIVED DATA
    ===================================================== */

    const selectedDistributor = useMemo(
        () =>
            distributors.find(
                distributor =>
                    String(distributor.id) ===
                    String(form.sellerDistributorId)
            ),
        [distributors, form.sellerDistributorId]
    );

    const totals = useMemo(() => {
        return items.reduce(
            (result, item) => {
                const quantity = number(item.quantity);
                const rate = number(item.unitPrice);

                const gross = quantity * rate;

                const itemDiscount = Math.min(
                    gross,
                    number(item.discount)
                );

                const taxable = Math.max(
                    0,
                    gross - itemDiscount
                );

                const cgstRate = number(item.cgstRate);
                const sgstRate = number(item.sgstRate);

                const cgst = taxable * cgstRate / 100;
                const sgst = taxable * sgstRate / 100;

                const total = taxable + cgst + sgst;

                return {
                    subtotal: result.subtotal + gross,
                    itemDiscount:
                        result.itemDiscount + itemDiscount,
                    taxable:
                        result.taxable + taxable,
                    cgst: result.cgst + cgst,
                    sgst: result.sgst + sgst,
                    gst: result.gst + cgst + sgst,
                    total: result.total + total,
                    quantity:
                        result.quantity + quantity
                };
            },
            {
                subtotal: 0,
                itemDiscount: 0,
                taxable: 0,
                cgst: 0,
                sgst: 0,
                gst: 0,
                total: 0,
                quantity: 0
            }
        );
    }, [items]);

    const billDiscount = Math.max(
        0,
        number(form.discount)
    );

    const roundOff = number(form.roundOff);

    // Bill-level discount reduces the taxable base. Allocate it
    // proportionally across lines so GST and the grand total stay
    // mathematically consistent with the displayed taxable amount.
    const appliedBillDiscount = Math.min(
        billDiscount,
        totals.taxable
    );

    const billDiscountRatio =
        totals.taxable > 0
            ? appliedBillDiscount / totals.taxable
            : 0;

    const taxableAfterBillDiscount =
        Math.max(0, totals.taxable - appliedBillDiscount);

    const gstAfterBillDiscount =
        totals.gst * (1 - billDiscountRatio);

    const grandTotal =
        taxableAfterBillDiscount +
        gstAfterBillDiscount +
        roundOff;

    const productCount = items.length;

    const selectedProductCount = items.filter(
        item => item.productId
    ).length;

    /* =====================================================
       PRODUCT FILTER
    ===================================================== */

    const productsForItem = useCallback(
        (_, index) => catalogueResults[index]?.content || [],
        [catalogueResults]
    );

    const categoryIsMobile = useCallback(categoryId => {
        const category = categories.find(row => String(row.id) === String(categoryId));
        return /^mobiles?$/i.test(String(category?.name || "").trim());
    }, [categories]);

    const subcategoriesFor = useCallback(categoryId => {
        const cached = subcategories[String(categoryId)];

        if (Array.isArray(cached) && cached.length) {
            return cached;
        }

        const category = categories.find(
            row => String(row.id) === String(categoryId)
        );

        return Array.isArray(category?.subCategories)
            ? category.subCategories.filter(row => row.active !== false)
            : [];
    }, [categories, subcategories]);

    const searchCatalogue = useCallback(async (index, query = "", categoryId = "", subCategoryId = "", page = 0) => {
        setCatalogueLoading(current => ({ ...current, [index]: true }));
        setCatalogueErrors(current => ({ ...current, [index]: "" }));
        try {
            const result = await searchPurchaseProducts({
                q: normalizeText(query),
                categoryId: categoryId || undefined,
                subCategoryId: subCategoryId || undefined,
                page,
                size: 12
            });
            setCatalogueResults(current => ({ ...current, [index]: result }));
            return result;
        } catch (requestError) {
            const emptyResult = { content: [], totalPages: 0, number: page };
            setCatalogueResults(current => ({ ...current, [index]: emptyResult }));
            setCatalogueErrors(current => ({ ...current, [index]: errorMessage(requestError, "Product search could not be loaded.") }));
            return emptyResult;
        } finally {
            setCatalogueLoading(current => ({ ...current, [index]: false }));
        }
    }, []);

    const loadAttributeTemplates = useCallback(async (index, categoryId, subCategoryId, isMobile, productId, initialAttributes = {}) => {
        const requestToken = (templateRequestTokens.current[index] || 0) + 1;
        templateRequestTokens.current[index] = requestToken;
        if (!categoryId || isMobile) {
            setAttributeTemplates(current => ({ ...current, [index]: [] }));
            return;
        }
        try {
            const templates = await getPurchaseSpecificationTemplates(categoryId, subCategoryId || undefined);
            if (templateRequestTokens.current[index] !== requestToken) return;
            const activeTemplates = Array.isArray(templates) ? templates : [];
            setAttributeTemplates(current => ({ ...current, [index]: activeTemplates }));
            // Preserve values typed before the catalogue card is chosen, while removing
            // values that do not belong to the newly selected subcategory.
            setItems(current => current.map((item, itemIndex) => {
                if (itemIndex !== index || (productId && String(item.productId) !== String(productId))) return item;
                const allowed = new Set(activeTemplates.map(template => template.specificationKey));
                const configuredValues = Object.fromEntries(Object.entries(initialAttributes || {}).filter(([key]) => allowed.has(key)));
                const typedValues = Object.fromEntries(Object.entries(item.attributes || {}).filter(([key]) => allowed.has(key)));
                return { ...item, attributes: { ...configuredValues, ...typedValues } };
            }));
        } catch (requestError) {
            if (templateRequestTokens.current[index] !== requestToken) return;
            setAttributeTemplates(current => ({ ...current, [index]: [] }));
            setError(errorMessage(requestError, "Product specification fields could not be loaded."));
        }
    }, []);

    /* =====================================================
       FORM HELPERS
    ===================================================== */

    const updateForm = (field, value) => {
        setForm(current => ({
            ...current,
            [field]: value
        }));
    };

    const updateItem = (index, field, value) => {
        setItems(current =>
            current.map((item, itemIndex) =>
                itemIndex === index
                    ? {
                          ...item,
                          [field]: value
                      }
                    : item
            )
        );
    };

    const removeItem = index => {
        setItems(current => {
            if (current.length === 1) {
                return [emptyItem()];
            }

            return current.filter(
                (_, itemIndex) => itemIndex !== index
            );
        });

        setProductSearches(current => {
            const next = {};
            Object.entries(current).forEach(([key, value]) => {
                const oldIndex = Number(key);
                if (oldIndex < index) next[oldIndex] = value;
                if (oldIndex > index) next[oldIndex - 1] = value;
            });
            return next;
        });
        setProductVariants(current => {
            const next = {};
            Object.entries(current).forEach(([key, value]) => {
                const oldIndex = Number(key);
                if (oldIndex < index) next[oldIndex] = value;
                if (oldIndex > index) next[oldIndex - 1] = value;
            });
            return next;
        });
    };

    const addItem = () => {
        setItems(current => [
            ...current,
            emptyItem()
        ]);

        setActiveStep(1);
    };

    /* =====================================================
       CATEGORY
    ===================================================== */

    const selectCategory = async (
        index,
        categoryId
    ) => {
        setItems(current =>
            current.map((item, itemIndex) =>
                itemIndex === index
                    ? {
                          ...item,
                          categoryId,
                          subCategoryId: "",
                          productId: "",
                          variantId: "",
                          attributes: {},
                          isMobile: false,
                          imeiTrackingRequired: false,
                          serials: []
                      }
                    : item
            )
        );

        setProductSearches(current => ({
            ...current,
            [index]: ""
        }));
        setAttributeTemplates(current => ({ ...current, [index]: [] }));
        setProductVariants(current => ({ ...current, [index]: [] }));

        if (!categoryId) {
            return;
        }

        try {
            const data =
                await getProductSubcategories(
                    categoryId
                );

            setSubcategories(current => ({
                ...current,
                [String(categoryId)]: Array.isArray(data)
                    ? data
                    : []
            }));
            void searchCatalogue(index, productSearches[index] || "", categoryId);
        } catch (requestError) {
            setError(
                errorMessage(
                    requestError,
                    "Subcategories could not be loaded."
                )
            );
        }
    };

    const selectSubCategory = (index, subCategoryId) => {
        const categoryId = items[index]?.categoryId || "";
        setItems(current => current.map((item, itemIndex) => itemIndex === index ? {
            ...item,
            subCategoryId,
                          productId: "",
                          variantId: "",
            attributes: {},
            isMobile: false,
            imeiTrackingRequired: false,
            serials: []
        } : item));
        setProductVariants(current => ({ ...current, [index]: [] }));
        void searchCatalogue(index, productSearches[index] || "", categoryId, subCategoryId);
        // Non-mobile purchase fields are driven by the selected subcategory. Showing
        // them immediately makes it clear which details are required before a card is chosen.
        void loadAttributeTemplates(index, categoryId, subCategoryId || undefined, categoryIsMobile(categoryId), null, {});
    };

    /* =====================================================
       PRODUCT
    ===================================================== */

    const selectProduct = (index, product) => {
        if (!product) {
            updateItem(index, "productId", "");
            return;
        }

        const selectedCategoryId = Number(
            items[index]?.categoryId || 0
        );

        const productCategoryId = Number(
            product.categoryId ||
                0
        );

        if (
            selectedCategoryId &&
            productCategoryId &&
            productCategoryId !== selectedCategoryId
        ) {
            setError(
                "Choose a product from the selected category."
            );
            return;
        }

        const gstRate = number(
            product.gstRate || 18
        );

        const mobileSpecifications =
            product.productSpecifications &&
            typeof product.productSpecifications === "object"
                ? product.productSpecifications
                : {};

        const firstConfiguredValue = (...values) =>
            values.find(value => String(value || "").trim())?.trim() || "";

        setItems(current =>
            current.map(
                (item, itemIndex) =>
                    itemIndex === index
                        ? {
                              ...item,
                              productId: String(product.id),
                              variantId: "",
                              categoryId: product.categoryId || item.categoryId,
                              subCategoryId: product.subCategoryId || "",
                              productName:
                                  product.name,
                              unitPrice:
                                  product.purchasePrice ||
                                  product.price ||
                                  "",
                              gstRate,
                              cgstRate:
                                  gstRate / 2,
                              sgstRate:
                                  gstRate / 2,
                              hsnCode:
                                  product.hsnCode ||
                                  "",
                              sku: product.sellerSku || "",
                              barcode: product.barcode || "",
                              brand:
                                  product.brand ||
                                  inferBrand(
                                      product.name
                                  ),
                              model: product.mobile ? (product.model || "") : "",
                              color: product.mobile
                                  ? firstConfiguredValue(
                                      product.colorOptions,
                                      mobileSpecifications.colorOptions,
                                      mobileSpecifications.color,
                                      mobileSpecifications.colour
                                  )
                                  : "",
                              ram: product.mobile
                                  ? firstConfiguredValue(
                                      product.ram,
                                      mobileSpecifications.ram
                                  )
                                  : "",
                              storage: product.mobile
                                  ? firstConfiguredValue(
                                      product.storage,
                                      mobileSpecifications.storage
                                  )
                                  : "",
                              isMobile: Boolean(product.mobile),
                              imeiTrackingRequired: false,
                              serials: [],
                              attributes: item.attributes || {}
                          }
                        : item
            )
        );

        setProductSearches(current => ({
            ...current,
            [index]: ""
        }));
        setError("");
        setProductVariants(current => ({ ...current, [index]: [] }));
        if (product.variantsEnabled) {
            setVariantLoading(current => ({ ...current, [index]: true }));
            void getPurchaseVariants(product.id).then(rows => {
                setProductVariants(current => ({ ...current, [index]: Array.isArray(rows) ? rows : [] }));
            }).catch(requestError => setError(errorMessage(requestError, "Product variants could not be loaded.")))
                .finally(() => setVariantLoading(current => ({ ...current, [index]: false })));
        }
        void loadAttributeTemplates(index, product.categoryId, product.subCategoryId, Boolean(product.mobile), product.id, product.productSpecifications || {});
        setActiveStep(2);
    };

    const updateProductSearch = (index, value) => {
        setProductSearches(current => ({ ...current, [index]: value }));
        window.clearTimeout(searchTimers.current[index]);
        searchTimers.current[index] = window.setTimeout(() => {
            const item = items[index] || {};
            void searchCatalogue(index, value, item.categoryId, item.subCategoryId);
        }, 250);
    };

    const selectVariant = (index, variantId) => {
        const variant = (productVariants[index] || []).find(row => String(row.id) === String(variantId));
        setItems(current => current.map((item, itemIndex) => {
            if (itemIndex !== index) return item;
            if (!variant) return { ...item, variantId: "" };
            return {
                ...item,
                variantId: String(variant.id),
                unitPrice: variant.purchasePrice ?? item.unitPrice,
                sku: variant.variantSku || item.sku,
                barcode: variant.barcode || item.barcode,
                attributes: { ...(item.attributes || {}), ...(variant.attributes || {}) }
            };
        }));
        setError("");
    };

    const updateAttribute = (index, key, value) => {
        setItems(current => current.map((item, itemIndex) => itemIndex === index ? {
            ...item,
            attributes: { ...(item.attributes || {}), [key]: value }
        } : item));
    };

    /* =====================================================
       GST
    ===================================================== */

    const changeGstRate = (
        index,
        value
    ) => {
        const gst = number(value);

        setItems(current =>
            current.map(
                (item, itemIndex) =>
                    itemIndex === index
                        ? {
                              ...item,
                              gstRate: gst,
                              cgstRate:
                                  gst / 2,
                              sgstRate:
                                  gst / 2
                          }
                        : item
            )
        );
    };

    

    /* =====================================================
       IMEI
    ===================================================== */

    const syncImeiRows = (
        index,
        quantity,
        required
    ) => {
        const safeQuantity = Math.max(
            1,
            Number(quantity) || 1
        );

        setItems(current =>
            current.map(
                (item, itemIndex) => {
                    if (
                        itemIndex !== index
                    ) {
                        return item;
                    }

                    const serials = required
                        ? Array.from(
                              {
                                  length: safeQuantity
                              },
                              (_, serialIndex) =>
                                  item.serials?.[
                                      serialIndex
                                   ] || {
                                       imei1: "",
                                       imei2: "",
                                       serialNumber: ""
                                   }
                          )
                        : [];

                    return {
                        ...item,
                        quantity,
                        imeiTrackingRequired:
                            required,
                        serials
                    };
                }
            )
        );
    };

    const updateImei = (
        itemIndex,
        serialIndex,
        field,
        value
    ) => {
        setItems(current =>
            current.map(
                (item, index) =>
                    index === itemIndex
                        ? {
                              ...item,
                              serials:
                                  item.serials.map(
                                      (
                                          serial,
                                          serialPosition
                                      ) =>
                                          serialPosition ===
                                          serialIndex
                                              ? {
                                                    ...serial,
                                                    [field]:
                                                        value
                                                }
                                              : serial
                                  )
                          }
                        : item
            )
        );
    };

    const scanImei = async index => {
        const code = normalizeText(imeiScanValues[index]);
        const item = items[index];
        if (!item?.isMobile || !item.productId) return;
        if (!isValidImei(code)) {
            setError("Scan or enter an exact 15-digit IMEI number.");
            return;
        }
        const duplicate = items.some(entry => (entry.serials || []).some(serial =>
            [serial.imei1, serial.imei2].map(normalizeText).includes(code)));
        if (duplicate) {
            setError(`Duplicate IMEI detected: ${code}.`);
            return;
        }
        const nextSerials = Array.from({ length: Math.max(1, number(item.quantity)) }, (_, serialIndex) =>
            item.serials?.[serialIndex] || { imei1: "", imei2: "", serialNumber: "" });
        const emptyIndex = nextSerials.findIndex(serial => !normalizeText(serial.imei1));
        if (emptyIndex < 0) {
            setError("All IMEI rows are already filled for this item.");
            return;
        }
        setImeiScanLoading(current => ({ ...current, [index]: true }));
        setError("");
        try {
            const result = await validatePurchaseImei(Number(item.productId), code);
            nextSerials[emptyIndex] = { ...nextSerials[emptyIndex], imei1: result.code };
            setItems(current => current.map((entry, itemIndex) => itemIndex === index ? {
                ...entry, imeiTrackingRequired: true, serials: nextSerials
            } : entry));
            setImeiScanValues(current => ({ ...current, [index]: "" }));
            window.requestAnimationFrame(() => scannerInputs.current[index]?.focus());
        } catch (requestError) {
            setError(errorMessage(requestError, "IMEI could not be added."));
        } finally {
            setImeiScanLoading(current => ({ ...current, [index]: false }));
        }
    };

    /* =====================================================
       VIEW PURCHASE
    ===================================================== */

    const viewPurchase = async purchase => {
        const requestId = ++detailRequestId.current;

        setSelectedPurchase(purchase);
        setPurchaseItems([]);
        setPayments([]);
        setPaymentSummary(null);
        setError("");

        try {
            const [
                itemData,
                summaryData,
                paymentData
            ] = await Promise.all([
                getPurchaseItems(
                    purchase.id
                ),
                getPurchasePaymentSummary(
                    purchase.id
                ),
                getPurchasePayments(
                    purchase.id
                )
            ]);

            if (requestId !== detailRequestId.current) return;

            setPurchaseItems(
                Array.isArray(itemData)
                    ? itemData
                    : []
            );

            setPaymentSummary(summaryData);

            setPayments(
                Array.isArray(paymentData)
                    ? paymentData
                    : []
            );
        } catch (requestError) {
            setError(
                errorMessage(
                    requestError,
                    "Purchase details could not be loaded."
                )
            );
        }
    };

    /* =====================================================
       PAYMENT
    ===================================================== */

    const recordPayment = async event => {
        event.preventDefault();
        setMessage("");
        setError("");

        if (!selectedPurchase || paymentSaving) {
            return;
        }

        if (String(selectedPurchase.status).toUpperCase() === "CANCELLED") {
            setError("Payments cannot be recorded for a cancelled purchase.");
            return;
        }

        const amount = number(
            payment.amount
        );

        const due = number(
            paymentSummary?.remainingAmount
        );

        if (amount <= 0) {
            setError(
                "Enter a valid payment amount."
            );
            return;
        }

        if (due <= 0) {
            setError("This purchase has no outstanding balance.");
            return;
        }

        if (amount > due) {
            setError(
                `Payment cannot exceed the outstanding amount of ${money(
                    due
                )}.`
            );
            return;
        }

        setPaymentSaving(true);

        try {
            await addPurchasePayment({
                purchaseId:
                    selectedPurchase.id,
                amount,
                paymentMethod:
                    payment.paymentMethod,
                transactionReference:
                    payment.transactionReference,
                notes: payment.notes
            });

            setMessage(
                "Payment recorded successfully."
            );

            setPayment({
                amount: "",
                paymentMethod: "UPI",
                transactionReference: "",
                notes: ""
            });

            await viewPurchase(
                selectedPurchase
            );
        } catch (requestError) {
            setError(
                errorMessage(
                    requestError,
                    "Payment could not be recorded."
                )
            );
        } finally {
            setPaymentSaving(false);
        }
    };

    /* =====================================================
       VALIDATION
    ===================================================== */

    const validatePurchase = () => {
        if (!form.sellerDistributorId) {
            return "Select a distributor.";
        }

        if (!normalizeText(form.invoiceNumber)) {
            return "Enter the distributor invoice number.";
        }

        if (items.length === 0) {
            return "Add at least one purchase item.";
        }

        if (items.some(item => {
            const quantity = number(item.quantity);
            const rate = number(item.unitPrice);
            return !item.productId || quantity <= 0 || rate <= 0;
        })) {
            return (
                "Every purchase item needs a product, positive quantity " +
                "and purchase rate."
            );
        }

        if (items.some(item => number(item.quantity) % 1 !== 0)) {
            return "Quantity must be a whole number for stock receipt items.";
        }

        for (let index = 0; index < items.length; index += 1) {
            const item = items[index];
            if ((productVariants[index] || []).length > 0 && !item.variantId) {
                return `Select an exact variant for ${item.productName || "this product"}.`;
            }
            if (item.isMobile) continue;
            const missing = (attributeTemplates[index] || []).find(template =>
                template.requiredField && !normalizeText(item.attributes?.[template.specificationKey]));
            if (missing) return `${missing.displayLabel} is required for ${item.productName || "this product"}.`;
        }

        if (items.some(item => {
            const discount = number(item.discount);
            const gross = number(item.quantity) * number(item.unitPrice);
            return discount < 0 || discount > gross;
        })) {
            return "Item discount must be between zero and the item value.";
        }

        if (items.some(item => {
            const images = item.images || [];
            return images.length > 0 && (images.length < 3 || images.length > 10);
        })) {
            return "Product images must contain between 3 and 10 images when provided.";
        }

        if (items.some(item =>
            (item.images || []).some(file => !String(file.type || "").startsWith("image/"))
        )) {
            return "Only image files can be uploaded as product images.";
        }

        if (items.some(item =>
            (item.images || []).some(file => file.size > MAX_UPLOAD_SIZE)
        )) {
            return "Each product image must be 10 MB or smaller.";
        }

        if (form.invoice && form.invoice.size > MAX_UPLOAD_SIZE) {
            return "The invoice attachment must be 10 MB or smaller.";
        }

        if (form.invoice && !String(form.invoice.type || "").match(/^(application\/pdf|image\/)/)) {
            return "Invoice attachment must be a PDF or image file.";
        }

        if (billDiscount > totals.taxable) {
            return "Bill discount cannot exceed the taxable subtotal.";
        }

        if (number(form.roundOff) < -100 || number(form.roundOff) > 100) {
            return "Round-off must be between -100 and 100.";
        }

        const seenImeis = new Set();

        for (const item of items) {
            if (!item.imeiTrackingRequired) continue;

            const quantity = number(item.quantity);
            if (item.serials.length !== quantity) {
                return "IMEI rows must match the quantity for every tracked item.";
            }

            for (const serial of item.serials) {
                const imei1 = normalizeText(serial.imei1);
                const imei2 = normalizeText(serial.imei2);

                if (!isValidImei(imei1)) {
                    return "IMEI 1 must contain exactly 15 digits for every tracked unit.";
                }

                if (imei2 && !isValidImei(imei2)) {
                    return "IMEI 2 must contain exactly 15 digits when provided.";
                }

                for (const imei of [imei1, imei2].filter(Boolean)) {
                    if (seenImeis.has(imei)) {
                        return `Duplicate IMEI detected: ${imei}. Each IMEI must be unique.`;
                    }
                    seenImeis.add(imei);
                }
            }
        }

        return "";
    };

    /* =====================================================
       SUBMIT
    ===================================================== */

    const submitPurchase = async event => {
        event.preventDefault();

        setMessage("");
        setError("");

        const validationError =
            validatePurchase();

        if (validationError) {
            setError(validationError);
            return;
        }

        setSaving(true);

        try {
            const purchaseData = { ...form };
            delete purchaseData.invoice;

            const saved =
                await createPurchase({
                    ...purchaseData,

                    sellerDistributorId:
                        Number(
                            form.sellerDistributorId
                        ),

                    purchaseDate: toDateTimeValue(
                        form.purchaseDate
                    ),

                    acknowledgementDate: toDateTimeValue(
                        form.acknowledgementDate
                    ),

                    invoiceNumber: normalizeText(
                        form.invoiceNumber
                    ),

                    roundOff: number(
                        form.roundOff
                    ),

                    discount:
                        billDiscount,

                    items: items.map(
                        ({
                            productId,
                            quantity,
                            unitPrice,
                            discount,
                            cgstRate,
                            sgstRate,
                            ...rest
                        }) => ({
                            ...Object.fromEntries(Object.entries(rest).filter(([key]) => !["categoryId", "subCategoryId", "isMobile", "gstRate", "images"].includes(key))),

                            productId:
                                Number(
                                    productId
                                ),

                            quantity:
                                Number(
                                    quantity
                                ),

                            unitPrice:
                                Number(
                                    unitPrice
                                ),

                            discount:
                                Number(
                                    discount || 0
                                ),

                            gstRate:
                                Number(
                                    cgstRate || 0
                                ) +
                                Number(
                                    sgstRate || 0
                                ),

                            cgstRate:
                                Number(
                                    cgstRate || 0
                                ),

                            sgstRate:
                                Number(
                                    sgstRate || 0
                                )
                        })
                    )
                });

            /* Invoice file */
            if (form.invoice) {
                await uploadPurchaseInvoice(
                    saved.id,
                    form.invoice
                );
            }

            /* Product images */
            let imageWarning = "";

            try {
                const savedItems =
                    await getPurchaseItems(
                        saved.id
                    );

                await Promise.all(
                    items.map(
                        (item, index) => {
                            if (
                                !item.images
                                    ?.length
                            ) {
                                return Promise.resolve();
                            }

                            const productId =
                                savedItems[
                                    index
                                ]?.product
                                    ?.id ||
                                savedItems[
                                    index
                                ]?.productId;

                            if (!productId) {
                                throw new Error(
                                    "Could not identify the purchased product for image upload."
                                );
                            }

                            return uploadPurchasedProductImages(
                                productId,
                                item.images
                            );
                        }
                    )
                );
            } catch (imageError) {
                imageWarning =
                    " Product images could not be uploaded.";
                console.error(
                    "Product image upload failed:",
                    imageError
                );
            }

            setMessage(
                `Purchase ${
                    saved.invoiceNumber
                } saved successfully. Stock updated.${imageWarning}`
            );

            setForm(initialForm());
            setItems([emptyItem()]);
            setProductSearches({});
            setActiveStep(1);

            await loadData();
        } catch (requestError) {
            setError(
                errorMessage(
                    requestError,
                    "Purchase could not be saved."
                )
            );
        } finally {
            setSaving(false);
        }
    };

    /* =====================================================
       CANCEL
    ===================================================== */

    const handleCancel = async purchase => {
        const confirmed =
            window.confirm(
                `Cancel invoice ${purchase.invoiceNumber}? Received stock will be reversed.`
            );

        if (!confirmed) {
            return;
        }

        try {
            await cancelPurchase(
                purchase.id
            );

            if (selectedPurchase?.id === purchase.id) {
                setSelectedPurchase(null);
                setPurchaseItems([]);
                setPayments([]);
                setPaymentSummary(null);
            }

            setMessage(
                `Invoice ${purchase.invoiceNumber} was cancelled and stock reversed.`
            );

            await loadData();
        } catch (requestError) {
            setError(
                errorMessage(
                    requestError,
                    "Purchase could not be cancelled."
                )
            );
        }
    };

    /* =====================================================
       RENDER
    ===================================================== */

    return (
        <main className="purchase-page">

            {/* =================================================
                HERO
            ================================================= */}

            <header className="purchase-hero">

                <div className="hero-content">

                    <div className="hero-badge">
                        <span />
                        INVENTORY COMMAND CENTER
                    </div>

                    <h1>
                        Stock
                        <span> purchases.</span>
                    </h1>

                    <p>
                        Capture distributor invoices,
                        receive stock, track IMEI numbers
                        and reconcile payments from one
                        workspace.
                    </p>

                    <div className="hero-actions">

                        <button
                            type="button"
                            onClick={() =>
                                navigate(
                                    "/seller/add-product"
                                )
                            }
                        >
                            + New product
                        </button>

                        <span>
                            {selectedProductCount}/
                            {productCount} products
                            configured
                        </span>

                    </div>

                </div>

                <div className="hero-dashboard">

                    <div className="hero-main-stat">
                        <small>
                            PURCHASE ORDERS
                        </small>

                        <strong>
                            {purchases.length}
                        </strong>

                        <span>
                            Recorded invoices
                        </span>
                    </div>

                    <div className="hero-mini-stat">
                        <strong>
                            {totals.quantity}
                        </strong>

                        <span>
                            Units this bill
                        </span>
                    </div>

                </div>

            </header>

            {/* =================================================
                ALERTS
            ================================================= */}

            {error && (
                <div
                    className="purchase-alert error"
                    role="alert"
                >
                    <span>!</span>
                    {error}
                </div>
            )}

            {message && (
                <div className="purchase-alert success">
                    <span>✓</span>
                    {message}
                </div>
            )}

            {/* =================================================
                PROGRESS
            ================================================= */}

            <nav className="purchase-steps">

                {[
                    ["01", "Invoice", 1],
                    ["02", "Products", 2],
                    ["03", "Tax & details", 3],
                    ["04", "Review", 4]
                ].map(
                    ([numberLabel, label, step]) => (
                        <button
                            key={step}
                            type="button"
                            className={
                                activeStep === step
                                    ? "active"
                                    : activeStep > step
                                    ? "completed"
                                    : ""
                            }
                            onClick={() =>
                                setActiveStep(
                                    step
                                )
                            }
                        >
                            <b>
                                {activeStep >
                                step
                                    ? "✓"
                                    : numberLabel}
                            </b>

                            <span>
                                {label}
                            </span>
                        </button>
                    )
                )}

            </nav>

            {/* =================================================
                MAIN
            ================================================= */}

            <section className="purchase-layout">

                <form
                    className="purchase-form"
                    onSubmit={
                        submitPurchase
                    }
                >

                    {/* =========================================
                        STEP 1 — INVOICE
                    ========================================= */}

                    <section
                        className={`purchase-section ${
                            activeStep === 1
                                ? "step-active"
                                : "step-hidden"
                        }`}
                    >

                        <div className="section-heading">

                            <div>
                                <p className="eyebrow">
                                    STEP 01
                                </p>

                                <h2>
                                    Distributor invoice
                                </h2>

                                <p className="section-description">
                                    Start with the source
                                    invoice and distributor.
                                </p>
                            </div>

                            <span className="section-number">
                                01
                            </span>

                        </div>

                        <div className="form-grid">

                            <label>
                                Distributor

                                <select
                                    required
                                    value={
                                        form.sellerDistributorId
                                    }
                                    onChange={event =>
                                        updateForm(
                                            "sellerDistributorId",
                                            event.target.value
                                        )
                                    }
                                >
                                    <option value="">
                                        Select distributor
                                    </option>

                                    {distributors.map(
                                        distributor => (
                                            <option
                                                key={
                                                    distributor.id
                                                }
                                                value={
                                                    distributor.id
                                                }
                                            >
                                                {
                                                    distributor.distributorName
                                                }
                                                {" · "}
                                                {
                                                    distributor.brand
                                                }
                                            </option>
                                        )
                                    )}
                                </select>
                            </label>

                            <label>
                                Invoice number

                                <input
                                    required
                                    value={
                                        form.invoiceNumber
                                    }
                                    onChange={event =>
                                        updateForm(
                                            "invoiceNumber",
                                            event.target.value
                                        )
                                    }
                                    placeholder="DIST-2026-0042"
                                />
                            </label>

                            <label>
                                Purchase date

                                <input
                                    type="datetime-local"
                                    value={
                                        form.purchaseDate
                                    }
                                    onChange={event =>
                                        updateForm(
                                            "purchaseDate",
                                            event.target.value
                                        )
                                    }
                                />
                            </label>

                            <label className="file-drop-zone">

                                <span>
                                    Invoice attachment
                                </span>

                                <input
                                    type="file"
                                    accept=".pdf,image/*"
                                    onChange={event =>
                                        updateForm(
                                            "invoice",
                                            event.target
                                                .files?.[0] ||
                                                null
                                        )
                                    }
                                />

                                <strong>
                                    {form.invoice
                                        ? form.invoice
                                              .name
                                        : "Choose PDF or image"}
                                </strong>

                                <small>
                                    Distributor tax
                                    invoice
                                </small>

                            </label>

                        </div>

                        {selectedDistributor && (
                            <div className="distributor-preview">

                                <div className="distributor-avatar">
                                    {String(
                                        selectedDistributor
                                            .distributorName ||
                                            "D"
                                    )
                                        .charAt(0)
                                        .toUpperCase()}
                                </div>

                                <div>
                                    <strong>
                                        {
                                            selectedDistributor.distributorName
                                        }
                                    </strong>

                                    <span>
                                        {
                                            selectedDistributor.brand
                                        }
                                    </span>
                                </div>

                                <small>
                                    Active distributor
                                </small>

                            </div>
                        )}

                        <details className="invoice-details">

                            <summary>
                                Advanced tax invoice /
                                e-invoice information
                            </summary>

                            <div className="form-grid">

                                <label>
                                    IRN
                                    <input
                                        value={
                                            form.irn
                                        }
                                        onChange={event =>
                                            updateForm(
                                                "irn",
                                                event.target.value
                                            )
                                        }
                                    />
                                </label>

                                <label>
                                    Acknowledgement no.
                                    <input
                                        value={
                                            form.acknowledgementNumber
                                        }
                                        onChange={event =>
                                            updateForm(
                                                "acknowledgementNumber",
                                                event.target.value
                                            )
                                        }
                                    />
                                </label>

                                <label>
                                    Acknowledgement date
                                    <input
                                        type="datetime-local"
                                        value={
                                            form.acknowledgementDate
                                        }
                                        onChange={event =>
                                            updateForm(
                                                "acknowledgementDate",
                                                event.target.value
                                            )
                                        }
                                    />
                                </label>

                                <label>
                                    E-way bill number
                                    <input
                                        value={
                                            form.ewayBillNumber
                                        }
                                        onChange={event =>
                                            updateForm(
                                                "ewayBillNumber",
                                                event.target.value
                                            )
                                        }
                                    />
                                </label>

                                <label>
                                    Delivery note
                                    <input
                                        value={
                                            form.deliveryNote
                                        }
                                        onChange={event =>
                                            updateForm(
                                                "deliveryNote",
                                                event.target.value
                                            )
                                        }
                                    />
                                </label>

                                <label>
                                    Reference number
                                    <input
                                        value={
                                            form.referenceNumber
                                        }
                                        onChange={event =>
                                            updateForm(
                                                "referenceNumber",
                                                event.target.value
                                            )
                                        }
                                    />
                                </label>

                                <label>
                                    Buyer order number
                                    <input
                                        value={
                                            form.buyerOrderNumber
                                        }
                                        onChange={event =>
                                            updateForm(
                                                "buyerOrderNumber",
                                                event.target.value
                                            )
                                        }
                                    />
                                </label>

                                <label>
                                    Dispatch document
                                    <input
                                        value={
                                            form.dispatchDocumentNumber
                                        }
                                        onChange={event =>
                                            updateForm(
                                                "dispatchDocumentNumber",
                                                event.target.value
                                            )
                                        }
                                    />
                                </label>

                                <label>
                                    Dispatched through
                                    <input
                                        value={
                                            form.dispatchedThrough
                                        }
                                        onChange={event =>
                                            updateForm(
                                                "dispatchedThrough",
                                                event.target.value
                                            )
                                        }
                                    />
                                </label>

                                <label>
                                    Destination
                                    <input
                                        value={
                                            form.destination
                                        }
                                        onChange={event =>
                                            updateForm(
                                                "destination",
                                                event.target.value
                                            )
                                        }
                                    />
                                </label>

                                <label>
                                    Terms of delivery
                                    <input
                                        value={
                                            form.termsOfDelivery
                                        }
                                        onChange={event =>
                                            updateForm(
                                                "termsOfDelivery",
                                                event.target.value
                                            )
                                        }
                                    />
                                </label>

                            </div>

                        </details>

                    </section>

                    {/* =========================================
                        STEP 2 — PRODUCTS
                    ========================================= */}

                    <section
                        className={`purchase-section product-section ${
                            activeStep === 2
                                ? "step-active"
                                : "step-hidden"
                        }`}
                    >

                        <div className="section-heading">

                            <div>
                                <p className="eyebrow">
                                    STEP 02
                                </p>

                                <h2>
                                    Build your stock receipt
                                </h2>

                                <p className="section-description">
                                    Select category,
                                    subcategory and
                                    product for every
                                    received line.
                                </p>
                            </div>

                            <button
                                type="button"
                                className="new-product-button"
                                onClick={() =>
                                    navigate(
                                        "/seller/add-product"
                                    )
                                }
                            >
                                + Add product
                            </button>

                        </div>

                        {items.map(
                            (item, index) => (
                                <div
                                    className="stock-item-card"
                                    key={index}
                                >

                                    <div className="stock-item-header">

                                        <div>
                                            <span>
                                                ITEM{" "}
                                                {String(
                                                    index +
                                                        1
                                                ).padStart(
                                                    2,
                                                    "0"
                                                )}
                                            </span>

                                            <strong>
                                                {item.productName ||
                                                    "New stock line"}
                                            </strong>
                                        </div>

                                        {items.length >
                                            1 && (
                                            <button
                                                type="button"
                                                className="remove-item"
                                                onClick={() =>
                                                    removeItem(
                                                        index
                                                    )
                                                }
                                            >
                                                Remove
                                            </button>
                                        )}

                                    </div>

                                    <div className="bill-line-title-row">

                                        <label>
                                            Description of goods / Mobile name

                                            <input
                                                value={
                                                    item.productName
                                                }
                                                onChange={event =>
                                                    updateItem(
                                                        index,
                                                        "productName",
                                                        event
                                                            .target
                                                            .value
                                                    )
                                                }
                                                placeholder="realme 16T 5G Starlight Black (6+128GB)"
                                            />
                                        </label>

                                        <label>
                                            Brand

                                            <input
                                                value={
                                                    item.brand
                                                }
                                                onChange={event =>
                                                    updateItem(
                                                        index,
                                                        "brand",
                                                        event
                                                            .target
                                                            .value
                                                    )
                                                }
                                                placeholder="realme"
                                            />
                                        </label>

                                    </div>

                                    <div className="category-select-row">

                                        <label>
                                            Category

                                            <select
                                                required
                                                value={
                                                    item.categoryId
                                                }
                                                onChange={event =>
                                                    void selectCategory(
                                                        index,
                                                        event
                                                            .target
                                                            .value
                                                    )
                                                }
                                            >
                                                <option value="">
                                                    Select category
                                                </option>

                                                {categories.map(
                                                    category => (
                                                        <option
                                                            key={
                                                                category.id
                                                            }
                                                            value={
                                                                category.id
                                                            }
                                                        >
                                                            {
                                                                category.name
                                                            }
                                                        </option>
                                                    )
                                                )}
                                            </select>
                                        </label>

                                        <label>
                                            Subcategory

                                            <select
                                                disabled={
                                                    !item.categoryId
                                                }
                                                value={
                                                    item.subCategoryId
                                                }
                                                onChange={event =>
                                                    selectSubCategory(
                                                        index,
                                                        event
                                                            .target
                                                            .value
                                                    )
                                                }
                                            >
                                                <option value="">
                                                    All subcategories
                                                </option>

                                                {subcategoriesFor(item.categoryId).map(
                                                    subcategory => (
                                                        <option
                                                            key={
                                                                subcategory.id
                                                            }
                                                            value={
                                                                subcategory.id
                                                            }
                                                        >
                                                            {
                                                                subcategory.name
                                                            }
                                                        </option>
                                                    )
                                                )}
                                            </select>
                                        </label>

                                    </div>

                                    {(
                                        <>

                                            <div className="catalogue-toolbar">

                                                <div>
                                                    <strong>
                                                        Product search
                                                    </strong>

                                                    <span>
                                                        {
                                                            productsForItem(
                                                                item,
                                                                index
                                                            ).length
                                                        }{" "}
                                                        result(s)
                                                    </span>
                                                </div>

                                                <input
                                                    value={
                                                        productSearches[index] || ""
                                                    }
                                                    onChange={event =>
                                                        updateProductSearch(index, event.target.value)
                                                    }
                                                    onKeyDown={event => {
                                                        if (event.key !== "Enter") return;
                                                        event.preventDefault();
                                                        window.clearTimeout(searchTimers.current[index]);
                                                        const code = normalizeText(event.currentTarget.value);
                                                        void searchCatalogue(index, code, item.categoryId, item.subCategoryId).then(result => {
                                                            if (result?.content?.length === 1) selectProduct(index, result.content[0]);
                                                        });
                                                    }}
                                                    placeholder="Search name, SKU, barcode, model or category..."
                                                />

                                            </div>

                                            <div className="available-product-grid">

                                                {productsForItem(
                                                    item,
                                                    index
                                                ).map(
                                                    product => {
                                                        const selected =
                                                            String(
                                                                item.productId
                                                            ) ===
                                                            String(
                                                                product.id
                                                            );

                                                        return (
                                                            <button
                                                                type="button"
                                                                key={
                                                                    product.id
                                                                }
                                                                className={`available-product-card ${
                                                                    selected
                                                                        ? "selected"
                                                                        : ""
                                                                }`}
                                                                onClick={() =>
                                                                    selectProduct(
                                                                        index,
                                                                        product
                                                                    )
                                                                }
                                                            >

                                                                <div className="product-card-top">

                                                                    <span className="product-icon">
                                                                        {String(
                                                                            product.brand ||
                                                                                product.name ||
                                                                                "P"
                                                                        )
                                                                            .charAt(
                                                                                0
                                                                            )
                                                                            .toUpperCase()}
                                                                    </span>

                                                                    {selected && (
                                                                        <b className="selected-check">
                                                                            ✓
                                                                        </b>
                                                                    )}

                                                                </div>

                                                                <strong>
                                                                    {
                                                                        product.name
                                                                    }
                                                                </strong>

                                                                <span>
                                                                    {product.brand ||
                                                                        "Generic product"}
                                                                </span>

                                                                <small>
                                                                    Stock{" "}
                                                                    {product.stock ??
                                                                        0}
                                                                    {" · "}
                                                                    {money(
                                                                        product.purchasePrice ||
                                                                            product.price
                                                                    )}
                                                                </small>

                                                            </button>
                                                        );
                                                    }
                                                )}

                                            </div>

                                            {catalogueLoading[index] && <p className="purchase-search-state">Searching catalogue…</p>}
                                            {catalogueErrors[index] && <p className="purchase-search-state error">{catalogueErrors[index]}</p>}

                                            {(catalogueResults[index]?.totalPages || 0) > 1 && (
                                                <div className="purchase-search-pagination">
                                                    <button type="button" disabled={!catalogueResults[index]?.number || catalogueLoading[index]}
                                                        onClick={() => void searchCatalogue(index, productSearches[index] || "", item.categoryId, item.subCategoryId, catalogueResults[index].number - 1)}>Previous</button>
                                                    <span>Page {(catalogueResults[index]?.number || 0) + 1} of {catalogueResults[index]?.totalPages}</span>
                                                    <button type="button" disabled={catalogueLoading[index] || (catalogueResults[index]?.number || 0) + 1 >= catalogueResults[index]?.totalPages}
                                                        onClick={() => void searchCatalogue(index, productSearches[index] || "", item.categoryId, item.subCategoryId, catalogueResults[index].number + 1)}>Next</button>
                                                </div>
                                            )}

                                            {!catalogueLoading[index] && productsForItem(
                                                item,
                                                index
                                            ).length ===
                                                0 && (
                                                <div className="no-products">
                                                    <strong>
                                                        No matching
                                                        products
                                                    </strong>

                                                    <span>
                                                        Add the product
                                                        first and it will
                                                        appear here.
                                                    </span>

                                                    <button
                                                        type="button"
                                                        onClick={() =>
                                                            navigate(
                                                                "/seller/add-product"
                                                            )
                                                        }
                                                    >
                                                        Add new product
                                                    </button>
                                                </div>
                                            )}

                                        </>
                                    )}

                                    {(item.productId || (!item.isMobile && item.subCategoryId && (attributeTemplates[index] || []).length > 0)) && (
                                        <div className="purchase-invoice-line">

                                            <div className="invoice-line-head">
                                                <strong>
                                                    {item.productId ? "Invoice line details" : "Purchase details"}
                                                </strong>

                                                <span>
                                                    {item.productId
                                                        ? "Same fields as distributor tax invoice"
                                                        : "Choose a catalogue product before saving this stock item"}
                                                </span>
                                            </div>

                                            <div className="invoice-line-grid">

                                                {productVariants[index]?.length > 0 && (
                                                    <label className="purchase-variant-select">
                                                        Product variant
                                                        <select required value={item.variantId || ""} onChange={event => selectVariant(index, event.target.value)}>
                                                            <option value="">Select exact variant</option>
                                                            {productVariants[index].map(variant => (
                                                                <option key={variant.id} value={variant.id} disabled={!variant.active || variant.availableStock < 0}>
                                                                    {Object.entries(variant.attributes || {}).map(([key, value]) => `${key}: ${value}`).join(" · ")} · SKU {variant.variantSku || "—"} · Stock {variant.availableStock}
                                                                </option>
                                                            ))}
                                                        </select>
                                                        {variantLoading[index] && <small>Loading variants…</small>}
                                                    </label>
                                                )}

                                                <label>
                                                    {item.isMobile ? "Mobile name" : "Product name"}

                                                    <input
                                                        value={
                                                            item.productName
                                                        }
                                                        onChange={event =>
                                                            updateItem(
                                                                index,
                                                                "productName",
                                                                event
                                                                    .target
                                                                    .value
                                                            )
                                                        }
                                                        placeholder={item.isMobile ? "realme 16T 5G" : "Product name"}
                                                    />
                                                </label>

                                                {item.isMobile && <>
                                                <label className="inline-imei-field">
                                                    IMEI / Serial

                                                    <button
                                                        type="button"
                                                        className={`inline-imei-toggle ${
                                                            item.imeiTrackingRequired
                                                                ? "active"
                                                                : ""
                                                        }`}
                                                        onClick={() =>
                                                            syncImeiRows(
                                                                index,
                                                                item.quantity,
                                                                !item.imeiTrackingRequired
                                                            )
                                                        }
                                                    >
                                                        {item.imeiTrackingRequired
                                                            ? "IMEI tracking on"
                                                            : "Enable Track IMEI"}
                                                    </button>
                                                </label>

                                                <label>
                                                    Color

                                                    <input
                                                        value={
                                                            item.color
                                                        }
                                                        onChange={event =>
                                                            updateItem(
                                                                index,
                                                                "color",
                                                                event
                                                                    .target
                                                                    .value
                                                            )
                                                        }
                                                        placeholder="Starlight Black"
                                                    />
                                                </label>

                                                <label>
                                                    RAM

                                                    <input
                                                        value={
                                                            item.ram
                                                        }
                                                        onChange={event =>
                                                            updateItem(
                                                                index,
                                                                "ram",
                                                                event
                                                                    .target
                                                                    .value
                                                            )
                                                        }
                                                        placeholder="6 GB"
                                                    />
                                                </label>

                                                <label>
                                                    Storage

                                                    <input
                                                        value={
                                                            item.storage
                                                        }
                                                        onChange={event =>
                                                            updateItem(
                                                                index,
                                                                "storage",
                                                                event
                                                                    .target
                                                                    .value
                                                            )
                                                        }
                                                        placeholder="128 GB"
                                                    />
                                                </label>
                                                </>}

                                                <label>
                                                    HSN/SAC

                                                    <input
                                                        value={
                                                            item.hsnCode
                                                        }
                                                        onChange={event =>
                                                            updateItem(
                                                                index,
                                                                "hsnCode",
                                                                event
                                                                    .target
                                                                    .value
                                                            )
                                                        }
                                                        placeholder="85171300"
                                                    />
                                                </label>

                                                <label>
                                                    Supplier SKU / product code
                                                    <input value={item.sku || ""} onChange={event => updateItem(index, "sku", event.target.value)} placeholder="Supplier SKU or part number" />
                                                </label>

                                                <label>
                                                    Barcode / EAN
                                                    <input value={item.barcode || ""} onChange={event => updateItem(index, "barcode", event.target.value)}
                                                        onKeyDown={event => { if (event.key === "Enter") event.preventDefault(); }}
                                                        placeholder="Scan or type barcode (optional)" />
                                                </label>

                                                <label>
                                                    Quantity

                                                    <input
                                                        type="number"
                                                        min="1"
                                                        value={
                                                            item.quantity
                                                        }
                                                        onChange={event =>
                                                            syncImeiRows(
                                                                index,
                                                                event
                                                                    .target
                                                                    .value,
                                                                item.imeiTrackingRequired
                                                            )
                                                        }
                                                    />
                                                </label>

                                                <label>
                                                    Rate

                                                    <input
                                                        type="number"
                                                        min="0.01"
                                                        step="0.01"
                                                        value={
                                                            item.unitPrice
                                                        }
                                                        onChange={event =>
                                                            updateItem(
                                                                index,
                                                                "unitPrice",
                                                                event
                                                                    .target
                                                                    .value
                                                            )
                                                        }
                                                        placeholder="Without GST"
                                                    />
                                                </label>

                                                <label>
                                                    Per

                                                    <select
                                                        value={
                                                            item.unit
                                                        }
                                                        onChange={event =>
                                                            updateItem(
                                                                index,
                                                                "unit",
                                                                event
                                                                    .target
                                                                    .value
                                                            )
                                                        }
                                                    >
                                                        <option value="NOS">
                                                            Nos
                                                        </option>
                                                        <option value="PCS">
                                                            Pcs
                                                        </option>
                                                        <option value="BOX">
                                                            Box
                                                        </option>
                                                    </select>
                                                </label>

                                                <label>
                                                    Amount

                                                    <input
                                                        readOnly
                                                        value={money(
                                                            Math.max(
                                                                0,
                                                                number(item.quantity) *
                                                                    number(item.unitPrice) -
                                                                    Math.min(
                                                                        number(item.quantity) *
                                                                            number(item.unitPrice),
                                                                        number(item.discount)
                                                                    )
                                                            )
                                                        )}
                                                    />
                                                </label>

                                            </div>

                                            {!item.isMobile && (attributeTemplates[index] || []).length > 0 && (
                                                <div className="purchase-attribute-panel">
                                                    <div className="invoice-line-head">
                                                        <strong>Product specifications</strong>
                                                        <span>Fields configured for this subcategory</span>
                                                    </div>
                                                    <div className="purchase-attribute-grid">
                                                        {(attributeTemplates[index] || []).map(template => {
                                                            const value = item.attributes?.[template.specificationKey] || "";
                                                            const options = parseTemplateOptions(template.optionsJson);
                                                            const label = <>{template.displayLabel}{template.requiredField ? " *" : ""}</>;
                                                            if (template.inputType === "TEXTAREA") return <label key={template.id}>{label}<textarea value={value} onChange={event => updateAttribute(index, template.specificationKey, event.target.value)} /></label>;
                                                            if (template.inputType === "BOOLEAN") return <label key={template.id}>{label}<select value={value} onChange={event => updateAttribute(index, template.specificationKey, event.target.value)}><option value="">Select</option><option value="true">Yes</option><option value="false">No</option></select></label>;
                                                            if (["SELECT", "MULTI_SELECT"].includes(template.inputType)) return <label key={template.id}>{label}<select multiple={template.inputType === "MULTI_SELECT"} value={template.inputType === "MULTI_SELECT" ? value.split(",").filter(Boolean) : value} onChange={event => updateAttribute(index, template.specificationKey, template.inputType === "MULTI_SELECT" ? Array.from(event.target.selectedOptions).map(option => option.value).join(",") : event.target.value)}><option value="">Select</option>{options.map(option => <option key={option} value={option}>{option}</option>)}</select></label>;
                                                            return <label key={template.id}>{label}<input type={template.inputType === "NUMBER" ? "number" : "text"} value={value} onChange={event => updateAttribute(index, template.specificationKey, event.target.value)} /></label>;
                                                        })}
                                                    </div>
                                                </div>
                                            )}

                                            {item.isMobile && (
                                                <div className="purchase-imei-scanner">
                                                    <strong>Scan or enter IMEI</strong>
                                                    <span>USB/Bluetooth scanners can type here; press Enter to add the exact IMEI.</span>
                                                    <div>
                                                        <input ref={element => { scannerInputs.current[index] = element; }} inputMode="numeric" value={imeiScanValues[index] || ""}
                                                            onChange={event => setImeiScanValues(current => ({ ...current, [index]: event.target.value }))}
                                                            onKeyDown={event => { if (event.key === "Enter") { event.preventDefault(); void scanImei(index); } }}
                                                            placeholder="Scan IMEI barcode or type IMEI" />
                                                        <button type="button" disabled={imeiScanLoading[index]} onClick={() => void scanImei(index)}>{imeiScanLoading[index] ? "Checking…" : "Add IMEI"}</button>
                                                    </div>
                                                </div>
                                            )}

                                            {item.isMobile && item.imeiTrackingRequired &&
                                                item.serials.length >
                                                    0 && (
                                                    <div className="inline-imei-list">

                                                        {item.serials.map(
                                                            (
                                                                serial,
                                                                serialIndex
                                                            ) => (
                                                                <div
                                                                    className="imei-entry"
                                                                    key={
                                                                        serialIndex
                                                                    }
                                                                >
                                                                    <b>
                                                                        UNIT{" "}
                                                                        {String(
                                                                            serialIndex +
                                                                                1
                                                                        ).padStart(
                                                                            2,
                                                                            "0"
                                                                        )}
                                                                    </b>

                                                                    <input
                                                                        required
                                                                        inputMode="numeric"
                                                                        value={
                                                                            serial.imei1
                                                                        }
                                                                        onChange={event =>
                                                                            updateImei(
                                                                                index,
                                                                                serialIndex,
                                                                                "imei1",
                                                                                event
                                                                                    .target
                                                                                    .value
                                                                            )
                                                                        }
                                                                        placeholder="IMEI 1 *"
                                                                    />

                                                                    <input
                                                                        inputMode="numeric"
                                                                        value={
                                                                            serial.imei2
                                                                        }
                                                                        onChange={event =>
                                                                            updateImei(
                                                                                index,
                                                                                serialIndex,
                                                                                "imei2",
                                                                                event
                                                                                    .target
                                                                                    .value
                                                                            )
                                                                        }
                                                                        placeholder="IMEI 2"
                                                                    />

                                                                    <input
                                                                        value={serial.serialNumber || ""}
                                                                        onChange={event => updateImei(index, serialIndex, "serialNumber", event.target.value)}
                                                                        placeholder="Serial number (optional)"
                                                                    />
                                                                </div>
                                                            )
                                                        )}

                                                    </div>
                                                )}

                                        </div>
                                    )}

                                    {item.isMobile && (
                                        <div className="received-product-grid">

                                            <label>
                                                Quantity

                                                <input
                                                    type="number"
                                                    min="1"
                                                    value={
                                                        item.quantity
                                                    }
                                                    onChange={event =>
                                                        syncImeiRows(
                                                            index,
                                                            event
                                                                .target
                                                                .value,
                                                            item.imeiTrackingRequired
                                                        )
                                                    }
                                                />
                                            </label>

                                            <label>
                                                Purchase rate

                                                <input
                                                    type="number"
                                                    min="0.01"
                                                    step="0.01"
                                                    value={
                                                        item.unitPrice
                                                    }
                                                    onChange={event =>
                                                        updateItem(
                                                            index,
                                                            "unitPrice",
                                                            event
                                                                .target
                                                                .value
                                                        )
                                                    }
                                                    placeholder="Without GST"
                                                />
                                            </label>

                                            <label>
                                                HSN

                                                <input
                                                    value={
                                                        item.hsnCode
                                                    }
                                                    onChange={event =>
                                                        updateItem(
                                                            index,
                                                            "hsnCode",
                                                            event
                                                                .target
                                                                .value
                                                        )
                                                    }
                                                />
                                            </label>

                                            <label>
                                                Colour

                                                <input
                                                    value={
                                                        item.color
                                                    }
                                                    onChange={event =>
                                                        updateItem(
                                                            index,
                                                            "color",
                                                            event
                                                                .target
                                                                .value
                                                        )
                                                    }
                                                    placeholder="Black"
                                                />
                                            </label>

                                            <label>
                                                RAM

                                                <input
                                                    value={
                                                        item.ram
                                                    }
                                                    onChange={event =>
                                                        updateItem(
                                                            index,
                                                            "ram",
                                                            event
                                                                .target
                                                                .value
                                                        )
                                                    }
                                                    placeholder="6 GB"
                                                />
                                            </label>

                                            <label>
                                                Storage

                                                <input
                                                    value={
                                                        item.storage
                                                    }
                                                    onChange={event =>
                                                        updateItem(
                                                            index,
                                                            "storage",
                                                            event
                                                                .target
                                                                .value
                                                        )
                                                    }
                                                    placeholder="128 GB"
                                                />
                                            </label>

                                            <label className="same-line-imei">
                                                IMEI / Serial

                                                <button
                                                    type="button"
                                                    className={`same-line-imei-toggle ${
                                                        item.imeiTrackingRequired
                                                            ? "active"
                                                            : ""
                                                    }`}
                                                    onClick={() =>
                                                        syncImeiRows(
                                                            index,
                                                            item.quantity,
                                                            !item.imeiTrackingRequired
                                                        )
                                                    }
                                                >
                                                    {item.imeiTrackingRequired
                                                        ? "Tracking on"
                                                        : "Track IMEI"}
                                                </button>
                                            </label>

                                        </div>
                                    )}

                                    {item.isMobile && (
                                        <div className="imei-panel">

                                            <label className="imei-toggle">

                                                <input
                                                    type="checkbox"
                                                    checked={
                                                        item.imeiTrackingRequired
                                                    }
                                                    onChange={event =>
                                                        syncImeiRows(
                                                            index,
                                                            item.quantity,
                                                            event
                                                                .target
                                                                .checked
                                                        )
                                                    }
                                                />

                                                <span>
                                                    <strong>
                                                        Track IMEI
                                                        numbers
                                                    </strong>

                                                    <small>
                                                        Required for
                                                        individually
                                                        serialized
                                                        mobile stock
                                                    </small>
                                                </span>

                                            </label>

                                            {item.imeiTrackingRequired &&
                                                item.serials.length >
                                                    0 && (
                                                    <div className="imei-entry-grid">

                                                        {item.serials.map(
                                                            (
                                                                serial,
                                                                serialIndex
                                                            ) => (
                                                                <div
                                                                    className="imei-entry"
                                                                    key={
                                                                        serialIndex
                                                                    }
                                                                >

                                                                    <b>
                                                                        UNIT{" "}
                                                                        {String(
                                                                            serialIndex +
                                                                                1
                                                                        ).padStart(
                                                                            2,
                                                                            "0"
                                                                        )}
                                                                    </b>

                                                                    <input
                                                                        required
                                                                        inputMode="numeric"
                                                                        value={
                                                                            serial.imei1
                                                                        }
                                                                        onChange={event =>
                                                                            updateImei(
                                                                                index,
                                                                                serialIndex,
                                                                                "imei1",
                                                                                event
                                                                                    .target
                                                                                    .value
                                                                            )
                                                                        }
                                                                        placeholder="IMEI 1 *"
                                                                    />

                                                                    <input
                                                                        inputMode="numeric"
                                                                        value={
                                                                            serial.imei2
                                                                        }
                                                                        onChange={event =>
                                                                            updateImei(
                                                                                index,
                                                                                serialIndex,
                                                                                "imei2",
                                                                                event
                                                                                    .target
                                                                                    .value
                                                                            )
                                                                        }
                                                                        placeholder="IMEI 2"
                                                                    />

                                                                </div>
                                                            )
                                                        )}

                                                    </div>
                                                )}

                                        </div>
                                    )}

                                    {item.isMobile && <div className="purchase-inline-details">

                                        <div className="inline-details-heading">
                                            <strong>
                                                Model details & attachment
                                            </strong>

                                            <span>
                                                Saved with this purchase line
                                            </span>
                                        </div>

                                        <div className="inline-details-grid">

                                            <label>
                                                Model

                                                <input
                                                    value={
                                                        item.model
                                                    }
                                                    onChange={event =>
                                                        updateItem(
                                                            index,
                                                            "model",
                                                            event
                                                                .target
                                                                .value
                                                        )
                                                    }
                                                    placeholder="16T 5G"
                                                />
                                            </label>

                                            <label>
                                                RAM

                                                <input
                                                    value={
                                                        item.ram
                                                    }
                                                    onChange={event =>
                                                        updateItem(
                                                            index,
                                                            "ram",
                                                            event
                                                                .target
                                                                .value
                                                        )
                                                    }
                                                    placeholder="6 GB"
                                                />
                                            </label>

                                            <label>
                                                Storage

                                                <input
                                                    value={
                                                        item.storage
                                                    }
                                                    onChange={event =>
                                                        updateItem(
                                                            index,
                                                            "storage",
                                                            event
                                                                .target
                                                                .value
                                                        )
                                                    }
                                                    placeholder="128 GB"
                                                />
                                            </label>

                                        </div>

                                    </div>}

                                </div>
                            )
                        )}

                        <button
                            type="button"
                            className="add-item-button"
                            onClick={addItem}
                        >
                            <span>+</span>
                            Add another stock item
                        </button>

                    </section>

                    {/* =========================================
                        STEP 3 — PRODUCT DETAILS / TAX
                    ========================================= */}

                    <section
                        className={`purchase-section ${
                            activeStep === 3
                                ? "step-active"
                                : "step-hidden"
                        }`}
                    >

                        <div className="section-heading">

                            <div>
                                <p className="eyebrow">
                                    STEP 03
                                </p>

                                <h2>
                                    Tax details
                                </h2>

                                <p className="section-description">
                                    Confirm invoice tax rates and totals.
                                </p>
                            </div>

                            <span className="section-number">
                                03
                            </span>

                        </div>

                        <div className="detail-cards">

                            {items.map(
                                (item, index) => (
                                    <div
                                        className="detail-card"
                                        key={index}
                                    >

                                        <div className="detail-card-title">

                                            <div className="product-icon">
                                                {String(
                                                    item.brand ||
                                                        item.productName ||
                                                        "P"
                                                )
                                                    .charAt(
                                                        0
                                                    )
                                                    .toUpperCase()}
                                            </div>

                                            <div>
                                                <strong>
                                                    {item.productName ||
                                                        `Item ${
                                                            index +
                                                            1
                                                        }`}
                                                </strong>

                                                <span>
                                                    {
                                                        item.brand
                                                    }
                                                </span>
                                            </div>

                                        </div>

                                        <div className="form-grid">

                                            <label>
                                                Brand *

                                                <input
                                                    required
                                                    value={
                                                        item.brand
                                                    }
                                                    onChange={event =>
                                                        updateItem(
                                                            index,
                                                            "brand",
                                                            event
                                                                .target
                                                                .value
                                                        )
                                                    }
                                                />
                                            </label>

                                            <label>
                                                Model

                                                <input
                                                    value={
                                                        item.model
                                                    }
                                                    onChange={event =>
                                                        updateItem(
                                                            index,
                                                            "model",
                                                            event
                                                                .target
                                                                .value
                                                        )
                                                    }
                                                />
                                            </label>

                                            {item.isMobile && <label>
                                                RAM

                                                <input
                                                    value={
                                                        item.ram
                                                    }
                                                    onChange={event =>
                                                        updateItem(
                                                            index,
                                                            "ram",
                                                            event
                                                                .target
                                                                .value
                                                        )
                                                    }
                                                    placeholder="8 GB"
                                                />
                                            </label>}

                                            {item.isMobile && <label>
                                                Storage

                                                <input
                                                    value={
                                                        item.storage
                                                    }
                                                    onChange={event =>
                                                        updateItem(
                                                            index,
                                                            "storage",
                                                            event
                                                                .target
                                                                .value
                                                        )
                                                    }
                                                    placeholder="128 GB"
                                                />
                                            </label>}

                                        </div>

                                        <label>
                                            Description

                                            <textarea
                                                rows="3"
                                                value={
                                                    item.description
                                                }
                                                onChange={event =>
                                                    updateItem(
                                                        index,
                                                        "description",
                                                        event
                                                            .target
                                                            .value
                                                    )
                                                }
                                                placeholder="Condition, specifications or other useful information..."
                                            />
                                        </label>

                                        <label className="image-upload-box">

                                            <span>
                                                Product images
                                            </span>

                                            <input
                                                type="file"
                                                accept="image/*"
                                                multiple
                                                onChange={event =>
                                                    updateItem(
                                                        index,
                                                        "images",
                                                        Array.from(
                                                            event
                                                                .target
                                                                .files ||
                                                                []
                                                        )
                                                    )
                                                }
                                            />

                                            <strong>
                                                {item.images
                                                    ?.length ||
                                                    0}{" "}
                                                images selected
                                            </strong>

                                            <small>
                                                Recommended:
                                                3–10 product
                                                images
                                            </small>

                                        </label>

                                    </div>
                                )
                            )}

                        </div>

                        <div className="tax-panel">

                            <div className="tax-panel-heading">
                                <div>
                                    <strong>
                                        GST configuration
                                    </strong>

                                    <span>
                                        Match the distributor
                                        invoice exactly.
                                    </span>
                                </div>
                            </div>

                            {items.map(
                                (item, index) => (
                                    <div
                                        className="tax-row"
                                        key={index}
                                    >

                                        <div>
                                            <strong>
                                                {item.productName ||
                                                    `Item ${
                                                        index +
                                                        1
                                                    }`}
                                            </strong>

                                            <small>
                                                Taxable{" "}
                                                {money(
                                                    Math.max(
                                                        0,
                                                        number(item.quantity) *
                                                            number(item.unitPrice) -
                                                            Math.min(
                                                                number(item.quantity) *
                                                                    number(item.unitPrice),
                                                                number(item.discount)
                                                            )
                                                    )
                                                )}
                                            </small>
                                        </div>

                                        <label>
                                            GST rate

                                            <select
                                                value={
                                                    item.gstRate
                                                }
                                                onChange={event =>
                                                    changeGstRate(
                                                        index,
                                                        event
                                                            .target
                                                            .value
                                                    )
                                                }
                                            >
                                                <option value="0">
                                                    0%
                                                </option>
                                                <option value="5">
                                                    5%
                                                </option>
                                                <option value="12">
                                                    12%
                                                </option>
                                                <option value="18">
                                                    18%
                                                </option>
                                                <option value="28">
                                                    28%
                                                </option>
                                            </select>
                                        </label>

                                        <label>
                                            CGST %

                                            <input
                                                type="number"
                                                min="0"
                                                max="100"
                                                step="0.01"
                                                value={
                                                    item.cgstRate
                                                }
                                                onChange={event =>
                                                    updateItem(
                                                        index,
                                                        "cgstRate",
                                                        event
                                                            .target
                                                            .value
                                                    )
                                                }
                                            />
                                        </label>

                                        <label>
                                            SGST %

                                            <input
                                                type="number"
                                                min="0"
                                                max="100"
                                                step="0.01"
                                                value={
                                                    item.sgstRate
                                                }
                                                onChange={event =>
                                                    updateItem(
                                                        index,
                                                        "sgstRate",
                                                        event
                                                            .target
                                                            .value
                                                    )
                                                }
                                            />
                                        </label>

                                        <strong className="tax-total">
                                            {money(
                                                Math.max(
                                                    0,
                                                    number(item.quantity) *
                                                        number(item.unitPrice) -
                                                        Math.min(
                                                            number(item.quantity) *
                                                                number(item.unitPrice),
                                                            number(item.discount)
                                                        )
                                                ) *
                                                    (number(item.cgstRate) +
                                                        number(item.sgstRate)) /
                                                    100
                                            )}
                                        </strong>

                                    </div>
                                )
                            )}

                        </div>

                        <label className="notes">
                            Purchase notes

                            <textarea
                                rows="3"
                                value={form.notes}
                                onChange={event =>
                                    updateForm(
                                        "notes",
                                        event.target.value
                                    )
                                }
                                placeholder="Optional notes about this purchase..."
                            />
                        </label>

                    </section>

                    {/* =========================================
                        FINAL SUMMARY
                    ========================================= */}

                    <section
                        className={`purchase-final-summary ${
                            activeStep === 4
                                ? "step-active"
                                : "step-hidden"
                        }`}
                    >

                        <div>
                            <span>
                                Ready to receive
                            </span>

                            <strong>
                                {totals.quantity} units
                            </strong>
                        </div>

                        <div>
                            <span>
                                Taxable
                            </span>

                            <strong>
                                {money(
                                    taxableAfterBillDiscount
                                )}
                            </strong>
                        </div>

                        <div>
                            <span>
                                GST
                            </span>

                            <strong>
                                {money(
                                    gstAfterBillDiscount
                                )}
                            </strong>
                        </div>

                        <div className="final-total">
                            <span>
                                Grand total
                            </span>

                            <strong>
                                {money(
                                    grandTotal
                                )}
                            </strong>
                        </div>

                        <label>
                            Bill discount

                            <input
                                type="number"
                                min="0"
                                step="0.01"
                                value={
                                    form.discount
                                }
                                onChange={event =>
                                    updateForm(
                                        "discount",
                                        event.target.value
                                    )
                                }
                            />
                        </label>

                        <label>
                            Round-off

                            <input
                                type="number"
                                step="0.01"
                                value={
                                    form.roundOff
                                }
                                onChange={event =>
                                    updateForm(
                                        "roundOff",
                                        event.target.value
                                    )
                                }
                                placeholder="0.00"
                            />
                        </label>

                    </section>

                    <button
                        type="submit"
                        className="save-purchase-button"
                        disabled={saving || loading}
                        aria-busy={saving}
                    >
                        <span>
                            {saving
                                ? "Saving purchase..."
                                : "Save purchase & receive stock"}
                        </span>

                        {!saving && (
                            <strong>
                                {money(
                                    grandTotal
                                )}
                            </strong>
                        )}
                    </button>

                </form>

                {/* =================================================
                    SIDEBAR
                ================================================= */}

                <aside className="purchase-history">

                    <div className="sidebar-summary">

                        <div className="sidebar-title">
                            <div>
                                <p className="eyebrow">
                                    LIVE SUMMARY
                                </p>

                                <h2>
                                    Current purchase
                                </h2>
                            </div>
                        </div>

                        <div className="summary-total">
                            <span>
                                Estimated total
                            </span>

                            <strong>
                                {money(
                                    grandTotal
                                )}
                            </strong>
                        </div>

                        <div className="summary-stats">

                            <div>
                                <strong>
                                    {items.length}
                                </strong>

                                <span>
                                    Lines
                                </span>
                            </div>

                            <div>
                                <strong>
                                    {totals.quantity}
                                </strong>

                                <span>
                                    Units
                                </span>
                            </div>

                            <div>
                                <strong>
                                    {Math.round(
                                        totals.gst
                                    )}
                                </strong>

                                <span>
                                    GST
                                </span>
                            </div>

                        </div>

                        <div className="summary-breakdown">

                            <p>
                                <span>
                                    Subtotal
                                </span>

                                <strong>
                                    {money(
                                        totals.subtotal
                                    )}
                                </strong>
                            </p>

                            <p>
                                <span>
                                    Item discount
                                </span>

                                <strong>
                                    -
                                    {money(
                                        totals.itemDiscount
                                    )}
                                </strong>
                            </p>

                            <p>
                                <span>
                                    Bill discount
                                </span>

                                <strong>
                                    -
                                    {money(
                                        billDiscount
                                    )}
                                </strong>
                            </p>

                            <p>
                                <span>
                                    CGST
                                </span>

                                <strong>
                                    {money(
                                        totals.cgst
                                    )}
                                </strong>
                            </p>

                            <p>
                                <span>
                                    SGST
                                </span>

                                <strong>
                                    {money(
                                        totals.sgst
                                    )}
                                </strong>
                            </p>

                            <p>
                                <span>
                                    Round-off
                                </span>

                                <strong>
                                    {money(
                                        roundOff
                                    )}
                                </strong>
                            </p>

                        </div>

                    </div>

                    {/* =============================================
                        HISTORY
                    ============================================= */}

                    <div className="history-section">

                        <div className="section-heading">

                            <div>
                                <p className="eyebrow">
                                    HISTORY
                                </p>

                                <h2>
                                    Recent purchases
                                </h2>
                            </div>

                            <button
                                className="text-button"
                                type="button"
                                onClick={() =>
                                    void loadData()
                                }
                            >
                                Refresh
                            </button>

                        </div>

                        {loading ? (
                            <div className="history-loading">
                                <span />
                                Loading invoices...
                            </div>
                        ) : purchases.length ===
                          0 ? (
                            <div className="empty-history">
                                <strong>
                                    No purchases yet
                                </strong>

                                <span>
                                    Your distributor
                                    invoices will appear
                                    here.
                                </span>
                            </div>
                        ) : (
                            <div className="invoice-list">

                                {purchases.map(
                                    purchase => (
                                        <article
                                            className="invoice-card"
                                            key={
                                                purchase.id
                                            }
                                        >

                                            <div className="invoice-card-top">

                                                <span
                                                    className={`status ${String(
                                                        purchase.status
                                                    ).toLowerCase()}`}
                                                >
                                                    {
                                                        purchase.status
                                                    }
                                                </span>

                                                <small>
                                                    {new Date(
                                                        purchase.purchaseDate
                                                    ).toLocaleDateString(
                                                        "en-IN",
                                                        {
                                                            day: "2-digit",
                                                            month: "short",
                                                            year: "numeric"
                                                        }
                                                    )}
                                                </small>

                                            </div>

                                            <h3>
                                                {
                                                    purchase.invoiceNumber
                                                }
                                            </h3>

                                            <strong>
                                                {money(
                                                    purchase.grandTotal
                                                )}
                                            </strong>

                                            <div className="invoice-actions">

                                                <button
                                                    type="button"
                                                    onClick={() =>
                                                        void viewPurchase(
                                                            purchase
                                                        )
                                                    }
                                                >
                                                    View details
                                                </button>

                                                {String(purchase.status).toUpperCase() ===
                                                    "COMPLETED" && (
                                                    <button
                                                        type="button"
                                                        className="danger-button"
                                                        onClick={() =>
                                                            void handleCancel(
                                                                purchase
                                                            )
                                                        }
                                                    >
                                                        Cancel
                                                    </button>
                                                )}

                                            </div>

                                        </article>
                                    )
                                )}

                            </div>
                        )}

                    </div>

                </aside>

            </section>

            {/* =================================================
                PURCHASE MODAL
            ================================================= */}

            {selectedPurchase && (
                <div
                    className="purchase-modal"
                    role="dialog"
                    aria-modal="true"
                >

                    <section>

                        <button
                            type="button"
                            className="modal-close"
                            onClick={() =>
                                setSelectedPurchase(
                                    null
                                )
                            }
                            aria-label="Close"
                        >
                            ×
                        </button>

                        <p className="eyebrow">
                            PURCHASE DETAILS
                        </p>

                        <h2>
                            {
                                selectedPurchase.invoiceNumber
                            }
                        </h2>

                        <div className="modal-status">
                            <span
                                className={`status ${String(
                                    selectedPurchase.status
                                ).toLowerCase()}`}
                            >
                                {
                                    selectedPurchase.status
                                }
                            </span>

                            <span>
                                {new Date(
                                    selectedPurchase.purchaseDate
                                ).toLocaleDateString(
                                    "en-IN"
                                )}
                            </span>
                        </div>

                        <div className="item-table">

                            {purchaseItems.map(
                                item => (
                                    <div
                                        key={
                                            item.id
                                        }
                                    >
                                        <span>
                                            {
                                                item.productName
                                            }
                                            {" × "}
                                            {
                                                item.quantity
                                            }

                                            {item.color
                                                ? ` · ${item.color}`
                                                : ""}
                                        </span>

                                        <strong>
                                            {money(
                                                item.totalPrice
                                            )}
                                        </strong>
                                    </div>
                                )
                            )}

                        </div>

                        <div className="payment-overview">

                            <div>
                                <span>
                                    Invoice total
                                </span>

                                <strong>
                                    {money(
                                        paymentSummary?.purchaseTotal
                                    )}
                                </strong>
                            </div>

                            <div>
                                <span>
                                    Paid
                                </span>

                                <strong>
                                    {money(
                                        paymentSummary?.totalPaid
                                    )}
                                </strong>
                            </div>

                            <div className="due">
                                <span>
                                    Outstanding
                                </span>

                                <strong>
                                    {money(
                                        paymentSummary?.remainingAmount
                                    )}
                                </strong>
                            </div>

                        </div>

                        {number(
                            paymentSummary?.remainingAmount
                        ) > 0 && (
                            <form
                                className="payment-form"
                                onSubmit={
                                    recordPayment
                                }
                            >

                                <div className="payment-heading">
                                    <strong>
                                        Record payment
                                    </strong>

                                    <span>
                                        Outstanding{" "}
                                        {money(
                                            paymentSummary?.remainingAmount
                                        )}
                                    </span>
                                </div>

                                <input
                                    required
                                    type="number"
                                    min="0.01"
                                    max={
                                        paymentSummary?.remainingAmount
                                    }
                                    step="0.01"
                                    value={
                                        payment.amount
                                    }
                                    onChange={event =>
                                        setPayment(
                                            current => ({
                                                ...current,
                                                amount:
                                                    event
                                                        .target
                                                        .value
                                            })
                                        )
                                    }
                                    placeholder="Payment amount"
                                />

                                <select
                                    value={
                                        payment.paymentMethod
                                    }
                                    onChange={event =>
                                        setPayment(
                                            current => ({
                                                ...current,
                                                paymentMethod:
                                                    event
                                                        .target
                                                        .value
                                            })
                                        )
                                    }
                                >
                                    <option>
                                        UPI
                                    </option>
                                    <option>
                                        CASH
                                    </option>
                                    <option>
                                        CARD
                                    </option>
                                    <option>
                                        BANK_TRANSFER
                                    </option>
                                </select>

                                <input
                                    value={
                                        payment.transactionReference
                                    }
                                    onChange={event =>
                                        setPayment(
                                            current => ({
                                                ...current,
                                                transactionReference:
                                                    event
                                                        .target
                                                        .value
                                            })
                                        )
                                    }
                                    placeholder="Transaction reference"
                                />

                                <button
                                    type="submit"
                                    className="primary-button"
                                    disabled={paymentSaving}
                                    aria-busy={paymentSaving}
                                >
                                    {paymentSaving
                                        ? "Recording..."
                                        : "Record payment"}
                                </button>

                            </form>
                        )}

                        <div className="payment-history">

                            <div className="payment-history-heading">
                                <strong>
                                    Payment history
                                </strong>
                            </div>

                            {payments.length ===
                            0 ? (
                                <p className="muted">
                                    No payments recorded.
                                </p>
                            ) : (
                                payments.map(
                                    entry => (
                                        <p
                                            key={
                                                entry.id
                                            }
                                        >
                                            <span>
                                                {new Date(
                                                    entry.paymentDate
                                                ).toLocaleDateString(
                                                    "en-IN"
                                                )}
                                                {" · "}
                                                {
                                                    entry.paymentMethod
                                                }
                                            </span>

                                            <strong>
                                                {money(
                                                    entry.amount
                                                )}
                                            </strong>
                                        </p>
                                    )
                                )
                            )}

                        </div>

                    </section>

                </div>
            )}

        </main>
    );
}

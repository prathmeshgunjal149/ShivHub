import {
    useEffect,
    useMemo,
    useRef,
    useState
} from "react";

import axios from "axios";
import { useNavigate } from "react-router-dom";

import useAuth from "../../hooks/useAuth";
import { API_BASE_URL } from "../../services/api";
import RazorpayCheckout from "../../components/payments/RazorpayCheckout";
import SellerOfflinePaymentPanel from "../../components/payments/SellerOfflinePaymentPanel";
import FinanceDetails from "../../components/payments/FinanceDetails";
import { newFinance } from "../../components/payments/financeUtils.js";
import { createOfflineRazorpayOrder, verifyOfflineRazorpayPayment } from "../../services/paymentService";
import { getCities, getDistricts, getStates } from "../../services/locationService";
import { scanBillingProduct } from "../../services/offlineBillingService";
import SearchAutocomplete from "../../components/common/SearchAutocomplete/SearchAutocomplete";
import { getSellerCustomerSearchSuggestions, getSellerProductSearchSuggestions } from "../../services/searchSuggestionService";
import SellerSidebar from "./SellerSidebar";
import api from "../../services/api";
import { variantSummary } from "../../components/products/productCommonFields";

import "./OfflineBilling.css";
import "./PremiumPolish.css";

const LOYALTY_POINTS_PER_RUPEE = 3;

/*
 * =========================================================
 * SHIVHUB - SELLER OFFLINE BILLING / POS
 * =========================================================
 *
 * Features:
 *
 * 1. Product search
 * 2. Add product to bill
 * 3. Increase / decrease quantity
 * 4. Remove product
 * 5. Registered customer
 * 6. Walk-in customer
 * 7. Product offer display
 * 8. Item discount
 * 9. GST calculation
 * 10. Bill discount
 * 11. Delivery charge
 * 12. Payment method
 * 13. Payment amount
 * 14. Change amount
 * 15. IMEI selection
 * 16. Generate offline bill
 *
 * IMEI APIs:
 *
 * GET
 * /api/offline-bills/imei/available?productId=123
 *
 * POST
 * /api/offline-billing
 *
 * =========================================================
 */


const OfflineBilling = () => {

    const navigate = useNavigate();

    const {
        user,
        logout
    } = useAuth();

    /*
     * =====================================================
     * STATE
     * =====================================================
     */

    const [products, setProducts] = useState([]);

    const [cartItems, setCartItems] = useState([]);

    /*
     * Available physical IMEI/serial units.
     *
     * Structure:
     *
     * {
     *     12: [
     *         {
     *             serialId: 101,
     *             imei1: "...",
     *             imei2: "...",
     *             serialNumber: "..."
     *         }
     *     ]
     * }
     */
    const [availableImeis, setAvailableImeis] = useState({});

    const [imeiLoading, setImeiLoading] = useState({});
    const [variantPicker,setVariantPicker]=useState(null);

    const [searchTerm, setSearchTerm] = useState("");

    // USB/Bluetooth scanners behave like a keyboard and normally finish with Enter.
    const [scanCode, setScanCode] = useState("");
    const [scanning, setScanning] = useState(false);
    const scanInputRef = useRef(null);

    const [customerType, setCustomerType] = useState("WALK_IN");

    const [customerId, setCustomerId] = useState("");

    const [customerProfileId, setCustomerProfileId] = useState("");

    const [customerName, setCustomerName] =
        useState("Walk-in Customer");

    const [customerMobile, setCustomerMobile] =
        useState("");

    const [customerEmail, setCustomerEmail] =
        useState("");

    const [whatsappConsent, setWhatsappConsent] =
        useState(false);

    const [customerAddress, setCustomerAddress] =
        useState("");

    const [customerCity, setCustomerCity] = useState("");

    const [customerDistrict, setCustomerDistrict] = useState("");

    const [customerState, setCustomerState] = useState("");

    const [customerPincode, setCustomerPincode] = useState("");

    const [locationStates, setLocationStates] = useState([]);
    const [locationDistricts, setLocationDistricts] = useState([]);
    const [locationCities, setLocationCities] = useState([]);

    const [customerDateOfBirth, setCustomerDateOfBirth] = useState("");

    const [customerGstin, setCustomerGstin] =
        useState("");
    const [customerLegalName, setCustomerLegalName] = useState("");
    const [customerTradeName, setCustomerTradeName] = useState("");
    const [placeOfSupply, setPlaceOfSupply] = useState("");

    const [customerLookupMessage, setCustomerLookupMessage] =
        useState("");

    const [customerLookupLoading, setCustomerLookupLoading] = useState(false);

    const [availableLoyaltyPoints, setAvailableLoyaltyPoints] = useState(0);
    const [loyaltyPointsToRedeem, setLoyaltyPointsToRedeem] = useState(0);

    const [discount, setDiscount] = useState(0);

    const [deliveryCharge, setDeliveryCharge] =
        useState(0);

    const [paymentMethod, setPaymentMethod] =
        useState("CASH");
    const [finance, setFinance] = useState(newFinance);

    const [paymentAmount, setPaymentAmount] =
        useState("");

    const [balanceDueDate, setBalanceDueDate] =
        useState("");

    const [transactionId, setTransactionId] =
        useState("");

    const [notes, setNotes] = useState("");

    const [salespeople, setSalespeople] = useState([]);

    const [salesPersonId, setSalesPersonId] = useState("");

    const [loading, setLoading] = useState(true);

    const [generatingBill, setGeneratingBill] =
        useState(false);

    const [message, setMessage] = useState("");

    const [error, setError] = useState("");

    const [billResponse, setBillResponse] =
        useState(null);

    const [razorpayOrder, setRazorpayOrder] = useState(null);

    /*
     * =====================================================
     * GET TOKEN
     * =====================================================
     */

    const getToken = () => {

        return localStorage.getItem(
            "shivhub_token"
        );
    };

    const focusScanInput = () => {
        window.setTimeout(() => scanInputRef.current?.focus(), 0);
    };

    useEffect(() => {
        focusScanInput();
    }, []);

    /*
     * =====================================================
     * CHECK WHETHER PRODUCT NEEDS IMEI
     * =====================================================
     */

    const isMobileProduct = (product) => {

        if (!product) {
            return false;
        }

        /*
         * Preferred backend flag.
         */
        if (product.imeiRequired === true || product.serialTrackingRequired === true) {
            return true;
        }

        /*
         * Fallback detection.
         */
        const category =
            String(
                product.category || ""
            ).toLowerCase();

        const name =
            String(
                product.name || ""
            ).toLowerCase();

        return (
            category.includes("mobile") ||
            category.includes("phone") ||
            category.includes("smartphone") ||
            name.includes("mobile") ||
            name.includes("iphone") ||
            name.includes("galaxy") ||
            name.includes("redmi") ||
            name.includes("realme") ||
            name.includes("oneplus") ||
            name.includes("pixel") ||
            name.includes("vivo") ||
            name.includes("oppo") ||
            name.includes("motorola") ||
            name.includes("nothing phone")
        );
    };

    /*
     * =====================================================
     * GET AVAILABLE IMEI COUNT
     * =====================================================
     */

    const getAvailableImeiCount = (productId) => {

        return (
            availableImeis[productId] || []
        ).length;
    };

    /*
     * =====================================================
     * LOAD AVAILABLE IMEI
     * =====================================================
     *
     * IMPORTANT:
     *
     * Backend Controller:
     *
     * GET /api/offline-bills/imei/available
     *
     * Query:
     *
     * ?productId=123
     *
     * =====================================================
     */

    const loadAvailableImeis = async (productId) => {

        try {

            const token = getToken();

            if (!token) {
                return;
            }

            setImeiLoading(previous => ({
                ...previous,
                [productId]: true
            }));

            const response = await axios.get(
                `${API_BASE_URL}/api/offline-bills/imei/available`,
                {
                    params: {
                        productId: productId
                    },
                    headers: {
                        Authorization:
                            `Bearer ${token}`
                    }
                }
            );

            const data =
                Array.isArray(response.data)
                    ? response.data
                    : [];

            setAvailableImeis(previous => ({
                ...previous,
                [productId]: data
            }));

        } catch (requestError) {

            console.error(
                "Could not load available IMEIs:",
                requestError
            );

            setAvailableImeis(previous => ({
                ...previous,
                [productId]: []
            }));

        } finally {

            setImeiLoading(previous => ({
                ...previous,
                [productId]: false
            }));
        }
    };

    /*
     * =====================================================
     * LOAD PRODUCTS
     * =====================================================
     */

    useEffect(() => {

        const loadProducts = async () => {

            try {

                setLoading(true);

                setError("");

                const token = getToken();

                if (!token) {

                    setError(
                        "Seller session not found. Please login again."
                    );

                    return;
                }

                const response =
                    await axios.get(
                        `${API_BASE_URL}/api/products/seller`,
                        {
                            headers: {
                                Authorization:
                                    `Bearer ${token}`
                            }
                        }
                    );

                const data =
                    Array.isArray(
                        response.data
                    )
                        ? response.data
                        : [];

                /*
                 * Only approved + active products.
                 */
                const approvedProducts =
                    data.filter(
                        product =>
                            product.approvalStatus ===
                                "APPROVED" &&
                            product.active === true
                    );

                setProducts(
                    approvedProducts
                );

            } catch (err) {

                console.error(
                    "Failed to load products:",
                    err
                );

                setError(
                    err?.response?.data?.message ||
                    "Unable to load products."
                );

            } finally {

                setLoading(false);
            }
        };

        loadProducts();

    }, []);

    /* Staff shown here are restricted by the backend to this seller's own active shops. */
    useEffect(() => {
        const loadSalespeople = async () => {
            try {
                const token = getToken();
                if (!token) return;
                const response = await axios.get(`${API_BASE_URL}/api/seller/salespeople`, {
                    headers: { Authorization: `Bearer ${token}` }
                });
                setSalespeople(Array.isArray(response.data) ? response.data : []);
            } catch (requestError) {
                console.warn("Unable to load sales staff:", requestError);
                setSalespeople([]);
            }
        };
        void loadSalespeople();
    }, []);

    /*
     * =====================================================
     * FILTER PRODUCTS
     * =====================================================
     */

    const filteredProducts =
        useMemo(() => {

            const search =
                searchTerm
                    .trim()
                    .toLowerCase();

            if (!search) {

                return products.slice(
                    0,
                    12
                );
            }

            return products
                .filter(product => {

                    const name =
                        product?.name
                            ?.toLowerCase() || "";

                    const category =
                        product?.category
                            ?.toLowerCase() || "";

                    const id =
                        String(
                            product?.id || ""
                        );

                    return (
                        name.includes(search) ||
                        category.includes(search) ||
                        id.includes(search)
                    );
                })
                .slice(
                    0,
                    20
                );

        }, [
            products,
            searchTerm
        ]);

    /*
     * =====================================================
     * OFFER PERCENTAGE
     * =====================================================
     */

    const getOfferPercentage = (
        product
    ) => {

        const offer =
            Number(
                product?.offerPercentage || 0
            );

        if (
            Number.isNaN(offer) ||
            offer <= 0
        ) {
            return 0;
        }

        return Math.min(
            offer,
            100
        );
    };

    /*
     * =====================================================
     * FINAL PRICE
     * =====================================================
     */

    const getFinalPrice = (
        product
    ) => {

        const price =
            Number(
                product?.price || 0
            );

        const offer =
            getOfferPercentage(
                product
            );

        if (offer <= 0) {
            return price;
        }

        return (
            price *
            (1 - offer / 100)
        );
    };

    const cartKeyFor = (item) => item.cartKey || `product-${item.productId}`;

    /*
     * Scanner lines stay separate only when they represent a physical
     * IMEI/serial unit. Catalogue scans use the normal product line and
     * therefore retain the existing quantity controls.
     */
    const addScannedProduct = (scan) => {
        if(scan.variantId){addProduct({id:scan.productId,name:scan.productName,variantId:scan.variantId,selectedAttributes:scan.selectedAttributes,productType:"NON_MOBILE",price:scan.sellingPrice,finalSellingPrice:scan.sellingPrice,stock:scan.stockAvailable,availableStock:scan.stockAvailable,gstRate:scan.gstRate,imageUrl:scan.imageUrl,serialTrackingRequired:false});return true;}
        const serialTracked = Boolean(scan.imeiTracked && scan.serialId);

        if (serialTracked && cartItems.some(item =>
            (item.purchaseSerialIds || []).includes(scan.serialId)
        )) {
            setError("This IMEI is already added to the bill.");
            return false;
        }

        const product = {
            id: scan.productId,
            name: scan.productName,
            brand: scan.brand || "",
            model: scan.model || "",
            colorOptions: scan.color || "",
            ram: scan.ram || "",
            storage: scan.storage || "",
            sellerSku: scan.sku || "",
            barcode: scan.barcode || "",
            hsnCode: scan.hsnCode || "",
            price: Number(scan.sellingPrice || 0),
            stock: Number(scan.stockAvailable || 0),
            gstRate: Number(scan.gstRate || 0),
            imageUrl: scan.imageUrl || "",
            warrantyDetails: scan.warrantyDetails || "",
            serialTrackingRequired: serialTracked,
            imeiRequired: serialTracked,
            active: true,
            approvalStatus: "APPROVED"
        };

        setCartItems(previous => {
            if (serialTracked) {
                return [
                    ...previous,
                    {
                        cartKey: `serial-${scan.serialId}`,
                        scanned: true,
                        scannedCode: scan.scannedCode,
                        scannedSerialId: scan.serialId,
                        scannedImei: scan.imei1 || scan.serialNumber || "",
                        productId: product.id,
                        productName: product.name,
                        product,
                        originalPrice: product.price,
                        unitPrice: product.price,
                        offerPercentage: 0,
                        stock: Number(scan.stockAvailable || 0),
                        quantity: 1,
                        discount: 0,
                        gstRate: Number(scan.gstRate || 0),
                        purchaseSerialIds: [scan.serialId]
                    }
                ];
            }

            const existing = previous.find(item =>
                item.productId === product.id && !item.scannedSerialId
            );

            if (existing) {
                if (existing.quantity >= Number(scan.stockAvailable || 0)) {
                    setError("Product is out of stock.");
                    return previous;
                }
                return previous.map(item => item === existing
                    ? { ...item, quantity: item.quantity + 1, stock: Number(scan.stockAvailable || 0), scanned: true, scannedCode: scan.scannedCode }
                    : item
                );
            }

            return [
                ...previous,
                {
                    cartKey: `product-${product.id}`,
                    scanned: true,
                    scannedCode: scan.scannedCode,
                    productId: product.id,
                    productName: product.name,
                    product,
                    originalPrice: product.price,
                    unitPrice: product.price,
                    offerPercentage: 0,
                    stock: Number(scan.stockAvailable || 0),
                    quantity: 1,
                    discount: 0,
                    gstRate: Number(scan.gstRate || 0),
                    purchaseSerialIds: []
                }
            ];
        });
        return true;
    };

    const handleScan = async (event) => {
        event?.preventDefault();
        const code = scanCode.trim();
        if (!code) {
            setError("Please scan or enter a valid code.");
            focusScanInput();
            return;
        }

        setError("");
        setMessage("");
        try {
            setScanning(true);
            const result = await scanBillingProduct(code);
            if (addScannedProduct(result)) {
                setMessage(result.message || "Scanned product added to the bill.");
                setScanCode("");
            }
        } catch (requestError) {
            setError(requestError?.response?.data?.message || "Unable to find a billable product for this scan.");
        } finally {
            setScanning(false);
            focusScanInput();
        }
    };

    /*
     * =====================================================
     * ADD PRODUCT
     * =====================================================
     */

    const addProduct = (
        product
    ) => {
        if(product.variantsEnabled&&!product.variantId){
            setVariantPicker({product,loading:true,variants:[]});
            api.get(`/api/seller/products/${product.id}/variants`).then(({data})=>setVariantPicker(current=>current?.product.id===product.id?{product,loading:false,variants:data.filter(variant=>variant.active)}:current)).catch(e=>{setVariantPicker(null);setError(e.response?.data?.message||"Unable to load product variants.");});return;
        }

        setMessage("");

        setError("");

        const existing =
            cartItems.find(
                item =>
                    item.productId ===
                    product.id &&
                    (item.variantId||null)===(product.variantId||null) &&
                    !item.scannedSerialId
            );

        /*
         * =================================================
         * PRODUCT ALREADY IN CART
         * =================================================
         */

        if (existing) {

            if (
                existing.quantity >=
                Number(product.stock || 0)
            ) {

                setError(
                    `Only ${product.stock} units available for ${product.name}.`
                );

                return;
            }

            /*
             * For IMEI products, check available
             * physical units.
             */

            if (
                isMobileProduct(product)
            ) {

                const imeis =
                    availableImeis[
                        product.id
                    ] || [];

                if (
                    imeis.length > 0 &&
                    existing.quantity >=
                    imeis.length
                ) {

                    setError(
                        `Only ${imeis.length} IMEI-tracked units are available for ${product.name}.`
                    );

                    return;
                }

                /*
                 * Load IMEI list if not already loaded.
                 */
                if (
                    !Object.prototype.hasOwnProperty.call(
                        availableImeis,
                        product.id
                    )
                ) {

                    void loadAvailableImeis(
                        product.id
                    );
                }
            }

            setCartItems(
                previous =>
                    previous.map(
                        item =>
                            item.productId ===
                            product.id &&
                            (item.variantId||null)===(product.variantId||null) &&
                            !item.scannedSerialId
                                ? {
                                    ...item,
                                    quantity:
                                        item.quantity + 1
                                }
                                : item
                    )
            );

            return;
        }

        /*
         * =================================================
         * OUT OF STOCK
         * =================================================
         */

        if (
            Number(product.stock || 0) <= 0
        ) {

            setError(
                `${product.name} is out of stock.`
            );

            return;
        }

        /*
         * =================================================
         * CREATE CART ITEM
         * =================================================
         */

        const newItem = {

            cartKey:
                product.variantId?`variant-${product.variantId}`:`product-${product.id}`,
            variantId:product.variantId,
            selectedAttributes:product.selectedAttributes,

            productId:
                product.id,

            productName:
                product.name,

            product:
                product,

            originalPrice:
                Number(
                    product.price || 0
                ),

            unitPrice:
                getFinalPrice(
                    product
                ),

            offerPercentage:
                getOfferPercentage(
                    product
                ),

            stock:
                Number(
                    product.stock || 0
                ),

            quantity: 1,

            discount: 0,

            gstRate: Number(product.gstRate ?? 18),

            /*
             * Important:
             *
             * This contains PurchaseItemSerial IDs.
             */
            purchaseSerialIds: []
        };

        setCartItems(
            previous => [
                ...previous,
                newItem
            ]
        );

        /*
         * =================================================
         * LOAD IMEI FOR MOBILE PRODUCT
         * =================================================
         */

        if (
            isMobileProduct(product)
        ) {

            void loadAvailableImeis(
                product.id
            );
        }
    };

    /*
     * =====================================================
     * TOGGLE IMEI
     * =====================================================
     */

    const toggleImei = (
        itemKey,
        serialId
    ) => {

        setMessage("");

        setError("");

        setCartItems(
            previous =>
                previous.map(
                    item => {

                        if (
                            cartKeyFor(item) !==
                            itemKey
                        ) {
                            return item;
                        }

                        const selected =
                            item.purchaseSerialIds ||
                            [];

                        const exists =
                            selected.includes(
                                serialId
                            );

                        /*
                         * Cannot select more IMEIs
                         * than quantity.
                         */

                        if (
                            !exists &&
                            selected.length >=
                            item.quantity
                        ) {

                            setError(
                                `Select only ${item.quantity} IMEI${item.quantity > 1 ? "s" : ""} for ${item.productName}.`
                            );

                            return item;
                        }

                        return {
                            ...item,

                            purchaseSerialIds:
                                exists
                                    ? selected.filter(
                                        id =>
                                            id !==
                                            serialId
                                    )
                                    : [
                                        ...selected,
                                        serialId
                                    ]
                        };
                    }
                )
        );
    };

    /*
     * =====================================================
     * INCREASE QUANTITY
     * =====================================================
     */

    const increaseQuantity = (
        itemKey
    ) => {

        setCartItems(
            previous =>
                previous.map(
                    item => {

                        if (
                            cartKeyFor(item) !==
                            itemKey
                        ) {
                            return item;
                        }

                        if (item.scannedSerialId) {
                            setError("Scanned IMEI quantity is fixed at 1.");
                            return item;
                        }

                        if (
                            item.quantity >=
                            item.stock
                        ) {

                            setError(
                                `Only ${item.stock} units available for ${item.productName}.`
                            );

                            return item;
                        }

                        /*
                         * For IMEI products,
                         * don't exceed physical units.
                         */

                        if (
                            isMobileProduct(
                                item.product
                            )
                        ) {

                            const imeiOptions =
                                availableImeis[
                                    item.productId
                                ] || [];

                            if (
                                imeiOptions.length > 0 &&
                                item.quantity >=
                                imeiOptions.length
                            ) {

                                setError(
                                    `Only ${imeiOptions.length} IMEI-tracked units are available for ${item.productName}.`
                                );

                                return item;
                            }

                            /*
                             * Make sure IMEI data is loaded.
                             */

                            if (
                                !Object.prototype.hasOwnProperty.call(
                                    availableImeis,
                                    item.productId
                                )
                            ) {

                                void loadAvailableImeis(
                                    item.productId
                                );
                            }
                        }

                        return {
                            ...item,

                            quantity:
                                item.quantity + 1,

                            purchaseSerialIds:
                                item.purchaseSerialIds ||
                                []
                        };
                    }
                )
        );
    };

    /*
     * =====================================================
     * DECREASE QUANTITY
     * =====================================================
     */

    const decreaseQuantity = (
        itemKey
    ) => {

        setCartItems(
            previous =>
                previous
                    .map(
                        item => {

                            if (
                                cartKeyFor(item) !==
                                itemKey
                            ) {
                                return item;
                            }

                            const newQuantity =
                                item.quantity - 1;

                            return {
                                ...item,

                                quantity:
                                    newQuantity,

                                purchaseSerialIds:
                                    (
                                        item.purchaseSerialIds ||
                                        []
                                    ).slice(
                                        0,
                                        Math.max(
                                            0,
                                            newQuantity
                                        )
                                    )
                            };
                        }
                    )
                    .filter(
                        item =>
                            item.quantity > 0
                    )
        );
    };

    /*
     * =====================================================
     * REMOVE ITEM
     * =====================================================
     */

    const removeItem = (
        itemKey
    ) => {

        setCartItems(
            previous =>
                previous.filter(
                    item =>
                    cartKeyFor(item) !==
                    itemKey
                )
        );
    };

    /*
     * =====================================================
     * UPDATE ITEM DISCOUNT
     * =====================================================
     */

    const updateItemDiscount = (
        itemKey,
        value
    ) => {

        const numericValue =
            Number(value);

        setCartItems(
            previous =>
                previous.map(
                    item => {

                        if (
                            cartKeyFor(item) !==
                            itemKey
                        ) {
                            return item;
                        }

                        const gross =
                            item.unitPrice *
                            item.quantity;

                        const safeDiscount =
                            Math.max(
                                0,
                                Math.min(
                                    Number.isNaN(
                                        numericValue
                                    )
                                        ? 0
                                        : numericValue,
                                    gross
                                )
                            );

                        return {
                            ...item,

                            discount:
                                safeDiscount
                        };
                    }
                )
        );
    };

    const updateUnitPrice = (itemKey, value) => {
        const numericValue = Number(value);
        const safePrice = Number.isNaN(numericValue) ? 0 : Math.max(0, numericValue);

        setCartItems(previous => previous.map(item => {
            if (cartKeyFor(item) !== itemKey) return item;
            const gross = safePrice * item.quantity;
            return {
                ...item,
                unitPrice: safePrice,
                discount: Math.min(Number(item.discount || 0), gross)
            };
        }));
    };

    /*
     * =====================================================
     * UPDATE GST
     * =====================================================
     */

    const updateGstRate = (
        itemKey,
        value
    ) => {

        const numericValue =
            Number(value);

        setCartItems(
            previous =>
                previous.map(
                    item =>
                        cartKeyFor(item) ===
                        itemKey
                            ? {
                                ...item,

                                gstRate:
                                    Math.max(
                                        0,
                                        Math.min(
                                            100,
                                            Number.isNaN(
                                                numericValue
                                            )
                                                ? 0
                                                : numericValue
                                        )
                                    )
                            }
                            : item
                )
        );
    };

    /*
     * =====================================================
     * ROUND
     * =====================================================
     */

    function round(value) {

        return Number(
            Number(value || 0)
                .toFixed(2)
        );
    }

    /*
     * =====================================================
     * BILL CALCULATION
     * =====================================================
     */

    const billCalculation =
        useMemo(() => {

            // The summary follows a tax invoice layout: subtotal is the taxable
            // (GST-exclusive) amount, while the product line keeps its actual
            // GST-inclusive selling price.
            let inclusiveSubtotal = 0;

            let taxableSubtotal = 0;

            let itemDiscountTotal = 0;

            let itemDiscountTaxableTotal = 0;

            let taxableBeforeBillDiscount = 0;

            let cgst = 0;

            let sgst = 0;

            cartItems.forEach(
                item => {

                    const gross =
                        item.unitPrice *
                        item.quantity;

                    const itemDiscount =
                        Number(
                            item.discount || 0
                        );

                    // Product/POS unit price is GST-inclusive. Split that final
                    // amount into taxable value + GST instead of adding GST once
                    // more. Example: Rs.57,990 at 18% remains Rs.57,990 total.
                    const gstRate =
                        Number(
                            item.gstRate || 0
                        );

                    const inclusiveAfterItemDiscount =
                        Math.max(
                            0,
                            gross -
                            itemDiscount
                        );

                    const grossTaxable = gstRate > 0
                        ? gross / (1 + gstRate / 100)
                        : gross;

                    const taxable = gstRate > 0
                        ? inclusiveAfterItemDiscount / (1 + gstRate / 100)
                        : inclusiveAfterItemDiscount;

                    const totalGst =
                        inclusiveAfterItemDiscount -
                        taxable;

                    const itemCgst =
                        totalGst / 2;

                    const itemSgst =
                        totalGst / 2;

                    inclusiveSubtotal +=
                        gross;

                    taxableSubtotal +=
                        grossTaxable;

                    itemDiscountTotal +=
                        itemDiscount;

                    itemDiscountTaxableTotal +=
                        Math.max(0, grossTaxable - taxable);

                    taxableBeforeBillDiscount +=
                        taxable;

                    cgst +=
                        itemCgst;

                    sgst +=
                        itemSgst;
                }
            );

            const safeBillDiscount =
                Math.max(
                    0,
                    Math.min(
                        Number(
                            discount || 0
                        ),
                        Math.max(
                            0,
                            inclusiveSubtotal -
                            itemDiscountTotal
                        )
                    )
                );

            const remainingInclusiveAmount = Math.max(
                0,
                inclusiveSubtotal - itemDiscountTotal
            );
            const billDiscountRatio = remainingInclusiveAmount > 0
                ? safeBillDiscount / remainingInclusiveAmount
                : 0;
            const finalTaxable = Math.max(
                0,
                taxableBeforeBillDiscount * (1 - billDiscountRatio)
            );

            /*
             * Proportionally reduce GST
             * when bill-level discount exists.
             */

            if (
                remainingInclusiveAmount > 0 &&
                safeBillDiscount > 0
            ) {

                const ratio =
                    billDiscountRatio;

                cgst =
                    cgst *
                    (1 - ratio);

                sgst =
                    sgst *
                    (1 - ratio);
            }

            const delivery =
                Math.max(
                    0,
                    Number(
                        deliveryCharge || 0
                    )
                );

            const grandTotal =
                Math.max(
                    0,
                    finalTaxable +
                    cgst +
                    sgst +
                    delivery
                );

            const totalDiscount =
                itemDiscountTotal +
                safeBillDiscount;

            return {

                subtotal:
                    round(
                        taxableSubtotal
                    ),

                itemDiscount:
                    round(
                        itemDiscountTaxableTotal
                    ),

                billDiscount:
                    round(
                        safeBillDiscount
                    ),

                totalDiscount:
                    round(
                        totalDiscount
                    ),

                taxableAmount:
                    round(
                        finalTaxable
                    ),

                cgst:
                    round(
                        cgst
                    ),

                sgst:
                    round(
                        sgst
                    ),

                deliveryCharge:
                    round(
                        delivery
                    ),

                grandTotal:
                    round(
                        grandTotal
                    )
            };

        }, [
            cartItems,
            discount,
            deliveryCharge
        ]);

    // The POS preview mirrors the server rule. The backend revalidates this
    // again while creating the bill, so an edited browser value cannot change it.
    const maximumRedeemablePoints = customerType === "REGISTERED"
        ? Math.max(0, Math.floor(Math.min(
            Number(availableLoyaltyPoints || 0),
            Math.floor(billCalculation.grandTotal * LOYALTY_POINTS_PER_RUPEE)
        ) / LOYALTY_POINTS_PER_RUPEE) * LOYALTY_POINTS_PER_RUPEE)
        : 0;
    const redeemedLoyaltyPoints = Math.min(
        maximumRedeemablePoints,
        Math.floor(Math.max(0, Number(loyaltyPointsToRedeem || 0)) / LOYALTY_POINTS_PER_RUPEE) * LOYALTY_POINTS_PER_RUPEE
    );
    const loyaltyDiscountPreview = round(redeemedLoyaltyPoints / LOYALTY_POINTS_PER_RUPEE);
    const payableGrandTotal = round(Math.max(0, billCalculation.grandTotal - loyaltyDiscountPreview));

    /*
     * =====================================================
     * PAYMENT
     * =====================================================
     */

    const paidAmount =
        Number(
            (paymentMethod === "FINANCE" ? finance.downpayment : paymentAmount) || 0
        );

    const changeAmount =
        Math.max(
            0,
            paidAmount -
            payableGrandTotal
        );

    const remainingAmount =
        Math.max(
            0,
            payableGrandTotal -
            paidAmount
        );

    /*
     * =====================================================
     * TRANSACTION ID REQUIRED
     * =====================================================
     */

    const requiresTransactionId =
        paymentMethod === "UPI" ||
        paymentMethod === "CARD" ||
        paymentMethod === "BANK_TRANSFER";

    /*
     * =====================================================
     * GENERATE BILL
     * =====================================================
     */

    const handleGenerateBill =
        async () => {

            setMessage("");

            setError("");

            setBillResponse(null);

            /*
             * No items.
             */

            if (
                cartItems.length === 0
            ) {

                setError(
                    "Please add at least one product."
                );

                return;
            }

            /*
             * =================================================
             * VALIDATE IMEI
             * =================================================
             */

            const missingImeiItem =
                cartItems.find(
                    item => {

                        const mobile =
                            isMobileProduct(
                                item.product
                            );

                        if (!mobile) {
                            return false;
                        }

                        const options =
                            availableImeis[
                                item.productId
                            ] || [];

                        const selected =
                            item.purchaseSerialIds ||
                            [];

                        // A successful scan already selected this exact unit.
                        // Final backend validation still verifies it has not been sold meanwhile.
                        if (item.scannedSerialId) {
                            return item.quantity !== 1 || selected.length !== 1;
                        }

                        /*
                         * Every mobile unit must have
                         * one physical serial selected.
                         */

                        return (
                            options.length <
                                item.quantity ||
                            selected.length !==
                                item.quantity
                        );
                    }
                );

            if (
                missingImeiItem
            ) {

                const options =
                    availableImeis[
                        missingImeiItem.productId
                    ] || [];

                const selected =
                    missingImeiItem.purchaseSerialIds ||
                    [];

                if (
                    options.length <
                    missingImeiItem.quantity
                ) {

                    setError(
                        `Only ${options.length} IMEI-tracked units are available for ${missingImeiItem.productName}, but ${missingImeiItem.quantity} unit(s) are being sold.`
                    );

                } else if (
                    selected.length <
                    missingImeiItem.quantity
                ) {

                    setError(
                        `Please select ${missingImeiItem.quantity} IMEI${missingImeiItem.quantity > 1 ? "s" : ""} for ${missingImeiItem.productName}.`
                    );

                } else {

                    setError(
                        `Too many IMEIs selected for ${missingImeiItem.productName}.`
                    );
                }

                return;
            }

            /*
             * =================================================
             * PAYMENT VALIDATION
             * =================================================
             */

            if (
                paymentMethod !== "RAZORPAY" && paymentMethod !== "FINANCE" &&
                (paymentAmount === "" || paymentAmount === null)
            ) {

                setError(
                    "Please enter payment amount."
                );

                return;
            }

            if (paidAmount < 0) {
                setError("Payment amount cannot be negative.");
                return;
            }

            if (paymentMethod !== "RAZORPAY" && paymentMethod !== "FINANCE" && paidAmount < payableGrandTotal && !balanceDueDate) {
                setError("For a partial payment, select the customer's promised balance payment date.");
                return;
            }

            if (paymentMethod !== "RAZORPAY" && paidAmount < payableGrandTotal && !customerEmail.trim()) {
                setError("Customer email is required for a partial-payment balance statement.");
                return;
            }

            if (paymentMethod === "FINANCE" && (!finance.companyId || !customerEmail.trim() || finance.installments.length !== Number(finance.tenureMonths))) {
                setError("Select an approved finance company, enter customer email and generate the complete EMI schedule.");
                return;
            }

            /*
             * =================================================
             * TRANSACTION ID
             * =================================================
             */

            if (
                requiresTransactionId &&
                !transactionId.trim()
            ) {

                setError(
                    `Transaction ID is required for ${paymentMethod}.`
                );

                return;
            }

            try {

                setGeneratingBill(true);

                const token =
                    getToken();

                if (!token) {

                    setError(
                        "Seller session not found. Please login again."
                    );

                    return;
                }

                /*
                 * =================================================
                 * REQUEST BODY
                 * =================================================
                 */

                let resolvedCustomer = {
                    customerId,
                    customerProfileId
                };

                if (customerType === "REGISTERED") {
                    const customerResponse = await axios.post(
                        `${API_BASE_URL}/api/seller/customers/register-or-update`,
                        {
                            name: customerName,
                            mobile: customerMobile,
                            email: customerEmail || null,
                            address: customerAddress || null,
                            city: customerCity || null,
                            district: customerDistrict || null,
                            state: customerState || null,
                            pincode: customerPincode || null,
                            dateOfBirth: customerDateOfBirth || null,
                            whatsappConsent
                        },
                        { headers: { Authorization: `Bearer ${token}` } }
                    );
                    if (!customerResponse.data?.found || !customerResponse.data?.customerProfileId) {
                        throw new Error("Unable to save and identify the registered customer.");
                    }
                    resolvedCustomer = customerResponse.data;
                    setCustomerId(customerResponse.data.customerId || "");
                    setCustomerProfileId(customerResponse.data.customerProfileId);
                }

                const billingAddress = [
                    customerAddress,
                    customerCity,
                    customerDistrict,
                    customerState,
                    customerPincode
                ].map(value => String(value || "").trim()).filter(Boolean).join(", ");

                const requestBody = {

                    customerId:
                        customerType ===
                            "REGISTERED" &&
                        resolvedCustomer.customerId
                            ? Number(
                                resolvedCustomer.customerId
                            )
                            : null,

                    customerProfileId:
                        customerType === "REGISTERED" &&
                        resolvedCustomer.customerProfileId
                            ? Number(resolvedCustomer.customerProfileId)
                            : null,

                    customerName:
                        customerName ||
                        "Walk-in Customer",

                    customerMobile:
                        customerMobile ||
                        null,

                    customerEmail:
                        customerEmail ||
                        null,

                    whatsappConsent,

                    customerAddress:
                        billingAddress ||
                        null,

                    customerGstin:
                        customerGstin ||
                        null,

                    customerLegalName: customerLegalName || null,
                    customerTradeName: customerTradeName || null,
                    placeOfSupply: placeOfSupply || customerState || null,

                    loyaltyPointsToRedeem: redeemedLoyaltyPoints,

                    salesPersonId:
                        salesPersonId
                            ? Number(salesPersonId)
                            : null,

                    /*
                     * IMPORTANT:
                     *
                     * purchaseSerialIds contains
                     * PurchaseItemSerial IDs selected
                     * from the IMEI picker.
                     */

                    items:
                        cartItems.map(
                            item => ({

                                productId:
                                    item.productId,
                                variantId:item.variantId,

                                quantity:
                                    item.quantity,

                                unitPrice:
                                    round(
                                        item.unitPrice
                                    ),

                                discount:
                                    round(
                                        item.discount
                                    ),

                                gstRate:
                                    round(
                                        item.gstRate
                                    ),

                                purchaseSerialIds:
                                    item.purchaseSerialIds ||
                                    []
                            })
                        ),

                    discount:
                        billCalculation
                            .billDiscount,

                    deliveryCharge:
                        billCalculation
                            .deliveryCharge,

                    paymentMethod:
                        paymentMethod,

                    finance: paymentMethod === "FINANCE" ? {
                        ...finance,
                        companyId: Number(finance.companyId), schemeId: finance.schemeId ? Number(finance.schemeId) : null,
                        installments: finance.installments.map(row => ({ ...row, amount: Number(row.amount) }))
                    } : null,

                    paymentAmount:
                        paymentMethod === "RAZORPAY" ? 0 : round(paidAmount),

                    balanceDueDate:
                        paymentMethod !== "RAZORPAY" && paymentMethod !== "FINANCE" && paidAmount < payableGrandTotal
                            ? balanceDueDate
                            : null,

                    transactionId:
                        transactionId.trim() ||
                        null,

                    notes:
                        notes.trim() ||
                        null
                };

                /*
                 * =================================================
                 * CREATE BILL
                 * =================================================
                 */

                const response =
                    await axios.post(
                        `${API_BASE_URL}/api/offline-billing`,
                        requestBody,
                        {
                            headers: {

                                Authorization:
                                    `Bearer ${token}`,

                                "Content-Type":
                                    "application/json"
                            }
                        }
                    );

                /*
                 * =================================================
                 * SUCCESS
                 * =================================================
                 */

                setBillResponse(
                    response.data
                );

                if (paymentMethod === "RAZORPAY") {
                    setRazorpayOrder(await createOfflineRazorpayOrder(response.data.billId));
                }

                /*
                 * =================================================
                 * UPDATE PRODUCT STOCK
                 * =================================================
                 */

                setProducts(
                    previous =>
                        previous.map(
                            product => {

                                const soldItem =
                                    cartItems.find(
                                        item =>
                                            item.productId ===
                                            product.id
                                    );

                                if (!soldItem) {
                                    return product;
                                }

                                return {

                                    ...product,

                                    stock:
                                        Math.max(
                                            0,
                                            Number(
                                                product.stock ||
                                                0
                                            ) -
                                            Number(
                                                soldItem.quantity ||
                                                0
                                            )
                                        )
                                };
                            }
                        )
                );

                /*
                 * =================================================
                 * REMOVE SOLD IMEIs FROM FRONTEND CACHE
                 * =================================================
                 */

                setAvailableImeis(
                    previous => {

                        const next = {
                            ...previous
                        };

                        cartItems.forEach(
                            item => {

                                const soldSerialIds =
                                    new Set(
                                        item.purchaseSerialIds ||
                                        []
                                    );

                                if (
                                    soldSerialIds.size ===
                                    0
                                ) {
                                    return;
                                }

                                next[
                                    item.productId
                                ] =
                                    (
                                        next[
                                            item.productId
                                        ] || []
                                    ).filter(
                                        serial =>
                                            !soldSerialIds.has(
                                                serial.serialId
                                            )
                                    );
                            }
                        );

                        return next;
                    }
                );

                setMessage(
                    "Offline bill generated successfully. Stock and selected IMEI units have been updated."
                );

                /*
                 * =================================================
                 * CLEAR BILL
                 * =================================================
                 */

                setCartItems([]);

                setSearchTerm("");

                setDiscount(0);

                setDeliveryCharge(0);

                setPaymentAmount("");

                setBalanceDueDate("");

                setTransactionId("");

                setNotes("");

            } catch (err) {

                console.error(
                    "Offline billing failed:",
                    err
                );

                const backendMessage =
                    err?.response?.data?.message ||
                    err?.response?.data?.error;

                setError(
                    backendMessage ||
                    "Unable to generate offline bill."
                );

            } finally {

                setGeneratingBill(false);
            }
        };

    /*
     * =====================================================
     * LOGOUT
     * =====================================================
     */

    const handleOfflineRazorpaySuccess = async payload => {
        if (!billResponse?.billId) throw new Error("Offline bill was not created.");
        const result = await verifyOfflineRazorpayPayment(billResponse.billId, payload);
        setRazorpayOrder(null);
        setBillResponse(current => current ? { ...current, paymentAmount: result.paidAmount, paymentStatus: result.paymentStatus } : current);
        setMessage(result.remainingAmount > 0 ? "Online payment verified. Balance remains due." : "Online payment verified. Bill is fully paid.");
    };

    /* Reuse the live location APIs already used by customer addresses. */
    useEffect(() => {
        let active = true;
        getStates()
            .then(values => { if (active) setLocationStates(Array.isArray(values) ? values : []); })
            .catch(() => { if (active) setLocationStates([]); });
        return () => { active = false; };
    }, []);

    useEffect(() => {
        let active = true;
        if (!customerState) {
            setLocationDistricts([]);
            return undefined;
        }
        getDistricts(customerState)
            .then(values => { if (active) setLocationDistricts(Array.isArray(values) ? values : []); })
            .catch(() => { if (active) setLocationDistricts([]); });
        return () => { active = false; };
    }, [customerState]);

    useEffect(() => {
        let active = true;
        if (!customerState || !customerDistrict) {
            setLocationCities([]);
            return undefined;
        }
        getCities(customerState, customerDistrict)
            .then(values => { if (active) setLocationCities(Array.isArray(values) ? values : []); })
            .catch(() => { if (active) setLocationCities([]); });
        return () => { active = false; };
    }, [customerState, customerDistrict]);

    const applyOfflinePaymentSummary = summary => {
        if (!summary) return;
        setBillResponse(current => current ? {
            ...current,
            paymentAmount: summary.paidAmount,
            paymentStatus: summary.paymentStatus
        } : current);
    };

    const handleOfflineRazorpayDismiss = () => {
        setRazorpayOrder(null);
        setMessage("Payment is pending. Open the bill again to retry Razorpay collection safely.");
    };

    const handleOfflineRazorpayError = paymentError => {
        setRazorpayOrder(null);
        setError(paymentError?.response?.data?.message || paymentError?.message || "Razorpay payment was not completed.");
    };

    const handleLogout = () => {

        logout();

        window.location.href =
            "/login";
    };

    const downloadGeneratedInvoice = async () => {
        if (!billResponse?.billId) {
            setError("Invoice ID was not returned. Please open it from the Invoice Report.");
            return;
        }

        try {
            setError("");
            const token = getToken();
            const response = await axios.get(
                `${API_BASE_URL}/api/seller/offline-bills/${billResponse.billId}/invoice.pdf`,
                { responseType: "blob", headers: { Authorization: `Bearer ${token}` } }
            );
            const url = URL.createObjectURL(new Blob([response.data], { type: "application/pdf" }));
            window.open(url, "_blank", "noopener,noreferrer");
            window.setTimeout(() => URL.revokeObjectURL(url), 60000);
        } catch (requestError) {
            setError(requestError?.response?.data?.message || "Unable to create invoice PDF.");
        }
    };

    const normalizeIndianMobile = value => {
        let mobile = String(value || "").replace(/[\s-]/g, "").trim();
        if (mobile.startsWith("+91")) mobile = mobile.slice(3);
        else if (mobile.startsWith("91") && mobile.length === 12) mobile = mobile.slice(2);
        return mobile;
    };

    const lookupRegisteredCustomer = async mobile => {
        const value = normalizeIndianMobile(mobile);
        setCustomerMobile(value);
        setWhatsappConsent(false);
        if (customerType !== "REGISTERED" || !/^[6-9][0-9]{9}$/.test(value)) {
            setCustomerLookupMessage("Enter a valid 10 digit Indian mobile number.");
            return;
        }
        try {
            setCustomerLookupLoading(true);
            const token = getToken();
            const response = await axios.get(
                `${API_BASE_URL}/api/seller/customers/lookup?mobile=${encodeURIComponent(value)}`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            const customer = response.data || {};
            if (!customer.found) {
                setCustomerId(""); setCustomerProfileId(""); setCustomerName(""); setCustomerEmail(""); setCustomerAddress("");
                setCustomerCity(""); setCustomerDistrict(""); setCustomerState(""); setCustomerPincode(""); setCustomerDateOfBirth("");
                setCustomerLookupMessage("Customer not found. Enter a name to create a reusable customer record when this bill is saved.");
                setAvailableLoyaltyPoints(0); setLoyaltyPointsToRedeem(0); return;
            }
            setCustomerId(customer.customerId || ""); setCustomerProfileId(customer.customerProfileId || "");
            setCustomerName(customer.fullName || ""); setCustomerMobile(customer.mobile || value); setCustomerEmail(customer.email || "");
            setCustomerAddress(customer.address || ""); setCustomerCity(customer.city || ""); setCustomerDistrict(customer.district || "");
            setCustomerState(customer.state || ""); setCustomerPincode(customer.pincode || ""); setCustomerDateOfBirth(customer.dateOfBirth || "");
            setAvailableLoyaltyPoints(Number(customer.availablePoints || 0)); setLoyaltyPointsToRedeem(0);
            const duplicateNote = customer.duplicateRecords ? ` ${customer.duplicateRecords} duplicate historical record(s) were detected for later review.` : "";
            setCustomerLookupMessage(`Existing customer found. ${customer.previousPurchaseCount || 0} previous purchase(s); ${customer.availablePoints || 0} available points.${duplicateNote}`);
        } catch (requestError) {
            setCustomerLookupMessage(requestError?.response?.data?.message || "Customer lookup failed.");
        } finally {
            setCustomerLookupLoading(false);
        }
    };

    const selectRegisteredCustomer = suggestion => {
        const mobile = suggestion?.metadata?.mobile || suggestion?.mobile || suggestion?.label;
        lookupRegisteredCustomer(mobile);
    };

    /*
     * =====================================================
     * RESET BILL
     * =====================================================
     */

    const resetBill = () => {

        setCartItems([]);

        setCustomerType(
            "WALK_IN"
        );

        setCustomerId("");

        setCustomerProfileId("");

        setCustomerName(
            "Walk-in Customer"
        );

        setCustomerMobile("");

        setCustomerEmail("");

        setWhatsappConsent(false);

        setCustomerAddress("");

        setCustomerCity("");

        setCustomerDistrict("");

        setCustomerState("");

        setCustomerPincode("");

        setCustomerDateOfBirth("");

        setCustomerGstin("");
        setCustomerLegalName("");
        setCustomerTradeName("");
        setPlaceOfSupply("");
        setCustomerLookupMessage("");
        setCustomerLookupLoading(false);
        setAvailableLoyaltyPoints(0);
        setLoyaltyPointsToRedeem(0);

        setDiscount(0);

        setDeliveryCharge(0);

        setPaymentMethod(
            "CASH"
        );

        setPaymentAmount("");

        setFinance(newFinance());

        setBalanceDueDate("");

        setTransactionId("");

        setNotes("");

        setSalesPersonId("");

        setMessage("");

        setError("");

        setBillResponse(null);

        setAvailableImeis({});
    };

    /*
     * =====================================================
     * BACK
     * =====================================================
     */

    const handleBack = () => {

        navigate(
            "/seller/dashboard"
        );
    };

    /*
     * =====================================================
     * CURRENCY FORMAT
     * =====================================================
     */

    const formatCurrency = (
        value
    ) => {

        return Number(
            value || 0
        ).toLocaleString(
            "en-IN",
            {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
            }
        );
    };

    /*
     * =====================================================
     * UI
     * =====================================================
     */

    return (

        <div className="seller-billing-workspace">
        <SellerSidebar />
        <div className="offline-billing-page">
            {variantPicker&&<div className="variant-modal-backdrop" onMouseDown={()=>setVariantPicker(null)}><section className="variant-modal" role="dialog" aria-modal="true" aria-label="Select product variant" onMouseDown={event=>event.stopPropagation()}><h2>{variantPicker.product.name} — Select variant</h2><button type="button" onClick={()=>setVariantPicker(null)}>Close</button>{variantPicker.loading?<p role="status">Loading variants…</p>:<div className="variant-options">{variantPicker.variants.map(variant=><button type="button" key={variant.id} disabled={variant.availableStock<1} onClick={()=>{const base=variantPicker.product;setVariantPicker(null);addProduct({...base,variantId:variant.id,selectedAttributes:variant.attributes,price:variant.sellingPriceIncludingGst,finalSellingPrice:variant.sellingPriceIncludingGst,offerPercentage:0,stock:variant.availableStock,availableStock:variant.availableStock,imageUrl:variant.imageUrl||base.imageUrl});}}>{variantSummary(variant.attributes)}<br />₹{variant.sellingPriceIncludingGst} · {variant.availableStock>0?`${variant.availableStock} available`:"Out of stock"}</button>)}</div>}</section></div>}

            {razorpayOrder && <RazorpayCheckout payment={razorpayOrder} internalId={billResponse?.billId} onSuccess={handleOfflineRazorpaySuccess} onDismiss={handleOfflineRazorpayDismiss} onError={handleOfflineRazorpayError} />}

            {/* =================================================
                NAVBAR
            ================================================= */}

            <header className="offline-navbar">

                <div className="offline-brand">

                    <h1>
                        Shiv<span>Hub</span>
                    </h1>

                    <p>
                        Seller POS Billing
                    </p>

                </div>

                <div className="offline-nav-actions">

                    <div className="offline-seller-info">

                        <span>
                            Seller
                        </span>

                        <strong>
                            {
                                user?.name ||
                                "Seller"
                            }
                        </strong>

                    </div>

                    <button
                        type="button"
                        className="offline-back-button"
                        onClick={() => navigate("/seller/offline-returns")}
                    >
                        POS Returns
                    </button>

                    <button
                        type="button"
                        className="offline-back-button"
                        onClick={
                            handleBack
                        }
                    >
                        ← Dashboard
                    </button>

                    <button
                        type="button"
                        className="offline-logout-button"
                        onClick={
                            handleLogout
                        }
                    >
                        Logout
                    </button>

                </div>

            </header>

            {/* =================================================
                MAIN
            ================================================= */}

            <main className="offline-main">

                {/* =================================================
                    PAGE HEADER
                ================================================= */}

                <section className="offline-page-header">

                    <div>

                        <span>
                            SHOP POS
                        </span>

                        <h2>
                            Create Offline Bill
                        </h2>

                        <p>
                            Create a bill for your
                            in-store customer.
                        </p>

                    </div>

                    <button
                        type="button"
                        className="new-bill-button"
                        onClick={
                            resetBill
                        }
                    >
                        + New Bill
                    </button>

                </section>

                {/* =================================================
                    ALERTS
                ================================================= */}

                {message && (

                    <div className="offline-success-message">

                        <span>
                            ✓
                        </span>

                        {message}

                    </div>

                )}

                {error && (

                    <div className="offline-error-message">

                        <span>
                            !
                        </span>

                        {error}

                    </div>

                )}

                {/* =================================================
                    BILL LAYOUT
                ================================================= */}

                <div className="offline-layout">

                    {/* =================================================
                        LEFT
                    ================================================= */}

                    <section className="offline-left">

                        {/* =================================================
                            PRODUCT SEARCH
                        ================================================= */}

                        <div className="offline-card">

                            <div className="offline-card-header">

                                <div>

                                    <span>
                                        PRODUCTS
                                    </span>

                                    <h3>
                                        Add Products
                                    </h3>

                                </div>

                                <span className="product-count">
                                    {
                                        cartItems.length
                                    }{" "}
                                    items
                                </span>

                            </div>

                            <form className="billing-scan-bar" onSubmit={handleScan}>
                                <label htmlFor="billing-scan-code">Scan IMEI / Barcode</label>
                                <div className="billing-scan-controls">
                                    <span className="billing-scan-icon" aria-hidden="true">▥</span>
                                    <SearchAutocomplete
                                        inputRef={scanInputRef}
                                        value={scanCode}
                                        onChange={setScanCode}
                                        onSelect={suggestion => {
                                            const product = products.find(item => String(item.id) === String(suggestion.id));
                                            if (product) addProduct(product);
                                            setScanCode("");
                                            focusScanInput();
                                        }}
                                        onEnterWithoutSelection={handleScan}
                                        fetchSuggestions={getSellerProductSearchSuggestions}
                                        scannerMode
                                        disabled={scanning}
                                        className="billing-scan-autocomplete"
                                        inputProps={{
                                            id: "billing-scan-code",
                                            type: "text",
                                            autoComplete: "off",
                                            "aria-label": "Scan IMEI or barcode"
                                        }}
                                        placeholder="Scan mobile IMEI, serial number, SKU or product barcode"
                                    />
                                    {scanCode && (
                                        <button type="button" className="scan-clear-button" onClick={() => { setScanCode(""); focusScanInput(); }}>
                                            Clear
                                        </button>
                                    )}
                                    <button type="submit" className="scan-add-button" disabled={scanning}>
                                        {scanning ? "Scanning…" : "Add"}
                                    </button>
                                </div>
                                <small>Exact IMEI/serial is checked first, then barcode and SKU.</small>
                            </form>

                            <div className="product-search">

                                <span>
                                    🔍
                                </span>

                                <SearchAutocomplete
                                    value={searchTerm}
                                    onChange={setSearchTerm}
                                    onSelect={suggestion => {
                                        const product = products.find(item => String(item.id) === String(suggestion.id));
                                        if (product) addProduct(product);
                                        setSearchTerm("");
                                    }}
                                    onEnterWithoutSelection={() => {}}
                                    fetchSuggestions={getSellerProductSearchSuggestions}
                                    className="offline-product-autocomplete"
                                    placeholder="Search product by name, category or ID..."
                                />

                            </div>

                            {/* =================================================
                                PRODUCTS
                            ================================================= */}

                            {loading ? (

                                <div className="offline-loading">
                                    Loading products...
                                </div>

                            ) : filteredProducts.length === 0 ? (

                                <div className="offline-empty">

                                    <div>
                                        📦
                                    </div>

                                    <p>
                                        No products found.
                                    </p>

                                </div>

                            ) : (

                                <div className="pos-product-grid">

                                    {filteredProducts.map(
                                        product => {

                                            const finalPrice =
                                                getFinalPrice(
                                                    product
                                                );

                                            const offer =
                                                getOfferPercentage(
                                                    product
                                                );

                                            const inCart =
                                                cartItems.find(
                                                    item =>
                                                        item.productId ===
                                                        product.id
                                                );

                                            const needsImei =
                                                isMobileProduct(
                                                    product
                                                );

                                            const imeiCount =
                                                getAvailableImeiCount(
                                                    product.id
                                                );

                                            return (

                                                <button
                                                    type="button"
                                                    className="pos-product-card"
                                                    key={
                                                        product.id
                                                    }
                                                    onClick={() =>
                                                        addProduct(
                                                            product
                                                        )
                                                    }
                                                >

                                                    <div className="pos-product-image">

                                                        {
                                                            product?.imageUrl
                                                                ? (
                                                                    <img
                                                                        src={
                                                                            product.imageUrl.startsWith(
                                                                                "http"
                                                                            )
                                                                                ? product.imageUrl
                                                                                : `${API_BASE_URL}/${product.imageUrl.replace(
                                                                                    /^\/+/,
                                                                                    ""
                                                                                )}`
                                                                        }
                                                                        alt={
                                                                            product.name
                                                                        }
                                                                    />
                                                                )
                                                                : "📦"
                                                        }

                                                    </div>

                                                    <div className="pos-product-info">

                                                        <strong>
                                                            {
                                                                product.name
                                                            }
                                                        </strong>

                                                        <span>
                                                            Stock:
                                                            {" "}
                                                            {
                                                                product.stock
                                                            }
                                                        </span>

                                                        <div className="pos-price-row">

                                                            <b>
                                                                ₹
                                                                {
                                                                    formatCurrency(
                                                                        finalPrice
                                                                    )
                                                                }
                                                            </b>

                                                            {
                                                                offer >
                                                                    0 && (

                                                                    <del>
                                                                        ₹
                                                                        {
                                                                            formatCurrency(
                                                                                product.price
                                                                            )
                                                                        }
                                                                    </del>

                                                                )
                                                            }

                                                        </div>

                                                        {
                                                            offer >
                                                                0 && (

                                                                <small>
                                                                    {
                                                                        offer
                                                                    }%
                                                                    OFF
                                                                </small>

                                                            )
                                                        }

                                                        {/* =================================================
                                                            IMEI BADGE
                                                        ================================================= */}

                                                        {
                                                            needsImei && (

                                                                <small className="imei-product-badge">
                                                                    📱 IMEI
                                                                    {
                                                                        imeiCount >
                                                                            0
                                                                            ? ` • ${imeiCount} available`
                                                                            : ""
                                                                    }
                                                                </small>

                                                            )
                                                        }

                                                        {
                                                            inCart && (

                                                                <em>
                                                                    In Bill:
                                                                    {" "}
                                                                    {
                                                                        inCart.quantity
                                                                    }
                                                                </em>

                                                            )
                                                        }

                                                    </div>

                                                </button>

                                            );

                                        }
                                    )}

                                </div>

                            )}

                        </div>

                        {/* =================================================
                            CART
                        ================================================= */}

                        <div className="offline-card">

                            <div className="offline-card-header">

                                <div>

                                    <span>
                                        BILL ITEMS
                                    </span>

                                    <h3>
                                        Current Bill
                                    </h3>

                                </div>

                            </div>

                            {cartItems.length === 0 ? (

                                <div className="cart-empty">

                                    <div>
                                        🛒
                                    </div>

                                    <h3>
                                        Bill is Empty
                                    </h3>

                                    <p>
                                        Click a product above
                                        to add it to the bill.
                                    </p>

                                </div>

                            ) : (

                                <div className="bill-items">

                                    {cartItems.map(
                                        item => {

                                            const gross =
                                                item.unitPrice *
                                                item.quantity;

                                            const itemTotal =
                                                Math.max(
                                                    0,
                                                    gross -
                                                    Number(
                                                        item.discount ||
                                                        0
                                                    )
                                                );

                                            const needsImei =
                                                isMobileProduct(
                                                    item.product
                                                );

                                            const imeiOptions =
                                                availableImeis[
                                                    item.productId
                                                ] || [];

                                            const selectedImeis =
                                                item.purchaseSerialIds ||
                                                [];

                                            const loadingImeis =
                                                imeiLoading[
                                                    item.productId
                                                ] === true;

                                            return (

                                                <div
                                                    className="bill-item"
                                                    key={
                                                        cartKeyFor(item)
                                                    }
                                                >

                                                    {/* =================================================
                                                        ITEM HEADER
                                                    ================================================= */}

                                                    <div className="bill-item-info">

                                                        <strong>
                                                            {
                                                                item.productName
                                                            }
                                                        </strong>

                                                        {item.scanned && <span className="scanned-chip">Scanned</span>}

                                                        {needsImei && <span className="imei-tracked-chip">IMEI tracked</span>}

                                                        {item.scannedImei && <small className="scanned-imei">IMEI: {item.scannedImei}</small>}
                                                        {item.selectedAttributes&&<small>{variantSummary(item.selectedAttributes)}</small>}

                                                        <small>

                                                            ₹
                                                            {
                                                                formatCurrency(
                                                                    item.unitPrice
                                                                )
                                                            }

                                                            {
                                                                item.offerPercentage >
                                                                    0
                                                                    ? ` • ${item.offerPercentage}% offer`
                                                                    : ""
                                                            }

                                                        </small>

                                                    </div>

                                                    {/* =================================================
                                                        QUANTITY
                                                    ================================================= */}

                                                    <div className="bill-quantity">

                                                        <button
                                                            type="button"
                                                            onClick={() =>
                                                                decreaseQuantity(
                                                                    cartKeyFor(item)
                                                                )
                                                            }
                                                        >
                                                            −
                                                        </button>

                                                        <strong>
                                                            {
                                                                item.quantity
                                                            }
                                                        </strong>

                                                        <button
                                                            type="button"
                                                            onClick={() =>
                                                                increaseQuantity(
                                                                    cartKeyFor(item)
                                                                )
                                                            }
                                                            disabled={Boolean(item.scannedSerialId)}
                                                        >
                                                            +
                                                        </button>

                                                    </div>

                                                    <div className="bill-item-unit-price">

                                                        <label>
                                                            Unit Price
                                                        </label>

                                                        <input
                                                            type="number"
                                                            min="0"
                                                            step="0.01"
                                                            value={item.unitPrice}
                                                            onChange={event => updateUnitPrice(cartKeyFor(item), event.target.value)}
                                                            aria-label={`Unit price for ${item.productName}`}
                                                        />

                                                    </div>

                                                    {/* =================================================
                                                        DISCOUNT
                                                    ================================================= */}

                                                    <div className="bill-item-discount">

                                                        <label>
                                                            Discount
                                                        </label>

                                                        <input
                                                            type="number"
                                                            min="0"
                                                            value={
                                                                item.discount
                                                            }
                                                            onChange={
                                                                event =>
                                                                    updateItemDiscount(
                                                                        cartKeyFor(item),
                                                                        event.target.value
                                                                    )
                                                            }
                                                        />

                                                    </div>

                                                    {/* =================================================
                                                        GST
                                                    ================================================= */}

                                                    <div className="bill-item-gst">

                                                        <label>
                                                            GST %
                                                        </label>

                                                        <input
                                                            type="number"
                                                            min="0"
                                                            max="100"
                                                            step="0.01"
                                                            value={
                                                                item.gstRate
                                                            }
                                                            onChange={
                                                                event =>
                                                                    updateGstRate(
                                                                        cartKeyFor(item),
                                                                        event.target.value
                                                                    )
                                                            }
                                                        />

                                                    </div>

                                                    {/* =================================================
                                                        TOTAL
                                                    ================================================= */}

                                                    <div className="bill-item-total">

                                                        <strong>
                                                            ₹
                                                            {
                                                                formatCurrency(
                                                                    itemTotal
                                                                )
                                                            }
                                                        </strong>

                                                        <button
                                                            type="button"
                                                            onClick={() =>
                                                                removeItem(
                                                                    cartKeyFor(item)
                                                                )
                                                            }
                                                        >
                                                            ×
                                                        </button>

                                                    </div>

                                                    {/* =================================================
                                                        IMEI SECTION
                                                    ================================================= */}

                                                    {
                                                        needsImei && !item.scannedSerialId && (

                                                            <div className="bill-imei-picker">

                                                                <div className="bill-imei-heading">

                                                                    <div>

                                                                        <strong>
                                                                            📱 IMEI Selection
                                                                        </strong>

                                                                        <small>
                                                                            {
                                                                                selectedImeis.length
                                                                            }
                                                                            /
                                                                            {
                                                                                item.quantity
                                                                            }
                                                                            {" "}
                                                                            selected
                                                                        </small>

                                                                    </div>

                                                                    <span>
                                                                        {
                                                                            imeiOptions.length
                                                                        }
                                                                        {" "}
                                                                        available
                                                                    </span>

                                                                </div>

                                                                {/* =================================================
                                                                    IMEI LOADING
                                                                ================================================= */}

                                                                {
                                                                    loadingImeis && (

                                                                        <div className="imei-loading">
                                                                            Loading available IMEIs...
                                                                        </div>

                                                                    )
                                                                }

                                                                {/* =================================================
                                                                    NO IMEI
                                                                ================================================= */}

                                                                {
                                                                    !loadingImeis &&
                                                                    imeiOptions.length ===
                                                                    0 && (

                                                                        <div className="imei-empty">

                                                                            <strong>
                                                                                ⚠ No available IMEI units
                                                                            </strong>

                                                                            <p>
                                                                                This product requires
                                                                                IMEI selection, but
                                                                                no available physical
                                                                                unit was found.
                                                                            </p>

                                                                        </div>

                                                                    )
                                                                }

                                                                {/* =================================================
                                                                    IMEI OPTIONS
                                                                ================================================= */}

                                                                {
                                                                    !loadingImeis &&
                                                                    imeiOptions.length >
                                                                    0 && (

                                                                        <div className="bill-imei-options">

                                                                            {
                                                                                imeiOptions.map(
                                                                                    serial => {

                                                                                        const serialId =
                                                                                            serial.serialId ??
                                                                                            serial.id;

                                                                                        const selected =
                                                                                            selectedImeis.includes(
                                                                                                serialId
                                                                                            );

                                                                                        return (

                                                                                            <label
                                                                                                key={
                                                                                                    serialId
                                                                                                }
                                                                                                className={
                                                                                                    `bill-imei-option ${
                                                                                                        selected
                                                                                                            ? "selected"
                                                                                                            : ""
                                                                                                    }`
                                                                                                }
                                                                                            >

                                                                                                <input
                                                                                                    type="checkbox"
                                                                                                    checked={
                                                                                                        selected
                                                                                                    }
                                                                                                    onChange={() =>
                                                                                                        toggleImei(
                                                                                                            cartKeyFor(item),
                                                                                                            serialId
                                                                                                        )
                                                                                                    }
                                                                                                />

                                                                                                <div className="imei-option-content">

                                                                                                    <strong>
                                                                                                        {
                                                                                                            selected
                                                                                                                ? "✓ "
                                                                                                                : ""
                                                                                                        }
                                                                                                        IMEI Unit
                                                                                                    </strong>

                                                                                                    <span>
                                                                                                        IMEI 1:
                                                                                                        {" "}
                                                                                                        {
                                                                                                            serial.imei1 ||
                                                                                                            "N/A"
                                                                                                        }
                                                                                                    </span>

                                                                                                    {
                                                                                                        serial.imei2 && (

                                                                                                            <span>
                                                                                                                IMEI 2:
                                                                                                                {" "}
                                                                                                                {
                                                                                                                    serial.imei2
                                                                                                                }
                                                                                                            </span>

                                                                                                        )
                                                                                                    }

                                                                                                    {
                                                                                                        serial.serialNumber && (

                                                                                                            <span>
                                                                                                                Serial:
                                                                                                                {" "}
                                                                                                                {
                                                                                                                    serial.serialNumber
                                                                                                                }
                                                                                                            </span>

                                                                                                        )
                                                                                                    }

                                                                                                </div>

                                                                                            </label>

                                                                                        );

                                                                                    }
                                                                                )
                                                                            }

                                                                        </div>

                                                                    )
                                                                }

                                                                {/* =================================================
                                                                    IMEI WARNING
                                                                ================================================= */}

                                                                {
                                                                    selectedImeis.length !==
                                                                    item.quantity && (

                                                                        <div className="imei-selection-warning">

                                                                            ⚠ Please select exactly
                                                                            {" "}
                                                                            {
                                                                                item.quantity
                                                                            }
                                                                            {" "}
                                                                            IMEI
                                                                            {
                                                                                item.quantity >
                                                                                1
                                                                                    ? "s"
                                                                                    : ""
                                                                            }
                                                                            {" "}
                                                                            before generating the bill.

                                                                        </div>

                                                                    )
                                                                }

                                                                {
                                                                    selectedImeis.length ===
                                                                    item.quantity && (

                                                                        <div className="imei-selection-success">

                                                                            ✓
                                                                            {" "}
                                                                            {
                                                                                item.quantity
                                                                            }
                                                                            {" "}
                                                                            IMEI
                                                                            {
                                                                                item.quantity >
                                                                                1
                                                                                    ? "s"
                                                                                    : ""
                                                                            }
                                                                            {" "}
                                                                            selected for this sale.

                                                                        </div>

                                                                    )
                                                                }

                                                            </div>

                                                        )
                                                    }

                                                </div>

                                            );

                                        }
                                    )}

                                </div>

                            )}

                        </div>

                    </section>

                    {/* =================================================
                        RIGHT
                    ================================================= */}

                    <aside className="offline-right">

                        {/* =================================================
                            CUSTOMER
                        ================================================= */}

                        <div className="offline-card">

                            <div className="offline-card-header">

                                <div>

                                    <span>
                                        CUSTOMER
                                    </span>

                                    <h3>
                                        Customer Details
                                    </h3>

                                </div>

                            </div>

                            <div className="customer-type-tabs">

                                <button
                                    type="button"
                                    className={
                                        customerType ===
                                            "WALK_IN"
                                            ? "active"
                                            : ""
                                    }
                                    onClick={() => {

                                        setCustomerType(
                                            "WALK_IN"
                                        );

                                        setCustomerId("");

                                        setCustomerProfileId("");

                                        setCustomerName(
                                            "Walk-in Customer"
                                        );

                                        setWhatsappConsent(false);
                                    }}
                                >
                                    Walk-in
                                </button>

                                <button
                                    type="button"
                                    className={
                                        customerType ===
                                            "REGISTERED"
                                            ? "active"
                                            : ""
                                    }
                                    onClick={() => {

                                        setCustomerType(
                                            "REGISTERED"
                                        );

                                        setCustomerId("");
                                        setCustomerProfileId("");
                                        setCustomerLookupMessage("");
                                        setCustomerName("");
                                        setCustomerMobile("");
                                        setCustomerEmail("");
                                        setCustomerAddress("");
                                        setCustomerCity("");
                                        setCustomerDistrict("");
                                        setCustomerState("");
                                        setCustomerPincode("");
                                        setCustomerDateOfBirth("");
                                        setWhatsappConsent(false);
                                        setAvailableLoyaltyPoints(0);
                                        setLoyaltyPointsToRedeem(0);
                                    }}
                                >
                                    Registered Customer
                                </button>

                            </div>

                            {
                                customerType ===
                                    "REGISTERED" && (

                                    <small className="salesperson-help">
                                        Search by mobile number. The customer identity is securely set from the lookup result.
                                    </small>

                                )
                            }

                            <div className="form-field customer-name-search-field">

                                <label>
                                    Customer Name
                                </label>

                                {customerType === "REGISTERED" ? (
                                    <>
                                        <SearchAutocomplete
                                            value={customerName}
                                            onChange={value => {
                                                setCustomerName(value);
                                                setCustomerLookupMessage("");
                                            }}
                                            onSelect={selectRegisteredCustomer}
                                            fetchSuggestions={getSellerCustomerSearchSuggestions}
                                            className="offline-customer-autocomplete"
                                            placeholder="Search customer by name"
                                            inputProps={{
                                                type: "text",
                                                "aria-label": "Search registered customer by name"
                                            }}
                                        />
                                        <small className="customer-search-helper">Start typing a name, then select the matching customer to securely fill their billing details.</small>
                                    </>
                                ) : (
                                    <input
                                        type="text"
                                        placeholder="Customer name"
                                        value={
                                            customerName
                                        }
                                        onChange={
                                            event =>
                                                setCustomerName(
                                                    event.target.value
                                                )
                                        }
                                    />
                                )}

                            </div>

                            {
                                paymentMethod !== "FINANCE" &&
                                paidAmount <
                                    payableGrandTotal &&
                                payableGrandTotal > 0 && (

                                    <div className="form-field">

                                        <label>
                                            Balance Payment Promise Date
                                        </label>

                                        <input
                                            type="date"
                                            value={
                                                balanceDueDate
                                            }
                                            onChange={
                                                event =>
                                                    setBalanceDueDate(
                                                        event.target.value
                                                    )
                                            }
                                        />

                                        <small>
                                            A full invoice will be issued. The
                                            remaining balance statement will be
                                            emailed to the customer.
                                        </small>

                                    </div>

                                )
                            }

                            <div className="form-row">

                                <div className="form-field customer-lookup-field">

                                    <label>
                                        Mobile
                                    </label>

                                    <div className="customer-lookup-search-row">
                                        <SearchAutocomplete
                                            value={customerMobile}
                                            onChange={value => {
                                                setCustomerMobile(value);
                                                setWhatsappConsent(false);
                                            }}
                                            onSelect={selectRegisteredCustomer}
                                            onEnterWithoutSelection={() => lookupRegisteredCustomer(customerMobile)}
                                            fetchSuggestions={getSellerCustomerSearchSuggestions}
                                            className="offline-customer-autocomplete"
                                            placeholder="Mobile, name or email"
                                            inputProps={{
                                                type: "text",
                                                onBlur: event => lookupRegisteredCustomer(event.target.value),
                                                "aria-label": "Search registered customer"
                                            }}
                                        />

                                        <button
                                            type="button"
                                            className="btn-secondary customer-lookup-button"
                                            onClick={() => lookupRegisteredCustomer(customerMobile)}
                                            disabled={customerLookupLoading}
                                        >
                                            {customerLookupLoading ? "Searching..." : "Search customer"}
                                        </button>
                                    </div>

                                    {customerLookupMessage && (

                                        <small className={`customer-lookup-status ${customerId ? "is-found" : ""}`} role="status">
                                            {customerLookupMessage}
                                        </small>
                                    )}

                                </div>

                                <div className="form-field">

                                    <label>
                                        Email
                                    </label>

                                    <input
                                        type="email"
                                        placeholder="Email"
                                        value={
                                            customerEmail
                                        }
                                        onChange={
                                            event =>
                                                setCustomerEmail(
                                                    event.target.value
                                                )
                                        }
                                    />

                                </div>

                            </div>

                            <div className="form-field">

                                <label>
                                    Address
                                </label>

                                <textarea
                                    rows="2"
                                    placeholder="Customer complete address"
                                    value={
                                        customerAddress
                                    }
                                    onChange={
                                        event =>
                                            setCustomerAddress(
                                                event.target.value
                                            )
                                    }
                                />

                            </div>

                            <>
                                <div className="form-row">
                                    <div className="form-field">
                                        <label>State</label>
                                        <select
                                            value={customerState}
                                            onChange={event => {
                                                setCustomerState(event.target.value);
                                                setCustomerDistrict("");
                                                setCustomerCity("");
                                            }}
                                        >
                                            <option value="">Select state</option>
                                            {customerState && !locationStates.includes(customerState) && <option value={customerState}>{customerState}</option>}
                                            {locationStates.map(state => <option key={state} value={state}>{state}</option>)}
                                        </select>
                                    </div>
                                    <div className="form-field">
                                        <label>District</label>
                                        <select
                                            value={customerDistrict}
                                            disabled={!customerState}
                                            onChange={event => {
                                                setCustomerDistrict(event.target.value);
                                                setCustomerCity("");
                                            }}
                                        >
                                            <option value="">Select district</option>
                                            {customerDistrict && !locationDistricts.includes(customerDistrict) && <option value={customerDistrict}>{customerDistrict}</option>}
                                            {locationDistricts.map(district => <option key={district} value={district}>{district}</option>)}
                                        </select>
                                    </div>
                                </div>
                                <div className="form-row">
                                    <div className="form-field">
                                        <label>City</label>
                                        <select
                                            value={customerCity}
                                            disabled={!customerDistrict}
                                            onChange={event => setCustomerCity(event.target.value)}
                                        >
                                            <option value="">Select city</option>
                                            {customerCity && !locationCities.includes(customerCity) && <option value={customerCity}>{customerCity}</option>}
                                            {locationCities.map(city => <option key={city} value={city}>{city}</option>)}
                                        </select>
                                    </div>
                                    <div className="form-field">
                                        <label>Pincode</label>
                                        <input value={customerPincode} inputMode="numeric" maxLength="6" onChange={event => setCustomerPincode(event.target.value.replace(/\D/g, ""))} />
                                    </div>
                                </div>
                                {customerType === "REGISTERED" && (
                                    <div className="form-field">
                                        <label>Date of birth</label>
                                        <input type="date" value={customerDateOfBirth} onChange={event => setCustomerDateOfBirth(event.target.value)} />
                                    </div>
                                )}
                            </>

                            <div className="form-field">

                                <label>
                                    Salesperson
                                </label>

                                <select
                                    value={salesPersonId}
                                    onChange={event => setSalesPersonId(event.target.value)}
                                >
                                    <option value="">
                                        Owner / self sale
                                    </option>

                                    {salespeople.map(staff => (
                                        <option key={staff.id} value={staff.id}>
                                            {staff.name} {staff.shopName ? `• ${staff.shopName}` : ""}
                                        </option>
                                    ))}
                                </select>

                                <small className="salesperson-help">
                                    This name is saved on the invoice and staff-wise sales report.
                                </small>

                            </div>

                            <label className="offline-whatsapp-consent">
                                <input
                                    type="checkbox"
                                    checked={whatsappConsent}
                                    disabled={!/^[6-9][0-9]{9}$/.test(String(customerMobile || "").replace(/\D/g, "").slice(-10))}
                                    onChange={event => setWhatsappConsent(event.target.checked)}
                                />
                                <span>
                                    Customer agrees to receive this invoice and future updates on WhatsApp.
                                </span>
                            </label>
                            <small className="offline-whatsapp-consent-help">
                                This is optional. A valid mobile number and the customer’s consent are required.
                            </small>

                            <div className="form-field">

                                <label>
                                    GSTIN
                                </label>

                                <input
                                    type="text"
                                    placeholder="Optional GSTIN"
                                    value={
                                        customerGstin
                                    }
                                    onChange={
                                        event =>
                                            setCustomerGstin(
                                                event.target.value
                                            )
                                    }
                                />

                            </div>

                            {customerGstin.trim() && <>
                                <div className="form-field">
                                    <label>Legal name</label>
                                    <input type="text" maxLength="180" placeholder="As per GST registration" value={customerLegalName} onChange={event => setCustomerLegalName(event.target.value)} />
                                </div>
                                <div className="form-field">
                                    <label>Trade name</label>
                                    <input type="text" maxLength="180" placeholder="Optional trade name" value={customerTradeName} onChange={event => setCustomerTradeName(event.target.value)} />
                                </div>
                                <div className="form-field">
                                    <label>Place of supply</label>
                                    <input type="text" maxLength="80" placeholder="State or two-digit state code" value={placeOfSupply} onChange={event => setPlaceOfSupply(event.target.value)} />
                                </div>
                            </>}

                        </div>

                        {/* =================================================
                            BILL SUMMARY
                        ================================================= */}

                        <div className="offline-card">

                            <div className="offline-card-header">

                                <div>

                                    <span>
                                        SUMMARY
                                    </span>

                                    <h3>
                                        Bill Summary
                                    </h3>

                                </div>

                            </div>

                            <p className="summary-editing-note">
                                Unit prices are GST-inclusive. Subtotal shows the GST-exclusive taxable value; adding CGST and SGST brings the grand total back to the selling price.
                            </p>

                            <div className="summary-row">

                                <span>
                                    Subtotal (excl. GST)
                                </span>

                                <strong>
                                    ₹
                                    {
                                        formatCurrency(
                                            billCalculation.subtotal
                                        )
                                    }
                                </strong>

                            </div>

                            <div className="summary-row">

                                <span>
                                    Item Discount (excl. GST)
                                </span>

                                <strong>
                                    − ₹
                                    {
                                        formatCurrency(
                                            billCalculation.itemDiscount
                                        )
                                    }
                                </strong>

                            </div>

                            <div className="summary-input-row">

                                <label>
                                    Bill Discount (incl. GST)
                                </label>

                                <input
                                    type="number"
                                    min="0"
                                    value={
                                        discount
                                    }
                                    onChange={
                                        event =>
                                            setDiscount(
                                                event.target.value
                                            )
                                    }
                                />

                            </div>

                            {customerType === "REGISTERED" && <div className="summary-input-row loyalty-redemption-row">
                                <label>
                                    <span>Loyalty points <strong>{availableLoyaltyPoints} available</strong></span>
                                    <small>3 points = Rs. 1 · Redeem up to {maximumRedeemablePoints} points (Rs. {formatCurrency(maximumRedeemablePoints / LOYALTY_POINTS_PER_RUPEE)})</small>
                                </label>
                                <input
                                    type="number"
                                    min="0"
                                    step={LOYALTY_POINTS_PER_RUPEE}
                                    max={maximumRedeemablePoints}
                                    value={redeemedLoyaltyPoints}
                                    onChange={event => {
                                        const requested = Math.max(0, Math.min(maximumRedeemablePoints, Number(event.target.value || 0)));
                                        setLoyaltyPointsToRedeem(Math.floor(requested / LOYALTY_POINTS_PER_RUPEE) * LOYALTY_POINTS_PER_RUPEE);
                                    }}
                                />
                            </div>}

                            {redeemedLoyaltyPoints > 0 && <div className="summary-row loyalty-discount-row">
                                <span>Loyalty reward discount ({redeemedLoyaltyPoints} points)</span>
                                <strong>− Rs. {formatCurrency(loyaltyDiscountPreview)}</strong>
                            </div>}

                            <div className="summary-row">

                                <span>
                                    CGST
                                </span>

                                <strong>
                                    ₹
                                    {
                                        formatCurrency(
                                            billCalculation.cgst
                                        )
                                    }
                                </strong>

                            </div>

                            <div className="summary-row">

                                <span>
                                    SGST
                                </span>

                                <strong>
                                    ₹
                                    {
                                        formatCurrency(
                                            billCalculation.sgst
                                        )
                                    }
                                </strong>

                            </div>

                            <div className="summary-input-row">

                                <label>
                                    Delivery Charge
                                </label>

                                <input
                                    type="number"
                                    min="0"
                                    value={
                                        deliveryCharge
                                    }
                                    onChange={
                                        event =>
                                            setDeliveryCharge(
                                                event.target.value
                                            )
                                    }
                                />

                            </div>

                            <div className="summary-total">

                                <span>
                                    GRAND TOTAL
                                </span>

                                <strong>
                                    ₹
                                    {
                                        formatCurrency(
                                            payableGrandTotal
                                        )
                                    }
                                </strong>

                            </div>

                        </div>

                        {/* =================================================
                            PAYMENT
                        ================================================= */}

                        <div className="offline-card">

                            <div className="offline-card-header">

                                <div>

                                    <span>
                                        PAYMENT
                                    </span>

                                    <h3>
                                        Payment Details
                                    </h3>

                                </div>

                            </div>

                            <div className="form-field">

                                <label>
                                    Payment Method
                                </label>

                                <select
                                    value={
                                        paymentMethod
                                    }
                                    onChange={
                                        event =>
                                            setPaymentMethod(
                                                event.target.value
                                            )
                                    }
                                >

                                    <option value="CASH">
                                        Cash
                                    </option>

                                    <option value="UPI">
                                        UPI
                                    </option>

                                    <option value="CARD">
                                        Card
                                    </option>

                                    <option value="FINANCE">Finance / EMI</option>

                                    <option value="BANK_TRANSFER">
                                        Bank Transfer
                                    </option>

                                    <option value="RAZORPAY">
                                        Razorpay Online (UPI / Card / Net Banking)
                                    </option>

                                </select>

                            </div>

                            <div className="form-field">

                                <label>
                                    {paymentMethod === "FINANCE" ? "Downpayment (edit below)" : paymentMethod === "RAZORPAY" ? "Online Amount" : "Amount Received"}
                                </label>

                                <input
                                    type="number"
                                    min="0"
                                    step="0.01"
                                    placeholder={paymentMethod === "RAZORPAY" ? "Full bill amount is collected online" : "Enter amount"}
                                    disabled={paymentMethod === "RAZORPAY" || paymentMethod === "FINANCE"}
                                    value={
                                        paymentMethod === "FINANCE" ? finance.downpayment : paymentAmount
                                    }
                                    onChange={
                                        event =>
                                            setPaymentAmount(
                                                event.target.value
                                            )
                                    }
                                />

                            </div>

                            {
                                requiresTransactionId && (

                                    <div className="form-field">

                                        <label>
                                            Transaction ID
                                        </label>

                                        <input
                                            type="text"
                                            placeholder="Enter transaction ID"
                                            value={
                                                transactionId
                                            }
                                            onChange={
                                                event =>
                                                    setTransactionId(
                                                        event.target.value
                                                    )
                                            }
                                        />

                                    </div>

                                )
                            }

                            {paymentMethod === "FINANCE" && <FinanceDetails value={finance} onChange={setFinance} total={payableGrandTotal} />}

                            <div className="payment-status-box">

                                {
                                    paidAmount >=
                                        payableGrandTotal &&
                                        payableGrandTotal >
                                        0
                                        ? (

                                            <>

                                                <span>
                                                    Change to Customer
                                                </span>

                                                <strong>
                                                    ₹
                                                    {
                                                        formatCurrency(
                                                            changeAmount
                                                        )
                                                    }
                                                </strong>

                                            </>

                                        )
                                        : (

                                            <>

                                                <span>
                                                    {paymentMethod === "FINANCE" ? "Provider gross balance" : "Balance Required"}
                                                </span>

                                                <strong>
                                                    ₹
                                                    {
                                                        formatCurrency(
                                                            remainingAmount
                                                        )
                                                    }
                                                </strong>

                                            </>

                                        )
                                }

                            </div>

                            <div className="form-field">

                                <label>
                                    Notes
                                </label>

                                <textarea
                                    rows="3"
                                    placeholder="Optional billing notes..."
                                    value={
                                        notes
                                    }
                                    onChange={
                                        event =>
                                            setNotes(
                                                event.target.value
                                            )
                                    }
                                />

                            </div>

                            <button
                                type="button"
                                className="generate-bill-button"
                                onClick={
                                    handleGenerateBill
                                }
                                disabled={
                                    generatingBill ||
                                    cartItems.length ===
                                    0
                                }
                            >

                                {
                                    generatingBill
                                        ? "Generating Bill..."
                                        : "🧾 Generate Bill"
                                }

                            </button>

                        </div>

                        {/* =================================================
                            SUCCESS BILL
                        ================================================= */}

                        {
                            billResponse && (

                                <div className="generated-bill-card">

                                    <div className="generated-bill-icon">
                                        ✓
                                    </div>

                                    <h3>
                                        Bill Generated
                                    </h3>

                                    <p>
                                        Bill Number
                                    </p>

                                    <strong>
                                        {
                                            billResponse.billNumber
                                        }
                                    </strong>

                                    <div className="generated-bill-total">

                                        <span>
                                            Grand Total
                                        </span>

                                        <b>
                                            ₹
                                            {
                                                formatCurrency(
                                                    billResponse.grandTotal
                                                )
                                            }
                                        </b>

                                    </div>

                                    <SellerOfflinePaymentPanel
                                        bill={billResponse}
                                        onRazorpay={setRazorpayOrder}
                                        onUpdated={applyOfflinePaymentSummary}
                                    />

                                    <button
                                        type="button"
                                        onClick={downloadGeneratedInvoice}
                                    >
                                        View / Print Invoice
                                    </button>

                                    <button
                                        type="button"
                                        onClick={
                                            resetBill
                                        }
                                    >
                                        Create New Bill
                                    </button>

                                </div>

                            )
                        }

                    </aside>

                </div>

            </main>

        </div>
        </div>
    );
};

export default OfflineBilling;

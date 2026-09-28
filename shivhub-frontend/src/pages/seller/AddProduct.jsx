import { useRef, useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "./AddProduct.css";
import "./PremiumPolish.css";
import { getMobileSpecifications } from "../../services/mobileSpecificationService";
import { API_BASE_URL } from "../../services/api";


const MIN_PRODUCT_IMAGES = 5;
const MAX_PRODUCT_IMAGES = 15;

const categorySpecificationFields = {
    LAPTOP: ["Processor", "Generation", "Storage type", "Storage capacity", "Screen size", "Display type", "Graphics", "Operating system", "Battery", "Ports", "Connectivity", "Keyboard type", "Warranty"],
    TV: ["Screen size", "Display technology", "Resolution", "Refresh rate", "Smart TV OS", "HDR", "Sound output", "HDMI ports", "USB ports", "Wi-Fi", "Bluetooth", "Wall mount included", "Warranty"],
    ACCESSORY: ["Compatible devices", "Connector/type", "Material", "Warranty"]
};

const createEmptyImage = () => ({
    type: "",
    file: null,
    url: "",
    preview: ""
});


const createEmptyImages = () => [
    createEmptyImage(),
    createEmptyImage(),
    createEmptyImage(),
    createEmptyImage(),
    createEmptyImage()
];


const AddProduct = () => {

    const navigate = useNavigate();


    /*
     * =====================================================
     * PRODUCT FORM
     * =====================================================
     */

    const [formData, setFormData] = useState({
        name: "",
        description: "",
        brand: "",
        model: "",
        modelNumber: "",
        ram: "",
        storage: "",
        colorOptions: "",
        compatibility: "",
        warrantyDetails: "",
        packageContents: "",
        hsnCode: "",
        sellerSku: "",
        barcode: "",
        gstRate: "18",
        shortHighlights: "",
        specificationDetails: "",
        price: "",
        stock: "",
        categoryId: "",
        subCategoryId: ""
    });


    /*
     * =====================================================
     * CATEGORIES
     * =====================================================
     */

    const [categories, setCategories] = useState([]);
    const [dynamicSpecs, setDynamicSpecs] = useState({});

    const [subCategories, setSubCategories] =
        useState([]);

    const [mobileSpecifications, setMobileSpecifications] =
        useState([]);

    const [specSearch, setSpecSearch] =
        useState("");

    const [productMode, setProductMode] =
        useState("CATALOGUE");

    const [catalogueSearch, setCatalogueSearch] =
        useState("");

    const [catalogueProducts, setCatalogueProducts] =
        useState([]);

    const [myProducts, setMyProducts] =
        useState([]);

    const [selectedCatalogueProduct, setSelectedCatalogueProduct] =
        useState(null);

    const [catalogueLoading, setCatalogueLoading] =
        useState(false);


    /*
     * =====================================================
     * PRODUCT IMAGE SLOTS
     * =====================================================
     *
     * Each image can be:
     *
     * FILE
     *
     * OR
     *
     * URL
     *
     */

    const [images, setImages] =
        useState(createEmptyImages());


    /*
     * =====================================================
     * LOADING
     * =====================================================
     */

    const [loadingCategories, setLoadingCategories] =
        useState(false);

    const [loadingSubCategories, setLoadingSubCategories] =
        useState(false);

    const [submitting, setSubmitting] =
        useState(false);


    /*
     * =====================================================
     * MESSAGES
     * =====================================================
     */

    const [message, setMessage] =
        useState("");

    const [error, setError] =
        useState("");


    /*
     * =====================================================
     * TOKEN
     * =====================================================
     */

    const getToken = () => {

        return localStorage.getItem(
            "shivhub_token"
        );
    };


    /*
     * =====================================================
     * LOAD CATEGORIES
     * =====================================================
     */

    useEffect(() => {

        let cancelled = false;


        const loadCategories = async () => {

            try {

                setLoadingCategories(true);
                setError("");


                const token = getToken();


                if (!token) {

                    setError(
                        "Login session not found. Please login again."
                    );

                    return;
                }


                const response =
                    await axios.get(
                        `${API_BASE_URL}/api/categories`,
                        {
                            headers: {
                                Authorization:
                                    `Bearer ${token}`
                            }
                        }
                    );


                if (!cancelled) {

                    setCategories(
                        Array.isArray(
                            response.data
                        )
                            ? response.data
                            : []
                    );
                }

            } catch (err) {

                console.error(
                    "Category loading error:",
                    err
                );


                if (!cancelled) {

                    setCategories([]);

                    setError(
                        err?.response?.data?.message ||
                        err?.response?.data?.error ||
                        "Categories could not be loaded."
                    );
                }

            } finally {

                if (!cancelled) {

                    setLoadingCategories(false);
                }
            }
        };


        loadCategories();


        return () => {

            cancelled = true;
        };

    }, []);


    useEffect(() => {
        let cancelled = false;

        const loadCatalogue = async () => {
            try {
                setCatalogueLoading(true);
                const token = getToken();
                const response = await axios.get(
                    `${API_BASE_URL}/api/products/catalogue`,
                    {
                        params: { q: catalogueSearch },
                        headers: token ? { Authorization: `Bearer ${token}` } : {}
                    }
                );
                if (!cancelled) {
                    setCatalogueProducts(Array.isArray(response.data) ? response.data : []);
                }
            } catch {
                if (!cancelled) {
                    setCatalogueProducts([]);
                }
            } finally {
                if (!cancelled) {
                    setCatalogueLoading(false);
                }
            }
        };

        const timer = setTimeout(loadCatalogue, 250);
        return () => {
            cancelled = true;
            clearTimeout(timer);
        };
    }, [catalogueSearch]);

    useEffect(() => {
        let cancelled = false;

        const loadMyProducts = async () => {
            try {
                const token = getToken();
                if (!token) return;
                const response = await axios.get(
                    `${API_BASE_URL}/api/products/seller`,
                    { headers: { Authorization: `Bearer ${token}` } }
                );
                if (!cancelled) {
                    setMyProducts(Array.isArray(response.data) ? response.data : []);
                }
            } catch {
                if (!cancelled) {
                    setMyProducts([]);
                }
            }
        };

        loadMyProducts();
        return () => {
            cancelled = true;
        };
    }, []);

    useEffect(() => {

        let cancelled = false;

        const loadMobileSpecifications = async () => {

            try {

                const data =
                    await getMobileSpecifications();

                if (!cancelled) {

                    setMobileSpecifications(
                        Array.isArray(data)
                            ? data
                            : []
                    );
                }

            } catch {

                if (!cancelled) {

                    setMobileSpecifications([]);
                }
            }
        };

        loadMobileSpecifications();

        return () => {

            cancelled = true;
        };

    }, []);


    /*
     * =====================================================
     * NORMAL INPUT
     * =====================================================
     */

    const handleChange = (event) => {

        const {
            name,
            value
        } = event.target;


        setFormData(previous => ({
            ...previous,
            [name]: value
        }));


        setError("");
        setMessage("");
    };


    /*
     * =====================================================
     * MOBILE SPECIFICATION AUTOFILL
     * =====================================================
     */

    const mobileSpecName = spec =>
        [
            spec.brand,
            spec.modelName,
            spec.variantName,
            spec.ram,
            spec.storage
        ]
            .filter(Boolean)
            .join(" ");

    const firstColor = spec =>
        String(spec.colorOptions || "")
            .split(",")
            .map(value => value.trim())
            .filter(Boolean)[0] || "";

    const combinedSpecificationDetails = spec =>
        [
            spec.displayDetails && `Display: ${spec.displayDetails}`,
            spec.processor && `Processor: ${spec.processor}`,
            spec.cameraDetails && `Camera: ${spec.cameraDetails}`,
            spec.batteryDetails && `Battery: ${spec.batteryDetails}`,
            spec.osDetails && `OS: ${spec.osDetails}`,
            spec.connectivityDetails && `Connectivity: ${spec.connectivityDetails}`,
            spec.otherDetails
        ]
            .filter(Boolean)
            .join("\n");

    const visibleMobileSpecifications = () => {

        const query =
            String(specSearch || formData.name || formData.brand)
                .trim()
                .toLowerCase();

        return mobileSpecifications
            .filter(spec => {

                if (!query) {

                    return true;
                }

                return mobileSpecName(spec)
                    .toLowerCase()
                    .includes(query);
            })
            .slice(0, 8);
    };

    const applyMobileSpecification = spec => {

        const specs =
            combinedSpecificationDetails(spec);

        setFormData(previous => ({
            ...previous,
            name:
                mobileSpecName(spec) ||
                previous.name,
            brand:
                spec.brand ||
                previous.brand,
            model:
                spec.modelName ||
                previous.model,
            ram:
                spec.ram ||
                previous.ram,
            storage:
                spec.storage ||
                previous.storage,
            colorOptions:
                spec.colorOptions ||
                firstColor(spec) ||
                previous.colorOptions,
            hsnCode:
                spec.hsnCode ||
                previous.hsnCode ||
                "85171300",
            specificationDetails:
                specs ||
                previous.specificationDetails
        }));

        setSpecSearch("");
        setMessage(
            "Saved mobile specifications applied. You can still edit manually."
        );
        setError("");
    };


    /*
     * =====================================================
     * CATEGORY CHANGE
     * =====================================================
     */

    const handleCategoryChange =
        async (event) => {

            const categoryId =
                event.target.value;


            setFormData(previous => ({
                ...previous,
                categoryId,
                subCategoryId: ""
            }));


            setSubCategories([]);
            setDynamicSpecs({});

            setError("");
            setMessage("");


            if (!categoryId) {

                return;
            }


            try {

                setLoadingSubCategories(true);


                const token = getToken();


                if (!token) {

                    throw new Error(
                        "Login session not found."
                    );
                }


                const response =
                    await axios.get(
                        `${API_BASE_URL}/api/categories/${categoryId}/subcategories`,
                        {
                            headers: {
                                Authorization:
                                    `Bearer ${token}`
                            }
                        }
                    );


                setSubCategories(
                    Array.isArray(
                        response.data
                    )
                        ? response.data
                        : []
                );

            } catch (err) {

                console.error(
                    "Subcategory loading error:",
                    err
                );


                setError(
                    err?.response?.data?.message ||
                    err?.response?.data?.error ||
                    "Subcategories could not be loaded."
                );

            } finally {

                setLoadingSubCategories(false);
            }
        };


    /*
     * =====================================================
     * IMAGE TYPE
     * =====================================================
     */

    const changeImageType =
        (index, type) => {

            setImages(previous => {

                const updated =
                    [...previous];


                if (updated[index].preview) {

                    URL.revokeObjectURL(
                        updated[index].preview
                    );
                }


                updated[index] = {
                    type,
                    file: null,
                    url: "",
                    preview: ""
                };


                return updated;
            });


            setError("");
            setMessage("");
        };


    /*
     * =====================================================
     * COMPUTER IMAGE
     * =====================================================
     */

    const handleFileChange =
        (index, event) => {

            const file =
                event.target.files?.[0];


            if (!file) {

                return;
            }


            const allowedTypes = [
                "image/jpeg",
                "image/png",
                "image/webp"
            ];


            if (
                !allowedTypes.includes(
                    file.type
                )
            ) {

                setError(
                    "Only JPG, PNG and WEBP images are allowed."
                );

                event.target.value = "";

                return;
            }


            if (
                file.size >
                10 * 1024 * 1024
            ) {

                setError(
                    `${file.name} is larger than 10MB.`
                );

                event.target.value = "";

                return;
            }


            const preview =
                URL.createObjectURL(file);


            setImages(previous => {

                const updated =
                    [...previous];


                if (updated[index].preview) {

                    URL.revokeObjectURL(
                        updated[index].preview
                    );
                }


                updated[index] = {
                    type: "FILE",
                    file,
                    url: "",
                    preview
                };


                return updated;
            });


            setError("");
            setMessage("");
        };


    /*
     * =====================================================
     * IMAGE URL
     * =====================================================
     */

    const handleUrlChange =
        (index, value) => {

            setImages(previous => {

                const updated =
                    [...previous];


                if (updated[index].preview) {

                    /*
                     * Only revoke blob URLs.
                     */
                    if (
                        updated[index].preview.startsWith(
                            "blob:"
                        )
                    ) {

                        URL.revokeObjectURL(
                            updated[index].preview
                        );
                    }
                }


                updated[index] = {
                    type:
                        value.trim()
                            ? "URL"
                            : "",
                    file: null,
                    url: value,
                    preview:
                        value.trim()
                            ? value.trim()
                            : ""
                };


                return updated;
            });


            setError("");
            setMessage("");
        };


    /*
     * =====================================================
     * CLEAR IMAGE
     * =====================================================
     */

    const clearImage =
        (index) => {

            setImages(previous => {

                const updated =
                    [...previous];


                if (
                    updated[index].preview &&
                    updated[index].preview.startsWith(
                        "blob:"
                    )
                ) {

                    URL.revokeObjectURL(
                        updated[index].preview
                    );
                }


                updated[index] =
                    createEmptyImage();


                return updated;
            });


            const input =
                document.getElementById(
                    `product-image-${index}`
                );


            if (input) {

                input.value = "";
            }


            setError("");
            setMessage("");
        };


    const addImageSlot = () => {

        setImages(previous => {

            if (previous.length >= MAX_PRODUCT_IMAGES) {
                return previous;
            }

            return [
                ...previous,
                createEmptyImage()
            ];
        });

        setError("");
        setMessage("");
    };


    const removeImageSlot =
        (index) => {

            setImages(previous => {

                if (previous.length <= MIN_PRODUCT_IMAGES) {
                    return previous;
                }

                const image =
                    previous[index];

                if (
                    image?.preview &&
                    image.preview.startsWith("blob:")
                ) {

                    URL.revokeObjectURL(
                        image.preview
                    );
                }

                return previous.filter(
                    (_, imageIndex) =>
                        imageIndex !== index
                );
            });

            setError("");
            setMessage("");
        };


    /*
     * =====================================================
     * VALIDATE IMAGES
     * =====================================================
     */

    const validateImages = () => {

        const filledImages =
            images.filter(image =>
                (image.type === "FILE" && image.file) ||
                (image.type === "URL" && image.url.trim())
            );

        if (
            filledImages.length < MIN_PRODUCT_IMAGES ||
            filledImages.length > MAX_PRODUCT_IMAGES
        ) {
            return `Please add between ${MIN_PRODUCT_IMAGES} and ${MAX_PRODUCT_IMAGES} product images.`;
        }

        for (let i = 0; i < images.length; i++) {

            const image =
                images[i];

            if (!image.type && !image.file && !image.url.trim()) {
                continue;
            }


            if (image.type === "FILE") {

                if (!image.file) {

                    return (
                        `Please add Image ${i + 1}.`
                    );
                }
            }


            else if (image.type === "URL") {

                if (!image.url.trim()) {

                    return (
                        `Please enter Image ${i + 1} URL.`
                    );
                }


                try {

                    new URL(
                        image.url.trim()
                    );

                } catch {

                    return (
                        `Image ${i + 1} URL is not valid.`
                    );
                }
            }


            else if (image.file || image.url.trim()) {

                return `Please choose Computer Upload or Image URL for Image ${i + 1}.`;
            }
        }


        return null;
    };


    /*
     * =====================================================
     * PRODUCT DESCRIPTION
     * =====================================================
     */

    const productDescriptionValue = () => {

        const manualDescription =
            formData.description.trim();

        if (manualDescription) {
            return manualDescription;
        }

        const details =
            formData.specificationDetails.trim();

        if (details) {
            return details;
        }

        return [
            formData.name,
            formData.brand && `Brand: ${formData.brand}`,
            formData.model && `Model: ${formData.model}`,
            formData.ram && `RAM: ${formData.ram}`,
            formData.storage && `Storage: ${formData.storage}`,
            formData.colorOptions && `Colour: ${formData.colorOptions}`,
            formData.hsnCode && `HSN/SAC: ${formData.hsnCode}`
        ]
            .filter(Boolean)
            .join("\n");
    };


    /*
     * =====================================================
     * VALIDATE FORM
     * =====================================================
     */

    const validateForm = () => {

        if (!formData.name.trim()) {

            return (
                "Product name is required."
            );
        }


        if (!productDescriptionValue().trim()) {

            return (
                "Please add product details or mobile specifications."
            );
        }


        if (
            !formData.price ||
            Number(formData.price) <= 0
        ) {

            return (
                "Enter a valid product price."
            );
        }


        if (
            formData.stock === "" ||
            Number(formData.stock) < 0
        ) {

            return (
                "Enter a valid stock quantity."
            );
        }


        if (!formData.categoryId) {

            return (
                "Please select a category."
            );
        }


        if (!formData.subCategoryId) {

            return (
                "Please select a subcategory."
            );
        }


        const selectedCategory = categories.find(
            category => String(category.id) === String(formData.categoryId)
        );

        const hasMultipleMobileOptions = [
            formData.ram,
            formData.storage,
            formData.colorOptions
        ].some(value => /[,|/]/.test(String(value || "")));


        if (
            selectedCategory?.name?.trim().toLowerCase() === "mobiles" &&
            hasMultipleMobileOptions
        ) {

            return (
                "For mobiles, add one colour, RAM and storage configuration per listing. " +
                "Create a separate listing for every configuration so its price, stock and IMEI stay correct."
            );
        }


        return validateImages();
    };


    /*
     * =====================================================
     * SUBMIT
     * =====================================================
     */

    const handleSubmit =
        async (event) => {

            event.preventDefault();


            setError("");
            setMessage("");


            const validationError =
                validateForm();


            if (validationError) {

                setError(
                    validationError
                );

                return;
            }


            const token =
                getToken();


            if (!token) {

                setError(
                    "Login session expired. Please login again."
                );

                return;
            }


            try {

                setSubmitting(true);


                /*
                 * =================================================
                 * PRODUCT JSON
                 * =================================================
                 */

            const productRequest = {

                    name:
                        formData.name.trim(),

                    description:
                        productDescriptionValue().trim(),

                    brand:
                        formData.brand.trim(),

                    model:
                        formData.model.trim(),

                    modelNumber:
                        formData.modelNumber.trim(),

                    ram:
                        formData.ram.trim(),

                    storage:
                        formData.storage.trim(),

                    colorOptions:
                        formData.colorOptions.trim(),

                    compatibility: formData.compatibility.trim(),

                    warrantyDetails: formData.warrantyDetails.trim(),

                    packageContents: formData.packageContents.trim(),

                    hsnCode:
                        formData.hsnCode.trim(),

                    barcode:
                        formData.barcode.trim(),

                    sellerSku:
                        formData.sellerSku.trim(),

                    gstRate:
                        Number(formData.gstRate || 0),

                    shortHighlights:
                        formData.shortHighlights.trim(),

                    specificationDetails:
                        formData.specificationDetails.trim(),

                    productSpecifications: Object.keys(dynamicSpecs).length ? JSON.stringify(dynamicSpecs) : null,

                    productType: dynamicCategory || "NEW_MOBILE",

                    serialTrackingRequired: dynamicCategory ? false : undefined,

                    price:
                        Number(
                            formData.price
                        ),

                    stock:
                        Number(
                            formData.stock
                        ),

                    categoryId:
                        Number(
                            formData.categoryId
                        ),

                    subCategoryId:
                        Number(
                            formData.subCategoryId
                        )
                };


                /*
                 * =================================================
                 * MULTIPART FORM
                 * =================================================
                 */

                const multipart =
                    new FormData();


                /*
                 * PRODUCT PART
                 */

                multipart.append(
                    "product",
                    new Blob(
                        [
                            JSON.stringify(
                                productRequest
                            )
                        ],
                        {
                            type:
                                "application/json"
                        }
                    )
                );


                /*
                 * =================================================
                 * LOCAL COMPUTER IMAGES
                 * =================================================
                 */

                images
                    .filter(
                        image =>
                            image.type === "FILE" &&
                            image.file
                    )
                    .forEach(
                        image => {

                            multipart.append(
                                "images",
                                image.file
                            );
                        }
                    );


                /*
                 * =================================================
                 * IMAGE URLS
                 * =================================================
                 */

                const imageUrls =
                    images
                        .filter(
                            image =>
                                image.type === "URL" &&
                                image.url.trim()
                        )
                        .map(
                            image =>
                                image.url.trim()
                        );


                multipart.append(
                    "imageUrls",
                    new Blob(
                        [
                            JSON.stringify(
                                imageUrls
                            )
                        ],
                        {
                            type:
                                "application/json"
                        }
                    )
                );


                /*
                 * =================================================
                 * ONE REQUEST
                 * =================================================
                 *
                 * IMPORTANT:
                 *
                 * Do NOT manually set Content-Type.
                 *
                 * Browser will create:
                 *
                 * multipart/form-data;
                 * boundary=...
                 *
                 */

                const response =
                    await axios.post(
                        `${API_BASE_URL}/api/products/seller`,
                        multipart,
                        {
                            headers: {
                                Authorization:
                                    `Bearer ${token}`
                            }
                        }
                    );


                /*
                 * =================================================
                 * CHECK RESPONSE
                 * =================================================
                 */

                if (!response.data?.id) {

                    throw new Error(
                        "Product was not created."
                    );
                }


                /*
                 * =================================================
                 * SUCCESS
                 * =================================================
                 */

                setMessage(
                    "Product successfully submitted for Admin approval."
                );


                /*
                 * =================================================
                 * RESET FORM
                 * =================================================
                 */

                setFormData({
                    name: "",
                    description: "",
                    brand: "",
                    model: "",
                    modelNumber: "",
                    ram: "",
                    storage: "",
                    colorOptions: "",
                    compatibility: "",
                    warrantyDetails: "",
                    packageContents: "",
                    hsnCode: "",
                    gstRate: "18",
                    shortHighlights: "",
                    specificationDetails: "",
                    price: "",
                    stock: "",
                    categoryId: "",
                    subCategoryId: ""
                });


                setSubCategories([]);
                setDynamicSpecs({});


                /*
                 * Revoke local previews.
                 */

                images.forEach(
                    image => {

                        if (
                            image.preview &&
                            image.preview.startsWith(
                                "blob:"
                            )
                        ) {

                            URL.revokeObjectURL(
                                image.preview
                            );
                        }
                    }
                );


                setImages(
                    createEmptyImages()
                );


            } catch (err) {

                console.error(
                    "Add Product Error:",
                    err
                );


                console.error(
                    "Backend response:",
                    err?.response?.data
                );


                setError(
                    err?.response?.data?.message ||
                    err?.response?.data?.error ||
                    err?.message ||
                    "Product add करताना error आला."
                );

            } finally {

                setSubmitting(false);
            }
        };

    const catalogueImage = (product) => {
        const firstImage =
            product?.images?.find(image => image.imageUrl)?.imageUrl ||
            product?.imageUrl ||
            "";

        if (!firstImage) {
            return "";
        }

        return firstImage.startsWith("http")
            ? firstImage
            : `${API_BASE_URL}${firstImage.startsWith("/") ? "" : "/"}${firstImage}`;
    };

    const selectCatalogueProduct = (product) => {
        setSelectedCatalogueProduct(product);
        setError("");
        setMessage("");
        setFormData(previous => ({
            ...previous,
            name: product.name || "",
            description: product.description || "",
            brand: product.brand || "",
            model: product.model || "",
            modelNumber: product.modelNumber || "",
            ram: product.ram || "",
            storage: product.storage || "",
            colorOptions: product.colorOptions || "",
            compatibility: product.compatibility || "",
            warrantyDetails: product.warrantyDetails || "",
            packageContents: product.packageContents || "",
            hsnCode: product.hsnCode || "",
            gstRate: product.gstRate == null ? previous.gstRate : String(product.gstRate),
            shortHighlights: product.shortHighlights || "",
            specificationDetails: product.specificationDetails || "",
            categoryId: product.categoryEntity?.id ? String(product.categoryEntity.id) : previous.categoryId,
            subCategoryId: product.subCategory?.id ? String(product.subCategory.id) : previous.subCategoryId,
            stock: "0"
        }));
    };

    const existingListingFor = (catalogueProductId) => {
        return myProducts.find(product =>
            Number(product.catalogueParentId) === Number(catalogueProductId) ||
            Number(product.catalogueParent?.id) === Number(catalogueProductId)
        );
    };

    const inclusivePriceBreakup = () => {
        const inclusive = Number(formData.price || 0);
        const rate = Number(formData.gstRate || selectedCatalogueProduct?.gstRate || 0);
        if (!inclusive || inclusive <= 0 || rate < 0) {
            return { taxable: 0, gst: 0, cgst: 0, sgst: 0, final: inclusive || 0, rate: rate || 0 };
        }
        const taxable = rate === 0 ? inclusive : inclusive / (1 + rate / 100);
        return {
            taxable,
            gst: inclusive - taxable,
            cgst: (inclusive - taxable) / 2,
            sgst: (inclusive - taxable) / 2,
            final: inclusive,
            rate
        };
    };

    const formatMoney = (value) =>
        `₹${Number(value || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

    const createListingFromCatalogue = async () => {
        setError("");
        setMessage("");

        if (!selectedCatalogueProduct?.id) {
            setError("Select an approved catalogue product first.");
            return;
        }

        if (!formData.price || Number(formData.price) <= 0) {
            setError("Selling Price (Including GST) is required.");
            return;
        }

        const token = getToken();
        if (!token) {
            setError("Login session expired. Please login again.");
            return;
        }

        try {
            setSubmitting(true);
            const response = await axios.post(
                `${API_BASE_URL}/api/products/seller/list-from-catalogue`,
                {
                    catalogueProductId: selectedCatalogueProduct.id,
                    price: Number(formData.price),
                    stock: Number(formData.stock || 0)
                },
                {
                    headers: { Authorization: `Bearer ${token}` }
                }
            );

            setMessage(response.data?.id
                ? "Approved catalogue product listed for your shop. Price and stock are independent."
                : "Listing saved.");
            setMyProducts(previous => response.data?.id ? [response.data, ...previous] : previous);
        } catch (err) {
            setError(
                err?.response?.data?.message ||
                err?.response?.data?.error ||
                "Could not create seller listing from catalogue."
            );
        } finally {
            setSubmitting(false);
        }
    };


    /*
     * =====================================================
     * CLEANUP PREVIEWS
     * =====================================================
     */

    const imagesForCleanup = useRef(images);
    useEffect(() => { imagesForCleanup.current = images; }, [images]);
    useEffect(() => {

        return () => {

            imagesForCleanup.current.forEach(
                image => {

                    if (
                        image.preview &&
                        image.preview.startsWith(
                            "blob:"
                        )
                    ) {

                        URL.revokeObjectURL(
                            image.preview
                        );
                    }
                }
            );
        };

    }, []);


    /*
     * =====================================================
     * UI
     * =====================================================
     */

    const selectedCategoryName = categories.find(category => String(category.id) === String(formData.categoryId))?.name || "";
    const dynamicCategory = /laptop/i.test(selectedCategoryName) ? "LAPTOP" : /\btv\b|television/i.test(selectedCategoryName) ? "TV" : /accessor/i.test(selectedCategoryName) ? "ACCESSORY" : null;
    const dynamicFields = dynamicCategory ? categorySpecificationFields[dynamicCategory] : [];

    return (

        <div className="add-product-page">


            {/* =================================================
                TOP BAR
            ================================================= */}

            <div className="add-product-topbar">

                <button
                    type="button"
                    className="back-button"
                    onClick={() =>
                        navigate(
                            "/seller/dashboard"
                        )
                    }
                    disabled={submitting}
                >
                    <span>←</span>
                    Back to Dashboard
                </button>


                <div className="topbar-badge">
                    SELLER PANEL
                </div>

            </div>


            {/* =================================================
                PAGE HEADING
            ================================================= */}

            <div className="add-product-heading">

                <div className="heading-content">

                    <span className="heading-small">
                        PRODUCT MANAGEMENT
                    </span>

                    <h1>
                        Add New Product
                    </h1>

                    <p>
                        Add your product details and
                        submit it for ShivHub Admin approval.
                    </p>

                </div>


                <div className="heading-status">

                    <span className="status-dot"></span>

                    Admin Review Enabled

                </div>

            </div>


            <section className="catalogue-mode-panel">
                <div className="catalogue-mode-copy">
                    <span>SMART LISTING FLOW</span>
                    <h2>Choose how this product enters your shop.</h2>
                    <p>
                        Reuse approved ShivHub catalogue specs when available, or submit a new product/variant for admin review.
                    </p>
                </div>

                <div className="catalogue-mode-tabs">
                    <button
                        type="button"
                        className={productMode === "CATALOGUE" ? "active" : ""}
                        onClick={() => setProductMode("CATALOGUE")}
                        disabled={submitting}
                    >
                        Select from Approved Catalogue
                    </button>
                    <button
                        type="button"
                        className={productMode === "NEW" ? "active" : ""}
                        onClick={() => {
                            setProductMode("NEW");
                            setSelectedCatalogueProduct(null);
                        }}
                        disabled={submitting}
                    >
                        Submit New Product
                    </button>
                </div>

                {productMode === "CATALOGUE" && (
                    <div className="catalogue-picker">
                        <label>
                            Search approved catalogue
                            <input
                                type="search"
                                value={catalogueSearch}
                                onChange={event => setCatalogueSearch(event.target.value)}
                                placeholder="Search brand, model, title..."
                                disabled={submitting}
                            />
                        </label>

                        <div className="catalogue-result-grid">
                            {catalogueLoading && <p className="catalogue-empty">Loading approved products...</p>}
                            {!catalogueLoading && !catalogueProducts.length && (
                                <p className="catalogue-empty">No approved catalogue match found. Use “Submit New Product”.</p>
                            )}
                            {!catalogueLoading && catalogueProducts.map(product => (
                                <button
                                    type="button"
                                    key={product.id}
                                    className={selectedCatalogueProduct?.id === product.id ? "catalogue-result-card selected" : "catalogue-result-card"}
                                    onClick={() => selectCatalogueProduct(product)}
                                    disabled={submitting}
                                >
                                    <span className="catalogue-thumb">
                                        {catalogueImage(product) ? <img src={catalogueImage(product)} alt={product.name} /> : "No image"}
                                    </span>
                                    <strong>{product.name}</strong>
                                    <small>{[product.brand, product.model, product.ram, product.storage, product.colorOptions].filter(Boolean).join(" • ")}</small>
                                    {existingListingFor(product.id) && <em>Already in My Products</em>}
                                </button>
                            ))}
                        </div>

                        {selectedCatalogueProduct && (
                            <div className="catalogue-listing-box">
                                <div>
                                    <span>Selected approved variant</span>
                                    <strong>{selectedCatalogueProduct.name}</strong>
                                    <p>Specifications and images are reused. Your selling price and shop stock remain independent.</p>
                                </div>
                                {existingListingFor(selectedCatalogueProduct.id) ? (
                                    <button
                                        type="button"
                                        className="catalogue-submit-button"
                                        onClick={() => navigate("/seller/products")}
                                    >
                                        Already in My Products - Edit listing
                                    </button>
                                ) : (
                                    <>
                                        <div className="listing-price-grid">
                                            <label>
                                                Selling Price (Including GST)
                                                <input
                                                    type="number"
                                                    min="1"
                                                    step="0.01"
                                                    name="price"
                                                    value={formData.price}
                                                    onChange={handleChange}
                                                    placeholder="Final customer price"
                                                />
                                            </label>
                                            <div className="listing-stock-note">
                                                <strong>Stock is not created here</strong>
                                                <span>Available stock will come from your purchase / IMEI inventory flow.</span>
                                            </div>
                                        </div>
                                        <div className="price-breakdown-preview">
                                            <span>Taxable: {formatMoney(inclusivePriceBreakup().taxable)}</span>
                                            <span>CGST: {formatMoney(inclusivePriceBreakup().cgst)}</span>
                                            <span>SGST: {formatMoney(inclusivePriceBreakup().sgst)}</span>
                                            <span>GST included: {formatMoney(inclusivePriceBreakup().gst)}</span>
                                            <strong>Customer pays: {formatMoney(inclusivePriceBreakup().final)}</strong>
                                        </div>
                                        <button type="button" className="catalogue-submit-button" onClick={createListingFromCatalogue} disabled={submitting}>
                                            {submitting ? "Saving listing..." : "List this product in my shop"}
                                        </button>
                                    </>
                                )}
                            </div>
                        )}
                    </div>
                )}
            </section>


            {/* =================================================
                SUCCESS
            ================================================= */}

            {message && (

                <div className="alert success-alert">

                    <div className="alert-icon">
                        ✓
                    </div>

                    <div>

                        <strong>
                            Product Submitted
                        </strong>

                        <p>
                            {message}
                        </p>

                    </div>

                </div>
            )}


            {/* =================================================
                ERROR
            ================================================= */}

            {error && (

                <div className="alert error-alert">

                    <div className="alert-icon">
                        !
                    </div>

                    <div>

                        <strong>
                            Something went wrong
                        </strong>

                        <p>
                            {error}
                        </p>

                    </div>

                </div>
            )}


            {/* =================================================
                FORM
            ================================================= */}

            <form
                className={productMode === "NEW" ? "add-product-card" : "add-product-card manual-hidden"}
                onSubmit={handleSubmit}
            >


                {/* =================================================
                    SECTION 01
                ================================================= */}

                <section className="form-section">

                    <div className="section-heading">

                        <div className="section-number">
                            01
                        </div>

                        <div>

                            <h2>
                                Product Information
                            </h2>

                            <p>
                                Tell customers about your product.
                            </p>

                        </div>

                    </div>


                    <div className="form-group full-width">

                        <label htmlFor="name">
                            Product Name
                            <span>*</span>
                        </label>

                        <input
                            id="name"
                            type="text"
                            name="name"
                            value={formData.name}
                            onChange={handleChange}
                            placeholder="Example: Samsung Galaxy S26 Ultra"
                            maxLength={150}
                            required
                        />

                        <small>
                            Use a clear and customer-friendly product name.
                        </small>

                    </div>


                    <div className="mobile-spec-autofill">

                        <div className="mobile-spec-autofill-head">

                            <div>

                                <h3>
                                    Mobile specifications
                                </h3>

                                <p>
                                    Select saved specs for auto-fill, or enter every detail manually.
                                </p>

                            </div>

                            <span>
                                Manual + Auto
                            </span>

                        </div>


                        <div className="form-group full-width">

                            <label htmlFor="specSearch">
                                Search saved mobile model
                            </label>

                            <input
                                id="specSearch"
                                type="text"
                                value={specSearch}
                                onChange={event =>
                                    setSpecSearch(
                                        event.target.value
                                    )
                                }
                                placeholder="Search: realme 16T, Samsung, iPhone..."
                                disabled={submitting}
                            />

                            <small>
                                These specs come from Admin Mobile Specs catalogue. If not found, fill details manually below.
                            </small>

                        </div>


                        <div className="mobile-spec-options">

                            {visibleMobileSpecifications().map(
                                spec => (

                                    <button
                                        type="button"
                                        key={spec.id}
                                        onClick={() =>
                                            applyMobileSpecification(
                                                spec
                                            )
                                        }
                                        disabled={submitting}
                                    >

                                        <strong>
                                            {spec.brand} {spec.modelName}
                                        </strong>

                                        <span>
                                            {
                                                [
                                                    spec.variantName,
                                                    spec.ram,
                                                    spec.storage,
                                                    spec.colorOptions
                                                ]
                                                    .filter(Boolean)
                                                    .join(" • ") ||
                                                "Verified specification"
                                            }
                                        </span>

                                    </button>
                                )
                            )}

                            {!visibleMobileSpecifications().length && (

                                <p>
                                    No saved model found. Add it manually now, or ask admin to save it in Mobile Specs.
                                </p>
                            )}

                        </div>


                        <div className="mobile-manual-grid">

                            <div className="form-group">

                                <label htmlFor="brand">
                                    Brand
                                </label>

                                <input
                                    id="brand"
                                    type="text"
                                    name="brand"
                                    value={formData.brand}
                                    onChange={handleChange}
                                    placeholder="Realme"
                                    disabled={submitting}
                                />

                            </div>


                            <div className="form-group">

                                <label htmlFor="model">
                                    Model
                                </label>

                                <input
                                    id="model"
                                    type="text"
                                    name="model"
                                    value={formData.model}
                                    onChange={handleChange}
                                    placeholder="16T 5G"
                                    disabled={submitting}
                                />

                            </div>


                            <div className="form-group">

                                <label htmlFor="modelNumber">
                                    Model number
                                </label>

                                <input
                                    id="modelNumber"
                                    type="text"
                                    name="modelNumber"
                                    value={formData.modelNumber}
                                    onChange={handleChange}
                                    placeholder="Manufacturer model code"
                                    disabled={submitting}
                                />

                            </div>


                            <div className="form-group">

                                <label htmlFor="ram">
                                    RAM
                                </label>

                                <input
                                    id="ram"
                                    type="text"
                                    name="ram"
                                    value={formData.ram}
                                    onChange={handleChange}
                                    placeholder="6 GB"
                                    disabled={submitting}
                                />

                            </div>


                            <div className="form-group">

                                <label htmlFor="storage">
                                    Storage
                                </label>

                                <input
                                    id="storage"
                                    type="text"
                                    name="storage"
                                    value={formData.storage}
                                    onChange={handleChange}
                                    placeholder="128 GB"
                                    disabled={submitting}
                                />

                            </div>


                            <div className="form-group">

                                <label htmlFor="colorOptions">
                                    Colour
                                </label>

                                <input
                                    id="colorOptions"
                                    type="text"
                                    name="colorOptions"
                                    value={formData.colorOptions}
                                    onChange={handleChange}
                                    placeholder="Starlight Black"
                                    disabled={submitting}
                                />

                                <small>
                                    Add one configuration per listing. Use a separate listing for each colour, RAM or storage option.
                                </small>

                            </div>


                            <div className="form-group">

                                <label htmlFor="hsnCode">
                                    HSN/SAC
                                </label>

                                <input
                                    id="hsnCode"
                                    type="text"
                                    name="hsnCode"
                                    value={formData.hsnCode}
                                    onChange={handleChange}
                                    placeholder="85171300"
                                    disabled={submitting}
                                />

                            </div>

                            <div className="form-group">

                                <label htmlFor="warrantyDetails">
                                    Warranty details
                                </label>

                                <input
                                    id="warrantyDetails"
                                    type="text"
                                    name="warrantyDetails"
                                    value={formData.warrantyDetails}
                                    onChange={handleChange}
                                    placeholder="e.g. 12 months manufacturer warranty"
                                    disabled={submitting}
                                />

                            </div>


                            <div className="form-group">

                                <label htmlFor="sellerSku">
                                    SKU / Product code
                                </label>

                                <input
                                    id="sellerSku"
                                    type="text"
                                    name="sellerSku"
                                    value={formData.sellerSku}
                                    onChange={handleChange}
                                    placeholder="Seller stock code"
                                    disabled={submitting}
                                />

                            </div>


                            <div className="form-group">

                                <label htmlFor="barcode">
                                    Product barcode (optional)
                                </label>

                                <input
                                    id="barcode"
                                    type="text"
                                    name="barcode"
                                    value={formData.barcode}
                                    onChange={handleChange}
                                    placeholder="EAN / UPC / manufacturer barcode"
                                    disabled={submitting}
                                />

                            </div>


                            <div className="form-group">

                                <label htmlFor="gstRate">
                                    GST rate %
                                </label>

                                <input
                                    id="gstRate"
                                    type="number"
                                    name="gstRate"
                                    value={formData.gstRate}
                                    onChange={handleChange}
                                    placeholder="18"
                                    min="0"
                                    max="100"
                                    step="0.01"
                                    disabled={submitting}
                                />

                            </div>

                        </div>


                        <div className="form-group full-width">

                            <label htmlFor="compatibility">
                                Compatible models / devices
                            </label>

                            <input
                                id="compatibility"
                                type="text"
                                name="compatibility"
                                value={formData.compatibility}
                                onChange={handleChange}
                                placeholder="Comma-separated model names, if applicable"
                                disabled={submitting}
                            />

                        </div>

                        <div className="form-group full-width">

                            <label htmlFor="packageContents">
                                In the box
                            </label>

                            <input
                                id="packageContents"
                                type="text"
                                name="packageContents"
                                value={formData.packageContents}
                                onChange={handleChange}
                                placeholder="e.g. Device, cable, charger, user guide"
                                disabled={submitting}
                            />

                        </div>

                        <div className="form-group full-width">

                            <label htmlFor="shortHighlights">
                                Short highlights
                            </label>

                            <textarea
                                id="shortHighlights"
                                name="shortHighlights"
                                value={formData.shortHighlights}
                                onChange={handleChange}
                                placeholder="Key points shown to customers, one per line..."
                                rows={3}
                                disabled={submitting}
                            />

                        </div>


                        <div className="form-group full-width">

                            <label htmlFor="specificationDetails">
                                Product description & full specification details
                            </label>

                            <textarea
                                id="specificationDetails"
                                name="specificationDetails"
                                value={formData.specificationDetails}
                                onChange={handleChange}
                                placeholder="Display, processor, camera, battery, OS, connectivity, box contents, warranty..."
                                rows={5}
                                disabled={submitting}
                            />

                        </div>

                        {dynamicFields.length > 0 && <div className="form-group full-width">
                            <label>Category-specific specifications</label>
                            <div className="form-grid">
                                {dynamicFields.map(field => <label key={field}>{field}<input value={dynamicSpecs[field] || ""} onChange={event => setDynamicSpecs(current => ({ ...current, [field]: event.target.value }))} placeholder={field} disabled={submitting} /></label>)}
                            </div>
                        </div>}

                    </div>

                </section>


                {/* =================================================
                    SECTION 02
                ================================================= */}

                <section className="form-section">

                    <div className="section-heading">

                        <div className="section-number">
                            02
                        </div>

                        <div>

                            <h2>
                                Category
                            </h2>

                            <p>
                                Select where your product belongs.
                            </p>

                        </div>

                    </div>


                    <div className="two-column">


                        {/* CATEGORY */}

                        <div className="form-group">

                            <label htmlFor="categoryId">
                                Category
                                <span>*</span>
                            </label>

                            <select
                                id="categoryId"
                                name="categoryId"
                                value={
                                    formData.categoryId
                                }
                                onChange={
                                    handleCategoryChange
                                }
                                disabled={
                                    loadingCategories ||
                                    submitting
                                }
                                required
                            >

                                <option value="">

                                    {loadingCategories
                                        ? "Loading categories..."
                                        : "Select Category"}

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

                        </div>


                        {/* SUBCATEGORY */}

                        <div className="form-group">

                            <label htmlFor="subCategoryId">

                                SubCategory

                                <span>*</span>

                            </label>


                            <select
                                id="subCategoryId"
                                name="subCategoryId"
                                value={
                                    formData.subCategoryId
                                }
                                onChange={
                                    handleChange
                                }
                                disabled={
                                    !formData.categoryId ||
                                    loadingSubCategories ||
                                    submitting
                                }
                                required
                            >

                                <option value="">

                                    {loadingSubCategories
                                        ? "Loading subcategories..."
                                        : !formData.categoryId
                                            ? "Select Category First"
                                            : "Select SubCategory"}

                                </option>


                                {subCategories.map(
                                    subCategory => (

                                        <option
                                            key={
                                                subCategory.id
                                            }
                                            value={
                                                subCategory.id
                                            }
                                        >
                                            {
                                                subCategory.name
                                            }
                                        </option>

                                    )
                                )}

                            </select>

                        </div>

                    </div>

                </section>


                {/* =================================================
                    SECTION 03
                ================================================= */}

                <section className="form-section">

                    <div className="section-heading">

                        <div className="section-number">
                            03
                        </div>

                        <div>

                            <h2>
                                Price & Inventory
                            </h2>

                            <p>
                                Set your selling price and available stock.
                            </p>

                        </div>

                    </div>


                    <div className="two-column">


                        {/* PRICE */}

                        <div className="form-group">

                            <label htmlFor="price">

                                Selling Price (Including GST)

                                <span>*</span>

                            </label>


                            <div className="currency-input">

                                <span>
                                    ₹
                                </span>

                                <input
                                    id="price"
                                    type="number"
                                    name="price"
                                    value={
                                        formData.price
                                    }
                                    onChange={
                                        handleChange
                                    }
                                    placeholder="69999"
                                    min="0"
                                    step="0.01"
                                    required
                                />

                            </div>

                        </div>

                        <div className="price-breakdown-preview full-width">
                            <span>Taxable value: {formatMoney(inclusivePriceBreakup().taxable)}</span>
                            <span>CGST: {formatMoney(inclusivePriceBreakup().cgst)}</span>
                            <span>SGST: {formatMoney(inclusivePriceBreakup().sgst)}</span>
                            <span>GST included @ {inclusivePriceBreakup().rate}%: {formatMoney(inclusivePriceBreakup().gst)}</span>
                            <strong>Final customer price: {formatMoney(inclusivePriceBreakup().final)}</strong>
                        </div>

                        {/* STOCK */}

                        <div className="form-group">

                            <label htmlFor="stock">

                                Available Stock

                                <span>*</span>

                            </label>


                            <input
                                id="stock"
                                type="number"
                                name="stock"
                                value={
                                    formData.stock
                                }
                                onChange={
                                    handleChange
                                }
                                placeholder="10"
                                min="0"
                                required
                            />

                        </div>

                    </div>

                </section>


                {/* =================================================
                    SECTION 04 - IMAGES
                ================================================= */}

                <section className="form-section">

                    <div className="section-heading">

                        <div className="section-number">
                            04
                        </div>

                        <div>

                            <h2>
                                Product Images
                            </h2>

                            <p>
                                Add minimum 5 product images. You can add up to 15 images.
                                Each image can be uploaded from your computer
                                or added using an image URL.
                            </p>

                        </div>

                    </div>


                    <div className="image-slots">


                        {images.map(
                            (image, index) => {

                                const isFile =
                                    image.type === "FILE";

                                const isUrl =
                                    image.type === "URL";


                                return (

                                    <div
                                        className="image-slot"
                                        key={index}
                                    >


                                        {/* HEADER */}

                                        <div className="image-slot-header">

                                            <div className="image-slot-title">

                                                <span className="image-number">
                                                    {index + 1}
                                                </span>

                                                <strong>
                                                    Image {index + 1}
                                                </strong>

                                                {index === 0 && (

                                                    <span className="main-badge">
                                                        Main
                                                    </span>

                                                )}

                                            </div>

                                        </div>


                                        {/* TYPE BUTTONS */}

                                        <div className="image-type-buttons">


                                            <button
                                                type="button"
                                                className={
                                                    isFile
                                                        ? "type-button active"
                                                        : "type-button"
                                                }
                                                onClick={() =>
                                                    changeImageType(
                                                        index,
                                                        "FILE"
                                                    )
                                                }
                                                disabled={
                                                    submitting
                                                }
                                            >
                                                📁 Upload from Computer
                                            </button>


                                            <button
                                                type="button"
                                                className={
                                                    isUrl
                                                        ? "type-button active"
                                                        : "type-button"
                                                }
                                                onClick={() =>
                                                    changeImageType(
                                                        index,
                                                        "URL"
                                                    )
                                                }
                                                disabled={
                                                    submitting
                                                }
                                            >
                                                🔗 Image URL
                                            </button>

                                        </div>


                                        {/* =================================================
                                            COMPUTER UPLOAD
                                        ================================================= */}

                                        {isFile && (

                                            <label
                                                className="file-upload-box"
                                                htmlFor={
                                                    `product-image-${index}`
                                                }
                                            >

                                                <input
                                                    id={
                                                        `product-image-${index}`
                                                    }
                                                    type="file"
                                                    accept="image/jpeg,image/png,image/webp"
                                                    onChange={
                                                        event =>
                                                            handleFileChange(
                                                                index,
                                                                event
                                                            )
                                                    }
                                                    disabled={
                                                        submitting
                                                    }
                                                />


                                                {image.preview ? (

                                                    <img
                                                        src={
                                                            image.preview
                                                        }
                                                        alt={
                                                            `Product image ${index + 1}`
                                                        }
                                                        className="selected-image-preview"
                                                    />

                                                ) : (

                                                    <div className="upload-placeholder">

                                                        <div className="upload-icon">
                                                            ↑
                                                        </div>

                                                        <strong>
                                                            Choose Image
                                                        </strong>

                                                        <small>
                                                            JPG · PNG · WEBP
                                                            · Maximum 10MB
                                                        </small>

                                                    </div>

                                                )}

                                            </label>

                                        )}


                                        {/* =================================================
                                            URL
                                        ================================================= */}

                                        {isUrl && (

                                            <div className="url-input-box">

                                                <div className="url-icon">
                                                    🔗
                                                </div>


                                                <input
                                                    type="url"
                                                    value={
                                                        image.url
                                                    }
                                                    onChange={
                                                        event =>
                                                            handleUrlChange(
                                                                index,
                                                                event.target.value
                                                            )
                                                    }
                                                    placeholder="Paste image address here..."
                                                    disabled={
                                                        submitting
                                                    }
                                                />


                                                {image.preview && (

                                                    <img
                                                        src={
                                                            image.preview
                                                        }
                                                        alt={
                                                            `URL preview ${index + 1}`
                                                        }
                                                        className="url-preview"
                                                        onError={
                                                            event => {

                                                                event.currentTarget.classList.add(
                                                                    "image-preview-error"
                                                                );
                                                            }
                                                        }
                                                    />

                                                )}

                                            </div>

                                        )}


                                        {/* REMOVE */}

                                        {images.length > MIN_PRODUCT_IMAGES && (

                                            <button
                                                type="button"
                                                className="remove-image"
                                                onClick={() =>
                                                    removeImageSlot(
                                                        index
                                                    )
                                                }
                                                disabled={
                                                    submitting
                                                }
                                            >
                                                Remove Slot
                                            </button>

                                        )}


                                        {(image.file ||
                                            image.url) && (

                                            <button
                                                type="button"
                                                className="remove-image"
                                                onClick={() =>
                                                    clearImage(
                                                        index
                                                    )
                                                }
                                                disabled={
                                                    submitting
                                                }
                                            >
                                                Remove Image
                                            </button>

                                        )}

                                    </div>

                                );
                            }
                        )}

                    </div>


                    <button
                        type="button"
                        className="add-image-slot"
                        onClick={addImageSlot}
                        disabled={
                            submitting ||
                            images.length >= MAX_PRODUCT_IMAGES
                        }
                    >
                        + Add more image slot
                        <span>
                            {images.length}/{MAX_PRODUCT_IMAGES}
                        </span>
                    </button>


                    <div className="image-note">

                        <strong>
                            Minimum 5 images required
                        </strong>

                        <span>
                            You can use any combination:
                            5 or more computer images,
                            5 or more URLs,
                            or a mix of both.
                        </span>

                    </div>

                </section>


                {/* =================================================
                    APPROVAL
                ================================================= */}

                <div className="approval-box">

                    <div className="approval-symbol">
                        ✓
                    </div>


                    <div className="approval-content">

                        <h3>
                            Admin Approval Required
                        </h3>

                        <p>

                            Your product will be submitted with

                            <strong>
                                {" "}PENDING{" "}
                            </strong>

                            status.

                            Customers will see the product
                            only after ShivHub Admin approves it.

                        </p>

                    </div>

                </div>


                {/* =================================================
                    ACTIONS
                ================================================= */}

                <div className="form-actions">


                    <button
                        type="button"
                        className="cancel-button"
                        onClick={() =>
                            navigate(
                                "/seller/dashboard"
                            )
                        }
                        disabled={
                            submitting
                        }
                    >
                        Cancel
                    </button>


                    <button
                        type="submit"
                        className="submit-button"
                        disabled={
                            submitting
                        }
                    >

                        {submitting ? (

                            <>
                                <span className="button-spinner"></span>
                                Submitting Product...
                            </>

                        ) : (

                            <>
                                Submit for Approval
                                <span>
                                    →
                                </span>
                            </>

                        )}

                    </button>

                </div>

            </form>

        </div>
    );
};


export default AddProduct;

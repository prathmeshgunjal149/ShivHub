import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../services/api";
import "./AdminAddProduct.css";


const MIN_PRODUCT_IMAGES = 5;
const MAX_PRODUCT_IMAGES = 15;


const createEmptyImage = () => ({
    type: "",
    file: null,
    url: "",
    preview: ""
});


const AdminAddProduct = () => {

    const navigate = useNavigate();


    const [formData, setFormData] = useState({
        name: "",
        description: "",
        price: "",
        offerPercentage: "",
        stock: "",
        sellerSku: "",
        barcode: "",
        categoryId: "",
        subCategoryId: ""
    });


    const [categories, setCategories] =
        useState([]);

    const [subCategories, setSubCategories] =
        useState([]);


    const [images, setImages] =
        useState([
            createEmptyImage(),
            createEmptyImage(),
            createEmptyImage(),
            createEmptyImage(),
            createEmptyImage()
        ]);


    const [loadingCategories, setLoadingCategories] =
        useState(false);

    const [loadingSubCategories, setLoadingSubCategories] =
        useState(false);

    const [submitting, setSubmitting] =
        useState(false);


    const [message, setMessage] =
        useState("");

    const [error, setError] =
        useState("");


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

                const token =
                    getToken();


                if (!token) {

                    setError(
                        "Admin session not found. Please login again."
                    );

                    return;
                }


                const response =
                    await api.get(
                        "/api/categories"
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

                    setError(
                        err?.response?.data?.message ||
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


    /*
     * =====================================================
     * INPUT CHANGE
     * =====================================================
     */

    const handleChange = (event) => {

        const {
            name,
            value
        } = event.target;


        setFormData(
            previous => ({
                ...previous,
                [name]: value
            })
        );


        setError("");

        setMessage("");
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


            setFormData(
                previous => ({
                    ...previous,
                    categoryId,
                    subCategoryId: ""
                })
            );


            setSubCategories([]);

            setError("");

            setMessage("");


            if (!categoryId) {

                return;
            }


            try {

                setLoadingSubCategories(true);


                const token =
                    getToken();


                if (!token) {

                    throw new Error(
                        "Admin session not found."
                    );
                }


                const response =
                    await api.get(
                        `/api/categories/${categoryId}/subcategories`
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
                    "SubCategory loading error:",
                    err
                );


                setError(
                    err?.response?.data?.message ||
                    "Subcategories could not be loaded."
                );


            } finally {

                setLoadingSubCategories(false);
            }
        };


    /*
     * =====================================================
     * SELECT LOCAL IMAGE
     * =====================================================
     */

    const handleFileChange =
        (index, event) => {

            const file =
                event.target.files?.[0];


            if (!file) {

                return;
            }


            setError("");

            setMessage("");


            const allowedTypes = [
                "image/jpeg",
                "image/png",
                "image/webp"
            ];


            if (!allowedTypes.includes(
                file.type
            )) {

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


                if (
                    updated[index].preview
                ) {

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
        };


    /*
     * =====================================================
     * IMAGE URL CHANGE
     * =====================================================
     */

    const handleUrlChange =
        (index, value) => {

            setError("");

            setMessage("");


            setImages(previous => {

                const updated =
                    [...previous];


                if (
                    updated[index].preview
                ) {

                    URL.revokeObjectURL(
                        updated[index].preview
                    );
                }


                updated[index] = {
                    type: value.trim()
                        ? "URL"
                        : "",
                    file: null,
                    url: value,
                    preview: value.trim()
                };


                return updated;
            });
        };


    /*
     * =====================================================
     * CLEAR IMAGE SLOT
     * =====================================================
     */

    const clearImage =
        (index) => {

            setImages(previous => {

                const updated =
                    [...previous];


                if (
                    updated[index].preview
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
                    `admin-image-file-${index}`
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

                if (image?.preview) {

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
     * VALIDATE FORM
     * =====================================================
     */

    const validateForm = () => {

        if (!formData.name.trim()) {

            return "Product name is required.";
        }


        if (!formData.description.trim()) {

            return "Product description is required.";
        }


        if (!formData.price) {

            return "Product price is required.";
        }


        if (
            Number(formData.price) <= 0
        ) {

            return "Price must be greater than 0.";
        }

        if (
            formData.offerPercentage !== "" &&
            (Number(formData.offerPercentage) < 0 || Number(formData.offerPercentage) > 100)
        ) {

            return "Offer discount must be between 0 and 100%.";
        }


        if (
            formData.stock === ""
        ) {

            return "Stock quantity is required.";
        }


        if (
            Number(formData.stock) < 0
        ) {

            return "Stock cannot be negative.";
        }


        if (!formData.categoryId) {

            return "Please select a category.";
        }


        if (!formData.subCategoryId) {

            return "Please select a subcategory.";
        }


        const completedImages =
            images.filter(
                image =>
                    image.type === "FILE" ||
                    image.type === "URL"
            );


        if (
            completedImages.length < MIN_PRODUCT_IMAGES ||
            completedImages.length > MAX_PRODUCT_IMAGES
        ) {

            return `Please provide between ${MIN_PRODUCT_IMAGES} and ${MAX_PRODUCT_IMAGES} product images. Use computer upload or image URL.`;
        }


        return null;
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
                    "Admin session not found. Please login again."
                );

                return;
            }


            try {

                setSubmitting(true);


                /*
                 * Product JSON
                 */

                const productRequest = {

                    name:
                        formData.name.trim(),

                    description:
                        formData.description.trim(),

                    price:
                        Number(
                            formData.price
                        ),

                    offerPercentage:
                        formData.offerPercentage === ""
                            ? null
                            : Number(formData.offerPercentage),

                    stock:
                        Number(
                            formData.stock
                        ),

                    barcode:
                        formData.barcode.trim(),

                    sellerSku:
                        formData.sellerSku.trim(),

                    categoryId:
                        Number(
                            formData.categoryId
                        ),

                    subCategoryId:
                        Number(
                            formData.subCategoryId
                        )
                };


                const multipart =
                    new FormData();


                /*
                 * JSON product part
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
                 * Local files
                 */

                images
                    .filter(
                        image =>
                            image.type ===
                            "FILE"
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
                 * External URLs
                 */

                const imageUrls =
                    images
                        .filter(
                            image =>
                                image.type ===
                                "URL"
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
                 * API
                 */

                const response =
                    await api.post(
                        "/api/admin/products",
                        multipart,
                        {
                            headers: {
                                "Content-Type": "multipart/form-data"
                            }
                        }
                    );


                if (!response.data?.id) {

                    throw new Error(
                        "Product was not created."
                    );
                }


                setMessage(
                    "Product successfully submitted for Admin approval."
                );


                /*
                 * Reset
                 */

                setFormData({
                    name: "",
                    description: "",
                    price: "",
                    offerPercentage: "",
                    stock: "",
                    categoryId: "",
                    subCategoryId: ""
                });


                images.forEach(
                    image => {

                        if (
                            image.preview
                        ) {

                            URL.revokeObjectURL(
                                image.preview
                            );
                        }
                    }
                );


                setImages([
                    createEmptyImage(),
                    createEmptyImage(),
                    createEmptyImage(),
                    createEmptyImage(),
                    createEmptyImage()
                ]);


                setSubCategories([]);


            } catch (err) {

                console.error(
                    "Admin Add Product Error:",
                    err
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


    /*
     * =====================================================
     * UI
     * =====================================================
     */

    return (

        <div className="admin-add-product-page">

            <div className="admin-add-product-topbar">

                <button
                    type="button"
                    className="admin-back-button"
                    onClick={() =>
                        navigate(
                            "/admin/dashboard"
                        )
                    }
                >
                    ← Back to Dashboard
                </button>


                <div className="admin-topbar-badge">
                    SHIVHUB ADMIN
                </div>

            </div>


            <div className="admin-add-product-heading">

                <div>

                    <span className="admin-heading-small">
                        PRODUCT MANAGEMENT
                    </span>

                    <h1>
                        Add ShivHub Product
                    </h1>

                    <p>
                        Add a product directly to
                        ShivHub catalogue.
                    </p>

                </div>


                <div className="admin-status-badge">

                    <span className="admin-status-dot"></span>

                    Admin Approval Required

                </div>

            </div>


            {message && (

                <div className="admin-alert admin-success">

                    <strong>
                        Product Submitted
                    </strong>

                    <p>
                        {message}
                    </p>

                </div>

            )}


            {error && (

                <div className="admin-alert admin-error">

                    <strong>
                        Something went wrong
                    </strong>

                    <p>
                        {error}
                    </p>

                </div>

            )}


            <form
                className="admin-product-card"
                onSubmit={handleSubmit}
            >

                {/* =================================================
                    PRODUCT INFORMATION
                ================================================= */}

                <section className="admin-form-section">

                    <div className="admin-section-heading">

                        <div className="admin-section-number">
                            01
                        </div>

                        <div>

                            <h2>
                                Product Information
                            </h2>

                            <p>
                                Enter complete product details.
                            </p>

                        </div>

                    </div>


                    <div className="admin-form-group">

                        <label>
                            Product Name *
                        </label>

                        <input
                            type="text"
                            name="name"
                            value={formData.name}
                            onChange={handleChange}
                            placeholder="Example: Samsung Galaxy S25"
                            maxLength={150}
                        />

                    </div>


                    <div className="admin-form-group">

                        <label>
                            Product Description *
                        </label>

                        <textarea
                            name="description"
                            value={formData.description}
                            onChange={handleChange}
                            placeholder="Write detailed product specifications..."
                            rows={6}
                        />

                    </div>

                </section>


                {/* =================================================
                    CATEGORY
                ================================================= */}

                <section className="admin-form-section">

                    <div className="admin-section-heading">

                        <div className="admin-section-number">
                            02
                        </div>

                        <div>

                            <h2>
                                Category
                            </h2>

                            <p>
                                Select category and subcategory.
                            </p>

                        </div>

                    </div>


                    <div className="admin-two-column">

                        <div className="admin-form-group">

                            <label>
                                Category *
                            </label>

                            <select
                                value={formData.categoryId}
                                onChange={
                                    handleCategoryChange
                                }
                                disabled={
                                    loadingCategories
                                }
                            >

                                <option value="">
                                    {
                                        loadingCategories
                                            ? "Loading categories..."
                                            : "Select Category"
                                    }
                                </option>


                                {categories.map(
                                    category => (

                                        <option
                                            key={category.id}
                                            value={category.id}
                                        >
                                            {category.name}
                                        </option>

                                    )
                                )}

                            </select>

                        </div>

                        <div className="admin-form-group">

                            <label>
                                Offer Discount %
                            </label>

                            <input
                                type="number"
                                name="offerPercentage"
                                value={formData.offerPercentage}
                                onChange={handleChange}
                                placeholder="Optional (e.g. 15)"
                                min="0"
                                max="100"
                                step="0.01"
                            />

                        </div>


                        <div className="admin-form-group">

                            <label>
                                Subcategory *
                            </label>

                            <select
                                value={
                                    formData.subCategoryId
                                }
                                onChange={event =>
                                    setFormData(
                                        previous => ({
                                            ...previous,
                                            subCategoryId:
                                                event.target.value
                                        })
                                    )
                                }
                                disabled={
                                    !formData.categoryId ||
                                    loadingSubCategories
                                }
                            >

                                <option value="">
                                    {
                                        loadingSubCategories
                                            ? "Loading subcategories..."
                                            : !formData.categoryId
                                                ? "Select Category First"
                                                : "Select Subcategory"
                                    }
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

                        <div className="admin-form-group">

                            <label>
                                SKU / Product code
                            </label>

                            <input
                                type="text"
                                name="sellerSku"
                                value={formData.sellerSku}
                                onChange={handleChange}
                                placeholder="Seller stock code"
                                maxLength={80}
                            />

                        </div>

                        <div className="admin-form-group">

                            <label>
                                Product Barcode (optional)
                            </label>

                            <input
                                type="text"
                                name="barcode"
                                value={formData.barcode}
                                onChange={handleChange}
                                placeholder="EAN / UPC / manufacturer barcode"
                                maxLength={80}
                            />

                        </div>

                    </div>

                </section>


                {/* =================================================
                    PRICE & STOCK
                ================================================= */}

                <section className="admin-form-section">

                    <div className="admin-section-heading">

                        <div className="admin-section-number">
                            03
                        </div>

                        <div>

                            <h2>
                                Price & Inventory
                            </h2>

                            <p>
                                Set selling price and stock.
                            </p>

                        </div>

                    </div>


                    <div className="admin-two-column">

                        <div className="admin-form-group">

                            <label>
                                Selling Price *
                            </label>

                            <div className="admin-price-input">

                                <span>₹</span>

                                <input
                                    type="number"
                                    name="price"
                                    value={formData.price}
                                    onChange={handleChange}
                                    placeholder="69999"
                                    min="0"
                                    step="0.01"
                                />

                            </div>

                        </div>


                        <div className="admin-form-group">

                            <label>
                                Available Stock *
                            </label>

                            <input
                                type="number"
                                name="stock"
                                value={formData.stock}
                                onChange={handleChange}
                                placeholder="10"
                                min="0"
                            />

                        </div>

                    </div>

                </section>


                {/* =================================================
                    IMAGES
                ================================================= */}

                <section className="admin-form-section">

                    <div className="admin-section-heading">

                        <div className="admin-section-number">
                            04
                        </div>

                        <div>

                            <h2>
                                Product Images
                            </h2>

                            <p>
                                Add minimum 5 images. You can add up to 15 images.
                                Each image can be uploaded
                                from your computer or added
                                using an image URL.
                            </p>

                        </div>

                    </div>


                    <div className="admin-image-note">

                        <strong>
                            Minimum 5 images required
                        </strong>

                        <span>
                            Computer upload + Image URL
                            can be mixed.
                        </span>

                    </div>


                    <div className="admin-image-grid">

                        {images.map(
                            (image, index) => (

                                <div
                                    className="admin-image-slot"
                                    key={index}
                                >

                                    <div className="admin-image-slot-header">

                                        <strong>
                                            Image {index + 1}
                                        </strong>

                                        {index === 0 && (
                                            <span>
                                                Main Image
                                            </span>
                                        )}

                                    </div>


                                    {image.preview ? (

                                        <div className="admin-preview">

                                            <img
                                                src={
                                                    image.preview
                                                }
                                                alt={
                                                    `Product ${index + 1}`
                                                }
                                            />

                                        </div>

                                    ) : (

                                        <div className="admin-empty-preview">
                                            No Image
                                        </div>

                                    )}


                                    <input
                                        id={
                                            `admin-image-file-${index}`
                                        }
                                        type="file"
                                        accept="image/jpeg,image/png,image/webp"
                                        hidden
                                        onChange={
                                            event =>
                                                handleFileChange(
                                                    index,
                                                    event
                                                )
                                        }
                                    />


                                    <label
                                        htmlFor={
                                            `admin-image-file-${index}`
                                        }
                                        className="admin-upload-button"
                                    >
                                        📁 Upload from Computer
                                    </label>


                                    <div className="admin-or">
                                        OR
                                    </div>


                                    <input
                                        type="text"
                                        className="admin-url-input"
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
                                        placeholder="Paste image URL"
                                    />


                                    {image.type && (

                                        <div className="admin-image-type">

                                            {image.type === "FILE"
                                                ? "✓ Computer Image"
                                                : "✓ Image URL"}

                                        </div>

                                    )}


                                    {image.type && (

                                        <button
                                            type="button"
                                            className="admin-clear-image"
                                            onClick={() =>
                                                clearImage(
                                                    index
                                                )
                                            }
                                        >
                                            Remove Image
                                        </button>

                                    )}


                                    {images.length > MIN_PRODUCT_IMAGES && (

                                        <button
                                            type="button"
                                            className="admin-clear-image"
                                            onClick={() =>
                                                removeImageSlot(
                                                    index
                                                )
                                            }
                                        >
                                            Remove Slot
                                        </button>

                                    )}

                                </div>

                            )
                        )}

                    </div>


                    <button
                        type="button"
                        className="admin-add-image-slot"
                        onClick={addImageSlot}
                        disabled={
                            submitting ||
                            images.length >= MAX_PRODUCT_IMAGES
                        }
                    >
                        + Add more image slot
                        <span>{images.length}/{MAX_PRODUCT_IMAGES}</span>
                    </button>

                </section>


                {/* =================================================
                    APPROVAL
                ================================================= */}

                <div className="admin-approval-box">

                    <div className="admin-approval-icon">
                        ✓
                    </div>

                    <div>

                        <h3>
                            Admin Product Approval
                        </h3>

                        <p>
                            This product will be created
                            with <strong>PENDING</strong> status.
                            It will become visible to customers
                            only after Admin approval.
                        </p>

                    </div>

                </div>


                {/* =================================================
                    ACTIONS
                ================================================= */}

                <div className="admin-form-actions">

                    <button
                        type="button"
                        className="admin-cancel-button"
                        onClick={() =>
                            navigate(
                                "/admin/dashboard"
                            )
                        }
                        disabled={submitting}
                    >
                        Cancel
                    </button>


                    <button
                        type="submit"
                        className="admin-submit-button"
                        disabled={submitting}
                    >

                        {submitting
                            ? "Submitting..."
                            : "Submit Product for Approval →"}

                    </button>

                </div>

            </form>

        </div>
    );
};


export default AdminAddProduct;

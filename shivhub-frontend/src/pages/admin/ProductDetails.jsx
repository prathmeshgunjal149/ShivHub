import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { approveProduct, getProduct, rejectProduct } from "../../services/adminService";
import "./AdminManagement.css";
import "./ProductDetailsImages.css";

export default function ProductDetails() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [product, setProduct] = useState(null);
    const [review, setReview] = useState("");
    const [reason, setReason] = useState("");
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [selectedImage, setSelectedImage] = useState(0);
    const [brokenImages, setBrokenImages] = useState({});

    useEffect(() => {
        setSelectedImage(0);
        setBrokenImages({});
        getProduct(id)
            .then(setProduct)
            .catch(() => setError("Product details could not be loaded."));
    }, [id]);

    const images = useMemo(() => {
        const gallery = Array.isArray(product?.images)
            ? product.images.map(image => image?.imageUrl).filter(Boolean)
            : [];

        return gallery.length
            ? gallery
            : (product?.imageUrl ? [product.imageUrl] : []);
    }, [product]);

    const decide = async action => {
        if (action === "reject" && !reason.trim()) {
            setError("Enter a rejection reason before sending the decision.");
            return;
        }

        try {
            setBusy(true);
            setError("");
            await (action === "approve"
                ? approveProduct(id, review)
                : rejectProduct(id, reason));
            navigate("/admin/products");
        } catch (requestError) {
            setError(
                requestError.response?.data?.message ||
                "The decision could not be saved."
            );
        } finally {
            setBusy(false);
        }
    };

    if (!product && !error) {
        return <section className="admin-management"><p>Loading product…</p></section>;
    }

    return <section className="admin-management">
        <Link className="back-link" to="/admin/products">← Products</Link>
        {error && <p className="admin-error">{error}</p>}

        {product && <>
            <header>
                <div>
                    <p className="eyebrow">Product submission</p>
                    <h1>{product.name}</h1>
                    <p>
                        <span className={`status ${product.approvalStatus?.toLowerCase()}`}>
                            {product.approvalStatus}
                        </span>
                        {" · Seller: "}{product.seller?.name || "—"}
                    </p>
                </div>
            </header>

            <div className="detail-grid">
                <article className="management-card product-review-gallery">
                    <div className="review-gallery-heading">
                        <div>
                            <p className="eyebrow">Submitted images</p>
                            <h2>Product preview</h2>
                        </div>
                        <span>{images.length} image{images.length === 1 ? "" : "s"}</span>
                    </div>

                    {images.length ? <>
                        <div className="review-main-image">
                            {brokenImages[selectedImage]
                                ? <div className="review-image-fallback"><strong>Image preview unavailable</strong><span>The submitted URL may be private or blocked by its host.</span><a href={images[selectedImage]} target="_blank" rel="noreferrer">Open submitted image</a></div>
                                : <img src={images[selectedImage]} alt={`${product.name} preview ${selectedImage + 1}`} onError={() => setBrokenImages(current => ({ ...current, [selectedImage]: true }))} />}
                        </div>

                        {images.length > 1 && <div className="review-image-thumbs">
                            {images.map((imageUrl, index) => <button
                                type="button"
                                key={`${imageUrl}-${index}`}
                                className={index === selectedImage ? "active" : ""}
                                onClick={() => setSelectedImage(index)}
                                aria-label={`View image ${index + 1}`}
                            >{brokenImages[index] ? <span className="review-thumb-fallback">!</span> : <img src={imageUrl} alt="" onError={() => setBrokenImages(current => ({ ...current, [index]: true }))} />}</button>)}
                        </div>}
                    </> : <div className="review-no-image">No product image was supplied.</div>}
                </article>

                <article className="management-card">
                    <h2>Product details</h2>
                    <dl>
                        <dt>Price</dt><dd>₹{Number(product.price || 0).toLocaleString("en-IN")}</dd>
                        <dt>Stock</dt><dd>{product.stock}</dd>
                        <dt>Category</dt><dd>{product.category || "—"}</dd>
                        <dt>Description</dt><dd>{product.description || "—"}</dd>
                    </dl>
                </article>
            </div>

            {product.approvalStatus === "PENDING" && <article className="management-card review-card">
                <h2>Decision</h2>
                <label>Approval note (optional)
                    <textarea value={review} onChange={event => setReview(event.target.value)} placeholder="Optional message for the seller" />
                </label>
                <label>Rejection reason
                    <textarea value={reason} onChange={event => setReason(event.target.value)} placeholder="Required if rejecting the product" />
                </label>
                <div className="decision-actions">
                    <button disabled={busy} className="approve" onClick={() => decide("approve")}>Approve & email</button>
                    <button disabled={busy} className="reject" onClick={() => decide("reject")}>Reject & email</button>
                </div>
            </article>}

            {product.adminReview && <article className="management-card">
                <h2>Admin review</h2>
                <p>{product.adminReview}</p>
            </article>}
        </>}
    </section>;
}

import { Link } from "react-router-dom";

export default function ProductRecommendationCard({ product, money, imageUrl, selectable, selected, onToggle }) {
    const price = product.finalPrice ?? product.finalSellingPrice ?? product.price ?? 0;

    return (
        <article className="shp-rec-card">
            {selectable && (
                <label className="shp-rec-check">
                    <input type="checkbox" checked={selected} onChange={() => onToggle(product)} />
                    Add
                </label>
            )}
            <Link to={`/product/${product.id}`} className="shp-rec-image">
                {product.imageUrl || product.images?.[0]?.imageUrl ? (
                    <img src={imageUrl(product.imageUrl || product.images?.[0]?.imageUrl)} alt={product.name} />
                ) : (
                    <span>No image</span>
                )}
            </Link>
            <div className="shp-rec-body">
                <p>{product.brand || product.category || "Product"}</p>
                <Link to={`/product/${product.id}`}>{product.name}</Link>
                <small>{[product.ram, product.storage].filter(Boolean).join(" • ") || product.subCategory}</small>
                <strong>{money(price)}</strong>
            </div>
        </article>
    );
}

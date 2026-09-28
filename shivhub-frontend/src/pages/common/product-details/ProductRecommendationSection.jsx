import ProductRecommendationCard from "./ProductRecommendationCard";

export default function ProductRecommendationSection({
    title,
    subtitle,
    products,
    money,
    imageUrl,
    selectable = false,
    selectedIds = [],
    onToggle
}) {
    if (!products?.length) return null;

    return (
        <section className="shp-rec-section">
            <div className="shp-section-head">
                <div>
                    <p>{subtitle}</p>
                    <h2>{title}</h2>
                </div>
            </div>
            <div className="shp-rec-row">
                {products.map(product => (
                    <ProductRecommendationCard
                        key={product.id}
                        product={product}
                        money={money}
                        imageUrl={imageUrl}
                        selectable={selectable}
                        selected={selectedIds.includes(Number(product.id))}
                        onToggle={onToggle}
                    />
                ))}
            </div>
        </section>
    );
}

export default function ProductDetailsSkeleton() {
    return (
        <main className="shp-product-page">
            <div className="shp-skeleton breadcrumb" />
            <section className="shp-product-shell">
                <div className="shp-skeleton gallery" />
                <div className="shp-skeleton info" />
                <div className="shp-skeleton panel" />
            </section>
        </main>
    );
}

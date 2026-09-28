import { useEffect, useState } from "react";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import { fadeIn, noTransform, scaleIn } from "../../../utils/animationVariants";

export default function ProductGallery({ product, images, selectedImage, setSelectedImage, imageUrl }) {
    const [fullScreen, setFullScreen] = useState(false);
    const reducedMotion = useReducedMotion();

    useEffect(() => {
        const closeOnEscape = event => { if (event.key === "Escape") setFullScreen(false); };
        window.addEventListener("keydown", closeOnEscape);
        return () => window.removeEventListener("keydown", closeOnEscape);
    }, []);

    const selectedUrl = selectedImage ? imageUrl(selectedImage.imageUrl) : "";

    return (
        <section className="shp-gallery" aria-label="Product images">
            <div className="shp-main-image">
                <AnimatePresence initial={false} mode="wait">
                    {selectedImage ? (
                        <motion.img
                            key={selectedImage.id || selectedUrl}
                            src={selectedUrl}
                            alt={product.name}
                            variants={reducedMotion ? noTransform : fadeIn}
                            initial="hidden"
                            animate="visible"
                            exit="exit"
                        />
                    ) : (
                        <motion.div key="image-fallback" className="shp-image-fallback" variants={fadeIn} initial="hidden" animate="visible" exit="exit">No image</motion.div>
                    )}
                </AnimatePresence>
                {selectedImage && <button type="button" className="shp-zoom-button" onClick={() => setFullScreen(true)} aria-label="View product image full screen">Expand</button>}
            </div>
            {images.length > 1 && (
                <div className="shp-thumbs" role="list">
                    {images.map((image, index) => (
                        <button
                            type="button"
                            key={image.id || image.imageUrl || index}
                            className={selectedImage?.imageUrl === image.imageUrl ? "active" : ""}
                            onClick={() => setSelectedImage(image)}
                            aria-label={`Show image ${index + 1}`}
                        >
                            <img src={imageUrl(image.imageUrl)} alt={`${product.name} ${index + 1}`} />
                        </button>
                    ))}
                </div>
            )}
            <AnimatePresence initial={false}>
                {fullScreen && selectedImage && (
                    <motion.div className="shp-image-lightbox" role="dialog" aria-modal="true" aria-label={`${product.name} image preview`} onClick={() => setFullScreen(false)} variants={reducedMotion ? noTransform : fadeIn} initial="hidden" animate="visible" exit="exit">
                        <button type="button" className="shp-lightbox-close" onClick={() => setFullScreen(false)} aria-label="Close full screen image">Close</button>
                        <motion.img src={selectedUrl} alt={product.name} onClick={event => event.stopPropagation()} variants={reducedMotion ? noTransform : scaleIn} initial="hidden" animate="visible" exit="exit" />
                    </motion.div>
                )}
            </AnimatePresence>
        </section>
    );
}

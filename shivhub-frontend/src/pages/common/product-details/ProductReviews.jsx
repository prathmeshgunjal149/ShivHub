import { MessageCircleMore, PenLine, Send, ShieldCheck, Star, UsersRound } from "lucide-react";
import { motion, useReducedMotion } from "framer-motion";

const safeNumber = value => Number(value || 0);

export default function ProductReviews({ reviewData, form, setForm, mine, working, submitReview, deleteReview, stars }) {
    const reducedMotion = useReducedMotion();
    const total = safeNumber(reviewData.totalReviews);
    const average = safeNumber(reviewData.averageRating);
    const breakdown = reviewData.ratingBreakdown || [];
    const percent = rating => total ? Math.round((safeNumber(breakdown[rating - 1]) / total) * 100) : 0;

    return <section className="shp-reviews">
        <div className="shp-reviews-grid">
            <div className="shp-review-overview">
                <div className="shp-review-heading">
                    <p>Customer reviews</p>
                    <h2>Ratings &amp; reviews</h2>
                    <span>See what verified customers say about this product.</span>
                </div>
                <div className="shp-rating-summary">
                    <div className="shp-review-score">
                        <strong>{average.toFixed(1)}</strong>
                        <span className="shp-score-stars" aria-label={`${average.toFixed(1)} out of 5 stars`}>{stars(average)}</span>
                        <b>{average.toFixed(1)} out of 5</b>
                        <small>Based on {total} review{total === 1 ? "" : "s"}</small>
                    </div>
                    <div className="shp-rating-breakdown" aria-label="Rating breakdown">
                        {[5, 4, 3, 2, 1].map(rating => {
                            const count = safeNumber(breakdown[rating - 1]);
                            return <div key={rating}>
                                <span>{rating} <Star size={14} fill="currentColor" aria-hidden="true" /></span>
                                <i><motion.b initial={{ width: 0 }} animate={{ width: `${percent(rating)}%` }} transition={reducedMotion ? { duration: 0 } : { duration: 0.48, delay: (5 - rating) * 0.05 }} /></i>
                                <em>{count} ({percent(rating)}%)</em>
                            </div>;
                        })}
                    </div>
                </div>
                <div className="shp-review-trust" aria-label="Review information">
                    <span><MessageCircleMore aria-hidden="true" /><b>Real reviews</b><small>from verified buyers</small></span>
                    <span><ShieldCheck aria-hidden="true" /><b>Authentic feedback</b><small>saved with each order</small></span>
                    <span><UsersRound aria-hidden="true" /><b>Helps you decide</b><small>make an informed choice</small></span>
                </div>
            </div>

            <form className="shp-review-form" onSubmit={submitReview}>
                <div className="shp-review-form-heading"><span><PenLine aria-hidden="true" /></span><div><h3>{mine ? "Edit your review" : "Write a review"}</h3><p>Share your experience with this product and help other customers.</p></div></div>
                <label className="shp-review-rating-label">Your rating <b>*</b></label>
                <div className="shp-star-picker" role="radiogroup" aria-label="Your rating">
                    {[1, 2, 3, 4, 5].map(rating => <motion.button type="button" key={rating} role="radio" aria-checked={form.rating === rating} aria-label={`${rating} star${rating === 1 ? "" : "s"}`} className={rating <= form.rating ? "selected" : ""} onClick={() => setForm(value => ({ ...value, rating }))} whileHover={reducedMotion ? undefined : { scale: 1.12, rotate: 4 }} whileTap={reducedMotion ? undefined : { scale: 0.92 }}><Star fill="currentColor" aria-hidden="true" /></motion.button>)}
                    <span>Click a star to rate</span>
                </div>
                <label className="shp-review-comment-label" htmlFor="product-review">Your review <b>*</b></label>
                <textarea id="product-review" required maxLength="500" value={form.comment} onChange={event => setForm(value => ({ ...value, comment: event.target.value }))} placeholder="Tell us about your experience with this product..." />
                <small className="shp-review-count">{form.comment.length}/500</small>
                <div className="shp-review-actions">
                    <motion.button type="submit" disabled={working} whileHover={reducedMotion || working ? undefined : { y: -2 }} whileTap={reducedMotion || working ? undefined : { scale: 0.98 }}><Send size={18} aria-hidden="true" />{mine ? "Save changes" : "Submit review"}</motion.button>
                    {mine && <button type="button" disabled={working} onClick={deleteReview}>Delete</button>}
                </div>
            </form>
        </div>

        <div className="shp-review-list">
            {reviewData.reviews?.length ? reviewData.reviews.map((review, index) => <motion.article key={review.id} className="shp-review-card" initial={reducedMotion ? false : { opacity: 0, y: 10 }} whileInView={reducedMotion ? undefined : { opacity: 1, y: 0 }} viewport={{ once: true, amount: 0.2 }} transition={{ duration: 0.22, delay: Math.min(index, 4) * 0.04 }}><div><strong>{review.customerName}</strong><span>{stars(review.rating)}</span></div><time>{new Date(review.updatedAt || review.createdAt).toLocaleDateString("en-IN", { year: "numeric", month: "short", day: "numeric" })}</time><p>{review.comment}</p></motion.article>) : <div className="shp-empty shp-review-empty"><MessageCircleMore aria-hidden="true" /><div><strong>No reviews yet.</strong><span>Be the first to review this product and help other customers make an informed decision.</span></div></div>}
        </div>
    </section>;
}

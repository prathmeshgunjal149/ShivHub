// Reusable loading, error and empty states for the product area.
export default function CustomerProductState({ type, message, onAction }) {
    return <div className={`marketplace-message ${type === "error" ? "error" : ""}`} role={type === "error" ? "alert" : undefined}>{type === "empty" && <h3>No approved products yet</h3>}{type === "no-results" && <h3>No matching products</h3>}<p>{message}</p>{onAction && <button type="button" onClick={onAction}>{type === "error" ? "Try again" : "Clear filters"}</button>}</div>;
}

import { API_BASE_URL } from "../../../services/api";

const fileUrl = (value) => {
    if (!value) return "";

    const stringValue = String(value).trim();

    if (/^https?:\/\//i.test(stringValue)) {
        return stringValue;
    }

    return `${API_BASE_URL}/${stringValue.replace(/^\/+/, "")}`;
};

const isImage = (value) =>
    /\.(png|jpe?g|webp|gif)(\?.*)?$/i.test(String(value || ""));





export function PurchaseBillImage({ url }) {
    if (!url) {
        return (
            <span className="purchase-file-empty">
                Not uploaded
            </span>
        );
    }

    const href = fileUrl(url);

    if (!href) {
        return (
            <span className="purchase-file-empty">
                Invalid file
            </span>
        );
    }

    return (
        <a
            className="purchase-bill-file"
            href={href}
            target="_blank"
            rel="noopener noreferrer"
            title="Open uploaded distributor bill"
        >
            {isImage(url) ? (
                <img
                    src={href}
                    alt="Uploaded distributor bill"
                    loading="lazy"
                />
            ) : (
                <span>Open bill file</span>
            )}
        </a>
    );
}



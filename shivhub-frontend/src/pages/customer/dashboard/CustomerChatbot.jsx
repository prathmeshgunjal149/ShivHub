import { useState } from "react";

/**
 * A deliberately local placeholder. It neither sends customer data nor calls an
 * API; the future assistant integration can replace its disabled composer.
 */
export default function CustomerChatbot() {
    const [open, setOpen] = useState(false);

    return (
        <aside className="customer-chatbot" aria-live="polite">
            {open && (
                <section className="customer-chatbot-panel" aria-label="ShivHub Assistant preview">
                    <header>
                        <span className="customer-chatbot-mark">SH</span>
                        <div><strong>ShivHub Assistant</strong><small>Coming soon</small></div>
                        <button type="button" onClick={() => setOpen(false)} aria-label="Close assistant">×</button>
                    </header>
                    <div className="customer-chatbot-message">
                        <span>✦</span>
                        <p>I’ll soon help you discover products, offers and order updates right here.</p>
                    </div>
                    <div className="customer-chatbot-composer">
                        <input disabled value="Chat assistant is being prepared" aria-label="Assistant preview" readOnly />
                        <button type="button" disabled aria-label="Send message">↑</button>
                    </div>
                </section>
            )}
            <button
                type="button"
                className="customer-chatbot-toggle"
                onClick={() => setOpen(current => !current)}
                aria-expanded={open}
                aria-label={open ? "Close ShivHub Assistant" : "Open ShivHub Assistant"}
            >
                <span>{open ? "×" : "✦"}</span><b>{open ? "Close" : "Need help?"}</b>
            </button>
        </aside>
    );
}

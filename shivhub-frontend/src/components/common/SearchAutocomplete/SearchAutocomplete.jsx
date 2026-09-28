import { useEffect, useId, useRef, useState } from "react";
import { AnimatePresence, motion, useReducedMotion } from "framer-motion";
import "./SearchAutocomplete.css";
import { fadeDown, noTransform } from "../../../utils/animationVariants";

const normaliseResults = response => Array.isArray(response) ? response : (response?.suggestions || []);

const escapeExpression = value => value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

export const HighlightedText = ({ text, query }) => {
    const label = String(text || "");
    const match = String(query || "").trim();
    if (!match) return label;
    const parts = label.split(new RegExp(`(${escapeExpression(match)})`, "ig"));
    return parts.map((part, index) =>
        part.toLowerCase() === match.toLowerCase()
            ? <mark key={`${part}-${index}`}>{part}</mark>
            : <span key={`${part}-${index}`}>{part}</span>
    );
};

/**
 * Controlled, accessible autocomplete field. Parent pages provide the data source and
 * selection behaviour so ordinary table filtering and scanner inputs keep their own flow.
 */
export default function SearchAutocomplete({
    value,
    onChange,
    onSelect,
    placeholder = "Search…",
    fetchSuggestions,
    minCharacters = 2,
    debounceMs = 300,
    suggestionType,
    renderSuggestion,
    getSuggestionKey = suggestion => `${suggestion.type || suggestionType || "item"}-${suggestion.id ?? suggestion.label}`,
    getSuggestionLabel = suggestion => suggestion?.label || "",
    disabled = false,
    className = "",
    inputClassName = "",
    onEnterWithoutSelection,
    scannerMode = false,
    inputRef,
    inputProps = {}
}) {
    const reducedMotion = useReducedMotion();
    const rootRef = useRef(null);
    const requestVersion = useRef(0);
    const fetchSuggestionsRef = useRef(fetchSuggestions);
    const getSuggestionKeyRef = useRef(getSuggestionKey);
    const lastInputAt = useRef(0);
    const [scannerCandidate, setScannerCandidate] = useState(false);
    const listId = useId();
    const [suggestions, setSuggestions] = useState([]);
    const [open, setOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const [completedQuery, setCompletedQuery] = useState("");
    const [highlightedIndex, setHighlightedIndex] = useState(-1);

    const query = String(value || "").trim();
    const canSearch = query.length >= minCharacters && typeof fetchSuggestions === "function" && !(scannerMode && scannerCandidate);

    useEffect(() => { fetchSuggestionsRef.current = fetchSuggestions; }, [fetchSuggestions]);
    useEffect(() => { getSuggestionKeyRef.current = getSuggestionKey; }, [getSuggestionKey]);

    useEffect(() => {
        const closeWhenOutside = event => {
            if (rootRef.current && !rootRef.current.contains(event.target)) setOpen(false);
        };
        document.addEventListener("pointerdown", closeWhenOutside);
        return () => document.removeEventListener("pointerdown", closeWhenOutside);
    }, []);

    useEffect(() => {
        if (!query) setScannerCandidate(false);
    }, [query]);

    useEffect(() => {
        const version = ++requestVersion.current;
        if (!canSearch) {
            setSuggestions([]);
            setLoading(false);
            setCompletedQuery("");
            setHighlightedIndex(-1);
            return undefined;
        }

        const timer = window.setTimeout(async () => {
            setLoading(true);
            try {
                const response = await fetchSuggestionsRef.current(query);
                if (requestVersion.current !== version) return;
                const unique = [];
                const seen = new Set();
                normaliseResults(response).forEach(item => {
                    const key = getSuggestionKeyRef.current(item);
                    if (item && !seen.has(key)) {
                        seen.add(key);
                        unique.push(item);
                    }
                });
                setSuggestions(unique);
                setCompletedQuery(query);
                setOpen(true);
                setHighlightedIndex(-1);
            } catch {
                if (requestVersion.current !== version) return;
                setSuggestions([]);
                setCompletedQuery(query);
                setOpen(true);
                setHighlightedIndex(-1);
            } finally {
                if (requestVersion.current === version) setLoading(false);
            }
        }, debounceMs);

        return () => window.clearTimeout(timer);
    }, [canSearch, debounceMs, query]);

    const selectSuggestion = suggestion => {
        setOpen(false);
        setHighlightedIndex(-1);
        onSelect?.(suggestion);
    };

    const handleKeyDown = event => {
        if (event.key === "ArrowDown" && open && suggestions.length) {
            event.preventDefault();
            setHighlightedIndex(previous => previous >= suggestions.length - 1 ? 0 : previous + 1);
            return;
        }
        if (event.key === "ArrowUp" && open && suggestions.length) {
            event.preventDefault();
            setHighlightedIndex(previous => previous <= 0 ? suggestions.length - 1 : previous - 1);
            return;
        }
        if (event.key === "Escape") {
            setOpen(false);
            setHighlightedIndex(-1);
            return;
        }
        if (event.key === "Enter") {
            event.preventDefault();
            if (scannerMode) {
                // Scanner input arrives as very fast key events. Cancel any queued suggestion
                // request and immediately hand the full code back to the existing scan flow.
                requestVersion.current += 1;
                setOpen(false);
            }
            if (!scannerMode && highlightedIndex >= 0 && suggestions[highlightedIndex]) {
                selectSuggestion(suggestions[highlightedIndex]);
            } else {
                setOpen(false);
                onEnterWithoutSelection?.(event);
            }
        }
    };

    const showDropdown = open && canSearch;
    const noResults = !loading && completedQuery === query && suggestions.length === 0;

    return (
        <div ref={rootRef} className={`search-autocomplete ${className}`.trim()}>
            <input
                {...inputProps}
                ref={inputRef}
                type={inputProps.type || "search"}
                value={value || ""}
                disabled={disabled}
                className={`search-autocomplete__input ${inputClassName}`.trim()}
                placeholder={placeholder}
                autoComplete="off"
                role="combobox"
                aria-autocomplete="list"
                aria-expanded={showDropdown}
                aria-controls={listId}
                aria-activedescendant={highlightedIndex >= 0 ? `${listId}-${highlightedIndex}` : undefined}
                onFocus={() => canSearch && setOpen(true)}
                onChange={event => {
                    const now = Date.now();
                    if (scannerMode) {
                        const isRapidInput = lastInputAt.current > 0 && now - lastInputAt.current < 70;
                        const hasMultipleCharacters = event.target.value.length > 1;
                        setScannerCandidate(previous => isRapidInput || (previous && hasMultipleCharacters));
                        lastInputAt.current = now;
                    }
                    onChange?.(event.target.value, event);
                }}
                onKeyDown={handleKeyDown}
            />
            <AnimatePresence initial={false}>
                {showDropdown && (
                <motion.div
                    id={listId}
                    className="search-autocomplete__menu"
                    role="listbox"
                    variants={reducedMotion ? noTransform : fadeDown}
                    initial="hidden"
                    animate="visible"
                    exit="exit"
                >
                    {loading && <div className="search-autocomplete__state" role="status">Searching…</div>}
                    {!loading && suggestions.map((suggestion, index) => (
                        <motion.button
                            id={`${listId}-${index}`}
                            key={getSuggestionKey(suggestion)}
                            type="button"
                            role="option"
                            aria-selected={highlightedIndex === index}
                            className={`search-autocomplete__option ${highlightedIndex === index ? "is-highlighted" : ""}`}
                            initial={reducedMotion ? { opacity: 0 } : { opacity: 0, y: -3 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ duration: 0.14, delay: Math.min(index, 5) * 0.018 }}
                            onMouseDown={event => event.preventDefault()}
                            onClick={() => selectSuggestion(suggestion)}
                        >
                            {renderSuggestion
                                ? renderSuggestion(suggestion, query)
                                : <DefaultSuggestion suggestion={suggestion} query={query} getSuggestionLabel={getSuggestionLabel} />}
                        </motion.button>
                    ))}
                    {noResults && <div className="search-autocomplete__state">No matching results found</div>}
                </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}

function DefaultSuggestion({ suggestion, query, getSuggestionLabel }) {
    return (
        <>
            {suggestion.imageUrl
                ? <img className="search-autocomplete__image" src={suggestion.imageUrl} alt="" />
                : <span className="search-autocomplete__type-icon" aria-hidden="true">⌕</span>}
            <span className="search-autocomplete__copy">
                <span className="search-autocomplete__label"><HighlightedText text={getSuggestionLabel(suggestion)} query={query} /></span>
                {suggestion.secondaryLabel && <span className="search-autocomplete__secondary">{suggestion.secondaryLabel}</span>}
            </span>
            {suggestion.type && <span className="search-autocomplete__badge">{suggestion.type}</span>}
            {suggestion.price != null && <span className="search-autocomplete__price">₹{Number(suggestion.price).toLocaleString("en-IN")}</span>}
        </>
    );
}

export const APPEARANCE_THEMES = ["light", "dark", "emerald", "violet"];
export const LANGUAGE_OPTIONS = [
    { code: "en", label: "English" },
    { code: "mr", label: "मराठी" },
    { code: "hi", label: "हिन्दी" }
];

const keyFor = (scope, preference) => `shivhub_${scope}_${preference}`;

export const readWorkspacePreference = (scope, preference, fallback) => {
    if (typeof window === "undefined") return fallback;
    return window.localStorage.getItem(keyFor(scope, preference)) || fallback;
};

export const saveWorkspacePreference = (scope, preference, value) => {
    window.localStorage.setItem(keyFor(scope, preference), value);
    window.dispatchEvent(new CustomEvent("shivhub:workspace-preferences", { detail: { scope } }));
};

export const applyWorkspaceAppearance = (scope) => {
    const theme = readWorkspacePreference(scope, "theme", "light");
    const language = readWorkspacePreference(scope, "language", "en");
    const safeTheme = APPEARANCE_THEMES.includes(theme) ? theme : "light";
    document.body.dataset[`${scope}Theme`] = safeTheme;
    document.documentElement.lang = language;
    return { theme: safeTheme, language };
};

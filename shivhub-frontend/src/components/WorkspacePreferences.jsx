import { useEffect, useState } from "react";
import {
    APPEARANCE_THEMES,
    LANGUAGE_OPTIONS,
    applyWorkspaceAppearance,
    readWorkspacePreference,
    saveWorkspacePreference
} from "../utils/workspacePreferences";

const themeNames = { light: "Ocean", dark: "Midnight", emerald: "Emerald", violet: "Violet" };

/** A local, role-scoped preference control. It does not send personal data to an API. */
export default function WorkspacePreferences({ scope, compact = false }) {
    const [theme, setTheme] = useState(() => readWorkspacePreference(scope, "theme", "light"));
    const [language, setLanguage] = useState(() => readWorkspacePreference(scope, "language", "en"));

    useEffect(() => {
        applyWorkspaceAppearance(scope);
    }, [scope, theme, language]);

    // The control is rendered in a few shared shells, so keep every copy in
    // sync without storing preferences on the server.
    useEffect(() => {
        const syncPreference = event => {
            if (event.detail?.scope !== scope) return;

            setTheme(readWorkspacePreference(scope, "theme", "light"));
            setLanguage(readWorkspacePreference(scope, "language", "en"));
            applyWorkspaceAppearance(scope);
        };

        window.addEventListener("shivhub:workspace-preferences", syncPreference);
        return () => window.removeEventListener("shivhub:workspace-preferences", syncPreference);
    }, [scope]);

    const update = (preference, value) => {
        if (preference === "theme") setTheme(value);
        else setLanguage(value);
        saveWorkspacePreference(scope, preference, value);
    };

    return <div className={`workspace-preferences ${compact ? "workspace-preferences--compact" : ""}`} aria-label="Appearance and language preferences">
        <label>
            <span>Appearance</span>
            <select value={theme} onChange={event => update("theme", event.target.value)}>
                {APPEARANCE_THEMES.map(option => <option key={option} value={option}>{themeNames[option]}</option>)}
            </select>
        </label>
        <label>
            <span>Language</span>
            <select value={language} onChange={event => update("language", event.target.value)}>
                {LANGUAGE_OPTIONS.map(option => <option key={option.code} value={option.code}>{option.label}</option>)}
            </select>
        </label>
    </div>;
}

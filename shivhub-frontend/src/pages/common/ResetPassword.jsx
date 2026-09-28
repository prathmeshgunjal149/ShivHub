import { useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import api from "../../services/api";
import "./Login.css";

export default function ResetPassword() {
    const navigate = useNavigate();
    const [params] = useSearchParams();
    const token = useMemo(() => params.get("token") || "", [params]);
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const submit = async (event) => {
        event.preventDefault();
        setError("");
        setMessage("");
        if (newPassword.length < 6) {
            setError("Password must be at least 6 characters.");
            return;
        }
        if (newPassword !== confirmPassword) {
            setError("Passwords do not match.");
            return;
        }
        try {
            setLoading(true);
            const { data } = await api.post("/api/auth/reset-password", { token, newPassword });
            setMessage(data?.message || "Password reset successfully.");
        } catch (err) {
            setError(err.response?.data?.message || "Reset link is invalid or expired.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <main className="login-page">
            <section className="login-container reset-card">
                <div className="login-card-header">
                    <strong>New <span>Password</span></strong>
                    <p className="login-subtitle">Create a new password for your customer/seller account.</p>
                </div>
                {error && <div className="login-error">{error}</div>}
                {message && <div className="login-success">{message}</div>}
                {!token ? (
                    <div className="login-error">Reset token is missing. Please request a new link.</div>
                ) : (
                    <form onSubmit={submit}>
                        <div className="form-group">
                            <label htmlFor="password">New password</label>
                            <input
                                id="password"
                                type="password"
                                value={newPassword}
                                onChange={(event) => setNewPassword(event.target.value)}
                                autoComplete="new-password"
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label htmlFor="confirmPassword">Confirm password</label>
                            <input
                                id="confirmPassword"
                                type="password"
                                value={confirmPassword}
                                onChange={(event) => setConfirmPassword(event.target.value)}
                                autoComplete="new-password"
                                required
                            />
                        </div>
                        <button type="submit" disabled={loading}>{loading ? "Saving..." : "Reset password"}</button>
                    </form>
                )}
                <p className="register-link">
                    Go back?
                    <button type="button" onClick={() => navigate("/login")}>Login</button>
                </p>
            </section>
        </main>
    );
}

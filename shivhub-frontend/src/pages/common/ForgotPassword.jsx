import { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../services/api";
import "./Login.css";

export default function ForgotPassword() {
    const navigate = useNavigate();
    const [email, setEmail] = useState("");
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    const submit = async (event) => {
        event.preventDefault();
        setError("");
        setMessage("");
        try {
            setLoading(true);
            const { data } = await api.post("/api/auth/forgot-password", { email });
            setMessage(data?.message || "If the email exists, a reset link has been sent.");
        } catch (err) {
            setError(err.response?.data?.message || "Unable to request password reset.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <main className="login-page">
            <section className="login-container reset-card">
                <div className="login-card-header">
                    <strong>Reset <span>Password</span></strong>
                    <p className="login-subtitle">Customer and seller accounts only. Admin reset is not public.</p>
                </div>
                {error && <div className="login-error">{error}</div>}
                {message && <div className="login-success">{message}</div>}
                <form onSubmit={submit}>
                    <div className="form-group">
                        <label htmlFor="email">Registered email</label>
                        <input
                            id="email"
                            type="email"
                            value={email}
                            onChange={(event) => setEmail(event.target.value)}
                            autoComplete="email"
                            required
                        />
                    </div>
                    <button type="submit" disabled={loading}>{loading ? "Sending..." : "Send reset link"}</button>
                </form>
                <p className="register-link">
                    Remembered it?
                    <button type="button" onClick={() => navigate("/login")}>Back to login</button>
                </p>
            </section>
        </main>
    );
}

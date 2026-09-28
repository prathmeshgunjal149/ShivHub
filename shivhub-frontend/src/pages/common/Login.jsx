import { useState } from "react";
import { useNavigate } from "react-router-dom";
import useAuth from "../../hooks/useAuth";
import api from "../../services/api";
import "./Login.css";

const Login = () => {
    const navigate = useNavigate();
    const { login, verifyLoginOtp } = useAuth();

    const [formData, setFormData] = useState({
        email: "",
        password: ""
    });
    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);
    const [otpRequired, setOtpRequired] = useState(false);
    const [otp, setOtp] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID || "";
    const facebookAppId = import.meta.env.VITE_FACEBOOK_APP_ID || "";

    const handleChange = event => {
        const { name, value } = event.target;
        setFormData(previous => ({ ...previous, [name]: value }));
    };

    const redirectAfterLogin = async response => {
        if (response.role === "ADMIN") {
            sessionStorage.removeItem("shivhub_pending_customer_action");
            navigate("/admin/dashboard");
            return;
        }

        if (response.role === "SELLER") {
            sessionStorage.removeItem("shivhub_pending_customer_action");
            navigate("/seller/dashboard");
            return;
        }

        const pendingRaw = sessionStorage.getItem("shivhub_pending_customer_action");
        if (pendingRaw) {
            try {
                const pending = JSON.parse(pendingRaw);
                const safeReturn = String(pending.returnTo || "");
                if (pending.type === "ADD_TO_CART" && Number(pending.productId) && safeReturn.startsWith("/product/")) {
                    await api.post("/api/cart", {
                        productId: Number(pending.productId),
                        quantity: Math.max(1, Number(pending.quantity || 1))
                    });
                    sessionStorage.removeItem("shivhub_pending_customer_action");
                    navigate(pending.buyNow ? "/customer/checkout" : safeReturn);
                    return;
                }
            } catch {
                sessionStorage.removeItem("shivhub_pending_customer_action");
            }
        }

        navigate("/customer/dashboard");
    };

    const handleSubmit = async event => {
        event.preventDefault();
        setError("");
        setLoading(true);

        try {
            const response = await login(formData);
            if (response.otpRequired) {
                setOtpRequired(true);
                return;
            }
            await redirectAfterLogin(response);
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Invalid email or password");
        } finally {
            setLoading(false);
        }
    };

    const handleOtpSubmit = async event => {
        event.preventDefault();
        setError("");
        setLoading(true);

        try {
            const response = await verifyLoginOtp(formData.email, otp);
            await redirectAfterLogin(response);
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Invalid or expired verification code");
        } finally {
            setLoading(false);
        }
    };

    const handleResendOtp = async () => {
        setError("");
        setLoading(true);

        try {
            await login(formData);
            setOtp("");
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Unable to send a new verification code");
        } finally {
            setLoading(false);
        }
    };

    const socialLogin = async (provider, token) => {
        setError("");
        setLoading(true);
        try {
            const response = await api.post("/api/auth/social-login", { provider, token });
            await redirectAfterLogin(response.data);
        } catch (requestError) {
            setError(requestError.response?.data?.message || `${provider} login failed`);
        } finally {
            setLoading(false);
        }
    };

    const loadScript = src => new Promise((resolve, reject) => {
        const existing = document.querySelector(`script[src="${src}"]`);
        if (existing) {
            resolve();
            return;
        }
        const script = document.createElement("script");
        script.src = src;
        script.async = true;
        script.defer = true;
        script.onload = resolve;
        script.onerror = reject;
        document.body.appendChild(script);
    });

    const handleGoogleLogin = async () => {
        if (!googleClientId) {
            setError("Google login is not configured. Add VITE_GOOGLE_CLIENT_ID.");
            return;
        }
        try {
            await loadScript("https://accounts.google.com/gsi/client");
            window.google.accounts.id.initialize({
                client_id: googleClientId,
                callback: credentialResponse => socialLogin("GOOGLE", credentialResponse.credential)
            });
            window.google.accounts.id.prompt(notification => {
                if (notification.isNotDisplayed?.() || notification.isSkippedMoment?.()) {
                    setError("Google popup was blocked or closed. Please try again.");
                }
            });
        } catch {
            setError("Could not load Google login.");
        }
    };

    const handleFacebookLogin = async () => {
        if (!facebookAppId) {
            setError("Facebook login is not configured. Add VITE_FACEBOOK_APP_ID.");
            return;
        }
        try {
            await loadScript("https://connect.facebook.net/en_US/sdk.js");
            window.FB.init({ appId: facebookAppId, cookie: false, xfbml: false, version: "v20.0" });
            window.FB.login(response => {
                if (response.authResponse?.accessToken) {
                    socialLogin("FACEBOOK", response.authResponse.accessToken);
                } else {
                    setError("Facebook login was cancelled.");
                }
            }, { scope: "email,public_profile" });
        } catch {
            setError("Could not load Facebook login.");
        }
    };

    return (
        <main className="login-page">
            <div className="auth-canvas-brand" aria-label="ShivHub">
                <strong><span aria-hidden="true">⌑</span> Shiv<em>Hub</em></strong>
                <small>Mobile covers &nbsp;|&nbsp; Cases &nbsp;|&nbsp; Accessories</small>
            </div>
            <section className="login-shell">
                <aside className="login-brand-panel" aria-label="ShivHub welcome">
                    <button type="button" className="back-shopping" onClick={() => navigate("/")}>Back to shopping</button>
                    <div className="brand-orbit orbit-one" aria-hidden="true" />
                    <div className="brand-orbit orbit-two" aria-hidden="true" />
                    <div className="login-brand-copy">
                        <p>One secure ShivHub login</p>
                        <h1>Welcome back to your electronics command center.</h1>
                        <span>Customers shop, sellers manage stock, and admins run operations from the same trusted sign-in flow.</span>
                    </div>
                    <div className="login-visual" aria-hidden="true">
                        <span />
                        <span />
                        <span />
                    </div>
                    <div className="login-role-strip" aria-hidden="true"><span>🛒 &nbsp;Shop</span><span>🏪 &nbsp;Sell</span><span>▥ &nbsp;Manage</span></div>
                </aside>

                <section className="login-container" aria-label="Login form">
                    <div className="login-card-header">
                        <strong>Shiv<span>Hub</span></strong>
                        <p className="login-subtitle">
                            {otpRequired
                                ? `A 6-digit verification code was sent to ${formData.email}`
                                : "Use your existing customer, seller or admin account."}
                        </p>
                    </div>

                    {error && <div className="login-error">{error}</div>}

                    {!otpRequired ? (
                        <form onSubmit={handleSubmit}>
                            <div className="form-group">
                                <label htmlFor="email">Email</label>
                                <input
                                    id="email"
                                    type="email"
                                    name="email"
                                    placeholder="you@example.com"
                                    value={formData.email}
                                    onChange={handleChange}
                                    autoComplete="username"
                                    required
                                />
                            </div>

                            <div className="form-group">
                                <label htmlFor="password">Password</label>
                                <div className="password-field">
                                    <input
                                        id="password"
                                        type={showPassword ? "text" : "password"}
                                        name="password"
                                        placeholder="Enter your password"
                                        value={formData.password}
                                        onChange={handleChange}
                                        autoComplete="current-password"
                                        required
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowPassword(value => !value)}
                                        aria-label={showPassword ? "Hide password" : "Show password"}
                                    >
                                        {showPassword ? "Hide" : "Show"}
                                    </button>
                                </div>
                            </div>

                            <button type="submit" disabled={loading}>
                                {loading ? "Checking..." : "Login securely"}
                            </button>
                            <div className="social-login-divider"><span>Customer social login</span></div>
                            <div className="social-login-actions">
                                <button type="button" className="google-login" onClick={handleGoogleLogin} disabled={loading}>
                                    Continue with Google
                                </button>
                                <button type="button" className="facebook-login" onClick={handleFacebookLogin} disabled={loading}>
                                    Continue with Facebook
                                </button>
                            </div>
                            <p className="forgot-password-link">
                                <button type="button" onClick={() => navigate("/forgot-password")}>
                                    Forgot password?
                                </button>
                            </p>
                        </form>
                    ) : (
                        <form className="otp-form" onSubmit={handleOtpSubmit}>
                            <div className="otp-icon" aria-hidden="true">✉</div>
                            <div className="otp-heading">
                                <h2>Verify your email</h2>
                                <p>Enter the 6-digit code sent to <strong>{formData.email}</strong>.</p>
                            </div>
                            <div className="form-group">
                                <label htmlFor="otp">Verification code</label>
                                <input
                                    id="otp"
                                    className="otp-input"
                                    type="text"
                                    inputMode="numeric"
                                    autoComplete="one-time-code"
                                    placeholder="000000"
                                    value={otp}
                                    onChange={event => setOtp(event.target.value.replace(/\D/g, "").slice(0, 6))}
                                    required
                                    pattern="[0-9]{6}"
                                    maxLength={6}
                                />
                            </div>
                            <button type="submit" disabled={loading || otp.length !== 6}>
                                {loading ? "Verifying..." : "Verify and login"}
                            </button>
                            <p className="otp-resend">
                                Didn&apos;t receive the code?
                                <button type="button" onClick={handleResendOtp} disabled={loading}>Resend code</button>
                            </p>
                            <button
                                className="otp-back-button"
                                type="button"
                                onClick={() => { setOtpRequired(false); setOtp(""); setError(""); }}
                            >
                                Use a different account
                            </button>
                        </form>
                    )}

                    <p className="register-link">
                        Don&apos;t have an account?
                        <button type="button" onClick={() => navigate("/register")}>Register</button>
                    </p>
                </section>
            </section>
        </main>
    );
};

export default Login;

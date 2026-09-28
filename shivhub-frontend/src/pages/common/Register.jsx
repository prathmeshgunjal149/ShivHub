import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import authService from "../../services/authService";
import "./Register.css";
import "./RegisterPolish.css";

const initialForm = {
    name: "",
    email: "",
    mobile: "",
    password: "",
    dateOfBirth: "",
    businessName: "",
    gstin: "",
    businessAddress: "",
    referralCode: new URLSearchParams(window.location.search).get("ref") || ""
};

const Register = () => {
    const navigate = useNavigate();
    const [accountType, setAccountType] = useState("CUSTOMER");
    const [formData, setFormData] = useState(initialForm);
    const [sellerOtpSent, setSellerOtpSent] = useState(false);
    const [sellerOtp, setSellerOtp] = useState("");
    const [customerOtpSent, setCustomerOtpSent] = useState(false);
    const [customerOtp, setCustomerOtp] = useState("");
    const [resendIn, setResendIn] = useState(0);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const [loading, setLoading] = useState(false);
    const [showPassword, setShowPassword] = useState(false);

    const handleChange = event => {
        const { name, value } = event.target;
        setFormData(previous => ({ ...previous, [name]: value }));
    };

    const handleAccountTypeChange = type => {
        setAccountType(type);
        setSellerOtpSent(false);
        setSellerOtp("");
        setCustomerOtpSent(false);
        setCustomerOtp("");
        setResendIn(0);
        setError("");
        setSuccess("");
    };

    useEffect(() => {
        if (!resendIn) return undefined;
        const timer = window.setInterval(() => setResendIn(value => Math.max(0, value - 1)), 1000);
        return () => window.clearInterval(timer);
    }, [resendIn]);

    const handleSubmit = async event => {
        event.preventDefault();
        setError("");
        setSuccess("");
        setLoading(true);

        try {
            if (accountType === "CUSTOMER") {
                if (!customerOtpSent) {
                    await authService.register(formData);
                    await authService.sendWhatsAppRegistrationOtp(formData.email);
                    setCustomerOtpSent(true);
                    setResendIn(60);
                    setSuccess("WhatsApp OTP sent. Verify your mobile number to activate login.");
                    return;
                }
                await authService.verifyWhatsAppRegistrationOtp(formData.email, customerOtp);
            } else if (!sellerOtpSent) {
                await authService.requestSellerRegistrationOtp(formData);
                setSellerOtpSent(true);
                setSuccess("OTP sent to your email. Enter the code to submit seller application.");
                return;
            } else {
                await authService.verifySellerRegistrationOtp(formData.email, sellerOtp);
            }

            setSuccess(accountType === "CUSTOMER"
                ? "Mobile verified successfully! Redirecting to login..."
                : "Seller registration submitted successfully! Waiting for Admin approval.");
            setFormData({ ...initialForm, referralCode: "" });
            setSellerOtp("");
            setSellerOtpSent(false);
            setTimeout(() => navigate("/login"), 2500);
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Registration failed. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    const isSeller = accountType === "SELLER";

    return (
        <main className="register-page">
            <div className="register-canvas-brand" aria-label="ShivHub">
                <strong><span aria-hidden="true">⌑</span> Shiv<em>Hub</em></strong>
                <small>Mobile covers &nbsp;|&nbsp; Cases &nbsp;|&nbsp; Accessories</small>
            </div>
            <section className="register-shell">
                <aside className="register-story-panel">
                    <button type="button" className="register-back" onClick={() => navigate("/")}>Back to shopping</button>
                    <span className="register-orbit orbit-a" aria-hidden="true" />
                    <span className="register-orbit orbit-b" aria-hidden="true" />
                    <div>
                        <p>Join ShivHub</p>
                        <h1>{isSeller ? "Start selling with cleaner stock control." : "Create your shopping account."}</h1>
                        <span>
                            {isSeller
                                ? "Submit your shop details, verify email OTP, and wait for admin approval before accessing seller tools."
                                : "Browse electronics, save cart actions and track your orders from one secure account."}
                        </span>
                    </div>
                    <div className="register-feature-grid" aria-hidden="true">
                        <span>IMEI stock</span>
                        <span>Invoices</span>
                        <span>Orders</span>
                    </div>
                </aside>

                <section className="register-container">
                    <div className="register-brand">
                        <div className="brand-icon">S</div>
                        <h1>Shiv<span>Hub</span></h1>
                    </div>

                    <h2>Create your account</h2>
                    <p className="register-subtitle">One onboarding page for customers and sellers.</p>

                    <div className="account-type-section">
                        <p className="account-type-label">I want to register as</p>
                        <div className="account-type-tabs">
                            <button type="button" className={accountType === "CUSTOMER" ? "account-type active" : "account-type"} onClick={() => handleAccountTypeChange("CUSTOMER")}>
                                <span className="type-icon">👤</span>
                                <span><strong>Customer</strong><small>Shop products</small></span>
                            </button>
                            <button type="button" className={accountType === "SELLER" ? "account-type active" : "account-type"} onClick={() => handleAccountTypeChange("SELLER")}>
                                <span className="type-icon">🏪</span>
                                <span><strong>Seller</strong><small>Sell products</small></span>
                            </button>
                        </div>
                    </div>

                    {isSeller && (
                        <div className="seller-info-box">
                            <div className="seller-info-icon">✓</div>
                            <div>
                                <strong>Seller approval required</strong>
                                <p>Email OTP verification is required first. Admin approval is required before seller dashboard access.</p>
                            </div>
                        </div>
                    )}

                    {error && <div className="register-message error"><span>!</span>{error}</div>}
                    {success && <div className="register-message success"><span>✓</span>{success}</div>}

                    <form className="register-form" onSubmit={handleSubmit}>
                        <div className="register-form-grid">
                            <label className="register-form-group">
                                <span>{isSeller ? "Owner / Contact Person" : "Full Name"}</span>
                                <div className="input-wrapper">
                                    <input id="name" type="text" name="name" placeholder={isSeller ? "Owner name" : "Your full name"} value={formData.name} onChange={handleChange} autoComplete="name" required />
                                </div>
                            </label>

                            {isSeller && (
                                <label className="register-form-group">
                                    <span>Shop / Business Name</span>
                                    <div className="input-wrapper">
                                        <input id="businessName" type="text" name="businessName" placeholder="Shop name" value={formData.businessName} onChange={handleChange} required />
                                    </div>
                                </label>
                            )}

                            <label className="register-form-group">
                                <span>Email Address</span>
                                <div className="input-wrapper">
                                    <input id="email" type="email" name="email" placeholder="you@example.com" value={formData.email} onChange={handleChange} autoComplete="email" required />
                                </div>
                            </label>

                            <label className="register-form-group">
                                <span>Mobile Number</span>
                                <div className="input-wrapper">
                                    <input id="mobile" type="tel" name="mobile" placeholder="10-digit mobile number" value={formData.mobile} onChange={handleChange} autoComplete="tel" maxLength={10} pattern="[6-9][0-9]{9}" required />
                                </div>
                            </label>

                            {accountType === "CUSTOMER" && (
                                <label className="register-form-group">
                                    <span>Date of Birth</span>
                                    <div className="input-wrapper"><input id="dateOfBirth" type="date" name="dateOfBirth" value={formData.dateOfBirth} onChange={handleChange} max={new Date().toISOString().slice(0, 10)} required /></div>
                                </label>
                            )}

                            {accountType === "CUSTOMER" && customerOtpSent && (
                                <label className="register-form-group">
                                    <span>WhatsApp OTP verification</span>
                                    <div className="input-wrapper"><input id="customerOtp" type="text" inputMode="numeric" placeholder="6-digit OTP" value={customerOtp} onChange={event => setCustomerOtp(event.target.value.replace(/\D/g, "").slice(0, 6))} minLength={6} maxLength={6} required /></div>
                                    <small className="password-hint">OTP expires in 5 minutes. {resendIn ? `Resend in ${resendIn}s` : <button type="button" className="link-button" onClick={async () => { try { setLoading(true); await authService.resendWhatsAppRegistrationOtp(formData.email); setResendIn(60); setSuccess("A new WhatsApp OTP was sent."); } catch (e) { setError(e.response?.data?.message || "Could not resend OTP."); } finally { setLoading(false); } }}>Resend OTP</button>}</small>
                                </label>
                            )}

                            {isSeller && (
                                <label className="register-form-group">
                                    <span>GSTIN Number</span>
                                    <div className="input-wrapper">
                                        <input id="gstin" type="text" name="gstin" placeholder="15-character GSTIN" value={formData.gstin} onChange={handleChange} minLength={15} maxLength={15} required />
                                    </div>
                                </label>
                            )}

                            {accountType === "CUSTOMER" && (
                                <label className="register-form-group">
                                    <span>Referral Code <small>(optional)</small></span>
                                    <div className="input-wrapper">
                                        <input id="referralCode" type="text" name="referralCode" placeholder="Friend referral code" value={formData.referralCode} onChange={handleChange} maxLength={20} />
                                    </div>
                                    <small className="password-hint">Reward applies after eligible delivered order.</small>
                                </label>
                            )}

                            <label className="register-form-group password-group">
                                <span>Password</span>
                                <div className="input-wrapper password-wrapper">
                                    <input id="password" type={showPassword ? "text" : "password"} name="password" placeholder="Minimum 6 characters" value={formData.password} onChange={handleChange} autoComplete="new-password" minLength={6} required />
                                    <button type="button" onClick={() => setShowPassword(value => !value)}>{showPassword ? "Hide" : "Show"}</button>
                                </div>
                            </label>

                            {isSeller && (
                                <label className="register-form-group full">
                                    <span>Business Address</span>
                                    <div className="input-wrapper textarea-wrapper">
                                        <textarea id="businessAddress" name="businessAddress" placeholder="Complete shop address" value={formData.businessAddress} onChange={handleChange} required />
                                    </div>
                                </label>
                            )}

                            {isSeller && sellerOtpSent && (
                                <label className="register-form-group">
                                    <span>Email OTP Verification</span>
                                    <div className="input-wrapper">
                                        <input id="sellerOtp" type="text" inputMode="numeric" placeholder="6-digit OTP" value={sellerOtp} onChange={event => setSellerOtp(event.target.value.replace(/\D/g, "").slice(0, 6))} minLength={6} maxLength={6} required />
                                    </div>
                                </label>
                            )}
                        </div>

                        <button type="submit" className="register-button" disabled={loading}>
                            {loading ? "Please wait..." : isSeller ? (sellerOtpSent ? "Verify OTP & Submit Application" : "Send Email OTP") : (customerOtpSent ? "Verify WhatsApp OTP" : "Create Customer Account")}
                        </button>
                    </form>

                    <p className="register-policy-links">
                        By continuing you agree to ShivHub <Link to="/terms">Terms</Link>, <Link to="/privacy-policy">Privacy Policy</Link>
                        {isSeller && <> and <Link to="/seller-terms">Seller Terms</Link></>}.
                    </p>

                    <div className="register-login">
                        <span>Already have an account?</span>
                        <button type="button" onClick={() => navigate("/login")}>Login</button>
                    </div>
                </section>
            </section>
        </main>
    );
};

export default Register;

const ProfileNav = ({ navigate }) => <nav className="profile-nav" aria-label="Customer navigation"><button type="button" className="profile-brand" onClick={() => navigate("/customer/dashboard")}><span aria-hidden="true"><ShoppingBag size={21} /></span><strong>Shiv<em>Hub</em></strong></button><div><button type="button" onClick={() => navigate("/customer/dashboard")}><Home size={18} /> Home</button><button type="button" onClick={() => navigate("/customer/orders")}><Package size={18} /> Orders</button><button type="button" onClick={() => navigate("/customer/wishlist")}><Heart size={18} /> Wishlist</button><button type="button" onClick={() => navigate("/customer/cart")}><ShoppingCart size={18} /> Cart</button><button type="button" className="active" onClick={() => navigate("/customer/profile")}><UserRound size={18} /> Profile</button></div></nav>;

import {
    useEffect,
    useState
} from "react";

import {
    useNavigate
} from "react-router-dom";

import api from "../../services/api";
import { Heart, Home, Package, Pencil, ShoppingBag, ShoppingCart, UserRound } from "lucide-react";

import "./CustomerProfile.css";
import "./CustomerProfileTheme.css";
import "./CustomerProfileFix.css";


export default function CustomerProfile() {

    const navigate = useNavigate();
    


    const [
        profile,
        setProfile
    ] = useState(null);

    const [loyalty, setLoyalty] = useState(null);
    const [garage, setGarage] = useState([]);
    const [serviceHistory, setServiceHistory] = useState([]);
    const [passwordForm, setPasswordForm] = useState({ currentPassword: "", newPassword: "" });
    const [passwordBusy, setPasswordBusy] = useState(false);


    const [
        form,
        setForm
    ] = useState({
        name: "",
        mobile: "",
        dateOfBirth: "",
        profilePhotoUrl: ""
    });


    const [
        loading,
        setLoading
    ] = useState(true);


    const [
        saving,
        setSaving
    ] = useState(false);


    const [
        error,
        setError
    ] = useState("");


    const [
        success,
        setSuccess
    ] = useState("");


    const [
        editing,
        setEditing
    ] = useState(false);

    const [
        photoBusy,
        setPhotoBusy
    ] = useState(false);


    /*
     * =====================================================
     * LOAD PROFILE
     * =====================================================
     */

    const loadProfile = async () => {

        try {

            setLoading(true);

            setError("");


            const response =
                await api.get(
                    "/api/customer/profile"
                );


            setProfile(
                response.data
            );

            api.get("/api/customer/loyalty").then(result => setLoyalty(result.data)).catch(() => setLoyalty(null));
            api.get("/api/customer/after-sales/eligible-purchases").then(result => setGarage(result.data || [])).catch(() => setGarage([]));
            api.get("/api/customer/after-sales/requests").then(result => setServiceHistory(result.data || [])).catch(() => setServiceHistory([]));


            setForm({
                name:
                    response.data?.name || "",

                mobile:
                    response.data?.mobile || "",

                dateOfBirth: response.data?.dateOfBirth || "",

                profilePhotoUrl:
                    response.data?.profilePhotoUrl || ""
            });


        } catch (err) {

            console.error(
                "Profile loading failed:",
                err
            );


            if (
                err.response?.status === 401 ||
                err.response?.status === 403
            ) {

                setError(
                    "Your login session has expired."
                );

            } else {

                setError(
                    err.response
                        ?.data
                        ?.message ||
                    "Unable to load your profile."
                );
            }


        } finally {

            setLoading(false);

        }
    };


    /*
     * =====================================================
     * INITIAL LOAD
     * =====================================================
     */

    useEffect(() => {

        loadProfile();

    }, []);


    /*
     * =====================================================
     * FORM CHANGE
     * =====================================================
     */

    const handleChange = (
        event
    ) => {

        const {
            name,
            value
        } = event.target;


        setForm(
            previous => ({
                ...previous,
                [name]: value
            })
        );

    };


    /*
     * =====================================================
     * SAVE PROFILE
     * =====================================================
     */

    const handleSave = async (
        event
    ) => {

        event.preventDefault();


        try {

            setSaving(true);

            setError("");

            setSuccess("");


            const response =
                await api.put(
                    "/api/customer/profile",
                    {
                        name:
                            form.name.trim(),

                        mobile:
                            form.mobile.trim(),

                        dateOfBirth: form.dateOfBirth || null,

                        profilePhotoUrl:
                            form.profilePhotoUrl.trim()
                    }
                );


            setProfile(
                response.data
            );


            setForm({
                name:
                    response.data?.name || "",

                mobile:
                    response.data?.mobile || "",

                dateOfBirth: response.data?.dateOfBirth || "",

                profilePhotoUrl:
                    response.data?.profilePhotoUrl || ""
            });


            setEditing(false);


            setSuccess(
                "Profile updated successfully."
            );


        } catch (err) {

            console.error(
                "Profile update failed:",
                err
            );


            setError(
                err.response
                    ?.data
                    ?.message ||
                "Unable to update profile."
            );


        } finally {

            setSaving(false);

        }
    };


    /*
     * =====================================================
     * CANCEL EDIT
     * =====================================================
     */

    const handleCancel = () => {

        setForm({
            name:
                profile?.name || "",

            mobile:
                profile?.mobile || "",

            dateOfBirth: profile?.dateOfBirth || "",

            profilePhotoUrl:
                profile?.profilePhotoUrl || ""
        });


        setEditing(false);

        setError("");

        setSuccess("");

    };

    const handlePhotoUpload = async (event) => {
        const file = event.target.files?.[0];
        event.target.value = "";
        if (!file) return;

        if (!file.type.startsWith("image/")) {
            setError("Please select a valid image file.");
            return;
        }

        if (file.size > 2 * 1024 * 1024) {
            setError("Profile photo must be 2 MB or smaller.");
            return;
        }

        const formData = new FormData();
        formData.append("file", file);

        try {
            setPhotoBusy(true);
            setError("");
            setSuccess("");
            const response = await api.post("/api/customer/profile/photo", formData, {
                headers: { "Content-Type": "multipart/form-data" }
            });
            setProfile(response.data);
            setForm(previous => ({
                ...previous,
                profilePhotoUrl: response.data?.profilePhotoUrl || ""
            }));
            setSuccess("Profile photo uploaded successfully.");
        } catch (err) {
            setError(err.response?.data?.message || "Unable to upload profile photo.");
        } finally {
            setPhotoBusy(false);
        }
    };

    const handlePhotoRemove = async () => {
        try {
            setPhotoBusy(true);
            setError("");
            setSuccess("");
            const response = await api.delete("/api/customer/profile/photo");
            setProfile(response.data);
            setForm(previous => ({
                ...previous,
                profilePhotoUrl: ""
            }));
            setSuccess("Profile photo removed.");
        } catch (err) {
            setError(err.response?.data?.message || "Unable to remove profile photo.");
        } finally {
            setPhotoBusy(false);
        }
    };

    const handleMarketingPreference = async (optOut) => {
        try {
            setSaving(true);
            setError("");
            setSuccess("");
            const response = await api.put("/api/customer/profile/marketing-preference", { optOut });
            setProfile(response.data);
            setSuccess(optOut ? "Marketing messages turned off." : "Marketing messages turned on.");
        } catch (err) {
            setError(err.response?.data?.message || "Unable to update communication preference.");
        } finally {
            setSaving(false);
        }
    };

    const changePassword = async event => {
        event.preventDefault();
        try {
            setPasswordBusy(true); setError(""); setSuccess("");
            const { data } = await api.post("/api/auth/change-password", passwordForm);
            setPasswordForm({ currentPassword: "", newPassword: "" });
            setSuccess(data?.message || "Password changed successfully.");
        } catch (err) { setError(err.response?.data?.message || "Unable to change password."); }
        finally { setPasswordBusy(false); }
    };


    /*
     * =====================================================
     * LOADING
     * =====================================================
     */

    if (loading) {

        return (

            <main className="customer-profile-page">

                <div className="customer-profile-loading">

                    <div className="customer-profile-spinner" />

                    <p>
                        Loading your profile...
                    </p>

                </div>

            </main>
        );
    }


    /*
     * =====================================================
     * ERROR
     * =====================================================
     */

    if (error && !profile) {

        return (

            <main className="customer-profile-page">

                <div className="customer-profile-error">

                    <div className="customer-profile-error-icon">
                        ⚠
                    </div>

                    <h2>
                        Unable to Load Profile
                    </h2>

                    <p>
                        {error}
                    </p>

                    <button
                        type="button"
                        onClick={
                            loadProfile
                        }
                    >
                        Try Again
                    </button>

                </div>

            </main>
        );
    }


    /*
     * =====================================================
     * MAIN PROFILE
     * =====================================================
     */

    return (

        <main className="customer-profile-page">

            <ProfileNav navigate={navigate} />


            {/* =================================================
                TOP BAR
            ================================================= */}

            <header className="customer-profile-topbar">

                <button
                    type="button"
                    className="customer-profile-back"
                    onClick={() =>
                        navigate(
                            "/customer/dashboard"
                        )
                    }
                >
                    ← Back to Dashboard
                </button>


                <div className="customer-profile-top-title">

                    <span>
                        SHIVHUB ACCOUNT
                    </span>

                    <h1>
                        My Profile
                    </h1>

                </div>


                <button
                    type="button"
                    className="customer-profile-cart"
                    onClick={() =>
                        navigate(
                            "/customer/cart"
                        )
                    }
                >
                    🛒 Cart
                </button>

            </header>



            {/* =================================================
                CONTENT
            ================================================= */}

            <section className="customer-profile-content">


                {/* =================================================
                    PROFILE HERO
                ================================================= */}

                <div className="customer-profile-hero">


                    <div className="customer-profile-avatar-wrap">
                        {
                            profile?.profilePhotoUrl ? (

                                <img
                                    className="customer-profile-avatar-large customer-profile-avatar-image"
                                    src={
                                        profile.profilePhotoUrl
                                    }
                                    alt={
                                        `${profile?.name || "Customer"} profile`
                                    }
                                />

                            ) : (

                                <div className="customer-profile-avatar-large">

                                    {
                                        profile?.name
                                            ?.charAt(0)
                                            ?.toUpperCase() ||
                                        "U"
                                    }

                                </div>

                            )
                        }
                        <button
                            type="button"
                            className="customer-profile-avatar-edit"
                            aria-label="Edit profile photo"
                            title="Upload or change profile photo"
                            onClick={() => setEditing(true)}
                        >
                            <Pencil size={14} />
                        </button>
                    </div>


                    <div>

                        <span>
                            CUSTOMER ACCOUNT
                        </span>

                        <h2>
                            {profile?.name}
                        </h2>

                        <p>
                            {profile?.email}
                        </p>

                        <label className="customer-profile-optout">
                            <input
                                type="checkbox"
                                checked={Boolean(profile?.marketingOptOut)}
                                onChange={event => handleMarketingPreference(event.target.checked)}
                                disabled={saving}
                            />
                            <span>
                                Do not send me greeting / campaign emails
                            </span>
                        </label>

                    </div>

                    <aside className="customer-profile-member-card">
                        <span>✦</span>
                        <div>
                            <strong>ShivHub member</strong>
                            <small>{loyalty?.availablePoints || 0} reward points available</small>
                        </div>
                        <button type="button" onClick={() => navigate("/customer/products")}>Explore rewards</button>
                    </aside>


                </div>

                {loyalty && <section className="customer-profile-card"><div className="customer-profile-section-heading"><div><span>SHIVHUB REWARDS</span><h2>Credit points</h2></div></div><div className="customer-profile-summary"><article><span>Available</span><strong>{loyalty.availablePoints || 0}</strong></article><article><span>Lifetime earned</span><strong>{loyalty.lifetimeEarnedPoints || 0}</strong></article><article><span>Lifetime redeemed</span><strong>{loyalty.lifetimeRedeemedPoints || 0}</strong></article></div>{loyalty.transactions?.length > 0 && <div className="customer-profile-history">{loyalty.transactions.slice(0, 5).map(item => <p key={item.id}><strong>{item.transactionType}</strong> {item.points > 0 ? "+" : ""}{item.points} points · {item.remarks}</p>)}</div>}</section>}

                <section className="customer-profile-card customer-profile-hub">
                    <div className="customer-profile-card-header"><div><span>MY DEVICES & SUPPORT</span><h2>Garage and repair tracking</h2></div><button type="button" className="customer-profile-edit-button" onClick={() => navigate("/customer/after-sales")}>Open service centre</button></div>
                    <div className="customer-profile-device-grid">{garage.slice(0, 6).map((device, index) => <article key={`${device.invoiceNumber}-${device.purchaseSerialId || index}`}><img src={device.productImageUrl || ""} alt="" onError={event => { event.currentTarget.style.display = "none"; }} /><div><strong>{device.productName}</strong><small>{device.invoiceNumber} · {device.maskedSerial || "Device"}</small><b className={device.warrantyActive ? "active" : "expired"}>{device.warrantyActive ? `Warranty active until ${new Date(device.warrantyEndDate).toLocaleDateString("en-IN")}` : "Warranty unavailable / expired"}</b></div></article>)}{!garage.length && <p className="customer-profile-empty">No eligible devices found yet. Your completed purchases will appear here.</p>}</div>
                    <div className="customer-profile-repair-list"><h3>Live repair / service status</h3>{serviceHistory.slice(0, 4).map(request => <button type="button" key={request.id} onClick={() => navigate("/customer/after-sales")}><span>{request.productName}</span><b>{String(request.status || "REQUESTED").replaceAll("_", " ")}</b><small>{request.requestNumber}</small></button>)}{!serviceHistory.length && <p className="customer-profile-empty">No repair or service requests yet.</p>}</div>
                </section>

                <section className="customer-profile-card customer-password-card"><div><span>ACCOUNT SECURITY</span><h2>Change password</h2><p>Use your current password and choose a new secure password.</p></div><form onSubmit={changePassword}><input required type="password" autoComplete="current-password" placeholder="Current password" value={passwordForm.currentPassword} onChange={event => setPasswordForm(current => ({ ...current, currentPassword: event.target.value }))} /><input required minLength="6" type="password" autoComplete="new-password" placeholder="New password (minimum 6 characters)" value={passwordForm.newPassword} onChange={event => setPasswordForm(current => ({ ...current, newPassword: event.target.value }))} /><button disabled={passwordBusy}>{passwordBusy ? "Saving…" : "Change password"}</button><button type="button" className="customer-profile-cancel" onClick={() => navigate("/forgot-password")}>Forgot password?</button></form></section>



                {/* =================================================
                    ALERTS
                ================================================= */}

                {
                    error && (

                        <div className="customer-profile-alert error">

                            {error}

                        </div>

                    )
                }


                {
                    success && (

                        <div className="customer-profile-alert success">

                            ✓ {success}

                        </div>

                    )
                }



                {/* =================================================
                    PROFILE CARD
                ================================================= */}

                <section className="customer-profile-card">


                    <div className="customer-profile-card-header">

                        <div>

                            <span>
                                PERSONAL INFORMATION
                            </span>

                            <h2>
                                Profile Details
                            </h2>

                        </div>


                        {
                            !editing && (

                                <button
                                    type="button"
                                    className="customer-profile-edit-button"
                                    onClick={() => {

                                        setEditing(true);

                                        setSuccess("");

                                        setError("");

                                    }}
                                >
                    <Pencil size={16} /> Edit Profile
                                </button>

                            )
                        }

                    </div>



                    {
                        editing ? (

                            <form
                                className="customer-profile-form"
                                onSubmit={
                                    handleSave
                                }
                            >


                                {/* NAME */}

                                <div className="customer-profile-field">

                                    <label htmlFor="name">
                                        Full Name
                                    </label>

                                    <input
                                        id="name"
                                        name="name"
                                        type="text"
                                        value={
                                            form.name
                                        }
                                        onChange={
                                            handleChange
                                        }
                                        placeholder="Enter your name"
                                        required
                                        minLength={2}
                                    />

                                </div>



                                {/* EMAIL */}

                                <div className="customer-profile-field">

                                    <label>
                                        Email Address
                                    </label>

                                    <input
                                        type="email"
                                        value={
                                            profile?.email ||
                                            ""
                                        }
                                        disabled
                                    />

                                    <small>
                                        Email cannot be changed
                                        from this profile page.
                                    </small>

                                </div>



                                {/* MOBILE */}

                                <div className="customer-profile-field">

                                    <label htmlFor="mobile">
                                        Mobile Number
                                    </label>

                                    <input
                                        id="mobile"
                                        name="mobile"
                                        type="tel"
                                        value={
                                            form.mobile
                                        }
                                        onChange={
                                            handleChange
                                        }
                                        placeholder="Enter 10 digit mobile number"
                                        maxLength={10}
                                        pattern="[6-9][0-9]{9}"
                                    />

                                    <small>
                                        Mobile OTP verification
                                        will be added before
                                        production.
                                    </small>

                                </div>


                                <div className="customer-profile-field">
                                    <label htmlFor="dateOfBirth">Date of Birth</label>
                                    <input id="dateOfBirth" name="dateOfBirth" type="date" value={form.dateOfBirth} onChange={handleChange} max={new Date().toISOString().slice(0, 10)} />
                                    <small>Used only for your profile and birthday greetings when marketing is enabled.</small>
                                </div>

                                {/* PROFILE PHOTO */}

                                <div className="customer-profile-field customer-profile-photo-field">

                                    <label htmlFor="profilePhotoFile">
                                        Profile Photo
                                    </label>

                                    <div className="customer-profile-photo-actions">
                                        <input
                                            id="profilePhotoFile"
                                            type="file"
                                            accept="image/png,image/jpeg,image/webp"
                                            onChange={handlePhotoUpload}
                                            disabled={photoBusy}
                                        />

                                        <button
                                            type="button"
                                            className="customer-profile-remove-photo"
                                            onClick={handlePhotoRemove}
                                            disabled={photoBusy || !profile?.profilePhotoUrl}
                                        >
                                            {photoBusy ? "Working..." : "Remove photo"}
                                        </button>
                                    </div>

                                    <small>
                                        Upload JPG, PNG or WEBP from your computer. Maximum size 2 MB.
                                    </small>

                                    <details className="customer-profile-url-fallback">
                                        <summary>Use image URL instead</summary>
                                        <input
                                            id="profilePhotoUrl"
                                            name="profilePhotoUrl"
                                            type="url"
                                            value={form.profilePhotoUrl}
                                            onChange={handleChange}
                                            placeholder="https://... or /uploads/..."
                                            maxLength={2000}
                                        />
                                    </details>

                                </div>



                                {/* ACTIONS */}

                                <div className="customer-profile-form-actions">

                                    <button
                                        type="button"
                                        className="customer-profile-cancel"
                                        onClick={
                                            handleCancel
                                        }
                                        disabled={
                                            saving
                                        }
                                    >
                                        Cancel
                                    </button>


                                    <button
                                        type="submit"
                                        className="customer-profile-save"
                                        disabled={
                                            saving
                                        }
                                    >

                                        {
                                            saving
                                                ? "Saving..."
                                                : "Save Changes"
                                        }

                                    </button>

                                </div>


                            </form>

                        ) : (

                            <div className="customer-profile-details">


                                {/* NAME */}

                                <div className="customer-profile-detail">

                                    <span>
                                        Full Name
                                    </span>

                                    <strong>
                                        {
                                            profile?.name ||
                                            "Not available"
                                        }
                                    </strong>

                                </div>



                                {/* EMAIL */}

                                <div className="customer-profile-detail">

                                    <span>
                                        Email Address
                                    </span>

                                    <strong>
                                        {
                                            profile?.email ||
                                            "Not available"
                                        }
                                    </strong>

                                </div>



                                {/* MOBILE */}

                                <div className="customer-profile-detail">

                                    <span>
                                        Mobile Number
                                    </span>

                                    <strong>
                                        {
                                            profile?.mobile ||
                                            "Not added"
                                        }

                                        {
                                            profile?.mobile && (

                                                <small className="mobile-not-verified">
                                                    Not verified
                                                </small>

                                            )
                                        }

                                    </strong>

                                </div>



                                {/* ROLE */}

                                <div className="customer-profile-detail">

                                    <span>
                                        Account Type
                                    </span>

                                    <strong>
                                        Customer
                                    </strong>

                                </div>


                                {/* PROFILE PHOTO */}

                                <div className="customer-profile-detail">

                                    <span>
                                        Profile Photo
                                    </span>

                                    <strong>
                                        {
                                            profile?.profilePhotoUrl
                                                ? "Added"
                                                : "Default avatar"
                                        }
                                    </strong>

                                </div>



                                {/* STATUS */}

                                <div className="customer-profile-detail">

                                    <span>
                                        Account Status
                                    </span>

                                    <strong className="profile-status">

                                        {
                                            profile?.status ||
                                            "APPROVED"
                                        }

                                    </strong>

                                </div>



                                {/* MEMBER SINCE */}

                                <div className="customer-profile-detail">

                                    <span>
                                        Member Since
                                    </span>

                                    <strong>

                                        {
                                            profile?.createdAt
                                                ? new Date(
                                                    profile.createdAt
                                                ).toLocaleDateString(
                                                    "en-IN",
                                                    {
                                                        day: "2-digit",
                                                        month: "short",
                                                        year: "numeric"
                                                    }
                                                )
                                                : "-"
                                        }

                                    </strong>

                                </div>


                            </div>

                        )
                    }


                </section>



                {/* =================================================
                    ACCOUNT ACTIONS
                ================================================= */}

                <div className="customer-profile-actions-grid">


                    <button
                        type="button"
                        onClick={() =>
                            navigate(
                                "/customer/cart"
                            )
                        }
                    >

                        <span>
                            🛒
                        </span>

                        <div>

                            <strong>
                                My Cart
                            </strong>

                            <small>
                                View your selected products
                            </small>

                        </div>

                    </button>



                    <button
                        type="button"
                        onClick={() =>
                            navigate(
                                "/customer/wishlist"
                            )
                        }
                    >

                        <span>
                            ♡
                        </span>

                        <div>

                            <strong>
                                Wishlist
                            </strong>

                            <small>
                                Products you saved
                            </small>

                        </div>

                    </button>

                    <button type="button" onClick={() => navigate("/customer/addresses")}>
                        <span>⌂</span>
                        <div><strong>My Addresses</strong><small>Manage your delivery addresses</small></div>
                    </button>

                    <button type="button" onClick={() => navigate("/customer/after-sales")}>
                        <span>🛠</span>
                        <div><strong>Service & repair history</strong><small>Track repairing, parts replacement and pickup updates</small></div>
                    </button>

                    <button type="button" onClick={() => navigate("/customer/referral")}>
                        <span>✦</span>
                        <div><strong>Refer & earn</strong><small>Invite friends, earn rewards and view coupon history</small></div>
                    </button>


                </div>


            </section>

        </main>
    );
}

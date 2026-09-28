import { lazy, Suspense, useEffect, useState } from "react";
import { Routes, Route, Navigate, useLocation } from "react-router-dom";
import { applyWorkspaceAppearance } from "../utils/workspacePreferences";
import AnimatedPage from "../components/common/animations/AnimatedPage";
import CustomerActionFeedback from "../components/common/CustomerActionFeedback";


/*
 * =========================================================
 * COMMON
 * =========================================================
 */

const Login = lazy(() => import("../pages/common/Login"));

const Register = lazy(() => import("../pages/common/Register"));

const ForgotPassword = lazy(() => import("../pages/common/ForgotPassword"));
const ResetPassword = lazy(() => import("../pages/common/ResetPassword"));

const ProductDetails = lazy(() => import("../pages/common/ProductDetails"));
const PolicyPage = lazy(() => import("../pages/common/PolicyPage"));
const SellerPolicySupport = lazy(() => import("../pages/seller/SellerPolicySupport"));
import SiteFooter from "../components/SiteFooter";
import AdminFooter from "../components/AdminFooter";
import AdminModuleNavigation from "../components/AdminModuleNavigation";
const PublicHomePage = lazy(() => import("../pages/public/PublicHomePage"));


/*
 * =========================================================
 * AUTH / ROUTE GUARDS
 * =========================================================
 */

import ProtectedRoute
    from "../components/ProtectedRoute";

import RoleRoute
    from "../components/RoleRoute";


/*
 * =========================================================
 * CUSTOMER
 * =========================================================
 */

const CustomerDashboard = lazy(() => import("../pages/customer/CustomerDashboard"));

const CustomerProducts = lazy(() => import("../pages/customer/CustomerProducts"));

const Cart = lazy(() => import("../pages/customer/Cart"));

const Wishlist = lazy(() => import("../pages/customer/Wishlist"));

const CustomerProfile = lazy(() => import("../pages/customer/CustomerProfile"));
const CustomerAddresses = lazy(() => import("../pages/customer/CustomerAddresses"));

const Checkout = lazy(() => import("../pages/customer/Checkout"));

const CustomerOrders = lazy(() => import("../pages/customer/CustomerOrders"));

const CustomerOrderDetails = lazy(() => import("../pages/customer/CustomerOrderDetails"));

const Referral = lazy(() => import("../pages/customer/Referral"));
const AfterSales = lazy(() => import("../pages/customer/AfterSales"));
const CustomerRewards = lazy(() => import("../pages/customer/CustomerRewards"));
const SecondHandMarket = lazy(() => import("../pages/customer/SecondHandMarket"));


/*
 * =========================================================
 * SELLER
 * =========================================================
 */

const SellerDashboard = lazy(() => import("../pages/seller/SellerDashboard"));

const AddProduct = lazy(() => import("../pages/seller/AddProduct"));
const AddOtherProduct = lazy(() => import("../pages/seller/AddOtherProduct"));

const GeneralPreownedWorkspace = lazy(() => import("../pages/seller/GeneralPreownedWorkspace"));

const OfflineBilling = lazy(() => import("../pages/seller/OfflineBilling"));
const SellerOfflineReturns = lazy(() => import("../pages/seller/SellerOfflineReturns"));

const Purchases = lazy(() => import("../pages/seller/Purchases"));

const StaffManagement = lazy(() => import("../pages/seller/StaffManagement"));
const SellerOrders = lazy(() => import("../pages/seller/SellerOrders"));
const SellerProducts = lazy(() => import("../pages/seller/SellerProducts"));
const SellerInventory = lazy(() => import("../pages/seller/SellerInventory"));
const Expenses = lazy(() => import("../pages/seller/Expenses"));
const Branches = lazy(() => import("../pages/seller/Branches"));
const PaymentModule = lazy(() => import("../pages/seller/PaymentModule"));
const DistributorPaymentsModule = lazy(() => import("../pages/seller/DistributorPaymentsModule"));
const DistributorCreditNotesModule = lazy(() => import("../pages/seller/DistributorCreditNotesModule"));
const CustomerLedgerModule = lazy(() => import("../pages/seller/CustomerLedgerModule"));
const CAReports = lazy(() => import("../pages/seller/CAReports"));
const Accounting = lazy(() => import("../pages/seller/Accounting"));
const StockTransfers = lazy(() => import("../pages/seller/StockTransfers"));
const ShopRegister = lazy(() => import("../pages/seller/ShopRegister"));
const InstantMobileBill = lazy(() => import("../pages/seller/InstantMobileBill"));

const SellerModulePlaceholder = lazy(() => import("../pages/seller/SellerModulePlaceholder"));



const SellerSettingsPage = lazy(() => import("../pages/seller/SellerSettingsPage"));
const SellerSubscriptionPage = lazy(() => import("../pages/seller/SellerSubscriptionPage"));
const FinanceManagement = lazy(() => import("../pages/seller/FinanceManagement"));

const DistributorManagement = lazy(() => import("../pages/admin/DistributorManagement"));

const DistributorRequest = lazy(() => import("../pages/seller/DistributorRequest"));

const SellerCustomers = lazy(() => import("../pages/seller/SellerCustomers"));

const SellerReports = lazy(() => import("../pages/seller/SellerReports"));
const SellerReportDetail = lazy(() => import("../pages/seller/SellerReportDetail"));
const SellerGSTReports = lazy(() => import("../pages/seller/SellerGSTReports"));
const SellerAfterSales = lazy(() => import("../pages/seller/AfterSales"));


/*
 * =========================================================
 * ADMIN
 * =========================================================
 */

const AdminDashboard = lazy(() => import("../pages/admin/AdminDashboard"));

const AdminCustomers = lazy(() => import("../pages/admin/AdminCustomers"));

const AdminCustomerDetails = lazy(() => import("../pages/admin/AdminCustomerDetails"));

const AdminAddProduct = lazy(() => import("../pages/admin/AdminAddProduct"));

const AdminSellerManagement = lazy(() => import("../pages/admin/AdminSellerManagement"));

const SellerRequests = lazy(() => import("../pages/admin/SellerRequests"));

const SellerDetails = lazy(() => import("../pages/admin/SellerDetails"));

const AdminSellerDetails = lazy(() => import("../pages/admin/AdminSellerDetails"));

const CampaignManagement = lazy(() => import("../pages/admin/CampaignManagement"));
const CampaignCreate = lazy(() => import("../pages/admin/CampaignCreate"));
const CampaignReport = lazy(() => import("../pages/admin/CampaignReport"));
const BirthdaySettings = lazy(() => import("../pages/admin/BirthdaySettings"));
const AfterSalesManagement = lazy(() => import("../pages/admin/AfterSalesManagement"));
const MarketplaceManagement = lazy(() => import("../pages/admin/MarketplaceManagement"));
const AdminApprovals = lazy(() => import("../pages/admin/AdminApprovals"));

const Products = lazy(() => import("../pages/admin/Products"));

const AdminProductDetails = lazy(() => import("../pages/admin/ProductDetails"));

const MobileSpecifications = lazy(() => import("../pages/admin/MobileSpecifications"));


/*
 * =========================================================
 * ADMIN MARKETING
 * =========================================================
 */

const MarketingDashboard = lazy(() => import("../pages/admin/marketing/MarketingDashboard"));

const MarketingList = lazy(() => import("../pages/admin/marketing/MarketingList"));

const MarketingDetails = lazy(() => import("../pages/admin/marketing/MarketingDetails"));

const MarketingReports = lazy(() => import("../pages/admin/marketing/MarketingReports"));


/*
 * =========================================================
 * ADMIN OTHER
 * =========================================================
 */

const Reports = lazy(() => import("../pages/admin/Reports"));

const Orders = lazy(() => import("../pages/admin/Orders"));

const Payments = lazy(() => import("../pages/admin/Payments"));

const Invoices = lazy(() => import("../pages/admin/Invoices"));
const Notifications = lazy(() => import("../pages/admin/Notifications"));

const Categories = lazy(() => import("../pages/admin/Categories"));
const SubcategoryFields = lazy(() => import("../pages/admin/SubcategoryFields"));
const DeliveryRules = lazy(() => import("../pages/admin/DeliveryRules"));
const ProductVariants = lazy(() => import("../pages/seller/ProductVariants"));

const Settings = lazy(() => import("../pages/admin/Settings"));
const AdminSubscriptions = lazy(() => import("../pages/admin/AdminSubscriptions"));

const Coupons = lazy(() => import("../pages/admin/Coupons"));

const AdminReferrals = lazy(() => import("../pages/admin/AdminReferrals"));



/*
 * =========================================================
 * APP ROUTES
 * =========================================================
 */

const ApplicationFooter = () => {
    const { pathname } = useLocation();
    if (["/login", "/register", "/forgot-password", "/reset-password"].includes(pathname)) return <div className="auth-footer-wrap"><SiteFooter /></div>;
    return pathname.startsWith("/admin/") ? <AdminFooter /> : <SiteFooter />;
};

const WorkspaceAppearanceSync = () => {
    const { pathname } = useLocation();
    const [revision, setRevision] = useState(0);

    useEffect(() => {
        const refresh = () => setRevision(value => value + 1);
        window.addEventListener("shivhub:workspace-preferences", refresh);
        return () => window.removeEventListener("shivhub:workspace-preferences", refresh);
    }, []);

    useEffect(() => {
        // Product details are a shared route, but customers open it from the
        // marketplace. Preserve their selected appearance on that screen too.
        const scope = pathname.startsWith("/customer/") || pathname.startsWith("/product/")
            ? "customer"
            : pathname.startsWith("/seller/")
                ? "seller"
                : pathname.startsWith("/admin/")
                    ? "admin"
                    : null;

        ["customer", "seller", "admin"].forEach(currentScope => {
            if (currentScope !== scope) delete document.body.dataset[`${currentScope}Theme`];
        });

        if (scope) applyWorkspaceAppearance(scope);

        // Customer/public screens share one visual system.  Keep this class
        // off the operational seller and admin workspaces so their themes
        // remain independent.
        document.body.classList.toggle(
            "customer-theme-v2",
            !pathname.startsWith("/seller/") && !pathname.startsWith("/admin/")
        );
    }, [pathname, revision]);

    return null;
};

const AnimatedRouteContent = ({ children }) => {
    const { pathname } = useLocation();
    // POS scanning must remain instant and keep the scanner input focused.
    const isPerformanceCritical = pathname === "/seller/offline-billing";
    return <AnimatedPage pageKey={pathname} disabled={isPerformanceCritical}>{children}</AnimatedPage>;
};

const AppRoutes = () => {

    return (

        <>
        <WorkspaceAppearanceSync />
        <AnimatedRouteContent>
        <AdminModuleNavigation />
        <Suspense fallback={<div role="status" style={{ padding: 24 }}>Loading page…</div>}><Routes>


            {/* =================================================
                PUBLIC ROUTES
            ================================================= */}

            <Route
                path="/"
                element={
                    <PublicHomePage />
                }
            />

            <Route
                path="/login"
                element={
                    <Login />
                }
            />

            <Route path="/forgot-password" element={<ForgotPassword />} />

            <Route path="/reset-password" element={<ResetPassword />} />


            <Route
                path="/register"
                element={
                    <Register />
                }
            />


            <Route
                path="/product/:id"
                element={
                    <ProductDetails />
                }
            />

            {["about", "contact", "terms", "privacy-policy", "cancellation-refund", "shipping-policy", "return-replacement", "warranty-policy", "help", "grievance", "seller-terms", "payment-security", "cookie-policy", "sitemap", "careers", "why-choose-us", "seller-support", "partner-program", "api-integration"].map(slug => (
                <Route key={slug} path={`/${slug}`} element={<PolicyPage slug={slug} />} />
            ))}


            {/* =================================================
                CUSTOMER ROUTES
            ================================================= */}

            <Route
                element={
                    <ProtectedRoute />
                }
            >

                <Route
                    element={
                        <RoleRoute
                            allowedRoles={[
                                "CUSTOMER"
                            ]}
                        />
                    }
                >


                    {/* =========================================
                        CUSTOMER DASHBOARD
                    ========================================= */}

                    <Route
                        path="/customer/dashboard"
                        element={
                            <CustomerDashboard />
                        }
                    />


                    {/* =========================================
                        CUSTOMER PRODUCTS
                    ========================================= */}

                    <Route
                        path="/customer/products"
                        element={
                            <CustomerProducts />
                        }
                    />


                    {/* =========================================
                        CUSTOMER CART
                    ========================================= */}

                    <Route
                        path="/customer/cart"
                        element={
                            <Cart />
                        }
                    />


                    {/* =========================================
                        CUSTOMER WISHLIST
                    ========================================= */}

                    <Route
                        path="/customer/wishlist"
                        element={
                            <Wishlist />
                        }
                    />


                    {/* =========================================
                        CUSTOMER PROFILE
                    ========================================= */}

                    <Route
                        path="/customer/profile"
                        element={
                            <CustomerProfile />
                        }
                    />


                    {/* =========================================
                        CUSTOMER CHECKOUT
                    ========================================= */}

                    <Route
                        path="/customer/checkout"
                        element={
                            <Checkout />
                        }
                    />


                    {/* =========================================
                        CUSTOMER ORDERS
                    ========================================= */}

                    <Route
                        path="/customer/orders"
                        element={
                            <CustomerOrders />
                        }
                    />


                    {/* =========================================
                        CUSTOMER ORDER DETAILS
                    ========================================= */}

                    <Route
                        path="/customer/orders/:id"
                        element={
                            <CustomerOrderDetails />
                        }
                    />


                    {/* =========================================
                        CUSTOMER REFERRAL
                    ========================================= */}

                    <Route
                        path="/customer/referral"
                        element={
                            <Referral />
                        }
                    />

                    <Route path="/customer/after-sales" element={<AfterSales />} />
                    <Route path="/customer/returns" element={<AfterSales />} />
                    <Route path="/customer/second-hand-market" element={<SecondHandMarket />} />

                    <Route path="/customer/rewards" element={<CustomerRewards />} />

                    <Route path="/customer/addresses" element={<CustomerAddresses />} />

                </Route>

            </Route>


            {/* =================================================
                SELLER ROUTES
            ================================================= */}

            <Route
                element={
                    <ProtectedRoute />
                }
            >

                <Route
                    element={
                        <RoleRoute
                            allowedRoles={[
                                "SELLER"
                            ]}
                        />
                    }
                >


                    {/* =========================================
                        SELLER DASHBOARD
                    ========================================= */}

                    <Route
                        path="/seller/dashboard"
                        element={
                            <SellerDashboard />
                        }
                    />


                    {/* =========================================
                        SELLER ADD PRODUCT
                    ========================================= */}

                    <Route
                        path="/seller/add-product"
                        element={
                            <AddProduct />
                        }
                    />

                    <Route path="/seller/add-other-product" element={<AddOtherProduct />} />

                    <Route
                        path="/seller/add-accessories"
                        element={
                            <AddOtherProduct />
                        }
                    />

                    <Route
                        path="/seller/general-preowned"
                        element={
                            <GeneralPreownedWorkspace />
                        }
                    />


                    {/* =========================================
                        SELLER OFFLINE BILLING
                    ========================================= */}

                    <Route
                        path="/seller/offline-billing"
                        element={
                            <OfflineBilling />
                        }
                    />

                    <Route
                        path="/seller/offline-returns"
                        element={<SellerOfflineReturns />}
                    />

                    {/* Distributor invoices update stock through the Purchase API. */}
                    <Route
                        path="/seller/purchases"
                        element={
                            <Purchases />
                        }
                    />

                    <Route
                        path="/seller/staff"
                        element={
                            <StaffManagement />
                        }
                    />

                    {/* Existing working modules use their actual pages. */}
                    <Route path="/seller/billing" element={<Navigate to="/seller/offline-billing" replace />} />
                    <Route path="/seller/distributors" element={<DistributorRequest />} />
                    <Route path="/seller/products" element={<SellerProducts />} />
                    <Route path="/seller/products/:id/variants" element={<ProductVariants />} />
                    <Route path="/seller/inventory" element={<SellerInventory />} />
                    <Route path="/seller/orders" element={<SellerOrders />} />
                    <Route path="/seller/branches" element={<Branches />} />
                    <Route path="/seller/expenses" element={<Expenses />} />
                    <Route path="/seller/payments" element={<PaymentModule />} />
                    <Route path="/seller/distributor-payments" element={<DistributorPaymentsModule />} />
                    <Route path="/seller/distributor-credit-notes" element={<DistributorCreditNotesModule />} />
                    <Route path="/seller/customer-ledger" element={<CustomerLedgerModule />} />
                    <Route path="/seller/ca-reports" element={<CAReports />} />
                    <Route path="/seller/accounting" element={<Accounting />} />
                    <Route path="/seller/stock-transfers" element={<StockTransfers />} />
                    <Route path="/seller/shop-register" element={<ShopRegister />} />
                    <Route path="/seller/instant-bills" element={<InstantMobileBill />} />
                    <Route path="/seller/settings" element={<SellerSettingsPage />} />
                    <Route path="/seller/subscription" element={<SellerSubscriptionPage />} />
                    <Route path="/seller/finance" element={<FinanceManagement />} />
                    <Route path="/seller/customers" element={<SellerCustomers />} />
                    <Route path="/seller/reports" element={<SellerReports />} />
                    <Route path="/seller/gst" element={<SellerGSTReports />} />
                    <Route path="/seller/after-sales" element={<SellerAfterSales />} />
                    <Route path="/seller/return-requests" element={<SellerAfterSales />} />
                    <Route path="/seller/reports/:report" element={<SellerReportDetail />} />
                    <Route path="/seller/policies" element={<SellerPolicySupport />} />
                    <Route path="/seller/offers" element={<SellerCustomers />} />
                    <Route path="/seller/messages" element={<SellerCustomers />} />

                    {/* Keep a signed-in seller inside seller area; never use public-login fallback. */}
                    <Route path="/seller/*" element={<SellerModulePlaceholder />} />

                </Route>

            </Route>


            {/* =================================================
                ADMIN ROUTES
            ================================================= */}

            <Route
                element={
                    <ProtectedRoute />
                }
            >

                <Route
                    element={
                        <RoleRoute
                            allowedRoles={[
                                "ADMIN"
                            ]}
                        />
                    }
                >


                    {/* =========================================
                        ADMIN DASHBOARD
                    ========================================= */}

                    <Route
                        path="/admin/dashboard"
                        element={
                            <AdminDashboard />
                        }
                    />


                    {/* =========================================
                        ADMIN OFFERS
                    ========================================= */}

                    <Route
                        path="/admin/offers"
                        element={
                            <Navigate
                                to="/admin/marketing/offers"
                                replace
                            />
                        }
                    />


                    {/* =========================================
                        ADMIN CUSTOMERS
                    ========================================= */}

                    <Route
                        path="/admin/customers"
                        element={
                            <AdminCustomers />
                        }
                    />


                    {/* =========================================
                        CUSTOMER DETAILS
                    ========================================= */}

                    <Route
                        path="/admin/customers/:id"
                        element={
                            <AdminCustomerDetails />
                        }
                    />


                    {/* =========================================
                        ADMIN SELLERS
                    ========================================= */}

                    <Route
                        path="/admin/sellers"
                        element={
                            <AdminSellerManagement />
                        }
                    />


                    {/* =========================================
                        ADMIN ADD PRODUCT
                    ========================================= */}

                    <Route
                        path="/admin/products/add"
                        element={
                            <AdminAddProduct />
                        }
                    />


                    {/* =========================================
                        SELLER REQUESTS
                    ========================================= */}

                    <Route
                        path="/admin/seller-requests"
                        element={
                            <SellerRequests />
                        }
                    />

                    <Route path="/admin/approvals" element={<AdminApprovals />} />


                    {/* =========================================
                        SELLER DETAILS
                    ========================================= */}

                    <Route
                        path="/admin/sellers/:id"
                        element={
                            <AdminSellerDetails />
                        }
                    />


                    {/* =========================================
                        SELLER REQUEST DETAILS
                    ========================================= */}

                    <Route
                        path="/admin/seller-requests/:id"
                        element={
                            <SellerDetails />
                        }
                    />


                    {/* =========================================
                        ADMIN PRODUCTS
                    ========================================= */}

                    <Route
                        path="/admin/products"
                        element={
                            <Products />
                        }
                    />


                    {/* =========================================
                        ADMIN PRODUCT DETAILS
                    ========================================= */}

                    <Route
                        path="/admin/products/:id"
                        element={
                            <AdminProductDetails />
                        }
                    />

                    <Route
                        path="/admin/mobile-specs"
                        element={
                            <MobileSpecifications />
                        }
                    />


                    {/* =========================================
                        ADMIN MARKETING DASHBOARD
                    ========================================= */}

                    <Route
                        path="/admin/marketing"
                        element={
                            <MarketingDashboard />
                        }
                    />

                    <Route path="/admin/campaigns" element={<CampaignManagement />} />
                    <Route path="/admin/campaigns/new" element={<CampaignCreate />} />
                    <Route path="/admin/campaigns/:id/report" element={<CampaignReport />} />
                    <Route path="/admin/birthday-settings" element={<BirthdaySettings />} />
                    <Route path="/admin/after-sales" element={<AfterSalesManagement />} />
                    <Route path="/admin/returns" element={<AfterSalesManagement />} />
                    <Route path="/admin/marketplace" element={<MarketplaceManagement />} />


                    {/* =========================================
                        ADMIN MARKETING REPORTS
                    ========================================= */}

                    <Route
                        path="/admin/marketing/reports"
                        element={
                            <MarketingReports />
                        }
                    />


                    {/* =========================================
                        ADMIN MARKETING LIST
                    ========================================= */}

                    <Route
                        path="/admin/marketing/:type"
                        element={
                            <MarketingList />
                        }
                    />


                    {/* =========================================
                        ADMIN MARKETING DETAILS
                    ========================================= */}

                    <Route
                        path="/admin/marketing/:type/:id"
                        element={
                            <MarketingDetails />
                        }
                    />


                    {/* =========================================
                        ADMIN REPORTS
                    ========================================= */}

                    <Route
                        path="/admin/reports"
                        element={
                            <Reports />
                        }
                    />


                    {/* =========================================
                        ADMIN ORDERS
                    ========================================= */}

                    <Route
                        path="/admin/orders"
                        element={
                            <Orders />
                        }
                    />


                    {/* =========================================
                        ADMIN PAYMENTS
                    ========================================= */}

                    <Route
                        path="/admin/payments"
                        element={
                            <Payments />
                        }
                    />


                    {/* =========================================
                        ADMIN INVOICES
                    ========================================= */}

                    <Route
                        path="/admin/invoices"
                        element={
                            <Invoices />
                        }
                    />


                    {/* =========================================
                        ADMIN CATEGORIES
                    ========================================= */}

                    <Route
                        path="/admin/categories"
                        element={
                            <Categories />
                        }
                    />

                    <Route path="/admin/coupons" element={<Coupons />} />
                    <Route path="/admin/subcategory-fields" element={<SubcategoryFields />} />
                    <Route path="/admin/delivery-rules" element={<DeliveryRules />} />
                    <Route path="/admin/subscriptions" element={<AdminSubscriptions />} />
                    <Route path="/admin/finance" element={<FinanceManagement admin />} />

                    <Route path="/admin/referrals" element={<AdminReferrals />} />

                    {/* WhatsApp provider readiness, campaign mappings and delivery audit. */}
                    <Route path="/admin/notifications" element={<Notifications />} />


                    {/* =========================================
                        ADMIN SETTINGS
                    ========================================= */}

                    <Route
                        path="/admin/settings"
                        element={
                            <Settings />
                        }
                    />

                    <Route path="/admin/distributors" element={<DistributorManagement />} />

                </Route>

            </Route>


            {/* =================================================
                DEFAULT ROUTE
            ================================================= */}

            {/* =================================================
                UNKNOWN ROUTE
            ================================================= */}

            <Route
                path="*"
                element={
                    <Navigate
                        to="/login"
                        replace
                    />
                }
            />

        </Routes></Suspense>
        </AnimatedRouteContent>
        <CustomerActionFeedback />
        <ApplicationFooter />
        </>
    );
};


export default AppRoutes;

import {
    BrowserRouter,
    Routes,
    Route,
    Navigate
} from "react-router-dom";


// =========================================================
// SELLER PAGES
// =========================================================

import SellerDashboard from "./pages/seller/SellerDashboard";
import AddProduct from "./pages/seller/AddProduct";


// =========================================================
// ADMIN PAGES
// =========================================================

import AdminDashboard from "./pages/admin/AdminDashboard";

import Categories from "./pages/admin/Categories";
import Coupons from "./pages/admin/Coupons";
import AdminCustomerDetails from "./pages/admin/AdminCustomerDetails";
import AdminCustomers from "./pages/admin/AdminCustomers";
import Invoices from "./pages/admin/Invoices";
import Notifications from "./pages/admin/Notifications";
import Offers from "./pages/admin/Offers";

import OrderDetails from "./pages/admin/OrderDetails";
import Orders from "./pages/admin/Orders";

import Payments from "./pages/admin/Payments";

import ProductDetails from "./pages/admin/ProductDetails";
import Products from "./pages/admin/Products";
import AdminAddProduct from "./pages/admin/AdminAddProduct";
import DistributorManagement from "./pages/admin/DistributorManagement";

import ReferralManagement from "./pages/admin/ReferralManagement";

import Reports from "./pages/admin/Reports";

import AdminSellerDetails from "./pages/admin/AdminSellerDetails";
import SellerRequests from "./pages/admin/SellerRequests";
import AdminSellerManagement from "./pages/admin/AdminSellerManagement";
import CampaignManagement from "./pages/admin/CampaignManagement";
import CampaignCreate from "./pages/admin/CampaignCreate";
import CampaignReport from "./pages/admin/CampaignReport";
import LoyaltyManagement from "./pages/admin/LoyaltyManagement";
import BirthdaySettings from "./pages/admin/BirthdaySettings";

import Settings from "./pages/admin/Settings";


import "./App.css";


/*
 * =========================================================
 * SHIVHUB
 * APPLICATION ROUTES
 * =========================================================
 *
 *
 * SELLER MODULE
 *
 * /seller/dashboard
 * /seller/add-product
 *
 *
 * ADMIN MODULE
 *
 * /admin/dashboard
 * /admin/categories
 * /admin/coupons
 * /admin/customers
 * /admin/customers/:id
 * /admin/invoices
 * /admin/notifications
 * /admin/offers
 * /admin/orders
 * /admin/orders/:id
 * /admin/payments
 * /admin/products
 * /admin/products/:id
 * /admin/referrals
 * /admin/reports
 * /admin/sellers
 * /admin/sellers/:id
 * /admin/seller-requests
 * /admin/settings
 *
 * =========================================================
 */


function App() {

    return (

        <BrowserRouter>

            <Routes>


                {/* =================================================
                    DEFAULT ROUTE
                ================================================= */}

                <Route
                    path="/"
                    element={
                        <Navigate
                            to="/seller/dashboard"
                            replace
                        />
                    }
                />


                {/* =================================================
                    ================= SELLER ======================
                ================================================= */}


                {/* SELLER DASHBOARD */}

                <Route
                    path="/seller/dashboard"
                    element={
                        <SellerDashboard />
                    }
                />


                {/* ADD PRODUCT */}

                <Route
                    path="/seller/add-product"
                    element={
                        <AddProduct />
                    }
                />



                {/* =================================================
                    ================= ADMIN ========================
                ================================================= */}


                {/* =================================================
                    ADMIN DASHBOARD
                ================================================= */}

                <Route
                    path="/admin/dashboard"
                    element={
                        <AdminDashboard />
                    }
                />


                {/* =================================================
                    CATEGORIES
                ================================================= */}

                <Route
                    path="/admin/categories"
                    element={
                        <Categories />
                    }
                />


                {/* =================================================
                    CUSTOMERS
                ================================================= */}

                <Route
                    path="/admin/customers"
                    element={<AdminCustomers />}
                />


                {/* CUSTOMER DETAILS */}

                <Route
                    path="/admin/customers/:id"
                    element={<AdminCustomerDetails />}
                />


                {/* =================================================
                    SELLERS
                ================================================= */}

                <Route
                    path="/admin/sellers"
                    element={<AdminSellerManagement />}
                />


                {/* SELLER DETAILS */}

                <Route
                    path="/admin/sellers/:id"
                    element={<AdminSellerDetails />}
                />


                {/* SELLER REQUESTS */}

                <Route
                    path="/admin/seller-requests"
                    element={
                        <SellerRequests />
                    }
                />


                {/* =================================================
                    PRODUCTS
                ================================================= */}

                <Route
                    path="/admin/products"
                    element={
                        <Products />
                    }
                />

                <Route
                    path="/admin/products/add"
                    element={<AdminAddProduct />}
                />


                {/* PRODUCT DETAILS */}

                <Route
                    path="/admin/products/:id"
                    element={
                        <ProductDetails />
                    }
                />


                {/* =================================================
                    ORDERS
                ================================================= */}

                <Route
                    path="/admin/orders"
                    element={
                        <Orders />
                    }
                />


                {/* ORDER DETAILS */}

                <Route
                    path="/admin/orders/:id"
                    element={
                        <OrderDetails />
                    }
                />


                {/* =================================================
                    INVOICES
                ================================================= */}

                <Route
                    path="/admin/invoices"
                    element={
                        <Invoices />
                    }
                />


                {/* =================================================
                    PAYMENTS
                ================================================= */}

                <Route
                    path="/admin/payments"
                    element={
                        <Payments />
                    }
                />


                {/* =================================================
                    ================= MARKETING ===================
                ================================================= */}


                {/* OFFERS */}

                <Route
    path="/admin/offers"
    element={<Offers />}
/>

                <Route path="/admin/campaigns" element={<CampaignManagement />} />
                <Route path="/admin/campaigns/new" element={<CampaignCreate />} />
                <Route path="/admin/campaigns/:id/report" element={<CampaignReport />} />
                <Route path="/admin/loyalty" element={<LoyaltyManagement />} />
                <Route path="/admin/birthday-settings" element={<BirthdaySettings />} />

                {/* COUPONS */}

                <Route
                    path="/admin/coupons"
                    element={
                        <Coupons />
                    }
                />


                {/* NOTIFICATIONS */}

                <Route
                    path="/admin/notifications"
                    element={
                        <Notifications />
                    }
                />


                {/* REFERRAL MANAGEMENT */}

                <Route
                    path="/admin/referrals"
                    element={
                        <ReferralManagement />
                    }
                />


                {/* =================================================
                    ================= BUSINESS ====================
                ================================================= */}


                {/* REPORTS */}

                <Route
                    path="/admin/reports"
                    element={
                        <Reports />
                    }
                />

                <Route
                    path="/admin/distributors"
                    element={<DistributorManagement />}
                />


                {/* SETTINGS */}

                <Route
                    path="/admin/settings"
                    element={
                        <Settings />
                    }
                />



                {/* =================================================
                    UNKNOWN ROUTE
                ================================================= */}

                <Route
                    path="*"
                    element={
                        <Navigate
                            to="/admin/dashboard"
                            replace
                        />
                    }
                />

            </Routes>

        </BrowserRouter>

    );

}


export default App;

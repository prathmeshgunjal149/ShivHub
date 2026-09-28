import SellerSidebar from "./SellerSidebar";
import PolicyPage from "../common/PolicyPage";

export default function SellerPolicySupport() {
    return (
        <div className="seller-data-page">
            <SellerSidebar />
            <main className="seller-data-main">
                <PolicyPage slug="seller-terms" />
            </main>
        </div>
    );
}

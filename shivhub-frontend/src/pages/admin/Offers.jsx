import { useEffect, useMemo, useState } from "react";
import "./Offers.css";

/*
=========================================================
 SHIVHUB ADMIN - OFFERS MANAGEMENT
=========================================================

 Admin can:

 1. Create Customer offers
 2. Create Seller offers
 3. Create All-user offers
 4. Add discount percentage
 5. Add coupon code
 6. Set start/end date
 7. Enable email notification
 8. Edit offers
 9. Delete offers
10. View offer history
11. Search offers
12. Filter by status
13. Store offers in localStorage

=========================================================
*/

const STORAGE_KEY = "shivhub_admin_offers";

const INITIAL_OFFERS = [
  {
    id: 1,
    title: "Diwali Mega Offer",
    description:
      "Celebrate Diwali with ShivHub and get amazing discounts on selected products.",
    audience: "CUSTOMER",
    discountType: "PERCENTAGE",
    discountValue: 20,
    couponCode: "DIWALI20",
    minOrderValue: 1000,
    maxDiscount: 2000,
    startDate: "2026-10-01",
    endDate: "2026-10-31",
    emailNotification: true,
    status: "ACTIVE",
    createdAt: "2026-08-01",
  },
  {
    id: 2,
    title: "Seller Boost Offer",
    description:
      "Special seller incentive for ShivHub sellers with increased sales benefits.",
    audience: "SELLER",
    discountType: "PERCENTAGE",
    discountValue: 10,
    couponCode: "SELLER10",
    minOrderValue: 0,
    maxDiscount: 0,
    startDate: "2026-08-20",
    endDate: "2026-09-30",
    emailNotification: true,
    status: "ACTIVE",
    createdAt: "2026-08-20",
  },
  {
    id: 3,
    title: "Welcome to ShivHub",
    description:
      "New customers get a special welcome discount on their first order.",
    audience: "CUSTOMER",
    discountType: "PERCENTAGE",
    discountValue: 10,
    couponCode: "WELCOME10",
    minOrderValue: 500,
    maxDiscount: 500,
    startDate: "2026-08-01",
    endDate: "2026-12-31",
    emailNotification: true,
    status: "ACTIVE",
    createdAt: "2026-08-01",
  },
];

/*
=========================================================
 HELPERS
=========================================================
*/

const getToday = () => {
  return new Date().toISOString().split("T")[0];
};

const calculateStatus = (offer) => {
  const today = getToday();

  if (!offer.startDate || !offer.endDate) {
    return "ACTIVE";
  }

  if (today < offer.startDate) {
    return "SCHEDULED";
  }

  if (today > offer.endDate) {
    return "EXPIRED";
  }

  return "ACTIVE";
};

const formatDate = (date) => {
  if (!date) return "-";

  const value = new Date(`${date}T00:00:00`);

  return value.toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
};

const getAudienceLabel = (audience) => {
  switch (audience) {
    case "CUSTOMER":
      return "Customers";

    case "SELLER":
      return "Sellers";

    case "ALL":
      return "Customers + Sellers";

    default:
      return audience;
  }
};

/*
=========================================================
 EMPTY FORM
=========================================================
*/

const EMPTY_FORM = {
  title: "",
  description: "",
  audience: "CUSTOMER",
  discountType: "PERCENTAGE",
  discountValue: "",
  couponCode: "",
  minOrderValue: "",
  maxDiscount: "",
  startDate: getToday(),
  endDate: "",
  emailNotification: true,
};

/*
=========================================================
 COMPONENT
=========================================================
*/

function Offers() {
  /*
  =======================================================
  OFFERS STATE

  IMPORTANT:
  We load localStorage directly inside useState.

  This avoids:

  useEffect()
      ↓
  setOffers()
      ↓
  render
      ↓
  effect again

  =======================================================
  */

  const [offers, setOffers] = useState(() => {
    const savedOffers = localStorage.getItem(STORAGE_KEY);

    if (savedOffers) {
      try {
        return JSON.parse(savedOffers);
      } catch (error) {
        console.error(
          "Failed to load saved ShivHub offers:",
          error
        );

        return INITIAL_OFFERS;
      }
    }

    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify(INITIAL_OFFERS)
    );

    return INITIAL_OFFERS;
  });

  /*
  =======================================================
  FORM
  =======================================================
  */

  const [form, setForm] = useState(EMPTY_FORM);

  /*
  =======================================================
  EDIT MODE
  =======================================================
  */

  const [editingId, setEditingId] = useState(null);

  /*
  =======================================================
  ACTIVE TAB
  =======================================================
  */

  const [activeTab, setActiveTab] = useState("ALL");

  /*
  =======================================================
  SEARCH
  =======================================================
  */

  const [search, setSearch] = useState("");

  /*
  =======================================================
  SHOW FORM
  =======================================================
  */

  const [showForm, setShowForm] = useState(false);

  /*
  =======================================================
  MESSAGE
  =======================================================
  */

  const [message, setMessage] = useState("");

  /*
  =======================================================
  SAVE TO LOCAL STORAGE
  =======================================================
  */

  useEffect(() => {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify(offers)
    );
  }, [offers]);

  /*
  =======================================================
  UPDATE STATUS

  Status is calculated only for display.
  =======================================================
  */

  const processedOffers = useMemo(() => {
    return offers.map((offer) => ({
      ...offer,
      calculatedStatus: calculateStatus(offer),
    }));
  }, [offers]);

  /*
  =======================================================
  FILTER OFFERS
  =======================================================
  */

  const filteredOffers = useMemo(() => {
    const searchValue = search.trim().toLowerCase();

    return processedOffers.filter((offer) => {
      const matchesSearch =
        !searchValue ||
        offer.title.toLowerCase().includes(searchValue) ||
        offer.description
          .toLowerCase()
          .includes(searchValue) ||
        offer.couponCode
          .toLowerCase()
          .includes(searchValue);

      const matchesTab =
        activeTab === "ALL" ||
        offer.calculatedStatus === activeTab;

      return matchesSearch && matchesTab;
    });
  }, [
    processedOffers,
    search,
    activeTab,
  ]);

  /*
  =======================================================
  DASHBOARD COUNTS
  =======================================================
  */

  const counts = useMemo(() => {
    const active = processedOffers.filter(
      (offer) =>
        offer.calculatedStatus === "ACTIVE"
    ).length;

    const scheduled = processedOffers.filter(
      (offer) =>
        offer.calculatedStatus === "SCHEDULED"
    ).length;

    const expired = processedOffers.filter(
      (offer) =>
        offer.calculatedStatus === "EXPIRED"
    ).length;

    const customerOffers = processedOffers.filter(
      (offer) =>
        offer.audience === "CUSTOMER" ||
        offer.audience === "ALL"
    ).length;

    const sellerOffers = processedOffers.filter(
      (offer) =>
        offer.audience === "SELLER" ||
        offer.audience === "ALL"
    ).length;

    return {
      total: processedOffers.length,
      active,
      scheduled,
      expired,
      customerOffers,
      sellerOffers,
    };
  }, [processedOffers]);

  /*
  =======================================================
  HANDLE FORM CHANGE
  =======================================================
  */

  const handleChange = (event) => {
    const {
      name,
      value,
      type,
      checked,
    } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]:
        type === "checkbox"
          ? checked
          : value,
    }));
  };

  /*
  =======================================================
  RESET FORM
  =======================================================
  */

  const resetForm = () => {
    setForm({
      ...EMPTY_FORM,
      startDate: getToday(),
    });

    setEditingId(null);
    setShowForm(false);
  };

  /*
  =======================================================
  VALIDATE FORM
  =======================================================
  */

  const validateForm = () => {
    if (!form.title.trim()) {
      setMessage("Please enter offer title.");
      return false;
    }

    if (!form.description.trim()) {
      setMessage(
        "Please enter offer description."
      );
      return false;
    }

    if (!form.discountValue) {
      setMessage(
        "Please enter discount value."
      );
      return false;
    }

    if (
      Number(form.discountValue) <= 0 ||
      Number(form.discountValue) > 100
    ) {
      setMessage(
        "Discount must be between 1% and 100%."
      );
      return false;
    }

    if (!form.startDate) {
      setMessage(
        "Please select start date."
      );
      return false;
    }

    if (!form.endDate) {
      setMessage(
        "Please select end date."
      );
      return false;
    }

    if (
      new Date(form.endDate) <
      new Date(form.startDate)
    ) {
      setMessage(
        "End date cannot be before start date."
      );
      return false;
    }

    return true;
  };

  /*
  =======================================================
  CREATE / UPDATE OFFER
  =======================================================
  */

  const handleSubmit = (event) => {
    event.preventDefault();

    setMessage("");

    if (!validateForm()) {
      return;
    }

    const status = calculateStatus(form);

    if (editingId) {
      /*
      ===================================================
      UPDATE EXISTING OFFER
      ===================================================
      */

      setOffers((previous) =>
        previous.map((offer) =>
          offer.id === editingId
            ? {
                ...offer,
                ...form,
                title: form.title.trim(),
                description:
                  form.description.trim(),
                couponCode:
                  form.couponCode
                    .trim()
                    .toUpperCase(),
                discountValue:
                  Number(
                    form.discountValue
                  ),
                minOrderValue:
                  Number(
                    form.minOrderValue || 0
                  ),
                maxDiscount:
                  Number(
                    form.maxDiscount || 0
                  ),
                calculatedStatus:
                  status,
              }
            : offer
        )
      );

      setMessage(
        "Offer updated successfully."
      );
    } else {
      /*
      ===================================================
      CREATE NEW OFFER
      ===================================================
      */

      const newOffer = {
        id: Date.now(),
        title: form.title.trim(),
        description:
          form.description.trim(),
        audience: form.audience,
        discountType:
          form.discountType,
        discountValue:
          Number(form.discountValue),
        couponCode:
          form.couponCode
            .trim()
            .toUpperCase(),
        minOrderValue:
          Number(
            form.minOrderValue || 0
          ),
        maxDiscount:
          Number(
            form.maxDiscount || 0
          ),
        startDate: form.startDate,
        endDate: form.endDate,
        emailNotification:
          form.emailNotification,
        status,
        createdAt: getToday(),
      };

      setOffers((previous) => [
        newOffer,
        ...previous,
      ]);

      setMessage(
        "New offer created successfully."
      );
    }

    setForm({
      ...EMPTY_FORM,
      startDate: getToday(),
    });

    setEditingId(null);

    setTimeout(() => {
      setMessage("");
    }, 3000);
  };

  /*
  =======================================================
  EDIT OFFER
  =======================================================
  */

  const handleEdit = (offer) => {
    setEditingId(offer.id);

    setForm({
      title: offer.title,
      description:
        offer.description,
      audience: offer.audience,
      discountType:
        offer.discountType,
      discountValue:
        offer.discountValue,
      couponCode:
        offer.couponCode,
      minOrderValue:
        offer.minOrderValue,
      maxDiscount:
        offer.maxDiscount,
      startDate:
        offer.startDate,
      endDate:
        offer.endDate,
      emailNotification:
        offer.emailNotification,
    });

    setShowForm(true);

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  };

  /*
  =======================================================
  DELETE OFFER
  =======================================================
  */

  const handleDelete = (id) => {
    const confirmed =
      window.confirm(
        "Are you sure you want to delete this offer?"
      );

    if (!confirmed) {
      return;
    }

    setOffers((previous) =>
      previous.filter(
        (offer) => offer.id !== id
      )
    );

    setMessage(
      "Offer deleted successfully."
    );

    setTimeout(() => {
      setMessage("");
    }, 3000);
  };

  /*
  =======================================================
  COPY COUPON
  =======================================================
  */

  const handleCopyCoupon = async (
    couponCode
  ) => {
    if (!couponCode) {
      return;
    }

    try {
      await navigator.clipboard.writeText(
        couponCode
      );

      setMessage(
        `Coupon ${couponCode} copied.`
      );

      setTimeout(() => {
        setMessage("");
      }, 2000);
    } catch (error) {
      console.error(
        "Coupon copy failed:",
        error
      );
    }
  };

  /*
  =======================================================
  RETURN
  =======================================================
  */

  return (
    <div className="offers-page">

      {/* =================================================
          HEADER
      ================================================= */}

      <div className="offers-header">

        <div>
          <p className="page-kicker">
            SHIVHUB ADMIN
          </p>

          <h1>
            Offers & Promotions
          </h1>

          <p className="page-description">
            Create and manage customer and seller
            offers from one place.
          </p>
        </div>

        <button
          className="primary-button"
          onClick={() => {
            setEditingId(null);
            setForm({
              ...EMPTY_FORM,
              startDate: getToday(),
            });
            setShowForm(true);
          }}
        >
          + Create Offer
        </button>

      </div>


      {/* =================================================
          MESSAGE
      ================================================= */}

      {message && (
        <div className="offer-message">
          {message}
        </div>
      )}


      {/* =================================================
          STAT CARDS
      ================================================= */}

      <div className="offer-stats">

        <div className="stat-card">
          <div className="stat-icon">
            🎁
          </div>

          <div>
            <span>
              Total Offers
            </span>

            <strong>
              {counts.total}
            </strong>
          </div>
        </div>


        <div className="stat-card active">
          <div className="stat-icon">
            ✓
          </div>

          <div>
            <span>
              Active
            </span>

            <strong>
              {counts.active}
            </strong>
          </div>
        </div>


        <div className="stat-card scheduled">
          <div className="stat-icon">
            ◷
          </div>

          <div>
            <span>
              Scheduled
            </span>

            <strong>
              {counts.scheduled}
            </strong>
          </div>
        </div>


        <div className="stat-card expired">
          <div className="stat-icon">
            ↺
          </div>

          <div>
            <span>
              Expired
            </span>

            <strong>
              {counts.expired}
            </strong>
          </div>
        </div>


        <div className="stat-card customer">
          <div className="stat-icon">
            👤
          </div>

          <div>
            <span>
              Customer Offers
            </span>

            <strong>
              {counts.customerOffers}
            </strong>
          </div>
        </div>


        <div className="stat-card seller">
          <div className="stat-icon">
            🏪
          </div>

          <div>
            <span>
              Seller Offers
            </span>

            <strong>
              {counts.sellerOffers}
            </strong>
          </div>
        </div>

      </div>


      {/* =================================================
          CREATE / EDIT FORM
      ================================================= */}

      {showForm && (
        <div className="offer-form-card">

          <div className="form-card-header">

            <div>
              <h2>
                {editingId
                  ? "Edit Offer"
                  : "Create New Offer"}
              </h2>

              <p>
                Configure the offer and choose
                who should receive it.
              </p>
            </div>

            <button
              className="close-button"
              onClick={resetForm}
              type="button"
            >
              ×
            </button>

          </div>


          <form onSubmit={handleSubmit}>

            <div className="form-grid">

              {/* TITLE */}

              <div className="form-group full">
                <label>
                  Offer Title
                </label>

                <input
                  type="text"
                  name="title"
                  value={form.title}
                  onChange={handleChange}
                  placeholder="Example: Diwali Mega Offer"
                />
              </div>


              {/* DESCRIPTION */}

              <div className="form-group full">
                <label>
                  Description
                </label>

                <textarea
                  name="description"
                  value={form.description}
                  onChange={handleChange}
                  placeholder="Write offer description..."
                  rows="4"
                />
              </div>


              {/* AUDIENCE */}

              <div className="form-group">
                <label>
                  Offer For
                </label>

                <select
                  name="audience"
                  value={form.audience}
                  onChange={handleChange}
                >
                  <option value="CUSTOMER">
                    Customers
                  </option>

                  <option value="SELLER">
                    Sellers
                  </option>

                  <option value="ALL">
                    Customers + Sellers
                  </option>
                </select>
              </div>


              {/* DISCOUNT TYPE */}

              <div className="form-group">
                <label>
                  Discount Type
                </label>

                <select
                  name="discountType"
                  value={form.discountType}
                  onChange={handleChange}
                >
                  <option value="PERCENTAGE">
                    Percentage
                  </option>

                  <option value="FLAT">
                    Flat Amount
                  </option>
                </select>
              </div>


              {/* DISCOUNT */}

              <div className="form-group">
                <label>
                  Discount Value
                </label>

                <input
                  type="number"
                  name="discountValue"
                  value={form.discountValue}
                  onChange={handleChange}
                  placeholder="20"
                  min="1"
                  max="100"
                />
              </div>


              {/* COUPON */}

              <div className="form-group">
                <label>
                  Coupon Code
                </label>

                <input
                  type="text"
                  name="couponCode"
                  value={form.couponCode}
                  onChange={handleChange}
                  placeholder="DIWALI20"
                />
              </div>


              {/* MIN ORDER */}

              <div className="form-group">
                <label>
                  Minimum Order Value
                </label>

                <input
                  type="number"
                  name="minOrderValue"
                  value={form.minOrderValue}
                  onChange={handleChange}
                  placeholder="1000"
                  min="0"
                />
              </div>


              {/* MAX DISCOUNT */}

              <div className="form-group">
                <label>
                  Maximum Discount
                </label>

                <input
                  type="number"
                  name="maxDiscount"
                  value={form.maxDiscount}
                  onChange={handleChange}
                  placeholder="2000"
                  min="0"
                />
              </div>


              {/* START DATE */}

              <div className="form-group">
                <label>
                  Start Date
                </label>

                <input
                  type="date"
                  name="startDate"
                  value={form.startDate}
                  onChange={handleChange}
                />
              </div>


              {/* END DATE */}

              <div className="form-group">
                <label>
                  End Date
                </label>

                <input
                  type="date"
                  name="endDate"
                  value={form.endDate}
                  onChange={handleChange}
                />
              </div>

            </div>


            {/* EMAIL */}

            <div className="email-option">

              <label>
                <input
                  type="checkbox"
                  name="emailNotification"
                  checked={
                    form.emailNotification
                  }
                  onChange={handleChange}
                />

                <span>
                  Send email notification to
                  selected audience
                </span>
              </label>

              <small>
                Example: "You have a new offer
                from ShivHub."
              </small>

            </div>


            {/* FORM BUTTONS */}

            <div className="form-actions">

              <button
                type="button"
                className="secondary-button"
                onClick={resetForm}
              >
                Cancel
              </button>

              <button
                type="submit"
                className="primary-button"
              >
                {editingId
                  ? "Update Offer"
                  : "Create Offer"}
              </button>

            </div>

          </form>

        </div>
      )}


      {/* =================================================
          OFFER HISTORY / LIST
      ================================================= */}

      <div className="offers-card">

        <div className="offers-card-header">

          <div>
            <h2>
              Offer History
            </h2>

            <p>
              View every offer created by admin.
            </p>
          </div>


          {/* SEARCH */}

          <div className="offer-search">

            <input
              type="text"
              value={search}
              onChange={(event) =>
                setSearch(
                  event.target.value
                )
              }
              placeholder="Search offers or coupon..."
            />

          </div>

        </div>


        {/* =================================================
            TABS
        ================================================= */}

        <div className="offer-tabs">

          <button
            className={
              activeTab === "ALL"
                ? "tab active"
                : "tab"
            }
            onClick={() =>
              setActiveTab("ALL")
            }
          >
            All
            <span>
              {counts.total}
            </span>
          </button>


          <button
            className={
              activeTab === "ACTIVE"
                ? "tab active"
                : "tab"
            }
            onClick={() =>
              setActiveTab("ACTIVE")
            }
          >
            Active
            <span>
              {counts.active}
            </span>
          </button>


          <button
            className={
              activeTab === "SCHEDULED"
                ? "tab active"
                : "tab"
            }
            onClick={() =>
              setActiveTab("SCHEDULED")
            }
          >
            Scheduled
            <span>
              {counts.scheduled}
            </span>
          </button>


          <button
            className={
              activeTab === "EXPIRED"
                ? "tab active"
                : "tab"
            }
            onClick={() =>
              setActiveTab("EXPIRED")
            }
          >
            Expired
            <span>
              {counts.expired}
            </span>
          </button>

        </div>


        {/* =================================================
            TABLE
        ================================================= */}

        {filteredOffers.length === 0 ? (

          <div className="empty-offers">

            <div className="empty-icon">
              🎁
            </div>

            <h3>
              No offers found
            </h3>

            <p>
              Try changing your search or
              create a new offer.
            </p>

          </div>

        ) : (

          <div className="offers-table-wrapper">

            <table className="offers-table">

              <thead>

                <tr>

                  <th>
                    OFFER
                  </th>

                  <th>
                    AUDIENCE
                  </th>

                  <th>
                    DISCOUNT
                  </th>

                  <th>
                    COUPON
                  </th>

                  <th>
                    VALIDITY
                  </th>

                  <th>
                    STATUS
                  </th>

                  <th>
                    EMAIL
                  </th>

                  <th>
                    ACTIONS
                  </th>

                </tr>

              </thead>


              <tbody>

                {filteredOffers.map(
                  (offer) => (

                    <tr key={offer.id}>

                      {/* OFFER */}

                      <td>

                        <div className="offer-title-cell">

                          <div className="offer-small-icon">
                            🎁
                          </div>

                          <div>

                            <strong>
                              {offer.title}
                            </strong>

                            <small>
                              {offer.description}
                            </small>

                          </div>

                        </div>

                      </td>


                      {/* AUDIENCE */}

                      <td>

                        <span
                          className={`audience-badge ${offer.audience.toLowerCase()}`}
                        >
                          {getAudienceLabel(
                            offer.audience
                          )}
                        </span>

                      </td>


                      {/* DISCOUNT */}

                      <td>

                        <strong className="discount-value">

                          {offer.discountType ===
                          "PERCENTAGE"
                            ? `${offer.discountValue}%`
                            : `₹${offer.discountValue}`}

                        </strong>

                      </td>


                      {/* COUPON */}

                      <td>

                        {offer.couponCode ? (

                          <button
                            className="coupon-code"
                            onClick={() =>
                              handleCopyCoupon(
                                offer.couponCode
                              )
                            }
                            title="Click to copy"
                          >
                            {offer.couponCode}
                          </button>

                        ) : (

                          <span className="no-coupon">
                            No Coupon
                          </span>

                        )}

                      </td>


                      {/* VALIDITY */}

                      <td>

                        <div className="validity">

                          <span>
                            {formatDate(
                              offer.startDate
                            )}
                          </span>

                          <span>
                            →
                          </span>

                          <span>
                            {formatDate(
                              offer.endDate
                            )}
                          </span>

                        </div>

                      </td>


                      {/* STATUS */}

                      <td>

                        <span
                          className={`status-badge ${offer.calculatedStatus.toLowerCase()}`}
                        >
                          {offer.calculatedStatus}
                        </span>

                      </td>


                      {/* EMAIL */}

                      <td>

                        {offer.emailNotification ? (

                          <span className="email-enabled">
                            ✓ Email
                          </span>

                        ) : (

                          <span className="email-disabled">
                            —
                          </span>

                        )}

                      </td>


                      {/* ACTIONS */}

                      <td>

                        <div className="action-buttons">

                          <button
                            className="edit-button"
                            onClick={() =>
                              handleEdit(
                                offer
                              )
                            }
                            title="Edit"
                          >
                            ✎
                          </button>


                          <button
                            className="delete-button"
                            onClick={() =>
                              handleDelete(
                                offer.id
                              )
                            }
                            title="Delete"
                          >
                            🗑
                          </button>

                        </div>

                      </td>

                    </tr>

                  )
                )}

              </tbody>

            </table>

          </div>

        )}

      </div>


      {/* =================================================
          INFORMATION
      ================================================= */}

      <div className="offer-info-box">

        <div className="info-icon">
          💡
        </div>

        <div>

          <h3>
            ShivHub Offer System
          </h3>

          <p>
            When backend notification APIs are
            connected, customer and seller email
            notifications will be automatically
            sent according to the selected audience.
          </p>

        </div>

      </div>

    </div>
  );
}

export default Offers;
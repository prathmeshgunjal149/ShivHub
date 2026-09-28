import { useNavigate } from "react-router-dom";

const categories = [
    { name: "Mobiles", icon: "📱" },
    { name: "Laptops", icon: "💻" },
    { name: "Televisions", icon: "📺" },
    { name: "Audio", icon: "🎧" },
    { name: "Smart Watches", icon: "⌚" },
    { name: "Accessories", icon: "🔌" },
    { name: "Cameras", icon: "📷" },
    { name: "Electronics", icon: "⚡" },
    { name: "Offers", icon: "🔥" }
];

const ProductScrollNavbar = () => {

    const navigate = useNavigate();

    return (
        <nav className="category-navbar">

            <div className="category-container">

                {categories.map((category) => (

                    <button
                        key={category.name}
                        onClick={() =>
                            navigate(
                                `/categories/${category.name.toLowerCase()}`
                            )
                        }
                    >
                        <span>{category.icon}</span>

                        <span>{category.name}</span>
                    </button>

                ))}

            </div>

        </nav>
    );
};

export default ProductScrollNavbar;
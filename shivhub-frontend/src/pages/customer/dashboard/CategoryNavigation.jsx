export default function CategoryNavigation({
    categories,
    activeCategory,
    onSelect
}) {

    return (
        <nav className="marketplace-category-navigation">

            <div className="marketplace-category-inner">

                {categories.map((category) => (

                    <button
                        key={category.name}
                        type="button"
                        className={
                            activeCategory === category.name
                                ? "marketplace-category-button active"
                                : "marketplace-category-button"
                        }
                        onClick={() =>
                            onSelect(category.name)
                        }
                    >

                        <span aria-hidden="true">
                            {category.name === "All" ? <Grid2X2 size={17} /> : <PackageSearch size={17} />}
                        </span>

                        <span>
                            {category.name}
                        </span>

                    </button>

                ))}

            </div>

        </nav>
    );
}
import { Grid2X2, PackageSearch } from "lucide-react";

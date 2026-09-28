import { useEffect, useState } from "react";
import { createAdminCategory, getAdminCategories, updateAdminCategory } from "../../services/adminService";
import AnimatedModal from "../../components/common/animations/AnimatedModal";
import "./AdminTools.css";

export default function Categories() {
    const [items, setItems] = useState([]);
    const [error, setError] = useState("");
    const [modal, setModal] = useState(null);
    const [name, setName] = useState("");
    const [saving, setSaving] = useState(false);

    const load = async () => {
        try {
            setError("");
            setItems(await getAdminCategories());
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Could not load categories.");
        }
    };

    useEffect(() => { load(); }, []);

    const open = item => {
        setModal(item || {});
        setName(item?.name || "");
        setError("");
    };

    const save = async event => {
        event.preventDefault();
        if (!name.trim()) {
            setError("Enter a category name.");
            return;
        }
        setSaving(true);
        try {
            if (modal.id) await updateAdminCategory(modal.id, { name });
            else await createAdminCategory(name);
            setModal(null);
            await load();
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Category could not be saved.");
        } finally {
            setSaving(false);
        }
    };

    const toggle = async item => {
        try {
            await updateAdminCategory(item.id, { active: !item.active });
            await load();
        } catch (requestError) {
            setError(requestError.response?.data?.message || "Category status could not be changed.");
        }
    };

    return (
        <main className="admin-tools">
            <header>
                <div><p className="eyebrow">CATALOGUE</p><h1>Categories</h1><p>Create, rename, and activate or hide product categories.</p></div>
                <button onClick={() => open(null)}>+ Add category</button>
            </header>
            {error && <p className="error">{error}</p>}
            <table>
                <thead><tr><th>Category</th><th>Subcategories</th><th>Status</th><th>Actions</th></tr></thead>
                <tbody>{items.map(item => <tr key={item.id}><td><strong>{item.name}</strong></td><td>{item.subCategories?.length || 0}</td><td className={item.active ? "active" : "inactive"}>{item.active ? "Active" : "Hidden"}</td><td className="actions"><button className="secondary" onClick={() => open(item)}>Edit</button><button className="secondary" onClick={() => toggle(item)}>{item.active ? "Hide" : "Activate"}</button></td></tr>)}</tbody>
            </table>
            {!items.length && <p className="empty">No categories found.</p>}
            <AnimatedModal open={Boolean(modal)} onBackdropMouseDown={() => !saving && setModal(null)} ariaLabel={modal?.id ? "Edit category" : "Add category"}>
                <form onSubmit={save}>
                    <h2>{modal?.id ? "Edit category" : "Add category"}</h2>
                    <label>Category name<input autoFocus value={name} maxLength="120" onChange={event => setName(event.target.value)} /></label>
                    <div className="actions"><button type="button" className="secondary" onClick={() => setModal(null)}>Cancel</button><button disabled={saving}>{saving ? "Saving…" : "Save category"}</button></div>
                </form>
            </AnimatedModal>
        </main>
    );
}

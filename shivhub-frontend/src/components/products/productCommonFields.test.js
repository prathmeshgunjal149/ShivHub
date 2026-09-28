import test from "node:test";
import assert from "node:assert/strict";
import { productCommonFields, variantRequest } from "./productCommonFields.js";

test("shirt common details exclude electronics fields and configured color is not duplicated", () => {
    const keys = productCommonFields("Men's Fashion", "Shirts", [{ specificationKey: "color" }]).map(field => field.key);
    assert.deepEqual(keys, []);
});
test("laptops retain model/warranty but not accessory compatibility", () => {
    const keys = productCommonFields("Computers", "Laptops").map(field => field.key);
    assert.ok(keys.includes("model")); assert.ok(keys.includes("warrantyDetails"));
    assert.ok(!keys.includes("compatibility")); assert.ok(!keys.includes("accessoryType"));
});
test("grocery/beauty and unselected subcategory do not inherit electronics fields", () => {
    assert.deepEqual(productCommonFields("Grocery & Daily Needs", "Rice"), []);
    assert.deepEqual(productCommonFields("Beauty & Personal Care", "Skin Care"), []);
    assert.deepEqual(productCommonFields("Electronics", ""), []);
});
test("variant request strips UI keys and keeps concurrency version and exact combination", () => {
    const row = variantRequest({ clientKey: "ui-key", reservedQuantity: 2, id: 1, version: 3,
        attributes: { size: "M" }, sellingPriceIncludingGst: "299.50", stockQuantity: "5" });
    assert.equal(row.version, 3); assert.equal(row.sellingPriceIncludingGst, 299.5);
    assert.deepEqual(row.attributes, { size: "M" }); assert.equal(row.stockQuantity, 5);
    assert.ok(!Object.hasOwn(row, "clientKey")); assert.ok(!Object.hasOwn(row, "reservedQuantity"));
});

import test from "node:test";
import assert from "node:assert/strict";
import { matchesAttributeFilters, valuesForAttribute } from "./productAttributeFilters.js";

test("uses stored non-mobile specifications as the source of customer filter values", () => {
    const shirt = { productSpecifications: JSON.stringify({ fabric: "Cotton", color: "Blue" }) };
    assert.deepEqual(valuesForAttribute(shirt, "fabric"), ["Cotton"]);
    assert.equal(matchesAttributeFilters(shirt, { fabric: "cotton", color: "Blue" }), true);
    assert.equal(matchesAttributeFilters(shirt, { fabric: "Linen" }), false);
});

test("matches cached variant axes without mixing the selected sizes", () => {
    const shirt = { productSpecifications: '{"size":"M, L","color":"Blue"}' };
    assert.equal(matchesAttributeFilters(shirt, { size: "L" }), true);
    assert.equal(matchesAttributeFilters(shirt, { size: "XL" }), false);
});

test("supports legacy customer-facing color fields without exposing internal variant data", () => {
    const product = { colorOptions: "Black, Silver", imei1: "never-used-by-filter" };
    assert.deepEqual(valuesForAttribute(product, "color"), ["Black", "Silver"]);
    assert.equal(matchesAttributeFilters(product, { color: "Silver" }), true);
});

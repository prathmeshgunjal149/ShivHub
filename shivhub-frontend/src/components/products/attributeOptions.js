export const attributeOptions = field => {
    if (!field.optionsJson) return [];
    try { const options = JSON.parse(field.optionsJson); return Array.isArray(options) ? options : []; }
    catch { return field.optionsJson.split(",").map(value => value.trim()).filter(Boolean); }
};

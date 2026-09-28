import api from "./api";
const unwrap = request => request.then(({ data }) => Array.isArray(data) ? data : []);
export const getStates = () => unwrap(api.get("/api/locations/states"));
export const getDistricts = state => state ? unwrap(api.get("/api/locations/districts", { params: { state } })) : Promise.resolve([]);
export const getCities = (state, district) => state && district ? unwrap(api.get("/api/locations/cities", { params: { state, district } })) : Promise.resolve([]);

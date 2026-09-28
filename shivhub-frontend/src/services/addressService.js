import api from "./api";
const unwrap = request => request.then(({ data }) => data);
export const getAddresses = () => unwrap(api.get("/api/customer/addresses"));
export const createAddress = payload => unwrap(api.post("/api/customer/addresses", payload));
export const updateAddress = (id, payload) => unwrap(api.put(`/api/customer/addresses/${id}`, payload));
export const setDefaultAddress = id => unwrap(api.patch(`/api/customer/addresses/${id}/default`));
export const deleteAddress = id => unwrap(api.delete(`/api/customer/addresses/${id}`));

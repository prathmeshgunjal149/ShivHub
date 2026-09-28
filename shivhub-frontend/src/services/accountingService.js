import api from "./api";

export const listFinancialYears = async () => (await api.get("/api/accounting/financial-years")).data;
export const createFinancialYear = async (payload) => (await api.post("/api/accounting/financial-years", payload)).data;
export const closeFinancialYear = async (id) => (await api.put(`/api/accounting/financial-years/${id}/close`)).data;

export const listLedgerAccounts = async () => (await api.get("/api/accounting/ledger-accounts")).data;
export const seedLedgerAccounts = async () => (await api.post("/api/accounting/ledger-accounts/defaults")).data;
export const createLedgerAccount = async (payload) => (await api.post("/api/accounting/ledger-accounts", payload)).data;

export const listJournalEntries = async (params = {}) => (await api.get("/api/accounting/journal-entries", { params })).data;
export const postJournalEntry = async (payload) => (await api.post("/api/accounting/journal-entries", payload)).data;

export const listOpeningBalances = async (financialYearId) => (await api.get(`/api/accounting/opening-balances/${financialYearId}`)).data;
export const saveOpeningBalance = async (payload) => (await api.post("/api/accounting/opening-balances", payload)).data;


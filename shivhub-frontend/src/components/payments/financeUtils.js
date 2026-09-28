export const newFinance = () => ({ companyId: "", schemeId: "", tenureMonths: 8, advanceMonths: 2, loanNumber: "", downpayment: 0, processingCharges: 0, dbdCharges: 0, otherCharges: 0, deduction: 0, adjustment: 0, remarks: "", installments: [] });

export const dateText = date => `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,"0")}-${String(date.getDate()).padStart(2,"0")}`;

export function generateEmis(start, months, amount) {
    const origin = new Date(`${start}T12:00:00`);
    if (!Number.isFinite(origin.getTime()) || months < 1 || months > 120) return [];
    const cents = Math.round(Math.max(0,amount)*100), base = Math.floor(cents/months);
    return Array.from({length:months}, (_,index) => {
        const end = new Date(origin.getFullYear(),origin.getMonth()+index+1,0);
        const due = new Date(origin.getFullYear(),origin.getMonth()+index,Math.min(origin.getDate(),end.getDate()));
        return {monthLabel:due.toLocaleDateString("en-IN",{month:"short",year:"numeric"}),amount:(base+(index===months-1?cents-base*months:0))/100,dueDate:dateText(due)};
    });
}

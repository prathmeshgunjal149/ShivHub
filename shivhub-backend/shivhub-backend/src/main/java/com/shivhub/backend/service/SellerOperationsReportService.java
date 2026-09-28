package com.shivhub.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shivhub.backend.dto.SellerOperationsReportResponse;
import com.shivhub.backend.entity.OfflineBill;
import com.shivhub.backend.entity.Product;
import com.shivhub.backend.entity.Purchase;
import com.shivhub.backend.entity.PurchaseItemSerial;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.enums.PurchaseStatus;
import com.shivhub.backend.repository.OfflineBillRepository;
import com.shivhub.backend.repository.ProductRepository;
import com.shivhub.backend.repository.PurchaseItemSerialRepository;
import com.shivhub.backend.repository.PurchaseRepository;
import com.shivhub.backend.repository.UserRepository;

/** Live operational reports sourced from seller-owned POS bills, purchases, products and serials. */
@Service
public class SellerOperationsReportService {
    private final UserRepository users; private final OfflineBillRepository bills; private final PurchaseRepository purchases; private final ProductRepository products; private final PurchaseItemSerialRepository serials;
    public SellerOperationsReportService(UserRepository users, OfflineBillRepository bills, PurchaseRepository purchases, ProductRepository products, PurchaseItemSerialRepository serials) { this.users=users; this.bills=bills; this.purchases=purchases; this.products=products; this.serials=serials; }
    @Transactional(readOnly=true)
    public SellerOperationsReportResponse get(String email, LocalDate startDate, LocalDate endDate) {
        User seller = users.findByEmail(email).orElseThrow(() -> new RuntimeException("Seller not found"));
        if (!seller.isEnabled() || seller.getRole()==null || !"SELLER".equals(seller.getRole().name())) throw new RuntimeException("Only active sellers can access reports");
        LocalDate start=startDate==null?LocalDate.now().withDayOfMonth(1):startDate, end=endDate==null?LocalDate.now():endDate;
        if(end.isBefore(start)) throw new RuntimeException("End date cannot be before start date.");
        LocalDateTime from=start.atStartOfDay(), until=end.plusDays(1).atStartOfDay();
        List<OfflineBill> filteredBills=bills.findBySellerIdAndCreatedAtBetweenOrderByCreatedAtDesc(seller.getId(),from,until);
        Map<String, Pay> payment=new HashMap<>(); Map<String, Cust> customer=new HashMap<>(); BigDecimal taxable=BigDecimal.ZERO,cgst=BigDecimal.ZERO,sgst=BigDecimal.ZERO,igst=BigDecimal.ZERO,discount=BigDecimal.ZERO,total=BigDecimal.ZERO;
        for(OfflineBill bill:filteredBills){String method=bill.getPaymentMethod()==null?"UNSPECIFIED":bill.getPaymentMethod().name(); payment.computeIfAbsent(method,x->new Pay()).add(amount(bill.getGrandTotal())); String name=blank(bill.getCustomerName())?"Walk-in Customer":bill.getCustomerName(); customer.computeIfAbsent(name+"|"+(bill.getCustomerMobile()==null?"":bill.getCustomerMobile()),x->new Cust(name,bill.getCustomerMobile())).add(amount(bill.getGrandTotal()),bill.getCreatedAt()); taxable=taxable.add(amount(bill.getTaxableAmount()));cgst=cgst.add(amount(bill.getCgst()));sgst=sgst.add(amount(bill.getSgst()));igst=igst.add(amount(bill.getIgst()));discount=discount.add(amount(bill.getDiscount()));total=total.add(amount(bill.getGrandTotal()));}
        List<SellerOperationsReportResponse.PaymentRow> payRows=payment.entrySet().stream().map(e->new SellerOperationsReportResponse.PaymentRow(e.getKey(),e.getValue().count,e.getValue().amount)).sorted((a,b)->b.amount().compareTo(a.amount())).toList();
        List<SellerOperationsReportResponse.CustomerRow> custRows=customer.values().stream().map(v->new SellerOperationsReportResponse.CustomerRow(v.name,v.mobile,v.count,v.total,v.last)).sorted((a,b)->b.totalSpent().compareTo(a.totalSpent())).toList();
        List<SellerOperationsReportResponse.GstRow> gst=List.of(new SellerOperationsReportResponse.GstRow("POS bills",taxable,cgst,sgst,igst,discount,total));
        List<SellerOperationsReportResponse.PurchaseRow> purchaseRows=purchases.findBySellerOrderByPurchaseDateDesc(seller).stream().filter(p->p.getPurchaseDate()!=null&&!p.getPurchaseDate().isBefore(from)&&p.getPurchaseDate().isBefore(until)).map(p->new SellerOperationsReportResponse.PurchaseRow(p.getId(),p.getInvoiceNumber(),p.getDistributor()==null||p.getDistributor().getDistributor()==null?"—":p.getDistributor().getDistributor().getBusinessName(),p.getPurchaseDate(),p.getStatus()==null?"—":p.getStatus().name(),amount(p.getGrandTotal()),p.getInvoiceFileUrl())).toList();
        List<SellerOperationsReportResponse.StockRow> stock=products.findBySeller(seller).stream().map(p->new SellerOperationsReportResponse.StockRow(p.getId(),p.getName(),p.getCategoryEntity()==null?p.getCategory():p.getCategoryEntity().getName(),p.getStock(),p.getStock()!=null&&p.getStock()<=5)).sorted(Comparator.comparing(SellerOperationsReportResponse.StockRow::stock,Comparator.nullsLast(Comparator.naturalOrder()))).toList();
        List<SellerOperationsReportResponse.SerialRow> serialRows=serials.findByPurchaseItemPurchaseSeller(seller).stream().map(s->new SellerOperationsReportResponse.SerialRow(s.getId(),s.getPurchaseItem()==null?safe("—"):safe(s.getPurchaseItem().getProductName()),s.getImei1(),s.getImei2(),s.getSerialNumber(),s.getStatus(),s.getSoldAt(),s.getSoldOfflineBillItem()==null||s.getSoldOfflineBillItem().getOfflineBill()==null?null:s.getSoldOfflineBillItem().getOfflineBill().getBillNumber())).sorted(Comparator.comparing(SellerOperationsReportResponse.SerialRow::soldAt,Comparator.nullsLast(Comparator.reverseOrder()))).toList();
        return new SellerOperationsReportResponse(payRows,gst,custRows,purchaseRows,stock,serialRows);
    }
    private BigDecimal amount(BigDecimal value){return value==null?BigDecimal.ZERO:value;} private boolean blank(String v){return v==null||v.isBlank();} private String safe(String v){return blank(v)?"—":v;}
    private static class Pay{long count;BigDecimal amount=BigDecimal.ZERO;void add(BigDecimal value){count++;amount=amount.add(value);}} private static class Cust{String name,mobile;long count;BigDecimal total=BigDecimal.ZERO;LocalDateTime last;Cust(String n,String m){name=n;mobile=m;}void add(BigDecimal value,LocalDateTime at){count++;total=total.add(value);if(last==null||(at!=null&&at.isAfter(last)))last=at;}}
}

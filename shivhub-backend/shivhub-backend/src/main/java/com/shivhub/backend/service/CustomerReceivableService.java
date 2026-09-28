package com.shivhub.backend.service;

import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.shivhub.backend.dto.*;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.repository.*;
import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor
public class CustomerReceivableService {
 private final CustomerReceivableRepository receivableRepository;
 private final CustomerReceivablePaymentRepository paymentRepository;
 private final EmailService emailService;
 @Transactional public Map<String,Object> create(CustomerReceivableRequest r, User seller) {
  if (r == null || r.getCustomerName() == null || r.getCustomerName().isBlank() || r.getSaleAmount() == null || r.getSaleAmount().signum() <= 0) throw new RuntimeException("Customer name and sale amount are required");
  CustomerReceivable x=new CustomerReceivable(); x.setSeller(seller); x.setCustomerName(r.getCustomerName().trim()); x.setCustomerMobile(r.getCustomerMobile()); x.setCustomerEmail(r.getCustomerEmail()); x.setInvoiceNumber(r.getInvoiceNumber()); x.setProductDetails(r.getProductDetails()); x.setImei(r.getImei()); x.setSaleAmount(r.getSaleAmount().setScale(2,RoundingMode.HALF_UP)); x.setSaleDate(r.getSaleDate()); x.setDueDate(r.getDueDate()); x.setNotes(r.getNotes());
  return view(receivableRepository.save(x));
 }
 @Transactional public Map<String,Object> receive(Long id, CustomerPaymentRequest r, User seller) {
  CustomerReceivable x=owned(id,seller); if(r==null||r.getAmount()==null||r.getAmount().signum()<=0||r.getPaymentMethod()==null) throw new RuntimeException("Amount and payment mode are required");
  BigDecimal remaining=remaining(x); BigDecimal amount=r.getAmount().setScale(2,RoundingMode.HALF_UP); if(amount.compareTo(remaining)>0) throw new RuntimeException("Collection cannot exceed remaining due of ₹"+remaining);
  CustomerReceivablePayment p=new CustomerReceivablePayment(); p.setReceivable(x); p.setAmount(amount); p.setPaymentMethod(r.getPaymentMethod()); p.setTransactionReference((r.getTransactionReference()==null||r.getTransactionReference().isBlank()) ? "CR-"+id+"-"+System.currentTimeMillis() : r.getTransactionReference().trim()); p.setNotes(r.getNotes()); p.setPaymentDate(r.getPaymentDate()); p.setRecordedBy(seller); paymentRepository.save(p);
  try { emailService.sendCustomerPaymentReceivedEmail(x.getCustomerEmail(), x.getCustomerName(), shopName(seller), x.getInvoiceNumber(), amount, remaining(x), p.getTransactionReference()); } catch (Exception ignored) { }
  return view(x);
 }
 @Transactional public Map<String,Object> reviseDueDate(Long id, CustomerDueDateRequest r, User seller) {
  CustomerReceivable x=owned(id,seller);
  if(r==null || r.getDueDate()==null) throw new RuntimeException("Promised payment date is required");
  if(remaining(x).signum()==0) throw new RuntimeException("This customer balance is already paid");
  x.setDueDate(r.getDueDate());
  if(r.getNotes()!=null && !r.getNotes().isBlank()) x.setNotes(r.getNotes().trim());
  return view(receivableRepository.save(x));
 }
 @Transactional(readOnly=true) public List<Map<String,Object>> list(User seller) { return receivableRepository.findBySellerOrderByDueDateAsc(seller).stream().map(this::view).toList(); }
 @Transactional(readOnly=true) public List<CustomerReceivablePayment> history(Long id, User seller) { return paymentRepository.findByReceivableOrderByPaymentDateDesc(owned(id,seller)); }
 public BigDecimal remaining(CustomerReceivable x) { return x.getSaleAmount().subtract(paymentRepository.totalPaid(x)).max(BigDecimal.ZERO).setScale(2,RoundingMode.HALF_UP); }
 private CustomerReceivable owned(Long id,User seller) { CustomerReceivable x=receivableRepository.findById(id).orElseThrow(()->new RuntimeException("Customer receivable not found")); if(!x.getSeller().getId().equals(seller.getId())) throw new RuntimeException("Not authorized"); return x; }
 private String shopName(User seller) { return seller.getBusinessName() == null || seller.getBusinessName().isBlank() ? "ShivHub Seller" : seller.getBusinessName(); }
 private Map<String,Object> view(CustomerReceivable x) { BigDecimal paid=paymentRepository.totalPaid(x); BigDecimal due=x.getSaleAmount().subtract(paid).max(BigDecimal.ZERO); String s=due.signum()==0?"PAID":paid.signum()>0?"PARTIAL":(x.getDueDate()!=null&&x.getDueDate().isBefore(LocalDate.now()))?"OVERDUE":"UNPAID"; Map<String,Object> v=new LinkedHashMap<>(); v.put("id",x.getId()); v.put("customerName",x.getCustomerName());v.put("customerMobile",x.getCustomerMobile());v.put("customerEmail",x.getCustomerEmail());v.put("invoiceNumber",x.getInvoiceNumber());v.put("productDetails",x.getProductDetails());v.put("imei",x.getImei());v.put("saleAmount",x.getSaleAmount());v.put("saleDate",x.getSaleDate());v.put("dueDate",x.getDueDate());v.put("notes",x.getNotes());v.put("paidAmount",paid);v.put("remainingAmount",due);v.put("paymentStatus",s);return v; }
}

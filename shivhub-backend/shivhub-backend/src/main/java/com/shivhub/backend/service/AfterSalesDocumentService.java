package com.shivhub.backend.service;

import java.util.Set;
import org.springframework.stereotype.Service;
import com.shivhub.backend.entity.*;
import com.shivhub.backend.repository.*;
import lombok.RequiredArgsConstructor;

/** Server-rendered, printable documents use live snapshots and never include purchase rate or internal notes. */
@Service
@RequiredArgsConstructor
public class AfterSalesDocumentService {
 private static final Set<String> TYPES=Set.of("JOB_SHEET","RECEIVING_RECEIPT","REPAIR_ESTIMATE","SERVICE_INVOICE","WARRANTY_CLAIM_RECEIPT","RETURN_RECEIPT","REPLACEMENT_RECEIPT","REFUND_RECEIPT","GST_CREDIT_NOTE");
 private final UserRepository users; private final ProductRepository products; private final ServiceEstimateRepository estimates; private final ServiceRefundRepository refunds; private final AfterSalesCreditNoteRepository creditNotes;
 public String render(ServiceRequest request,String type,boolean internal) {
  String kind=type==null?"JOB_SHEET":type.trim().toUpperCase();if(!TYPES.contains(kind))throw new IllegalArgumentException("Unsupported after-sales document type");
  User customer=users.findById(request.getCustomerId()).orElse(null);Product product=products.findById(request.getProductId()).orElse(null);ServiceEstimate estimate=estimates.findByServiceRequestId(request.getId()).orElse(null);ServiceRefund refund=refunds.findByServiceRequestIdOrderByInitiatedAtDesc(request.getId()).stream().findFirst().orElse(null);AfterSalesCreditNote credit=creditNotes.findByServiceRequestId(request.getId()).orElse(null);
  StringBuilder html=new StringBuilder("<!doctype html><html><head><meta charset='utf-8'><title>").append(e(kind)).append("</title><style>body{font:14px Arial;color:#172033;max-width:760px;margin:30px auto;padding:20px}header{border-bottom:2px solid #5b36d6;margin-bottom:20px}h1{margin:0}.grid{display:grid;grid-template-columns:1fr 1fr;gap:12px}.box{padding:12px;border:1px solid #dde1ea;border-radius:8px}small{color:#667085}@media print{body{margin:0}}</style></head><body><header><h1>ShivHub</h1><p>").append(e(kind.replace('_',' '))).append(" · ").append(e(request.getRequestNumber())).append("</p></header>");
  html.append("<div class='grid'><div class='box'><strong>Customer</strong><br>").append(e(customer==null?"Customer":customer.getName())).append("<br>").append(e(customer==null?"":customer.getMobile())).append("</div><div class='box'><strong>Product</strong><br>").append(e(product==null?"Product":product.getName())).append("<br>").append(e(serial(request))).append("</div><div class='box'><strong>Request</strong><br>Status: ").append(e(request.getStatus().name().replace('_',' '))).append("<br>Type: ").append(e(request.getRequestType().name().replace('_',' '))).append("</div><div class='box'><strong>Sale reference</strong><br>").append(request.getOrderId()!=null?"Online order #"+request.getOrderId():"Offline bill #"+request.getOfflineBillId()).append("<br>Warranty end: ").append(e(String.valueOf(request.getWarrantyEndDate()))).append("</div></div>");
  if(estimate!=null)html.append("<h2>Charges</h2><div class='box'>Inspection: ₹").append(estimate.getInspectionCharge()).append("<br>Parts: ₹").append(estimate.getPartsAmount()).append("<br>Labour: ₹").append(estimate.getLabourAmount()).append("<br>GST: ₹").append(estimate.getCgst().add(estimate.getSgst()).add(estimate.getIgst())).append("<br><strong>Total: ₹").append(estimate.getGrandTotal()).append("</strong></div>");
  if(refund!=null)html.append("<h2>Refund</h2><div class='box'>Amount: ₹").append(refund.getRefundAmount()).append("<br>Method: ").append(e(refund.getRefundMethod().name())).append("<br>Status: ").append(e(refund.getRefundStatus().name())).append("<br>Reference: ").append(e(refund.getTransactionReference())).append("</div>");
  if(credit!=null)html.append("<h2>GST Credit Note</h2><div class='box'>Number: ").append(e(credit.getCreditNoteNumber())).append("<br>Taxable: ₹").append(credit.getTaxableAmount()).append("<br>CGST: ₹").append(credit.getCgst()).append("<br>SGST: ₹").append(credit.getSgst()).append("<br>Total: ₹").append(credit.getGrandTotal()).append("</div>");
  html.append("<p><small>Generated from live ShivHub after-sales records. Original invoice totals are preserved.</small></p></body></html>");return html.toString();
 }
 private String serial(ServiceRequest r){return r.getImei1Snapshot()!=null?r.getImei1Snapshot():(r.getSerialNumberSnapshot()!=null?r.getSerialNumberSnapshot():"Non-serialised item");} private String e(String value){return value==null?"-":value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
}

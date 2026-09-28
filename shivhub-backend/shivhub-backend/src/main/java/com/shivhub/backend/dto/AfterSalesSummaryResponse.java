package com.shivhub.backend.dto;
import java.util.Map;
import lombok.Data;
@Data public class AfterSalesSummaryResponse { private long newRequests; private long underReview; private long productsReceived; private long pendingInspection; private long waitingCustomerApproval; private long inRepair; private long readyForDelivery; private long pendingRefunds; private long overdueServices; private Map<String, Long> byStatus; }

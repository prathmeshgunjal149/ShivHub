# ShivHub CA / Accounting / Stock Transfer File Map

This file shows where each new module lives, so the project stays easy to understand.

## Backend: Stock Transfer / Distributor Adjustment

Purpose: Phone/IMEI dusrya shop la transfer karne, stock kami karne, distributor adjustment pending/settled track karne.

Entity:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/entity/StockTransferAdjustment.java`

DTO:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/dto/CreateStockTransferAdjustmentRequest.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/dto/ConfirmDistributorAdjustmentRequest.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/dto/StockTransferAdjustmentResponse.java`

Repository:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/repository/StockTransferAdjustmentRepository.java`

Service:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/service/StockTransferAdjustmentService.java`

Controller:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/controller/StockTransferAdjustmentController.java`

Related existing file:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/entity/PurchaseItemSerial.java`

Important statuses:
- `DRAFT`
- `HANDOVER_CONFIRMED`
- `ADJUSTMENT_CONFIRMED`
- IMEI status after handover: `TRANSFERRED_OUT`

## Backend: Online Order IMEI / Reserved Stock

Purpose: Online order confirm zalyavar stock reserve karne, dispatch la selected IMEI sold karne.

Updated entity files:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/entity/Product.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/entity/Order.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/entity/OrderItem.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/entity/PurchaseItemSerial.java`

DTO:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/dto/OrderItemSerialSelectionRequest.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/dto/UpdateOrderStatusRequest.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/dto/OrderItemResponse.java`

Repository:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/repository/PurchaseItemSerialRepository.java`

Service:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/service/OrderService.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/service/SellerOrderService.java`

Controller:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/controller/AdminOrderController.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/controller/SellerOrderController.java`

## Backend: Accounting Base

Purpose: CA reports, ledger, opening balances, Trial Balance / P&L / Balance Sheet foundation.

Enums:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/enums/LedgerAccountType.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/enums/JournalEntryStatus.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/enums/OpeningBalanceType.java`

Entities:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/entity/FinancialYear.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/entity/LedgerAccount.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/entity/JournalEntry.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/entity/JournalLine.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/entity/OpeningBalance.java`

DTO:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/dto/FinancialYearRequest.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/dto/LedgerAccountRequest.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/dto/JournalEntryRequest.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/dto/JournalLineRequest.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/dto/OpeningBalanceRequest.java`

Repositories:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/repository/FinancialYearRepository.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/repository/LedgerAccountRepository.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/repository/JournalEntryRepository.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/repository/JournalLineRepository.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/repository/OpeningBalanceRepository.java`

Services:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/service/FinancialYearService.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/service/LedgerAccountService.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/service/JournalPostingService.java`
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/service/OpeningBalanceService.java`

Controller:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/controller/AccountingController.java`

## Backend: CA Reports Export

Purpose: CA la download dyaycha report/export layer.

DTO:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/dto/CaExportRequest.java`

Service:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/service/CaExportService.java`

Controller:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/controller/CaReportController.java`

Existing report source:
- `shivhub-backend/shivhub-backend/src/main/java/com/shivhub/backend/service/SellerSalesReportService.java`

## Frontend: Seller Pages

CA Reports:
- `shivhub-frontend/src/pages/seller/CAReports.jsx`
- `shivhub-frontend/src/pages/seller/CAReports.css`
- `shivhub-frontend/src/services/caReportService.js`

Accounting:
- `shivhub-frontend/src/pages/seller/Accounting.jsx`
- `shivhub-frontend/src/pages/seller/Accounting.css`
- `shivhub-frontend/src/services/accountingService.js`

Stock Transfers:
- `shivhub-frontend/src/pages/seller/StockTransfers.jsx`
- `shivhub-frontend/src/pages/seller/StockTransfers.css`
- `shivhub-frontend/src/services/stockTransferService.js`

Route/menu files:
- `shivhub-frontend/src/routes/AppRoutes.jsx`
- `shivhub-frontend/src/pages/seller/SellerSidebar.jsx`

## Rule Going Forward

New feature add kartana files asa split karayche:

- Entity: database table
- DTO: request/response body
- Repository: database query
- Service: business logic
- Controller: API endpoint
- Frontend service: API call
- Frontend page: screen UI
- Frontend CSS: page design

Report data manually duplicate karaycha nahi. Existing transaction modules source of truth rahnar.

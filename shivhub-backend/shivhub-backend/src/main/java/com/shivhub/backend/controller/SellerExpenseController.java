package com.shivhub.backend.controller;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.shivhub.backend.dto.SellerExpenseRequest;
import com.shivhub.backend.entity.SellerExpense;
import com.shivhub.backend.entity.User;
import com.shivhub.backend.repository.SellerExpenseRepository;
import com.shivhub.backend.repository.UserRepository;

@RestController @RequestMapping("/api/seller/expenses")
public class SellerExpenseController {
    private final SellerExpenseRepository expenses; private final UserRepository users; private final com.shivhub.backend.service.EmailService emailService;
    public SellerExpenseController(SellerExpenseRepository expenses, UserRepository users, com.shivhub.backend.service.EmailService emailService) { this.expenses = expenses; this.users = users; this.emailService = emailService; }
    @GetMapping public List<SellerExpense> list(Authentication auth, @RequestParam(required=false) LocalDate from, @RequestParam(required=false) LocalDate to) { User seller=seller(auth); LocalDate end=to==null?LocalDate.now():to; return expenses.findBySellerAndExpenseDateBetweenOrderByExpenseDateDesc(seller, from==null?end.withDayOfMonth(1):from, end); }
    @PostMapping public ResponseEntity<SellerExpense> create(Authentication auth, @Valid @RequestBody SellerExpenseRequest request) { User seller=seller(auth); SellerExpense expense=new SellerExpense(); expense.setSeller(seller); expense.setCategory(request.category().trim()); expense.setDescription(request.description().trim()); expense.setAmount(request.amount()); expense.setExpenseDate(request.expenseDate()); expense.setPaymentMethod(request.paymentMethod()); expense.setNotes(request.notes()); SellerExpense saved=expenses.save(expense); try { if(seller.getEmail()!=null&&!seller.getEmail().isBlank()) emailService.sendExpenseCreatedEmail(seller.getEmail(), seller.getName(), saved.getCategory(), saved.getDescription(), saved.getAmount(), saved.getExpenseDate(), saved.getPaymentMethod(), saved.getNotes()); } catch(Exception ignored) { } return ResponseEntity.status(201).body(saved); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(Authentication auth,@PathVariable Long id) { SellerExpense item=expenses.findById(id).orElseThrow(()->new RuntimeException("Expense not found")); if(!item.getSeller().getId().equals(seller(auth).getId())) throw new RuntimeException("You do not own this expense"); expenses.delete(item); return ResponseEntity.noContent().build(); }
    private User seller(Authentication auth) { return users.findByEmail(auth.getName()).orElseThrow(()->new RuntimeException("Seller not found")); }
}

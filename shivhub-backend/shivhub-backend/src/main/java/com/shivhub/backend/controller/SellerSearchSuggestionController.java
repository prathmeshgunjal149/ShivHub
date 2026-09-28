package com.shivhub.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.SearchSuggestionsResponse;
import com.shivhub.backend.service.SearchSuggestionService;

@RestController
@RequestMapping("/api/seller/search")
@PreAuthorize("hasRole('SELLER')")
public class SellerSearchSuggestionController {
    private final SearchSuggestionService service;
    public SellerSearchSuggestionController(SearchSuggestionService service) { this.service = service; }

    @GetMapping("/suggestions")
    public ResponseEntity<SearchSuggestionsResponse> suggestions(Authentication authentication,
                                                                  @RequestParam(required = false) String type,
                                                                  @RequestParam(name = "q", required = false) String query,
                                                                  @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(service.sellerSuggestions(authentication.getName(), type, query, limit));
    }
}

package com.shivhub.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shivhub.backend.dto.SearchSuggestionsResponse;
import com.shivhub.backend.service.SearchSuggestionService;

@RestController
@RequestMapping("/api/admin/search")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSearchSuggestionController {
    private final SearchSuggestionService service;
    public AdminSearchSuggestionController(SearchSuggestionService service) { this.service = service; }

    @GetMapping("/suggestions")
    public ResponseEntity<SearchSuggestionsResponse> suggestions(@RequestParam(required = false) String type,
                                                                  @RequestParam(name = "q", required = false) String query,
                                                                  @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(service.adminSuggestions(type, query, limit));
    }
}

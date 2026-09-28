package com.shivhub.backend.dto;

import java.util.List;

public record SearchSuggestionsResponse(String query, List<SearchSuggestionItem> suggestions) {}

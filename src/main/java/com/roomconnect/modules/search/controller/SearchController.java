package com.roomconnect.modules.search.controller;

import com.roomconnect.modules.search.dto.SearchRequest;
import com.roomconnect.modules.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * GET /api/search?lat=&lng=&radiusKm=&category=&...
     * Public endpoint — no auth required.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> search(SearchRequest req) {
        SearchService.SearchResult result = searchService.search(req);
        return ResponseEntity.ok(Map.of(
                "items",      result.items(),
                "total",      result.total(),
                "page",       result.page(),
                "size",       result.size(),
                "totalPages", result.size() > 0 ? (int) Math.ceil((double) result.total() / result.size()) : 0
        ));
    }
}

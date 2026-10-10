package com.example.orders.shared.web;

import com.example.orders.shared.error.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageSupport {
    private static final int MAX_PAGE_SIZE = 100;

    private PageSupport() { }

    public static Pageable create(int page, int size, String sort, String direction, Set<String> allowedSorts) {
        if (page < 0) throw new BadRequestException("INVALID_PAGE", "Page must be zero or greater");
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("INVALID_PAGE_SIZE", "Page size must be between 1 and 100");
        }
        if (!allowedSorts.contains(sort)) {
            throw new BadRequestException("INVALID_SORT", "Allowed sort fields: " + String.join(", ", allowedSorts));
        }
        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("INVALID_SORT_DIRECTION", "Sort direction must be asc or desc");
        }
        return PageRequest.of(page, size, Sort.by(sortDirection, sort));
    }
}

package com.abrik.bank_cards.bank_cards.dto.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    public static <T> PageResponse<T> of(List<T> content,
                                         int page,
                                         int size,
                                         long totalElements,
                                         int totalPages,
                                         boolean hasNext,
                                         boolean hasPrevious) {
        return new PageResponse<>(content, page, size, totalElements, totalPages, hasNext, hasPrevious);
    }
}

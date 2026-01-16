package com.skillstorm.reserveone.dto;

import java.util.List;

public class PaymentTransactionListResponseDto {
    private List<PaymentTransactionDto> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;

    public List<PaymentTransactionDto> getContent() {
        return content;
    }

    public void setContent(List<PaymentTransactionDto> content) {
        this.content = content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}

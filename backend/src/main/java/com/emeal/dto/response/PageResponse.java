package com.emeal.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public class PageResponse<T> {

    private List<T> content;
    private int pageNo;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public PageResponse() {
    }

    public PageResponse(List<T> content, int pageNo, int pageSize, long totalElements, int totalPages, boolean last) {
        this.content = content;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
    }

    public static <T> PageResponseBuilder<T> builder() {
        return new PageResponseBuilder<>();
    }

    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }

    public int getPageNo() { return pageNo; }
    public void setPageNo(int pageNo) { this.pageNo = pageNo; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }

    public static <T> PageResponse<T> fromPage(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    public static <T, R> PageResponse<R> fromPage(Page<T> page, List<R> mappedContent) {
        return PageResponse.<R>builder()
                .content(mappedContent)
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    public static class PageResponseBuilder<T> {
        private List<T> content;
        private int pageNo;
        private int pageSize;
        private long totalElements;
        private int totalPages;
        private boolean last;

        PageResponseBuilder() {}

        public PageResponseBuilder<T> content(List<T> content) { this.content = content; return this; }
        public PageResponseBuilder<T> pageNo(int pageNo) { this.pageNo = pageNo; return this; }
        public PageResponseBuilder<T> pageSize(int pageSize) { this.pageSize = pageSize; return this; }
        public PageResponseBuilder<T> totalElements(long totalElements) { this.totalElements = totalElements; return this; }
        public PageResponseBuilder<T> totalPages(int totalPages) { this.totalPages = totalPages; return this; }
        public PageResponseBuilder<T> last(boolean last) { this.last = last; return this; }

        public PageResponse<T> build() {
            return new PageResponse<>(content, pageNo, pageSize, totalElements, totalPages, last);
        }
    }
}

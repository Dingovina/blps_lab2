package itmo.blps.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class PageResponse<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int pageNumber;
    private int size;

    public PageResponse(List<T> content, long totalElements, int totalPages, int pageNumber, int size) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.pageNumber = pageNumber;
        this.size = size;
    }

    public List<T> getContent() {
        return content;
    }

    @JsonProperty("totalElements")
    public long getTotalElements() {
        return totalElements;
    }

    @JsonProperty("totalPages")
    public int getTotalPages() {
        return totalPages;
    }

    @JsonProperty("pageNumber")
    public int getPageNumber() {
        return pageNumber;
    }

    public int getSize() {
        return size;
    }
}

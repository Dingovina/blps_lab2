package itmo.blps.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int pageNumber;
    private int size;
}

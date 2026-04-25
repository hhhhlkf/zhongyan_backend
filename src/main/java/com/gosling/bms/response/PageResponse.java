package com.gosling.bms.response;

import lombok.Data;

import java.util.List;

@Data
public class PageResponse<T> {

    private List<T> fileList;
    private int page;
    private int pageSize;
    private int total;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    private Long snapshotTime;
}

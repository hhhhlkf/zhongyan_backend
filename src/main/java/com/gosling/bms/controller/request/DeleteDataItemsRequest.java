package com.gosling.bms.controller.request;

import lombok.Data;

import java.util.List;

@Data
public class DeleteDataItemsRequest {
    private String type;
    private String task;
    private List<String> names;
}

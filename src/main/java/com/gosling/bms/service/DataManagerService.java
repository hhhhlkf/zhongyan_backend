package com.gosling.bms.service;

import com.gosling.bms.dao.entity.FileData;
import com.gosling.bms.response.PageResponse;

import java.util.List;


public interface DataManagerService {

    /**
     * 获取文件列表
     *
     * @param type 文件类型
     * @param task 任务名称，包含history, collect, process
     * @return 文件列表
     */
    List<FileData> getFileList(String type, String task);

    PageResponse<FileData> getHistoryPage(String type, int page, int pageSize);

    PageResponse<FileData> getRecentPage(String type, String task, int page, int pageSize, Long snapshotTime);

    DeleteItemsResult deleteItems(String type, String task, List<String> names);

    DeleteItemsResult deleteAllItems(String type, String task);

    Boolean deleteHistoryFile(String type, String name);

    /**
     * 获取文件列表
     *
     * @param type 文件类型
     * @param task 任务名称，包含collect, process
     * @return
     */
    Boolean transferFile(String type, String task);

    /**
     * 监控文件夹并传输文件
     *
     * @return 是否成功
     */
    Boolean monitorAndTransferFile();


    /**
     * 关闭监控
     *
     * @return 是否成功
     */
    Boolean closeMonitor();


}

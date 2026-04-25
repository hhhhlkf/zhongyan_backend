package com.gosling.bms.controller;

import com.gosling.bms.controller.request.DeleteDataItemsRequest;
import com.gosling.bms.dao.entity.FileData;
import com.gosling.bms.exception.BaseException;
import com.gosling.bms.response.PageResponse;
import com.gosling.bms.response.ResponseResult;
import com.gosling.bms.service.CameraService;
import com.gosling.bms.service.DataManagerService;
import com.gosling.bms.service.DeleteItemsResult;
import com.gosling.bms.service.MethodService;
import com.gosling.bms.service.TransferService;
import com.gosling.bms.service.SonyCameraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
public class DataController {


    @Autowired
    private CameraService cameraService;

    @Autowired
    private DataManagerService dataManagerService;

    @Autowired
    private MethodService methodService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private SonyCameraService sonyCameraService;

    /**
     * 控制摄像头的开关和速度
     *
     * @param type     摄像头类型
     * @param status   开启或关闭摄像头
     * @param camSpeed 摄像头速度
     * @return 操作结果
     */
    @ResponseResult
    @GetMapping("/camera/control")
    public Map<String, Object> cameraControl(@RequestParam("type") String type, @RequestParam("status") Boolean status, @RequestParam("camSpeed") Integer camSpeed) {
        log.info("Received camera control request: type={}, status={}, camSpeed={}", type, status, camSpeed);
        if (!cameraService.isCameraAvailable(type)) {
            log.error("Camera type {} is not available", type);
            throw new BaseException("Camera " + type + " unavailable");
        }

        boolean result = status ? cameraService.startCamera(type, camSpeed) : cameraService.stopCamera(type);

        if (!result) {
            String action = status ? "start" : "stop";
            log.error("Failed to {} camera {}", action, type);
            throw new BaseException("Camera " + type + " " + action + " failed");
        }

        log.info("Camera {} {}ed successfully", type, status ? "start" : "stop");
        return Map.of("status", status);
    }

    /**
     * 传输控制
     *
     * @param status 是否开启传输监控
     * @return 监控状态
     */
    @ResponseResult
    @GetMapping("/data/transfer/control")
    public Map<String, Object> transferControl(@RequestParam("status") Boolean status) {
        if (status == null) {
            log.warn("Transfer control status is null, defaulting to true");
            status = true; // 默认开启监控
        }
        if (!transferService.isTransferAvailable()) {
            log.error("Transfer service is not available");
            throw new BaseException("Transfer service is not available");
        }

        if (status) {
            boolean startResult = transferService.startTransfer();
            if (!startResult) {
                log.error("Failed to start transfer service");
                throw new BaseException("Transfer service start failed");
            }
            log.info("Transfer service started successfully");
            return Map.of("status", true);
        } else {
            Boolean stopResult = transferService.stopTransfer();
            if (!stopResult) {
                log.error("Failed to stop transfer service");
                throw new BaseException("Transfer service stop failed");
            }
            log.info("Transfer service stopped");
            return Map.of("status", false);
        }
    }

    /**
     * 检查摄像头是否可用
     *
     * @param type 摄像头类型
     * @return 摄像头可用状态
     */
    @ResponseResult
    @GetMapping("/camera/available")
    public Map<String, Object> isCameraAvailable(@RequestParam String type) {
        if (!cameraService.isCameraAvailable(type)) {
            log.info("Camera type {} is unavailable", type);
            return Map.of("status", false);
        }
        log.info("Camera type {} is available", type);
        return Map.of("status", true);
    }

    @ResponseResult
    @GetMapping("/camera/rgb/timelapse/runtime")
    public Map<String, Object> getRgbTimelapseRuntime(@RequestParam(required = false) String mac) {
        log.info("Received Sony RGB timelapse runtime request, mac={}", mac);
        return sonyCameraService.getTimelapseRuntime(mac);
    }

    @ResponseResult
    @GetMapping("/camera/rgb/stop")
    public Map<String, Object> stopRgbCamera(@RequestParam(required = false) String mac) {
        log.info("Received Sony RGB camera stop request, mac={}", mac);
        return sonyCameraService.stop(mac);
    }

    @ResponseResult
    @GetMapping("/camera/rgb/timelapse/forever")
    public Map<String, Object> startRgbTimelapseForever(@RequestParam Integer interval,
                                                        @RequestParam(required = false) String mac) {
        if (interval == null || interval <= 0) {
            throw new BaseException("interval must be greater than 0");
        }
        log.info("Received Sony RGB timelapse forever request, interval={}, mac={}", interval, mac);
        return sonyCameraService.startTimelapseForever(interval, mac);
    }

    @ResponseResult
    @GetMapping("/camera/rgb/timelapse")
    public Map<String, Object> startRgbTimelapse(@RequestParam Integer interval,
                                                 @RequestParam Integer count,
                                                 @RequestParam(required = false) String mac) {
        if (interval == null || interval <= 0) {
            throw new BaseException("interval must be greater than 0");
        }
        if (count == null || count <= 0) {
            throw new BaseException("count must be greater than 0");
        }
        log.info("Received Sony RGB timelapse count request, interval={}, count={}, mac={}", interval, count, mac);
        return sonyCameraService.startTimelapse(interval, count, mac);
    }

    @ResponseResult
    @GetMapping("/camera/rgb/timelapse/forever/restart")
    public Map<String, Object> restartRgbTimelapseForever(@RequestParam Integer interval,
                                                          @RequestParam(required = false) String mac) {
        if (interval == null || interval <= 3) {
            throw new BaseException("interval must be greater than 3");
        }
        log.info("Received Sony RGB timelapse forever restart request, interval={}, mac={}", interval, mac);
        return sonyCameraService.restartTimelapseForever(interval, mac);
    }

    @ResponseResult
    @GetMapping("/camera/rgb/timelapse/restart")
    public Map<String, Object> restartRgbTimelapse(@RequestParam Integer interval,
                                                   @RequestParam Integer count,
                                                   @RequestParam(required = false) String mac) {
        if (interval == null || interval <= 3) {
            throw new BaseException("interval must be greater than 3");
        }
        if (count == null || count <= 3) {
            throw new BaseException("count must be greater than 3");
        }
        log.info("Received Sony RGB timelapse restart request, interval={}, count={}, mac={}", interval, count, mac);
        return sonyCameraService.restartTimelapse(interval, count, mac);
    }

    /**
     * 获取各模态数据历史记录
     *
     * @param type 模态类型
     * @return 历史记录列表
     */
    @ResponseResult
    @GetMapping("/data/history")
    public PageResponse<FileData> getHistoryList(@RequestParam String type,
                                                 @RequestParam(defaultValue = "1") Integer page) {
        if (page == null || page < 1) {
            throw new BaseException("page must be greater than or equal to 1");
        }
        return dataManagerService.getHistoryPage(type, page, 5);
    }


    /**
     * 转移采集的图片和处理的图片到history目录
     *
     * @param modal 模态类型
     * @param task 任务名称
     * @return 数据列表
     */
    @ResponseResult
    @GetMapping("/data/transfer")
    public Map<String, Object> transferData(@RequestParam String modal, @RequestParam String task) {
        Boolean b = dataManagerService.transferFile(modal, task);
        if (!b) {
            log.warn("Failed to transfer data for modal: {}, task: {}", modal, task);
        }
        return Map.of("status", b);
    }

    @ResponseResult
    @DeleteMapping("/data/items")
    public DeleteItemsResult deleteDataItems(@RequestBody DeleteDataItemsRequest request) {
        if (request == null) {
            throw new BaseException("request body must not be null");
        }
        return dataManagerService.deleteItems(request.getType(), request.getTask(), request.getNames());
    }

    @ResponseResult
    @DeleteMapping("/data/items/all")
    public DeleteItemsResult deleteAllDataItems(@RequestBody DeleteDataItemsRequest request) {
        if (request == null) {
            throw new BaseException("request body must not be null");
        }
        return dataManagerService.deleteAllItems(request.getType(), request.getTask());
    }

    @ResponseResult
    @GetMapping("/data/history/delete")
    public Map<String, Object> deleteHistoryData(@RequestParam String type,
                                                 @RequestParam String name) {
        Boolean deleted = dataManagerService.deleteHistoryFile(type, name);
        if (!deleted) {
            log.warn("History file not found for deletion: type={}, name={}", type, name);
        }
        return Map.of("status", deleted);
    }

    /**
     * 获取收集或处理数据列表
     *
     * @param type 模态类型
     * @param status 任务名称
     * @return 数据列表
     */
    @ResponseResult
    @GetMapping("/data/select")
    public Map<String, Object> selectMethod(@RequestParam String type, @RequestParam Boolean status){
        if (status){
            Boolean startResult = methodService.startMethod(type);
            if (!startResult) {
                log.error("Failed to start method: {}", type);
                throw new BaseException("Method " + type + " start failed");
            }
            log.info("Method {} started successfully", type);
            return Map.of("status", true);
        } else {
            Boolean stopResult = methodService.stopMethod(type);
            if (!stopResult) {
                log.error("Failed to stop method: {}", type);
                throw new BaseException("Method " + type + " stop failed");
            }
            log.info("Method {} stopped successfully", type);
            return Map.of("status", false);
        }
    }

    /**
     * 获取最近的数据文件列表
     *
     * @param type 模态类型
     * @param task 任务名称
     * @return 最近数据文件列表
     */

    @ResponseResult
    @GetMapping("/data/recent")
    public PageResponse<FileData> getRecentData(@RequestParam String type,
                                                @RequestParam String task,
                                                @RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(required = false) Long snapshotTime){
        if (page == null || page < 1) {
            throw new BaseException("page must be greater than or equal to 1");
        }
        return dataManagerService.getRecentPage(type, task, page, 10, null);
    }



}

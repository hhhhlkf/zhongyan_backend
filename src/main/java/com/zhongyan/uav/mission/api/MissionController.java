package com.zhongyan.uav.mission.api;

import com.zhongyan.uav.common.response.ResponseResult;
import com.zhongyan.uav.mission.api.request.CreateMissionRequest;
import com.zhongyan.uav.mission.api.response.MissionView;
import com.zhongyan.uav.mission.application.MissionApplicationService;
import com.zhongyan.uav.mission.application.MissionQueryService;
import com.zhongyan.uav.mission.domain.MissionStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@ResponseResult
@RestController
@RequestMapping("/missions")
public class MissionController {
    private final MissionApplicationService missionApplicationService;
    private final MissionQueryService missionQueryService;

    /**
     * 创建 Mission API 控制器。
     */
    public MissionController(MissionApplicationService missionApplicationService,
                             MissionQueryService missionQueryService) {
        this.missionApplicationService = missionApplicationService;
        this.missionQueryService = missionQueryService;
    }

    /**
     * 创建 Mission 草稿。
     */
    @PostMapping
    public MissionView createMission(@RequestBody CreateMissionRequest request) {
        return MissionView.from(missionApplicationService.createMission(request.toInput()));
    }

    /**
     * 按状态查询 Mission 列表。
     */
    @GetMapping
    public List<MissionView> listMissions(@RequestParam(defaultValue = "DRAFT") MissionStatus status) {
        return missionQueryService.listByStatus(status).stream()
                .map(MissionView::from)
                .collect(Collectors.toList());
    }

    /**
     * 按 Mission 编号查询详情。
     */
    @GetMapping("/{missionId}")
    public MissionView getMission(@PathVariable String missionId) {
        return MissionView.from(missionQueryService.getMission(missionId));
    }
}

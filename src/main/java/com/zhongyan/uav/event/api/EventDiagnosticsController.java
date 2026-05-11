package com.zhongyan.uav.event.api;

import com.zhongyan.uav.common.response.ResponseResult;
import com.zhongyan.uav.event.application.DeadLetterEventService;
import com.zhongyan.uav.event.application.EventReplayService;
import com.zhongyan.uav.event.application.OutboxPublishResult;
import com.zhongyan.uav.event.application.OutboxPublishService;
import com.zhongyan.uav.event.domain.EventEnvelope;
import com.zhongyan.uav.event.domain.OutboxStatus;
import com.zhongyan.uav.event.port.OutboxRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@ResponseResult
@RestController
@RequestMapping("/events")
public class EventDiagnosticsController {
    private final OutboxRepository outboxRepository;
    private final OutboxPublishService outboxPublishService;
    private final EventReplayService eventReplayService;
    private final DeadLetterEventService deadLetterEventService;

    public EventDiagnosticsController(OutboxRepository outboxRepository,
                                      OutboxPublishService outboxPublishService,
                                      EventReplayService eventReplayService,
                                      DeadLetterEventService deadLetterEventService) {
        this.outboxRepository = outboxRepository;
        this.outboxPublishService = outboxPublishService;
        this.eventReplayService = eventReplayService;
        this.deadLetterEventService = deadLetterEventService;
    }

    @GetMapping("/outbox/pending")
    public List<EventEnvelope> pendingOutbox(@RequestParam(defaultValue = "50") int limit) {
        return outboxRepository.findByStatus(OutboxStatus.PENDING, limit);
    }

    @GetMapping("/outbox/failed")
    public List<EventEnvelope> failedOutbox(@RequestParam(defaultValue = "50") int limit) {
        return outboxRepository.findByStatus(OutboxStatus.FAILED, limit);
    }

    @GetMapping("/dead-letters")
    public List<EventEnvelope> deadLetters(@RequestParam(defaultValue = "50") int limit) {
        return deadLetterEventService.listDeadLetters(limit);
    }

    @PostMapping("/outbox/publish")
    public List<OutboxPublishResult> publishPending(@RequestParam(defaultValue = "50") int limit) {
        return outboxPublishService.publishPending(limit);
    }

    @PostMapping("/outbox/retry-failed")
    public List<OutboxPublishResult> retryFailed(@RequestParam(defaultValue = "50") int limit) {
        return outboxPublishService.retryFailed(limit);
    }

    @PostMapping("/replay/{aggregateType}/{aggregateId}")
    public List<EventEnvelope> replayAggregate(@PathVariable String aggregateType,
                                               @PathVariable String aggregateId) {
        return eventReplayService.replayAggregate(aggregateType, aggregateId);
    }
}

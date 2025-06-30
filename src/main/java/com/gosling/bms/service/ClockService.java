package com.gosling.bms.service;

public interface ClockService {
    // 时间校准

    /**
     * 校准时间
     *
     * @return 是否成功
     */
    Boolean calibrateTime();
}

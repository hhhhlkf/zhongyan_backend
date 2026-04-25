package com.gosling.bms.service.impl;

import com.gosling.bms.conf.CameraConfig;
import com.gosling.bms.conf.DataConfig;
import com.gosling.bms.exception.BaseException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SonyCameraServiceImplTest {

    @Test
    void shouldBuildTimelapseForeverCommandWithDefaultMac() {
        SonyCameraServiceImpl service = new SonyCameraServiceImpl(buildConfig(), buildDataConfig());

        String command = service.buildTimelapseForeverCommand(buildRgbCamera(), 5, null);

        assertEquals("'python3' '/home/nvidia/zhongyan/model/Camera/camera_controller.py' --mac '9C:69:D3:6C:D2:4D' timelapse --interval 5 --forever",
                command);
    }

    @Test
    void shouldBuildTimelapseCountCommandWithExplicitMac() {
        SonyCameraServiceImpl service = new SonyCameraServiceImpl(buildConfig(), buildDataConfig());

        String command = service.buildTimelapseCountCommand(buildRgbCamera(), 3, 10, "AA:BB:CC:DD:EE:FF");

        assertEquals("'python3' '/home/nvidia/zhongyan/model/Camera/camera_controller.py' --mac 'AA:BB:CC:DD:EE:FF' timelapse --interval 3 --count 10",
                command);
    }

    @Test
    void shouldParseSuccessfulJsonResponse() {
        SonyCameraServiceImpl service = new SonyCameraServiceImpl(buildConfig(), buildDataConfig());

        Map<String, Object> result = service.parseCommandResponse(
                "{\"command\":\"status\",\"status\":\"success\",\"message\":\"ok\",\"camera_count\":1}",
                "",
                0,
                "status");

        assertEquals("success", result.get("status"));
        assertEquals("status", result.get("command"));
        assertEquals(1, result.get("camera_count"));
    }

    @Test
    void shouldThrowWhenCommandFails() {
        SonyCameraServiceImpl service = new SonyCameraServiceImpl(buildConfig(), buildDataConfig());

        assertThrows(BaseException.class, () -> service.parseCommandResponse(
                "{\"command\":\"stop\",\"status\":\"error\",\"message\":\"failed\"}",
                "traceback",
                1,
                "stop"));
    }

    @Test
    void shouldRestartForeverByStoppingBeforeStarting() {
        List<String> callOrder = new ArrayList<>();
        TestSonyCameraServiceImpl service = new TestSonyCameraServiceImpl(buildConfig(), callOrder);

        Map<String, Object> result = service.restartTimelapseForever(2, null);

        assertEquals(List.of("stop", "wait", "startForever"), callOrder);
        assertEquals(true, result.get("restart"));
        assertEquals(2, result.get("interval"));
        assertEquals("success", ((Map<?, ?>) result.get("stop_result")).get("status"));
        assertEquals(9527, result.get("pid"));
    }

    @Test
    void shouldRestartCountByStoppingBeforeStarting() {
        List<String> callOrder = new ArrayList<>();
        TestSonyCameraServiceImpl service = new TestSonyCameraServiceImpl(buildConfig(), callOrder);

        Map<String, Object> result = service.restartTimelapse(3, 12, null);

        assertEquals(List.of("stop", "wait", "startCount"), callOrder);
        assertEquals(3, result.get("interval"));
        assertEquals(12, result.get("count"));
    }

    @Test
    void shouldNotRestartWhenStopFails() {
        List<String> callOrder = new ArrayList<>();
        TestSonyCameraServiceImpl service = new TestSonyCameraServiceImpl(buildConfig(), callOrder);
        service.failStop = true;

        assertThrows(BaseException.class, () -> service.restartTimelapseForever(2, null));
        assertEquals(List.of("stop"), callOrder);
    }

    @Test
    void shouldThrowSpecificMessageWhenStartFailsAfterStop() {
        List<String> callOrder = new ArrayList<>();
        TestSonyCameraServiceImpl service = new TestSonyCameraServiceImpl(buildConfig(), callOrder);
        service.failStartForever = true;

        BaseException exception = assertThrows(BaseException.class, () -> service.restartTimelapseForever(2, null));

        assertEquals(List.of("stop", "wait", "startForever"), callOrder);
        assertEquals("Sony camera timelapse restart failed after stop succeeded: start failed", exception.getMessage());
    }

    @Test
    void shouldReturnStoppedRuntimeWhenNoTaskStarted() {
        SonyCameraServiceImpl service = new SonyCameraServiceImpl(buildConfig(), buildDataConfig());

        Map<String, Object> runtime = service.getTimelapseRuntime(null);

        assertEquals("stopped", runtime.get("status"));
        assertEquals(false, runtime.get("running"));
        assertEquals("9C:69:D3:6C:D2:4D", runtime.get("cameraKey"));
    }

    @Test
    void shouldMarkRuntimeStoppedAfterStop() {
        TestSonyCameraServiceImpl service = new TestSonyCameraServiceImpl(buildConfig(), new ArrayList<>());
        service.seedRuntime("9C:69:D3:6C:D2:4D", "running", "forever", 3, null);

        service.stop(null);

        Map<String, Object> runtime = service.getTimelapseRuntime(null);
        assertEquals("stopped", runtime.get("status"));
        assertFalse((Boolean) runtime.get("running"));
    }

    @Test
    void shouldClearTimeDirectoryWhenStartingNewSession() {
        List<String> callOrder = new ArrayList<>();
        TestSonyCameraServiceImpl service = new TestSonyCameraServiceImpl(buildConfig(), callOrder);

        service.startTimelapseForeverInternal(5, null, true);

        assertEquals(List.of("clearTime", "startForever"), callOrder);
    }

    private CameraConfig buildConfig() {
        CameraConfig config = new CameraConfig();
        config.setCameras(Collections.singletonList(buildRgbCamera()));
        return config;
    }

    private DataConfig buildDataConfig() {
        DataConfig dataConfig = new DataConfig();
        dataConfig.setBasePath("src/main/resources/static/images/res");
        dataConfig.setTime("time");
        return dataConfig;
    }

    private CameraConfig.CameraInfo buildRgbCamera() {
        CameraConfig.CameraInfo camera = new CameraConfig.CameraInfo();
        camera.setType("rgb");
        camera.setHost("192.168.1.101");
        camera.setUsername("root");
        camera.setPassword("root");
        camera.setControllerScript("/home/nvidia/zhongyan/model/Camera/camera_controller.py");
        camera.setPythonCommand("python3");
        camera.setDefaultMac("9C:69:D3:6C:D2:4D");
        camera.setCommandTimeoutMs(30000);
        return camera;
    }

    private static final class TestSonyCameraServiceImpl extends SonyCameraServiceImpl {
        private final List<String> callOrder;
        private boolean failStop;
        private boolean failStartForever;

        private TestSonyCameraServiceImpl(CameraConfig cameraConfig, List<String> callOrder) {
            super(cameraConfig, buildStaticDataConfig());
            this.callOrder = callOrder;
        }

        @Override
        public Map<String, Object> stop(String mac) {
            callOrder.add("stop");
            if (failStop) {
                throw new BaseException("stop failed");
            }
            Map<String, Object> result = successResult("stop", null);
            seedRuntime("9C:69:D3:6C:D2:4D", "stopped", "forever", 3, null);
            return result;
        }

        @Override
        public Map<String, Object> startTimelapseForever(Integer interval, String mac) {
            callOrder.add("startForever");
            if (failStartForever) {
                throw new BaseException("start failed");
            }
            return successResult("timelapse", 9527);
        }

        @Override
        public Map<String, Object> startTimelapse(Integer interval, Integer count, String mac) {
            callOrder.add("startCount");
            return successResult("timelapse", 9528);
        }

        @Override
        void waitBeforeRestart() {
            callOrder.add("wait");
        }

        @Override
        Map<String, Object> startTimelapseForeverInternal(Integer interval, String mac, boolean clearTimeDirectory) {
            if (clearTimeDirectory) {
                clearRgbTimeDirectory();
            }
            return startTimelapseForever(interval, mac);
        }

        @Override
        Map<String, Object> startTimelapseInternal(Integer interval, Integer count, String mac, boolean clearTimeDirectory) {
            if (clearTimeDirectory) {
                clearRgbTimeDirectory();
            }
            return startTimelapse(interval, count, mac);
        }

        @Override
        void clearRgbTimeDirectory() {
            callOrder.add("clearTime");
        }

        private Map<String, Object> successResult(String command, Integer pid) {
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
            result.put("command", command);
            result.put("status", "success");
            result.put("message", "ok");
            if (pid != null) {
                result.put("pid", pid);
            }
            return result;
        }

        private void seedRuntime(String cameraKey, String status, String captureMode, Integer interval, Integer count) {
            try {
                Class<?> runtimeClass = Class.forName("com.gosling.bms.service.impl.SonyCameraServiceImpl$TimelapseRuntime");
                java.lang.reflect.Constructor<?> constructor = runtimeClass.getDeclaredConstructor(String.class, String.class, Integer.class, Integer.class);
                constructor.setAccessible(true);
                Object runtime = constructor.newInstance(cameraKey, captureMode, interval, count);

                java.lang.reflect.Field statusField = runtimeClass.getDeclaredField("status");
                statusField.setAccessible(true);
                statusField.set(runtime, status);

                java.lang.reflect.Field startedAtField = runtimeClass.getDeclaredField("startedAt");
                startedAtField.setAccessible(true);
                startedAtField.set(runtime, new java.util.Date());

                java.lang.reflect.Field runtimeMapField = SonyCameraServiceImpl.class.getDeclaredField("timelapseRuntimeMap");
                runtimeMapField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, Object> runtimeMap = (Map<String, Object>) runtimeMapField.get(this);
                runtimeMap.put(cameraKey, runtime);
            } catch (Exception e) {
                throw new AssertionError("Failed to seed runtime", e);
            }
        }

        private static DataConfig buildStaticDataConfig() {
            DataConfig dataConfig = new DataConfig();
            dataConfig.setBasePath("src/main/resources/static/images/res");
            dataConfig.setTime("time");
            return dataConfig;
        }
    }
}

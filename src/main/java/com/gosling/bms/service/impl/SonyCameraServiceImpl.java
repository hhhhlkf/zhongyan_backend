package com.gosling.bms.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gosling.bms.conf.CameraConfig;
import com.gosling.bms.conf.DataConfig;
import com.gosling.bms.exception.BaseException;
import com.gosling.bms.service.SonyCameraService;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class SonyCameraServiceImpl implements SonyCameraService {

    private static final String RGB_CAMERA_TYPE = "rgb";
    private static final int DEFAULT_COMMAND_TIMEOUT_MS = 30000;
    private static final long RESTART_WAIT_MS = 1000L;
    private static final String TIMELAPSE_START_MARKER = "success";
    private static final long TIMELAPSE_START_TIMEOUT_MS = 15000L;

    private final CameraConfig cameraConfig;
    private final DataConfig dataConfig;
    private final ExecutorService timelapseExecutor = Executors.newCachedThreadPool();
    private final ConcurrentHashMap<String, TimelapseRuntime> timelapseRuntimeMap = new ConcurrentHashMap<>();

    public SonyCameraServiceImpl(CameraConfig cameraConfig, DataConfig dataConfig) {
        this.cameraConfig = cameraConfig;
        this.dataConfig = dataConfig;
    }

    @Override
    public Map<String, Object> getStatus(String mac) {
        CameraConfig.CameraInfo camera = getRgbCamera();
        String command = buildStatusCommand(camera, mac);

        return executeJsonCommand(camera, command, "status");
    }

    @Override
    public Map<String, Object> getTimelapseRuntime(String mac) {
        CameraConfig.CameraInfo camera = getRgbCamera();
        String cameraKey = resolveCameraKey(camera, mac);
        TimelapseRuntime runtime = timelapseRuntimeMap.get(cameraKey);
        if (runtime == null) {
            return buildRuntimeResponse(cameraKey, null);
        }
        return buildRuntimeResponse(cameraKey, runtime);
    }

    @Override
    public Map<String, Object> stop(String mac) {
        CameraConfig.CameraInfo camera = getRgbCamera();
        String command = buildStopCommand(camera, mac);
        Map<String, Object> result = executeJsonCommand(camera, command, "stop");
        markTimelapseStopped(resolveCameraKey(camera, mac), "stopped");
        return result;
    }

    @Override
    public Map<String, Object> startTimelapseForever(Integer interval, String mac) {
        return startTimelapseForeverInternal(interval, mac, true);
    }

    @Override
    public Map<String, Object> startTimelapse(Integer interval, Integer count, String mac) {
        return startTimelapseInternal(interval, count, mac, true);
    }

    @Override
    public Map<String, Object> restartTimelapseForever(Integer interval, String mac) {
        validatePositive(interval, "interval");
        return restartTimelapseInternal(interval, null, mac, true);
    }

    @Override
    public Map<String, Object> restartTimelapse(Integer interval, Integer count, String mac) {
        validatePositive(interval, "interval");
        validatePositive(count, "count");
        return restartTimelapseInternal(interval, count, mac, false);
    }

    String buildStatusCommand(CameraConfig.CameraInfo camera, String mac) {
        return buildBaseCommand(camera, mac) + " status";
    }

    String buildStopCommand(CameraConfig.CameraInfo camera, String mac) {
        return buildBaseCommand(camera, mac) + " stop";
    }

    String buildTimelapseForeverCommand(CameraConfig.CameraInfo camera, Integer interval, String mac) {
        validatePositive(interval, "interval");
        return buildBaseCommand(camera, mac) + " timelapse --interval " + interval + " --forever" + " --daemon";
    }

    String buildTimelapseCountCommand(CameraConfig.CameraInfo camera, Integer interval, Integer count, String mac) {
        validatePositive(interval, "interval");
        validatePositive(count, "count");
        return buildBaseCommand(camera, mac) + " timelapse --interval " + interval + " --count " + count + " --daemon";
    }

    Map<String, Object> parseCommandResponse(String stdout, String stderr, int exitStatus, String fallbackCommand) {
        String jsonLine = extractJsonLine(stdout);
        if (!StringUtils.hasText(jsonLine)) {
            throw new BaseException(buildErrorMessage("Sony camera command returned empty stdout", stderr, exitStatus));
        }

        JSONObject jsonObject;
        try {
            jsonObject = JSON.parseObject(jsonLine);
        } catch (Exception e) {
            throw new BaseException("Sony camera command returned invalid JSON: " + jsonLine, e);
        }

        String status = jsonObject.getString("status");
        String command = jsonObject.getString("command");
        String message = jsonObject.getString("message");

        if (exitStatus != 0 || !"success".equalsIgnoreCase(status)) {
            String commandName = StringUtils.hasText(command) ? command : fallbackCommand;
            String detail = StringUtils.hasText(message) ? message : "Sony camera command failed";
            throw new BaseException(buildErrorMessage(commandName + " failed: " + detail, stderr, exitStatus));
        }

        return new LinkedHashMap<>(jsonObject);
    }

    private Map<String, Object> executeTimelapseCommand(CameraConfig.CameraInfo camera,
                                                        String command,
                                                        String mac,
                                                        String captureMode,
                                                        Integer interval,
                                                        Integer count,
                                                        boolean clearTimeDirectory) {
        String cameraKey = resolveCameraKey(camera, mac);
        TimelapseRuntime existingRuntime = timelapseRuntimeMap.get(cameraKey);
        if (existingRuntime != null && existingRuntime.isActive()) {
            throw new BaseException("Sony camera timelapse is already running.");
        }
        if (clearTimeDirectory) {
            clearRgbTimeDirectory();
        }

        TimelapseRuntime runtime = new TimelapseRuntime(cameraKey, captureMode, interval, count);
        timelapseRuntimeMap.put(cameraKey, runtime);

        CompletableFuture<Map<String, Object>> startSignal = new CompletableFuture<>();
        CompletableFuture<Void> taskFuture = CompletableFuture.runAsync(
                () -> runTimelapseCommand(camera, command, runtime, startSignal),
                timelapseExecutor
        );
        runtime.setTaskFuture(taskFuture);

        try {
            System.out.println("Waiting for timelapse to start with command: " + command);
            return startSignal.get(TIMELAPSE_START_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markTimelapseFailed(runtime, "Sony camera timelapse start interrupted.");
            throw new BaseException("Sony camera timelapse start interrupted.", e);
        } catch (TimeoutException e) {
            markTimelapseFailed(runtime, "Sony camera timelapse start timeout.");
            throw new BaseException("Sony camera timelapse start timeout.", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof BaseException) {
                throw (BaseException) cause;
            }
            throw new BaseException("Sony camera timelapse start failed: " + cause.getMessage(), cause);
        }
    }

    private Map<String, Object> restartTimelapseInternal(Integer interval, Integer count, String mac, boolean forever) {
        Map<String, Object> stopResult = stop(mac);
        waitBeforeRestart();

        try {
            Map<String, Object> startResult = forever
                    ? startTimelapseForeverInternal(interval, mac, false)
                    : startTimelapseInternal(interval, count, mac, false);
            LinkedHashMap<String, Object> response = new LinkedHashMap<>(startResult);
            response.put("restart", true);
            response.put("interval", interval);
            if (!forever) {
                response.put("count", count);
            }
            response.put("stop_result", stopResult);
            return response;
        } catch (BaseException e) {
            throw new BaseException("Sony camera timelapse restart failed after stop succeeded: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> executeJsonCommand(CameraConfig.CameraInfo camera, String command, String fallbackCommand) {
        CommandResult result = execRemoteCommand(camera, command);
        return parseCommandResponse(result.getStdout(), result.getStderr(), result.getExitStatus(), fallbackCommand);
    }

    private void runTimelapseCommand(CameraConfig.CameraInfo camera,
                                     String command,
                                     TimelapseRuntime runtime,
                                     CompletableFuture<Map<String, Object>> startSignal) {
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
        try {
            session = jsch.getSession(camera.getUsername(), camera.getHost(), 22);
            session.setPassword(camera.getPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(getCommandTimeoutMs(camera));

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            channel.setErrStream(stderr);

            InputStream stdout = channel.getInputStream();
            channel.connect(getCommandTimeoutMs(camera));

            byte[] buffer = new byte[1024];
            long idleDeadline = System.currentTimeMillis() + getCommandTimeoutMs(camera);
            while (true) {
                boolean hasNewOutput = false;
                while (stdout.available() > 0) {
                    int read = stdout.read(buffer);
                    if (read < 0) {
                        break;
                    }
                    hasNewOutput = true;
                    stdoutBuffer.write(buffer, 0, read);
                    String output = stdoutBuffer.toString(StandardCharsets.UTF_8.name());
                    if (!startSignal.isDone() && output.contains(TIMELAPSE_START_MARKER)) {
                        runtime.markRunning();
                        startSignal.complete(buildTimelapseStartResponse(runtime));
                    }
                }

                if (hasNewOutput) {
                    idleDeadline = System.currentTimeMillis() + getCommandTimeoutMs(camera);
                }

                if (channel.isClosed()) {
                    while (stdout.available() > 0) {
                        int read = stdout.read(buffer);
                        if (read < 0) {
                            break;
                        }
                        stdoutBuffer.write(buffer, 0, read);
                    }
                    break;
                }

                if (System.currentTimeMillis() > idleDeadline) {
                    throw new BaseException("Sony camera command timeout: " + command);
                }

                Thread.sleep(200L);
            }

            String stdoutText = stdoutBuffer.toString(StandardCharsets.UTF_8.name()).trim();
            String stderrText = stderr.toString(StandardCharsets.UTF_8.name()).trim();
            int exitStatus = channel.getExitStatus();
            log.info("Sony timelapse command finished on {} exitStatus={}, stdout={}, stderr={}",
                    camera.getHost(), exitStatus, stdoutText, stderrText);

            if (!startSignal.isDone()) {
                if (stdoutText.contains(TIMELAPSE_START_MARKER) && exitStatus == 0) {
                    runtime.markRunning();
                    startSignal.complete(buildTimelapseStartResponse(runtime));
                } else {
                    String detail = StringUtils.hasText(stderrText) ? stderrText : stdoutText;
                    BaseException exception = new BaseException(buildErrorMessage(
                            "Sony camera timelapse start marker not found: " + detail,
                            stderrText,
                            exitStatus
                    ));
                    markTimelapseFailed(runtime, exception.getMessage());
                    startSignal.completeExceptionally(exception);
                    return;
                }
            }

            if (exitStatus == 0) {
                markTimelapseStopped(runtime.getCameraKey(), "completed");
            } else {
                markTimelapseFailed(runtime, buildErrorMessage("Sony camera timelapse failed", stderrText, exitStatus));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            BaseException exception = new BaseException("Sony camera timelapse interrupted.", e);
            markTimelapseFailed(runtime, exception.getMessage());
            startSignal.completeExceptionally(exception);
        } catch (Exception e) {
            BaseException exception = e instanceof BaseException
                    ? (BaseException) e
                    : new BaseException("Failed to execute Sony camera timelapse command: " + e.getMessage(), e);
            markTimelapseFailed(runtime, exception.getMessage());
            startSignal.completeExceptionally(exception);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private CameraConfig.CameraInfo getRgbCamera() {
        List<CameraConfig.CameraInfo> cameraList = cameraConfig.getCameras();
        if (cameraList == null || cameraList.isEmpty()) {
            throw new BaseException("No cameras configured.");
        }

        for (CameraConfig.CameraInfo camera : cameraList) {
            if (RGB_CAMERA_TYPE.equalsIgnoreCase(camera.getType())) {
                validateControllerConfig(camera);
                return camera;
            }
        }
        throw new BaseException("RGB camera is not configured.");
    }

    private void validateControllerConfig(CameraConfig.CameraInfo camera) {
        if (!StringUtils.hasText(camera.getControllerScript())) {
            throw new BaseException("RGB Sony controller script is not configured.");
        }
    }

    private String buildBaseCommand(CameraConfig.CameraInfo camera, String mac) {
        String pythonCommand = StringUtils.hasText(camera.getPythonCommand()) ? camera.getPythonCommand() : "python3";
        List<String> commandParts = new ArrayList<>();
        commandParts.add(shellQuote(pythonCommand));
        commandParts.add(shellQuote(camera.getControllerScript()));

        String targetMac = StringUtils.hasText(mac) ? mac : camera.getDefaultMac();
        if (StringUtils.hasText(targetMac)) {
            commandParts.add("--mac");
            commandParts.add(shellQuote(targetMac));
        }

        return String.join(" ", commandParts);
    }

    private String resolveCameraKey(CameraConfig.CameraInfo camera, String mac) {
        String targetMac = StringUtils.hasText(mac) ? mac : camera.getDefaultMac();
        return StringUtils.hasText(targetMac) ? targetMac : camera.getHost();
    }

    private CommandResult execRemoteCommand(CameraConfig.CameraInfo camera, String command) {
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;
        System.out.println("Executing command on camera " + camera.getHost() + ": " + command);
        try {
            session = jsch.getSession(camera.getUsername(), camera.getHost(), 22);
            session.setPassword(camera.getPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(getCommandTimeoutMs(camera));

            channel = (ChannelExec) session.openChannel("exec");
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            channel.setCommand(command);
            channel.setInputStream(null);
            channel.setErrStream(stderr);

            InputStream stdout = channel.getInputStream();
            channel.connect(getCommandTimeoutMs(camera));

            ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            long deadline = System.currentTimeMillis() + getCommandTimeoutMs(camera);
            while (true) {
                while (stdout.available() > 0) {
                    int read = stdout.read(buffer);
                    if (read < 0) {
                        break;
                    }
                    stdoutBuffer.write(buffer, 0, read);
                }

                if (channel.isClosed()) {
                    while (stdout.available() > 0) {
                        int read = stdout.read(buffer);
                        if (read < 0) {
                            break;
                        }
                        stdoutBuffer.write(buffer, 0, read);
                    }
                    break;
                }

                if (System.currentTimeMillis() > deadline) {
                    throw new BaseException("Sony camera command timeout: " + command);
                }

                Thread.sleep(200L);
            }

            String stdoutText = stdoutBuffer.toString(StandardCharsets.UTF_8.name()).trim();
            String stderrText = stderr.toString(StandardCharsets.UTF_8.name()).trim();
            int exitStatus = channel.getExitStatus();
            log.info("Sony camera command executed on {} exitStatus={}, stdout={}, stderr={}",
                    camera.getHost(), exitStatus, stdoutText, stderrText);
            return new CommandResult(stdoutText, stderrText, exitStatus);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BaseException("Sony camera command interrupted.", e);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            throw new BaseException("Failed to execute Sony camera command: " + e.getMessage(), e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private int getCommandTimeoutMs(CameraConfig.CameraInfo camera) {
        Integer timeout = camera.getCommandTimeoutMs();
        return timeout != null && timeout > 0 ? timeout : DEFAULT_COMMAND_TIMEOUT_MS;
    }

    void waitBeforeRestart() {
        try {
            Thread.sleep(RESTART_WAIT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BaseException("Sony camera restart interrupted.", e);
        }
    }

    Map<String, Object> startTimelapseForeverInternal(Integer interval, String mac, boolean clearTimeDirectory) {
        CameraConfig.CameraInfo camera = getRgbCamera();
        String command = buildTimelapseForeverCommand(camera, interval, mac);
        return executeTimelapseCommand(camera, command, mac, "forever", interval, null, clearTimeDirectory);
    }

    Map<String, Object> startTimelapseInternal(Integer interval, Integer count, String mac, boolean clearTimeDirectory) {
        CameraConfig.CameraInfo camera = getRgbCamera();
        String command = buildTimelapseCountCommand(camera, interval, count, mac);
        return executeTimelapseCommand(camera, command, mac, "count", interval, count, clearTimeDirectory);
    }

    void clearRgbTimeDirectory() {
        if (dataConfig == null || dataConfig.getBasePath() == null || dataConfig.getTime() == null) {
            return;
        }
        Path timeDir = Path.of(dataConfig.getBasePath(), RGB_CAMERA_TYPE, dataConfig.getTime());
        try {
            Files.createDirectories(timeDir);
            try (java.util.stream.Stream<Path> stream = Files.walk(timeDir)) {
                stream.sorted(Comparator.reverseOrder())
                        .filter(path -> !path.equals(timeDir))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception e) {
                                throw new BaseException("Failed to delete file in rgb time directory: " + path, e);
                            }
                        });
            }
        } catch (Exception e) {
            throw new BaseException("Failed to clear rgb time directory: " + timeDir, e);
        }
    }

    private void validatePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BaseException(fieldName + " must be greater than 0");
        }
    }

    private String extractJsonLine(String stdout) {
        if (!StringUtils.hasText(stdout)) {
            return "";
        }

        String[] lines = stdout.trim().split("\\r?\\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.startsWith("{") && line.endsWith("}")) {
                return line;
            }
        }
        return stdout.trim();
    }

    private String buildErrorMessage(String message, String stderr, int exitStatus) {
        if (StringUtils.hasText(stderr)) {
            return message + " (exit=" + exitStatus + ", stderr=" + stderr + ")";
        }
        return message + " (exit=" + exitStatus + ")";
    }

    private String shellQuote(String value) {
        if (!StringUtils.hasText(value)) {
            return "''";
        }
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private Map<String, Object> buildTimelapseStartResponse(TimelapseRuntime runtime) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("command", "timelapse");
        response.put("status", "success");
        response.put("message", "Timelapse started.");
        response.put("started", true);
        response.put("running", true);
        response.put("captureMode", runtime.getCaptureMode());
        response.put("interval", runtime.getInterval());
        if (runtime.getCount() != null) {
            response.put("count", runtime.getCount());
        }
        response.put("cameraKey", runtime.getCameraKey());
        response.put("startedAt", runtime.getStartedAt());
        return response;
    }

    private Map<String, Object> buildRuntimeResponse(String cameraKey, TimelapseRuntime runtime) {
        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("cameraKey", cameraKey);
        if (runtime == null) {
            response.put("status", "stopped");
            response.put("running", false);
            return response;
        }

        response.put("status", runtime.getStatus());
        response.put("running", runtime.isActive());
        response.put("captureMode", runtime.getCaptureMode());
        response.put("interval", runtime.getInterval());
        if (runtime.getCount() != null) {
            response.put("count", runtime.getCount());
        }
        response.put("startedAt", runtime.getStartedAt());
        response.put("finishedAt", runtime.getFinishedAt());
        if (StringUtils.hasText(runtime.getLastError())) {
            response.put("message", runtime.getLastError());
        }
        return response;
    }

    private void markTimelapseStopped(String cameraKey, String message) {
        TimelapseRuntime runtime = timelapseRuntimeMap.get(cameraKey);
        if (runtime == null) {
            return;
        }
        runtime.markStopped(message);
    }

    private void markTimelapseFailed(TimelapseRuntime runtime, String errorMessage) {
        if (runtime == null) {
            return;
        }
        runtime.markFailed(errorMessage);
    }

    private static final class CommandResult {
        private final String stdout;
        private final String stderr;
        private final int exitStatus;

        private CommandResult(String stdout, String stderr, int exitStatus) {
            this.stdout = stdout;
            this.stderr = stderr;
            this.exitStatus = exitStatus;
        }

        private String getStdout() {
            return stdout;
        }

        private String getStderr() {
            return stderr;
        }

        private int getExitStatus() {
            return exitStatus;
        }
    }

    private static final class TimelapseRuntime {
        private final String cameraKey;
        private final String captureMode;
        private final Integer interval;
        private final Integer count;
        private volatile String status;
        private volatile String lastError;
        private volatile Date startedAt;
        private volatile Date finishedAt;
        private volatile CompletableFuture<Void> taskFuture;

        private TimelapseRuntime(String cameraKey, String captureMode, Integer interval, Integer count) {
            this.cameraKey = cameraKey;
            this.captureMode = captureMode;
            this.interval = interval;
            this.count = count;
            this.status = "starting";
        }

        private String getCameraKey() {
            return cameraKey;
        }

        private String getCaptureMode() {
            return captureMode;
        }

        private Integer getInterval() {
            return interval;
        }

        private Integer getCount() {
            return count;
        }

        private String getStatus() {
            return status;
        }

        private String getLastError() {
            return lastError;
        }

        private Date getStartedAt() {
            return startedAt;
        }

        private Date getFinishedAt() {
            return finishedAt;
        }

        private void setTaskFuture(CompletableFuture<Void> taskFuture) {
            this.taskFuture = taskFuture;
        }

        private boolean isActive() {
            return "starting".equalsIgnoreCase(status) || "running".equalsIgnoreCase(status);
        }

        private void markRunning() {
            this.status = "running";
            if (this.startedAt == null) {
                this.startedAt = new Date();
            }
            this.finishedAt = null;
            this.lastError = null;
        }

        private void markStopped(String message) {
            this.status = "stopped";
            this.finishedAt = new Date();
            this.lastError = message;
        }

        private void markFailed(String errorMessage) {
            this.status = "failed";
            this.finishedAt = new Date();
            this.lastError = errorMessage;
        }
    }
}

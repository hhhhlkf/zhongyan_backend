package com.zhongyan.uav.device.infrastructure.ssh;

import com.zhongyan.uav.device.domain.DeviceCommandPayload;
import com.zhongyan.uav.device.domain.DeviceCommandResult;
import com.zhongyan.uav.device.infrastructure.DevicePayloads;
import com.zhongyan.uav.device.port.DeviceCommandExecutor;
import com.zhongyan.uav.common.resilience.ExternalCallGuard;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SshDeviceCommandExecutor implements DeviceCommandExecutor {
    private final Clock clock;
    private final int defaultConnectTimeoutMs;
    private final ExternalCallGuard guard;

    public SshDeviceCommandExecutor() {
        this(Clock.systemUTC());
    }

    public SshDeviceCommandExecutor(Clock clock) {
        this(clock, 10000, 1, 250, 3, 30000);
    }

    public SshDeviceCommandExecutor(Clock clock, int defaultConnectTimeoutMs, int retryMaxAttempts,
                                    long retryBackoffMs, int circuitFailureThreshold,
                                    long circuitOpenDurationMs) {
        this.clock = clock;
        this.defaultConnectTimeoutMs = Math.max(1000, defaultConnectTimeoutMs);
        this.guard = new ExternalCallGuard("device-ssh", retryMaxAttempts, retryBackoffMs,
                circuitFailureThreshold, circuitOpenDurationMs);
    }

    @Override
    public DeviceCommandResult execute(DeviceCommandPayload payload) {
        Map<String, Object> parameters = payload.parameters();
        String host = DevicePayloads.text(parameters, null, "host", "hostname", "ip");
        String command = DevicePayloads.text(parameters, null, "command", "remoteCommand");
        if (host == null || command == null) {
            return failure(payload, "SSH_BAD_REQUEST", "SSH host and command are required", "");
        }

        Instant startedAt = clock.instant();
        List<String> commandLine = commandLine(parameters, host, command);
        Process process = null;
        try {
            process = startProcess(commandLine);
            Process runningProcess = process;
            CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> readOutput(runningProcess));
            boolean finished = process.waitFor(payload.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return failure(payload, "SSH_TIMEOUT", "SSH command timed out", outputFuture.join());
            }
            String output = outputFuture.join();
            int exitCode = process.exitValue();
            Duration elapsed = Duration.between(startedAt, clock.instant());
            Map<String, Object> metadata = metadata(parameters, host, elapsed, exitCode);
            if (exitCode == 0) {
                return new DeviceCommandResult(payload.commandId(), payload.taskId(), payload.deviceId(),
                        true, "0", "ssh command executed", output, null, elapsed, metadata, clock.instant());
            }
            return new DeviceCommandResult(payload.commandId(), payload.taskId(), payload.deviceId(),
                    false, String.valueOf(exitCode), "ssh command failed", output, null, elapsed, metadata,
                    clock.instant());
        } catch (IOException ex) {
            return failure(payload, "SSH_IO_ERROR", "SSH command I/O failure: " + ex.getMessage(), "");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return failure(payload, "SSH_INTERRUPTED", "SSH command interrupted: " + ex.getMessage(), "");
        } catch (SshCommandException ex) {
            return failure(payload, "SSH_IO_ERROR", "SSH command launch failure: " + ex.getCause().getMessage(), "");
        } catch (IllegalStateException ex) {
            return failure(payload, "SSH_CIRCUIT_OPEN", ex.getMessage(), "");
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private Process startProcess(List<String> commandLine) throws IOException {
        try {
            return guard.execute("startProcess", () -> {
                try {
                    return new ProcessBuilder(commandLine).redirectErrorStream(true).start();
                } catch (IOException ex) {
                    throw new SshCommandException(ex);
                }
            });
        } catch (SshCommandException ex) {
            throw ex;
        }
    }

    private List<String> commandLine(Map<String, Object> parameters, String host, String command) {
        String username = DevicePayloads.text(parameters, null, "username", "user");
        int port = DevicePayloads.integer(parameters, 22, "port", "sshPort");
        int connectTimeoutSeconds = Math.max(1, DevicePayloads.integer(parameters, defaultConnectTimeoutMs,
                "connect-timeout-ms", "connectTimeoutMs") / 1000);
        String strictHostKeyChecking = DevicePayloads.text(parameters, "accept-new", "strictHostKeyChecking");
        String identityFile = DevicePayloads.text(parameters, null, "identityFile", "privateKeyPath");

        List<String> commandLine = new ArrayList<>();
        commandLine.add("ssh");
        commandLine.add("-o");
        commandLine.add("BatchMode=yes");
        commandLine.add("-o");
        commandLine.add("ConnectTimeout=" + connectTimeoutSeconds);
        commandLine.add("-o");
        commandLine.add("StrictHostKeyChecking=" + strictHostKeyChecking);
        if (identityFile != null) {
            commandLine.add("-i");
            commandLine.add(identityFile);
        }
        commandLine.add("-p");
        commandLine.add(String.valueOf(port));
        commandLine.add(username == null ? host : username + "@" + host);
        commandLine.add(command);
        return commandLine;
    }

    private String readOutput(Process process) {
        try (var reader = process.inputReader()) {
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
            return output.toString();
        } catch (IOException ex) {
            return ex.getMessage();
        }
    }

    private Map<String, Object> metadata(Map<String, Object> parameters, String host,
                                         Duration elapsed, int exitCode) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("protocol", "SSH");
        metadata.put("host", host);
        metadata.put("port", DevicePayloads.integer(parameters, 22, "port", "sshPort"));
        metadata.put("exitCode", exitCode);
        metadata.put("elapsedMs", elapsed.toMillis());
        if (parameters.containsKey("password")) {
            metadata.put("passwordAuthUnsupported", true);
        }
        return metadata;
    }

    private DeviceCommandResult failure(DeviceCommandPayload payload, String exitCode,
                                        String message, String rawOutput) {
        return DeviceCommandResult.failure(payload, exitCode, message, rawOutput,
                Map.of("protocol", "SSH", "failureClass", exitCode), clock.instant());
    }

    private static class SshCommandException extends RuntimeException {
        private SshCommandException(Throwable cause) {
            super(cause);
        }
    }
}

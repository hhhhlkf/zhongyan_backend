package com.zhongyan.uav.db;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationScriptTests {
    private static final Pattern MIGRATION_NAME = Pattern.compile("V(\\d+)__.+\\.sql");

    @Test
    void flywayMigrationsAreSequentialAndCoverCoreTables() throws IOException {
        Path migrationRoot = Path.of("src/main/resources/db/migration");
        List<Path> migrations;
        try (var paths = Files.list(migrationRoot)) {
            migrations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .collect(Collectors.toList());
        }

        assertThat(migrations).extracting(path -> path.getFileName().toString())
                .containsExactly(
                        "V1__enable_postgres_extensions.sql",
                        "V2__core_mission_task_asset.sql",
                        "V3__config_center.sql",
                        "V4__telemetry_event_outbox_agent.sql",
                        "V5__document_table_comments.sql");

        for (int index = 0; index < migrations.size(); index++) {
            Matcher matcher = MIGRATION_NAME.matcher(migrations.get(index).getFileName().toString());
            assertThat(matcher.matches()).isTrue();
            assertThat(Integer.parseInt(matcher.group(1))).isEqualTo(index + 1);
        }

        String allSql = migrations.stream()
                .map(this::readString)
                .collect(Collectors.joining("\n"))
                .toLowerCase();

        assertThat(allSql).contains("create extension if not exists postgis");
        assertThat(allSql).contains("create extension if not exists vector");
        assertThat(allSql).contains("create table mission");
        assertThat(allSql).contains("create table task_attempt");
        assertThat(allSql).contains("create table task_command");
        assertThat(allSql).contains("create table task_event");
        assertThat(allSql).contains("create table asset");
        assertThat(allSql).contains("create table device_config");
        assertThat(allSql).contains("create table camera_config");
        assertThat(allSql).contains("create table model_config");
        assertThat(allSql).contains("create table transfer_config");
        assertThat(allSql).contains("create table config_validation");
        assertThat(allSql).contains("create table uav_telemetry");
        assertThat(allSql).contains("create table event_outbox");
        assertThat(allSql).contains("create table agent_session");
        assertThat(allSql).contains("comment on table mission");
        assertThat(allSql).contains("comment on table device_config");
        assertThat(allSql).contains("comment on table agent_tool_call");
    }

    @Test
    void localProfileEnablesDatabaseAndTestProfileKeepsItDisabled() throws IOException {
        String localProfile = Files.readString(Path.of("src/main/resources/application-local.yml"));
        String testProfile = Files.readString(Path.of("src/main/resources/application-test.yml"));

        assertThat(localProfile).contains("jdbc:postgresql://localhost:5432/zhongyan_uav");
        assertThat(localProfile).contains("flyway:");
        assertThat(localProfile).contains("enabled: true");

        assertThat(testProfile).contains("DataSourceAutoConfiguration");
        assertThat(testProfile).contains("FlywayAutoConfiguration");
        assertThat(testProfile).contains("enabled: false");
    }

    private String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read " + path, exception);
        }
    }
}

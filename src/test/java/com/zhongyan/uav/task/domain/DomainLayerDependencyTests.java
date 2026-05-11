package com.zhongyan.uav.task.domain;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DomainLayerDependencyTests {
    private static final List<String> FORBIDDEN_IMPORTS = List.of(
            "import org.springframework.",
            "import jakarta.persistence.",
            "import javax.persistence.",
            "import org.apache.kafka.",
            "import io.minio.",
            "import com.jcraft.jsch.");

    /**
     * 扫描领域层源码，确保领域对象没有引入基础设施框架依赖。
     */
    @Test
    void domainLayerDoesNotImportInfrastructureFrameworks() throws IOException {
        Path sourceRoot = Path.of("src/main/java/com/zhongyan/uav");
        List<Path> domainSources;
        try (var paths = Files.walk(sourceRoot)) {
            domainSources = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().replace('\\', '/').contains("/domain/"))
                    .filter(path -> path.toString().endsWith(".java"))
                    .collect(Collectors.toList());
        }

        assertThat(domainSources).isNotEmpty();

        for (Path source : domainSources) {
            String content = Files.readString(source);
            assertThat(FORBIDDEN_IMPORTS)
                    .describedAs(source.toString())
                    .noneMatch(content::contains);
        }
    }
}

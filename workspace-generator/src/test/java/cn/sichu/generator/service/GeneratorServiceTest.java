package cn.sichu.generator.service;

import cn.sichu.generator.model.GenColumn;
import cn.sichu.generator.model.form.GeneratorRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
class GeneratorServiceTest {

    private GeneratorService generatorService;

    @BeforeEach
    void setUp() {
        GeneratorMetadataService metadataService = mock(GeneratorMetadataService.class);
        when(metadataService.listColumns("ies", "ies_device")).thenReturn(List.of(
            new GenColumn("device_name", "character varying", false, "设备名称"),
            new GenColumn("status", "smallint", false, "状态")));
        generatorService = new GeneratorService(metadataService);
    }

    @Test
    void previewBuildsBackendFrontendAndMenuFiles() {
        Map<String, String> files = generatorService.preview(request());

        assertEquals(12, files.size());
        assertTrue(files.containsKey("cn/sichu/ies/device/entity/IesDevice.java"));
        assertTrue(files.get("cn/sichu/ies/device/entity/IesDevice.java")
            .contains("class IesDevice extends TenantEntity"));
        assertTrue(files.get("cn/sichu/ies/device/controller/IesDeviceController.java")
            .contains("ies:device:create"));
        assertTrue(files.get("views/ies/device/index.vue").contains("TableCard"));
        assertTrue(files.get("types/ies/device.ts").contains("interface IesDeviceRow"));
        assertTrue(files.get("sql/device-menu.sql").contains("ies:device:list"));
    }

    @Test
    void generatedJavaFilesCompile() throws Exception {
        Map<String, String> files = generatorService.preview(request());
        Path source = Files.createTempDirectory("workspace-generated-source");
        Path classes = Files.createTempDirectory("workspace-generated-classes");
        List<String> javaFiles = files.entrySet().stream()
            .filter(entry -> entry.getKey().endsWith(".java"))
            .map(entry -> {
                try {
                    Path file = source.resolve(entry.getKey());
                    Files.createDirectories(file.getParent());
                    Files.writeString(file, entry.getValue());
                    return file.toString();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).toList();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        List<String> arguments = new ArrayList<>(List.of("-classpath",
            System.getProperty("java.class.path"), "-d", classes.toString()));
        arguments.addAll(javaFiles);
        int exit = compiler.run(null, null, null, arguments.toArray(String[]::new));

        assertEquals(0, exit);
    }

    @Test
    void downloadContainsAllGeneratedFiles() throws Exception {
        int count = 0;
        try (ZipInputStream zip = new ZipInputStream(
            new ByteArrayInputStream(generatorService.download(request())))) {
            while (zip.getNextEntry() != null) {
                count++;
            }
        }

        assertEquals(12, count);
    }

    private GeneratorRequest request() {
        GeneratorRequest request = new GeneratorRequest();
        request.setSchemaName("ies");
        request.setTableName("ies_device");
        request.setModuleName("ies");
        request.setBusinessName("device");
        request.setPackageName("cn.sichu.ies.device");
        request.setFunctionName("设备管理");
        request.setPermissionPrefix("ies:device");
        return request;
    }
}

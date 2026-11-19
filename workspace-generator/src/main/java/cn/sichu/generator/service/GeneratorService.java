package cn.sichu.generator.service;

import cn.sichu.common.exception.BusinessException;
import cn.sichu.common.result.ResultCode;
import cn.sichu.generator.model.GenColumn;
import cn.sichu.generator.model.form.GeneratorRequest;
import lombok.RequiredArgsConstructor;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
@Service
@RequiredArgsConstructor
public class GeneratorService {

    private static final Set<String> AUDIT_COLUMNS =
        Set.of("id", "tenant_id", "create_by", "create_time", "update_by",
            "update_time", "is_deleted", "deleted_by", "deleted_time",
            "remark");

    private final GeneratorMetadataService metadataService;

    public Map<String, String> preview(GeneratorRequest request) {
        validate(request);
        List<Map<String, Object>> columns =
            metadataService.listColumns(request.getSchemaName(),
                    request.getTableName()).stream()
                .filter(column -> !AUDIT_COLUMNS.contains(column.name()))
                .map(this::columnContext).toList();
        if (columns.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND,
                "目标表没有可生成的业务字段");
        }

        String className =
            request.getClassName() == null || request.getClassName().isBlank() ?
                toClassName(request.getTableName()) : request.getClassName();
        if (!className.matches("^[A-Z][A-Za-z0-9]*$")) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                "类名格式不正确");
        }
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("schemaName", request.getSchemaName());
        context.put("tableName", request.getTableName());
        context.put("packageName", request.getPackageName());
        context.put("packagePath", request.getPackageName().replace('.', '/'));
        context.put("moduleName", request.getModuleName());
        context.put("businessName", request.getBusinessName());
        context.put("ClassName", className);
        context.put("className",
            Character.toLowerCase(className.charAt(0)) + className.substring(
                1));
        context.put("functionName", request.getFunctionName());
        context.put("permissionPrefix", request.getPermissionPrefix());
        context.put("baseEntity",
            Boolean.TRUE.equals(request.getTenantScoped()) ? "TenantEntity" :
                "BaseEntity");
        context.put("columns", columns);
        context.put("createTimeType", LocalDateTime.class.getName());
        context.put("now", LocalDateTime.now().toString());

        Map<String, String> result = new LinkedHashMap<>();
        for (String template : templates()) {
            result.put(outputPath(template, context),
                render(template, context));
        }
        return result;
    }

    public byte[] download(GeneratorRequest request) {
        Map<String, String> files = preview(request);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output)) {
                for (Map.Entry<String, String> entry : files.entrySet()) {
                    zip.putNextEntry(new ZipEntry(entry.getKey()));
                    zip.write(entry.getValue()
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    zip.closeEntry();
                }
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FAILED, "生成 ZIP 失败");
        }
    }

    private Map<String, Object> columnContext(GenColumn column) {
        Map<String, Object> context = new LinkedHashMap<>();
        String javaName = toCamelName(column.name());
        context.put("columnName", column.name());
        context.put("name", javaName);
        context.put("javaType", javaType(column.dataType()));
        context.put("tsType", tsType(column.dataType()));
        context.put("nullable", column.nullable());
        context.put("comment",
            column.comment() == null ? javaName : column.comment());
        return context;
    }

    private String render(String templateName, Map<String, Object> values) {
        Properties properties = new Properties();
        properties.setProperty("resource.loaders", "class");
        properties.setProperty("resource.loader.class.class",
            "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        VelocityEngine engine = new VelocityEngine(properties);
        engine.init();
        Template template = engine.getTemplate(templateName, "UTF-8");
        StringWriter writer = new StringWriter();
        template.merge(new VelocityContext(values), writer);
        return writer.toString();
    }

    private List<String> templates() {
        return List.of("vm/java/entity.java.vm", "vm/java/mapper.java.vm",
            "vm/java/service.java.vm", "vm/java/serviceImpl.java.vm",
            "vm/java/controller.java.vm", "vm/java/form.java.vm",
            "vm/java/query.java.vm", "vm/java/vo.java.vm", "vm/ts/api.ts.vm",
            "vm/ts/types.ts.vm", "vm/vue/index.vue.vm", "vm/sql/menu.sql.vm");
    }

    private String outputPath(String template, Map<String, Object> context) {
        String base = switch (template) {
            case "vm/java/entity.java.vm" -> "entity";
            case "vm/java/mapper.java.vm" -> "mapper";
            case "vm/java/service.java.vm" -> "service";
            case "vm/java/serviceImpl.java.vm" -> "service/impl";
            case "vm/java/controller.java.vm" -> "controller";
            case "vm/java/form.java.vm" -> "model/form";
            case "vm/java/query.java.vm" -> "model/query";
            case "vm/java/vo.java.vm" -> "model/vo";
            default -> null;
        };
        String className = context.get("ClassName").toString();
        if (base != null) {
            String suffix = switch (template) {
                case "vm/java/mapper.java.vm" -> "Mapper";
                case "vm/java/service.java.vm" -> "Service";
                case "vm/java/serviceImpl.java.vm" -> "ServiceImpl";
                case "vm/java/controller.java.vm" -> "Controller";
                case "vm/java/form.java.vm" -> "Form";
                case "vm/java/query.java.vm" -> "Query";
                case "vm/java/vo.java.vm" -> "VO";
                default -> "";
            };
            String prefix =
                template.equals("vm/java/service.java.vm") ? "I" : "";
            return context.get("packagePath") + "/" + base + "/" + prefix
                + className + suffix + ".java";
        }
        return switch (template) {
            case "vm/ts/api.ts.vm" ->
                "api/" + context.get("moduleName") + "/" + context.get(
                    "businessName") + ".ts";
            case "vm/ts/types.ts.vm" ->
                "types/" + context.get("moduleName") + "/" + context.get(
                    "businessName") + ".ts";
            case "vm/vue/index.vue.vm" ->
                "views/" + context.get("moduleName") + "/" + context.get(
                    "businessName") + "/index.vue";
            case "vm/sql/menu.sql.vm" ->
                "sql/" + context.get("businessName") + "-menu.sql";
            default ->
                throw new IllegalArgumentException("未知模板: " + template);
        };
    }

    private void validate(GeneratorRequest request) {
        String identifier = "^[a-z][a-z0-9_]*$";
        if (!request.getSchemaName().matches(identifier)
            || !request.getTableName().matches(identifier)
            || !request.getModuleName().matches(identifier)
            || !request.getBusinessName().matches(identifier)) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                "schema、表名、模块名和业务名格式不正确");
        }
        if (!request.getPackageName()
            .matches("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                "包名格式不正确");
        }
        if (!request.getPermissionPrefix()
            .matches("^[a-z][a-z0-9]*(:[a-z0-9]+)+$")) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                "权限前缀格式不正确");
        }
    }

    private String toClassName(String tableName) {
        StringBuilder result = new StringBuilder();
        for (String part : tableName.split("_")) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
            }
        }
        return result.toString();
    }

    private String toCamelName(String columnName) {
        String className = toClassName(columnName);
        return Character.toLowerCase(className.charAt(0)) + className.substring(
            1);
    }

    private String javaType(String dataType) {
        return switch (dataType) {
            case "bigint" -> "Long";
            case "integer", "smallint" -> "Integer";
            case "boolean" -> "Boolean";
            case "numeric", "decimal", "real", "double precision" ->
                "BigDecimal";
            case "date" -> "LocalDate";
            case "time", "time without time zone" -> "LocalTime";
            case "timestamp", "timestamp without time zone",
                 "timestamp with time zone" -> "LocalDateTime";
            default -> "String";
        };
    }

    private String tsType(String dataType) {
        return switch (dataType) {
            case "bigint", "integer", "smallint", "numeric", "decimal", "real",
                 "double precision" -> "number";
            case "boolean" -> "boolean";
            default -> "string";
        };
    }
}

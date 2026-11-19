package cn.sichu.common.core.utils;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
public final class CsvUtils {

    private CsvUtils() {
    }

    public static void write(HttpServletResponse response, String filename,
        List<String> headers, List<List<String>> rows) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + filename + "\"");
        PrintWriter writer = response.getWriter();
        writer.write('﻿');
        writer.println(headers.stream().map(CsvUtils::escape)
            .collect(Collectors.joining(",")));
        for (List<String> row : rows) {
            writer.println(row.stream().map(CsvUtils::escape)
                .collect(Collectors.joining(",")));
        }
        writer.flush();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")
            || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

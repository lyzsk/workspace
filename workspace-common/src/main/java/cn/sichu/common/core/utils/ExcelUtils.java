package cn.sichu.common.core.utils;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.fesod.sheet.FastExcel;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
public final class ExcelUtils {

    private ExcelUtils() {
    }

    public static void write(HttpServletResponse response, String filename,
        String sheetName, List<String> headers, List<List<String>> rows)
        throws IOException {
        response.setContentType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + filename + "\"");
        write(response.getOutputStream(), sheetName, headers, rows);
    }

    public static void write(OutputStream outputStream, String sheetName,
        List<String> headers, List<List<String>> rows) {
        FastExcel.write(outputStream)
            .head(headers.stream().map(List::of).toList()).sheet(sheetName)
            .doWrite(rows);
    }
}

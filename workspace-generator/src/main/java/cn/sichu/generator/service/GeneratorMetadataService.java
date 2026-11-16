package cn.sichu.generator.service;

import cn.sichu.generator.model.GenColumn;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
@Service
@RequiredArgsConstructor
public class GeneratorMetadataService {

    private final JdbcTemplate jdbcTemplate;

    public List<String> listTables(String schemaName) {
        return jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = ? AND table_type = 'BASE TABLE' ORDER BY table_name",
            String.class, schemaName);
    }

    public List<GenColumn> listColumns(String schemaName, String tableName) {
        return jdbcTemplate.query(
            "SELECT column_name, data_type, is_nullable, col_description((table_schema || '.' || table_name)::regclass::oid, ordinal_position) "
                + "FROM information_schema.columns WHERE table_schema = ? AND table_name = ? "
                + "ORDER BY ordinal_position",
            (rs, rowNum) -> new GenColumn(rs.getString("column_name"),
                rs.getString("data_type"), "YES".equals(rs.getString("is_nullable")),
                rs.getString("col_description")), schemaName, tableName);
    }
}

package cn.sichu.common.core.file;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
public interface FileStorage {

    String store(InputStream input, String storageKey) throws IOException;

    InputStream open(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;
}

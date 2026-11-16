package cn.sichu.system.file.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void storesReadsDeletesAndRejectsTraversal() throws Exception {
        LocalFileStorage storage = new LocalFileStorage();
        ReflectionTestUtils.setField(storage, "storageRoot", tempDir.toString());
        storage.initialize();
        byte[] content = {1, 2, 3};

        storage.store(new ByteArrayInputStream(content), "notice/test.txt");

        assertArrayEquals(content, storage.open("notice/test.txt").readAllBytes());
        assertThrows(IllegalArgumentException.class,
            () -> storage.open("../outside.txt"));
        storage.delete("notice/test.txt");
    }
}

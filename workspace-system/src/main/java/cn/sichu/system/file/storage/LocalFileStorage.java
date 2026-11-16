package cn.sichu.system.file.storage;

import cn.sichu.common.file.FileStorage;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Component
public class LocalFileStorage implements FileStorage {

    @Value("${workspace.file.storage-root:./data/files}")
    private String storageRoot;

    private Path root;

    @PostConstruct
    void initialize() throws IOException {
        root = Path.of(storageRoot).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public String store(InputStream input, String storageKey) throws IOException {
        Path target = resolve(storageKey);
        Files.createDirectories(target.getParent());
        Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        return storageKey;
    }

    @Override
    public InputStream open(String storageKey) throws IOException {
        return Files.newInputStream(resolve(storageKey));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    private Path resolve(String storageKey) {
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        return target;
    }
}

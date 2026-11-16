package cn.sichu.system.file.model;

import cn.sichu.system.file.entity.SysFile;

import java.io.InputStream;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
public record FileDownload(SysFile file, InputStream input) {
}

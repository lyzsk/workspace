package cn.sichu.system.file.model.vo;

import cn.sichu.system.file.entity.SysFile;
import lombok.Data;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Data
public class SysFileVO {

    private String id;

    private String originalName;

    private String contentType;

    private Long sizeBytes;

    public static SysFileVO from(SysFile file) {
        SysFileVO vo = new SysFileVO();
        vo.setId(file.getId() == null ? null : file.getId().toString());
        vo.setOriginalName(file.getOriginalName());
        vo.setContentType(file.getContentType());
        vo.setSizeBytes(file.getSizeBytes());
        return vo;
    }
}

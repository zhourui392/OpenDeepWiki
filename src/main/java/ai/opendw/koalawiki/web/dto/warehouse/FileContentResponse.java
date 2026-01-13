package ai.opendw.koalawiki.web.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件内容响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileContentResponse {

    /**
     * 文件路径
     */
    private String path;

    /**
     * 文件内容
     */
    private String content;

    /**
     * 文件大小（字节）
     */
    private Long size;

    /**
     * 文件类型/扩展名
     */
    private String fileType;

    /**
     * 是否为二进制文件
     */
    private Boolean isBinary;

    /**
     * 编码格式
     */
    private String encoding;
}

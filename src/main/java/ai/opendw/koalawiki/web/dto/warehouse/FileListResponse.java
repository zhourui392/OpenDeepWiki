package ai.opendw.koalawiki.web.dto.warehouse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文件列表响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileListResponse {

    /**
     * 文件列表
     */
    private List<FileInfo> files;

    /**
     * 当前路径
     */
    private String currentPath;

    /**
     * 文件总数
     */
    private Integer totalCount;

    /**
     * 文件信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        /**
         * 文件名
         */
        private String name;

        /**
         * 文件路径
         */
        private String path;

        /**
         * 是否为目录
         */
        private Boolean isDirectory;

        /**
         * 文件大小（字节）
         */
        private Long size;

        /**
         * 修改时间
         */
        private Long lastModified;

        /**
         * 文件扩展名
         */
        private String extension;
    }
}

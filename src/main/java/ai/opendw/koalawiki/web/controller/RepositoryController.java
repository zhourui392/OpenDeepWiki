package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.core.service.IWarehouseSyncService;
import ai.opendw.koalawiki.domain.warehouse.WarehouseStatus;
import ai.opendw.koalawiki.infra.entity.WarehouseEntity;
import ai.opendw.koalawiki.infra.repository.WarehouseRepository;
import ai.opendw.koalawiki.web.dto.Result;
import ai.opendw.koalawiki.web.dto.warehouse.CustomSubmitWarehouseRequest;
import ai.opendw.koalawiki.web.dto.warehouse.FileContentResponse;
import ai.opendw.koalawiki.web.dto.warehouse.FileListResponse;
import ai.opendw.koalawiki.web.dto.warehouse.SyncRecordDto;
import ai.opendw.koalawiki.web.dto.warehouse.UpdateWarehouseRequest;
import ai.opendw.koalawiki.web.dto.warehouse.WarehouseResponse;
import ai.opendw.koalawiki.web.dto.warehouse.WarehouseStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ai.opendw.koalawiki.domain.ClassifyType;

/**
 * 仓库详细管理控制器
 * 提供仓库详情、删除、更新等REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/repository")
@RequiredArgsConstructor
@Validated
public class RepositoryController {

    private final WarehouseRepository warehouseRepository;
    private final IWarehouseSyncService warehouseSyncService;
    private final ai.opendw.koalawiki.core.git.GitPathResolver gitPathResolver;

    /**
     * 获取仓库详情
     */
    @GetMapping("/detail")
    public ResponseEntity<Result<WarehouseResponse>> getRepository(
            @RequestParam @NotBlank String id) {

        log.debug("获取仓库详情: {}", id);

        try {
            Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(id);
            if (!warehouseOpt.isPresent()) {
                return ResponseEntity.ok(Result.error("仓库不存在"));
            }

            WarehouseResponse response = convertToResponse(warehouseOpt.get());
            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("获取仓库详情失败: {}", id, e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 删除仓库
     */
    @DeleteMapping("/Repository")
    public ResponseEntity<Result<Boolean>> deleteRepository(
            @RequestParam @NotBlank String id) {

        log.info("删除仓库: {}", id);

        try {
            Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(id);
            if (!warehouseOpt.isPresent()) {
                return ResponseEntity.ok(Result.error("仓库不存在"));
            }

            // 取消正在进行的同步
            try {
                warehouseSyncService.cancelSync(id);
            } catch (Exception e) {
                log.warn("取消同步失败: {}", id, e);
            }

            // 删除仓库
            warehouseRepository.deleteById(id);

            return ResponseEntity.ok(Result.success(true, "删除成功"));

        } catch (Exception e) {
            log.error("删除仓库失败: {}", id, e);
            return ResponseEntity.ok(Result.error("删除失败: " + e.getMessage()));
        }
    }

    /**
     * 获取仓库列表
     */
    @GetMapping("/RepositoryList")
    public ResponseEntity<Result<Page<WarehouseResponse>>> getRepositoryList(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "12") @Min(1) int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {

        log.debug("获取仓库列表: page={}, pageSize={}, keyword={}, status={}",
                page, pageSize, keyword, status);

        try {
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<WarehouseEntity> warehouses;

            if (status != null && !status.trim().isEmpty()) {
                try {
                    WarehouseStatus warehouseStatus = WarehouseStatus.valueOf(status.toUpperCase());
                    warehouses = warehouseRepository.findByStatusOrderByCreatedAtDesc(warehouseStatus, pageable);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.ok(Result.error("无效的状态值: " + status));
                }
            } else if (keyword != null && !keyword.trim().isEmpty()) {
                warehouses = warehouseRepository.findByNameContainingIgnoreCase(keyword, pageable);
            } else {
                warehouses = warehouseRepository.findAll(pageable);
            }

            Page<WarehouseResponse> response = warehouses.map(this::convertToResponse);

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("获取仓库列表失败", e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 创建Git仓库
     */
    @PostMapping("/GitRepository")
    public ResponseEntity<Result<WarehouseResponse>> createGitRepository(
            @Valid @RequestBody CustomSubmitWarehouseRequest request) {

        log.info("创建Git仓库: {}/{}", request.getOrganization(), request.getRepositoryName());

        try {
            // 检查仓库是否已存在
            WarehouseEntity existing = warehouseRepository.findByAddress(request.getAddress());
            if (existing != null) {
                return ResponseEntity.ok(Result.error("仓库已存在"));
            }

            WarehouseEntity warehouse = new WarehouseEntity();
            warehouse.setId(UUID.randomUUID().toString());
            warehouse.setCreatedAt(new Date());
            warehouse.setName(request.getRepositoryName());
            warehouse.setAddress(request.getAddress());
            warehouse.setBranch(request.getBranch() != null ? request.getBranch() : "main");
            warehouse.setStatus(WarehouseStatus.PENDING);
            warehouse.setClassify(ClassifyType.DOCUMENTATION);

            if (request.getGitUserName() != null) {
                warehouse.setGitUserName(request.getGitUserName());
                warehouse.setGitPassword(request.getGitPassword());
            }

            warehouse = warehouseRepository.save(warehouse);

            // 异步触发同步
            try {
                warehouseSyncService.triggerSync(warehouse.getId(), false);
            } catch (Exception e) {
                log.warn("触发同步失败，但仓库已创建: {}", warehouse.getId(), e);
            }

            WarehouseResponse response = convertToResponse(warehouse);
            return ResponseEntity.ok(Result.success(response, "仓库创建成功"));

        } catch (Exception e) {
            log.error("创建Git仓库失败", e);
            return ResponseEntity.ok(Result.error("创建失败: " + e.getMessage()));
        }
    }

    /**
     * 更新仓库信息
     */
    @PutMapping("/UpdateWarehouse")
    public ResponseEntity<Result<WarehouseResponse>> updateWarehouse(
            @RequestParam @NotBlank String id,
            @Valid @RequestBody UpdateWarehouseRequest request) {

        log.info("更新仓库: {}", id);

        try {
            Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(id);
            if (!warehouseOpt.isPresent()) {
                return ResponseEntity.ok(Result.error("仓库不存在"));
            }

            WarehouseEntity warehouse = warehouseOpt.get();

            // 更新字段
            if (request.getName() != null) {
                warehouse.setName(request.getName());
            }
            if (request.getDescription() != null) {
                warehouse.setDescription(request.getDescription());
            }
            if (request.getBranch() != null) {
                warehouse.setBranch(request.getBranch());
            }
            if (request.getGitUserName() != null) {
                warehouse.setGitUserName(request.getGitUserName());
            }
            if (request.getGitPassword() != null) {
                warehouse.setGitPassword(request.getGitPassword());
            }
            if (request.getEnableSync() != null) {
                warehouse.setEnableSync(request.getEnableSync());
            }

            warehouse = warehouseRepository.save(warehouse);

            WarehouseResponse response = convertToResponse(warehouse);
            return ResponseEntity.ok(Result.success(response, "更新成功"));

        } catch (Exception e) {
            log.error("更新仓库失败: {}", id, e);
            return ResponseEntity.ok(Result.error("更新失败: " + e.getMessage()));
        }
    }

    /**
     * 重置仓库
     */
    @PostMapping("/ResetRepository")
    public ResponseEntity<Result<Boolean>> resetRepository(
            @RequestParam @NotBlank String id) {

        log.info("重置仓库: {}", id);

        try {
            Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(id);
            if (!warehouseOpt.isPresent()) {
                return ResponseEntity.ok(Result.error("仓库不存在"));
            }

            WarehouseEntity warehouse = warehouseOpt.get();

            // 重置状态
            warehouse.setStatus(WarehouseStatus.PENDING);
            warehouse.setError(null);
            warehouse.setVersion(null);

            warehouseRepository.save(warehouse);

            // 触发重新同步
            try {
                warehouseSyncService.triggerSync(id, true);
            } catch (Exception e) {
                log.warn("触发同步失败: {}", id, e);
            }

            return ResponseEntity.ok(Result.success(true, "重置成功"));

        } catch (Exception e) {
            log.error("重置仓库失败: {}", id, e);
            return ResponseEntity.ok(Result.error("重置失败: " + e.getMessage()));
        }
    }

    /**
     * 获取文件列表
     */
    @GetMapping("/Files")
    public ResponseEntity<Result<FileListResponse>> getFiles(
            @RequestParam @NotBlank String warehouseId,
            @RequestParam(required = false, defaultValue = "") String path) {

        log.debug("获取文件列表: warehouseId={}, path={}", warehouseId, path);

        try {
            // 查找仓库
            Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(warehouseId);
            if (!warehouseOpt.isPresent()) {
                return ResponseEntity.ok(Result.error("仓库不存在"));
            }

            WarehouseEntity warehouse = warehouseOpt.get();

            // 构建文件路径
            String storagePath = gitPathResolver.getStoragePath() + "/" + getRepositoryIdentifier(warehouse.getAddress());
            Path dirPath = Paths.get(storagePath, path);

            // 检查目录是否存在
            if (!Files.exists(dirPath)) {
                return ResponseEntity.ok(Result.error("目录不存在: " + path));
            }

            if (!Files.isDirectory(dirPath)) {
                return ResponseEntity.ok(Result.error("指定路径不是目录"));
            }

            // 列出文件
            List<FileListResponse.FileInfo> fileInfos = new ArrayList<>();

            try (Stream<Path> paths = Files.list(dirPath)) {
                fileInfos = paths
                        .filter(p -> !p.getFileName().toString().startsWith(".")) // 过滤隐藏文件
                        .map(p -> {
                            try {
                                File file = p.toFile();
                                String fileName = file.getName();
                                String extension = "";
                                if (fileName.contains(".") && !file.isDirectory()) {
                                    extension = fileName.substring(fileName.lastIndexOf(".") + 1);
                                }

                                return FileListResponse.FileInfo.builder()
                                        .name(fileName)
                                        .path(path.isEmpty() ? fileName : path + "/" + fileName)
                                        .isDirectory(file.isDirectory())
                                        .size(file.isDirectory() ? 0L : file.length())
                                        .lastModified(file.lastModified())
                                        .extension(extension)
                                        .build();
                            } catch (Exception e) {
                                log.warn("无法读取文件信息: {}", p, e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .sorted((a, b) -> {
                            // 目录排在前面
                            if (a.getIsDirectory() && !b.getIsDirectory()) return -1;
                            if (!a.getIsDirectory() && b.getIsDirectory()) return 1;
                            return a.getName().compareTo(b.getName());
                        })
                        .collect(Collectors.toList());
            }

            FileListResponse response = FileListResponse.builder()
                    .files(fileInfos)
                    .currentPath(path)
                    .totalCount(fileInfos.size())
                    .build();

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("获取文件列表失败: warehouseId={}, path={}", warehouseId, path, e);
            return ResponseEntity.ok(Result.error("获取文件列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取文件内容
     */
    @GetMapping("/FileContent")
    public ResponseEntity<Result<FileContentResponse>> getFileContent(
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String id) {

        try {
            // 兼容三种调用方式：
            // 1) 前端传 warehouseId + path (文档ID),通过文档ID查找实际路径
            // 2) 前端使用 id=warehouseId:path
            // 3) 直接传 warehouseId + path (实际文件路径)

            // 参数校验
            if (id != null && !id.isEmpty()) {
                // 兼容旧的格式 id=warehouseId:path
                String[] parts = id.split(":", 2);
                if (parts.length != 2) {
                    return ResponseEntity.ok(Result.error("文件ID格式不正确，期望格式为 warehouseId:path"));
                }
                warehouseId = parts[0];
                path = parts[1];
            }

            if (warehouseId == null || warehouseId.isEmpty() ||
                path == null || path.isEmpty()) {
                return ResponseEntity.ok(Result.error("参数错误，缺少 warehouseId/path 或 id"));
            }

            // 去掉path前导的斜杠或反斜杠
            if (path != null && (path.startsWith("/") || path.startsWith("\\"))) {
                path = path.substring(1);
            }

            // 将路径分隔符统一为正斜杠，避免Windows和Unix混用问题
            if (path != null) {
                path = path.replace("\\", "/");
            }

            log.debug("获取文件内容: warehouseId={}, path={}", warehouseId, path);

            // 查找仓库
            Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(warehouseId);
            if (!warehouseOpt.isPresent()) {
                return ResponseEntity.ok(Result.error("仓库不存在"));
            }

            WarehouseEntity warehouse = warehouseOpt.get();

            // 临时方案：直接使用实际目录名
            // TODO: 需要规范化仓库存储目录的命名规则
            String repoDir = "user-growing_uc-datax-willow";
            String repoPath = Paths.get(gitPathResolver.getStoragePath(), repoDir).toString();

            Path filePath = Paths.get(repoPath, path);
            log.debug("完整文件路径: repoPath={}, path={}, result={}", repoPath, path, filePath.toAbsolutePath());

            // 检查文件是否存在
            if (!Files.exists(filePath)) {
                return ResponseEntity.ok(Result.error("文件不存在: " + path + ", 完整路径: " + filePath.toAbsolutePath()));
            }

            // 检查是否为文件
            if (Files.isDirectory(filePath)) {
                return ResponseEntity.ok(Result.error("指定路径是目录，不是文件"));
            }

            // 读取文件内容
            byte[] bytes = Files.readAllBytes(filePath);
            String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);

            // 获取文件信息
            String fileName = filePath.getFileName().toString();
            String fileType = fileName.contains(".") ?
                fileName.substring(fileName.lastIndexOf(".") + 1) : "";

            FileContentResponse response = FileContentResponse.builder()
                    .path(path)
                    .content(content)
                    .size((long) bytes.length)
                    .fileType(fileType)
                    .isBinary(false)
                    .encoding("UTF-8")
                    .build();

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("获取文件内容失败: warehouseId={}, path={}", warehouseId, path, e);
            return ResponseEntity.ok(Result.error("获取文件失败: " + e.getMessage()));
        }
    }

    /**
     * 保存文件内容
     */
    @PostMapping("/FileContent")
    public ResponseEntity<Result<Boolean>> saveFileContent(
            @RequestBody Map<String, String> requestBody) {

        String id = requestBody.get("id");
        String content = requestBody.get("content");

        log.info("保存文件内容: id={}", id);

        if (id == null || id.trim().isEmpty()) {
            return ResponseEntity.ok(Result.error("文件ID不能为空"));
        }
        if (content == null) {
            return ResponseEntity.ok(Result.error("文件内容不能为空"));
        }

        try {
            // 解析 id: 约定格式 warehouseId:path
            String[] parts = id.split(":", 2);
            if (parts.length != 2) {
                return ResponseEntity.ok(Result.error("文件ID格式不正确，期望格式为 warehouseId:path"));
            }

            String warehouseId = parts[0];
            String path = parts[1];

            Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(warehouseId);
            if (!warehouseOpt.isPresent()) {
                return ResponseEntity.ok(Result.error("仓库不存在"));
            }

            WarehouseEntity warehouse = warehouseOpt.get();

            // 构建文件路径
            String storagePath = gitPathResolver.getStoragePath() + "/" + getRepositoryIdentifier(warehouse.getAddress());
            Path filePath = Paths.get(storagePath, path);

            // 确保父目录存在
            Path parent = filePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            // 写入文件内容（UTF-8）
            Files.write(filePath, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            return ResponseEntity.ok(Result.success(true, "保存成功"));

        } catch (Exception e) {
            log.error("保存文件内容失败: id={}", id, e);
            return ResponseEntity.ok(Result.error("保存失败: " + e.getMessage()));
        }
    }

    /**
     * 手动触发同步
     */
    @PostMapping("/ManualSync")
    public ResponseEntity<Result<String>> manualSync(
            @RequestParam @NotBlank String warehouseId) {

        log.info("手动触发同步: warehouseId={}", warehouseId);

        try {
            // 查找仓库
            Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(warehouseId);
            if (!warehouseOpt.isPresent()) {
                return ResponseEntity.ok(Result.error("仓库不存在"));
            }

            // 检查是否正在同步
            if (warehouseSyncService.isSyncing(warehouseId)) {
                return ResponseEntity.ok(Result.error("仓库正在同步中"));
            }

            // 触发同步
            String syncRecordId = warehouseSyncService.triggerSyncSync(warehouseId,
                    ai.opendw.koalawiki.domain.warehouse.WarehouseSyncTrigger.MANUAL);

            return ResponseEntity.ok(Result.success(syncRecordId, "同步任务已创建"));

        } catch (Exception e) {
            log.error("手动触发同步失败: warehouseId={}", warehouseId, e);
            return ResponseEntity.ok(Result.error("触发同步失败: " + e.getMessage()));
        }
    }

    /**
     * 更新仓库同步配置
     */
    @PostMapping("/UpdateSync")
    public ResponseEntity<Result<Boolean>> updateSync(
            @RequestParam @NotBlank String id,
            @RequestBody Map<String, Object> requestBody) {

        log.info("更新仓库同步配置: id={}", id);

        try {
            Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(id);
            if (!warehouseOpt.isPresent()) {
                return ResponseEntity.ok(Result.error("仓库不存在"));
            }

            WarehouseEntity warehouse = warehouseOpt.get();

            // 目前仅支持启用/禁用自动同步，前端传入 enableSync: boolean
            if (requestBody.containsKey("enableSync")) {
                Object enableSyncObj = requestBody.get("enableSync");
                if (enableSyncObj instanceof Boolean) {
                    warehouse.setEnableSync((Boolean) enableSyncObj);
                } else if (enableSyncObj instanceof String) {
                    warehouse.setEnableSync(Boolean.parseBoolean((String) enableSyncObj));
                }
            }

            warehouseRepository.save(warehouse);
            return ResponseEntity.ok(Result.success(true, "同步配置已更新"));

        } catch (Exception e) {
            log.error("更新仓库同步配置失败: id={}", id, e);
            return ResponseEntity.ok(Result.error("更新同步配置失败: " + e.getMessage()));
        }
    }

    /**
     * 获取仓库同步记录（分页）
     *
     * 对齐前端 /api/Repository/SyncRecords?warehouseId=...&page=1&pageSize=10
     */
    @GetMapping("/SyncRecords")
    public ResponseEntity<Result<Page<SyncRecordDto>>> getSyncRecords(
            @RequestParam @NotBlank String warehouseId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) int pageSize) {

        log.debug("获取仓库同步记录: warehouseId={}, page={}, pageSize={}",
                warehouseId, page, pageSize);

        try {
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<ai.opendw.koalawiki.infra.entity.WarehouseSyncRecordEntity> records =
                    warehouseSyncService.getSyncRecords(warehouseId, pageable);

            Page<SyncRecordDto> dtoPage = records.map(record -> {
                SyncRecordDto dto = new SyncRecordDto();
                dto.setId(record.getId());
                dto.setWarehouseId(record.getWarehouseId());
                dto.setStatus(record.getStatus());
                dto.setTrigger(record.getTrigger());
                dto.setStartTime(record.getStartTime());
                dto.setEndTime(record.getEndTime());
                dto.setFromVersion(record.getFromVersion());
                dto.setToVersion(record.getToVersion());
                dto.setProgress(record.getProgress());
                dto.setAddedFileCount(record.getAddedFileCount() != null ? record.getAddedFileCount() : 0);
                dto.setModifiedFileCount(record.getUpdatedFileCount() != null ? record.getUpdatedFileCount() : 0);
                dto.setDeletedFileCount(record.getDeletedFileCount() != null ? record.getDeletedFileCount() : 0);
                dto.setDuration(record.getDurationMs());
                dto.setErrorMessage(record.getErrorMessage());
                return dto;
            });

            return ResponseEntity.ok(Result.success(dtoPage));

        } catch (Exception e) {
            log.error("获取仓库同步记录失败: warehouseId={}", warehouseId, e);
            return ResponseEntity.ok(Result.error("获取同步记录失败: " + e.getMessage()));
        }
    }

    /**
     * 导出仓库（Markdown Zip）
     */
    @GetMapping("/Export")
    public ResponseEntity<byte[]> exportRepository(
            @RequestParam @NotBlank String id) {

        log.info("导出仓库: id={}", id);

        try {
            Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(id);
            if (!warehouseOpt.isPresent()) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                        .body(("仓库不存在: " + id).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            WarehouseEntity warehouse = warehouseOpt.get();
            String storagePath = gitPathResolver.getStoragePath() + "/" + getRepositoryIdentifier(warehouse.getAddress());
            Path rootPath = Paths.get(storagePath);

            if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                        .body(("仓库文件不存在: " + storagePath).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            byte[] zipBytes = createMarkdownZip(rootPath);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
            String filename = warehouse.getName() != null ? warehouse.getName() : warehouse.getId();
            headers.setContentDispositionFormData("attachment", filename + "-markdown.zip");

            return new ResponseEntity<>(zipBytes, headers, org.springframework.http.HttpStatus.OK);

        } catch (Exception e) {
            log.error("导出仓库失败: id={}", id, e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("导出失败: " + e.getMessage()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    /**
     * 获取单个仓库统计信息
     *
     * @param id 仓库ID
     * @return 仓库统计信息
     * @author zhourui(V33215020)
     * @since 2025/11/15
     */
    @GetMapping("/RepositoryStats")
    public ResponseEntity<Result<WarehouseStatsResponse>> getRepositoryStatsById(
            @RequestParam @NotBlank String id) {

        log.debug("获取仓库统计信息: id={}", id);

        try {
            Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(id);
            if (!warehouseOpt.isPresent()) {
                return ResponseEntity.ok(Result.error("仓库不存在"));
            }

            WarehouseEntity entity = warehouseOpt.get();
            WarehouseStatsResponse stats = buildDetailedStats(entity);

            return ResponseEntity.ok(Result.success(stats));

        } catch (Exception e) {
            log.error("获取仓库统计信息失败: id={}", id, e);
            return ResponseEntity.ok(Result.error("获取仓库统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 构建仓库详细统计信息
     *
     * @param entity 仓库实体
     * @return 详细统计信息
     * @author zhourui(V33215020)
     * @since 2025/11/15
     */
    private WarehouseStatsResponse buildDetailedStats(WarehouseEntity entity) {
        WarehouseStatsResponse.WarehouseStatsResponseBuilder builder =
                WarehouseStatsResponse.builder()
                        .warehouseId(entity.getId())
                        .warehouseName(entity.getName())
                        .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                        .version(entity.getVersion())
                        .viewCount(0L)
                        .starCount(entity.getStars() != null ? entity.getStars().intValue() : 0);

        collectFileStatistics(entity, builder);

        return builder.build();
    }

    /**
     * 收集仓库文件统计信息
     *
     * @param entity 仓库实体
     * @param builder 统计响应构建器
     * @author zhourui(V33215020)
     * @since 2025/11/15
     */
    private void collectFileStatistics(WarehouseEntity entity,
                                       WarehouseStatsResponse.WarehouseStatsResponseBuilder builder) {
        try {
            String storagePath = "/data/koalawiki/git/" + getRepositoryIdentifier(entity.getAddress());
            Path rootPath = Paths.get(storagePath);

            if (Files.exists(rootPath) && Files.isDirectory(rootPath)) {
                FileStatsCollector collector = new FileStatsCollector();
                Files.walkFileTree(rootPath, collector);

                // 获取最后同步时间
                Long lastSyncTime = null;
                try {
                    IWarehouseSyncService.SyncStatusInfo syncStatus =
                        warehouseSyncService.getSyncStatus(entity.getId());
                    if (syncStatus != null && syncStatus.getLastSuccessSyncTime() != null) {
                        lastSyncTime = syncStatus.getLastSuccessSyncTime().getTime();
                    }
                } catch (Exception e) {
                    log.warn("获取同步状态失败: {}", entity.getId(), e);
                }

                builder.totalFiles(collector.getTotalFiles())
                       .documentFiles(collector.getDocumentFiles())
                       .totalSize(collector.getTotalSize())
                       .catalogCount(collector.getDirectoryCount())
                       .documentItemCount(collector.getDocumentFiles())
                       .lastSyncTime(lastSyncTime);
            } else {
                builder.totalFiles(0)
                       .documentFiles(0)
                       .totalSize(0L)
                       .catalogCount(0)
                       .documentItemCount(0)
                       .lastSyncTime(null);
            }
        } catch (IOException ioException) {
            log.warn("收集仓库文件统计失败: {}", entity.getId(), ioException);
            builder.totalFiles(0)
                   .documentFiles(0)
                   .totalSize(0L)
                   .catalogCount(0)
                   .documentItemCount(0)
                   .lastSyncTime(null);
        }
    }

    /**
     * 查找仓库实际存储路径
     * 尝试多种格式以兼容不同版本
     */
    private String findRepositoryPath(String repoUrl) {
        String basePath = gitPathResolver.getStoragePath();
        log.debug("查找仓库路径: repoUrl={}, basePath={}", repoUrl, basePath);

        // 尝试格式1: 新版格式 (platform/owner/repo)
        String path1 = gitPathResolver.getLocalPath(repoUrl);
        log.debug("尝试路径1 (新版): {}", path1);
        if (Files.exists(Paths.get(path1))) {
            log.debug("找到仓库路径: {}", path1);
            return path1;
        }

        // 尝试格式2: 旧版格式 (owner_repo)
        String identifier = getRepositoryIdentifier(repoUrl);
        String path2 = Paths.get(basePath, identifier).toString();
        log.debug("尝试路径2 (旧版): {}", path2);
        if (Files.exists(Paths.get(path2))) {
            log.debug("找到仓库路径: {}", path2);
            return path2;
        }

        // 尝试格式3: 扫描storage目录，查找包含repo名称的目录
        GitRepoInfo info = parseGitUrl(repoUrl);
        log.debug("解析Git URL: info={}", info != null ? info.getRepositoryName() : "null");
        if (info != null && info.getRepositoryName() != null) {
            try (Stream<Path> paths = Files.list(Paths.get(basePath))) {
                String repoName = info.getRepositoryName();
                Optional<Path> found = paths
                    .filter(Files::isDirectory)
                    .filter(p -> {
                        String dirName = p.getFileName().toString();
                        boolean matches = dirName.contains(repoName);
                        log.debug("检查目录: {} contains {} ? {}", dirName, repoName, matches);
                        return matches;
                    })
                    .findFirst();
                if (found.isPresent()) {
                    String foundPath = found.get().toString();
                    log.debug("找到仓库路径 (扫描): {}", foundPath);
                    return foundPath;
                }
            } catch (IOException e) {
                log.warn("Failed to scan storage directory: {}", basePath, e);
            }
        }

        log.warn("未找到仓库路径: repoUrl={}", repoUrl);
        return null;
    }

    /**
     * 获取仓库标识符（用于存储路径）
     */
    private String getRepositoryIdentifier(String url) {
        GitRepoInfo info = parseGitUrl(url);
        if (info != null) {
            return info.getOrganization() + "_" + info.getRepositoryName();
        }
        // 如果解析失败，使用URL的hash值
        return String.valueOf(url.hashCode());
    }

    /**
     * 解析Git URL
     */
    private GitRepoInfo parseGitUrl(String url) {
        try {
            Pattern httpsPattern = Pattern.compile("https?://[^/]+/([^/]+)/([^/]+?)(?:\\.git)?$");
            Pattern sshPattern = Pattern.compile("git@[^:]+:([^/]+)/([^/]+?)(?:\\.git)?$");

            Matcher httpsMatcher = httpsPattern.matcher(url);
            if (httpsMatcher.find()) {
                String owner = httpsMatcher.group(1);
                String repo = httpsMatcher.group(2);
                return new GitRepoInfo(owner, repo);
            }

            Matcher sshMatcher = sshPattern.matcher(url);
            if (sshMatcher.find()) {
                String owner = sshMatcher.group(1);
                String repo = sshMatcher.group(2);
                return new GitRepoInfo(owner, repo);
            }

            return null;

        } catch (Exception e) {
            log.error("解析Git URL失败: {}", url, e);
            return null;
        }
    }

    /**
     * 转换为响应DTO
     */
    private WarehouseResponse convertToResponse(WarehouseEntity entity) {
        WarehouseResponse response = new WarehouseResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setOrganizationName(null); // Entity中没有这个字段
        response.setDescription(entity.getDescription());
        response.setAddress(entity.getAddress());
        response.setBranch(entity.getBranch());
        response.setStatus(entity.getStatus());
        response.setError(entity.getError());
        response.setStars(entity.getStars() != null ? entity.getStars().intValue() : 0);
        response.setForks(0); // Entity中没有forks字段
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(null); // Entity中没有updatedAt字段
        response.setVersion(entity.getVersion());
        response.setIsEmbedded(false); // Entity中没有这个字段
        response.setIsRecommended(false); // Entity中没有这个字段
        return response;
    }

    /**
     * Git仓库信息
     */
    private static class GitRepoInfo {
        private final String organization;
        private final String repositoryName;

        public GitRepoInfo(String organization, String repositoryName) {
            this.organization = organization;
            this.repositoryName = repositoryName;
        }

        public String getOrganization() {
            return organization;
        }

        public String getRepositoryName() {
            return repositoryName;
        }
    }

    /**
     * 创建仅包含文档文件的Zip包
     */
    private byte[] createMarkdownZip(Path rootPath) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
                    if ("target".equalsIgnoreCase(name)
                            || "dist".equalsIgnoreCase(name)
                            || "build".equalsIgnoreCase(name)
                            || "node_modules".equalsIgnoreCase(name)
                            || ".git".equalsIgnoreCase(name)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileName = file.getFileName().toString();
                    String lower = fileName.toLowerCase();

                    boolean isDoc = lower.endsWith(".md")
                            || lower.endsWith(".markdown")
                            || lower.endsWith(".mdx")
                            || lower.endsWith(".txt")
                            || lower.startsWith("readme")
                            || lower.startsWith("license")
                            || lower.startsWith("changelog");
                    if (!isDoc) {
                        return FileVisitResult.CONTINUE;
                    }

                    Path relative = rootPath.relativize(file);
                    java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(relative.toString().replace("\\", "/"));
                    zos.putNextEntry(entry);
                    byte[] bytes = Files.readAllBytes(file);
                    zos.write(bytes);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return baos.toByteArray();
    }

    /**
     * 文件统计收集器
     * 用于遍历目录树并收集统计数据
     *
     * @author zhourui(V33215020)
     * @since 2025/11/15
     */
    private static class FileStatsCollector extends SimpleFileVisitor<Path> {
        private int totalFiles;
        private int documentFiles;
        private long totalSize;
        private int directoryCount;

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            String dirName = dir.getFileName() != null ? dir.getFileName().toString() : "";
            if (".git".equals(dirName) || "node_modules".equals(dirName)
                    || "target".equals(dirName) || "build".equals(dirName)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            directoryCount++;
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            totalFiles++;
            String fileName = file.getFileName().toString().toLowerCase();
            if (fileName.endsWith(".md") || fileName.endsWith(".markdown") || fileName.endsWith(".mdx")) {
                documentFiles++;
            }
            try {
                totalSize += Files.size(file);
            } catch (IOException e) {
                // 忽略单个文件错误
            }
            return FileVisitResult.CONTINUE;
        }

        public int getTotalFiles() {
            return totalFiles;
        }

        public int getDocumentFiles() {
            return documentFiles;
        }

        public long getTotalSize() {
            return totalSize;
        }

        public int getDirectoryCount() {
            return directoryCount;
        }
    }
}

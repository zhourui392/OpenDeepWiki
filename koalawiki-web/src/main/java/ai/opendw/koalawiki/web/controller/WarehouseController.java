package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.app.service.IDocumentCatalogService;
import ai.opendw.koalawiki.core.git.CommitInfo;
import ai.opendw.koalawiki.core.git.GitService;
import ai.opendw.koalawiki.core.service.IWarehouseSyncService;
import ai.opendw.koalawiki.domain.document.DocumentCatalog;
import ai.opendw.koalawiki.domain.warehouse.WarehouseStatus;
import ai.opendw.koalawiki.infra.entity.WarehouseEntity;
import ai.opendw.koalawiki.infra.repository.WarehouseRepository;
import ai.opendw.koalawiki.web.dto.Result;
import ai.opendw.koalawiki.web.dto.warehouse.BranchListResponse;
import ai.opendw.koalawiki.web.dto.warehouse.CustomSubmitWarehouseRequest;
import ai.opendw.koalawiki.web.dto.warehouse.FileContentResponse;
import ai.opendw.koalawiki.web.dto.warehouse.SubmitWarehouseRequest;
import ai.opendw.koalawiki.web.dto.warehouse.WarehouseResponse;
import ai.opendw.koalawiki.web.dto.warehouse.WarehouseStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ai.opendw.koalawiki.domain.ClassifyType;

/**
 * 仓库管理控制器
 * 提供仓库CRUD和Git操作相关的REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/Warehouse")
@RequiredArgsConstructor
@Validated
public class WarehouseController {

    private final WarehouseRepository warehouseRepository;
    private final IWarehouseSyncService warehouseSyncService;
    private final GitService gitService;
    private final IDocumentCatalogService documentCatalogService;

    /**
     * 提交Git仓库
     */
    @PostMapping("/SubmitWarehouse")
    public ResponseEntity<Result<WarehouseResponse>> submitWarehouse(
            @Valid @RequestBody SubmitWarehouseRequest request) {

        log.info("提交仓库: {}", request.getAddress());

        try {
            // 1. 解析仓库信息
            GitRepoInfo repoInfo = parseGitUrl(request.getAddress());
            if (repoInfo == null) {
                return ResponseEntity.ok(Result.error("无法解析仓库地址，请检查URL格式"));
            }

            // 2. 检查仓库是否已存在
            WarehouseEntity existing = warehouseRepository.findByUrl(request.getAddress());
            if (existing != null) {
                return ResponseEntity.ok(Result.error("仓库已存在"));
            }

            // 3. 创建仓库记录
            WarehouseEntity warehouse = new WarehouseEntity();
            warehouse.setId(UUID.randomUUID().toString());
            warehouse.setCreatedAt(new Date());
            warehouse.setName(repoInfo.getRepositoryName());
            warehouse.setUrl(request.getAddress());
            warehouse.setDefaultBranch(request.getBranch() != null ? request.getBranch() : "main");
            warehouse.setStatus(WarehouseStatus.PENDING);
            warehouse.setClassifyType(ClassifyType.DOCUMENTATION);
            warehouse.setUserId("default-user"); // TODO: 从认证信息获取
            warehouse.setIsPublic(true);

            if (request.getGitUserName() != null) {
                warehouse.setAuthUsername(request.getGitUserName());
                warehouse.setAuthPassword(request.getGitPassword());
            }

            // 4. 保存仓库
            warehouse = warehouseRepository.save(warehouse);

            // 5. 异步触发同步
            try {
                warehouseSyncService.triggerSync(warehouse.getId(), false);
            } catch (Exception e) {
                log.warn("触发同步失败，但仓库已创建: {}", warehouse.getId(), e);
            }

            // 6. 返回结果
            WarehouseResponse response = convertToResponse(warehouse);
            return ResponseEntity.ok(Result.success(response, "仓库提交成功"));

        } catch (Exception e) {
            log.error("提交仓库失败: {}", request.getAddress(), e);
            return ResponseEntity.ok(Result.error("提交失败: " + e.getMessage()));
        }
    }

    /**
     * 自定义提交仓库
     */
    @PostMapping("/CustomSubmitWarehouse")
    public ResponseEntity<Result<WarehouseResponse>> customSubmitWarehouse(
            @Valid @RequestBody CustomSubmitWarehouseRequest request) {

        log.info("自定义提交仓库: {}/{}", request.getOrganization(), request.getRepositoryName());

        try {
            // 检查仓库是否已存在
            WarehouseEntity existing = warehouseRepository.findByUrl(request.getAddress());
            if (existing != null) {
                return ResponseEntity.ok(Result.error("仓库已存在"));
            }

            WarehouseEntity warehouse = new WarehouseEntity();
            warehouse.setId(UUID.randomUUID().toString());
            warehouse.setCreatedAt(new Date());
            warehouse.setName(request.getRepositoryName());
            warehouse.setUrl(request.getAddress());
            warehouse.setDefaultBranch(request.getBranch() != null ? request.getBranch() : "main");
            warehouse.setStatus(WarehouseStatus.PENDING);
            warehouse.setClassifyType(ClassifyType.DOCUMENTATION);
            warehouse.setUserId("default-user"); // TODO: 从认证信息获取
            warehouse.setIsPublic(true);

            if (request.getGitUserName() != null) {
                warehouse.setAuthUsername(request.getGitUserName());
                warehouse.setAuthPassword(request.getGitPassword());
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
            log.error("自定义提交仓库失败", e);
            return ResponseEntity.ok(Result.error("创建失败: " + e.getMessage()));
        }
    }

    /**
     * 上传文件并提交仓库
     */
    @PostMapping("/UploadAndSubmitWarehouse")
    public ResponseEntity<Result<WarehouseResponse>> uploadAndSubmitWarehouse(
            @RequestParam String organization,
            @RequestParam String repositoryName,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String gitUserName,
            @RequestParam(required = false) String gitPassword,
            @RequestParam(required = false) String email) {

        log.info("上传文件并提交仓库: {}/{}", organization, repositoryName);

        try {
            // TODO: 实现文件上传和解压逻辑
            // 1. 保存上传文件
            // 2. 解压文件
            // 3. 创建仓库记录
            // 4. 导入文件到仓库

            return ResponseEntity.ok(Result.error("文件上传功能待实现"));

        } catch (Exception e) {
            log.error("上传并提交仓库失败", e);
            return ResponseEntity.ok(Result.error("上传失败: " + e.getMessage()));
        }
    }

    /**
     * 获取仓库列表（分页）
     */
    @GetMapping("/WarehouseList")
    public ResponseEntity<Result<Page<WarehouseResponse>>> getWarehouseList(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "12") @Min(1) int pageSize,
            @RequestParam(required = false) String keyword) {

        log.debug("获取仓库列表: page={}, pageSize={}, keyword={}", page, pageSize, keyword);

        try {
            Pageable pageable = PageRequest.of(page - 1, pageSize);
            Page<WarehouseEntity> warehouses;

            if (keyword != null && !keyword.trim().isEmpty()) {
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
     * 获取最后一个仓库
     */
    @GetMapping("/LastWarehouse")
    public ResponseEntity<Result<WarehouseResponse>> getLastWarehouse() {
        log.debug("获取最后一个仓库");

        try {
            // 按创建时间倒序取第一个
            Pageable pageable = PageRequest.of(0, 1, org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
            Page<WarehouseEntity> warehouses = warehouseRepository.findAll(pageable);

            if (warehouses.hasContent()) {
                WarehouseResponse response = convertToResponse(warehouses.getContent().get(0));
                return ResponseEntity.ok(Result.success(response));
            } else {
                return ResponseEntity.ok(Result.error("没有找到仓库"));
            }

        } catch (Exception e) {
            log.error("获取最后一个仓库失败", e);
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取Git变更日志（最近提交）
     */
    @GetMapping("/ChangeLog")
    public ResponseEntity<Result<List<ai.opendw.koalawiki.core.document.processors.ChangeLogProcessor.ChangeLogEntry>>> getChangeLog(
            @RequestParam @NotBlank String address,
            @RequestParam(required = false, defaultValue = "50") @Min(1) int limit) {

        log.debug("获取变更日志: address={}, limit={}", address, limit);

        try {
            // 解析本地仓库路径
            String repositoryIdentifier = getRepositoryIdentifier(address);
            String localPath = "/data/koalawiki/git/" + repositoryIdentifier;

            // 获取提交历史
            List<CommitInfo> commits = gitService.getCommitHistory(localPath, limit);

            // 转换为简化的变更日志条目
            List<ai.opendw.koalawiki.core.document.processors.ChangeLogProcessor.ChangeLogEntry> entries =
                    new ArrayList<>();

            java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd");

            for (CommitInfo commit : commits) {
                ai.opendw.koalawiki.core.document.processors.ChangeLogProcessor.ChangeLogEntry entry =
                        new ai.opendw.koalawiki.core.document.processors.ChangeLogProcessor.ChangeLogEntry();
                entry.setCommitId(commit.getCommitId());
                entry.setAuthor(commit.getAuthor());
                entry.setCommitTime(commit.getCommitTime());
                entry.setDate(df.format(commit.getCommitTime()));
                entry.setMessage(commit.getMessage());
                entry.setType("commit");
                entries.add(entry);
            }

            return ResponseEntity.ok(Result.success(entries));

        } catch (Exception e) {
            log.error("获取变更日志失败: address={}", address, e);
            return ResponseEntity.ok(Result.error("获取变更日志失败: " + e.getMessage()));
        }
    }

    /**
     * 获取Git分支列表
     */
    @GetMapping("/BranchList")
    public ResponseEntity<Result<BranchListResponse>> getBranchList(
            @RequestParam String address,
            @RequestParam(required = false) String gitUserName,
            @RequestParam(required = false) String gitPassword) {

        log.debug("获取分支列表: {}", address);

        try {
            // TODO: 实现真实的Git分支获取逻辑
            // 目前返回默认分支列表
            List<String> branches = Arrays.asList("main", "master", "develop");
            BranchListResponse response = BranchListResponse.builder()
                    .branches(branches)
                    .defaultBranch("main")
                    .build();

            return ResponseEntity.ok(Result.success(response));

        } catch (Exception e) {
            log.error("获取分支列表失败: {}", address, e);
            return ResponseEntity.ok(Result.error("获取分支失败: " + e.getMessage()));
        }
    }

    /**
     * 获取Git分支（别名）
     */
    @GetMapping("/GetGitBranches")
    public ResponseEntity<Result<BranchListResponse>> getGitBranches(
            @RequestParam String address,
            @RequestParam(required = false) String gitUserName,
            @RequestParam(required = false) String gitPassword) {
        return getBranchList(address, gitUserName, gitPassword);
    }

    /**
     * 获取文件内容
     */
    @GetMapping("/FileContent")
    public ResponseEntity<Result<FileContentResponse>> getFileContent(
            @RequestParam String warehouseId,
            @RequestParam String path) {

        log.debug("获取文件内容: warehouseId={}, path={}", warehouseId, path);

        try {
            // 查找仓库
            Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(warehouseId);
            if (!warehouseOpt.isPresent()) {
                return ResponseEntity.ok(Result.error("仓库不存在"));
            }

            WarehouseEntity warehouse = warehouseOpt.get();

            // 构建文件路径 - 使用Git存储路径
            String storagePath = "/data/koalawiki/git/" + getRepositoryIdentifier(warehouse.getUrl());
            java.nio.file.Path filePath = java.nio.file.Paths.get(storagePath, path);

            // 检查文件是否存在
            if (!java.nio.file.Files.exists(filePath)) {
                return ResponseEntity.ok(Result.error("文件不存在: " + path));
            }

            // 检查是否为文件
            if (java.nio.file.Files.isDirectory(filePath)) {
                return ResponseEntity.ok(Result.error("指定路径是目录，不是文件"));
            }

            // 读取文件内容
            byte[] bytes = java.nio.file.Files.readAllBytes(filePath);
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
     * 按行获取文件内容
     */
    @GetMapping("/filecontentLine")
    public ResponseEntity<Result<List<String>>> getFileContentByLine(
            @RequestParam @NotBlank String warehouseId,
            @RequestParam @NotBlank String path) {

        log.debug("按行获取文件内容: warehouseId={}, path={}", warehouseId, path);

        try {
            Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(warehouseId);
            if (!warehouseOpt.isPresent()) {
                return ResponseEntity.ok(Result.error("仓库不存在"));
            }

            WarehouseEntity warehouse = warehouseOpt.get();
            String storagePath = "/data/koalawiki/git/" + getRepositoryIdentifier(warehouse.getUrl());
            Path filePath = Paths.get(storagePath, path);

            if (!Files.exists(filePath)) {
                return ResponseEntity.ok(Result.error("文件不存在: " + path));
            }
            if (Files.isDirectory(filePath)) {
                return ResponseEntity.ok(Result.error("指定路径是目录，不是文件"));
            }

            List<String> lines = Files.readAllLines(filePath, java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.ok(Result.success(lines));

        } catch (Exception e) {
            log.error("按行获取文件内容失败: warehouseId={}, path={}", warehouseId, path, e);
            return ResponseEntity.ok(Result.error("获取文件失败: " + e.getMessage()));
        }
    }

    /**
     * 导出仓库Markdown压缩包
     */
    @PostMapping("/ExportMarkdownZip")
    public ResponseEntity<byte[]> exportMarkdownZip(
            @RequestParam @NotBlank String warehouseId) {

        log.info("导出Markdown Zip: warehouseId={}", warehouseId);

        try {
            Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(warehouseId);
            if (!warehouseOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(("仓库不存在: " + warehouseId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            WarehouseEntity warehouse = warehouseOpt.get();
            String storagePath = "/data/koalawiki/git/" + getRepositoryIdentifier(warehouse.getUrl());
            Path rootPath = Paths.get(storagePath);

            if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(("仓库文件不存在: " + storagePath).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            byte[] zipBytes = createMarkdownZip(rootPath);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String filename = warehouse.getName() != null ? warehouse.getName() : warehouse.getId();
            headers.setContentDispositionFormData("attachment", filename + "-markdown.zip");

            return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("导出Markdown Zip失败: warehouseId={}", warehouseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("导出失败: " + e.getMessage()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    /**
     * 获取仓库统计信息汇总
     *
     * @return 所有仓库的统计信息列表
     * @author zhourui(V33215020)
     * @since 2025/11/15
     */
    @GetMapping("/Stats")
    public ResponseEntity<Result<List<WarehouseStatsResponse>>> getWarehouseStats() {

        log.debug("获取所有仓库统计信息");

        try {
            List<WarehouseEntity> entities = warehouseRepository.findAll();
            List<WarehouseStatsResponse> statsList = new ArrayList<>();

            for (WarehouseEntity entity : entities) {
                WarehouseStatsResponse stats = buildWarehouseStats(entity);
                statsList.add(stats);
            }

            return ResponseEntity.ok(Result.success(statsList));

        } catch (Exception e) {
            log.error("获取仓库统计信息失败", e);
            return ResponseEntity.ok(Result.error("获取仓库统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 构建单个仓库的统计信息
     *
     * @param entity 仓库实体
     * @return 仓库统计响应
     * @author zhourui(V33215020)
     * @since 2025/11/15
     */
    private WarehouseStatsResponse buildWarehouseStats(WarehouseEntity entity) {
        WarehouseStatsResponse.WarehouseStatsResponseBuilder builder =
                WarehouseStatsResponse.builder()
                        .warehouseId(entity.getId())
                        .warehouseName(entity.getName())
                        .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                        .version(entity.getVersion())
                        .viewCount(0L)
                        .starCount(entity.getStarCount() != null ? entity.getStarCount().intValue() : 0);

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
            String storagePath = "/data/koalawiki/git/" + getRepositoryIdentifier(entity.getUrl());
            Path rootPath = Paths.get(storagePath);

            if (Files.exists(rootPath) && Files.isDirectory(rootPath)) {
                FileStatsCounter counter = new FileStatsCounter();
                Files.walkFileTree(rootPath, counter);

                builder.totalFiles(counter.getTotalFiles())
                       .documentFiles(counter.getDocumentFiles())
                       .totalSize(counter.getTotalSize())
                       .catalogCount(counter.getDirectoryCount())
                       .documentItemCount(counter.getDocumentFiles());
            } else {
                builder.totalFiles(0)
                       .documentFiles(0)
                       .totalSize(0L)
                       .catalogCount(0)
                       .documentItemCount(0);
            }
        } catch (IOException ioException) {
            log.warn("统计仓库文件信息失败: {}", entity.getId(), ioException);
            builder.totalFiles(0)
                   .documentFiles(0)
                   .totalSize(0L)
                   .catalogCount(0)
                   .documentItemCount(0);
        }
    }

    /**
     * 解析Git URL
     */
    private GitRepoInfo parseGitUrl(String url) {
        try {
            // 支持的格式:
            // https://github.com/owner/repo.git
            // https://github.com/owner/repo
            // git@github.com:owner/repo.git

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
     * 创建仅包含文档文件的Zip包
     */
    private byte[] createMarkdownZip(Path rootPath) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
                    // 跳过构建/依赖目录
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
     * 转换为响应DTO
     */
    private WarehouseResponse convertToResponse(WarehouseEntity entity) {
        WarehouseResponse response = new WarehouseResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setOrganizationName(null); // Entity中没有这个字段
        response.setDescription(entity.getDescription());
        response.setAddress(entity.getUrl());
        response.setBranch(entity.getDefaultBranch());
        response.setStatus(entity.getStatus());
        response.setError(entity.getError());
        response.setStars(entity.getStarCount() != null ? entity.getStarCount().intValue() : 0);
        response.setForks(0); // Entity中没有forks字段
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(null); // Entity中没有updatedAt字段
        response.setVersion(entity.getVersion());
        response.setIsEmbedded(false); // Entity中没有这个字段
        response.setIsRecommended(false); // Entity中没有这个字段
        response.setUserId(entity.getUserId());
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
     * 文件统计计数器，用于遍历仓库目录时收集统计信息
     */
    private static class FileStatsCounter extends SimpleFileVisitor<Path> {

        private int totalFiles;
        private int documentFiles;
        private long totalSize;
        private int directoryCount;

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            directoryCount++;
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            totalFiles++;
            if (file.getFileName().toString().toLowerCase().endsWith(".md")) {
                documentFiles++;
            }
            try {
                totalSize += Files.size(file);
            } catch (IOException e) {
                // ignore single file error
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

    /**
     * 获取文档树结构
     * 返回仓库的文档目录树，用于前端展示
     *
     * @param warehouseId 仓库ID
     * @return 文档树结构
     * @author zhourui(V33215020)
     * @since 2025/11/15
     */
    @GetMapping("/GetDocumentTree")
    public ResponseEntity<Result<DocumentTreeNode>> getDocumentTree(
            @RequestParam @NotBlank String warehouseId) {

        log.debug("获取文档树: warehouseId={}", warehouseId);

        try {
            Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(warehouseId);
            if (!warehouseOpt.isPresent()) {
                return ResponseEntity.ok(Result.error("仓库不存在"));
            }

            DocumentCatalog catalogTree = documentCatalogService.getCatalogTree(warehouseId);

            if (catalogTree == null) {
                log.warn("仓库没有文档目录: {}", warehouseId);
                DocumentTreeNode emptyTree = buildEmptyDocumentTree(warehouseId);
                return ResponseEntity.ok(Result.success(emptyTree));
            }

            DocumentTreeNode treeNode = convertCatalogToTreeNode(catalogTree);
            return ResponseEntity.ok(Result.success(treeNode));

        } catch (Exception e) {
            log.error("获取文档树失败: warehouseId={}", warehouseId, e);
            return ResponseEntity.ok(Result.error("获取文档树失败: " + e.getMessage()));
        }
    }

    /**
     * 获取思维导图数据
     * 返回适用于思维导图组件的JSON数据格式
     *
     * @param warehouseId 仓库ID
     * @return 思维导图数据
     * @author zhourui(V33215020)
     * @since 2025/11/15
     */
    @GetMapping("/minimap")
    public ResponseEntity<Result<MindMapNode>> getMindMap(
            @RequestParam @NotBlank String warehouseId) {

        log.debug("获取思维导图: warehouseId={}", warehouseId);

        try {
            Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(warehouseId);
            if (!warehouseOpt.isPresent()) {
                return ResponseEntity.ok(Result.error("仓库不存在"));
            }

            WarehouseEntity warehouse = warehouseOpt.get();
            DocumentCatalog catalogTree = documentCatalogService.getCatalogTree(warehouseId);

            MindMapNode rootNode;
            if (catalogTree == null) {
                rootNode = buildEmptyMindMapNode(warehouse);
            } else {
                rootNode = convertCatalogToMindMapNode(catalogTree, warehouse);
            }

            return ResponseEntity.ok(Result.success(rootNode));

        } catch (Exception e) {
            log.error("获取思维导图失败: warehouseId={}", warehouseId, e);
            return ResponseEntity.ok(Result.error("获取思维导图失败: " + e.getMessage()));
        }
    }

    /**
     * 构建空文档树
     */
    private DocumentTreeNode buildEmptyDocumentTree(String warehouseId) {
        DocumentTreeNode node = new DocumentTreeNode();
        node.setId(warehouseId);
        node.setName("Root");
        node.setType("directory");
        node.setChildren(new ArrayList<>());
        return node;
    }

    /**
     * 转换DocumentCatalog为DocumentTreeNode
     */
    private DocumentTreeNode convertCatalogToTreeNode(DocumentCatalog catalog) {
        DocumentTreeNode node = new DocumentTreeNode();
        node.setId(catalog.getId());
        node.setName(catalog.getName());
        node.setUrl(catalog.getUrl());
        node.setDescription(catalog.getDescription());
        node.setType("catalog");
        node.setOrder(catalog.getOrder());
        node.setIsCompleted(catalog.getIsCompleted());

        if (catalog.getI18nTranslations() != null && !catalog.getI18nTranslations().isEmpty()) {
            node.setChildren(new ArrayList<>());
        }

        return node;
    }

    /**
     * 构建空思维导图节点
     */
    private MindMapNode buildEmptyMindMapNode(WarehouseEntity warehouse) {
        MindMapNode node = new MindMapNode();
        node.setId(warehouse.getId());
        node.setTopic(warehouse.getName() != null ? warehouse.getName() : "未命名仓库");
        node.setDirection("right");
        node.setExpanded(true);
        node.setChildren(new ArrayList<>());
        return node;
    }

    /**
     * 转换DocumentCatalog为MindMapNode
     */
    private MindMapNode convertCatalogToMindMapNode(DocumentCatalog catalog, WarehouseEntity warehouse) {
        MindMapNode rootNode = new MindMapNode();
        rootNode.setId(warehouse.getId());
        rootNode.setTopic(warehouse.getName() != null ? warehouse.getName() : "未命名仓库");
        rootNode.setDirection("right");
        rootNode.setExpanded(true);

        List<MindMapNode> children = new ArrayList<>();
        MindMapNode catalogNode = buildMindMapNodeFromCatalog(catalog);
        if (catalogNode != null) {
            children.add(catalogNode);
        }
        rootNode.setChildren(children);

        return rootNode;
    }

    /**
     * 从DocumentCatalog构建MindMapNode
     */
    private MindMapNode buildMindMapNodeFromCatalog(DocumentCatalog catalog) {
        if (catalog == null) {
            return null;
        }

        MindMapNode node = new MindMapNode();
        node.setId(catalog.getId());
        node.setTopic(catalog.getName());
        node.setExpanded(false);
        node.setChildren(new ArrayList<>());

        return node;
    }

    /**
     * 文档树节点
     */
    public static class DocumentTreeNode {
        private String id;
        private String name;
        private String url;
        private String description;
        private String type;
        private Integer order;
        private Boolean isCompleted;
        private List<DocumentTreeNode> children;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Integer getOrder() { return order; }
        public void setOrder(Integer order) { this.order = order; }
        public Boolean getIsCompleted() { return isCompleted; }
        public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; }
        public List<DocumentTreeNode> getChildren() { return children; }
        public void setChildren(List<DocumentTreeNode> children) { this.children = children; }
    }

    /**
     * 思维导图节点
     */
    public static class MindMapNode {
        private String id;
        private String topic;
        private String direction;
        private Boolean expanded;
        private List<MindMapNode> children;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
        public Boolean getExpanded() { return expanded; }
        public void setExpanded(Boolean expanded) { this.expanded = expanded; }
        public List<MindMapNode> getChildren() { return children; }
        public void setChildren(List<MindMapNode> children) { this.children = children; }
    }

    /**
     * 批量操作仓库
     * 支持批量删除、启用、禁用等操作
     *
     * @param request 批量操作请求
     * @return 操作结果
     * @author zhourui(V33215020)
     * @since 2025/11/15
     */
    @PostMapping("/BatchOperate")
    public ResponseEntity<Result<BatchOperateResponse>> batchOperate(
            @Valid @RequestBody BatchOperateRequest request) {

        log.info("批量操作仓库: operation={}, count={}", request.getOperation(),
                request.getWarehouseIds() != null ? request.getWarehouseIds().size() : 0);

        try {
            if (request.getWarehouseIds() == null || request.getWarehouseIds().isEmpty()) {
                return ResponseEntity.ok(Result.error("仓库ID列表不能为空"));
            }

            int successCount = 0;
            int failedCount = 0;
            List<String> failedIds = new ArrayList<>();

            for (String warehouseId : request.getWarehouseIds()) {
                try {
                    boolean success = executeBatchOperation(warehouseId, request.getOperation());
                    if (success) {
                        successCount++;
                    } else {
                        failedCount++;
                        failedIds.add(warehouseId);
                    }
                } catch (Exception e) {
                    log.error("批量操作单个仓库失败: warehouseId={}", warehouseId, e);
                    failedCount++;
                    failedIds.add(warehouseId);
                }
            }

            BatchOperateResponse response = new BatchOperateResponse();
            response.setTotalCount(request.getWarehouseIds().size());
            response.setSuccessCount(successCount);
            response.setFailedCount(failedCount);
            response.setFailedIds(failedIds);

            String message = String.format("批量操作完成: 成功 %d, 失败 %d", successCount, failedCount);
            return ResponseEntity.ok(Result.success(response, message));

        } catch (Exception e) {
            log.error("批量操作仓库失败", e);
            return ResponseEntity.ok(Result.error("批量操作失败: " + e.getMessage()));
        }
    }

    /**
     * 执行单个仓库的批量操作
     */
    private boolean executeBatchOperation(String warehouseId, String operation) {
        Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(warehouseId);
        if (!warehouseOpt.isPresent()) {
            return false;
        }

        WarehouseEntity warehouse = warehouseOpt.get();

        switch (operation.toLowerCase()) {
            case "delete":
                warehouseRepository.delete(warehouse);
                return true;

            case "enable":
                warehouse.setStatus(WarehouseStatus.READY);
                warehouseRepository.save(warehouse);
                return true;

            case "disable":
                warehouse.setStatus(WarehouseStatus.CANCELED);
                warehouseRepository.save(warehouse);
                return true;

            case "sync":
                try {
                    warehouseSyncService.triggerSync(warehouseId, false);
                    return true;
                } catch (Exception e) {
                    log.error("触发同步失败: warehouseId={}", warehouseId, e);
                    return false;
                }

            default:
                log.warn("不支持的批量操作: {}", operation);
                return false;
        }
    }

    /**
     * 批量操作请求
     */
    public static class BatchOperateRequest {
        private List<String> warehouseIds;
        private String operation;

        public List<String> getWarehouseIds() { return warehouseIds; }
        public void setWarehouseIds(List<String> warehouseIds) { this.warehouseIds = warehouseIds; }
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
    }

    /**
     * 批量操作响应
     */
    public static class BatchOperateResponse {
        private int totalCount;
        private int successCount;
        private int failedCount;
        private List<String> failedIds;

        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
        public List<String> getFailedIds() { return failedIds; }
        public void setFailedIds(List<String> failedIds) { this.failedIds = failedIds; }
    }
}

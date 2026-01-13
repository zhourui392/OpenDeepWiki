package ai.opendw.koalawiki.web.controller;

import ai.opendw.koalawiki.core.git.CommitInfo;
import ai.opendw.koalawiki.core.git.GitService;
import ai.opendw.koalawiki.core.service.IWarehouseSyncService;
import ai.opendw.koalawiki.domain.warehouse.WarehouseStatus;
import ai.opendw.koalawiki.infra.entity.WarehouseEntity;
import ai.opendw.koalawiki.infra.repository.WarehouseRepository;
import ai.opendw.koalawiki.web.dto.Result;
import ai.opendw.koalawiki.web.dto.warehouse.BranchListResponse;
import ai.opendw.koalawiki.web.dto.warehouse.CustomSubmitWarehouseRequest;
import ai.opendw.koalawiki.web.dto.warehouse.FileContentResponse;
import ai.opendw.koalawiki.web.dto.warehouse.SubmitWarehouseRequest;
import ai.opendw.koalawiki.web.dto.warehouse.WarehouseListResponse;
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
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
@Validated
public class WarehouseController {

    private final WarehouseRepository warehouseRepository;
    private final IWarehouseSyncService warehouseSyncService;
    private final GitService gitService;
    private final ai.opendw.koalawiki.core.git.GitPathResolver gitPathResolver;

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
            WarehouseEntity existing = warehouseRepository.findByAddress(request.getAddress());
            if (existing != null) {
                return ResponseEntity.ok(Result.error("仓库已存在"));
            }

            // 3. 创建仓库记录
            WarehouseEntity warehouse = new WarehouseEntity();
            warehouse.setId(UUID.randomUUID().toString());
            warehouse.setCreatedAt(new Date());
            warehouse.setName(repoInfo.getRepositoryName());
            warehouse.setAddress(request.getAddress());
            warehouse.setBranch(request.getBranch() != null ? request.getBranch() : "master");
            warehouse.setStatus(WarehouseStatus.PENDING);
            warehouse.setClassify(ClassifyType.DOCUMENTATION);
            warehouse.setUserId("default-admin-uuid-0001"); // TODO: 从认证信息获取

            if (request.getGitUserName() != null) {
                warehouse.setGitUserName(request.getGitUserName());
                warehouse.setGitPassword(request.getGitPassword());
            }

            // 5. 保存仓库
            warehouse = warehouseRepository.save(warehouse);

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
            WarehouseEntity existing = warehouseRepository.findByAddress(request.getAddress());
            if (existing != null) {
                return ResponseEntity.ok(Result.error("仓库已存在"));
            }

            WarehouseEntity warehouse = new WarehouseEntity();
            warehouse.setId(UUID.randomUUID().toString());
            warehouse.setCreatedAt(new Date());
            warehouse.setName(request.getRepositoryName());
            warehouse.setAddress(request.getAddress());
            warehouse.setBranch(request.getBranch() != null ? request.getBranch() : "master");
            warehouse.setStatus(WarehouseStatus.PENDING);
            warehouse.setClassify(ClassifyType.DOCUMENTATION);
            warehouse.setUserId("default-admin-uuid-0001"); // TODO: 从认证信息获取

            if (request.getGitUserName() != null) {
                warehouse.setGitUserName(request.getGitUserName());
                warehouse.setGitPassword(request.getGitPassword());
            }

            warehouse = warehouseRepository.save(warehouse);

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
    @GetMapping("/list")
    public ResponseEntity<Result<WarehouseListResponse>> getWarehouseList(
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

            List<WarehouseResponse> items = warehouses.getContent().stream()
                    .map(this::convertToResponse)
                    .collect(java.util.stream.Collectors.toList());

            WarehouseListResponse response = WarehouseListResponse.builder()
                            .items(items)
                            .total((int) warehouses.getTotalElements())
                            .page(page)
                            .pageSize(pageSize)
                            .totalPages(warehouses.getTotalPages())
                            .build();

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
            String storagePath = gitPathResolver.getStoragePath() + "/" + getRepositoryIdentifier(warehouse.getAddress());
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
            String storagePath = gitPathResolver.getStoragePath() + "/" + getRepositoryIdentifier(warehouse.getAddress());
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
            String storagePath = gitPathResolver.getStoragePath() + "/" + getRepositoryIdentifier(warehouse.getAddress());
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
            // https://code.example.com/group/subgroup/repo.git (多级路径)
            // git@github.com:owner/repo.git

            // HTTPS格式: 支持多级路径，取最后一个路径段作为仓库名，之前的所有路径作为组织名
            Pattern httpsPattern = Pattern.compile("https?://[^/]+/(.+?)/([^/]+?)(?:\\.git)?$");
            Pattern sshPattern = Pattern.compile("git@[^:]+:(.+?)/([^/]+?)(?:\\.git)?$");

            Matcher httpsMatcher = httpsPattern.matcher(url);
            if (httpsMatcher.find()) {
                String owner = httpsMatcher.group(1);  // 可能包含多级路径，如 "group/subgroup"
                String repo = httpsMatcher.group(2);
                return new GitRepoInfo(owner, repo);
            }

            Matcher sshMatcher = sshPattern.matcher(url);
            if (sshMatcher.find()) {
                String owner = sshMatcher.group(1);  // 可能包含多级路径
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
     * 删除单个仓库
     *
     * @param warehouseId 仓库ID
     * @return 删除结果
     * @author zhourui(V33215020)
     * @since 2025/11/21
     */
    @PostMapping("/delete")
    public ResponseEntity<Result<Void>> deleteWarehouse(@RequestParam @NotBlank String warehouseId) {
        log.info("删除仓库: {}", warehouseId);

        try {
            Optional<WarehouseEntity> warehouseOpt = warehouseRepository.findById(warehouseId);
            if (!warehouseOpt.isPresent()) {
                return ResponseEntity.ok(Result.error("仓库不存在"));
            }

            WarehouseEntity warehouse = warehouseOpt.get();
            warehouseRepository.delete(warehouse);

            log.info("仓库删除成功: {}", warehouseId);
            return ResponseEntity.ok(Result.success(null, "仓库删除成功"));

        } catch (Exception e) {
            log.error("删除仓库失败: warehouseId={}", warehouseId, e);
            return ResponseEntity.ok(Result.error("删除失败: " + e.getMessage()));
        }
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

# O2O 微服务文档管理系统技术方案

> 基于 OpenDeepWiki 的扩展设计，支持 50+ Spring Boot + Dubbo 微服务的文档管理

---

## 1. 背景与目标

### 1.1 现状分析

当前 OpenDeepWiki 系统具备以下能力：
- 单仓库 Git 集成与同步
- 服务文档库配置（`ServiceDocumentLibrary`）
- Dubbo/Feign 依赖分析（`ServiceDependencyAnalyzer`）
- 业务流程追踪与 Mermaid 图生成
- AI 文档生成（Claude/Codex Agent）
- MCP Server 接口

### 1.2 目标场景

| 场景 | 用户 | 需求 |
|------|------|------|
| 技术方案设计 | AI Agent | 快速检索相关服务、接口、依赖关系，生成集成方案 |
| 新人上手 | 开发人员 | 了解服务职责边界、核心链路、接入方式 |
| 影响评估 | 架构师 | 修改某服务时，评估上下游影响范围 |
| 故障排查 | 运维人员 | 快速定位服务依赖链路、查看接口规范 |
| 知识沉淀 | 团队 | 统一管理 50+ 服务的技术文档 |

### 1.3 设计原则

1. **渐进式扩展**：基于现有架构扩展，不破坏已有功能
2. **领域驱动**：按 O2O 业务领域组织服务和文档
3. **AI 优先**：文档结构优化适配 AI 上下文消费
4. **可扩展性**：支持未来接入更多服务和文档类型

---

## 2. 系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           用户层 (Users Layer)                            │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │  开发人员    │  │   架构师    │  │  AI Agent   │  │    运维     │    │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘    │
└─────────┼────────────────┼────────────────┼────────────────┼────────────┘
          │                │                │                │
          ▼                ▼                ▼                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          接入层 (Access Layer)                           │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐         │
│  │   Vue 前端       │  │   REST API      │  │   MCP Server    │         │
│  │  (人类友好UI)    │  │  (标准HTTP)     │  │  (AI工具集成)   │         │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘         │
└───────────┼────────────────────┼────────────────────┼───────────────────┘
            │                    │                    │
            ▼                    ▼                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        应用服务层 (Application Layer)                     │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌───────────────────┐  ┌───────────────────┐  ┌───────────────────┐   │
│  │ ClusterAppService │  │ DocumentAppService│  │ SearchAppService  │   │
│  │   (集群管理)       │  │   (文档编排)       │  │   (搜索服务)       │   │
│  └─────────┬─────────┘  └─────────┬─────────┘  └─────────┬─────────┘   │
│            │                      │                      │              │
│  ┌─────────┴──────────────────────┴──────────────────────┴─────────┐   │
│  │                        AI Context Builder                        │   │
│  │                    (分层上下文构建器)                              │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
            │                      │                      │
            ▼                      ▼                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         领域服务层 (Domain Layer)                         │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐            │
│  │ ServiceCluster │  │ BusinessDomain │  │ DubboInterface │            │
│  │   (服务集群)    │  │   (业务领域)    │  │  (接口注册)    │            │
│  └───────┬────────┘  └───────┬────────┘  └───────┬────────┘            │
│          │                   │                   │                      │
│  ┌───────┴───────────────────┴───────────────────┴───────┐             │
│  │               ServiceDependencyGraph                   │             │
│  │                  (全局依赖图)                           │             │
│  └────────────────────────────────────────────────────────┘             │
└─────────────────────────────────────────────────────────────────────────┘
            │                      │                      │
            ▼                      ▼                      ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       基础设施层 (Infrastructure Layer)                   │
├─────────────────────────────────────────────────────────────────────────┤
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐            │
│  │  JPA 持久化     │  │   Git 集成     │  │   AI Agent     │            │
│  │  (MySQL/PG)    │  │   (JGit)       │  │ (Claude/Codex) │            │
│  └────────────────┘  └────────────────┘  └────────────────┘            │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 模块划分

```
koalawiki-domain/          # 领域模型层（扩展）
├── cluster/               # [新增] 服务集群聚合
├── domain/                # [新增] 业务领域聚合
├── dubbo/                 # [新增] Dubbo接口聚合
├── ai/                    # [现有] AI文档聚合
├── warehouse/             # [现有] 仓库聚合
└── user/                  # [现有] 用户聚合

koalawiki-core/            # 核心服务层（扩展）
├── analysis/              # [现有] 代码分析
│   ├── dubbo/             # [新增] Dubbo专项分析
│   └── topology/          # [新增] 拓扑生成
├── ai/                    # [现有] AI Agent
│   └── context/           # [新增] 上下文构建器
└── search/                # [新增] 搜索引擎

koalawiki-app/             # 应用服务层（扩展）
├── cluster/               # [新增] 集群管理应用服务
├── domain/                # [新增] 领域管理应用服务
└── service/ai/            # [现有] AI文档服务

koalawiki-web/             # 接口层（扩展）
├── controller/cluster/    # [新增] 集群API
├── controller/domain/     # [新增] 领域API
└── controller/            # [现有] 其他API

mcp-server/                # MCP服务（扩展）
└── document_mcp_server.py # [扩展] 新增工具
```

---

## 3. 领域模型设计

### 3.1 服务集群聚合（ServiceCluster）

```java
/**
 * 服务集群聚合根
 * 管理多个仓库形成的微服务集群
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class ServiceCluster {

    /**
     * 集群ID
     */
    private String id;

    /**
     * 集群名称（如：O2O核心交易集群）
     */
    private String name;

    /**
     * 集群编码（唯一标识，如：o2o-trade）
     */
    private String code;

    /**
     * 集群描述
     */
    private String description;

    /**
     * 技术栈（如：Spring Boot 2.7 + Dubbo 3.0）
     */
    private String techStack;

    /**
     * 负责人
     */
    private String owner;

    /**
     * 关联的仓库ID列表
     */
    private List<String> warehouseIds;

    /**
     * 全局依赖图（缓存）
     */
    private transient ServiceDependencyGraph dependencyGraph;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
```

### 3.2 业务领域聚合（BusinessDomain）

```java
/**
 * 业务领域聚合
 * O2O业务领域划分（如：交易域、商品域、履约域）
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class BusinessDomain {

    /**
     * 领域ID
     */
    private String id;

    /**
     * 所属集群ID
     */
    private String clusterId;

    /**
     * 领域名称（如：交易域）
     */
    private String name;

    /**
     * 领域编码（如：trade）
     */
    private String code;

    /**
     * 领域描述
     */
    private String description;

    /**
     * 领域负责人
     */
    private String owner;

    /**
     * 领域颜色（用于可视化）
     */
    private String color;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    /**
     * 关联的服务列表
     */
    private List<ServiceInfo> services;

    /**
     * 创建时间
     */
    private Date createdAt;
}

/**
 * 服务信息（值对象）
 */
@Data
public class ServiceInfo {

    /**
     * 服务ID（对应仓库ID或服务配置ID）
     */
    private String serviceId;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务描述
     */
    private String description;

    /**
     * 服务仓库ID
     */
    private String warehouseId;

    /**
     * 服务类型（PROVIDER/CONSUMER/GATEWAY）
     */
    private ServiceType type;

    /**
     * 服务状态
     */
    private ServiceStatus status;
}
```

### 3.3 Dubbo 接口聚合（DubboInterfaceRegistry）

```java
/**
 * Dubbo接口注册表
 * 记录所有Dubbo接口及其提供者信息
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Data
public class DubboInterfaceRegistry {

    /**
     * 注册表ID
     */
    private String id;

    /**
     * 所属集群ID
     */
    private String clusterId;

    /**
     * 接口全限定名
     */
    private String interfaceName;

    /**
     * 接口版本
     */
    private String version;

    /**
     * 接口分组
     */
    private String group;

    /**
     * 提供者服务ID
     */
    private String providerServiceId;

    /**
     * 提供者服务名
     */
    private String providerServiceName;

    /**
     * 接口方法列表
     */
    private List<DubboMethodInfo> methods;

    /**
     * 接口描述（从Javadoc提取）
     */
    private String description;

    /**
     * 消费者服务列表
     */
    private List<String> consumerServiceIds;

    /**
     * 是否已废弃
     */
    private Boolean deprecated;

    /**
     * 废弃说明
     */
    private String deprecatedReason;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}

/**
 * Dubbo方法信息
 */
@Data
public class DubboMethodInfo {

    /**
     * 方法名
     */
    private String name;

    /**
     * 返回类型
     */
    private String returnType;

    /**
     * 参数列表
     */
    private List<MethodParameter> parameters;

    /**
     * 方法描述
     */
    private String description;

    /**
     * 是否已废弃
     */
    private Boolean deprecated;
}

/**
 * 方法参数
 */
@Data
public class MethodParameter {

    /**
     * 参数名
     */
    private String name;

    /**
     * 参数类型
     */
    private String type;

    /**
     * 参数描述
     */
    private String description;

    /**
     * 是否必填
     */
    private Boolean required;
}
```

### 3.4 扩展文档类型

```java
/**
 * 文档类型枚举（扩展）
 */
public enum DocType {

    // 现有类型
    SYSTEM_DOC("系统说明文档"),
    ARCHITECTURE("架构文档"),
    API_GUIDE("API指南"),
    MODULE_GUIDE("模块指南"),

    // 新增类型
    SERVICE_OVERVIEW("服务概览"),
    DUBBO_INTERFACE("Dubbo接口文档"),
    DOMAIN_MODEL("领域模型"),
    CALL_CHAIN("调用链文档"),
    DEPENDENCY_MAP("依赖图"),
    INTEGRATION_GUIDE("集成指南"),
    TROUBLESHOOTING("故障排查手册"),
    CHANGE_LOG("变更日志"),

    // AI专用类型
    AI_CONTEXT_SUMMARY("AI上下文摘要"),
    AI_SERVICE_INDEX("AI服务索引");

    private final String displayName;

    DocType(String displayName) {
        this.displayName = displayName;
    }
}
```

---

## 4. 数据库设计

### 4.1 新增表结构

#### 4.1.1 服务集群表

```sql
-- V8__service_cluster.sql

-- 服务集群表
CREATE TABLE service_cluster (
    id VARCHAR(36) PRIMARY KEY COMMENT '集群ID',
    name VARCHAR(128) NOT NULL COMMENT '集群名称',
    code VARCHAR(64) NOT NULL UNIQUE COMMENT '集群编码',
    description TEXT COMMENT '集群描述',
    tech_stack VARCHAR(255) COMMENT '技术栈描述',
    owner VARCHAR(64) COMMENT '负责人',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME COMMENT '删除时间',

    INDEX idx_code (code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务集群表';

-- 集群-仓库关联表
CREATE TABLE cluster_warehouse (
    id VARCHAR(36) PRIMARY KEY COMMENT 'ID',
    cluster_id VARCHAR(36) NOT NULL COMMENT '集群ID',
    warehouse_id VARCHAR(36) NOT NULL COMMENT '仓库ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_cluster_warehouse (cluster_id, warehouse_id),
    INDEX idx_cluster (cluster_id),
    INDEX idx_warehouse (warehouse_id),

    CONSTRAINT fk_cw_cluster FOREIGN KEY (cluster_id) REFERENCES service_cluster(id),
    CONSTRAINT fk_cw_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='集群仓库关联表';
```

#### 4.1.2 业务领域表

```sql
-- V9__business_domain.sql

-- 业务领域表
CREATE TABLE business_domain (
    id VARCHAR(36) PRIMARY KEY COMMENT '领域ID',
    cluster_id VARCHAR(36) NOT NULL COMMENT '所属集群ID',
    name VARCHAR(64) NOT NULL COMMENT '领域名称',
    code VARCHAR(32) NOT NULL COMMENT '领域编码',
    description VARCHAR(500) COMMENT '领域描述',
    owner VARCHAR(64) COMMENT '领域负责人',
    color VARCHAR(20) DEFAULT '#1890ff' COMMENT '显示颜色',
    sort_order INT DEFAULT 0 COMMENT '排序权重',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME COMMENT '删除时间',

    UNIQUE KEY uk_cluster_code (cluster_id, code),
    INDEX idx_cluster (cluster_id),

    CONSTRAINT fk_domain_cluster FOREIGN KEY (cluster_id) REFERENCES service_cluster(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务领域表';

-- 服务-领域关联表
CREATE TABLE service_domain_mapping (
    id VARCHAR(36) PRIMARY KEY COMMENT 'ID',
    domain_id VARCHAR(36) NOT NULL COMMENT '领域ID',
    warehouse_id VARCHAR(36) NOT NULL COMMENT '仓库ID（即服务）',
    service_name VARCHAR(128) COMMENT '服务名称',
    service_type VARCHAR(20) DEFAULT 'PROVIDER' COMMENT '服务类型',
    description VARCHAR(500) COMMENT '服务描述',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_domain_warehouse (domain_id, warehouse_id),
    INDEX idx_domain (domain_id),
    INDEX idx_warehouse (warehouse_id),

    CONSTRAINT fk_sdm_domain FOREIGN KEY (domain_id) REFERENCES business_domain(id),
    CONSTRAINT fk_sdm_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务领域映射表';
```

#### 4.1.3 Dubbo 接口注册表

```sql
-- V10__dubbo_interface_registry.sql

-- Dubbo接口注册表
CREATE TABLE dubbo_interface_registry (
    id VARCHAR(36) PRIMARY KEY COMMENT '注册ID',
    cluster_id VARCHAR(36) NOT NULL COMMENT '所属集群ID',
    interface_name VARCHAR(255) NOT NULL COMMENT '接口全限定名',
    version VARCHAR(32) DEFAULT '' COMMENT '接口版本',
    group_name VARCHAR(64) DEFAULT '' COMMENT '接口分组',
    provider_warehouse_id VARCHAR(36) COMMENT '提供者仓库ID',
    provider_service_name VARCHAR(128) COMMENT '提供者服务名',
    description TEXT COMMENT '接口描述',
    methods JSON COMMENT '方法列表（JSON）',
    deprecated TINYINT(1) DEFAULT 0 COMMENT '是否废弃',
    deprecated_reason VARCHAR(500) COMMENT '废弃原因',
    source_file VARCHAR(500) COMMENT '源文件路径',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_interface (cluster_id, interface_name, version, group_name),
    INDEX idx_cluster (cluster_id),
    INDEX idx_provider (provider_warehouse_id),
    INDEX idx_interface_name (interface_name),

    CONSTRAINT fk_dir_cluster FOREIGN KEY (cluster_id) REFERENCES service_cluster(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Dubbo接口注册表';

-- Dubbo接口消费者关联表
CREATE TABLE dubbo_interface_consumer (
    id VARCHAR(36) PRIMARY KEY COMMENT 'ID',
    interface_registry_id VARCHAR(36) NOT NULL COMMENT '接口注册ID',
    consumer_warehouse_id VARCHAR(36) NOT NULL COMMENT '消费者仓库ID',
    consumer_service_name VARCHAR(128) COMMENT '消费者服务名',
    source_class VARCHAR(255) COMMENT '使用该接口的类',
    source_field VARCHAR(64) COMMENT '注入的字段名',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_consumer (interface_registry_id, consumer_warehouse_id, source_class),
    INDEX idx_interface (interface_registry_id),
    INDEX idx_consumer (consumer_warehouse_id),

    CONSTRAINT fk_dic_registry FOREIGN KEY (interface_registry_id) REFERENCES dubbo_interface_registry(id),
    CONSTRAINT fk_dic_consumer FOREIGN KEY (consumer_warehouse_id) REFERENCES warehouses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Dubbo接口消费者表';
```

#### 4.1.4 全局依赖图缓存表

```sql
-- V11__dependency_graph_cache.sql

-- 服务依赖图缓存表
CREATE TABLE service_dependency_graph (
    id VARCHAR(36) PRIMARY KEY COMMENT '图ID',
    cluster_id VARCHAR(36) NOT NULL COMMENT '所属集群ID',
    graph_type VARCHAR(32) NOT NULL COMMENT '图类型（FULL/DOMAIN/SERVICE）',
    scope_id VARCHAR(36) COMMENT '作用域ID（领域ID或服务ID）',
    graph_data LONGTEXT NOT NULL COMMENT '图数据（JSON格式）',
    node_count INT DEFAULT 0 COMMENT '节点数',
    edge_count INT DEFAULT 0 COMMENT '边数',
    mermaid_code LONGTEXT COMMENT 'Mermaid图代码',
    generated_at DATETIME NOT NULL COMMENT '生成时间',
    expires_at DATETIME COMMENT '过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_cluster_type_scope (cluster_id, graph_type, scope_id),
    INDEX idx_cluster (cluster_id),
    INDEX idx_expires (expires_at),

    CONSTRAINT fk_sdg_cluster FOREIGN KEY (cluster_id) REFERENCES service_cluster(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务依赖图缓存表';
```

### 4.2 现有表扩展

```sql
-- V12__extend_existing_tables.sql

-- 扩展 warehouses 表
ALTER TABLE warehouses
    ADD COLUMN cluster_id VARCHAR(36) COMMENT '所属集群ID',
    ADD COLUMN domain_id VARCHAR(36) COMMENT '所属领域ID',
    ADD COLUMN service_type VARCHAR(20) DEFAULT 'PROVIDER' COMMENT '服务类型',
    ADD INDEX idx_cluster (cluster_id),
    ADD INDEX idx_domain (domain_id);

-- 扩展 ai_document 表
ALTER TABLE ai_document
    ADD COLUMN cluster_id VARCHAR(36) COMMENT '所属集群ID',
    ADD COLUMN domain_id VARCHAR(36) COMMENT '所属领域ID',
    ADD COLUMN ai_context_level VARCHAR(20) COMMENT 'AI上下文级别（L1/L2/L3）',
    ADD INDEX idx_cluster (cluster_id),
    ADD INDEX idx_domain (domain_id);

-- 扩展 doc_type 范围（确保支持新类型）
ALTER TABLE ai_document MODIFY COLUMN doc_type VARCHAR(32);
ALTER TABLE service_document_config MODIFY COLUMN doc_type VARCHAR(32);
ALTER TABLE generation_task MODIFY COLUMN doc_type VARCHAR(32);
```

---

## 5. API 接口设计

### 5.1 服务集群 API

```
基础路径: /api/v1/clusters

┌────────────────────────────────────────────────────────────────────────┐
│ 接口                          │ 方法   │ 路径                         │
├────────────────────────────────────────────────────────────────────────┤
│ 创建集群                      │ POST   │ /                            │
│ 获取集群列表                  │ GET    │ /                            │
│ 获取集群详情                  │ GET    │ /{clusterId}                 │
│ 更新集群                      │ PUT    │ /{clusterId}                 │
│ 删除集群                      │ DELETE │ /{clusterId}                 │
│ 添加仓库到集群                │ POST   │ /{clusterId}/warehouses      │
│ 移除集群中的仓库              │ DELETE │ /{clusterId}/warehouses/{wid}│
│ 获取集群依赖图                │ GET    │ /{clusterId}/dependency-graph│
│ 刷新集群依赖图                │ POST   │ /{clusterId}/refresh-graph   │
│ 获取集群统计                  │ GET    │ /{clusterId}/statistics      │
│ 批量导入仓库                  │ POST   │ /{clusterId}/import          │
└────────────────────────────────────────────────────────────────────────┘
```

### 5.2 业务领域 API

```
基础路径: /api/v1/clusters/{clusterId}/domains

┌────────────────────────────────────────────────────────────────────────┐
│ 接口                          │ 方法   │ 路径                         │
├────────────────────────────────────────────────────────────────────────┤
│ 创建领域                      │ POST   │ /                            │
│ 获取领域列表                  │ GET    │ /                            │
│ 获取领域详情                  │ GET    │ /{domainId}                  │
│ 更新领域                      │ PUT    │ /{domainId}                  │
│ 删除领域                      │ DELETE │ /{domainId}                  │
│ 添加服务到领域                │ POST   │ /{domainId}/services         │
│ 移除领域中的服务              │ DELETE │ /{domainId}/services/{sid}   │
│ 获取领域依赖图                │ GET    │ /{domainId}/dependency-graph │
│ 获取领域文档列表              │ GET    │ /{domainId}/documents        │
│ 生成领域文档                  │ POST   │ /{domainId}/generate-docs    │
└────────────────────────────────────────────────────────────────────────┘
```

### 5.3 Dubbo 接口 API

```
基础路径: /api/v1/clusters/{clusterId}/dubbo-interfaces

┌────────────────────────────────────────────────────────────────────────┐
│ 接口                          │ 方法   │ 路径                         │
├────────────────────────────────────────────────────────────────────────┤
│ 获取接口列表                  │ GET    │ /                            │
│ 搜索接口                      │ GET    │ /search?keyword=&version=    │
│ 获取接口详情                  │ GET    │ /{interfaceId}               │
│ 获取接口消费者                │ GET    │ /{interfaceId}/consumers     │
│ 获取接口文档                  │ GET    │ /{interfaceId}/document      │
│ 生成接口文档                  │ POST   │ /{interfaceId}/generate-doc  │
│ 刷新接口注册表                │ POST   │ /refresh                     │
│ 按服务查询接口                │ GET    │ /by-service/{warehouseId}    │
│ 获取接口调用链                │ GET    │ /{interfaceId}/call-chain    │
└────────────────────────────────────────────────────────────────────────┘
```

### 5.4 跨服务搜索 API

```
基础路径: /api/v1/search

┌────────────────────────────────────────────────────────────────────────┐
│ 接口                          │ 方法   │ 路径                         │
├────────────────────────────────────────────────────────────────────────┤
│ 全局搜索                      │ GET    │ /?q=&type=&cluster=          │
│ 搜索服务                      │ GET    │ /services?q=                 │
│ 搜索接口                      │ GET    │ /interfaces?q=               │
│ 搜索文档                      │ GET    │ /documents?q=                │
│ 语义搜索（AI增强）            │ POST   │ /semantic                    │
│ 相关服务推荐                  │ GET    │ /related/{serviceId}         │
│ 影响分析                      │ POST   │ /impact-analysis             │
└────────────────────────────────────────────────────────────────────────┘
```

### 5.5 请求/响应示例

#### 创建集群

```json
// POST /api/v1/clusters
// Request
{
    "name": "O2O核心交易集群",
    "code": "o2o-trade",
    "description": "O2O业务核心交易系统，包含订单、支付、结算等服务",
    "techStack": "Spring Boot 2.7 + Dubbo 3.0 + MySQL 8.0",
    "owner": "zhourui"
}

// Response
{
    "code": 200,
    "data": {
        "id": "cluster-uuid-001",
        "name": "O2O核心交易集群",
        "code": "o2o-trade",
        "description": "O2O业务核心交易系统，包含订单、支付、结算等服务",
        "techStack": "Spring Boot 2.7 + Dubbo 3.0 + MySQL 8.0",
        "owner": "zhourui",
        "status": "ACTIVE",
        "warehouseCount": 0,
        "domainCount": 0,
        "createdAt": "2025-11-28T10:00:00Z"
    }
}
```

#### 获取集群依赖图

```json
// GET /api/v1/clusters/{clusterId}/dependency-graph
// Response
{
    "code": 200,
    "data": {
        "clusterId": "cluster-uuid-001",
        "nodeCount": 52,
        "edgeCount": 128,
        "generatedAt": "2025-11-28T10:30:00Z",
        "services": [
            {
                "id": "order-service",
                "name": "订单服务",
                "domain": "交易域",
                "type": "PROVIDER",
                "interfaceCount": 15
            }
        ],
        "dependencies": [
            {
                "source": "order-service",
                "target": "product-service",
                "type": "DUBBO",
                "interfaceName": "com.example.product.api.ProductService",
                "callCount": 3
            }
        ],
        "mermaidCode": "graph LR\n  order-service --> product-service\n  ..."
    }
}
```

---

## 6. AI 上下文优化策略

### 6.1 分层上下文模型

```
┌─────────────────────────────────────────────────────────────────────────┐
│ L1: 全局服务索引 (~2KB)                                                  │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ - 集群名称、技术栈                                                  │ │
│ │ - 领域列表（名称、服务数量）                                        │ │
│ │ - 服务清单（服务名、领域归属、核心接口数）                          │ │
│ │ - 热门接口 Top20                                                   │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────────────┤
│ L2: 领域服务摘要 (~5KB per domain)                                      │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ - 领域描述、职责边界                                                │ │
│ │ - 领域内服务列表（含简要描述）                                      │ │
│ │ - 领域内核心接口清单                                                │ │
│ │ - 领域对外依赖（其他领域）                                          │ │
│ │ - 核心业务流程摘要                                                  │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────────────┤
│ L3: 目标服务详情 (~10KB per service)                                    │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ - 服务完整描述                                                      │ │
│ │ - 提供的 Dubbo 接口（含方法签名）                                   │ │
│ │ - 依赖的 Dubbo 接口                                                │ │
│ │ - 核心领域模型                                                      │ │
│ │ - 数据库表结构摘要                                                  │ │
│ │ - 配置项说明                                                        │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────────────┤
│ L4: 相关服务扩展 (~3KB per related service)                             │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ - 上游服务摘要（调用当前服务的）                                    │ │
│ │ - 下游服务摘要（被当前服务调用的）                                  │ │
│ │ - 相关接口的调用示例                                                │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.2 上下文构建器实现

```java
/**
 * 微服务AI上下文构建器
 * 针对50+服务场景优化的分层上下文构建
 *
 * @author zhourui(V33215020)
 * @since 2025/11/28
 */
@Service
@RequiredArgsConstructor
public class MicroserviceContextBuilder {

    private final ServiceClusterService clusterService;
    private final DubboInterfaceService interfaceService;
    private final AIDocumentService documentService;

    /**
     * 构建分层上下文
     *
     * @param clusterId 集群ID
     * @param targetServiceId 目标服务ID（可选）
     * @param contextLevel 上下文级别（L1-L4）
     * @return AI上下文
     */
    public AIContext buildLayeredContext(String clusterId,
                                          String targetServiceId,
                                          ContextLevel contextLevel) {
        AIContext context = new AIContext();

        // L1: 全局服务索引（始终包含）
        context.setGlobalIndex(buildGlobalIndex(clusterId));

        if (contextLevel.ordinal() >= ContextLevel.L2.ordinal()
            && targetServiceId != null) {
            // L2: 目标服务所属领域摘要
            String domainId = findDomainByService(targetServiceId);
            context.setDomainSummary(buildDomainSummary(domainId));
        }

        if (contextLevel.ordinal() >= ContextLevel.L3.ordinal()
            && targetServiceId != null) {
            // L3: 目标服务详情
            context.setServiceDetail(buildServiceDetail(targetServiceId));
        }

        if (contextLevel.ordinal() >= ContextLevel.L4.ordinal()
            && targetServiceId != null) {
            // L4: 相关服务扩展
            context.setRelatedServices(buildRelatedServices(targetServiceId));
        }

        return context;
    }

    /**
     * 按需构建上下文（根据查询意图智能选择）
     *
     * @param query 用户查询
     * @param hints 提示信息
     * @return AI上下文
     */
    public AIContext buildOnDemandContext(String query, List<String> hints) {
        // 1. 分析查询意图
        QueryIntent intent = analyzeQueryIntent(query);

        // 2. 识别相关服务
        List<String> relevantServices = findRelevantServices(query, hints);

        // 3. 构建最小必要上下文
        AIContext context = new AIContext();
        context.setGlobalIndex(buildCompactGlobalIndex());

        for (String serviceId : relevantServices) {
            // 只加载相关服务的摘要
            context.addServiceSummary(buildServiceSummary(serviceId));
        }

        // 4. 加载相关接口详情
        if (intent.needsInterfaceDetail()) {
            List<String> interfaceNames = extractInterfaceNames(query);
            for (String interfaceName : interfaceNames) {
                context.addInterfaceDetail(
                    interfaceService.getInterfaceDetail(interfaceName)
                );
            }
        }

        return context;
    }

    /**
     * 构建全局服务索引（精简版）
     */
    private GlobalServiceIndex buildGlobalIndex(String clusterId) {
        ServiceCluster cluster = clusterService.getById(clusterId);

        GlobalServiceIndex index = new GlobalServiceIndex();
        index.setClusterName(cluster.getName());
        index.setTechStack(cluster.getTechStack());
        index.setTotalServices(cluster.getWarehouseIds().size());

        // 领域列表
        List<DomainBrief> domains = clusterService.listDomains(clusterId)
            .stream()
            .map(d -> new DomainBrief(d.getName(), d.getCode(),
                                       d.getServices().size()))
            .collect(Collectors.toList());
        index.setDomains(domains);

        // 服务清单（精简）
        List<ServiceBrief> services = cluster.getWarehouseIds().stream()
            .map(this::buildServiceBrief)
            .collect(Collectors.toList());
        index.setServices(services);

        // 热门接口 Top20
        List<InterfaceBrief> hotInterfaces =
            interfaceService.getTopInterfaces(clusterId, 20);
        index.setHotInterfaces(hotInterfaces);

        return index;
    }
}
```

### 6.3 上下文大小预估

| 上下文级别 | 包含内容 | 预估大小 | 适用场景 |
|------------|----------|----------|----------|
| L1 | 全局索引 | ~2KB | 服务定位、快速导航 |
| L1+L2 | 全局索引 + 领域摘要 | ~7KB | 领域内方案设计 |
| L1+L2+L3 | 全局索引 + 领域 + 目标服务 | ~17KB | 服务改造、接口设计 |
| L1+L2+L3+L4 | 完整四层 | ~30KB | 复杂集成方案 |
| 按需加载 | 动态选择 | ~5-15KB | AI自主查询 |

---

## 7. MCP Server 扩展设计

### 7.1 新增工具清单

```python
# 扩展后的 MCP Server 工具列表

TOOLS = [
    # ============ 集群管理 ============
    Tool(
        name="list_clusters",
        description="列出所有服务集群",
        inputSchema={...}
    ),
    Tool(
        name="get_cluster_overview",
        description="获取集群概览（包含领域列表、服务统计）",
        inputSchema={
            "type": "object",
            "properties": {
                "cluster_id": {"type": "string", "description": "集群ID"}
            },
            "required": ["cluster_id"]
        }
    ),

    # ============ 领域查询 ============
    Tool(
        name="list_domains",
        description="列出集群下的所有业务领域",
        inputSchema={
            "type": "object",
            "properties": {
                "cluster_id": {"type": "string", "description": "集群ID"}
            },
            "required": ["cluster_id"]
        }
    ),
    Tool(
        name="get_domain_services",
        description="获取领域内的所有服务及其接口摘要",
        inputSchema={
            "type": "object",
            "properties": {
                "cluster_id": {"type": "string"},
                "domain_code": {"type": "string", "description": "领域编码，如 trade/product"}
            },
            "required": ["cluster_id", "domain_code"]
        }
    ),

    # ============ 服务查询 ============
    Tool(
        name="get_service_detail",
        description="获取服务详情（接口、依赖、配置）",
        inputSchema={
            "type": "object",
            "properties": {
                "service_id": {"type": "string", "description": "服务ID（仓库ID）"}
            },
            "required": ["service_id"]
        }
    ),
    Tool(
        name="get_service_dependencies",
        description="获取服务的上下游依赖关系",
        inputSchema={
            "type": "object",
            "properties": {
                "service_id": {"type": "string"},
                "direction": {
                    "type": "string",
                    "enum": ["upstream", "downstream", "both"],
                    "default": "both"
                },
                "depth": {"type": "integer", "default": 2}
            },
            "required": ["service_id"]
        }
    ),

    # ============ Dubbo 接口 ============
    Tool(
        name="search_dubbo_interfaces",
        description="搜索 Dubbo 接口（支持接口名、方法名模糊匹配）",
        inputSchema={
            "type": "object",
            "properties": {
                "cluster_id": {"type": "string"},
                "keyword": {"type": "string", "description": "搜索关键词"},
                "version": {"type": "string"},
                "limit": {"type": "integer", "default": 20}
            },
            "required": ["cluster_id", "keyword"]
        }
    ),
    Tool(
        name="get_interface_detail",
        description="获取 Dubbo 接口详情（方法签名、参数、返回值、消费者列表）",
        inputSchema={
            "type": "object",
            "properties": {
                "interface_name": {"type": "string", "description": "接口全限定名"}
            },
            "required": ["interface_name"]
        }
    ),
    Tool(
        name="get_interface_call_chain",
        description="获取接口的调用链路图",
        inputSchema={
            "type": "object",
            "properties": {
                "interface_name": {"type": "string"},
                "max_depth": {"type": "integer", "default": 5}
            },
            "required": ["interface_name"]
        }
    ),

    # ============ 文档查询 ============
    Tool(
        name="search_documents",
        description="跨服务搜索文档",
        inputSchema={
            "type": "object",
            "properties": {
                "query": {"type": "string", "description": "搜索关键词"},
                "cluster_id": {"type": "string"},
                "domain_code": {"type": "string"},
                "doc_type": {"type": "string"},
                "limit": {"type": "integer", "default": 10}
            },
            "required": ["query"]
        }
    ),
    Tool(
        name="get_ai_context",
        description="获取 AI 上下文（分层结构，用于技术方案设计）",
        inputSchema={
            "type": "object",
            "properties": {
                "cluster_id": {"type": "string"},
                "target_service_id": {"type": "string"},
                "context_level": {
                    "type": "string",
                    "enum": ["L1", "L2", "L3", "L4"],
                    "default": "L2"
                }
            },
            "required": ["cluster_id"]
        }
    ),

    # ============ 分析建议 ============
    Tool(
        name="suggest_integration",
        description="根据需求推荐集成方案和相关服务",
        inputSchema={
            "type": "object",
            "properties": {
                "cluster_id": {"type": "string"},
                "requirement": {"type": "string", "description": "需求描述"},
                "context_services": {
                    "type": "array",
                    "items": {"type": "string"},
                    "description": "已知相关的服务ID列表"
                }
            },
            "required": ["cluster_id", "requirement"]
        }
    ),
    Tool(
        name="analyze_impact",
        description="分析服务或接口变更的影响范围",
        inputSchema={
            "type": "object",
            "properties": {
                "service_id": {"type": "string"},
                "interface_name": {"type": "string"},
                "change_type": {
                    "type": "string",
                    "enum": ["ADD_FIELD", "REMOVE_FIELD", "MODIFY_SIGNATURE", "DEPRECATE"],
                    "description": "变更类型"
                }
            },
            "required": ["change_type"]
        }
    ),

    # ============ 拓扑可视化 ============
    Tool(
        name="get_topology_graph",
        description="获取服务拓扑图（Mermaid 格式）",
        inputSchema={
            "type": "object",
            "properties": {
                "cluster_id": {"type": "string"},
                "scope": {
                    "type": "string",
                    "enum": ["cluster", "domain", "service"],
                    "default": "cluster"
                },
                "scope_id": {"type": "string", "description": "领域ID或服务ID"},
                "format": {
                    "type": "string",
                    "enum": ["mermaid", "json"],
                    "default": "mermaid"
                }
            },
            "required": ["cluster_id"]
        }
    )
]
```

### 7.2 MCP Server 实现示例

```python
#!/usr/bin/env python3
"""
OpenDeepWiki MCP Server - 微服务文档管理系统

支持50+ Spring Boot + Dubbo 微服务的文档查询和AI上下文构建

@author zhourui(V33215020)
@since 2025/11/28
"""

import asyncio
import httpx
from mcp.server import Server
from mcp.types import Tool, TextContent

API_BASE_URL = "http://localhost:18091/api/v1"

app = Server("opendeepwiki-microservice-server")


@app.call_tool()
async def call_tool(name: str, arguments: dict) -> list[TextContent]:
    async with httpx.AsyncClient(timeout=30.0) as client:

        # ============ 集群管理 ============
        if name == "list_clusters":
            response = await client.get(f"{API_BASE_URL}/clusters")
            return [TextContent(type="text", text=format_response(response))]

        elif name == "get_cluster_overview":
            cluster_id = arguments["cluster_id"]
            response = await client.get(
                f"{API_BASE_URL}/clusters/{cluster_id}",
                params={"include": "domains,statistics"}
            )
            return [TextContent(type="text", text=format_response(response))]

        # ============ 领域查询 ============
        elif name == "get_domain_services":
            cluster_id = arguments["cluster_id"]
            domain_code = arguments["domain_code"]
            response = await client.get(
                f"{API_BASE_URL}/clusters/{cluster_id}/domains",
                params={"code": domain_code, "include": "services"}
            )
            return [TextContent(type="text", text=format_response(response))]

        # ============ Dubbo 接口 ============
        elif name == "search_dubbo_interfaces":
            cluster_id = arguments["cluster_id"]
            keyword = arguments["keyword"]
            version = arguments.get("version", "")
            limit = arguments.get("limit", 20)

            response = await client.get(
                f"{API_BASE_URL}/clusters/{cluster_id}/dubbo-interfaces/search",
                params={"keyword": keyword, "version": version, "limit": limit}
            )
            return [TextContent(type="text", text=format_response(response))]

        elif name == "get_interface_detail":
            interface_name = arguments["interface_name"]
            response = await client.get(
                f"{API_BASE_URL}/dubbo-interfaces",
                params={"name": interface_name, "include": "methods,consumers"}
            )
            return [TextContent(type="text", text=format_response(response))]

        # ============ AI 上下文 ============
        elif name == "get_ai_context":
            cluster_id = arguments["cluster_id"]
            target_service_id = arguments.get("target_service_id")
            context_level = arguments.get("context_level", "L2")

            response = await client.get(
                f"{API_BASE_URL}/clusters/{cluster_id}/ai-context",
                params={
                    "targetServiceId": target_service_id,
                    "level": context_level
                }
            )
            return [TextContent(type="text", text=format_response(response))]

        # ============ 拓扑图 ============
        elif name == "get_topology_graph":
            cluster_id = arguments["cluster_id"]
            scope = arguments.get("scope", "cluster")
            scope_id = arguments.get("scope_id")
            fmt = arguments.get("format", "mermaid")

            response = await client.get(
                f"{API_BASE_URL}/clusters/{cluster_id}/topology",
                params={"scope": scope, "scopeId": scope_id, "format": fmt}
            )
            return [TextContent(type="text", text=format_response(response))]

        # ============ 影响分析 ============
        elif name == "analyze_impact":
            service_id = arguments.get("service_id")
            interface_name = arguments.get("interface_name")
            change_type = arguments["change_type"]

            response = await client.post(
                f"{API_BASE_URL}/search/impact-analysis",
                json={
                    "serviceId": service_id,
                    "interfaceName": interface_name,
                    "changeType": change_type
                }
            )
            return [TextContent(type="text", text=format_response(response))]

        else:
            raise ValueError(f"Unknown tool: {name}")


def format_response(response):
    """格式化API响应"""
    response.raise_for_status()
    data = response.json()

    # 如果是分页数据，提取核心内容
    if isinstance(data, dict) and "data" in data:
        return json.dumps(data["data"], ensure_ascii=False, indent=2)

    return json.dumps(data, ensure_ascii=False, indent=2)


async def main():
    from mcp.server.stdio import stdio_server
    async with stdio_server() as (read_stream, write_stream):
        await app.run(
            read_stream,
            write_stream,
            app.create_initialization_options()
        )


if __name__ == "__main__":
    asyncio.run(main())
```

---

## 8. 前端设计概要

### 8.1 页面结构

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 顶部导航栏                                                               │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ Logo  │ 集群选择器 ▼ │ 全局搜索 🔍 │ 用户信息                         │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────┐  ┌───────────────────────────────────────────────┐   │
│  │              │  │                                               │   │
│  │  左侧导航    │  │              主内容区                          │   │
│  │              │  │                                               │   │
│  │ ▼ 交易域     │  │  ┌─────────────────────────────────────────┐ │   │
│  │   订单服务   │  │  │        服务拓扑图 / 文档内容            │ │   │
│  │   支付服务   │  │  │                                         │ │   │
│  │   结算服务   │  │  │                                         │ │   │
│  │              │  │  └─────────────────────────────────────────┘ │   │
│  │ ▼ 商品域     │  │                                               │   │
│  │   商品服务   │  │  ┌─────────────────────────────────────────┐ │   │
│  │   库存服务   │  │  │        接口列表 / 依赖关系              │ │   │
│  │   价格服务   │  │  │                                         │ │   │
│  │              │  │  └─────────────────────────────────────────┘ │   │
│  │ ▼ 履约域     │  │                                               │   │
│  │   配送服务   │  │                                               │   │
│  │   物流服务   │  │                                               │   │
│  │              │  │                                               │   │
│  └──────────────┘  └───────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 8.2 核心页面

| 页面 | 路由 | 功能 |
|------|------|------|
| 集群概览 | `/clusters/:id` | 集群统计、领域分布、服务拓扑 |
| 领域详情 | `/clusters/:id/domains/:code` | 领域服务列表、领域拓扑 |
| 服务详情 | `/services/:id` | 服务文档、接口列表、依赖图 |
| Dubbo接口 | `/interfaces/:name` | 接口详情、消费者、调用链 |
| 全局搜索 | `/search` | 跨服务搜索、语义搜索 |
| 集群管理 | `/admin/clusters` | 集群CRUD、仓库导入 |
| 领域管理 | `/admin/domains` | 领域CRUD、服务归属 |

---

## 9. 实施路线图与阶段评估

### Phase 1: 基础设施（2-3周）

#### 9.1.1 任务清单

```
Week 1-2:
├── 数据库迁移脚本（V8-V12）
├── 领域模型实现（ServiceCluster, BusinessDomain, DubboInterfaceRegistry）
├── JPA Entity 和 Repository
└── 基础 CRUD 服务

Week 3:
├── 集群管理 API 实现
├── 领域管理 API 实现
└── 单元测试和集成测试
```

#### 9.1.2 关键要点

| 要点 | 说明 | 优先级 | 风险等级 |
|------|------|--------|----------|
| **外键约束策略** | 新表与 `warehouses` 表建立外键关联，需确保级联删除策略正确。建议使用软删除，避免物理删除导致关联数据异常 | P0 | 高 |
| **迁移脚本幂等性** | V8-V12 脚本必须支持重复执行不报错，使用 `IF NOT EXISTS` 和条件判断 | P0 | 中 |
| **现有数据兼容** | `warehouses` 表新增 `cluster_id`、`domain_id` 字段后，历史数据为 NULL，需提供默认集群和未分类领域兜底 | P0 | 高 |
| **领域模型与 JPA 分离** | 严格保持 Domain 层纯净，JPA Entity 在 Infra 层，通过 Mapper 转换 | P1 | 低 |
| **批量导入接口** | 50+ 仓库需支持 Excel/JSON 批量导入，单次导入需控制在 100 个以内，超出分批处理 | P1 | 中 |

#### 9.1.3 技术决策

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 决策 1: 集群与仓库的关系                                                  │
├─────────────────────────────────────────────────────────────────────────┤
│ 方案 A: 仓库只能属于一个集群（一对多）                                     │
│ 方案 B: 仓库可属于多个集群（多对多）                                       │
│                                                                         │
│ 【选择方案 A】                                                           │
│ 理由: O2O 场景下，一个服务仓库通常只属于一个业务集群，多对多增加复杂度且    │
│       实际需求不强。如有跨集群复用需求，可通过"服务别名"或"引用"机制解决。   │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ 决策 2: 领域与服务的关系                                                  │
├─────────────────────────────────────────────────────────────────────────┤
│ 方案 A: 服务只能属于一个领域（一对多）                                     │
│ 方案 B: 服务可属于多个领域（多对多，通过 service_domain_mapping）          │
│                                                                         │
│ 【选择方案 B】                                                           │
│ 理由: 部分基础服务（如用户服务）可能被多个领域共用，多对多更灵活。          │
│       通过 service_domain_mapping 表管理，主领域通过 sort_order=0 标识。  │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ 决策 3: 默认集群处理                                                      │
├─────────────────────────────────────────────────────────────────────────┤
│ 迁移时自动创建 "default" 集群和 "unclassified" 领域                       │
│ 所有历史仓库归入默认集群，历史文档归入未分类领域                            │
│ 提供管理界面支持批量调整归属                                               │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 9.1.4 验收标准

- [ ] V8-V12 迁移脚本在 H2/MySQL/PostgreSQL 均可执行
- [ ] 集群 CRUD API 通过 Postman 测试
- [ ] 领域 CRUD API 通过 Postman 测试
- [ ] 仓库可成功关联到集群和领域
- [ ] 历史数据迁移后查询正常
- [ ] 单元测试覆盖率 > 80%

---

### Phase 2: Dubbo 增强（2-3周）

#### 9.2.1 任务清单

```
Week 4-5:
├── Dubbo 接口扫描器增强
├── 接口注册表自动更新
├── 接口文档生成器
└── 消费者关系分析

Week 6:
├── Dubbo 接口 API 实现
├── 接口搜索功能
└── 接口调用链生成
```

#### 9.2.2 关键要点

| 要点 | 说明 | 优先级 | 风险等级 |
|------|------|--------|----------|
| **Dubbo 注解兼容** | 需同时支持 `@DubboService`（Dubbo 3.x）和 `@Service`（Dubbo 2.x），以及 `@DubboReference` 和 `@Reference` | P0 | 高 |
| **接口版本与分组** | Dubbo 接口可能有多版本（version）和多分组（group），注册表需支持复合唯一键 | P0 | 高 |
| **方法签名解析** | 需完整解析方法参数类型（含泛型）、返回值、异常声明，建议使用 JavaParser 而非正则 | P0 | 中 |
| **Javadoc 提取** | 从源码提取接口和方法的 Javadoc 注释作为描述，需处理无注释情况 | P1 | 低 |
| **增量扫描** | 50+ 服务全量扫描耗时过长，需实现基于 Git commit 的增量扫描 | P1 | 高 |
| **消费者追踪** | 分析 `@DubboReference` 字段，建立接口-消费者映射，需跨仓库关联 | P0 | 中 |

#### 9.2.3 技术决策

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 决策 4: 接口扫描触发机制                                                  │
├─────────────────────────────────────────────────────────────────────────┤
│ 方案 A: 仓库同步时自动触发扫描                                            │
│ 方案 B: 定时任务周期扫描                                                  │
│ 方案 C: 手动触发 + 仓库同步时触发                                         │
│                                                                         │
│ 【选择方案 C】                                                           │
│ 理由: 仓库同步后自动扫描确保数据及时，手动触发支持批量刷新场景。           │
│       定时任务作为兜底，每 6 小时检查一次是否有遗漏。                       │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ 决策 5: 接口文档生成策略                                                  │
├─────────────────────────────────────────────────────────────────────────┤
│ 方案 A: 每个接口单独生成文档                                              │
│ 方案 B: 按服务聚合生成接口文档                                            │
│ 方案 C: 两种都支持，按需选择                                              │
│                                                                         │
│ 【选择方案 C】                                                           │
│ 理由: 单接口文档便于精确查询，服务级文档便于整体了解。                      │
│       DocType 区分: DUBBO_INTERFACE（单接口）vs SERVICE_OVERVIEW（含接口列表）│
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ 决策 6: 跨仓库接口关联                                                    │
├─────────────────────────────────────────────────────────────────────────┤
│ 接口消费者可能在不同仓库，需要集群级别的关联分析                            │
│ 扫描时记录接口全限定名，后续通过名称匹配建立 Provider-Consumer 关系         │
│ 支持模糊匹配处理接口名导入语句简写情况                                      │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 9.2.4 核心扫描流程

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Dubbo 接口扫描流程                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. 仓库同步完成事件触发                                                  │
│         │                                                               │
│         ▼                                                               │
│  2. 扫描 Java 文件，识别 @DubboService 类                                │
│         │                                                               │
│         ▼                                                               │
│  3. 解析实现的接口，提取方法签名                                          │
│         │                                                               │
│         ├──► 3a. 解析 Javadoc 注释                                      │
│         │                                                               │
│         ├──► 3b. 解析 @Deprecated 标记                                  │
│         │                                                               │
│         ▼                                                               │
│  4. 写入 dubbo_interface_registry 表                                    │
│         │                                                               │
│         ▼                                                               │
│  5. 扫描 @DubboReference 字段，建立消费关系                               │
│         │                                                               │
│         ▼                                                               │
│  6. 写入 dubbo_interface_consumer 表                                    │
│         │                                                               │
│         ▼                                                               │
│  7. 触发依赖图缓存刷新                                                    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 9.2.5 验收标准

- [ ] 正确识别 Dubbo 2.x 和 3.x 注解
- [ ] 接口方法签名解析完整（含泛型）
- [ ] 消费者关系跨仓库正确关联
- [ ] 接口搜索支持模糊匹配
- [ ] 增量扫描性能 < 30s（单仓库）
- [ ] 接口调用链生成正确

---

### Phase 3: AI 能力增强（2周）

#### 9.3.1 任务清单

```
Week 7:
├── AI 上下文构建器实现
├── 分层上下文策略
└── MCP Server 工具扩展

Week 8:
├── 语义搜索基础
├── 集成建议原型
└── 影响分析功能
```

#### 9.3.2 关键要点

| 要点 | 说明 | 优先级 | 风险等级 |
|------|------|--------|----------|
| **上下文大小控制** | Claude 上下文窗口有限，L1-L4 各层需严格控制大小，提供压缩和截断策略 | P0 | 高 |
| **分层缓存** | L1 全局索引变化少，可缓存较长时间；L3/L4 变化频繁，需按需生成 | P1 | 中 |
| **MCP 工具原子性** | 每个 MCP 工具应职责单一，复杂查询通过组合多个工具实现 | P0 | 低 |
| **语义搜索降级** | 若 Embedding 服务不可用，降级为关键词搜索 | P1 | 中 |
| **影响分析深度** | 默认分析 2 层依赖，深度过大会导致结果过多，需支持配置 | P1 | 低 |

#### 9.3.3 技术决策

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 决策 7: AI 上下文缓存策略                                                 │
├─────────────────────────────────────────────────────────────────────────┤
│ L1 全局索引:                                                             │
│   - 缓存时间: 1 小时                                                     │
│   - 刷新时机: 集群/领域/服务变更时主动失效                                 │
│                                                                         │
│ L2 领域摘要:                                                             │
│   - 缓存时间: 30 分钟                                                    │
│   - 刷新时机: 领域内服务变更时主动失效                                     │
│                                                                         │
│ L3 服务详情:                                                             │
│   - 缓存时间: 10 分钟                                                    │
│   - 刷新时机: 服务仓库同步后主动失效                                       │
│                                                                         │
│ L4 相关服务:                                                             │
│   - 不缓存，实时计算                                                      │
│   - 理由: 依赖关系可能频繁变化，且计算成本可控                             │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ 决策 8: 语义搜索实现方案                                                  │
├─────────────────────────────────────────────────────────────────────────┤
│ 方案 A: 使用外部向量数据库（如 Milvus、Pinecone）                         │
│ 方案 B: 使用 PostgreSQL pgvector 扩展                                    │
│ 方案 C: 先用关键词搜索 + TF-IDF，后续迭代引入向量搜索                      │
│                                                                         │
│ 【选择方案 C】                                                           │
│ 理由: 快速上线，验证需求后再决定是否引入向量搜索。                          │
│       关键词搜索配合 MySQL FULLTEXT 索引，足以满足初期需求。               │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ 决策 9: MCP Server 部署方式                                               │
├─────────────────────────────────────────────────────────────────────────┤
│ 方案 A: 与主应用同进程（通过 HTTP 调用）                                  │
│ 方案 B: 独立 Python 进程（通过 stdio）                                    │
│                                                                         │
│ 【选择方案 B】                                                           │
│ 理由: MCP 协议基于 stdio，独立进程更符合规范。                             │
│       Python 实现灵活，便于快速迭代工具。                                  │
│       通过 systemd 管理进程，确保稳定性。                                  │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 9.3.4 上下文压缩策略

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        上下文压缩策略                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│ 1. 服务描述压缩                                                          │
│    - 最大 200 字符，超出截断并添加 "..."                                  │
│    - 保留首句（通常是核心功能说明）                                        │
│                                                                         │
│ 2. 接口列表压缩                                                          │
│    - 每服务最多显示 10 个核心接口                                         │
│    - 按调用频率排序（若有统计数据）                                        │
│    - 其余接口仅显示数量: "... 及其他 15 个接口"                           │
│                                                                         │
│ 3. 方法签名压缩                                                          │
│    - 参数超过 3 个时: "method(Param1, Param2, ...)"                      │
│    - 泛型简化: "List<UserDTO>" → "List<UserDTO>"（保留一层）              │
│                                                                         │
│ 4. 依赖关系压缩                                                          │
│    - 同类型依赖合并: "依赖商品域 3 个服务"                                 │
│    - 详情按需展开                                                        │
│                                                                         │
│ 5. 动态裁剪                                                              │
│    - 若总上下文超过 max-size，优先裁剪 L4 → L3 → L2                       │
│    - 保证 L1 始终完整                                                    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 9.3.5 验收标准

- [ ] L1-L4 上下文生成正确且大小可控
- [ ] MCP Server 15+ 工具均可正常调用
- [ ] 上下文缓存命中率 > 70%
- [ ] 搜索响应时间 < 500ms
- [ ] 影响分析结果准确

---

### Phase 4: 前端与可视化（2周）

#### 9.4.1 任务清单

```
Week 9:
├── 集群概览页面
├── 领域导航组件
├── 服务拓扑可视化

Week 10:
├── 全局搜索页面
├── 接口详情页面
├── 管理后台页面
```

#### 9.4.2 关键要点

| 要点 | 说明 | 优先级 | 风险等级 |
|------|------|--------|----------|
| **拓扑图性能** | 50+ 节点的依赖图渲染需优化，建议使用 D3.js/ECharts 力导向图 | P0 | 高 |
| **按需加载** | 领域列表、服务列表采用懒加载，避免首屏加载过慢 | P1 | 中 |
| **响应式设计** | 支持大屏展示（全局拓扑）和普通屏幕（文档阅读） | P1 | 低 |
| **搜索体验** | 全局搜索支持快捷键（Ctrl+K），实时搜索建议 | P1 | 低 |
| **路由设计** | 支持深链接，可直接分享服务/接口页面 URL | P0 | 低 |

#### 9.4.3 技术决策

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 决策 10: 拓扑图技术选型                                                   │
├─────────────────────────────────────────────────────────────────────────┤
│ 方案 A: ECharts Graph                                                   │
│ 方案 B: D3.js Force Layout                                              │
│ 方案 C: Vis.js Network                                                  │
│ 方案 D: 后端生成 Mermaid，前端渲染                                        │
│                                                                         │
│ 【选择方案 A + D 结合】                                                  │
│ 理由:                                                                    │
│   - 集群级全景图: ECharts Graph，支持缩放、拖拽、节点点击                  │
│   - 服务级调用链: Mermaid 时序图，更直观展示调用顺序                        │
│   - 两者互补，满足不同场景需求                                            │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ 决策 11: 状态管理方案                                                     │
├─────────────────────────────────────────────────────────────────────────┤
│ 现有项目使用 Vue 3 + Pinia                                               │
│ 新增状态:                                                                │
│   - clusterStore: 当前选中集群、集群列表                                  │
│   - domainStore: 领域列表、当前领域                                       │
│   - searchStore: 搜索历史、搜索结果缓存                                   │
│                                                                         │
│ 持久化策略:                                                              │
│   - 当前集群 ID 存入 localStorage，刷新后保持                              │
│   - 搜索历史最多保留 20 条                                                │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 9.4.4 页面交互设计

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        集群概览页交互设计                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  集群: O2O核心交易集群 ▼     [刷新依赖图] [导出] [设置]          │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌──────────────────┐  ┌──────────────────────────────────────────┐   │
│  │ 统计卡片区       │  │                                          │   │
│  │ ┌──────┐┌──────┐│  │                                          │   │
│  │ │服务数││接口数││  │           服务拓扑图                       │   │
│  │ │  52  ││ 328  ││  │                                          │   │
│  │ └──────┘└──────┘│  │     [交易域]──┬──>[商品域]                │   │
│  │ ┌──────┐┌──────┐│  │        │      │      │                   │   │
│  │ │领域数││文档数││  │        ▼      │      ▼                   │   │
│  │ │  6   ││ 156  ││  │     [履约域]<─┴──[用户域]                 │   │
│  │ └──────┘└──────┘│  │                                          │   │
│  │                  │  │  点击节点: 展开领域内服务                  │   │
│  │ [热门接口 Top10] │  │  双击节点: 跳转领域详情                    │   │
│  │  - OrderService  │  │  拖拽: 调整布局                          │   │
│  │  - ProductAPI    │  │  滚轮: 缩放                              │   │
│  │  - PaymentFacade │  │                                          │   │
│  │  ...             │  │                                          │   │
│  └──────────────────┘  └──────────────────────────────────────────┘   │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ 领域列表                                                        │   │
│  │ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │   │
│  │ │ 交易域  │ │ 商品域  │ │ 履约域  │ │ 用户域  │ │ 营销域  │   │   │
│  │ │ 8 服务  │ │ 6 服务  │ │ 5 服务  │ │ 4 服务  │ │ 7 服务  │   │   │
│  │ │ 45 接口 │ │ 38 接口 │ │ 28 接口 │ │ 22 接口 │ │ 35 接口 │   │   │
│  │ └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 9.4.5 验收标准

- [ ] 集群概览页正确展示统计和拓扑图
- [ ] 拓扑图 50 节点渲染流畅（FPS > 30）
- [ ] 领域导航支持展开/折叠
- [ ] 全局搜索响应 < 300ms
- [ ] 所有页面支持深链接
- [ ] 移动端基本可用

---

### 9.5 阶段依赖关系

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          阶段依赖关系图                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│    Phase 1                Phase 2                Phase 3                │
│  ┌─────────┐           ┌─────────┐           ┌─────────┐              │
│  │ 基础设施 │──────────▶│  Dubbo  │──────────▶│   AI    │              │
│  │         │           │  增强   │           │  增强   │              │
│  └────┬────┘           └────┬────┘           └────┬────┘              │
│       │                     │                     │                    │
│       │    ┌────────────────┴─────────────────────┘                    │
│       │    │                                                           │
│       ▼    ▼                                                           │
│  ┌─────────────┐                                                       │
│  │   Phase 4   │                                                       │
│  │ 前端可视化  │                                                       │
│  └─────────────┘                                                       │
│                                                                         │
│  说明:                                                                  │
│  - Phase 4 依赖 Phase 1 提供的 API（集群/领域 CRUD）                     │
│  - Phase 4 依赖 Phase 2 提供的 API（Dubbo 接口查询）                     │
│  - Phase 4 可与 Phase 3 并行，MCP 功能不阻塞前端开发                     │
│  - Phase 2 依赖 Phase 1 的集群/领域数据结构                              │
│  - Phase 3 依赖 Phase 1、Phase 2 的数据作为上下文输入                    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 9.6 里程碑与交付物

| 里程碑 | 时间点 | 交付物 |
|--------|--------|--------|
| M1: 基础设施完成 | Week 3 末 | 数据库 Schema、集群/领域 API、批量导入功能 |
| M2: Dubbo 增强完成 | Week 6 末 | 接口注册表、接口搜索、调用链分析 |
| M3: AI 能力完成 | Week 8 末 | MCP Server 扩展、上下文构建器、搜索功能 |
| M4: 前端上线 | Week 10 末 | 完整 UI、拓扑可视化、管理后台 |
| M5: 全面验收 | Week 11 | 端到端测试通过、文档完善、培训完成 |

---

## 10. 风险与缓解

| 风险 | 描述 | 缓解措施 |
|------|------|----------|
| 大规模扫描性能 | 50+ 服务同时扫描可能耗时 | 增量扫描、异步处理、缓存依赖图 |
| 上下文过大 | 服务过多导致 AI 上下文超限 | 分层策略、按需加载、摘要压缩 |
| 接口变更追踪 | 接口频繁变更难以追踪 | 定时同步、版本对比、变更通知 |
| 历史数据迁移 | 已有文档缺少集群/领域信息 | 默认集群兜底、批量迁移工具 |

---

## 11. 附录

### A. 枚举定义

```java
public enum ServiceType {
    PROVIDER,    // 服务提供者
    CONSUMER,    // 服务消费者
    GATEWAY,     // 网关服务
    SCHEDULER,   // 调度服务
    MIDDLEWARE   // 中间件
}

public enum ContextLevel {
    L1,  // 全局索引
    L2,  // 领域摘要
    L3,  // 服务详情
    L4   // 完整扩展
}

public enum DependencyType {
    DUBBO,       // Dubbo RPC
    FEIGN,       // Feign HTTP
    MQ,          // 消息队列
    DATABASE,    // 数据库
    CACHE        // 缓存
}
```

### B. 配置项

```yaml
koalawiki:
  cluster:
    # 依赖图缓存过期时间（小时）
    graph-cache-ttl: 24
    # 最大支持服务数
    max-services: 200

  dubbo:
    # 是否启用注册中心同步
    registry-sync-enabled: false
    # 注册中心地址
    registry-address: nacos://localhost:8848
    # 接口扫描间隔（分钟）
    scan-interval: 60

  ai:
    context:
      # 默认上下文级别
      default-level: L2
      # 最大上下文大小（KB）
      max-size: 50
      # 服务摘要最大长度
      service-summary-max-length: 500
```

---

**文档版本**: v1.0
**创建日期**: 2025-11-28
**作者**: zhourui(V33215020)

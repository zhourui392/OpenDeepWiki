# OpenDeepWiki MCP Server

将OpenDeepWiki的文档查看服务封装为MCP服务。

## 功能

提供3个MCP工具：

1. **list_services** - 列出仓库下的所有服务配置
2. **get_service** - 获取指定服务的文档配置详情
3. **list_documents** - 列出仓库下的所有AI生成文档

## 安装

```bash
cd mcp-server
pip install -r requirements.txt
```

## 配置

在Claude Desktop配置文件中添加：

**macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
**Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "opendeepwiki": {
      "command": "python",
      "args": ["/home/ubuntu/workspace/OpenDeepWiki/mcp-server/document_mcp_server.py"]
    }
  }
}
```

## 使用

启动OpenDeepWiki后端服务（端口18091），然后重启Claude Desktop即可使用。

## 示例

在Claude中可以这样使用：

```
请列出仓库 warehouse-123 下的所有服务配置
```

```
获取仓库 warehouse-123 中服务 user-service 的文档配置
```

```
列出仓库 warehouse-123 的前10个文档
```

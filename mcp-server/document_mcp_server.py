#!/usr/bin/env python3
"""
OpenDeepWiki MCP Server - 微服务文档管理系统

支持50+ Spring Boot + Dubbo 微服务的文档查询和AI上下文构建

提供以下工具：
1. 集群管理：list_clusters, get_cluster_overview
2. 领域查询：list_domains, get_domain_services
3. 服务查询：get_service_detail, get_service_dependencies
4. Dubbo接口：search_dubbo_interfaces, get_interface_detail, get_interface_call_chain
5. 文档查询：search_documents, get_ai_context
6. 分析建议：suggest_integration, analyze_impact
7. 拓扑可视化：get_topology_graph

@author zhourui(V33215020)
@since 2025/11/28
"""

import asyncio
import json
import httpx
from mcp.server import Server
from mcp.types import Tool, TextContent

API_BASE_URL = "http://localhost:18091/api/v1"

app = Server("opendeepwiki-microservice-server")


@app.list_tools()
async def list_tools() -> list[Tool]:
    """定义所有可用的MCP工具"""
    return [
        # ============ 集群管理 ============
        Tool(
            name="list_clusters",
            description="列出所有服务集群",
            inputSchema={
                "type": "object",
                "properties": {
                    "status": {
                        "type": "string",
                        "description": "集群状态过滤（ACTIVE/INACTIVE）",
                        "enum": ["ACTIVE", "INACTIVE"]
                    }
                }
            }
        ),
        Tool(
            name="get_cluster_overview",
            description="获取集群概览（包含领域列表、服务统计、热门接口）",
            inputSchema={
                "type": "object",
                "properties": {
                    "cluster_id": {
                        "type": "string",
                        "description": "集群ID"
                    }
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
                    "cluster_id": {
                        "type": "string",
                        "description": "集群ID"
                    }
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
                    "cluster_id": {
                        "type": "string",
                        "description": "集群ID"
                    },
                    "domain_code": {
                        "type": "string",
                        "description": "领域编码，如 trade/product/fulfillment"
                    }
                },
                "required": ["cluster_id", "domain_code"]
            }
        ),

        # ============ 服务查询 ============
        Tool(
            name="get_service_detail",
            description="获取服务详情（接口列表、依赖关系、配置项）",
            inputSchema={
                "type": "object",
                "properties": {
                    "service_id": {
                        "type": "string",
                        "description": "服务ID（仓库ID）"
                    }
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
                    "service_id": {
                        "type": "string",
                        "description": "服务ID"
                    },
                    "direction": {
                        "type": "string",
                        "enum": ["upstream", "downstream", "both"],
                        "default": "both",
                        "description": "依赖方向"
                    },
                    "depth": {
                        "type": "integer",
                        "default": 2,
                        "description": "依赖深度"
                    }
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
                    "cluster_id": {
                        "type": "string",
                        "description": "集群ID"
                    },
                    "keyword": {
                        "type": "string",
                        "description": "搜索关键词"
                    },
                    "version": {
                        "type": "string",
                        "description": "接口版本"
                    },
                    "limit": {
                        "type": "integer",
                        "default": 20,
                        "description": "返回数量限制"
                    }
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
                    "cluster_id": {
                        "type": "string",
                        "description": "集群ID"
                    },
                    "interface_name": {
                        "type": "string",
                        "description": "接口全限定名"
                    }
                },
                "required": ["cluster_id", "interface_name"]
            }
        ),
        Tool(
            name="get_interface_call_chain",
            description="获取接口的调用链路图",
            inputSchema={
                "type": "object",
                "properties": {
                    "interface_name": {
                        "type": "string",
                        "description": "接口全限定名"
                    },
                    "max_depth": {
                        "type": "integer",
                        "default": 5,
                        "description": "最大追踪深度"
                    }
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
                    "query": {
                        "type": "string",
                        "description": "搜索关键词"
                    },
                    "cluster_id": {
                        "type": "string",
                        "description": "集群ID（可选）"
                    },
                    "domain_code": {
                        "type": "string",
                        "description": "领域编码（可选）"
                    },
                    "doc_type": {
                        "type": "string",
                        "description": "文档类型（可选）"
                    },
                    "limit": {
                        "type": "integer",
                        "default": 10,
                        "description": "返回数量限制"
                    }
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
                    "cluster_id": {
                        "type": "string",
                        "description": "集群ID"
                    },
                    "target_service_id": {
                        "type": "string",
                        "description": "目标服务ID（可选）"
                    },
                    "context_level": {
                        "type": "string",
                        "enum": ["L1", "L2", "L3", "L4"],
                        "default": "L2",
                        "description": "上下文级别：L1全局索引/L2领域摘要/L3服务详情/L4完整扩展"
                    },
                    "max_size_kb": {
                        "type": "integer",
                        "default": 50,
                        "description": "最大上下文大小（KB）"
                    },
                    "format": {
                        "type": "string",
                        "enum": ["json", "markdown"],
                        "default": "markdown",
                        "description": "输出格式"
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
                    "cluster_id": {
                        "type": "string",
                        "description": "集群ID"
                    },
                    "requirement": {
                        "type": "string",
                        "description": "需求描述"
                    },
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
                    "service_id": {
                        "type": "string",
                        "description": "服务ID"
                    },
                    "interface_name": {
                        "type": "string",
                        "description": "接口全限定名"
                    },
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
                    "cluster_id": {
                        "type": "string",
                        "description": "集群ID"
                    },
                    "scope": {
                        "type": "string",
                        "enum": ["cluster", "domain", "service"],
                        "default": "cluster",
                        "description": "拓扑范围"
                    },
                    "scope_id": {
                        "type": "string",
                        "description": "领域ID或服务ID（scope为domain/service时必填）"
                    },
                    "format": {
                        "type": "string",
                        "enum": ["mermaid", "json"],
                        "default": "mermaid",
                        "description": "输出格式"
                    }
                },
                "required": ["cluster_id"]
            }
        ),

        # ============ 原有工具（兼容） ============
        Tool(
            name="list_services",
            description="列出指定仓库下的所有服务配置",
            inputSchema={
                "type": "object",
                "properties": {
                    "warehouse_id": {
                        "type": "string",
                        "description": "仓库ID"
                    }
                },
                "required": ["warehouse_id"]
            }
        ),
        Tool(
            name="get_service",
            description="获取指定服务的文档配置详情",
            inputSchema={
                "type": "object",
                "properties": {
                    "warehouse_id": {
                        "type": "string",
                        "description": "仓库ID"
                    },
                    "service_id": {
                        "type": "string",
                        "description": "服务ID"
                    }
                },
                "required": ["warehouse_id", "service_id"]
            }
        ),
        Tool(
            name="list_documents",
            description="列出指定仓库下的所有AI生成文档",
            inputSchema={
                "type": "object",
                "properties": {
                    "warehouse_id": {
                        "type": "string",
                        "description": "仓库ID"
                    },
                    "page": {
                        "type": "integer",
                        "description": "页码，从0开始",
                        "default": 0
                    },
                    "size": {
                        "type": "integer",
                        "description": "每页大小",
                        "default": 20
                    }
                },
                "required": ["warehouse_id"]
            }
        )
    ]


@app.call_tool()
async def call_tool(name: str, arguments: dict) -> list[TextContent]:
    """执行MCP工具调用"""
    async with httpx.AsyncClient(timeout=30.0) as client:
        try:
            result = await _dispatch_tool(client, name, arguments)
            return [TextContent(type="text", text=result)]
        except httpx.HTTPStatusError as e:
            error_msg = f"HTTP Error: {e.response.status_code} - {e.response.text}"
            return [TextContent(type="text", text=error_msg)]
        except Exception as e:
            return [TextContent(type="text", text=f"Error: {str(e)}")]


async def _dispatch_tool(client: httpx.AsyncClient, name: str, arguments: dict) -> str:
    """分发工具调用到对应的处理函数"""

    # ============ 集群管理 ============
    if name == "list_clusters":
        status = arguments.get("status")
        params = {"status": status} if status else {}
        response = await client.get(f"{API_BASE_URL}/clusters", params=params)
        return _format_response(response)

    elif name == "get_cluster_overview":
        cluster_id = arguments["cluster_id"]
        response = await client.get(
            f"{API_BASE_URL}/ai-context/clusters/{cluster_id}/global-index"
        )
        return _format_response(response)

    # ============ 领域查询 ============
    elif name == "list_domains":
        cluster_id = arguments["cluster_id"]
        response = await client.get(
            f"{API_BASE_URL}/clusters/{cluster_id}/domains"
        )
        return _format_response(response)

    elif name == "get_domain_services":
        cluster_id = arguments["cluster_id"]
        domain_code = arguments["domain_code"]
        response = await client.get(
            f"{API_BASE_URL}/ai-context/clusters/{cluster_id}/domains/{domain_code}"
        )
        return _format_response(response)

    # ============ 服务查询 ============
    elif name == "get_service_detail":
        service_id = arguments["service_id"]
        response = await client.get(
            f"{API_BASE_URL}/ai-context/services/{service_id}"
        )
        return _format_response(response)

    elif name == "get_service_dependencies":
        service_id = arguments["service_id"]
        direction = arguments.get("direction", "both")
        depth = arguments.get("depth", 2)
        response = await client.get(
            f"{API_BASE_URL}/clusters/services/{service_id}/dependencies",
            params={"direction": direction, "depth": depth}
        )
        return _format_response(response)

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
        return _format_response(response)

    elif name == "get_interface_detail":
        cluster_id = arguments["cluster_id"]
        interface_name = arguments["interface_name"]
        response = await client.get(
            f"{API_BASE_URL}/ai-context/clusters/{cluster_id}/interfaces",
            params={"interfaceName": interface_name}
        )
        return _format_response(response)

    elif name == "get_interface_call_chain":
        interface_name = arguments["interface_name"]
        max_depth = arguments.get("max_depth", 5)
        response = await client.get(
            f"{API_BASE_URL}/dubbo-interfaces/call-chain",
            params={"interfaceName": interface_name, "maxDepth": max_depth}
        )
        return _format_response(response)

    # ============ 文档查询 ============
    elif name == "search_documents":
        query = arguments["query"]
        params = {"q": query, "limit": arguments.get("limit", 10)}
        if arguments.get("cluster_id"):
            params["clusterId"] = arguments["cluster_id"]
        if arguments.get("domain_code"):
            params["domainCode"] = arguments["domain_code"]
        if arguments.get("doc_type"):
            params["docType"] = arguments["doc_type"]
        response = await client.get(f"{API_BASE_URL}/search/documents", params=params)
        return _format_response(response)

    elif name == "get_ai_context":
        cluster_id = arguments["cluster_id"]
        target_service_id = arguments.get("target_service_id")
        context_level = arguments.get("context_level", "L2")
        max_size_kb = arguments.get("max_size_kb", 50)
        fmt = arguments.get("format", "markdown")

        if fmt == "markdown":
            response = await client.get(
                f"{API_BASE_URL}/ai-context/clusters/{cluster_id}/markdown",
                params={
                    "targetServiceId": target_service_id,
                    "level": context_level
                }
            )
            return response.text
        else:
            response = await client.get(
                f"{API_BASE_URL}/ai-context/clusters/{cluster_id}",
                params={
                    "targetServiceId": target_service_id,
                    "level": context_level,
                    "maxSizeKb": max_size_kb,
                    "format": "JSON"
                }
            )
            return _format_response(response)

    # ============ 分析建议 ============
    elif name == "suggest_integration":
        cluster_id = arguments["cluster_id"]
        requirement = arguments["requirement"]
        context_services = arguments.get("context_services", [])
        response = await client.post(
            f"{API_BASE_URL}/ai-context/on-demand",
            json={
                "clusterId": cluster_id,
                "query": requirement,
                "hints": context_services
            }
        )
        return _format_response(response)

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
        return _format_response(response)

    # ============ 拓扑可视化 ============
    elif name == "get_topology_graph":
        cluster_id = arguments["cluster_id"]
        scope = arguments.get("scope", "cluster")
        scope_id = arguments.get("scope_id")
        fmt = arguments.get("format", "mermaid")
        response = await client.get(
            f"{API_BASE_URL}/clusters/{cluster_id}/topology",
            params={"scope": scope, "scopeId": scope_id, "format": fmt}
        )
        return _format_response(response)

    # ============ 原有工具（兼容） ============
    elif name == "list_services":
        warehouse_id = arguments["warehouse_id"]
        response = await client.get(
            f"{API_BASE_URL}/warehouses/{warehouse_id}/services"
        )
        return _format_response(response)

    elif name == "get_service":
        warehouse_id = arguments["warehouse_id"]
        service_id = arguments["service_id"]
        response = await client.get(
            f"{API_BASE_URL}/warehouses/{warehouse_id}/services/{service_id}"
        )
        return _format_response(response)

    elif name == "list_documents":
        warehouse_id = arguments["warehouse_id"]
        page = arguments.get("page", 0)
        size = arguments.get("size", 20)
        response = await client.get(
            f"{API_BASE_URL}/warehouses/{warehouse_id}/documents",
            params={"page": page, "size": size}
        )
        return _format_response(response)

    else:
        raise ValueError(f"Unknown tool: {name}")


def _format_response(response: httpx.Response) -> str:
    """格式化API响应"""
    response.raise_for_status()

    try:
        data = response.json()

        # 如果是分页数据，提取核心内容
        if isinstance(data, dict):
            if "data" in data:
                data = data["data"]
            elif "content" in data:
                # 分页响应
                data = {
                    "content": data["content"],
                    "totalElements": data.get("totalElements"),
                    "totalPages": data.get("totalPages")
                }

        return json.dumps(data, ensure_ascii=False, indent=2)
    except json.JSONDecodeError:
        return response.text


async def main():
    """启动MCP服务器"""
    from mcp.server.stdio import stdio_server
    async with stdio_server() as (read_stream, write_stream):
        await app.run(
            read_stream,
            write_stream,
            app.create_initialization_options()
        )


if __name__ == "__main__":
    asyncio.run(main())

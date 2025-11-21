#!/usr/bin/env python3
"""
OpenDeepWiki MCP Server - 文档查看服务的MCP封装

提供三个核心工具：
1. list_services - 列出仓库下的所有服务配置
2. get_service - 获取指定服务的文档配置
3. list_documents - 列出仓库下的所有文档

@author zhourui(V33215020)
@since 2025/11/21
"""

import asyncio
import httpx
from mcp.server import Server
from mcp.types import Tool, TextContent

API_BASE_URL = "http://localhost:18091/api/v1"

app = Server("opendeepwiki-document-server")

@app.list_tools()
async def list_tools() -> list[Tool]:
    return [
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
    async with httpx.AsyncClient() as client:
        if name == "list_services":
            warehouse_id = arguments["warehouse_id"]
            response = await client.get(
                f"{API_BASE_URL}/warehouses/{warehouse_id}/services"
            )
            response.raise_for_status()
            data = response.json()
            return [TextContent(type="text", text=str(data))]

        elif name == "get_service":
            warehouse_id = arguments["warehouse_id"]
            service_id = arguments["service_id"]
            response = await client.get(
                f"{API_BASE_URL}/warehouses/{warehouse_id}/services/{service_id}"
            )
            response.raise_for_status()
            data = response.json()
            return [TextContent(type="text", text=str(data))]

        elif name == "list_documents":
            warehouse_id = arguments["warehouse_id"]
            page = arguments.get("page", 0)
            size = arguments.get("size", 20)
            response = await client.get(
                f"{API_BASE_URL}/warehouses/{warehouse_id}/documents",
                params={"page": page, "size": size}
            )
            response.raise_for_status()
            data = response.json()
            return [TextContent(type="text", text=str(data))]

        else:
            raise ValueError(f"Unknown tool: {name}")

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

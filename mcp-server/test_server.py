#!/usr/bin/env python3
"""
MCP Server测试脚本

@author zhourui(V33215020)
@since 2025/11/21
"""

import asyncio
import json
from document_mcp_server import app

async def test_list_tools():
    print("=== 测试 list_tools ===")
    tools = await app._tool_manager.list_tools()
    for tool in tools:
        print(f"工具: {tool.name}")
        print(f"描述: {tool.description}")
        print(f"参数: {json.dumps(tool.inputSchema, indent=2, ensure_ascii=False)}")
        print()

async def main():
    await test_list_tools()
    print("✓ MCP Server配置正确")
    print("\n使用说明：")
    print("1. 确保OpenDeepWiki后端服务运行在 http://localhost:18091")
    print("2. 在Claude Desktop配置文件中添加此MCP服务")
    print("3. 重启Claude Desktop即可使用")

if __name__ == "__main__":
    asyncio.run(main())

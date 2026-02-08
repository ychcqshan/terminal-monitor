#!/bin/bash
# Python Agent 本地测试脚本

echo "=========================================="
echo "终端安全监控 Agent - 本地测试"
echo "=========================================="

# 检查依赖
echo ""
echo "[1] 检查Python依赖..."
python3 -c "import psutil; print(f'  psutil版本: {psutil.__version__}')" 2>/dev/null || echo "  错误: psutil未安装，请运行: pip install psutil"
python3 -c "import requests; print(f'  requests版本: {requests.__version__}')" 2>/dev/null || echo "  错误: requests未安装，请运行: pip install requests"

# 测试进程采集
echo ""
echo "[2] 测试进程采集..."
python3 -c "
import sys
sys.path.insert(0, 'agent')
from collectors.process import ProcessCollector
collector = ProcessCollector()
processes = collector.collect_simple()
print(f'  成功采集 {len(processes)} 个进程')
if processes:
    print(f'  示例进程: {processes[0][\"name\"]} (PID: {processes[0][\"pid\"]})')
" 2>/dev/null || echo "  错误: 请确保在项目根目录运行"

# 测试端口采集
echo ""
echo "[3] 测试端口采集..."
python3 -c "
import sys
sys.path.insert(0, 'agent')
from collectors.port import PortCollector
collector = PortCollector()
ports = collector.collect_listening_ports()
print(f'  成功采集 {len(ports)} 个监听端口')
if ports:
    print(f'  示例端口: {ports[0][\"port\"]}/{ports[0][\"protocol\"]} ({ports[0][\"process_name\"]})')
" 2>/dev/null || echo "  错误: 请确保在项目根目录运行"

# 测试配置加载
echo ""
echo "[4] 测试配置加载..."
python3 -c "
import sys
sys.path.insert(0, 'agent')
from config import load_config
config = load_config()
print(f'  服务器地址: {config.server_url}')
print(f'  采集间隔: {config.collect_interval}秒')
print(f'  心跳间隔: {config.heartbeat_interval}秒')
" 2>/dev/null || echo "  错误: 请确保在项目根目录运行"

# 测试数据库
echo ""
echo "[5] 测试SQLite数据库..."
python3 -c "
import sys
import os
sys.path.insert(0, 'agent')
from storage.db import AgentDatabase
os.makedirs('agent/data', exist_ok=True)
db = AgentDatabase('agent/data/test.db')
print('  数据库初始化成功')
print('  表列表: agents, processes, ports, data_queue')
" 2>/dev/null || echo "  错误: 请确保在项目根目录运行"

echo ""
echo "=========================================="
echo "测试完成!"
echo "=========================================="

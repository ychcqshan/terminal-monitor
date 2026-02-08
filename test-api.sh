#!/bin/bash
# API联调测试脚本

BASE_URL="http://localhost:8080"

echo "=========================================="
echo "终端安全监控系统 - API联调测试"
echo "=========================================="

# 测试1: 健康检查
echo ""
echo "[测试1] 健康检查"
echo "curl -X GET ${BASE_URL}/api/health"
curl -s -X GET "${BASE_URL}/api/health" | jq . || echo "请求失败，请确保后端服务已启动"
echo ""

# 测试2: 注册Agent
echo "[测试2] 注册Agent"
AGENT_ID="test-agent-001"
echo "curl -X POST ${BASE_URL}/api/agents/register \\
  -H 'Content-Type: application/json' \\
  -d '{\"agentId\": \"${AGENT_ID}\", \"name\": \"测试Agent\", \"platform\": \"Linux 5.4\", \"hostname\": \"test-server\", \"ipAddress\": \"192.168.1.100\"}'"

curl -s -X POST "${BASE_URL}/api/agents/register" \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": "'${AGENT_ID}'",
    "name": "测试Agent",
    "platform": "Linux 5.4",
    "hostname": "test-server",
    "ipAddress": "192.168.1.100"
  }' | jq .
echo ""

# 测试3: 发送心跳
echo "[测试3] 发送心跳"
echo "curl -X POST ${BASE_URL}/api/agents/${AGENT_ID}/heartbeat"
curl -s -X POST "${BASE_URL}/api/agents/${AGENT_ID}/heartbeat" \
  -H "Content-Type: application/json" \
  -d '{"status": "online"}' | jq .
echo ""

# 测试4: 获取Agent列表
echo "[测试4] 获取Agent列表"
echo "curl -X GET ${BASE_URL}/api/agents"
curl -s -X GET "${BASE_URL}/api/agents" | jq .
echo ""

# 测试5: 上报监控数据
echo "[测试5] 上报监控数据"
echo "curl -X POST ${BASE_URL}/api/agents/${AGENT_ID}/data"

curl -s -X POST "${BASE_URL}/api/agents/${AGENT_ID}/data" \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": "'$(date -Iseconds)'",
    "processes": [
      {
        "pid": 1234,
        "name": "nginx",
        "cpu_percent": 2.5,
        "memory_percent": 1.2,
        "status": "online"
      },
      {
        "pid": 5678,
        "name": "mysql",
        "cpu_percent": 5.0,
        "memory_percent": 15.5,
        "status": "online"
      }
    ],
    "ports": [
      {
        "port": 80,
        "protocol": "TCP",
        "status": "LISTEN",
        "pid": 1234,
        "process_name": "nginx"
      },
      {
        "port": 3306,
        "protocol": "TCP",
        "status": "LISTEN",
        "pid": 5678,
        "process_name": "mysql"
      }
    ]
  }' | jq .
echo ""

# 测试6: 获取Agent详情
echo "[测试6] 获取Agent详情"
echo "curl -X GET ${BASE_URL}/api/agents/${AGENT_ID}"
curl -s -X GET "${BASE_URL}/api/agents/${AGENT_ID}" | jq .
echo ""

# 测试7: 获取Agent进程信息
echo "[测试7] 获取Agent进程信息"
echo "curl -X GET ${BASE_URL}/api/agents/${AGENT_ID}/processes"
curl -s -X GET "${BASE_URL}/api/agents/${AGENT_ID}/processes" | jq .
echo ""

# 测试8: 获取Agent端口信息
echo "[测试8] 获取Agent端口信息"
echo "curl -X GET ${BASE_URL}/api/agents/${AGENT_ID}/ports"
curl -s -X GET "${BASE_URL}/api/agents/${AGENT_ID}/ports" | jq .
echo ""

# 测试9: 获取系统状态
echo "[测试9] 获取系统状态统计"
echo "curl -X GET ${BASE_URL}/api/agents/status"
curl -s -X GET "${BASE_URL}/api/agents/status" | jq .
echo ""

echo "=========================================="
echo "测试完成!"
echo "=========================================="

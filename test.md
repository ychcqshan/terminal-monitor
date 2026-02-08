curl -X POST http://172.31.208.1:8080/api/agents/test-001/heartbeat \
  -H "Content-Type: application/json" \
  -d '{"status": "online"}'

  curl -X POST http://172.31.208.1:8080/api/agents/register \
  -H "Content-Type: application/json" \
  -d '{"agentId": "test-001", "name": "测试", "platform": "Win10", "hostname": "testpc", "ipAddress": "192.168.1.1"}'
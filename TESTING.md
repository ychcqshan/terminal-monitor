# 联调测试指南

本文档提供终端安全监控系统的完整联调测试步骤。

## 环境准备

### 1. 启动MySQL数据库

```bash
# 使用Docker启动MySQL
docker run -d --name terminal-monitor-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=password \
  -e MYSQL_DATABASE=terminal_monitor \
  mysql:8.0

docker run -d --name terminal-monitor-mysql   -p 3306:3306   -e MYSQL_ROOT_PASSWORD=password   -e MYSQL_DATABASE=terminal_monitor   mysql:8.0
# 或者使用docker-compose
docker-compose up -d mysql
```

### 2. 启动后端服务

```bash
cd backend

# 方式一：使用Maven启动（开发模式）
./mvnw spring-boot:run

# 方式二：先打包再运行
./mvnw clean package -DskipTests
java -jar target/terminal-monitor-backend-1.0.0.jar
```

后端服务启动后，API将运行在 `http://localhost:8080`

### 3. 启动前端服务

```bash
cd web

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

前端服务启动后，Web界面将运行在 `http://localhost:3000`

### 4. 启动Agent

```bash
cd agent

# 安装依赖
pip install -r requirements.txt

# 启动Agent（自动注册到后端）
python main.py --server-url http://localhost:8080

# 或者使用配置文件
python main.py config.json
```

## API测试

### 自动化测试

```bash
# 测试后端API
chmod +x test-api.sh
./test-api.sh

# 测试Agent模块
chmod +x test-agent.sh
./test-agent.sh
```

### 手动API测试

#### 健康检查
```bash
curl http://localhost:8080/api/health
```

预期响应：
```json
{
  "status": "ok",
  "timestamp": 1234567890
}
```

#### 注册Agent
```bash
curl -X POST http://localhost:8080/api/agents/register \
  -H "Content-Type: application/json" \
  -d '{
    "agentId": "agent-001",
    "name": "测试Agent",
    "platform": "Linux 5.4",
    "hostname": "test-server",
    "ipAddress": "192.168.1.100"
  }'
```

#### 发送心跳
```bash
curl -X POST http://localhost:8080/api/agents/agent-001/heartbeat \
  -H "Content-Type: application/json" \
  -d '{"status": "online"}'
```

#### 上报监控数据
```bash
curl -X POST http://localhost:8080/api/agents/agent-001/data \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": "2024-01-01T12:00:00",
    "processes": [
      {"pid": 1234, "name": "nginx", "cpu_percent": 2.5, "memory_percent": 1.2, "status": "online"},
      {"pid": 5678, "name": "mysql", "cpu_percent": 5.0, "memory_percent": 15.5, "status": "online"}
    ],
    "ports": [
      {"port": 80, "protocol": "TCP", "status": "LISTEN", "pid": 1234, "process_name": "nginx"},
      {"port": 3306, "protocol": "TCP", "status": "LISTEN", "pid": 5678, "process_name": "mysql"}
    ]
  }'
```

#### 获取Agent列表
```bash
curl http://localhost:8080/api/agents
```

#### 获取Agent详情
```bash
curl http://localhost:8080/api/agents/agent-001
```

#### 获取进程信息
```bash
curl http://localhost:8080/api/agents/agent-001/processes
```

#### 获取端口信息
```bash
curl http://localhost:8080/api/agents/agent-001/ports
```

#### 获取系统状态
```bash
curl http://localhost:8080/api/agents/status
```

## Docker部署测试

### 一键启动所有服务

```bash
cd terminal-monitor
docker-compose up -d
```

### 验证服务状态

```bash
# 检查容器状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 检查后端健康
curl http://localhost:8080/api/health

# 检查前端
curl http://localhost:3000
```

### 服务端口映射

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | 数据库 |
| Backend | 8080 | API服务 |
| Web | 3000 | Web界面 |

## 常见问题排查

### 1. 后端启动失败

**问题**: 数据库连接失败
```
Connection to mysql failed
```
**解决**:
- 确认MySQL容器已启动: `docker ps`
- 检查数据库配置: `application.yml`
- 确认数据库已创建: `SHOW DATABASES`

**问题**: 端口被占用
```
Port 8080 already in use
```
**解决**:
- 修改端口或停止占用进程: `lsof -i :8080`

### 2. 前端无法连接后端

**问题**: CORS错误或连接拒绝
**解决**:
- 检查后端CORS配置: `@CrossOrigin(origins = "*")`
- 检查代理配置: `vite.config.js`

### 3. Agent无法注册

**问题**: 400错误或连接超时
**解决**:
- 确认后端服务正在运行
- 检查Agent配置中的服务器地址
- 查看Agent日志

### 4. 数据未显示

**问题**: Web界面显示空数据
**解决**:
- 确认Agent正在运行并上报数据
- 检查数据库是否有数据: `SELECT * FROM agents`
- 查看后端日志

## 监控命令

### 查看实时日志

```bash
# 后端日志
docker-compose logs -f backend

# Agent日志（本地运行）
tail -f agent/logs/agent.log
```

### 数据库查询

```bash
# 进入MySQL容器
docker exec -it terminal-monitor-mysql mysql -uroot -ppassword

# 查询Agent列表
SELECT * FROM terminal_monitor.agents;

# 查询进程数据
SELECT * FROM terminal_monitor.processes LIMIT 10;

# 查询端口数据
SELECT * FROM terminal_monitor.ports LIMIT 10;
```

## 性能测试

### 使用wrk进行压力测试

```bash
# 测试健康检查接口
wrk -t4 -c100 -d30s http://localhost:8080/api/health

# 测试Agent列表接口
wrk -t4 -c100 -d30s http://localhost:8080/api/agents
```

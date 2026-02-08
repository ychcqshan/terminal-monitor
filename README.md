# 终端安全监控轻量化工具

## 项目概述

轻量级终端安全监控解决方案，支持Linux/Windows双平台，包含：
- **Agent**: Python采集端（进程+端口）
- **Backend**: Java SpringBoot后端 + MySQL
- **Web**: Vue3管理界面

## 快速启动

### 方式一：Docker部署（推荐）

```bash
# 克隆项目后
cd terminal-monitor

# 一键启动所有服务
docker-compose up -d

# 服务访问
# Web界面: http://localhost:3000
# 后端API: http://localhost:8080/api
```

### 方式二：本地开发

#### 1. 启动MySQL
```bash
docker run -d --name mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=password \
  -e MYSQL_DATABASE=terminal_monitor \
  mysql:8.0
```

#### 2. 启动后端
```bash
cd backend
./mvnw spring-boot:run
```

#### 3. 启动前端
```bash
cd web
npm install
npm run dev
```

#### 4. 启动Agent
```bash
cd agent
pip install -r requirements.txt
python main.py --server-url http://localhost:8080
```

## 功能特性

| 模块 | 功能 |
|------|------|
| Agent | 进程采集、端口采集、心跳上报、本地存储 |
| 后端 | Agent注册、心跳、数据接收、列表查询 |
| Web | Dashboard总览、Agent管理、详情查看 |

## 配置说明

### Agent配置
```yaml
# agent/config.json 或环境变量
{
  "server_url": "http://localhost:8080",
  "collect_interval": 60,
  "heartbeat_interval": 30,
  "db_path": "./data/agent.db"
}
```

### 后端配置
```yaml
# backend/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/terminal_monitor
    username: root
    password: password
```

## API接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/health` | GET | 健康检查 |
| `/api/agents/register` | POST | Agent注册 |
| `/api/agents/{id}/heartbeat` | POST | 心跳 |
| `/api/agents/{id}/data` | POST | 数据上报 |
| `/api/agents` | GET | Agent列表 |
| `/api/agents/{id}` | GET | Agent详情 |

## 技术栈

- **Agent**: Python 3.8+ / psutil / requests
- **后端**: Java 17 / SpringBoot 3.2 / MySQL
- **前端**: Vue 3 / Element Plus / Vite

## 目录结构

```
terminal-monitor/
├── agent/           # Python Agent
│   ├── collectors/  # 数据采集
│   ├── storage/     # SQLite存储
│   ├── reporter/    # 数据上报
│   └── main.py      # 入口
├── backend/          # Java后端
│   └── src/main/java/com/monitor/
│       ├── controller/
│       ├── entity/
│       ├── repository/
│       └── service/
├── web/             # Vue3前端
│   └── src/
│       ├── api/
│       ├── views/
│       └── router/
└── docker-compose.yml
```

## License

MIT

"""
终端安全监控Agent主程序
"""

import sys
import platform
import uuid
import socket
import time
import threading
from datetime import datetime
from typing import Dict, Optional
from config import load_config
from collectors.process import ProcessCollector
from collectors.port import PortCollector
from storage.db import AgentDatabase
from reporter.uploader import DataReporter
from utils.logger import setup_logger


logger = setup_logger()


class TerminalMonitorAgent:
    """终端监控Agent"""

    def __init__(self, config_file: Optional[str] = None):
        self.config = load_config(config_file)
        self.agent_id = self.config.agent_id or str(uuid.uuid4())
        self.agent_name = self.config.agent_name or socket.gethostname()
        self.platform_name = self._get_platform()
        self.hostname = socket.gethostname()
        self.ip_address = self._get_ip_address()

        self.process_collector = ProcessCollector()
        self.port_collector = PortCollector()
        self.db = AgentDatabase(self.config.db_path)
        self.reporter = DataReporter(
            self.config.server_url,
            self.config.retry_times,
            self.config.retry_interval
        )

        self.running = False
        self.collect_thread = None
        self.heartbeat_thread = None

    def _get_platform(self) -> str:
        """获取平台信息"""
        system = platform.system()
        release = platform.release()
        platform_info = f"{system} {release}"
        logger.info(f"平台信息: {platform_info}")
        return platform_info

    def _get_ip_address(self) -> str:
        """获取IP地址（优先获取外网IP）"""
        ip_methods = [
            ("通过UDP Socket", self._get_ip_socket),
            ("通过HTTP请求", self._get_ip_http),
            ("通过主机名解析", self._get_ip_hostname)
        ]

        for method_name, method in ip_methods:
            try:
                ip = method()
                if ip and ip not in ("127.0.0.1", "localhost", None):
                    logger.info(f"[IP地址] {method_name}获取成功: {ip}")
                    return ip
            except Exception as e:
                logger.warning(f"[IP地址] {method_name}获取失败: {e}")
                continue

        logger.warning("[IP地址] 所有方法均失败，使用默认地址 127.0.0.1")
        return "127.0.0.1"

    def _get_ip_socket(self) -> Optional[str]:
        """通过Socket获取IP"""
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.settimeout(3)
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except Exception:
            return None

    def _get_ip_http(self) -> Optional[str]:
        """通过HTTP请求获取外网IP"""
        try:
            import urllib.request
            response = urllib.request.urlopen("https://api.ipify.org", timeout=3)
            return response.read().decode('utf-8')
        except Exception:
            return None

    def _get_ip_hostname(self) -> Optional[str]:
        """通过主机名解析获取IP"""
        try:
            hostname = socket.gethostname()
            return socket.gethostbyname(hostname)
        except Exception:
            return None

    def initialize(self) -> bool:
        """初始化Agent"""
        logger.info("正在初始化Agent...")

        try:
            self.db.save_agent(
                self.agent_id,
                self.agent_name,
                self.platform_name,
                self.hostname,
                self.ip_address
            )

            if self._register_to_server():
                self.db.update_agent_status(self.agent_id, "online")
                logger.info(f"Agent初始化完成，ID: {self.agent_id}")
                return True
            else:
                logger.warning("服务器注册失败，将使用离线模式")
                self.db.update_agent_status(self.agent_id, "offline")
                return True

        except Exception as e:
            logger.error(f"Agent初始化失败: {e}")
            return False

    def _register_to_server(self) -> bool:
        """注册到服务器"""
        agent_info = {
            "agentId": self.agent_id,
            "name": self.agent_name,
            "platform": self.platform_name,
            "hostname": self.hostname,
            "ipAddress": self.ip_address
        }

        result = self.reporter.register_agent(agent_info)
        if result:
            logger.info("Agent注册成功")
            return True
        else:
            logger.warning("Agent注册失败")
            return False

    def start(self):
        """启动Agent"""
        if not self.initialize():
            logger.error("初始化失败，退出")
            return

        self.running = True

        self.collect_thread = threading.Thread(
            target=self._collect_loop,
            daemon=True
        )
        self.collect_thread.start()

        self.heartbeat_thread = threading.Thread(
            target=self._heartbeat_loop,
            daemon=True
        )
        self.heartbeat_thread.start()

        logger.info("Agent已启动")

        try:
            while self.running:
                time.sleep(1)
        except KeyboardInterrupt:
            self.stop()

    def _collect_loop(self):
        """数据采集循环"""
        logger.info(f"数据采集循环已启动，间隔: {self.config.collect_interval}秒")

        while self.running:
            try:
                logger.info("[采集循环] 开始执行数据采集...")
                self._collect_and_report()
            except Exception as e:
                logger.error(f"[采集循环] 采集异常: {e}")

            logger.info(f"[采集循环] 等待 {self.config.collect_interval} 秒后进行下一次采集")
            time.sleep(self.config.collect_interval)

    def _collect_and_report(self):
        """采集并上报数据"""
        timestamp = datetime.now().isoformat()
        logger.info(f"[数据采集] 开始采集，时间戳: {timestamp}")

        processes = self.process_collector.collect_simple()
        ports = self.port_collector.collect_listening_ports()

        logger.info(f"[数据采集] ========== 进程数据 ==========")
        for i, proc in enumerate(processes[:10], 1):
            logger.info(f"  [{i:3d}] PID:{proc.get('pid'):<6} 名称:{proc.get('name'):<20} CPU:{proc.get('cpu_percent'):>5.1f}% 内存:{proc.get('memory_percent'):>5.1f}%")
        if len(processes) > 10:
            logger.info(f"  ... 共 {len(processes)} 个进程，显示前10个")
        logger.info(f"[数据采集] ========== 端口数据 ==========")
        for i, port in enumerate(ports[:10], 1):
            logger.info(f"  [{i:3d}] 端口:{port.get('port'):<5} 协议:{port.get('protocol'):<5} 状态:{port.get('status'):<10} 进程:{port.get('process_name'):<20}")
        if len(ports) > 10:
            logger.info(f"  ... 共 {len(ports)} 个端口，显示前10个")
        logger.info(f"[数据采集] =================================")

        local_data = {
            "timestamp": timestamp,
            "processes": processes,
            "ports": ports
        }

        local_data = {
            "timestamp": timestamp,
            "processes": processes,
            "ports": ports
        }

        self.db.save_processes(self.agent_id, processes)
        self.db.save_ports(self.agent_id, ports)
        self.db.save_agent(self.agent_id, self.agent_name,
                          self.platform_name, self.hostname, self.ip_address)

        if self.reporter.send_data(self.agent_id, "monitor_data", local_data):
            logger.info("[数据采集] 数据上报成功")
        else:
            logger.warning("[数据采集] 数据上报失败，已本地存储")
            self.db.queue_data("monitor_data", local_data)

    def _heartbeat_loop(self):
        """心跳循环"""
        logger.info(f"心跳已启动，间隔: {self.config.heartbeat_interval}秒")

        while self.running:
            try:
                if self.reporter.send_heartbeat(self.agent_id, "online"):
                    self.db.update_agent_status(self.agent_id, "online")
                    logger.debug("[心跳] 心跳成功，在线状态")
                else:
                    self.db.update_agent_status(self.agent_id, "offline")
                    logger.warning("[心跳] 心跳失败，离线状态")
            except Exception as e:
                logger.error(f"[心跳] 心跳异常: {e}")
                self.db.update_agent_status(self.agent_id, "offline")

            time.sleep(self.config.heartbeat_interval)

    def stop(self):
        """停止Agent"""
        logger.info("正在停止Agent...")
        self.running = False

        if self.collect_thread:
            self.collect_thread.join(timeout=5)
        if self.heartbeat_thread:
            self.heartbeat_thread.join(timeout=5)

        self.db.update_agent_status(self.agent_id, "offline")
        logger.info("Agent已停止")

    def get_status(self) -> Dict:
        """获取Agent状态"""
        agent = self.db.get_agent(self.agent_id)
        if agent:
            status = {
                "id": agent.get("id"),
                "name": agent.get("name"),
                "status": agent.get("status"),
                "platform": agent.get("platform"),
                "hostname": agent.get("hostname"),
                "ip_address": agent.get("ip_address")
            }
            logger.info(f"[状态查询] Agent状态: {status}")
            return status
        else:
            logger.warning("[状态查询] 未找到Agent状态")
            return {}


def main():
    """主入口"""
    config_file: Optional[str] = None
    if len(sys.argv) > 1:
        config_file = sys.argv[1]

    agent = TerminalMonitorAgent(config_file)

    try:
        agent.start()
    except KeyboardInterrupt:
        agent.stop()


if __name__ == "__main__":
    main()

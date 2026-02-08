"""
端口信息采集模块
"""

import platform
import psutil
from typing import List, Dict
from utils.logger import setup_logger


logger = setup_logger()


class PortCollector:
    """端口信息采集器"""

    def __init__(self):
        self.system = platform.system()

    def collect(self) -> List[Dict]:
        """采集端口信息"""
        ports = []

        try:
            connections = psutil.net_connections(kind='inet')

            for conn in connections:
                try:
                    process = None
                    if conn.pid:
                        try:
                            process = psutil.Process(conn.pid)
                        except (psutil.NoSuchProcess, psutil.AccessDenied):
                            pass

                    port_info = {
                        "port": conn.laddr.port if conn.laddr else None,
                        "protocol": "TCP" if conn.type == 1 else "UDP",
                        "status": self._map_status(conn.status),
                        "pid": conn.pid,
                        "process_name": process.name() if process else None,
                        "local_address": f"{conn.laddr.ip}:{conn.laddr.port}" if conn.laddr else None,
                        "remote_address": f"{conn.raddr.ip}:{conn.raddr.port}" if conn.raddr else None
                    }
                    ports.append(port_info)
                except Exception:
                    continue

            logger.info(f"采集到 {len(ports)} 个端口连接")

        except Exception as e:
            logger.error(f"端口采集失败: {e}")

        return ports

    def collect_listening_ports(self) -> List[Dict]:
        """仅采集监听端口"""
        ports = []
        try:
            for conn in psutil.net_connections(kind='inet'):
                if conn.status == 'LISTEN':
                    try:
                        process = None
                        if conn.pid:
                            try:
                                process = psutil.Process(conn.pid)
                            except (psutil.NoSuchProcess, psutil.AccessDenied):
                                pass

                        port_info = {
                            "port": conn.laddr.port if conn.laddr else None,
                            "protocol": "TCP" if conn.type == 1 else "UDP",
                            "status": "LISTEN",
                            "pid": conn.pid,
                            "process_name": process.name() if process else None
                        }
                        ports.append(port_info)
                    except Exception:
                        continue

            logger.debug(f"采集到 {len(ports)} 个监听端口")
        except Exception as e:
            logger.error(f"监听端口采集失败: {e}")

        return ports

    def _map_status(self, status: str) -> str:
        """映射连接状态"""
        status_map = {
            'ESTABLISHED': 'established',
            'LISTEN': 'listening',
            'SYN_SENT': 'syn_sent',
            'SYN_RECEIVED': 'syn_received',
            'FIN_WAIT_1': 'fin_wait_1',
            'FIN_WAIT_2': 'fin_wait_2',
            'TIME_WAIT': 'time_wait',
            'CLOSE': 'closed',
            'CLOSE_WAIT': 'close_wait',
            'LAST_ACK': 'last_ack'
        }
        return status_map.get(status, status.lower())

    def get_ports_by_process(self, process_name: str) -> List[Dict]:
        """获取指定进程打开的端口"""
        ports = self.collect()
        return [p for p in ports if p.get('process_name') == process_name]

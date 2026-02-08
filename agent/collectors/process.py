"""
进程信息采集模块
"""

import platform
import psutil
from datetime import datetime
from typing import List, Dict
from utils.logger import setup_logger


logger = setup_logger()


class ProcessCollector:
    """进程信息采集器"""

    def __init__(self):
        self.system = platform.system()

    def collect(self) -> List[Dict]:
        """采集进程信息"""
        processes = []
        try:
            for proc in psutil.process_iter(['pid', 'name', 'cpu_percent',
                                              'memory_percent', 'status',
                                              'create_time', 'username']):
                try:
                    info = proc.info
                    create_time = None
                    if info.get('create_time'):
                        create_time = datetime.fromtimestamp(
                            info['create_time']
                        ).isoformat()

                    process = {
                        "pid": info.get('pid'),
                        "name": info.get('name'),
                        "cpu_percent": info.get('cpu_percent'),
                        "memory_percent": info.get('memory_percent'),
                        "status": info.get('status'),
                        "create_time": create_time,
                        "username": info.get('username')
                    }
                    processes.append(process)
                except (psutil.NoSuchProcess, psutil.AccessDenied,
                        psutil.ZombieProcess):
                    continue

            logger.info(f"采集到 {len(processes)} 个进程信息")
        except Exception as e:
            logger.error(f"进程采集失败: {e}")

        return processes

    def collect_simple(self) -> List[Dict]:
        """轻量级进程采集（仅核心字段）"""
        processes = []
        try:
            for proc in psutil.process_iter(['pid', 'name', 'cpu_percent',
                                              'memory_percent', 'status']):
                try:
                    info = proc.info
                    process = {
                        "pid": info.get('pid'),
                        "name": info.get('name'),
                        "cpu_percent": round(info.get('cpu_percent', 0), 2),
                        "memory_percent": round(info.get('memory_percent', 0), 2),
                        "status": info.get('status', 'unknown')
                    }
                    processes.append(process)
                except (psutil.NoSuchProcess, psutil.AccessDenied):
                    continue

            logger.debug(f"轻量采集到 {len(processes)} 个进程")
        except Exception as e:
            logger.error(f"轻量进程采集失败: {e}")

        return processes

    def get_top_processes(self, by: str = 'cpu', limit: int = 10) -> List[Dict]:
        """获取Top进程"""
        processes = self.collect_simple()
        if by == 'cpu':
            processes.sort(key=lambda x: x.get('cpu_percent', 0), reverse=True)
        else:
            processes.sort(key=lambda x: x.get('memory_percent', 0), reverse=True)
        return processes[:limit]

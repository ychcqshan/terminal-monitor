"""
数据上报模块
"""

import json
import time
import requests
from typing import Dict, List, Optional
from datetime import datetime
from utils.logger import setup_logger


logger = setup_logger()


class DataReporter:
    """数据上报器"""

    def __init__(self, server_url: str, retry_times: int = 3,
                 retry_interval: int = 5):
        self.server_url = server_url.rstrip('/')
        self.retry_times = retry_times
        self.retry_interval = retry_interval
        self.session = requests.Session()

    def register_agent(self, agent_info: Dict) -> Optional[Dict]:
        """注册Agent"""
        url = f"{self.server_url}/api/agents/register"
        return self._post_with_retry(url, agent_info)

    def send_heartbeat(self, agent_id: str, status: str = "online") -> bool:
        """发送心跳"""
        url = f"{self.server_url}/api/agents/{agent_id}/heartbeat"
        data = {
            "status": status,
            "timestamp": datetime.now().isoformat()
        }
        result = self._post_with_retry(url, data)
        return result is not None

    def send_data(self, agent_id: str, data_type: str, data: Dict) -> bool:
        """上报数据"""
        url = f"{self.server_url}/api/agents/{agent_id}/data"
        payload = {
            "data_type": data_type,
            "data": data,
            "timestamp": datetime.now().isoformat()
        }
        print(f"DEBUG: Payload to be sent: {payload}") # <--- 在这里添加打印语句
        result = self._post_with_retry(url, payload)
        return result is not None

    def send_batch_data(self, agent_id: str, data_type: str,
                       data_list: List[Dict]) -> bool:
        """批量上报数据"""
        url = f"{self.server_url}/api/agents/{agent_id}/data/batch"
        payload = {
            "data_type": data_type,
            "data_list": data_list,
            "timestamp": datetime.now().isoformat()
        }
        result = self._post_with_retry(url, payload)
        return result is not None

    def check_server_health(self) -> bool:
        """检查服务器健康状态"""
        try:
            url = f"{self.server_url}/api/health"
            response = self.session.get(url, timeout=5)
            return response.status_code == 200
        except Exception as e:
            logger.warning(f"服务器健康检查失败: {e}")
            return False

    def _post_with_retry(self, url: str, data: Dict,
                          max_retries: Optional[int] = None) -> Optional[Dict]:
        """带重试的POST请求"""
        if max_retries is None:
            max_retries = self.retry_times

        for attempt in range(max_retries):
            try:
                response = self.session.post(
                    url,
                    json=data,
                    headers={'Content-Type': 'application/json'},
                    timeout=30
                )

                if response.status_code == 200:
                    try:
                        return response.json()
                    except json.JSONDecodeError:
                        return {"status": "success"}
                elif response.status_code == 400:
                    logger.error(f"请求参数错误: {response.text}")
                    return None
                elif response.status_code == 404:
                    logger.error(f"资源不存在: {url}")
                    return None
                else:
                    logger.warning(f"请求失败 ({response.status_code}): {response.text}")

            except requests.exceptions.Timeout:
                logger.warning(f"请求超时 (尝试 {attempt + 1}/{max_retries})")
            except requests.exceptions.ConnectionError as e:
                logger.warning(f"连接失败 (尝试 {attempt + 1}/{max_retries}): {e}")
            except Exception as e:
                logger.error(f"上报异常: {e}")

            if attempt < max_retries - 1:
                time.sleep(self.retry_interval)

        logger.error(f"数据上报失败，已重试 {max_retries} 次")
        return None

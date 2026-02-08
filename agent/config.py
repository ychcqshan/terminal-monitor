"""
配置管理模块
"""

import os
import json
from pathlib import Path
from typing import Optional


class Config:
    """Agent配置类"""

    def __init__(self, config_file: Optional[str] = None):
        self._config = {}
        self._load_default()
        if config_file and Path(config_file).exists():
            self._load_file(config_file)
        else:
            self._load_env()

    def _load_default(self):
        """加载默认配置"""
        self._config = {
            "server_url": "http://localhost:8080",
            "agent_id": "",
            "agent_name": "",
            "collect_interval": 60,
            "heartbeat_interval": 30,
            "retry_times": 3,
            "retry_interval": 5,
            "log_level": "INFO",
            "db_path": "./data/agent.db",
            "platform": "auto"
        }

    def _load_file(self, config_file: str):
        """从JSON文件加载配置"""
        try:
            with open(config_file, 'r', encoding='utf-8') as f:
                user_config = json.load(f)
                self._config.update(user_config)
        except Exception as e:
            print(f"[WARN] 加载配置文件失败: {e}")

    def _load_env(self):
        """从环境变量加载配置"""
        env_mappings = {
            "SERVER_URL": ("server_url", str),
            "AGENT_ID": ("agent_id", str),
            "AGENT_NAME": ("agent_name", str),
            "COLLECT_INTERVAL": ("collect_interval", int),
            "HEARTBEAT_INTERVAL": ("heartbeat_interval", int),
            "RETRY_TIMES": ("retry_times", int),
            "RETRY_INTERVAL": ("retry_interval", int),
            "LOG_LEVEL": ("log_level", str),
            "DB_PATH": ("db_path", str),
            "PLATFORM": ("platform", str)
        }

        for env_key, (config_key, cast_type) in env_mappings.items():
            env_value = os.environ.get(env_key)
            if env_value is not None:
                try:
                    self._config[config_key] = cast_type(env_value)
                except (ValueError, TypeError):
                    pass

    @property
    def server_url(self) -> str:
        return self._config["server_url"]

    @property
    def agent_id(self) -> str:
        return self._config["agent_id"]

    @property
    def agent_name(self) -> str:
        return self._config["agent_name"]

    @property
    def collect_interval(self) -> int:
        return self._config["collect_interval"]

    @property
    def heartbeat_interval(self) -> int:
        return self._config["heartbeat_interval"]

    @property
    def retry_times(self) -> int:
        return self._config["retry_times"]

    @property
    def retry_interval(self) -> int:
        return self._config["retry_interval"]

    @property
    def log_level(self) -> str:
        return self._config["log_level"]

    @property
    def db_path(self) -> str:
        return self._config["db_path"]

    @property
    def platform(self) -> str:
        return self._config["platform"]

    def get(self, key: str, default=None):
        return self._config.get(key, default)

    def set(self, key: str, value):
        self._config[key] = value


def load_config(config_file: Optional[str] = None) -> Config:
    """加载配置"""
    return Config(config_file)

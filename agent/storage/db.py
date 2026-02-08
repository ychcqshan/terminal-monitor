"""
SQLite本地存储模块
"""

import sqlite3
import json
from pathlib import Path
from datetime import datetime
from typing import List, Dict, Optional


class AgentDatabase:
    """Agent SQLite数据库"""

    def __init__(self, db_path: str = "./data/agent.db"):
        self.db_path = db_path
        self._ensure_db()

    def _ensure_db(self):
        """确保数据库目录存在"""
        db_file = Path(self.db_path)
        db_file.parent.mkdir(parents=True, exist_ok=True)
        self._init_tables()

    def _get_connection(self) -> sqlite3.Connection:
        return sqlite3.connect(self.db_path)

    def _init_tables(self):
        """初始化表结构"""
        conn = self._get_connection()
        cursor = conn.cursor()

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS agents (
                id TEXT PRIMARY KEY,
                name TEXT,
                platform TEXT,
                hostname TEXT,
                ip_address TEXT,
                status TEXT DEFAULT 'offline',
                created_at TEXT,
                updated_at TEXT
            )
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS processes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                agent_id TEXT,
                pid INTEGER,
                name TEXT,
                cpu_percent REAL,
                memory_percent REAL,
                status TEXT,
                create_time TEXT,
                collected_at TEXT,
                FOREIGN KEY (agent_id) REFERENCES agents(id)
            )
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS ports (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                agent_id TEXT,
                port INTEGER,
                protocol TEXT,
                status TEXT,
                pid INTEGER,
                process_name TEXT,
                collected_at TEXT,
                FOREIGN KEY (agent_id) REFERENCES agents(id)
            )
        ''')

        cursor.execute('''
            CREATE TABLE IF NOT EXISTS data_queue (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                data_type TEXT,
                data TEXT,
                retry_count INTEGER DEFAULT 0,
                created_at TEXT
            )
        ''')

        conn.commit()
        conn.close()

    def save_agent(self, agent_id: str, name: str, platform: str,
                   hostname: str, ip_address: str) -> bool:
        """保存Agent信息"""
        conn = self._get_connection()
        cursor = conn.cursor()
        now = datetime.now().isoformat()

        cursor.execute('''
            INSERT OR REPLACE INTO agents (id, name, platform, hostname, ip_address, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
        ''', (agent_id, name, platform, hostname, ip_address, now))

        conn.commit()
        conn.close()
        return True

    def update_agent_status(self, agent_id: str, status: str) -> bool:
        """更新Agent状态"""
        conn = self._get_connection()
        cursor = conn.cursor()
        now = datetime.now().isoformat()

        cursor.execute('''
            UPDATE agents SET status = ?, updated_at = ? WHERE id = ?
        ''', (status, now, agent_id))

        conn.commit()
        conn.close()
        return True

    def get_agent(self, agent_id: str) -> Optional[Dict]:
        """获取Agent信息"""
        conn = self._get_connection()
        cursor = conn.cursor()

        cursor.execute('SELECT * FROM agents WHERE id = ?', (agent_id,))
        row = cursor.fetchone()

        conn.close()
        if row:
            return {
                "id": row[0],
                "name": row[1],
                "platform": row[2],
                "hostname": row[3],
                "ip_address": row[4],
                "status": row[5],
                "created_at": row[6],
                "updated_at": row[7]
            }
        return None

    def save_processes(self, agent_id: str, processes: List[Dict]) -> bool:
        """保存进程信息"""
        conn = self._get_connection()
        cursor = conn.cursor()
        now = datetime.now().isoformat()

        for proc in processes:
            cursor.execute('''
                INSERT INTO processes (agent_id, pid, name, cpu_percent, memory_percent, status, create_time, collected_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ''', (agent_id, proc.get("pid"), proc.get("name"),
                  proc.get("cpu_percent"), proc.get("memory_percent"),
                  proc.get("status"), proc.get("create_time"), now))

        conn.commit()
        conn.close()
        return True

    def save_ports(self, agent_id: str, ports: List[Dict]) -> bool:
        """保存端口信息"""
        conn = self._get_connection()
        cursor = conn.cursor()
        now = datetime.now().isoformat()

        for port in ports:
            cursor.execute('''
                INSERT INTO ports (agent_id, port, protocol, status, pid, process_name, collected_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            ''', (agent_id, port.get("port"), port.get("protocol"),
                  port.get("status"), port.get("pid"),
                  port.get("process_name"), now))

        conn.commit()
        conn.close()
        return True

    def get_processes(self, agent_id: str, limit: int = 100) -> List[Dict]:
        """获取进程信息"""
        conn = self._get_connection()
        cursor = conn.cursor()

        cursor.execute('''
            SELECT * FROM processes WHERE agent_id = ? ORDER BY collected_at DESC LIMIT ?
        ''', (agent_id, limit))

        rows = cursor.fetchall()
        conn.close()

        return [
            {
                "id": row[0],
                "agent_id": row[1],
                "pid": row[2],
                "name": row[3],
                "cpu_percent": row[4],
                "memory_percent": row[5],
                "status": row[6],
                "create_time": row[7],
                "collected_at": row[8]
            }
            for row in rows
        ]

    def get_ports(self, agent_id: str, limit: int = 100) -> List[Dict]:
        """获取端口信息"""
        conn = self._get_connection()
        cursor = conn.cursor()

        cursor.execute('''
            SELECT * FROM ports WHERE agent_id = ? ORDER BY collected_at DESC LIMIT ?
        ''', (agent_id, limit))

        rows = cursor.fetchall()
        conn.close()

        return [
            {
                "id": row[0],
                "agent_id": row[1],
                "port": row[2],
                "protocol": row[3],
                "status": row[4],
                "pid": row[5],
                "process_name": row[6],
                "collected_at": row[7]
            }
            for row in rows
        ]

    def queue_data(self, data_type: str, data: Dict) -> bool:
        """将数据加入队列"""
        conn = self._get_connection()
        cursor = conn.cursor()
        now = datetime.now().isoformat()

        cursor.execute('''
            INSERT INTO data_queue (data_type, data, created_at)
            VALUES (?, ?, ?)
        ''', (data_type, json.dumps(data, ensure_ascii=False), now))

        conn.commit()
        conn.close()
        return True

    def get_queued_data(self, limit: int = 50) -> List[Dict]:
        """获取待上报数据"""
        conn = self._get_connection()
        cursor = conn.cursor()

        cursor.execute('''
            SELECT * FROM data_queue ORDER BY created_at ASC LIMIT ?
        ''', (limit,))

        rows = cursor.fetchall()
        conn.close()

        return [
            {"id": row[0], "data_type": row[1], "data": json.loads(row[2]),
             "retry_count": row[3], "created_at": row[4]}
            for row in rows
        ]

    def remove_queued_data(self, data_id: int) -> bool:
        """删除已上报数据"""
        conn = self._get_connection()
        cursor = conn.cursor()

        cursor.execute('DELETE FROM data_queue WHERE id = ?', (data_id,))

        conn.commit()
        conn.close()
        return True

    def increment_retry(self, data_id: int) -> bool:
        """增加重试计数"""
        conn = self._get_connection()
        cursor = conn.cursor()

        cursor.execute('''
            UPDATE data_queue SET retry_count = retry_count + 1 WHERE id = ?
        ''', (data_id,))

        conn.commit()
        conn.close()
        return True

"""
登录日志采集模块
采集系统登录日志，包括本地登录、远程登录、SSH等
兼容Windows和Linux
"""

import platform
import subprocess
import re
import json
from typing import Dict, List, Optional
from datetime import datetime, timedelta
from utils.logger import setup_logger

logger = setup_logger()


class LoginCollector:
    """登录日志采集器"""

    def __init__(self):
        self.system = platform.system()

    def collect(self, hours: int = 24) -> List[Dict]:
        """采集登录日志"""
        login_list = []

        try:
            if self.system == "Windows":
                login_list = self._collect_login_windows(hours)
            elif self.system == "Linux":
                login_list = self._collect_login_linux(hours)
        except Exception as e:
            logger.error(f"[登录日志] 采集失败: {e}")

        logger.info(f"[登录日志] 采集到 {len(login_list)} 条记录")
        return login_list

    def _collect_login_windows(self, hours: int = 24) -> List[Dict]:
        """Windows平台采集登录日志"""
        login_list = []

        try:
            self._get_current_session(login_list)
            self._get_rdp_sessions(login_list)
            self._get_user_accounts(login_list)
            self._get_net_join_status(login_list)
            self._get_recent_boot_time(login_list)

            logger.debug(f"[登录日志] Windows 采集到 {len(login_list)} 条")
        except Exception as e:
            logger.error(f"[登录日志] Windows采集失败: {e}")

        return login_list

    def _get_current_session(self, login_list: List[Dict]):
        """获取当前登录会话"""
        try:
            ps_script = """
            $session = qwinsta 2>$null
            if ($LASTEXITCODE -ne 0) {
                Write-Output "[]"
            } else {
                $session | Where-Object { $_ -match '\\S' } | ForEach-Object {
                    $parts = $_ -split '\\s+'
                    if ($parts.Count -ge 4 -and $parts[0] -ne 'SESSIONNAME' -and $parts[0] -ne '') {
                        $userName = $parts[0]
                        $state = $parts[3]
                        if ($state -eq 'Active') {
                            @{
                                Username = $userName
                                State = $state
                                SessionName = $parts[1]
                                ID = $parts[2]
                            } | ConvertTo-Json
                        }
                    }
                }
            }
            """
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command", ps_script],
                capture_output=True, text=True, timeout=30
            )

            if result.returncode == 0 and result.stdout.strip() and result.stdout.strip() != "[]":
                for line in result.stdout.strip().split('\n'):
                    if line.strip():
                        try:
                            session = json.loads(line)
                            username = session.get("Username", "")
                            if username:
                                login_list.append({
                                    "username": username,
                                    "login_type": "rdp",
                                    "login_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                                    "login_ip": "",
                                    "login_status": "success",
                                    "session_id": str(session.get("ID", "")),
                                    "source": "qwinsta"
                                })
                        except json.JSONDecodeError:
                            continue
        except Exception as e:
            logger.debug(f"[登录日志] 当前会话查询失败: {e}")

    def _get_rdp_sessions(self, login_list: List[Dict]):
        """获取RDP会话信息"""
        try:
            ps_script = """
            query session 2>$null | Where-Object { $_ -match 'Active' } | ForEach-Object {
                $parts = $_ -split '\\s+'
                if ($parts.Count -ge 4) {
                    @{
                        Username = $parts[1]
                        SessionName = $parts[2]
                        ID = $parts[3]
                    } | ConvertTo-Json
                }
            }
            """
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command", ps_script],
                capture_output=True, text=True, timeout=30
            )

            if result.returncode == 0 and result.stdout.strip():
                for line in result.stdout.strip().split('\n'):
                    if line.strip():
                        try:
                            session = json.loads(line)
                            username = session.get("Username", "")
                            if username and username != "services":
                                login_list.append({
                                    "username": username,
                                    "login_type": "rdp",
                                    "login_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                                    "login_ip": "",
                                    "login_status": "success",
                                    "session_id": str(session.get("ID", "")),
                                    "source": "rdp_session"
                                })
                        except json.JSONDecodeError:
                            continue
        except Exception as e:
            logger.debug(f"[登录日志] RDP会话查询失败: {e}")

    def _get_user_accounts(self, login_list: List[Dict]):
        """获取本地用户账户列表"""
        try:
            ps_script = """
            Get-LocalUser 2>$null | Where-Object {$_.Enabled -eq $true} | Select-Object Name,LastLogin | ConvertTo-Json
            """
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command", ps_script],
                capture_output=True, text=True, timeout=30
            )

            if result.returncode == 0 and result.stdout.strip():
                try:
                    users = json.loads(result.stdout)
                    if not isinstance(users, list):
                        users = [users]
                    for user in users:
                        name = user.get("Name", "")
                        last_login = user.get("LastLogin", "")
                        system_accounts = ["DefaultAccount", "Guest", "WDAGUtilityAccount"]
                        if name and name not in system_accounts:
                            login_list.append({
                                "username": name,
                                "login_type": "local",
                                "login_time": last_login if last_login else datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                                "login_ip": "",
                                "login_status": "success",
                                "source": "local_account"
                            })
                except json.JSONDecodeError:
                    pass
        except Exception as e:
            logger.debug(f"[登录日志] 用户账户查询失败: {e}")

    def _get_net_join_status(self, login_list: List[Dict]):
        """获取网络域加入状态"""
        try:
            ps_script = """
            $computer = Get-WmiObject -Class Win32_ComputerSystem -ErrorAction SilentlyContinue
            if ($computer) {
                if ($computer.PartOfDomain) {
                    @{
                        DomainJoined = $true
                        Domain = $computer.Domain
                        ComputerName = $computer.Name
                    } | ConvertTo-Json
                }
            }
            """
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command", ps_script],
                capture_output=True, text=True, timeout=30
            )

            if result.returncode == 0 and result.stdout.strip():
                try:
                    info = json.loads(result.stdout)
                    if info and info.get("DomainJoined"):
                        login_list.append({
                            "username": info.get("ComputerName", ""),
                            "login_type": "network",
                            "login_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                            "login_ip": "",
                            "login_status": "success",
                            "source": "domain_join"
                        })
                except json.JSONDecodeError:
                    pass
        except Exception as e:
            logger.debug(f"[登录日志] 域状态查询失败: {e}")

    def _get_recent_boot_time(self, login_list: List[Dict]):
        """获取系统启动时间"""
        try:
            ps_script = """
            (Get-CimInstance -ClassName Win32_OperatingSystem -ErrorAction SilentlyContinue -Property LastBootUpTime).LastBootUpTime | ConvertTo-Json
            """
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command", ps_script],
                capture_output=True, text=True, timeout=30
            )

            if result.returncode == 0 and result.stdout.strip():
                boot_time = result.stdout.strip()
                if boot_time and boot_time != "null":
                    try:
                        boot_dt = datetime.fromisoformat(boot_time.replace("Z", "+00:00"))
                        login_list.append({
                            "username": "SYSTEM",
                            "login_type": "system",
                            "login_time": boot_dt.strftime("%Y-%m-%d %H:%M:%S"),
                            "login_ip": "",
                            "login_status": "success",
                            "source": "boot_time"
                        })
                    except:
                        login_list.append({
                            "username": "SYSTEM",
                            "login_type": "system",
                            "login_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                            "login_ip": "",
                            "login_status": "success",
                            "source": "boot_time"
                        })
        except Exception as e:
            logger.debug(f"[登录日志] 启动时间查询失败: {e}")

    def _map_logon_type_windows(self, logon_type: str) -> str:
        """映射Windows登录类型"""
        logon_types = {
            '2': 'local',
            '3': 'network',
            '4': 'batch',
            '5': 'service',
            '7': 'unlock',
            '8': 'network_cleartext',
            '9': 'runas',
            '10': 'remote_interactive',
            '11': 'cached_interactive'
        }
        return logon_types.get(logon_type, 'other')

    def _collect_login_linux(self, hours: int = 24) -> List[Dict]:
        """Linux平台采集登录日志"""
        login_list = []

        try:
            self._get_lastlog(login_list)
            self._get_last_command(login_list, hours)
            self._get_ssh_logs(login_list, hours)
            self._get_who_command(login_list)

            logger.debug(f"[登录日志] Linux 采集到 {len(login_list)} 条")
        except Exception as e:
            logger.error(f"[登录日志] Linux采集失败: {e}")

        return login_list

    def _get_lastlog(self, login_list: List[Dict]):
        """获取lastlog信息"""
        try:
            result = subprocess.run(
                ["lastlog", "-t", "30"],
                capture_output=True, text=True, timeout=30
            )

            if result.returncode == 0:
                for line in result.stdout.split('\n'):
                    if '用户名' in line or 'Username' in line:
                        continue
                    if line.strip():
                        parts = line.split()
                        if len(parts) >= 3:
                            username = parts[0]
                            if username.startswith(('systemd', 'mysql', 'postgres', 'www-data')):
                                continue

                            time_str = ' '.join(parts[-3:])
                            try:
                                login_time = datetime.strptime(time_str, "%a %b %d %H:%M:%S")
                                if login_time.year == 1900:
                                    login_time = login_time.replace(year=datetime.now().year)
                                login_list.append({
                                    'username': username,
                                    'login_type': 'local',
                                    'login_time': login_time.strftime("%Y-%m-%d %H:%M:%S"),
                                    'login_ip': '',
                                    'login_status': 'success',
                                    'source': 'lastlog'
                                })
                            except:
                                pass
        except Exception as e:
            logger.debug(f"[登录日志] lastlog查询失败: {e}")

    def _get_last_command(self, login_list: List[Dict], hours: int):
        """获取last命令结果"""
        try:
            result = subprocess.run(
                ["last", "-t", f"{hours}hours", "-a"],
                capture_output=True, text=True, timeout=30
            )

            if result.returncode == 0:
                for line in result.stdout.split('\n'):
                    if not line.strip() or 'wtmp' in line or 'beginning' in line:
                        continue

                    parts = line.split()
                    if len(parts) >= 6:
                        username = parts[0]
                        if username.startswith(('systemd', 'reboot', 'shutdown')):
                            continue

                        date_str = ' '.join(parts[-3:])
                        try:
                            login_time = datetime.strptime(date_str, "%a %b %d %H:%M:%S")
                            if login_time.year == 1900:
                                login_time = login_time.replace(year=datetime.now().year)
                        except:
                            continue

                        login_type = 'local'
                        tty = parts[1] if len(parts) > 1 else ''

                        if 'ssh' in tty.lower() or 'pts' in tty.lower():
                            ip = ''
                            for part in parts:
                                if part.startswith(('192.', '10.', '172.', '::1', '127.')) or \
                                   re.match(r'\d+\.\d+\.\d+\.\d+', part):
                                    ip = part
                                    break
                            login_type = 'ssh' if ip else 'ssh'
                            login_ip = ip
                        elif tty in ['tty1', 'tty2', 'console']:
                            login_type = 'local'
                            login_ip = ''
                        else:
                            login_type = 'other'
                            login_ip = ''

                        login_list.append({
                            'username': username,
                            'login_type': login_type,
                            'login_time': login_time.strftime("%Y-%m-%d %H:%M:%S"),
                            'login_ip': login_ip,
                            'login_status': 'success',
                            'source': 'last'
                        })
        except Exception as e:
            logger.debug(f"[登录日志] last命令查询失败: {e}")

    def _get_ssh_logs(self, login_list: List[Dict], hours: int):
        """获取SSH登录日志"""
        try:
            log_files = [
                '/var/log/auth.log',
                '/var/log/secure',
                '/var/log/sshd.log',
                '/var/log/messages'
            ]

            time_filter = (datetime.now() - timedelta(hours=hours)).strftime("%b %d %H:%M:%S")

            for log_file in log_files:
                try:
                    result = subprocess.run(
                        ["grep", "-i", "sshd", log_file],
                        capture_output=True, text=True, timeout=30
                    )

                    if result.returncode == 0:
                        for line in result.stdout.split('\n'):
                            if time_filter in line or datetime.now().strftime("%b %d") in line:
                                if 'Accepted' in line or 'Failed' in line:
                                    self._parse_ssh_log_line(line, login_list)
                except:
                    continue
        except Exception as e:
            logger.debug(f"[登录日志] SSH日志查询失败: {e}")

    def _parse_ssh_log_line(self, line: str, login_list: List[Dict]):
        """解析SSH日志行"""
        try:
            user_match = re.search(r'for user (\S+)', line)
            if not user_match:
                user_match = re.search(r'user (\S+) ', line)

            if not user_match:
                return

            username = user_match.group(1)
            if username in ['root', 'systemd', 'mysql']:
                return

            ip_match = re.search(r'(\d{1,3}\.\d{1,3}\d{1,3}\.\d{1,3})', line)
            if not ip_match:
                ip_match = re.search(r'from (\S+)', line)

            login_ip = ip_match.group(1) if ip_match else ''

            login_status = 'success' if 'Accepted' in line else 'failed'

            time_str = ' '.join(line.split()[:3])
            try:
                login_time = datetime.strptime(time_str, "%b %d %H:%M:%S")
                if login_time.month == datetime.now().month and login_time.day > datetime.now().day + 1:
                    login_time = login_time.replace(year=datetime.now().year - 1)
                else:
                    login_time = login_time.replace(year=datetime.now().year)
            except:
                login_time = datetime.now()

            login_list.append({
                'username': username,
                'login_type': 'ssh',
                'login_time': login_time.strftime("%Y-%m-%d %H:%M:%S"),
                'login_ip': login_ip,
                'login_status': login_status,
                'source': 'ssh_log'
            })
        except Exception as e:
            logger.debug(f"[登录日志] SSH日志解析失败: {e}")

    def _get_who_command(self, login_list: List[Dict]):
        """获取当前登录用户"""
        try:
            result = subprocess.run(
                ["who"],
                capture_output=True, text=True, timeout=30
            )

            if result.returncode == 0:
                for line in result.stdout.split('\n'):
                    if line.strip():
                        parts = line.split()
                        if len(parts) >= 3:
                            username = parts[0]
                            tty = parts[1]

                            exists = any(
                                l.get('username') == username and
                                l.get('login_type') == 'local' and
                                l.get('source') == 'who'
                                for l in login_list
                            )
                            if not exists:
                                login_list.append({
                                    'username': username,
                                    'login_type': 'local',
                                    'login_time': datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                                    'login_ip': '',
                                    'login_status': 'success',
                                    'source': 'who'
                                })
        except Exception as e:
            logger.debug(f"[登录日志] who命令查询失败: {e}")

    def get_failed_logins(self, hours: int = 24) -> List[Dict]:
        """获取失败的登录尝试"""
        all_logs = self.collect(hours)
        return [log for log in all_logs if log.get('login_status') == 'failed']

    def get_successful_logins(self, hours: int = 24) -> List[Dict]:
        """获取成功的登录"""
        all_logs = self.collect(hours)
        return [log for log in all_logs if log.get('login_status') == 'success']

    def get_remote_logins(self, hours: int = 24) -> List[Dict]:
        """获取远程登录"""
        all_logs = self.collect(hours)
        remote_types = ['ssh', 'rdp', 'telnet', 'vnc', 'network']
        return [log for log in all_logs if log.get('login_type') in remote_types]
"""
已安装软件信息采集模块
采集终端已安装的软件清单列表
兼容Windows和Linux
"""

import platform
import subprocess
import re
import json
from typing import Dict, List, Optional
from datetime import datetime
from utils.logger import setup_logger

logger = setup_logger()


class SoftwareCollector:
    """已安装软件信息采集器"""

    def __init__(self):
        self.system = platform.system()

    def collect(self) -> List[Dict]:
        """采集所有已安装软件信息"""
        software_list = []

        try:
            if self.system == "Windows":
                software_list = self._collect_software_windows()
            elif self.system == "Linux":
                software_list = self._collect_software_linux()
        except Exception as e:
            logger.error(f"[已安装软件] 采集失败: {e}")

        logger.info(f"[已安装软件] 采集到 {len(software_list)} 个软件")
        return software_list

    def _collect_software_windows(self) -> List[Dict]:
        """Windows平台采集已安装软件"""
        software_list = []

        try:
            ps_script = """
            Get-ItemProperty HKLM:\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\* | 
            Select-Object DisplayName,DisplayVersion,Publisher,InstallDate,InstallLocation,EstimatedSize |
            Where-Object {$_.DisplayName -notlike $null} |
            ConvertTo-Json
            """
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command", ps_script],
                capture_output=True, text=True, timeout=120
            )

            if result.returncode == 0 and result.stdout.strip():
                try:
                    data = json.loads(result.stdout)
                    if not isinstance(data, list):
                        data = [data]

                    for sw in data:
                        if sw.get("DisplayName"):
                            install_date = ""
                            if sw.get("InstallDate"):
                                install_date = sw["InstallDate"]
                                if install_date.isdigit() and len(install_date) == 8:
                                    try:
                                        install_date = datetime.strptime(install_date, "%Y%m%d").strftime("%Y-%m-%d")
                                    except:
                                        pass

                            size_kb = int(sw.get("EstimatedSize", 0)) if sw.get("EstimatedSize") else 0

                            software_list.append({
                                "software_name": sw["DisplayName"],
                                "version": sw.get("DisplayVersion", ""),
                                "publisher": sw.get("Publisher", ""),
                                "install_date": install_date,
                                "install_location": sw.get("InstallLocation", ""),
                                "size": size_kb,
                                "software_type": self._detect_software_type(sw["DisplayName"]),
                                "source": "windows_registry"
                            })
                except json.JSONDecodeError as e:
                    logger.error(f"[已安装软件] Windows JSON解析失败: {e}")

            ps_script_64 = """
            Get-ItemProperty HKLM:\\Software\\Wow6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\* | 
            Select-Object DisplayName,DisplayVersion,Publisher,InstallDate,InstallLocation,EstimatedSize |
            Where-Object {$_.DisplayName -notlike $null} |
            ConvertTo-Json
            """
            result_64 = subprocess.run(
                ["powershell", "-NoProfile", "-Command", ps_script_64],
                capture_output=True, text=True, timeout=120
            )

            if result_64.returncode == 0 and result_64.stdout.strip():
                try:
                    data = json.loads(result_64.stdout)
                    if not isinstance(data, list):
                        data = [data]

                    for sw in data:
                        if sw.get("DisplayName"):
                            install_date = ""
                            if sw.get("InstallDate"):
                                install_date = sw["InstallDate"]
                                if install_date.isdigit() and len(install_date) == 8:
                                    try:
                                        install_date = datetime.strptime(install_date, "%Y%m%d").strftime("%Y-%m-%d")
                                    except:
                                        pass

                            size_kb = int(sw.get("EstimatedSize", 0)) if sw.get("EstimatedSize") else 0

                            existing = False
                            for existing_sw in software_list:
                                if existing_sw["software_name"] == sw["DisplayName"]:
                                    existing = True
                                    break

                            if not existing:
                                software_list.append({
                                    "software_name": sw["DisplayName"],
                                    "version": sw.get("DisplayVersion", ""),
                                    "publisher": sw.get("Publisher", ""),
                                    "install_date": install_date,
                                    "install_location": sw.get("InstallLocation", ""),
                                    "size": size_kb,
                                    "software_type": self._detect_software_type(sw["DisplayName"]),
                                    "source": "windows_registry_64"
                                })
                except json.JSONDecodeError:
                    pass

            ps_chocolatey = """
            chocolatey list -lo 2>$null | Select-Object -Skip 1 | ForEach-Object {
                $parts = $_ -split ' '
                @{
                    Name = $parts[0]
                    Version = $parts[1] if $parts.Length -gt 1 else ''
                } | ConvertTo-Json
            }
            """
            choco_result = subprocess.run(
                ["powershell", "-NoProfile", "-Command", ps_chocolatey],
                capture_output=True, text=True, timeout=60
            )

            if choco_result.returncode == 0 and choco_result.stdout.strip():
                for line in choco_result.stdout.strip().split('\n'):
                    if line.strip():
                        try:
                            sw = json.loads(line)
                            if sw.get("Name"):
                                existing = False
                                for existing_sw in software_list:
                                    if existing_sw["software_name"] == sw["Name"]:
                                        existing = True
                                        break
                                if not existing:
                                    software_list.append({
                                        "software_name": sw["Name"],
                                        "version": sw.get("Version", ""),
                                        "publisher": "",
                                        "install_date": "",
                                        "install_location": "",
                                        "size": 0,
                                        "software_type": "package_manager",
                                        "source": "chocolatey"
                                    })
                        except json.JSONDecodeError:
                            pass

            logger.debug(f"[已安装软件] Windows 采集到 {len(software_list)} 个")
        except Exception as e:
            logger.error(f"[已安装软件] Windows采集失败: {e}")

        return software_list

    def _collect_software_linux(self) -> List[Dict]:
        """Linux平台采集已安装软件"""
        software_list = []

        try:
            dpkg_result = subprocess.run(
                ["dpkg-query", "-W", "-f=${Package}\\t${Version}\\t${Maintainer}\\t${Installed-Size}\\n", "*"],
                capture_output=True, text=True, timeout=120
            )

            if dpkg_result.returncode == 0:
                for line in dpkg_result.stdout.strip().split('\n'):
                    if line.strip():
                        parts = line.split('\t')
                        if len(parts) >= 1:
                            name = parts[0]
                            version = parts[1] if len(parts) > 1 else ""
                            publisher = parts[2] if len(parts) > 2 else ""
                            size = int(parts[3]) if len(parts) > 3 and parts[3].isdigit() else 0

                            software_list.append({
                                "software_name": name,
                                "version": version,
                                "publisher": publisher,
                                "install_date": "",
                                "install_location": "",
                                "size": size,
                                "software_type": self._detect_software_type(name),
                                "source": "dpkg"
                            })

            rpm_result = subprocess.run(
                ["rpm", "-qa", "--queryformat", "%{NAME}\\t%{VERSION}\\t%{VENDOR}\\t%{SIZE}\\n"],
                capture_output=True, text=True, timeout=120
            )

            if rpm_result.returncode == 0:
                existing_names = {sw["software_name"] for sw in software_list}
                for line in rpm_result.stdout.strip().split('\n'):
                    if line.strip():
                        parts = line.split('\t')
                        if len(parts) >= 1:
                            name = parts[0]
                            if name not in existing_names:
                                version = parts[1] if len(parts) > 1 else ""
                                publisher = parts[2] if len(parts) > 2 else ""
                                size = int(parts[3]) if len(parts) > 3 and parts[3].isdigit() else 0

                                software_list.append({
                                    "software_name": name,
                                    "version": version,
                                    "publisher": publisher,
                                    "install_date": "",
                                    "install_location": "",
                                    "size": size,
                                    "software_type": self._detect_software_type(name),
                                    "source": "rpm"
                                })

            pip_result = subprocess.run(
                ["pip", "list", "--format=json"],
                capture_output=True, text=True, timeout=60
            )

            if pip_result.returncode == 0:
                try:
                    pip_packages = json.loads(pip_result.stdout)
                    for pkg in pip_packages:
                        name = pkg.get("name", "")
                        version = pkg.get("version", "")

                        software_list.append({
                            "software_name": f"python-{name}",
                            "version": version,
                            "publisher": "PyPI",
                            "install_date": "",
                            "install_location": "",
                            "size": 0,
                            "software_type": "python_package",
                            "source": "pip"
                        })
                except json.JSONDecodeError:
                    pass

            npm_result = subprocess.run(
                ["npm", "list", "-g", "--depth=0", "--json"],
                capture_output=True, text=True, timeout=60
            )

            if npm_result.returncode == 0:
                try:
                    npm_data = json.loads(npm_result.stdout)
                    dependencies = npm_data.get("dependencies", {})
                    for name, info in dependencies.items():
                        version = info.get("version", "")
                        software_list.append({
                            "software_name": f"node-{name}",
                            "version": version,
                            "publisher": "npm",
                            "install_date": "",
                            "install_location": "",
                            "size": 0,
                            "software_type": "node_package",
                            "source": "npm"
                        })
                except json.JSONDecodeError:
                    pass

            logger.debug(f"[已安装软件] Linux 采集到 {len(software_list)} 个")
        except Exception as e:
            logger.error(f"[已安装软件] Linux采集失败: {e}")

        return software_list

    def _detect_software_type(self, name: str) -> str:
        """检测软件类型"""
        name_lower = name.lower()

        type_keywords = {
            'development': ['python', 'java', 'node', 'ruby', 'go', 'rust', 'gcc', 'compiler', 'sdk', 'jdk', 'npm', 'pip'],
            'database': ['mysql', 'postgresql', 'redis', 'mongodb', 'oracle', 'sqlite', 'mariadb', 'postgres'],
            'web_server': ['nginx', 'apache', 'httpd', 'iis', 'tomcat', 'jetty'],
            'security': ['firewall', 'antivirus', 'clamav', 'selinux', 'audit', 'security'],
            'media': ['vlc', 'ffmpeg', 'mplayer', 'codec', 'audio', 'video'],
            'office': ['libreoffice', 'openoffice', 'office', 'word', 'excel'],
            'browser': ['firefox', 'chrome', 'chromium', 'edge', 'opera', 'browser'],
            'communication': ['slack', 'teams', 'zoom', 'skype', 'discord', 'telegram', 'wechat'],
            'virtualization': ['docker', 'virtualbox', 'vmware', 'kvm', 'hyper-v', 'container'],
            'backup': ['rsync', 'tar', 'backup', 'restic', 'borg'],
            'system': ['systemd', 'kernel', 'firmware', 'driver', 'utility'],
            'network': ['ssh', 'ftp', 'telnet', 'vpn', 'wireguard', 'openvpn']
        }

        for software_type, keywords in type_keywords.items():
            for keyword in keywords:
                if keyword in name_lower:
                    return software_type

        return 'application'

    def get_software_count(self) -> int:
        """获取软件总数"""
        software_list = self.collect()
        return len(software_list)

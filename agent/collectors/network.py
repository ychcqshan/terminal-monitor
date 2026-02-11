"""
网卡信息采集模块
采集网卡信息、MAC地址、IP地址等
兼容Windows和Linux
"""

import platform
import subprocess
import re
import socket
import json
from typing import Dict, List, Optional
from utils.logger import setup_logger

logger = setup_logger()


class NetworkCollector:
    """网卡信息采集器"""

    def __init__(self):
        self.system = platform.system()

    def collect(self) -> List[Dict]:
        """采集所有网卡信息"""
        interfaces = []

        try:
            if self.system == "Windows":
                interfaces = self._collect_interfaces_windows()
            elif self.system == "Linux":
                interfaces = self._collect_interfaces_linux()
        except Exception as e:
            logger.error(f"[网卡信息] 采集失败: {e}")

        logger.info(f"[网卡信息] 采集到 {len(interfaces)} 个网卡")
        return interfaces

    def _collect_interfaces_windows(self) -> List[Dict]:
        """Windows平台采集网卡信息"""
        interfaces = []

        try:
            ps_script = """
            Get-NetAdapter | Where-Object {$_.Status -eq 'Up'} | Select-Object Name,MacAddress,InterfaceDescription | ConvertTo-Json
            """
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command", ps_script],
                capture_output=True, text=True, timeout=30
            )

            if result.returncode == 0 and result.stdout.strip():
                try:
                    adapter_data = json.loads(result.stdout)
                    if not isinstance(adapter_data, list):
                        adapter_data = [adapter_data]

                    for adapter in adapter_data:
                        adapter_name = adapter.get("Name", "")
                        mac = adapter.get("MacAddress", "")
                        if mac:
                            mac = ":".join([mac[i:i+2] for i in range(0, 12, 2)])

                        interface = {
                            "name": adapter_name,
                            "mac": mac,
                            "ips": [],
                            "status": "up" if mac else "down",
                            "speed": 0,
                            "description": adapter.get("InterfaceDescription", "")
                        }

                        ps_ip_script = f"""
                        Get-NetIPConfiguration -InterfaceAlias "{adapter_name}" | Select-Object IPv4Address,IPv6Address | ConvertTo-Json
                        """
                        ip_result = subprocess.run(
                            ["powershell", "-NoProfile", "-Command", ps_ip_script],
                            capture_output=True, text=True, timeout=30
                        )

                        if ip_result.returncode == 0 and ip_result.stdout.strip():
                            try:
                                ip_data = json.loads(ip_result.stdout)
                                if isinstance(ip_data, list) and len(ip_data) > 0:
                                    ip_data = ip_data[0]
                                if ip_data.get("IPv4Address"):
                                    ips = ip_data["IPv4Address"]
                                    if isinstance(ips, list):
                                        for ip in ips:
                                            if ip.get("IPAddress"):
                                                interface["ips"].append({
                                                    "address": ip["IPAddress"],
                                                    "version": "IPv4",
                                                    "prefix": str(ip.get("PrefixLength", ""))
                                                })
                                if ip_data.get("IPv6Address"):
                                    ips = ip_data["IPv6Address"]
                                    if isinstance(ips, list):
                                        for ip in ips:
                                            if ip.get("IPAddress"):
                                                interface["ips"].append({
                                                    "address": ip["IPAddress"],
                                                    "version": "IPv6",
                                                    "prefix": str(ip.get("PrefixLength", ""))
                                                })
                            except json.JSONDecodeError:
                                pass

                        if interface["name"]:
                            interfaces.append(interface)
                except json.JSONDecodeError as e:
                    logger.error(f"[网卡信息] Windows 网卡 JSON解析失败: {e}")

            logger.debug(f"[网卡信息] Windows 网卡数: {len(interfaces)}")
        except Exception as e:
            logger.error(f"[网卡信息] Windows采集失败: {e}")

        return interfaces

    def _collect_interfaces_linux(self) -> List[Dict]:
        """Linux平台采集网卡信息"""
        interfaces = []

        try:
            result = subprocess.run(
                ["ip", "-j", "addr", "show"],
                capture_output=True, text=True, timeout=30
            )

            if result.returncode == 0:
                iface_list = json.loads(result.stdout)

                for iface in iface_list:
                    iface_name = iface.get("ifname", "")
                    if iface_name in ["lo", "docker0", "veth"]:
                        continue

                    mac = ""
                    for addr_info in iface.get("addr_info", []):
                        if addr_info.get("family") == "link":
                            mac = addr_info.get("local", "")
                            break

                    ips = []
                    for addr_info in iface.get("addr_info", []):
                        local = addr_info.get("local", "")
                        if local and local != mac:
                            prefix = addr_info.get("prefixlen", "")
                            family = addr_info.get("family", "")
                            version = "IPv6" if family == "inet6" else "IPv4"
                            ips.append({
                                "address": local,
                                "version": version,
                                "prefix": prefix
                            })

                    status = "up"
                    try:
                        with open(f'/sys/class/net/{iface_name}/carrier', 'r') as f:
                            carrier = f.read().strip()
                            status = "up" if carrier == "1" else "down"
                    except:
                        pass

                    speed = 0
                    try:
                        with open(f'/sys/class/net/{iface_name}/speed', 'r') as f:
                            speed = int(f.read().strip()) * 1000000
                    except:
                        pass

                    mtu = 0
                    try:
                        with open(f'/sys/class/net/{iface_name}/mtu', 'r') as f:
                            mtu = int(f.read().strip())
                    except:
                        pass

                    interface = {
                        "name": iface_name,
                        "mac": mac,
                        "ips": ips,
                        "status": status,
                        "speed": speed,
                        "mtu": mtu
                    }

                    if interface["name"]:
                        interfaces.append(interface)

            result = subprocess.run(
                ["ip", "-j", "route", "show", "default"],
                capture_output=True, text=True, timeout=30
            )
            if result.returncode == 0:
                routes = json.loads(result.stdout)
                for route in routes:
                    gateway = route.get("gateway", "")
                    iface = route.get("dev", "")
                    for interface in interfaces:
                        if interface["name"] == iface and not interface.get("gateway"):
                            interface["gateway"] = gateway
                            break

            logger.debug(f"[网卡信息] Linux 网卡数: {len(interfaces)}")
        except Exception as e:
            logger.error(f"[网卡信息] Linux采集失败: {e}")

        return interfaces

    def get_active_ip(self) -> str:
        """获取活动的IP地址"""
        try:
            if self.system == "Windows":
                return self._get_active_ip_windows()
            elif self.system == "Linux":
                return self._get_active_ip_linux()
            return "127.0.0.1"
        except Exception as e:
            logger.error(f"[网卡信息] 获取活动IP失败: {e}")
            return "127.0.0.1"

    def _get_active_ip_windows(self) -> str:
        """Windows获取活动IP"""
        try:
            ps_script = """
            Get-NetIPAddress | Where-Object {$_.AddressFamily -eq 2 -and $_.PrefixOrigin -ne 'WellKnown'} | Select-Object IPAddress | ConvertTo-Json
            """
            result = subprocess.run(
                ["powershell", "-NoProfile", "-Command", ps_script],
                capture_output=True, text=True, timeout=30
            )

            if result.returncode == 0 and result.stdout.strip():
                try:
                    data = json.loads(result.stdout)
                    if not isinstance(data, list):
                        data = [data]
                    for ip_entry in data:
                        if ip_entry.get("IPAddress"):
                            ip = ip_entry["IPAddress"]
                            if not ip.startswith("169.254") and not ip.startswith("fe80"):
                                return ip
                except json.JSONDecodeError:
                    pass
        except Exception as e:
            logger.debug(f"[网卡信息] Windows获取IP失败: {e}")
        return "127.0.0.1"

    def _get_active_ip_linux(self) -> str:
        """Linux获取活动IP"""
        try:
            result = subprocess.run(
                ["ip", "route", "get", "8.8.8.8"],
                capture_output=True, text=True, timeout=30
            )
            if result.returncode == 0:
                match = re.search(r'src\s+(\d+\.\d+\.\d+\.\d+)', result.stdout)
                if match:
                    return match.group(1)
        except:
            pass
        return "127.0.0.1"

    def get_mac_addresses(self) -> List[Dict]:
        """获取所有MAC地址"""
        interfaces = self.collect()
        mac_list = []
        for iface in interfaces:
            if iface.get("mac") and iface["mac"] != "00:00:00:00:00:00":
                mac_list.append({
                    "name": iface["name"],
                    "mac": iface["mac"],
                    "ips": [ip.get("address") for ip in iface.get("ips", [])]
                })
        return mac_list

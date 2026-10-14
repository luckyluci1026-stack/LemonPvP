#!/usr/bin/env python3
"""Minecraft Server List Ping against a running test server (status handshake + ping).

Usage: slp.py <port> [host]
Proves that the network thread accepts connections and the server answers; exits 1 otherwise.
This is not a player login (offline-mode login with a real client is not part of the CI).
"""
import json
import socket
import struct
import sys
import time


def varint(value: int) -> bytes:
    out = b""
    value &= 0xFFFFFFFF
    while True:
        byte = value & 0x7F
        value >>= 7
        if value:
            out += bytes([byte | 0x80])
        else:
            return out + bytes([byte])


def read_varint(sock) -> int:
    result = 0
    for shift in range(0, 35, 7):
        b = sock.recv(1)
        if not b:
            raise EOFError("connection closed")
        result |= (b[0] & 0x7F) << shift
        if not b[0] & 0x80:
            return result
    raise ValueError("varint too long")


def read_exact(sock, n: int) -> bytes:
    data = b""
    while len(data) < n:
        chunk = sock.recv(n - len(data))
        if not chunk:
            raise EOFError("connection closed")
        data += chunk
    return data


def packet(packet_id: int, payload: bytes = b"") -> bytes:
    body = varint(packet_id) + payload
    return varint(len(body)) + body


def main():
    port = int(sys.argv[1])
    host = sys.argv[2] if len(sys.argv) > 2 else "127.0.0.1"
    with socket.create_connection((host, port), timeout=10) as sock:
        addr = host.encode("utf-8")
        handshake = varint(-1) + varint(len(addr)) + addr + struct.pack(">H", port) + varint(1)
        sock.sendall(packet(0x00, handshake) + packet(0x00))
        read_varint(sock)  # packet length
        if read_varint(sock) != 0x00:
            raise ValueError("unexpected status response id")
        status = json.loads(read_exact(sock, read_varint(sock)).decode("utf-8"))
        start = time.monotonic()
        sock.sendall(packet(0x01, struct.pack(">q", 4242)))
        read_varint(sock)
        if read_varint(sock) != 0x01 or struct.unpack(">q", read_exact(sock, 8))[0] != 4242:
            raise ValueError("bad pong")
        rtt = (time.monotonic() - start) * 1000
    version = status.get("version", {})
    players = status.get("players", {})
    print(f"SLP ok: {version.get('name')} (protocol {version.get('protocol')}), "
          f"players {players.get('online')}/{players.get('max')}, ping {rtt:.1f} ms")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:  # noqa: BLE001 - any failure is a failed check
        print(f"SLP FAILED: {e!r}")
        sys.exit(1)

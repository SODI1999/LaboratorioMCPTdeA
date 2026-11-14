"""Build and exercise the HTTP profile against an isolated in-memory H2 database."""

import socket
import subprocess
import time
from pathlib import Path

from verify_mcp_http import verify


def check():
    """Build HTTP, initialize MCP and check tools and seeded read-only queries.

    Requires Java, Maven Wrapper and a free loopback port 8080. Creates only
    generated files under target and an in-memory database; always stops its JVM.
    Subprocess and protocol failures propagate to the check exit code.
    """
    root = Path(__file__).resolve().parents[1]
    subprocess.run(["cmd.exe", "/c", str(root / "mvnw.cmd"), "-Phttp", "-DskipTests", "package"], cwd=root, check=True)
    with (root / "target/http-check-process.log").open("w", encoding="utf-8") as log:
        process = subprocess.Popen(["java", "-jar", "target/mcp-bank-tdea-http-0.0.1-SNAPSHOT.jar",
            "--spring.profiles.active=http", "--spring.datasource.url=jdbc:h2:mem:http-transport-check",
            "--logging.file.name=./target/http-check.log"], cwd=root, stdout=log, stderr=log)
        try:
            deadline = time.monotonic() + 40
            while True:
                if process.poll() is not None:
                    raise RuntimeError("HTTP process exited before its port became ready; see target/http-check.log")
                try:
                    with socket.create_connection(("127.0.0.1", 8080), timeout=.3):
                        break
                except (ConnectionRefusedError, TimeoutError):
                    if time.monotonic() >= deadline:
                        raise TimeoutError("HTTP startup exceeded 40 seconds")
                    time.sleep(.2)
            verify(expected_balances=("2,500,000.00", "1,200,000.00"), delivery_history=False,
                   output_path=root / "target/http-smoke.json")
        finally:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()


if __name__ == "__main__":
    check()

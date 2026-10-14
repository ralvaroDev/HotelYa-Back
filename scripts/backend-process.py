"""Identify only this checkout's backend JVM and bootRun Gradle wrapper."""
import os
from pathlib import Path
import shlex
import signal
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parent.parent
MAIN = "pe.ralvaro.hotelyaback.HotelyaBackApplication"
WRAPPER = str(ROOT / "gradle/wrapper/gradle-wrapper.jar")


def processes():
    output = subprocess.check_output(["ps", "-axo", "pid=,stat=,command="], text=True)
    found = []
    for line in output.splitlines():
        parts = line.strip().split(None, 2)
        if len(parts) != 3 or parts[1].startswith("Z"):
            continue
        try:
            args = shlex.split(parts[2])
        except ValueError:
            continue
        if not args or Path(args[0]).name != "java":
            continue
        own_classpath = any(
            entry.startswith(str(ROOT) + "/")
            for arg in args for entry in arg.split(":")
        )
        app = MAIN in args and own_classpath
        wrapper = ("org.gradle.wrapper.GradleWrapperMain" in args
                   and WRAPPER in args and "bootRun" in args)
        if app or wrapper:
            found.append(int(parts[0]))
    return found


if sys.argv[1:] == ["status"]:
    sys.exit(0 if processes() else 1)
if sys.argv[1:] != ["stop"]:
    raise SystemExit("Uso: backend-process.py status|stop")

targets = processes()
if not targets:
    print("El backend de HotelYa ya está detenido.")
    sys.exit(0)
for pid in targets:
    # Recheck identity immediately before signalling; no saved PID can go stale.
    if pid in processes():
        try:
            os.kill(pid, signal.SIGTERM)
        except ProcessLookupError:
            pass
deadline = time.monotonic() + 20
while processes():
    if time.monotonic() >= deadline:
        raise SystemExit("El backend sigue activo tras 20 segundos; no se apagó Supabase.")
    time.sleep(0.2)
print("Backend de HotelYa detenido.")

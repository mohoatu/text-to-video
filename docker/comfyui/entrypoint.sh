#!/bin/sh
# `pip install torch` pulls the CUDA-enabled build, which crashes outright on
# a host with no NVIDIA driver at all (torch.cuda.current_device() raises
# instead of falling back) unless ComfyUI is told --cpu explicitly. Detect
# that case here so the same image works on both GPU and CPU-only hosts
# without the caller having to know which.
set -e

if python3 -c "import torch, sys; sys.exit(0 if torch.cuda.is_available() else 1)" 2>/dev/null; then
    echo "[entrypoint] NVIDIA GPU detected, running with CUDA acceleration"
    exec python3 main.py --listen 0.0.0.0 "$@"
else
    echo "[entrypoint] No usable NVIDIA GPU detected, running in --cpu mode (slow)"
    exec python3 main.py --listen 0.0.0.0 --cpu "$@"
fi

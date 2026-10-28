# Containerized ComfyUI for the videogen visual-generation stage. Runs the
# standard SD1.5 txt2img workflow videogen submits (CheckpointLoaderSimple ->
# KSampler -> SaveImage) — no AnimateDiff/custom nodes required.
#
# GPU note: `pip install torch` here pulls the CUDA-enabled build, which uses
# an NVIDIA GPU automatically under nvidia-docker/nvidia-container-toolkit on
# Linux. entrypoint.sh detects whether a usable GPU is actually present and
# adds --cpu if not — without that, this image crashes outright on startup on
# a host with no NVIDIA driver at all, rather than just running slowly (verified:
# torch.cuda.current_device() raises instead of falling back; see entrypoint.sh).
# Docker Desktop on macOS cannot pass through Apple's Metal/MPS GPU to a
# container at all; for Apple Silicon, running ComfyUI natively on the host
# (see TASKS.md) and pointing the app container at it via
# videogen.visual.comfyui-base-url is faster than containerizing it.
FROM python:3.12-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends git curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/comfyui
RUN git clone --depth 1 https://github.com/comfyanonymous/ComfyUI.git .

RUN pip install --no-cache-dir --default-timeout=120 --retries 10 torch torchvision torchaudio \
    && pip install --no-cache-dir --default-timeout=120 --retries 10 -r requirements.txt

# Checkpoints are large (the default v1-5-pruned-emaonly.safetensors is
# ~4.3GB) and change independently of the image, so they live in a volume
# rather than being baked in — see docker-compose.yml and its setup note.
VOLUME ["/opt/comfyui/models", "/opt/comfyui/output"]

COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

EXPOSE 8188

HEALTHCHECK --interval=15s --timeout=3s --start-period=30s \
    CMD curl -f http://localhost:8188/system_stats || exit 1

# Auto-detects whether a usable NVIDIA GPU is present and adds --cpu only if
# not — without this, ComfyUI (via torch's CUDA-enabled build) crashes
# outright on startup on a host with no NVIDIA driver at all, rather than
# just running slowly.
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]

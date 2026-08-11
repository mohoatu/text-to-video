# Technical Solution Proposal

## 1. Overview
This document proposes a practical technical solution for the Vietnamese local-first AI text-to-video platform based on the architecture described in the project documentation. The design follows a hybrid model:

- Cloud services handle orchestration, storage, queues, and API exposure.
- Local inference and rendering are performed on a dedicated workstation or GPU-enabled machine to reduce cloud cost.

The target outcome is a scalable MVP that can generate short-form videos with scripting, narration, visuals, subtitles, and final export.

## 2. Solution Objectives
The proposed solution should:
- support Vietnamese prompt input
- generate structured scene data using a local LLM
- convert narration to Vietnamese speech
- generate or retrieve visuals for each scene
- assemble the final video with FFmpeg
- provide asynchronous job processing and status tracking
- remain affordable for early-stage deployment

## 3. Recommended Architecture

### 3.1 High-Level Flow
1. User submits a prompt through a web or API client.
2. The backend receives the request and stores job metadata.
3. A background worker picks up the job from the queue.
4. The orchestration layer sends the prompt to a local LLM for plan generation.
5. The workflow calls downstream services for:
   - TTS generation
   - visual generation or retrieval
   - subtitle generation
   - FFmpeg-based assembly
6. The final MP4 file and intermediate assets are stored in cloud storage.
7. The user receives a completion notification and download link.

### 3.2 Infrastructure Components
- API Layer: AWS API Gateway or a lightweight Spring Boot entrypoint
- Application Layer: Java Spring Boot services
- Queue Layer: Amazon SQS
- Storage Layer: Amazon S3 for assets and final videos
- Metadata Layer: Amazon DynamoDB for jobs and status tracking
- Messaging/Notification: Amazon SNS or WebSocket-based status updates
- Observability: CloudWatch, structured logs, metrics
- Local Worker Node: Mac M1 / RTX 4070/4080/4090 workstation running Ollama, ComfyUI, FFmpeg, and local TTS tools

## 4. Infrastructure Design

### 4.1 Cloud Side (Orchestration and Storage)
The cloud side is responsible for control logic, persistence, and API traffic. It should be deployed with a small, cost-efficient footprint.

Recommended services:
- Amazon API Gateway
- Amazon ECS Fargate or EC2 for Spring Boot services
- Amazon SQS for asynchronous job queues
- Amazon S3 for intermediate files and final videos
- Amazon DynamoDB for job records and metadata
- Amazon CloudWatch for logs and monitoring
- AWS Secrets Manager for credentials and API keys

### 4.2 Local Side (Heavy Compute)
The local side is responsible for expensive work such as:
- local LLM inference
- visual generation
- audio synthesis
- media rendering and FFmpeg assembly

Recommended local environment:
- Option A: Mac Studio M2 Ultra / M3 Max for better local integration and stability
- Option B: Windows/Linux desktop with RTX 4070/4080/4090 for better price/performance

Required local software:
- Ollama or LM Studio for LLM inference
- ComfyUI or Draw Things for image/video generation
- FFmpeg for editing and subtitle rendering
- Piper, Coqui TTS, or a similar local TTS engine

## 5. Technical Stack

### 5.1 Backend
- Java 21
- Spring Boot 3.x
- Spring Web / Spring WebFlux
- Spring AI
- Spring Validation
- Lombok
- OpenAPI / Swagger

### 5.2 AI and Media Processing
- Ollama or LM Studio for local LLM execution
- Spring AI Ollama connector
- Piper / Coqui TTS for Vietnamese speech synthesis
- ComfyUI or Draw Things for local visual generation
- FFmpeg for concatenation, mixing, subtitles, and export

### 5.3 Data and Messaging
- Amazon S3
- Amazon DynamoDB
- Amazon SQS
- Amazon SNS (optional for notifications)

### 5.4 DevOps and Deployment
- Docker
- Docker Compose for local development
- GitHub Actions for CI/CD
- AWS ECS for production deployment
- CloudWatch for monitoring

## 6. Recommended Deployment Topology

### MVP Deployment
- One Spring Boot API service deployed on ECS Fargate
- One SQS queue for jobs
- One worker service running background tasks
- One local GPU workstation acting as the inference/render node
- One S3 bucket for media assets
- One DynamoDB table for job status and metadata

### Production-Ready Extension
- Multiple worker nodes for parallel rendering
- Separate queues for script, audio, visuals, and final rendering stages
- CDN distribution for public video delivery
- Optional Kubernetes-based scaling if workload grows significantly

## 7. Security and Reliability
- Store secrets in AWS Secrets Manager
- Apply IAM least-privilege policies
- Use HTTPS for all external communications
- Retry mechanisms for transient generation failures
- Structured logging and error tracing for each pipeline stage
- Timeout and dead-letter queue handling for failed jobs

## 8. Estimated Cost
The cost estimate depends on whether the project is run as a low-volume MVP or a larger production workload.

### 8.1 One-Time Setup Cost
| Item | Estimated Cost (USD) | Notes |
|---|---:|---|
| Local GPU workstation | $2,500 – $7,000 | RTX 4070/4080/4090 desktop or Mac Studio |
| SSD storage upgrade | $200 – $800 | Recommended for intermediate assets |
| Networking and peripherals | $100 – $300 | Optional |

### 8.2 Monthly Cloud Cost (MVP)
| Service | Estimated Monthly Cost (USD) | Notes |
|---|---:|---|
| API Gateway | $1 – $5 | Low usage |
| ECS Fargate / EC2 | $15 – $50 | Small app and worker services |
| S3 storage | $5 – $20 | Depends on video volume |
| DynamoDB | $10 – $25 | Low-to-medium metadata volume |
| SQS | $1 – $5 | Queue usage |
| CloudWatch and logs | $3 – $10 | Monitoring and logs |
| Total | $35 – $115 | MVP range |

### 8.3 Estimated Cost by Scale
| Scenario | Estimated Monthly Cloud Cost | Estimated Initial Hardware Cost |
|---|---:|---:|
| MVP / personal use | $35 – $115 | $2,500 – $7,000 |
| Small team / 50–100 videos/month | $100 – $300 | $3,000 – $7,000 |
| Production / 300+ videos/month | $300 – $1,000+ | $5,000+ |

### 8.4 Approximate Cost in Vietnamese Dong
Using an exchange rate of roughly 26,000 VND/USD:
- MVP monthly cloud cost: about 910,000 – 2,990,000 VND
- Initial local hardware cost: about 65,000,000 – 182,000,000 VND

## 9. Recommended Implementation Plan
### Phase 1 – MVP
- Build the Spring Boot API and job queue
- Connect to local LLM via Ollama
- Implement basic script-to-scene generation
- Add Vietnamese TTS integration
- Add FFmpeg video assembly
- Deploy locally and validate with sample prompts

### Phase 2 – Optimization
- Add better error handling and retries
- Improve subtitle generation and timing alignment
- Add worker scaling and queue prioritization
- Improve asset storage and cleanup workflows

### Phase 3 – Commercialization
- Add user authentication and billing
- Introduce subscription and credit-based pricing
- Add hosted rendering and API plans
- Expand with more templates and voice options

## 10. Conclusion
The recommended solution is a hybrid architecture where the cloud handles orchestration and storage while a local GPU workstation handles the heavy AI and render tasks. This approach provides a strong balance between cost, performance, and scalability for an MVP and future commercial rollout.

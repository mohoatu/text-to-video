# Architecture Decision Report

## 1. Decision Summary
The recommended implementation approach for this project is a hybrid architecture in which the cloud handles orchestration, storage, queueing, and API entry points, while a local GPU workstation performs the heavy AI and media rendering tasks.

This approach is the most suitable option because it balances cost, performance, privacy, and scalability. It is especially appropriate for a Vietnamese AI text-to-video platform that needs to stay affordable during the MVP phase while remaining extensible for later commercial deployment.

## 2. Why This Approach Was Chosen
The project has three core constraints:
- cost efficiency is critical
- local hardware should be leveraged where possible
- the solution should be capable of evolving into a product later

A fully local solution is simple but limited. A fully cloud-based solution is scalable but expensive. A managed SaaS/API-first approach is fast, but it creates dependency on external providers and weakens the local-first positioning.

The hybrid model addresses these tradeoffs most effectively.

## 3. Detailed Comparison of Options

### Option A: Fully Local Deployment
Description:
- All services run on one workstation or local server.
- LLM inference, TTS, image/video generation, and FFmpeg rendering are executed locally.

Pros:
- Lowest ongoing cost
- Maximum privacy and data control
- No dependency on cloud GPU providers
- Ideal for experimentation and personal use

Cons:
- Limited concurrent processing
- Hardware bottlenecks may slow down generation
- Harder to serve multiple users simultaneously
- More difficult to maintain and scale

Best for:
- Solo developers
- Small experiments
- Internal prototypes

### Option B: Hybrid Cloud + Local Rendering
Description:
- Cloud services manage APIs, queueing, status tracking, and storage.
- Local GPU machines perform the computationally heavy tasks.

Pros:
- Best balance of cost and performance
- Matches the architecture already described in the project plan
- Suitable for MVP and early commercialization
- Keeps cloud costs manageable
- Allows local inference without sacrificing orchestration features

Cons:
- Requires integration between cloud and local environments
- Needs network reliability between services
- More complex than a single-machine setup

Best for:
- MVPs
- Small teams
- Commercial products with moderate volume

### Option C: Fully Cloud-Based Deployment
Description:
- All AI inference and rendering happen in cloud GPU services.

Pros:
- Easy horizontal scaling
- Better concurrency and availability
- Simpler remote deployment model for multi-user workloads

Cons:
- Highest monthly cost
- Dependence on paid GPU infrastructure
- Less aligned with the goal of keeping operations inexpensive

Best for:
- Large-scale production platforms
- Teams comfortable with high cloud spend

### Option D: Managed Third-Party API Approach
Description:
- The platform uses third-party APIs for LLM, TTS, and video generation, then wraps them in a custom product experience.

Pros:
- Faster to build initially
- Less engineering effort for core model integration
- Good for quick demos

Cons:
- Vendor lock-in
- Operating costs can become unpredictable
- Less control over quality and compliance

Best for:
- Fast prototyping
- Validation before investing in custom infrastructure

## 4. Recommended Technical Design

### 4.1 Cloud Layer
The cloud layer should provide orchestration and operational services.

Recommended components:
- API Gateway for external access
- Spring Boot backend for request handling
- SQS queue for async job processing
- S3 storage for generated assets and final videos
- DynamoDB for job metadata and status
- CloudWatch for monitoring and logs
- Secrets Manager for credentials and keys

### 4.2 Local Execution Layer
The local layer should host the compute-intensive tasks.

Recommended components:
- Ollama or LM Studio for LLM inference
- TTS engine such as Piper or Coqui TTS
- ComfyUI or Draw Things for image/video generation
- FFmpeg for editing and export
- A shared storage mount or upload mechanism to access assets from the cloud side

### 4.3 Workflow Execution Model
The platform should follow this execution flow:
1. User sends a prompt through the API.
2. The system creates a job record and places it in the queue.
3. A worker retrieves the task.
4. The worker requests script and scene generation from the local LLM.
5. The system calls TTS and media generation services.
6. FFmpeg assembles the final video and subtitles.
7. The output is uploaded to S3.
8. The job status is updated and sent back to the user.

## 5. Recommended Stack

### Backend
- Java 21
- Spring Boot 3.x
- Spring Web / Spring WebFlux
- Spring AI
- Lombok
- Swagger/OpenAPI

### AI and Media
- Ollama or LM Studio
- Spring AI Ollama connector
- Piper / Coqui TTS
- ComfyUI or Draw Things
- FFmpeg

### Storage and Messaging
- Amazon S3
- Amazon DynamoDB
- Amazon SQS
- Optional SNS for notifications

### DevOps
- Docker
- Docker Compose
- GitHub Actions
- AWS ECS or EC2 for deployment

## 6. Rollout Plan

### Phase 1 – MVP
Goal: validate the core video-generation flow.

Deliverables:
- simple prompt-to-video pipeline
- local LLM integration
- Vietnamese TTS
- FFmpeg-based assembly
- job status tracking

### Phase 2 – Optimization
Goal: improve quality and stability.

Deliverables:
- retry logic and failure handling
- better subtitle timing
- improved asset organization and cleanup
- support for multiple concurrent jobs

### Phase 3 – Commercialization
Goal: turn the MVP into a real product.

Deliverables:
- user authentication
- billing and credits
- hosted rendering
- API access for partners

## 7. Operational Considerations
- Local hardware should be monitored for GPU utilization and temperature.
- Failed jobs should be retried with clear logging.
- Media files should be retained temporarily and cleaned based on retention rules.
- The system should support a simple fallback path when a media generation step fails.

## 8. Final Recommendation
The hybrid architecture is the most suitable choice for this project because it aligns with the original business goals: low cost, local AI support, Chinese/Vietnamese language focus, and a realistic path to commercialization.

In practical terms, the project should be built as:
- a Spring Boot-based orchestration platform in the cloud
- a local GPU-enabled rendering worker for AI processing
- a queue-based pipeline for asynchronous job execution

This gives the product a strong foundation without overcommitting to expensive cloud GPU usage during the early stages.

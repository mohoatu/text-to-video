# Business Requirements Document

## 1. Executive Summary
This document defines the business requirements for a Vietnamese-language, local-first AI text-to-video platform. The solution will enable users to transform a simple prompt into a short video containing narration, visuals, subtitles, and final export, while keeping operating costs low through local inference and modular architecture.

## 2. Business Objective
The project aims to provide an affordable and practical video-generation platform for Vietnamese creators, businesses, and educators. The initial focus is on an MVP that can run locally on Mac M1 or GPU-based workstation environments, with a clear path to future SaaS and enterprise deployment.

## 3. Scope of the Solution
The platform will support:
- Vietnamese prompt input
- Automatic script generation
- Scene breakdown with narration and visual prompts
- Vietnamese text-to-speech
- Local or hybrid visual generation
- Subtitle generation and video export
- Basic job tracking and history

## 4. Target Users
- Individual content creators and influencers
- Small and medium businesses
- Marketing and communications teams
- Education and training providers
- Agencies requiring localized video production

## 5. Business Requirements
### 5.1 Core Functional Requirements
- Users can submit a text prompt and receive a generated short video.
- The system must support asynchronous processing for long-running jobs.
- Users must be able to monitor job status: queued, processing, completed, or failed.
- The platform must store generated assets and final exports.
- The system must support future expansion to multilingual workflows, with Vietnamese as the initial priority.

### 5.2 Non-Functional Requirements
- Low operational cost for MVP deployment
- Reasonable performance on local hardware
- Secure handling of user content and generated files
- Clear logging and error reporting
- Modular design to support future cloud scaling

## 6. Success Criteria
- The platform can generate a 30–60 second video within acceptable processing time.
- The cost per generated video is lower than comparable cloud-only solutions.
- User adoption and completion rate are sufficient for iterative product improvement.
- The system demonstrates strong potential for commercialization.

## 7. Business Risks and Constraints
- Local hardware limitations may affect output speed and quality.
- Vietnamese TTS and subtitle quality may vary by model.
- Model performance may differ across hardware environments.
- User adoption depends on reliability, ease of use, and output quality.

## 8. Expected Business Outcome
The project will establish a viable local-first AI video generation product that can be used as an MVP, validated with real users, and later monetized through subscriptions, enterprise packages, or API-based services.

## 9. Assumptions
- The MVP will focus on short-form content rather than full cinematic production.
- Local inference is acceptable for early-stage validation.
- The product will evolve from a self-hosted solution to a commercial platform over time.

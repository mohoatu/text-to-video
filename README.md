### Tài Liệu Phát Triển Hệ Thống Local AI Text-to-Video Tiếng Việt

**Kiến trúc:** Java Spring Boot, Spring AI, Local LLM (Ollama/LM Studio), AWS Services & FFmpeg/ComfyUI Layer. 

### 1. Kế Hoạch Tổng Quan (Project Planning)

### 1.1. Mục Tiêu Dự Án

Xây dựng một hệ thống tự động hóa (Pipeline) nhận đầu vào là một chủ đề/văn bản tiếng Việt, sau đó tự động lập kịch bản, tạo giọng đọc, sinh hình ảnh/video minh họa và biên tập thành một video ngắn (.mp4) hoàn chỉnh có chèn sẵn phụ đề. 

### 1.2. Đối Tượng Sử Dụng & Phạm Vi

* **Môi trường vận hành:** Lai (Hybrid) — Xử lý logic doanh nghiệp và điều phối trên Cloud (AWS), xử lý sinh render nặng tận dụng phần cứng cục bộ (M1 Mac / GPU PC) qua API local để tiết kiệm chi phí Cloud.
* **Ngôn ngữ mục tiêu:** Tiếng Việt (Yêu cầu xử lý chính xác 6 thanh điệu, ngữ điệu tự nhiên theo vùng miền và tự động tạo phụ đề chính xác).

### 1.3. Các Giai Đoạn Phát Triển (Milestones)

* **Giai đoạn 1 (MVP - Lõi Hệ Thống):** Xây dựng Spring AI Agent lên kịch bản cấu trúc JSON và tích hợp API giọng đọc (TTS) Tiếng Việt.
* **Giai đoạn 2 (Xử lý Đồ Họa & Video):** Kết nối kênh sinh video (Local ComfyUI hoặc Cloud API) và viết bộ engine biên tập bằng FFmpeg.
* **Giai đoạn 3 (Bất Đồng Bộ & Hạ Tầng):** Tích hợp AWS SQS, S3 để xử lý hàng đợi tránh hiện tượng nghẽn mạch/timeout kết nối, đóng gói hệ thống.

### 2. Thiết Kế Kiến Trúc Hệ Thống (System Architecture)

Hệ thống được thiết kế dựa trên mô hình **AI Agent Orchestration**, ánh xạ trực tiếp từ sơ đồ gốc nhưng thay đổi luồng dữ liệu sang dạng xử lý pipeline sinh file phương tiện. 

[Khách Hàng (Web/App)]
         │
         ▼ (1) Gửi Prompt Tiếng Việt (HTTP POST)
[Amazon API Gateway]
         │
         ▼ (2) Định tuyến
[SPRING BOOT BACKEND CORE]
         │
         ├───► (3) Gửi kịch bản gốc ──► [Amazon SQS (Hàng đợi)]
         │                                      │
         │   ┌──────────────────────────────────┘
         ▼   ▼ (Xử lý Bất Đồng Bộ - Async Background Worker)
[SPRING AI AGENT ORCHESTRATOR]
         │
         ├──► (4) Gửi Prompt cấu trúc ──► [Local LLM via Ollama / LM Studio]
         │◄────────────────────────────── [Trả về JSON kịch bản chia phân cảnh]
         │
         ├──► (5) Kích hoạt Tool Calling Layer (Thực thi các Function độc lập)
         │         │
         │         ├──► [Tool 1: VN TTS Service] ────► Sinh tiếng (.mp3) chuẩn thanh điệu
         │         ├──► [Tool 2: Video Gen Service] ─► Gọi Cloud API hoặc Local ComfyUI sinh .mp4
         │         └──► [Tool 3: RAG Enterprise] ───► Lấy thêm ngữ cảnh dữ liệu từ OpenSearch (Nếu có)
         │
         ├──► (6) Upload tất cả nguyên liệu thô lên [Amazon S3 Storage]
         │
         ├──► (7) Triển khai Khối Biên Tập [FFmpeg Service / AWS Lambda]
         │         │
         │         └─► Tổng hợp Audio + Video Clip + Burn-in Auto Subtitles ──► Video Thành Phẩm (.mp4)
         │
         ▼ (8) Lưu thông tin phiên/lịch sử
[Amazon DynamoDB] ◄──► [WebSocket Thông Báo Hoàn Thành] ──► [Trả Video Cho Khách Hàng]

### Thành Phần Chi Tiết Trong Kiến Trúc:

1. **Users / Channels:** Giao diện Web/Mobile nhận yêu cầu làm video từ user (ví dụ: *"Làm video TikTok 60 giây giới thiệu về Tháp Rùa Hồ Gươm"*).
2. **Spring AI Agent Core:** Đóng vai trò "Tổng đạo diễn". Nhận kịch bản thô, gọi mô hình local qua Ollama/LM Studio để chia nhỏ video thành các phân cảnh (Scene 1, Scene 2 kèm text đọc và prompt vẽ tranh cụ thể).
3. **Tool Calling Layer (Hệ thống tạo nguyên liệu):** 

  * *Kênh Tiếng:* Tích hợp ElevenLabs, Vbee hoặc FPT.AI API để nhận diện văn bản tiếng Việt và chuyển sang file âm thanh chất lượng cao.
  * *Kênh Hình:* Gửi prompt vẽ/sinh video sang **Amazon Nova Reel** hoặc **Local ComfyUI API** (nếu chạy local trên Mac M1) để lấy về các block video 4-5 giây.
4. **Events & Async Processing (AWS SQS & S3):** Vì quá trình sinh video kéo dài, SQS lưu hàng đợi, S3 làm kho chứa trung gian cho file audio/video thô và file video cuối cùng.
5. **FFmpeg Processing Block:** Service chạy lệnh ngầm để nối chuỗi clip, đè nhạc nền, sync khớp thời gian của hình theo tiếng, và tự động tạo phụ đề tiếng Việt chạy trên màn hình.

### 3. Danh Sách Nhiệm Vụ Triển Khai (Tasklist)

Dưới đây là bảng phân rã công việc cụ thể cần thực hiện, bạn có thể check-list theo tiến độ: 

### Tầng Lõi Ứng Dụng (Spring Boot Backend)

* [ ] **Task 1.1:** Khởi tạo project Spring Boot (v3.x), thêm các thư viện spring-ai-ollama-spring-boot-starter, spring-boot-starter-web, lombok.
* [ ] **Task 1.2:** Thiết lập cấu hình kết nối local LLM trong file application.yml (ví dụ: endpoint http://localhost:11434, model name qwen2.5:7b).
* [ ] **Task 1.3:** Viết REST Controller tiếp nhận chuỗi yêu cầu prompt làm video từ giao diện người dùng.

### Tầng Trí Tuệ Nhân Tạo (Spring AI & Local LLM)

* [ ] **Task 2.1:** Thiết kế **System Prompt bằng tiếng Việt** chuyên biệt để ép mô hình local (ví dụ: Qwen, Llama, Mistral) luôn trả về dữ liệu có cấu trúc JSON cố định (Ví dụ: gồm mảng các phân cảnh, mỗi phân cảnh bắt buộc phải có trường voice_text và visual_prompt).
* [ ] **Task 2.2:** Viết mã Java bóc tách (Parse) dữ liệu JSON đầu ra từ LLM thành một danh sách các Object (List<Scene>) trong mã nguồn.

### Tầng Tích Hợp Công Cụ (Tool Calling Layer)

* [ ] **Task 3.1 (Kênh Tiếng):** Viết VietnameseTTSService kết nối với API (ElevenLabs/Vbee). Kiểm thử gửi text tiếng Việt có dấu, đảm bảo tổng hợp ra file âm thanh đúng cao độ.
* [ ] **Task 3.2 (Kênh Hình - Chọn 1 trong 2):** 

  * *Phương án Local AI:* Viết client kết nối **Ollama / LM Studio** để gọi mô hình local cho việc sinh kịch bản và prompt hình ảnh.
  * *Phương án Local Mac M1:* Thiết lập **ComfyUI / Draw Things**, bật chế độ mở port API (--listen). Viết LocalVideoGenService trong Java để gửi HTTP Request chứa prompt hình ảnh sang máy local, nhận về file video ngắn.
* [ ] **Task 3.3 (Lưu trữ):** Cấu hình AmazonS3Client để tự động tải các file audio và video ngắn tạo ra từ các tool lên một Bucket tạm thời.

### Tầng Biên Tập & Xử Lý Bất Đồng Bộ (FFmpeg & Queue)

* [ ] **Task 4.1:** Cài đặt công cụ dòng lệnh **FFmpeg** lên máy chạy server (hoặc đóng gói vào Dockerfile).
* [ ] **Task 4.2:** Viết hàm Java thực thi lệnh hệ thống (ProcessBuilder) để chạy lệnh FFmpeg: 

  * Ghép (Concatenate) chuỗi các file video ngắn thành một video dài duy nhất.
  * Trộn (Mix) file âm thanh giọng đọc vào video.
  * Tự động tính toán thời lượng (duration) từng phân cảnh để kéo giãn/co ngắn clip cho khớp giọng đọc.
  * Sử dụng bộ lọc drawtext hoặc file .srt để ép cứng phụ đề tiếng Việt vào video.
* [ ] **Task 4.3:** Cấu hình **Amazon SQS Listener** trong Spring Boot. Khi có request, đẩy tin nhắn vào hàng đợi. Worker ngầm tự nhặt tin nhắn và chạy toàn bộ chuỗi Task 2.1 -> Task 4.2 bất đồng bộ để tránh bị nghẽn mạng (Timeout).

### Kiểm Thử & Hoàn Thiện (Testing & Deploy)

* [ ] **Task 5.1:** Test End-to-End với prompt tiếng Việt đầy đủ (Ví dụ: *"Kể câu chuyện Thạch Sanh Lý Thanh trong 30 giây"*).
* [ ] **Task 5.2:** Kiểm tra xung đột phông chữ tiếng Việt (lỗi hiển thị dấu) khi FFmpeg tiến hành ghi phụ đề lên video.
* [ ] **Task 5.3:** Viết Dockerfile tối ưu (chứa cả môi trường chạy Java và phần mềm FFmpeg), sẵn sàng đẩy lên Amazon ECS khi cần phân phối thực tế.

<img width="1536" height="1024" alt="image" src="https://github.com/user-attachments/assets/fa38c6ba-426e-439a-926a-b14d5b8a3c20" />


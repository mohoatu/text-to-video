package com.example.videogen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.videogen.model.JobStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Exercises the full create-job -> async pipeline -> completed-output flow
 * using the offline "stub" profile, so it runs without Ollama, piper,
 * ComfyUI, or ffmpeg installed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("stub")
class JobPipelineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createJobRunsThroughStubPipelineToCompletion() throws Exception {
        String requestBody = """
                {"prompt": "Ho Guom la mot ho nuoc noi tieng o Ha Noi. Vao buoi sang co rat nhieu nguoi tap the duc quanh ho."}
                """;

        MvcResult createResult = mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String jobId = created.get("id").asText();
        assertThat(created.get("status").asText()).isEqualTo(JobStatus.PENDING.name());

        JsonNode finalJob = pollUntilTerminal(jobId);

        assertThat(finalJob.get("status").asText())
                .as("job errorMessage: %s", finalJob.path("errorMessage").asText(null))
                .isEqualTo(JobStatus.COMPLETED.name());

        mockMvc.perform(get("/api/jobs/{id}/output", jobId))
                .andExpect(status().isOk());
    }

    private JsonNode pollUntilTerminal(String jobId) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        JsonNode job;
        do {
            MvcResult result = mockMvc.perform(get("/api/jobs/{id}", jobId))
                    .andExpect(status().isOk())
                    .andReturn();
            job = objectMapper.readTree(result.getResponse().getContentAsString());
            String status = job.get("status").asText();
            if (JobStatus.COMPLETED.name().equals(status) || JobStatus.FAILED.name().equals(status)) {
                return job;
            }
            Thread.sleep(100);
        } while (System.currentTimeMillis() < deadline);
        throw new AssertionError("Job " + jobId + " did not reach a terminal state in time, last seen: " + job);
    }
}

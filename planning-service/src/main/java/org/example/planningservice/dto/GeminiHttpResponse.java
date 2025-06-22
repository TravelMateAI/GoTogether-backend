package org.example.planningservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeminiHttpResponse {
    public List<Candidate> candidates;
    public String modelVersion;
    public UsageMetadata usageMetadata;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Candidate {
        public Content content;
        public String finishReason;
        public Double avgLogprobs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        public List<Part> parts;
        public String role;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        public String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageMetadata {
        public int candidatesTokenCount;
        public List<TokensDetail> candidatesTokensDetails;
        public int promptTokenCount;
        public List<TokensDetail> promptTokensDetails;
        public int totalTokenCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokensDetail {
        public String modality;
        public int tokenCount;
    }
}


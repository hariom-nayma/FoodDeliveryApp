package com.fooddelivery.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadRequest {
    private String gstNumber;
    private String fssaiNumber;
    private List<DocumentDto> documents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentDto {
        private String type; // Enum name
        private String fileUrl;
    }
}

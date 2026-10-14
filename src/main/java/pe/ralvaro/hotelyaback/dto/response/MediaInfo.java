package pe.ralvaro.hotelyaback.dto.response;

import java.util.UUID;

public record MediaInfo(UUID id, String path, String contentType, String originalName, long byteSize) {
}

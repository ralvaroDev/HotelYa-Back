package pe.ralvaro.hotelyaback.dto.response;

import java.util.Map;

public record ErrorBody(String message, Map<String, String> fields) {
}

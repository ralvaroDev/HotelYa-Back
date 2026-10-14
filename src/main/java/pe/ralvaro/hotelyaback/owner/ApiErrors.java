package pe.ralvaro.hotelyaback.owner;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
@RestControllerAdvice
public class ApiErrors {
    public record ErrorBody(String message, Map<String,String> fields) {}
    @ExceptionHandler(ApiFailure.class) ResponseEntity<ErrorBody> failure(ApiFailure e) { return ResponseEntity.status(e.status).body(new ErrorBody(e.getMessage(),e.fields)); }
    @ExceptionHandler(HttpMessageNotReadableException.class) ResponseEntity<ErrorBody> unreadable() { return ResponseEntity.badRequest().body(new ErrorBody("La solicitud contiene datos inválidos.",Map.of())); }
    @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<ErrorBody> conflict() { return ResponseEntity.status(409).body(new ErrorBody("Los datos entran en conflicto con un registro existente.",Map.of())); }
    @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<ErrorBody> tooLarge() { return ResponseEntity.status(413).body(new ErrorBody("El archivo supera el tamaño permitido.",Map.of("file","Máximo 10 MB por imagen y 100 MB por video."))); }
}

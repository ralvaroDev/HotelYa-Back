package pe.ralvaro.hotelyaback.owner;
import java.util.Map;
public class ApiFailure extends RuntimeException {
    public final int status;
    public final Map<String,String> fields;
    public ApiFailure(int status, String message) { this(status,message,Map.of()); }
    public ApiFailure(int status, String message, Map<String,String> fields) { super(message); this.status=status; this.fields=fields; }
    public static ApiFailure field(String field, String message) { return new ApiFailure(400,"Revisa los campos indicados.", Map.of(field,message)); }
}

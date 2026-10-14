package pe.ralvaro.hotelyaback.security;

import java.util.List;
import org.springframework.security.core.userdetails.User;

public final class OwnerPrincipal extends User {
    private final Long ownerId;

    public OwnerPrincipal(Long ownerId, String email, String passwordHash) {
        super(email,passwordHash,List.of());
        this.ownerId=ownerId;
    }

    public Long ownerId() { return ownerId; }
}

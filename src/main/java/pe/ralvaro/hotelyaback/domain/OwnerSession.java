package pe.ralvaro.hotelyaback.domain;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="owner_session")
public class OwnerSession {
    @Id public UUID id;
    @Column(name="owner_id",nullable=false) public Long ownerId;
    @Column(name="refresh_hash",nullable=false,length=64) public String refreshHash;
    @Column(name="expires_at",nullable=false) public Instant expiresAt;
    @Column(nullable=false) public boolean revoked;
    protected OwnerSession() {}
    public OwnerSession(Long ownerId) { id=UUID.randomUUID(); this.ownerId=ownerId; expiresAt=Instant.now().plusSeconds(7*86400); }
}

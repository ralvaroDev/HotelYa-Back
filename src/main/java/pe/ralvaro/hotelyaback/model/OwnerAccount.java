package pe.ralvaro.hotelyaback.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "owner_account")
public class OwnerAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, unique = true, length = 254)
    public String email;
    @Column(name = "password_hash", nullable = false, length = 100)
    public String passwordHash;
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    protected OwnerAccount() {
    }

    public OwnerAccount(String email, String hash) {
        this.email = email;
        passwordHash = hash;
        createdAt = Instant.now();
    }
}

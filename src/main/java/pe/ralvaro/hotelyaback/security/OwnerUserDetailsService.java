package pe.ralvaro.hotelyaback.security;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.ralvaro.hotelyaback.repository.OwnerAccountRepository;

@Service
@Transactional(readOnly = true)
public class OwnerUserDetailsService implements UserDetailsService {
    private final OwnerAccountRepository accounts;

    public OwnerUserDetailsService(OwnerAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public OwnerPrincipal loadUserByUsername(String email) {
        var account = accounts.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Correo o contraseña incorrectos."));
        return new OwnerPrincipal(account.id, account.email, account.passwordHash);
    }
}

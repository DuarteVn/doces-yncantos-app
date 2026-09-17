package com.montseubolo.api.repository;

import java.util.Optional;

import com.montseubolo.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}

package com.montseubolo.api.repository;

import com.montseubolo.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    // TODO: adicionar as consultas customizadas (ex.: findByEmail).

}

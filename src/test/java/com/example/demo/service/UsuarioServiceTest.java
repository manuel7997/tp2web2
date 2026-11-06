package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.dto.auth.RegistroRequestDTO;
import com.example.demo.dto.auth.UsuarioDTO;
import com.example.demo.exception.ConflictoException;
import com.example.demo.model.Rol;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    // Encoder real (no un mock): así se comprueba que lo guardado es un hash BCrypt de verdad.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UsuarioService service;

    @BeforeEach
    void preparar() {
        service = new UsuarioService(usuarioRepository, passwordEncoder);
    }

    @Test
    void registrar_contrasenaValida_guardaElHashYNuncaElTextoPlano() {
        when(usuarioRepository.existePorEmail("ana@example.com")).thenReturn(false);
        when(usuarioRepository.guardar(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        service.registrar(new RegistroRequestDTO("ana@example.com", "clave12345"));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).guardar(guardado.capture());
        String hash = guardado.getValue().getPasswordHash();
        assertThat(hash).isNotEqualTo("clave12345");
        assertThat(hash).doesNotContain("clave12345");
        assertThat(hash).startsWith("$2"); // formato BCrypt
        assertThat(passwordEncoder.matches("clave12345", hash)).isTrue();
    }

    @Test
    void registrar_usuarioNuevo_tieneRolUserYEmailNormalizado() {
        when(usuarioRepository.existePorEmail("ana@example.com")).thenReturn(false);
        when(usuarioRepository.guardar(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        UsuarioDTO registrado = service.registrar(new RegistroRequestDTO("  Ana@Example.COM ", "clave12345"));

        assertThat(registrado.rol()).isEqualTo(Rol.USER);
        assertThat(registrado.email()).isEqualTo("ana@example.com");
    }

    @Test
    void registrar_emailExistente_lanzaConflictoYNoGuarda() {
        when(usuarioRepository.existePorEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(new RegistroRequestDTO("ana@example.com", "clave12345")))
                .isInstanceOf(ConflictoException.class);

        verify(usuarioRepository, never()).guardar(any());
    }
}
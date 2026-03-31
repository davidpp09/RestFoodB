package restaurante.api.usuario;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id_usuarios")
@Table(name = "usuarios")
@Entity(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_usuarios;

    @Column(unique = true, nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Roles rol;

    @Column(nullable = false)
    private String contrasena;

    @Column(nullable = false)
    private Boolean estatus;

    @Column(unique = true, nullable = false)
    private String email;

    public Usuario(DatosRegistroUsuario datosRegistroUsuario) {
        this.id_usuarios = null;
        this.nombre = datosRegistroUsuario.nombre().toUpperCase();
        this.rol = datosRegistroUsuario.rol();
        this.contrasena = datosRegistroUsuario.contrasena();
        this.estatus = true;
        this.email = datosRegistroUsuario.email();
    }

    public Usuario(Long id_usuarios) {
        this.id_usuarios = id_usuarios;
    }

    public void actualizarInformacion(@Valid DatosActualizacionUsuario datos) {
        if (datos.nombre() != null) {
            this.nombre = datos.nombre();
        }
        if (datos.email() != null) {
            this.email = datos.email();
        }

    }

    public void eliminarUsuario(Long id) {
        this.estatus = false;
    }

    public void activarUsuario(Long id) {
        this.estatus = true;
    }
}

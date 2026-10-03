package restaurante.api.mesa;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import restaurante.api.infra.errores.ValidacionException;
import restaurante.api.usuario.Usuario;

@Getter
@NoArgsConstructor
@EqualsAndHashCode(of = "id_mesas")
@Table(name = "mesas")
@Entity(name = "mesa")
public class Mesa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_mesas;

    @Column(unique = true, nullable = false)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

    // La mesera que atiende esta mesa. Una sola: asignarla a otra se la quita a la
    // anterior. null = sin asignar (solo ADMIN/DEV pueden abrirla).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_asignado")
    private Usuario usuarioAsignado;

    // Baja lógica: la mesa deja de aparecer en las tablets pero su historial queda.
    @Column(nullable = false)
    private Boolean activa = true;

    public Mesa(Long id_mesas) {
        this.id_mesas = id_mesas;
    }

    public Mesa(Long id_mesas, String numero, Estado estado) {
        this.id_mesas = id_mesas;
        this.numero = numero;
        this.estado = estado;
        this.activa = true;
    }

    public Mesa(String numero, Usuario usuarioAsignado) {
        this.numero = numero;
        this.estado = Estado.LIBRE;
        this.activa = true;
        this.usuarioAsignado = usuarioAsignado;
    }

    public void abrirMesa() {
        if (!Boolean.TRUE.equals(this.activa)) {
            throw new ValidacionException("La mesa " + numero + " está dada de baja.");
        }
        if (this.estado == Estado.OCUPADA) {
            throw new ValidacionException("La mesa ya está en uso, no se puede abrir otra cuenta.");
        }
        this.estado = Estado.OCUPADA;
    }

    public void liberar() {
        this.estado = Estado.LIBRE;
    }

    public boolean estaAsignadaA(Usuario usuario) {
        return usuarioAsignado != null && usuario != null
                && usuarioAsignado.getId_usuarios().equals(usuario.getId_usuarios());
    }

    public void asignarA(Usuario usuario) {
        this.usuarioAsignado = usuario;
    }

    public void renombrar(String numero) {
        this.numero = numero;
    }

    // Una mesa con cuenta abierta no se da de baja: la mesera perdería la orden de vista.
    public void darDeBaja() {
        if (this.estado == Estado.OCUPADA) {
            throw new ValidacionException("La mesa " + numero + " tiene una cuenta abierta; ciérrala antes de darla de baja.");
        }
        this.activa = false;
    }

    public void reactivar() {
        this.activa = true;
    }
}

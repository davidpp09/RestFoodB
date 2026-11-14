package restaurante.api.infra.errores;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;

/**
 * Traduce cada excepción a un código HTTP y al MISMO cuerpo de error
 * (DatosRespuestaError). Antes había tres formas: cuerpo vacío en algunos 404,
 * un arreglo suelto en las validaciones y {"mensaje"} en el resto, y el
 * frontend, que solo lee `mensaje`, mostraba "Error" a secas en los dos primeros.
 *
 * Regla para lo que no es 500: el mensaje se escribe para quien usa la tablet,
 * no para quien programa. En los 500 nunca se manda el detalle de la excepción
 * (puede traer SQL, rutas o nombres de tablas): se manda un texto genérico y el
 * detalle completo va al log del servidor.
 */
@RestControllerAdvice
public class TratadorDeErrores {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErrores.class);

    // ---------- 400: la petición viene mal ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DatosRespuestaError> validacionDeCampos(MethodArgumentNotValidException e) {
        var errores = e.getFieldErrors().stream().map(DatosErrorValidacion::new).toList();
        // El mensaje es el del primer campo: es lo que la pantalla de contraseña
        // ya mostraba leyendo el arreglo viejo, así que el usuario ve lo mismo.
        String mensaje = errores.isEmpty()
                ? "Hay datos inválidos en la petición."
                : errores.get(0).error();
        return ResponseEntity.badRequest().body(new DatosRespuestaError(mensaje, errores));
    }

    // Reglas de negocio propias ("Máximo 6 platillos activos", "La mesa ya está ocupada")
    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<DatosRespuestaError> reglaDeNegocio(ValidacionException e) {
        return ResponseEntity.badRequest().body(new DatosRespuestaError(e.getMessage()));
    }

    // JSON mal formado o un valor que no existe en un enum (p. ej. "tipo": "REGALO").
    // Antes caía en el 500 genérico, como si fuera una falla del servidor.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<DatosRespuestaError> cuerpoIlegible() {
        return ResponseEntity.badRequest().body(new DatosRespuestaError(
                "La petición no tiene el formato esperado. Revisa los datos enviados."));
    }

    // ?fecha=ayer en vez de ?fecha=2026-11-14, o un id que no es número. Antes: 500.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<DatosRespuestaError> parametroConTipoIncorrecto(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(new DatosRespuestaError(
                "El valor de '" + e.getName() + "' no es válido."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<DatosRespuestaError> parametroFaltante(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest().body(new DatosRespuestaError(
                "Falta el parámetro '" + e.getParameterName() + "'."));
    }

    // ---------- 401 / 403 ----------

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<DatosRespuestaError> credencialesInvalidas() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new DatosRespuestaError(
                "Credenciales inválidas. Por favor, verifica tu correo y contraseña."));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<DatosRespuestaError> usuarioDesactivado() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new DatosRespuestaError(
                "Este usuario está desactivado. Contacta al administrador."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<DatosRespuestaError> accesoDenegado() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new DatosRespuestaError(
                "Acceso denegado. No tienes los permisos necesarios para este recurso."));
    }

    // ---------- 404 / 405 / 409 ----------

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<DatosRespuestaError> noEncontrado(RecursoNoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new DatosRespuestaError(e.getMessage()));
    }

    // EntityNotFoundException: getReferenceById() sobre un id que no existe.
    // NoSuchElementException: un `findById(id).orElseThrow()` sin mensaje. Antes
    // el primero respondía 404 con el cuerpo VACÍO y el segundo 500.
    @ExceptionHandler({EntityNotFoundException.class, NoSuchElementException.class})
    public ResponseEntity<DatosRespuestaError> registroInexistente() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new DatosRespuestaError(
                "No se encontró el registro solicitado. Puede que ya se haya eliminado."));
    }

    // Una ruta que no existe. Desde Spring 6.1 esto llega como excepción, y sin
    // este manejador el genérico de abajo lo convertía en un 500.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<DatosRespuestaError> rutaInexistente() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new DatosRespuestaError(
                "La ruta solicitada no existe."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<DatosRespuestaError> metodoNoPermitido(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(new DatosRespuestaError(
                "Esta ruta no acepta " + e.getMethod() + "."));
    }

    // Violaciones de integridad (FK en uso, nombres duplicados)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<DatosRespuestaError> conflictoDeIntegridad() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new DatosRespuestaError(
                "No se puede completar la operación: el registro está en uso por otros datos o ya existe uno igual."));
    }

    // ---------- 500 ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DatosRespuestaError> errorInterno(Exception e, HttpServletRequest peticion) {
        // Con la ruta y el método en el log: antes era un printStackTrace() sin
        // fecha ni contexto, y no había forma de saber qué pantalla lo provocó.
        log.error("Error no controlado en {} {}", peticion.getMethod(), peticion.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new DatosRespuestaError(
                "Error interno en el servidor."));
    }
}

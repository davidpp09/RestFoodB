-- Cada mesa pasa a tener UNA mesera asignada, guardada en la propia mesa.
--
-- Hasta ahora la mesera solo tenía un número de sección (1, 2 o 3) y la tabla que
-- traducía sección → mesas vivía quemada en el frontend (MeseroPanel.jsx):
--   sección 1 → mesas 1-15, sección 2 → 16-30, sección 3 → 31-50.
-- Con eso no se podía dar de alta una mesera con mesas propias, ni crear la mesa
-- 51 (quedaba fuera de todos los rangos), ni repartir distinto sin desplegar.
--
-- Esta migración copia ese mismo reparto a la base, así que el día que se
-- despliega cada mesera sigue viendo exactamente sus mismas mesas.

ALTER TABLE mesas
    ADD COLUMN id_usuario_asignado BIGINT NULL,
    -- Baja lógica: una mesa con órdenes no se puede borrar (FK RESTRICT protege
    -- el historial), igual que productos.eliminado en V1.
    ADD COLUMN activa BIT(1) NOT NULL DEFAULT b'1',
    ADD CONSTRAINT fk_mesas_usuario_asignado
        FOREIGN KEY (id_usuario_asignado) REFERENCES usuarios (id_usuarios),
    -- Dos mesas "5" harían ambiguo a quién se le lleva la comanda.
    ADD CONSTRAINT uq_mesas_numero UNIQUE (numero);

-- MIN() para que el resultado sea determinista si algún día dos meseras activas
-- comparten sección (hoy no pasa: 3 meseras, una por sección).
UPDATE mesas m
SET m.id_usuario_asignado = (
    SELECT MIN(u.id_usuarios)
    FROM usuarios u
    WHERE u.rol = 'MESERO'
      AND u.estatus = b'1'
      AND u.seccion = CASE
          WHEN m.id_mesas BETWEEN 1  AND 15 THEN 1
          WHEN m.id_mesas BETWEEN 16 AND 30 THEN 2
          WHEN m.id_mesas BETWEEN 31 AND 50 THEN 3
      END
);

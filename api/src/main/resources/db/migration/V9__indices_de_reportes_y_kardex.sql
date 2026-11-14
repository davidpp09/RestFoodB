-- Índices para las consultas que filtran por fecha y estatus.
--
-- Hasta V8, `ordenes` solo tenía índices sobre sus llaves foráneas, así que el
-- corte del día, la cocina, las entregas y las comandas recorrían la tabla
-- completa (EXPLAIN: type=ALL). Con ~165 órdenes al día eso no se nota hoy, pero
-- la tabla solo crece y el corte es justo lo que se corre con prisa al cerrar.
--
-- Regla para el orden de las columnas: primero las que se comparan con `=`,
-- al final la que va por rango (BETWEEN). Un índice compuesto solo sirve de
-- izquierda a derecha, y deja de aprovecharse después de la primera columna
-- de rango.
--
-- Cada índice dice qué consulta atiende (OrdenRepository y compañía), para que
-- el día que una consulta cambie se sepa cuál sobra.

-- Corte del día (estatus = PAGADA, fecha_cierre entre...) y, por su prefijo
-- `estatus`, también el panel de cocina (estatus = PREPARANDO).
CREATE INDEX idx_ordenes_estatus_cierre ON ordenes (estatus, fecha_cierre);

-- Siguiente número de comanda (se calcula al abrir CADA orden, con lock) y
-- "mis comandas": id_usuario = ?, fecha_apertura entre...
-- El índice viejo id_usuario_idx también recorría el historial completo del
-- empleado; este para en el día.
CREATE INDEX idx_ordenes_usuario_apertura ON ordenes (id_usuario, fecha_apertura);

-- Entregas del día: tipo = LLEVAR, fecha_apertura entre...
CREATE INDEX idx_ordenes_tipo_apertura ON ordenes (tipo, fecha_apertura);

-- Comandas del día del panel de admin: solo el rango de fecha_apertura.
CREATE INDEX idx_ordenes_apertura ON ordenes (fecha_apertura);

-- Cancelaciones de platillo de un día o periodo (reportes y comandas):
-- tipo_evento = PLATILLO_CANCELADO, timestamp entre...
CREATE INDEX idx_eventos_tipo_fecha ON eventos_orden (tipo_evento, timestamp);

-- Kardex de un insumo, lo más nuevo primero. Con el índice de una sola columna
-- MySQL encontraba los movimientos pero los tenía que ordenar en memoria
-- (filesort); con (id_insumo, fecha) ya salen ordenados. El id_movimiento que
-- desempata no hace falta ponerlo: InnoDB agrega la llave primaria al final de
-- todo índice secundario. Lo va a necesitar el kardex paginado.
CREATE INDEX idx_movimientos_insumo_fecha ON movimientos_inventario (id_insumo, fecha);

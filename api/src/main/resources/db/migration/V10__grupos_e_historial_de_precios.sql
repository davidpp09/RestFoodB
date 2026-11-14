-- Un precio impreso puede representar varios productos. No se siembran ids:
-- desarrollo y producción tienen catálogos con identificadores distintos.
CREATE TABLE grupos_precio (
    id_grupo_precio BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    clave VARCHAR(60) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    campo VARCHAR(10) NOT NULL,
    CONSTRAINT ck_grupo_campo CHECK (campo IN ('DESAYUNO', 'COMIDA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE grupos_precio_productos (
    id_grupo_precio BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    PRIMARY KEY (id_grupo_precio, id_producto),
    CONSTRAINT fk_grupo_producto_grupo FOREIGN KEY (id_grupo_precio) REFERENCES grupos_precio(id_grupo_precio),
    CONSTRAINT fk_grupo_producto_producto FOREIGN KEY (id_producto) REFERENCES productos(id_productos)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- El alta no tiene precio anterior: NULL evita inventar un cambio desde cero.
-- Las FK impiden borrar productos/usuarios y perder la atribución del historial.
CREATE TABLE historial_precios (
    id_historial BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_producto BIGINT NOT NULL,
    campo VARCHAR(10) NOT NULL,
    precio_anterior DECIMAL(10,2) NULL,
    precio_nuevo DECIMAL(10,2) NOT NULL,
    id_usuario BIGINT NOT NULL,
    fecha DATETIME(6) NOT NULL,
    origen VARCHAR(20) NOT NULL,
    CONSTRAINT fk_historial_producto FOREIGN KEY (id_producto) REFERENCES productos(id_productos),
    CONSTRAINT fk_historial_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuarios),
    CONSTRAINT ck_historial_campo CHECK (campo IN ('DESAYUNO', 'COMIDA')),
    CONSTRAINT ck_historial_origen CHECK (origen IN ('CARTA', 'DIA', 'MENU')),
    INDEX idx_historial_producto_fecha (id_producto, fecha, id_historial)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

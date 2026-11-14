-- Siembra por nombre exacto, nunca por ids del catálogo de una máquina.

-- Solo asocia productos vigentes. Las ausencias quedan visibles en el editor;

-- no inventa productos ni cambia precios para hacer coincidir la plantilla.

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-01', 'Pozole de pollo o cerdo', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-01' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Pozole de pollo' OR BINARY p.nombre = BINARY 'Pozole de cerdo');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-02', 'Pancita chica', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-02' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Pancita chica');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-03', 'Pancita grande', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-03' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Pancita grande');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-04', 'Desayunos - grupo 80', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-04' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Rollos de jamón con ensalada' OR BINARY p.nombre = BINARY 'Huevos con chorizo' OR BINARY p.nombre = BINARY 'Molletes con jamón' OR BINARY p.nombre = BINARY 'Salchichas con huevo' OR BINARY p.nombre = BINARY 'Huevos rancheros' OR BINARY p.nombre = BINARY 'Huevos divorciados');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-05', 'Desayunos - grupo 60', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-05' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Enchiladas sencillas' OR BINARY p.nombre = BINARY 'Enfrijoladas sencillas' OR BINARY p.nombre = BINARY 'Huevos en salsa verde' OR BINARY p.nombre = BINARY 'Molletes sencillos');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-06', 'Desayunos - grupo 70', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-06' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Chilaquiles sencillos' OR BINARY p.nombre = BINARY 'Sopes sencillos' OR BINARY p.nombre = BINARY 'Huevos a la mexicana' OR BINARY p.nombre = BINARY 'Huevos con jamón');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-07', 'Desayunos - grupo 90', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-07' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Molletes con tocino' OR BINARY p.nombre = BINARY 'Omelet de jamón con queso' OR BINARY p.nombre = BINARY 'Omelet poblano' OR BINARY p.nombre = BINARY 'Huevos con tocino' OR BINARY p.nombre = BINARY 'Tacos dorados de pollo' OR BINARY p.nombre = BINARY 'Tacos dorados de jamón con queso' OR BINARY p.nombre = BINARY 'Chuleta ahumada con ensalada');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-08', 'Desayunos - grupo 95', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-08' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Omelet vegetariano' OR BINARY p.nombre = BINARY 'Molletes con chorizo' OR BINARY p.nombre = BINARY 'Enchiladas verdes con pollo' OR BINARY p.nombre = BINARY 'Enfrijoladas de pollo' OR BINARY p.nombre = BINARY 'Enfrijoladas de jamón' OR BINARY p.nombre = BINARY 'Enfrijoladas con jamón' OR BINARY p.nombre = BINARY 'Chilaquiles con huevo' OR BINARY p.nombre = BINARY 'Chilaquiles con chuleta ahumada' OR BINARY p.nombre = BINARY 'Omelet de champiñones con queso' OR BINARY p.nombre = BINARY 'Omelet de queso en salsa verde');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-09', 'Desayunos - grupo 105', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-09' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Molletes con pechuga asada' OR BINARY p.nombre = BINARY 'Molletes con bistec' OR BINARY p.nombre = BINARY 'Chilaquiles con pechuga asada' OR BINARY p.nombre = BINARY 'Chilaquiles con pollo deshebrado' OR BINARY p.nombre = BINARY 'Chilaquiles con chorizo' OR BINARY p.nombre = BINARY 'Enchiladas con pechuga asada' OR BINARY p.nombre = BINARY 'Enchiladas con carne asada' OR BINARY p.nombre = BINARY 'Bistec encebollado con ensalada' OR BINARY p.nombre = BINARY 'Pechuga encebollada con ensalada' OR BINARY p.nombre = BINARY 'Pechuga a la mexicana con ensalada' OR BINARY p.nombre = BINARY 'Pechuga empanizada con ensalada' OR BINARY p.nombre = BINARY 'Milanesa de res con ensalada' OR BINARY p.nombre = BINARY 'Enchiladas de mole con pollo' OR BINARY p.nombre = BINARY 'Ensalada California' OR BINARY p.nombre = BINARY 'Hotcakes');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-10', 'Desayunos - grupo 110', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-10' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Chilaquiles con carne asada' OR BINARY p.nombre = BINARY 'Carne asada con papas' OR BINARY p.nombre = BINARY 'Sopecitos con pollo deshebrado' OR BINARY p.nombre = BINARY 'Hamburguesa con papas' OR BINARY p.nombre = BINARY 'Alambre de res' OR BINARY p.nombre = BINARY 'Alambre de pollo' OR BINARY p.nombre = BINARY 'Sopecitos con pechuga asada' OR BINARY p.nombre = BINARY 'Sopecitos con bistec asado' OR BINARY p.nombre = BINARY 'Chilaquiles con pechuga empanizada' OR BINARY p.nombre = BINARY 'Chilaquiles con milanesa' OR BINARY p.nombre = BINARY 'Milanesa con papas' OR BINARY p.nombre = BINARY 'Pechuga empanizada con papas');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-11', 'Arrachera gratinada', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-11' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Arrachera gratinada con papas');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-12', 'Bisteck o pechuga a la Tampiqueña', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-12' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Bistec a la Tampiqueña' OR BINARY p.nombre = BINARY 'Pechuga a la Tampiqueña');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-13', 'Enchiladas suizas', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-13' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Enchiladas suizas');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-14', 'Enfrijoladas a la Veracruzana', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-14' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Enfrijoladas a la Veracruzana');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-15', 'Chilaquiles c/ arrachera', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-15' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Chilaquiles con arrachera');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-16', 'Filete de pescado c/ papas', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-16' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Filete empanizado con papas');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-17', 'Cecina c/ guacamole', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-17' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Cecina con guacamole');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-18', 'Pollo Esp. c/ mole Poblano', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-18' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Pollo especial con mole Poblano');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-19', 'Arrachera a la Tampiqueña', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-19' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Arrachera a la Tampiqueña');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-20', 'Pechuga a la Cordon Blue', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-20' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Pechuga a la cordon blue con papas');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-21', 'Alambre de arrachera', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-21' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Alambre de arrachera');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-22', 'Oreja de Elefante', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-22' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Milanesa oreja de elefante');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-23', 'Chilaquiles con Cecina', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-23' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Chilaquiles con Cecina');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-24', 'Té', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-24' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Té');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-25', 'Coctel de fruta mixta', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-25' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Coctel de fruta mixta');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-26', 'Café de olla', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-26' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Café de olla');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-27', 'Coctel de fruta con miel y granola', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-27' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Coctel de fruta con miel y granola');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-28', 'Pan c / mantequilla y azúcar', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-28' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Pan con mantequilla y azúcar');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-29', 'Jugo de naranja', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-29' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Jugo de naranja');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-30', 'Refresco', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-30' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Refresco');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-31', 'Licuados (fresa,chocolate, platano)', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-31' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Licuados (fresa, chocolate, plátano)');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-32', 'Nescafé', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-32' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Nescafé');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-33', 'Orden de papas a la francesa', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-33' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Orden de papas a la francesa');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-34', 'Jarra de agua', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-34' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Jarra de agua del día');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('desayuno-35', 'Desechable para llevar', 'DESAYUNO');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'desayuno-35' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Desechable para llevar');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-01', 'POZOLE DE POLLO O CERDO', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-01' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Pozole de pollo' OR BINARY p.nombre = BINARY 'Pozole de cerdo');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-02', 'PANCITA (CHICA)', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-02' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Pancita chica');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-03', 'Pancita grande', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-03' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Pancita grande');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-04', 'FILETE EMPANIZADO CON PAPAS', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-04' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Filete empanizado con papas');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-05', 'CHILE EN NOGADA', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-05' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Chile en nogada');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-06', 'PLATO NORTEÑO', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-06' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Plato norteño ');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-07', 'ARRACHERA GRATINADA CON PAPAS', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-07' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Arrachera gratinada con papas');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-08', 'MOJARRA FRITA CON ENSALADA', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-08' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Mojarra frita con ensalada');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-09', 'Comidas - grupo 80', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-09' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Huevos divorciados' OR BINARY p.nombre = BINARY 'Enchiladas de mole sencillas' OR BINARY p.nombre = BINARY 'Huevos rancheros' OR BINARY p.nombre = BINARY 'Huevos a la mexicana' OR BINARY p.nombre = BINARY 'Huevos con jamón');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-10', 'Comidas - grupo 65', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-10' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Chilaquiles sencillos' OR BINARY p.nombre = BINARY 'Sopes sencillos' OR BINARY p.nombre = BINARY 'Enchiladas sencillas' OR BINARY p.nombre = BINARY 'Enfrijoladas sencillas' OR BINARY p.nombre = BINARY 'Huevos en salsa verde');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-11', 'Comidas - grupo 105', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-11' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Enchiladas verdes con pollo' OR BINARY p.nombre = BINARY 'Enfrijoladas de pollo' OR BINARY p.nombre = BINARY 'Pechuga asada con ensalada' OR BINARY p.nombre = BINARY 'Pechuga encebollada con ensalada' OR BINARY p.nombre = BINARY 'Pechuga a la mexicana con ensalada' OR BINARY p.nombre = BINARY 'Bistec asado con ensalada' OR BINARY p.nombre = BINARY 'Bistec encebollado con ensalada' OR BINARY p.nombre = BINARY 'Bistec a la mexicana con ensalada' OR BINARY p.nombre = BINARY 'Puntas de res en salsa verde' OR BINARY p.nombre = BINARY 'Puntas de pollo en salsa verde' OR BINARY p.nombre = BINARY 'Pollo frito con ensalada' OR BINARY p.nombre = BINARY 'Caldo tlalpeño' OR BINARY p.nombre = BINARY 'Alambre vegetariano' OR BINARY p.nombre = BINARY 'Chuleta ahumada con papas' OR BINARY p.nombre = BINARY 'Sopecitos con quesillo');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-12', 'Comidas - grupo 95', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-12' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Tacos dorados de pollo' OR BINARY p.nombre = BINARY 'Omelet de queso en salsa verde' OR BINARY p.nombre = BINARY 'Omelet vegetariano' OR BINARY p.nombre = BINARY 'Sopecitos con chorizo' OR BINARY p.nombre = BINARY 'Tacos dorados de jamón con queso' OR BINARY p.nombre = BINARY 'Enfrijoladas de jamón' OR BINARY p.nombre = BINARY 'Enfrijoladas con jamón' OR BINARY p.nombre = BINARY 'Chilaquiles con huevo' OR BINARY p.nombre = BINARY 'Rollos de jamón con ensalada' OR BINARY p.nombre = BINARY 'Chuleta ahumada con ensalada' OR BINARY p.nombre = BINARY 'Sopecitos con huevo');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-13', 'Comidas - grupo 110', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-13' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Sopecitos con pechuga asada' OR BINARY p.nombre = BINARY 'Sopecitos con bistec asado' OR BINARY p.nombre = BINARY 'Milanesa de res con ensalada' OR BINARY p.nombre = BINARY 'Pechuga empanizada con ensalada' OR BINARY p.nombre = BINARY 'Ensalada de atún' OR BINARY p.nombre = BINARY 'Ensalada California' OR BINARY p.nombre = BINARY 'Nugguets con papas' OR BINARY p.nombre = BINARY 'Enchiladas de mole con pollo' OR BINARY p.nombre = BINARY 'Enchiladas con pechuga asada' OR BINARY p.nombre = BINARY 'Enchiladas con carne asada' OR BINARY p.nombre = BINARY 'Chilaquiles con pechuga asada' OR BINARY p.nombre = BINARY 'Chilaquiles con carne asada' OR BINARY p.nombre = BINARY 'Chilaquiles con chuleta ahumada' OR BINARY p.nombre = BINARY 'Chilaquiles con pollo deshebrado' OR BINARY p.nombre = BINARY 'Carne asada con papas' OR BINARY p.nombre = BINARY 'Tacos dorados de chorizo ahogados');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-14', 'Comidas - grupo 115', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-14' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Alambre de pollo' OR BINARY p.nombre = BINARY 'Alambre de res' OR BINARY p.nombre = BINARY 'Milanesa con papas' OR BINARY p.nombre = BINARY 'Pechuga empanizada con papas' OR BINARY p.nombre = BINARY 'Chilaquiles con milanesa' OR BINARY p.nombre = BINARY 'Chilaquiles con pechuga empanizada' OR BINARY p.nombre = BINARY 'Hamburguesa con papas' OR BINARY p.nombre = BINARY 'Enchiladas con pechuga empanizada' OR BINARY p.nombre = BINARY 'Enchiladas con milanesa de res' OR BINARY p.nombre = BINARY 'Enchiladas verdes gratinadas');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-15', 'Pollo c/ mole especial', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-15' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Pollo con mole especial');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-16', 'Enchiladas de mole gratinadas', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-16' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Enchiladas de mole gratinadas');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-17', 'Chilaquiles c/ arrachera', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-17' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Chilaquiles con arrachera');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-18', 'Enchiladas suizas', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-18' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Enchiladas suizas');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-19', 'Pechuga especial', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-19' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Pechuga especial');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-20', 'Pechuga a la cordon blue con papas', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-20' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Pechuga a la cordon blue con papas');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-21', 'Cecina c/ guacamole', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-21' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Cecina con guacamole');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-22', 'Bistec o pechuga a la tampiqueña', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-22' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Bistec a la Tampiqueña' OR BINARY p.nombre = BINARY 'Pechuga a la Tampiqueña');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-23', 'Sopecitos c/ arrachera', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-23' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Sopecitos con arrachera');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-24', 'Arrachera a la tampiqueña', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-24' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Arrachera a la Tampiqueña');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-25', 'Chilaquiles de frijol con pechuga empanizada o milanesa de res', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-25' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Chilaquiles de frijol con pechuga empanizada' OR BINARY p.nombre = BINARY 'Chilaquiles de frijol con milanesa de res');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-26', 'Chilaquiles de frijol c/ arrachera', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-26' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Chilaquiles de frijol con arrachera');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-27', 'Milanesa oreja de elefante', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-27' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Milanesa oreja de elefante');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-28', 'Enchiladas de mole con pechuga empanizada o milanesa de res', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-28' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Enchiladas de mole con pechuga empanizada' OR BINARY p.nombre = BINARY 'Enchiladas de mole con milanesa de res');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-29', 'Alambre de arrachera', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-29' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Alambre de arrachera');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-30', 'Sopecitos c/ cecina', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-30' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Sopecitos con cecina');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-32', 'Camarones empanizados', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-32' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Camarones empanizados');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-33', 'Filete empanizado c/ ensalada', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-33' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Filete empanizado con ensalada');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-34', 'Camarones a la diabla', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-34' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Camarones a la diabla');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-35', 'Filete empapelado', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-35' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Filete empapelado');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-36', 'Camarones al coco', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-36' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Camarones al coco');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-38', 'Coctel camaron chico', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-38' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Coctel camarón chico');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-39', 'Filete a la diabla', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-39' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Filete a la diabla');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-40', 'Coctel de Camaron grande', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-40' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Coctel de Camarón grande');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-41', 'Filete al mojo de ajo', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-41' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Filete al mojo de ajo');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-42', 'Alambre de Camaron', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-42' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Alambre de Camarón');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-43', 'Filete al ajillo', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-43' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Filete al ajillo');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-45', 'Mojarra a la diabla', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-45' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Mojarra a la diabla');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-46', 'Desechable para llevar', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-46' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Desechable para llevar');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-47', 'Mojarra al mojo de ajo', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-47' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Mojarra al mojo de ajo');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-48', 'Huevo estrellado', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-48' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Huevo estrellado');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-49', 'Mojarra al ajillo', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-49' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Mojarra al ajillo');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-50', 'Platano tabasco', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-50' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Plátano tabasco');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-51', 'Sopa o consome', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-51' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Sopa o consomé');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-52', 'Arroz o Espagueti', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-52' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Arroz o Espagueti');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-53', 'Crema del día', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-53' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Crema del día');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-54', 'Refesco', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-54' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Refresco');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-55', 'Orden de Guacamole', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-55' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Orden de Guacamole');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-56', 'Orden de papas', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-56' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Orden de papas a la francesa');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-57', 'Jarra de agua del día', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-57' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Jarra de agua del día');

INSERT INTO grupos_precio(clave,nombre,campo) VALUES ('comida-58', 'Jarra de agua de fruta natural 2lts', 'COMIDA');

INSERT INTO grupos_precio_productos(id_grupo_precio,id_producto)
SELECT g.id_grupo_precio,p.id_productos FROM grupos_precio g CROSS JOIN productos p
WHERE g.clave = 'comida-58' AND p.eliminado = 0 AND (BINARY p.nombre = BINARY 'Jarra de agua de fruta natural 2lts');

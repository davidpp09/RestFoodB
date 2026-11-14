-- La carta permite precios ausentes: retirar un importe también debe auditarse.
ALTER TABLE historial_precios MODIFY COLUMN precio_nuevo DECIMAL(10,2) NULL;

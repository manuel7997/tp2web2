-- Cada lista pertenece a un usuario. Las listas que ya existían no tienen dueño,
-- así que la columna nace nullable, se les asigna el primer usuario registrado
-- y recién después se exige NOT NULL.
ALTER TABLE listas ADD COLUMN usuario_id BIGINT;

UPDATE listas
SET usuario_id = (SELECT MIN(id) FROM usuarios)
WHERE usuario_id IS NULL;

ALTER TABLE listas ALTER COLUMN usuario_id SET NOT NULL;

ALTER TABLE listas
    ADD CONSTRAINT fk_listas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id);

CREATE INDEX idx_listas_usuario_id ON listas (usuario_id);
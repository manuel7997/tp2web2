-- 1) Crear la lista por defecto solamente si hay favoritos sin lista
-- y todavía no existe una lista llamada "Sin clasificar".
INSERT INTO listas (nombre)
SELECT 'Sin clasificar'
WHERE EXISTS (
    SELECT 1
    FROM favoritos
    WHERE lista_id IS NULL
)
AND NOT EXISTS (
    SELECT 1
    FROM listas
    WHERE nombre = 'Sin clasificar'
);

-- 2) Asignar los favoritos sin lista a "Sin clasificar"
UPDATE favoritos
SET lista_id = (
    SELECT MIN(id)
    FROM listas
    WHERE nombre = 'Sin clasificar'
)
WHERE lista_id IS NULL;

-- 3) Exigir que todos los favoritos tengan una lista
ALTER TABLE favoritos
ALTER COLUMN lista_id SET NOT NULL;

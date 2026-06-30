-- =============================================================================
-- PROCEDIMIENTO 1: pa_consultar_predio
-- Descripción:
-- Muestra toda la información detallada de un predio a partir de su código,
-- utilizando la vista vw_predio_detallado.
-- =============================================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS pa_consultar_predio$$

CREATE PROCEDURE pa_consultar_predio
(
    IN p_codigo VARCHAR(10)
)
BEGIN

    SELECT *
    FROM vw_predio_detallado
    WHERE codigo_predio = p_codigo;

END$$

DELIMITER ;
-- =============================================================================
-- PROCEDIMIENTO 2: pa_propietarios_por_escala
-- Descripción:
-- Muestra los propietarios clasificados en una determinada escala tributaria.
-- =============================================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS pa_propietarios_por_escala$$

CREATE PROCEDURE pa_propietarios_por_escala
(
    IN p_escala VARCHAR(4)
)
BEGIN

    SELECT
        *
    FROM vw_propietario_persona
    WHERE ProEscCod = p_escala;

END$$

DELIMITER ;
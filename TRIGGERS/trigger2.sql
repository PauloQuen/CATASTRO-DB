-- =============================================================================
-- TRIGGER 2: VALIDACIÓN DE FECHA DE INSCRIPCIÓN REGISTRAL
-- =============================================================================
-- Descripción:
-- Antes de registrar una nueva partida registral, verifica que la
-- fecha de inscripción (PrtFecIns) no sea mayor que la fecha actual.
--
-- Si la fecha ingresada pertenece al futuro, la operación se cancela
-- mostrando un mensaje de error.
-- =============================================================================

DELIMITER $$

DROP TRIGGER IF EXISTS trg_validar_partida$$

CREATE TRIGGER trg_validar_partida
BEFORE INSERT
ON c3m_partida_registral
FOR EACH ROW
BEGIN

    IF NEW.PrtFecIns > CURDATE() THEN

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT =
        'La fecha de inscripción de la partida registral no puede ser futura.';

    END IF;

END$$

DELIMITER ;
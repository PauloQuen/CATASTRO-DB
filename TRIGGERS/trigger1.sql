-- =============================================================================
-- TRIGGER 1: VALIDACIÓN DE PAGO DE TRIBUTO
-- =============================================================================
-- Descripción:
-- Antes de actualizar un registro de PAT_TRIBUTO_CAB, verifica que
-- si se registra un monto pagado (TriMonPag > 0), obligatoriamente
-- exista una fecha de pago (TriFecPag).
--
-- Si la fecha de pago es NULL, la operación se cancela y se muestra
-- un mensaje de error al usuario.
-- =============================================================================

DELIMITER $$

DROP TRIGGER IF EXISTS trg_validar_pago$$

CREATE TRIGGER trg_validar_pago
BEFORE UPDATE
ON pat_tributo_cab
FOR EACH ROW
BEGIN

    IF NEW.TriMonPag > 0
       AND NEW.TriFecPag IS NULL THEN

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT =
        'Debe registrar la fecha de pago del tributo.';

    END IF;

END$$

DELIMITER ;
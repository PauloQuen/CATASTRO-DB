-- =============================================================================
-- TRIGGER 3: VALIDACIÓN DE CLASIFICACIÓN DEL PROPIETARIO
-- =============================================================================
-- Descripción:
-- Antes de registrar un propietario, verifica la consistencia de la
-- clasificación tributaria.
--
-- Reglas aplicadas:
-- 1. Si existe un código de escala tributaria, debe existir también
--    el año de vigencia.
-- 2. Si existe el año de vigencia, debe existir el código de escala.
-- 3. La fecha de clasificación no puede ser una fecha futura.
--
-- Si cualquiera de estas reglas no se cumple, la operación es
-- cancelada mostrando un mensaje de error.
-- =============================================================================

DELIMITER $$

DROP TRIGGER IF EXISTS trg_validar_propietario$$

CREATE TRIGGER trg_validar_propietario
BEFORE INSERT
ON h8m_propietario
FOR EACH ROW
BEGIN

    IF NEW.ProEscCod IS NOT NULL
       AND NEW.ProEscVig IS NULL THEN

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT =
        'Debe indicar el año de vigencia de la escala tributaria.';

    END IF;

    IF NEW.ProEscVig IS NOT NULL
       AND NEW.ProEscCod IS NULL THEN

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT =
        'Debe indicar el código de la escala tributaria.';

    END IF;

    IF NEW.ProFecCla IS NOT NULL
       AND NEW.ProFecCla > CURDATE() THEN

        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT =
        'La fecha de clasificación no puede ser futura.';

    END IF;

END$$

DELIMITER ;
-- =============================================================================
-- TRIGGER 1: VALIDACIÓN DE PAGO DE TRIBUTO
-- =============================================================================
-- Descripción:
-- Antes de insertar o actualizar un registro de PAT_TRIBUTO_CAB, verifica que:
--   1. Si se registra un monto pagado (trimonpag > 0), debe existir
--      obligatoriamente la fecha de pago (trifecpag).
--   2. La fecha de pago no puede ser una fecha futura.
--   3. El monto pagado no puede exceder el monto calculado del tributo
--      (mejora respecto a la versión original: evita pagos inconsistentes).
-- =============================================================================

CREATE OR REPLACE FUNCTION trg_validar_pago_fn()
RETURNS TRIGGER AS $$
BEGIN

    IF NEW.trimonpag > 0 AND NEW.trifecpag IS NULL THEN
        RAISE EXCEPTION 'Debe registrar la fecha de pago del tributo.';
    END IF;

    IF NEW.trifecpag IS NOT NULL AND NEW.trifecpag > CURRENT_DATE THEN
        RAISE EXCEPTION 'La fecha de pago del tributo no puede ser futura.';
    END IF;

    IF NEW.trimoncal IS NOT NULL
       AND NEW.trimonpag IS NOT NULL
       AND NEW.trimonpag > NEW.trimoncal THEN
        RAISE EXCEPTION 'El monto pagado (%.2f) no puede ser mayor al monto calculado del tributo (%.2f).',
            NEW.trimonpag, NEW.trimoncal;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validar_pago ON public.pat_tributo_cab;

CREATE TRIGGER trg_validar_pago
    BEFORE INSERT OR UPDATE
    ON public.pat_tributo_cab
    FOR EACH ROW
    EXECUTE FUNCTION trg_validar_pago_fn();

-- =============================================================================
-- TRIGGER 2: VALIDACIÓN DE FECHA DE INSCRIPCIÓN REGISTRAL
-- =============================================================================
-- Descripción:
-- Antes de insertar o actualizar una partida registral, verifica que:
--   1. La fecha de inscripción (prtfecins) no sea una fecha futura.
--   2. La fecha de última actualización (prtfecultact), si existe, no sea
--      futura ni anterior a la fecha de inscripción (mejora agregada).
-- =============================================================================

CREATE OR REPLACE FUNCTION trg_validar_partida_fn()
RETURNS TRIGGER AS $$
BEGIN

    IF NEW.prtfecins > CURRENT_DATE THEN
        RAISE EXCEPTION 'La fecha de inscripción de la partida registral no puede ser futura.';
    END IF;

    IF NEW.prtfecultact IS NOT NULL THEN
        IF NEW.prtfecultact > CURRENT_DATE THEN
            RAISE EXCEPTION 'La fecha de última actualización de la partida registral no puede ser futura.';
        END IF;

        IF NEW.prtfecultact < NEW.prtfecins THEN
            RAISE EXCEPTION 'La fecha de última actualización no puede ser anterior a la fecha de inscripción.';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validar_partida ON public.c3m_partida_registral;

CREATE TRIGGER trg_validar_partida
    BEFORE INSERT OR UPDATE
    ON public.c3m_partida_registral
    FOR EACH ROW
    EXECUTE FUNCTION trg_validar_partida_fn();

-- =============================================================================
-- TRIGGER 3: VALIDACIÓN DE CLASIFICACIÓN DEL PROPIETARIO
-- =============================================================================
-- Descripción:
-- Antes de insertar o actualizar un propietario, verifica la consistencia
-- de su clasificación tributaria:
--   1. Si existe código de escala tributaria (proesccod), debe existir
--      también el año de vigencia (proescvig).
--   2. Si existe el año de vigencia, debe existir el código de escala.
--   3. La fecha de clasificación (profeccla) no puede ser futura.
-- =============================================================================

CREATE OR REPLACE FUNCTION trg_validar_propietario_fn()
RETURNS TRIGGER AS $$
BEGIN

    IF NEW.proesccod IS NOT NULL AND NEW.proescvig IS NULL THEN
        RAISE EXCEPTION 'Debe indicar el año de vigencia de la escala tributaria.';
    END IF;

    IF NEW.proescvig IS NOT NULL AND NEW.proesccod IS NULL THEN
        RAISE EXCEPTION 'Debe indicar el código de la escala tributaria.';
    END IF;

    IF NEW.profeccla IS NOT NULL AND NEW.profeccla > CURRENT_DATE THEN
        RAISE EXCEPTION 'La fecha de clasificación no puede ser futura.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validar_propietario ON public.h8m_propietario;

CREATE TRIGGER trg_validar_propietario
    BEFORE INSERT OR UPDATE
    ON public.h8m_propietario
    FOR EACH ROW
    EXECUTE FUNCTION trg_validar_propietario_fn();
-- =============================================================================
-- PROCEDIMIENTO: pa_consultar_predio
-- Descripción:
-- Muestra toda la información detallada de un predio a partir de su código,
-- utilizando la vista vw_predio_detallado.
-- =============================================================================

DROP PROCEDURE IF EXISTS pa_consultar_predio;

CREATE OR REPLACE PROCEDURE pa_consultar_predio
(
    IN p_codigo VARCHAR(10),
    INOUT p_resultado REFCURSOR
)
LANGUAGE plpgsql
AS $$
BEGIN
    OPEN p_resultado FOR
    SELECT *
    FROM vw_predio_detallado
    WHERE codigo_predio = p_codigo;
END;
$$;

-- =============================================================================
-- PROCEDIMIENTO: pa_propietarios_por_escala
-- Descripción:
-- Muestra los propietarios clasificados en una determinada escala tributaria.
-- =============================================================================

DROP PROCEDURE IF EXISTS pa_propietarios_por_escala;

CREATE OR REPLACE PROCEDURE pa_propietarios_por_escala
(
    IN p_escala VARCHAR(4),
    INOUT p_resultado REFCURSOR
)
LANGUAGE plpgsql
AS $$
BEGIN

    OPEN p_resultado FOR
    SELECT *
    FROM vw_propietario_persona
    WHERE ProEscCod = p_escala;

END;
$$;

-- =============================================================================
-- PROCEDIMIENTO: pa_reporte_tributos_anio
-- Descripción:
-- Muestra todos los tributos registrados durante un año determinado,
-- incluyendo el propietario y los montos calculados y pagados.
-- =============================================================================

DROP PROCEDURE IF EXISTS pa_reporte_tributos_anio;

CREATE OR REPLACE PROCEDURE pa_reporte_tributos_anio
(
    IN p_anio INT,
    INOUT p_resultado REFCURSOR
)
LANGUAGE plpgsql
AS $$
BEGIN

    OPEN p_resultado FOR
    SELECT
        t.TriCod,
        t.TriAnio,
        p.ProCod,
        pe.PerDNI,
        CONCAT(pe.PerNom, ' ', pe.PerApePat, ' ', pe.PerApeMat) AS propietario,
        t.TriMonCal,
        t.TriMonPag,
        t.TriFecVen,
        t.TriFecPag
    FROM pat_tributo_cab t
    INNER JOIN h8m_propietario p
        ON t.TriPro = p.ProCod
    INNER JOIN h6m_persona pe
        ON p.ProPer = pe.PerDNI
    WHERE t.TriAnio = p_anio
    ORDER BY propietario;

END;
$$;

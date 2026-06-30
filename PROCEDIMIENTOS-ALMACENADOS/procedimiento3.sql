-- =============================================================================
-- PROCEDIMIENTO 3: pa_reporte_tributos_anio
-- Descripción:
-- Muestra todos los tributos registrados durante un año determinado,
-- incluyendo el propietario y los montos calculados y pagados.
-- =============================================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS pa_reporte_tributos_anio$$

CREATE PROCEDURE pa_reporte_tributos_anio
(
    IN p_anio INT
)
BEGIN

    SELECT
        t.TriCod,
        t.TriAnio,
        p.ProCod,
        pe.PerDNI,
        CONCAT(pe.PerNom,' ',pe.PerApePat,' ',pe.PerApeMat)
            AS propietario,
        t.TriMonCal,
        t.TriMonPag,
        t.TriFecVen,
        t.TriFecPag

    FROM pat_tributo_cab t

    INNER JOIN h8m_propietario p
        ON t.TriPro=p.ProCod

    INNER JOIN h6m_persona pe
        ON p.ProPer=pe.PerDNI

    WHERE p_anio=t.TriAnio

    ORDER BY propietario;

END$$

DELIMITER ;
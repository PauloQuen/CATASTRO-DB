-- =============================================================================
-- VISTA 2: vw_propietario_persona
-- Combinación de 2 tablas relacionadas: Propietario + Persona
-- =============================================================================
CREATE OR REPLACE VIEW vw_propietario_persona AS
SELECT
    pr.ProCod,
    pe.PerDNI,
    CONCAT(pe.PerNom, ' ', pe.PerApePat, ' ', pe.PerApeMat) AS nombre_completo,
    pe.PerIng                                                AS ingreso_mensual,
    pr.ProViv                                                AS predio_principal,
    pr.ProEscCod,
    pr.ProEscVig,
    pr.ProEstReg
FROM h8m_propietario pr
INNER JOIN h6m_persona pe ON pr.ProPer = pe.PerDNI;

-- Prueba:
SELECT * FROM vw_propietario_persona;
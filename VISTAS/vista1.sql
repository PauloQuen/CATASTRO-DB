-- =============================================================================
-- VISTA 1: vw_vivienda_completa
-- Tabla Maestra (c3m_vivienda) con sus descripciones resueltas
-- Muestra el predio junto al NOMBRE del tipo y uso de predio, no solo el código
-- =============================================================================
CREATE OR REPLACE VIEW vw_vivienda_completa AS
SELECT
    v.VivCod,
    v.VivVal,
    v.VivUbigeo,
    tp.TipPrNom   AS tipo_predio,
    tp.TipPrDes   AS tipo_predio_desc,
    up.UsoPrNom   AS uso_predio,
    up.UsoPrDes   AS uso_predio_desc,
    v.VivEstReg
FROM c3m_vivienda v
LEFT JOIN c5m_tipo_predio tp ON v.VivTipPr = tp.TipPrCod
LEFT JOIN c5m_uso_predio  up ON v.VivUsoPr = up.UsoPrCod;

-- Prueba:
SELECT * FROM vw_vivienda_completa;
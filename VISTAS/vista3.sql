-- =============================================================================
-- VISTA 3: vw_predio_detallado
-- Tabla fundamental (Vivienda) + TODAS sus tablas referenciadas:
-- Dirección, Tipo de Vía, Zona, Municipalidad, Distrito, Provincia, Región,
-- Tipo de Predio y Uso de Predio.
-- =============================================================================
CREATE OR REPLACE VIEW vw_predio_detallado AS
SELECT
    v.VivCod                                            AS codigo_predio,
    v.VivVal                                            AS valor_predio,
    v.VivUbigeo                                         AS ubigeo,
    tp.TipPrNom                                         AS tipo_predio,
    up.UsoPrNom                                         AS uso_predio,
    CONCAT(vt.VtNom, ' ', d.DirViaNom,
           ' ', IFNULL(d.DirNum,'S/N'))                  AS direccion,
    d.DirUrb                                            AS urbanizacion,
    z.ZonNom                                            AS zona,
    z.ZonTip                                            AS tipo_zona,
    m.MunNom                                            AS municipalidad,
    m.MunAlcNom                                         AS alcalde,
    di.DisNom                                           AS distrito,
    pv.ProNom                                           AS provincia,
    r.RegNom                                            AS region,
    v.VivEstReg                                         AS estado_predio
FROM c3m_vivienda v
LEFT  JOIN c3m_direccion      d  ON v.VivDir    = d.DirCod
LEFT  JOIN c3m_via_tipo       vt ON d.DirViaTip = vt.VtCod
LEFT  JOIN c5m_tipo_predio    tp ON v.VivTipPr  = tp.TipPrCod
LEFT  JOIN c5m_uso_predio     up ON v.VivUsoPr  = up.UsoPrCod
INNER JOIN c2m_zona           z  ON v.VivZon    = z.ZonCod
INNER JOIN c1m_municipalidad  m  ON z.ZonMun    = m.MunCod
INNER JOIN c1m_distrito       di ON m.MunDis    = di.DisCod
INNER JOIN c1m_provincia      pv ON di.DisPro   = pv.ProCod
INNER JOIN c1m_region         r  ON pv.ProReg   = r.RegCod;

-- Prueba:
SELECT * FROM vw_predio_detallado;
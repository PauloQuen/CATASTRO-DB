-- =====================================================================
-- PROCEDIMIENTOS ALMACENADOS - SISTEMA CATASTRAL (PARTE 2)
-- Requiere haber ejecutado antes: catastroDB-postgreSQL-corregido.sql
--                                 triggers_catastro.sql
--                                 vistas_catastro.sql
--                                 procedimientos_catastro.sql (parte 1)
-- =====================================================================


-- =====================================================================
-- 4) sp_generar_reporte_recaudacion
-- =====================================================================
-- TABLAS INVOLUCRADAS:
--   pcm_reportes_pre (definicion/metadata del reporte)
--   -> pct_reportes_cab (cabecera de esta corrida especifica)
--   -> pct_reportes_det (una linea por cada propietario tributante)
--   apoyado en: pat_tributo_cab, h8m_prop_vivienda, c3m_vivienda,
--               c2m_zona, pbm_presupuesto_anual
--
-- PARA QUE SIRVE:
-- Es el caso que motivo esta tanda de procedimientos: "generame el
-- reporte de recaudacion de tal municipalidad para tal año (opcional:
-- de tal zona)". Congela en el tiempo cuanto se tributo y cuanto se
-- recaudo, linea por linea por propietario, para auditoria y consulta
-- posterior (aunque despues cambien los pagos, el reporte ya generado
-- no se mueve).
--
-- COMO FUNCIONA (mismo principio cab/det que ya usamos en el Trigger 3b):
--   1) Registra la "definicion" del reporte en pcm_reportes_pre
--   2) Ubica el presupuesto anual de esa municipalidad/año (si existe)
--      y lo vincula en la cabecera
--   3) Crea la cabecera del reporte (pct_reportes_cab) con totales en 0
--   4) Genera una fila de detalle POR CADA tributo (pat_tributo_cab) de
--      un propietario que tenga al menos un predio vigente en esa
--      municipalidad (y zona, si se especifico)
--   5) Al terminar, la cabecera se actualiza con la SUMA REAL de su
--      propio detalle -- nunca un numero calculado aparte -- para
--      garantizar que cabecera y detalle siempre cuadren exacto
-- =====================================================================

CREATE OR REPLACE PROCEDURE sp_generar_reporte_recaudacion(
    IN  p_mun         INTEGER,
    IN  p_anio        SMALLINT,
    IN  p_zon         INTEGER,
    IN  p_titulo      VARCHAR(120),
    OUT p_repcabcod   INTEGER
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_repcod     INTEGER;
    v_precod     INTEGER;
    v_total_tri  NUMERIC(14,2) := 0;
    v_total_rec  NUMERIC(14,2) := 0;
    v_cant_det   INTEGER := 0;
    v_trib       RECORD;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM public.c1m_municipalidad WHERE muncod = p_mun AND munestreg = '1'
    ) THEN
        RAISE EXCEPTION 'La municipalidad % no existe o no esta activa', p_mun;
    END IF;

    IF p_zon IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM public.c2m_zona WHERE zoncod = p_zon AND zonmun = p_mun
    ) THEN
        RAISE EXCEPTION 'La zona % no pertenece a la municipalidad %', p_zon, p_mun;
    END IF;

    -- 1) Definicion/metadata del reporte
    INSERT INTO public.pcm_reportes_pre
        (repmun, repzon, reptip, reptitulo, repper, repfecgen, repestreg)
    VALUES
        (p_mun, p_zon, 'REC', COALESCE(p_titulo, 'Reporte de Recaudacion ' || p_anio),
         p_anio, CURRENT_DATE, '1')
    RETURNING repcod INTO v_repcod;

    -- 2) Presupuesto anual asociado, si existe
    SELECT precod INTO v_precod
    FROM public.pbm_presupuesto_anual
    WHERE premun = p_mun AND preanio = p_anio AND preestreg = '1'
    LIMIT 1;

    -- 3) Cabecera del reporte (totales provisionales en 0)
    INSERT INTO public.pct_reportes_cab
        (repcabrep, repcabpre, repcabfec, repcabtottri, repcabtotrec, repcabobs, repcabestreg)
    VALUES
        (v_repcod, v_precod, CURRENT_DATE, 0, 0,
         'Generado automaticamente por sp_generar_reporte_recaudacion', '1')
    RETURNING repcabcod INTO p_repcabcod;

    -- 4) Detalle: un tributo por propietario con predio vigente en la
    --    municipalidad (y zona, si se filtro)
    FOR v_trib IN
        SELECT cab.tricod, cab.tripro, cab.trimoncal, cab.trimonpag,
               (SELECT pv.provivviv
                FROM public.h8m_prop_vivienda pv
                JOIN public.c3m_vivienda viv ON viv.vivcod = pv.provivviv
                JOIN public.c2m_zona zon ON zon.zoncod = viv.vivzon
                WHERE pv.provivprp = cab.tripro
                  AND pv.provivfecfin IS NULL AND pv.provivestreg = '1'
                  AND zon.zonmun = p_mun
                  AND (p_zon IS NULL OR zon.zoncod = p_zon)
                ORDER BY pv.provivviv
                LIMIT 1) AS vivcod_ref
        FROM public.pat_tributo_cab cab
        WHERE cab.trianio = p_anio
          AND cab.triestreg = '1'
          AND EXISTS (
              SELECT 1
              FROM public.h8m_prop_vivienda pv
              JOIN public.c3m_vivienda viv ON viv.vivcod = pv.provivviv
              JOIN public.c2m_zona zon ON zon.zoncod = viv.vivzon
              WHERE pv.provivprp = cab.tripro
                AND pv.provivfecfin IS NULL AND pv.provivestreg = '1'
                AND zon.zonmun = p_mun
                AND (p_zon IS NULL OR zon.zoncod = p_zon)
          )
    LOOP
        INSERT INTO public.pct_reportes_det
            (repdetcab, repdetprop, repdetviv, repdettri, repdetmoncal, repdetmonpag, repdetestreg)
        VALUES
            (p_repcabcod, v_trib.tripro, v_trib.vivcod_ref, v_trib.tricod,
             v_trib.trimoncal, v_trib.trimonpag, '1');

        v_total_tri := v_total_tri + v_trib.trimoncal;
        v_total_rec := v_total_rec + v_trib.trimonpag;
        v_cant_det  := v_cant_det + 1;
    END LOOP;

    -- 5) La cabecera refleja la SUMA REAL de su propio detalle
    UPDATE public.pct_reportes_cab
    SET repcabtottri = v_total_tri,
        repcabtotrec = v_total_rec
    WHERE repcabcod = p_repcabcod;

    IF v_cant_det = 0 THEN
        RAISE NOTICE 'Reporte % generado sin tributos para el periodo (sin recaudacion registrada)', p_repcabcod;
    ELSE
        RAISE NOTICE 'Reporte % generado: % tributos, total tributado %, total recaudado %',
            p_repcabcod, v_cant_det, v_total_tri, v_total_rec;
    END IF;
END;
$$;


-- =====================================================================
-- 5) sp_archivar_reporte
-- =====================================================================
-- TABLAS INVOLUCRADAS: pct_reportes_cab/det  ->  pch_reportes_cab/det
--
-- PARA QUE SIRVE:
-- Cierra un periodo: mueve un reporte "activo" (pct_*) hacia su version
-- historica/archivada (pch_*, Clase H = Historico). Esto es tipico en
-- sistemas municipales: el reporte de un mes cerrado no deberia poder
-- editarse mas, pero tampoco se borra -- queda archivado con su propia
-- fecha de archivo, y el original queda enlazado via hrepcabori/hrepdetori
-- para trazabilidad.
--
-- COMO FUNCIONA:
--   1) Verifica que el reporte este activo (no archivado ya)
--   2) Crea la cabecera historica, copiando los totales ya consolidados
--      del reporte original (no los recalcula: lo que se archiva es
--      exactamente lo que ese reporte decia en su momento)
--   3) Copia cada linea de detalle a su version historica
--   4) Marca el reporte original como archivado (repcabestreg = '0'),
--      sin eliminarlo (ademas la FK hrepcabori -> pct_reportes_cab tiene
--      ON DELETE RESTRICT, no se podria borrar aunque se quisiera)
-- =====================================================================

CREATE OR REPLACE PROCEDURE sp_archivar_reporte(
    IN  p_repcabcod    INTEGER,
    OUT p_hrepcabcod   INTEGER
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_cab           RECORD;
    v_det           RECORD;
    v_cant_lineas   INTEGER := 0;
BEGIN
    SELECT * INTO v_cab
    FROM public.pct_reportes_cab
    WHERE repcabcod = p_repcabcod AND repcabestreg = '1';

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El reporte % no existe o ya fue archivado', p_repcabcod;
    END IF;

    INSERT INTO public.pch_reportes_cab
        (hrepcabori, hrepcabfec, hrepcabfecarch, hrepcabtottri, hrepcabtotrec, hrepcabobs, hrepcabestreg)
    VALUES
        (v_cab.repcabcod, v_cab.repcabfec, CURRENT_DATE, v_cab.repcabtottri, v_cab.repcabtotrec,
         v_cab.repcabobs, '1')
    RETURNING hrepcabcod INTO p_hrepcabcod;

    FOR v_det IN
        SELECT * FROM public.pct_reportes_det
        WHERE repdetcab = p_repcabcod AND repdetestreg = '1'
    LOOP
        INSERT INTO public.pch_reportes_det
            (hrepdetcab, hrepdetori, hrepdetmoncal, hrepdetmonpag, hrepdetestreg)
        VALUES
            (p_hrepcabcod, v_det.repdetcod, v_det.repdetmoncal, v_det.repdetmonpag, '1');

        v_cant_lineas := v_cant_lineas + 1;
    END LOOP;

    UPDATE public.pct_reportes_cab
    SET repcabestreg = '0'
    WHERE repcabcod = p_repcabcod;

    RAISE NOTICE 'Reporte % archivado como historico % (% lineas de detalle copiadas)',
        p_repcabcod, p_hrepcabcod, v_cant_lineas;
END;
$$;


-- =====================================================================
-- 6) sp_cerrar_presupuesto_anual
-- =====================================================================
-- TABLAS INVOLUCRADAS:
--   pbm_presupuesto_anual  <-  pbm_tributo_presupuesto
--                          <-  pat_tributo_cab (via h8m_prop_vivienda/vivienda/zona)
--
-- PARA QUE SIRVE:
-- Cierra el año fiscal de una municipalidad. En la realidad, cerrar un
-- presupuesto implica: (a) verificar/consolidar por ultima vez cuanto
-- se recaudo realmente, (b) avisar si queda deuda tributaria pendiente
-- de ese año (para que Rentas la gestione antes o durante el cobro
-- coactivo del año siguiente), y (c) bloquear el presupuesto para que
-- ya no reciba mas aportes (el Trigger 4 de pagos solo imputa a
-- presupuestos con preestreg = '1').
--
-- COMO FUNCIONA:
--   1) Verifica que el presupuesto exista y siga abierto
--   2) Recalcula el monto recaudado desde la fuente real (suma de
--      pbm_tributo_presupuesto), corrigiendo cualquier diferencia
--      antes de cerrar (auditoria final)
--   3) Calcula cuanta deuda tributaria del año quedo sin cobrar, y lo
--      informa como advertencia (no bloquea el cierre, es informativo)
--   4) Marca el presupuesto como cerrado (preestreg = '0')
-- =====================================================================

CREATE OR REPLACE PROCEDURE sp_cerrar_presupuesto_anual(
    IN p_precod INTEGER
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_pre         RECORD;
    v_total_real  NUMERIC(14,2);
    v_deuda_pend  NUMERIC(14,2);
BEGIN
    SELECT * INTO v_pre
    FROM public.pbm_presupuesto_anual
    WHERE precod = p_precod AND preestreg = '1';

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El presupuesto % no existe o ya esta cerrado', p_precod;
    END IF;

    SELECT COALESCE(SUM(tripremonapor), 0) INTO v_total_real
    FROM public.pbm_tributo_presupuesto
    WHERE triprepre = p_precod AND tripreestreg = '1';

    IF v_total_real <> v_pre.premonpag THEN
        RAISE NOTICE 'Se corrige el monto recaudado del presupuesto % de % a % (segun aportes verificados)',
            p_precod, v_pre.premonpag, v_total_real;
    END IF;

    SELECT COALESCE(SUM(cab.trimoncal - cab.trimonpag), 0) INTO v_deuda_pend
    FROM public.pat_tributo_cab cab
    JOIN public.h8m_propietario pro   ON pro.procod = cab.tripro
    JOIN public.h8m_prop_vivienda pv  ON pv.provivprp = pro.procod
                                      AND pv.provivfecfin IS NULL AND pv.provivestreg = '1'
    JOIN public.c3m_vivienda viv      ON viv.vivcod = pv.provivviv
    JOIN public.c2m_zona zon          ON zon.zoncod = viv.vivzon
    WHERE zon.zonmun = v_pre.premun
      AND cab.trianio = v_pre.preanio
      AND cab.triestreg = '1'
      AND cab.trimonpag < cab.trimoncal;

    UPDATE public.pbm_presupuesto_anual
    SET premonpag = v_total_real,
        preestreg = '0'
    WHERE precod = p_precod;

    IF v_deuda_pend > 0 THEN
        RAISE WARNING 'Presupuesto % cerrado con deuda tributaria pendiente de S/ % para el año %',
            p_precod, v_deuda_pend, v_pre.preanio;
    END IF;

    RAISE NOTICE 'Presupuesto % (municipalidad %, año %) cerrado. Estimado: %, Recaudado final: %, Gastado: %, Saldo: %',
        p_precod, v_pre.premun, v_pre.preanio, v_pre.premonest, v_total_real, v_pre.premongas,
        (v_pre.premonest - v_pre.premongas);
END;
$$;

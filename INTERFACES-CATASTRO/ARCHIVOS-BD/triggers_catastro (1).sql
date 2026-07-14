-- =====================================================================
-- TRIGGERS DE LOGICA DE NEGOCIO - SISTEMA CATASTRAL MUNICIPAL
-- Requiere haber ejecutado antes: catastroDB-postgreSQL-corregido.sql
-- =====================================================================


-- =====================================================================
-- TRIGGER 1: Sincronizar el valor catastral vigente de la vivienda
-- =====================================================================
-- TABLAS INVOLUCRADAS: c5m_valor_catastral  ->  c3m_vivienda
--
-- CASO REAL:
-- Catastro registra una valorizacion (autoavaluo) de cada predio TODOS
-- LOS AÑOS y guarda el historico completo en c5m_valor_catastral (una
-- fila por vivienda por año). Pero el campo c3m_vivienda.vivval es el
-- "valor oficial vigente" que usa el resto del sistema (por ejemplo,
-- para calcular el impuesto predial). Si nadie lo actualiza a mano,
-- ese valor queda desfasado apenas se registra una nueva valorizacion.
--
-- COMO FUNCIONA:
-- Cada vez que se inserta o corrige una valorizacion, el trigger busca
-- cual es el año MAS RECIENTE y activo para esa vivienda y copia ese
-- monto a c3m_vivienda.vivval. Así el valor "oficial" siempre refleja
-- la ultima valorizacion, sin importar si la insertaron en orden o si
-- corrigieron un registro antiguo.
-- =====================================================================

CREATE OR REPLACE FUNCTION fn_sync_valor_vivienda()
RETURNS TRIGGER AS $$
DECLARE
    v_valor_vigente NUMERIC(10,2);
BEGIN
    SELECT valmon INTO v_valor_vigente
    FROM public.c5m_valor_catastral
    WHERE valviv = NEW.valviv
      AND valestreg = '1'
    ORDER BY valanio DESC
    LIMIT 1;

    IF v_valor_vigente IS NOT NULL THEN
        UPDATE public.c3m_vivienda
        SET vivval = v_valor_vigente
        WHERE vivcod = NEW.valviv
          AND vivval IS DISTINCT FROM v_valor_vigente;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_valor_catastral_sync_vivienda ON public.c5m_valor_catastral;
CREATE TRIGGER trg_valor_catastral_sync_vivienda
AFTER INSERT OR UPDATE OF valmon, valanio, valestreg ON public.c5m_valor_catastral
FOR EACH ROW
EXECUTE FUNCTION fn_sync_valor_vivienda();


-- =====================================================================
-- TRIGGER 2: Cerrar automaticamente la titularidad anterior de un predio
-- =====================================================================
-- TABLAS INVOLUCRADAS: h8m_prop_vivienda (contra si misma)
--
-- CASO REAL:
-- Cuando un predio se vende o se hereda, el area de Catastro registra
-- al NUEVO propietario insertando una fila en h8m_prop_vivienda con su
-- fecha de inicio de titularidad. El problema: si nadie cierra el
-- registro del propietario ANTERIOR (poniendole fecha de fin), el
-- sistema terminaria mostrando dos "propietarios vigentes" al mismo
-- tiempo para la misma vivienda, lo cual no puede pasar en la realidad
-- (una propiedad tiene un solo titular activo en un momento dado).
--
-- COMO FUNCIONA:
-- Al insertar una nueva titularidad para una vivienda, el trigger busca
-- si esa misma vivienda tiene otro propietario con titularidad abierta
-- (provivfecfin IS NULL) y le coloca como fecha de cierre el dia
-- anterior al inicio del nuevo propietario. Esto mantiene integro el
-- historial de la cadena de propietarios del predio (util para
-- partidas registrales, herencias y compraventas).
-- =====================================================================

CREATE OR REPLACE FUNCTION fn_cerrar_titularidad_anterior()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE public.h8m_prop_vivienda
    SET provivfecfin = NEW.provivfecini - 1
    WHERE provivviv = NEW.provivviv
      AND provivprp <> NEW.provivprp
      AND provivfecfin IS NULL
      AND provivestreg = '1';

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_cerrar_titularidad_anterior ON public.h8m_prop_vivienda;
CREATE TRIGGER trg_cerrar_titularidad_anterior
BEFORE INSERT ON public.h8m_prop_vivienda
FOR EACH ROW
EXECUTE FUNCTION fn_cerrar_titularidad_anterior();


-- =====================================================================
-- TRIGGER 3: Calcular el tributo predial segun la escala tributaria
-- =====================================================================
-- TABLAS INVOLUCRADAS:
--   pat_tributo_cab  ->  h8m_propietario  ->  h6m_persona  ->  p9m_escala_tributo
--
-- CASO REAL:
-- El impuesto predial anual no deberia depender de que alguien escriba
-- "a mano" el monto a pagar: debe calcularse segun una ESCALA
-- TRIBUTARIA progresiva basada en el ingreso declarado del propietario
-- (igual que la tabla p9m_escala_tributo ya define: rangos de ingreso
-- con un monto fijo + un porcentaje adicional). Asi, dos propietarios
-- con predios similares pero distinto nivel de ingreso pagan montos
-- distintos, reflejando la realidad de escalas tributarias
-- progresivas/diferenciadas que usan las municipalidades.
--
-- COMO FUNCIONA:
-- Al insertar un nuevo tributo anual (pat_tributo_cab), el trigger:
--   1) Ubica al propietario (tripro) y su ingreso (via h6m_persona.pering)
--   2) Busca la escala tributaria vigente cuyo rango [escingmin,escingmax]
--      contiene ese ingreso
--   3) Calcula trimoncal = monto fijo de la escala + % de tributacion
--      sobre el ingreso
--   4) Deja registrada en h8m_propietario que escala/vigencia se uso,
--      para trazabilidad y auditoria posterior
-- Si no hay ingreso o no existe una escala que cubra ese rango, el
-- trigger detiene la operacion con un error claro (evita tributos sin
-- sustento tributario).
-- =====================================================================

CREATE OR REPLACE FUNCTION fn_calcular_tributo_por_escala()
RETURNS TRIGGER AS $$
DECLARE
    v_ingreso NUMERIC(10,2);
    v_escala  RECORD;
BEGIN
    SELECT per.pering INTO v_ingreso
    FROM public.h8m_propietario pro
    JOIN public.h6m_persona per ON per.perdni = pro.proper
    WHERE pro.procod = NEW.tripro;

    IF v_ingreso IS NULL THEN
        RAISE EXCEPTION 'No se pudo determinar el ingreso del propietario % para calcular el tributo', NEW.tripro;
    END IF;

    SELECT * INTO v_escala
    FROM public.p9m_escala_tributo
    WHERE v_ingreso BETWEEN escingmin AND escingmax
      AND escestreg = '1'
    ORDER BY escvig DESC
    LIMIT 1;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe una escala tributaria vigente para el ingreso del propietario % (S/ %)', NEW.tripro, v_ingreso;
    END IF;

    NEW.trimoncal := COALESCE(v_escala.escmonfij, 0)
                    + ROUND(v_ingreso * COALESCE(v_escala.escportrib, 0) / 100.0, 2);

    UPDATE public.h8m_propietario
    SET proesccod  = v_escala.esccod,
        proescvig  = v_escala.escvig,
        profeccla  = CURRENT_DATE
    WHERE procod = NEW.tripro;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_calcular_tributo_por_escala ON public.pat_tributo_cab;
CREATE TRIGGER trg_calcular_tributo_por_escala
BEFORE INSERT ON public.pat_tributo_cab
FOR EACH ROW
EXECUTE FUNCTION fn_calcular_tributo_por_escala();


-- =====================================================================
-- TRIGGER 3b: Repartir el tributo calculado entre las viviendas del
--             propietario (relacion CABECERA -> DETALLE)
-- =====================================================================
-- TABLAS INVOLUCRADAS:
--   pat_tributo_cab  ->  h8m_prop_vivienda  ->  c3m_vivienda  ->  pat_tributo_det
--
-- CASO REAL:
-- pat_tributo_cab es la CABECERA: un tributo anual por propietario. Pero
-- un propietario puede tener varios predios (varias filas vigentes en
-- h8m_prop_vivienda), y el DETALLE (pat_tributo_det) debe explicar cuanto
-- le corresponde pagar a CADA predio dentro de ese total. Sin este
-- trigger, el Trigger 3 calculaba el monto total del propietario pero
-- nunca llenaba el detalle, dejando pat_tributo_det vacio o
-- desincronizado del total de la cabecera.
--
-- COMO FUNCIONA:
-- Despues de crearse la cabecera (con su trimoncal ya calculado por el
-- Trigger 3), este trigger:
--   1) Busca todas las viviendas VIGENTES del propietario (las que en
--      h8m_prop_vivienda tienen provivfecfin IS NULL, es decir, las que
--      todavia le pertenecen)
--   2) Reparte el monto total de forma proporcional al valor catastral
--      de cada predio (predios mas valiosos pagan una porcion mayor)
--   3) A la ULTIMA vivienda del reparto le asigna el remanente exacto
--      (total menos lo ya repartido), para que la suma del detalle
--      siempre cuadre exactamente con el total de la cabecera, sin
--      diferencias de centavos por redondeo
--   4) Si ninguna vivienda tiene valor catastral registrado, reparte en
--      partes iguales en vez de fallar
-- Si el propietario no tiene ninguna vivienda vigente, el trigger detiene
-- la operacion (no se puede cobrar un predial sin predio).
-- =====================================================================

CREATE OR REPLACE FUNCTION fn_generar_detalle_tributo()
RETURNS TRIGGER AS $$
DECLARE
    v_total_valor     NUMERIC(14,2);
    v_cant_viviendas  INTEGER;
    v_monto_acumulado NUMERIC(14,2) := 0;
    v_monto_viv       NUMERIC(14,2);
    v_contador        INTEGER := 0;
    v_viv             RECORD;
BEGIN
    -- Si se recalcula el tributo, se limpia el detalle anterior
    DELETE FROM public.pat_tributo_det WHERE tricod = NEW.tricod;

    SELECT COUNT(*), SUM(COALESCE(viv.vivval, 0))
    INTO v_cant_viviendas, v_total_valor
    FROM public.h8m_prop_vivienda pv
    JOIN public.c3m_vivienda viv ON viv.vivcod = pv.provivviv
    WHERE pv.provivprp = NEW.tripro
      AND pv.provivfecfin IS NULL
      AND pv.provivestreg = '1';

    IF v_cant_viviendas IS NULL OR v_cant_viviendas = 0 THEN
        RAISE EXCEPTION 'El propietario % no tiene viviendas vigentes; no se puede generar el detalle del tributo', NEW.tripro;
    END IF;

    FOR v_viv IN
        SELECT pv.provivviv AS vivcod, COALESCE(viv.vivval, 0) AS vivval
        FROM public.h8m_prop_vivienda pv
        JOIN public.c3m_vivienda viv ON viv.vivcod = pv.provivviv
        WHERE pv.provivprp = NEW.tripro
          AND pv.provivfecfin IS NULL
          AND pv.provivestreg = '1'
        ORDER BY pv.provivviv
    LOOP
        v_contador := v_contador + 1;

        IF v_contador = v_cant_viviendas THEN
            -- Ultima vivienda: se lleva el remanente exacto (ajusta el redondeo)
            v_monto_viv := NEW.trimoncal - v_monto_acumulado;
        ELSIF v_total_valor > 0 THEN
            v_monto_viv := ROUND(NEW.trimoncal * (v_viv.vivval / v_total_valor), 2);
        ELSE
            v_monto_viv := ROUND(NEW.trimoncal / v_cant_viviendas, 2);
        END IF;

        INSERT INTO public.pat_tributo_det (tricod, tridetviv, tridetmon, tridetestreg)
        VALUES (NEW.tricod, v_viv.vivcod, v_monto_viv, '1');

        v_monto_acumulado := v_monto_acumulado + v_monto_viv;
    END LOOP;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_generar_detalle_tributo ON public.pat_tributo_cab;
CREATE TRIGGER trg_generar_detalle_tributo
AFTER INSERT ON public.pat_tributo_cab
FOR EACH ROW
EXECUTE FUNCTION fn_generar_detalle_tributo();

-- Regla de integridad: un mismo tributo (cabecera) no puede tener dos
-- filas de detalle para la misma vivienda.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_tridet_cab_viv'
    ) THEN
        ALTER TABLE public.pat_tributo_det
            ADD CONSTRAINT uq_tridet_cab_viv UNIQUE (tricod, tridetviv);
    END IF;
END $$;


-- =====================================================================
-- TRIGGER 4: Cascada de pago de tributo hacia el presupuesto municipal
-- =====================================================================
-- TABLAS INVOLUCRADAS:
--   pat_tributo_cab  ->  pam_historial_tributario
--                    ->  h8m_propietario -> c3m_vivienda -> c2m_zona -> pbm_presupuesto_anual
--                    ->  pbm_tributo_presupuesto
--                    ->  pbm_presupuesto_anual
--
-- CASO REAL:
-- Cuando un vecino paga su impuesto predial (se actualiza trimonpag en
-- pat_tributo_cab), ese pago no es un hecho aislado: en la realidad
-- dispara TRES cosas en la tesoreria municipal:
--   1) Queda constancia del pago en el historial tributario del
--      propietario (para consultas, reclamos, certificados de no
--      adeudo, etc.)
--   2) Ese pago "aporta" al presupuesto anual de la municipalidad a la
--      que pertenece el predio (tabla puente tributo-presupuesto)
--   3) El monto recaudado real del presupuesto municipal de ese año
--      sube en la misma cantidad, para poder comparar en todo momento
--      "lo estimado" (premonest) vs "lo realmente recaudado" (premonpag)
--
-- COMO FUNCIONA:
-- El trigger se dispara solo cuando trimonpag efectivamente AUMENTA
-- (un pago nuevo, parcial o total). Calcula cuanto aumento, inserta el
-- historial, ubica el presupuesto anual de la municipalidad del predio
-- para el año del tributo, y si existe, registra/incrementa el aporte
-- en pbm_tributo_presupuesto y actualiza pbm_presupuesto_anual.premonpag.
-- =====================================================================

CREATE OR REPLACE FUNCTION fn_registrar_pago_tributo()
RETURNS TRIGGER AS $$
DECLARE
    v_presupuesto RECORD;
    v_monto_nuevo NUMERIC(14,2);
BEGIN
    IF NEW.trimonpag IS NOT DISTINCT FROM OLD.trimonpag THEN
        RETURN NEW;
    END IF;

    v_monto_nuevo := NEW.trimonpag - COALESCE(OLD.trimonpag, 0);
    IF v_monto_nuevo <= 0 THEN
        RETURN NEW;
    END IF;

    -- 1) Historial tributario del propietario
    INSERT INTO public.pam_historial_tributario
        (histri, hisanio, hismoncal, hismonpag, hisfecpag, hisfecarch, hisobs, hisestreg)
    VALUES
        (NEW.tricod, NEW.trianio, NEW.trimoncal, v_monto_nuevo,
         COALESCE(NEW.trifecpag, CURRENT_DATE), CURRENT_DATE,
         'Pago registrado automaticamente por trigger', '1');

    -- 2) Presupuesto anual de la municipalidad donde esta el predio que
    --    el propietario tiene ACTUALMENTE vigente segun la cadena de
    --    titularidad (h8m_prop_vivienda), no un campo de residencia aparte.
    SELECT pre.* INTO v_presupuesto
    FROM public.pbm_presupuesto_anual pre
    JOIN public.h8m_prop_vivienda pv ON pv.provivprp = NEW.tripro
                                     AND pv.provivfecfin IS NULL
                                     AND pv.provivestreg = '1'
    JOIN public.c3m_vivienda viv ON viv.vivcod = pv.provivviv
    JOIN public.c2m_zona zon ON zon.zoncod = viv.vivzon
    WHERE pre.premun = zon.zonmun
      AND pre.preanio = NEW.trianio
      AND pre.preestreg = '1'
    LIMIT 1;

    IF NOT FOUND THEN
        -- No se detiene el pago del vecino por falta de presupuesto registrado;
        -- simplemente no hay donde imputar el aporte todavia.
        RETURN NEW;
    END IF;

    -- 3) Aporte de este pago al presupuesto (tabla puente)
    INSERT INTO public.pbm_tributo_presupuesto
        (triprepre, tripretri, tripremonapor, triprefecreg, tripreestreg)
    VALUES
        (v_presupuesto.precod, NEW.tricod, v_monto_nuevo, CURRENT_DATE, '1')
    ON CONFLICT (triprepre, tripretri)
    DO UPDATE SET tripremonapor = public.pbm_tributo_presupuesto.tripremonapor + EXCLUDED.tripremonapor,
                  triprefecreg  = EXCLUDED.triprefecreg;

    -- 4) Recaudacion real del presupuesto anual municipal
    UPDATE public.pbm_presupuesto_anual
    SET premonpag = premonpag + v_monto_nuevo
    WHERE precod = v_presupuesto.precod;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_pago_tributo_cascada ON public.pat_tributo_cab;
CREATE TRIGGER trg_pago_tributo_cascada
AFTER UPDATE OF trimonpag ON public.pat_tributo_cab
FOR EACH ROW
EXECUTE FUNCTION fn_registrar_pago_tributo();

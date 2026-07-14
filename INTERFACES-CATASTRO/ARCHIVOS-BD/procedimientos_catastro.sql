-- =====================================================================
-- PROCEDIMIENTOS ALMACENADOS - SISTEMA CATASTRAL
-- Requiere haber ejecutado antes: catastroDB-postgreSQL-corregido.sql
--                                 triggers_catastro.sql
--                                 vistas_catastro.sql
-- =====================================================================


-- =====================================================================
-- 1) sp_generar_presupuesto_anual
-- =====================================================================
-- PARA QUE SIRVE:
-- Crea el presupuesto anual de una municipalidad para un año fiscal.
-- Es la base sobre la que despues se apoyan los tributos (Trigger 4
-- cascada de pagos) y el reporte de recaudacion -- sin un presupuesto
-- creado, un pago de tributo no tiene donde "aterrizar".
--
-- VALIDACIONES:
--   - La municipalidad debe existir y estar activa.
--   - No debe existir ya un presupuesto activo para esa municipalidad
--     en ese año (evita duplicar el presupuesto 2026 dos veces).
--   - El monto estimado debe ser mayor a 0.
--
-- PARAMETRO DE SALIDA:
--   p_precod  -> devuelve el codigo del presupuesto recien creado,
--                para que quien lo invoque (la app u otro procedimiento)
--                sepa a que presupuesto referirse despues.
-- =====================================================================

CREATE OR REPLACE PROCEDURE sp_generar_presupuesto_anual(
    IN  p_mun               INTEGER,
    IN  p_anio               SMALLINT,
    IN  p_monto_estimado     NUMERIC(14,2),
    IN  p_fecha_aprobacion   DATE,
    OUT p_precod             INTEGER
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM public.c1m_municipalidad
        WHERE muncod = p_mun AND munestreg = '1'
    ) THEN
        RAISE EXCEPTION 'La municipalidad % no existe o no esta activa', p_mun;
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.pbm_presupuesto_anual
        WHERE premun = p_mun AND preanio = p_anio AND preestreg = '1'
    ) THEN
        RAISE EXCEPTION 'Ya existe un presupuesto activo para la municipalidad % en el año %', p_mun, p_anio;
    END IF;

    IF p_monto_estimado IS NULL OR p_monto_estimado <= 0 THEN
        RAISE EXCEPTION 'El monto estimado debe ser mayor a 0';
    END IF;

    INSERT INTO public.pbm_presupuesto_anual
        (premun, preanio, premonest, premonpag, premongas, prefecapr, preestreg)
    VALUES
        (p_mun, p_anio, p_monto_estimado, 0, 0, p_fecha_aprobacion, '1')
    RETURNING precod INTO p_precod;

    RAISE NOTICE 'Presupuesto anual creado: municipalidad %, año %, estimado %, precod=%',
        p_mun, p_anio, p_monto_estimado, p_precod;
END;
$$;


-- =====================================================================
-- 2) sp_generar_tributos_anuales
-- =====================================================================
-- PARA QUE SIRVE:
-- Corre el proceso anual de generacion de tributos: recorre a TODOS los
-- propietarios activos que tengan al menos un predio vigente y les
-- genera su tributo del año (si es que aun no lo tienen). Cada
-- INSERT dispara en cadena el Trigger 3 (calculo por escala) y el
-- Trigger 3b (reparto proporcional en el detalle) que ya construimos.
--
-- COMO FUNCIONA:
--   - Si el propietario ya tiene un tributo activo para ese año, se
--     omite (no se duplica).
--   - Si falla el calculo para un propietario puntual (por ejemplo, su
--     ingreso no cae en ninguna escala tributaria), el proceso NO se
--     detiene para todos los demas: se registra como advertencia y
--     continua con el siguiente propietario.
--   - Al final informa cuantos se generaron y cuantos se omitieron.
-- =====================================================================

CREATE OR REPLACE PROCEDURE sp_generar_tributos_anuales(
    IN p_anio               SMALLINT,
    IN p_fecha_vencimiento  DATE DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_prop       RECORD;
    v_generados  INTEGER := 0;
    v_omitidos   INTEGER := 0;
    v_fallidos   INTEGER := 0;
    v_fecven     DATE;
BEGIN
    v_fecven := COALESCE(p_fecha_vencimiento, MAKE_DATE(p_anio, 6, 30));

    FOR v_prop IN
        SELECT DISTINCT pro.procod
        FROM public.h8m_propietario pro
        JOIN public.h8m_prop_vivienda pv ON pv.provivprp = pro.procod
                                          AND pv.provivfecfin IS NULL
                                          AND pv.provivestreg = '1'
        WHERE pro.proestreg = '1'
    LOOP
        IF EXISTS (
            SELECT 1 FROM public.pat_tributo_cab
            WHERE tripro = v_prop.procod AND trianio = p_anio AND triestreg = '1'
        ) THEN
            v_omitidos := v_omitidos + 1;
            CONTINUE;
        END IF;

        BEGIN
            INSERT INTO public.pat_tributo_cab (tripro, trianio, trifecven)
            VALUES (v_prop.procod, p_anio, v_fecven);
            v_generados := v_generados + 1;
        EXCEPTION WHEN OTHERS THEN
            v_fallidos := v_fallidos + 1;
            RAISE WARNING 'No se pudo generar el tributo % del propietario %: %',
                p_anio, v_prop.procod, SQLERRM;
        END;
    END LOOP;

    RAISE NOTICE 'Generacion de tributos % completa. Generados: %, omitidos (ya existian): %, fallidos: %',
        p_anio, v_generados, v_omitidos, v_fallidos;
END;
$$;


-- =====================================================================
-- 3) sp_registrar_transferencia_predio
-- =====================================================================
-- PARA QUE SIRVE:
-- Encapsula el evento real de "se vendio / se heredo un predio":
-- valida que el predio y el nuevo propietario existan y esten activos,
-- valida que no se este "transfiriendo" al mismo dueño que ya lo tiene,
-- e inserta la nueva titularidad. El Trigger 2 (cerrar_titularidad_anterior)
-- se encarga automaticamente de cerrar el periodo del dueño anterior.
--
-- VALIDACIONES:
--   - El predio debe existir y estar activo.
--   - El nuevo propietario debe existir y estar activo.
--   - Si el predio no tiene dueño vigente, se avisa que se registra
--     como primera titularidad (no es un error, solo informativo).
--   - No se permite "transferir" el predio a quien ya es su dueño
--     vigente (evita registros duplicados sin sentido).
-- =====================================================================

CREATE OR REPLACE PROCEDURE sp_registrar_transferencia_predio(
    IN p_vivcod        VARCHAR(10),
    IN p_procod_nuevo  INTEGER,
    IN p_fecha_inicio  DATE,
    IN p_titulo        VARCHAR(20) DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_dueno_actual INTEGER;
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM public.c3m_vivienda WHERE vivcod = p_vivcod AND vivestreg = '1'
    ) THEN
        RAISE EXCEPTION 'El predio % no existe o no esta activo', p_vivcod;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM public.h8m_propietario WHERE procod = p_procod_nuevo AND proestreg = '1'
    ) THEN
        RAISE EXCEPTION 'El propietario % no existe o no esta activo', p_procod_nuevo;
    END IF;

    SELECT provivprp INTO v_dueno_actual
    FROM public.h8m_prop_vivienda
    WHERE provivviv = p_vivcod
      AND provivfecfin IS NULL
      AND provivestreg = '1'
    LIMIT 1;

    IF v_dueno_actual IS NOT NULL AND v_dueno_actual = p_procod_nuevo THEN
        RAISE EXCEPTION 'El propietario % ya es el dueño vigente del predio %', p_procod_nuevo, p_vivcod;
    END IF;

    IF v_dueno_actual IS NULL THEN
        RAISE NOTICE 'El predio % no tenia propietario vigente; se registra como primera titularidad', p_vivcod;
    END IF;

    INSERT INTO public.h8m_prop_vivienda (provivprp, provivviv, provivfecini, provivtitulo)
    VALUES (p_procod_nuevo, p_vivcod, p_fecha_inicio, p_titulo);

    RAISE NOTICE 'Predio % transferido al propietario % desde %', p_vivcod, p_procod_nuevo, p_fecha_inicio;
END;
$$;

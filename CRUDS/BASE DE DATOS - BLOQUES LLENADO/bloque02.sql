-- =============================================================================
-- BLOQUE 02
-- CATÁLOGOS MAESTROS
--
-- Tablas:
--   c3m_via_tipo
--   c5m_tipo_predio
--   c5m_uso_predio
--   p9m_escala_tributo
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE p9m_escala_tributo;
TRUNCATE TABLE c5m_uso_predio;
TRUNCATE TABLE c5m_tipo_predio;
TRUNCATE TABLE c3m_via_tipo;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- TABLA: c3m_via_tipo
-- =============================================================================

INSERT INTO c3m_via_tipo
(VtCod,VtNom,VtEstReg)
VALUES

('AV','Avenida','1'),
('JR','Jirón','1'),
('CA','Calle','1'),
('PS','Pasaje','1'),
('PJ','Pasaje Peatonal','1'),
('PQ','Parque','1'),
('ML','Malecón','1'),
('PR','Prolongación','1'),
('UR','Urbanización','1'),
('AS','Asentamiento Humano','1'),
('CO','Condominio','1'),
('CR','Carretera','1'),
('CM','Camino','1'),
('BV','Boulevard','1'),
('AL','Alameda','1');

-- =============================================================================
-- TABLA: c5m_tipo_predio
-- =============================================================================

INSERT INTO c5m_tipo_predio
(TipPrCod,TipPrNom,TipPrDes,TipPrEstReg)
VALUES

('TP01','Casa',
'Vivienda unifamiliar independiente.',
'1'),

('TP02','Departamento',
'Unidad inmobiliaria dentro de un edificio.',
'1'),

('TP03','Dúplex',
'Vivienda distribuida en dos niveles.',
'1'),

('TP04','Tríplex',
'Vivienda distribuida en tres niveles.',
'1'),

('TP05','Local Comercial',
'Predio destinado al comercio.',
'1'),

('TP06','Oficina',
'Predio destinado a actividades administrativas.',
'1'),

('TP07','Terreno',
'Predio sin construcción.',
'1'),

('TP08','Edificio',
'Predio conformado por múltiples unidades.',
'1'),

('TP09','Depósito',
'Predio utilizado para almacenamiento.',
'1'),

('TP10','Estacionamiento',
'Predio destinado al parqueo de vehículos.',
'1');

-- =============================================================================
-- TABLA: c5m_uso_predio
-- =============================================================================

INSERT INTO c5m_uso_predio
(UsoPrCod,UsoPrNom,UsoPrDes,UsoPrEstReg)
VALUES

('UP01','Residencial',
'Uso destinado a vivienda.',
'1'),

('UP02','Comercial',
'Uso destinado al comercio.',
'1'),

('UP03','Industrial',
'Uso destinado a procesos industriales.',
'1'),

('UP04','Educativo',
'Uso destinado a instituciones educativas.',
'1'),

('UP05','Salud',
'Uso destinado a establecimientos de salud.',
'1'),

('UP06','Recreativo',
'Uso destinado a parques y recreación.',
'1'),

('UP07','Mixto',
'Predio con uso residencial y comercial.',
'1'),

('UP08','Institucional',
'Uso destinado a entidades públicas.',
'1'),

('UP09','Agrícola',
'Uso destinado a actividades agrícolas.',
'1'),

('UP10','Otros',
'Otros usos permitidos.',
'1');

-- =============================================================================
-- TABLA: p9m_escala_tributo
-- =============================================================================

INSERT INTO p9m_escala_tributo
(EscCod,
EscVig,
EscNom,
EscDesc,
EscIngMin,
EscIngMax,
EscPorTrib,
EscMonFij,
EscEstReg)
VALUES

('E001',2024,
'Escala I',
'Ingresos muy bajos',
0.00,
1200.00,
0.30,
NULL,
'1'),

('E002',2024,
'Escala II',
'Ingresos bajos',
1200.01,
2500.00,
0.60,
NULL,
'1'),

('E003',2024,
'Escala III',
'Ingresos medios',
2500.01,
4500.00,
0.90,
NULL,
'1'),

('E004',2024,
'Escala IV',
'Ingresos medio altos',
4500.01,
7000.00,
1.20,
NULL,
'1'),

('E005',2024,
'Escala V',
'Ingresos altos',
7000.01,
10000.00,
1.60,
NULL,
'1'),

('E006',2024,
'Escala VI',
'Ingresos muy altos',
10000.01,
999999.99,
2.00,
NULL,
'1');
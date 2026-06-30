-- =============================================================================
-- BLOQUE 04A
-- MAESTRO DE VIVIENDAS
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE c3m_vivienda;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO c3m_vivienda
(
    VivCod,
    VivZon,
    VivDir,
    VivUbigeo,
    VivTipPr,
    VivUsoPr,
    VivVal,
    VivEstReg
)
VALUES

('VIV000001',1,1,'040101000001','TP01','UP01',185000.00,'1'),
('VIV000002',1,2,'040101000002','TP01','UP01',210000.00,'1'),
('VIV000003',2,3,'040101000003','TP01','UP01',195500.00,'1'),
('VIV000004',2,4,'040101000004','TP01','UP01',225000.00,'1'),
('VIV000005',3,5,'040101000005','TP01','UP01',235000.00,'1'),

('VIV000006',4,6,'040101000006','TP08','UP07',820000.00,'1'),
('VIV000007',5,7,'040101000007','TP08','UP07',915000.00,'1'),
('VIV000008',6,8,'040101000008','TP08','UP07',980000.00,'1'),
('VIV000009',7,9,'040101000009','TP08','UP07',1100000.00,'1'),
('VIV000010',8,10,'040101000010','TP08','UP07',1200000.00,'1'),

('VIV000011',9,11,'040101000011','TP01','UP01',175000.00,'1'),
('VIV000012',10,12,'040101000012','TP01','UP01',192000.00,'1'),
('VIV000013',11,13,'040101000013','TP01','UP01',203000.00,'1'),
('VIV000014',12,14,'040101000014','TP01','UP01',247000.00,'1'),
('VIV000015',13,15,'150101000001','TP05','UP02',560000.00,'1'),

('VIV000016',14,16,'150101000002','TP08','UP07',1400000.00,'1'),
('VIV000017',15,17,'150101000003','TP08','UP07',1500000.00,'1'),
('VIV000018',16,18,'150101000004','TP05','UP02',620000.00,'1'),
('VIV000019',17,19,'150101000005','TP06','UP08',450000.00,'1'),
('VIV000020',18,20,'150101000006','TP01','UP01',265000.00,'1');


-- =============================================================================
-- BLOQUE 04B
-- ESPECIALIZACIÓN DE VIVIENDAS
--
-- Tablas:
--      c3m_casa_particular
--      c3m_bloque_casas
-- =============================================================================

SET FOREIGN_KEY_CHECKS=0;

TRUNCATE TABLE c4m_departamento;
TRUNCATE TABLE c3m_bloque_casas;
TRUNCATE TABLE c3m_casa_particular;

SET FOREIGN_KEY_CHECKS=1;

-- =============================================================================
-- CASAS PARTICULARES
-- =============================================================================

INSERT INTO c3m_casa_particular
(
    CasVivCod,
    CasMetC,
    CasOd,
    CasEstReg
)
VALUES

('VIV000001',145.50,'Casa de un nivel con jardín','1'),
('VIV000002',162.80,'Casa familiar','1'),
('VIV000003',138.20,'Casa de material noble','1'),
('VIV000004',174.90,'Casa de dos pisos','1'),
('VIV000005',180.30,'Casa residencial','1'),
('VIV000011',126.40,'Casa unifamiliar','1'),
('VIV000012',132.75,'Casa tradicional','1'),
('VIV000013',141.90,'Casa esquina','1'),
('VIV000014',189.60,'Casa moderna','1'),

('VIV000015',250.00,'Local comercial independiente','1'),

('VIV000018',210.50,'Local comercial','1'),

('VIV000019',165.80,'Oficina independiente','1'),

('VIV000020',156.30,'Casa residencial','1');

-- =============================================================================
-- BLOQUES DE CASAS (EDIFICIOS)
-- =============================================================================

INSERT INTO c3m_bloque_casas
(
    BloVivCod,
    BloMetB,
    BloOd,
    BloEstReg
)
VALUES

('VIV000006',1250.00,'Edificio Multifamiliar Recoleta','1'),

('VIV000007',1680.00,'Condominio Cayma','1'),

('VIV000008',1845.00,'Residencial La Tomilla','1'),

('VIV000009',2120.00,'Residencial Ciudad Municipal','1'),

('VIV000010',2465.00,'Conjunto Habitacional','1'),

('VIV000016',3520.00,'Centro Empresarial Lima','1'),

('VIV000017',4180.00,'Edificio Financiero','1');


-- =============================================================================
-- BLOQUE 04C
-- DEPARTAMENTOS
--
-- Tabla:
--      c4m_departamento
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE c4m_departamento;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO c4m_departamento
(
    DepBloCod,
    DepEsc,
    DepNiv,
    DepPue,
    DepMetP,
    DepOd,
    DepEstReg
)
VALUES

-- ============================================================================
-- EDIFICIO VIV000006
-- ============================================================================

('VIV000006','A',1,'101',82.50,'Departamento 101','1'),
('VIV000006','A',1,'102',79.80,'Departamento 102','1'),
('VIV000006','A',2,'201',85.10,'Departamento 201','1'),
('VIV000006','A',2,'202',88.60,'Departamento 202','1'),
('VIV000006','A',3,'301',92.40,'Penthouse','1'),

-- ============================================================================
-- EDIFICIO VIV000007
-- ============================================================================

('VIV000007','A',1,'101',75.00,'Departamento 101','1'),
('VIV000007','A',1,'102',77.50,'Departamento 102','1'),
('VIV000007','A',2,'201',82.30,'Departamento 201','1'),
('VIV000007','A',2,'202',81.20,'Departamento 202','1'),
('VIV000007','A',3,'301',95.40,'Penthouse','1'),

-- ============================================================================
-- EDIFICIO VIV000008
-- ============================================================================

('VIV000008','A',1,'101',71.50,'Departamento 101','1'),
('VIV000008','A',1,'102',73.80,'Departamento 102','1'),
('VIV000008','A',2,'201',80.20,'Departamento 201','1'),
('VIV000008','A',2,'202',81.60,'Departamento 202','1'),
('VIV000008','A',3,'301',93.00,'Penthouse','1'),

-- ============================================================================
-- EDIFICIO VIV000009
-- ============================================================================

('VIV000009','A',1,'101',78.90,'Departamento 101','1'),
('VIV000009','A',1,'102',79.30,'Departamento 102','1'),
('VIV000009','A',2,'201',84.20,'Departamento 201','1'),
('VIV000009','A',2,'202',87.60,'Departamento 202','1'),
('VIV000009','A',3,'301',96.40,'Penthouse','1'),

-- ============================================================================
-- EDIFICIO VIV000010
-- ============================================================================

('VIV000010','A',1,'101',83.50,'Departamento 101','1'),
('VIV000010','A',1,'102',84.10,'Departamento 102','1'),
('VIV000010','A',2,'201',89.70,'Departamento 201','1'),
('VIV000010','A',2,'202',91.20,'Departamento 202','1'),
('VIV000010','A',3,'301',105.30,'Penthouse','1'),

-- ============================================================================
-- EDIFICIO VIV000016
-- ============================================================================

('VIV000016','A',1,'101',120.50,'Oficina 101','1'),
('VIV000016','A',2,'201',126.80,'Oficina 201','1'),
('VIV000016','A',3,'301',131.60,'Oficina 301','1'),
('VIV000016','A',4,'401',138.40,'Oficina 401','1'),
('VIV000016','A',5,'501',145.20,'Oficina Gerencia','1'),

-- ============================================================================
-- EDIFICIO VIV000017
-- ============================================================================

('VIV000017','A',1,'101',135.40,'Oficina 101','1'),
('VIV000017','A',2,'201',142.10,'Oficina 201','1'),
('VIV000017','A',3,'301',148.80,'Oficina 301','1'),
('VIV000017','A',4,'401',156.30,'Oficina Ejecutiva','1'),
('VIV000017','A',5,'501',168.50,'Oficina Presidencia','1');



-- =============================================================================
-- BLOQUE 04D
-- PARTIDAS REGISTRALES TIPO SUNARP
--
-- Tabla:
--      c3m_partida_registral
-- =============================================================================

SET FOREIGN_KEY_CHECKS=0;

TRUNCATE TABLE c3m_partida_registral;

SET FOREIGN_KEY_CHECKS=1;

INSERT INTO c3m_partida_registral
(
    PrtViv,
    PrtZonReg,
    PrtNumPartida,
    PrtFolioTomo,
    PrtFolioFolio,
    PrtAsiento,
    PrtTipoActo,
    PrtPropNom,
    PrtPropDNI,
    PrtEst,
    PrtFecIns,
    PrtFecUltAct,
    PrtAreaM2,
    PrtObs,
    PrtEstReg
)
VALUES

('VIV000001','ZRXII','110000001','T001','F001','C0001',
'Compraventa','Juan Pérez Quispe','72154896',
'A','2018-03-15','2024-01-10',145.50,
'Sin observaciones','1'),

('VIV000002','ZRXII','110000002','T001','F002','C0001',
'Compraventa','María Torres Díaz','45612378',
'A','2019-07-21','2024-01-12',162.80,
'Sin observaciones','1'),

('VIV000003','ZRXII','110000003','T001','F003','C0001',
'Compraventa','Luis Gutiérrez Soto','41236587',
'A','2020-01-18','2024-02-05',138.20,
'Sin observaciones','1'),

('VIV000004','ZRXII','110000004','T001','F004','C0001',
'Compraventa','Rosa Medina Flores','74859632',
'A','2017-09-10','2024-02-20',174.90,
'Sin observaciones','1'),

('VIV000005','ZRXII','110000005','T001','F005','C0001',
'Compraventa','Carlos Ramírez León','49875612',
'A','2021-04-15','2024-03-10',180.30,
'Sin observaciones','1'),

('VIV000006','ZRXII','110000006','T002','F001','C0001',
'Declaratoria de Fábrica','Inmobiliaria Recoleta SAC','20547896321',
'A','2016-06-20','2024-02-15',1250.00,
'Edificio multifamiliar','1'),

('VIV000007','ZRXII','110000007','T002','F002','C0001',
'Declaratoria de Fábrica','Constructora Cayma SAC','20458796325',
'A','2018-08-05','2024-01-18',1680.00,
'Condominio inscrito','1'),

('VIV000008','ZRXII','110000008','T002','F003','C0001',
'Declaratoria de Fábrica','Residencial La Tomilla SAC','20548963214',
'A','2019-11-08','2024-01-20',1845.00,
'Edificio multifamiliar','1'),

('VIV000009','ZRXII','110000009','T002','F004','C0001',
'Declaratoria de Fábrica','Inversiones Pachacutec SAC','20587412369',
'A','2020-05-10','2024-02-18',2120.00,
'Edificio residencial','1'),

('VIV000010','ZRXII','110000010','T002','F005','C0001',
'Declaratoria de Fábrica','Conjunto Habitacional Sur SAC','20612345789',
'A','2022-03-12','2024-03-02',2465.00,
'Conjunto habitacional','1'),

('VIV000011','ZRXII','110000011','T003','F001','C0001',
'Compraventa','José Herrera Ruiz','72369854',
'A','2016-07-15','2024-01-05',126.40,
'Sin observaciones','1'),

('VIV000012','ZRXII','110000012','T003','F002','C0001',
'Compraventa','Ana Salazar Paredes','47896521',
'A','2018-10-11','2024-02-08',132.75,
'Sin observaciones','1'),

('VIV000013','ZRXII','110000013','T003','F003','C0001',
'Compraventa','Miguel Vargas Ramos','75489632',
'A','2021-01-09','2024-02-14',141.90,
'Sin observaciones','1'),

('VIV000014','ZRXII','110000014','T003','F004','C0001',
'Compraventa','Patricia Gómez Silva','71458963',
'A','2020-12-18','2024-03-01',189.60,
'Sin observaciones','1'),

('VIV000015','ZRXII','110000015','T003','F005','C0001',
'Compraventa','Comercial El Sol SAC','20541236987',
'A','2019-09-05','2024-02-28',250.00,
'Local comercial','1'),

('VIV000016','ZRIX','150000001','T004','F001','C0001',
'Declaratoria de Fábrica','Centro Empresarial Lima SAC','20598745632',
'A','2017-05-20','2024-01-25',3520.00,
'Edificio corporativo','1'),

('VIV000017','ZRIX','150000002','T004','F002','C0001',
'Declaratoria de Fábrica','Finanzas del Perú SAC','20635789412',
'A','2018-11-11','2024-02-11',4180.00,
'Edificio financiero','1'),

('VIV000018','ZRIX','150000003','T004','F003','C0001',
'Compraventa','Negocios Lima SAC','20458963215',
'A','2021-08-18','2024-03-04',210.50,
'Local comercial','1'),

('VIV000019','ZRIX','150000004','T004','F004','C0001',
'Compraventa','Consultores Integrales SAC','20574123698',
'A','2022-02-14','2024-03-10',165.80,
'Oficina administrativa','1'),

('VIV000020','ZRIX','150000005','T004','F005','C0001',
'Compraventa','Julia Fernández Soto','73124589',
'A','2020-04-22','2024-03-15',156.30,
'Casa habitación','1');





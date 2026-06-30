-- =============================================================================
-- BLOQUE 03
-- ZONAS Y DIRECCIONES
--
-- Tablas:
--      c2m_zona
--      c3m_direccion
-- =============================================================================

SET FOREIGN_KEY_CHECKS=0;

TRUNCATE TABLE c3m_direccion;
TRUNCATE TABLE c2m_zona;

SET FOREIGN_KEY_CHECKS=1;

-- =============================================================================
-- ZONAS
-- =============================================================================

INSERT INTO c2m_zona
(
ZonMun,
ZonNom,
ZonDes,
ZonTip,
ZonSupKm2,
ZonEstReg
)
VALUES

(1,'Centro Histórico',
'Zona monumental del distrito',
'U',
3.50,
'1'),

(1,'Vallecito',
'Zona residencial tradicional',
'U',
2.10,
'1'),

(1,'Umacollo',
'Zona residencial de crecimiento',
'U',
4.60,
'1'),

(1,'La Recoleta',
'Zona urbana residencial',
'U',
2.90,
'1'),

(2,'Cayma Alta',
'Zona urbana de expansión',
'U',
5.80,
'1'),

(2,'Cayma Baja',
'Zona comercial',
'U',
3.40,
'1'),

(2,'Buenos Aires',
'Zona residencial',
'U',
2.70,
'1'),

(2,'La Tomilla',
'Zona urbana',
'U',
4.20,
'1'),

(3,'Ciudad Municipal',
'Zona residencial',
'U',
6.80,
'1'),

(3,'Semirural Pachacutec',
'Zona urbana consolidada',
'U',
7.10,
'1'),

(3,'José Luis Bustamante',
'Zona comercial',
'U',
3.90,
'1'),

(3,'Alto Libertad',
'Zona residencial',
'U',
5.00,
'1'),

(4,'Centro Cívico',
'Zona administrativa',
'U',
8.00,
'1'),

(4,'Barrios Altos',
'Zona histórica',
'U',
4.60,
'1'),

(4,'Mirones',
'Zona residencial',
'U',
5.20,
'1'),

(5,'Centro Financiero',
'Zona empresarial',
'U',
6.50,
'1'),

(5,'Santa Beatriz',
'Zona residencial',
'U',
4.30,
'1'),

(5,'Cercado Norte',
'Zona comercial',
'U',
5.80,
'1');

-- =============================================================================
-- DIRECCIONES
-- =============================================================================

INSERT INTO c3m_direccion
(
DirZon,
DirViaTip,
DirViaNom,
DirNum,
DirInt,
DirUrb,
DirRef,
DirCodPos,
DirLat,
DirLon,
DirEstReg
)
VALUES

(1,'AV','Ejército','101',NULL,'Centro Histórico',
'Frente al Hospital','04001',
-16.3928512,-71.5361425,'1'),

(1,'CA','San Francisco','235',NULL,'Centro Histórico',
'Cerca a la Plaza','04001',
-16.3984551,-71.5376654,'1'),

(2,'JR','Lima','456','201','Vallecito',
'Costado del parque','04002',
-16.4012220,-71.5298764,'1'),

(2,'AV','Dolores','789',NULL,'Vallecito',
'Frente al colegio','04002',
-16.4051245,-71.5276512,'1'),

(3,'CA','Los Pinos','120','102','Umacollo',
'Esquina principal','04003',
-16.4108547,-71.5327415,'1'),

(4,'AV','Goyeneche','1500',NULL,'Recoleta',
'Frente al hospital','04004',
-16.3974558,-71.5241224,'1'),

(5,'AV','Ejército','2500','501','Cayma',
'Real Plaza','04013',
-16.3764215,-71.5442003,'1'),

(6,'CA','Tronchadero','560',NULL,'Cayma',
'A media cuadra del mercado','04013',
-16.3812110,-71.5421156,'1'),

(7,'JR','Los Arces','890','302','Buenos Aires',
'Frente al parque','04014',
-16.3708544,-71.5488541,'1'),

(8,'AV','Bolognesi','1045',NULL,'La Tomilla',
'Cruce con Ejército','04015',
-16.3662147,-71.5471210,'1'),

(9,'CA','Los Cedros','220',NULL,'Ciudad Municipal',
'Costado del estadio','04016',
-16.3551247,-71.5601452,'1'),

(10,'AV','Aviación','950',NULL,'Pachacutec',
'Frente a la comisaría','04017',
-16.3512447,-71.5621475,'1'),

(11,'JR','Los Álamos','402','202','Bustamante',
'Cerca al mercado','04018',
-16.3485221,-71.5598452,'1'),

(12,'CA','Libertad','650',NULL,'Alto Libertad',
'Frente al colegio','04019',
-16.3462000,-71.5575200,'1'),

(13,'AV','Abancay','1800',NULL,'Centro Cívico',
'Ministerio Público','15001',
-12.0463740,-77.0427934,'1'),

(14,'JR','Ancash','980','401','Barrios Altos',
'Frente a iglesia','15001',
-12.0501222,-77.0307441,'1'),

(15,'CA','Virú','415',NULL,'Mirones',
'Mercado principal','15001',
-12.0557111,-77.0504114,'1'),

(16,'AV','Arequipa','2525','1203','San Isidro',
'Centro Empresarial','15046',
-12.0975444,-77.0341255,'1'),

(17,'AV','Petit Thouars','3180',NULL,'Santa Beatriz',
'Cerca al estadio','15046',
-12.0845221,-77.0400112,'1'),

(18,'CA','Washington','775',NULL,'Cercado',
'Palacio de Justicia','15001',
-12.0587445,-77.0365224,'1');
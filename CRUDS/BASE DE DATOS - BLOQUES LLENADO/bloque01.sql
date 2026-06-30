-- =============================================================================
-- BLOQUE 01
-- UBIGEO GEOGRÁFICO Y ESTRUCTURA MUNICIPAL
-- Tablas:
--   c1m_region
--   c1m_provincia
--   c1m_distrito
--   c1m_municipalidad
-- =============================================================================

-- Desactivar validación de claves foráneas
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE c1m_municipalidad;
TRUNCATE TABLE c1m_distrito;
TRUNCATE TABLE c1m_provincia;
TRUNCATE TABLE c1m_region;

-- Activar nuevamente las claves foráneas
SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- REGIONES
-- =============================================================================

INSERT INTO c1m_region (RegCod, RegNom, RegEstReg) VALUES
(1,'Amazonas','1'),
(2,'Áncash','1'),
(3,'Apurímac','1'),
(4,'Arequipa','1'),
(5,'Ayacucho','1'),
(6,'Cajamarca','1'),
(7,'Callao','1'),
(8,'Cusco','1'),
(9,'Huancavelica','1'),
(10,'Huánuco','1'),
(11,'Ica','1'),
(12,'Junín','1'),
(13,'La Libertad','1'),
(14,'Lambayeque','1'),
(15,'Lima','1'),
(16,'Loreto','1'),
(17,'Madre de Dios','1'),
(18,'Moquegua','1'),
(19,'Pasco','1'),
(20,'Piura','1'),
(21,'Puno','1'),
(22,'San Martín','1'),
(23,'Tacna','1'),
(24,'Tumbes','1'),
(25,'Ucayali','1');

-- =============================================================================
-- PROVINCIAS
-- =============================================================================

INSERT INTO c1m_provincia (ProCod, ProReg, ProNom, ProEstReg) VALUES
(401,4,'Arequipa','1'),
(402,4,'Camaná','1'),
(403,4,'Caravelí','1'),
(404,4,'Castilla','1'),
(405,4,'Caylloma','1'),
(406,4,'Condesuyos','1'),
(407,4,'Islay','1'),
(408,4,'La Unión','1'),

(1501,15,'Lima','1'),
(1502,15,'Barranca','1'),
(1503,15,'Cajatambo','1'),
(1504,15,'Canta','1'),
(1505,15,'Cañete','1'),
(1506,15,'Huaral','1'),
(1507,15,'Huarochirí','1'),
(1508,15,'Huaura','1');

-- =============================================================================
-- DISTRITOS
-- =============================================================================

INSERT INTO c1m_distrito (DisCod,DisPro,DisNom,DisEstReg) VALUES

(40101,401,'Arequipa','1'),
(40102,401,'Alto Selva Alegre','1'),
(40103,401,'Cayma','1'),
(40104,401,'Cerro Colorado','1'),
(40105,401,'Characato','1'),
(40106,401,'Chiguata','1'),
(40107,401,'Jacobo Hunter','1'),
(40108,401,'José Luis Bustamante y Rivero','1'),
(40109,401,'Mariano Melgar','1'),
(40110,401,'Miraflores','1'),
(40111,401,'Mollebaya','1'),
(40112,401,'Paucarpata','1'),
(40113,401,'Pocsi','1'),
(40114,401,'Polobaya','1'),
(40115,401,'Quequeña','1'),
(40116,401,'Sabandía','1'),
(40117,401,'Sachaca','1'),
(40118,401,'San Juan de Siguas','1'),
(40119,401,'San Juan de Tarucani','1'),
(40120,401,'Santa Isabel de Siguas','1'),
(40121,401,'Santa Rita de Siguas','1'),
(40122,401,'Socabaya','1'),
(40123,401,'Tiabaya','1'),
(40124,401,'Uchumayo','1'),
(40125,401,'Vítor','1'),
(40126,401,'Yanahuara','1'),
(40127,401,'Yarabamba','1'),
(40128,401,'Yura','1'),

(150101,1501,'Lima','1'),
(150102,1501,'Ancón','1'),
(150103,1501,'Ate','1'),
(150104,1501,'Barranco','1'),
(150105,1501,'Breña','1'),
(150106,1501,'Carabayllo','1'),
(150107,1501,'Chaclacayo','1'),
(150108,1501,'Chorrillos','1'),
(150109,1501,'Cieneguilla','1'),
(150110,1501,'Comas','1');

-- =============================================================================
-- MUNICIPALIDADES
-- =============================================================================

INSERT INTO c1m_municipalidad
(MunCod,MunDis,MunNom,MunAlcNom,MunCor,MunTelCon,MunEstReg)
VALUES

(1,40126,'Municipalidad Distrital de Yanahuara',
'Sergio Bolliger',
'mesa.partes@muniyanahuara.gob.pe',
'054253661',
'1'),

(2,40103,'Municipalidad Distrital de Cayma',
'Juan Carlos Linares',
'mesa.partes@municayma.gob.pe',
'054382350',
'1'),

(3,40104,'Municipalidad Distrital de Cerro Colorado',
'Manuel Vera',
'mesa.partes@municerrocolorado.gob.pe',
'054251512',
'1'),

(4,40112,'Municipalidad Distrital de Paucarpata',
'Luis Cornejo',
'contacto@munipaucarpata.gob.pe',
'054401122',
'1'),

(5,150101,'Municipalidad Metropolitana de Lima',
'Rafael López Aliaga',
'mesa.partes@munlima.gob.pe',
'014326000',
'1');


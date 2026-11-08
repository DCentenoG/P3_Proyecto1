-- ===========================================================================
--  Sistema de Reserva de Recursos - Esquema de la base de datos
--  EIF206 Programacion 3 - Proyecto 2
--
--  Motor: Oracle Database 11g Express Edition (11.2.0.2.0)
--
--  Ejecucion:
--      sqlplus system/<clave>@//localhost:1521/XEXDB @db/schema.sql
--  o abriendo este archivo en SQL Developer y usando "Run Script" (F5).
--
--  El script es idempotente: borra las tablas si existen y las vuelve a crear,
--  asi que se puede ejecutar varias veces mientras se desarrolla.
--
--  NOTA SOBRE LA VERSION: 11g no tiene columnas IDENTITY (llegaron en 12c) ni
--  "DROP TABLE IF EXISTS". Por eso los identificadores se generan en Java
--  derivandolos de MAX(id)+1 al cargar, igual que hacia el repositorio XML, y
--  el borrado de tablas se hace con bloques PL/SQL que ignoran el error
--  ORA-00942 ("table or view does not exist").
-- ===========================================================================


-- ---------------------------------------------------------------------------
-- 1. Borrado previo. En orden inverso a las dependencias: primero las tablas
--    puente, que apuntan a las demas.
-- ---------------------------------------------------------------------------

BEGIN EXECUTE IMMEDIATE 'DROP TABLE RESERVA_RECURSO';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE RESERVA_CATEGORIA';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE RESERVA';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE RECURSO';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE CATEGORIA';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE USUARIO';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -942 THEN RAISE; END IF; END;
/


-- ---------------------------------------------------------------------------
-- 2. USUARIO
--    Administradores y funcionarios comparten tabla y un unico contador de id,
--    igual que en el modelo (Model.UserContainer genera ids unicos para ambos,
--    y Model.User define la identidad por ese id).
--    Un administrador no tiene nombre ni telefono: esas columnas quedan nulas.
-- ---------------------------------------------------------------------------

CREATE TABLE USUARIO (
    id          NUMBER(10)      NOT NULL,
    tipo        VARCHAR2(10)    NOT NULL,
    password    VARCHAR2(100)   NOT NULL,
    nombre      VARCHAR2(100),
    telefono    NUMBER(15),
    CONSTRAINT pk_usuario       PRIMARY KEY (id),
    CONSTRAINT ck_usuario_tipo  CHECK (tipo IN ('ADMIN', 'EMPLOYEE'))
);

COMMENT ON TABLE  USUARIO          IS 'Administradores y funcionarios del sistema';
COMMENT ON COLUMN USUARIO.tipo     IS 'ADMIN o EMPLOYEE';
COMMENT ON COLUMN USUARIO.nombre   IS 'Solo para EMPLOYEE; nulo en ADMIN';
COMMENT ON COLUMN USUARIO.telefono IS 'Solo para EMPLOYEE; nulo en ADMIN';


-- ---------------------------------------------------------------------------
-- 3. CATEGORIA
--    El id es texto con formato 'CAT-000001', que es el que ya usan las vistas
--    y las plantillas de reportes del Proyecto 1.
-- ---------------------------------------------------------------------------

CREATE TABLE CATEGORIA (
    id          VARCHAR2(12)    NOT NULL,
    descripcion VARCHAR2(100)   NOT NULL,
    CONSTRAINT pk_categoria     PRIMARY KEY (id)
);

COMMENT ON TABLE  CATEGORIA    IS 'Categorias de recursos (salas, proyectores, ...)';
COMMENT ON COLUMN CATEGORIA.id IS 'Formato CAT-000001, generado por Model.CategoryContainer';


-- ---------------------------------------------------------------------------
-- 4. RECURSO
--    Clave primaria COMPUESTA (categoria_id, id): el id de un recurso solo es
--    unico dentro de su categoria. Es la misma identidad que define
--    Model.Resource.equals, y por eso el recurso 1 de "Salas" y el 1 de
--    "Proyectores" son recursos distintos.
-- ---------------------------------------------------------------------------

CREATE TABLE RECURSO (
    categoria_id    VARCHAR2(12)    NOT NULL,
    id              NUMBER(10)      NOT NULL,
    descripcion     VARCHAR2(100)   NOT NULL,
    CONSTRAINT pk_recurso           PRIMARY KEY (categoria_id, id),
    CONSTRAINT fk_recurso_categoria FOREIGN KEY (categoria_id)
                                    REFERENCES CATEGORIA (id)
);

COMMENT ON TABLE RECURSO IS 'Recursos concretos de cada categoria';


-- ---------------------------------------------------------------------------
-- 5. RESERVA
--    Las horas van como texto 'HH:mm' porque Oracle no tiene un tipo
--    equivalente a LocalTime: DATE y TIMESTAMP siempre arrastran una fecha.
--    Guardarlas asi las deja legibles al inspeccionar la tabla a mano.
--
--    recibida_en / vista_en corresponden a Model.RequestNotification: cuando
--    llego la solicitud al administrador y cuando la marco como vista.
--    vista_en nulo significa "no vista".
-- ---------------------------------------------------------------------------

CREATE TABLE RESERVA (
    id              NUMBER(10)      NOT NULL,
    empleado_id     NUMBER(10)      NOT NULL,
    actividad       VARCHAR2(200)   NOT NULL,
    fecha           DATE            NOT NULL,
    hora_inicio     VARCHAR2(5)     NOT NULL,
    hora_fin        VARCHAR2(5)     NOT NULL,
    estado          VARCHAR2(12)    NOT NULL,
    motivo_rechazo  VARCHAR2(500),
    recibida_en     TIMESTAMP,
    vista_en        TIMESTAMP,
    CONSTRAINT pk_reserva           PRIMARY KEY (id),
    CONSTRAINT fk_reserva_empleado  FOREIGN KEY (empleado_id)
                                    REFERENCES USUARIO (id),
    CONSTRAINT ck_reserva_estado    CHECK (estado IN
                                    ('IN_REVIEW', 'ACCEPTED', 'REJECTED', 'CANCELLED'))
);

COMMENT ON TABLE  RESERVA                IS 'Reservas solicitadas por los funcionarios';
COMMENT ON COLUMN RESERVA.hora_inicio    IS 'Formato HH:mm';
COMMENT ON COLUMN RESERVA.hora_fin       IS 'Formato HH:mm';
COMMENT ON COLUMN RESERVA.estado         IS 'Valores del enum Model.State';
COMMENT ON COLUMN RESERVA.motivo_rechazo IS 'Solo cuando estado = REJECTED';
COMMENT ON COLUMN RESERVA.vista_en       IS 'Nulo mientras el administrador no la haya visto';


-- ---------------------------------------------------------------------------
-- 6. RESERVA_CATEGORIA  -  las categorias REQUERIDAS por la reserva
--    Es lo que el funcionario pidio al crearla ("necesito una sala y un
--    proyector"), antes de que el administrador decida cuales recursos asigna.
-- ---------------------------------------------------------------------------

CREATE TABLE RESERVA_CATEGORIA (
    reserva_id      NUMBER(10)      NOT NULL,
    categoria_id    VARCHAR2(12)    NOT NULL,
    CONSTRAINT pk_reserva_categoria PRIMARY KEY (reserva_id, categoria_id),
    CONSTRAINT fk_rc_reserva        FOREIGN KEY (reserva_id)
                                    REFERENCES RESERVA (id) ON DELETE CASCADE,
    CONSTRAINT fk_rc_categoria      FOREIGN KEY (categoria_id)
                                    REFERENCES CATEGORIA (id)
);

COMMENT ON TABLE RESERVA_CATEGORIA IS 'Categorias que la reserva solicito';


-- ---------------------------------------------------------------------------
-- 7. RESERVA_RECURSO  -  los recursos ASIGNADOS al aprobarla
--    Se llena cuando el administrador aprueba la solicitud y elige un recurso
--    concreto por cada categoria requerida.
-- ---------------------------------------------------------------------------

CREATE TABLE RESERVA_RECURSO (
    reserva_id      NUMBER(10)      NOT NULL,
    categoria_id    VARCHAR2(12)    NOT NULL,
    recurso_id      NUMBER(10)      NOT NULL,
    CONSTRAINT pk_reserva_recurso   PRIMARY KEY (reserva_id, categoria_id, recurso_id),
    CONSTRAINT fk_rr_reserva        FOREIGN KEY (reserva_id)
                                    REFERENCES RESERVA (id) ON DELETE CASCADE,
    CONSTRAINT fk_rr_recurso        FOREIGN KEY (categoria_id, recurso_id)
                                    REFERENCES RECURSO (categoria_id, id)
);

COMMENT ON TABLE RESERVA_RECURSO IS 'Recursos asignados a la reserva al aprobarla';


-- ---------------------------------------------------------------------------
-- 8. Indices
--    Las claves primarias ya crean su indice. Estos son para las consultas que
--    mas se repiten y para las columnas de clave foranea, que Oracle NO indexa
--    automaticamente.
-- ---------------------------------------------------------------------------

-- "Mis reservas" y toda operacion que parte del funcionario.
CREATE INDEX ix_reserva_empleado ON RESERVA (empleado_id);

-- Calendarizacion y estadisticas filtran por fecha.
CREATE INDEX ix_reserva_fecha ON RESERVA (fecha);

-- El sidebar del administrador lista las solicitudes En revision.
CREATE INDEX ix_reserva_estado ON RESERVA (estado);

-- Verificacion de disponibilidad: que reservas ocupan este recurso.
CREATE INDEX ix_rr_recurso ON RESERVA_RECURSO (categoria_id, recurso_id);

-- Borrado de una categoria: la base verifica que nadie la referencie.
CREATE INDEX ix_rc_categoria ON RESERVA_CATEGORIA (categoria_id);


-- ---------------------------------------------------------------------------
-- 9. Sobre el borrado
--    Las claves foraneas de las dos tablas puente hacia RESERVA son
--    ON DELETE CASCADE: si una reserva desaparece, sus filas de categorias
--    requeridas y recursos asignados se van con ella.
--
--    El resto NO tiene cascada, a proposito. La capa de servicio ya impide
--    eliminar un funcionario con reservas o una categoria con recursos, y
--    conviene que la base sea la segunda linea de defensa: si algun dia esa
--    validacion falla, el error aparece aqui en vez de corromper los datos.
-- ---------------------------------------------------------------------------

COMMIT;

-- Verificacion rapida: deberia devolver las 6 tablas.
SELECT table_name FROM user_tables
WHERE table_name IN ('USUARIO', 'CATEGORIA', 'RECURSO', 'RESERVA',
                     'RESERVA_CATEGORIA', 'RESERVA_RECURSO')
ORDER BY table_name;

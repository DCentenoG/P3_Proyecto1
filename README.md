# Sistema de Gestión y Reserva de Recursos

Proyecto académico para el curso **EIF206 - Programación III**, Universidad Nacional de Costa Rica, Facultad de Ciencias Exactas y Naturales, Escuela de Informática.

Sistema de escritorio en Java (Swing) para que funcionarios y administradores de una organización gestionen usuarios, categorías y recursos, registren y consulten reservas, visualicen la calendarización de actividades/recursos y generen estadísticas y reportes. Incluye una funcionalidad de Inteligencia Artificial que permite registrar una reserva describiéndola en lenguaje natural, y generación de reportes en PDF.

En su segunda etapa el sistema se replanteó como una **aplicación distribuida**. La interfaz y la lógica de negocio dejaron de vivir en un mismo proceso: hoy corren como dos programas independientes que se comunican por **sockets con hilos**, aplicando el patrón **Proxy** para que el Frontend invoque el servicio remoto como si fuera local. A eso se suman la migración de la persistencia del archivo XML a una **base de datos Oracle**, un **área de alertas** que avisa en tiempo real a todos los usuarios conectados cuando alguien registra una reserva, y una **suite de pruebas automatizadas** con JUnit Jupiter, de unidad (Surefire) y de integración (Failsafe).

Este repositorio contiene el historial completo de las dos etapas. Las secciones que siguen conservan lo que se hizo en el Proyecto 1 y marcan aparte lo que se agregó o cambió en el Proyecto 2.

## Integrantes y roles

| Integrante | Rol dentro del proyecto |
|---|---|
| **Daniel Centeno Gutierrez** | *Proyecto 1:* Funcionalidades de la capa Vista, capa Controlador, generación de reportes PDF y autocompletado de reservas por IA.<br><br>*Proyecto 2:* Reestructuración a módulos Maven; identidad de las entidades por id en lugar de comparación por referencia; traslado de la lógica de consulta del Controlador a Service e introducción de los DTOs; definición de `IService` como contrato único; serialización del dominio y paquete `Protocol`; migración completa a base de datos Oracle (esquema, los cuatro DAOs, repositorio e importador del XML); y la suite de pruebas JUnit. |
| **Juan Pablo Arroyo Zumbado** | *Proyecto 1:* Lógica interna de la capa Modelo y manejo de excepciones, capa Controlador y autocompletado de reservas por IA.<br><br>*Proyecto 2:* Implementación de la comunicación por sockets e hilos — el servidor del Backend (`ReservationServer`, `ClientHandler`, `ClientRegistry`, `PushChannel`) y el Proxy del Frontend (`ServiceProxy`, `NotificationListener`) — y el área de alertas en tiempo real (`AlertCenter`, `AlertsPanel`), con su integración en el ciclo de vida de la sesión. |

## Funcionalidades implementadas

- **Autenticación**: inicio de sesión y cambio de clave, para roles de Administrador y Funcionario.
- **Gestión de funcionarios, categorías y recursos** (Administrador): alta, edición, borrado y filtrado/búsqueda en tiempo real (CRUD) sobre cada entidad.
- **Reservas** (Funcionario): registro de reservas asignando recursos por categoría (todo-o-nada: si alguna categoría requerida no tiene disponibilidad, no se guarda nada de la reserva), consulta de "Mis reservas" y cancelación.
- **Autocompletado de reservas por IA**: un campo de "Frase" en el formulario de reservas donde el funcionario describe la reserva en lenguaje natural (ej. "Necesito la sala de reuniones mañana de 2 a 4pm"); el sistema invoca un LLM (API de Groq) que extrae actividad, fecha, horas y categorías y llena el formulario, que el usuario puede revisar/editar antes de guardar. La IA solo interpreta texto: la validación de disponibilidad y existencia de recursos la sigue haciendo la capa Service/Model, igual que en el flujo manual.
- **Calendarización**: vista de calendario de actividades y de recursos, para Administrador y Funcionario.
- **Estadísticas**: paneles de conteo por categoría de recurso y por semana, filtrables por rango de fechas, con gráfico de barras.
- **Reportes en PDF**: generación e impresión de reportes (categorías, funcionarios, recursos, actividades, calendarización, "mis reservas") a partir de plantillas JasperReports (`.jrxml`), abiertos con el visor nativo del sistema operativo.

### Agregadas en el Proyecto 2

- **Arquitectura distribuida en dos procesos**: el Backend corre sin interfaz gráfica escuchando en un puerto, y uno o varios Frontend se conectan a él por la red. El Frontend no tiene ninguna referencia a la base de datos.
- **Gestión de solicitudes de reserva**: toda reserva nace *En revisión* y es un administrador quien la aprueba, eligiendo el recurso concreto de cada categoría requerida, o la rechaza indicando un motivo obligatorio. Las solicitudes pendientes se revisan desde un sidebar permanente, con contador de nuevas y marca de "vista / no vista" independiente del estado.
- **Ciclo de vida completo de la reserva**: cuatro estados (*En revisión*, *Aceptada*, *Rechazada*, *Cancelada*). Editar una reserva la devuelve a revisión; cancelarla libera sus recursos sin borrarla del historial.
- **Área de alertas en tiempo real**: cuando un usuario registra una reserva, el Backend se lo avisa a todos los demás Frontend conectados, que la muestran de inmediato en su área de alertas sin necesidad de refrescar. Las alertas se pueden ver en detalle y descartar.
- **Persistencia en base de datos Oracle**: las seis tablas del esquema reemplazan al archivo XML, con escritura transaccional en cada operación.
- **Pruebas automatizadas**: 133 pruebas JUnit Jupiter, de unidad y de integración (ver la sección *Pruebas*).

## Arquitectura

### Proyecto 1 — MVC por capas en un solo proceso

El proyecto sigue el patrón **Modelo-Vista-Controlador (MVC)** por capas, según lo exige el enunciado del curso:

- **`View`** — pantallas y componentes Swing (login, CRUD, reservas, calendario, estadísticas). No contiene lógica de negocio.
- **`Controller`** — listeners de eventos de cada vista; traducen la interacción del usuario en llamadas a la capa `Service`, validan formato de datos de entrada y muestran errores/confirmaciones. `SessionContext` centraliza el estado de la sesión (usuario autenticado, acceso a los distintos `Service`).
- **`Service`** — lógica de dominio/orquestación: `ReservationService`, `UserService`, `ResourceService`, y la integración con IA (`AIReservationExtractionService`). Es la única capa responsable de validar reglas de negocio (disponibilidad, existencia de categorías, fechas) antes de persistir.
- **`Model`** — entidades del dominio (`Employee`, `Admin`, `Reservation`, `Resource`, `ResourceCategory`, etc.) y las excepciones de negocio en `Model/exceptions` (`CategoryNotFoundException`, `EmployeeNotFoundException`, `ResourceUnavailableException`, etc.), todas extendiendo `ModelException`.
- **`Persistence`** — lectura/escritura del archivo XML que almacena el estado del sistema (`SystemXmlRepository`, `SystemData`).
- **`Report`** — capa de generación de reportes PDF: `ReportService` compila y llena las plantillas `.jrxml` con JasperReports a partir de DTOs "row" (`CategoryReportRow`, `EmployeeReportRow`, `ReservationReportRow`, `ResourceReportRow`, `ScheduleReportRow`).

Las seis capas vivían en un mismo proceso y se llamaban entre sí por referencias directas de Java.

### Proyecto 2 — Arquitectura distribuida en tres módulos Maven

Las mismas capas se repartieron entre tres módulos, de modo que la separación entre Frontend y Backend sea una propiedad verificable del proyecto y no solo una intención:

| Módulo | Contiene | Depende de |
|---|---|---|
| **`common`** | `Model` y sus excepciones, los DTOs, el paquete `Protocol` y la interfaz `IService` | nada |
| **`backend`** | capas Lógica y Datos: implementación de `IService`, `Persistence` (DAOs JDBC) y `Server` (sockets e hilos) | `common` |
| **`frontend`** | capa de Presentación (MVC: `View`, `Controller`), `Report` y `Network` (el Proxy) | `common` |

El Frontend y el Backend son dos procesos independientes:

- **`IService` (en `common`) es el único contrato entre los dos.** Declara las 32 operaciones del sistema. Lo implementan dos clases distintas: `LocalService` en el Backend, que ejecuta la operación de verdad, y `ServiceProxy` en el Frontend, que la manda por el socket. Ninguna otra parte del Frontend sabe que hay una red de por medio.
- **`Protocol` (en `common`) define lo que viaja por el cable.** `Request` (nombre de la operación y sus argumentos), `Response` (resultado o mensaje de error), `Notification` (aviso que el Backend empuja al Frontend), `Hello` (primer mensaje de cada conexión) y `MessageCodec`, que serializa cada mensaje y lo envía con un prefijo de longitud.
- **`Server` (en `backend`) atiende a cada cliente en su propio hilo.** `ReservationServer` acepta conexiones y le da un hilo a cada una; `ClientHandler` lee la petición, busca la operación en `IService` por reflexión y la invoca sobre el servicio real, dentro de un candado único que protege el estado compartido; `ClientRegistry` lleva las sesiones abiertas y sus canales de notificación.
- **`Network` (en `frontend`) es el lado cliente del Proxy.** `ServiceProxy` traduce cada llamada de `IService` en un `Request`, espera su `Response` y convierte el error que vino por la red en una `ServiceException` con el mismo mensaje que vería el usuario si todo corriera localmente. `NotificationListener` escucha en un hilo aparte los avisos que llegan del Backend.

Cada cliente mantiene **dos conexiones**: un canal de peticiones, que el Frontend abre y por el que pregunta, y un canal de notificaciones, por el que el Backend le avisa sin que haya preguntado. Ambos se correlacionan con el id de sesión que el servidor entrega al saludar.

```
   Máquina A                                      Máquina B
┌──────────────────────┐                     ┌──────────────────────┐
│  Frontend (Swing)    │   peticiones  ────► │  Backend (sin GUI)   │
│  View / Controller   │                     │  Server / Service    │
│  Report              │ ◄──── respuestas    │  Persistence         │
│  Network.ServiceProxy│                     │  ReservationServer   │
│                      │ ◄──── alertas       │                      │
└──────────────────────┘                     └──────────┬───────────┘
          (uno o varios Frontend conectados)            │ JDBC
                                                        ▼
                                              ┌──────────────────────┐
                                              │  Oracle Database     │
                                              └──────────────────────┘
```

## Estructura de carpetas

```
P3_Proyecto1/
├── pom.xml                              # POM padre: módulos, versiones y plugins de prueba
├── db/
│   ├── schema.sql                       # Creación de las seis tablas (Oracle)
│   └── database.properties.example      # Plantilla de la configuración de conexión
│
├── common/                              # Lenguaje común entre Frontend y Backend
│   ├── pom.xml
│   └── src/
│       ├── main/java/
│       │   ├── Model/                   # Entidades del dominio
│       │   │   └── exceptions/          # Excepciones de negocio (extienden ModelException)
│       │   ├── Dto/                     # ReservationSummary, ResourceSlot, StatRow, PendingRequest
│       │   ├── Protocol/                # Request, Response, Notification, Hello, MessageCodec
│       │   └── Service/                 # IService (contrato único) y ServiceException
│       └── test/java/
│           ├── Model/                   # Pruebas unitarias del dominio
│           └── Protocol/                # Pruebas unitarias de serialización
│
├── backend/                             # Capas Lógica y Datos. Proceso independiente, sin GUI
│   ├── pom.xml
│   └── src/
│       ├── main/java/
│       │   ├── Server/                  # ReservationServer, ClientHandler, ClientRegistry,
│       │   │                            #   PushChannel y ServerMain (punto de entrada)
│       │   ├── Service/                 # LocalService (implementa IService), los Service de
│       │   │                            #   dominio y la integración con IA
│       │   ├── Persistence/             # ConnectionManager, los cuatro DAOs,
│       │   │                            #   SystemOracleRepository y el repositorio XML heredado
│       │   └── ImportarXmlAOracle.java  # Utilitario de migración del XML a la base de datos
│       └── test/java/
│           ├── Persistence/             # Pruebas de integración contra Oracle
│           └── Server/                  # Prueba de integración del servidor por socket
│
├── frontend/                            # Capa de Presentación (MVC) y reportes PDF
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   ├── Main.java            # Punto de entrada del Frontend
│       │   │   ├── Network/             # ServiceProxy, NotificationListener, ConnectionSettings
│       │   │   ├── Controller/          # Listeners de cada vista, SessionContext y AlertCenter
│       │   │   ├── View/                # Pantallas y componentes Swing
│       │   │   └── Report/              # Generación de reportes PDF con JasperReports
│       │   └── resources/
│       │       ├── Resources/Icons/     # Íconos de la interfaz (.png)
│       │       └── ReportDesign/        # Plantillas de reportes (.jrxml)
│       └── test/java/
│           └── Network/                 # Prueba de integración del Proxy
│
└── src/Data/
    └── system.xml                       # Datos del Proyecto 1. Hoy es respaldo y fuente
                                         #   del importador a la base de datos
```

> Nota: `src/Data/system.xml` se mantiene fuera de `src/main/resources` a propósito. En el Proyecto 1 era el archivo de datos reales de la app en ejecución (no un recurso empaquetado) y se cargaba con una ruta relativa al directorio de trabajo del proceso. En el Proyecto 2 dejó de ser la fuente de verdad, pero se conservó: es el respaldo de los datos históricos y lo que lee `ImportarXmlAOracle` para cargarlos en Oracle.

## Tecnologías y dependencias

- **Java 20** (`maven.compiler.source`/`target` en el POM padre)
- **Maven** como gestor de dependencias y build, con un **reactor de tres módulos** (`pom` padre + `common`, `backend`, `frontend`)
- **Swing** para la interfaz gráfica de escritorio
- **Sockets TCP e hilos del JDK** (`java.net.ServerSocket`, `java.net.Socket`, `java.lang.Thread`, `java.util.concurrent`) para la comunicación entre procesos — sin librerías de terceros
- **Serialización de Java** (`java.io.Serializable`, `ObjectOutputStream`/`ObjectInputStream`) para transportar las entidades del dominio y los mensajes del protocolo
- **Oracle Database 11g Express Edition** como motor de base de datos
- **Driver JDBC de Oracle 23.26.0.0.0** (`com.oracle.database.jdbc:ojdbc11`) — declarado **únicamente** en el módulo `backend`
- **JasperReports 6.20.6** (`net.sf.jasperreports:jasperreports`) — generación de reportes PDF a partir de plantillas `.jrxml` diseñadas en Jaspersoft Studio. Declarado únicamente en `frontend`
- **org.json 20251224** — construcción/parseo de JSON para la llamada a la API de IA. Declarado únicamente en `backend`
- **API de Groq** — endpoint `chat/completions` (compatible con el formato de OpenAI), usada por `AIReservationExtractionService` para interpretar la frase en lenguaje natural del funcionario
- **JUnit Jupiter 5.11.0** + **Maven Surefire Plugin 3.2.5** (pruebas unitarias `*Test.java`) + **Maven Failsafe Plugin 3.2.5** (pruebas de integración `*IT.java`), según lo exige el enunciado del curso

Las versiones se fijan en un solo lugar, el `dependencyManagement` del POM padre; cada módulo declara solo el `groupId`/`artifactId` de lo que realmente usa:

| Módulo | Dependencias declaradas |
|---|---|
| `common` | ninguna (solo JUnit, heredado del padre) |
| `backend` | `common`, `org.json`, `ojdbc11` |
| `frontend` | `common`, `jasperreports` |

Que el Frontend no pueda tocar la base de datos es comprobable:

```bash
mvn -pl frontend dependency:tree
```

No debe mencionar ningún driver JDBC.

## Requisitos previos

- JDK 20 instalado
- Maven (viene empaquetado con IntelliJ IDEA; no requiere instalación aparte)
- **Oracle Database 11g Express Edition** (o superior) instalado y en ejecución, accesible desde la máquina que corra el Backend. El driver JDBC no requiere instalar nada: es una dependencia de Maven y viaja con el proyecto
- Una API key de Groq, generada en `console.groq.com` (tier gratuito real, sin tarjeta ni facturación). Solo hace falta en la máquina que corra el **Backend**
- Para probar con Frontend y Backend en máquinas distintas: ambas en la misma red, y el puerto del Backend (5000 por omisión) abierto en el firewall de la máquina servidora

## Configuración

1. Clonar el repositorio y abrir la carpeta del proyecto en IntelliJ apuntando directamente al `pom.xml` **de la raíz** (File → Open → seleccionar `pom.xml` → "Open as Project"). IntelliJ reconoce los tres módulos a partir de él.

2. **Configurar la conexión a la base de datos.** Copiar `db/database.properties.example` a `db/database.properties` y completar los datos de la instancia local:

   ```properties
   db.host=localhost
   db.port=1521
   db.service=XE
   db.user=system
   db.password=TU_CLAVE
   ```

   `db/database.properties` está en el `.gitignore` porque contiene la clave, así que **no viene con el clon**: hay que crearlo en cada máquina. Si se prefiere tenerlo en otra ruta, se puede indicar con la variable de entorno `RESERVAS_DB_CONFIG`.

3. **Crear el esquema.** Ejecutar `db/schema.sql` contra esa instancia, desde SQL Developer o desde `sqlplus`. El script borra y recrea las seis tablas, así que también sirve para reiniciar la base desde cero.

4. **Cargar los datos de ejemplo (opcional).** Ejecutar la clase `ImportarXmlAOracle` del módulo `backend` con el argumento `--reemplazar` para migrar a Oracle el contenido de `src/Data/system.xml`. Sin el argumento solo imprime un reporte de lo que haría, sin escribir nada. Si se omite este paso, el Backend detecta la base vacía al arrancar y crea un administrador de prueba, imprimiendo sus credenciales en consola.

5. **Configurar la API key de la IA.** Definir la variable de entorno `GROQ_API_KEY` en la máquina del Backend:

   ```powershell
   [Environment]::SetEnvironmentVariable('GROQ_API_KEY', 'tu-clave-aqui', 'User')
   ```

   También puede definirse en IntelliJ: Run/Debug Configurations → Modify options → Environment variables. Sin esta variable el resto del sistema funciona con normalidad; solo el botón "Generar con IA" mostrará un error controlado.

## Compilación y ejecución

La compilación se hace **siempre desde el módulo raíz**, nunca desde un módulo individual, porque `backend` y `frontend` necesitan el artefacto de `common` instalado:

```bash
mvn clean install    # Compila los tres módulos y corre las pruebas unitarias
mvn test             # Solo las pruebas unitarias (Surefire)
mvn verify           # Pruebas unitarias + de integración (Surefire + Failsafe)
```

En IntelliJ, equivale a usar el Lifecycle de Maven del módulo `sistema-reservas` (el padre), no el de un submódulo.

Para correr el sistema hacen falta **dos procesos, en este orden**:

1. **El Backend.** Ejecutar `Server.ServerMain` del módulo `backend`. Escucha en el puerto 5000 por omisión; se puede cambiar pasándolo como argumento (`ServerMain 6000`) o con la variable de entorno `RESERVAS_PORT`. Arranca sin interfaz gráfica e imprime en consola la URL de la base y el puerto en que quedó escuchando.

2. **El Frontend.** Ejecutar `Main` del módulo `frontend`. Sin argumentos se conecta a `localhost:5000`; para conectarse a otra máquina se le pasan el host y el puerto (`Main 192.168.1.50 5000`).

Se pueden abrir varios Frontend a la vez, en la misma máquina o en máquinas distintas, contra un mismo Backend. Es la forma de ver el área de alertas funcionando: al registrar una reserva desde uno, el aviso aparece en los demás.

## Pruebas

El proyecto incluye **133 pruebas** con JUnit Jupiter, divididas como exige el enunciado entre pruebas de unidad (Surefire) y de integración (Failsafe). La convención de nombres es lo que decide qué plugin corre qué: `*Test.java` lo toma Surefire, `*IT.java` lo toma Failsafe.

| Clase | Módulo | Plugin | Qué cubre | Pruebas |
|---|---|---|---|---|
| `ReservationTest` | `common` | Surefire | Disponibilidad de recursos y flujo de estados | 31 |
| `ResourceTest` | `common` | Surefire | Identidad compuesta (categoría, id) de un recurso | 8 |
| `EmployeeTest` | `common` | Surefire | Solicitudes de reserva y estabilidad del id | 13 |
| `UserContainerTest` | `common` | Surefire | Generación de ids y búsqueda de usuarios | 10 |
| `CategoryContainerTest` | `common` | Surefire | Catálogo de categorías y recursos | 12 |
| `MessageCodecTest` | `common` | Surefire | Serialización de los mensajes y del dominio | 15 |
| `SystemOracleRepositoryIT` | `backend` | Failsafe | Camino de lectura completo contra Oracle | 6 |
| `OracleDaoWriteIT` | `backend` | Failsafe | Escritura de los cuatro DAOs contra Oracle | 12 |
| `ReservationServerIT` | `backend` | Failsafe | El servidor real por un socket real | 13 |
| `ServiceProxyIT` | `frontend` | Failsafe | El Proxy real por un socket real | 13 |

Dos detalles de diseño de las pruebas:

- **Las pruebas de integración contra Oracle se omiten, no fallan, cuando no hay base de datos.** Como `db/database.properties` no está en el repositorio, en una máquina recién clonada no existe. Cada prueba empieza comprobando que la base responda; si no, JUnit la reporta como omitida y dice por qué, de modo que `mvn verify` nunca se rompe por una conexión sin configurar.
- **Ninguna prueba deja datos en la base.** Cada una corre dentro de una transacción que se revierte al terminar, salga bien o mal, así que lo escrito es visible para la propia prueba pero nunca se confirma. Una de las pruebas de `OracleDaoWriteIT` verifica justamente eso: cuenta los registros antes, escribe, comprueba que dentro de la transacción ya se ven, y confirma que después del rollback el total volvió al original.
- **Las pruebas de sockets no necesitan base de datos.** Levantan el servidor real contra un doble de `IService` construido con un proxy dinámico, así que miden la red y no la persistencia.

## Persistencia de datos

### Proyecto 1 — Archivo XML

Todo el estado (usuarios, categorías, recursos, reservas) se guardaba en el archivo XML `src/Data/system.xml`, leído y escrito por `Persistence/SystemXmlRepository`. Si el archivo no existía o estaba vacío, la aplicación sembraba datos de prueba al iniciar.

### Proyecto 2 — Base de datos Oracle

La persistencia pasó a una base de datos relacional, con **seis tablas** creadas por `db/schema.sql`:

| Tabla | Contenido |
|---|---|
| `USUARIO` | administradores y funcionarios, con su tipo, clave, nombre y teléfono |
| `CATEGORIA` | categorías de recursos (`CAT-000001`, …) |
| `RECURSO` | recursos, con llave primaria compuesta `(categoria_id, id)` |
| `RESERVA` | reservas, con su funcionario, actividad, fecha, horario, estado, motivo de rechazo y las marcas de la notificación |
| `RESERVA_CATEGORIA` | tabla puente: categorías que requiere cada reserva |
| `RESERVA_RECURSO` | tabla puente: recursos asignados a cada reserva |

El acceso se hace con una **DAO por entidad** (`UserDAO`, `CategoryDAO`, `ResourceDAO`, `ReservationDAO`), coordinadas por `SystemOracleRepository`. `ConnectionManager` es el único punto del proyecto que conoce la URL, el usuario y la clave, y los lee del `db/database.properties` que no está versionado.

El XML no se eliminó. Dejó de ser la fuente de verdad, pero se conserva como respaldo de los datos históricos del Proyecto 1 y como fuente del utilitario `ImportarXmlAOracle`, que los migra a Oracle en una sola transacción.

## Decisiones de diseño más importantes

### Proyecto 1

- **Migración de proyecto plano de IntelliJ a Maven.** El proyecto arrancó sin `pom.xml`, con un solo `.iml` y `src/` sin separar `main`/`test`. Se migró a una estructura Maven estándar antes de implementar los reportes PDF, por dos razones: Maven gestiona automáticamente JasperReports y sus ~20 librerías dependientes (evita agregar JARs a mano, algo frágil y no reproducible entre las tres computadoras del equipo), y el enunciado del curso exige explícitamente **Surefire Plugin** y **Failsafe Plugin** para las pruebas, que solo existen en un proyecto Maven. La migración se aplicó dentro del repositorio real (`P3_Proyecto1`, con su historial de git y remoto de GitHub) en vez de partir de cero en un proyecto Maven nuevo, para no perder ese historial.

- **Capa `Service` como única responsable de las reglas de negocio.** La lógica de armar una reserva (asignar el primer recurso disponible por categoría, todo-o-nada) se movió del `Controller` al `Service`/`Model` (`ReservationService`, `Employee.tryBook`), siguiendo el mismo patrón que ya usaban `UserService`/`ResourceService` (reciben una instancia compartida de `Service` por constructor, expuesta vía `SessionContext`). El objetivo es que el `Controller` solo valide formato de entrada y traduzca la interacción del usuario, y que la validación de disponibilidad, existencia de recursos y reglas de negocio viva en un solo lugar — incluido el flujo de autocompletado por IA, que reutiliza el mismo `ReservationService.createReservation` que el flujo manual.

- **La IA solo interpreta texto; nunca valida ni decide.** El autocompletado por lenguaje natural se diseñó para que el LLM únicamente extraiga datos (actividad, fecha, horas, categorías) en un XML de esquema fijo, dejando vacío lo que no esté claro y sin inventar valores. La disponibilidad de recursos y la existencia de categorías las sigue validando exclusivamente la capa Service/Model, igual que en el flujo manual — así un error o alucinación del modelo de IA nunca puede crear una reserva inválida.

- **Groq como proveedor de IA, tras descartar Gemini.** El enunciado exige un modelo de lenguaje gratuito. Se intentó primero con **Gemini** (Google), que sí tiene tier gratuito real, pero la cuenta quedó bloqueada con un 403 `permission_denied` (un problema conocido de Google en cuentas nuevas sin facturación) que no se pudo resolver a tiempo. Se optó entonces por **Groq** (`groq.com`), que sí ofrece un tier gratuito real sin tarjeta ni facturación para modelos de texto. Al ser Groq compatible con el mismo formato de mensajes estilo OpenAI que ya se había implementado, el cambio de proveedor quedó aislado a `AIReservationExtractionService`, sin tocar el resto de la arquitectura.

- **`java.net.http.HttpClient` y `org.json` en vez de un SDK de terceros para la IA.** Para la llamada a la API del LLM se usó el cliente HTTP estándar del JDK y la librería `org.json` (ya liviana) en vez de agregar un SDK propietario del proveedor, precisamente para que un futuro cambio de proveedor (como el de Gemini a Groq) quedara aislado a una sola clase y no arrastrara dependencias nuevas cada vez.

- **Prevenir en los selectores en vez de avisar después de fallar.** Siguiendo una preferencia general del equipo de "prevenir en vez de avisar", los selectores de fecha deshabilitan directamente los días pasados (`min = LocalDate.now()` en `DatePickerDialog`) en vez de permitir seleccionarlos y solo mostrar el error al intentar guardar la reserva.

### Proyecto 2

- **Las reservas pasan por la revisión de un administrador antes de ocupar recursos.** Al registrar una reserva se conserva lo que pide el enunciado: el sistema verifica que cada categoría requerida tenga al menos un recurso libre y, si alguna no lo tiene, no guarda nada e informa cuáles fallaron. La diferencia es que la reserva ya no toma automáticamente el primer recurso disponible: queda *En revisión* y es el administrador quien la aprueba, eligiendo el recurso concreto de cada categoría, o la rechaza indicando un motivo obligatorio que el funcionario consulta desde "Mis reservas". Se decidió así porque elegir qué sala, laptop o proyector específico se usa es una decisión de gestión que el sistema no puede tomar bien por sí solo — el "primer disponible" solo refleja el orden en que se registraron los recursos. El ciclo de vida quedó explícito en cuatro estados (`Model.State`) con las transiciones dentro de `Reservation` (`approve`, `reject`, `cancel`, `returnToReview`, `markRequestSeen`), que rechaza cualquier cambio inválido con `InvalidReservationOperationException`; `ReservationService` solo valida la entrada, traduce esas excepciones a un mensaje para el usuario y persiste, así que las reglas del flujo viven en un solo lugar sin importar desde qué pantalla se dispare la acción. Solo las reservas *Aceptadas* ocupan recursos y aparecen en Calendarización, Actividades y Estadísticas: una solicitud pendiente o rechazada no debe bloquear recursos ni figurar como una actividad que tal vez nunca ocurra. La disponibilidad se verifica dos veces, al crear y otra vez al aprobar, porque entre ambos momentos pueden aprobarse otras solicitudes sobre los mismos recursos y puede haber varios Frontend revisando a la vez; el formulario de revisión ofrece solo los recursos que siguen libres, y el Backend vuelve a validar la asignación con sus objetos reales y no con las copias que llegaron por la red. Editar una reserva la devuelve a *En revisión* y libera sus recursos, lo que permite corregir una rechazada sin crear otra; cancelarla la deja en estado *Cancelada* en vez de borrarla, para conservar el historial. Aparte del estado, cada solicitud lleva su propia marca de vista / no vista (`RequestNotification`, columnas `recibida_en` y `vista_en`), que distingue "ya la vi" de "ya la decidí", ordena la lista y, al estar en la base de datos, es la misma para todos los administradores entre sesiones.

- **Sidebar permanente con pestañas separadas para alertas y solicitudes.** Las solicitudes se muestran en un sidebar fijo a la derecha de la ventana del administrador, y no en una pestaña propia, para que pueda ver las pendientes y el contador de nuevas mientras trabaja en cualquier otra pantalla. El área de alertas que exige el Proyecto 2 ocupa ese mismo lado, así que el sidebar se dividió en dos pestañas en lugar de partir el espacio vertical y ver ambas comprimidas. Son conceptos distintos: las alertas son solo informativas, viven en la sesión actual y descartarlas no envía nada al Backend, mientras que las solicitudes persisten y exigen una acción. El funcionario solo tiene el área de alertas.

- **Tres módulos Maven para que la separación entre Frontend y Backend sea verificable.** El enunciado exige que la capa de presentación no acceda a los datos. Repartir el código en `common`, `backend` y `frontend` convierte esa exigencia en algo que se comprueba con un comando (`mvn -pl frontend dependency:tree` no menciona ningún driver JDBC) en vez de depender de la disciplina de no escribir el import equivocado. La regla que se mantuvo es que `common` no declara ninguna dependencia: si alguna vez la necesitara, es señal de que lo que se le quiso meter pertenece al Backend o al Frontend.

- **`IService` como único contrato, sin sobrecargas.** Toda la comunicación pasa por esa interfaz, que implementan tanto el servicio real del Backend como el Proxy del Frontend. Se decidió que no declare dos operaciones con el mismo nombre, porque el servidor despacha las peticiones buscando el método por su nombre: así el nombre identifica la operación sin ambigüedad y el protocolo no necesita cargar las firmas. El servidor verifica esa regla al arrancar y falla de inmediato si alguien agrega una sobrecarga.

- **Identidad por id en lugar de comparación por referencia.** En el Proyecto 1 las entidades se comparaban con `==`, que funciona mientras todo corre en un mismo proceso. En cuanto un objeto viaja por un socket, lo que llega al otro lado es una copia y la comparación por referencia deja de servir. Se le agregó un id a `Reservation` y se reescribieron `equals`/`hashCode` de las entidades para que la identidad sea el id. El id de una reserva es inmutable en la práctica: editarla, cancelarla o volver a solicitarla modifica el mismo objeto en vez de reemplazarlo, así que conserva su id toda la vida. En el caso de `Resource` la identidad es la **pareja (categoría, id)**, porque el id solo es único dentro de su categoría: antes se comparaba `(id, descripción)` sin la categoría, y eso hacía que reservar la sala 1 bloqueara el proyector 1.

- **Un `ObjectOutputStream` nuevo por mensaje, en vez de confiar en `reset()`.** `ObjectOutputStream` cachea por identidad los objetos que ya envió y la segunda vez manda solo una referencia, de modo que el cliente seguiría viendo el estado viejo de un objeto que el servidor ya modificó. La solución habitual es llamar a `reset()` después de cada escritura, pero basta olvidarlo una vez para introducir un error difícil de encontrar. `MessageCodec` serializa cada mensaje con un stream nuevo sobre un arreglo de bytes y lo envía con un prefijo de longitud: el caché nunca se acumula, y si un objeto no se puede serializar el error ocurre **antes** de escribir nada en el socket, así que la conexión no queda a medias. Cada clase que viaja declara además un `serialVersionUID` fijo, para que agregarle un campo no haga que el otro extremo rechace el objeto.

- **El error viaja como texto, no como excepción.** Sería tentador serializar la `ServiceException` completa y relanzarla en el cliente, pero una excepción arrastra su causa, y la causa suele ser una clase que solo existe en el Backend (un error de JDBC o de la API de IA). Al llegar al Frontend, que no tiene esas clases en su classpath, la deserialización fallaría con `ClassNotFoundException` y el usuario vería un error de infraestructura en vez del mensaje real. Mandando solo el texto, el Proxy reconstruye una excepción limpia con ese mensaje y el Frontend no necesita conocer ni una clase del Backend. Un error inesperado (lo que no es un error de negocio) se registra en el log del servidor y al cliente solo le llega un aviso genérico.

- **Dos canales por cliente y un hilo por conexión.** Una petición siempre nace en el Frontend y el Backend contesta; una alerta nace en el Backend, cuando algo cambió por acción de otro usuario, y puede llegar en cualquier momento. Mezclar ambos sentidos en una sola conexión obligaría a distinguir en el cliente si lo que llegó es la respuesta que estaba esperando o un aviso. Por eso cada cliente abre dos conexiones, correlacionadas por el id de sesión que el servidor entrega al saludar: una para peticiones y otra solo para recibir avisos. El servidor atiende cada conexión en su propio hilo, protege el estado compartido con un único candado, y hace el envío de las alertas **fuera** de ese candado, para que escribir en los sockets de los demás clientes no frene a los otros hilos. Las operaciones que no tocan datos compartidos (la llamada a la IA) quedan exentas del candado para no bloquear a nadie durante los segundos que tarda.

- **Oracle en lugar de MySQL.** El enunciado indica MySQL, pero el curso se trabaja sobre Oracle, así que se implementó sobre **Oracle Database 11g Express Edition**. Eso impuso algunas restricciones que están presentes en el esquema: 11g no tiene columnas `IDENTITY` (llegaron en 12c), así que los ids se siguen generando en Java y el repositorio retoma los contadores desde el mayor id en uso; tampoco admite `DROP TABLE IF EXISTS`, por lo que el script borra las tablas con un bloque PL/SQL que ignora el error de "tabla inexistente".

- **Carga completa en memoria al arrancar, con escritura inmediata.** El Backend lee la base entera al iniciar y mantiene los contenedores del dominio en memoria; cada operación que modifica datos los escribe de inmediato en la base, dentro de su propia transacción. Se eligió esto en vez de consultar la base en cada lectura porque las reglas de negocio (`Reservation.isResourceFree`, `Employee.tryBook`) operan sobre los contenedores en memoria, y reescribirlas como consultas SQL habría sido un cambio de alcance mucho mayor. La defensa honesta de esta decisión es que funciona **porque hay exactamente un proceso Backend**: es él quien tiene la verdad en memoria y el único que escribe. Con dos instancias del servidor contra la misma base, los contenedores se desincronizarían.

- **Una conexión JDBC por operación.** `ConnectionManager.open()` devuelve una conexión nueva cada vez, con `autoCommit` apagado, y quien la pide hace `commit` al terminar bien o `rollback` si algo falló. No se comparte una sola conexión entre hilos porque `java.sql.Connection` **no es thread-safe** y el servidor atiende a cada cliente en su propio hilo. Para la escala de este sistema el costo de abrir una conexión es irrelevante, y así se evita por completo la clase de errores que aparece cuando dos hilos comparten una.

- **El XML se conserva como respaldo en vez de eliminarse.** Al migrar a base de datos lo natural habría sido borrar `src/Data/system.xml` y el repositorio que lo lee. Se mantuvieron a propósito: el XML es el respaldo de los datos del Proyecto 1 y la fuente de `ImportarXmlAOracle`, que los carga en Oracle en una sola transacción. Durante el desarrollo esa decisión ya sirvió una vez, al recuperar los datos después de recrear el esquema.

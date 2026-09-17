# Sistema de Gestión y Reserva de Recursos

Proyecto académico para el curso **EIF206 - Programación III**, Universidad Nacional de Costa Rica, Facultad de Ciencias Exactas y Naturales, Escuela de Informática.

Sistema de escritorio en Java (Swing) para que funcionarios y administradores de una organización gestionen usuarios, categorías y recursos, registren y consulten reservas, visualicen la calendarización de actividades/recursos y generen estadísticas y reportes. Incluye una funcionalidad de Inteligencia Artificial que permite registrar una reserva describiéndola en lenguaje natural, y generación de reportes en PDF.

## Integrantes y roles

| Integrante | Rol dentro del proyecto |
|---|---|
| **Daniel Centeno Gutierrez** | Funcionalidades de la capa Vista, capa Controlador, generación de reportes PDF y autocompletado de reservas por IA |
| **Juan Pablo Arroyo Zumbado** | Lógica interna de la capa Modelo y manejo de excepciones, capa Controlador y autocompletado de reservas por IA |

## Funcionalidades implementadas

- **Autenticación**: inicio de sesión y cambio de clave, para roles de Administrador y Funcionario.
- **Gestión de funcionarios, categorías y recursos** (Administrador): alta, edición, borrado y filtrado/búsqueda en tiempo real (CRUD) sobre cada entidad.
- **Reservas** (Funcionario): registro de reservas asignando recursos por categoría (todo-o-nada: si alguna categoría requerida no tiene disponibilidad, no se guarda nada de la reserva), consulta de "Mis reservas" y cancelación.
- **Autocompletado de reservas por IA**: un campo de "Frase" en el formulario de reservas donde el funcionario describe la reserva en lenguaje natural (ej. "Necesito la sala de reuniones mañana de 2 a 4pm"); el sistema invoca un LLM (API de Groq) que extrae actividad, fecha, horas y categorías y llena el formulario, que el usuario puede revisar/editar antes de guardar. La IA solo interpreta texto: la validación de disponibilidad y existencia de recursos la sigue haciendo la capa Service/Model, igual que en el flujo manual.
- **Calendarización**: vista de calendario de actividades y de recursos, para Administrador y Funcionario.
- **Estadísticas**: paneles de conteo por categoría de recurso y por semana, filtrables por rango de fechas, con gráfico de barras.
- **Reportes en PDF**: generación e impresión de reportes (categorías, funcionarios, recursos, actividades, calendarización, "mis reservas") a partir de plantillas JasperReports (`.jrxml`), abiertos con el visor nativo del sistema operativo.

## Arquitectura

El proyecto sigue el patrón **Modelo-Vista-Controlador (MVC)** por capas, según lo exige el enunciado del curso:

- **`View`** — pantallas y componentes Swing (login, CRUD, reservas, calendario, estadísticas). No contiene lógica de negocio.
- **`Controller`** — listeners de eventos de cada vista; traducen la interacción del usuario en llamadas a la capa `Service`, validan formato de datos de entrada y muestran errores/confirmaciones. `SessionContext` centraliza el estado de la sesión (usuario autenticado, acceso a los distintos `Service`).
- **`Service`** — lógica de dominio/orquestación: `ReservationService`, `UserService`, `ResourceService`, y la integración con IA (`AIReservationExtractionService`). Es la única capa responsable de validar reglas de negocio (disponibilidad, existencia de categorías, fechas) antes de persistir.
- **`Model`** — entidades del dominio (`Employee`, `Admin`, `Reservation`, `Resource`, `ResourceCategory`, etc.) y las excepciones de negocio en `Model/exceptions` (`CategoryNotFoundException`, `EmployeeNotFoundException`, `ResourceUnavailableException`, etc.), todas extendiendo `ModelException`.
- **`Persistence`** — lectura/escritura del archivo XML que almacena el estado del sistema (`SystemXmlRepository`, `SystemData`).
- **`Report`** — capa de generación de reportes PDF: `ReportService` compila y llena las plantillas `.jrxml` con JasperReports a partir de DTOs "row" (`CategoryReportRow`, `EmployeeReportRow`, `ReservationReportRow`, `ResourceReportRow`, `ScheduleReportRow`).

## Estructura de carpetas

```
P3_Proyecto1/
├── pom.xml                          # Configuración de Maven (dependencias, plugins de test)
├── src/
│   ├── Data/
│   │   └── system.xml               # Datos reales de la aplicación (usuarios, categorías, recursos, reservas)
│   └── main/
│       ├── java/
│       │   ├── Main.java            # Punto de entrada de la aplicación
│       │   ├── Controller/          # Listeners de eventos de cada vista + SessionContext
│       │   ├── Model/               # Entidades del dominio
│       │   │   └── exceptions/      # Excepciones de negocio (extienden ModelException)
│       │   ├── Persistence/         # Lectura/escritura del XML de datos
│       │   ├── Service/             # Lógica de negocio y orquestación (incluye IA)
│       │   ├── Report/              # Generación de reportes PDF con JasperReports
│       │   └── View/                # Pantallas y componentes Swing
│       └── resources/
│           ├── Resources/Icons/     # Íconos de la interfaz (.png)
│           └── ReportDesign/        # Plantillas de reportes (.jrxml)
└── target/                          # Salida de compilación de Maven (generada, no versionada)
```

> Nota: `src/Data/system.xml` se mantiene fuera de `src/main/resources` a propósito — es el archivo de datos reales de la app en ejecución (no un recurso empaquetado), y `SessionContext` lo carga con una ruta relativa al directorio de trabajo del proceso.

## Tecnologías y dependencias

- **Java 20** (`maven.compiler.source`/`target`)
- **Maven** como gestor de dependencias y build (`pom.xml`)
- **Swing** para la interfaz gráfica de escritorio
- **JasperReports 6.20.6** (`net.sf.jasperreports:jasperreports`) — generación de reportes PDF a partir de plantillas `.jrxml` diseñadas en Jaspersoft Studio
- **org.json 20251224** — construcción/parseo de JSON para la llamada a la API de IA
- **API de Groq** — endpoint `chat/completions` (compatible con el formato de OpenAI), usada por `AIReservationExtractionService` para interpretar la frase en lenguaje natural del funcionario
- **JUnit Jupiter 5.11.0** + **Maven Surefire Plugin** (pruebas unitarias `*Test.java`) + **Maven Failsafe Plugin** (pruebas de integración `*IT.java`), según lo exige el enunciado del curso

## Requisitos previos

- JDK 20 instalado
- Maven (viene empaquetado con IntelliJ IDEA; no requiere instalación aparte)
- Una API key de Groq, generada en `console.groq.com` (tier gratuito real, sin tarjeta ni facturación)

## Configuración

1. Clonar el repositorio y abrir la carpeta del proyecto en IntelliJ apuntando directamente a su `pom.xml` (File → Open → seleccionar `pom.xml` → "Open as Project").
2. Configurar la variable de entorno `GROQ_API_KEY` con la API key de Groq, necesaria para el autocompletado por IA:
   - En IntelliJ: Run/Debug Configurations → Modify options → Environment variables.
   - Sin esta variable, el resto del sistema (reservas manuales, CRUD, calendario, estadísticas, reportes PDF) funciona con normalidad; solo el botón "Generar con IA" mostrará un error controlado.

## Compilación y ejecución

```bash
mvn compile        # Compila el proyecto
mvn test           # Corre las pruebas unitarias (Surefire)
mvn verify          # Corre pruebas unitarias + de integración (Surefire + Failsafe)
```

Para ejecutar la aplicación, correr el método `main` de `src/main/java/Main.java` desde IntelliJ (▶), o generar el jar con Maven y ejecutarlo con `java -jar`.

## Persistencia de datos

El sistema no usa base de datos: todo el estado (usuarios, categorías, recursos, reservas) se guarda en el archivo XML `src/Data/system.xml`, leído y escrito por `Persistence/SystemXmlRepository`. Si el archivo no existe o está vacío, la aplicación siembra datos de prueba al iniciar (un administrador y un funcionario de ejemplo).

## Decisiones de diseño más importantes

- **Migración de proyecto plano de IntelliJ a Maven.** El proyecto arrancó sin `pom.xml`, con un solo `.iml` y `src/` sin separar `main`/`test`. Se migró a una estructura Maven estándar antes de implementar los reportes PDF, por dos razones: Maven gestiona automáticamente JasperReports y sus ~20 librerías dependientes (evita agregar JARs a mano, algo frágil y no reproducible entre las tres computadoras del equipo), y el enunciado del curso exige explícitamente **Surefire Plugin** y **Failsafe Plugin** para las pruebas, que solo existen en un proyecto Maven. La migración se aplicó dentro del repositorio real (`P3_Proyecto1`, con su historial de git y remoto de GitHub) en vez de partir de cero en un proyecto Maven nuevo, para no perder ese historial.

- **Capa `Service` como única responsable de las reglas de negocio.** La lógica de armar una reserva (asignar el primer recurso disponible por categoría, todo-o-nada) se movió del `Controller` al `Service`/`Model` (`ReservationService`, `Employee.tryBook`), siguiendo el mismo patrón que ya usaban `UserService`/`ResourceService` (reciben una instancia compartida de `Service` por constructor, expuesta vía `SessionContext`). El objetivo es que el `Controller` solo valide formato de entrada y traduzca la interacción del usuario, y que la validación de disponibilidad, existencia de recursos y reglas de negocio viva en un solo lugar — incluido el flujo de autocompletado por IA, que reutiliza el mismo `ReservationService.createReservation` que el flujo manual.

- **La IA solo interpreta texto; nunca valida ni decide.** El autocompletado por lenguaje natural se diseñó para que el LLM únicamente extraiga datos (actividad, fecha, horas, categorías) en un XML de esquema fijo, dejando vacío lo que no esté claro y sin inventar valores. La disponibilidad de recursos y la existencia de categorías las sigue validando exclusivamente la capa Service/Model, igual que en el flujo manual — así un error o alucinación del modelo de IA nunca puede crear una reserva inválida.

- **Groq como proveedor de IA, tras descartar Gemini.** El enunciado exige un modelo de lenguaje gratuito. Se intentó primero con **Gemini** (Google), que sí tiene tier gratuito real, pero la cuenta quedó bloqueada con un 403 `permission_denied` (un problema conocido de Google en cuentas nuevas sin facturación) que no se pudo resolver a tiempo. Se optó entonces por **Groq** (`groq.com`), que sí ofrece un tier gratuito real sin tarjeta ni facturación para modelos de texto. Al ser Groq compatible con el mismo formato de mensajes estilo OpenAI que ya se había implementado, el cambio de proveedor quedó aislado a `AIReservationExtractionService`, sin tocar el resto de la arquitectura.

- **`java.net.http.HttpClient` y `org.json` en vez de un SDK de terceros para la IA.** Para la llamada a la API del LLM se usó el cliente HTTP estándar del JDK y la librería `org.json` (ya liviana) en vez de agregar un SDK propietario del proveedor, precisamente para que un futuro cambio de proveedor (como el de Gemini a Groq) quedara aislado a una sola clase y no arrastrara dependencias nuevas cada vez.

- **Prevenir en los selectores en vez de avisar después de fallar.** Siguiendo una preferencia general del equipo de "prevenir en vez de avisar", los selectores de fecha deshabilitan directamente los días pasados (`min = LocalDate.now()` en `DatePickerDialog`) en vez de permitir seleccionarlos y solo mostrar el error al intentar guardar la reserva.

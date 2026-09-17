# Sistema de Gestión y Reserva de Recursos

Proyecto académico para el curso **EIF206 - Programación III**, Universidad Nacional de Costa Rica, Facultad de Ciencias Exactas y Naturales, Escuela de Informática.

Sistema de escritorio en Java (Swing) para que funcionarios y administradores de una organización gestionen usuarios, categorías y recursos, registren y consulten reservas, visualicen la calendarización de actividades/recursos y generen estadísticas y reportes. Incluye una funcionalidad de Inteligencia Artificial que permite registrar una reserva describiéndola en lenguaje natural, y generación de reportes en PDF.

## Integrantes y roles

| Integrante | Rol dentro del proyecto |
|---|---|
| **Daniel** | Funcionalidades de la capa Vista, capa Controlador, generación de reportes PDF y autocompletado de reservas por IA |
| **Juan Pablo** | Lógica interna de la capa Modelo y manejo de excepciones, capa Controlador y autocompletado de reservas por IA |

## Funcionalidades implementadas

- **Autenticación**: inicio de sesión y cambio de clave, para roles de Administrador y Funcionario.
- **Gestión de funcionarios, categorías y recursos** (Administrador): alta, edición, borrado y filtrado/búsqueda en tiempo real (CRUD) sobre cada entidad.
- **Reservas** (Funcionario): registro de reservas asignando recursos por categoría (todo-o-nada: si alguna categoría requerida no tiene disponibilidad, no se guarda nada de la reserva), consulta de "Mis reservas" y cancelación.
- **Autocompletado de reservas por IA**: un campo de "Frase" en el formulario de reservas donde el funcionario describe la reserva en lenguaje natural (ej. "Necesito la sala de reuniones mañana de 2 a 4pm"); el sistema invoca un LLM (API de Grok/xAI) que extrae actividad, fecha, horas y categorías y llena el formulario, que el usuario puede revisar/editar antes de guardar. La IA solo interpreta texto: la validación de disponibilidad y existencia de recursos la sigue haciendo la capa Service/Model, igual que en el flujo manual.
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
- **API de Grok (xAI)** — endpoint `chat/completions`, usada por `AIReservationExtractionService` para interpretar la frase en lenguaje natural del funcionario
- **JUnit Jupiter 5.11.0** + **Maven Surefire Plugin** (pruebas unitarias `*Test.java`) + **Maven Failsafe Plugin** (pruebas de integración `*IT.java`), según lo exige el enunciado del curso

## Requisitos previos

- JDK 20 instalado
- Maven (viene empaquetado con IntelliJ IDEA; no requiere instalación aparte)
- Una API key de xAI (Grok) en `console.x.ai`, con crédito prepago cargado (Grok no tiene tier gratuito)
- (Opcional, solo para editar las plantillas de reportes) Jaspersoft Studio Community, versión compatible con JasperReports 6.20.6

## Configuración

1. Clonar el repositorio y abrir la carpeta del proyecto en IntelliJ apuntando directamente a su `pom.xml` (File → Open → seleccionar `pom.xml` → "Open as Project").
2. Configurar la variable de entorno `XAI_API_KEY` con la API key de xAI, necesaria para el autocompletado por IA:
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

## Estado del proyecto / pendientes conocidos

- El autocompletado de reservas por IA está implementado y conectado end-to-end, pero tiene errores conocidos aún sin depurar (comportamiento a validar con distintas frases de entrada).
- El proveedor de IA usado actualmente es **Grok (xAI)**; se intentó primero con **Gemini (Google)**, descartado por un bloqueo de cuenta (403 `permission_denied`) que no se pudo resolver a tiempo. El código de la integración con Gemini se conserva documentado por si se retoma más adelante.
- Los reportes en PDF (6 plantillas: categorías, funcionarios, recursos, actividades, calendarización, mis reservas) están completos y probados.

## Historial de ramas de desarrollo

El trabajo se organizó en ramas por área (`View_Features`, `Controller_features`, `Persistence_Features`, `Model_Features`), una rama para la migración de estructura plana de IntelliJ a Maven (`Migracion_Maven`), y ramas de feature para los reportes PDF (`PDF_ReportGenerator`) y el autocompletado por IA (`AI_Autocomplete`, construida sobre `PDF_ReportGenerator`), integradas posteriormente sobre `Model_Features`.

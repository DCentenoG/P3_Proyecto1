# Cómo configurar la clave de la IA (Grok) para correr el proyecto

Este instructivo es para poder probar la funcionalidad de "Generar con IA" en la pantalla de Reservas. El programa necesita una clave de la API de Grok (xAI) disponible como **variable de entorno** llamada `XAI_API_KEY`. Sin esa variable, el resto del programa funciona normal — solo el botón de IA del campo "Frase" va a mostrar un mensaje de error y no va a rellenar el formulario automáticamente.

> **Nota:** el proyecto usaba antes Gemini (`GEMINI_API_KEY`). Se cambió a Grok porque la cuenta de Google del equipo quedó bloqueada con un error 403 que no se pudo resolver a tiempo. Si ya tenías configurada `GEMINI_API_KEY`, no hace daño dejarla, pero ya no la usa el programa — ahora necesitas `XAI_API_KEY`.

## 1. Conseguir la clave

Si el equipo ya tiene una clave compartida, pídesela a Daniel y sáltate al paso 2.

Si necesitas generar la tuya propia:

1. Entra a [console.x.ai](https://console.x.ai) y crea una cuenta (o inicia sesión).
2. Ve a la sección **API Keys** y genera una clave nueva.
3. **A diferencia de Gemini, Grok no tiene tier gratuito.** Necesitas cargar crédito prepago a la cuenta (en la sección de **Billing** de console.x.ai) antes de que la clave funcione. Con un par de dólares alcanza de sobra para todas las pruebas del proyecto, porque cada llamada procesa una frase corta y devuelve un XML corto.

## 2. Configurar la variable de entorno

Elige **una** de las dos opciones (no hace falta hacer ambas).

### Opción A: Variable de entorno de Windows (recomendada — la más "para siempre")

Sirve sin importar cómo corras el programa (desde IntelliJ, desde una terminal, etc.) y no depende de ninguna configuración de ejecución en particular.

1. Busca en el menú de inicio de Windows: **"Editar las variables de entorno del sistema"**.
2. En la ventana que se abre, haz clic en el botón **"Variables de entorno..."**.
3. En la sección de arriba (**"Variables de usuario"**), haz clic en **"Nueva..."**.
4. Nombre de la variable: `XAI_API_KEY` (exactamente así, respetando mayúsculas).
5. Valor de la variable: pega tu clave de la API.
6. Dale **Aceptar** en las tres ventanas que quedaron abiertas.
7. **Reinicia IntelliJ por completo** (cerrar y volver a abrir) para que herede la variable nueva del sistema operativo — si ya lo tenías abierto antes de crear la variable, no la va a ver hasta que lo reinicies.

### Opción B: Solo dentro de IntelliJ (Run/Debug Configuration)

Útil si prefieres no tocar la configuración de todo Windows, pero hay que repetirla si borras la configuración de ejecución.

1. Ve a **Run → Edit Configurations...** (o el ícono de configuraciones junto al botón ▶ verde, arriba a la derecha).
2. Si no existe todavía una configuración para correr `Main`, créala: botón **"+"** → **Application**. Ponle un nombre (por ejemplo "Main") y en **"Main class"** selecciona tu clase `Main`.
3. Debajo de esos campos, busca el enlace **"Modify options"** y haz clic ahí.
4. En el menú que aparece, selecciona **"Environment variables"** — esto agrega un campo nuevo al formulario.
5. Haz clic en el ícono al final de ese campo (una hojita pequeña) para abrir el editor.
6. Presiona **"+"** y agrega: Name = `XAI_API_KEY`, Value = tu clave de la API.
7. Dale **OK** en el editor, y luego **OK**/**Apply** en la ventana de configuraciones.
8. A partir de ahora, corre el programa siempre con el botón ▶ usando esa configuración (no con "Run" desde el botón verde que aparece flotando sobre el método `main` de otra clase distinta, porque eso podría crear una configuración nueva sin la variable).

## 3. Verificar que funciona

1. Corre el programa e inicia sesión como funcionario.
2. Ve a la pantalla de **Reservas**.
3. En el campo **"Frase"**, escribe algo como: *"Necesito la sala de reuniones el viernes de 2pm a 4pm para una capacitación"*.
4. Haz clic en el botón de IA (el ícono junto al campo, con el tooltip "Generar reserva a partir de la frase (IA)").
5. Después de 1-3 segundos, los campos de Actividad, Fecha, Hora inicio, Hora fin y la lista de Categorías deberían llenarse solos. Revísalos y corrígelos si algo no quedó exacto (la fecha resuelve exacto por lo general, pero las categorías dependen de que la frase mencione algo parecido a una categoría ya existente en el sistema).

## Si algo sale mal

| Mensaje que aparece | Qué significa | Qué hacer |
|---|---|---|
| "No se encontró la variable de entorno XAI_API_KEY..." | La variable no quedó configurada, o IntelliJ no se reinició después de crearla | Repite el paso 2 (Opción A o B) y reinicia IntelliJ |
| "La API de Grok respondió con error 401" | La clave está mal copiada o no es válida | Vuelve a copiar la clave desde console.x.ai, sin espacios de más |
| "La API de Grok respondió con error 403" | La cuenta no tiene crédito cargado, o la clave no tiene permisos | Revisa la sección Billing en console.x.ai y confirma que hay saldo disponible |
| "La API de Grok respondió con error 429" | Se alcanzó el límite de solicitudes por minuto, o el saldo se agotó | Esperar un momento y volver a intentar, o revisar el saldo |
| "Grok no devolvió texto en la respuesta." | Pasa rara vez, con respuestas vacías del modelo | Reformular la frase de otra forma y volver a intentar |
| "No se pudo contactar el servicio de IA..." | No hay conexión a internet en ese momento | Revisar la conexión y volver a intentar |

En cualquiera de estos casos, el resto del formulario sigue disponible para llenarse a mano — la IA nunca bloquea poder registrar la reserva.

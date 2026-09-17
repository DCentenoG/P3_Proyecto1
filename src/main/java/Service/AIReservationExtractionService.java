package Service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/*
Capa que invoca al modelo de lenguaje (LLM) pedido por el enunciado ("el
sistema, invocando un modelo de lenguaje (LLM), extraera de la frase los
datos y llenara el formulario"). Usa la API de Groq (groq.com, no confundir
con Grok/xAI), endpoint "Chat Completions" compatible con el formato de
OpenAI (modelo + arreglo de mensajes system/user). Se eligio Groq porque
ofrece un tier gratuito real de modelos de solo texto, sin necesidad de
tarjeta ni facturacion (a diferencia de Grok/xAI, que es prepago desde el
inicio, y de Gemini, cuya cuenta del equipo quedo bloqueada con un 403 -
ver claude/integracion-api-gemini-reservas-ia.md en el proyecto de Claude
para ese historial). Esta clase UNICAMENTE interpreta la frase: no valida
disponibilidad, no valida que las categorias existan, no inventa datos
que no esten en la frase. Esa responsabilidad sigue siendo de
ReservationService/Employee#tryBook - aqui solo se llena texto.
*/
public class AIReservationExtractionService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    // Modelo de texto del tier gratuito de Groq: rapido y de sobra para esta
    // extraccion (frase corta, XML corto de salida). Revisar en
    // console.groq.com/docs/models si este nombre ya no esta disponible
    // cuando se corra el proyecto.
    private static final String MODEL = "openai/gpt-oss-20b";

    private static final String ENV_VAR_NAME = "GROQ_API_KEY";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Envía la frase escrita por el funcionario a Groq y devuelve los
     * datos que la IA pudo extraer. Lanza {@link AIExtractionException}
     * ante cualquier falla (sin red, clave ausente/inválida, límite de
     * uso alcanzado, XML mal formado) para que el Controller lo traduzca
     * en un mensaje y deje el formulario disponible para llenado manual.
     */
    public ExtractedReservationData extractFromPhrase(String phrase) throws AIExtractionException {
        String apiKey = System.getenv(ENV_VAR_NAME);
        if (apiKey == null || apiKey.isBlank()) {
            throw new AIExtractionException("No se encontró la variable de entorno " + ENV_VAR_NAME
                    + " con la clave de la API de Groq.");
        }

        String requestBody = buildRequestBody(phrase);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new AIExtractionException("No se pudo contactar el servicio de IA: " + e.getMessage(), e);
        }

        if (response.statusCode() != 200) {
            throw new AIExtractionException("La API de Groq respondió con error " + response.statusCode()
                    + ": " + response.body());
        }

        String xml = extractOutputText(response.body());
        return parseXml(xml);
    }

    private String buildRequestBody(String phrase) {
        JSONObject systemMessage = new JSONObject().put("role", "system").put("content", buildSystemInstruction());
        JSONObject userMessage = new JSONObject().put("role", "user").put("content", phrase);

        return new JSONObject()
                .put("model", MODEL)
                .put("temperature", 0)
                .put("messages", new JSONArray().put(systemMessage).put(userMessage))
                .toString();
    }

    private String buildSystemInstruction() {
        return "Eres un asistente que EXCLUSIVAMENTE extrae datos de una frase en espanol "
                + "para llenar un formulario de reserva de recursos de una organizacion. "
                + "No valides disponibilidad, no inventes datos, no expliques nada. "
                + "Responde UNICAMENTE con XML bien formado, sin bloques de codigo markdown, "
                + "sin texto antes ni despues, siguiendo exactamente este esquema: "
                + "<reservaExtraida><actividad></actividad><fecha></fecha>"
                + "<horaInicio></horaInicio><horaFin></horaFin>"
                + "<categorias><categoria></categoria></categorias></reservaExtraida>. "
                + "El formato de la fecha debe ser yyyy-MM-dd y el de las horas HH:mm en 24 horas. "
                + "Puede haber cero, una o varias etiquetas <categoria> segun cuantos recursos "
                + "distintos se mencionen en la frase. Si un dato no aparece en la frase o no "
                + "puedes determinarlo con certeza, deja la etiqueta correspondiente vacia; nunca "
                + "inventes un valor. La fecha de referencia de HOY es " + LocalDate.now()
                + " (usala para resolver expresiones relativas como \"manana\" o \"el viernes\").";
    }

    private String extractOutputText(String responseBody) throws AIExtractionException {
        JSONObject json;
        try {
            json = new JSONObject(responseBody);
        } catch (Exception e) {
            throw new AIExtractionException("La respuesta de la API de Groq no es un JSON válido.", e);
        }

        JSONArray choices = json.optJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new AIExtractionException("Groq no devolvió ninguna respuesta.");
        }

        JSONObject message = choices.getJSONObject(0).optJSONObject("message");
        String content = (message != null) ? message.optString("content", null) : null;
        if (content == null || content.isBlank()) {
            throw new AIExtractionException("Groq no devolvió texto en la respuesta.");
        }
        // Por si el modelo igual envuelve la salida en un bloque de código pese a la instrucción.
        return content.replaceAll("(?s)```xml\\s*|```", "").trim();
    }

    private ExtractedReservationData parseXml(String xml) throws AIExtractionException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            ExtractedReservationData data = new ExtractedReservationData();
            data.setActivity(textOrNull(doc, "actividad"));

            String fechaText = textOrNull(doc, "fecha");
            if (fechaText != null) {
                data.setDate(LocalDate.parse(fechaText));
            }
            String inicioText = textOrNull(doc, "horaInicio");
            if (inicioText != null) {
                data.setStartTime(LocalTime.parse(inicioText));
            }
            String finText = textOrNull(doc, "horaFin");
            if (finText != null) {
                data.setEndTime(LocalTime.parse(finText));
            }

            List<String> categories = new ArrayList<>();
            NodeList nodes = doc.getElementsByTagName("categoria");
            for (int i = 0; i < nodes.getLength(); i++) {
                String text = nodes.item(i).getTextContent();
                if (text != null && !text.isBlank()) {
                    categories.add(text.trim());
                }
            }
            data.setCategoryDescriptions(categories);
            return data;
        } catch (Exception e) {
            // Cubre SAXException (XML mal formado), DateTimeParseException (fecha/hora
            // en un formato inesperado) y cualquier otra falla de interpretación.
            throw new AIExtractionException("La respuesta de la IA no se pudo interpretar como el XML esperado.", e);
        }
    }

    private String textOrNull(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        String text = nodes.item(0).getTextContent();
        return (text == null || text.isBlank()) ? null : text.trim();
    }
}

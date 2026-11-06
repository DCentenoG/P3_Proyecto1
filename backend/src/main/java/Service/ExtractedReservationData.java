package Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Datos que la IA (ver {@link AIReservationExtractionService}) logró
 * extraer de la frase en lenguaje natural escrita por el funcionario en
 * la pantalla de Reservas. Cualquier campo que la IA no haya podido
 * determinar con certeza queda en {@code null} (o, en el caso de las
 * categorías, en una lista vacía) — nunca se inventa un valor.
 * <p>
 * Este DTO solo sirve para precargar el formulario; la validación real
 * (disponibilidad, existencia de las categorías, fechas pasadas, etc.)
 * sigue siendo responsabilidad de {@link ReservationService}, tal como
 * lo pide el enunciado.
 */
public class ExtractedReservationData {

    private String activity;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private List<String> categoryDescriptions = new ArrayList<>();

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public List<String> getCategoryDescriptions() {
        return categoryDescriptions;
    }

    public void setCategoryDescriptions(List<String> categoryDescriptions) {
        this.categoryDescriptions = (categoryDescriptions != null) ? categoryDescriptions : new ArrayList<>();
    }

    /** {@code true} si la IA no pudo identificar absolutamente ningún dato en la frase. */
    public boolean isEmpty() {
        return activity == null && date == null && startTime == null && endTime == null
                && categoryDescriptions.isEmpty();
    }
}

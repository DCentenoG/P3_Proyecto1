package Controller;

import View.ActivitySchedulingView;
import View.CRUDView;
import View.CalendarView;
import View.EntityType;
import View.MainFrame;
import View.ReservationsView;
import View.StatisticsView;

import javax.swing.JTabbedPane;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquesta el manejo de eventos de {@link MainFrame}: conecta el
 * {@link TopBarListener} y recorre cada tab ya construido para
 * asignarle el listener correspondiente según el tipo de vista que
 * contiene (Reservas, Calendarización, Actividades, Estadísticas o el
 * CRUD genérico de Funcionarios/Categorías/Recursos).
 */
public final class MainFrameListener {

    private final MainFrame mainFrame;
    private final SessionContext session;

    public MainFrameListener(MainFrame mainFrame, SessionContext session) {
        this.mainFrame = mainFrame;
        this.session = session;
        wire();
    }

    private void wire() {
        new TopBarListener(mainFrame, session);

        JTabbedPane tabs = mainFrame.getTabbedPane();
        CrudViewListener resourceListener = null;
        CrudViewListener categoryListener = null;
        CalendarViewListener calendarListener = null;
        ActivitySchedulingViewListener activityListener = null;
        ReservationsViewListener reservationsListener = null;
        List<CrudViewListener> crudListeners = new ArrayList<>();

        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component component = tabs.getComponentAt(i);
            if (component instanceof ReservationsView reservationsView) {
                reservationsListener = new ReservationsViewListener(reservationsView, session);
            } else if (component instanceof CalendarView calendarView) {
                calendarListener = new CalendarViewListener(calendarView, session);
            } else if (component instanceof ActivitySchedulingView activityView) {
                activityListener = new ActivitySchedulingViewListener(activityView, session);
            } else if (component instanceof StatisticsView statisticsView) {
                new StatisticsViewListener(statisticsView, session);
            } else if (component instanceof CRUDView crudView) {
                CrudViewListener listener = new CrudViewListener(crudView, session);
                crudListeners.add(listener);
                if (crudView.getEntityType() == EntityType.CATEGORIA) {
                    categoryListener = listener;
                } else if (crudView.getEntityType() == EntityType.RECURSO) {
                    resourceListener = listener;
                }
            }
        }

        // El combo de categorías de Recursos (y el de Calendarización, que ya
        // se sincroniza a sí mismo al construirse) se deben mantener
        // sincronizados cada vez que Categorías cambia; y como una categoría
        // editada/borrada también puede cambiar qué recursos existen, la
        // grilla de Calendarización (no solo el combo) debe refrescarse.
        if (categoryListener != null && (resourceListener != null || calendarListener != null)) {
            CrudViewListener finalResourceListener = resourceListener;
            CalendarViewListener finalCalendarListenerForCategory = calendarListener;
            categoryListener.setAfterCategoryChange(() -> {
                if (finalResourceListener != null) {
                    finalResourceListener.refreshCategoryOptions();
                }
                if (finalCalendarListenerForCategory != null) {
                    finalCalendarListenerForCategory.refreshCategoryOptions();
                    finalCalendarListenerForCategory.refresh();
                }
            });
        }
        if (resourceListener != null) {
            resourceListener.refreshCategoryOptions();
        }

        // Los tres CRUD (Funcionarios/Categorías/Recursos) se arman una sola
        // vez al iniciar sesión y quedan vivos en sus pestañas mientras dure
        // la sesión: si se registra o elimina algo en uno de ellos, los
        // otros deben refrescar su propio listado con los datos ya
        // actualizados, en vez de mostrar la foto de su última búsqueda
        // hasta que el usuario la repita manualmente. Un alta/edición/baja de
        // Recursos también puede cambiar las columnas de Calendarización.
        CalendarViewListener finalCalendarListenerForCrud = calendarListener;
        for (CrudViewListener listener : crudListeners) {
            listener.setAfterAnyChange(() -> {
                for (CrudViewListener other : crudListeners) {
                    if (other != listener) {
                        other.refresh();
                    }
                }
                if (finalCalendarListenerForCrud != null) {
                    finalCalendarListenerForCrud.refresh();
                }
            });
        }

        // Igual que con los tres CRUD: Calendarización y Actividades se
        // arman una sola vez y quedan vivas en sus pestañas, así que una
        // reserva creada o cancelada en "Registrar Reserva" debe
        // refrescarlas de inmediato, sin que el usuario tenga que repetir
        // manualmente la búsqueda en cada una.
        if (reservationsListener != null && (calendarListener != null || activityListener != null)) {
            CalendarViewListener finalCalendarListenerForReservation = calendarListener;
            ActivitySchedulingViewListener finalActivityListener = activityListener;
            reservationsListener.setAfterReservationChange(() -> {
                if (finalCalendarListenerForReservation != null) {
                    finalCalendarListenerForReservation.refresh();
                }
                if (finalActivityListener != null) {
                    finalActivityListener.refresh();
                }
            });
        }
    }
}

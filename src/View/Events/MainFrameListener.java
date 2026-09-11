package View.Events;

import View.ActivitySchedulingView;
import View.CRUDView;
import View.CalendarView;
import View.EntityType;
import View.MainFrame;
import View.ReservationsView;
import View.StatisticsView;

import javax.swing.JTabbedPane;
import java.awt.Component;

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

        for (int i = 0; i < tabs.getTabCount(); i++) {
            Component component = tabs.getComponentAt(i);
            if (component instanceof ReservationsView reservationsView) {
                new ReservationsViewListener(reservationsView, session);
            } else if (component instanceof CalendarView calendarView) {
                new CalendarViewListener(calendarView, session);
            } else if (component instanceof ActivitySchedulingView activityView) {
                new ActivitySchedulingViewListener(activityView, session);
            } else if (component instanceof StatisticsView statisticsView) {
                new StatisticsViewListener(statisticsView, session);
            } else if (component instanceof CRUDView crudView) {
                CrudViewListener listener = new CrudViewListener(crudView, session);
                if (crudView.getEntityType() == EntityType.CATEGORIA) {
                    categoryListener = listener;
                } else if (crudView.getEntityType() == EntityType.RECURSO) {
                    resourceListener = listener;
                }
            }
        }

        // El combo de categorías de Recursos se construyó vacío (MainFrame no conoce
        // la sesión al crear las vistas); se sincroniza aquí con las categorías reales,
        // y se mantiene sincronizado cada vez que Categorías cambia.
        if (resourceListener != null) {
            resourceListener.refreshCategoryOptions();
            if (categoryListener != null) {
                CrudViewListener finalResourceListener = resourceListener;
                categoryListener.setAfterCategoryChange(finalResourceListener::refreshCategoryOptions);
            }
        }
    }
}

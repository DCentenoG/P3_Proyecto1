package View;

import Model.Admin;
import Model.User;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Ventana principal del Sistema de Reservas.
 * <p>
 * Contiene el {@link TopBar} (siempre visible, sin importar el tab activo)
 * y un {@link JTabbedPane} cuyo contenido varía según el rol del usuario
 * con el que se ingresó: un {@code Funcionario} ve Reservas,
 * Calendarización, Actividades y Estadísticas; un {@code Administrador}
 * ve Funcionarios, Categorías y Recursos en lugar de Reservas. Fijo a la
 * derecha está el {@link AlertsPanel} con las alertas; en el caso del
 * administrador, ese sidebar tiene dos pestañas: "Alertas" y
 * "Solicitudes" (el {@link RequestsSidebar} con las solicitudes de
 * reserva pendientes de revisión). Al cambiar de tab, este
 * resalta su fondo e ícono tal como se definió en el mockup.
 * <p>
 * Esta clase solo construye la interfaz gráfica; la lógica de negocio y
 * el manejo de eventos (guardar, buscar, cerrar sesión, etc.) se
 * conectan desde fuera, en una etapa posterior.
 */
public class MainFrame extends JFrame {

    private final TopBar topBar;
    private final JTabbedPane tabbedPane;
    private final RequestsSidebar requestsSidebar;
    private final List<TabComponent> tabComponents = new ArrayList<>();
    private final AlertsPanel alertsPanel;

    public MainFrame(User user) {
        super("Sistema de Reservas");
        boolean isAdmin = user instanceof Admin;
        String roleLabel = isAdmin ? "ADMINISTRADOR" : "FUNCIONARIO";

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImages(IconLibrary.getAppIconImages());

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UITheme.BACKGROUND);
        setContentPane(content);

        // Barra superior + pestañas a la izquierda; el sidebar de solicitudes
        // (solo administrador) ocupa todo el alto de la ventana a la derecha,
        // como en el mockup.
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(UITheme.BACKGROUND);

        topBar = new TopBar(String.valueOf(user.getId()), roleLabel);
        main.add(topBar, BorderLayout.NORTH);

        tabbedPane = buildTabbedPane(isAdmin);
        main.add(tabbedPane, BorderLayout.CENTER);
        content.add(main, BorderLayout.CENTER);

        alertsPanel = new AlertsPanel();
// El sidebar de solicitudes es exclusivo del administrador; las alertas son para todos.
        requestsSidebar = isAdmin ? new RequestsSidebar() : null;

        // El administrador tiene dos secciones a la derecha (alertas y solicitudes):
        // cada una va en su propia pestaña para que ocupe todo el alto del sidebar.
        // El funcionario solo tiene las alertas.
        JComponent rightColumn = isAdmin ? buildSidebarTabs() : alertsPanel;
        rightColumn.setPreferredSize(new Dimension(AlertsPanel.PANEL_WIDTH, 10));
        content.add(rightColumn, BorderLayout.EAST);

// La ventana se ensancha lo que ocupa la columna derecha, para que las pestanas conserven su espacio.
        int sidebarWidth = AlertsPanel.PANEL_WIDTH;
        setSize(900 + sidebarWidth, 680);
        setMinimumSize(new Dimension(820 + sidebarWidth, 600));
        setLocationRelativeTo(null);
    }

    private JTabbedPane buildTabbedPane(boolean isAdmin) {
        JTabbedPane pane = new JTabbedPane(JTabbedPane.TOP);
        pane.setUI(new FlatTabbedPaneUI());
        pane.setBackground(UITheme.BACKGROUND);
        pane.setOpaque(true);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setFocusable(false);

        if (isAdmin) {
            addTab(pane, "Funcionarios", IconLibrary.GROUP, IconLibrary.GROUP_WHITE, new EmployeesView());
            addTab(pane, "Categorías", IconLibrary.CATEGORIES, IconLibrary.CATEGORIES_WHITE, new CategoriesView());
            addTab(pane, "Recursos", IconLibrary.DEVICE, IconLibrary.DEVICE_WHITE, new ResourceSchedulingView());
        } else {
            addTab(pane, "Reservas", IconLibrary.LIST, IconLibrary.LIST_WHITE, new ReservationsView());
        }
        addTab(pane, "Calendarización", IconLibrary.CALENDAR, IconLibrary.CALENDAR_WHITE, new CalendarView());
        addTab(pane, "Actividades", IconLibrary.CLIPBOARD, IconLibrary.CLIPBOARD_WHITE, new ActivitySchedulingView());
        addTab(pane, "Estadísticas", IconLibrary.STATISTICS, IconLibrary.STATISTICS_WHITE, new StatisticsView());

        pane.setSelectedIndex(0);
        refreshTabAppearance(pane);
        pane.addChangeListener(e -> refreshTabAppearance(pane));
        return pane;
    }

    /**
     * Pestañas del sidebar del administrador: "Alertas" (izquierda) y
     * "Solicitudes" (derecha), cada una con su sección completa. Usan la
     * misma estética plana de las pestañas principales (activa en azul con
     * texto blanco, inactiva en el fondo claro con texto azul), repartidas a
     * partes iguales en el ancho del sidebar.
     */
    private JTabbedPane buildSidebarTabs() {
        JTabbedPane pane = new JTabbedPane(JTabbedPane.TOP);
        pane.setUI(new FlatTabbedPaneUI());
        pane.setBackground(UITheme.SIDEBAR_BACKGROUND);
        pane.setOpaque(true);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setFocusable(false);

        List<SidebarTab> sidebarTabs = new ArrayList<>();
        // El delegado de Swing suma 3 px a cada pestaña: se descuentan para que
        // ambas quepan en una sola fila, a lo ancho del sidebar.
        int tabWidth = AlertsPanel.PANEL_WIDTH / 2 - 3;
        String[] titles = {"Alertas", "Solicitudes"};
        JComponent[] sections = {alertsPanel, requestsSidebar};
        for (int i = 0; i < titles.length; i++) {
            SidebarTab tab = new SidebarTab(titles[i], tabWidth);
            pane.addTab(titles[i], sections[i]);
            pane.setTabComponentAt(i, tab);
            sidebarTabs.add(tab);
        }

        pane.setSelectedIndex(0);
        Runnable refresh = () -> {
            for (int i = 0; i < sidebarTabs.size(); i++) {
                sidebarTabs.get(i).setSelectedState(i == pane.getSelectedIndex());
            }
        };
        refresh.run();
        pane.addChangeListener(e -> refresh.run());
        return pane;
    }

    private void addTab(JTabbedPane pane, String title, String blueIcon, String whiteIcon, JComponent view) {
        TabComponent tabComponent = new TabComponent(title, blueIcon, whiteIcon);
        pane.addTab(title, view);
        pane.setTabComponentAt(pane.getTabCount() - 1, tabComponent);
        tabComponents.add(tabComponent);
    }

    private void refreshTabAppearance(JTabbedPane pane) {
        for (int i = 0; i < tabComponents.size(); i++) {
            tabComponents.get(i).setSelectedState(i == pane.getSelectedIndex());
        }
    }

    public TopBar getTopBar() {
        return topBar;
    }

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    /** Sidebar de solicitudes, o {@code null} si el usuario no es administrador. */
    public RequestsSidebar getRequestsSidebar() {
        return requestsSidebar;
    }

    public AlertsPanel getAlertsPanel() { return alertsPanel;}

    /**
     * Pestaña del sidebar del administrador: solo texto (siempre visible),
     * con los mismos colores que las pestañas principales.
     */
    private static final class SidebarTab extends JPanel {

        private final JLabel textLabel = new JLabel();

        SidebarTab(String title, int width) {
            super(new BorderLayout());
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(11, 0, 11, 0));

            textLabel.setText(title);
            textLabel.setFont(UITheme.TAB_FONT);
            textLabel.setHorizontalAlignment(SwingConstants.CENTER);
            add(textLabel, BorderLayout.CENTER);

            setPreferredSize(new Dimension(width, getPreferredSize().height));
            setSelectedState(false);
        }

        void setSelectedState(boolean selected) {
            setBackground(selected ? UITheme.ACCENT_BLUE : UITheme.TAB_INACTIVE_BG);
            textLabel.setForeground(selected ? UITheme.WHITE : UITheme.TEXT_BLUE);
            repaint();
        }
    }

    /**
     * Componente visual de un tab: ícono siempre visible y etiqueta que solo
     * se muestra cuando el tab está activo, con el cambio de color de fondo
     * e ícono (azul→blanco) definido en el mockup.
     */
    private static final class TabComponent extends JPanel {

        private final JLabel iconLabel = new JLabel();
        private final JLabel textLabel = new JLabel();
        private final String blueIcon;
        private final String whiteIcon;

        TabComponent(String title, String blueIcon, String whiteIcon) {
            super(new FlowLayout(FlowLayout.CENTER, 8, 0));
            this.blueIcon = blueIcon;
            this.whiteIcon = whiteIcon;

            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

            textLabel.setText(title);
            textLabel.setFont(UITheme.TAB_FONT);
            textLabel.setForeground(UITheme.WHITE);

            add(iconLabel);
            add(textLabel);

            setSelectedState(false);
        }

        void setSelectedState(boolean selected) {
            setBackground(selected ? UITheme.ACCENT_BLUE : UITheme.TAB_INACTIVE_BG);
            iconLabel.setIcon(IconLibrary.get(selected ? whiteIcon : blueIcon, UITheme.ICON_TAB));
            textLabel.setVisible(selected);
            revalidate();
            repaint();
        }
    }
}

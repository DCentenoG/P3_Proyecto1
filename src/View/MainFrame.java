package View;

import Model.Admin;
import Model.User;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
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
 * ve además Funcionarios, Categorías y Recursos. Al cambiar de tab, este
 * resalta su fondo e ícono tal como se definió en el mockup.
 * <p>
 * Esta clase solo construye la interfaz gráfica; la lógica de negocio y
 * el manejo de eventos (guardar, buscar, cerrar sesión, etc.) se
 * conectan desde fuera, en una etapa posterior.
 */
public class MainFrame extends JFrame {

    private final TopBar topBar;
    private final JTabbedPane tabbedPane;
    private final List<TabComponent> tabComponents = new ArrayList<>();

    public MainFrame(User user) {
        super("Sistema de Reservas");
        boolean isAdmin = user instanceof Admin;
        String roleLabel = isAdmin ? "ADMINISTRADOR" : "FUNCIONARIO";

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UITheme.BACKGROUND);
        setContentPane(content);

        topBar = new TopBar(String.valueOf(user.getId()), roleLabel);
        content.add(topBar, BorderLayout.NORTH);

        tabbedPane = buildTabbedPane(isAdmin);
        content.add(tabbedPane, BorderLayout.CENTER);

        setSize(900, 640);
        setMinimumSize(new Dimension(820, 560));
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

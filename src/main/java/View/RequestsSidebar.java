package View;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Sidebar de solicitudes del administrador (mockup): panel fijo al este
 * de la ventana principal, visible en todo momento y solo cuando se
 * ingresa como Administrador (un funcionario no lo tiene).
 * <p>
 * Contiene:
 * <ul>
 *   <li>El título "Solicitudes", que muestra además cuántas solicitudes
 *       nuevas (sin marcar como vistas) hay, p. ej. "Solicitudes - 2 nuevas";
 *       sin solicitudes nuevas queda solo "Solicitudes".</li>
 *   <li>Una tabla con el funcionario que hizo cada solicitud y la actividad.
 *       Las solicitudes no vistas llevan el ícono de exclamación a la
 *       derecha de la actividad (ver {@link #setUnseenRows}).</li>
 *   <li>Si no hay ninguna solicitud, en lugar de la tabla se muestra un
 *       mensaje que lo indica (ver {@link #showEmptyState}).</li>
 *   <li>Los botones "Marcar como vista" y "Revisar", que operan sobre la
 *       solicitud seleccionada en la tabla.</li>
 * </ul>
 * Solo construye la interfaz gráfica: la carga de las solicitudes, su
 * orden y las acciones se conectan desde el manejo de eventos en una
 * etapa posterior.
 */
public class RequestsSidebar extends JPanel {

    /** Columnas de la tabla, en el orden del mockup. */
    public static final String[] COLUMNS = {"Funcionario", "Actividad"};
    /** Índice de la columna "Actividad" (donde va el ícono de las solicitudes no vistas). */
    public static final int ACTIVITY_COLUMN = 1;

    /** Ancho fijo del sidebar. */
    public static final int SIDEBAR_WIDTH = 260;

    private static final String TITLE = "Solicitudes";
    private static final String NO_REQUESTS_MESSAGE = "No hay solicitudes para reservas";
    /** Filas en blanco iniciales, para que la tabla se vea como en el mockup aun sin datos. */
    private static final int BLANK_ROWS = 12;
    private static final int UNSEEN_ICON_SIZE = 14;

    private final JLabel titleLabel;
    private final JTable table;
    private final JPanel tableContainer;
    private final JButton markAsSeenButton;
    private final JButton reviewButton;

    /** Filas (índices de la tabla) cuyas solicitudes aún no se marcaron como vistas. */
    private final Set<Integer> unseenRows = new HashSet<>();

    public RequestsSidebar() {
        super(new BorderLayout(0, 10));
        setBackground(UITheme.SIDEBAR_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 12, 14, 12));
        setPreferredSize(new Dimension(SIDEBAR_WIDTH, 10));

        titleLabel = UITheme.createSectionTitle(TITLE);
        add(titleLabel, BorderLayout.NORTH);

        table = buildTable();
        JScrollPane tableScroll = new JScrollPane(table);
        UITheme.styleScrollPane(tableScroll);

        JLabel emptyLabel = new JLabel(NO_REQUESTS_MESSAGE, SwingConstants.CENTER);
        emptyLabel.setFont(UITheme.TABLE_FONT);
        emptyLabel.setForeground(UITheme.TEXT_BLUE);
        JPanel emptyPanel = new JPanel(new BorderLayout());
        emptyPanel.setOpaque(false);
        emptyPanel.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        emptyPanel.add(emptyLabel, BorderLayout.NORTH);

        tableContainer = new JPanel(new CardLayout());
        tableContainer.setOpaque(false);
        tableContainer.add(tableScroll, "table");
        tableContainer.add(emptyPanel, "empty");
        add(tableContainer, BorderLayout.CENTER);

        markAsSeenButton = UITheme.createMarkAsSeenButton();
        markAsSeenButton.setToolTipText("Marcar como vista la solicitud seleccionada");
        UITheme.compactActionButton(markAsSeenButton);
        reviewButton = UITheme.createReviewButton();
        reviewButton.setToolTipText("Revisar la solicitud seleccionada");
        UITheme.compactActionButton(reviewButton);

        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setOpaque(false);
        actions.add(markAsSeenButton);
        actions.add(reviewButton);
        add(actions, BorderLayout.SOUTH);
    }

    private JTable buildTable() {
        DefaultTableModel model = new DefaultTableModel(new Object[BLANK_ROWS][COLUMNS.length], COLUMNS) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable sidebarTable = new JTable(model);
        UITheme.styleTable(sidebarTable, UITheme.SIDEBAR_TABLE_HEADER);
        sidebarTable.getColumnModel().getColumn(ACTIVITY_COLUMN).setCellRenderer(new ActivityCellRenderer());
        return sidebarTable;
    }

    // ---------------------------------------------------------------
    // Estado visual (lo usa el controlador al cargar las solicitudes)
    // ---------------------------------------------------------------

    /**
     * Actualiza el título con la cantidad de solicitudes nuevas sin ver:
     * "Solicitudes - 1 nueva", "Solicitudes - 2 nuevas", o solo
     * "Solicitudes" si no hay ninguna.
     */
    public void setUnseenCount(int count) {
        if (count <= 0) {
            titleLabel.setText(TITLE);
        } else {
            titleLabel.setText(TITLE + " - " + count + (count == 1 ? " nueva" : " nuevas"));
        }
    }

    /** Indica qué filas de la tabla corresponden a solicitudes no vistas (llevan el ícono de exclamación). */
    public void setUnseenRows(Collection<Integer> rows) {
        unseenRows.clear();
        unseenRows.addAll(rows);
        table.repaint();
    }

    /** Alterna entre la tabla y el mensaje de "no hay solicitudes para reservas". */
    public void showEmptyState(boolean empty) {
        CardLayout layout = (CardLayout) tableContainer.getLayout();
        layout.show(tableContainer, empty ? "empty" : "table");
    }

    // ---- Métodos de acceso para el futuro controlador ----

    public JTable getTable() {
        return table;
    }

    /** Marca como vista la notificación de la solicitud seleccionada (no cambia el estado de la reserva). */
    public JButton getMarkAsSeenButton() {
        return markAsSeenButton;
    }

    /** Abre la revisión de la solicitud seleccionada. */
    public JButton getReviewButton() {
        return reviewButton;
    }

    // ---------------------------------------------------------------
    // Celda "Actividad": texto + ícono de exclamación si no se ha visto
    // ---------------------------------------------------------------

    /**
     * Dibuja la actividad igual que cualquier otra celda y, si la fila es
     * de una solicitud no vista, agrega el ícono de exclamación pegado a la
     * derecha, sin necesidad de una columna aparte.
     */
    private final class ActivityCellRenderer implements TableCellRenderer {

        private final DefaultTableCellRenderer text = new DefaultTableCellRenderer();
        private final JLabel icon = new JLabel(IconLibrary.get(IconLibrary.EXCLAMATION, UNSEEN_ICON_SIZE));
        private final JPanel cell = new JPanel(new BorderLayout(4, 0));

        ActivityCellRenderer() {
            icon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
            cell.add(text, BorderLayout.CENTER);
            cell.add(icon, BorderLayout.EAST);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            text.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            text.setOpaque(false);
            // Opaco solo si la fila está seleccionada: si no, se deja ver el fondo de la
            // tabla y sus líneas de la grilla, igual que en las demás celdas.
            cell.setOpaque(isSelected);
            cell.setBackground(table.getSelectionBackground());
            icon.setVisible(value != null && unseenRows.contains(row));
            return cell;
        }
    }
}

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
 * El controlador le entrega las solicitudes ya ordenadas con
 * {@link #showRequests(List)} y el sidebar se encarga de mostrarlas: la
 * tabla, los íconos de las no vistas, el contador del título y el mensaje
 * de "no hay solicitudes". Las acciones de los botones se conectan desde
 * el controlador ({@code Controller}), que obtiene la solicitud elegida con
 * {@link #getSelectedRequestIndex()}.
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

    /** Cantidad de solicitudes mostradas (las filas siguientes son de relleno, en blanco). */
    private int requestCount = 0;

    /**
     * Lo que el sidebar muestra de una solicitud: el funcionario que la hizo,
     * la actividad y si su notificación aún no se marcó como vista.
     */
    public record RequestRow(String employeeName, String activity, boolean unseen) {
    }

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
        sidebarTable.getColumnModel().getColumn(0).setCellRenderer(new TooltipCellRenderer());
        sidebarTable.getColumnModel().getColumn(ACTIVITY_COLUMN).setCellRenderer(new ActivityCellRenderer());
        return sidebarTable;
    }

    // ---------------------------------------------------------------
    // Carga de las solicitudes (la usa el controlador)
    // ---------------------------------------------------------------

    /**
     * Muestra {@code requests} en la tabla, en el orden recibido (el
     * controlador las entrega ya ordenadas: nuevas primero, luego vistas).
     * Además actualiza el ícono de las no vistas y el contador del título,
     * y si no hay ninguna solicitud muestra el mensaje en lugar de la tabla.
     * Como el orden puede cambiar, se quita la selección anterior y la tabla
     * vuelve al principio (donde entran las solicitudes nuevas).
     */
    public void showRequests(List<RequestRow> requests) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        table.clearSelection();
        requestCount = requests.size();

        List<Integer> unseen = new ArrayList<>();
        model.setRowCount(Math.max(BLANK_ROWS, requestCount));
        for (int row = 0; row < model.getRowCount(); row++) {
            if (row < requestCount) {
                RequestRow request = requests.get(row);
                model.setValueAt(request.employeeName(), row, 0);
                model.setValueAt(request.activity(), row, ACTIVITY_COLUMN);
                if (request.unseen()) {
                    unseen.add(row);
                }
            } else {
                model.setValueAt(null, row, 0);
                model.setValueAt(null, row, ACTIVITY_COLUMN);
            }
        }

        setUnseenRows(unseen);
        setUnseenCount(unseen.size());
        showEmptyState(requestCount == 0);
        if (requestCount > 0) {
            table.scrollRectToVisible(table.getCellRect(0, 0, true));
        }
    }

    /**
     * Índice (en la lista entregada a {@link #showRequests(List)}) de la
     * solicitud seleccionada, o {@code -1} si no hay ninguna seleccionada
     * (incluye haber seleccionado una fila de relleno en blanco).
     */
    public int getSelectedRequestIndex() {
        int row = table.getSelectedRow();
        return (row >= 0 && row < requestCount) ? row : -1;
    }

    // ---------------------------------------------------------------
    // Estado visual (lo aplica showRequests)
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
    // Celda "Funcionario": texto normal, con el nombre completo en el tooltip
    // ---------------------------------------------------------------

    /** Igual que una celda normal, pero con el texto completo como tooltip (la columna es angosta). */
    private static final class TooltipCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setToolTipText(value == null ? null : value.toString());
            return this;
        }
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
            boolean unseen = value != null && unseenRows.contains(row);
            icon.setVisible(unseen);
            // El texto se recorta si es largo: el tooltip muestra la actividad completa.
            cell.setToolTipText(value == null ? null : value + (unseen ? " (nueva)" : ""));
            return cell;
        }
    }
}

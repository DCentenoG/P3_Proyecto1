package View;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;

/**
 * Pestaña "Solicitudes" del administrador (mockup): listado de las
 * reservas que están pendientes de revisión, con el funcionario que la
 * solicitó, la actividad, la fecha y el horario pedidos.
 * <p>
 * Debajo de la tabla están el botón de imprimir el listado y el botón
 * "Revisar", que abre {@link ReviewRequestForm} para la solicitud
 * seleccionada. Una solicitud ya revisada (aprobada o rechazada) deja
 * de aparecer en este listado.
 * <p>
 * Solo construye la interfaz gráfica: la carga de las solicitudes, la
 * impresión y la revisión se conectan desde el manejo de eventos en una
 * etapa posterior.
 */
public class RequestsView extends JPanel {

    /** Columnas del listado, en el orden del mockup. */
    public static final String[] COLUMNS = {"ID Funcionario", "Nombre", "Actividad", "Fecha", "Horario"};

    private static final String NO_REQUESTS_MESSAGE = "No hay solicitudes pendientes de revisión.";

    private final JTable table;
    private final JPanel tableContainer;
    private final JButton printButton;
    private final JButton reviewButton;

    public RequestsView() {
        super(new BorderLayout(0, 6));
        setBackground(UITheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

        add(UITheme.leftAligned(UITheme.createLabel("Listado")), BorderLayout.NORTH);

        table = buildTable();
        JScrollPane tableScroll = new JScrollPane(table);
        UITheme.styleScrollPane(tableScroll);

        JLabel emptyLabel = UITheme.createLabel(NO_REQUESTS_MESSAGE);
        emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel emptyPanel = new JPanel(new BorderLayout());
        emptyPanel.setOpaque(false);
        emptyPanel.add(emptyLabel, BorderLayout.CENTER);

        tableContainer = new JPanel(new CardLayout());
        tableContainer.setOpaque(false);
        tableContainer.add(tableScroll, "table");
        tableContainer.add(emptyPanel, "empty");
        add(tableContainer, BorderLayout.CENTER);

        printButton = UITheme.createIconOnlyButton(IconLibrary.PRINTER, 30);
        printButton.setToolTipText("Imprimir el listado de solicitudes");
        reviewButton = UITheme.createReviewButton();
        reviewButton.setToolTipText("Revisar la solicitud seleccionada");
        add(buildActionsRow(), BorderLayout.SOUTH);
    }

    private static JTable buildTable() {
        DefaultTableModel model = new DefaultTableModel(new Object[7][COLUMNS.length], COLUMNS) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        UITheme.styleTable(table);
        return table;
    }

    private JPanel buildActionsRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        row.add(printButton);
        row.add(reviewButton);
        return row;
    }

    // ---- Métodos de acceso para el futuro controlador ----

    public JTable getTable() {
        return table;
    }

    public JButton getPrintButton() {
        return printButton;
    }

    /** Abre la revisión de la solicitud seleccionada en la tabla. */
    public JButton getReviewButton() {
        return reviewButton;
    }

    /** Alterna entre la tabla y el mensaje de "no hay solicitudes pendientes". */
    public void showEmptyState(boolean empty) {
        CardLayout layout = (CardLayout) tableContainer.getLayout();
        layout.show(tableContainer, empty ? "empty" : "table");
    }
}

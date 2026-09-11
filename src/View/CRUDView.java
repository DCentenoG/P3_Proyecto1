package View;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

/**
 * Vista CRUD genérica: dado un {@link EntityType} arma la sección de
 * "Búsqueda" (filtros centrados + ícono de buscar + ícono de imprimir),
 * la sección de "Listado" (tabla con las columnas propias de la
 * entidad, sin acciones incrustadas) y, debajo de la tabla, los botones
 * independientes "Editar" y "Borrar" que operan sobre la fila
 * seleccionada, tal como se repite en las páginas de Funcionarios,
 * Categorías y Recursos del mockup. Las vistas específicas
 * ({@link EmployeesView}, {@link CategoriesView},
 * {@link ResourceSchedulingView}) solo instancian esta clase con su
 * entidad correspondiente.
 */
public class CRUDView extends JPanel {

    private final EntityType entityType;
    private final FilterBuilder filterBuilder;
    private final JButton searchButton;
    private final JButton printButton;
    private final JTable table;
    private final JButton editButton;
    private final JButton deleteButton;

    public CRUDView(EntityType entityType, List<String> categoryOptions) {
        super(new BorderLayout(0, 14));
        this.entityType = entityType;
        setBackground(UITheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

        filterBuilder = new FilterBuilder(entityType, categoryOptions);
        searchButton = UITheme.createIconOnlyButton(IconLibrary.SEARCH, 22);
        searchButton.setToolTipText("Buscar");
        printButton = UITheme.createIconOnlyButton(IconLibrary.PRINTER, 22);
        printButton.setToolTipText("Imprimir");

        add(buildHeader(), BorderLayout.NORTH);

        table = TableBuilder.build(entityType, 7);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(UITheme.FIELD_BORDER));
        add(tableScroll, BorderLayout.CENTER);

        editButton = UITheme.createEditButton();
        deleteButton = UITheme.createDeleteButton();
        add(buildActionsRow(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        header.add(UITheme.leftAligned(UITheme.createSectionTitle(entityType.getPluralTitle())));
        header.add(Box.createVerticalStrut(16));

        header.add(UITheme.leftAligned(UITheme.createLabel("Búsqueda")));
        header.add(Box.createVerticalStrut(6));
        header.add(UITheme.centered(buildSearchRow()));
        header.add(Box.createVerticalStrut(18));

        header.add(UITheme.leftAligned(UITheme.createLabel("Listado")));
        header.add(Box.createVerticalStrut(6));

        return header;
    }

    private JPanel buildSearchRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        row.setOpaque(false);
        row.add(filterBuilder.getPanel());
        row.add(searchButton);
        row.add(printButton);
        return row;
    }

    private JPanel buildActionsRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        row.add(editButton);
        row.add(deleteButton);
        return row;
    }

    // ---- Métodos de acceso para el futuro controlador ----

    public EntityType getEntityType() {
        return entityType;
    }

    public FilterBuilder getFilterBuilder() {
        return filterBuilder;
    }

    public JButton getSearchButton() {
        return searchButton;
    }

    public JButton getPrintButton() {
        return printButton;
    }

    public JTable getTable() {
        return table;
    }

    public JButton getEditButton() {
        return editButton;
    }

    public JButton getDeleteButton() {
        return deleteButton;
    }
}

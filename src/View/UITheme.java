package View;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Sistema de diseño compartido por toda la capa View: colores oficiales
 * del mockup, tipografías y fábricas de componentes con apariencia
 * consistente (etiquetas, campos, botones). Ninguna clase de View debe
 * declarar sus propios colores o fuentes; todas usan estas constantes,
 * de modo que un cambio de estética se hace en un solo lugar.
 */
public final class UITheme {

    // ---- Paleta oficial (ver "Conver de Juanpa" / mockup) ----
    public static final Color BACKGROUND = Color.decode("#F8FAFC");
    public static final Color TEXT_BLUE = Color.decode("#1E3A8A");
    public static final Color ACCENT_BLUE = Color.decode("#2563EB");
    public static final Color DANGER_RED = Color.decode("#C20606");
    public static final Color ADD_GREEN = Color.decode("#05862C");
    public static final Color CALENDAR_EVENT = Color.decode("#7DC4F4");
    public static final Color STATS_PANEL = Color.decode("#C3CBE3");

    public static final Color WHITE = Color.WHITE;
    public static final Color FIELD_BORDER = new Color(203, 213, 225);
    public static final Color TABLE_GRID = new Color(226, 232, 240);
    public static final Color TAB_INACTIVE_BG = BACKGROUND;
    public static final Color NOTE_GRAY = new Color(100, 116, 139);

    // ---- Tipografía ----
    private static final String FONT_FAMILY = "SansSerif";
    public static final Font WINDOW_TITLE_FONT = new Font(FONT_FAMILY, Font.BOLD, 17);
    public static final Font SECTION_FONT = new Font(FONT_FAMILY, Font.BOLD, 15);
    public static final Font LABEL_FONT = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font FIELD_FONT = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font TAB_FONT = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font BUTTON_FONT = new Font(FONT_FAMILY, Font.BOLD, 12);
    public static final Font TABLE_HEADER_FONT = new Font(FONT_FAMILY, Font.BOLD, 12);
    public static final Font TABLE_FONT = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font NOTE_FONT = new Font(FONT_FAMILY, Font.ITALIC, 12);

    // ---- Medidas ----
    public static final int ICON_SMALL = 18;
    public static final int ICON_MEDIUM = 22;
    public static final int ICON_TAB = 22;
    public static final int ICON_ACTION = 30;

    /** Ancho y alto mínimos de un campo de texto/combo, como en el mockup (formularios espaciosos). */
    public static final int FIELD_MIN_WIDTH = 220;
    public static final int FIELD_HEIGHT = 32;

    /** Tamaño de los íconos de aviso/pregunta que muestran los JOptionPane de {@code DialogHelper}. */
    public static final int OPTION_ICON_SIZE = 32;

    private UITheme() {
        // Clase de utilidades: no debe instanciarse.
    }

    /** Etiqueta de campo/sección: azul oscuro y en negrita, como en el mockup. */
    public static JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(TEXT_BLUE);
        return label;
    }

    /** Título de sección (p. ej. "Nueva reserva", "Mis reservas"). */
    public static JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SECTION_FONT);
        label.setForeground(TEXT_BLUE);
        return label;
    }

    /** Nota aclaratoria en gris/itálica (p. ej. "*: Campo obligatorio"). */
    public static JLabel createNote(String text) {
        JLabel label = new JLabel(text);
        label.setFont(NOTE_FONT);
        label.setForeground(NOTE_GRAY);
        return label;
    }

    /**
     * Fila de encabezado con un título de sección a la izquierda y una nota
     * aclaratoria al extremo derecho (p. ej. "Nueva reserva" ... "*: Campo obligatorio").
     */
    public static JPanel sectionTitleWithNote(String title, String note) {
        JPanel row = new JPanel(new java.awt.BorderLayout());
        row.setOpaque(false);
        row.add(createSectionTitle(title), java.awt.BorderLayout.WEST);
        row.add(createNote(note), java.awt.BorderLayout.EAST);
        return row;
    }

    /** Borde + tipografía estándar para campos de texto y de clave. */
    public static Border fieldBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER),
                BorderFactory.createEmptyBorder(4, 8, 4, 8));
    }

    /** Agrupa una etiqueta arriba y un campo/combo debajo, como en los filtros y formularios. */
    public static JPanel labeledField(String label, javax.swing.JComponent field) {
        JPanel group = new JPanel(new java.awt.BorderLayout(0, 4));
        group.setOpaque(false);
        group.add(createLabel(label), java.awt.BorderLayout.NORTH);
        group.add(field, java.awt.BorderLayout.CENTER);
        return group;
    }

    /** Envuelve un componente para que quede alineado a la izquierda dentro de un contenedor vertical (BoxLayout). */
    public static JPanel leftAligned(java.awt.Component component) {
        JPanel wrapper = new JPanel(new java.awt.BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(component, java.awt.BorderLayout.WEST);
        wrapper.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        return wrapper;
    }

    /** Envuelve un componente para que quede centrado horizontalmente dentro de un contenedor vertical (BoxLayout). */
    public static JPanel centered(java.awt.Component component) {
        JPanel wrapper = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
        wrapper.setOpaque(false);
        wrapper.add(component);
        wrapper.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        return wrapper;
    }

    /**
     * Fila horizontal de componentes separados por {@code gap} píxeles, sin
     * margen antes del primero ni después del último. A diferencia de
     * {@link java.awt.FlowLayout} (que reserva {@code hgap} también como
     * margen izquierdo del contenedor), esta fila deja el primer componente
     * exactamente en el borde izquierdo, para que quede alineado en pantalla
     * con cualquier etiqueta puesta arriba mediante {@link #leftAligned}.
     */
    public static JPanel row(int gap, java.awt.Component... components) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.X_AXIS));
        for (int i = 0; i < components.length; i++) {
            if (i > 0 && gap > 0) {
                panel.add(Box.createHorizontalStrut(gap));
            }
            panel.add(components[i]);
        }
        return panel;
    }

    /**
     * Marca un campo de texto de fecha/semana como "solo selección": el
     * usuario no puede escribir directamente sobre él, únicamente mediante
     * el botón selector asociado a ese campo. La lógica de selección de
     * fecha en sí se conecta en una etapa posterior; por ahora el campo
     * simplemente deja de aceptar edición manual.
     */
    public static void lockAsPickerOnly(JTextField field) {
        field.setEditable(false);
    }

    /**
     * Tipografía, borde y tamaño mínimo estándar de un campo de texto. El
     * ancho/alto reales se calculan como el mayor entre lo que el campo ya
     * traía y {@link #FIELD_MIN_WIDTH}/{@link #FIELD_HEIGHT}, para que
     * ningún formulario quede con campos angostos como en el mockup.
     */
    public static void styleField(JTextField field) {
        field.setFont(FIELD_FONT);
        field.setBackground(WHITE);
        field.setBorder(fieldBorder());
        int width = Math.max(field.getPreferredSize().width, FIELD_MIN_WIDTH);
        field.setPreferredSize(new Dimension(width, FIELD_HEIGHT));
    }

    /**
     * Tipografía, colores, borde y tamaño mínimo estándar de un combo
     * (lista desplegable) de filtro o formulario, para que se vea igual
     * de "plano" y uniforme que un {@link #styleField(JTextField)}: mismo
     * fondo blanco, mismo borde gris y mismo azul de acento al
     * seleccionar una opción de la lista desplegada (ver también
     * {@link #installSwingTheme()}, que fija esos mismos colores de
     * selección a nivel de {@link UIManager} para toda la aplicación).
     */
    public static void styleCombo(JComboBox<?> combo) {
        combo.setFont(FIELD_FONT);
        combo.setBackground(WHITE);
        combo.setForeground(Color.BLACK);
        combo.setBorder(BorderFactory.createLineBorder(FIELD_BORDER));
        int width = Math.max(combo.getPreferredSize().width, FIELD_MIN_WIDTH);
        combo.setPreferredSize(new Dimension(width, FIELD_HEIGHT));
    }

    /**
     * Borde, fondo y barras de desplazamiento planas estándar de un
     * {@link JScrollPane} (tablas, la lista de categorías de
     * {@link ReservationsView}, las celdas de calendarización, etc.):
     * mismo borde gris que un campo de texto y una barra de scroll
     * delgada y sin flechas ({@link FlatScrollBarUI}) en vez del relieve
     * por defecto del Look &amp; Feel.
     */
    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(FIELD_BORDER));
        scrollPane.setBackground(WHITE);
        scrollPane.getViewport().setBackground(WHITE);
        scrollPane.getVerticalScrollBar().setUI(new FlatScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new FlatScrollBarUI());
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
    }

    /**
     * Botón con ícono arriba y texto debajo (como "Ingresar"/"Cancelar" del
     * login, o "Guardar reserva"/"Cancelar reserva"/"Limpiar").
     */
    public static JButton createStackedIconButton(String text, String iconFile, int iconSize) {
        JButton button = new JButton(text, IconLibrary.get(iconFile, iconSize));
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setIconTextGap(6);
        button.setFont(BUTTON_FONT);
        button.setForeground(ACCENT_BLUE);
        stripChrome(button);
        return button;
    }

    /**
     * Botón con ícono a la izquierda y texto a la derecha, en línea (como
     * "Cambiar clave" / "Salir" del TopBar).
     */
    public static JButton createInlineIconButton(String text, String iconFile, int iconSize) {
        JButton button = new JButton(text, IconLibrary.get(iconFile, iconSize));
        button.setHorizontalTextPosition(SwingConstants.RIGHT);
        button.setVerticalTextPosition(SwingConstants.CENTER);
        button.setIconTextGap(6);
        button.setFont(LABEL_FONT);
        button.setForeground(ACCENT_BLUE);
        stripChrome(button);
        return button;
    }

    /** Botón que muestra únicamente un ícono (buscar, imprimir, "...", etc.). */
    public static JButton createIconOnlyButton(String iconFile, int iconSize) {
        JButton button = new JButton(IconLibrary.get(iconFile, iconSize));
        stripChrome(button);
        return button;
    }

    /**
     * Botón "Agregar" independiente (rectángulo verde {@code #05862C} de
     * esquinas redondeadas con ícono y texto blancos), usado junto a
     * {@link #createEditButton()} y {@link #createDeleteButton()} bajo el
     * listado de {@link CRUDView} para crear un nuevo registro sin
     * necesidad de seleccionar ninguna fila de la tabla.
     */
    public static JButton createAddButton() {
        return new RoundedActionButton("Agregar", IconLibrary.PLUS_WHITE, ADD_GREEN);
    }

    /**
     * Botón "Editar" independiente (rectángulo azul de esquinas redondeadas
     * con ícono y texto blancos), usado junto a {@link #createDeleteButton()}
     * bajo el listado de {@link CRUDView} para operar sobre la fila
     * seleccionada de la tabla.
     */
    public static JButton createEditButton() {
        return new RoundedActionButton("Editar", IconLibrary.PENCIL_WHITE, ACCENT_BLUE);
    }

    /**
     * Botón "Borrar" independiente (rectángulo rojo {@code #C20606} de
     * esquinas redondeadas con ícono y texto blancos), usado junto a
     * {@link #createEditButton()} bajo el listado de {@link CRUDView}.
     */
    public static JButton createDeleteButton() {
        return new RoundedActionButton("Borrar", IconLibrary.TRASH_WHITE, DANGER_RED);
    }

    /**
     * Botón cuadrado y compacto de esquinas redondeadas con un ícono
     * blanco centrado, sin texto (misma familia visual que
     * {@link #createAddButton()}/{@link #createDeleteButton()} pero para
     * espacios reducidos, p. ej. agregar/quitar filas de una minitabla).
     */
    public static JButton createSmallRoundedIconButton(String iconFile, Color fill, int size) {
        JButton button = new RoundedActionButton("", iconFile, fill);
        button.setMargin(new java.awt.Insets(0, 0, 0, 0));
        button.setPreferredSize(new Dimension(size, size));
        return button;
    }

    /**
     * Aplica la apariencia estándar de tabla (encabezado azul con texto
     * blanco, grilla suave, sin edición inline) usada por todas las
     * tablas de la aplicación: la de "Mis reservas" y las que arma
     * {@code TableBuilder} para los CRUD.
     */
    public static void styleTable(JTable table) {
        table.setFont(TABLE_FONT);
        table.setRowHeight(26);
        table.setGridColor(TABLE_GRID);
        table.setShowGrid(true);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new java.awt.Dimension(0, 0));

        JTableHeader header = table.getTableHeader();
        header.setFont(TABLE_HEADER_FONT);
        header.setBackground(ACCENT_BLUE);
        header.setForeground(WHITE);
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new TableHeaderRenderer());
    }

    private static final class TableHeaderRenderer extends DefaultTableCellRenderer {
        TableHeaderRenderer() {
            setHorizontalAlignment(SwingConstants.LEFT);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString());
            setFont(TABLE_HEADER_FONT);
            setBackground(ACCENT_BLUE);
            setForeground(WHITE);
            setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            return this;
        }
    }

    /**
     * Botón gris neutro para abrir selectores (fecha, hora, semana), como
     * los botones "▾"/"…" del mockup: visualmente distinto del botón de
     * acción azul, ya que no dispara una acción sino que abre un selector.
     */
    public static JButton createPickerButton(String glyph) {
        return createPickerButton(glyph, 34, 30);
    }

    /**
     * Igual que {@link #createPickerButton(String)} pero con un tamaño
     * propio, para los selectores de fecha re-escalados a un espacio más
     * reducido (p. ej. los filtros de rango de fecha de Estadísticas).
     */
    public static JButton createPickerButton(String glyph, int width, int height) {
        JButton button = new JButton(glyph);
        button.setFont(FIELD_FONT);
        button.setForeground(new Color(71, 85, 105));
        button.setBackground(new Color(226, 232, 240));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(FIELD_BORDER));
        button.setMargin(new java.awt.Insets(0, 0, 0, 0));
        button.setPreferredSize(new Dimension(width, height));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /**
     * Uniforma con un solo llamado, al iniciar la aplicación (ver
     * {@code Main}, antes de crear cualquier ventana), la apariencia de
     * los componentes cuyo estilo por defecto viene del Look &amp; Feel en
     * vez de las fábricas de {@code UITheme}:
     * <ul>
     *   <li>Los {@link JOptionPane} de {@code Controller.DialogHelper}
     *       (confirmar/avisar/informar/error): mismo fondo, tipografía,
     *       color de acento en los botones ("Sí"/"No"/"Aceptar") e íconos
     *       de {@link IconLibrary#WARNING}, {@link IconLibrary#QUESTION} e
     *       {@link IconLibrary#INFORMATION} para avisos, confirmaciones y
     *       mensajes informativos.</li>
     *   <li>Los {@link JComboBox} (combos de filtro/formulario): color de
     *       selección de la lista desplegada, a juego con
     *       {@link #styleCombo(JComboBox)}.</li>
     * </ul>
     * Para seguir ajustando el estilo basta con agregar más líneas
     * {@code UIManager.put(...)} aquí; estas son las claves más usadas del
     * look and feel por defecto de Swing (Metal):
     * <ul>
     *   <li>{@code "OptionPane.background"} / {@code "Panel.background"}: fondo del diálogo.</li>
     *   <li>{@code "OptionPane.messageFont"} / {@code "OptionPane.messageForeground"}: tipografía y color del texto.</li>
     *   <li>{@code "OptionPane.buttonFont"}, {@code "Button.background"}, {@code "Button.foreground"}: botones "Sí"/"No"/"Aceptar".</li>
     *   <li>{@code "OptionPane.errorIcon"}, {@code "OptionPane.warningIcon"}, {@code "OptionPane.informationIcon"},
     *       {@code "OptionPane.questionIcon"}: íconos (aceptan un {@link javax.swing.Icon}, p. ej. de {@link IconLibrary}).</li>
     *   <li>{@code "ComboBox.selectionBackground"} / {@code "ComboBox.selectionForeground"}: colores de la opción
     *       resaltada en la lista desplegada de un combo.</li>
     * </ul>
     * Nota: la barra de título del diálogo (borde y botón "X") la dibuja el
     * sistema operativo, no Swing, así que no se puede recolorear desde
     * aquí. Si se necesita también controlar esa barra, la alternativa es
     * reemplazar los métodos de {@code DialogHelper} por diálogos propios
     * (un {@link javax.swing.JDialog} a medida, como {@link FormDialog})
     * en vez de {@code JOptionPane}.
     */
    public static void installSwingTheme() {
        UIManager.put("OptionPane.background", BACKGROUND);
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("OptionPane.messageFont", FIELD_FONT);
        UIManager.put("OptionPane.messageForeground", TEXT_BLUE);
        UIManager.put("OptionPane.buttonFont", BUTTON_FONT);
        UIManager.put("Button.background", ACCENT_BLUE);
        UIManager.put("Button.foreground", WHITE);
        UIManager.put("OptionPane.warningIcon", IconLibrary.get(IconLibrary.WARNING, OPTION_ICON_SIZE));
        UIManager.put("OptionPane.questionIcon", IconLibrary.get(IconLibrary.QUESTION, OPTION_ICON_SIZE));
        UIManager.put("OptionPane.informationIcon", IconLibrary.get(IconLibrary.INFORMATION, OPTION_ICON_SIZE));
        UIManager.put("OptionPane.errorIcon", IconLibrary.get(IconLibrary.REMOVE, OPTION_ICON_SIZE));

        UIManager.put("ComboBox.selectionBackground", ACCENT_BLUE);
        UIManager.put("ComboBox.selectionForeground", WHITE);
        UIManager.put("ComboBox.background", WHITE);
        UIManager.put("ComboBox.buttonBackground", WHITE);
    }

    private static void stripChrome(JButton button) {
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setMargin(new java.awt.Insets(0, 0, 0, 0));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}

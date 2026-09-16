package View;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Selector de fecha en forma de mini-calendario navegable por mes, con la
 * misma estética que el resto de la aplicación (colores/tipografía de
 * {@link UITheme}). Se abre junto al botón "▾" de un campo de fecha
 * bloqueado con {@link UITheme#lockAsPickerOnly}; al elegir un día se
 * cierra solo y devuelve la fecha elegida, o {@code null} si se canceló.
 * <p>
 * Admite un rango [{@code min}, {@code max}] opcional (cualquiera de los
 * dos puede ser {@code null}): los días fuera de ese rango se muestran
 * deshabilitados, para que no se pueda elegir, por ejemplo, una fecha de
 * fin anterior a la fecha de inicio ya escogida (o viceversa).
 */
public final class DatePickerDialog extends JDialog {

    private static final Locale LOCALE = Locale.of("es", "ES");
    private static final int CELL_SIZE = 34;
    private static final Color DISABLED_DAY = new Color(203, 213, 225);

    private final LocalDate min;
    private final LocalDate max;
    private LocalDate viewedMonth;
    private LocalDate selected;

    private final JLabel monthLabel = new JLabel();
    private final JPanel daysGrid = new JPanel(new GridLayout(0, 7, 4, 4));

    private DatePickerDialog(Window owner, LocalDate initial, LocalDate min, LocalDate max) {
        super(owner, "Seleccionar fecha", ModalityType.APPLICATION_MODAL);
        this.min = min;
        this.max = max;
        this.selected = initial;

        LocalDate reference = initial != null ? initial : LocalDate.now();
        if (min != null && reference.isBefore(min)) {
            reference = min;
        }
        if (max != null && reference.isAfter(max)) {
            reference = max;
        }
        this.viewedMonth = reference.withDayOfMonth(1);

        initComponents();
        // Poblar la grilla ANTES de pack(): si se empaqueta la ventana con
        // daysGrid todavía vacío (GridLayout sin hijos mide alto 0), el
        // diálogo queda más chico de lo necesario y los botones de día
        // terminan renderizados fuera del área visible/clicable.
        renderMonth();
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(getOwner());
    }

    /**
     * Muestra el selector y devuelve la fecha elegida, o {@code null} si el
     * usuario canceló. {@code min}/{@code max} (cualquiera puede ser
     * {@code null}) acotan las fechas seleccionables.
     */
    public static LocalDate show(Window owner, LocalDate initial, LocalDate min, LocalDate max) {
        DatePickerDialog dialog = new DatePickerDialog(owner, initial, min, max);
        dialog.setVisible(true);
        return dialog.selected;
    }

    private void initComponents() {
        setResizable(false);

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(UITheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 16, 20));
        setContentPane(content);

        content.add(buildHeader(), BorderLayout.NORTH);
        content.add(buildCalendar(), BorderLayout.CENTER);
        content.add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JButton prevButton = UITheme.createPickerButton("‹");
        prevButton.setToolTipText("Mes anterior");
        prevButton.addActionListener(e -> {
            viewedMonth = viewedMonth.minusMonths(1);
            renderMonth();
        });

        JButton nextButton = UITheme.createPickerButton("›");
        nextButton.setToolTipText("Mes siguiente");
        nextButton.addActionListener(e -> {
            viewedMonth = viewedMonth.plusMonths(1);
            renderMonth();
        });

        monthLabel.setFont(UITheme.SECTION_FONT);
        monthLabel.setForeground(UITheme.TEXT_BLUE);
        monthLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(prevButton, BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(nextButton, BorderLayout.EAST);
        return header;
    }

    private JPanel buildCalendar() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 6));
        wrapper.setOpaque(false);

        JPanel weekdays = new JPanel(new GridLayout(1, 7, 4, 0));
        weekdays.setOpaque(false);
        DayOfWeek[] order = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY};
        for (DayOfWeek day : order) {
            JLabel label = new JLabel(day.getDisplayName(TextStyle.NARROW, LOCALE).toUpperCase(LOCALE));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(UITheme.LABEL_FONT);
            label.setForeground(UITheme.NOTE_GRAY);
            weekdays.add(label);
        }
        wrapper.add(weekdays, BorderLayout.NORTH);

        daysGrid.setOpaque(false);
        wrapper.add(daysGrid, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildFooter() {
        JButton cancelButton = UITheme.createStackedIconButton("Cancelar", IconLibrary.REMOVE, 22);
        cancelButton.addActionListener(e -> {
            selected = null;
            dispose();
        });

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        footer.setOpaque(false);
        footer.add(cancelButton);
        return footer;
    }

    private void renderMonth() {
        monthLabel.setText(capitalize(viewedMonth.getMonth().getDisplayName(TextStyle.FULL, LOCALE))
                + " " + viewedMonth.getYear());

        daysGrid.removeAll();
        YearMonth yearMonth = YearMonth.from(viewedMonth);
        int leadingBlanks = viewedMonth.getDayOfWeek().getValue() - 1; // Lunes=1 -> 0 blancos
        int totalCells = 42; // 6 filas x 7 columnas: tamaño fijo para que el diálogo no cambie de alto entre meses

        for (int i = 0; i < totalCells; i++) {
            int dayNumber = i - leadingBlanks + 1;
            if (dayNumber < 1 || dayNumber > yearMonth.lengthOfMonth()) {
                daysGrid.add(new JLabel());
                continue;
            }
            daysGrid.add(buildDayButton(viewedMonth.withDayOfMonth(dayNumber)));
        }

        daysGrid.revalidate();
        daysGrid.repaint();
    }

    private JButton buildDayButton(LocalDate date) {
        boolean allowed = isAllowed(date);
        boolean isToday = date.equals(LocalDate.now());

        JButton button = new JButton(String.valueOf(date.getDayOfMonth()));
        button.setFont(UITheme.FIELD_FONT);
        button.setPreferredSize(new Dimension(CELL_SIZE, CELL_SIZE));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFocusPainted(false);

        if (!allowed) {
            button.setEnabled(false);
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setForeground(DISABLED_DAY);
            return button;
        }

        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        if (date.equals(selected)) {
            button.setBackground(UITheme.ACCENT_BLUE);
            button.setForeground(UITheme.WHITE);
            button.setBorderPainted(false);
        } else {
            button.setBackground(UITheme.WHITE);
            button.setForeground(UITheme.TEXT_BLUE);
            button.setBorder(BorderFactory.createLineBorder(isToday ? UITheme.ACCENT_BLUE : UITheme.BACKGROUND));
        }
        button.addActionListener(e -> {
            selected = date;
            dispose();
        });
        return button;
    }

    private boolean isAllowed(LocalDate date) {
        return (min == null || !date.isBefore(min)) && (max == null || !date.isAfter(max));
    }

    private static String capitalize(String text) {
        if (text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}

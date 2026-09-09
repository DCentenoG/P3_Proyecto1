package View;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Constantes y métodos de apoyo para dar una apariencia consistente a las
 * pantallas de autenticación (LoginFrame y PasswordChangeForm), siguiendo
 * los colores y la tipografía definidos en el mockup del proyecto.
 */
public final class FormStyle {

    public static final Color ACCENT = new Color(51, 102, 238);
    public static final Color BACKGROUND = new Color(250, 250, 251);
    public static final Color FIELD_BORDER = new Color(190, 190, 195);

    public static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FIELD_FONT = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 13);

    private FormStyle() {
        // Clase de utilidades: no debe instanciarse.
    }

    /** Crea una etiqueta azul y en negrita, como las de "ID:" o "Clave:" en el mockup. */
    public static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setForeground(ACCENT);
        return label;
    }

    /** Aplica el borde y la tipografía de los campos de texto/clave del mockup. */
    public static void styleField(JTextField field) {
        field.setFont(FIELD_FONT);
        field.setPreferredSize(new Dimension(280, 30));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }

    /**
     * Crea uno de los botones circulares del mockup (flecha, check o X).
     * Si {@code text} es nulo o vacío, el botón se muestra solo con el
     * ícono, sin texto debajo (como en PasswordChangeForm).
     */
    public static JButton createIconButton(String text, CircleIcon.Symbol symbol, int iconSize) {
        boolean hasText = text != null && !text.isEmpty();
        JButton button = hasText
                ? new JButton(text, new CircleIcon(symbol, iconSize, ACCENT))
                : new JButton(new CircleIcon(symbol, iconSize, ACCENT));

        if (hasText) {
            button.setVerticalTextPosition(SwingConstants.BOTTOM);
            button.setHorizontalTextPosition(SwingConstants.CENTER);
            button.setIconTextGap(8);
            button.setFont(BUTTON_FONT);
            button.setForeground(ACCENT);
        }

        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
}

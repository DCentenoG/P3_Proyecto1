package View;

import javax.swing.JButton;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * Botón rectangular de esquinas redondeadas, relleno de color sólido con
 * ícono blanco y texto, usado para las acciones independientes "Editar"
 * (azul) y "Borrar" (rojo) del listado de {@link CRUDView}: el usuario
 * selecciona una fila de la tabla y luego pulsa uno de estos botones,
 * en lugar de tener un ícono de acción pegado a cada fila.
 */
final class RoundedActionButton extends JButton {

    private static final int ARC = 14;

    private final Color fill;

    RoundedActionButton(String text, String iconFile, Color fill) {
        super(text, IconLibrary.get(iconFile, 16));
        this.fill = fill;
        setFont(UITheme.BUTTON_FONT);
        setForeground(UITheme.WHITE);
        setHorizontalTextPosition(SwingConstants.RIGHT);
        setVerticalTextPosition(SwingConstants.CENTER);
        setIconTextGap(8);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setMargin(new Insets(8, 22, 8, 22));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
        g2.dispose();
        super.paintComponent(g);
    }
}

package View;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Icono circular reutilizable para los botones de acción de las pantallas
 * de autenticación (LoginFrame y PasswordChangeForm). Dibuja un círculo
 * delineado con una flecha, una marca de verificación o una X en su
 * interior, replicando el estilo de íconos del mockup del proyecto sin
 * depender de imágenes externas.
 */
public class CircleIcon implements Icon {

    /** Símbolos disponibles para dibujar dentro del círculo. */
    public enum Symbol { ARROW_RIGHT, CHECK, CLOSE }

    private final Symbol symbol;
    private final int size;
    private final Color color;
    private final float strokeWidth;

    public CircleIcon(Symbol symbol, int size, Color color) {
        this.symbol = symbol;
        this.size = size;
        this.color = color;
        this.strokeWidth = Math.max(2f, size / 20f);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int margin = (int) Math.ceil(strokeWidth) + 1;
        int diameter = size - margin * 2;
        g2.drawOval(x + margin, y + margin, diameter, diameter);

        int cx = x + size / 2;
        int cy = y + size / 2;
        int r = diameter / 2;

        switch (symbol) {
            case ARROW_RIGHT:
                drawArrow(g2, cx, cy, r);
                break;
            case CHECK:
                drawCheck(g2, cx, cy, r);
                break;
            case CLOSE:
                drawClose(g2, cx, cy, r);
                break;
        }
        g2.dispose();
    }

    private void drawArrow(Graphics2D g2, int cx, int cy, int r) {
        int shaftHalf = (int) (r * 0.45);
        int tipX = cx + shaftHalf;
        g2.drawLine(cx - shaftHalf, cy, tipX, cy);

        int headSize = (int) (r * 0.5);
        g2.drawLine(tipX, cy, tipX - headSize, cy - headSize);
        g2.drawLine(tipX, cy, tipX - headSize, cy + headSize);
    }

    private void drawCheck(Graphics2D g2, int cx, int cy, int r) {
        int[] xs = { cx - (int) (r * 0.5), cx - (int) (r * 0.1), cx + (int) (r * 0.55) };
        int[] ys = { cy + (int) (r * 0.05), cy + (int) (r * 0.45), cy - (int) (r * 0.35) };
        g2.drawPolyline(xs, ys, 3);
    }

    private void drawClose(Graphics2D g2, int cx, int cy, int r) {
        int offset = (int) (r * 0.5);
        g2.drawLine(cx - offset, cy - offset, cx + offset, cy + offset);
        g2.drawLine(cx + offset, cy - offset, cx - offset, cy + offset);
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }
}

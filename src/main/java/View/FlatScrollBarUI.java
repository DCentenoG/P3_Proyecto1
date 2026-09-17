package View;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * Delegado de UI para las barras de desplazamiento (JScrollBar) de todos
 * los {@link javax.swing.JScrollPane} de la aplicación (tablas, la lista
 * de categorías de {@link ReservationsView}, las celdas de
 * calendarización, etc.). Reemplaza las flechas y el relieve gris con
 * degradado que dibuja por defecto el Look &amp; Feel por una barra plana
 * y delgada -sin botones de flecha-, con un pulgar redondeado que usa los
 * mismos colores del resto de la interfaz ({@link UITheme#FIELD_BORDER} en
 * reposo, {@link UITheme#ACCENT_BLUE} al pasar el mouse), para que se vea
 * uniforme sin importar el sistema operativo. Se instala centralizadamente
 * desde {@link UITheme#styleScrollPane(javax.swing.JScrollPane)}.
 */
final class FlatScrollBarUI extends BasicScrollBarUI {

    private static final Color TRACK_COLOR = UITheme.BACKGROUND;
    private static final Color THUMB_COLOR = new Color(180, 190, 205);
    private static final Color THUMB_HOVER_COLOR = UITheme.ACCENT_BLUE;
    private static final int THUMB_INSET = 3;

    @Override
    protected void configureScrollBarColors() {
        // Los colores reales se aplican en paintTrack/paintThumb; se deja vacío
        // para que BasicScrollBarUI no instale los del Look & Feel por defecto.
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return zeroSizeButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return zeroSizeButton();
    }

    private JButton zeroSizeButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        g.setColor(TRACK_COLOR);
        g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !c.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isThumbRollover() ? THUMB_HOVER_COLOR : THUMB_COLOR);
        int arc = Math.min(thumbBounds.width, thumbBounds.height);
        g2.fillRoundRect(thumbBounds.x + THUMB_INSET, thumbBounds.y + THUMB_INSET,
                thumbBounds.width - THUMB_INSET * 2, thumbBounds.height - THUMB_INSET * 2, arc, arc);
        g2.dispose();
    }

    @Override
    protected Dimension getMinimumThumbSize() {
        return new Dimension(12, 12);
    }
}

package View;

import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

/**
 * Delegado de UI para el {@link javax.swing.JTabbedPane} de {@link MainFrame}.
 * Elimina el relieve/degradado y los bordes que dibuja por defecto el Look
 * &amp; Feel, dejando una franja de tabs completamente plana: el color de
 * cada tab lo pinta su propio componente ({@code TabComponent}), no esta
 * clase. Así se logra la estética minimalista del mockup sin importar el
 * Look &amp; Feel del sistema operativo.
 */
final class FlatTabbedPaneUI extends BasicTabbedPaneUI {

    @Override
    protected void installDefaults() {
        super.installDefaults();
        tabInsets = new Insets(0, 0, 0, 0);
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
        tabAreaInsets = new Insets(0, 0, 0, 0);
        contentBorderInsets = new Insets(0, 0, 0, 0);
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                   int x, int y, int w, int h, boolean isSelected) {
        // Sin borde: los tabs son rectángulos planos.
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                       int x, int y, int w, int h, boolean isSelected) {
        // El color real (activo/inactivo) lo pinta el TabComponent de cada tab;
        // aquí solo se evita el relieve por defecto del Look & Feel.
        g.setColor(UITheme.BACKGROUND);
        g.fillRect(x, y, w, h);
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        // Sin caja alrededor del área de contenido.
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex,
                                        Rectangle iconRect, Rectangle textRect, boolean isSelected) {
        // Sin indicador de foco visible.
    }
}

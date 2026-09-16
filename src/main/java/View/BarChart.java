package View;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gráfico de barras genérico usado por {@link StatisticsView} para
 * interpretar visualmente tanto la tabla de recursos (Categoría→
 * Cantidad) como la de actividades (Semana→Cantidad). Solo dibuja los
 * datos que se le entreguen mediante {@link #setData}; el cálculo de
 * esos datos a partir de las reservas/actividades reales se conecta
 * desde el manejo de eventos.
 */
public class BarChart extends JComponent {

    private static final Color AXIS_COLOR = new Color(148, 163, 184);

    private Map<String, Integer> data = new LinkedHashMap<>();

    public BarChart() {
        setOpaque(true);
        setBackground(UITheme.WHITE);
        setPreferredSize(new Dimension(260, 190));
    }

    /** Reemplaza los datos mostrados (etiqueta → cantidad, en el orden en que deben dibujarse). */
    public void setData(Map<String, Integer> data) {
        this.data = (data == null) ? new LinkedHashMap<>() : data;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        g2.setColor(getBackground());
        g2.fillRect(0, 0, width, height);

        if (data.isEmpty()) {
            paintEmptyMessage(g2, width, height);
            g2.dispose();
            return;
        }

        int paddingLeft = 12;
        int paddingRight = 12;
        int paddingTop = 20;
        int paddingBottom = 32;
        int chartWidth = width - paddingLeft - paddingRight;
        int chartHeight = height - paddingTop - paddingBottom;

        int max = Math.max(1, data.values().stream().max(Integer::compareTo).orElse(1));

        g2.setColor(AXIS_COLOR);
        g2.drawLine(paddingLeft, paddingTop + chartHeight, paddingLeft + chartWidth, paddingTop + chartHeight);

        int barCount = data.size();
        int gap = 14;
        int barWidth = Math.max(8, (chartWidth - gap * (barCount + 1)) / barCount);

        g2.setFont(UITheme.TABLE_FONT);
        FontMetrics metrics = g2.getFontMetrics();

        int x = paddingLeft + gap;
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            int value = entry.getValue();
            int barHeight = (int) Math.round((value / (double) max) * (chartHeight - 18));
            int y = paddingTop + chartHeight - barHeight;

            g2.setColor(UITheme.ACCENT_BLUE);
            g2.fillRoundRect(x, y, barWidth, Math.max(barHeight, 2), 5, 5);

            String valueLabel = String.valueOf(value);
            g2.setColor(UITheme.TEXT_BLUE);
            g2.drawString(valueLabel, x + (barWidth - metrics.stringWidth(valueLabel)) / 2f, y - 4);

            String label = ellipsize(entry.getKey(), metrics, barWidth + gap - 4);
            g2.drawString(label, x + (barWidth - metrics.stringWidth(label)) / 2f, paddingTop + chartHeight + 16);

            x += barWidth + gap;
        }

        g2.dispose();
    }

    private void paintEmptyMessage(Graphics2D g2, int width, int height) {
        g2.setColor(UITheme.TEXT_BLUE);
        g2.setFont(UITheme.FIELD_FONT);
        String message = "Sin datos para el período seleccionado";
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(message, (width - metrics.stringWidth(message)) / 2f, height / 2f);
    }

    private static String ellipsize(String text, FontMetrics metrics, int maxWidth) {
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        String shortened = text;
        while (shortened.length() > 1 && metrics.stringWidth(shortened + "…") > maxWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened + "…";
    }
}

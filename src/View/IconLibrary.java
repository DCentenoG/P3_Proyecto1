package View;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Carga y escala los íconos reales del proyecto, ubicados en
 * {@code /Resources/Icons}. Sustituye cualquier ícono dibujado por
 * código: toda la capa View debe usar esta clase para obtener sus
 * {@link ImageIcon}, nunca generar íconos por su cuenta.
 * <p>
 * Los íconos ya vienen listos en dos variantes según el nombre de
 * archivo: la versión "azul" (p. ej. {@code calendar.png}), pensada
 * para fondos claros, y la versión "blanca" (sufijo {@code W}, p. ej.
 * {@code calendarW.png}), pensada para usarse sobre fondos de color
 * (tab activo, botones de acción). Las instancias ya escaladas se
 * cachean para no repetir el escalado de las imágenes fuente (512x512).
 */
public final class IconLibrary {

    private static final String BASE_PATH = "/Resources/Icons/";
    private static final Map<String, ImageIcon> CACHE = new ConcurrentHashMap<>();

    // Nombres de archivo de los íconos disponibles en /Resources/Icons.
    public static final String CALENDAR = "calendar.png";
    public static final String CALENDAR_WHITE = "calendarW.png";
    public static final String CATEGORIES = "categories.png";
    public static final String CATEGORIES_WHITE = "categoriesW.png";
    public static final String CHECKED = "checked.png";
    public static final String CLIPBOARD = "clipboard.png";
    public static final String CLIPBOARD_WHITE = "clipboardW.png";
    public static final String DEVICE = "device.png";
    public static final String DEVICE_WHITE = "deviceW.png";
    public static final String DISKETTE = "diskette.png";
    public static final String ERASER = "eraser.png";
    public static final String GENERATIVE = "generative.png";
    public static final String GROUP = "group.png";
    public static final String GROUP_WHITE = "groupW.png";
    public static final String LIST = "list.png";
    public static final String LIST_WHITE = "listW.png";
    public static final String LOGOUT = "logout.png";
    public static final String NEXT = "next.png";
    public static final String PADLOCK = "padlock.png";
    public static final String PENCIL_WHITE = "pencilW.png";
    public static final String PLUS_WHITE = "plus.png";
    public static final String PRINTER = "printer.png";
    public static final String REMOVE = "remove.png";
    public static final String SEARCH = "search.png";
    public static final String STATISTICS = "statistics.png";
    public static final String STATISTICS_WHITE = "statisticsW.png";
    public static final String TRASH_WHITE = "trash-canW.png";

    private IconLibrary() {
        // Clase de utilidades: no debe instanciarse.
    }

    /** Devuelve el ícono {@code fileName} escalado a un cuadrado de {@code size} px. */
    public static ImageIcon get(String fileName, int size) {
        String key = fileName + '@' + size;
        return CACHE.computeIfAbsent(key, k -> load(fileName, size));
    }

    private static ImageIcon load(String fileName, int size) {
        URL url = IconLibrary.class.getResource(BASE_PATH + fileName);
        if (url == null) {
            throw new IllegalArgumentException("No se encontró el ícono: " + BASE_PATH + fileName);
        }
        Image source = new ImageIcon(url).getImage();

        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        var g2 = scaled.createGraphics();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(source, 0, 0, size, size, null);
        g2.dispose();

        return new ImageIcon(scaled);
    }
}

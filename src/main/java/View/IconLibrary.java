package View;

import javax.swing.ImageIcon;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
    public static final String SEARCH_BLUE = "search.png";
    public static final String SEARCH_BLACK = "searchB.png";
    public static final String STATISTICS = "statistics.png";
    public static final String STATISTICS_WHITE = "statisticsW.png";
    public static final String TRASH_WHITE = "trash-canW.png";
    public static final String WARNING = "warning.png";
    public static final String QUESTION = "question.png";
    public static final String INFORMATION = "information.png";
    public static final String LOGO = "logo.png";

    /** Tamaños generados para el ícono de la aplicación (ventana y barra de tareas). */
    private static final int[] APP_ICON_SIZES = {16, 24, 32, 48, 64, 128, 256};

    private IconLibrary() {
        // Clase de utilidades: no debe instanciarse.
    }

    /** Devuelve el ícono {@code fileName} escalado a un cuadrado de {@code size} px. */
    public static ImageIcon get(String fileName, int size) {
        String key = fileName + '@' + size;
        return CACHE.computeIfAbsent(key, k -> load(fileName, size));
    }

    /**
     * Devuelve el logo de la aplicación ({@code logo.png}) en varias
     * resoluciones, pensado para {@link javax.swing.JFrame#setIconImages}:
     * el sistema operativo elige automáticamente la variante más nítida
     * según dónde se use (ícono de la ventana, barra de tareas, alt-tab).
     */
    public static List<Image> getAppIconImages() {
        List<Image> images = new ArrayList<>();
        for (int size : APP_ICON_SIZES) {
            images.add(get(LOGO, size).getImage());
        }
        return images;
    }

    private static ImageIcon load(String fileName, int size) {
        URL url = IconLibrary.class.getResource(BASE_PATH + fileName);
        if (url == null) {
            throw new IllegalArgumentException("No se encontró el ícono: " + BASE_PATH + fileName);
        }
        Image source = new ImageIcon(url).getImage();
        return new ImageIcon(highQualityScale(source, size, size));
    }

    /**
     * Reduce la imagen fuente (512x512) al tamaño pedido en pasos sucesivos
     * de mitad en mitad, en vez de un único escalado directo. Un solo paso
     * de {@code drawImage} con reducciones tan grandes (p. ej. de 512 a
     * 18-22 px, usadas en los tabs/botones) hace que el interpolador
     * bilineal descarte la mayoría de los píxeles fuente, lo que se ve
     * como un ícono "pixelado"/con bordes ásperos. Reduciendo a la mitad
     * en cada paso, cada píxel de salida siempre promedia una vecindad
     * pequeña y representativa de la imagen anterior, dando un resultado
     * mucho más nítido (técnica estándar para miniaturas de alta calidad
     * en Java 2D).
     */
    private static BufferedImage highQualityScale(Image source, int targetWidth, int targetHeight) {
        BufferedImage current = toBufferedImage(source);
        int width = current.getWidth();
        int height = current.getHeight();

        while (width / 2 > targetWidth && height / 2 > targetHeight) {
            width = Math.max(width / 2, targetWidth);
            height = Math.max(height / 2, targetHeight);
            current = scaleStep(current, width, height);
        }
        return scaleStep(current, targetWidth, targetHeight);
    }

    private static BufferedImage scaleStep(Image source, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(source, 0, 0, width, height, null);
        g2.dispose();
        return scaled;
    }

    private static BufferedImage toBufferedImage(Image source) {
        if (source instanceof BufferedImage bufferedImage) {
            return bufferedImage;
        }
        return scaleStep(source, source.getWidth(null), source.getHeight(null));
    }
}

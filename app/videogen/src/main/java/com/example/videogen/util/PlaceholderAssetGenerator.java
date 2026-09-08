package com.example.videogen.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Generates a simple placeholder still image carrying the visual prompt text.
 * Used as a fallback when a real image/video generator is unavailable or disabled,
 * so the rest of the pipeline (assembly, subtitles) can still run end-to-end.
 */
public final class PlaceholderAssetGenerator {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

    private PlaceholderAssetGenerator() {
    }

    public static Path generatePlaceholderImage(Path outputFile, String label) throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(30, 41, 59));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(new Color(226, 232, 240));
            g.setFont(new Font("SansSerif", Font.BOLD, 32));
            drawWrapped(g, label == null ? "" : label, 60, 120, WIDTH - 120);
        } finally {
            g.dispose();
        }
        Files.createDirectories(outputFile.getParent());
        ImageIO.write(image, "png", outputFile.toFile());
        return outputFile;
    }

    private static void drawWrapped(Graphics2D g, String text, int x, int y, int maxWidth) {
        List<String> lines = wrap(g, text, maxWidth);
        int lineHeight = g.getFontMetrics().getHeight();
        int curY = y;
        for (String line : lines) {
            g.drawString(line, x, curY);
            curY += lineHeight + 8;
        }
    }

    private static List<String> wrap(Graphics2D g, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (g.getFontMetrics().stringWidth(candidate) > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }
}

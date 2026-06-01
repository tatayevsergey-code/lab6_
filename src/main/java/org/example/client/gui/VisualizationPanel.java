package org.example.client.gui;

import org.example.common.models.Organization;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class VisualizationPanel extends JPanel {
    private final List<Organization> organizations = new ArrayList<>();
    private final Map<String, Color> userColors = new ConcurrentHashMap<>();
    private final Random colorRandom = new Random(42); // Фиксированный seed для стабильных цветов

    public VisualizationPanel() {
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    public void setData(Collection<Organization> data) {
        organizations.clear();
        if (data != null) {
            organizations.addAll(data);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (organizations.isEmpty()) {
            drawEmptyState(g2);
            return;
        }

        int width = getWidth();
        int height = getHeight();
        int padding = 40;
        int legendWidth = 150;

        // 1. Вычисляем границы координат для масштабирования
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        float maxTurnover = Float.MIN_VALUE;

        for (Organization org : organizations) {
            if (org.getCoordinates() != null) {
                double x = org.getCoordinates().getX();
                long y = org.getCoordinates().getY() != 0 ? org.getCoordinates().getY() : 0;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
            maxTurnover = Math.max(maxTurnover, org.getAnnualTurnover());
        }

        // Защита от деления на ноль при одном объекте
        if (maxX == minX) { maxX += 1; minX -= 1; }
        if (maxY == minY) { maxY += 1; minY -= 1; }

        double rangeX = maxX - minX;
        double rangeY = maxY - minY;

        // 2. Масштабирование с сохранением пропорций
        double scale = Math.min(
                (width - padding * 2 - legendWidth) / rangeX,
                (height - padding * 2) / rangeY
        );

        // 3. Рисуем сетку и оси
        drawGrid(g2, width, height, padding, legendWidth, minX, maxX, minY, maxY, scale);

        // 4. Рисуем объекты
        int centerX = width - legendWidth - padding;
        int centerY = height - padding;

        for (Organization org : organizations) {
            if (org.getCoordinates() == null) continue;

            double x = org.getCoordinates().getX();
            long y = org.getCoordinates().getY() != 0 ? org.getCoordinates().getY() : 0;
            float turnover = org.getAnnualTurnover();

            // Преобразуем координаты в пиксели (инвертируем Y для экранной системы)
            int pixelX = (int) (padding + (x - minX) * scale);
            int pixelY = (int) (centerY - (y - minY) * scale);

            // Размер фигуры пропорционален обороту (логарифмическая шкала для наглядности)
            int radius = Math.max(8, (int) (Math.log1p(turnover) * 4));

            // Цвет по пользователю
            Color color = getUserColor(org.getUsername());

            // Рисуем объект
            g2.setColor(color);
            g2.fillOval(pixelX - radius, pixelY - radius, radius * 2, radius * 2);

            // Обводка
            g2.setColor(Color.DARK_GRAY);
            g2.drawOval(pixelX - radius, pixelY - radius, radius * 2, radius * 2);

            // Подпись (название)
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            String label = org.getName() != null && org.getName().length() > 12
                    ? org.getName().substring(0, 10) + ".."
                    : (org.getName() != null ? org.getName() : "N/A");
            g2.drawString(label, pixelX - radius, pixelY - radius - 4);
        }

        // 5. Рисуем легенду
        drawLegend(g2, width, height, padding, legendWidth);
    }

    private void drawEmptyState(Graphics2D g2) {
        g2.setColor(Color.GRAY);
        g2.setFont(new Font("SansSerif", Font.ITALIC, 16));
        String text = "📊 Нет данных для отображения";
        FontMetrics fm = g2.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(text)) / 2;
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, x, y);
    }

    private void drawGrid(Graphics2D g2, int width, int height, int padding, int legendWidth,
                          double minX, double maxX, double minY, double maxY, double scale) {
        g2.setColor(new Color(240, 240, 240));
        g2.setStroke(new BasicStroke(1f));

        // Вертикальные линии
        int steps = 5;
        for (int i = 0; i <= steps; i++) {
            double val = minX + (maxX - minX) * i / steps;
            int x = (int) (padding + (val - minX) * scale);
            g2.drawLine(x, padding, x, height - padding);

            // Подпись оси X
            g2.setColor(Color.GRAY);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.drawString(String.valueOf((int) val), x - 10, height - padding + 15);
            g2.setColor(new Color(240, 240, 240));
        }

        // Горизонтальные линии
        for (int i = 0; i <= steps; i++) {
            double val = minY + (maxY - minY) * i / steps;
            int y = (int) (height - padding - (val - minY) * scale);
            g2.drawLine(padding, y, width - padding - legendWidth, y);

            // Подпись оси Y
            g2.setColor(Color.GRAY);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.drawString(String.valueOf((int) val), padding - 25, y + 4);
            g2.setColor(new Color(240, 240, 240));
        }
    }

    private void drawLegend(Graphics2D g2, int width, int height, int padding, int legendWidth) {
        int legendX = width - legendWidth + 5;
        int legendY = padding;

        g2.setColor(new Color(250, 250, 250));
        g2.fillRect(legendX - 10, legendY - 10, legendWidth + 5, height - padding * 2 + 20);
        g2.setColor(Color.DARK_GRAY);
        g2.drawRect(legendX - 10, legendY - 10, legendWidth + 5, height - padding * 2 + 20);

        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.drawString("Пользователи:", legendX, legendY + 10);

        int yPos = legendY + 25;
        Set<String> uniqueUsers = new LinkedHashSet<>();
        for (Organization org : organizations) {
            if (org.getUsername() != null) uniqueUsers.add(org.getUsername());
        }

        for (String user : uniqueUsers) {
            g2.setColor(getUserColor(user));
            g2.fillOval(legendX, yPos, 12, 12);
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.drawString(user, legendX + 18, yPos + 10);
            yPos += 20;
        }
    }

    private Color getUserColor(String username) {
        if (username == null) return Color.GRAY;
        return userColors.computeIfAbsent(username, k -> {
            int hash = k.hashCode();
            // Генерируем приятные, различимые цвета
            int r = Math.abs(hash) % 180 + 40;
            int g = Math.abs(hash >> 8) % 180 + 40;
            int b = Math.abs(hash >> 16) % 180 + 40;
            return new Color(r, g, b);
        });
    }
}

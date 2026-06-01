package org.example.client.gui;

import org.example.client.network.ClientNetworkManager;
import org.example.common.models.Organization;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class CollectionTableFrame extends JFrame {
    private final ClientNetworkManager networkManager;
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JPanel controlPanel;
    private VisualizationPanel visualizationPanel;
    private JComboBox<Locale> localeCombo;

    // ✅ Ссылки на компоненты для безопасного обновления
    private JLabel langLabel;
    private String username;

    // 🔹 Новые поля для фильтрации
    private Collection<Organization> originalCollection;
    private JPanel filterPanel;
    private JLabel minLabel, maxLabel;
    private JTextField minTurnoverField, maxTurnoverField;
    private JButton applyFilterButton, resetFilterButton;

    private ResourceBundle bundle;
    private Locale currentLocale;
    private final List<JButton> commandButtons = new ArrayList<>();

    public CollectionTableFrame(Collection<Organization> collection, String username, ClientNetworkManager networkManager) {
        this.networkManager = networkManager;
        this.username = username;
        this.originalCollection = collection; // Сохраняем исходные данные для фильтрации

        currentLocale = new Locale("ru");
        bundle = loadBundle(currentLocale);

        setTitle(bundle.getString("frame.title").replace("{user}", username));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1400, 850);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));

        // 1. Модель таблицы
        tableModel = new DefaultTableModel(getColumnNames(), 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 0 -> Long.class;
                    case 1, 6, 7, 8 -> String.class;
                    case 2, 3, 5 -> Double.class;
                    case 4 -> LocalDate.class;
                    default -> Object.class;
                };
            }
        };

        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setRowHeight(28);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));

        // Сортировщик
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        add(scrollPane, BorderLayout.CENTER);

        // 2. Панель управления
        controlPanel = createControlPanel();
        controlPanel.setPreferredSize(new Dimension(240, getHeight()));
        add(controlPanel, BorderLayout.WEST);

        // 3. Визуализация
        visualizationPanel = createVisualizationPanel();
        visualizationPanel.setPreferredSize(new Dimension(getWidth(), 180));
        add(visualizationPanel, BorderLayout.SOUTH);

        populateTable(collection);

        // Центрирование ячеек
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    private ResourceBundle loadBundle(Locale locale) {
        try {
            return ResourceBundle.getBundle("Messages", locale);
        } catch (MissingResourceException e) {
            return new ResourceBundle() {
                @Override public Object handleGetObject(String key) { return getFallbackTranslation(key); }
                @Override public Enumeration<String> getKeys() { return Collections.emptyEnumeration(); }
            };
        }
    }

    private String getFallbackTranslation(String key) {
        return switch (key) {
            case "frame.title" -> "Коллекция — {user}";
            case "col.id", "col.name", "col.x", "col.y", "col.date", "col.turnover",
                 "col.type", "col.address", "col.owner" -> key.toUpperCase();
            case "btn.show", "btn.info", "btn.clear", "btn.removeHead", "btn.add", "btn.serverStatus" -> key;
            case "panel.control" -> "Управление";
            case "panel.viz" -> "Визуализация данных";
            case "lang.title" -> "Язык";
            case "filter.title" -> "Фильтр по обороту";
            case "filter.min" -> "Мин.";
            case "filter.max" -> "Макс.";
            case "filter.apply" -> "Применить";
            case "filter.reset" -> "Сброс";
            default -> "[" + key + "]";
        };
    }

    private void changeLocale(Locale newLocale) {
        if (newLocale.equals(currentLocale)) return;
        currentLocale = newLocale;
        bundle = loadBundle(newLocale);
        applyLocale();
    }

    private void applyLocale() {
        System.out.println("🔄 Применяю локаль: " + currentLocale.toLanguageTag());
        try {
            setTitle(bundle.getString("frame.title").replace("{user}", username));
            tableModel.setColumnIdentifiers(getColumnNames());
            table.getTableHeader().resizeAndRepaint();
            table.revalidate();

            commandButtons.forEach(btn -> btn.setText(bundle.getString("btn." + btn.getActionCommand())));
            controlPanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("panel.control")));
            visualizationPanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("panel.viz")));
            langLabel.setText(bundle.getString("lang.title") + ":");

            // Обновление фильтра
            if (filterPanel != null) {
                filterPanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("filter.title")));
                minLabel.setText(bundle.getString("filter.min") + ":");
                maxLabel.setText(bundle.getString("filter.max") + ":");
                applyFilterButton.setText(bundle.getString("filter.apply"));
                resetFilterButton.setText(bundle.getString("filter.reset"));
            }

            getContentPane().revalidate();
            getContentPane().repaint();
            revalidate();
            repaint();
            System.out.println("✅ Локаль успешно применена");
        } catch (MissingResourceException e) {
            System.err.println("❌ ОШИБКА BUNDLE: Не найден ключ '" + e.getKey() + "'");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] getColumnNames() {
        return new String[]{
                bundle.getString("col.id"), bundle.getString("col.name"),
                bundle.getString("col.x"), bundle.getString("col.y"),
                bundle.getString("col.date"), bundle.getString("col.turnover"),
                bundle.getString("col.type"), bundle.getString("col.address"),
                bundle.getString("col.owner")
        };
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        // Кнопки команд
        String[] actions = {"show", "info", "clear", "removeHead", "add", "serverStatus"};
        for (String action : actions) {
            JButton btn = new JButton(bundle.getString("btn." + action));
            btn.setActionCommand(action);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(200, 35));
            btn.addActionListener(e -> handleCommand(action));
            commandButtons.add(btn);
            panel.add(btn);
            panel.add(Box.createVerticalStrut(8));
        }

        panel.add(Box.createVerticalStrut(15));

        // 🔹 ПАНЕЛЬ ФИЛЬТРАЦИИ
        filterPanel = new JPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("filter.title")));
        filterPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        filterPanel.setMaximumSize(new Dimension(200, 130));

        JPanel minPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        minLabel = new JLabel(bundle.getString("filter.min") + ":");
        minTurnoverField = new JTextField(8);
        minPanel.add(minLabel);
        minPanel.add(minTurnoverField);

        JPanel maxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        maxLabel = new JLabel(bundle.getString("filter.max") + ":");
        maxTurnoverField = new JTextField(8);
        maxPanel.add(maxLabel);
        maxPanel.add(maxTurnoverField);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));
        applyFilterButton = new JButton(bundle.getString("filter.apply"));
        resetFilterButton = new JButton(bundle.getString("filter.reset"));
        applyFilterButton.addActionListener(e -> applyFilter());
        resetFilterButton.addActionListener(e -> resetFilter());
        btnPanel.add(applyFilterButton);
        btnPanel.add(resetFilterButton);

        filterPanel.add(minPanel);
        filterPanel.add(maxPanel);
        filterPanel.add(btnPanel);
        panel.add(filterPanel);

        panel.add(Box.createVerticalGlue());

        // Переключатель языка
        JPanel langPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        langLabel = new JLabel(bundle.getString("lang.title") + ":");
        localeCombo = new JComboBox<>(new Locale[]{
                new Locale("ru"), new Locale("nl"), new Locale("pl"), new Locale("en", "IE")
        });
        localeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(((Locale) value).getDisplayName(currentLocale));
                return this;
            }
        });
        localeCombo.setSelectedItem(currentLocale);
        localeCombo.addActionListener(e -> changeLocale((Locale) localeCombo.getSelectedItem()));

        langPanel.add(langLabel);
        langPanel.add(Box.createHorizontalStrut(5));
        langPanel.add(localeCombo);
        panel.add(langPanel);

        return panel;
    }

    private VisualizationPanel createVisualizationPanel() {
        visualizationPanel = new VisualizationPanel();
        visualizationPanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("panel.viz")));
        return visualizationPanel;
    }

    // Добавьте метод для обновления визуализации:
    public void updateVisualization(Collection<Organization> data) {
        if (visualizationPanel != null) {
            visualizationPanel.setData(data);
        }
    }

    private void populateTable(Collection<Organization> collection) {
        tableModel.setRowCount(0);
        if (collection == null) return;

        for (Organization org : collection) {
            double x = org.getCoordinates() != null ? org.getCoordinates().getX() : 0.0;
            double y = org.getCoordinates() != null ? org.getCoordinates().getY() : 0.0;
            String address = org.getOfficialAddress() != null ? org.getOfficialAddress().toString() : "-";

            tableModel.addRow(new Object[]{
                    org.getId(), org.getName(), x, y,
                    org.getCreationDate(), org.getAnnualTurnover(),
                    org.getType(), address, org.getUsername()
            });
        }
    }

    // 🔹 ФИЛЬТРАЦИЯ ЧЕРЕЗ STREAMS API
    private void applyFilter() {
        if (originalCollection == null) return;
        try {
            Float min = minTurnoverField.getText().trim().isEmpty() ? null : Float.parseFloat(minTurnoverField.getText().trim());
            Float max = maxTurnoverField.getText().trim().isEmpty() ? null : Float.parseFloat(maxTurnoverField.getText().trim());

            Collection<Organization> filtered = originalCollection.stream()
                    .filter(org -> {
                        float turnover = org.getAnnualTurnover();
                        if (min != null && turnover < min) return false;
                        if (max != null && turnover > max) return false;
                        return true;
                    })
                    .collect(Collectors.toList());

            populateTable(filtered);
            updateVisualization(filtered);
            System.out.println("🔍 Применён фильтр: " + filtered.size() + " из " + originalCollection.size() + " организаций");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Введите корректные числовые значения", "Ошибка фильтра", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void resetFilter() {
        minTurnoverField.setText("");
        maxTurnoverField.setText("");
        if (originalCollection != null) {
            populateTable(originalCollection);
            updateVisualization(originalCollection);
            System.out.println("🔄 Фильтр сброшен");
        }
    }

    private void handleCommand(String command) {
        System.out.println("[UI Command] " + command + " | Locale: " + currentLocale.toLanguageTag());
        JOptionPane.showMessageDialog(this, "Команда \"" + bundle.getString("btn." + command) + "\" вызвана.", "Управление", JOptionPane.INFORMATION_MESSAGE);
    }
}
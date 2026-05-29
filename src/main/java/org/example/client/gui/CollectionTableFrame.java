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

public class CollectionTableFrame extends JFrame {
    private final ClientNetworkManager networkManager;
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JPanel controlPanel;
    private final JPanel visualizationPanel;
    private JComboBox<Locale> localeCombo;

    // ✅ Сохраняем ссылки на компоненты для безопасного обновления
    private JLabel langLabel;
    private String username;

    private ResourceBundle bundle;
    private Locale currentLocale;
    private final List<JButton> commandButtons = new ArrayList<>();

    public CollectionTableFrame(Collection<Organization> collection, String username, ClientNetworkManager networkManager) {
        this.networkManager = networkManager;
        this.username = username;

        // Инициализация локали и ResourceBundle
        currentLocale = new Locale("ru");
        bundle = loadBundle(currentLocale);

        // Базовые настройки окна
        setTitle(bundle.getString("frame.title").replace("{user}", username));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1400, 850);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));

        // 1. Модель таблицы с типами данных для корректной сортировки
        tableModel = new DefaultTableModel(getColumnNames(), 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 0 -> Long.class;               // ID
                    case 1, 6, 7, 8 -> String.class;    // Название, Тип, Адрес, Владелец
                    case 2, 3, 5 -> Double.class;       // X, Y, Оборот
                    case 4 -> LocalDate.class;          // Дата создания
                    default -> Object.class;
                };
            }
        };

        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setRowHeight(28);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));

        // Настройка сортировщика
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        // По умолчанию сортируем по ID (возрастание)
        sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        add(scrollPane, BorderLayout.CENTER);

        // 2. Левая панель управления
        controlPanel = createControlPanel();
        controlPanel.setPreferredSize(new Dimension(220, getHeight()));
        add(controlPanel, BorderLayout.WEST);

        // 3. Нижняя область визуализации
        visualizationPanel = createVisualizationPanel();
        visualizationPanel.setPreferredSize(new Dimension(getWidth(), 180));
        add(visualizationPanel, BorderLayout.SOUTH);

        // Заполнение таблицы данными
        populateTable(collection);

        // Центрирование для всех типов данных
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
            case "col.id" -> "ID"; case "col.name" -> "Название"; case "col.x" -> "X";
            case "col.y" -> "Y"; case "col.date" -> "Дата"; case "col.turnover" -> "Оборот";
            case "col.type" -> "Тип"; case "col.address" -> "Адрес"; case "col.owner" -> "Владелец";
            case "btn.show" -> "Показать"; case "btn.info" -> "Инфо"; case "btn.clear" -> "Очистить";
            case "btn.removeHead" -> "Удалить голову"; case "btn.add" -> "Добавить";
            case "btn.serverStatus" -> "Статус сервера"; case "panel.control" -> "Управление";
            case "panel.viz" -> "Визуализация данных"; case "lang.title" -> "Язык";
            default -> key;
        };
    }

    private void changeLocale(Locale newLocale) {
        if (newLocale.equals(currentLocale)) return;
        currentLocale = newLocale;
        bundle = loadBundle(newLocale);

        // ActionListener JComboBox уже выполняется в EDT,
        // поэтому вызываем applyLocale напрямую без лишнего invokeLater
        applyLocale();
    }

    private void applyLocale() {
        System.out.println("🔄 Применяю локаль: " + currentLocale.toLanguageTag());

        try {
            // 1. Заголовок окна
            setTitle(bundle.getString("frame.title").replace("{user}", username));

            // 2. Таблица
            tableModel.setColumnIdentifiers(getColumnNames());
            table.getTableHeader().resizeAndRepaint(); // Критично для заголовков JTable
            table.revalidate();

            // 3. Кнопки с отладкой
            for (JButton btn : commandButtons) {
                String key = "btn." + btn.getActionCommand();
                String text = bundle.getString(key); // Если ключа нет, вылетит MissingResourceException
                System.out.println("  🔘 [" + btn.getActionCommand() + "] -> \"" + text + "\"");
                btn.setText(text);
                btn.revalidate();
            }

            // 4. Рамки и метки
            controlPanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("panel.control")));
            visualizationPanel.setBorder(BorderFactory.createTitledBorder(bundle.getString("panel.viz")));
            langLabel.setText(bundle.getString("lang.title") + ":");

            // 5. Принудительная перерисовка всего фрейма
            getContentPane().revalidate();
            getContentPane().repaint();
            revalidate();
            repaint();

            System.out.println("✅ Локаль успешно применена");
        } catch (MissingResourceException e) {
            System.err.println("❌ ОШИБКА BUNDLE: Не найден ключ '" + e.getKey() + "'");
            System.err.println("   Проверьте файлы Messages_*.properties в src/main/resources/");
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

        panel.add(Box.createVerticalGlue());

        // ✅ Инициализируем метку языка сразу в поле класса
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

    private JPanel createVisualizationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(bundle.getString("panel.viz")));

        JLabel placeholder = new JLabel("📊 Область для графиков / диаграмм", SwingConstants.CENTER);
        placeholder.setFont(new Font("SansSerif", Font.ITALIC, 16));
        placeholder.setForeground(Color.GRAY);
        panel.add(placeholder, BorderLayout.CENTER);

        return panel;
    }

    private void populateTable(Collection<Organization> collection) {
        tableModel.setRowCount(0);
        if (collection == null) return;

        for (Organization org : collection) {
            double x = org.getCoordinates() != null ? org.getCoordinates().getX() : 0.0;
            double y = org.getCoordinates() != null ? org.getCoordinates().getY() : null;
            String address = org.getOfficialAddress() != null ? org.getOfficialAddress().toString() : "-";

            tableModel.addRow(new Object[]{
                    org.getId(), org.getName(), x, y,
                    org.getCreationDate(), org.getAnnualTurnover(),
                    org.getType(), address, org.getUsername()
            });
        }
    }

    private void handleCommand(String command) {
        System.out.println("[UI Command] " + command + " | Locale: " + currentLocale.toLanguageTag());
        JOptionPane.showMessageDialog(this, "Команда \"" + bundle.getString("btn." + command) + "\" вызвана.", "Управление", JOptionPane.INFORMATION_MESSAGE);
    }
}
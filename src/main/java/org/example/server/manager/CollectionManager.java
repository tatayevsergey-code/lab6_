package org.example.server.manager;

import org.example.common.models.Organization;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

public class CollectionManager {
    private PriorityQueue<Organization> collection = new PriorityQueue<>();
    private LocalDateTime lastInitTime;
    private LocalDateTime lastSaveTime;
    private final DatabaseManager dbManager;
    private final Set<Long> usedIds = new HashSet<>();

    public CollectionManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        loadCollection();
    }

    private void loadCollection() {
        try {
            List<Organization> loaded = dbManager.loadCollection();
            usedIds.clear();
            collection.clear();
            for (Organization org : loaded) {
                usedIds.add(org.getId());
                collection.add(org);
            }
            lastInitTime = LocalDateTime.now();
            System.out.println("Загружены данные из PostgreSQL. В коллекции " + loaded.size() + " объект(а/ов).");
        } catch (SQLException e) {
            System.err.println("Ошибка при загрузке коллекции из БД: " + e.getMessage());
            collection.clear();
            usedIds.clear();
        }
    }

    /** Добавление: сначала БД, только при успехе -> память */
    public synchronized boolean addToCollection(Organization element) {
        try {
            long generatedId = dbManager.insertOrganization(element);
            element.setId(generatedId);
            collection.add(element);
            usedIds.add(generatedId);
            return true;
        } catch (SQLException e) {
            System.err.println("Ошибка при добавлении в БД. Объект не добавлен в коллекцию: " + e.getMessage());
            return false;
        }
    }

    /** Удаление: сначала БД, затем память */
    public synchronized void removeFromCollection(Organization element) {
        try {
            dbManager.deleteOrganization(element.getId());
            collection.remove(element);
            usedIds.remove(element.getId());
        } catch (SQLException e) {
            System.err.println("Ошибка при удалении из БД: " + e.getMessage());
        }
    }

    /** Обновление: меняем в памяти, синхронизируем с БД */
//    public synchronized boolean updateOrganization(Organization updated) {
//        if (!checkExist(updated.getId())) return false;
//        try {
//            dbManager.updateOrganization(updated);
//            collection.remove(updated);
//            collection.add(updated); // PriorityQueue пересортируется при необходимости
//            return true;
//        } catch (SQLException e) {
//            System.err.println("Ошибка при обновлении в БД: " + e.getMessage());
//            return false;
//        }
//    }

    /** Обновление: сначала БД, только при успехе -> память. ID сохраняется. */
    public synchronized boolean updateOrganization(Organization updated) {
        long id = updated.getId();
        Organization old = getById(id);
        if (old == null) {
            System.err.println("Объект с ID " + id + " не найден в коллекции.");
            return false;
        }

        // Удаляем старую версию из PriorityQueue по прямой ссылке.
        // Это надёжнее, чем remove(updated), так как equals() уже не совпадёт из-за изменённых полей.
        collection.remove(old);

        try {
            // 1. Обновляем запись в БД (WHERE id=?)
            dbManager.updateOrganization(updated);
            // 2. Только при успехе добавляем новую версию в память
            collection.add(updated);
            return true;
        } catch (SQLException e) {
            // Откат состояния коллекции при ошибке БД: возвращаем старый объект
            collection.add(old);
            System.err.println("Ошибка при обновлении в БД. Изменения отменены: " + e.getMessage());
            return false;
        }
    }

    /** Очистка: сначала БД, затем память */
    public synchronized void clearCollection() {
        try {
            dbManager.clearTable();
            collection.clear();
            usedIds.clear();
            lastSaveTime = LocalDateTime.now();
        } catch (SQLException e) {
            System.err.println("Ошибка при очистке БД: " + e.getMessage());
        }
    }

    /** Заглушка: данные уже сохранены при каждом изменении */
    public synchronized void saveCollection() {
        lastSaveTime = LocalDateTime.now();
        System.out.println("Данные автоматически сохраняются в PostgreSQL при каждом изменении.");
    }

    public Organization getById(long id) {
        for (Organization element : collection) {
            if (element.getId() == id) return element;
        }
        return null;
    }

    public boolean checkExist(long id) {
        return usedIds.contains(id);
    }

    public int size() { return collection.size(); }
    public boolean isEmpty() { return collection.isEmpty(); }
    public PriorityQueue<Organization> getCollection() { return collection; }
    public LocalDateTime getTime() { return lastInitTime; }
    public LocalDateTime getLastSaveTime() { return lastSaveTime; }
}


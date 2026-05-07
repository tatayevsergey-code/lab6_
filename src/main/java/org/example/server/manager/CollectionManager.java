package org.example.server.manager;
import org.example.common.models.Organization;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CollectionManager {
    private final PriorityQueue<Organization> collection = new PriorityQueue<>();
    private LocalDateTime lastInitTime;
    private LocalDateTime lastSaveTime;
    private final DatabaseManager dbManager;
    private final Set<Long> usedIds = new HashSet<>();

    // Заменяем synchronized на ReadWriteLock
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public CollectionManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        loadCollection();
    }

    public DatabaseManager getDbManager() { return dbManager; }

    private void loadCollection() {
        rwLock.writeLock().lock();
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
            collection.clear(); usedIds.clear();
        } finally { rwLock.writeLock().unlock(); }
    }

    public boolean addToCollection(Organization element, String username) {
        rwLock.writeLock().lock();
        try {
            long generatedId = dbManager.insertOrganization(element, username);
            element.setId(generatedId);
            collection.add(element);
            usedIds.add(generatedId);
            return true;
        } catch (SQLException e) {
            System.err.println("Ошибка при добавлении в БД: " + e.getMessage());
            return false;
        } finally { rwLock.writeLock().unlock(); }
    }

    public void removeFromCollection(Organization element) {
        rwLock.writeLock().lock();
        try {
            dbManager.deleteOrganization(element.getId());
            collection.remove(element);
            usedIds.remove(element.getId());
        } catch (SQLException e) {
            System.err.println("Ошибка при удалении из БД: " + e.getMessage());
        } finally { rwLock.writeLock().unlock(); }
    }

    public boolean updateOrganization(Organization updated) {
        rwLock.writeLock().lock();
        try {
            long id = updated.getId();
            Organization old = getByIdInternal(id);
            if (old == null) return false;

            collection.remove(old);
            try {
                dbManager.updateOrganization(updated);
                collection.add(updated);
                return true;
            } catch (SQLException e) {
                collection.add(old);
                System.err.println("Ошибка при обновлении в БД. Изменения отменены: " + e.getMessage());
                return false;
            }
        } finally { rwLock.writeLock().unlock(); }
    }

    public void clearCollection(String username) {
        rwLock.writeLock().lock();
        try {
            dbManager.clearTable(username);
            collection.clear(); usedIds.clear();
            loadCollection();
            lastSaveTime = LocalDateTime.now();
        } catch (SQLException e) {
            System.err.println("Ошибка при очистке БД: " + e.getMessage());
        } finally { rwLock.writeLock().unlock(); }
    }

    public void saveCollection() {
        rwLock.writeLock().lock();
        try { lastSaveTime = LocalDateTime.now(); }
        finally { rwLock.writeLock().unlock(); }
    }

    // --- READ LOCK методы ---
    public Organization getById(long id) {
        rwLock.readLock().lock();
        try { return getByIdInternal(id); }
        finally { rwLock.readLock().unlock(); }
    }

    private Organization getByIdInternal(long id) {
        for (Organization element : collection) {
            if (element.getId() == id) return element;
        }
        return null;
    }

    public boolean checkExist(long id) {
        rwLock.readLock().lock();
        try { return usedIds.contains(id); }
        finally { rwLock.readLock().unlock(); }
    }

    public int size() {
        rwLock.readLock().lock();
        try { return collection.size(); }
        finally { rwLock.readLock().unlock(); }
    }

    public boolean isEmpty() {
        rwLock.readLock().lock();
        try { return collection.isEmpty(); }
        finally { rwLock.readLock().unlock(); }
    }

    // Возвращаем копию, чтобы внешние потоки не ломали итерацию
    public PriorityQueue<Organization> getCollection() {
        rwLock.readLock().lock();
        try { return new PriorityQueue<>(collection); }
        finally { rwLock.readLock().unlock(); }
    }

    public LocalDateTime getTime() {
        rwLock.readLock().lock();
        try { return lastInitTime; }
        finally { rwLock.readLock().unlock(); }
    }

    public LocalDateTime getLastSaveTime() {
        rwLock.readLock().lock();
        try { return lastSaveTime; }
        finally { rwLock.readLock().unlock(); }
    }
}
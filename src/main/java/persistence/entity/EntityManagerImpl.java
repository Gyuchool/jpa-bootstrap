package persistence.entity;

import bootstrap.MetaModel;
import bootstrap.MetaModelImpl;
import jakarta.persistence.GenerationType;
import jdbc.JdbcTemplate;
import persistence.entity.event.DeleteEvent;
import persistence.entity.event.UpdateEvent;
import persistence.sql.column.Columns;
import persistence.sql.column.IdColumn;
import persistence.sql.dialect.Dialect;

import java.lang.reflect.Field;

public class EntityManagerImpl implements EntityManager {

    private final Dialect dialect;
    private final PersistenceContext persistContext;
    private final MetaModel metaModel;

    public EntityManagerImpl(Dialect dialect, MetaModel metaModel) {
        this(dialect, new HibernatePersistContext(), metaModel);
    }

    public EntityManagerImpl(Dialect dialect, PersistenceContext persistContext, MetaModel metaModel) {
        this.dialect = dialect;
        this.persistContext = persistContext;
        this.metaModel = metaModel;
    }

    @Override
    public <T> T find(Class<T> clazz, Long id) {
        EntityMetaData entityMetaData = new EntityMetaData(clazz, new Columns(clazz.getDeclaredFields()));
        EntityLoader entityLoader = metaModel.getEntityLoader(clazz);
        Object entity = persistContext.getEntity(clazz, id)
                .orElseGet(() -> {
                    T findEntity = entityLoader.find(clazz, id);
                    savePersistence(findEntity, id);
                    return findEntity;
                });
        persistContext.getDatabaseSnapshot(entityMetaData, id);
        return clazz.cast(entity);
    }

    @Override
    public <T> T persist(Object entity) {
        IdColumn idColumn = new IdColumn(entity);
        GenerationType generationType = idColumn.getIdGeneratedStrategy(dialect).getGenerationType();
        EntityPersister entityPersister = metaModel.getEntityPersister(entity.getClass());

        if (dialect.getIdGeneratedStrategy(generationType).isAutoIncrement()) {
            long id = entityPersister.insertByGeneratedKey(entity);
            savePersistence(entity, id);
            setIdValue(entity, getIdField(entity, idColumn), id);
            return (T) entity;
        }

        savePersistence(entity, idColumn.getValue());
        entityPersister.insert(entity);

        return (T) entity;
    }

    private void setIdValue(Object entity, Field idField, long idValue) {
        try {
            idField.set(entity, idValue);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Field getIdField(Object entity, IdColumn idColumn) {
        Field idField;
        try {
            idField = entity.getClass().getDeclaredField(idColumn.getName());
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        idField.setAccessible(true);
        return idField;
    }

    @Override
    public void remove(Object entity) {
        IdColumn idColumn = new IdColumn(entity);
        persistContext.removeEntity(entity.getClass(), idColumn.getValue());
        persistContext.addDeleteActionQueue(new DeleteEvent<>(idColumn.getValue(), entity));
    }

    @Override
    public <T> T merge(T entity) {
        IdColumn idColumn = new IdColumn(entity);
        EntityMetaData entityMetaData = new EntityMetaData(entity);
        EntityMetaData previousEntity = persistContext.getSnapshot(entity, idColumn.getValue());
         if (entityMetaData.isDirty(previousEntity)) {
            persistContext.addUpdateActionQueue(new UpdateEvent<>(idColumn.getValue(), entity));
            savePersistence(entity, idColumn.getValue());
            return entity;
        }
        return entity;
    }

    private void savePersistence(Object entity, Object id) {
        persistContext.getDatabaseSnapshot(new EntityMetaData(entity), id);
        persistContext.addEntity(entity, id);
    }


    @Override
    public void flush() {
        persistContext.getUpdateActionQueue()
                .forEach(event -> {
                    EntityPersister entityPersister = metaModel.getEntityPersister(event.getEntity().getClass());
                    entityPersister.update(event.getEntity(), event.getId());
                });
        persistContext.getDeleteActionQueue()
            .forEach(event -> {
                EntityPersister entityPersister = metaModel.getEntityPersister(event.getEntity().getClass());
                entityPersister.delete(event.getEntity(), event.getId());
                persistContext.updateEntityEntryToGone(event.getEntity(), event.getId());
            });
    }

    @Override
    public Dialect getDialect() {
        return dialect;
    }


}

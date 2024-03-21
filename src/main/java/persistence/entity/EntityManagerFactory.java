package persistence.entity;

public interface EntityManagerFactory {

    EntityManager createEntityManager();

    EntityManager currentEntityManager();
}

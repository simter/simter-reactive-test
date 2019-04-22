package tech.simter.reactive.test.jpa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.persistence.*;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A application-managed {@link EntityManager} for use in unit tests.
 * <p>
 * All method run in a separate transaction with auto commit and never rollback after test method.
 * Typically use with embedded-database for unit test.
 * <p>
 * Idea comes from {@link org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager}.
 * But this is a application-managed {@link EntityManager}, not container-managed {@link EntityManager}.
 *
 * @author RJ
 */
@Component
public class TestEntityManager {
  private final EntityManagerFactory emf;

  @Autowired
  public TestEntityManager(EntityManagerFactory entityManagerFactory) {
    Assert.notNull(entityManagerFactory, "EntityManagerFactory must not be null");
    this.emf = entityManagerFactory;
  }

  @SafeVarargs
  public final <E> void persist(E... entities) {
    if (entities.length == 0) return;
    doInTransaction(em -> {
      for (E entity : entities) em.persist(entity);
    });
  }

  @SuppressWarnings("unchecked")
  public <E, ID> ID persistAndGetId(E entity) {
    return doInTransaction(em -> {
      em.persist(entity);
      return (ID) this.emf.getPersistenceUnitUtil().getIdentifier(entity);
    });
  }

  @SafeVarargs
  public final <E> void remove(E... entities) {
    doInTransaction(em -> {
      for (E entity : entities) em.remove(em.merge(entity));
    });
  }

  public <E> Optional<E> find(Class<E> entityClass, Object primaryKey) {
    return doInTransaction(em -> {
      return Optional.ofNullable(em.find(entityClass, primaryKey));
    });
  }

  public <E> List<E> queryList(Function<EntityManager, TypedQuery<E>> fn) {
    return doInTransaction(em -> {
      return fn.apply(em).getResultList();
    });
  }

  public <E> Optional<E> querySingle(Function<EntityManager, TypedQuery<E>> fn) {
    return doInTransaction(em -> {
      try {
        return Optional.of(fn.apply(em).getSingleResult());
      } catch (NoResultException e) {
        return Optional.empty();
      }
    });
  }

  public int executeUpdate(Function<EntityManager, Query> fn) {
    return doInTransaction(em -> {
      return fn.apply(em).executeUpdate();
    });
  }

  private void doInTransaction(Consumer<EntityManager> fn) {
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    try {
      fn.accept(em);
      em.getTransaction().commit();
      em.close();
    } catch (Exception e) {
      em.getTransaction().rollback();
      throw e;
    }
  }

  private <R> R doInTransaction(Function<EntityManager, R> fn) {
    EntityManager em = emf.createEntityManager();
    em.getTransaction().begin();
    try {
      R result = fn.apply(em);
      em.getTransaction().commit();
      em.close();
      return result;
    } catch (Exception e) {
      em.getTransaction().rollback();
      throw e;
    }
  }
}

package tech.simter.reactive.test.jpa;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.simter.reactive.jpa.ReactiveJpaWrapper;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A test encapsulation of {@link EntityManager} for reactive world.
 * <p>
 * Idea comes from `org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager`.
 *
 * @author RJ
 */
public class TestReactiveEntityManager implements ReactiveEntityManager {
  private final EntityManagerFactory emf;
  private final ReactiveJpaWrapper wrapper;

  public TestReactiveEntityManager(ReactiveJpaWrapper wrapper, EntityManagerFactory emf) {
    this.emf = emf;
    this.wrapper = wrapper;
  }

  private EntityManager createEntityManager() {
    return emf.createEntityManager();
  }

  /**
   * Persist entities in a transaction with auto commit when this {@link Mono} be subscribed.
   *
   * @param entities the entities to persist
   * @param <E>      the entity type
   * @return a {@link Mono}
   */
  @SafeVarargs
  public final <E> Mono<Void> persist(E... entities) {
    if (entities == null || entities.length == 0) return Mono.empty();
    else {
      return wrapper.fromRunnable(() -> {
        EntityManager em = createEntityManager();
        em.getTransaction().begin();
        for (E entity : entities) em.persist(entity);
        em.getTransaction().commit();
      });
    }
  }

  /**
   * Merge entities in a transaction with auto commit when this {@link Mono} be subscribed.
   *
   * @param entities the entities to merge
   * @param <E>      the entity type
   * @return a {@link Flux} with the merged entity
   */
  @SafeVarargs
  public final <E> Flux<E> merge(E... entities) {
    if (entities == null || entities.length == 0) return Flux.empty();
    else {
      return wrapper.fromIterable(() -> {
        List<E> merged = new ArrayList<>();
        EntityManager em = createEntityManager();
        em.getTransaction().begin();
        for (E entity : entities) merged.add(em.merge(entity));
        em.getTransaction().commit();
        return merged;
      });
    }
  }

  @Override
  public ReactiveQuery createQuery(String qlString) {
    return new ReactiveQueryImpl(qlString);
  }

  @Override
  public <T> ReactiveTypedQuery<T> createQuery(String qlString, Class<T> resultClass) {
    return new ReactiveTypedQueryImpl<>(qlString, resultClass);
  }

  private class ReactiveQueryImpl implements ReactiveQuery {
    private final Map<String, Object> params = new HashMap<>();
    private String qlString;

    public ReactiveQueryImpl(String qlString) {
      this.qlString = qlString;
    }

    @Override
    public ReactiveQuery setParameter(String name, Object value) {
      params.put(name, value);
      return this;
    }

    @Override
    public Mono<Object> getSingleResult() {
      return wrapper.fromCallable(() -> {
        EntityManager em = createEntityManager();
        em.getTransaction().begin();
        try {
          Query query = em.createQuery(qlString);
          if (!params.isEmpty()) params.forEach(query::setParameter);
          Object result = query.getSingleResult();
          em.getTransaction().commit();
          return result;
        } catch (Exception e) {
          em.getTransaction().rollback();
          throw e;
        }
      });
    }
  }

  private class ReactiveTypedQueryImpl<T> implements ReactiveTypedQuery<T> {
    private final Map<String, Object> params = new HashMap<>();
    private String qlString;
    private Class<T> resultClass;
    private int startPosition;
    private int maxResult;

    public ReactiveTypedQueryImpl(String qlString, Class<T> resultClass) {
      this.qlString = qlString;
      this.resultClass = resultClass;
    }

    @Override
    public ReactiveTypedQuery<T> setParameter(String name, Object value) {
      params.put(name, value);
      return this;
    }

    @Override
    public ReactiveTypedQuery<T> setFirstResult(int startPosition) {
      this.startPosition = startPosition;
      return this;
    }

    @Override
    public ReactiveTypedQuery<T> setMaxResults(int maxResult) {
      this.maxResult = maxResult;
      return this;
    }

    @Override
    public Mono<T> getSingleResult() {
      return wrapper.fromCallable(() -> doInTransaction(TypedQuery::getSingleResult));
    }

    @Override
    public Flux<T> getResultList() {
      return wrapper.fromIterable(() -> doInTransaction(TypedQuery::getResultList));
    }

    @Override
    public Flux<T> getResultStream() {
      return wrapper.fromStream(() -> doInTransaction(TypedQuery::getResultStream));
    }

    private <R> R doInTransaction(Function<TypedQuery<T>, R> fn) {
      EntityManager em = createEntityManager();
      em.getTransaction().begin();
      try {
        TypedQuery<T> query = em.createQuery(qlString, resultClass);
        if (!params.isEmpty()) params.forEach(query::setParameter);
        if (startPosition > 0) query.setFirstResult(startPosition);
        if (maxResult > 0) query.setMaxResults(maxResult);

        R result = fn.apply(query);

        em.getTransaction().commit();
        return result;
      } catch (Exception e) {
        em.getTransaction().rollback();
        throw e;
      }
    }
  }
}
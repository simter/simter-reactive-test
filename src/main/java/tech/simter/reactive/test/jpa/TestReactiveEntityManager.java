package tech.simter.reactive.test.jpa;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.simter.reactive.jpa.ReactiveJpaWrapper;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * A test encapsulation of {@link EntityManager} for reactive world.
 * <p>
 * Idea comes from `org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager`.
 *
 * @author RJ
 */
public class TestReactiveEntityManager {
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
}
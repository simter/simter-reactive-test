package tech.simter.reactive.test.jpa;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.persistence.EntityManager;

/**
 * A test encapsulation of {@link EntityManager} for reactive world.
 * <p>
 * Idea comes from `org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager`.
 *
 * @author RJ
 */
public interface ReactiveEntityManager {
  /**
   * Persist entities in a transaction with auto commit when this {@link Mono} be subscribed.
   *
   * @param entities the entities to persist
   * @param <E>      the entity type
   * @return a {@link Mono}
   */
  <E> Mono<Void> persist(E... entities);

  /**
   * Merge entities in a transaction with auto commit when this {@link Mono} be subscribed.
   *
   * @param entities the entities to merge
   * @param <E>      the entity type
   * @return a {@link Flux} with the merged entity
   */
  <E> Flux<E> merge(E... entities);

  /**
   * Create an instance of {@link ReactiveQuery} for executing a Java Persistence query language statement.
   *
   * @param qlString a Java Persistence query string
   * @return the new {@link ReactiveQuery} instance.
   * If the query string is invalid return a {@link IllegalArgumentException}.
   */
  ReactiveQuery createQuery(String qlString);

  <T> ReactiveTypedQuery<T> createQuery(String qlString, Class<T> resultClass);
}
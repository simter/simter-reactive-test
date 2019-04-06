package tech.simter.reactive.test.jpa;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveTypedQuery<T> {
  ReactiveTypedQuery<T> setParameter(String name, Object value);

  ReactiveTypedQuery<T> setFirstResult(int startPosition);

  ReactiveTypedQuery<T> setMaxResults(int maxResult);

  Mono<T> getSingleResult();

  Flux<T> getResultList();

  Flux<T> getResultStream();
}

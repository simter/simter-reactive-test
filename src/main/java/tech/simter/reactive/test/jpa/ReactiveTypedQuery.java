package tech.simter.reactive.test.jpa;

import reactor.core.publisher.Mono;

public interface ReactiveTypedQuery<T> {
  ReactiveTypedQuery<T> setParameter(String name, Object value);

  Mono<T> getSingleResult();
}

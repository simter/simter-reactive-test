package tech.simter.reactive.test.jpa;

import reactor.core.publisher.Mono;

public interface ReactiveQuery {
  ReactiveQuery setParameter(String name, Object value);

  Mono<Object> getSingleResult();
}

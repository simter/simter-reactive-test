# simter-reactive-test

Simter reactive test encapsulation.

## Reactive JPA encapsulation

This module just create one `@ReactiveDataJpaTest` annotation class. It's an annotation use to instead of `@DataJpaTest` for reactive JPA test.

This annotation inherits from [DataJpaTest] and only disabled `Transactional`. Because `EntityManager` could not share between threads, and in reactive world operation do not guarantee run on same thread all the time. So no need to start `Transactional` on the unit test class. But must consider that transaction would not rollback default, you need to manual manage it by yourself.

### Usage:

Maven: 

```xml
<dependency>
  <groupId>tech.simter.reactive</groupId>
  <artifactId>simter-reactive-jpa</artifactId>
  <version>{version}</version>
</dependency>
<dependency>
  <groupId>tech.simter.reactive</groupId>
  <artifactId>simter-reactive-test</artifactId>
  <version>{version}</version>
</dependency>
```

Java: 

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.test.test;
import tech.simter.reactive.jpa.ReactiveEntityManager;
import tech.simter.reactive.test.jpa.ReactiveDataJpaTest;

@SpringJUnitConfig(tech.simter.reactive.jpa.ModuleConfiguration.class)
@ReactiveDataJpaTest
public class TheTest {
  @Autowired
  private ReactiveEntityManager rem;

  @Test
  public test() {
    // save
    MyPo po = new MyPo();
    StepVerifier.create(rem.persist(po))
      .expectNext(po).verifyComplete();

    // find one
    StepVerifier.create(
      rem.createQuery("select t from MyPo t where id = :id", MyPo.class)
      .setParameter("id", 123)
      .getSingleResult()
    ).expectNext(po).verifyComplete();

    // find list
    StepVerifier.create(
      rem.createQuery("select t from MyPo t", MyPo.class)
      .getResultList()
    ).expectNext(po).verifyComplete();
  }
}
```

[DataJpaTest]: https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-testing.html#boot-features-testing-spring-boot-applications-testing-autoconfigured-jpa-test
package tech.simter.reactive.test.jpa;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;
import javax.persistence.EntityManager;

/**
 * An annotation to instead of {@link DataJpaTest} for reactive JPA test.
 * <p>
 * This annotation inherits from {@link DataJpaTest} and disabled {@link Transactional}.
 * Because {@link EntityManager} could not share between threads, and in reactive world
 * operation do not guarantee run on same thread all the time. So no need to start
 * {@link Transactional} on the unit test class. But must consider that transaction
 * would not rollback default, you need to manual manage it by yourself.
 *
 * @author RJ
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@DataJpaTest
@Transactional(propagation = Propagation.NEVER)
public @interface ReactiveDataJpaTest {
}
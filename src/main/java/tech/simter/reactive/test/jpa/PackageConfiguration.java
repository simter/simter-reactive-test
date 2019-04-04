package tech.simter.reactive.test.jpa;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.simter.reactive.jpa.ReactiveJpaWrapper;

import javax.persistence.EntityManagerFactory;

/**
 * All configuration for this module.
 *
 * @author RJ
 */
@Configuration("tech.simter.reactive.test.jpa.PackageConfiguration")
@ConditionalOnClass(name = "tech.simter.reactive.jpa.ReactiveJpaWrapper")
public class PackageConfiguration {
  /**
   * Register a {@link TestReactiveEntityManager} for reactive JPA test.
   */
  @Bean
  public TestReactiveEntityManager testReactiveEntityManager(ReactiveJpaWrapper wrapper, EntityManagerFactory emf) {
    return new TestReactiveEntityManager(wrapper, emf);
  }
}
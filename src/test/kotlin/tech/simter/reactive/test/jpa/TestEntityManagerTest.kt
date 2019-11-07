package tech.simter.reactive.test.jpa

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.util.*
import javax.persistence.Entity
import javax.persistence.EntityManagerFactory
import javax.persistence.Id
import javax.persistence.Persistence

@SpringJUnitConfig(TestEntityManagerTest.Cfg::class)
class TestEntityManagerTest @Autowired constructor(
  private val emf: EntityManagerFactory,
  private val tem: TestEntityManager
) {
  @Configuration
  open class Cfg {
    @Bean
    open fun entityManagerFactory(): EntityManagerFactory {
      return Persistence.createEntityManagerFactory("default")
    }

    @Bean
    open fun testEntityManager(emf: EntityManagerFactory): TestEntityManager {
      return TestEntityManager(emf)
    }
  }

  private fun randomString(): String = UUID.randomUUID().toString()

  private fun createBooks(vararg books: Book) {
    val em = emf.createEntityManager()
    em.transaction.begin()
    for (book in books) em.persist(book)
    em.transaction.commit()
    em.close()
  }

  private fun findBookById(id: String): Book? {
    val em = emf.createEntityManager()
    em.transaction.begin()
    val book = em.find(Book::class.java, id)
    em.transaction.commit()
    em.close()
    return book
  }

  private fun findBooks(sql: String, params: Map<String, Any>): List<Book> {
    val em = emf.createEntityManager()
    em.transaction.begin()
    val query = em.createQuery(sql, Book::class.java)
    params.forEach { query.setParameter(it.key, it.value) }
    em.transaction.commit()
    val list = query.resultList
    em.close()
    return list
  }

  @Test
  fun `persist and get id`() {
    // do persist
    val book = Book(id = randomString(), title = "test")
    val id = tem.persistAndGetId<Book, String>(book)

    // verify id
    assertEquals(book.id, id)

    // verify persisted
    val found = findBookById(book.id!!)
    assertEquals(book, found)
  }

  @Test
  fun `persist one`() {
    // do persist
    val book = Book(id = randomString(), title = "test")
    tem.persist(book)

    // verify persisted
    val found = findBookById(book.id!!)
    assertEquals(book, found)
  }

  @Test
  fun `persist many`() {
    // do persist
    val books = List(2) { Book(id = randomString(), title = "test") }
    tem.persist(*books.toTypedArray())

    // verify persisted
    val list = findBooks("select b from Book b where b.id in :ids", mapOf("ids" to books.map { it.id }))
    assertEquals(2, list.size)
    list.forEach { assertTrue(books.contains(it)) }
  }


  @Test
  fun `remove one`() {
    // prepare data
    val book = Book().apply { id = randomString(); title = "test" }
    createBooks(book)

    // do remove
    tem.remove(book)

    // verify removed
    val found = findBookById(book.id!!)
    assertNull(found)
  }

  @Test
  fun `remove many`() {
    // prepare data
    val books = List(2) { Book(id = randomString(), title = "test") }
    createBooks(*books.toTypedArray())

    // do remove
    tem.remove(*books.toTypedArray())

    // verify removed
    val list = findBooks("select b from Book b where b.id in :ids", mapOf("ids" to books.map { it.id }))
    assertEquals(0, list.size)
  }

  @Test
  fun `found it`() {
    // prepare data
    val book = Book().apply { id = randomString(); title = "test" }
    createBooks(book)

    // invoke and verify
    val actual = tem.find(Book::class.java, book.id!!)
    assertEquals(Optional.of(book), actual)
  }

  @Test
  fun `found nothing`() {
    val actual = tem.find(Book::class.java, randomString())
    assertFalse(actual.isPresent)
  }

  @Test
  fun `query list`() {
    // prepare data
    val books = List(2) { Book(id = randomString(), title = "test") }
    createBooks(*books.toTypedArray())

    // query and verify
    val list: List<Book> = tem.queryList { em ->
      em.createQuery("select b from Book b where b.id in :ids", Book::class.java)
        .setParameter("ids", books.map { it.id })
    }
    assertEquals(2, list.size)
    list.forEach { assertTrue(books.contains(it)) }
  }

  @Test
  fun `native query list`() {
    // prepare data
    val books = List(2) { Book(id = randomString(), title = "test") }
    createBooks(*books.toTypedArray())

    // query and verify
    val list: List<Book> = tem.nativeQueryList<Book> { em ->
      em.createNativeQuery("select b.id, b.title from book b where b.id in :ids", Book::class.java)
        .setParameter("ids", books.map { it.id })
    }
    assertEquals(2, list.size)
    list.forEach { assertTrue(books.contains(it)) }
  }

  @Test
  fun `query single`() {
    // prepare data
    val books = List(2) { Book(id = randomString(), title = "test") }
    createBooks(*books.toTypedArray())

    // query and verify
    val actual = tem.querySingle { em ->
      em.createQuery("select b from Book b where b.id = :id", Book::class.java)
        .setParameter("id", books[0].id!!)
    }
    assertEquals(Optional.of(books[0]), actual)
  }

  @Test
  fun `native query single`() {
    // prepare data
    val books = List(2) { Book(id = randomString(), title = "test") }
    createBooks(*books.toTypedArray())

    // query and verify
    val actual = tem.nativeQuerySingle<Book> { em ->
      em.createNativeQuery("select b.id, b.title from book b where b.id = :id", Book::class.java)
        .setParameter("id", books[0].id!!)
    }
    assertEquals(Optional.of(books[0]), actual)
  }

  @Test
  fun `execute delete`() {
    // prepare data
    val books = List(2) { Book(id = randomString(), title = "test") }
    createBooks(*books.toTypedArray())

    // execute and verify
    val actual: Int = tem.executeUpdate { em ->
      em.createQuery("delete from Book b where b.id in :ids")
        .setParameter("ids", books.map { it.id })
    }
    assertEquals(books.size, actual)
  }

  @Test
  fun `execute update`() {
    // prepare data
    val books = List(2) { Book(id = randomString(), title = "test") }
    createBooks(*books.toTypedArray())

    // execute update
    val newTitle = UUID.randomUUID().toString()
    val actual: Int = tem.executeUpdate { em ->
      em.createQuery("update Book b set b.title = :title where b.id = :id")
        .setParameter("id", books[0].id!!)
        .setParameter("title", newTitle)
    }
    assertEquals(1, actual)

    // verify updated
    val found = findBookById(books[0].id!!)!!
    assertEquals(found.title, newTitle)
  }
}

@Entity
data class Book(@Id var id: String?, var title: String?) {
  constructor() : this(null, null)
}
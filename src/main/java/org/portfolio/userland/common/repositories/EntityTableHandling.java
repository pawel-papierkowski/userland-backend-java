package org.portfolio.userland.common.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.portfolio.userland.common.services.table.TableHelper;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Common entity table handling.
 * @param <R> Request with metadata.
 * @param <E> Entity.
 */
public abstract class EntityTableHandling<R extends TableReq, E> {
  /** Entity manager. */
  @PersistenceContext
  protected EntityManager entityManager;

  /** Actual class of E. */
  private final Class<E> entityClass;

  /**
   * Constructor. Uses reflection to determine actual class of E. Magic!
   */
  public EntityTableHandling() {
    // This reflection magic looks up the generic hierarchy to find the actual class provided for 'E' by the concrete
    // subclass.
    // NOTE: If you ever create a deeply nested generic hierarchy
    // (e.g., AbstractUserHandling<E> extends EntityTableHandling<UserTableReq, E>),
    // the logic getGenericSuperclass() might need to traverse higher up the class hierarchy to find the actual class type.
    Type genericSuperclass = getClass().getGenericSuperclass();
    if (genericSuperclass instanceof ParameterizedType) {
      Type[] actualTypeArguments = ((ParameterizedType) genericSuperclass).getActualTypeArguments();
      // 'E' is the second parameter (index 1) in EntityTableHandling<R, E>
      this.entityClass = (Class<E>) actualTypeArguments[1];
    } else {
      throw new IllegalStateException("Class must be parameterized");
    }
  }

  /**
   * Return total count of entries for given filtering. Field tableMeta does not matter here.
   * @param req Table view request.
   * @return Count of entries.
   */
  public Long countEntries(R req) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> cq = cb.createQuery(Long.class);
    Root<E> entity = cq.from(entityClass);

    cq.select(cb.count(entity));

    // Note same predicates are generated for count and page content itself.
    List<Predicate> predicates = generatePredicates(req, cb, entity);
    if (!predicates.isEmpty()) {
      cq.where(cb.and(predicates.toArray(new Predicate[0])));
    }

    // Execute the query to get the single result (the count).
    return entityManager.createQuery(cq).getSingleResult();
  }

  /**
   * View page of user config entries. Note: tableMeta must be filled.
   * @param req Table view request.
   * @return Page of entities.
   */
  public List<E> viewPage(R req) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<E> cq = cb.createQuery(entityClass);
    Root<E> entity = cq.from(entityClass);

    // Note same predicates are generated for count and page content itself.
    List<Predicate> predicates = generatePredicates(req, cb, entity);
    if (!predicates.isEmpty()) {
      cq.where(cb.and(predicates.toArray(new Predicate[0])));
    }

    TableHelper.applySorting(cb, cq, entity, req.tableMeta());
    TypedQuery<E> query = entityManager.createQuery(cq);
    TableHelper.applyPagination(query, req.tableMeta());
    return query.getResultList();
  }

  //

  /**
   * Filtering logic for page result extracted from table.
   * @param req Filtering data.
   * @param cb Criteria builder.
   * @param entity Entity as <code>Root</code>.
   * @return List of predicates that you should assemble using AND.
   */
  protected abstract List<Predicate> generatePredicates(R req, CriteriaBuilder cb, Root<E> entity);
}

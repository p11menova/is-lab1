package com.example.repository;

import com.example.models.Person;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PersonRepository {

    @PersistenceContext(unitName = "my-pu")
    private EntityManager em;

    @Transactional
    public Person saveOrUpdate(Person person) {

        return em.merge(person);
    }

    public Optional<Person> findById(Long id) {

        return Optional.ofNullable(em.find(Person.class, id));
    }

    public boolean existsByNameAndBirthday(String name, java.time.LocalDateTime birthday) {
        if (birthday == null) {
            Long count =
                    em.createQuery(
                                    "SELECT COUNT(p) FROM Person p WHERE p.name = :name AND"
                                            + " p.birthday IS NULL",
                                    Long.class)
                            .setParameter("name", name)
                            .getSingleResult();
            return count > 0;
        }
        Long count =
                em.createQuery(
                                "SELECT COUNT(p) FROM Person p WHERE p.name = :name AND p.birthday"
                                        + " = :birthday",
                                Long.class)
                        .setParameter("name", name)
                        .setParameter("birthday", birthday)
                        .getSingleResult();
        return count > 0;
    }

    public boolean existsByNameAndBirthdayExcludingId(
            String name, java.time.LocalDateTime birthday, Long excludeId) {
        if (birthday == null) {
            Long count =
                    em.createQuery(
                                    "SELECT COUNT(p) FROM Person p WHERE p.name = :name AND"
                                            + " p.birthday IS NULL AND p.id != :excludeId",
                                    Long.class)
                            .setParameter("name", name)
                            .setParameter("excludeId", excludeId)
                            .getSingleResult();
            return count > 0;
        }
        Long count =
                em.createQuery(
                                "SELECT COUNT(p) FROM Person p WHERE p.name = :name AND p.birthday"
                                        + " = :birthday AND p.id != :excludeId",
                                Long.class)
                        .setParameter("name", name)
                        .setParameter("birthday", birthday)
                        .setParameter("excludeId", excludeId)
                        .getSingleResult();
        return count > 0;
    }

    public List<Person> findAll() {

        return em.createQuery("SELECT p FROM Person p", Person.class).getResultList();
    }

    @Transactional
    public void delete(Person person) {

        em.remove(em.contains(person) ? person : em.merge(person));
    }

    @Transactional
    public void deleteById(Long id) {
        Person p = em.find(Person.class, id);
        if (p != null) {
            em.remove(p);
        }
    }
}

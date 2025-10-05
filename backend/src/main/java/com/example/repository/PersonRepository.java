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

package com.kutlu.rediscsc.service;

import com.kutlu.rediscsc.entity.Person;
import com.kutlu.rediscsc.repository.PersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.util.List;
import java.util.Optional;

@Service
public class PersonService {

    private static final Logger logger = LoggerFactory.getLogger(PersonService.class);

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private JedisPooled jedisPooled;

    public List<Person> findAll() {
        logger.info("Fetching all persons from the database");
        return personRepository.findAll();
    }

    public Optional<Person> findById(Long id) {
        String cacheKey = "person:" + id;
        if (jedisPooled.exists(cacheKey)) {
            logger.info("Fetching person with id {} from cache", id);
            Person person = new Person();
            person.setId(id);
            person.setName(jedisPooled.hget(cacheKey, "name"));
            person.setSurname(jedisPooled.hget(cacheKey, "surname"));
            return Optional.of(person);
        } else {
            logger.info("Fetching person with id {} from the database", id);
            Optional<Person> person = personRepository.findById(id);
            person.ifPresent(p -> {
                jedisPooled.hset(cacheKey, "name", p.getName());
                jedisPooled.hset(cacheKey, "surname", p.getSurname());
                logger.info("Caching person with id {}", id);
            });
            return person;
        }
    }

    public Person save(Person person) {
        logger.info("Saving person with id {}", person.getId());
        Person savedPerson = personRepository.save(person);
        String cacheKey = "person:" + savedPerson.getId();
        jedisPooled.hset(cacheKey, "name", savedPerson.getName());
        jedisPooled.hset(cacheKey, "surname", savedPerson.getSurname());
        logger.info("Caching person with id {}", savedPerson.getId());
        return savedPerson;
    }

    public void deleteById(Long id) {
        logger.info("Deleting person with id {}", id);
        personRepository.deleteById(id);
        String cacheKey = "person:" + id;
        jedisPooled.del(cacheKey);
        logger.info("Removed person with id {} from cache", id);
    }
}
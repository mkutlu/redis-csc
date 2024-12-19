package com.kutlu.rediscsc.conf;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.*;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.csc.Cacheable;
import redis.clients.jedis.csc.DefaultCacheable;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Configuration
public class JedisConfig {

    @Bean
    public JedisPooled jedisPooled() {
        // Fetch Redis connection details from environment variables, with defaults
        String redisHost = System.getenv("REDIS_HOST");
        if (redisHost == null || redisHost.isEmpty()) {
            redisHost = "localhost";
        }

        String redisPort = System.getenv("REDIS_PORT");
        int port = 6379; // Default Redis Port
        if (redisPort != null && !redisPort.isEmpty()) {
            try {
                port = Integer.parseInt(redisPort);
            } catch (NumberFormatException e) {
                System.out.println("Invalid REDIS_PORT value. Using default port 6379.");
            }
        }

        String redisPassword = System.getenv("REDIS_PASSWORD");

        // Configure client-side cache with custom Cacheable (you can choose either)
        CacheConfig cacheConfig = CacheConfig.builder()
                .maxSize(1000) // Cache size
                .cacheable(new PrefixCacheable(Set.of("foo", "user", "session", "person", "hello"))) // Cache based on multiple prefixes - note that I only cache one of the keys from mget
                //.cacheable(new SpecificKeysCacheable(Set.of("user:1001", "user:1002", "foo", "person:1", "session:1", "hola"))) // Uncomment for specific keys caching
                .build();

        // Configure Redis connection pool
        GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(10); // Maximum number of connections
        poolConfig.setMaxIdle(5);   // Maximum number of idle connections
        poolConfig.setMinIdle(2);   // Minimum number of idle connections
        poolConfig.setTestWhileIdle(true); // Test connections while idle
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30)); // Eviction policy timing
        poolConfig.setBlockWhenExhausted(true); // Block if connections exhausted
        poolConfig.setMaxWait(Duration.ofSeconds(2)); // Wait time when pool is exhausted

        // Initialize JedisPooled with pool and cache
        HostAndPort node = new HostAndPort(redisHost, port);

        // Build JedisClientConfig without a password if none is provided
        JedisClientConfig clientConfig;
        if (redisPassword != null && !redisPassword.isEmpty()) {
            clientConfig = DefaultJedisClientConfig.builder()
                    .resp3()
                    .password(redisPassword)
                    .build();
        } else {
            clientConfig = DefaultJedisClientConfig.builder()
                    .resp3()
                    .build();  // No password provided
        }
        System.out.println("Connecting to Redis at: "+ redisHost + " " + port);

        // Using the constructor that accepts poolConfig and cacheConfig
        return new JedisPooled(node, clientConfig, cacheConfig, poolConfig);
    }

    // Cacheable implementation for multiple prefix-based caching
    public static class PrefixCacheable implements Cacheable {
        private final Set<String> prefixes;

        // Accept a set of prefixes instead of a single prefix
        public PrefixCacheable(Set<String> prefixes) {
            this.prefixes = prefixes;
        }

        @Override
        public boolean isCacheable(ProtocolCommand command, List<Object> keys) {
            // Check if the command is cacheable by default
            if (!DefaultCacheable.isDefaultCacheableCommand(command)) {
                return false;  // Exit immediately if it's not a cacheable command
            }

            // Custom logic: Check if any of the key(s) start with any of the given prefixes
            for (Object key : keys) {
                for (String prefix : prefixes) {
                    if (key.toString().startsWith(prefix)) {
                        return true;  // Cache if the key matches any of the prefixes
                    }
                }
            }
            return false;  // Otherwise, don't cache
        }
    }
}

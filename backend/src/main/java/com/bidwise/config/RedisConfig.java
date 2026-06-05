package com.bidwise.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * Redis wiring for the atomic bidding fast-path. {@code StringRedisTemplate} is
 * auto-configured by Spring Boot; here we load the place-bid Lua script as a bean.
 */
@Configuration
public class RedisConfig {

    /** The atomic check-and-set script; returns a two-element {@code [accepted, value]}. */
    @Bean
    @SuppressWarnings("unchecked")
    public RedisScript<List> placeBidScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("redis/place-bid.lua")));
        script.setResultType(List.class);
        return script;
    }
}

package com.bidwise.bid;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * The atomic price guard for live auctions, backed by Redis. The current price is
 * held per listing in integer cents and advanced via a Lua check-and-set, which is
 * race-free across threads and application instances. Postgres remains the durable
 * source of truth (the {@code bids} table); this just serializes who gets to raise
 * the price and to what.
 */
@Component
public class AtomicBidPrice {

    private final StringRedisTemplate redis;
    private final RedisScript<List> placeBidScript;

    public AtomicBidPrice(StringRedisTemplate redis, RedisScript<List> placeBidScript) {
        this.redis = redis;
        this.placeBidScript = placeBidScript;
    }

    /** Outcome of a {@link #tryRaise} attempt. */
    public record Result(boolean accepted, boolean initialized, BigDecimal currentPrice) {
    }

    /**
     * Seeds the price key (only if absent) to {@code floor}, the value a new bid must
     * exceed by one increment. Callers pass {@code startPrice - increment} when no bids
     * exist yet (so the first bid clears at {@code startPrice}), or the current price
     * when bids already exist (e.g. reseeding after a Redis restart). Idempotent.
     */
    public void seed(long listingId, BigDecimal floor, Duration ttl) {
        redis.opsForValue().setIfAbsent(key(listingId), String.valueOf(toCents(floor)), ttl);
    }

    /**
     * Atomically accepts {@code amount} as the new price iff it clears
     * {@code currentPrice + increment}. The returned {@link Result} reports whether it
     * was accepted and the authoritative current price afterwards.
     */
    public Result tryRaise(long listingId, BigDecimal amount, BigDecimal increment) {
        List<?> raw = redis.execute(
                placeBidScript,
                List.of(key(listingId)),
                String.valueOf(toCents(amount)),
                String.valueOf(toCents(increment)));
        if (raw == null || raw.size() < 2) {
            return new Result(false, false, null);
        }
        long flag = ((Number) raw.get(0)).longValue();
        long valueCents = ((Number) raw.get(1)).longValue();
        if (flag < 0) {
            return new Result(false, false, null);
        }
        return new Result(flag == 1, true, fromCents(valueCents));
    }

    /** Pushes the price key's expiry out (used when anti-snipe extends the auction). */
    public void touchTtl(long listingId, Duration ttl) {
        redis.expire(key(listingId), ttl);
    }

    /** Removes the price key once the auction is over. */
    public void clear(long listingId) {
        redis.delete(key(listingId));
    }

    private static String key(long listingId) {
        return "auction:" + listingId + ":price";
    }

    private static long toCents(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    private static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}

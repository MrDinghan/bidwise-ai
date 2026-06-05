-- Atomic check-and-set for an auction's live price.
--
-- KEYS[1] = price key (auction:{id}:price), holding the current price in integer cents
-- ARGV[1] = bid amount in cents
-- ARGV[2] = minimum increment in cents
--
-- The key is seeded to (startPrice - increment) when the auction opens, so a single
-- monotonic rule covers both the opening bid and later raises: a bid is accepted iff
-- it clears (currentPrice + increment).
--
-- Returns a two-element array {accepted, value}:
--   {1, newPriceCents}     -- accepted; price advanced to the bid
--   {0, currentPriceCents} -- rejected; the bid did not clear the current bar
--   {-1, 0}                -- not initialized (key missing/expired); caller should reseed

local raw = redis.call('GET', KEYS[1])
if not raw then
  return {-1, 0}
end

local price = tonumber(raw)
local amount = tonumber(ARGV[1])
local increment = tonumber(ARGV[2])

if amount >= price + increment then
  redis.call('SET', KEYS[1], amount, 'KEEPTTL')
  return {1, amount}
end

return {0, price}

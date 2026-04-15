-- KEYS[1] = waiting queue key
-- KEYS[2] = active queue key
-- KEYS[3] = active expiry zset key
--
-- ARGV[1] = entry key prefix
-- ARGV[2] = active status
-- ARGV[3] = activatedAt (ISO-8601)
-- ARGV[4] = expiresAt (ISO-8601)
-- ARGV[5] = expiresAt epoch milli
-- ARGV[6] = max active count
-- ARGV[7] = promote batch size

local activeCount = redis.call('ZCARD', KEYS[2])
local availableSlots = tonumber(ARGV[6]) - activeCount

if availableSlots <= 0 then
    return 0
end

local promoteCount = math.min(availableSlots, tonumber(ARGV[7]))
local tokens = redis.call('ZRANGE', KEYS[1], 0, promoteCount - 1)

if #tokens == 0 then
    return 0
end

local promotedCount = 0

for _, token in ipairs(tokens) do
    local entryKey = ARGV[1] .. token
    local sequence = redis.call('HGET', entryKey, 'sequence')

    if sequence then
        redis.call('HSET', entryKey,
            'status', ARGV[2],
            'activatedAt', ARGV[3],
            'expiresAt', ARGV[4],
            'lastUpdatedAt', ARGV[3]
        )

        redis.call('ZREM', KEYS[1], token)
        redis.call('ZADD', KEYS[2], tonumber(sequence), token)
        redis.call('ZADD', KEYS[3], tonumber(ARGV[5]), token)

        promotedCount = promotedCount + 1
    else
        redis.call('ZREM', KEYS[1], token)
    end
end

return promotedCount
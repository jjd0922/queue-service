-- KEYS[1] = active expiry zset key
-- KEYS[2] = active queue zset key
--
-- ARGV[1] = entry key prefix
-- ARGV[2] = expired status
-- ARGV[3] = lastUpdatedAt (ISO-8601)
-- ARGV[4] = now epoch millis
-- ARGV[5] = batch size
-- ARGV[6] = user index key prefix

local tokens = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', tonumber(ARGV[4]), 'LIMIT', 0, tonumber(ARGV[5]))
local expiredCount = 0

for _, token in ipairs(tokens) do
    local entryKey = ARGV[1] .. token
    local status = redis.call('HGET', entryKey, 'status')

    if status == 'ACTIVE' then
        local userId = redis.call('HGET', entryKey, 'userId')

        redis.call('HSET', entryKey,
            'status', ARGV[2],
            'lastUpdatedAt', ARGV[3]
        )

        redis.call('ZREM', KEYS[2], token)
        redis.call('ZREM', KEYS[1], token)

        if userId and userId ~= '' then
            redis.call('DEL', ARGV[6] .. userId)
        end

        expiredCount = expiredCount + 1
    else
        redis.call('ZREM', KEYS[1], token)
        redis.call('ZREM', KEYS[2], token)
    end
end

return expiredCount
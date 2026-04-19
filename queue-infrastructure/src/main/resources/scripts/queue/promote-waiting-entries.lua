local waitingKey = KEYS[1]
local activeKey = KEYS[2]

local entryKeyPrefix = ARGV[1]
local activeStatus = ARGV[2]
local activatedAt = ARGV[3]
local expiresAt = ARGV[4]
local expiresAtScore = tonumber(ARGV[5])
local maxActiveCount = tonumber(ARGV[6])
local promoteBatchSize = tonumber(ARGV[7])

local activeCount = redis.call('ZCARD', activeKey)
local available = maxActiveCount - activeCount

if available <= 0 then
    return 0
end

local promoteCount = math.min(available, promoteBatchSize)
local tokens = redis.call('ZRANGE', waitingKey, 0, promoteCount - 1)

if #tokens == 0 then
    return 0
end

for _, token in ipairs(tokens) do
    redis.call('ZREM', waitingKey, token)
    redis.call('ZADD', activeKey, expiresAtScore, token)

    local entryKey = entryKeyPrefix .. token

    if redis.call('EXISTS', entryKey) == 1 then
        redis.call('HSET', entryKey,
            'status', activeStatus,
            'activatedAt', activatedAt,
            'expiresAt', expiresAt,
            'lastUpdatedAt', activatedAt
        )
    end
end

return #tokens
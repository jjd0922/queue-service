local existingToken = redis.call('GET', KEYS[1])

if existingToken then
    local existingEntryKey = ARGV[1] .. existingToken

    if redis.call('EXISTS', existingEntryKey) == 1 then
        local existingStatus = redis.call('HGET', existingEntryKey, 'status')
        local existingSequence = redis.call('HGET', existingEntryKey, 'sequence')
        local existingEnteredAt = redis.call('HGET', existingEntryKey, 'enteredAt')
        local existingActivatedAt = redis.call('HGET', existingEntryKey, 'activatedAt')
        local existingExpiresAt = redis.call('HGET', existingEntryKey, 'expiresAt')
        local existingLastUpdatedAt = redis.call('HGET', existingEntryKey, 'lastUpdatedAt')

        return {
            '0',
            existingToken,
            existingStatus,
            existingSequence,
            existingEnteredAt,
            existingActivatedAt or '',
            existingExpiresAt or '',
            existingLastUpdatedAt
        }
    end

    redis.call('DEL', KEYS[1])
end

local sequence = redis.call('INCR', KEYS[2])

redis.call('HSET', KEYS[3],
    'token', ARGV[2],
    'queueId', ARGV[3],
    'userId', ARGV[4],
    'status', ARGV[5],
    'sequence', tostring(sequence),
    'enteredAt', ARGV[6],
    'activatedAt', '',
    'expiresAt', '',
    'lastUpdatedAt', ARGV[7]
)

redis.call('ZADD', KEYS[4], sequence, ARGV[2])
redis.call('SET', KEYS[1], ARGV[2])

return {
    '1',
    ARGV[2],
    ARGV[5],
    tostring(sequence),
    ARGV[6],
    '',
    '',
    ARGV[7]
}
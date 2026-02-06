package com.akansha.peak_server.redis;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class LeaderboardRedisService {
    private static final String LEADERBOARD_KEY = "leaderboard:global";

    @Autowired
    private StringRedisTemplate redisTemplate;

    private ZSetOperations<String,String>  zSetOps;

    @PostConstruct
    public void init(){
        this.zSetOps = redisTemplate.opsForZSet();
    }

//    @PostConstruct
//    public void testRedis(){
//        zSetOps.add(LEADERBOARD_KEY, "test-user", 999);
//        System.out.println(
//                zSetOps.reverseRangeWithScores(LEADERBOARD_KEY,0,2)
//        );
//    }

    public void updateScore(String userId, double score){
        zSetOps.add(LEADERBOARD_KEY, userId, score);
    }

    public Set<ZSetOperations.TypedTuple<String>> getTopPlayers(int topN){
        return zSetOps.reverseRangeWithScores(LEADERBOARD_KEY, 0, topN - 1);
    }
}

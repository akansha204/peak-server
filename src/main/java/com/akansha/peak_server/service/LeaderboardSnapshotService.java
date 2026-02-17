package com.akansha.peak_server.service;

import com.akansha.peak_server.redis.LeaderboardRedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import com.akansha.peak.grpc.LeaderboardSnapshot;
import org.springframework.stereotype.Service;

import java.util.Set;


@Service
public class LeaderboardSnapshotService {

    @Autowired
    private LeaderboardRedisService leaderboardRedisService;

    public LeaderboardSnapshot getCurrentSnapshot(int topN){
        Set<ZSetOperations.TypedTuple<String>> players = leaderboardRedisService.getTopPlayers(topN);
        LeaderboardSnapshot.Builder snapshotBuilder = LeaderboardSnapshot.newBuilder();

        int position = 0;
        int rank = 1;
        Double prevscore = null;
        if(players != null){
            for(ZSetOperations.TypedTuple<String> player : players){
                position++;
                Double currscore = player.getScore();
                if(prevscore == null || !currscore.equals(prevscore)){
                    rank = position;
                }
                snapshotBuilder.addEntries(
                        com.akansha.peak.grpc.LeaderboardEntry.newBuilder()
                                .setUserId(player.getValue())
                                .setScore(player.getScore().longValue())
                                .setRank(rank)
                                .build()
                );
                prevscore = currscore;
            }
        }
        return snapshotBuilder.build();
    }

}

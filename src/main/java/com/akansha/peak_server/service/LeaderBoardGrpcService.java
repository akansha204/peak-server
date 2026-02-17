package com.akansha.peak_server.service;

import com.akansha.peak.grpc.ClientEvent;
import com.akansha.peak.grpc.LeaderboardServiceGrpc;
import com.akansha.peak.grpc.ServerEvent;
import com.akansha.peak_server.redis.LeaderboardRedisService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.grpc.server.service.GrpcService;
import com.akansha.peak.grpc.LeaderboardSnapshot;
import com.akansha.peak.grpc.LeaderboardEntry;

import java.util.Set;

@GrpcService
public class LeaderBoardGrpcService extends LeaderboardServiceGrpc.LeaderboardServiceImplBase {
    @Autowired
    private LeaderboardRedisService leaderboardRedisService;

    @Override
    public StreamObserver<ClientEvent> streamLeaderboard(StreamObserver<ServerEvent> responseObserver) {
        return new StreamObserver<ClientEvent>() {
            @Override
            public void onNext(ClientEvent clientEvent){
                if(clientEvent.getPayloadCase() == ClientEvent.PayloadCase.JOIN){
                    String userId = clientEvent.getJoin().getUserId();
                    if(userId == null || userId.isEmpty()){
                        responseObserver.onError(
                                Status.INVALID_ARGUMENT
                                        .withDescription("User ID cannot be null or empty")
                                        .asRuntimeException()
                        );
                        return;
                    }
                    leaderboardRedisService.joinLeaderboard(clientEvent.getJoin().getUserId());
                    sendSnapshot(responseObserver);
                } else if(clientEvent.getPayloadCase() == ClientEvent.PayloadCase.SCOREUPDATE){
                    boolean updated = leaderboardRedisService.updateScore(
                            clientEvent.getScoreUpdate().getUserId(),
                            clientEvent.getScoreUpdate().getScore()
                    );
                    if(!updated){
                        responseObserver.onError(
                                Status.NOT_FOUND
                                        .withDescription("User not found: " + clientEvent.getScoreUpdate().getUserId())
                                        .asRuntimeException()
                        );
                        return;
                    }
                    sendSnapshot(responseObserver);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error in client stream: " + throwable.getMessage());
            }
            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };

    }
    private void sendSnapshot(StreamObserver<ServerEvent> responseObserver){
        Set<ZSetOperations.TypedTuple<String>> players = leaderboardRedisService.getTopPlayers(10);

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
                        LeaderboardEntry.newBuilder()
                                .setUserId(player.getValue())
                                .setScore(player.getScore().longValue())
                                .setRank(rank)
                                .build()
                );
                prevscore = currscore;
            }
        }
        responseObserver.onNext(
                ServerEvent.newBuilder()
                        .setSnapshot(snapshotBuilder.build())
                        .build()
        );
    }

}

package com.akansha.peak_server.service;

import com.akansha.peak.grpc.LeaderboardQueryServiceGrpc;
import com.akansha.peak.grpc.LeaderboardSnapshot;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class LeaderboardQuerygrpcService extends LeaderboardQueryServiceGrpc.LeaderboardQueryServiceImplBase{
    @Autowired
    private LeaderboardSnapshotService leaderboardSnapshotService;
    @Override
    public void getLeaderboard(Empty request, StreamObserver<LeaderboardSnapshot> responseObserver) {
        responseObserver.onNext(
                leaderboardSnapshotService.getCurrentSnapshot(10)
        );
        responseObserver.onCompleted();

    }
}

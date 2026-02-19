# Peak (v2)
Real-time Leaderboard Service using gRPC and Redis.

---

##  What’s New in v2

Peak v2 introduces architectural improvements and cleaner separation of concerns while preserving the original real-time streaming model.

### Key Improvements

- Shared ranking logic (Standard Competition Ranking – ties share same rank)
- Separation of mutation and query responsibilities
- Added unary `GetLeaderboard` RPC for read-only dashboard access
- Refactored snapshot-building into a dedicated service layer
- Improved validation for JOIN and SCORE_UPDATE events
- Cleaner layered architecture (Redis → Business → gRPC)

---

## Updated Architecture (v2)

```
Client (Streaming) → LeaderboardGrpcService
Client (Unary)     → LeaderboardQueryGrpcService
                        ↓
                LeaderboardSnapshotService
                        ↓
                LeaderboardRedisService
                        ↓
                        Redis (ZSET)
```

### Design Philosophy

- Redis remains the single source of truth.
- Business logic (ranking + snapshot building) is isolated in a dedicated service.
- gRPC layer handles transport only.
- Full leaderboard snapshots are sent after each mutation.
- Unary RPC enables dashboard-style leaderboard viewing without joining.

---

## Ranking Model (v2)

Implements **Standard Competition Ranking** (shared ranks for equal scores).

Example:

| User | Score | Rank |
|------|-------|------|
| A    | 100   | 1    |
| B    | 100   | 1    |
| C    | 90    | 2    |

Ranks are calculated in memory after fetching sorted data from Redis.

---

## gRPC API (v2 Overview)

### Streaming Service

- `JOIN` – Adds user (if not existing) with default score 0 and returns snapshot
- `SCORE_UPDATE` – Updates score and returns updated snapshot
- `SNAPSHOT` – Full leaderboard snapshot streamed to client

### Unary Service

- `getLeaderboard` – Returns current leaderboard snapshot (read-only)

## Protofile Quickview

```protobuf
syntax = "proto3";
import "google/protobuf/empty.proto";

package peak;

option java_multiple_files = true;
option java_package = "com.akansha.peak.grpc";
option java_outer_classname = "LeaderboardProto";

service LeaderboardService {
  rpc streamLeaderboard(stream ClientEvent) returns (stream ServerEvent);
}

service LeaderboardQueryService {
  rpc getLeaderboard(google.protobuf.Empty) returns (LeaderboardSnapshot);
}

message ClientEvent {
  oneof payload {
    JoinLeaderboard join = 1;
    ScoreUpdate scoreUpdate = 2;
  }
}

message JoinLeaderboard {
  string userId = 1;
}

message ScoreUpdate {
  string userId = 1;
  int64 score = 2;
}

message ServerEvent {
  oneof payload {
    LeaderboardSnapshot snapshot = 1;
    LeaderboardUpdate update = 2;
  }
}

message LeaderboardSnapshot {
  repeated LeaderboardEntry entries = 1;
}

message LeaderboardUpdate {
  LeaderboardEntry entry = 1;
}

message LeaderboardEntry {
  string userId = 1;
  int64 score = 2;
  int32 rank = 3;
}

```

---


# Peak (v1)
Real-time Leaderboard Service using gRPC and Redis.
## Overview
**Peak** is a backend service that provides a **real-time leaderboard** using **gRPC bidirectional streaming** and **Redis Sorted Sets**.

Clients can:  
- Join the leaderboard stream
- Send score updates
- Receive updated leaderboard snapshots and rankings in real-time

The server is **stateless** with redis acting as the source of truth.

---
## Tech Stack
- Java
- Spring Boot
- gRPC (bidirectional streaming)
- Redis (Sorted Sets)

## gRPC API (Simplified)

### Client Events
- `JOIN` – Request current leaderboard snapshot
- `SCORE_UPDATE` – Update user score

### Server Events
- `SNAPSHOT` – Leaderboard snapshot with ranked entries

## Redis Data Model

- **Key**: `leaderboard:global`
- **Type**: Sorted Set (ZSET)
- **Member**: `userId` (String)
- **Score**: `score` (Double)


## How Ranking Works

1. Scores are stored in Redis ZSET
2. Leaderboard is fetched using `ZREVRANGE`
3. Ranks are assigned sequentially in memory
4. Snapshot is streamed back to the client


## Running the Server
1. Ensure Redis is running locally on default port (6379) by docker.  
```bash
 docker run -d --name redis -p 6379:6379 redis
 ```
2. Run the spring-boot application
```bash
 ./mvnw spring-boot:run
 ```

## Testing the Server with grpcurl 
- List services:
```bash
docker run --rm --network=host \
  fullstorydev/grpcurl \
  -plaintext localhost:9090 list
```
- Join Leaderboard Stream:
```bash
docker run --rm --network=host \
  fullstorydev/grpcurl \
  -plaintext \
  -d '{"join":{}}' \
  localhost:9090 \
  peak.LeaderboardService/streamLeaderboard
```
- Update Score:
```bash
docker run --rm --network=host \
  fullstorydev/grpcurl \
  -plaintext \
  -d '{"scoreUpdate":{"userId":"alice","score":500}}' \
  localhost:9090 \
  peak.LeaderboardService/streamLeaderboard
```
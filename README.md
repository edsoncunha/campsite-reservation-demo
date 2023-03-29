# Upgrade code challenge
## Dependencies to run:

- docker-compose
- [hey](https://github.com/rakyll/hey) (optional)

## How to run automated tests

```
./gradlew test
```

## How to start the application
Set execute permission on script and run

```
chmod +x start.sh
./start.sh
```

API documentation will be available at [localhost:8080/documentation.html]()

## How to run a simple load test
100 parallel agents sending 40 request per second each:
```
hey -c 100 -q 40 -z 60s 'http://localhost:8080/api/reservations/availability?startDate=2024-03-31&endDate=2024-04-22'
```

## Notes for the reviewer

- The requirements mention parallel reservation attempts. There is a specific integration test to show that a race condition is avoided during reservations
- The system is expected to have more searches than reservations, so a cache was added to improve performance. 
- When a reservation is completed, the cache is evicted. For simplicity, all entries are remove. An improvement would be evicting only the entries that overlap the reservation added to the database.
- The implemented cache is very simple and uses an in-memory hashmap as data store. That implies the cache would be specific for each server in a multi server deployment. In a realistic deployment, we would very likely have a horizontal autoscaler and a centralized cache store, such as Redis.
- I _really_ would like to implement a more sophisticated load test, by using kubernetes + HPA + [Gatling](https://gatling.io/), but unfortunately had not enough time for that.
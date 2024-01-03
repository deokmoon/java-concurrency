# Stock Service

### mini project for resolve Java concurrency Problem

## Getting Started

```bash
> docker run --name mariadb -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root mariadb
> docker exec -it mariadb

> mysql -u root -p
> create database stock_example;
> use stock_example;

> docker pull redis
> docker run --name redis -d -p 6379:6379 redis
<- 확인 ->
> docker exec -it redis redis-cli 
```

## 자바 동시성 이슈

- 여러 쓰레드가 접근 시 동시성 문제 발생
- 동시성 문제 해결을 위해 동기화 처리

### synchronized

- 메소드 전체에 동기화 처리
- 하나의 쓰레드만 접근 가능할 수 있게 처리

 ```java
   public synchronized void decrease(Long quantity) {
       this.quantity -= quantity;
   }
``` 
- 서버가 여러대 일 경우 동시성 보장하지 못함


### database lock
#### optimistic lock
- lock 을 걸지않고 문제가 발생할 때 처리 -> 충돌방지 개념과 유사
- version column 을 만들어 해결
- 실패 로직을 구현해야함
- 충돌이 빈번하게 일어나게 된다면 pessimistic lock 보다 더 느려짐

~~~ java
@Entity
public class Stock {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	private Long version;
	
	private Long productId;
	private Long quantity;
	
	@Lock(value = LockModeType.OPTIMISTIC)
    @Query("select s from Stock s where s.id = :id")
    Stock findByIdOptimisticLock(Long id);
}
~~~

#### pessimistic lock
- 다른 트랜잭션이 특정 row 의 lock 을 얻는 것을 방지
- 특정 row 를 update 하거나 delete 할 수 있음
- 일반 select 는 별다른 lock 이 없기 때문에 조회 가능
~~~ java
    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.id = :id")
    Stock findByIdPessimisticLock(Long id);
~~~

### named lock - mybatis
- MySql 서버의 메모리에 직접 동작 -> 낮은 메모리 -> 분산시스템엔 비적합
- 이름과 함께 lock 획득. 해당 lock 은 다른 세션에서 획득 및 해제 불가
- connection pool 을 다수 차지할 수 있어 다른 datasource 사용 권장

~~~java
    //repository
    @Query(value = "select get_lock(:key, 3000)", nativeQuery = true)
        void getLock(String key);
    
    @Query(value = "select release_lock(:key)", nativeQuery = true)
        void releaseLock(String key);

    // 사용
    public void decrease(Long id, Long quantity) {
		try {
			lockRepository.getLock(id.toString());
			stockService.decrease(id, quantity);
		} finally {
			lockRepository.releaseLock(id.toString());
		}
	}
    
~~~

### Redis lock
#### lettuce
- 구현이 간단
- spin lock 방식으로 redis 에 부하 발생 가능성
- 재시도가 필요하지 않은 경우 고려
- setIfAbsent 통해 SETNX 사용 -> key 가 존재하지 않을 경우에만 값을 지정
~~~ java
@RequiredArgsConstructor
@Repository
public class RedisLockRepository {

    private final RedisTemplate<String, String> redisTemplate;

    public Boolean lock(Object key) {
        return redisTemplate
                .opsForValue()
                .setIfAbsent(key.toString(), "lock", Duration.ofMillis(3000)); // lock 사용시간 3초
    }

    public Boolean unlock(Object key) {
        return redisTemplate.delete(key.toString());
    }
}

~~~
~~~java
@Component
public class LettuceLockStockFacade {

	private final RedisLockRepository redisLockRepository;
	private final StockService stockService;

	public LettuceLockStockFacade(RedisLockRepository redisLockRepository,
		StockService stockService) {
		this.redisLockRepository = redisLockRepository;
		this.stockService = stockService;
	}

	public void decrease(Long key, Long quantity) throws InterruptedException {
		while (!redisLockRepository.lock(key)) { // 해당 key 가 사용중이면 sleep 을 걸어 계속 대기(spin-lock)
			Thread.sleep(100);
		}
		try {
			stockService.decrease(key, quantity);
		} finally {
			redisLockRepository.unlock(key); // 로직 종료 후 사용한 key 삭제
		}
	}
}
~~~
- 스핀락으로 구현
    - 대기 중인 스레드가 공유 자원의 상태를 무한 루프를 이용해 확인하는 방식

#### redisson
- pub / sub 방식으로 구현 - redis 부하 적음
- 구현이 복잡
- 별도의 library 사용
- 재시도가 필요한 경우 고려

~~~java
    public void decrease(Long id, Long quantity) {
		var lock = redissonClient.getLock(id.toString());
		try {
			boolean available = lock.tryLock(10, 1, TimeUnit.SECONDS);
			if (!available) {
				System.out.println("lock get fail");
				return;
			}
			stockService.decrease(id, quantity);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			lock.unlock();
		}
	}
~~~
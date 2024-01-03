package com.stock_service.facade;

import java.util.concurrent.TimeUnit;

import com.stock_service.service.StockService;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
public class RedissonLockFacade {

	private final RedissonClient redissonClient;
	private final StockService stockService;

	public RedissonLockFacade(RedissonClient redissonClient, StockService stockService) {
		this.redissonClient = redissonClient;
		this.stockService = stockService;
	}

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
}

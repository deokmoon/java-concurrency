package com.stock_service.facade;

import static org.assertj.core.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.stock_service.domain.Stock;
import com.stock_service.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LettuceLockStockFacadeTest {

	@Autowired
	private LettuceLockStockFacade lettuceLockStockFacade;

	@Autowired
	private StockRepository stockRepository;

	@BeforeEach
	void init() {
		Stock stock = new Stock(1L, 100L);
		stockRepository.save(stock);
	}

	@AfterEach
	void clean() {
		stockRepository.deleteAll();
	}

	@Test
	@DisplayName("동시에 재고를 감소시킨다.")
	void concurrencyDecreaseTest() throws InterruptedException {
		//given
		int threadCount = 100;
		ExecutorService executorService = Executors.newFixedThreadPool(32);
		CountDownLatch latch = new CountDownLatch(threadCount);

		for (int i = 0; i < threadCount; i++) {
			executorService.submit(() -> {
				try {
					lettuceLockStockFacade.decrease(1L, 1L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} finally {
					latch.countDown();
				}
			});
		}

		latch.await();

		Stock stock = stockRepository.findById(1L).orElseThrow();
		assertThat(stock.getQuantity()).isZero();
	}
}
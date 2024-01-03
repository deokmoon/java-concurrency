package com.stock_service.service;

import com.stock_service.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

	private final StockRepository stockRepository;

	public StockService(StockRepository stockRepository) {
		this.stockRepository = stockRepository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public synchronized void decrease(Long id, Long quantity) {
		stockRepository.findById(id).ifPresent(stock -> {
			stock.decrease(quantity);
			stockRepository.saveAndFlush(stock);
		});
	}
}

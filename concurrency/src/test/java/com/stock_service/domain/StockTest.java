package com.stock_service.domain;

import static org.assertj.core.api.Assertions.*;

import com.stock_service.domain.Stock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StockTest {

	@Test
	@DisplayName("재고를 감소시킨다.")
	void stockDecreaseTest() {
		// given
		Stock stock = new Stock(1L, 10L);

		// when
		stock.decrease(5L);

		// then
		assertThat(stock.getQuantity()).isEqualTo(5L);
	}

	@Test
	@DisplayName("재고가 부족하면 예외를 발생시킨다.")
	void stockDecreaseExceptionTest() {
		// given
		Stock stock = new Stock(1L, 10L);

		// when
		Throwable throwable = catchThrowable(() -> stock.decrease(11L));

		// then
		assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
	}

}
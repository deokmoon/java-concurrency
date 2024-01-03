package com.stock_service.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Version;

@Entity
public class Stock {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long productId;

	private Long quantity;

	@Version
	private Long version;

	public Stock() {
	}

	public Stock(Long productId, Long quantity) {
		this.productId = productId;
		this.quantity = quantity;
	}

	public Long getQuantity() {
		return this.quantity;
	}

	public void decrease(Long quantity) {
		if (this.quantity < quantity) {
			throw new IllegalArgumentException("재고가 부족합니다.");
		}
		if (this.quantity - quantity < 0) {
			throw new IllegalArgumentException("재고가 부족합니다.");
		}
		this.quantity -= quantity;
	}
}

package khtml.backend.alzi.priceData;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class PriceData {
	@Id
	private String serialNumber;
	private String marketNumber;
	private String marketName;
	private String itemNuber;
	private String itemName;
	private String actualSalesSpecifications;
	private String price;
	private LocalDate date;
	private String note;
	private String marketTypeNumber;
	private String marketType;
	private String boroughCode;
	private String boroughName;

	@Builder
	public PriceData(String serialNumber, String marketNumber, String marketName, String itemNuber, String itemName,
		String actualSalesSpecifications, String price, LocalDate date, String note, String marketTypeNumber,
		String marketType, String boroughCode, String boroughName) {
		this.serialNumber = serialNumber;
		this.marketNumber = marketNumber;
		this.marketName = marketName;
		this.itemNuber = itemNuber;
		this.itemName = itemName;
		this.actualSalesSpecifications = actualSalesSpecifications;
		this.price = price;
		this.date = date;
		this.note = note;
		this.marketTypeNumber = marketTypeNumber;
		this.marketType = marketType;
		this.boroughCode = boroughCode;
		this.boroughName = boroughName;
	}
}

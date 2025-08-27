package khtml.backend.alzi.market;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Data
public class Market {
	@Id
	private String code; // 기존 Primary Key 유지
	
	private String name;
	private String address;
	private String roadNameAddress;
	private String city;
	private String district;
	private String type; // 시장 유형 (전통시장, 대형마트 등)

	@Builder
	public Market(String code, String name, String address, String roadNameAddress, 
	              String city, String district, String type) {
		this.code = code;
		this.name = name;
		this.address = address;
		this.roadNameAddress = roadNameAddress;
		this.city = city;
		this.district = district;
		this.type = type;
	}
}

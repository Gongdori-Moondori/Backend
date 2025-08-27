package khtml.backend.alzi.market;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Entity
@NoArgsConstructor
@Data
public class Market {
	@Id
	private String code;
	private String name;
	private String address;
	private String RoadNameAddress;
	private String city;
	private String district;

	@Builder
	public Market(String code, String name, String address, String roadNameAddress, String city, String district) {
		this.code = code;
		this.name = name;
		this.address = address;
		RoadNameAddress = roadNameAddress;
		this.city = city;
		this.district = district;
	}
}

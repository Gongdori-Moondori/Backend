package khtml.backend.alzi.market;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketRepository extends JpaRepository<Market, String> {
	List<Market> findAllByDistrict(String district);
}

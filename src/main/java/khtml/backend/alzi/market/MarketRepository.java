package khtml.backend.alzi.market;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketRepository extends JpaRepository<Market, String> {
	List<Market> findAllByDistrict(String district);
	Optional<Market> findByName(String name);
	
	// 시장명 존재 여부 확인
	boolean existsByName(String name);
	
	// 여러 시장명으로 조회
	List<Market> findByNameIn(List<String> names);
}

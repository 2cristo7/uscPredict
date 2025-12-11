package usc.uscPredict;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@SpringBootApplication
@EnableJpaRepositories(
    basePackages = "usc.uscPredict.repository",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {usc.uscPredict.repository.RefreshTokenRepository.class}
    )
)
@EnableRedisRepositories(
    basePackages = "usc.uscPredict.repository",
    includeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {usc.uscPredict.repository.RefreshTokenRepository.class}
    )
)
public class UscPredictApplication {

	public static void main(String[] args) {
		SpringApplication.run(UscPredictApplication.class, args);
	}

}

package tripmaster.reward;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import rewardCentral.RewardCentral;

/**
 * Bean class to access rewardCentral library
 */
@Configuration
public class RewardCentralBean {

	@Bean
	public RewardCentral rewardCentral() {
		return new RewardCentral();
	}
	
}

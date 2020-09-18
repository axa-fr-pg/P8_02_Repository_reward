package tripmaster.reward;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import tripmaster.common.user.User;
import tripmaster.common.user.UserAttraction;
import tripmaster.common.user.UserAttractionLists;

/**
 * API class for reward methods
 */
@RestController
public class RewardController {

	private Logger logger = LoggerFactory.getLogger(RewardController.class);
	@Autowired private RewardService rewardService;
	
	/**
	 * Adds new rewards to all users reward lists for each given attraction (if not already rewarded for a given user). 
	 * @param attractionUserLists containing both the user list for which the rewards shall be added and the attraction list for which a reward shall be added (if not already done for a given user).
	 * @return List of users updated with added rewards.
	 */
	@PatchMapping("/addAllNewRewardsAllUsers")
	public List<User> addAllNewRewardsAllUsers(@RequestBody UserAttractionLists attractionUserLists) {
		logger.debug("addAllNewRewardsAllUsers userListName of size = " + attractionUserLists.userList.size() 
			+ " and attractionList of size " + attractionUserLists.attractionList.size());
		return rewardService.addAllNewRewardsAllUsers(attractionUserLists.userList, attractionUserLists.attractionList);
	}

	/**
	 * Calculates the number of reward points for a given user.
	 * @param user for which the calculation shall be done.
	 * @return int number of points.
	 */
	@GetMapping("/sumOfAllRewardPoints")
	public int sumOfAllRewardPoints(@RequestBody User user) {
		logger.debug("getLastUserLocation for User " + user.userName);
		return rewardService.sumOfAllRewardPoints(user);		
	}

	/**
	 * Gets the number of a reward points for a given attraction & user pair. 
	 * @param userAttraction containing both the user and the attraction for which the points shall be calculated.
	 * @return int number of points.
	 * @see rewardCentral.getAttractionRewardPoints
	 */
	@GetMapping("/getRewardPoints")
	public int getRewardPoints(@RequestBody UserAttraction userAttraction) {
		logger.debug("getLastUserLocation for User " + userAttraction.user.userName
		+ " and Attraction " + userAttraction.attraction.name);
	return rewardService.getRewardPoints(userAttraction.attraction, userAttraction.user);
	}
}

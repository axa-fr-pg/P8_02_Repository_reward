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


@RestController
public class RewardController {

	private Logger logger = LoggerFactory.getLogger(RewardController.class);
	@Autowired private RewardService rewardService;
	
	@PatchMapping("/addAllNewRewardsAllUsers")
	public List<User> addAllNewRewardsAllUsers(@RequestBody UserAttractionLists attractionUserLists) {
		logger.debug("addAllNewRewardsAllUsers userListName of size = " + attractionUserLists.userList.size() 
			+ " and attractionList of size " + attractionUserLists.attractionList.size());
		return rewardService.addAllNewRewardsAllUsers(attractionUserLists.userList, attractionUserLists.attractionList);
	}

	@GetMapping("/sumOfAllRewardPoints")
	public int sumOfAllRewardPoints(@RequestBody User user) {
		logger.debug("getLastUserLocation for User " + user.userName);
		return rewardService.sumOfAllRewardPoints(user);		
	}

	@GetMapping("/getRewardPoints")
	public int getRewardPoints(@RequestBody UserAttraction userAttraction) {
		logger.debug("getLastUserLocation for User " + userAttraction.user.userName
		+ " and Attraction " + userAttraction.attraction.name);
	return rewardService.getRewardPoints(userAttraction.attraction, userAttraction.user);
	}
}

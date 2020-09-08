package tripmaster.reward;

import java.util.List;

import tripmaster.common.attraction.AttractionData;
import tripmaster.common.location.VisitedLocationData;
import tripmaster.common.user.User;


public interface RewardService {

	void setProximityMaximalDistance(int proximityBuffer);

	int getProximityMaximalDistance();

	boolean nearAttraction(VisitedLocationData visitedLocation, AttractionData attractionData);

	int getRewardPoints(AttractionData attractionData, User user);

	void addAllNewRewards(User user, List<AttractionData> attractions);

	List<User> addAllNewRewardsAllUsers(List<User> userList, List<AttractionData> attractions);

	int sumOfAllRewardPoints(User user);

}
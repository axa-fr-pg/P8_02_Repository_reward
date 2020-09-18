package tripmaster.reward;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import rewardCentral.RewardCentral;
import tripmaster.common.attraction.AttractionData;
import tripmaster.common.location.LocationData;
import tripmaster.common.location.VisitedLocationData;
import tripmaster.common.user.User;
import tripmaster.common.user.UserReward;

/**
 * Class for reward services. Implements RewardService interface.
 * @see tripmaster.reward.RewardService
 */
@Service
public class RewardServiceImpl implements RewardService {
    private static final int NUMBER_OF_EXPECTED_USER_PARTITIONS = 25;
    private static final int THREAD_POOL_SIZE = NUMBER_OF_EXPECTED_USER_PARTITIONS * 2;
    
    private static final int DEFAULT_PROXIMITY_MAXIMAL_DISTANCE = 10;
	private int proximityMaximalDistance = DEFAULT_PROXIMITY_MAXIMAL_DISTANCE;

	private Logger logger = LoggerFactory.getLogger(RewardServiceImpl.class);
	
	@Autowired private RewardCentral rewardCentral;
	
	/**
	 * Sets the maximal proximity distance used by the nearAttraction method.
	 * @param proximityBuffer the new maximal distance to be considered as within the proximity range.
	 * @see tripmaster.reward.RewardServiceImpl.nearAttraction
	 */
	@Override
	public void setProximityMaximalDistance(int proximityBuffer) {
		this.proximityMaximalDistance = proximityBuffer;
	}
	
	/**
	 * Gets the maximal proximity distance used by the nearAttraction method.
	 * @return int the current maximal distance to be considered as within the proximity range.
	 * @see tripmaster.reward.RewardServiceImpl.nearAttraction
	 */
	@Override
	public int getProximityMaximalDistance() {
		return this.proximityMaximalDistance;
	}
	
	/**
	 * Assesses whether a visited location is within the proxility range of an attraction.
	 * @param visitedLocation the user location to be assessed
	 * @param attractionData the attraction location to be assessed
	 * @return boolean true if both locations have a distance lower or equal to the maximal proximity distance. False otherwise.
	 * @see tripmaster.reward.RewardServiceImpl.getProximityMaximalDistance
	 * @see tripmaster.reward.RewardServiceImpl.setProximityMaximalDistance
	 */
	@Override
	public boolean nearAttraction(VisitedLocationData visitedLocation, AttractionData attractionData) {
		logger.debug("nearAttraction " + attractionData.name);
		LocationData attractionLocation = new LocationData(attractionData.latitude, attractionData.longitude);
		if (attractionLocation.getDistance(visitedLocation.location) > proximityMaximalDistance) {
			return false;
		}
		return true;
	}
	
	/**
	 * Gets the number of a reward points for a given attraction & user pair. 
	 * @param user for which the points shall be calculated
	 * @param attractionData for which the points shall be calculated
	 * @return int number of points.
	 * @see rewardCentral.getAttractionRewardPoints
	 */
	@Override
	public int getRewardPoints(AttractionData attractionData, User user) {
		logger.debug("getRewardPoints userName = " + user.userName + " for attraction " + attractionData.name );
		int points = rewardCentral.getAttractionRewardPoints(attractionData.id, user.userId);
		return points;
	}
	
	/**
	 * Adds a new reward to the user reward list for each given attraction (if not already rewarded). 
	 * @param user for which the rewards shall be added.
	 * @param attractions list of AttractionData for which a reward shall be added (if not already done for this user).
	 */
	@Override
	public void addAllNewRewards(User user, List<AttractionData> attractions)	{
		logger.debug("addAllNewRewards userName = " + user.userName 
			+ " and attractionList of size " + attractions.size());
		for(VisitedLocationData visitedLocation : user.getVisitedLocations()) {
			for(AttractionData attractionData : attractions) {
				long numberOfRewardsOfTheUserForThisAttraction = 
						user.getUserRewards().stream().filter(reward -> 
						reward.attraction.name.equals(attractionData.name)).count();
				if( numberOfRewardsOfTheUserForThisAttraction == 0) {
					if(nearAttraction(visitedLocation, attractionData)) {
						logger.debug("addAllNewRewards new Reward for userName = " + user.userName + " for attraction " + attractionData.name );
						user.addUserReward(new UserReward(visitedLocation, attractionData, getRewardPoints(attractionData, user)));
					}
				}
			}
		}
	}
	
	// For performance reasons it is required to split users for submission on several threads
	private List<List<User>> divideUserList(List<User> userList) {
		List<List<User>> partitionList = new LinkedList<List<User>>();
		int expectedSize = userList.size() / NUMBER_OF_EXPECTED_USER_PARTITIONS;
		if (expectedSize == 0) {
			partitionList.add(userList);
			return partitionList;
		}
		for (int i = 0; i < userList.size(); i += expectedSize) {
			partitionList.add(userList.subList(i, Math.min(i + expectedSize, userList.size())));
		}
		return partitionList;
	}
	
	/**
	 * Adds new rewards to all users reward lists for each given attraction (if not already rewarded for a given user). 
	 * @param userList for which the rewards shall be added.
	 * @param attractions list of AttractionData for which a reward shall be added (if not already done for a given user).
	 * @return List of users updated with added rewards.
	 * @see tripmaster.reward.RewardServiceImpl.addAllNewRewards
	 */
	@Override
	public List<User> addAllNewRewardsAllUsers(List<User> userList, List<AttractionData> attractions)	{
		logger.debug("addAllNewRewardsAllUsers userListName of size = " + userList.size() 
			+ " and attractionList of size " + attractions.size());
		// The number of threads has been defined after several tests to match the performance target
		ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_POOL_SIZE);
		// Divide user list into several parts and submit work separately for these parts
		divideUserList(userList).stream().parallel().forEach( partition -> {
			try {
				logger.debug("addAllNewRewardsAllUsers submits calculation for user partition of size" +  partition.size());
				forkJoinPool.submit( () -> partition.stream().parallel().forEach(user -> {
					addAllNewRewards(user, attractions);
				})).get();
			} catch (InterruptedException | ExecutionException e) {
				logger.error("addAllNewRewardsAllUsers got an exception");
				e.printStackTrace();
				throw new RuntimeException("addAllNewRewardsAllUsers got an exception");
			}
		});
		forkJoinPool.shutdown();
		return userList;
	}
	
	/**
	 * Calculates the number of reward points for a given user.
	 * @param user for which the calculation shall be done.
	 * @return int number of points.
	 */
	@Override
	public int sumOfAllRewardPoints(User user) {
		logger.debug("sumOfAllRewardPoints userName = " + user.userName) ;
		int cumulativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.rewardPoints).sum();
		return cumulativeRewardPoints;
	}
}

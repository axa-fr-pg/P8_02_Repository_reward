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

@Service
public class RewardServiceImpl implements RewardService {
    private static final int NUMBER_OF_EXPECTED_USER_PARTITIONS = 25;
    private static final int THREAD_POOL_SIZE = NUMBER_OF_EXPECTED_USER_PARTITIONS * 2;
    
    private static final int DEFAULT_PROXIMITY_MAXIMAL_DISTANCE = 10;
	private int proximityMaximalDistance = DEFAULT_PROXIMITY_MAXIMAL_DISTANCE;

	private Logger logger = LoggerFactory.getLogger(RewardServiceImpl.class);
	
	@Autowired private RewardCentral rewardCentral;
	
	@Override
	public void setProximityMaximalDistance(int proximityBuffer) {
		this.proximityMaximalDistance = proximityBuffer;
	}
	
	@Override
	public int getProximityMaximalDistance() {
		return this.proximityMaximalDistance;
	}
	
	@Override
	public boolean nearAttraction(VisitedLocationData visitedLocation, AttractionData attractionData) {
		logger.debug("nearAttraction " + attractionData.name);
		LocationData attractionLocation = new LocationData(attractionData.latitude, attractionData.longitude);
		if (attractionLocation.getDistance(visitedLocation.location) > proximityMaximalDistance) {
			return false;
		}
		return true;
	}
	
	@Override
	public int getRewardPoints(AttractionData attractionData, User user) {
		logger.debug("getRewardPoints userName = " + user.userName + " for attraction " + attractionData.name );
		int points = rewardCentral.getAttractionRewardPoints(attractionData.id, user.userId);
		return points;
	}
	
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
	
	@Override
	public List<User> addAllNewRewardsAllUsers(List<User> userList, List<AttractionData> attractions)	{
		logger.debug("addAllNewRewardsAllUsers userListName of size = " + userList.size() 
			+ " and attractionList of size " + attractions.size());
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
	
	@Override
	public int sumOfAllRewardPoints(User user) {
		logger.debug("sumOfAllRewardPoints userName = " + user.userName) ;
		int cumulativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.rewardPoints).sum();
		return cumulativeRewardPoints;
	}
}

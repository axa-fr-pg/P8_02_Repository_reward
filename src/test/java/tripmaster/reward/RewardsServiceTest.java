package tripmaster.reward;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import rewardCentral.RewardCentral;
import tripmaster.common.attraction.AttractionData;
import tripmaster.common.location.LocationData;
import tripmaster.common.location.VisitedLocationData;
import tripmaster.common.user.User;
import tripmaster.common.user.UserReward;
import tripmaster.reward.RewardService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RewardsServiceTest {
	
	@Autowired RewardService rewardService;
	@MockBean RewardCentral rewardCentral;
	
	private static final int REWARD_POINTS_PER_ATTRACTION = 123;
	public final static double LATITUDE_REWARD_ONE = 0.21;
	public final static double LONGITUDE_REWARD_ONE = -0.00022;
	public final static double LATITUDE_ATTRACTION_ONE = 0.31;
	public final static double LONGITUDE_ATTRACTION_ONE = -0.00032;
	private static User user;
	private UUID nullId = new UUID(0,0);

	@Before
	public void prepareUser() {
		user = new User(new UUID(11,12), "user_name", "user_phone", "user_email");
	}
	
	@Test
	public void givenPrerequisitesToAdd1RewardOk_whenAddAllNewRewards_thenAddsCorrectReward() {
		// MOCK rewardCentral
		AttractionData expectedAttraction = new AttractionData(nullId, "attraction_name", "attraction_city", "attraction_state", 0.31, -0.32);
		AttractionData tooFarAttraction = new AttractionData(nullId, "far_name", "far_city", "far_state", 99, -99);
		when(rewardCentral.getAttractionRewardPoints(eq(expectedAttraction.id), eq(user.userId)))
			.thenReturn(REWARD_POINTS_PER_ATTRACTION);
		List<AttractionData> attractions = Arrays.asList(expectedAttraction, tooFarAttraction);
		assertEquals(2, attractions.size());
		// GIVEN user was close enough to the attraction
		rewardService.setProximityMaximalDistance(10); // statute miles
		double latitudeDifferenceMakingItCloseEnough = 0.14; // degrees
		LocationData location = new LocationData(
				expectedAttraction.latitude + latitudeDifferenceMakingItCloseEnough,
				expectedAttraction.longitude);
		VisitedLocationData visitedLocation = new VisitedLocationData(user.userId, location, new Date(0));
		user.addToVisitedLocations(visitedLocation);
		// WHEN
		rewardService.addAllNewRewards(user, attractions);
		List<UserReward> userRewards = user.getUserRewards();
		// THEN
		assertNotNull(userRewards);
		assertEquals(1, userRewards.size());
		assertNotNull(userRewards.get(0));
		assertEquals(REWARD_POINTS_PER_ATTRACTION, userRewards.get(0).rewardPoints);
	}

	@Test
	public void givenTooFarToAddReward_whenAddAllNewRewards_thenAddsNoReward() {
		// MOCK rewardCentral
		AttractionData attraction = new AttractionData(nullId, "attraction_name", "attraction_city", "attraction_state", 0.31, -0.32);
		when(rewardCentral.getAttractionRewardPoints(eq(attraction.id), eq(user.userId)))
			.thenReturn(REWARD_POINTS_PER_ATTRACTION);
		List<AttractionData> attractions = Arrays.asList(attraction);
		// GIVEN user was close enough to the attraction
		rewardService.setProximityMaximalDistance(10); // statute miles
		double latitudeDifferenceMakingItTooFar = 0.15; // degrees
		LocationData location = new LocationData(attraction.latitude + latitudeDifferenceMakingItTooFar, attraction.longitude);
		VisitedLocationData visitedLocation = new VisitedLocationData(user.userId, location, new Date(0));
		user.addToVisitedLocations(visitedLocation);
		// WHEN
		rewardService.addAllNewRewards(user, attractions);
		List<UserReward> userRewards = user.getUserRewards();
		// THEN
		assertNotNull(userRewards);
		assertEquals(0, userRewards.size());
	}

	@Test
	public void givenAlreadyRewardedVisit_whenAddAllNewRewards_thenAddsNoReward() {
		// MOCK rewardCentral
		AttractionData expectedAttraction = new AttractionData(nullId, "attraction_name", "attraction_city", "attraction_state", 0.31, -0.32);
		AttractionData tooFarAttraction = new AttractionData(nullId, "far_name", "far_city", "far_state", 99, -99);
		when(rewardCentral.getAttractionRewardPoints(eq(expectedAttraction.id), eq(user.userId)))
			.thenReturn(999 + REWARD_POINTS_PER_ATTRACTION);
		List<AttractionData> attractions = Arrays.asList(expectedAttraction, tooFarAttraction);
		assertEquals(2, attractions.size());
		// GIVEN user has already been rewarded for this attraction
		rewardService.setProximityMaximalDistance(10); // statute miles
		double latitudeDifferenceMakingItCloseEnough = 0.14; // degrees
		LocationData location = new LocationData(expectedAttraction.latitude + latitudeDifferenceMakingItCloseEnough, expectedAttraction.longitude);
		VisitedLocationData visitedLocation = new VisitedLocationData(user.userId, location, new Date(0));
		user.addToVisitedLocations(visitedLocation);
		UserReward userReward = new UserReward(visitedLocation, expectedAttraction, REWARD_POINTS_PER_ATTRACTION);
		user.addUserReward(userReward);
		// WHEN
		rewardService.addAllNewRewards(user, attractions);
		List<UserReward> userRewards = user.getUserRewards();
		// THEN
		assertNotNull(userRewards);
		assertEquals(1, userRewards.size());
		assertNotNull(userRewards.get(0));
		assertEquals(REWARD_POINTS_PER_ATTRACTION, userRewards.get(0).rewardPoints);
	}

	@Test
	public void givenMaximalProximityBuffer_whenAddAllNewRewards_thenAddsRewardsForAllAttractions() {
		// GIVEN
		int numberOfTestAttractions = 999;
		List<AttractionData> attractions = generateAllAttractions(numberOfTestAttractions);
		// MOCK rewardCentral
		when(rewardCentral.getAttractionRewardPoints(any(UUID.class), eq(user.userId)))
			.thenReturn(REWARD_POINTS_PER_ATTRACTION);
		// GIVEN user is close enough to all attractions
		rewardService.setProximityMaximalDistance((Integer.MAX_VALUE/2 ) -1);
		LocationData location = new LocationData();
		VisitedLocationData visitedLocation = new VisitedLocationData(user.userId, location, new Date(0));
		user.addToVisitedLocations(visitedLocation);
		// WHEN
		rewardService.addAllNewRewards(user, attractions);
		List<UserReward> userRewards = user.getUserRewards();
		// THEN
		assertNotNull(userRewards);
		assertEquals(numberOfTestAttractions, userRewards.size());
		userRewards.forEach( reward -> {
			assertNotNull(reward);
			assertEquals(REWARD_POINTS_PER_ATTRACTION, reward.rewardPoints);
		});
	}
	
	@Test
	public void givenUserRewards_whenSumOfAllRewardPoints_thenReturnsCorrectTotal() {
		// GIVEN
		givenMaximalProximityBuffer_whenAddAllNewRewards_thenAddsRewardsForAllAttractions();
		int numberOfRewards = user.getUserRewards().size();
		// WHEN
		int totalPoints = rewardService.sumOfAllRewardPoints(user);
		// THEN
		assertEquals(numberOfRewards * REWARD_POINTS_PER_ATTRACTION, totalPoints);
	}
	
	@Test
	public void givenPrerequisitesToAddRewardsOk_whenAddAllNewRewardsAllUsers_thenAddsRewardsForAllUsers() {
		// GIVEN test Users
		User user1 = generateUser(1);
		User user2 = generateUser(2);
		List<User> initialUsers = Arrays.asList(user1, user2);
		// GIVEN test Attractions
		List<AttractionData> attractions = generateAllAttractions(2);
		// MOCK rewardCentral
		when(rewardCentral.getAttractionRewardPoints(any(UUID.class), eq(user1.userId)))
			.thenReturn(REWARD_POINTS_PER_ATTRACTION);
		when(rewardCentral.getAttractionRewardPoints(any(UUID.class), eq(user2.userId)))
			.thenReturn(2 * REWARD_POINTS_PER_ATTRACTION);
		// GIVEN each user is close to one attraction
		rewardService.setProximityMaximalDistance(8); // so that only one attraction matches per user
		// WHEN
		List<User> updatedUsers = rewardService.addAllNewRewardsAllUsers(initialUsers, attractions);
		// THEN
		int totalRewardPoints = 0;
		for (User u : updatedUsers) {
			assertNotNull(u);
			assertNotNull(u.getUserRewards());
			assertEquals(1, u.getUserRewards().size());
			totalRewardPoints += u.getUserRewards().get(0).rewardPoints;
		}
		assertEquals(3 * REWARD_POINTS_PER_ATTRACTION, totalRewardPoints);
	}
	
	private List<AttractionData> generateAllAttractions(int numberOfTestAttractions) {
		List<AttractionData> attractions = new ArrayList<AttractionData>();	
		for (int i=0; i<numberOfTestAttractions; i++) {
			int index = numberOfTestAttractions - i;
			AttractionData attraction = new AttractionData(nullId, "name"+index, "city"+index, "state"+index, 
					LATITUDE_ATTRACTION_ONE*index, LONGITUDE_ATTRACTION_ONE*index);	
			attractions.add(attraction);
		}
		return attractions;
	}

	private User generateUser(int index) {
		User user = new User(new UUID(11*index,12*index), "name"+index, "phone"+index, "email"+index);
		user.addToVisitedLocations(generateVisitedLocation(user.userId, index));
		return user;
	}
	
	private VisitedLocationData generateVisitedLocation(UUID userId, int index) {
		LocationData location = new LocationData(LATITUDE_REWARD_ONE*index,LONGITUDE_REWARD_ONE*index);
		VisitedLocationData visitedLocation = new VisitedLocationData(userId, location, new Date(index));
		return visitedLocation;
	}
}

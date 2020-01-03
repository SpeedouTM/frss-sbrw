package com.soapboxrace.core.bo;

import com.soapboxrace.core.dao.*;
import com.soapboxrace.core.jpa.*;
import com.soapboxrace.jaxb.http.ExitPath;
import com.soapboxrace.jaxb.http.PursuitArbitrationPacket;
import com.soapboxrace.jaxb.http.PursuitEventResult;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.NotAuthorizedException;

@Stateless
public class EventResultPursuitBO {

	@EJB
	private EventSessionDAO eventSessionDao;

	@EJB
	private EventDataDAO eventDataDao;

	@EJB
	private AchievementDAO achievementDAO;

	@EJB
	private PersonaDAO personaDAO;

	@EJB
	private AchievementsBO achievementsBO;

	@EJB
	private RewardPursuitBO rewardPursuitBO;

	@EJB
	private CarDamageBO carDamageBO;

	@EJB
	private OwnedCarDAO ownedCarDAO;

	public PursuitEventResult handlePursitEnd(EventSessionEntity eventSessionEntity, Long activePersonaId, PursuitArbitrationPacket pursuitArbitrationPacket,
			Boolean isBusted) {
		Long eventSessionId = eventSessionEntity.getId();
		eventSessionEntity.setEnded(System.currentTimeMillis());

		eventSessionDao.update(eventSessionEntity);

		EventDataEntity eventDataEntity = eventDataDao.findByPersonaAndEventSessionId(activePersonaId, eventSessionId);
		eventDataEntity.setAlternateEventDurationInMilliseconds(pursuitArbitrationPacket.getAlternateEventDurationInMilliseconds());
		eventDataEntity.setCarId(pursuitArbitrationPacket.getCarId());
		eventDataEntity.setCopsDeployed(pursuitArbitrationPacket.getCopsDeployed());
		eventDataEntity.setCopsDisabled(pursuitArbitrationPacket.getCopsDisabled());
		eventDataEntity.setCopsRammed(pursuitArbitrationPacket.getCopsRammed());
		eventDataEntity.setCostToState(pursuitArbitrationPacket.getCostToState());
		eventDataEntity.setEventDurationInMilliseconds(pursuitArbitrationPacket.getEventDurationInMilliseconds());
		eventDataEntity.setEventModeId(eventDataEntity.getEvent().getEventModeId());
		eventDataEntity.setFinishReason(pursuitArbitrationPacket.getFinishReason());
		eventDataEntity.setHacksDetected(pursuitArbitrationPacket.getHacksDetected());
		eventDataEntity.setHeat(pursuitArbitrationPacket.getHeat());
		eventDataEntity.setInfractions(pursuitArbitrationPacket.getInfractions());
		eventDataEntity.setLongestJumpDurationInMilliseconds(pursuitArbitrationPacket.getLongestJumpDurationInMilliseconds());
		eventDataEntity.setPersonaId(activePersonaId);
		eventDataEntity.setRoadBlocksDodged(pursuitArbitrationPacket.getRoadBlocksDodged());
		eventDataEntity.setSpikeStripsDodged(pursuitArbitrationPacket.getSpikeStripsDodged());
		eventDataEntity.setSumOfJumpsDurationInMilliseconds(pursuitArbitrationPacket.getSumOfJumpsDurationInMilliseconds());
		eventDataEntity.setTopSpeed(pursuitArbitrationPacket.getTopSpeed());
		eventDataDao.update(eventDataEntity);

		PursuitEventResult pursuitEventResult = new PursuitEventResult();
		pursuitEventResult.setAccolades(rewardPursuitBO.getPursuitAccolades(activePersonaId, pursuitArbitrationPacket, eventSessionEntity, isBusted));
		pursuitEventResult.setDurability(carDamageBO.updateDamageCar(activePersonaId, pursuitArbitrationPacket, 0));
		pursuitEventResult.setEventId(eventDataEntity.getEvent().getId());
		pursuitEventResult.setEventSessionId(eventSessionId);
		pursuitEventResult.setExitPath(ExitPath.EXIT_TO_FREEROAM);
		pursuitEventResult.setHeat(isBusted ? 1 : pursuitArbitrationPacket.getHeat());
		pursuitEventResult.setInviteLifetimeInMilliseconds(0);
		pursuitEventResult.setLobbyInviteId(0);
		pursuitEventResult.setPersonaId(activePersonaId);

		PersonaEntity persona = personaDAO.findById(activePersonaId);
		OwnedCarEntity ownedCarEntity = ownedCarDAO.findById(pursuitArbitrationPacket.getCarId());
		if (!ownedCarEntity.getCarSlot().getPersona().getPersonaId().equals(activePersonaId)) {
			throw new NotAuthorizedException("That's not your car!");
		}
		ownedCarEntity.setHeat(pursuitEventResult.getHeat());
		ownedCarDAO.update(ownedCarEntity);

		achievementsBO.update(persona,
				achievementDAO.findByName("achievement_ACH_CLOCKED_AIRTIME"),
				pursuitArbitrationPacket.getSumOfJumpsDurationInMilliseconds());
		achievementsBO.update(persona,
				achievementDAO.findByName("achievement_ACH_INCUR_COSTTOSTATE"),
				(long) pursuitArbitrationPacket.getCostToState());
		
		if (!isBusted) {
			// achievement_ACH_PURSUIT
			achievementsBO.update(persona,
					achievementDAO.findByName("achievement_ACH_PURSUIT"),
					1L);
		}

		AchievementDefinitionEntity achievement1 = achievementDAO.findByName("achievement_ACH_DRIVE_MILES");
		Float distance = eventDataEntity.getEvent().getRanksDistance();
		if (achievement1 != null && distance != null) {
			achievementsBO.update(persona, achievement1, (long) (distance * 1000f));
		}
		
		return pursuitEventResult;
	}

}

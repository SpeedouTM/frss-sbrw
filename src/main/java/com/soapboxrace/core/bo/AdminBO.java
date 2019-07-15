package com.soapboxrace.core.bo;

import com.soapboxrace.core.api.util.MiscUtils;
import com.soapboxrace.core.dao.BanDAO;
import com.soapboxrace.core.dao.PersonaDAO;
import com.soapboxrace.core.dao.UserDAO;
import com.soapboxrace.core.jpa.BanEntity;
import com.soapboxrace.core.jpa.PersonaEntity;
import com.soapboxrace.core.jpa.UserEntity;
import com.soapboxrace.core.xmpp.OpenFireSoapBoxCli;
import com.soapboxrace.core.xmpp.XmppChat;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Stateless
public class AdminBO {
	@EJB
	private TokenSessionBO tokenSessionBo;

	@EJB
	private PersonaDAO personaDao;

	@EJB
	private UserDAO userDao;

	@EJB
	private BanDAO banDAO;

	@EJB
	private OpenFireSoapBoxCli openFireSoapBoxCli;

	@EJB
	private ParameterBO parameterBO;

	public void sendCommand(Long personaId, Long abuserPersonaId, String command)
	{
		CommandInfo commandInfo = CommandInfo.parse(command);
		PersonaEntity personaEntity = personaDao.findById(abuserPersonaId);

		if (personaEntity == null)
			return;

		switch (commandInfo.action)
		{
			case BAN:
				if (banDAO.findByUser(personaEntity.getUser()) != null) {
					openFireSoapBoxCli.send(XmppChat.createSystemMessage("User is already banned!"), personaId);
					break;
				}

				sendBan(personaEntity, personaDao.findById(personaId), commandInfo.timeEnd, commandInfo.reason);
				openFireSoapBoxCli.send(XmppChat.createSystemMessage("Banned user!"), personaId);
				break;
			case KICK:
				sendKick(personaEntity.getUser().getId(), personaEntity.getPersonaId());
				openFireSoapBoxCli.send(XmppChat.createSystemMessage("Kicked user!"), personaId);
				break;
			case UNBAN:
				BanEntity existingBan;
				if ((existingBan = banDAO.findByUser(personaEntity.getUser())) == null) {
					openFireSoapBoxCli.send(XmppChat.createSystemMessage("User is not banned!"), personaId);
					break;
				}

				banDAO.delete(existingBan);
				openFireSoapBoxCli.send(XmppChat.createSystemMessage("Unbanned user!"), personaId);

				break;
			default:
				break;
		}
	}

	private void sendBan(PersonaEntity personaEntity, PersonaEntity bannedBy, LocalDateTime endsOn, String reason)
	{
		UserEntity userEntity = personaEntity.getUser();
		BanEntity banEntity = new BanEntity();
		banEntity.setUserEntity(userEntity);
		banEntity.setEndsAt(endsOn);
		banEntity.setStarted(LocalDateTime.now());
		banEntity.setReason(reason);
		banEntity.setBannedBy(bannedBy);
		banEntity.setHwid(userEntity.getGameHardwareHash());
		banEntity.setIp(userEntity.getIpAddress());
		banDAO.insert(banEntity);
		sendKick(userEntity.getId(), personaEntity.getPersonaId());
		banWebhook(banEntity, personaEntity);
	}

	private void sendKick(Long userId, Long personaId)
	{
		openFireSoapBoxCli.send("<NewsArticleTrans><ExpiryTime><", personaId);
		tokenSessionBo.deleteByUserId(userId);
	}

	private void banWebhook(BanEntity banEntity, PersonaEntity personaEntity) {
		if (parameterBO.getStrParam("BAN_WEBHOOK") == null) {
			return;
		}
		Map<String, Object> field1 = new HashMap<>();
		field1.put("name", "Player");
		field1.put("value", personaEntity.getName());
		field1.put("inline", true);

		Map<String, Object> field2 = new HashMap<>();
		field2.put("name", "Reason");
		field2.put("value", banEntity.getReason());
		field1.put("inline", true);

		Map<String, Object> embed = new HashMap<>();
		embed.put("title", ":hammer: Player banned");
		embed.put("color", 0xff0000);
		ZonedDateTime zonedDateTime = banEntity.getStarted().atZone(ZoneId.systemDefault());
		embed.put("timestamp", zonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
		embed.put("fields", Arrays.asList(field1, field2));

		Map<String, Object> map = new HashMap<>();
		map.put("embeds", Collections.singletonList(embed));
		ClientBuilder.newClient()
				.target(parameterBO.getStrParam("BAN_WEBHOOK"))
				.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.json(map));
	}

	private static class CommandInfo
	{
		public CommandInfo.CmdAction action;
		public String reason;
		public LocalDateTime timeEnd;

		public enum CmdAction
		{
			KICK,
			BAN,
			ALERT,
			UNBAN,
			UNKNOWN
		}

		public static CommandInfo parse(String cmd)
		{
			cmd = cmd.replaceFirst("/", "");

			String[] split = cmd.split(" ");
			CommandInfo.CmdAction action;
			CommandInfo info = new CommandInfo();

			switch (split[0].toLowerCase().trim())
			{
				case "ban":
					action = CmdAction.BAN;
					break;
				case "kick":
					action = CmdAction.KICK;
					break;
				case "unban":
					action = CmdAction.UNBAN;
					break;
				default:
					action = CmdAction.UNKNOWN;
					break;
			}

			info.action = action;

			switch (action)
			{
				case BAN:
				{
					LocalDateTime endTime;
					String reason = null;

					if (split.length >= 2)
					{
						long givenTime = MiscUtils.lengthToMiliseconds(split[1]);
						if (givenTime != 0)
						{
							endTime = LocalDateTime.now().plusSeconds(givenTime / 1000);
							info.timeEnd = endTime;

							if (split.length > 2)
							{
								reason = MiscUtils.argsToString(split, 2, split.length);
							}
						} else
						{
							reason = MiscUtils.argsToString(split, 1, split.length);
						}
					}

					info.reason = reason;
					break;
				}
			}

			return info;
		}
	}
}
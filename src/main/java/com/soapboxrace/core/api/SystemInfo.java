package com.soapboxrace.core.api;

import com.soapboxrace.core.api.util.Secured;
import com.soapboxrace.core.api.util.VersionUtil;
import com.soapboxrace.core.bo.ParameterBO;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;

@Path("/systeminfo")
public class SystemInfo {

	@EJB
	private ParameterBO parameterBO;

	@GET
	@Secured
	@Produces(MediaType.APPLICATION_XML)
	public com.soapboxrace.jaxb.http.SystemInfo systemInfo() {
		com.soapboxrace.jaxb.http.SystemInfo systemInfo = new com.soapboxrace.jaxb.http.SystemInfo();
		systemInfo.setBranch("production");
		systemInfo.setChangeList("620384");
		systemInfo.setClientVersion("637");
		systemInfo.setClientVersionCheck(true);
		systemInfo.setDeployed("08/20/2013 11:24:40");
		systemInfo.setEntitlementsToDownload(true);
		systemInfo.setForcePermanentSession(true);
		systemInfo.setJidPrepender("sbrw");
		systemInfo.setLauncherServiceUrl("http://127.0.0.1");
		systemInfo.setNucleusNamespace("sbrw-live");
		systemInfo.setNucleusNamespaceWeb("sbr_web");
		systemInfo.setPersonaCacheTimeout(900);
		String portalDomain = parameterBO.getStrParam("PORTAL_DOMAIN");
		if (portalDomain == null) {
			portalDomain = "localhost";
		}
		systemInfo.setPortalDomain(portalDomain);
		systemInfo.setPortalStoreFailurePage(portalDomain + "/fail");
		systemInfo.setPortalTimeOut("6000");
		systemInfo.setShardName("CORE");
		GregorianCalendar c = new GregorianCalendar();
		try {
			XMLGregorianCalendar date2 = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
			systemInfo.setTime(date2);
		} catch (Exception e) {
			e.printStackTrace();
		}
		systemInfo.setVersion(VersionUtil.getVersionHash());
		return systemInfo;
	}
}

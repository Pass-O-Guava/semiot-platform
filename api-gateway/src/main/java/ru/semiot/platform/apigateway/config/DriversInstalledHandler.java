package ru.semiot.platform.apigateway.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.semiot.platform.apigateway.beans.TSDBQueryService;
import ru.semiot.platform.apigateway.beans.impl.ExternalQueryService;
import ru.semiot.platform.apigateway.beans.impl.OSGiApiService;
import ru.semiot.platform.apigateway.beans.impl.SPARQLQueryService;
import rx.Observable;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/config/DriversInstalled", asyncSupported = true)
public class DriversInstalledHandler extends HttpServlet {

  private static final Logger logger = LoggerFactory.getLogger(DriversInstalledHandler.class);
  public static final String QUERY_ID_SYSTEMS_SERNSORS_FOR_DRIVER = "SELECT DISTINCT "
      + "?system_id ?sensor_id  " 
        + "WHERE { "
        + "GRAPH <urn:semiot:graphs:private> { "
          + "?system  semiot:hasDriver "
          + "<urn:semiot:drivers:${DRIVER}> } "
        + "?system  dcterms:identifier ?system_id; "
        + "ssn:hasSubSystem ?sensor. "
        + "?sensor dcterms:identifier ?sensor_id. }";
  
  public static final String QUERY_ID_SYSTEMS_PROCESSES_FOR_DRIVER = "SELECT DISTINCT "
      + "?system_id ?process_id  " 
        + "WHERE { "
        + "GRAPH <urn:semiot:graphs:private> { "
          + "?system  semiot:hasDriver "
          + "<urn:semiot:drivers:${DRIVER}> } "
        + "?system  dcterms:identifier ?system_id; "
        + "semiot:supportedProcess ?process. "
        +  "?process dcterms:identifier ?process_id. }";

  @Inject
  OSGiApiService service;

  @Inject
  SPARQLQueryService query;

  @Inject
  TSDBQueryService tsdbQuery;

  @Inject
  ExternalQueryService externalService;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    final AsyncContext ctx = req.startAsync();

    Observable<JsonArray> jsonArray = service.getBundlesJsonArray();

    jsonArray.subscribe((jsArray) -> ctx.getRequest().setAttribute("jsonArray", jsArray),
        (Throwable e) -> logger.warn(e.getMessage(), e),
        () -> ctx.dispatch("/configuration/DriversInstalled"));
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    HashMap<String, String> parameters = getRequestParameters(request);
    String pid = parameters.get("id_bundle");
    if (StringUtils.isNotBlank(pid)) {
      if (request.getParameter("uninstall") != null) {
        final AsyncContext ctx = request.startAsync();
        Observable<String> unistall = service.sendPostUninstall(pid);
        unistall.map((__) -> {
          Observable<JsonArray> jsonArray = service.getBundlesJsonArray();
          JsonArray jsArray = jsonArray.toBlocking().single();
          ctx.getRequest().setAttribute("jsonArray", jsArray);
          return jsArray;
        }).subscribe(ConfigHelper.dispatch(ctx, "/configuration/DriversInstalled"));
      } else if (request.getParameter("uninstallWithDeleteData") != null) {
        final AsyncContext ctx = request.startAsync();
        Observable<String> unistall = service.sendPostUninstall(pid);
        unistall.map((__) -> {
          Observable<ResultSet> obsSystemsRS = query.select(
              QUERY_ID_SYSTEMS_SERNSORS_FOR_DRIVER.replace("${DRIVER}", pid));
          ResultSet systemsSensorsRS = obsSystemsRS.toBlocking().single();
          Observable<ResultSet> commresSystemsRS = query.select(
              QUERY_ID_SYSTEMS_PROCESSES_FOR_DRIVER.replace("${DRIVER}", pid));
          ResultSet systemsProcessesRS = commresSystemsRS.toBlocking().single(); 
          tsdbQuery.remove(systemsSensorsRS, systemsProcessesRS).subscribe();
          externalService.sendRsRemoveFromFuseki(pid).subscribe();
          Observable<JsonArray> jsonArray = service.getBundlesJsonArray();
          JsonArray jsArray = jsonArray.toBlocking().single();
          ctx.getRequest().setAttribute("jsonArray", jsArray);
          return jsArray;
        }).subscribe(ConfigHelper.dispatch(ctx, "/configuration/DriversInstalled"));
      }
    }
  }

  private static HashMap<String, String> getRequestParameters(
      HttpServletRequest request) {
    HashMap<String, String> parameters = new HashMap<String, String>();
    Enumeration _enum = request.getParameterNames();
    while (_enum.hasMoreElements()) {
      String key = (String) _enum.nextElement();
      String value = request.getParameter(key);
      // request.get
      parameters.put(key, value);
    }
    return parameters;
  }
}

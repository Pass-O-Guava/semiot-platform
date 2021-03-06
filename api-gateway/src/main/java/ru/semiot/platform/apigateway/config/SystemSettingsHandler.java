package ru.semiot.platform.apigateway.config;

import ru.semiot.platform.apigateway.beans.impl.OSGiApiService;
import rx.Observable;

import java.io.IOException;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/config/SystemSettings", asyncSupported = true)
public class SystemSettingsHandler extends HttpServlet {

  @Inject
  OSGiApiService service;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    final AsyncContext ctx = req.startAsync();

    Observable<JsonObject> jsonProperties = service.getConfiguration(BundleConstants.MANAGER_PID);
    Observable<Boolean> managerIsConfigurated = service.managerIsConfigurated();

    Observable.zip(jsonProperties, managerIsConfigurated,
        (jsProperties, mngrIsConfigurated) -> {
          JsonObject jsonDomain = jsProperties.getJsonObject(BundleConstants.MANAGER_DOMAIN);

          ctx.getRequest().setAttribute("jsonDomain", jsonDomain);
          ctx.getRequest().setAttribute("mngrIsConfigurated", mngrIsConfigurated);

          return new String();
        }).subscribe(ConfigHelper.dispatch(ctx, "/configuration/SystemSettings"));
  }

}

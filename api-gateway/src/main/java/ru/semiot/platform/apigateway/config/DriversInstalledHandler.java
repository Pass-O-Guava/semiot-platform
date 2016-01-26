package ru.semiot.platform.apigateway.config;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map.Entry;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/DriverInstalledHandler")
public class DriversInstalledHandler extends HttpServlet {
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    	HashMap<String, String> parameters = getRequestParameters(request);
    	
    	if (request.getParameter("uninstall") != null) {
    		try {
    			QueryUtils.uninstall(parameters.get("id_bundle"));
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
        }

    	request.getRequestDispatcher("/config/DriversInstalled").forward(request, response);
    }
    
    private static HashMap<String, String> getRequestParameters(HttpServletRequest request) {
        HashMap<String, String> parameters = new HashMap<String, String>();
        Enumeration _enum = request.getParameterNames();
        while (_enum.hasMoreElements()) {
            String key = (String) _enum.nextElement();
            String value = request.getParameter(key);
            //request.get
            parameters.put(key, value);
            System.out.println(key + " " + value);
        }
        return parameters;
    }
    
}

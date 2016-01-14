<%@page import="java.util.HashMap"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="ru.semiot.platform.apigateway.config.HttpClientConfig"%>
<%@page import="java.util.Iterator"%>

<%
	String pid = String.valueOf(request.getAttribute("symbolicName"));
	String url = "http://localhost:8181/system/console/configMgr/" + pid;
	HashMap<String, String> hmap = new HashMap<String, String>(); 
	// hmap.put("post", "true");
	HttpClientConfig clientConfig = new HttpClientConfig();
	JSONObject jsonObject = new JSONObject(clientConfig.sendPost(url, null, null));
	JSONObject jsonProperties = jsonObject.getJSONObject("properties");
	
%>

<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>

<head>
  <title>Configuration Driver</title>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="http://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css">
  <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
  <script src="http://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js"></script>
</head>
	<body>
		<div class="container">
		<h3>Configuration Driver</h3>
			<form action="${pageContext.request.contextPath}/ConfigurationDriverHandler" method="post">
				<div class="small-table">
					<table class="table table-hover">
			            <tr>
			            <tr>
			            	<th>№</th>
			                <th>Name</th>
			                <th>Value</th>
			                <th>Optional</th>
			            </tr>
							<%
		                	Iterator<String> it = jsonProperties.keys();
		                	int i = 0;
		                	while(it.hasNext()) {
		                		String key = it.next();
		                		JSONObject jsonProperty = jsonProperties.getJSONObject(key);
			                %>
			                <td><%=++i%>
			                <td><%=jsonProperty.get("name")%>
			                <td><input type="text" id="txtfld<%=i%>" onClick="SelectAll('txtfld<%=i%>');" 
			                	name=<%=key%> style="width:200px" 
			                	value = <%=jsonProperty.get("value")%> />
			                <td><%=jsonProperty.get("optional")%>
			            </tr>
			            <%
				            }
				        %>
					</table>
				</div>
				<div class="text-right">
		            <button class="btn btn-primary btn-sm" name="save" type="submit">Save</button>
		        </div>
		        <input type="hidden" name="url" id="url" value=<%=url%> />
			</form>
		
		<script>
		function SelectAll(id)
		{
		    document.getElementById(id).focus();
		    document.getElementById(id).select();
		}
		</script>
	</body>
</html>
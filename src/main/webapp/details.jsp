<%@page import="java.util.ArrayList"%>
<%@page contentType="text/html; charset=UTF-8" %>
<%@page import="com.github.jochenw.reqrec.model.RequestRegistry.Request"%>
<%@page import="java.util.List"%>
<%@page import="com.github.jochenw.reqrec.app.App"%>
<%@page import="com.github.jochenw.reqrec.model.RequestRegistry.RequestInfo"%>
<%@page import="java.io.IOException" %>
<%@page import="java.io.UncheckedIOException" %>
<%
  final String id = request.getParameter("id");
  if (id == null  ||  id.length() == 0) {
    throw new IllegalStateException("Missing, or empty, parameter: id");
  }
  final Request r = App.getInstance().getRequestRegistry().getRequest(id);
  final RequestInfo ri = r.getRequestInfo();
%>
<html>
  <head>
    <title>Request details for Request Id: <%= id %></title>
  </head>
  <body>
    <h1>Request details for Request Id: <%= id %></h1>
    <table>
      <tr>
        <th align="right">Id:</th><td><%= ri.getId() %></td>
      </tr>
      <tr>
        <th align="right">Request URI:</th><td><%= ri.getRequestUri() %></td>
      </tr>
      <tr>
        <th align="right">Request Method:</th><td><%= ri.getMethod() %></td>
      </tr>
      <tr>
        <th align="right">Local Address:</th><td><%= ri.getLocalAddr() + ":" + ri.getLocalPort() %></td>
      </tr>
      <tr>
        <th align="right">Remote Address:</th><td><%= ri.getRemoteAddr() + ":" + ri.getRemotePort() %></td>
      </tr>
      <tr>
        <th align="right">Headers</th>
        <td>
          <table>
            <tr><th>Name</th><th>Value</th></tr>
            <% final List<String> hdrs = new ArrayList<String>(); %>
            <% ri.headers((n,v) -> { hdrs.add(n); hdrs.add(v); }); %>
            <% for (int i = 0;  i < hdrs.size();  i += 2) { %>
            <% final String n = hdrs.get(i); %>
            <% final String v = hdrs.get(i+1); %>
              <tr><td><%= n %></td><td><%= v %></td></tr>
            <% } %>
          </table>
        </td>
      </tr>
<% if (r.hasBody()) { %>
      <tr>
        <th align="right">Body:</th><td><textarea name="bodytext"><%= r.getText("raw") %></textarea></td>
      </tr>
<% } %>
    </table>
  </body>
</html>

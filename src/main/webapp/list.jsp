<%@page contentType="text/html; charset=UTF-8"
        import="java.util.List,com.github.jochenw.reqrec.app.App,com.github.jochenw.reqrec.model.RequestRegistry.RequestInfo" %>
<html>
  <head>
    <title>List of recorded requests</title>
  </head>
  <body>
    <h1>List of recorded requests</h1>
    <table>
      <tr><th>Id</th><th>URI</th><th>Method</th><th>Local Addr</th><th>Remote Addr</th></tr>
<% List<RequestInfo> list = App.getInstance().getRequestRegistry().getRequestList();
   for (RequestInfo ri : list) { %>
      <tr><td><a href="details.jsp?id=<%= ri.getId() %>"><%= ri.getId() %></a></td><td><%= ri.getRequestUri() %><td><%= ri.getMethod() %></td>
          <td><%= ri.getLocalAddr() + ":" + ri.getLocalPort() %></td><td><%= ri.getRemoteAddr() + ":" + ri.getRemotePort() %></td></tr>
<% } %>
    </table>
  </body>
</html>

package ru.semiot.platform.apigateway.utils;

import java.net.URI;

import javax.servlet.ServletRequest;

public class URIUtils {

  private static final String COLON = ":";
  private static final String PROTOCOL_SEPARATOR = "://";
  private static final int HTTP_PORT = 80;
  private static final int HTTPS_PORT = 443;

  public static final String extractRootURL(URI uri) {
    final StringBuilder builder = new StringBuilder(uri.getScheme())
        .append(PROTOCOL_SEPARATOR).append(uri.getHost());

    if (uri.getPort() != HTTP_PORT && uri.getPort() != HTTPS_PORT && uri.getPort() != -1) {
      builder.append(COLON).append(uri.getPort());
    }
    return builder.toString();
  }

  public static final String extractHostName(URI uri) {
    final StringBuilder builder = new StringBuilder(uri.getHost());

    if (uri.getPort() != HTTP_PORT && uri.getPort() != HTTPS_PORT && uri.getPort() != -1) {
      builder.append(COLON).append(uri.getPort());
    }
    return builder.toString();
  }

  public static final String rewriteURI(ServletRequest request, String newPath) {
    final StringBuilder builder = new StringBuilder(request.getScheme())
        .append(PROTOCOL_SEPARATOR)
        .append(request.getServerName());

    if (request.getServerPort() != HTTP_PORT && request.getServerPort() != HTTPS_PORT) {
      builder.append(COLON).append(request.getServerPort());
    }

    builder.append(request.getServletContext().getContextPath()).append(newPath);

    return builder.toString();
  }

}

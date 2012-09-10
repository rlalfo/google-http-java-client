/*
 * Copyright (c) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.client.http;

import com.google.api.client.util.ObjectParser;
import com.google.api.client.util.StringUtils;
import com.google.common.base.Preconditions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTTP request.
 *
 * <p>
 * Implementation is not thread-safe.
 * </p>
 *
 * @since 1.0
 * @author Yaniv Inbar
 */
public final class HttpRequest {

  /**
   * Current version of the Google API Client Library for Java.
   *
   * @since 1.8
   */
  public static final String VERSION = "1.12.0-beta-SNAPSHOT";

  /**
   * User agent suffix for all requests.
   *
   * <p>
   * Includes a {@code "(gzip)"} suffix in case the server -- as Google's servers may do -- checks
   * the {@code User-Agent} header to try to detect if the client accepts gzip-encoded responses.
   * </p>
   *
   * @since 1.4
   */
  public static final String USER_AGENT_SUFFIX = "Google-HTTP-Java-Client/" + VERSION + " (gzip)";

  /**
   * HTTP request execute interceptor to intercept the start of {@link #execute()} (before executing
   * the HTTP request) or {@code null} for none.
   */
  private HttpExecuteInterceptor interceptor;

  /** HTTP request headers. */
  private HttpHeaders headers = new HttpHeaders();

  /**
   * HTTP response headers.
   *
   * <p>
   * For example, this can be used if you want to use a subclass of {@link HttpHeaders} called
   * MyHeaders to process the response:
   * </p>
   *
   * <pre>
  static String executeAndGetValueOfSomeCustomHeader(HttpRequest request) {
    MyHeaders responseHeaders = new MyHeaders();
    request.responseHeaders = responseHeaders;
    HttpResponse response = request.execute();
    return responseHeaders.someCustomHeader;
  }
   * </pre>
   */
  private HttpHeaders responseHeaders = new HttpHeaders();

  /**
   * Set the number of retries that will be allowed to execute as the result of an
   * {@link HttpUnsuccessfulResponseHandler} before being terminated or {@code 0} to not retry
   * requests. The default value is {@code 10}.
   */
  private int numRetries = 10;

  /**
   * Determines the limit to the content size that will be logged during {@link #execute()}.
   *
   * <p>
   * Content will only be logged if {@link #isLoggingEnabled} is {@code true}.
   * </p>
   *
   * <p>
   * If the content size is greater than this limit then it will not be logged.
   * </p>
   *
   * <p>
   * Can be set to {@code 0} to disable content logging. This is useful for example if content has
   * sensitive data such as authentication information.
   * </p>
   *
   * <p>
   * Defaults to 16KB.
   * </p>
   */
  private int contentLoggingLimit = 0x4000;

  /** Determines whether logging should be enabled for this request. Defaults to {@code true}. */
  private boolean loggingEnabled = true;

  /** Determines whether logging in form of curl commands should be enabled for this request. */
  private boolean curlLoggingEnabled = true;

  /** HTTP request content or {@code null} for none. */
  private HttpContent content;

  /** HTTP transport. */
  private final HttpTransport transport;

  /**
   * HTTP request method or {@code null} for none or if request method is not one of the values in
   * {@link HttpMethod}.
   */
  @Deprecated
  private HttpMethod method;

  /** HTTP request method or {@code null} for none. */
  private String requestMethod;

  /** HTTP request URL. */
  private GenericUrl url;

  /** Timeout in milliseconds to establish a connection or {@code 0} for an infinite timeout. */
  private int connectTimeout = 20 * 1000;

  /**
   * Timeout in milliseconds to read data from an established connection or {@code 0} for an
   * infinite timeout.
   */
  private int readTimeout = 20 * 1000;

  /** HTTP unsuccessful (non-2XX) response handler or {@code null} for none. */
  private HttpUnsuccessfulResponseHandler unsuccessfulResponseHandler;

  /** Map from normalized content type to HTTP parser. */
  @Deprecated
  private final Map<String, HttpParser> contentTypeToParserMap = new HashMap<String, HttpParser>();

  /** Parser used to parse responses. */
  private ObjectParser objectParser;

  /** Whether to enable gzip compression of HTTP content ({@code false} by default). */
  private boolean enableGZipContent;

  /** The {@link BackOffPolicy} to use between retry attempts or {@code null} for none. */
  private BackOffPolicy backOffPolicy;

  /** Whether to automatically follow redirects ({@code true} by default). */
  private boolean followRedirects = true;

  /**
   * Whether to throw an exception at the end of {@link #execute()} on an HTTP error code (non-2XX)
   * after all retries and response handlers have been exhausted ({@code true} by default).
   */
  private boolean throwExceptionOnExecuteError = true;

  /**
   * Whether to retry the request if an {@link IOException} is encountered in
   * {@link LowLevelHttpRequest#execute()}.
   */
  private boolean retryOnExecuteIOException = false;

  /**
   * Whether to not add the suffix {@link #USER_AGENT_SUFFIX} to the User-Agent header
   * ({@code false} by default).
   */
  private boolean suppressUserAgentSuffix;

  /**
   * @param transport HTTP transport
   * @param requestMethod HTTP request method or {@code null} for none
   */
  HttpRequest(HttpTransport transport, String requestMethod) {
    this.transport = transport;
    setRequestMethod(requestMethod);
  }

  /**
   * Returns the HTTP transport.
   *
   * @since 1.5
   */
  public HttpTransport getTransport() {
    return transport;
  }

  /**
   * Returns the HTTP request method or {@code null} for none or if request method is not one of the
   * values in {@link HttpMethod}.
   *
   * @since 1.5
   * @deprecated (scheduled to be removed in 1.13) Use {@link #getRequestMethod()} instead
   */
  @Deprecated
  public HttpMethod getMethod() {
    return method;
  }

  /**
   * Sets the HTTP request method.
   *
   * @since 1.5
   * @deprecated (scheduled to be removed in 1.13) Use {@link #setRequestMethod} instead
   */
  @Deprecated
  public HttpRequest setMethod(HttpMethod method) {
    return setRequestMethod(method.toString());
  }

  /**
   * Returns the HTTP request method or {@code null} for none.
   *
   * @since 1.12
   */
  public String getRequestMethod() {
    return requestMethod;
  }

  /**
   * Sets the HTTP request method or {@code null} for none.
   *
   * @since 1.12
   */
  @SuppressWarnings("deprecation")
  public HttpRequest setRequestMethod(String requestMethod) {
    Preconditions.checkArgument(requestMethod == null || HttpMediaType.matchesToken(requestMethod));
    this.requestMethod = requestMethod;
    method = null;
    if (HttpMethods.DELETE.equals(requestMethod)) {
      method = HttpMethod.DELETE;
    }
    if (HttpMethods.GET.equals(requestMethod)) {
      method = HttpMethod.GET;
    }
    if (HttpMethods.HEAD.equals(requestMethod)) {
      method = HttpMethod.HEAD;
    }
    if ("PATCH".equals(requestMethod)) {
      method = HttpMethod.PATCH;
    }
    if (HttpMethods.POST.equals(requestMethod)) {
      method = HttpMethod.POST;
    }
    if (HttpMethods.PUT.equals(requestMethod)) {
      method = HttpMethod.PUT;
    }
    return this;
  }

  /**
   * Returns the HTTP request URL.
   *
   * @since 1.5
   */
  public GenericUrl getUrl() {
    return url;
  }

  /**
   * Sets the HTTP request URL.
   *
   * @since 1.5
   */
  public HttpRequest setUrl(GenericUrl url) {
    this.url = Preconditions.checkNotNull(url);
    return this;
  }

  /**
   * Returns the HTTP request content or {@code null} for none.
   *
   * @since 1.5
   */
  public HttpContent getContent() {
    return content;
  }

  /**
   * Sets the HTTP request content or {@code null} for none.
   *
   * @since 1.5
   */
  public HttpRequest setContent(HttpContent content) {
    this.content = content;
    return this;
  }

  /**
   * Returns whether to enable gzip compression of HTTP content.
   *
   * @since 1.5
   */
  public boolean getEnableGZipContent() {
    return enableGZipContent;
  }

  /**
   * Returns whether to enable gzip compression of HTTP content.
   *
   * <p>
   * By default it is {@code false}.
   * </p>
   *
   * <p>
   * To avoid the overhead of GZip compression for small content, one may want to set this to
   * {@code true} only for {@link HttpContent#getLength()} above a certain limit. For example:
   * </p>
   *
   * <pre>
  public static class MyInterceptor implements HttpExecuteInterceptor {
    public void intercept(HttpRequest request) throws IOException {
      if (request.getContent() != null && request.getContent().getLength() >= 256) {
        request.setEnableGZipContent(true);
      }
    }
  }
   * </pre>
   *
   * @since 1.5
   */
  public HttpRequest setEnableGZipContent(boolean enableGZipContent) {
    this.enableGZipContent = enableGZipContent;
    return this;
  }

  /**
   * Returns the {@link BackOffPolicy} to use between retry attempts or {@code null} for none.
   *
   * @since 1.7
   */
  public BackOffPolicy getBackOffPolicy() {
    return backOffPolicy;
  }

  /**
   * Sets the {@link BackOffPolicy} to use between retry attempts or {@code null} for none.
   *
   * @since 1.7
   */
  public HttpRequest setBackOffPolicy(BackOffPolicy backOffPolicy) {
    this.backOffPolicy = backOffPolicy;
    return this;
  }

  /**
   * Returns the limit to the content size that will be logged during {@link #execute()}.
   *
   * <p>
   * If the content size is greater than this limit then it will not be logged.
   * </p>
   *
   * <p>
   * Content will only be logged if {@link #isLoggingEnabled} is {@code true}.
   * </p>
   *
   * <p>
   * Can be set to {@code 0} to disable content logging. This is useful for example if content has
   * sensitive data such as authentication information.
   * </p>
   *
   * <p>
   * Defaults to 16KB.
   * </p>
   *
   * @since 1.7
   */
  public int getContentLoggingLimit() {
    return contentLoggingLimit;
  }

  /**
   * Set the limit to the content size that will be logged during {@link #execute()}.
   *
   * <p>
   * If the content size is greater than this limit then it will not be logged.
   * </p>
   *
   * <p>
   * Content will only be logged if {@link #isLoggingEnabled} is {@code true}.
   * </p>
   *
   * <p>
   * Can be set to {@code 0} to disable content logging. This is useful for example if content has
   * sensitive data such as authentication information.
   * </p>
   *
   * <p>
   * Defaults to 16KB.
   * </p>
   *
   * @since 1.7
   */
  public HttpRequest setContentLoggingLimit(int contentLoggingLimit) {
    Preconditions.checkArgument(
        contentLoggingLimit >= 0, "The content logging limit must be non-negative.");
    this.contentLoggingLimit = contentLoggingLimit;
    return this;
  }

  /**
   * Returns whether logging should be enabled for this request.
   *
   * <p>
   * Defaults to {@code true}.
   * </p>
   *
   * @since 1.9
   */
  public boolean isLoggingEnabled() {
    return loggingEnabled;
  }

  /**
   * Sets whether logging should be enabled for this request.
   *
   * <p>
   * Defaults to {@code true}.
   * </p>
   *
   * @since 1.9
   */
  public HttpRequest setLoggingEnabled(boolean loggingEnabled) {
    this.loggingEnabled = loggingEnabled;
    return this;
  }

  /**
   * Returns whether logging in form of curl commands is enabled for this request.
   *
   * @since 1.11
   */
  public boolean isCurlLoggingEnabled() {
    return curlLoggingEnabled;
  }

  /**
   * Sets whether logging in form of curl commands should be enabled for this request.
   *
   * <p>
   * Defaults to {@code true}.
   * </p>
   *
   * @since 1.11
   */
  public HttpRequest setCurlLoggingEnabled(boolean curlLoggingEnabled) {
    this.curlLoggingEnabled = curlLoggingEnabled;
    return this;
  }

  /**
   * Returns the timeout in milliseconds to establish a connection or {@code 0} for an infinite
   * timeout.
   *
   * @since 1.5
   */
  public int getConnectTimeout() {
    return connectTimeout;
  }

  /**
   * Sets the timeout in milliseconds to establish a connection or {@code 0} for an infinite
   * timeout.
   *
   * <p>
   * By default it is 20000 (20 seconds).
   * </p>
   *
   * @since 1.5
   */
  public HttpRequest setConnectTimeout(int connectTimeout) {
    Preconditions.checkArgument(connectTimeout >= 0);
    this.connectTimeout = connectTimeout;
    return this;
  }

  /**
   * Returns the timeout in milliseconds to read data from an established connection or {@code 0}
   * for an infinite timeout.
   *
   * <p>
   * By default it is 20000 (20 seconds).
   * </p>
   *
   * @since 1.5
   */
  public int getReadTimeout() {
    return readTimeout;
  }

  /**
   * Sets the timeout in milliseconds to read data from an established connection or {@code 0} for
   * an infinite timeout.
   *
   * @since 1.5
   */
  public HttpRequest setReadTimeout(int readTimeout) {
    Preconditions.checkArgument(readTimeout >= 0);
    this.readTimeout = readTimeout;
    return this;
  }

  /**
   * Returns the HTTP request headers.
   *
   * @since 1.5
   */
  public HttpHeaders getHeaders() {
    return headers;
  }

  /**
   * Sets the HTTP request headers.
   *
   * <p>
   * By default, this is a new unmodified instance of {@link HttpHeaders}.
   * </p>
   *
   * @since 1.5
   */
  public HttpRequest setHeaders(HttpHeaders headers) {
    this.headers = Preconditions.checkNotNull(headers);
    return this;
  }

  /**
   * Returns the HTTP response headers.
   *
   * @since 1.5
   */
  public HttpHeaders getResponseHeaders() {
    return responseHeaders;
  }

  /**
   * Sets the HTTP response headers.
   *
   * <p>
   * By default, this is a new unmodified instance of {@link HttpHeaders}.
   * </p>
   *
   * <p>
   * For example, this can be used if you want to use a subclass of {@link HttpHeaders} called
   * MyHeaders to process the response:
   * </p>
   *
   * <pre>
  static String executeAndGetValueOfSomeCustomHeader(HttpRequest request) {
    MyHeaders responseHeaders = new MyHeaders();
    request.responseHeaders = responseHeaders;
    HttpResponse response = request.execute();
    return responseHeaders.someCustomHeader;
  }
   * </pre>
   *
   * @since 1.5
   */
  public HttpRequest setResponseHeaders(HttpHeaders responseHeaders) {
    this.responseHeaders = Preconditions.checkNotNull(responseHeaders);
    return this;
  }

  /**
   * Returns the HTTP request execute interceptor to intercept the start of {@link #execute()}
   * (before executing the HTTP request) or {@code null} for none.
   *
   * @since 1.5
   */
  public HttpExecuteInterceptor getInterceptor() {
    return interceptor;
  }

  /**
   * Sets the HTTP request execute interceptor to intercept the start of {@link #execute()} (before
   * executing the HTTP request) or {@code null} for none.
   *
   * @since 1.5
   */
  public HttpRequest setInterceptor(HttpExecuteInterceptor interceptor) {
    this.interceptor = interceptor;
    return this;
  }

  /**
   * Returns the HTTP unsuccessful (non-2XX) response handler or {@code null} for none.
   *
   * @since 1.5
   */
  public HttpUnsuccessfulResponseHandler getUnsuccessfulResponseHandler() {
    return unsuccessfulResponseHandler;
  }

  /**
   * Returns the HTTP unsuccessful (non-2XX) response handler or {@code null} for none.
   *
   * @since 1.5
   */
  public HttpRequest setUnsuccessfulResponseHandler(
      HttpUnsuccessfulResponseHandler unsuccessfulResponseHandler) {
    this.unsuccessfulResponseHandler = unsuccessfulResponseHandler;
    return this;
  }

  /**
   * Returns the number of retries that will be allowed to execute as the result of an
   * {@link HttpUnsuccessfulResponseHandler} before being terminated or {@code 0} to not retry
   * requests.
   *
   * @since 1.5
   */
  public int getNumberOfRetries() {
    return numRetries;
  }

  /**
   * Returns the number of retries that will be allowed to execute as the result of an
   * {@link HttpUnsuccessfulResponseHandler} before being terminated or {@code 0} to not retry
   * requests.
   *
   * <p>
   * The default value is {@code 10}.
   * </p>
   *
   * @since 1.5
   */
  public HttpRequest setNumberOfRetries(int numRetries) {
    Preconditions.checkArgument(numRetries >= 0);
    this.numRetries = numRetries;
    return this;
  }

  /**
   * Adds an HTTP response content parser.
   * <p>
   * If there is already a previous parser defined for this new parser (as defined by
   * {@link #getParser(String)} then the previous parser will be removed.
   * </p>
   *
   * <p>
   * Any parser set by calling {@link #setParser(ObjectParser)} will be preferred over this parser.
   * </p>
   *
   * @since 1.4
   * @deprecated (scheduled to be removed in 1.12) Use {@link #setParser(ObjectParser)} instead.
   */
  @Deprecated
  public void addParser(HttpParser parser) {
    String contentType = normalizeMediaType(parser.getContentType());
    contentTypeToParserMap.put(contentType, parser);
  }

  /**
   * Sets the {@link ObjectParser} used to parse the response to this request or {@code null} for
   * none.
   *
   * <p>
   * This parser will be preferred over any registered HttpParser.
   * </p>
   *
   * @since 1.10
   */
  public HttpRequest setParser(ObjectParser parser) {
    this.objectParser = parser;
    return this;
  }

  /**
   * Returns the HTTP response content parser to use for the given content type or {@code null} if
   * none is defined.
   *
   * @param contentType content type or {@code null} for {@code null} result
   * @return HTTP response content parser or {@code null} for {@code null} input
   * @since 1.4
   * @deprecated (scheduled to be removed in 1.12) Use {@link #getParser()} instead.
   */
  @Deprecated
  public final HttpParser getParser(String contentType) {
    contentType = normalizeMediaType(contentType);
    return contentTypeToParserMap.get(contentType);
  }

  /**
   * Returns the {@link ObjectParser} used to parse the response or {@code null} for none.
   *
   * @since 1.10
   */
  public final ObjectParser getParser() {
    return objectParser;
  }

  /**
   * Returns whether to follow redirects automatically.
   *
   * @since 1.6
   */
  public boolean getFollowRedirects() {
    return followRedirects;
  }

  /**
   * Sets whether to follow redirects automatically.
   *
   * <p>
   * The default value is {@code true}.
   * </p>
   *
   * @since 1.6
   */
  public HttpRequest setFollowRedirects(boolean followRedirects) {
    this.followRedirects = followRedirects;
    return this;
  }

  /**
   * Returns whether to throw an exception at the end of {@link #execute()} on an HTTP error code
   * (non-2XX) after all retries and response handlers have been exhausted.
   *
   * @since 1.7
   */
  public boolean getThrowExceptionOnExecuteError() {
    return throwExceptionOnExecuteError;
  }

  /**
   * Sets whether to throw an exception at the end of {@link #execute()} on a HTTP error code
   * (non-2XX) after all retries and response handlers have been exhausted.
   *
   * <p>
   * The default value is {@code true}.
   * </p>
   *
   * @since 1.7
   */
  public HttpRequest setThrowExceptionOnExecuteError(boolean throwExceptionOnExecuteError) {
    this.throwExceptionOnExecuteError = throwExceptionOnExecuteError;
    return this;
  }

  /**
   * Returns whether to retry the request if an {@link IOException} is encountered in
   * {@link LowLevelHttpRequest#execute()}.
   *
   * @since 1.9
   */
  public boolean getRetryOnExecuteIOException() {
    return retryOnExecuteIOException;
  }

  /**
   * Sets whether to retry the request if an {@link IOException} is encountered in
   * {@link LowLevelHttpRequest#execute()}.
   *
   * <p>
   * The default value is {@code false}.
   * </p>
   *
   * @since 1.9
   */
  public HttpRequest setRetryOnExecuteIOException(boolean retryOnExecuteIOException) {
    this.retryOnExecuteIOException = retryOnExecuteIOException;
    return this;
  }

  /**
   * Returns whether to not add the suffix {@link #USER_AGENT_SUFFIX} to the User-Agent header.
   *
   * @since 1.11
   */
  public boolean getSuppressUserAgentSuffix() {
    return suppressUserAgentSuffix;
  }

  /**
   * Sets whether to not add the suffix {@link #USER_AGENT_SUFFIX} to the User-Agent header.
   *
   * <p>
   * The default value is {@code false}.
   * </p>
   *
   * @since 1.11
   */
  public HttpRequest setSuppressUserAgentSuffix(boolean suppressUserAgentSuffix) {
    this.suppressUserAgentSuffix = suppressUserAgentSuffix;
    return this;
  }

  /**
   * Execute the HTTP request and returns the HTTP response.
   * <p>
   * Note that regardless of the returned status code, the HTTP response content has not been parsed
   * yet, and must be parsed by the calling code.
   * <p>
   * Almost all details of the request and response are logged if {@link Level#CONFIG} is loggable.
   * The only exception is the value of the {@code Authorization} header which is only logged if
   * {@link Level#ALL} is loggable.
   * <p>
   * Callers should call {@link HttpResponse#disconnect} when the returned HTTP response object is
   * no longer needed. However, {@link HttpResponse#disconnect} does not have to be called if the
   * response stream is properly closed. Example usage:
   * </p>
   *
   * <pre>
     HttpResponse response = request.execute();
     try {
       // process the HTTP response object
     } finally {
       response.disconnect();
     }
   * </pre>
   *
   * @return HTTP response for an HTTP success response (or HTTP error response if
   *         {@link #getThrowExceptionOnExecuteError()} is {@code false})
   * @throws HttpResponseException for an HTTP error response (only if
   *         {@link #getThrowExceptionOnExecuteError()} is {@code true})
   * @see HttpResponse#isSuccessStatusCode()
   */
  @SuppressWarnings("deprecation")
  public HttpResponse execute() throws IOException {
    boolean retrySupported = false;
    Preconditions.checkArgument(numRetries >= 0);
    int retriesRemaining = numRetries;
    if (backOffPolicy != null) {
      // Reset the BackOffPolicy at the start of each execute.
      backOffPolicy.reset();
    }
    HttpResponse response = null;
    IOException executeException;

    Preconditions.checkNotNull(requestMethod);
    Preconditions.checkNotNull(url);

    do {
      // Cleanup any unneeded response from a previous iteration
      if (response != null) {
        response.ignore();
      }

      response = null;
      executeException = null;

      // run the interceptor
      if (interceptor != null) {
        interceptor.intercept(this);
      }
      // build low-level HTTP request
      String urlString = url.build();
      LowLevelHttpRequest lowLevelHttpRequest;
      if (requestMethod.equals(HttpMethods.DELETE)) {
        lowLevelHttpRequest = transport.buildDeleteRequest(urlString);
      } else if (requestMethod.equals(HttpMethods.GET)) {
        lowLevelHttpRequest = transport.buildGetRequest(urlString);
      } else if (requestMethod.equals(HttpMethods.HEAD)) {
        lowLevelHttpRequest = transport.buildHeadRequest(urlString);
      } else if (requestMethod.equals("PATCH")) {
        lowLevelHttpRequest = transport.buildPatchRequest(urlString);
      } else if (requestMethod.equals(HttpMethods.POST)) {
        lowLevelHttpRequest = transport.buildPostRequest(urlString);
      } else if (requestMethod.equals(HttpMethods.PUT)) {
        lowLevelHttpRequest = transport.buildPutRequest(urlString);
      } else {
        lowLevelHttpRequest = transport.buildRequest(requestMethod, urlString);
      }
      Logger logger = HttpTransport.LOGGER;
      boolean loggable = loggingEnabled && logger.isLoggable(Level.CONFIG);
      StringBuilder logbuf = null;
      StringBuilder curlbuf = null;
      // log method and URL
      if (loggable) {
        logbuf = new StringBuilder();
        logbuf.append("-------------- REQUEST  --------------").append(StringUtils.LINE_SEPARATOR);
        logbuf.append(requestMethod)
            .append(' ').append(urlString).append(StringUtils.LINE_SEPARATOR);

        // setup curl logging
        if (curlLoggingEnabled) {
          curlbuf = new StringBuilder("curl -v --compressed");
          if (!requestMethod.equals(HttpMethods.GET)) {
            curlbuf.append(" -X ").append(requestMethod);
          }
        }
      }
      // add to user agent
      String originalUserAgent = headers.getUserAgent();
      if (!suppressUserAgentSuffix) {
        if (originalUserAgent == null) {
          headers.setUserAgent(USER_AGENT_SUFFIX);
        } else {
          headers.setUserAgent(originalUserAgent + " " + USER_AGENT_SUFFIX);
        }
      }
      // headers
      HttpHeaders.serializeHeaders(headers, logbuf, curlbuf, logger, lowLevelHttpRequest);
      if (!suppressUserAgentSuffix) {
        // set the original user agent back so that retries do not keep appending to it
        headers.setUserAgent(originalUserAgent);
      }

      // content
      HttpContent content = this.content;
      if (content != null) {
        String contentEncoding = content.getEncoding();
        long contentLength = content.getLength();
        String contentType = content.getType();
        // log content
        if (loggable) {
          content = new LogContent(
              content, contentType, contentEncoding, contentLength, contentLoggingLimit);
        }
        // gzip
        String underlyingEncoding = content.getEncoding();
        if (enableGZipContent) {
          content = new GZipContent(content, contentType);
          contentEncoding = content.getEncoding();
          contentLength = content.getLength();
        }
        // append content headers to log buffer
        if (loggable) {
          if (contentType != null) {
            String header = "Content-Type: " + contentType;
            logbuf.append(header).append(StringUtils.LINE_SEPARATOR);
            if (curlbuf != null) {
              curlbuf.append(" -H '" + header + "'");
            }
          }
          if (underlyingEncoding != null) {
            // do not use the gzip encoding as the content won't be logged as gzip
            String header = "Content-Encoding: " + underlyingEncoding;
            logbuf.append(header).append(StringUtils.LINE_SEPARATOR);
            if (curlbuf != null) {
              curlbuf.append(" -H '" + header + "'");
            }
          }
          if (contentLength >= 0) {
            String header = "Content-Length: " + contentLength;
            logbuf.append(header).append(StringUtils.LINE_SEPARATOR);
            // do not log @ curl as the user will most likely manipulate the content
          }
        }
        if (curlbuf != null) {
          curlbuf.append(" -d '@-'");
        }
        lowLevelHttpRequest.setContent(content);
      }
      // log from buffer
      if (loggable) {
        logger.config(logbuf.toString());
        if (curlbuf != null) {
          curlbuf.append(" -- ");
          curlbuf.append(urlString);
          if (content != null) {
            curlbuf.append(" << $$$");
          }
          logger.config(curlbuf.toString());
        }
      }

      // We need to make sure our content type can support retry
      // null content is inherently able to be retried
      retrySupported = retriesRemaining > 0 && (content == null || content.retrySupported());

      // execute
      lowLevelHttpRequest.setTimeout(connectTimeout, readTimeout);
      try {
        LowLevelHttpResponse lowLevelHttpResponse = lowLevelHttpRequest.execute();
        // Flag used to indicate if an exception is thrown before the response is constructed.
        boolean responseConstructed = false;
        try {
          response = new HttpResponse(this, lowLevelHttpResponse);
          responseConstructed = true;
        } finally {
          if (!responseConstructed) {
            lowLevelHttpResponse.getContent().close();
          }
        }
      } catch (IOException e) {
        if (!retryOnExecuteIOException) {
          throw e;
        }
        // Save the exception in case the retries do not work and we need to re-throw it later.
        executeException = e;
        logger.log(Level.WARNING, "exception thrown while executing request", e);
      }

      // Flag used to indicate if an exception is thrown before the response has completed
      // processing.
      boolean responseProcessed = false;
      try {
        if (response != null && !response.isSuccessStatusCode()) {
          boolean errorHandled = false;
          boolean redirectRequest = false;
          boolean backOffRetry = false;
          if (unsuccessfulResponseHandler != null) {
            // Even if we don't have the potential to retry, we might want to run the
            // handler to fix conditions (like expired tokens) that might cause us
            // trouble on our next request
            errorHandled =
                unsuccessfulResponseHandler.handleResponse(this, response, retrySupported);
          }
          if (!errorHandled) {
            if (handleRedirect(response.getStatusCode(), response.getHeaders())) {
              // The unsuccessful request's error could not be handled and it is a redirect request.
              redirectRequest = true;
            } else if (retrySupported && backOffPolicy != null
                && backOffPolicy.isBackOffRequired(response.getStatusCode())) {
              // The unsuccessful request's error could not be handled and should be backed off
              // before
              // retrying.
              long backOffTime = backOffPolicy.getNextBackOffMillis();
              if (backOffTime != BackOffPolicy.STOP) {
                sleep(backOffTime);
                backOffRetry = true;
              }
            }
          }
          // A retry is required if the error was successfully handled or if it is a redirect
          // request
          // or if the back off policy determined a retry is necessary.
          retrySupported &= (errorHandled || redirectRequest || backOffRetry);
          // need to close the response stream before retrying a request
          if (retrySupported) {
            response.ignore();
          }
        } else {
          // Retry is not required for a successful status code unless the response is null.
          retrySupported &= (response == null);
        }
        // Once there are no more retries remaining, this will be -1
        // Count redirects as retries, we want a finite limit of redirects.
        retriesRemaining--;

        responseProcessed = true;
      } finally {
        if (response != null && !responseProcessed) {
          response.disconnect();
        }
      }
    } while (retrySupported);

    if (response == null) {
      // Retries did not help resolve the execute exception, re-throw it.
      throw executeException;
    }

    if (throwExceptionOnExecuteError && !response.isSuccessStatusCode()) {
      try {
        throw new HttpResponseException(response);
      } finally {
        response.disconnect();
      }
    }
    return response;
  }

  /**
   * Sets up this request object to handle the necessary redirect if redirects are turned on, it is
   * a redirect status code and the header has a location.
   *
   * <p>
   * When the status code is {@code 303} the method on the request is changed to a GET as per the
   * RFC2616 specification.
   * </p>
   *
   * @since 1.11
   */
  public boolean handleRedirect(int statusCode, HttpHeaders headers) {
    String redirectLocation = headers.getLocation();
    if (getFollowRedirects() && HttpStatusCodes.isRedirect(statusCode)
        && redirectLocation != null) {
      setUrl(new GenericUrl(redirectLocation));

      if (statusCode == HttpStatusCodes.STATUS_CODE_SEE_OTHER) {
        setRequestMethod(HttpMethods.GET);
      }
      return true;
    }
    return false;
  }

  /**
   * An exception safe sleep where if the sleeping is interrupted the exception is ignored.
   *
   * @param millis to sleep
   */
  private void sleep(long millis) {
    try {
      // TODO(rmistry): Provide a way to mock out Thread.sleep to check that sleep gets called with
      // expected values.
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      // Ignore.
    }
  }

  /**
   * Returns the normalized media type without parameters of the form {@code type "/" subtype"} as
   * specified in <a href="http://tools.ietf.org/html/rfc2616#section-3.7">Media Types</a>.
   *
   * @param mediaType unnormalized media type with possible parameters or {@code null} for
   *        {@code null} result
   * @return normalized media type without parameters or {@code null} for {@code null} input
   * @since 1.4
   * @deprecated (scheduled to be removed in 1.12) Use
   *             {@link HttpMediaType#equalsIgnoreParameters(HttpMediaType)} instead
   */
  @Deprecated
  public static String normalizeMediaType(String mediaType) {
    if (mediaType == null) {
      return null;
    }
    int semicolon = mediaType.indexOf(';');
    return semicolon == -1 ? mediaType : mediaType.substring(0, semicolon);
  }
}

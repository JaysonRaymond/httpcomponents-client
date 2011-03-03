/*
 * ====================================================================
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.client.protocol;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.CookieStore;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.HttpRoutedConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecRegistry;
import org.apache.http.cookie.SM;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.cookie.BestMatchSpecFactory;
import org.apache.http.impl.cookie.BrowserCompatSpecFactory;
import org.apache.http.impl.cookie.IgnoreSpecFactory;
import org.apache.http.impl.cookie.NetscapeDraftSpecFactory;
import org.apache.http.impl.cookie.RFC2109Spec;
import org.apache.http.impl.cookie.RFC2109SpecFactory;
import org.apache.http.impl.cookie.RFC2965SpecFactory;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestRequestAddCookies {

    private HttpHost target;
    private CookieStore cookieStore;
    private CookieSpecRegistry cookieSpecRegistry;

    @Before
    public void setUp() {
        this.target = new HttpHost("localhost", 80);
        this.cookieStore = new BasicCookieStore();
        BasicClientCookie cookie1 = new BasicClientCookie("name1", "value1");
        cookie1.setDomain("localhost");
        cookie1.setPath("/");
        this.cookieStore.addCookie(cookie1);
        BasicClientCookie cookie2 = new BasicClientCookie("name2", "value2");
        cookie2.setDomain("localhost");
        cookie2.setPath("/");
        this.cookieStore.addCookie(cookie2);

        this.cookieSpecRegistry = new CookieSpecRegistry();
        this.cookieSpecRegistry.register(
                CookiePolicy.BEST_MATCH,
                new BestMatchSpecFactory());
        this.cookieSpecRegistry.register(
                CookiePolicy.BROWSER_COMPATIBILITY,
                new BrowserCompatSpecFactory());
        this.cookieSpecRegistry.register(
                CookiePolicy.NETSCAPE,
                new NetscapeDraftSpecFactory());
        this.cookieSpecRegistry.register(
                CookiePolicy.RFC_2109,
                new RFC2109SpecFactory());
        this.cookieSpecRegistry.register(
                CookiePolicy.RFC_2965,
                new RFC2965SpecFactory());
        this.cookieSpecRegistry.register(
                CookiePolicy.IGNORE_COOKIES,
                new IgnoreSpecFactory());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRequestParameterCheck() throws Exception {
        HttpContext context = new BasicHttpContext();
        HttpRequestInterceptor interceptor = new RequestAddCookies();
        interceptor.process(null, context);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testContextParameterCheck() throws Exception {
        HttpRequest request = new BasicHttpRequest("GET", "/");
        HttpRequestInterceptor interceptor = new RequestAddCookies();
        interceptor.process(request, null);
    }

    @Test
    public void testAddCookies() throws Exception {
        HttpRequest request = new BasicHttpRequest("GET", "/");

        HttpRoute route = new HttpRoute(this.target, null, false);

        HttpRoutedConnection conn = Mockito.mock(HttpRoutedConnection.class);
        Mockito.when(conn.getRoute()).thenReturn(route);
        Mockito.when(conn.isSecure()).thenReturn(Boolean.FALSE);

        HttpContext context = new BasicHttpContext();
        context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, this.target);
        context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
        context.setAttribute(ClientContext.COOKIE_STORE, this.cookieStore);
        context.setAttribute(ClientContext.COOKIESPEC_REGISTRY, this.cookieSpecRegistry);

        HttpRequestInterceptor interceptor = new RequestAddCookies();
        interceptor.process(request, context);

        Header[] headers1 = request.getHeaders(SM.COOKIE);
        Assert.assertNotNull(headers1);
        Assert.assertEquals(1, headers1.length);
        Assert.assertEquals("name1=value1; name2=value2", headers1[0].getValue());
        Header[] headers2 = request.getHeaders(SM.COOKIE2);
        Assert.assertNotNull(headers2);
        Assert.assertEquals(1, headers2.length);
        Assert.assertEquals("$Version=1", headers2[0].getValue());

        CookieOrigin cookieOrigin = (CookieOrigin) context.getAttribute(
                ClientContext.COOKIE_ORIGIN);
        Assert.assertNotNull(cookieOrigin);
        Assert.assertEquals("localhost", cookieOrigin.getHost());
        Assert.assertEquals(80, cookieOrigin.getPort());
        Assert.assertEquals("/", cookieOrigin.getPath());
        Assert.assertFalse(cookieOrigin.isSecure());
    }

    @Test
    public void testCookiesForConnectRequest() throws Exception {
        HttpRequest request = new BasicHttpRequest("CONNECT", "www.somedomain.com");

        HttpRoute route = new HttpRoute(this.target, null, false);

        HttpRoutedConnection conn = Mockito.mock(HttpRoutedConnection.class);
        Mockito.when(conn.getRoute()).thenReturn(route);
        Mockito.when(conn.isSecure()).thenReturn(Boolean.FALSE);

        HttpContext context = new BasicHttpContext();
        context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, this.target);
        context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
        context.setAttribute(ClientContext.COOKIE_STORE, this.cookieStore);
        context.setAttribute(ClientContext.COOKIESPEC_REGISTRY, this.cookieSpecRegistry);

        HttpRequestInterceptor interceptor = new RequestAddCookies();
        interceptor.process(request, context);

        Header[] headers1 = request.getHeaders(SM.COOKIE);
        Assert.assertNotNull(headers1);
        Assert.assertEquals(0, headers1.length);
        Header[] headers2 = request.getHeaders(SM.COOKIE2);
        Assert.assertNotNull(headers2);
        Assert.assertEquals(0, headers2.length);
    }

    @Test
    public void testNoCookieStore() throws Exception {
        HttpRequest request = new BasicHttpRequest("GET", "/");

        HttpRoute route = new HttpRoute(this.target, null, false);

        HttpRoutedConnection conn = Mockito.mock(HttpRoutedConnection.class);
        Mockito.when(conn.getRoute()).thenReturn(route);
        Mockito.when(conn.isSecure()).thenReturn(Boolean.FALSE);

        HttpContext context = new BasicHttpContext();
        context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, this.target);
        context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
        context.setAttribute(ClientContext.COOKIE_STORE, null);
        context.setAttribute(ClientContext.COOKIESPEC_REGISTRY, this.cookieSpecRegistry);

        HttpRequestInterceptor interceptor = new RequestAddCookies();
        interceptor.process(request, context);

        Header[] headers1 = request.getHeaders(SM.COOKIE);
        Assert.assertNotNull(headers1);
        Assert.assertEquals(0, headers1.length);
        Header[] headers2 = request.getHeaders(SM.COOKIE2);
        Assert.assertNotNull(headers2);
        Assert.assertEquals(0, headers2.length);
    }

    @Test
    public void testNoCookieSpecRegistry() throws Exception {
        HttpRequest request = new BasicHttpRequest("GET", "/");

        HttpRoute route = new HttpRoute(this.target, null, false);

        HttpRoutedConnection conn = Mockito.mock(HttpRoutedConnection.class);
        Mockito.when(conn.getRoute()).thenReturn(route);
        Mockito.when(conn.isSecure()).thenReturn(Boolean.FALSE);

        HttpContext context = new BasicHttpContext();
        context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, this.target);
        context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
        context.setAttribute(ClientContext.COOKIE_STORE, this.cookieStore);
        context.setAttribute(ClientContext.COOKIESPEC_REGISTRY, null);

        HttpRequestInterceptor interceptor = new RequestAddCookies();
        interceptor.process(request, context);

        Header[] headers1 = request.getHeaders(SM.COOKIE);
        Assert.assertNotNull(headers1);
        Assert.assertEquals(0, headers1.length);
        Header[] headers2 = request.getHeaders(SM.COOKIE2);
        Assert.assertNotNull(headers2);
        Assert.assertEquals(0, headers2.length);
    }

    @Test
    public void testNoTargetHost() throws Exception {
        HttpRequest request = new BasicHttpRequest("GET", "/");

        HttpRoute route = new HttpRoute(this.target, null, false);

        HttpRoutedConnection conn = Mockito.mock(HttpRoutedConnection.class);
        Mockito.when(conn.getRoute()).thenReturn(route);
        Mockito.when(conn.isSecure()).thenReturn(Boolean.FALSE);

        HttpContext context = new BasicHttpContext();
        context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, null);
        context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
        context.setAttribute(ClientContext.COOKIE_STORE, this.cookieStore);
        context.setAttribute(ClientContext.COOKIESPEC_REGISTRY, this.cookieSpecRegistry);

        HttpRequestInterceptor interceptor = new RequestAddCookies();
        interceptor.process(request, context);

        Header[] headers1 = request.getHeaders(SM.COOKIE);
        Assert.assertNotNull(headers1);
        Assert.assertEquals(0, headers1.length);
        Header[] headers2 = request.getHeaders(SM.COOKIE2);
        Assert.assertNotNull(headers2);
        Assert.assertEquals(0, headers2.length);
    }

    @Test
    public void testNoHttpConnection() throws Exception {
        HttpRequest request = new BasicHttpRequest("GET", "/");

        HttpContext context = new BasicHttpContext();
        context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, this.target);
        context.setAttribute(ExecutionContext.HTTP_CONNECTION, null);
        context.setAttribute(ClientContext.COOKIE_STORE, this.cookieStore);
        context.setAttribute(ClientContext.COOKIESPEC_REGISTRY, this.cookieSpecRegistry);

        HttpRequestInterceptor interceptor = new RequestAddCookies();
        interceptor.process(request, context);

        Header[] headers1 = request.getHeaders(SM.COOKIE);
        Assert.assertNotNull(headers1);
        Assert.assertEquals(0, headers1.length);
        Header[] headers2 = request.getHeaders(SM.COOKIE2);
        Assert.assertNotNull(headers2);
        Assert.assertEquals(0, headers2.length);
    }

    @Test
    public void testAddCookiesUsingExplicitCookieSpec() throws Exception {
        HttpRequest request = new BasicHttpRequest("GET", "/");
        request.getParams().setParameter(AllClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2109);

        HttpRoute route = new HttpRoute(this.target, null, false);

        HttpRoutedConnection conn = Mockito.mock(HttpRoutedConnection.class);
        Mockito.when(conn.getRoute()).thenReturn(route);
        Mockito.when(conn.isSecure()).thenReturn(Boolean.FALSE);

        HttpContext context = new BasicHttpContext();
        context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, this.target);
        context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
        context.setAttribute(ClientContext.COOKIE_STORE, this.cookieStore);
        context.setAttribute(ClientContext.COOKIESPEC_REGISTRY, this.cookieSpecRegistry);

        HttpRequestInterceptor interceptor = new RequestAddCookies();
        interceptor.process(request, context);

        CookieSpec cookieSpec = (CookieSpec) context.getAttribute(
                ClientContext.COOKIE_SPEC);
        Assert.assertTrue(cookieSpec instanceof RFC2109Spec);

        Header[] headers1 = request.getHeaders(SM.COOKIE);
        Assert.assertNotNull(headers1);
        Assert.assertEquals(2, headers1.length);
        Assert.assertEquals("$Version=0; name1=value1", headers1[0].getValue());
        Assert.assertEquals("$Version=0; name2=value2", headers1[1].getValue());

        CookieOrigin cookieOrigin = (CookieOrigin) context.getAttribute(
                ClientContext.COOKIE_ORIGIN);
        Assert.assertNotNull(cookieOrigin);
        Assert.assertEquals("localhost", cookieOrigin.getHost());
        Assert.assertEquals(80, cookieOrigin.getPort());
        Assert.assertEquals("/", cookieOrigin.getPath());
        Assert.assertFalse(cookieOrigin.isSecure());
    }

}

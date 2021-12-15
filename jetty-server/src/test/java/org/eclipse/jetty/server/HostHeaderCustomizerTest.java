//
//  ========================================================================
//  Copyright (c) 1995-2021 Mort Bay Consulting Pty Ltd and others.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpTester;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HostHeaderCustomizerTest
{
    @Test
    public void testHostHeaderCustomizerSecondNoHostAuthorityInUriLine() throws Exception
    {
        Server server = new Server();
        HttpConfiguration httpConfig = new HttpConfiguration();
        final String redirectPath = "/redirect";
        httpConfig.addCustomizer((connector, channelConfig, request) ->
        {
            // Mimic ForwardedRequestCustomizer
            if (request.getHttpURI().getAuthority() == null)
            {
                request.setAuthority("default.eclipse.org", -1);
            }
        });
        httpConfig.addCustomizer(new HostHeaderCustomizer("hostheader.eclipse.org", 9009));
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        server.addConnector(connector);
        server.setHandler(new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                baseRequest.setHandled(true);
                assertEquals("jetty.eclipse.org", request.getServerName());
                assertEquals(8888, request.getServerPort());
                assertEquals("jetty.eclipse.org:8888", request.getHeader("Host"));
                response.sendRedirect(redirectPath);
            }
        });
        server.start();
        try
        {
            try (Socket socket = new Socket("localhost", connector.getLocalPort()))
            {
                try (OutputStream output = socket.getOutputStream())
                {
                    String request =
                        "GET http://jetty.eclipse.org:8888/foo HTTP/1.0\r\n" +
                            "\r\n";
                    output.write(request.getBytes(StandardCharsets.UTF_8));
                    output.flush();

                    HttpTester.Input input = HttpTester.from(socket.getInputStream());
                    HttpTester.Response response = HttpTester.parseResponse(input);
                    assertNotNull(response);

                    String actualLocation = response.get("location");
                    String expectedLocation = String.format("http://jetty.eclipse.org:8888%s", redirectPath);
                    assertThat(actualLocation, is(expectedLocation));
                }
            }
        }
        finally
        {
            server.stop();
        }
    }

    @Test
    public void testHostHeaderCustomizerNoHostAuthorityInUriLine() throws Exception
    {
        Server server = new Server();
        HttpConfiguration httpConfig = new HttpConfiguration();
        final String redirectPath = "/redirect";
        httpConfig.addCustomizer(new HostHeaderCustomizer("test_server_name", 13));
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        server.addConnector(connector);
        server.setHandler(new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                baseRequest.setHandled(true);
                assertEquals("jetty.eclipse.org", request.getServerName());
                assertEquals(8888, request.getServerPort());
                assertEquals("jetty.eclipse.org:8888", request.getHeader("Host"));
                response.sendRedirect(redirectPath);
            }
        });
        server.start();
        try
        {
            try (Socket socket = new Socket("localhost", connector.getLocalPort()))
            {
                try (OutputStream output = socket.getOutputStream())
                {
                    String request =
                        "GET http://jetty.eclipse.org:8888/foo HTTP/1.0\r\n" +
                            "\r\n";
                    output.write(request.getBytes(StandardCharsets.UTF_8));
                    output.flush();

                    HttpTester.Input input = HttpTester.from(socket.getInputStream());
                    HttpTester.Response response = HttpTester.parseResponse(input);
                    assertNotNull(response);

                    String actualLocation = response.get("location");
                    String expectedLocation = String.format("http://jetty.eclipse.org:8888%s", redirectPath);
                    assertThat(actualLocation, is(expectedLocation));
                }
            }
        }
        finally
        {
            server.stop();
        }
    }

    @Test
    public void testHostHeaderCustomizerNoHost() throws Exception
    {
        Server server = new Server();
        HttpConfiguration httpConfig = new HttpConfiguration();
        final String serverName = "test_server_name";
        final int serverPort = 13;
        final String redirectPath = "/redirect";
        httpConfig.addCustomizer(new HostHeaderCustomizer(serverName, serverPort));
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        server.addConnector(connector);
        server.setHandler(new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                baseRequest.setHandled(true);
                assertEquals(serverName, request.getServerName());
                assertEquals(serverPort, request.getServerPort());
                assertEquals(serverName + ":" + serverPort, request.getHeader("Host"));
                response.sendRedirect(redirectPath);
            }
        });
        server.start();
        try
        {
            try (Socket socket = new Socket("localhost", connector.getLocalPort()))
            {
                try (OutputStream output = socket.getOutputStream())
                {
                    String request =
                        "GET / HTTP/1.0\r\n" +
                            "\r\n";
                    output.write(request.getBytes(StandardCharsets.UTF_8));
                    output.flush();

                    HttpTester.Input input = HttpTester.from(socket.getInputStream());
                    HttpTester.Response response = HttpTester.parseResponse(input);
                    assertNotNull(response);

                    assertThat(response.getStatus(), is(302));

                    String actualLocation = response.get("location");
                    String expectedLocation = String.format("http://%s:%d%s", serverName, serverPort, redirectPath);
                    assertThat(actualLocation, is(expectedLocation));
                }
            }
        }
        finally
        {
            server.stop();
        }
    }

    @Test
    public void testHostHeaderCustomizerValidAbsoluteHttpUri() throws Exception
    {
        Server server = new Server();
        HttpConfiguration httpConfig = new HttpConfiguration();
        final String redirectPath = "/redirect";
        httpConfig.addCustomizer(new HostHeaderCustomizer("test_server_name", 13));
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        server.addConnector(connector);
        server.setHandler(new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                baseRequest.setHandled(true);
                assertEquals("jetty.eclipse.org", request.getServerName());
                assertEquals(8888, request.getServerPort());
                assertEquals("jetty.eclipse.org:8888", request.getHeader("Host"));
                response.sendRedirect(redirectPath);
            }
        });
        server.start();
        try
        {
            try (Socket socket = new Socket("localhost", connector.getLocalPort()))
            {
                try (OutputStream output = socket.getOutputStream())
                {
                    String request =
                        "GET http://jetty.eclipse.org:8888/foo HTTP/1.1\r\n" +
                            "Host: jetty.eclipse.org:8888\r\n" +
                            "Connection: close\r\n" +
                            "\r\n";
                    output.write(request.getBytes(StandardCharsets.UTF_8));
                    output.flush();

                    HttpTester.Input input = HttpTester.from(socket.getInputStream());
                    HttpTester.Response response = HttpTester.parseResponse(input);
                    assertNotNull(response);

                    String actualLocation = response.get("location");
                    String expectedLocation = String.format("http://jetty.eclipse.org:8888%s", redirectPath);
                    assertThat(actualLocation, is(expectedLocation));
                }
            }
        }
        finally
        {
            server.stop();
        }
    }

    @Test
    public void testHostHeaderCustomizerValidHost() throws Exception
    {
        Server server = new Server();
        HttpConfiguration httpConfig = new HttpConfiguration();
        final String redirectPath = "/redirect";
        httpConfig.addCustomizer(new HostHeaderCustomizer("test_server_name", 13));
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        server.addConnector(connector);
        server.setHandler(new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                baseRequest.setHandled(true);
                assertEquals("jetty.eclipse.org", request.getServerName());
                assertEquals(8888, request.getServerPort());
                assertEquals("jetty.eclipse.org:8888", request.getHeader("Host"));
                response.sendRedirect(redirectPath);
            }
        });
        server.start();
        try
        {
            try (Socket socket = new Socket("localhost", connector.getLocalPort()))
            {
                try (OutputStream output = socket.getOutputStream())
                {
                    String request =
                        "GET /foo HTTP/1.1\r\n" +
                            "Host: jetty.eclipse.org:8888\r\n" +
                            "Connection: close\r\n" +
                            "\r\n";
                    output.write(request.getBytes(StandardCharsets.UTF_8));
                    output.flush();

                    HttpTester.Input input = HttpTester.from(socket.getInputStream());
                    HttpTester.Response response = HttpTester.parseResponse(input);
                    assertNotNull(response);

                    String actualLocation = response.get("location");
                    String expectedLocation = String.format("http://jetty.eclipse.org:8888%s", redirectPath);
                    assertThat(actualLocation, is(expectedLocation));
                }
            }
        }
        finally
        {
            server.stop();
        }
    }

    @Test
    public void testHostHeaderCustomizerEmptyHost() throws Exception
    {
        Server server = new Server();
        HttpConfiguration httpConfig = new HttpConfiguration();
        String connectorHost = "127.0.0.1";
        httpConfig.addCustomizer(new HostHeaderCustomizer("test_server_name", 13));
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        server.addConnector(connector);
        server.setHandler(new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                baseRequest.setHandled(true);
                assertEquals(connectorHost, request.getServerName());
                assertEquals(connector.getLocalPort(), request.getServerPort());
                assertEquals("", request.getHeader("Host"));
                response.sendRedirect("/redirect");
            }
        });
        server.start();
        try
        {
            try (Socket socket = new Socket("localhost", connector.getLocalPort()))
            {
                try (OutputStream output = socket.getOutputStream())
                {
                    String request =
                        "GET /foo HTTP/1.1\r\n" +
                            "Host: \r\n" +
                            "Connection: close\r\n" +
                            "\r\n";
                    output.write(request.getBytes(StandardCharsets.UTF_8));
                    output.flush();

                    HttpTester.Input input = HttpTester.from(socket.getInputStream());
                    HttpTester.Response response = HttpTester.parseResponse(input);
                    assertNotNull(response);

                    String actualLocation = response.get("location");
                    String expectedLocation = String.format("http://%s:%d/redirect", connectorHost, connector.getLocalPort());
                    assertThat(actualLocation, is(expectedLocation));
                }
            }
        }
        finally
        {
            server.stop();
        }
    }

    @Test
    public void testHostHeaderCustomizerEmptyHostWithPort() throws Exception
    {
        Server server = new Server();
        HttpConfiguration httpConfig = new HttpConfiguration();
        String connectorHost = "127.0.0.1";
        httpConfig.addCustomizer(new HostHeaderCustomizer("test_server_name", 13));
        ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
        server.addConnector(connector);
        server.setHandler(new AbstractHandler()
        {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
            {
                baseRequest.setHandled(true);
                assertEquals("", request.getServerName());
                assertEquals(9999, request.getServerPort());
                assertEquals(":9999", request.getHeader("Host"));
                response.sendRedirect("/redirect");
            }
        });
        server.start();
        try
        {
            try (Socket socket = new Socket("localhost", connector.getLocalPort()))
            {
                try (OutputStream output = socket.getOutputStream())
                {
                    String request =
                        "GET /foo HTTP/1.1\r\n" +
                            "Host: :9999\r\n" +
                            "Connection: close\r\n" +
                            "\r\n";
                    output.write(request.getBytes(StandardCharsets.UTF_8));
                    output.flush();

                    HttpTester.Input input = HttpTester.from(socket.getInputStream());
                    HttpTester.Response response = HttpTester.parseResponse(input);
                    assertNotNull(response);

                    assertTrue(HttpStatus.isRedirection(response.getStatus()));

                    String actualLocation = response.get("location");
                    String expectedLocation = String.format("http://:%d/redirect", 9999);
                    assertThat(actualLocation, is(expectedLocation));
                }
            }
        }
        finally
        {
            server.stop();
        }
    }
}

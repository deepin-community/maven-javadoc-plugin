package org.apache.maven.plugins.javadoc;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.maven.plugins.javadoc.JavadocUtil;
import org.apache.maven.plugins.javadoc.ProxyServer.AuthAsyncProxyServlet;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.handler.MovedContextHandler;
import org.mortbay.util.ByteArrayISO8859Writer;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id: JavadocUtilTest.java 1801354 2017-07-09 08:49:46Z rfscholte $
 */
public class JavadocUtilTest
    extends PlexusTestCase
{
    /**
     * Method to test the javadoc version parsing.
     *
     * @throws Exception if any
     */
    public void testParseJavadocVersion()
        throws Exception
    {
        String version = null;
        try
        {
            JavadocUtil.extractJavadocVersion( version );
            assertTrue( "Not catch null", false );
        }
        catch ( IllegalArgumentException e )
        {
            assertTrue( true );
        }

        // Sun JDK 1.4
        version = "java full version \"1.4.2_12-b03\"";
        assertEquals( "1.4.2", JavadocUtil.extractJavadocVersion( version ) );

        // Sun JDK 1.5
        version = "java full version \"1.5.0_07-164\"";
        assertEquals(  "1.5.0", JavadocUtil.extractJavadocVersion( version ) );

        // IBM JDK 1.4
        version = "java full version \"J2RE 1.4.2 IBM Windows 32 build cn1420-20040626\"";
        assertEquals( "1.4.2", JavadocUtil.extractJavadocVersion( version ) );

        // IBM JDK 1.5
        version = "javadoc version compl??te de \"J2RE 1.5.0 IBM Windows 32 build pwi32pdev-20070426a\"";
        assertEquals( "1.5.0", JavadocUtil.extractJavadocVersion( version ) );

        // IBM JDK 1.5
        version = "J2RE 1.5.0 IBM Windows 32 build pwi32devifx-20070323 (ifix 117674: SR4 + 116644 + 114941 + 116110 + 114881)";
        assertEquals( "1.5.0", JavadocUtil.extractJavadocVersion( version ) );

        // FreeBSD
        version = "java full version \"diablo-1.5.0-b01\"";
        assertEquals( "1.5.0", JavadocUtil.extractJavadocVersion( version ) );

        // BEA
        version = "java full version \"1.5.0_11-b03\"";
        assertEquals( "1.5.0", JavadocUtil.extractJavadocVersion( version ) );

        // Other tests
        version = "java full version \"1.5.0_07-164\"" + System.getProperty( "line.separator" );
        assertEquals( "1.5.0", JavadocUtil.extractJavadocVersion( version ) );
        
        version = System.getProperty( "line.separator" ) + "java full version \"1.5.0_07-164\"";
        assertEquals( "1.5.0", JavadocUtil.extractJavadocVersion( version ) );
        
        version = System.getProperty( "line.separator" ) + "java full version \"1.5.0_07-164\""
            + System.getProperty( "line.separator" );
        assertEquals( "1.5.0", JavadocUtil.extractJavadocVersion( version ));
        
        version = "java full" + System.getProperty( "line.separator" ) + " version \"1.5.0_07-164\"";
        assertEquals( "1.5.0", JavadocUtil.extractJavadocVersion( version ) );

        version = "java full version \"1.99.123-b01\"";
        assertEquals( "1.99.123", JavadocUtil.extractJavadocVersion( version ) );

        version = "java full version \"1.5.0.07-164\"";
        assertEquals( "1.5.0", JavadocUtil.extractJavadocVersion( version ) );

        version = "java full version \"1.4\"";
        assertEquals( "1.4" , JavadocUtil.extractJavadocVersion( version ) );

        version = "java full version \"1.A.B_07-164\"";
        try
        {
            JavadocUtil.extractJavadocVersion( version );
            // does not fail since JEP 223 support addition
            //assertTrue( "Not catch wrong pattern", false );
        }
        catch ( PatternSyntaxException e )
        {
            assertTrue( true );
        }

        version = "SCO-UNIX-J2SE-1.5.0_09*FCS-UW714-OSR6*_20061114";
        assertEquals( "1.5.0", JavadocUtil.extractJavadocVersion( version ) );

        // Java 9 EA
        version = "java full version \"9-ea+113\"";
        assertEquals( "9", JavadocUtil.extractJavadocVersion( version ) );

        // Java 9 EA Jigsaw
        version = "java full version \"9-ea+113-2016-04-14-161743.javare.4852.nc\"";
        assertEquals( "9", JavadocUtil.extractJavadocVersion( version ) );

        version = "java version \"9-ea\"";
        assertEquals( "9", JavadocUtil.extractJavadocVersion( version ) );

        // JEP 223 example for future versions
        version = "java full version \"9+100\"";
        assertEquals( "9", JavadocUtil.extractJavadocVersion( version ) );

        version = "java full version \"9.0.1+20\"";
        assertEquals( "9.0.1", JavadocUtil.extractJavadocVersion( version ) );

        version = "java full version \"10+100\"";
        assertEquals( "10" , JavadocUtil.extractJavadocVersion( version ) );

        version = "java full version \"10.0.1+20\"";
        assertEquals( "10.0.1" , JavadocUtil.extractJavadocVersion( version ) );
    }

    /**
     * Method to test the javadoc memory parsing.
     *
     * @throws Exception if any
     */
    public void testParseJavadocMemory()
        throws Exception
    {
        String memory = null;
        try
        {
            JavadocUtil.parseJavadocMemory( memory );
            assertTrue( "Not catch null", false );
        }
        catch ( IllegalArgumentException e )
        {
            assertTrue( true );
        }

        memory = "128";
        assertEquals( JavadocUtil.parseJavadocMemory( memory ), "128m" );

        memory = "128k";
        assertEquals( JavadocUtil.parseJavadocMemory( memory ), "128k" );
        memory = "128kb";
        assertEquals( JavadocUtil.parseJavadocMemory( memory ), "128k" );

        memory = "128m";
        assertEquals( JavadocUtil.parseJavadocMemory( memory ), "128m" );
        memory = "128mb";
        assertEquals( JavadocUtil.parseJavadocMemory( memory ), "128m" );

        memory = "1g";
        assertEquals( JavadocUtil.parseJavadocMemory( memory ), "1024m" );
        memory = "1gb";
        assertEquals( JavadocUtil.parseJavadocMemory( memory ), "1024m" );

        memory = "1t";
        assertEquals( JavadocUtil.parseJavadocMemory( memory ), "1048576m" );
        memory = "1tb";
        assertEquals( JavadocUtil.parseJavadocMemory( memory ), "1048576m" );

        memory = System.getProperty( "line.separator" ) + "128m";
        assertEquals( JavadocUtil.parseJavadocMemory( memory ), "128m" );
        memory = System.getProperty( "line.separator" ) + "128m" + System.getProperty( "line.separator" );
        assertEquals( JavadocUtil.parseJavadocMemory( memory ), "128m" );

        memory = "     128m";
        assertEquals( JavadocUtil.parseJavadocMemory( memory ), "128m" );
        memory = "     128m     ";
        assertEquals( JavadocUtil.parseJavadocMemory( memory ), "128m" );

        memory = "1m28m";
        try
        {
            JavadocUtil.parseJavadocMemory( memory );
            assertTrue( "Not catch wrong pattern", false );
        }
        catch ( IllegalArgumentException e )
        {
            assertTrue( true );
        }
        memory = "ABC128m";
        try
        {
            JavadocUtil.parseJavadocMemory( memory );
            assertTrue( "Not catch wrong pattern", false );
        }
        catch ( IllegalArgumentException e )
        {
            assertTrue( true );
        }
    }

    /**
     * Method to test the validate encoding parsing.
     *
     * @throws Exception if any
     */
    public void testValidateEncoding()
        throws Exception
    {
        assertFalse( "Not catch null", JavadocUtil.validateEncoding( null ) );
        assertTrue( "UTF-8 not supported on this plateform", JavadocUtil.validateEncoding( "UTF-8" ) );
        assertTrue( "ISO-8859-1 not supported on this plateform", JavadocUtil.validateEncoding( "ISO-8859-1" ) );
        assertFalse( "latin is supported on this plateform???", JavadocUtil.validateEncoding( "latin" ) );
        assertFalse( "WRONG is supported on this plateform???", JavadocUtil.validateEncoding( "WRONG" ) );
    }

    /**
     * Method to test the hiding proxy password.
     *
     * @throws Exception if any
     */
    public void testHideProxyPassword()
        throws Exception
    {
        String cmdLine = "javadoc.exe " + "-J-Dhttp.proxySet=true " + "-J-Dhttp.proxyHost=127.0.0.1 "
        + "-J-Dhttp.proxyPort=80 " + "-J-Dhttp.nonProxyHosts=\"www.google.com|*.somewhere.com\" "
        + "-J-Dhttp.proxyUser=\"toto\" " + "-J-Dhttp.proxyPassword=\"toto\" " + "@options @packages";
        cmdLine = JavadocUtil.hideProxyPassword( cmdLine, null );
        assertFalse(cmdLine.contains("-J-Dhttp.proxyPassword=\"****\""));

        Settings settings = new Settings();
        Proxy proxy = new Proxy();
        proxy.setActive( true );
        proxy.setHost( "127.0.0.1" );
        proxy.setPort( 80 );
        proxy.setProtocol( "http" );
        proxy.setUsername( "toto" );
        proxy.setPassword( "toto" );
        proxy.setNonProxyHosts( "www.google.com|*.somewhere.com" );
        settings.addProxy( proxy );

        cmdLine = "javadoc.exe " + "-J-Dhttp.proxySet=true " + "-J-Dhttp.proxyHost=127.0.0.1 "
            + "-J-Dhttp.proxyPort=80 " + "-J-Dhttp.nonProxyHosts=\"www.google.com|*.somewhere.com\" "
            + "-J-Dhttp.proxyUser=\"toto\" " + "-J-Dhttp.proxyPassword=\"toto\" " + "@options @packages";
        cmdLine = JavadocUtil.hideProxyPassword( cmdLine, settings );
        assertTrue(cmdLine.contains("-J-Dhttp.proxyPassword=\"****\""));

        settings = new Settings();
        proxy = new Proxy();
        proxy.setActive( true );
        proxy.setHost( "127.0.0.1" );
        proxy.setPort( 80 );
        proxy.setProtocol( "http" );
        proxy.setUsername( "toto" );
        proxy.setNonProxyHosts( "www.google.com|*.somewhere.com" );
        settings.addProxy( proxy );

        cmdLine = "javadoc.exe " + "-J-Dhttp.proxySet=true " + "-J-Dhttp.proxyHost=127.0.0.1 "
        + "-J-Dhttp.proxyPort=80 " + "-J-Dhttp.nonProxyHosts=\"www.google.com|*.somewhere.com\" "
        + "-J-Dhttp.proxyUser=\"toto\" " + "-J-Dhttp.proxyPassword=\"toto\" " + "@options @packages";
        cmdLine = JavadocUtil.hideProxyPassword( cmdLine, null );
        assertFalse(cmdLine.contains("-J-Dhttp.proxyPassword=\"****\""));
    }

    /**
     * Method to test isValidPackageList()
     *
     * @throws Exception if any
     */
    public void testIsValidPackageList()
        throws Exception
    {
        Settings settings = null;
        Proxy proxy;

        URL url = null;
        URL wrongUrl;
        try
        {
            JavadocUtil.isValidPackageList( url, settings, false );
            fail();
        }
        catch ( IllegalArgumentException e )
        {
            assertTrue( true );
        }

        url = new File( getBasedir(), "/pom.xml" ).toURL();
        assertTrue( JavadocUtil.isValidPackageList( url, settings, false ) );

        try
        {
            assertFalse( JavadocUtil.isValidPackageList( url, settings, true ) );
        }
        catch ( IOException e )
        {
            assertTrue( true );
        }

        url = this.getClass().getResource( "/JavadocUtilTest-package-list.txt" ).toURI().toURL();
        assertTrue( JavadocUtil.isValidPackageList( url, settings, true ) );

        url = new URL( "http://maven.apache.org/plugins/maven-javadoc-plugin/apidocs/package-list" );
        assertTrue( JavadocUtil.isValidPackageList( url, settings, true ) );

        wrongUrl = new URL( "http://maven.apache.org/plugins/maven-javadoc-plugin/apidocs/package-list2" );
        try
        {
            JavadocUtil.isValidPackageList( wrongUrl, settings, false );
            fail();
        }
        catch ( IOException e )
        {
            assertTrue( true );
        }

        // real proxy
        ProxyServer proxyServer = null;
        AuthAsyncProxyServlet proxyServlet;
        try
        {
            proxyServlet = new AuthAsyncProxyServlet();
            proxyServer = new ProxyServer( proxyServlet );
            proxyServer.start();

            settings = new Settings();

            assertTrue( JavadocUtil.isValidPackageList( url, settings, true ) );

            try
            {
                JavadocUtil.isValidPackageList( wrongUrl, settings, false );
                fail();
            }
            catch ( IOException e )
            {
                assertTrue( true );
            }
        }
        finally
        {
            if ( proxyServer != null )
            {
                proxyServer.stop();
            }
        }

        Map<String, String> authentications = new HashMap<>();
        authentications.put( "foo", "bar" );
        // wrong auth
        try
        {
            proxyServlet = new AuthAsyncProxyServlet( authentications );
            proxyServer = new ProxyServer( proxyServlet );
            proxyServer.start();

            settings = new Settings();
            proxy = new Proxy();
            proxy.setActive( true );
            proxy.setHost( proxyServer.getHostName() );
            proxy.setPort( proxyServer.getPort() );
            proxy.setProtocol( "http" );
            settings.addProxy( proxy );

            JavadocUtil.isValidPackageList( url, settings, false );
            fail();
        }
        catch ( FileNotFoundException e )
        {
            assertTrue( true );
        }
        finally
        {
            if ( proxyServer != null )
            {
                proxyServer.stop();
            }
        }

        // auth proxy
        try
        {
            proxyServlet = new AuthAsyncProxyServlet( authentications );
            proxyServer = new ProxyServer( proxyServlet );
            proxyServer.start();

            settings = new Settings();
            proxy = new Proxy();
            proxy.setActive( true );
            proxy.setHost( proxyServer.getHostName() );
            proxy.setPort( proxyServer.getPort() );
            proxy.setProtocol( "http" );
            proxy.setUsername( "foo" );
            proxy.setPassword( "bar" );
            settings.addProxy( proxy );

            assertTrue( JavadocUtil.isValidPackageList( url, settings, true ) );

            try
            {
                JavadocUtil.isValidPackageList( wrongUrl, settings, false );
                fail();
            }
            catch ( IOException e )
            {
                assertTrue( true );
            }
        }
        finally
        {
            if ( proxyServer != null )
            {
                proxyServer.stop();
            }
        }

        // timeout
        try
        {
            proxyServlet = new AuthAsyncProxyServlet( authentications, 3000 ); // more than 2000, see fetchURL
            proxyServer = new ProxyServer( proxyServlet );
            proxyServer.start();

            settings = new Settings();
            proxy = new Proxy();
            proxy.setActive( true );
            proxy.setHost( proxyServer.getHostName() );
            proxy.setPort( proxyServer.getPort() );
            proxy.setProtocol( "http" );
            proxy.setUsername( "foo" );
            proxy.setPassword( "bar" );
            settings.addProxy( proxy );

            JavadocUtil.isValidPackageList( url, settings, true );
            fail();
        }
        catch ( SocketTimeoutException e )
        {
            assertTrue( true );
        }
        finally
        {
            if ( proxyServer != null )
            {
                proxyServer.stop();
            }
        }

        // nonProxyHosts
        try
        {
            proxyServlet = new AuthAsyncProxyServlet( authentications );
            proxyServer = new ProxyServer( proxyServlet );
            proxyServer.start();

            settings = new Settings();
            proxy = new Proxy();
            proxy.setActive( true );
            proxy.setHost( proxyServer.getHostName() );
            proxy.setPort( proxyServer.getPort() );
            proxy.setProtocol( "http" );
            proxy.setUsername( "foo" );
            proxy.setPassword( "bar" );
            proxy.setNonProxyHosts( "maven.apache.org" );
            settings.addProxy( proxy );

            assertTrue( JavadocUtil.isValidPackageList( url, settings, true ) );
        }
        finally
        {
            if ( proxyServer != null )
            {
                proxyServer.stop();
            }
        }
    }

    public void testGetRedirectUrlNotHttp()
        throws Exception
    {
        URL url = new URI( "ftp://some.where" ).toURL();
        assertEquals( url.toString(), JavadocUtil.getRedirectUrl( url, new Settings() ).toString() );

        url = new URI( "file://some/where" ).toURL();
        assertEquals( url.toString(), JavadocUtil.getRedirectUrl( url, new Settings() ).toString() );
    }

    /**
     * Tests a redirect from localhost:port1 to localhost:port2
     */
    public void testGetRedirectUrl()
        throws Exception
    {
        Server server = null, redirectServer = null;
        try
        {
            redirectServer = new Server( 0 );
            redirectServer.addHandler( new AbstractHandler()
            {
                @Override
                public void handle( String target, HttpServletRequest request, HttpServletResponse response,
                                    int dispatch )
                    throws IOException, ServletException
                {
                    response.setStatus( HttpServletResponse.SC_OK );
                    ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer( 100 );
                    writer.write( "<html>Hello world</html>" );
                    writer.flush();
                    response.setContentLength( writer.size() );
                    OutputStream out = response.getOutputStream();
                    writer.writeTo( out );
                    out.close();
                    writer.close();
                }
            } );
            redirectServer.start();

            server = new Server( 0 );
            MovedContextHandler handler = new MovedContextHandler();
            int redirectPort = redirectServer.getConnectors()[0].getLocalPort();
            handler.setNewContextURL( "http://localhost:" + redirectPort );
            server.addHandler( handler );
            server.start();

            URL url = new URI( "http://localhost:" + server.getConnectors()[0].getLocalPort() ).toURL();
            URL redirectUrl = JavadocUtil.getRedirectUrl( url, new Settings() );

            assertTrue( redirectUrl.toString().startsWith( "http://localhost:" + redirectPort ) );
        }
        finally
        {
            stopSilently( server );
            stopSilently( redirectServer );
        }
    }

    /**
     * Method to test copyJavadocResources()
     *
     * @throws Exception if any
     */
    public void testCopyJavadocResources()
        throws Exception
    {
        File input = new File( getBasedir(), "src/test/resources/unit/docfiles-test/docfiles/" );
        assertTrue( input.exists() );

        File output = new File( getBasedir(), "target/test/unit/docfiles-test/target/output" );
        if ( output.exists() )
        {
            FileUtils.deleteDirectory( output );
        }
        assertTrue( output.mkdirs() );

        JavadocUtil.copyJavadocResources( output, input, null );
        List<String> expected = new ArrayList<>();
        expected.add( "test" + File.separator + "doc-files" + File.separator + "excluded-dir1" + File.separator
            + "sample-excluded1.gif" );
        expected.add( "test" + File.separator + "doc-files" + File.separator + "excluded-dir2" + File.separator
            + "sample-excluded2.gif" );
        expected.add( "test" + File.separator + "doc-files" + File.separator + "included-dir1" + File.separator
            + "sample-included1.gif" );
        expected.add( "test" + File.separator + "doc-files" + File.separator + "included-dir2" + File.separator
            + "sample-included2.gif" );
        assertTrue( EqualsBuilder.reflectionEquals( expected, FileUtils.getFiles( output, null, null, false ) ) );
        expected = new ArrayList<>();
        expected.add( "" );
        expected.add( "test" + File.separator + "doc-files" + File.separator + "excluded-dir1" );
        expected.add( "test" + File.separator + "doc-files" + File.separator + "excluded-dir1" );
        expected.add( "test" + File.separator + "doc-files" + File.separator + "included-dir1" );
        expected.add( "test" + File.separator + "doc-files" + File.separator + "included-dir2" );
        assertTrue( EqualsBuilder.reflectionEquals( expected,
                                                    FileUtils.getDirectoryNames( new File( output,
                                                                                           "test/doc-files" ),
                                                                                 null, null, false ) ) );

        input = new File( getBasedir(), "src/test/resources/unit/docfiles-test/docfiles/" );
        assertTrue( input.exists() );

        output = new File( getBasedir(), "target/test/unit/docfiles-test/target/output" );
        if ( output.exists() )
        {
            FileUtils.deleteDirectory( output );
        }
        assertTrue( output.mkdirs() );

        JavadocUtil.copyJavadocResources( output, input, "excluded-dir1:excluded-dir2" );
        expected = new ArrayList<>();
        expected.add( "test" + File.separator + "doc-files" + File.separator + "included-dir1" + File.separator
            + "sample-included1.gif" );
        expected.add( "test" + File.separator + "doc-files" + File.separator + "included-dir2" + File.separator
            + "sample-included2.gif" );
        assertTrue( EqualsBuilder.reflectionEquals( expected, FileUtils.getFiles( output, null, null, false ) ) );
        expected = new ArrayList<>();
        expected.add( "" );
        expected.add( "test" + File.separator + "doc-files" + File.separator + "included-dir1" );
        expected.add( "test" + File.separator + "doc-files" + File.separator + "included-dir2" );
        assertTrue( EqualsBuilder.reflectionEquals( expected,
                                                    FileUtils.getDirectoryNames( new File( output,
                                                                                           "test/doc-files" ),
                                                                                 null, null, false ) ) );
    }

    /**
     * Method to test pruneDirs()
     *
     * @throws Exception if any
     */
    public void testPruneDirs()
        throws Exception
    {
        List<String> list = new ArrayList<>();
        list.add( getBasedir() + "/target/classes" );
        list.add( getBasedir() + "/target/classes" );
        list.add( getBasedir() + "/target/classes" );

        String FS = System.getProperty( "file.separator" );
        Set<String> expected = Collections.singleton( getBasedir() + FS +"target" + FS + "classes" );

        assertEquals( expected, JavadocUtil.pruneDirs( null, list ) );
    }

    /**
     * Method to test unifyPathSeparator()
     *
     * @throws Exception if any
     */
    public void testUnifyPathSeparator()
        throws Exception
    {
        assertEquals( null, JavadocUtil.unifyPathSeparator( null ) );

        final String ps = File.pathSeparator;

        // Windows
        String path1 = "C:\\maven-javadoc-plugin\\src\\main\\java";
        String path2 = "C:\\maven-javadoc-plugin\\src\\main\\javadoc";
        assertEquals( path1 + ps + path2, JavadocUtil.unifyPathSeparator( path1 + ";" + path2 ) );
        assertEquals( path1 + ps + path2, JavadocUtil.unifyPathSeparator( path1 + ":" + path2 ) );

        path1 = "C:/maven-javadoc-plugin/src/main/java";
        path2 = "C:/maven-javadoc-plugin/src/main/javadoc";
        assertEquals( path1 + ps + path2, JavadocUtil.unifyPathSeparator( path1 + ";" + path2 ) );
        assertEquals( path1 + ps + path2, JavadocUtil.unifyPathSeparator( path1 + ":" + path2 ) );
        assertEquals( path1 + ps + path2 + ps + path1 + ps + path2, JavadocUtil.unifyPathSeparator( path1 + ";"
            + path2 + ";" + path1 + ":" + path2 ) );

        // Unix
        path1 = "/tmp/maven-javadoc-plugin/src/main/java";
        path2 = "/tmp/maven-javadoc-plugin/src/main/javadoc";
        assertEquals( path1 + ps + path2, JavadocUtil.unifyPathSeparator( path1 + ";" + path2 ) );
        assertEquals( path1 + ps + path2, JavadocUtil.unifyPathSeparator( path1 + ":" + path2 ) );
        assertEquals( path1 + ps + path2 + ps + path1 + ps + path2, JavadocUtil.unifyPathSeparator( path1 + ";"
            + path2 + ":" + path1 + ":" + path2 ) );
    }

    private void stopSilently( Server server )
    {
        try
        {
            if ( server != null )
            {
                server.stop();
            }
        }
        catch ( Exception e )
        {
            // ignored
        }
    }
}

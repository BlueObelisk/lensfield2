/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield.maven;

import org.apache.maven.properties.internal.EnvironmentUtils;
import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.*;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.sonatype.aether.ConfigurationProperties;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.*;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.DefaultRepositoryCache;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;
import org.sonatype.aether.util.graph.manager.ClassicDependencyManager;
import org.sonatype.aether.util.graph.selector.AndDependencySelector;
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector;
import org.sonatype.aether.util.graph.selector.OptionalDependencySelector;
import org.sonatype.aether.util.graph.selector.ScopeDependencySelector;
import org.sonatype.aether.util.graph.transformer.*;
import org.sonatype.aether.util.graph.traverser.FatArtifactTraverser;
import org.sonatype.aether.util.repository.DefaultAuthenticationSelector;
import org.sonatype.aether.util.repository.DefaultMirrorSelector;
import org.sonatype.aether.util.repository.DefaultProxySelector;

import java.io.File;
import java.util.*;


/**
 * @author sea36
 */
public class DependencyResolver {

    // --- From MavenCli ---

    public static final String userHome = System.getProperty( "user.home" );

    public static final File userMavenConfigurationHome = new File( userHome, ".m2" );

    public static final File DEFAULT_USER_SETTINGS_FILE = new File( userMavenConfigurationHome, "settings.xml" );

    public static final File DEFAULT_GLOBAL_SETTINGS_FILE =
            new File( System.getProperty( "maven.home", System.getProperty( "user.dir", "" ) ), "conf/settings.xml" );

    // ---------------------

    private SettingsBuilder settingsBuilder;
    private SettingsDecrypter settingsDecrypter;

    private boolean offline;

    private final RepositorySystem repositorySystem;
    private final List<RemoteRepository> remoteRepos = new ArrayList<RemoteRepository>();
    private final Settings settings;


    public DependencyResolver() throws Exception {

        ClassWorld classWorld = new ClassWorld( "plexus.core", Thread.currentThread().getContextClassLoader() );
        ContainerConfiguration cc = new DefaultContainerConfiguration()
                .setClassWorld( classWorld )
                .setName( "maven" );
        DefaultPlexusContainer  container = new DefaultPlexusContainer( cc );

        this.settingsBuilder = container.lookup( SettingsBuilder.class );
        this.settingsDecrypter = container.lookup( SettingsDecrypter.class );

        this.settings = getSettings2();
        this.repositorySystem = newRepositorySystem();

        addRepository("central", "http://repo1.maven.org/maven2/");
    }


    public Settings getSettings2() throws SettingsBuildingException {

        Properties systemProperties = new Properties();
        EnvironmentUtils.addEnvVars( systemProperties );
        systemProperties.putAll( System.getProperties() );

        File userSettingsFile = DEFAULT_USER_SETTINGS_FILE;
        File globalSettingsFile = DEFAULT_GLOBAL_SETTINGS_FILE;

        SettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest();
        settingsRequest.setGlobalSettingsFile( globalSettingsFile );
        settingsRequest.setUserSettingsFile( userSettingsFile );
        settingsRequest.setSystemProperties( systemProperties );
//        settingsRequest.setUserProperties( cliRequest.userProperties );

        SettingsBuildingResult settingsResult = settingsBuilder.build( settingsRequest );
        Settings settings = settingsResult.getEffectiveSettings();
        return settings;

    }




    private static Settings loadSettings() throws Exception {
        SettingsBuilder settingsBuilder = newSettingsBuilder();
        SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        SettingsBuildingResult result = settingsBuilder.build(request);
        Settings settings = result.getEffectiveSettings();
        return settings;
    }

    public void addRepository(String id, String url) {
        remoteRepos.add(new RemoteRepository(id, "default", url));
    }


    private static RepositorySystem newRepositorySystem() throws Exception {
        return newManagedSystem();
    }

    private static RepositorySystem newManagedSystem() throws Exception {
        return new DefaultPlexusContainer().lookup(RepositorySystem.class);
    }

    private static SettingsBuilder newSettingsBuilder() throws Exception {
        return new DefaultPlexusContainer().lookup(SettingsBuilder.class);
    }

    private static RepositorySystem newManualSystem() {
        DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.setServices( WagonProvider.class, new ManualWagonProvider() );
        locator.addService( RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class );

        return locator.getService( RepositorySystem.class );
    }



    public RepositorySystemSession newRepositorySession() {

        // Based on org.apache.maven.DefaultMaven

        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession();
        session.setCache(new DefaultRepositoryCache());
        session.setIgnoreInvalidArtifactDescriptor( true );
        session.setIgnoreMissingArtifactDescriptor( true );

        Map<Object, Object> configProps = new LinkedHashMap<Object, Object>();
//        configProps.put( ConfigurationProperties.USER_AGENT, getUserAgent() );
        configProps.put( ConfigurationProperties.INTERACTIVE, Boolean.FALSE );
//        configProps.putAll( request.getSystemProperties() );
//        configProps.putAll( request.getUserProperties() );

        session.setOffline(offline);
        session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_WARN);
        session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_NEVER);

        session.setNotFoundCachingEnabled(true);
        session.setTransferErrorCachingEnabled(true);

//        session.setArtifactTypeRegistry( RepositoryUtils.newArtifactTypeRegistry( artifactHandlerManager ) );

        LocalRepository localRepo = new LocalRepository(getLocalRepository());
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(localRepo));

        DefaultSettingsDecryptionRequest decrypt = new DefaultSettingsDecryptionRequest();
        decrypt.setProxies( settings.getProxies() );
        decrypt.setServers( settings.getServers() );
        SettingsDecryptionResult decrypted = settingsDecrypter.decrypt( decrypt );

        for ( SettingsProblem problem : decrypted.getProblems() ) {
            System.err.println( problem.getMessage());
            problem.getException().printStackTrace();
        }

        DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
        for ( Mirror mirror : settings.getMirrors() )
        {
            mirrorSelector.add( mirror.getId(), mirror.getUrl(), mirror.getLayout(), false, mirror.getMirrorOf(),
                                mirror.getMirrorOfLayouts() );
        }
        session.setMirrorSelector( mirrorSelector );

        DefaultProxySelector proxySelector = new DefaultProxySelector();
        for ( Proxy proxy : decrypted.getProxies() )
        {
            Authentication proxyAuth = new Authentication( proxy.getUsername(), proxy.getPassword() );
            proxySelector.add( new org.sonatype.aether.repository.Proxy( proxy.getProtocol(), proxy.getHost(), proxy.getPort(),
                                                                proxyAuth ), proxy.getNonProxyHosts() );
        }
        session.setProxySelector( proxySelector );

        DefaultAuthenticationSelector authSelector = new DefaultAuthenticationSelector();
        for ( Server server : decrypted.getServers() )
        {
            Authentication auth =
                new Authentication( server.getUsername(), server.getPassword(), server.getPrivateKey(),
                                    server.getPassphrase() );
            authSelector.add( server.getId(), auth );

            if ( server.getConfiguration() != null )
            {
                Xpp3Dom dom = (Xpp3Dom) server.getConfiguration();
                for ( int i = dom.getChildCount() - 1; i >= 0; i-- )
                {
                    Xpp3Dom child = dom.getChild( i );
                    if ( "wagonProvider".equals( child.getName() ) )
                    {
                        dom.removeChild( i );
                    }
                }

                XmlPlexusConfiguration config = new XmlPlexusConfiguration( dom );
                configProps.put( "aether.connector.wagon.config." + server.getId(), config );
            }

            configProps.put( "aether.connector.perms.fileMode." + server.getId(), server.getFilePermissions() );
            configProps.put( "aether.connector.perms.dirMode." + server.getId(), server.getDirectoryPermissions() );
        }
        session.setAuthenticationSelector( authSelector );

        DependencyTraverser depTraverser = new FatArtifactTraverser();
        session.setDependencyTraverser( depTraverser );

        DependencyManager depManager = new ClassicDependencyManager();
        session.setDependencyManager( depManager );

        DependencySelector depFilter =
            new AndDependencySelector( new ScopeDependencySelector( "test", "provided" ), new OptionalDependencySelector(),
                                     new ExclusionDependencySelector() );
        session.setDependencySelector( depFilter );

        DependencyGraphTransformer transformer =
            new ChainedDependencyGraphTransformer( new ConflictMarker(), new JavaEffectiveScopeCalculator(),
                                                   new NearestVersionConflictResolver(),
                                                   new JavaDependencyContextRefiner() );
        session.setDependencyGraphTransformer( transformer );

        session.setTransferListener( new ConsoleTransferListener(System.out));

//        session.setRepositoryListener( new ConsoleRepositoryListener(System.out));

//        session.setUserProps( request.getUserProperties() );
//        session.setSystemProps( request.getSystemProperties() );
        session.setConfigProps( configProps );

        return session;
    }

    private String getLocalRepository() {
        if (settings.getLocalRepository() != null) {
            return settings.getLocalRepository();
        }
        return org.apache.maven.repository.RepositorySystem.defaultUserLocalRepository.getAbsolutePath();
    }


    public void setOffline(boolean offline) {
        this.offline = offline;
    }



    public List<Artifact> resolveDependencies(String groupId, String artifactId, String version) throws ArtifactResolutionException, DependencyCollectionException {
        return resolveDependencies(new DefaultArtifact(groupId, artifactId, "jar", version));
    }

    public List<Artifact> resolveDependencies(Artifact... artifacts) throws ArtifactResolutionException, DependencyCollectionException {

        RepositorySystemSession session = newRepositorySession();

        List<Dependency> dependencies = new ArrayList<Dependency>();
        for (Artifact artifact : artifacts) {
            dependencies.add(new Dependency(artifact, "runtime"));
        }

        CollectRequest collectRequest = new CollectRequest((Dependency)null, dependencies, remoteRepos);
        DependencyNode node = repositorySystem.collectDependencies(session, collectRequest).getRoot();
        repositorySystem.resolveDependencies(session, node, null);

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        node.accept(nlg);
        List<Artifact> files = new ArrayList<Artifact>();
        for (DependencyNode n : nlg.getNodes()) {
            files.add(n.getDependency().getArtifact());
        }
        return files;
    }

}
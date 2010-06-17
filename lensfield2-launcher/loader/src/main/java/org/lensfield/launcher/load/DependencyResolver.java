package org.lensfield.launcher.load;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.cli.AbstractMavenTransferListener;
import org.apache.maven.cli.MavenLoggerManager;
import org.apache.maven.model.Repository;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author sea36
 */
public class DependencyResolver {

    private RepositorySystem repositorySystem;

    private SettingsBuilder settingsBuilder;

    private File localRepository;
    private List<ArtifactRepository> remoteRepos = new ArrayList<ArtifactRepository>();

    public DependencyResolver() throws Exception {

        ClassWorld classWorld = new ClassWorld("plexus.core", DependencyResolver.class.getClassLoader());

        // --- This magic from m2eclipse/MavenPlugin() ---
        ContainerConfiguration cc = new DefaultContainerConfiguration();
        cc.setName("maven-plexus");
        cc.setClassWorld(classWorld);

        DefaultPlexusContainer plexus = new DefaultPlexusContainer(cc);
        plexus.setLoggerManager(new MavenLoggerManager(new ConsoleLogger(org.codehaus.plexus.logging.Logger.LEVEL_WARN, "LOG")));

        this.repositorySystem = plexus.lookup(RepositorySystem.class);
        this.settingsBuilder = plexus.lookup(SettingsBuilder.class);
        // ----

    }

    public void addRepository(String id, String url) throws Exception {
        Repository repository = new Repository();
        repository.setId(id);
        repository.setUrl(url);
        ArtifactRepository repo = repositorySystem.buildArtifactRepository(repository);
        remoteRepos.add(repo);
    }

    public void setLocalRepository(File localRepository) {
        this.localRepository = localRepository;
    }

    public Set<Artifact> resolveDependencies(String groupId, String artifactId, String version, boolean update) throws Exception {

        // --- This magic from m2elipse/internal.embedder.MavenImpl.resolve() ---
        Artifact artifact = repositorySystem.createArtifact(groupId, artifactId, version, "compile", "jar");
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setLocalRepository(getLocalRepository());
        request.setRemoteRepositories(remoteRepos);
        request.setArtifact(artifact);
        request.setResolveTransitively(true);

        // TODO check these settings
        request.setForceUpdate(update);
        request.setOffline(!update);

        request.setTransferListener(new AbstractMavenTransferListener(System.err) {});

        ArtifactResolutionResult result = repositorySystem.resolve(request);
        // ----

        if (!result.isSuccess()) {
            System.err.println(" ** ERROR: Unable to resolve dependency "+groupId+":"+artifact+":"+version);
            System.err.println("           Try running with '--update'");
            if (result.hasExceptions()) {
                for (Exception ex : result.getExceptions()) {
                    System.err.println(ex.getMessage());
                }
            }
            throw new ArtifactResolutionException("", artifact);
        }

        return result.getArtifacts();
    }


    private ArtifactRepository getLocalRepository() throws InvalidRepositoryException {
        return repositorySystem.createLocalRepository(localRepository);
    }


    public Settings getSettings() {
        // Adapted from m2eclipse/org.maven.ide.eclipse.internal.embedder.MavenImpl
        SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        try {
            return settingsBuilder.build(request).getEffectiveSettings();
        } catch(SettingsBuildingException ex) {
            System.err.println(" ** ERROR: Could not read settings.xml, assuming default values");
            return new Settings();
        }
    }

}
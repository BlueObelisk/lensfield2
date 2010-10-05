/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.log4j.Logger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
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
import org.lensfield.api.LensfieldInput;
import org.lensfield.model.Build;
import org.lensfield.model.Model;
import org.lensfield.state.*;
import org.lensfield.state.Process;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * @author sea36
 */
public class DependencyResolver {

    private static final Logger LOG = Logger.getLogger(DependencyResolver.class);

    static {
        System.setProperty("maven.artifact.threads", "1");  // Prevents hanging threads
    }

    private static final ClassWorld classWorld = new ClassWorld("plexus.core", DependencyResolver.class.getClassLoader());

    private final RepositorySystem repositorySystem;
    private final List<ArtifactRepository> remoteRepositories = new ArrayList<ArtifactRepository>();

    private boolean forceUpdate = false;
    private boolean offline = false;
    private SettingsBuilder settingsBuilder;
    private Settings settings;

    private ClassLoader parentClassloader;

    public DependencyResolver(List<String> repositories) throws Exception {
        this(repositories, LensfieldInput.class.getClassLoader());
    }

    public DependencyResolver(List<String> repositories, ClassLoader parentClassLoader) throws Exception {
        this.parentClassloader = parentClassLoader;

        // --- This magic from m2eclipse/MavenPlugin() ---
        ContainerConfiguration cc = new DefaultContainerConfiguration();
        cc.setName("maven-plexus");
        cc.setClassWorld(classWorld);

        DefaultPlexusContainer plexus = new DefaultPlexusContainer(cc);
        plexus.setLoggerManager(new MavenLoggerManager(new ConsoleLogger(org.codehaus.plexus.logging.Logger.LEVEL_WARN, "LOG")));

        this.repositorySystem = plexus.lookup(RepositorySystem.class);
        this.settingsBuilder = plexus.lookup(SettingsBuilder.class);
        // ----
        
        this.settings = getSettings();

        // Init remote repositories
        for (String repoUrl : repositories) {
            Repository repository = new Repository();
            repository.setId(UUID.randomUUID().toString());
            repository.setUrl(repoUrl);
            ArtifactRepository repo = repositorySystem.buildArtifactRepository(repository);
            remoteRepositories.add(repo);
        }

    }


    private ListMultimap<Integer, Artifact> resolveDependencies(Collection<org.lensfield.model.Dependency> dependencies) throws InvalidRepositoryException, LensfieldException, MalformedURLException {

        ListMultimap<Integer,Artifact> map = ArrayListMultimap.create();

        for (org.lensfield.model.Dependency dependency : dependencies) {

            // --- This magic from m2elipse/internal.embedder.MavenImpl.resolve() ---
            Artifact artifact = repositorySystem.createArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), "compile", "jar");
            ArtifactResolutionRequest request = new ArtifactResolutionRequest();
            request.setLocalRepository(getLocalRepository());
            request.setRemoteRepositories(getRemoteRepositories());
            request.setArtifact(artifact);
            request.setResolveTransitively(true);
            request.setForceUpdate(forceUpdate);
            request.setOffline(isOffline());

            request.setTransferListener(new AbstractMavenTransferListener(System.err) {});

            ArtifactResolutionResult result = repositorySystem.resolve(request);
            // ----

            if (!result.isSuccess()) {
                throw new LensfieldException("Unable to resolve dependency: "+result.getMissingArtifacts());
            }

            map.putAll(0, result.getArtifacts());

        }

        return map;
    }


    private ArtifactRepository getLocalRepository() throws InvalidRepositoryException {
        File localRepo;
        if (System.getProperty("m2.repo") != null) {
            localRepo = new File(System.getProperty("m2.repo"));
        }
        else if (System.getenv("M2_REPO") != null) {
            localRepo = new File(System.getenv("M2_REPO"));
        }
        else if (settings.getLocalRepository() != null) {
            localRepo = new File(settings.getLocalRepository());
        } else {
            localRepo = RepositorySystem.defaultUserLocalRepository;
        }
        return repositorySystem.createLocalRepository(localRepo);
    }


    private List<ArtifactRepository> getRemoteRepositories() {
        ArrayList<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();

        for (ArtifactRepository repo : remoteRepositories) {
            repositories.add(repo);
        }

        addDefaultRepository(repositories);

        return repositories;
    }


    private void addDefaultRepository(ArrayList<ArtifactRepository> repositories) {
        for (ArtifactRepository repository : repositories) {
            if (RepositorySystem.DEFAULT_REMOTE_REPO_ID.equals(repository.getId())) {
                return;
            }
        }
        try {
            repositories.add(0, repositorySystem.createDefaultRemoteRepository());
        } catch(InvalidRepositoryException ex) {
            ex.printStackTrace();
        }
    }

    public void configureDependencies(Model model, BuildState buildState) throws Exception {

        System.err.println("[DEBUG] Resolving global dependencies");
        ListMultimap<Integer,Artifact> globalDependencies = resolveDependencies(model.getDependencies());
        for (Build build : model.getBuilds()) {
            System.err.println("[DEBUG] Resolving dependencies for: "+build.getName());
            Process task = buildState.getTask(build.getName());
            configureDependencies(build, task, globalDependencies);
        }

    }

    private void configureDependencies(Build build, Process task, ListMultimap<Integer,Artifact> globalDependencies)  throws Exception {
        ListMultimap<Integer,Artifact> buildDependencies = resolveDependencies(build.getDependencies());
        buildDependencies.putAll(globalDependencies);
        List<Artifact> dependencyList = getDependencyList(buildDependencies);
        updateBuildState(task, dependencyList);
    }

    private void updateBuildState(org.lensfield.state.Process task, List<Artifact> dependencyList) throws Exception {
        for (Artifact artifact : dependencyList) {
            Dependency dependency = new Dependency(artifact.getId(), artifact.getFile());
            task.addDependency(dependency);
        }
        URL[] urls = getUrls(dependencyList);
        task.setDependencies(urls, parentClassloader);
    }

    private List<Artifact> getDependencyList(ListMultimap<Integer,Artifact> dependencyMap) {
        List<Integer> depthList = new ArrayList<Integer>(dependencyMap.keySet());
        Collections.sort(depthList);
        Set<Artifact> dependencyList = new LinkedHashSet<Artifact>();
        for (Integer depth : depthList) {
            dependencyList.addAll(dependencyMap.get(depth));
        }
        return new ArrayList<Artifact>(dependencyList);
    }

    private URL[] getUrls(List<Artifact> dependencyList) throws MalformedURLException {
        int n = dependencyList.size();
        URL[] urls = new URL[n];
        for (int i = 0; i < n; i++) {
            File f = dependencyList.get(i).getFile();
            URL url = f.toURI().toURL();
            urls[i] = url;
        }
        return urls;
    }

    private Settings getSettings() {
        // Adapted from m2eclipse/org.maven.ide.eclipse.internal.embedder.MavenImpl
        SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        try {
            return settingsBuilder.build(request).getEffectiveSettings();
        } catch(SettingsBuildingException ex) {
            String msg = "Could not read settings.xml, assuming default values";
            LOG.error(msg);
            /*
            * NOTE: This method provides input for various other core functions, just bailing out would make m2e highly
            * unusuable. Instead, we fail gracefully and just ignore the broken settings, using defaults.
            */
            return new Settings();
        }
    }


    public ClassLoader createClassLoader(List<org.lensfield.model.Dependency> dependencies) throws Exception {
        ListMultimap<Integer,Artifact> buildDependencies = resolveDependencies(dependencies);
        List<Artifact> dependencyList = getDependencyList(buildDependencies);

        URL[] urls = getUrls(dependencyList);
        URLClassLoader loader = new URLClassLoader(urls, parentClassloader);
        return loader;        
    }


    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }
    
}

/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionNode;
import org.apache.maven.cli.AbstractMavenTransferListener;
import org.apache.maven.cli.MavenLoggerManager;
import org.apache.maven.model.Repository;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.lensfield.model.Build;
import org.lensfield.model.Dependency;

import org.apache.log4j.Logger;
import org.lensfield.model.Model;
import org.lensfield.state.BuildState;
import org.lensfield.state.DependencyState;
import org.lensfield.state.TaskState;

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

    private final RepositorySystem repositorySystem;
    private final List<ArtifactRepository> remoteRepositories = new ArrayList<ArtifactRepository>();

    private boolean forceUpdate = false;
    private boolean offline = false;


    public DependencyResolver(List<String> repositories) throws PlexusContainerException, ComponentLookupException, InvalidRepositoryException {

        // --- This magic from m2eclipse/MavenPlugin() ---
        ClassWorld classWorld = new ClassWorld("plexus.core",
                Thread.currentThread().getContextClassLoader());

        ContainerConfiguration cc = new DefaultContainerConfiguration();
        cc.setName("maven-plexus");
        cc.setClassWorld(classWorld);

        DefaultPlexusContainer plexus = new DefaultPlexusContainer(cc);
        plexus.setLoggerManager(new MavenLoggerManager(new ConsoleLogger(org.codehaus.plexus.logging.Logger.LEVEL_WARN, "LOG")));

        repositorySystem = plexus.lookup(RepositorySystem.class);
        // ----

        // Init remote repositories
        for (String repoUrl : repositories) {
            Repository repository = new Repository();
            repository.setId(UUID.randomUUID().toString());
            repository.setUrl(repoUrl);
            ArtifactRepository repo = repositorySystem.buildArtifactRepository(repository);
            remoteRepositories.add(repo);
        }

    }


    private ListMultimap<Integer, Artifact> resolveDependencies(Collection<Dependency> dependencies) throws InvalidRepositoryException, LensfieldException, MalformedURLException {

        ListMultimap<Integer,Artifact> map = ArrayListMultimap.create();

        for (Dependency dependency : dependencies) {

            // --- This magic from m2elipse/internal.embedder.MavenImpl.resolve() ---
            Artifact artifact = repositorySystem.createArtifact(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), "compile", "jar");
            ArtifactResolutionRequest request = new ArtifactResolutionRequest();
            request.setLocalRepository(getLocalRepository());
            request.setRemoteRepositories(getRemoteRepositories());
            request.setArtifact(artifact);
            request.setResolveTransitively(true);
            request.setForceUpdate(forceUpdate);
            request.setOffline(offline);

            request.setTransferListener(new AbstractMavenTransferListener(System.err) {});

            ArtifactResolutionResult result = repositorySystem.resolve(request);
            // ----

            if (!result.isSuccess()) {
                throw new LensfieldException("Unable to resolve dependency: "+result.getMissingArtifacts());
            }

            if (result.getOriginatingArtifact() != null) {
                map.put(0, result.getOriginatingArtifact());
            }
            for (ResolutionNode node : result.getArtifactResolutionNodes()) {
                if (node.getArtifact() != null) {
                    map.put(node.getDepth(), node.getArtifact());
                }
            }
        }

        return map;
    }


    private ArtifactRepository getLocalRepository() throws InvalidRepositoryException {
        return repositorySystem.createLocalRepository(RepositorySystem.defaultUserLocalRepository);
    }


    private List<ArtifactRepository> getRemoteRepositories() {
        ArrayList<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();

//        for(Profile profile : getActiveProfiles()) {
//          addArtifactRepositories(repositories, profile.getRepositories());
//        }

        for (ArtifactRepository repo : remoteRepositories) {
            repositories.add(repo);
        }

        addDefaultRepository(repositories);

//        injectSettings(repositories);
//        removeDuplicates(repositories);

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
//            MavenLogger.log("Unexpected exception", ex);
            ex.printStackTrace();
        }
    }


//  private void injectSettings(ArrayList<ArtifactRepository> repositories) throws CoreException {
//    Settings settings = getSettings();
//
//    repositorySystem.injectMirror(repositories, getMirrors());
//    repositorySystem.injectProxy(repositories, settings.getProxies());
//    repositorySystem.injectAuthentication(repositories, settings.getServers());
//  }


//
//  private List<ArtifactRepository> removeDuplicateRepositories(ArrayList<ArtifactRepository> repositories) {
//    ArrayList<ArtifactRepository> result = new ArrayList<ArtifactRepository>();
//
//    HashSet<String> keys = new HashSet<String>();
//    for (ArtifactRepository repository : repositories) {
//      StringBuilder key = new StringBuilder();
//      if (repository.getId() != null) {
//        key.append(repository.getId());
//      }
//      key.append(':').append(repository.getUrl()).append(':');
//      if (repository.getAuthentication() != null && repository.getAuthentication().getUsername() != null) {
//        key.append(repository.getAuthentication().getUsername());
//      }
//      if (keys.add(key.toString())) {
//        result.add(repository);
//      }
//    }
//    return result;
//  }
//
//
//
//  private void addArtifactRepositories(ArrayList<ArtifactRepository> artifactRepositories, List<Repository> repositories) throws CoreException {
//    for(Repository repository : repositories) {
//      try {
//        ArtifactRepository artifactRepository = repositorySystem.buildArtifactRepository(repository);
//        artifactRepositories.add(artifactRepository);
//      } catch(InvalidRepositoryException ex) {
//        throw new CoreException(new Status(IStatus.ERROR, IMavenConstants.PLUGIN_ID, -1, "Could not read settings.xml",
//            ex));
//      }
//    }
//  }


    public void configureDependencies(Model model, BuildState buildState) throws Exception {

        ListMultimap<Integer,Artifact> globalDependencies = resolveDependencies(model.getDependencies());
        for (Build build : model.getBuilds()) {
            TaskState task = buildState.getTask(build.getName());
            configureDependencies(build, task, globalDependencies);
        }

    }

    private void configureDependencies(Build build, TaskState task, ListMultimap<Integer,Artifact> globalDependencies)  throws Exception {
        ListMultimap<Integer,Artifact> buildDependencies = resolveDependencies(build.getDependencies());
        buildDependencies.putAll(globalDependencies);
        List<Artifact> dependencyList = getDependencyList(buildDependencies);
        updateBuildState(task, dependencyList);
    }

    private void updateBuildState(TaskState task, List<Artifact> dependencyList) throws Exception {
        for (Artifact artifact : dependencyList) {
            DependencyState dependency = new DependencyState(artifact.getId(), artifact.getFile());
            task.addDependency(dependency);
        }
        URL[] urls = getUrls(dependencyList);
        URLClassLoader classloader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
        Class<?> clazz = classloader.loadClass(task.getClassName());
        task.setClazz(clazz);
    }

    private List<Artifact> getDependencyList(ListMultimap<Integer,Artifact> dependencyMap) {
        List<Integer> depthList = new ArrayList<Integer>(dependencyMap.keySet());
        Collections.sort(depthList);
        List<Artifact> dependencyList = new ArrayList<Artifact>();
        for (Integer depth : depthList) {
            dependencyList.addAll(dependencyMap.get(depth));
        }
        return dependencyList;
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
        
}

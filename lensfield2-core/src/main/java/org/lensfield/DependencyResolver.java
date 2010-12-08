/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import org.apache.log4j.Logger;
import org.lensfield.api.LensfieldInput;
import org.lensfield.model.BuildStep;
import org.lensfield.model.Model;
import org.lensfield.state.BuildState;
import org.lensfield.state.Dependency;
import org.lensfield.state.Process;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * @author sea36
 */
public class DependencyResolver {

    private static final Logger LOG = Logger.getLogger(DependencyResolver.class);

    private org.lensfield.maven.DependencyResolver resolver;

    private ClassLoader parentClassloader = LensfieldInput.class.getClassLoader();


    public DependencyResolver(List<String> repositories) throws Exception {
        this.resolver = new org.lensfield.maven.DependencyResolver();
        for (String url : repositories) {
            resolver.addRepository(UUID.randomUUID().toString(), url);
        }
    }

    public void setOffline(boolean offline) {
        resolver.setOffline(offline);
    }

    public void configureDependencies(Model model, BuildState buildState) throws Exception {
        LOG.info("Resolving global dependencies");
        for (BuildStep build : model.getBuildSteps()) {
            LOG.info("Resolving dependencies for: "+build.getName());
            Process task = buildState.getTask(build.getName());
            configureDependencies(task, build.getDependencies(), model.getDependencies());
        }
    }

    private void configureDependencies(Process task, List<org.lensfield.model.Dependency> dependencies, List<org.lensfield.model.Dependency> globalDependencies) throws Exception {
        List<Artifact> dependencyList = resolveDependencies(dependencies, globalDependencies);
        updateBuildState(task, dependencyList);
    }


    private void updateBuildState(Process task, List<Artifact> dependencyList) throws Exception {
        for (Artifact artifact : dependencyList) {
            Dependency dependency = new Dependency(artifact.getGroupId()+":"+artifact.getArtifactId()+":"+artifact.getVersion(), artifact.getFile());
            task.addDependency(dependency);
        }
        URL[] urls = getUrls(dependencyList);
        task.setDependencyUrls(urls, parentClassloader);
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

    private List<Artifact> resolveDependencies(List<org.lensfield.model.Dependency> taskDependencies, List<org.lensfield.model.Dependency> globalDependencies) throws ArtifactResolutionException, DependencyCollectionException {
        List<Artifact> artifacts = new ArrayList<Artifact>();
        for (org.lensfield.model.Dependency d : taskDependencies) {
            artifacts.add(new DefaultArtifact(d.getGroupId(), d.getArtifactId(), "jar", d.getVersion()));
        }
        for (org.lensfield.model.Dependency d : globalDependencies) {
            artifacts.add(new DefaultArtifact(d.getGroupId(), d.getArtifactId(), "jar", d.getVersion()));
        }
        return resolver.resolveDependencies(artifacts.toArray(new Artifact[artifacts.size()]));
    }

}

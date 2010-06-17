package org.lensfield.launcher;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * @author sea36
 */
public class LensfieldLauncher {

    private ClassWorld classWorld;
    private boolean update;

    private Properties properties;

    private LensfieldLauncher(ClassWorld classWorld, boolean update) throws Exception {

        this.classWorld = classWorld;
        this.update = update;

    }

    private void load() throws Exception {

        DependencyResolver resolver = new DependencyResolver(classWorld);
        Settings settings = resolver.getSettings();
        File localRepo;
        if (settings.getLocalRepository() != null) {
            localRepo = new File(settings.getLocalRepository());
        } else {
            localRepo = RepositorySystem.defaultUserLocalRepository;
        }
        resolver.setLocalRepository(localRepo);

        if (update) {
            resolver.addRepository("ucc-repo", "https://maven.ch.cam.ac.uk/m2repo");
        }

        ClassRealm api = classWorld.newRealm("lensfield.api");
        for (Artifact a : resolver.resolveDependencies("org.lensfield", "lensfield2-api", "0.1-SNAPSHOT", update)) {
            api.addURL(a.getFile().toURI().toURL());
        }
        for (Artifact a : resolver.resolveDependencies("log4j", "log4j", "1.2.13", update)) {
            api.addURL(a.getFile().toURI().toURL());
        }

        ClassRealm core = classWorld.newRealm("lensfield.core", api);
        core.importFrom("plexus.core", "");
        for (Artifact a : resolver.resolveDependencies("org.lensfield", "lensfield2-cli", "0.1-SNAPSHOT", update)) {
            core.addURL(a.getFile().toURI().toURL());
        }

    }

    private void run(String[] args) throws Exception {

        Class<?> clazz = classWorld.getRealm("lensfield.core").loadClass("org.lensfield.cli.LensfieldCli");
        Method method = clazz.getMethod("main", String[].class, ClassWorld.class);
        method.invoke(null, new Object[]{args, classWorld});

    }

    public static void main(String[] args, ClassWorld classworld) throws Exception {

        boolean update;
        if (args.length == 1 && "--update".equals(args[0])) {
            System.err.println("Updating lensfield...");
            update = true;
        } else {
            update = false;
        }

        LensfieldLauncher loader = new LensfieldLauncher(classworld, update);
        try {
            loader.load();
        } catch (ArtifactResolutionException e) {
            return;
        }
        if (update) {
            return;
        }
        try {
            loader.run(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println();
            System.err.println(" ** BUILD FAILED");
            System.exit(1);
        }

    }

}
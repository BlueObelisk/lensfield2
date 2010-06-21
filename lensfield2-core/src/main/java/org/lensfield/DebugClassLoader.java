package org.lensfield;

import org.lensfield.api.LensfieldInput;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author sea36
 */
public class DebugClassLoader extends URLClassLoader {

    private static final Map<ClassLoader,String> MAP = new HashMap<ClassLoader, String>();
    private static final Map<String,Integer> COUNT = new HashMap<String, Integer>();

    static {
        MAP.put(ClassLoader.getSystemClassLoader(), "[JVM-SYSTEM]");
        MAP.put(ClassLoader.getSystemClassLoader().getParent(), "[JVM-EXT]");
        MAP.put(LensfieldInput.class.getClassLoader(), "[LENSFIELD-API]");
        MAP.put(Lensfield.class.getClassLoader(), "[LENSFIELD-APP]");
    }

    public DebugClassLoader(URL[] urls, ClassLoader parent, String name) {
        super(urls, parent);
        register(name, this);
    }

    private static synchronized void register(String name, ClassLoader cl) {
        Integer i = COUNT.get(name);
        if (i == null) {
            i = 1;
        } else {
            i++;
        }
        COUNT.put(name,i);
        MAP.put(cl, name+"/"+i);
    }


    public static String name(ClassLoader cl) {
        return MAP.containsKey(cl) ? MAP.get(cl) : (cl == null ? null : cl.toString());
    }

    public static void debug(ClassLoader cl) {
        System.err.println("--- CLASSLOADER: "+name(cl)+"  : "+cl);
        while ((cl = cl.getParent()) != null) {
            System.err.println(" "+name(cl)+"  : "+cl);
        }
        System.err.println("-----");
    }

    public static void debug(Class clazz) {
        System.err.println("--- CLASS: "+clazz.getName());
        ClassLoader cl = clazz.getClassLoader();
        while (cl != null) {
            System.err.println(" "+name(cl)+"  : "+cl);
            cl = cl.getParent();
        }
        System.err.println("-----");
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> clazz = super.loadClass(name);
//        System.err.println("!!! CLASSLOADER: "+name(this)+" /load: "+clazz.getName()+" loader: "+name(clazz.getClassLoader()));
        return clazz;
    }


    public String toString() {
        return "CLASSLOADER:"+name(this);
    }

}

/*
 * Copyright 2010 Sam Adams
 */
package org.lensfield;

import org.junit.Test;
import org.lensfield.model.BuildStep;
import org.lensfield.model.Model;
import org.lensfield.model.Process;
import org.lensfield.model.Source;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

/**
 * @author sea36
 */
public class LensfieldProcessorTest {

    @Test
    public void testResolveBuildOrderLinear() throws LensfieldException {

        Model model = new Model();
        model.addSource(new Source("step1", "**/*.1"));
        model.addBuildStep(new BuildStep("step2", "com.example.Foo", "step1", "**/*.2"));
        model.addBuildStep(new BuildStep("step3", "com.example.Foo", "step2", "**/*.3"));

        Lensfield lf = new Lensfield(model, null);

        ArrayList<Process> order = lf.resolveBuildOrder();
        assertEquals(3, order.size());
        assertEquals("step1", order.get(0).getName());
        assertEquals("step2", order.get(1).getName());
        assertEquals("step3", order.get(2).getName());
    }

    @Test
    public void testResolveBuildOrderShuffledLinear() throws LensfieldException {
        Model model = new Model();
        model.addBuildStep(new BuildStep("step3", "com.example.Foo", "step2", "**/*.3"));
        model.addSource(new Source("step1", "**/*.1"));
        model.addBuildStep(new BuildStep("step2", "com.example.Foo", "step1", "**/*.2"));
        
        Lensfield lf = new Lensfield(model, null);

        ArrayList<Process> order = lf.resolveBuildOrder();
        assertEquals(3, order.size());
        assertEquals("step1", order.get(0).getName());
        assertEquals("step2", order.get(1).getName());
        assertEquals("step3", order.get(2).getName());
    }

    /*
    @Test
    public void testResolveBuildOrderBranched() throws LensfieldException {
        Model model = new Model();
        model.addSource(new Source("step1", "** /*.1"));
        model.addBuildStep(new BuildStep("step2", "foo", "step1", "");
        model.addBuildStep(new BuildStep("step3", "foo", "step2", "");
        model.addBuildStep(new BuildStep("step4", "foo", "step1", "");
        model.addBuildStep(new BuildStep("step5", "foo", new String[]{"step1", "step7"}, new String[]);
        model.addBuildStep(new BuildStep("step6", "foo", "step4", "");
        model.addBuildStep(new BuildStep("step7", "foo", "step3", "");


        List<String> order = model.resolveBuildOrder();
        assertEquals(7, order.size());
        assertEquals("step1", order.get(0));
        assertEquals("step2", order.get(1));
        assertEquals("step4", order.get(2));
        assertEquals("step3", order.get(3));
        assertEquals("step6", order.get(4));
        assertEquals("step7", order.get(5));
        assertEquals("step5", order.get(6));
    }

    @Test(expected = LensfieldException.class)
    public void testResolveBuildOrderCyclic() throws LensfieldException {
        Lensfield proc = new Lensfield();
        proc.addBuildStep(new BuildStep("step1", "foo", new String[0], "");
        proc.addBuildStep(new BuildStep("step2", "foo", "step1","step3"}, "");
        proc.addBuildStep(new BuildStep("step3", "foo", "step2"}, "");
        proc.resolveBuildOrder();
    }


    @Test
    public void testCheckBuildsOkay() throws LensfieldException {
        Lensfield proc = new Lensfield();
        proc.addBuildStep(new BuildStep("step1", "foo", new String[0], "");
        proc.addBuildStep(new BuildStep("step2", "foo", "step1"}, "");
        proc.addBuildStep(new BuildStep("step3", "foo", "step2"}, "");
        proc.checkBuilds();
    }

*/

    @Test(expected = LensfieldException.class)
    public void testCheckBuildsMissingDependency() throws LensfieldException {
        Model model = new Model();
        model.addSource(new Source("step1", "**/*.1"));
        model.addBuildStep(new BuildStep("step2", "foo", "stepX", ""));
        model.addBuildStep(new BuildStep("step3", "foo", "step2", ""));

        Lensfield lf = new Lensfield(model, null);
        lf.checkBuildStepsExist();

    }


}

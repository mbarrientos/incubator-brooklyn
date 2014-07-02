package brooklyn.entity.trait;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import brooklyn.entity.BrooklynAppUnitTestSupport;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.FailingEntity.RecordingEventListener;
import brooklyn.location.basic.SimulatedLocation;
import brooklyn.management.Task;
import brooklyn.test.entity.TestEntity;
import brooklyn.util.task.Tasks;

import com.google.common.collect.ImmutableList;

public class StartableMethodsTest extends BrooklynAppUnitTestSupport {

    private SimulatedLocation loc;
    private TestEntity entity;
    private TestEntity entity2;
    private RecordingEventListener listener;
    
    @BeforeMethod(alwaysRun=true)
    @Override
    public void setUp() throws Exception {
        super.setUp();
        loc = new SimulatedLocation();
        listener = new RecordingEventListener();
    }
    
    @Test
    public void testStopSequentially() {
        entity = app.createAndManageChild(EntitySpec.create(FailingEntity.class)
                .configure(FailingEntity.LISTENER, listener));
        entity2 = app.createAndManageChild(EntitySpec.create(FailingEntity.class)
                .configure(FailingEntity.LISTENER, listener));
        app.start(ImmutableList.of(loc));
        listener.events.clear();
        
        StartableMethods.stopSequentially(ImmutableList.of(entity, entity2));
        
        assertEquals(listener.events.get(0)[0], entity);
        assertEquals(listener.events.get(1)[0], entity2);
    }
    
    @Test
    public void testStopSequentiallyContinuesOnFailure() {
        try {
            entity = app.createAndManageChild(EntitySpec.create(FailingEntity.class)
                    .configure(FailingEntity.FAIL_ON_STOP, true)
                    .configure(FailingEntity.LISTENER, listener));
            entity2 = app.createAndManageChild(EntitySpec.create(FailingEntity.class)
                    .configure(FailingEntity.LISTENER, listener));
            app.start(ImmutableList.of(loc));
            listener.events.clear();
            
            try {
                StartableMethods.stopSequentially(ImmutableList.of(entity, entity2));
                fail();
            } catch (Exception e) {
                // success; expected exception to be propagated
            }
            
            assertEquals(listener.events.get(0)[0], entity);
            assertEquals(listener.events.get(1)[0], entity2);
        } finally {
            // get rid of entity that will fail on stop, so that tearDown won't encounter exception
            Entities.unmanage(entity);
        }
    }
    
    @Test
    public void testStopSequentiallyContinuesOnFailureInSubTask() throws Exception {
        try {
            entity = app.createAndManageChild(EntitySpec.create(FailingEntity.class)
                    .configure(FailingEntity.FAIL_ON_STOP, true)
                    .configure(FailingEntity.FAIL_IN_SUB_TASK, true)
                    .configure(FailingEntity.LISTENER, listener));
            entity2 = app.createAndManageChild(EntitySpec.create(FailingEntity.class)
                    .configure(FailingEntity.LISTENER, listener));
            app.start(ImmutableList.of(loc));
            listener.events.clear();
            
            try {
                Task<?> task = Tasks.builder().name("stopSequentially")
                        .body(new Runnable() {
                            @Override public void run() {
                                StartableMethods.stopSequentially(ImmutableList.of(entity, entity2));
                            }})
                        .build();
                Entities.submit(app, task).getUnchecked();
                fail();
            } catch (Exception e) {
                // success; expected exception to be propagated
                if (!(e.toString().contains("Error stopping"))) throw e;
            }
            
            assertEquals(listener.events.get(0)[0], entity);
            assertEquals(listener.events.get(1)[0], entity2);
        } finally {
            // get rid of entity that will fail on stop, so that tearDown won't encounter exception
            Entities.unmanage(entity);
        }
    }
}

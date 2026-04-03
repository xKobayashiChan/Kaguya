package com.example.lexiyaddons.event;

import com.example.lexiyaddons.event.events.Event;
import com.example.lexiyaddons.event.events.EventStoppable;
import com.example.lexiyaddons.event.types.Priority;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class EventManagerTest {

    // --- Test events ---

    public static class TestEvent implements Event {
        public int counter = 0;
    }

    public static class AnotherTestEvent implements Event {
        public String message = "";
    }

    public static class TestStoppableEvent extends EventStoppable {
        public int handleCount = 0;
    }

    // --- Test listeners ---

    public static class TestListener {
        public int callCount = 0;

        @EventTarget
        public void onTestEvent(TestEvent event) {
            callCount++;
            event.counter++;
        }
    }

    public static class AnotherTestListener {
        public boolean wasCalled = false;

        @EventTarget
        public void onAnotherEvent(AnotherTestEvent event) {
            wasCalled = true;
            event.message += "handled";
        }
    }

    public static class MultiEventListener {
        public int testCalls = 0;
        public int anotherCalls = 0;

        @EventTarget
        public void onTest(TestEvent event) {
            testCalls++;
        }

        @EventTarget
        public void onAnother(AnotherTestEvent event) {
            anotherCalls++;
        }
    }

    public static class PriorityHighListener {
        public static List<String> callOrder;

        @EventTarget(Priority.HIGH)
        public void onEvent(TestEvent event) {
            callOrder.add("HIGH");
        }
    }

    public static class PriorityLowListener {
        @EventTarget(Priority.LOW)
        public void onEvent(TestEvent event) {
            PriorityHighListener.callOrder.add("LOW");
        }
    }

    public static class StoppableListener1 {
        @EventTarget(Priority.HIGHEST)
        public void onEvent(TestStoppableEvent event) {
            event.handleCount++;
            event.stop();
        }
    }

    public static class StoppableListener2 {
        @EventTarget(Priority.LOWEST)
        public void onEvent(TestStoppableEvent event) {
            event.handleCount++;
        }
    }

    public static class NoAnnotationListener {
        // This method should NOT be registered
        public void onEvent(TestEvent event) {
        }
    }

    public static class WrongParamCountListener {
        // This method should NOT be registered (two params)
        @EventTarget
        public void onEvent(TestEvent event, String extra) {
        }
    }

    /**
     * Clears the EventManager's internal REGISTRY_MAP via reflection.
     * Note: cleanMap(false) has a bug where iterator.next() is never called
     * due to short-circuit evaluation of (!onlyEmptyEntries || ...), causing
     * IllegalStateException. We use reflection to clear the map directly.
     */
    @SuppressWarnings("unchecked")
    private static void clearRegistryMap() {
        try {
            Field field = EventManager.class.getDeclaredField("REGISTRY_MAP");
            field.setAccessible(true);
            ((HashMap<?, ?>) field.get(null)).clear();
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear REGISTRY_MAP", e);
        }
    }

    @Before
    public void setUp() {
        clearRegistryMap();
    }

    @After
    public void tearDown() {
        clearRegistryMap();
    }

    // --- register / call ---

    @Test
    public void registerAndCallEvent() {
        TestListener listener = new TestListener();
        EventManager.register(listener);
        TestEvent event = new TestEvent();
        EventManager.call(event);
        assertEquals(1, listener.callCount);
        assertEquals(1, event.counter);
    }

    @Test
    public void callEventWithNoListeners() {
        TestEvent event = new TestEvent();
        Event result = EventManager.call(event);
        assertSame(event, result);
        assertEquals(0, event.counter);
    }

    @Test
    public void multipleListenersReceiveEvent() {
        TestListener listener1 = new TestListener();
        TestListener listener2 = new TestListener();
        EventManager.register(listener1);
        EventManager.register(listener2);
        TestEvent event = new TestEvent();
        EventManager.call(event);
        assertEquals(1, listener1.callCount);
        assertEquals(1, listener2.callCount);
        assertEquals(2, event.counter);
    }

    @Test
    public void listenerOnlyReceivesCorrectEventType() {
        TestListener testListener = new TestListener();
        AnotherTestListener anotherListener = new AnotherTestListener();
        EventManager.register(testListener);
        EventManager.register(anotherListener);

        TestEvent testEvent = new TestEvent();
        EventManager.call(testEvent);
        assertEquals(1, testListener.callCount);
        assertFalse(anotherListener.wasCalled);

        AnotherTestEvent anotherEvent = new AnotherTestEvent();
        EventManager.call(anotherEvent);
        assertEquals(1, testListener.callCount); // still 1
        assertTrue(anotherListener.wasCalled);
    }

    @Test
    public void multiEventListenerReceivesBoth() {
        MultiEventListener listener = new MultiEventListener();
        EventManager.register(listener);

        EventManager.call(new TestEvent());
        EventManager.call(new AnotherTestEvent());

        assertEquals(1, listener.testCalls);
        assertEquals(1, listener.anotherCalls);
    }

    // --- unregister ---

    @Test(expected = UnsupportedOperationException.class)
    public void unregisterThrowsDueToCopyOnWriteArrayList() {
        // Documents a known bug: CopyOnWriteArrayList iterators don't support remove().
        // EventManager.unregister() calls iterator.remove() on CopyOnWriteArrayList,
        // which throws UnsupportedOperationException.
        TestListener listener = new TestListener();
        EventManager.register(listener);
        EventManager.unregister(listener);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void unregisterSpecificEventTypeThrowsDueToCopyOnWriteArrayList() {
        // Same bug as unregister(Object) - CopyOnWriteArrayList iterator doesn't support remove()
        MultiEventListener listener = new MultiEventListener();
        EventManager.register(listener);
        EventManager.unregister(listener, TestEvent.class);
    }

    @Test
    public void unregisterNonRegisteredDoesNotThrow() {
        TestListener listener = new TestListener();
        // unregister iterates REGISTRY_MAP.values(), which is empty for unregistered listener
        // This should NOT throw because the outer loop doesn't enter
        EventManager.unregister(listener);
    }

    // --- priority ordering ---

    @Test
    public void highPriorityCalledBeforeLow() {
        PriorityHighListener.callOrder = new ArrayList<>();
        PriorityHighListener highListener = new PriorityHighListener();
        PriorityLowListener lowListener = new PriorityLowListener();

        EventManager.register(lowListener);
        EventManager.register(highListener);

        EventManager.call(new TestEvent());

        assertEquals(2, PriorityHighListener.callOrder.size());
        assertEquals("HIGH", PriorityHighListener.callOrder.get(0));
        assertEquals("LOW", PriorityHighListener.callOrder.get(1));
    }

    // --- stoppable events ---

    @Test
    public void stoppableEventStopsProcessing() {
        StoppableListener1 listener1 = new StoppableListener1();
        StoppableListener2 listener2 = new StoppableListener2();
        EventManager.register(listener1);
        EventManager.register(listener2);

        TestStoppableEvent event = new TestStoppableEvent();
        EventManager.call(event);

        assertEquals(1, event.handleCount); // only first listener handled it
        assertTrue(event.isStopped());
    }

    // --- methods without annotation are not registered ---

    @Test
    public void methodsWithoutAnnotationAreIgnored() {
        NoAnnotationListener listener = new NoAnnotationListener();
        EventManager.register(listener);

        TestEvent event = new TestEvent();
        EventManager.call(event);
        assertEquals(0, event.counter); // listener method was not called
    }

    // --- call returns the event ---

    @Test
    public void callReturnsTheEvent() {
        TestEvent event = new TestEvent();
        Event returned = EventManager.call(event);
        assertSame(event, returned);
    }

    // --- removeEntry ---

    @Test
    public void removeEntryRemovesAllListenersForEvent() {
        TestListener listener = new TestListener();
        EventManager.register(listener);
        EventManager.removeEntry(TestEvent.class);

        TestEvent event = new TestEvent();
        EventManager.call(event);
        assertEquals(0, listener.callCount);
    }

    // --- register with specific event class ---

    @Test
    public void registerWithEventClassOnlyRegistersMatchingMethods() {
        MultiEventListener listener = new MultiEventListener();
        EventManager.register(listener, TestEvent.class);

        EventManager.call(new TestEvent());
        EventManager.call(new AnotherTestEvent());

        assertEquals(1, listener.testCalls);
        assertEquals(0, listener.anotherCalls); // not registered for this type
    }

    // --- cleanMap ---

    @Test(expected = UnsupportedOperationException.class)
    public void cleanMapTrueRemovesEmptyEntriesButUnregisterFails() {
        // Unregister itself throws due to CopyOnWriteArrayList iterator bug,
        // so cleanMap(true) is never reached
        TestListener listener = new TestListener();
        EventManager.register(listener);
        EventManager.unregister(listener);
    }

    @Test(expected = IllegalStateException.class)
    public void cleanMapFalseThrowsDueToMissingNextCall() {
        // Documents a known bug: cleanMap(false) never calls iterator.next()
        // before iterator.remove() because of short-circuit evaluation in
        // (!onlyEmptyEntries || mapIterator.next().getValue().isEmpty())
        TestListener listener = new TestListener();
        EventManager.register(listener);
        EventManager.cleanMap(false);
    }

    // --- multiple calls ---

    @Test
    public void multipleEventCallsIncrementCounter() {
        TestListener listener = new TestListener();
        EventManager.register(listener);

        EventManager.call(new TestEvent());
        EventManager.call(new TestEvent());
        EventManager.call(new TestEvent());

        assertEquals(3, listener.callCount);
    }
}

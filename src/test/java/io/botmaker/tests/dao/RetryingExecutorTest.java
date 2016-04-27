package io.botmaker.tests.dao;

import io.botmaker.simpleredis.util.RetryingExecutor;
import io.botmaker.tests.AbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class RetryingExecutorTest extends AbstractTest {

    @Parameterized.Parameters
    public static java.util.List<Object[]> data() {
        return Arrays.asList(new Object[5][0]);
    }

    @Test
    public void testFailure() {
        final int[] executions = new int[1];
        executions[0] = 0;

        final RetryingExecutor retryingExecutor = new RetryingExecutor(10, 200, new IClosure() {
            @Override
            public void execute(final Object params) throws Exception {
                assertEquals(params, "hi");

                executions[0] = executions[0] + 1;

                throw new RuntimeException("some kind of fake problems");
            }
        }, "hi");

        long time = 0;

        try {
            time = System.currentTimeMillis();
            retryingExecutor.startExecution();

        } catch (final Exception _exception) {
            time = System.currentTimeMillis() - time;

            assertEquals(executions[0], 10);
            assertTrue(time >= (160 * 10));
        }
    }

    @Test
    public void testOk() throws Exception {
        final RetryingExecutor retryingExecutor = new RetryingExecutor(10, 200, new IClosure() {
            @Override
            public void execute(final Object params) throws Exception {
                System.err.println("there");
            }
        }, "hi");

        retryingExecutor.startExecution();
    }
}

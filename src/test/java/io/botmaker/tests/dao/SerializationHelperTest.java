package io.botmaker.tests.dao;

import io.botmaker.simpleredis.dao.SerializationHelper;
import io.botmaker.simpleredis.util.RandomUtils;
import io.botmaker.tests.AbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class SerializationHelperTest extends AbstractTest {

    @Parameterized.Parameters
    public static java.util.List<Object[]> data() {
        return Arrays.asList(new Object[5][0]);
    }

    @Test
    public void testSerialization() {
        final TestClass result = (TestClass) SerializationHelper.getObjectFromBytes(
                SerializationHelper.getBytes(
                        SerializationHelper.getObjectFromBytes(
                                SerializationHelper.getBytes(testClass)
                        )

                )
        );

        assertEquals(result, testClass);
    }

    @Test
    public void testFileSerialization() throws Exception {
        File file = null;
        TestClass result = testClass;

        try {
            file = File.createTempFile(this.getClass().getName() + "-" + RandomUtils.getInstance().getRandomSafeString(20), "dat");

            SerializationHelper.saveObject2File(result, file);
            result = (TestClass) SerializationHelper.loadObjectFromFile(file);

            SerializationHelper.saveObject2File(result, file);
            result = (TestClass) SerializationHelper.loadObjectFromFile(file);

            SerializationHelper.saveObject2File(result, file);
            result = (TestClass) SerializationHelper.loadObjectFromFile(file);

            assertEquals(result, testClass);
        } finally {
            if (file != null) {
                file.deleteOnExit();
            }
        }
    }
}

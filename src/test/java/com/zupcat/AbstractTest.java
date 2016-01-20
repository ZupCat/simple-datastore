package com.zupcat;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.zupcat.sample.Address;
import com.zupcat.sample.User;
import com.zupcat.sample.UserDAO;
import com.zupcat.service.SimpleDatastoreService;
import com.zupcat.service.SimpleDatastoreServiceFactory;
import com.zupcat.util.RandomUtils;
import org.junit.After;
import org.junit.Before;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractTest {

    private static final Object LOCK_OBJECT = new Object();
    private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
    protected SimpleDatastoreService service;
    protected TestClass testClass;

    protected static List<User> buildUsers(final String uniqueId) {
        final int samples = 5;
        final List<User> result = new ArrayList<>(samples);
        final RandomUtils randomUtils = RandomUtils.getInstance();

        for (int i = 0; i < samples; i++) {
            final User sample = new User();
            sample.FIRSTNAME.set("First Name " + randomUtils.getRandomSafeString(10));
            sample.LASTNAME.set("LAST Name " + randomUtils.getRandomSafeString(10));
            sample.AGE.set(randomUtils.getIntBetweenInclusive(1, 100));
            sample.BYTES.set(new byte[]{0x42, 0x55});

            final Map<String, String> complexValue = new HashMap<>();
            complexValue.put(randomUtils.getRandomSafeAlphaNumberString(5), randomUtils.getRandomSafeAlphaNumberString(10));
            complexValue.put(randomUtils.getRandomSafeAlphaNumberString(5), randomUtils.getRandomSafeAlphaNumberString(10));
            complexValue.put(randomUtils.getRandomSafeAlphaNumberString(5), randomUtils.getRandomSafeAlphaNumberString(10));
            sample.COMPLEX_MAP_STRING_STRING.set(complexValue);

            sample.LONG_VALUE.set(randomUtils.getRandomLong());
            sample.IS_FAKE.set(randomUtils.getRandomBoolean());

            final Address address = new Address();
            address.setStreet("Sesamo Street " + randomUtils.getRandomSafeString(10));
            address.setNumber("1st " + randomUtils.getRandomSafeString(10));
            address.setOrder(randomUtils.getRandomInt(Integer.MAX_VALUE));
            sample.ADDRESS.set(address);

            sample.ADDRESSES.add(address);
            sample.ADDRESSES_MAP.put(address.getStreet(), address);

            final Address address2 = new Address();
            address2.setStreet("Sesamo Street " + randomUtils.getRandomSafeString(10));
            address2.setNumber("1st " + randomUtils.getRandomSafeString(10));
            address2.setOrder(randomUtils.getRandomInt(Integer.MAX_VALUE));
            sample.ADDRESSES.add(address2);
            sample.ADDRESSES_MAP.put(address2.getStreet(), address2);

            for (int j = 0; j < 10; j++) {
                sample.MAP_STRING_STRING.put("" + j, randomUtils.getRandomSafeString(10));
                sample.MAP_STRING_LONG.put("" + j, randomUtils.getRandomLong());
                sample.MAP_STRING_INTEGER.put("" + j, randomUtils.getRandomInt(Integer.MAX_VALUE));
                sample.LIST_STRING.add(randomUtils.getRandomSafeString(10));
                sample.LIST_INT.add(randomUtils.getRandomInt(Integer.MAX_VALUE));
                sample.LIST_LONG.add(randomUtils.getRandomLong());
            }

            result.add(sample);
        }

        final User sample = new User();
        sample.FIRSTNAME.set("hernan");
        sample.LASTNAME.set("liendo" + uniqueId);
        sample.AGE.set(18);
        sample.LONG_VALUE.set(23123213L);
        sample.IS_FAKE.set(false);

        result.add(sample);

        return result;
    }

    @Before
    public void setUp() throws Exception {
        synchronized (LOCK_OBJECT) {
            helper.setUp();

            service = SimpleDatastoreServiceFactory.getSimpleDatastoreService();
            service.registerDAO(new UserDAO());

            testClass = new TestClass();
            testClass.other = new TestClass();

            final RandomUtils randomUtils = RandomUtils.getInstance();

            testClass.s = randomUtils.getRandomSafeAlphaNumberString(5);
            testClass.i = randomUtils.getRandomInt(1000000);
            testClass.l = randomUtils.getRandomLong();

            testClass.other.s = randomUtils.getRandomSafeAlphaNumberString(5);
            testClass.other.i = randomUtils.getRandomInt(1000000);
            testClass.other.l = randomUtils.getRandomLong();
        }
    }

//    @BeforeClass
//    public static void oneTimeSetUp() {
//    }

//    @AfterClass
//    public static void oneTimeTearDown() {
//    }

    @After
    public void tearDown() throws Exception {
    }

    public static final class TestClass implements Serializable {

        private static final long serialVersionUID = 471847964351314234L;

        public String s;
        public int i;
        public long l;
        public TestClass other;

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final TestClass testClass = (TestClass) o;

            return i == testClass.i && l == testClass.l && !(other != null ? !other.equals(testClass.other) : testClass.other != null) && !(s != null ? !s.equals(testClass.s) : testClass.s != null);
        }

        @Override
        public int hashCode() {
            int result = s != null ? s.hashCode() : 0;
            result = 31 * result + i;
            result = 31 * result + (int) (l ^ (l >>> 32));
            result = 31 * result + (other != null ? other.hashCode() : 0);
            return result;
        }
    }
}

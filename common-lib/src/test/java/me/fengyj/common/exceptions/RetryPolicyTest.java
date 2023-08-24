package me.fengyj.common.exceptions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RetryPolicyTest {

    @Test
    public void test_fixed_interval() {

        var policy = new RetryPolicy(2, 0.1);

        var ex = Assertions.assertThrows(RetrievableException.class, () -> policy.run(() -> {
            throw new RetrievableException(ErrorSeverity.Info, null, null);
        }, null, null));

        Assertions.assertEquals(2, ex.getTriedTimes());

        ex = Assertions.assertThrows(RetrievableException.class, () -> policy.get(() -> {
            throw new RetrievableException(ErrorSeverity.Info, null, null);
        }, null, null));

        Assertions.assertEquals(2, ex.getTriedTimes());
    }

    @Test
    public void test_jitter_interval() {

        RetryPolicy policy = new RetryPolicy(4, 0.1, RetryPolicy.AdditionIntervalPolicy.Jitter);

        var ex = Assertions.assertThrows(RetrievableException.class, () -> policy.run(() -> {
            throw new RetrievableException(ErrorSeverity.Info, null, null);
        }, null, null));

        Assertions.assertEquals(4, ex.getTriedTimes());

        ex = Assertions.assertThrows(RetrievableException.class, () -> policy.get(() -> {
            throw new RetrievableException(ErrorSeverity.Info, null, null);
        }, null, null));

        Assertions.assertEquals(4, ex.getTriedTimes());
    }

    @Test
    public void test_increasing_interval() {

        RetryPolicy policy = new RetryPolicy(
                5,
                0.1,
                RetryPolicy.AdditionIntervalPolicy.Increasing);

        var ex = Assertions.assertThrows(RetrievableException.class, () -> policy.run(() -> {
            throw new RetrievableException(ErrorSeverity.Info, null, null);
        }, null, null));

        Assertions.assertEquals(5, ex.getTriedTimes());

        ex = Assertions.assertThrows(RetrievableException.class, () -> policy.get(() -> {
            throw new RetrievableException(ErrorSeverity.Info, null, null);
        }, null, null));

        Assertions.assertEquals(5, ex.getTriedTimes());
    }

    @Test
    public void test_jitter_and_increasing_interval() {

        RetryPolicy policy = new RetryPolicy(
                3,
                0.1,
                RetryPolicy.AdditionIntervalPolicy.Jitter,
                RetryPolicy.AdditionIntervalPolicy.Increasing);

        var ex = Assertions.assertThrows(RetrievableException.class, () -> policy.run(() -> {
            throw new RetrievableException(ErrorSeverity.Info, null, null);
        }, null, null));

        Assertions.assertEquals(3, ex.getTriedTimes());

        ex = Assertions.assertThrows(RetrievableException.class, () -> policy.get(() -> {
            throw new RetrievableException(ErrorSeverity.Info, null, null);
        }, null, null));

        Assertions.assertEquals(3, ex.getTriedTimes());
    }
}

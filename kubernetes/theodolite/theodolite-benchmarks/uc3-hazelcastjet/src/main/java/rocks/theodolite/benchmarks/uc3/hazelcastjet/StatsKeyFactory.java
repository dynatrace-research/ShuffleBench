package rocks.theodolite.benchmarks.uc3.hazelcastjet;

import java.time.LocalDateTime;

/**
 * Factory interface for creating a stats key from a sensor id and a {@link LocalDateTime} object
 * and vice versa.
 *
 * @param <T> Type of the key
 */
public interface StatsKeyFactory<T> {

  T createKey(String sensorId, LocalDateTime dateTime);

  String getSensorId(T key);

}

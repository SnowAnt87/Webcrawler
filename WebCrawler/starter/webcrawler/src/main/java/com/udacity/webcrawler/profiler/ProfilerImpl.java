package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    if (!isProfiled(klass)) {
      throw new IllegalArgumentException("Class " + klass.getName() + " is not annotated with @Profiled");
    }

    // Create a dynamic proxy that intercepts method calls
    return (T) Proxy.newProxyInstance(
        klass.getClassLoader(),
        new Class<?>[]{klass},
        new ProfilingMethodInterceptor<>(delegate, clock, state)
    );
  }

  /**
   * Checks if any method in the class is annotated with @Profiled.
   */
  private <T> boolean isProfiled(Class<T> klass) {
    for (Method method : klass.getDeclaredMethods()) {
      if (method.isAnnotationPresent(Profiled.class)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void writeData(Path path) {
    try (Writer writer = Files.newBufferedWriter(path, Files.exists(path) 
        ? java.nio.file.StandardOpenOption.APPEND 
        : java.nio.file.StandardOpenOption.CREATE)) {
      writeData(writer);
    } catch (IOException e) {
      throw new RuntimeException("Failed to write profiling data", e);
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }

  /**
   * Invocation handler for profiling method execution.
   */
  private static class ProfilingMethodInterceptor<T> implements InvocationHandler {

    private final T delegate;
    private final Clock clock;
    private final ProfilingState state;

    ProfilingMethodInterceptor(T delegate, Clock clock, ProfilingState state) {
      this.delegate = delegate;
      this.clock = clock;
      this.state = state;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isAnnotationPresent(Profiled.class)) {
            long startTime = clock.millis();
            try {
                // Attempt to invoke the actual method on the delegate object
                return method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                // Unwrap the original exception and propagate it
                throw e.getCause();
            } catch (IllegalAccessException e) {
                // Wrap IllegalAccessException in a RuntimeException
                throw new RuntimeException("Failed to access method: " + method.getName(), e);
            } finally {
                long endTime = clock.millis();
                Duration duration = Duration.ofMillis(endTime - startTime);
                state.record(delegate.getClass(), method, duration);
            }
        } else {
            // Method is not annotated with @Profiled, just invoke normally
            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                // Unwrap and propagate the original exception
                throw e.getCause();
            } catch (IllegalAccessException e) {
                // Wrap IllegalAccessException in a RuntimeException
                throw new RuntimeException("Failed to access method: " + method.getName(), e);
            }
        }
    }

  }
}

package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Clock;
import java.util.Objects;
import java.time.Duration;
import java.lang.reflect.InvocationTargetException;



/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object delegate; // The object being profiled
  private final ProfilingState state; // Tracks profiling data

  ProfilingMethodInterceptor(Object delegate, Clock clock, ProfilingState state) {
    this.delegate = Objects.requireNonNull(delegate);
    this.clock = Objects.requireNonNull(clock);
    this.state = Objects.requireNonNull(state);
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

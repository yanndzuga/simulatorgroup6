package de.frauas.group6.traffic.simulator.core;

/**
 * Functional interface for TraaS commands that return a value.
 * Allows handling checked exceptions cleanly inside lambdas.
 * @param <T> The return type
 */
@FunctionalInterface
public interface TraasCommand<T> {
    T execute() throws Exception;
}
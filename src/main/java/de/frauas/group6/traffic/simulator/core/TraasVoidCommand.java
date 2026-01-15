package de.frauas.group6.traffic.simulator.core;

/**
 * Functional interface for TraaS commands that do not return a value (void).
 * Allows handling checked exceptions cleanly.
 */
@FunctionalInterface
public interface TraasVoidCommand {
    void execute() throws Exception;
}
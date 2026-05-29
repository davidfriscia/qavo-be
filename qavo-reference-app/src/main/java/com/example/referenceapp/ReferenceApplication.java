/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package com.example.referenceapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Qavo reference application.
 *
 * <p>Note how little this class does: importing {@code qavo-starter-web} plus the two auth plugins
 * activates security, error handling, structured logging, validation, OpenAPI, and the
 * registration/login endpoints through auto-configuration. The application code in the sibling
 * packages focuses purely on its own domain (a small "widget" catalog), following the platform's
 * {@code api / application / domain / infrastructure} layering (architecture &sect;4, &sect;7).
 */
@SpringBootApplication
public class ReferenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReferenceApplication.class, args);
    }
}

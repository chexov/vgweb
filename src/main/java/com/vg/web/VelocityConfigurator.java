package com.vg.web;

import static org.apache.velocity.runtime.RuntimeConstants.INPUT_ENCODING;
import static org.apache.velocity.runtime.RuntimeConstants.OUTPUT_ENCODING;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

public class VelocityConfigurator extends ResourceLoader {

    private static final String VELOCITY_WEB_ROOT = "velocity.web.root";
    private ClassLoader classLoader;

    public static void configure(File logRoot, String webRoot) {
        Velocity.setProperty(VELOCITY_WEB_ROOT, webRoot);
        Velocity.setProperty(Velocity.RESOURCE_LOADER, "class");
        Velocity.setProperty("class.resource.loader.class", VelocityConfigurator.class.getName());
        Velocity.setProperty("class.resource.loader.cache", false);
        Velocity.setProperty("class.resource.loader.modificationCheckInterval", "0");
        Velocity.setProperty("runtime.log", "/tmp/velocity_" + System.currentTimeMillis() + ".log");
        Velocity.setProperty(Velocity.UBERSPECT_CLASSNAME, FieldAwareUberspect.class.getName());
        Velocity.setProperty(INPUT_ENCODING, "utf-8");
        Velocity.setProperty(OUTPUT_ENCODING, "utf-8");
        try {
            Velocity.init();
        } catch (Exception e) {
            throw new RuntimeException("Velocity.init failed", e);
        }
    }

    public void init(ExtendedProperties configuration) {
        this.classLoader = this.getClass().getClassLoader();
    }

    public InputStream getResourceStream(String resourceName) throws ResourceNotFoundException {
        InputStream result = null;

        if (StringUtils.isEmpty(resourceName)) {
            throw new ResourceNotFoundException("No template name provided");
        }

        try {
            URL url = classLoader.getResource(resourceName);
            if (url == null)
                url = classLoader.getResource(Velocity.getProperty(VELOCITY_WEB_ROOT) + resourceName);
            URLConnection conn = url.openConnection();
            result = conn.getInputStream();
        } catch (Exception fnfe) {
            throw new ResourceNotFoundException("Resource not found: " + resourceName);
        }

        if (result == null) {
            String msg = "ClasspathResourceLoader Error: cannot find resource " + resourceName;

            throw new ResourceNotFoundException(msg);
        }

        return result;
    }

    public boolean isSourceModified(Resource resource) {
        return false;
    }

    public long getLastModified(Resource resource) {
        return resource.getLastModified();
    }

}

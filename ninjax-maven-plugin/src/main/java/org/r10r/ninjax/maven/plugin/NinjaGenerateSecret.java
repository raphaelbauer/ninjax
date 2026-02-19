package org.r10r.ninjax.maven.plugin;


import java.security.SecureRandom;
import java.util.Base64;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.r10r.ninjax.core.NinjaConstants;

@Mojo(name = "generateSecret", defaultPhase = LifecyclePhase.NONE, threadSafe = true)
public class NinjaGenerateSecret extends AbstractMojo {

    @Override
    public void execute() {
        try {
            byte[] keyBytes = new byte[32]; // 32 bytes = 256-bit for HS256
            new SecureRandom().nextBytes(keyBytes);

            String secretString = Base64.getEncoder().encodeToString(keyBytes);

            getLog().info("Generated HS256 secret that is useful as '" + NinjaConstants.NINJA_APPLICATION_SECRET_KEY + "':");
            getLog().info(secretString);
        } catch (Exception e) {
            getLog().error("Failed to generate HS256 secret", e);
        }
    }
}
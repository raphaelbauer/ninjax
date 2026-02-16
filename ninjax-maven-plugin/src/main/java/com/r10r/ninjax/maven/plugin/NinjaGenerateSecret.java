package com.r10r.ninjax.maven.plugin;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import javax.crypto.SecretKey;
import com.r10r.ninjax.core.NinjaConstants;

@Mojo(name = "generateSecret", defaultPhase = LifecyclePhase.NONE, threadSafe = true)
public class NinjaGenerateSecret extends AbstractMojo {

    @Override
    public void execute() {
        try {
            SecretKey key = Jwts.SIG.HS256.key().build();
            String secretString = Encoders.BASE64.encode(key.getEncoded());

            getLog().info("Generated secret that is useful as '" + NinjaConstants.NINJA_APPLICATION_SECRET_KEY + "':");
            getLog().info(secretString);
        } catch (Exception e) {
            getLog().error("Failed to generate HS256 secret", e);
        }
    }
}
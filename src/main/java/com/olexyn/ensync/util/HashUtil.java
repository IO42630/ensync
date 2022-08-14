package com.olexyn.ensync.util;

import com.olexyn.ensync.lock.LockKeeper;
import com.olexyn.min.log.LogU;

import java.math.BigInteger;
import java.nio.channels.Channels;
import java.nio.file.Path;
import java.security.MessageDigest;

public class HashUtil {

    public static String getHash(Path path) {
        var thisFc = LockKeeper.getFc(path);
        try (var is = Channels.newInputStream(thisFc)) {
            var m = MessageDigest.getInstance("SHA256");
            byte[] buffer = new byte[262144];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                m.update(buffer, 0, bytesRead);
            }
            var i = new BigInteger(1, m.digest());
            return String.format("%1$032X", i);
        } catch (Exception e) {
            LogU.warnPlain("Failed to create Hash.\n%s", e.getMessage());
            return null;
        }
    }

}

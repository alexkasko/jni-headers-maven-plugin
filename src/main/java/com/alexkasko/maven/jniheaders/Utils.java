package com.alexkasko.maven.jniheaders;

import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;

/**
 * Internal utilities
 *
 * @author alexkasko
 * Date: 4/11/13
 */
class Utils {
    /**
     * Search fore JDK directory that contains {@code javap} and {@code javah}
     *
     * @return JDK directory
     * @throws IOException if JDK directory was not found
     */
    static File jdkHome(Log log) throws IOException {
        // current JAVA_HOME
        String javaHomeProp = System.getProperty("java.home");
        log.debug("getProperty(\"java.home\"): [" + javaHomeProp + "]");
        if(null != javaHomeProp) {
            File javaHome = new File(javaHomeProp);
            if(valid(javaHome)) return javaHome;
            if(valid(javaHome.getParentFile())) return javaHome.getParentFile();
        }
        // search in env
        String javaHomeEnv = System.getenv("JAVA_HOME");
        log.debug("System.getenv(\"JAVA_HOME\"): [" + javaHomeEnv + "]");
        if(null != javaHomeEnv) {
            File javaHome = new File(javaHomeEnv);
            if (valid(javaHome)) return javaHome;
            if (valid(javaHome.getParentFile())) return javaHome.getParentFile();
        }
        String jdkHomeEnv = System.getenv("JDK_HOME");
        log.debug("System.getenv(\"JDK_HOME\"): [" + jdkHomeEnv + "]");
        if(null != jdkHomeEnv) {
            File javaHome = new File(jdkHomeEnv);
            if (valid(javaHome)) return javaHome;
            if (valid(javaHome.getParentFile())) return javaHome.getParentFile();
        }
        throw new IOException("Cannot find JDK_HOME directory with $JDK_HOME/bin/javap and" +
                "$JDK_HOME/bin/javah utilities, please check JDK_HOME environment variable");
    }

    private static boolean valid(File dir) {
        if(null == dir || !dir.exists() || !dir.isDirectory()) return false;
        File javah = new File(dir, "bin/javah");
        File javap = new File(dir, "bin/javap");
        if(javah.exists() && javah.isFile() && javap.exists() && javap.isFile()) return true;
        File javahExe = new File(dir, "bin/javah.exe");
        File javapExe = new File(dir, "bin/javap.exe");
        return javahExe.exists() && javahExe.isFile() && javapExe.exists() && javapExe.isFile();
    }
}

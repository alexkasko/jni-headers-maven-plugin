package com.alexkasko.maven.jniheaders;

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
    static File jdkHome() throws IOException {
        // current JAVA_HOME
        String javaHomeProp = System.getProperty("java.home");
        if(null != javaHomeProp) {
            File javaHome = new File(javaHomeProp);
            if(valid(javaHome)) return javaHome;
            if(valid(javaHome.getParentFile())) return javaHome.getParentFile();
        }
        // search in env
        String javaHomeEnv = System.getenv("JAVA_HOME");
        if(null != javaHomeEnv) {
            File javaHome = new File(javaHomeEnv);
            if (valid(javaHome)) return javaHome;
            if (valid(javaHome.getParentFile())) return javaHome.getParentFile();
        }
        String jdkHomeEnv = System.getenv("JDK_HOME");
        if(null != jdkHomeEnv) {
            File javaHome = new File(jdkHomeEnv);
            if (valid(javaHome)) return javaHome;
            if (valid(javaHome.getParentFile())) return javaHome.getParentFile();
        }
        throw new IOException("Cannot find JDK home directory, check JDK_HOME property");
    }

    private static boolean valid(File dir) {
        if(null != dir && !dir.exists() && dir.isDirectory()) return false;
        File javah = new File(dir, "bin/javah");
        File javap = new File(dir, "bin/javap");
        return javah.exists() && javah.isFile() && javap.exists() && javap.isFile();
    }
}
